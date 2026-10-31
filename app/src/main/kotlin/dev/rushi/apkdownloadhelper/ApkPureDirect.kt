package dev.rushi.apkdownloadhelper

import java.net.URLDecoder
import java.util.Base64
import java.util.Locale

/**
 * APKPure's direct file endpoint.
 *
 * The web flow costs three HTML pages (`/apk-info/<pkg>` -> `/versions` ->
 * `/downloading/<version>`) before it names a file. `d.apkpure.com/b/<kind>/<pkg>`
 * answers with a single redirect straight to the signed CDN URL, which already
 * carries the file name, its version and its size. That is the endpoint APKPure's
 * own client uses, and it is the whole reason their APKPure path is one request.
 *
 * Only the parts this helper needs are modelled here: the URL to ask, and the
 * facts to read out of the redirect.
 */
private const val APK_PURE_DIRECT_HOST = "d.apkpure.com"

/** The redirect target APKPure hands back for one build. */
internal data class ApkPureDirectFile(
    val url: String,
    val versionName: String?,
    val versionCode: Long?,
    val fileKind: String,
    val sizeBytes: Long?
)

/**
 * `https://d.apkpure.com/b/APK/com.example?versionCode=123`, or `?version=latest`
 * when no build is pinned. [kind] is `APK` or `XAPK`.
 */
internal fun apkPureDirectUrl(packageName: String, kind: String, versionCode: Long?): String =
    buildString {
        append("https://")
        append(APK_PURE_DIRECT_HOST)
        append("/b/")
        append(kind)
        append('/')
        append(packageName)
        append(
            if (versionCode != null && versionCode > 0L) {
                "?versionCode=$versionCode"
            } else {
                "?version=latest"
            }
        )
    }

/**
 * Reads APKPure's redirect target.
 *
 * Returns null for anything that is not a signed `*.winudf.com` file URL (the
 * `https://apkpure.com` bounce it sends for an unknown package, an empty answer,
 * a captive-portal page), so callers treat "no shortcut" and "different app"
 * the same way: fall back to the web flow.
 */
internal fun apkPureDirectFile(location: String?, packageName: String): ApkPureDirectFile? {
    val url = location?.trim()?.normalizedHttpUrlOrNull() ?: return null
    val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return null
    val host = uri.host?.lowercase(Locale.US).orEmpty()
    if (!host.endsWith("winudf.com")) return null

    val segments = uri.path.orEmpty().trim('/').split('/')
    val fileKind = when (segments.firstOrNull()?.uppercase(Locale.US)) {
        "APK" -> "apk"
        "XAPK" -> "xapk"
        else -> return null
    }
    val params = apkPureQueryParameters(uri.rawQuery)

    // The URL names its app in a query parameter and again inside the encoded path;
    // either one contradicting the request means APKPure answered with another app.
    val namedPackage = params["package_name"]?.takeIf(String::isNotBlank)
    if (namedPackage != null &&
        !namedPackage.equals(packageName, ignoreCase = true)
    ) {
        return null
    }
    val encodedPackage = apkPureEncodedPathPackage(segments.getOrNull(1))
    if (encodedPackage != null && encodedPackage != packageName) return null

    return ApkPureDirectFile(
        url = url,
        versionName = apkPureVersionFromFileName(params["filename"]),
        versionCode = apkPureVersionCodeFromPath(segments.getOrNull(1))
            ?: params["versionCode"]?.toLongOrNull(),
        fileKind = fileKind,
        sizeBytes = params["full_size"]?.toLongOrNull()
    )
}

/**
 * `Instagram_453.0.0.57.79_APKPure.apk` -> `453.0.0.57.79`.
 *
 * APKPure puts the app label first and a fixed `_APKPure` suffix last, so the
 * version is the segment between them.
 */
internal fun apkPureVersionFromFileName(fileName: String?): String? {
    val base = fileName
        ?.trim()
        ?.substringBeforeLast('.', "")
        ?.takeIf(String::isNotBlank)
        ?: return null
    val withoutSuffix = base.substringBeforeLast("_APKPure", base)
    val version = withoutSuffix.substringAfterLast('_').trim()
    return version.takeIf { it.isNotEmpty() && it != withoutSuffix }
}

/**
 * The build code APKPure hides in its encoded path segment:
 * `com.instagram.android_385914557_eab9f104` -> `385914557`.
 *
 * Reading it from the redirect means the exact build is known without a second
 * request, so a candidate can be checked against the requested code before any
 * bytes are fetched.
 */
internal fun apkPureVersionCodeFromPath(rawPathSegment: String?): Long? {
    val decoded = decodeUrlSafeBase64(rawPathSegment) ?: return null
    val parts = decoded.split('_')
    if (parts.size < 3) return null
    return parts[parts.size - 2].toLongOrNull()
}

/** The package name hidden in the same encoded path segment. */
internal fun apkPureEncodedPathPackage(rawPathSegment: String?): String? {
    val decoded = decodeUrlSafeBase64(rawPathSegment) ?: return null
    val pkg = decoded.substringBefore('_').trim()
    return pkg.takeIf { it.contains('.') }
}

private fun decodeUrlSafeBase64(rawPathSegment: String?): String? {
    val raw = rawPathSegment?.trim()?.takeIf(String::isNotEmpty) ?: return null
    // The segment is emitted without padding, which the decoder rejects.
    val padded = raw + "=".repeat((4 - raw.length % 4) % 4)
    return runCatching {
        String(Base64.getUrlDecoder().decode(padded), Charsets.UTF_8)
    }.getOrNull()
}

private fun apkPureQueryParameters(rawQuery: String?): Map<String, String> =
    rawQuery
        .orEmpty()
        .split('&')
        .mapNotNull { pair ->
            val key = pair.substringBefore('=', "").trim()
            if (key.isEmpty()) {
                null
            } else {
                key to URLDecoder.decode(pair.substringAfter('=', ""), "UTF-8")
            }
        }
        .toMap()
