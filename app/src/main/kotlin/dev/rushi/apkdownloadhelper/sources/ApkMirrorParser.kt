package dev.rushi.apkdownloadhelper.sources

import android.net.Uri
import android.util.Log
import dev.rushi.apkdownloadhelper.DOWNLOAD_FILE_KIND_ORDER
import dev.rushi.apkdownloadhelper.TAG
import dev.rushi.apkdownloadhelper.download.apkMirrorVersionSlug
import dev.rushi.apkdownloadhelper.download.compareVersionNames
import dev.rushi.apkdownloadhelper.download.fileKindFromUrl
import dev.rushi.apkdownloadhelper.download.fileKindFromUrlOrNull
import dev.rushi.apkdownloadhelper.download.hasVariantBuildMarker
import dev.rushi.apkdownloadhelper.download.normalizedHttpUrlOrNull
import dev.rushi.apkdownloadhelper.download.parseInfoTableValue
import dev.rushi.apkdownloadhelper.download.sanitizeFileName
import dev.rushi.apkdownloadhelper.download.slugForUrl
import dev.rushi.apkdownloadhelper.download.sourceFailureMessage
import dev.rushi.apkdownloadhelper.download.variantFileSuffix
import dev.rushi.apkdownloadhelper.download.versionNameEquals
import dev.rushi.apkdownloadhelper.ui.model.ApkMirrorLatestInfo
import dev.rushi.apkdownloadhelper.ui.model.ApkMirrorVariant
import dev.rushi.apkdownloadhelper.ui.model.CandidateDownloadFile
import dev.rushi.apkdownloadhelper.ui.model.CandidateOption
import dev.rushi.apkdownloadhelper.ui.model.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.model.HelperRequest
import dev.rushi.apkdownloadhelper.ui.model.VersionStatus
import dev.rushi.apkdownloadhelper.ui.model.identityKey
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder
import java.util.Locale

/** App-listing slugs that carry a different version stream than the canonical app. */
private val APKMIRROR_EDITION_SLUG_REGEX = Regex(
    """(amazon|fire-tablet|fire-tv|androidtv|wear|go-edition|lite|beta|alpha|enterprise|kids|headunit|auto)""",
    RegexOption.IGNORE_CASE
)

private const val APKMIRROR_BROWSER_NOTE =
    "Complete any browser check, then choose the file on the APKMirror page."

private const val APKMIRROR_SEARCH_NOTE =
    "Open APKMirror's search results, then choose the app and version."

internal class ApkMirrorParser(private val ctx: SourceParserContext) : ApkSourceParser {
    override val source: DownloadSource = DownloadSource.APK_MIRROR

    override suspend fun findCandidates(
        request: HelperRequest,
        option: CandidateOption
    ): List<DownloadCandidate> = when (option) {
        CandidateOption.REQUESTED -> findApkMirrorRequested(request)
        CandidateOption.LATEST -> findApkMirrorLatest(request)
        CandidateOption.MANUAL -> emptyList()
    }

    override fun searchUrl(packageName: String): String? = apkMirrorPackageSearchUrl(packageName)

    override fun requestedFallbackCandidate(request: HelperRequest): DownloadCandidate =
        apkMirrorRequested(request)

    override fun latestFallbackCandidate(request: HelperRequest): DownloadCandidate =
        apkMirrorLatest(request)

    private val historyCache = HashMap<String, List<DownloadCandidate>>()

    override suspend fun resolveHistory(request: HelperRequest): List<DownloadCandidate> {
        historyCache[request.packageName]?.let { return it }
        val result = resolveHistoryUncached(request)
        historyCache[request.packageName] = result
        return result
    }

    private suspend fun resolveHistoryUncached(request: HelperRequest): List<DownloadCandidate> {
        val searchDoc = fetchDocument(apkMirrorPackageSearchUrl(request.packageName))
        val appPageUrl = resolveApkMirrorAppPage(searchDoc, request) ?: return emptyList()
        val category = appPageUrl.trimEnd('/').substringAfterLast('/').takeIf(String::isNotBlank)
            ?: return emptyList()

        val releaseUrls = mutableListOf<String>()
        for (page in 1..3) {
            val pageUrl = if (page == 1) {
                apkMirrorUploadsUrl(appPageUrl)
            } else {
                "https://www.apkmirror.com/uploads/page/$page/?appcategory=$category"
            }
            val doc: Document = try {
                fetchDocument(pageUrl, referer = appPageUrl)
            } catch (e: HttpRateLimitedException) {
                break
            } catch (e: Exception) {
                continue
            }
            // Filter by the app page path: the uploads page also carries
            // trending/sidebar release links for other apps, which must not
            // leak into this app's version history.
            val links = apkMirrorReleaseLinks(doc, appPageUrl)
            if (links.isEmpty()) break
            releaseUrls += links
        }

        return releaseUrls
            .distinct()
            .mapNotNull { releaseUrl ->
                val versionName = apkMirrorVersionFromReleaseUrl(releaseUrl, appPageUrl) ?: return@mapNotNull null
                DownloadCandidate(
                    source = DownloadSource.APK_MIRROR,
                    name = request.appName,
                    packageName = request.packageName,
                    versionName = versionName,
                    versionCode = null,
                    url = releaseUrl,
                    fileKind = "web",
                    option = CandidateOption.LATEST,
                    directDownload = false,
                    versionStatus = request.versionStatus(versionName, null),
                    formatMatches = true,
                    note = null
                )
            }
            .sortedWith { left, right -> compareVersionNames(right.versionName, left.versionName) }
    }

    override suspend fun resolveHistoryCandidate(
        request: HelperRequest,
        candidate: DownloadCandidate
    ): DownloadCandidate? {
        // History rows carry the release-page URL. Resolve it the same way the
        // Recommended/Latest tabs do; return null when no direct download can be
        // produced so the UI can offer an "Open link" action instead.
        return runCatching {
            apkMirrorCandidatesFromReleaseUrl(
                request = request,
                releaseUrl = candidate.url,
                versionName = candidate.versionName,
                option = candidate.option
            )
        }
            .getOrNull()
            ?.firstOrNull { it.directDownload }
    }

    private fun apkMirrorRequested(request: HelperRequest): DownloadCandidate {
        val browserUrl = apkMirrorBrowserUrl(request)
        val searchPage = apkMirrorIsSearchPage(browserUrl)
        return DownloadCandidate(
            source = DownloadSource.APK_MIRROR,
            name = request.appName,
            packageName = request.packageName,
            versionName = request.versionName,
            versionCode = request.versionCode ?: request.versionCodes.singleOrNull(),
            url = browserUrl,
            fileKind = request.requestedFormatLabel,
            option = CandidateOption.REQUESTED,
            directDownload = false,
            versionStatus = VersionStatus.REQUESTED,
            formatMatches = true,
            note = if (searchPage) APKMIRROR_SEARCH_NOTE else APKMIRROR_BROWSER_NOTE,
            captchaUrl = browserUrl.takeUnless { searchPage }
        )
    }

    private fun apkMirrorLatest(request: HelperRequest): DownloadCandidate =
        apkMirrorLatestWebCandidate(request, runCatching {
            resolveApkMirrorLatestInfo(request, apkMirrorPackageSearchUrl(request.packageName))
        }.getOrNull())

    private suspend fun findApkMirrorLatest(request: HelperRequest): List<DownloadCandidate> {
        // The resolver often hands back the exact variant page, which is two fetches
        // instead of the search -> app -> uploads -> release walk below.
        apkMirrorFastPath(request, CandidateOption.LATEST)?.let { return it }
        apkMirrorApiLatestCandidates(request)?.let { return it }

        val latestInfo = resolveApkMirrorLatestInfo(
            request = request,
            searchUrl = apkMirrorPackageSearchUrl(request.packageName)
        )
        val latestReleaseUrl = latestInfo
            ?.openUrl
            ?.takeIf(::apkMirrorLooksLikeReleaseUrl)
        val directCandidates = latestReleaseUrl?.let { releaseUrl ->
            apkMirrorCandidatesFromReleaseUrl(
                request = request,
                releaseUrl = releaseUrl,
                versionName = latestInfo.versionName ?: apkMirrorVersionFromReleaseUrl(releaseUrl),
                option = CandidateOption.LATEST
            )
        }

        return directCandidates.orEmpty()
            .ifEmpty { listOf(apkMirrorLatestWebCandidate(request, latestInfo)) }
    }

    /**
     * APKMirror's own app-exists API, used before the search -> app -> uploads -> release walk.
     *
     * The endpoint names the latest stable release and its variant pages in one request, so
     * this skips the search, app, uploads and release pages. It is private and can
     * disappear, and it has no per-version
     * history, so it is only consulted for "latest" and any failure falls through to scraping.
     * Secondary builds are skipped here for the same reason [apkMirrorLatestReleaseUrl] ignores
     * them: a "-SECONDARY" release is not the build the latest tab is meant to offer.
     */
    private suspend fun apkMirrorApiLatestCandidates(
        request: HelperRequest
    ): List<DownloadCandidate>? {
        val api = ctx.apkMirrorApi ?: return null
        val data = runCatching {
            api.appExists(ApkMirrorAppExistsRequest(pnames = listOf(request.packageName)))
                .data
                .firstOrNull { it.pname == request.packageName && it.exists }
        }
            .onFailure { Log.w(TAG, "APKMirror API latest lookup failed", it) }
            .getOrNull()
            ?: return null

        val version = data.release.version.trim()
        if (version.isEmpty()) return null
        if (listOf("alpha", "beta", "secondary").any { version.contains(it, ignoreCase = true) }) {
            return null
        }
        val releaseUrl = apkMirrorAbsoluteUrl(data.release.link)
            ?.takeIf(::apkMirrorLooksLikeReleaseUrl)
            ?: return null
        val variants = data.apks
            .flatMap { it.toApkMirrorVariants() }
            .mapIndexed { index, variant -> variant.copy(number = index + 1) }
        if (variants.isEmpty()) return null

        val selected = apkMirrorSelectableVariants(request, variants, CandidateOption.LATEST)
        if (selected.isEmpty()) return null
        val multiVariant = selected.size > 1
        return selected.mapNotNull { variant ->
            runCatching {
                apkMirrorDirectCandidateFromReleaseUrl(
                    request = request,
                    releaseUrl = releaseUrl,
                    versionName = version,
                    option = CandidateOption.LATEST,
                    variant = variant,
                    variantNumber = variant.number.takeIf { multiVariant }
                )
            }
                .onFailure { Log.w(TAG, "APKMirror API variant failed: ${variant.url}", it) }
                .getOrNull()
        }
            .distinctBy(DownloadCandidate::identityKey)
            .takeIf(List<DownloadCandidate>::isNotEmpty)
    }

    private fun ApkMirrorAppExistsApk.toApkMirrorVariants(): List<ApkMirrorVariant> {
        val url = apkMirrorAbsoluteUrl(link) ?: return emptyList()
        val kinds = downloadTypes.mapNotNull { type ->
            when (type.trim().lowercase(Locale.US)) {
                "apk" -> "apk"
                "bundle", "apkm" -> "apkm"
                "apks" -> "apks"
                else -> null
            }
        }.distinct().ifEmpty { listOf("apk") }
        val arch = apkMirrorApiArchLabel(arches)
        val dpi = apkMirrorApiDpiLabel(dpis)
        val code = versionCode.toLongOrNull()?.takeIf { it > 0L }
        return kinds.map { kind ->
            ApkMirrorVariant(
                url = url,
                type = if (kind == "apk") "APK" else "BUNDLE",
                fileKind = kind,
                arch = arch,
                dpi = dpi,
                isBundle = kind != "apk",
                versionCode = code
            )
        }
    }

    private fun apkMirrorApiArchLabel(arches: List<String>): String? {
        val labels = arches.map { it.trim() }.filter(String::isNotBlank).distinct()
        if (labels.isEmpty()) return null
        val allAbis = setOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        if (labels.map { it.lowercase(Locale.US) }.containsAll(allAbis)) return "Universal"
        return labels.joinToString(", ")
    }

    private fun apkMirrorApiDpiLabel(dpis: List<String>?): String? {
        val labels = dpis.orEmpty()
            .map { it.trim().lowercase(Locale.US) }
            .filter(String::isNotBlank)
        if (labels.isEmpty()) return null
        if (labels.any { it == "nodpi" || it == "anydpi" }) return "nodpi"
        val numbers = labels.mapNotNull { label ->
            Regex("""\d+""").find(label)?.value?.toIntOrNull()
        }.sorted()
        if (numbers.isEmpty()) return labels.joinToString("-")
        return if (numbers.size == 1) {
            "${numbers.first()}dpi"
        } else {
            "${numbers.first()}-${numbers.last()}dpi"
        }
    }

    /**
     * Morphe's resolver shortcut: the page its own download dialog would open, used directly.
     *
     * Returns null whenever the resolver is off, answers with something this helper cannot
     * continue from (an app page, a Google search, another site), or fails - the caller then
     * scrapes as it always did, so this is only ever a shortcut and never a dependency.
     */
    private fun apkMirrorFastPath(
        request: HelperRequest,
        option: CandidateOption
    ): List<DownloadCandidate>? {
        val resolved = ctx.resolveViaMorphe?.invoke(request) ?: return null
        return apkMirrorCandidatesFromResolvedUrl(request, resolved, option)
    }

    override suspend fun findCandidatesFromResolvedUrl(
        request: HelperRequest,
        url: String,
        option: CandidateOption
    ): List<DownloadCandidate>? = apkMirrorCandidatesFromResolvedUrl(request, url, option)

    private fun apkMirrorCandidatesFromResolvedUrl(
        request: HelperRequest,
        resolved: String,
        option: CandidateOption
    ): List<DownloadCandidate>? {
        if (resolved.isBlank()) return null

        return when (apkMirrorPageOf(resolved)) {
            ApkMirrorPage.VARIANT -> {
                // Already the build the resolver picked for this device's ABI.
                val releaseUrl = apkMirrorReleaseUrlOfVariantPage(resolved) ?: return null
                val version = request.requestedVersionNames.firstOrNull()
                    ?: apkMirrorVersionFromReleaseUrl(
                        releaseUrl,
                        appPageUrl = apkMirrorAppPageOfReleaseUrl(releaseUrl)
                    )
                val direct = runCatching {
                    apkMirrorDirectCandidateFromReleaseUrl(
                        request = request,
                        // A variant page is treated as the page to read: the variant
                        // page carries its own download button.
                        releaseUrl = resolved,
                        versionName = version,
                        option = option,
                        variant = null
                    )
                }
                    .onFailure { Log.w(TAG, "Morphe resolver variant page failed: $resolved", it) }
                    .getOrNull()
                // APKMirror now answers the helper's HTTP client with a Cloudflare
                // challenge. Keep the exact page the resolver named and open it in
                // the in-app browser instead of discarding the shortcut.
                listOf(direct ?: apkMirrorResolvedWebCandidate(request, resolved, version, option))
            }

            ApkMirrorPage.RELEASE -> {
                val version = request.requestedVersionNames.firstOrNull()
                    ?: apkMirrorVersionFromReleaseUrl(
                        resolved,
                        appPageUrl = apkMirrorAppPageOfReleaseUrl(resolved)
                    )
                val candidates = runCatching {
                    apkMirrorCandidatesFromReleaseUrl(
                        request = request,
                        releaseUrl = resolved,
                        versionName = version,
                        option = option
                    )
                }
                    .onFailure { Log.w(TAG, "Morphe resolver release page failed: $resolved", it) }
                    .getOrNull()
                    .orEmpty()
                candidates.ifEmpty {
                    listOf(apkMirrorResolvedWebCandidate(request, resolved, version, option))
                }
            }

            null -> null
        }
    }

    /** The exact APKMirror page the resolver named, offered to the in-app browser. */
    private fun apkMirrorResolvedWebCandidate(
        request: HelperRequest,
        pageUrl: String,
        versionName: String?,
        option: CandidateOption
    ): DownloadCandidate = DownloadCandidate(
        source = DownloadSource.APK_MIRROR,
        name = request.appName,
        packageName = request.packageName,
        versionName = versionName,
        versionCode = request.versionCode ?: request.versionCodes.singleOrNull(),
        url = pageUrl,
        fileKind = "web",
        option = option,
        directDownload = false,
        versionStatus = request.versionStatus(versionName, null),
        formatMatches = true,
        note = APKMIRROR_BROWSER_NOTE,
        captchaUrl = pageUrl
    )

    private fun apkMirrorLatestWebCandidate(
        request: HelperRequest,
        latestInfo: ApkMirrorLatestInfo?
    ): DownloadCandidate {
        val browserUrl = latestInfo?.openUrl ?: apkMirrorPackageSearchUrl(request.packageName)
        val searchPage = apkMirrorIsSearchPage(browserUrl)
        return DownloadCandidate(
            source = DownloadSource.APK_MIRROR,
            name = request.appName,
            packageName = request.packageName,
            versionName = latestInfo?.versionName,
            versionCode = null,
            url = browserUrl,
            fileKind = "web",
            option = CandidateOption.LATEST,
            directDownload = false,
            versionStatus = VersionStatus.LATEST,
            formatMatches = true,
            note = if (searchPage) APKMIRROR_SEARCH_NOTE else APKMIRROR_BROWSER_NOTE,
            captchaUrl = browserUrl.takeUnless { searchPage }
        )
    }

    private fun findApkMirrorRequested(request: HelperRequest): List<DownloadCandidate> {
        if (!request.hasKnownVersionRequest) return emptyList()

        apkMirrorFastPath(request, CandidateOption.REQUESTED)?.let { return it }

        val searchDoc = fetchDocument(apkMirrorPackageSearchUrl(request.packageName))
        val appPageUrl = resolveApkMirrorAppPage(
            searchDoc = searchDoc,
            request = request
        ) ?: throw SourceAppNotFoundException(request.packageName)
        val appDoc = fetchDocument(appPageUrl)
        val requested = apkMirrorRequestedReleaseUrl(
            request = request,
            appPageUrl = appPageUrl,
            appDoc = appDoc
        )?.let { releaseUrl ->
            apkMirrorCandidatesFromReleaseUrl(
                request = request,
                releaseUrl = releaseUrl,
                versionName = request.requestedVersionNames.firstOrNull()
                    ?: apkMirrorVersionFromReleaseUrl(releaseUrl),
                option = CandidateOption.REQUESTED
            )
        }

        return requested.orEmpty()
    }

    private fun apkMirrorPackageSearchUrl(packageName: String): String =
        apkMirrorSearchUrl(packageName)

    private fun resolveApkMirrorLatestInfo(
        request: HelperRequest,
        searchUrl: String
    ): ApkMirrorLatestInfo? {
        val searchDoc = fetchDocument(searchUrl)
        val appPageUrl = resolveApkMirrorAppPage(
            searchDoc = searchDoc,
            request = request
        ) ?: throw SourceAppNotFoundException(request.packageName)
        val searchVersion = apkMirrorSearchVersionForApp(searchDoc, appPageUrl)
            ?: apkMirrorVersions(searchDoc.html()).firstOrNull()
        val appDoc = fetchDocument(appPageUrl)
        val uploadsDoc = runCatching { fetchDocument(apkMirrorUploadsUrl(appPageUrl), referer = appPageUrl) }
            .getOrNull()
        val latestReleaseUrl = apkMirrorLatestReleaseUrl(
            (
                uploadsDoc?.let { apkMirrorReleaseLinks(it, appPageUrl) }.orEmpty() +
                    apkMirrorReleaseLinks(appDoc, appPageUrl)
                ).distinct()
        )
        val latestVersion = latestReleaseUrl?.let { apkMirrorVersionFromReleaseUrl(it, appPageUrl) }
            ?: searchVersion
            ?: apkMirrorVersions(appDoc.html()).firstOrNull()

        return ApkMirrorLatestInfo(
            versionName = latestVersion,
            openUrl = latestReleaseUrl ?: appPageUrl
        )
    }

    private fun resolveApkMirrorAppPage(searchDoc: Document, request: HelperRequest): String? {
        if (searchDoc.isApkMirrorNoResults()) return null
        for (candidate in apkMirrorAppPageCandidates(searchDoc, request)) {
            val candidateDoc: Document = try {
                fetchDocument(candidate)
            } catch (e: HttpRateLimitedException) {
                throw e
            } catch (e: Exception) {
                continue
            }
            if (candidateDoc.apkMirrorMatchesPackage(request.packageName)) return candidate
        }

        return null
    }

    private fun apkMirrorAppPageCandidates(searchDoc: Document, request: HelperRequest): List<String> {
        val expectedSlugs = apkMirrorExpectedAppSlugs(request)
        return searchDoc.select("a[href]")
            .asSequence()
            .mapNotNull { apkMirrorAbsoluteUrl(it.attr("href")) }
            .filter { url ->
                runCatching {
                    java.net.URI(url).path.matches(Regex("""/apk/[^/]+/[^/]+/?"""))
                }.getOrDefault(false)
            }
            .distinct()
            .sortedBy { url -> apkMirrorAppSlugScore(url, expectedSlugs) }
            .take(20)
            .toList()
    }

    private fun apkMirrorExpectedAppSlugs(request: HelperRequest): Set<String> =
        buildSet {
            add(request.appName.slugForUrl())
            val packageParts = request.packageName.split(".")
            packageParts.takeLast(2)
                .joinToString("-")
                .slugForUrl()
                .takeIf(String::isNotBlank)
                ?.let(::add)
            packageParts.takeLast(3)
                .joinToString("-")
                .slugForUrl()
                .takeIf(String::isNotBlank)
                ?.let(::add)
        }

    private fun apkMirrorAppSlugScore(url: String, expectedSlugs: Set<String>): Int {
        val segments = runCatching {
            java.net.URI(url).path.trim('/').split('/')
        }.getOrDefault(emptyList())
        // Path layout: /apk/{developer}/{app-slug}/  the developer segment
        // alone is ambiguous when a publisher lists several editions (e.g.
        // TikTok vs its Amazon Appstore edition), so score the app segment too.
        val developerSlug = segments.getOrNull(1).orEmpty()
        val appSlug = segments.getOrNull(2).orEmpty()

        fun scoreSlug(slug: String): Int = when {
            slug in expectedSlugs -> 0
            expectedSlugs.any { slug.endsWith(it) } -> 1
            expectedSlugs.any { slug.contains(it) } -> 2
            else -> 3
        }

        // Score both the developer and the app segment (e.g. TikTok's canonical
        // listing vs its Amazon Appstore edition live under the same developer).
        var score = minOf(scoreSlug(developerSlug), scoreSlug(appSlug))

        // Prefer the canonical listing over special editions, which share the
        // package name but carry different version streams (amazon, lite, tv...).
        if (APKMIRROR_EDITION_SLUG_REGEX.containsMatchIn(appSlug)) score += 4
        return score
    }

    private fun apkMirrorSearchVersionForApp(searchDoc: Document, appPageUrl: String): String? {
        val appPath = runCatching { java.net.URI(appPageUrl).path.trimEnd('/') }.getOrNull()
            ?: return null
        val row = searchDoc.select("div.appRow")
            .firstOrNull { row ->
                row.select("a[href]")
                    .asSequence()
                    .map { it.absUrl("href") }
                    .any { url ->
                        runCatching { java.net.URI(url).path.trimEnd('/') == appPath }
                            .getOrDefault(false)
                    }
            }

        return row
            ?.select("img[alt]")
            ?.asSequence()
            ?.map { it.attr("alt") }
            ?.mapNotNull { alt -> Regex("""\b(\d+(?:\.\d+)+(?:[.\w-]*)?)\b""").findAll(alt).lastOrNull()?.value }
            ?.firstOrNull()
    }

    private fun apkMirrorVersions(html: String): List<String> =
        Regex(
            """Version:\s*</span>\s*<span[^>]*class=["'][^"']*infoSlide-value[^"']*["'][^>]*>\s*([^<]+?)\s*</span>""",
            RegexOption.IGNORE_CASE
        )
            .findAll(html)
            .mapNotNull { match -> match.groupValues.getOrNull(1)?.trim()?.takeIf(String::isNotBlank) }
            .filterNot { it.contains("alpha", ignoreCase = true) || it.contains("beta", ignoreCase = true) }
            .distinct()
            .toList()

    private fun apkMirrorReleaseLinks(doc: Document, appPageUrl: String? = null): List<String> {
        val appPath = appPageUrl
            ?.let { url -> runCatching { java.net.URI(url).path.trimEnd('/') }.getOrNull() }

        return doc.select("a[href]")
            .asSequence()
            .mapNotNull { link -> apkMirrorAbsoluteUrl(link.attr("href")) }
            .filter { url ->
                runCatching {
                    val path = java.net.URI(url).path
                    path.startsWith("/apk/") &&
                        path.trimEnd('/').endsWith("-release") &&
                        (appPath == null || path.startsWith("$appPath/"))
                }.getOrDefault(false)
            }
            .distinct()
            .toList()
    }

    internal fun apkMirrorLatestReleaseUrl(releaseLinks: List<String>): String? {
        // "-SECONDARY" is an alternate package of the same version Morphe
        // cannot patch, so it must never be offered as "latest".
        val patchable = releaseLinks.filterNot { it.hasVariantBuildMarker() }
        if (patchable.isEmpty()) return null
        return patchable
            .filterNot { it.contains("alpha", ignoreCase = true) || it.contains("beta", ignoreCase = true) }
            .ifEmpty { patchable }
            .maxWithOrNull { left, right ->
                compareVersionNames(
                    apkMirrorVersionFromReleaseUrl(left),
                    apkMirrorVersionFromReleaseUrl(right)
                )
            }
    }

    private fun apkMirrorRequestedReleaseUrl(
        request: HelperRequest,
        appPageUrl: String,
        appDoc: Document
    ): String? {
        val requestedVersions = request.requestedVersionNames
        if (requestedVersions.isEmpty()) return null

        request.sourceHintUrlsFor(DownloadSource.APK_MIRROR)
            .asSequence()
            .mapNotNull(::apkMirrorAbsoluteUrl)
            .filter(::apkMirrorLooksLikeReleaseUrl)
            .toList()
            .let { urls -> apkMirrorPreferredReleaseUrl(urls, requestedVersions) }
            ?.let { return it }

        apkMirrorPreferredReleaseUrl(apkMirrorReleaseLinks(appDoc, appPageUrl), requestedVersions)
            ?.let { return it }

        apkMirrorConstructedReleaseUrl(appPageUrl, appDoc, requestedVersions)
            ?.let { return it }

        val category = appPageUrl.trimEnd('/').substringAfterLast('/').takeIf(String::isNotBlank)
            ?: return null
        val uploadsUrl = "https://www.apkmirror.com/uploads/?appcategory=$category"
        for (page in 1..5) {
            val pageUrl = if (page == 1) {
                uploadsUrl
            } else {
                "https://www.apkmirror.com/uploads/page/$page/?appcategory=$category"
            }
            val doc: Document = try {
                fetchDocument(pageUrl, referer = appPageUrl)
            } catch (e: HttpRateLimitedException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "APKMirror uploads page resolve failed: $pageUrl", e)
                continue
            }

            apkMirrorPreferredReleaseUrl(apkMirrorReleaseLinks(doc), requestedVersions)
                ?.let { return it }
        }

        return null
    }

    /**
     * Among release URLs matching one of [requestedVersions], prefer the normal
     * build. APKMirror publishes alternate "-SECONDARY" builds of the same
     * version which Morphe cannot patch; they parse to the same version number,
     * so without this the first-match race hands one to the caller.
     */
    internal fun apkMirrorPreferredReleaseUrl(
        urls: List<String>,
        requestedVersions: List<String>
    ): String? {
        val matching = urls.filter { url ->
            requestedVersions.any { version -> apkMirrorReleaseUrlMatchesVersion(url, version) }
        }
        return matching.firstOrNull { !it.hasVariantBuildMarker() } ?: matching.firstOrNull()
    }

    /**
     * Builds a release URL from the app page's latest slug when the requested
     * version is no longer listed on the app or uploads pages.
     *
     * APKMirror release slugs are predictable: an app page whose latest release
     * is `example-app-2-0-0-release` can be probed for
     * `example-app-1-5-0-release`. The app slug is stripped first because it can
     * itself end in a digit (`file-manager-7-3-5-4-release`).
     */
    private fun apkMirrorConstructedReleaseUrl(
        appPageUrl: String,
        appDoc: Document,
        requestedVersions: List<String>
    ): String? {
        val releases = apkMirrorReleaseLinks(appDoc, appPageUrl)
        val base = releases.firstOrNull { !it.hasVariantBuildMarker() } ?: releases.firstOrNull()
            ?: return null
        val appPath = runCatching { java.net.URI(appPageUrl).path.trimEnd('/') }.getOrNull()
            ?.takeIf(String::isNotBlank)
            ?: return null
        val appSlug = appPath.substringAfterLast('/').takeIf(String::isNotBlank) ?: return null
        val baseSegment = runCatching {
            java.net.URI(base).path.trimEnd('/').substringAfterLast('/')
        }.getOrNull()
            ?.removeSuffix("-release")
            ?.takeIf(String::isNotBlank)
            ?: return null
        val versionOnly = Regex(
            """^\d+(?:-\d+)*(?:-(?:secondary|alpha|beta|rc)\d*)?$""",
            RegexOption.IGNORE_CASE
        )
        val prefix = when {
            baseSegment.startsWith("$appSlug-") -> "$appSlug-"
            // Some release URLs end in just the version (no app slug prefix).
            versionOnly.matches(baseSegment) -> "$appSlug-"
            else -> Regex("""^(.*?-)\d+(?:-\d+)*(?:-(?:alpha|beta|rc)\d*)?$""", RegexOption.IGNORE_CASE)
                .find(baseSegment)
                ?.groupValues
                ?.getOrNull(1)
        } ?: return null

        for (requested in requestedVersions) {
            val versionSlug = requested.apkMirrorVersionSlug()
            if (versionSlug.isBlank()) continue
            val candidate = "https://www.apkmirror.com$appPath/$prefix$versionSlug-release/"
            val exists = try {
                val doc = fetchDocument(candidate, referer = appPageUrl)
                !doc.isCloudflareChallenge()
            } catch (e: HttpRateLimitedException) {
                throw e
            } catch (e: Exception) {
                false
            }
            if (exists) {
                Log.i(TAG, "APKMirror constructed missing release URL: $candidate")
                return candidate
            }
        }
        return null
    }

    private fun apkMirrorUploadsUrl(appPageUrl: String): String {
        val category = runCatching {
            Uri.parse(appPageUrl).path
                ?.trim('/')
                ?.substringAfterLast('/')
                ?.takeIf(String::isNotBlank)
        }.getOrNull()
            ?: appPageUrl.substringBefore('#').substringBefore('?').trimEnd('/').substringAfterLast('/')
        return "https://www.apkmirror.com/uploads/?appcategory=$category"
    }

    private fun apkMirrorCandidatesFromReleaseUrl(
        request: HelperRequest,
        releaseUrl: String,
        versionName: String?,
        option: CandidateOption
    ): List<DownloadCandidate> {
        val directCandidates = runCatching {
            apkMirrorDirectCandidatesFromReleaseUrl(
                request = request,
                releaseUrl = releaseUrl,
                versionName = versionName,
                option = option
            )
        }
            .onFailure { Log.w(TAG, "APKMirror direct resolve failed: $releaseUrl", it) }
        val directFailureMessage = directCandidates.exceptionOrNull()
            ?.let { sourceFailureMessage(DownloadSource.APK_MIRROR, it) }
        val resolvedDirectCandidates = directCandidates.getOrDefault(emptyList())

        return resolvedDirectCandidates.ifEmpty {
            listOf(
                apkMirrorReleaseFallbackCandidate(
                    request = request,
                    releaseUrl = releaseUrl,
                    versionName = versionName,
                    option = option,
                    note = directFailureMessage
                )
            )
        }
    }

    private fun apkMirrorDirectCandidatesFromReleaseUrl(
        request: HelperRequest,
        releaseUrl: String,
        versionName: String?,
        option: CandidateOption
    ): List<DownloadCandidate> {
        val releaseDoc = fetchDocument(releaseUrl)
        if (releaseDoc.isCloudflareChallenge()) {
            throw SourceChallengeException(
                "APKMirror showed a Cloudflare browser-verification page instead of the release."
            )
        }

        val variants = apkMirrorVariants(releaseDoc)
        val selectedVariants = if (variants.isEmpty()) {
            listOf(null)
        } else {
            apkMirrorSelectableVariants(request, variants, option)
        }
        if (selectedVariants.isEmpty()) return emptyList()

        // One release page can carry several variants (BUNDLE/APK, per-arch,
        // per-dpi). Numbering them only matters when there is more than one, which
        // is exactly the case where two cards can otherwise look identical.
        val multiVariant = selectedVariants.size > 1
        return selectedVariants
            .mapNotNull { variant ->
                apkMirrorDirectCandidateFromReleaseUrl(
                    request = request,
                    releaseUrl = releaseUrl,
                    versionName = versionName,
                    option = option,
                    variant = variant,
                    variantNumber = variant?.number?.takeIf { multiVariant }
                )
            }
            .distinctBy(DownloadCandidate::identityKey)
    }

    private fun apkMirrorDirectCandidateFromReleaseUrl(
        request: HelperRequest,
        releaseUrl: String,
        versionName: String?,
        option: CandidateOption,
        variant: ApkMirrorVariant?,
        variantNumber: Int? = null
    ): DownloadCandidate? {
        val variantPageUrl = variant?.url ?: releaseUrl
        val variantDoc = if (variant == null) {
            fetchDocument(releaseUrl)
        } else {
            fetchDocument(variantPageUrl, referer = releaseUrl)
        }
        if (variantDoc.isCloudflareChallenge()) {
            throw SourceChallengeException(
                "APKMirror showed a Cloudflare browser-verification page instead of the variant."
            )
        }

        val isBundle = variant?.isBundle ?: apkMirrorPageLooksBundle(variantDoc)
        val versionCode = variant?.versionCode ?: apkMirrorVersionCodeFromVariantPage(variantDoc)
        val requestedCodes = request.requestedVersionCodes
        if (
            option == CandidateOption.REQUESTED &&
            requestedCodes.isNotEmpty() &&
            versionCode != null &&
            versionCode !in requestedCodes
        ) {
            Log.w(
                TAG,
                "Skipping APKMirror build $versionCode at $variantPageUrl: " +
                    "requested ${requestedCodes.joinToString()}."
            )
            return null
        }
        // APKMirror can serve a split bundle even from an APK variant page.
        // When the request only wants a plain APK, both the button and the
        // final link have to ask for the base APK explicitly.
        val forceBaseApk = !request.allowSplitArchive &&
            request.requestedFileKinds.isNotEmpty() &&
            request.requestedFileKinds.all { it == "apk" }
        val downloadButtonUrl = apkMirrorDownloadButtonUrl(variantDoc, forceBaseApk) ?: return null
        val downloadDoc = fetchDocument(downloadButtonUrl, referer = variantPageUrl)
        if (downloadDoc.isCloudflareChallenge()) {
            throw SourceChallengeException(
                "APKMirror showed a Cloudflare browser-verification page instead of the download page."
            )
        }

        val rawFinalUrl = apkMirrorFinalDownloadUrl(downloadDoc) ?: return null
        val finalUrl = if (forceBaseApk) apkMirrorForceBaseApkUrl(rawFinalUrl) else rawFinalUrl
        if (forceBaseApk) {
            Log.i(TAG, "APKMirror forcing base APK for ${request.packageName} from $variantPageUrl")
        }
        val resolvedVersion = versionName
            ?: apkMirrorVersionFromReleaseUrl(releaseUrl)
        // A bundle variant can still serve its base APK: APKMirror sometimes
        // puts forcebaseapk=true on the only download button, and the CDN then
        // returns a plain .apk. The file that will actually be downloaded wins
        // over the page badge, or Morphe gets an APK named .apkm.
        val urlFileKind = fileKindFromUrlOrNull(finalUrl)
        val downloadsBaseApk = forceBaseApk ||
            downloadButtonUrl.contains("forcebaseapk=true", ignoreCase = true) ||
            finalUrl.contains("forcebaseapk=true", ignoreCase = true) ||
            urlFileKind == "apk"
        val fileKind = when {
            downloadsBaseApk -> "apk"
            urlFileKind != null -> urlFileKind
            variant != null -> variant.fileKind
            isBundle -> "apkm"
            else -> fileKindFromUrl(finalUrl)
        }
        val variantLabel = variant?.displayLabel()
        val variantFileSuffix = variantLabel.variantFileSuffix()
        // APKMirror publishes the file's SHA-256 in the variant page's safe-
        // download modal, under the "APK file hashes" block. Single APKs carry
        // it; bundles don't (there is no single file to hash).
        val expectedSha256 = if (isBundle) null else apkMirrorVariantFileSha256(variantDoc)

        return DownloadCandidate(
            source = DownloadSource.APK_MIRROR,
            name = request.appName,
            packageName = request.packageName,
            versionName = resolvedVersion,
            versionCode = versionCode,
            url = finalUrl,
            fileKind = fileKind,
            option = option,
            directDownload = true,
            versionStatus = request.versionStatus(resolvedVersion, versionCode),
            formatMatches = request.acceptsFormat(fileKind),
            variantLabel = variantLabel,
            variantTag = variant?.uiTag(),
            variantNumber = variantNumber,
            files = listOf(
                CandidateDownloadFile(
                    url = finalUrl,
                    fileName = "${request.packageName}-${resolvedVersion ?: option.name.lowercase(Locale.US)}-apkmirror$variantFileSuffix.$fileKind"
                        .sanitizeFileName(),
                    referer = downloadButtonUrl,
                    expectedSha256 = expectedSha256
                )
            )
        )
    }

    /**
     * The file's SHA-256 from the variant page's safe-download modal.
     *
     * The modal lists hashes under two labels: "APK file hashes" (the file's
     * MD5/SHA-1/SHA-256) and "APK certificate fingerprints" (the signer's
     * SHA-1/SHA-256). The file hash is the SHA-256 immediately following the
     * "APK file hashes" label, not the page's first SHA-256 (which would be
     * the certificate's).
     */
    private fun apkMirrorVariantFileSha256(doc: Document): String? {
        val modal = doc.selectFirst("#safeDownload .modal-body")
            ?: doc.selectFirst(".safeDownload .modal-body")
            ?: return null
        val hashesBlock = modal.select("div")
            .firstOrNull { it.text().contains("APK file hashes", ignoreCase = true) }
        val blockText = hashesBlock?.text() ?: modal.text()
        val fileSection = blockText.substringAfter("APK file hashes", "")
            .substringBefore("APK certificate fingerprints")
        val hash = Regex("[0-9a-fA-F]{64}")
            .find(fileSection)
            ?.value
        Log.d(TAG, "APKMirror variant file SHA-256 extracted: ${hash ?: "none"}")
        return hash
    }

    private fun apkMirrorReleaseFallbackCandidate(
        request: HelperRequest,
        releaseUrl: String,
        versionName: String?,
        option: CandidateOption,
        note: String? = null
    ) = DownloadCandidate(
        source = DownloadSource.APK_MIRROR,
        name = request.appName,
        packageName = request.packageName,
        versionName = versionName ?: apkMirrorVersionFromReleaseUrl(releaseUrl),
        versionCode = null,
        url = releaseUrl,
        fileKind = "web",
        option = option,
        directDownload = false,
        versionStatus = request.versionStatus(versionName ?: apkMirrorVersionFromReleaseUrl(releaseUrl), null),
        formatMatches = true,
        note = note,
        captchaUrl = releaseUrl
    )

    private fun apkMirrorBrowserUrl(request: HelperRequest): String {
        val hintedPage = request.sourceHintUrlsFor(DownloadSource.APK_MIRROR)
            .asSequence()
            .mapNotNull(::apkMirrorAbsoluteUrl)
            .firstOrNull(::isApkMirrorHostUrl)
        return hintedPage ?: apkMirrorPackageSearchUrl(request.packageName)
    }

    /** Only an actual APKMirror host is a page this source can open. */
    private fun isApkMirrorHostUrl(url: String): Boolean {
        val host = urlHostOf(url) ?: return false
        return host == "apkmirror.com" || host.endsWith(".apkmirror.com")
    }

    private fun apkMirrorIsSearchPage(url: String): Boolean =
        runCatching {
            if (!isApkMirrorHostUrl(url)) return@runCatching false
            val query = java.net.URI(url).query.orEmpty()
            query.contains("post_type=app_release") || query.contains("searchtype=app")
        }.getOrDefault(false)

    /**
     * The release page's variant rows, numbered in the order it lists them.
     *
     * The table's own header row also carries `table-row headerFont` and is skipped
     * by [apkMirrorVariantFromRow], so the number counts real variants rather than
     * raw rows, and the first one is 1.
     */
    private fun apkMirrorVariants(releaseDoc: Document): List<ApkMirrorVariant> =
        releaseDoc.select("div.table-row.headerFont")
            .mapNotNull(::apkMirrorVariantFromRow)
            .mapIndexed { index, variant -> variant.copy(number = index + 1) }

    private fun apkMirrorSelectableVariants(
        request: HelperRequest,
        variants: List<ApkMirrorVariant>,
        option: CandidateOption
    ): List<ApkMirrorVariant> {
        // A requested build pins the numeric code. Once the page publishes any
        // code, a row without the requested one is rejected before its variant
        // page is fetched, rather than downloading a mismatched build and
        // relying on file validation afterwards.
        val candidates = if (option == CandidateOption.REQUESTED) {
            val requestedCodes = request.requestedVersionCodes
            if (requestedCodes.isEmpty()) {
                variants
            } else {
                val exact = variants.filter {
                    it.versionCode != null && it.versionCode in requestedCodes
                }
                when {
                    exact.isNotEmpty() -> exact
                    variants.any { it.versionCode != null } -> emptyList()
                    else -> variants
                }
            }
        } else {
            variants
        }
        if (candidates.isEmpty()) return emptyList()

        val wantedKinds = apkMirrorWantedVariantKinds(request)
        for (kind in wantedKinds) {
            val typedVariants = candidates.filter {
                it.fileKind == kind &&
                    request.acceptsFormat(it.fileKind)
            }
            typedVariants.preferRunnableArch(request)?.let { return it }
        }

        // No variant matches the requested format (e.g. APKS requested but the
        // release is only offered as an APKM bundle): offer the best variant anyway
        // and let the UI flag the format mismatch, like other sources do.
        candidates.filter { request.acceptsFormat(it.fileKind) }
            .preferRunnableArch(request)
            ?.let { return it }
        return candidates.preferRunnableArch(request) ?: candidates
    }

    /**
     * The variants a device can actually run, best first, or null when empty.
     *
     * One release can carry both `arm64-v8a` and `armeabi-v7a` builds. Handing an
     * arm64 phone the 32-bit one patches fine but installs a 32-bit app, so a build
     * for another ABI is dropped whenever a runnable one exists - and kept only as a
     * last resort, so a source that publishes nothing else still yields something.
     */
    private fun List<ApkMirrorVariant>.preferRunnableArch(
        request: HelperRequest
    ): List<ApkMirrorVariant>? {
        if (isEmpty()) return null
        val ranked = sortedWith(
            compareBy(
                { archMatchRank(it.arch, request.availableAbis) },
                {
                    apkMirrorDpiRank(
                        it.dpi,
                        request.deviceDpi,
                        request.preferDensitySpecificVariants
                    )
                }
            )
        )
        val runnable = ranked.filter {
            archMatchRank(it.arch, request.availableAbis) < ARCH_RANK_MISMATCH
        }
        val pool = runnable.ifEmpty { ranked }
        // nodpi is the safe default. A density-specific row is only preferred
        // when the user opts in, as those rows can be split APKs.
        val bestDpiRank = pool.minOf {
            apkMirrorDpiRank(
                it.dpi,
                request.deviceDpi,
                request.preferDensitySpecificVariants
            )
        }
        return pool.filter {
            apkMirrorDpiRank(
                it.dpi,
                request.deviceDpi,
                request.preferDensitySpecificVariants
            ) == bestDpiRank
        }
    }

    private fun apkMirrorVariantFromRow(row: Element): ApkMirrorVariant? {
        val url = row.selectFirst("div.table-cell:nth-child(1) a[href]")
            ?.attr("href")
            ?.let(::apkMirrorAbsoluteUrl)
            ?: return null
        val type = row.selectFirst("div.table-cell:nth-child(1) span.apkm-badge")
            ?.text()
            ?.trim()
            ?.uppercase(Locale.US)
            ?.takeIf(String::isNotBlank)
            ?: "APK"
        val fileKind = apkMirrorVariantFileKind(type)
        val cells = row.select("div.table-cell")
        val arch = cells.getOrNull(1)?.text()?.trim()?.takeIf(String::isNotBlank)
        val dpi = cells.getOrNull(3)?.text()?.trim()?.takeIf(String::isNotBlank)
        val versionCode = apkMirrorVariantVersionCode(row)

        return ApkMirrorVariant(
            url = url,
            type = type,
            fileKind = fileKind,
            arch = arch,
            dpi = dpi,
            isBundle = fileKind != "apk",
            versionCode = versionCode
        )
    }

    /**
     * The build code APKMirror prints under the variant's version name. The
     * first table cell also carries the upload date in a `.wrapText` span, so
     * only a plain numeric `.colorLightBlack` span is the code.
     */
    private fun apkMirrorVariantVersionCode(row: Element): Long? {
        val firstCell = row.selectFirst("div.table-cell") ?: return null
        return firstCell.select("span.colorLightBlack")
            .asSequence()
            .filterNot { it.hasClass("wrapText") }
            .filterNot { it.select(".dateyear_utc").isNotEmpty() }
            .mapNotNull { span -> span.text().trim().toLongOrNull() }
            .firstOrNull { code -> code > 0L }
    }

    /** The build code on a variant page, e.g. "Version: 1.2.3 (45) (123456)". */
    private fun apkMirrorVersionCodeFromVariantPage(doc: Document): Long? {
        val value = parseInfoTableValue(doc, "Version")
            ?: parseInfoTableValue(doc, "Version code")
            ?: return null
        return Regex("""\((\d{1,12})\)""")
            .findAll(value)
            .lastOrNull()
            ?.groupValues
            ?.getOrNull(1)
            ?.toLongOrNull()
    }

    /**
     * The variant as APKMirror's own table words it, for the card's badge: the
     * architecture and the screen DPI, `nodpi` included.
     *
     * Kept apart from the label used for file names, which drops `nodpi` because a
     * file name has no room to explain it.
     */
    private fun ApkMirrorVariant.uiTag(): String? = listOfNotNull(
        arch?.trim()?.takeIf(String::isNotBlank)?.archDisplayLabel(),
        dpi?.trim()?.takeIf(String::isNotBlank)
    ).joinToString(" / ").takeIf(String::isNotBlank)

    private fun apkMirrorWantedVariantKinds(request: HelperRequest): List<String> {
        val requestedKinds = request.requestedFileKinds
        return DOWNLOAD_FILE_KIND_ORDER
            .filter { it in requestedKinds }
            .ifEmpty { DOWNLOAD_FILE_KIND_ORDER }
    }

    private fun apkMirrorVariantFileKind(type: String): String {
        val normalized = type.lowercase(Locale.US)
        return when {
            "apks" in normalized -> "apks"
            "xapk" in normalized -> "xapk"
            "apkm" in normalized || "bundle" in normalized -> "apkm"
            else -> "apk"
        }
    }

    private fun ApkMirrorVariant.displayLabel(): String? {
        val archLabel = arch?.archDisplayLabel()
        val dpiLabel = dpi
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?.takeUnless { it.equals("nodpi", ignoreCase = true) || it.equals("anydpi", ignoreCase = true) }
        return listOfNotNull(archLabel, dpiLabel)
            .joinToString(" / ")
            .takeIf(String::isNotBlank)
    }


    private fun apkMirrorDownloadButtonUrl(doc: Document, forceBaseApk: Boolean): String? {
        val urls = (
            doc.select("a.downloadButton[href]").map { it.attr("href") } +
                doc.select("a[href*=/download/?key][href]").map { it.attr("href") }
            )
            .mapNotNull(::apkMirrorAbsoluteUrl)
            .distinct()

        return if (forceBaseApk) {
            urls.firstOrNull { it.contains("forcebaseapk", ignoreCase = true) } ?: urls.firstOrNull()
        } else {
            urls.firstOrNull { !it.contains("forcebaseapk", ignoreCase = true) } ?: urls.firstOrNull()
        }
    }

    private fun apkMirrorForceBaseApkUrl(url: String): String {
        if (url.contains("forcebaseapk=", ignoreCase = true)) return url
        val host = runCatching { java.net.URI(url).host }.getOrNull() ?: return url
        val isApkMirror = host.equals("apkmirror.com", ignoreCase = true) ||
            host.endsWith(".apkmirror.com", ignoreCase = true)
        if (!isApkMirror) return url
        val separator = if ('?' in url) "&" else "?"
        return "$url${separator}forcebaseapk=true"
    }

    private fun apkMirrorFinalDownloadUrl(doc: Document): String? =
        doc.selectFirst("a#download-link[href]")
            ?.attr("href")
            ?.let(::apkMirrorAbsoluteUrl)
            ?: doc.select("a[href]")
                .asSequence()
                .map { it.attr("href") }
                .firstOrNull { href ->
                    href.contains("download.php", ignoreCase = true) ||
                        href.contains("/download/?key=", ignoreCase = true)
                }
                ?.let(::apkMirrorAbsoluteUrl)

    private fun apkMirrorPageLooksBundle(doc: Document): Boolean =
        doc.select("span.apkm-badge, .apkm-badge")
            .any { it.text().contains("bundle", ignoreCase = true) }

internal fun apkMirrorVersionFromReleaseUrl(url: String, appPageUrl: String? = null): String? {
    val path = runCatching { java.net.URI(url).path.trimEnd('/') }.getOrNull() ?: return null
    var slug = path.substringAfterLast('/').removeSuffix("-release")
    // A release slug is "<app-slug>-<version-slug>". The app slug can itself
    // end in a digit (e.g. "file-manager-7" in "file-manager-7-3-5-4-release"),
    // which would otherwise leak into the version ("7.3.5.4" instead of
    // "3.5.4") and make Fast Mode "Latest" think an older build is newer than
    // the request. Strip the app slug using the app page URL when available.
    val appSlug = appPageUrl
        ?.let { runCatching { java.net.URI(it).path.trimEnd('/').substringAfterLast('/') }.getOrNull() }
        ?.takeIf(String::isNotBlank)
    if (appSlug != null && slug.startsWith("$appSlug-")) {
        slug = slug.removePrefix("$appSlug-")
    }
    return Regex("""\d+(?:[-.]\d+)+(?:[-.](?:alpha|beta|rc)\d*)?""", RegexOption.IGNORE_CASE)
        .findAll(slug)
        .lastOrNull()
        ?.value
        ?.replace("-", ".")
}

private fun apkMirrorReleaseUrlMatchesVersion(url: String, version: String): Boolean =
        apkMirrorLooksLikeReleaseUrl(url) &&
            (
                apkMirrorVersionFromReleaseUrl(url).versionNameEquals(version) ||
                    runCatching { java.net.URI(url).path.lowercase(Locale.US) }
                        .getOrDefault(url.lowercase(Locale.US))
                        .contains(version.apkMirrorVersionSlug())
                )

    private fun apkMirrorLooksLikeReleaseUrl(url: String): Boolean =
        runCatching {
            val path = java.net.URI(url).path
            path.startsWith("/apk/") && path.trimEnd('/').endsWith("-release")
        }.getOrDefault(false)

    private fun apkMirrorAbsoluteUrl(url: String): String? {
        val normalized = url.substringBefore("#").trim().replace("&amp;", "&")
        if (normalized.isBlank()) return null
        return when {
            normalized.startsWith("http://", ignoreCase = true) ||
                normalized.startsWith("https://", ignoreCase = true) -> normalized
            normalized.startsWith("//") -> "https:$normalized"
            normalized.startsWith("/") -> "https://www.apkmirror.com$normalized"
            else -> "https://www.apkmirror.com/$normalized"
        }.normalizedHttpUrlOrNull()
    }

    private fun Document.isCloudflareChallenge(): Boolean =
        title().contains("Just a moment", ignoreCase = true) ||
            text().contains("Enable JavaScript and cookies to continue", ignoreCase = true)

    private fun Document.isApkMirrorNoResults(): Boolean =
        select("p").any { it.text().contains("No results found matching your query", ignoreCase = true) }

    private fun Document.apkMirrorMatchesPackage(packageName: String): Boolean {
        val hasPackageId = select("[id]").any { it.id() == packageName }
        val hasPlayPackage = select("a[href]")
            .asSequence()
            .map { it.absUrl("href").ifBlank { it.attr("href") } }
            .any { url ->
                url.contains("play.google.com", ignoreCase = true) &&
                    runCatching { Uri.parse(url).getQueryParameter("id") == packageName }
                        .getOrDefault(false)
            }
        val tablePackage = parseInfoTableValue(this, "Package Name") == packageName
        return hasPackageId || hasPlayPackage || tablePackage
    }

    private fun fetchDocument(url: String, referer: String? = null): Document {
        ctx.prefetch?.takeText(url, referer)?.let { html ->
            return Jsoup.parse(html, url)
        }
        return ctx.document(url, referer)
    }
}

/** The APKMirror search URL used by the parser and its prefetch. */
internal fun apkMirrorSearchUrl(packageName: String): String {
    val query = URLEncoder.encode(packageName, "UTF-8")
    return "https://www.apkmirror.com/?post_type=app_release&searchtype=app&s=$query"
}

/**
 * Orders APKMirror's density variants for a device. Lower is better.
 *
 * nodpi wins by default. When [preferDensitySpecific] is on, a range that
 * covers the screen wins, then the closest density-specific range, then nodpi.
 * Density-specific rows can be split APKs, so that choice is opt-in.
 */
internal fun apkMirrorDpiRank(
    dpi: String?,
    deviceDpi: Int?,
    preferDensitySpecific: Boolean = false
): Int {
    val normalized = dpi?.trim()?.lowercase(Locale.US).orEmpty()
    val isNodpi = normalized == "nodpi" || normalized == "anydpi"
    if (!preferDensitySpecific) return if (isNodpi) 0 else 1
    if (isNodpi) return 10_000

    val range = Regex("""(\d+)(?:\s*-\s*(\d+))?""").find(normalized)
        ?: return Int.MAX_VALUE
    if (deviceDpi == null || deviceDpi <= 0) return 1
    val low = range.groupValues[1].toIntOrNull() ?: return Int.MAX_VALUE
    val high = range.groupValues[2].toIntOrNull() ?: low
    return when {
        deviceDpi in low..high -> 0
        deviceDpi < low -> 1 + (low - deviceDpi)
        else -> 1 + (deviceDpi - high)
    }
}

