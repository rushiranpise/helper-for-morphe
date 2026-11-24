package dev.rushi.apkdownloadhelper.ui.model

import dev.rushi.apkdownloadhelper.download.sanitizeFileName
import dev.rushi.apkdownloadhelper.ui.screen.captcha.urlDecoded
import java.util.Locale

internal data class CandidateDownloadFile(
    val url: String,
    val fileName: String,
    val size: Long? = null,
    val referer: String? = null,
    val cookieHeader: String? = null,
    // The exact browser identity that earned the captured cookies. APKMirror's
    // Cloudflare clearance is tied to the user agent, so the download must
    // introduce itself the same way the WebView did.
    val userAgent: String? = null,
    // The SHA-256 the source publishes for the file, when it does. When set,
    // the downloaded bytes are checked against it before handoff, catching
    // corrupted or wrong builds outright instead of only a package/version
    // mismatch. (Sources that publish it: APKMirror variant page, Uptodown
    // download page.)
    val expectedSha256: String? = null
)

internal data class BrowserDownloadCapture(
    val downloadUrl: String,
    val refererUrl: String? = null,
    val cookieHeader: String? = null,
    val userAgent: String? = null
)

private val GENERIC_BROWSER_DOWNLOAD_NAMES = setOf(
    "download",
    "download.asp",
    "download.aspx",
    "download.cgi",
    "download.jsp",
    "download.php",
    "download.pl"
)

internal fun capturedDownloadFileName(
    candidate: DownloadCandidate,
    downloadUrl: String,
    fileKind: String
): String {
    val decodedUrl = downloadUrl.urlDecoded()
    val path = runCatching { java.net.URI(decodedUrl).path }.getOrDefault("")
    val fileName = path.substringAfterLast('/').takeIf { it.isNotBlank() && it != "/" }
    val usableFileName = fileName
        ?.takeIf { it.contains('.') }
        ?.takeUnless { it.lowercase(Locale.US) in GENERIC_BROWSER_DOWNLOAD_NAMES }
    val extension = usableFileName
        ?.substringAfterLast('.', "")
        ?.takeIf { it.isNotBlank() }
        ?: fileKind
    val baseName = usableFileName
        ?: "${candidate.packageName}-${candidate.versionName ?: "download"}.$extension"
    return baseName.sanitizeFileName()
}

internal data class DownloadedApkMetadata(
    val packageName: String,
    val versionName: String?,
    val versionCode: Long?,
    val minSdkVersion: Int? = null
)
