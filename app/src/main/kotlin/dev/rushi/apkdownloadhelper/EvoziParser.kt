package dev.rushi.apkdownloadhelper

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** APKCube's own catalogue, entered directly instead of through Evozi. */
internal class ApkCubeParser(
    ctx: SourceParserContext
) : ApkCubeSourceParser(
    ctx = ctx,
    source = DownloadSource.APK_CUBE,
    captchaNote = APKCUBE_CAPTCHA_NOTE
)

/** Evozi's APK Downloader, which hands off to the same APKCube catalogue. */
internal class EvoziParser(
    ctx: SourceParserContext
) : ApkCubeSourceParser(
    ctx = ctx,
    source = DownloadSource.EVOZI,
    captchaNote = EVOZI_CAPTCHA_NOTE
)

/**
 * Resolves apps and per-version download pages in APKCube's catalogue. The actual
 * APK file is gated behind a Cloudflare Turnstile captcha, so this parser resolves
 * the app and version and hands the user a candidate for the exact download page.
 */
internal abstract class ApkCubeSourceParser(
    private val ctx: SourceParserContext,
    override val source: DownloadSource,
    private val captchaNote: String
) : ApkSourceParser {

    override fun searchUrl(packageName: String): String? = apkCubeSearchUrl(packageName)

    override suspend fun findCandidates(
        request: HelperRequest,
        option: CandidateOption
    ): List<DownloadCandidate> = when (option) {
        CandidateOption.LATEST -> latestCandidates(request)
        CandidateOption.REQUESTED -> requestedCandidates(request)
        CandidateOption.MANUAL -> emptyList()
    }

    private fun latestCandidates(request: HelperRequest): List<DownloadCandidate> {
        val pageUrl = searchUrl(request.packageName) ?: return emptyList()
        val html = try {
            ctx.text(pageUrl)
        } catch (e: HttpRateLimitedException) {
            throw e
        } catch (e: Exception) {
            throw SourceAppNotFoundException(request.packageName)
        }
        val flight = html.unescapeFlightPayload()
        val downloadHref = flight.apkCubeDownloadHref(request.packageName) ?: return emptyList()
        val downloadUrl = "https://apkcube.com$downloadHref"
            .normalizedHttpUrlOrNull()
            ?: return emptyList()
        // The downloader page used to carry the version in its flight payload. It now
        // often names only the app, while the download page title still has the exact
        // version, so prefer that and keep the payload as a fallback.
        val versionName = runCatching { ctx.text(downloadUrl) }
            .getOrNull()
            ?.apkCubeVersionFromTitle()
            ?: flight.apkCubePageVersion()
            ?: return emptyList()
        return listOf(
            DownloadCandidate(
                source = source,
                name = request.appName,
                packageName = request.packageName,
                versionName = versionName,
                versionCode = null,
                url = downloadUrl,
                fileKind = "web",
                option = CandidateOption.LATEST,
                directDownload = false,
                versionStatus = request.versionStatus(versionName, null),
                formatMatches = true,
                note = captchaNote,
                captchaUrl = downloadUrl
            )
        )
    }

    private fun requestedCandidates(request: HelperRequest): List<DownloadCandidate> {
        val requestedName = request.requestedVersionName ?: return emptyList()
        // The old-versions page lives under the app's slug path, which only the
        // app page knows  fetch it first to learn the slug.
        val pageUrl = searchUrl(request.packageName) ?: return emptyList()
        val html = try {
            ctx.text(pageUrl)
        } catch (e: HttpRateLimitedException) {
            throw e
        } catch (e: Exception) {
            throw SourceAppNotFoundException(request.packageName)
        }
        val downloadHref = html.unescapeFlightPayload().apkCubeDownloadHref(request.packageName)
            ?: return emptyList()
        val slug = downloadHref
            .trim('/')
            .substringBefore('/')
            .takeIf(String::isNotBlank)
            ?: return emptyList()

        val versionsUrl = "https://apkcube.com/$slug/${request.packageName}/old-versions"
        val versionsHtml = try {
            ctx.text(versionsUrl)
        } catch (e: HttpRateLimitedException) {
            throw e
        } catch (e: Exception) {
            return emptyList()
        }
        val match = versionsHtml.unescapeFlightPayload().apkCubeVersionNames()
            .firstOrNull { it.versionNameEquals(requestedName) }
            ?: return emptyList()

        val encodedVersion = URLEncoder.encode(match, StandardCharsets.UTF_8.name())
        val downloadUrl = "https://apkcube.com/$slug/${request.packageName}/download?version=$encodedVersion"
            .normalizedHttpUrlOrNull()
            ?: return emptyList()
        return listOf(
            DownloadCandidate(
                source = source,
                name = request.appName,
                packageName = request.packageName,
                versionName = match,
                versionCode = null,
                url = downloadUrl,
                fileKind = "web",
                option = CandidateOption.REQUESTED,
                directDownload = false,
                versionStatus = VersionStatus.REQUESTED,
                formatMatches = true,
                note = captchaNote,
                captchaUrl = downloadUrl
            )
        )
    }
}

private const val EVOZI_CAPTCHA_NOTE =
    "APKCube (via Evozi) gates downloads behind a Cloudflare captcha. Solve it in the in-app browser or open the page manually."

private const val APKCUBE_CAPTCHA_NOTE =
    "APKCube gates downloads behind a Cloudflare captcha. Solve it in the in-app browser or open the page manually."

private fun apkCubeSearchUrl(packageName: String): String =
    "https://apkcube.com/apk-downloader?url=" +
        URLEncoder.encode(packageName, StandardCharsets.UTF_8.name())

/**
 * The Next.js flight payload JSON-escapes its content (`\"` for quotes), so
 * the page HTML contains literal backslash-quote pairs inside the script
 * payload. Collapse `\"` back to `"` so the fields can be matched as plain
 * JSON. The backslash is built from its char code to keep the source free of
 * fragile escape sequences.
 */
private fun String.unescapeFlightPayload(): String {
    val backslash = 92.toChar()
    if (indexOf(backslash) < 0) return this
    val sb = StringBuilder(length)
    var i = 0
    while (i < length) {
        val c = this[i]
        if (c == backslash && i + 1 < length && this[i + 1] == '"') {
            sb.append('"')
            i += 2
        } else {
            sb.append(c)
            i++
        }
    }
    return sb.toString()
}

/**
 * The app's download-page href inside the flight payload, e.g.
 * `href":"/battery-guru-battery-health/com.paget96.batteryguru/download"`.
 */
private fun String.apkCubeDownloadHref(packageName: String): String? =
    Regex("""href":"(/[a-z0-9-]+/${Regex.escape(packageName)}/download[^"]*)"""")
        .find(this)
        ?.groupValues
        ?.getOrNull(1)

/** The version in a title such as `Download WhatsApp 2.26.44.90 APK for Android`. */
private fun String.apkCubeVersionFromTitle(): String? =
    Regex(
        """<title>\s*Download\s+.+?\s+(\d[^<]*?)\s+APK\s+for\s+Android""",
        RegexOption.IGNORE_CASE
    )
        .find(this)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        ?.takeIf(String::isNotBlank)

/**
 * The version shown next to the app name on older APKCube downloader pages, e.g.
 * `children":["2.5.0.6"," · ","","Paget96"]`. The page has other
 * `children` arrays earlier (nav labels like "apk-downloader"), so prefer a
 * token that looks like a version (starts with a digit) before falling back to
 * the first non-blank match.
 */
private fun String.apkCubePageVersion(): String? {
    val versionLike = Regex("""children":\["(\d[^"]{1,40})"""")
        .find(this)
        ?.groupValues
        ?.getOrNull(1)
    return versionLike ?: Regex("""children":\["([^"]{1,40})"""")
        .find(this)
        ?.groupValues
        ?.getOrNull(1)
        ?.takeIf(String::isNotBlank)
}

/** Version names listed on the old-versions page, from `versionName":"X"` fields. */
private fun String.apkCubeVersionNames(): List<String> =
    Regex("""versionName":"([^"]+)"""")
        .findAll(this)
        .mapNotNull { it.groupValues.getOrNull(1)?.takeIf(String::isNotBlank) }
        .toList()
