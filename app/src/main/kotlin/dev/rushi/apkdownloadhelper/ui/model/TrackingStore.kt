package dev.rushi.apkdownloadhelper.ui.model

import android.content.Context
import android.content.SharedPreferences
import dev.rushi.apkdownloadhelper.updater.ArchiveApp

/**
 * Tracked packages and their last-seen index state.
 *
 * The baseline is deliberately separate from the current index: the tracked
 * list needs to keep counting until the user actually opens an app, so simply
 * fetching the index cannot clear the badges.
 */
internal object TrackingStore {
    private const val PREFS = "morphe_tracking"
    private const val KEY_PACKAGES = "packages"
    private const val SNAPSHOT_PREFIX = "snapshot:"
    private const val NOTIFIED_PREFIX = "notified:"

    fun load(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_PACKAGES, emptySet()).orEmpty().toSet()

    /** Adds a package that may not be in the index yet, without a baseline. */
    fun trackPackage(context: Context, packageName: String): Set<String> {
        val normalized = trackingPackageKey(packageName)
        if (normalized.isEmpty()) return load(context)

        val current = load(context).toMutableSet()
        if (!current.add(normalized)) return current

        prefs(context).edit()
            .putStringSet(KEY_PACKAGES, current)
            .clearTrackingState(normalized)
            .apply()
        return current
    }

    fun untrack(context: Context, packageName: String): Set<String> {
        val key = trackingPackageKey(packageName)
        val current = load(context).toMutableSet()
        current.remove(key)
        prefs(context).edit()
            .putStringSet(KEY_PACKAGES, current)
            .clearTrackingState(key)
            .apply()
        return current
    }

    fun loadSnapshots(
        context: Context,
        packages: Set<String> = load(context)
    ): Map<String, TrackedAppSnapshot> {
        val prefs = prefs(context)
        return packages.mapNotNull { packageName ->
            val versionsKey = versionsKey(packageName)
            val patchesKey = patchesKey(packageName)
            if (!prefs.contains(versionsKey) && !prefs.contains(patchesKey)) {
                return@mapNotNull null
            }
            packageName to TrackedAppSnapshot(
                versions = prefs.getStringSet(versionsKey, emptySet()).orEmpty().toSet(),
                patches = prefs.getStringSet(patchesKey, emptySet()).orEmpty().toSet()
            )
        }.toMap()
    }

    fun toggle(context: Context, app: ArchiveApp): Set<String> {
        val prefs = prefs(context)
        val packageName = trackingPackageKey(app.packageName)
        val current = load(context).toMutableSet()
        val editor = prefs.edit()
        if (current.add(packageName)) {
            val snapshot = app.trackingSnapshot()
            editor.putStringSet(versionsKey(packageName), snapshot.versions)
            editor.putStringSet(patchesKey(packageName), snapshot.patches)
            editor.putStringSet(notifiedVersionsKey(packageName), snapshot.versions)
            editor.putStringSet(notifiedPatchesKey(packageName), snapshot.patches)
        } else {
            current.remove(packageName)
            editor.clearTrackingState(packageName)
        }
        editor.putStringSet(KEY_PACKAGES, current)
        editor.apply()
        return current
    }

    /** Seeds the seen baseline for packages that just appeared in the index. */
    fun seedSeen(context: Context, apps: Iterable<ArchiveApp>) {
        if (apps.none()) return
        val editor = prefs(context).edit()
        apps.forEach { app ->
            val packageName = trackingPackageKey(app.packageName)
            val snapshot = app.trackingSnapshot()
            editor.putStringSet(versionsKey(packageName), snapshot.versions)
            editor.putStringSet(patchesKey(packageName), snapshot.patches)
        }
        editor.commit()
    }

    /** Records the app's current state as seen, clearing its badges. */
    fun markSeen(context: Context, app: ArchiveApp) {
        val packageName = trackingPackageKey(app.packageName)
        if (packageName !in load(context)) return
        val snapshot = app.trackingSnapshot()
        prefs(context).edit()
            .putStringSet(versionsKey(packageName), snapshot.versions)
            .putStringSet(patchesKey(packageName), snapshot.patches)
            .putStringSet(notifiedVersionsKey(packageName), snapshot.versions)
            .putStringSet(notifiedPatchesKey(packageName), snapshot.patches)
            .apply()
    }

    /**
     * State covered by the last background notification. It is separate from
     * the seen snapshot so a repeated check does not notify for changes the
     * user has not opened yet.
     */
    fun loadNotifiedSnapshots(
        context: Context,
        packages: Set<String> = load(context)
    ): Map<String, TrackedAppSnapshot> {
        val prefs = prefs(context)
        return packages.mapNotNull { packageName ->
            val versionsKey = notifiedVersionsKey(packageName)
            val patchesKey = notifiedPatchesKey(packageName)
            if (!prefs.contains(versionsKey) && !prefs.contains(patchesKey)) {
                return@mapNotNull null
            }
            packageName to TrackedAppSnapshot(
                versions = prefs.getStringSet(versionsKey, emptySet()).orEmpty().toSet(),
                patches = prefs.getStringSet(patchesKey, emptySet()).orEmpty().toSet()
            )
        }.toMap()
    }

    /** Advances the notification baseline after a notification was posted. */
    fun markNotified(context: Context, apps: Iterable<ArchiveApp>) {
        val editor = prefs(context).edit()
        apps.forEach { app ->
            val packageName = trackingPackageKey(app.packageName)
            val snapshot = app.trackingSnapshot()
            editor.putStringSet(notifiedVersionsKey(packageName), snapshot.versions)
            editor.putStringSet(notifiedPatchesKey(packageName), snapshot.patches)
        }
        // The worker can be stopped right after this returns, so write the
        // dedupe baseline synchronously instead of leaving it to an async flush.
        editor.commit()
    }

    /** Keeps a second AppBrowserScreen in sync when tracking changes elsewhere. */
    fun observe(context: Context, onChanged: () -> Unit): AutoCloseable {
        val prefs = prefs(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == KEY_PACKAGES || key.startsWith(SNAPSHOT_PREFIX)) {
                onChanged()
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        return AutoCloseable { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private const val KEY_CACHED_APPS_JSON = "cached_tracked_apps_json"

    fun cacheTrackedApps(context: Context, allApps: List<ArchiveApp>) {
        val trackedSet = load(context)
        val trackedApps = allApps.filter { trackingPackageKey(it.packageName) in trackedSet }
        val array = org.json.JSONArray()
        trackedApps.forEach { app ->
            array.put(
                org.json.JSONObject().apply {
                    put("packageName", app.packageName)
                    put("name", app.name)
                    put("iconColor", app.iconColor ?: "")
                    put("iconUrl", app.iconUrl ?: "")
                }
            )
        }
        prefs(context).edit().putString(KEY_CACHED_APPS_JSON, array.toString()).apply()
    }

    fun loadCachedTrackedApps(context: Context): List<ArchiveApp> {
        val raw = prefs(context).getString(KEY_CACHED_APPS_JSON, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = org.json.JSONArray(raw)
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val pkg = obj.optString("packageName").takeIf { it.isNotBlank() } ?: continue
                    add(
                        ArchiveApp(
                            packageName = pkg,
                            name = obj.optString("name").ifBlank { pkg },
                            iconColor = obj.optString("iconColor").ifBlank { null },
                            iconUrl = obj.optString("iconUrl").ifBlank { null }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun versionsKey(packageName: String) = "$SNAPSHOT_PREFIX$packageName:versions"

    private fun patchesKey(packageName: String) = "$SNAPSHOT_PREFIX$packageName:patches"

    private fun notifiedVersionsKey(packageName: String) =
        "$NOTIFIED_PREFIX$packageName:versions"

    private fun notifiedPatchesKey(packageName: String) =
        "$NOTIFIED_PREFIX$packageName:patches"

    private fun SharedPreferences.Editor.clearTrackingState(
        packageName: String
    ): SharedPreferences.Editor {
        remove(versionsKey(packageName))
        remove(patchesKey(packageName))
        remove(notifiedVersionsKey(packageName))
        remove(notifiedPatchesKey(packageName))
        return this
    }
}
