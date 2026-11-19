package dev.rushi.apkdownloadhelper.ui

import android.content.Context
import android.content.SharedPreferences
import dev.rushi.apkdownloadhelper.updater.ArchiveApp

/** What an app looked like when the user last saw it. */
internal data class TrackedAppSnapshot(
    val versions: Set<String> = emptySet(),
    val patches: Set<String> = emptySet()
)

/** New versions and patch names since [TrackedAppSnapshot]. */
internal data class TrackedAppChanges(
    val updates: Int = 0,
    val newPatches: Int = 0
)

private fun cleanNames(values: Iterable<String?>): Set<String> = values
    .mapNotNull { it?.trim() }
    .filter { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
    .toSet()

internal fun ArchiveApp.trackingSnapshot(): TrackedAppSnapshot = TrackedAppSnapshot(
    versions = cleanNames(versions + patchDetails.flatMap { it.versions }),
    patches = cleanNames(patches + patchDetails.map { it.name })
)

internal fun ArchiveApp.trackedChanges(snapshot: TrackedAppSnapshot?): TrackedAppChanges {
    if (snapshot == null) return TrackedAppChanges()
    val current = trackingSnapshot()
    return TrackedAppChanges(
        updates = (current.versions - snapshot.versions).size,
        newPatches = (current.patches - snapshot.patches).size
    )
}

/**
 * Tracked packages and their last-seen index state.
 *
 * The baseline is deliberately separate from the current index: the tracked
 * list needs to keep counting until the user actually opens an app, so simply
 * fetching the index cannot clear the badges.
 */
internal object MorpheTracking {
    private const val PREFS = "morphe_tracking"
    private const val KEY_PACKAGES = "packages"
    private const val SNAPSHOT_PREFIX = "snapshot:"

    fun load(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_PACKAGES, emptySet()).orEmpty().toSet()

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
        val current = load(context).toMutableSet()
        val editor = prefs.edit()
        if (current.add(app.packageName)) {
            val snapshot = app.trackingSnapshot()
            editor.putStringSet(versionsKey(app.packageName), snapshot.versions)
            editor.putStringSet(patchesKey(app.packageName), snapshot.patches)
        } else {
            current.remove(app.packageName)
            editor.remove(versionsKey(app.packageName))
            editor.remove(patchesKey(app.packageName))
        }
        editor.putStringSet(KEY_PACKAGES, current)
        editor.apply()
        return current
    }

    /** Records the app's current state as seen, clearing its badges. */
    fun markSeen(context: Context, app: ArchiveApp) {
        if (app.packageName !in load(context)) return
        val snapshot = app.trackingSnapshot()
        prefs(context).edit()
            .putStringSet(versionsKey(app.packageName), snapshot.versions)
            .putStringSet(patchesKey(app.packageName), snapshot.patches)
            .apply()
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

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun versionsKey(packageName: String) = "$SNAPSHOT_PREFIX$packageName:versions"

    private fun patchesKey(packageName: String) = "$SNAPSHOT_PREFIX$packageName:patches"
}
