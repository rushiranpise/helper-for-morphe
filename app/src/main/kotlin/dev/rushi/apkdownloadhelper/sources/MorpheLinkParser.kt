package dev.rushi.apkdownloadhelper.sources

import dev.rushi.apkdownloadhelper.DOWNLOAD_FILE_KIND_ORDER
import dev.rushi.apkdownloadhelper.DOWNLOAD_FILE_KIND_SET
import dev.rushi.apkdownloadhelper.TAG
import dev.rushi.apkdownloadhelper.download.fileKindFromUrlOrNull
import dev.rushi.apkdownloadhelper.download.normalizedHttpUrlOrNull
import dev.rushi.apkdownloadhelper.download.sanitizeFileName
import dev.rushi.apkdownloadhelper.ui.CandidateDownloadFile
import dev.rushi.apkdownloadhelper.ui.CandidateOption
import dev.rushi.apkdownloadhelper.ui.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.DownloadSource
import dev.rushi.apkdownloadhelper.ui.HelperRequest
import dev.rushi.apkdownloadhelper.ui.isApkMirrorFileUrl
import dev.rushi.apkdownloadhelper.ui.isApkPureFileUrl
import dev.rushi.apkdownloadhelper.ui.isCapturableDownload
import dev.rushi.apkdownloadhelper.ui.isUptodownFileUrl
import dev.rushi.apkdownloadhelper.ui.looksLikeApkDownload
import java.net.URI
import java.util.Locale

/**
 * A first-class source for the page Morphe's resolver names.
 *
 * The resolver is the only component that knows where Morphe itself would send a
 * request, so this source keeps its answer instead of discarding it after one source
 * fails. Known hosts hand the page to their normal parser; direct file endpoints become
 * a one-file candidate; everything else opens in the in-app browser with the exact link
 * the resolver returned.
 */
internal class MorpheLinkParser(
    private val ctx: SourceParserContext,
    delegates: List<ApkSourceParser> = emptyList(),
    private val resolveLink: (HelperRequest) -> String? = { request ->
        ctx.resolveViaMorphe?.invoke(request)
    }
) : ApkSourceParser {
    override val source: DownloadSource = DownloadSource.MORPHE_LINK

    private val delegatesBySource = delegates.associateBy(ApkSourceParser::source)

    override suspend fun findCandidates(
        request: HelperRequest,
        option: CandidateOption
    ): List<DownloadCandidate> {
        if (option == CandidateOption.MANUAL) return emptyList()
        val resolved = resolveLink(request)
            ?.trim()
            ?.normalizedHttpUrlOrNull()
        val fallback = request.fallbackWebUrl
            .trim()
            .normalizedHttpUrlOrNull()
        val targets = buildList {
            resolved?.let { add(it to false) }
            fallback?.takeIf { it != resolved }?.let { add(it to true) }
        }
        if (targets.isEmpty()) return emptyList()

        // Try the resolver answer first, then the request's fallback page. A
        // fallback page can be a direct file, a GitHub release or a source page,
        // so it gets the same orchestration instead of always opening a browser.
        for ((url, _) in targets) {
            if (url.isCaptchaGatedSourcePage()) {
                android.util.Log.i(
                    TAG,
                    "Morphe resolver named a captcha-gated page; only direct files are used: $url"
                )
                continue
            }
            val candidates = resolvedCandidates(request, url, option)
            if (candidates.isNotEmpty()) return candidates
        }

        val (browserUrl, fromFallback) = targets
            .firstOrNull { (url, _) -> !url.isCaptchaGatedSourcePage() }
            ?: return emptyList()
        val browser = browserCandidate(
            request = request,
            url = browserUrl,
            option = option,
            resolvedSource = morpheLinkSourceOf(browserUrl)
        )
        return listOf(
            if (fromFallback) {
                browser.copy(note = "Morphe did not return a link. Continue from the request's fallback page.")
            } else {
                browser
            }
        )
    }

    /** Direct files, GitHub releases and known source pages, in that order. */
    private suspend fun resolvedCandidates(
        request: HelperRequest,
        url: String,
        option: CandidateOption
    ): List<DownloadCandidate> {
        directFileCandidate(request, url, option)?.let { return listOf(it) }

        val github = githubReleaseCandidates(ctx, request, url, option)
        if (github.isNotEmpty()) return github

        val resolvedSource = morpheLinkSourceOf(url) ?: return emptyList()
        if (!ctx.isSourceEnabled(resolvedSource)) return emptyList()
        // Uptodown and APKCube only get the direct-file path above. Their pages
        // normally need a Turnstile challenge, so handing the request to their
        // parser here would stop the whole resolution on a captcha before the
        // API-backed sources have run.
        if (resolvedSource == DownloadSource.UPTODOWN ||
            resolvedSource == DownloadSource.APK_CUBE
        ) {
            return emptyList()
        }
        return runCatching {
            resolvedSource
                .let(delegatesBySource::get)
                ?.findCandidatesFromResolvedUrl(request, url, option)
                .orEmpty()
        }
            .onFailure { android.util.Log.w(TAG, "Morphe Link handoff failed: $url", it) }
            .getOrDefault(emptyList())
            .map { it.asMorpheLink() }
    }

    /**
     * No search page exists for this source, but a request can still carry the page
     * Morphe would have opened. Keeping that link makes the source useful even when the
     * resolver itself is off or has no answer.
     */
    override fun requestedFallbackCandidate(request: HelperRequest): DownloadCandidate? =
        fallbackCandidate(request, CandidateOption.REQUESTED)

    override fun latestFallbackCandidate(request: HelperRequest): DownloadCandidate? =
        fallbackCandidate(request, CandidateOption.LATEST)

    private fun fallbackCandidate(
        request: HelperRequest,
        option: CandidateOption
    ): DownloadCandidate? {
        val fallback = request.fallbackWebUrl.trim().normalizedHttpUrlOrNull() ?: return null
        return browserCandidate(request, fallback, option, resolvedSource = null).copy(
            note = "Morphe did not return a link. Open the request's fallback page.",
            syntheticFallback = true
        )
    }

    private fun directFileCandidate(
        request: HelperRequest,
        url: String,
        option: CandidateOption
    ): DownloadCandidate? {
        val fileKind = directFileKind(url, request) ?: return null
        val versionName = request.requestedVersionNames.firstOrNull()
            .takeIf { option == CandidateOption.REQUESTED }
        val versionCode = (request.versionCode ?: request.versionCodes.singleOrNull())
            .takeIf { option == CandidateOption.REQUESTED }
        val fileName = directFileName(request, url, versionName, fileKind)

        return DownloadCandidate(
            source = DownloadSource.MORPHE_LINK,
            name = request.appName,
            packageName = request.packageName,
            versionName = versionName,
            versionCode = versionCode,
            url = url,
            fileKind = fileKind,
            option = option,
            directDownload = true,
            versionStatus = request.versionStatus(versionName, versionCode),
            formatMatches = request.acceptsFormat(fileKind),
            note = "Direct file resolved by Morphe.",
            files = listOf(CandidateDownloadFile(url = url, fileName = fileName))
        )
    }

    private fun browserCandidate(
        request: HelperRequest,
        url: String,
        option: CandidateOption,
        resolvedSource: DownloadSource?
    ): DownloadCandidate {
        val versionName = request.requestedVersionNames.firstOrNull()
            .takeIf { option == CandidateOption.REQUESTED }
        val versionCode = (request.versionCode ?: request.versionCodes.singleOrNull())
            .takeIf { option == CandidateOption.REQUESTED }
        val note = if (resolvedSource != null) {
            "Morphe resolved this request to ${resolvedSource.label}. Continue in the in-app browser."
        } else {
            "Morphe resolved this link. Continue in the in-app browser."
        }

        return DownloadCandidate(
            source = DownloadSource.MORPHE_LINK,
            name = request.appName,
            packageName = request.packageName,
            versionName = versionName,
            versionCode = versionCode,
            url = url,
            fileKind = "web",
            option = option,
            directDownload = false,
            versionStatus = request.versionStatus(versionName, versionCode),
            formatMatches = true,
            note = note,
            captchaUrl = url,
            viaSource = resolvedSource
        )
    }

    private fun directFileKind(url: String, request: HelperRequest): String? = when {
        url.looksLikeApkDownload() -> fileKindFromUrlOrNull(url) ?: "apk"
        url.isUptodownFileUrl() -> preferredFileKind(request)
        url.isApkPureFileUrl() -> fileKindFromUrlOrNull(url) ?: "apk"
        url.isApkMirrorFileUrl() -> preferredFileKind(request)
        else -> null
    }

    private fun preferredFileKind(request: HelperRequest): String =
        DOWNLOAD_FILE_KIND_ORDER.firstOrNull { it in request.requestedFileKinds } ?: "apk"

    private fun directFileName(
        request: HelperRequest,
        url: String,
        versionName: String?,
        fileKind: String
    ): String {
        val urlName = runCatching {
            URI(url).path.orEmpty().substringAfterLast('/')
        }
            .getOrNull()
            ?.takeIf { name ->
                name.substringAfterLast('.', "")
                    .lowercase(Locale.US) in DOWNLOAD_FILE_KIND_SET
            }
        return (urlName ?: "${request.packageName}-${versionName ?: "latest"}-morphe.$fileKind")
            .sanitizeFileName()
    }
}

/** True for source pages that normally hide their file behind a captcha. */
private fun String.isCaptchaGatedSourcePage(): Boolean =
    !isCapturableDownload() && when (morpheLinkSourceOf(this)) {
        DownloadSource.UPTODOWN,
        DownloadSource.APK_CUBE -> true
        else -> false
    }

/** The parser that should continue from a resolved page, based on its host. */
internal fun morpheLinkSourceOf(url: String): DownloadSource? {
    val host = urlHostOf(url) ?: return null
    return when {
        host == "apkmirror.com" || host.endsWith(".apkmirror.com") -> DownloadSource.APK_MIRROR
        host == "uptodown.com" ||
            host.endsWith(".uptodown.com") ||
            host.endsWith(".uptodown.net") -> DownloadSource.UPTODOWN
        host == "apkpure.com" || host.endsWith(".apkpure.com") -> DownloadSource.APK_PURE
        host == "aptoide.com" || host.endsWith(".aptoide.com") -> DownloadSource.APTOIDE
        host == "apkcube.com" || host.endsWith(".apkcube.com") -> DownloadSource.APK_CUBE
        // Evozi now redirects to APKCube, so it is no longer a separate source.
        else -> null
    }
}

/** Keep the source's own identity while retaining where its page came from. */
private fun DownloadCandidate.asMorpheLink(): DownloadCandidate {
    val underlying = source
    val provenance = "Resolved via ${underlying.label}."
    return copy(
        source = DownloadSource.MORPHE_LINK,
        note = listOfNotNull(provenance, note).joinToString(" "),
        viaSource = underlying
    )
}

