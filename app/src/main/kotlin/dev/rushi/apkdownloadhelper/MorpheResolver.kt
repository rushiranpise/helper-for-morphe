package dev.rushi.apkdownloadhelper

import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

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

/** `pkg~version~abi`, the query form Morphe's own download dialog uses. */
internal fun morpheResolveQuery(request: HelperRequest): String {
    val version = request.requestedVersionNames.firstOrNull()?.takeIf(String::isNotBlank)
        ?: request.versionName?.trim()?.takeIf(String::isNotBlank)
        ?: "any"
    val abi = request.availableAbis.firstOrNull()?.takeIf(String::isNotBlank) ?: "arm64-v8a"
    return "${request.packageName}~$version~$abi"
}

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

    fun resolve(request: HelperRequest): String? = runCatching {
        var url = "$API/v2/web-search/" + URLEncoder.encode(morpheResolveQuery(request), "UTF-8")
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
