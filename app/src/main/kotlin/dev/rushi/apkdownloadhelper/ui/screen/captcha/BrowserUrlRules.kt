package dev.rushi.apkdownloadhelper.ui.screen.captcha

import android.webkit.CookieManager
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Percent-decodes a URL without touching `+` (which is a literal plus in a path) and
 * without `Uri.decode`, so these predicates stay usable off-device in unit tests.
 */
internal fun String.urlDecoded(): String = runCatching {
    URLDecoder.decode(replace("+", "%2B"), StandardCharsets.UTF_8.name())
}.getOrDefault(this)

/** True for URLs that point at an APK-family file (extension or filename hint). */
internal fun String.looksLikeApkDownload(): Boolean {
    val decoded = urlDecoded().lowercase(Locale.US)
    return Regex("""\.(apk|apks|apkm|xapk)(\?|#|$)""").containsMatchIn(decoded) ||
        Regex("""filename[^.]*\.(apk|apks|apkm|xapk)""").containsMatchIn(decoded)
}

/**
 * Uptodown's file links carry no extension at all (`dw.uptodown.com/dwn/<token>`), so the
 * extension test alone misses them: the in-app browser saw a navigation to a URL it could
 * not recognise as a file and let the page handle it.
 */
internal fun String.isUptodownFileUrl(): Boolean {
    val uri = runCatching { java.net.URI(urlDecoded()) }.getOrNull() ?: return false
    val host = uri.host?.lowercase(Locale.US) ?: return false
    if (!host.endsWith("uptodown.com") && !host.endsWith("uptodown.net")) return false
    return uri.path.orEmpty().contains("/dwn/", ignoreCase = true)
}

/**
 * Cookies the browser holds for a captured file URL, falling back to the page
 * that produced it. The downloader imports this header into its own jar; it is
 * the same session that passed the challenge, so the file hop must reuse it.
 */
internal fun browserCookieHeader(downloadUrl: String, referer: String?): String? {
    val manager = CookieManager.getInstance()
    return manager.getCookie(downloadUrl)?.takeIf(String::isNotBlank)
        ?: referer?.let { manager.getCookie(it) }?.takeIf(String::isNotBlank)
}

/**
 * APKMirror's final file endpoint. The link carries no extension; the site
 * answers it with a redirect to the CDN, so it is safe to capture here.
 */
internal fun String.isApkMirrorFileUrl(): Boolean {
    val uri = runCatching { java.net.URI(urlDecoded()) }.getOrNull() ?: return false
    val host = uri.host?.lowercase(Locale.US) ?: return false
    val apkMirrorHost = host == "apkmirror.com" || host.endsWith(".apkmirror.com")
    return apkMirrorHost && uri.path.orEmpty().contains("download.php", ignoreCase = true)
}

/**
 * APKPure's direct file endpoint (`d.apkpure.com/b/APK/<pkg>`). Capturing it
 * lets the downloader follow the signed CDN redirect with the page's cookies.
 */
internal fun String.isApkPureFileUrl(): Boolean {
    val uri = runCatching { java.net.URI(urlDecoded()) }.getOrNull() ?: return false
    val host = uri.host?.lowercase(Locale.US) ?: return false
    val apkPureHost = host == "apkpure.com" || host.endsWith(".apkpure.com")
    return apkPureHost && uri.path.orEmpty().trim('/').startsWith("b/", ignoreCase = true)
}

/** A URL the in-app browser should treat as the file it was opened to capture. */
internal fun String.isCapturableDownload(): Boolean =
    looksLikeApkDownload() || isUptodownFileUrl() || isApkMirrorFileUrl() || isApkPureFileUrl()

/** Navigation sent by a page script when its build code is not the requested one. */
internal fun String.isBrowserBuildMismatchUrl(): Boolean =
    startsWith("morphe-helper://build-mismatch", ignoreCase = true)

internal fun String?.browserBuildMismatchMessage(expected: String?): String =
    "Download page is build ${this?.takeIf(String::isNotBlank) ?: "unknown"}, " +
        "expected ${expected?.takeIf(String::isNotBlank) ?: "the requested build"}."

/** APKCube uses its own catalogue and browser flow. */
internal fun DownloadSource.isApkCubeBrowserFlow(): Boolean =
    this == DownloadSource.APK_CUBE

/**
 * APKCube's download flow stays on its own site. Off-site links on the download
 * page are prelander ads, so the in-app browser must not follow them.
 */
internal fun String.isApkCubeHostedUrl(): Boolean {
    val uri = runCatching { java.net.URI(urlDecoded()) }.getOrNull() ?: return false
    val host = uri.host?.lowercase(Locale.US) ?: return false
    return host == "apkcube.com" || host.endsWith(".apkcube.com")
}

/**
 * Navigation allowed during an APKCube download: the catalogue itself, its
 * file hosts, and direct file links. Every other host is a prelander ad.
 */
internal fun String.isApkCubeAllowedNavigation(): Boolean {
    if (isApkCubeHostedUrl() || looksLikeApkDownload()) return true
    val uri = runCatching { java.net.URI(urlDecoded()) }.getOrNull() ?: return false
    val host = uri.host?.lowercase(Locale.US) ?: return false
    if (
        host == "cloudflarestorage.com" ||
        host.endsWith(".cloudflarestorage.com") ||
        host == "r2.dev" ||
        host.endsWith(".r2.dev")
    ) {
        return true
    }
    // APKCube's file endpoint is not always on a CDN host and can carry no
    // extension. Only a real file endpoint is allowed here; generic `/download`
    // paths on unknown hosts are prelander pages.
    val path = uri.path.orEmpty().lowercase(Locale.US)
    return path.endsWith(".apk") ||
        path.endsWith(".apks") ||
        path.endsWith(".apkm") ||
        path.endsWith(".xapk") ||
        path.contains("download.php")
}
