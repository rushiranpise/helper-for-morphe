package dev.rushi.apkdownloadhelper.sources

import dev.rushi.apkdownloadhelper.ui.HelperRequest
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

/**
 * Which APKMirror page a resolved URL points at.
 *
 * Only the shapes this helper can continue from are listed: a variant page (one build,
 * already picked for the device's ABI by the resolver) and a release page (still needs the
 * variant choice). Anything else - an app page, a search results page, or a Google search
 * URL the resolver falls back to when it finds nothing - is not a shortcut.
 */
internal enum class ApkMirrorPage { VARIANT, RELEASE }

private const val APKMIRROR_HOST = "www.apkmirror.com"
private const val RESOLVER_HOST = "api.morphe.software"
private const val RESOLVED_LINK_TTL_MILLIS = 10L * 60L * 1000L
private const val RESOLVED_LINK_CACHE_SIZE = 64

/**
 * Small in-memory cache for resolver answers.
 *
 * Every source asks the same question for one request, and a fallback source asks it
 * again after the first source fails. The TTL stays short because a resolver answer can
 * be a signed CDN link whose signature expires.
 */
internal class ResolvedLinkCache(
    private val ttlMillis: Long = RESOLVED_LINK_TTL_MILLIS,
    private val maxEntries: Int = RESOLVED_LINK_CACHE_SIZE
) {
    private data class Entry(val url: String, val storedAt: Long)

    // Access order makes the first key the least recently used one, so eviction does
    // not throw away a link that a later source is about to ask for again.
    private val entries = LinkedHashMap<String, Entry>(16, 0.75f, true)

    @Synchronized
    fun get(key: String, now: Long = System.currentTimeMillis()): String? {
        val entry = entries[key] ?: return null
        if (now - entry.storedAt > ttlMillis) {
            entries.remove(key)
            return null
        }
        return entry.url
    }

    @Synchronized
    fun put(key: String, url: String, now: Long = System.currentTimeMillis()) {
        entries[key] = Entry(url, now)
        while (entries.size > maxEntries) {
            entries.remove(entries.keys.first())
        }
    }

    @Synchronized
    fun clear() = entries.clear()
}

/**
 * The resolver queries for [request], best first.
 *
 * Each is `pkg~version~abi`, with a build code appended when the request carries one.
 * Morphe added the build code (and an optional form factor after it) so the resolver
 * can pick one APKMirror variant instead of a release page. The helper has no form
 * factor in the intent.
 *
 * A patch source can declare one build per ABI (Hushfacebook names an arm64-v8a build
 * and an armeabi-v7a build for the same version), and Morphe then sends every code
 * without saying which is which. Dropping them all and asking for `pkg~version~abi`
 * alone lets the resolver pick its default variant for the ABI, which need not be any
 * of the builds the patches want. So there is one query per code, in the order Morphe
 * sent them, and the bare three-part query last as the fallback it always was.
 */
internal fun morpheResolveQueries(request: HelperRequest): List<String> {
    val version = request.requestedVersionNames.firstOrNull()?.takeIf(String::isNotBlank)
        ?: request.versionName?.trim()?.takeIf(String::isNotBlank)
        ?: "any"
    val abi = request.availableAbis.firstOrNull()?.takeIf(String::isNotBlank) ?: "arm64-v8a"
    val base = listOf(request.packageName, version, abi)
    val buildCodes = (listOfNotNull(request.versionCode) + request.versionCodes)
        .filter { it > 0L }
        .distinct()
    return buildCodes.map { code -> (base + code.toString()).joinToString("~") } +
        base.joinToString("~")
}

/** The best of [morpheResolveQueries]: the query a request with at most one build code sends. */
internal fun morpheResolveQuery(request: HelperRequest): String =
    morpheResolveQueries(request).first()

/**
 * Whether a resolver answer settles on one build: an APKMirror variant page or an
 * Uptodown file page. A release page, an app page or the resolver's Google fallback
 * still leaves the build to be chosen.
 */
internal fun resolvedUrlNamesOneBuild(url: String): Boolean =
    apkMirrorPageOf(url) == ApkMirrorPage.VARIANT || uptodownResolvedPageOf(url) != null

/** The host of [url], lowercased, or null when it cannot be read. */
internal fun urlHostOf(url: String): String? =
    runCatching { java.net.URI(url).host?.lowercase(Locale.US) }.getOrNull()

/**
 * The APKMirror page kind of [url], or null when it is not an APKMirror page worth
 * continuing from.
 */
internal fun apkMirrorPageOf(url: String): ApkMirrorPage? {
    if (urlHostOf(url) != APKMIRROR_HOST) return null
    val path = runCatching { java.net.URI(url).path.orEmpty() }.getOrNull().orEmpty()
    return when {
        path.contains("-android-apk-download") -> ApkMirrorPage.VARIANT
        path.contains("-release") -> ApkMirrorPage.RELEASE
        else -> null
    }
}

/** A Uptodown file page: one build of one app, its file id in the path. */
internal data class UptodownResolvedPage(val url: String, val fileId: String)

private val UPTODOWN_FILE_PAGE_PATH = Regex("""/android/download/(\d+)(?:-x)?/?$""")

/**
 * The Uptodown file page the resolver points at, or null for anything else it answers
 * with (an APKMirror page, the site's generic search, a Google fallback).
 *
 * The resolver answers with an app's own subdomain (`instagram.en.uptodown.com`) and a
 * file id, so the page it names is one specific build - the same shape our variant walk
 * builds by hand. Requiring the app subdomain keeps a generic `en.uptodown.com` listing
 * from being mistaken for one.
 */
internal fun uptodownResolvedPageOf(url: String): UptodownResolvedPage? {
    val host = urlHostOf(url) ?: return null
    if (!host.endsWith(".uptodown.com") || host.startsWith("www.")) return null
    // <app>.<lang>.uptodown.com: the app slug is what makes the URL specific.
    if (host.count { it == '.' } < 3) return null
    val path = runCatching { java.net.URI(url).path.orEmpty() }.getOrNull() ?: return null
    val match = UPTODOWN_FILE_PAGE_PATH.find(path) ?: return null
    return UptodownResolvedPage(url, match.groupValues[1])
}

/**
 * The release page a variant page belongs to, e.g.
 * `.../youtube-21-20-405-release/youtube-21-20-405-4-android-apk-download/` ->
 * `.../youtube-21-20-405-release/`.
 */
internal fun apkMirrorReleaseUrlOfVariantPage(url: String): String? {
    val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return null
    val path = uri.path.orEmpty()
    val trimmed = path.trimEnd('/')
    if (!trimmed.contains("-android-apk-download")) return null
    val parent = trimmed.substringBeforeLast('/')
    if (parent.isEmpty()) return null
    return buildString {
        append(uri.scheme ?: "https")
        append("://")
        append(uri.host ?: APKMIRROR_HOST)
        append(parent)
        append('/')
    }
}

/** The app page a release page belongs to, used to strip the app slug from the version. */
internal fun apkMirrorAppPageOfReleaseUrl(releaseUrl: String): String? {
    val uri = runCatching { java.net.URI(releaseUrl) }.getOrNull() ?: return null
    val trimmed = uri.path.orEmpty().trimEnd('/')
    val parent = trimmed.substringBeforeLast('/', "")
    if (parent.isEmpty()) return null
    return "${uri.scheme ?: "https"}://${uri.host ?: APKMIRROR_HOST}$parent/"
}

/**
 * Asks Morphe's resolver where its own download dialog would go.
 *
 * The manager resolves a request to a concrete source page with a chain of HEAD requests,
 * so a helper following the same chain lands on the page directly instead of walking a
 * site's search, app, uploads and release pages itself. It sends no more than a package
 * name, version and ABI.
 *
 * Returns the final URL when it leaves the resolver for another host, and null when the
 * chain does not produce one - callers then scrape as before, which is also what the
 * setting is for.
 */
internal object MorpheResolver {

    private const val API = "https://$RESOLVER_HOST"

    /** Morphe's own dialog identifies itself this way; anything else is refused. */
    private const val USER_AGENT = "Morphe-Manager/1.0"

    private const val MAX_HOPS = 6

    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val cache = ResolvedLinkCache()
    private val inFlight = ConcurrentHashMap<String, FutureTask<String?>>()

    /**
     * The page the resolver names for [request], or null when no query produces one.
     *
     * With several build codes, the first query whose answer names one build wins, so
     * a code the resolver does not know (or a code for another ABI that it answers with
     * a release page) does not stop a later code from landing on its variant page. When
     * no query names a build, the first answer of any kind is kept, as before.
     */
    fun resolve(request: HelperRequest): String? {
        val queries = morpheResolveQueries(request)
        var fallback: String? = null
        for (query in queries) {
            val resolved = resolveQuery(query) ?: continue
            if (queries.size == 1 || resolvedUrlNamesOneBuild(resolved)) return resolved
            if (fallback == null) fallback = resolved
        }
        return fallback
    }

    private fun resolveQuery(query: String): String? {
        cache.get(query)?.let { return it }

        // Sources can resolve in parallel. Coalesce the same query so they wait
        // for one resolver chain instead of each starting their own.
        val task = FutureTask { resolveUncached(query) }
        val running = inFlight.putIfAbsent(query, task)
        if (running != null) {
            return runCatching { running.get() }.getOrNull()
        }
        return try {
            task.run()
            val resolved = task.get()
            if (resolved != null) cache.put(query, resolved)
            resolved
        } catch (error: Exception) {
            null
        } finally {
            inFlight.remove(query, task)
        }
    }

    private fun resolveUncached(query: String): String? = runCatching {
        var url = "$API/v2/web-search/" + URLEncoder.encode(query, "UTF-8")
            .replace("+", "%20")
            .replace("%7E", "~")

        repeat(MAX_HOPS) {
            val response = client.newCall(
                Request.Builder().url(url).header("User-Agent", USER_AGENT).head().build()
            ).execute()
            response.use { result ->
                val location = result.header("Location")
                if (location.isNullOrBlank()) {
                    // No further hop: this is the answer, but only if it left the API.
                    if (urlHostOf(url) == RESOLVER_HOST) return null
                    return url
                }
                val next = runCatching { java.net.URI(url).resolve(location).toString() }
                    .getOrNull() ?: return null
                if (urlHostOf(next) != RESOLVER_HOST) return next
                url = next
            }
        }
        null
    }.getOrNull()
}

