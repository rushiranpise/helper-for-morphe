package dev.rushi.apkdownloadhelper.updater

import android.content.Context
import android.util.Log
import com.google.gson.annotations.SerializedName
import dev.rushi.apkdownloadhelper.gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Models + fetcher for the Morphe archive index that powers the "Find New
 * Apps" findapps. The index is deliberately NOT cached: every open fetches the
 * current JSON live so the list always reflects the archive right now.
 */
internal data class MorpheArchiveData(
    @SerializedName("generatedAt") val generatedAt: String? = null,
    @SerializedName("repoCount") val repoCount: Int = 0,
    @SerializedName("appCount") val appCount: Int = 0,
    @SerializedName("patchCount") val patchCount: Int = 0,
    @SerializedName("universalPatchCount") val universalPatchCount: Int = 0,
    @SerializedName("repos") val repos: List<MorpheRepoRef> = emptyList(),
    @SerializedName("apps") val apps: List<ArchiveApp> = emptyList()
)

/**
 * One fetch of the index: the apps, plus when the archive built it. The build
 * time matters because a source can change its patch set between runs, and the
 * index goes on describing the repo as it was at build time.
 */
internal data class MorpheArchiveIndex(
    val generatedAt: String? = null,
    val repoCount: Int = 0,
    val appCount: Int = 0,
    val patchCount: Int = 0,
    val universalPatchCount: Int = 0,
    val apps: List<ArchiveApp> = emptyList()
)

internal data class ArchiveApp(
    @SerializedName("packageName") val packageName: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("patches") val patches: List<String> = emptyList(),
    @SerializedName("patchDetails") val patchDetails: List<ArchivePatch> = emptyList(),
    @SerializedName("versions") val versions: List<String> = emptyList(),
    @SerializedName("sources") val sources: List<ArchiveSource> = emptyList(),
    @SerializedName("iconColor") val iconColor: String? = null,
    @SerializedName("iconUrl") val iconUrl: String? = null
) {
    val patchCount: Int
        get() = patches.size

    /** Number of distinct source repos/bundles offering patches for this app. */
    val sourceCount: Int
        get() = sources.size

    @Transient
    private var isIndexed: Boolean = false

    @Transient
    var nameLower: String = ""
        private set

    @Transient
    var packageLower: String = ""
        private set

    @Transient
    var searchBlob: String = ""
        private set

    fun ensureSearchIndex(): ArchiveApp {
        if (!isIndexed) {
            nameLower = name.lowercase(Locale.US)
            packageLower = packageName.lowercase(Locale.US)
            val patchNames = patches.map { it.lowercase(Locale.US) }
            val detailNames = patchDetails.map { it.name.lowercase(Locale.US) }
            val detailDesc = patchDetails.mapNotNull { it.description?.lowercase(Locale.US) }
            val repoNames = sources.map { it.repo.lowercase(Locale.US) }
            val sourcePatchNames = sources.flatMap { it.patches }.map { it.name.lowercase(Locale.US) }
            val sourcePatchDesc = sources.flatMap { it.patches }
                .mapNotNull { it.description?.lowercase(Locale.US) }
            searchBlob = buildString {
                append(nameLower).append(' ')
                append(packageLower).append(' ')
                repoNames.forEach { append(it).append(' ') }
                patchNames.forEach { append(it).append(' ') }
                detailNames.forEach { append(it).append(' ') }
                detailDesc.forEach { append(it).append(' ') }
                sourcePatchNames.forEach { append(it).append(' ') }
                sourcePatchDesc.forEach { append(it).append(' ') }
            }
            isIndexed = true
        }
        return this
    }

    fun matchesQuery(query: String): Boolean {
        if (query.isBlank()) return true
        ensureSearchIndex()
        val q = query.trim().lowercase(Locale.US)
        return searchBlob.contains(q)
    }

    fun scoreMatch(queryLower: String): Int {
        ensureSearchIndex()
        return when {
            nameLower.startsWith(queryLower) -> 1000
            packageLower.startsWith(queryLower) -> 800
            nameLower.contains(queryLower) -> 600
            packageLower.contains(queryLower) -> 400
            sources.any { it.repo.lowercase(Locale.US).contains(queryLower) } -> 300
            patches.any { it.lowercase(Locale.US).contains(queryLower) } ||
                sources.any { source ->
                    source.patches.any { it.name.lowercase(Locale.US).contains(queryLower) }
                } -> 200
            patchDetails.any {
                it.name.lowercase(Locale.US).contains(queryLower) ||
                    it.description?.lowercase(Locale.US)?.contains(queryLower) == true
            } || sources.any { source ->
                source.patches.any {
                    it.description?.lowercase(Locale.US)?.contains(queryLower) == true
                }
            } -> 150
            else -> 100
        }
    }
}

internal data class ArchivePatch(
    @SerializedName("name") val name: String = "",
    @SerializedName("description") val description: String? = null,
    @SerializedName("versions") val rawVersions: List<String> = emptyList(),
    @SerializedName("compatiblePackages") val compatiblePackages: List<ArchiveCompatiblePackage> = emptyList(),
    @Transient val versions: List<String> = emptyList()
)

internal data class ArchiveCompatiblePackage(
    @SerializedName("packageName") val packageName: String? = null,
    @SerializedName("versions") val versions: List<String>? = null,
    @SerializedName("targets") val targets: List<ArchivePatchTarget>? = null
)

internal data class ArchivePatchTarget(
    @SerializedName("version") val version: String? = null
)

internal data class ArchiveSource(
    @SerializedName("repo") val repo: String = "",
    @SerializedName("host") val host: String? = null,
    @SerializedName("webUrl") val webUrl: String? = null,
    @SerializedName("addUrl") val addUrl: String? = null,
    @SerializedName("listUrl") val listUrl: String? = null,
    @SerializedName("patches") val patches: List<ArchivePatch> = emptyList(),
    /** The repo's latest release, used to rank otherwise identical sources. */
    @SerializedName("latestChanges") val latestChanges: ArchiveChanges? = null
)

internal data class MorpheRepoRef(
    @SerializedName("repo") val repo: String = "",
    @SerializedName("listUrl") val listUrl: String? = null
)

/** A repo's most recent release, as published in its patch bundle. */
internal data class ArchiveChanges(
    @SerializedName("title") val title: String = "",
    @SerializedName("date") val date: String? = null
)

/** One patch set for an app: the repo carrying it, plus any repos carrying the same one. */
internal data class ArchiveSourceGroup(
    val primary: ArchiveSource,
    val mirrors: List<ArchiveSource>
)

/**
 * Collapses [sources] into one entry per distinct patch set.
 *
 * A fork re-releases its upstream's patches under its own name, so the index
 * lists both and a mirror ends up looking like an independent choice. The set of
 * patch names is what identifies a group, so identical sets in a different order
 * still count as one.
 *
 * The newest release leads each group, and the groups, so the maintained copy is
 * the one on top. Ties keep the index's own order, the sorts being stable.
 */
internal fun groupArchiveSources(sources: List<ArchiveSource>): List<ArchiveSourceGroup> {
    val byPatchSet = LinkedHashMap<String, MutableList<ArchiveSource>>()
    sources.forEach { source ->
        val key = source.patches.map { it.name }.sorted().joinToString("\u0000")
        byPatchSet.getOrPut(key) { mutableListOf() }.add(source)
    }
    return byPatchSet.values
        .map { members ->
            val ranked = members.sortedByDescending { it.latestChanges?.date.orEmpty() }
            ArchiveSourceGroup(primary = ranked.first(), mirrors = ranked.drop(1))
        }
        .sortedByDescending { it.primary.latestChanges?.date.orEmpty() }
}

/**
 * Fetches the Morphe archive index over the network on every call.
 * Returns the apps list, or throws so the caller can surface an error state.
 */
/** How old the index looks to the user, and whether that age is a problem. */
internal data class ArchiveFreshness(val label: String, val stale: Boolean)

/**
 * The archive rebuilds daily, so anything past two days means the run itself has
 * stopped rather than the build simply being recent.
 */
private const val STALE_AFTER_MS = 48L * 60L * 60L * 1000L

/**
 * How long ago the index was built, phrased for the findapps header. Returns null
 * when the timestamp is missing or unreadable, so the caller shows nothing rather
 * than claiming a freshness it cannot know.
 */
internal fun archiveFreshness(
    generatedAt: String?,
    now: Long = System.currentTimeMillis()
): ArchiveFreshness? {
    val builtAt = parseArchiveTimestamp(generatedAt) ?: return null
    val ageMs = (now - builtAt).coerceAtLeast(0L)
    val minutes = ageMs / 60_000L
    val stale = ageMs > STALE_AFTER_MS
    val label = when {
        minutes < 1L -> "just now"
        minutes < 60L -> "${minutes} min ago"
        // Hours only hold up while the daily rebuild is merely late; once it has
        // stopped, the day it was built says more than an ever-growing count.
        !stale -> "${minutes / 60L} h ago"
        else -> SimpleDateFormat("d MMM", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(builtAt))
    }
    return ArchiveFreshness(label = label, stale = stale)
}

/**
 * A source's newest release, phrased for its card: the bundle version and the
 * day it was published, e.g. `v1.21.5 · 8 Sep 2026`.
 *
 * Both come from the repo's own changelog entry rather than the index build, so a
 * source whose row has gone stale still shows the release it actually ships. That
 * is what makes the newest-release-first ordering visible instead of implied.
 *
 * Returns null when the bundle declares no release at all, rather than inventing one.
 */
internal fun formatBundleRelease(changes: ArchiveChanges?): String? {
    val version = changes?.title?.substringBefore(" (")?.trim()
        .takeIf { !it.isNullOrEmpty() }
        // Bundles write the version bare; the `v` reads as a version rather than a
        // number in a line that also carries a patch count and a date.
        ?.let { if (it.first().isDigit()) "v$it" else it }
    val date = formatReleaseDate(releaseDateOf(changes))
    return listOfNotNull(version, date).takeIf { it.isNotEmpty() }?.joinToString(" \u00b7 ")
}

/** Extracts just the bundle version from [changes], e.g. `v2.9.0`. */
internal fun bundleVersionOf(changes: ArchiveChanges?): String? =
    changes?.title?.substringBefore(" (")?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { if (it.first().isDigit()) "v$it" else it }

/**
 * The day a repo's changelog entry was published, as the bundle writes it.
 *
 * The dedicated date field wins; the parenthesised part of the title covers the
 * bundles that never set one. Null when the entry says nothing either way.
 */
internal fun releaseDateOf(changes: ArchiveChanges?): String? {
    val field = changes?.date?.trim().orEmpty()
    if (field.isNotEmpty()) return field
    val title = changes?.title.orEmpty()
    return title.substringAfter('(', "").substringBefore(')').trim().takeIf { it.isNotEmpty() }
}

/** Well-formed dates only, so a stray changelog string cannot decide an ordering. */
private val ISO_DATE = Regex("""\d{4}-\d{2}-\d{2}""")

/**
 * The newest release date any of the app's sources declares.
 *
 * Kept as the ISO date so it sorts chronologically as plain text, and so the list
 * can show an index entry whose sources have moved on since the last archive build.
 * Null when no source declares one, which drops the app to the end of that sort
 * rather than inventing a date for it.
 */
internal fun ArchiveApp.newestReleaseDate(): String? = sources
    .mapNotNull { releaseDateOf(it.latestChanges) }
    .filter { ISO_DATE.matches(it) }
    .maxOrNull()

/** `2026-09-08` as `8 Sep 2026`, or the value unchanged when it is not a date. */
internal fun formatReleaseDate(value: String?): String? {
    val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val utc = TimeZone.getTimeZone("UTC")
    val parsed = runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .apply {
                timeZone = utc
                isLenient = false
            }
            .parse(raw)
    }.getOrNull() ?: return raw
    // Formatted in UTC as well: the bundle's date is a calendar day, so rendering
    // it in local time would name the previous day anywhere west of Greenwich.
    return SimpleDateFormat("d MMM yyyy", Locale.US)
        .apply { timeZone = utc }
        .format(parsed)
}

/** The archive's own `%Y-%m-%d %H:%M UTC` stamp, or null when it is not that. */
private fun parseArchiveTimestamp(value: String?): Long? {
    val trimmed = value?.trim()?.removeSuffix("UTC")?.trim() ?: return null
    if (trimmed.isEmpty()) return null
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            .apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }
            .parse(trimmed)
            ?.time
    }.getOrNull()
}

internal object MorpheArchive {

    const val INDEX_URL =
        "https://raw.githubusercontent.com/rushiforai/morphe-archive/refs/heads/main/docs/data.json"

    private const val TAG = "MorpheArchive"
    private const val CONNECT_TIMEOUT_S = 10L
    private const val READ_TIMEOUT_S = 45L

    /** Shared client: used for the index fetch and for source avatars. */
    val http = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    @Volatile
    private var patchListCache = java.util.concurrent.ConcurrentHashMap<String, List<ArchivePatch>>()

    /** Call after a successful index refresh to force fresh patch lists next fetch. */
    fun invalidatePatchListCache() {
        // Replace the map instead of clearing it so requests already in flight
        // cannot write a stale response back into the live cache.
        patchListCache = java.util.concurrent.ConcurrentHashMap()
    }

    /**
     * Fetches a repo's `patches-list.json` (from `listUrl`) and returns a map of
     * `patchName -> supportedVersions` strictly for [packageName].
     */
    suspend fun fetchPatchVersions(
        source: ArchiveSource,
        packageName: String
    ): Map<String, List<String>>? = withContext(Dispatchers.IO) {
        val candidateUrls = buildList {
            source.listUrl?.takeIf { it.isNotBlank() }?.let(::add)
            if (source.repo.contains('/')) {
                if (source.host?.lowercase() == "gitlab.com") {
                    add("https://gitlab.com/${source.repo}/-/raw/main/patches-list.json")
                    add("https://gitlab.com/${source.repo}/-/raw/master/patches-list.json")
                } else {
                    add("https://raw.githubusercontent.com/${source.repo}/main/patches-list.json")
                    add("https://raw.githubusercontent.com/${source.repo}/master/patches-list.json")
                }
            }
        }.distinct()

        if (candidateUrls.isEmpty()) return@withContext null

        val cache = patchListCache
        var listPatches: List<ArchivePatch> = emptyList()
        for (url in candidateUrls) {
            val cached = cache[url]
            if (cached != null) {
                listPatches = cached
                break
            }
            val fetched = runCatching {
                val req = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .build()
                http.newCall(req).execute().use { res ->
                    if (!res.isSuccessful) return@use emptyList()
                    val json = res.body?.string()?.trim().orEmpty()
                    if (json.isEmpty()) return@use emptyList()
                    val listType = object : com.google.gson.reflect.TypeToken<List<ArchivePatch>>() {}.type
                    if (json.startsWith("[")) {
                        gson.fromJson<List<ArchivePatch>>(json, listType).orEmpty()
                    } else {
                        val rootObj = com.google.gson.JsonParser.parseString(json).asJsonObject
                        val arr = listOf("patches", "items", "data")
                            .firstNotNullOfOrNull { key ->
                                rootObj.get(key)?.takeIf { it.isJsonArray }?.asJsonArray
                            }
                        if (arr != null) {
                            gson.fromJson<List<ArchivePatch>>(arr, listType).orEmpty()
                        } else {
                            emptyList()
                        }
                    }
                }
            }.getOrDefault(emptyList())

            if (fetched.isNotEmpty()) {
                cache[url] = fetched
                listPatches = fetched
                break
            }
        }

        if (listPatches.isEmpty()) return@withContext null

        listPatches.mapNotNull { patch ->
            val compatiblePackages = patch.compatiblePackages.orEmpty()
            val matchingPackages = compatiblePackages.filter {
                it.packageName.isNullOrBlank() || it.packageName.equals(packageName, ignoreCase = true)
            }
            // A patch that only names other packages is not part of this app's set.
            if (compatiblePackages.isNotEmpty() && matchingPackages.isEmpty()) {
                return@mapNotNull null
            }
            val targetVersions = matchingPackages.flatMap { it.targets.orEmpty() }
                .mapNotNull { it.version?.trim()?.takeIf { v -> v.isNotEmpty() && v != "null" } }
            val compatVersions = matchingPackages.flatMap { it.versions.orEmpty() }
                .mapNotNull { it.trim().takeIf { v -> v.isNotEmpty() && v != "null" } }
            val directVersions = patch.rawVersions.orEmpty()
                .mapNotNull { it.trim().takeIf { v -> v.isNotEmpty() && v != "null" } }

            patch.name.trim() to (directVersions + targetVersions + compatVersions).distinct()
        }.toMap()
    }

    private const val CACHE_FILE_NAME = "morphe_archive_cache.json"

    fun loadCachedIndex(context: Context): MorpheArchiveIndex? {
        return runCatching {
            val file = File(context.filesDir, CACHE_FILE_NAME)
            if (!file.exists() || file.length() == 0L) return null
            val body = file.readText()
            val parsed = gson.fromJson(body, MorpheArchiveData::class.java) ?: return null
            hydrateIndex(parsed)
        }.getOrNull()
    }

    /**
     * Replaces the cache in one step: a failed or interrupted write leaves the
     * previous index in place instead of truncating it.
     */
    private fun writeCacheFile(context: Context, body: String) {
        val directory = context.filesDir
        val temporary = File(directory, "$CACHE_FILE_NAME.tmp")
        try {
            temporary.writeText(body)
            check(temporary.renameTo(File(directory, CACHE_FILE_NAME))) {
                "Could not replace $CACHE_FILE_NAME"
            }
        } finally {
            temporary.delete()
        }
    }

    private fun hydrateIndex(parsed: MorpheArchiveData): MorpheArchiveIndex {
        val listUrlByRepo = parsed.repos
            .filter { it.repo.isNotBlank() && !it.listUrl.isNullOrBlank() }
            .associate { it.repo.lowercase(Locale.US) to it.listUrl }
        val hydratedApps = parsed.apps.map { app ->
            fun ArchivePatch.extractVersions(): List<String> {
                val matchingPackages = compatiblePackages.filter {
                    it.packageName.isNullOrBlank() || it.packageName.equals(app.packageName, ignoreCase = true)
                }
                val targetVersions = matchingPackages.flatMap { it.targets.orEmpty() }
                    .mapNotNull { it.version?.trim()?.takeIf { v -> v.isNotEmpty() && v != "null" } }
                val compatVersions = matchingPackages.flatMap { it.versions.orEmpty() }
                    .mapNotNull { it.trim().takeIf { v -> v.isNotEmpty() && v != "null" } }
                val directVersions = rawVersions
                    .mapNotNull { it.trim().takeIf { v -> v.isNotEmpty() && v != "null" } }
                return (directVersions + targetVersions + compatVersions).distinct()
            }

            val detailsByName = app.patchDetails.associate { it.name to it.extractVersions() }

            fun ArchivePatch.withResolvedVersions(): ArchivePatch {
                val direct = extractVersions()
                val resolved = direct.ifEmpty { detailsByName[name].orEmpty() }
                return copy(versions = resolved)
            }

            app.copy(
                patchDetails = app.patchDetails.map { it.withResolvedVersions() },
                sources = app.sources.map { source ->
                    source.copy(
                        listUrl = source.listUrl?.takeIf { it.isNotBlank() }
                            ?: listUrlByRepo[source.repo.lowercase(Locale.US)],
                        patches = source.patches.map { it.withResolvedVersions() }
                    )
                }
            ).ensureSearchIndex()
        }

        return MorpheArchiveIndex(
            generatedAt = parsed.generatedAt,
            repoCount = parsed.repoCount,
            appCount = parsed.appCount.takeIf { it > 0 } ?: hydratedApps.size,
            patchCount = parsed.patchCount,
            universalPatchCount = parsed.universalPatchCount,
            apps = hydratedApps
        )
    }

    suspend fun fetchIndex(context: Context? = null): MorpheArchiveIndex = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(INDEX_URL)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Mobile Safari/537.36"
            )
            .header("Accept", "application/json")
            .build()
        try {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Index request failed: HTTP ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                val parsed = gson.fromJson(body, MorpheArchiveData::class.java)
                    ?: error("Index response was empty")
                if (parsed.apps.isEmpty()) {
                    error("Index response contained no apps")
                }
                val index = hydrateIndex(parsed)
                if (context != null) {
                    runCatching { writeCacheFile(context, body) }
                        .onFailure { Log.w(TAG, "Could not cache the app index", it) }
                }
                index
            }
        } catch (e: Exception) {
            if (context != null) {
                val cached = loadCachedIndex(context)
                if (cached != null) return@withContext cached
            }
            throw e
        }
    }

    /**
     * A source's avatar image URL. GitHub exposes {owner}.png directly;
     * GitLab needs an ID-based URL we can't derive, so it falls back to null
     * (the UI then shows a letter tile).
     */
    fun avatarUrlFor(source: ArchiveSource): String? {
        val owner = source.repo.substringBefore('/').takeIf { it.isNotBlank() } ?: return null
        return when (source.host?.lowercase()) {
            "github.com" -> "https://github.com/$owner.png?size=96"
            else -> null
        }
    }
}

