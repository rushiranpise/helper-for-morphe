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
        val searchPageUrl = searchUrl(request.packageName) ?: return emptyList()
        val html = try {
            ctx.text(searchPageUrl)
        } catch (e: HttpRateLimitedException) {
            throw e
        } catch (e: Exception) {
            throw SourceAppNotFoundException(request.packageName)
        }
        val flight = html.unescapeFlightPayload()
        val downloadHref = flight.apkCubeDownloadHref(request.packageName) ?: return emptyList()
        val downloadPageUrl = "https://apkcube.com$downloadHref"
            .normalizedHttpUrlOrNull()
            ?: return emptyList()
        // The downloader page used to carry the version in its flight payload. It now
        // often names only the app, while the download page title still has the exact
        // version, so prefer that and keep the payload as a fallback.
        val downloadHtml = runCatching { ctx.text(downloadPageUrl) }.getOrNull()
        val downloadPayload = downloadHtml?.unescapeFlightPayload()
        val versionName = downloadHtml
            ?.apkCubeVersionFromTitle()
            ?: flight.apkCubePageVersion()
            ?: return emptyList()
        val versionCode = downloadPayload?.apkCubeLatestVersionCode()
            ?: flight.apkCubeLatestVersionCode()
        val downloadUrl = apkCubeTargetedDownloadUrl(
            baseUrl = downloadPageUrl,
            versionCode = versionCode,
            arch = apkCubeArch(request)
        )
        return listOf(
            DownloadCandidate(
                source = source,
                name = request.appName,
                packageName = request.packageName,
                versionName = versionName,
                versionCode = versionCode,
                url = downloadUrl,
                fileKind = "web",
                option = CandidateOption.LATEST,
                directDownload = false,
                versionStatus = request.versionStatus(versionName, versionCode),
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
        val searchPageUrl = searchUrl(request.packageName) ?: return emptyList()
        val html = try {
            ctx.text(searchPageUrl)
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
        val matches = versionsHtml.unescapeFlightPayload().apkCubeVersionEntries()
            .filter { it.versionName.versionNameEquals(requestedName) }
        if (matches.isEmpty()) return emptyList()
        val match = matches.firstOrNull { entry ->
            entry.versionCode != null && entry.versionCode in request.requestedVersionCodes
        } ?: matches.firstOrNull { it.arch == apkCubeArch(request) }
            ?: matches.first()

        val requestedCode = request.versionCode ?: request.versionCodes.singleOrNull()
        val versionCode = requestedCode ?: match.versionCode
        val encodedVersion = URLEncoder.encode(match.versionName, StandardCharsets.UTF_8.name())
        val downloadPageUrl = "https://apkcube.com/$slug/${request.packageName}/download?version=$encodedVersion"
            .normalizedHttpUrlOrNull()
            ?: return emptyList()
        val downloadUrl = apkCubeTargetedDownloadUrl(
            baseUrl = downloadPageUrl,
            versionCode = versionCode,
            arch = apkCubeArch(request)
        )
        return listOf(
            DownloadCandidate(
                source = source,
                name = request.appName,
                packageName = request.packageName,
                versionName = match.versionName,
                versionCode = versionCode,
                url = downloadUrl,
                fileKind = "web",
                option = CandidateOption.REQUESTED,
                directDownload = false,
                versionStatus = request.versionStatus(match.versionName, versionCode),
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

/** One build row from the old-versions page flight payload. */
private data class ApkCubeVersionEntry(
    val versionName: String,
    val versionCode: Long?,
    val arch: String?
)

/**
 * Build rows listed on the old-versions page. The payload repeats a version for
 * each architecture and format, so keep the build code and ABI instead of just
 * the version name; those are what the download page's `vc` and `arch` query
 * parameters target.
 */
private fun String.apkCubeVersionEntries(): List<ApkCubeVersionEntry> =
    Regex(
        """"versionName":"([^"]+)"[^{}]*?"versionCode":(\d+)[^{}]*?(?:"arch":"([^"]+)")?""""
    )
        .findAll(this)
        .mapNotNull { match ->
            val versionName = match.groupValues.getOrNull(1)
                ?.takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            ApkCubeVersionEntry(
                versionName = versionName,
                versionCode = match.groupValues.getOrNull(2)?.toLongOrNull(),
                arch = match.groupValues.getOrNull(3)?.takeIf(String::isNotBlank)
            )
        }
        .toList()

/** The build code the page carries for its current release. */
private fun String.apkCubeLatestVersionCode(): Long? =
    Regex("""(?:playVersionCode|versionCode)\\?":(\d+)""")
        .find(this)
        ?.groupValues
        ?.getOrNull(1)
        ?.toLongOrNull()

/** APKCube accepts these ABI labels for its `arch` query parameter. */
private val APK_CUBE_ARCHS = listOf("arm64-v8a", "armeabi-v7a", "universal")

private fun apkCubeArch(request: HelperRequest): String =
    APK_CUBE_ARCHS.firstOrNull { it in request.availableAbis } ?: "universal"

private fun apkCubeTargetedDownloadUrl(
    baseUrl: String,
    versionCode: Long?,
    arch: String
): String {
    val separator = if ('?' in baseUrl) '&' else '?'
    val params = buildList {
        versionCode?.takeIf { it > 0L }?.let {
            // APKCube ignores `vc` unless it is also told to target that build.
            add("target=version")
            add("vc=$it")
        }
        add("arch=${URLEncoder.encode(arch, StandardCharsets.UTF_8.name())}")
        add("langs=standard")
        add("dpi=all")
    }
    return "$baseUrl$separator${params.joinToString("&")}"
}
