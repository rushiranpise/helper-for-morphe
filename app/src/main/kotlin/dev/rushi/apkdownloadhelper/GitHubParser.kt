package dev.rushi.apkdownloadhelper

import android.util.Log
import com.google.gson.annotations.SerializedName
import java.net.URLDecoder
import java.util.Locale

/**
 * GitHub releases, for apps that publish their APK as a release asset.
 *
 * GitHub has no search by package name, so this source only works from a link: Morphe's
 * resolver sends some apps to their GitHub release (or straight to an attached file) instead
 * of APKMirror or Uptodown, and Morphe's own download dialog follows it there. This source
 * takes that link, reads the release through the GitHub API, and picks the asset built for
 * this device.
 */
private const val GITHUB_HOST = "github.com"
private const val GITHUB_API = "https://api.github.com"

private val DOWNLOADABLE_EXTENSIONS = setOf("apk", "apkm", "apks", "xapk")

/**
 * Where a github.com releases link points.
 *
 * @param tag The one release it names, or null for the list of every release.
 * @param assetName The file it names, when it links an attached file directly.
 */
internal data class GitHubReleaseLink(
    val owner: String,
    val repo: String,
    val tag: String?,
    val assetName: String? = null
) {
    val releasesPageUrl: String
        get() = "https://$GITHUB_HOST/$owner/$repo/releases"
}

/**
 * Reads a github.com releases link, in every shape Morphe's resolver answers with:
 * `/releases`, `/releases/latest`, `/releases/tag/<tag>`, `/releases#release-<tag>` and
 * `/releases/download/<tag>/<file>`.
 *
 * Returns null for anything else, including other pages of a repository, so an unrelated
 * GitHub link never turns into API lookups.
 */
internal fun gitHubReleaseLinkOf(url: String): GitHubReleaseLink? {
    val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return null
    val host = uri.host?.lowercase(Locale.US)?.removePrefix("www.") ?: return null
    if (host != GITHUB_HOST) return null
    val parts = uri.rawPath.orEmpty().split('/').filter(String::isNotEmpty)
    if (parts.size < 3 || parts[2] != "releases") return null
    val owner = parts[0]
    val repo = parts[1]
    val rest = parts.drop(3).map(::decodePathSegment)

    return when (rest.firstOrNull()) {
        null -> GitHubReleaseLink(
            owner,
            repo,
            tag = uri.fragment?.substringAfter("release-", "")?.ifEmpty { null }
        )
        "latest" -> GitHubReleaseLink(owner, repo, tag = null)
        "tag" -> rest.drop(1).joinToString("/").ifEmpty { null }
            ?.let { GitHubReleaseLink(owner, repo, tag = it) }
        "download" -> {
            // A tag may itself contain '/', but GitHub encodes it in download links, so the
            // tag is one segment and the file name is the last.
            if (rest.size < 3) return null
            GitHubReleaseLink(owner, repo, tag = rest[1], assetName = rest.last())
        }
        else -> null
    }
}

private fun decodePathSegment(segment: String): String =
    runCatching { URLDecoder.decode(segment.replace("+", "%2B"), "UTF-8") }.getOrDefault(segment)

/**
 * The version a release tag names: `v1.14.2` and `1.14.2` both read as `1.14.2`.
 *
 * Only a leading `v` directly before a digit is dropped, so a tag that is a word
 * (`vanilla-2.0`) keeps its spelling.
 */
internal fun gitHubVersionOfTag(tag: String): String {
    val trimmed = tag.trim()
    return if (trimmed.length > 1 && (trimmed[0] == 'v' || trimmed[0] == 'V') && trimmed[1].isDigit()) {
        trimmed.substring(1)
    } else {
        trimmed
    }
}

internal data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String? = null,
    val name: String? = null,
    @SerializedName("html_url") val htmlUrl: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubAsset> = emptyList()
)

internal data class GitHubAsset(
    val name: String? = null,
    @SerializedName("browser_download_url") val downloadUrl: String? = null,
    val size: Long? = null,
    /** `sha256:<hex>`, published for assets uploaded since mid 2025. */
    val digest: String? = null
)

private val ABI_MENTION = Regex("""arm64|aarch64|v8a|armeabi|v7a|armv7|x86""", RegexOption.IGNORE_CASE)

/**
 * The architecture rank of an asset by its file name.
 *
 * Release assets name their ABI loosely (`app-arm64-v8a-release.apk`, Brave's
 * `BraveMonox64.apk`) or not at all (`app-release.apk`), and a name that mentions none is a
 * universal build rather than a mismatch.
 *
 * A name that says `universal` next to an ABI (`Bravearm64Universal.apk`) is still built for
 * that ABI only: it is dropped on other devices, and ranks as universal on a matching one so
 * the plain ABI build stays ahead of it.
 */
internal fun gitHubAssetArchRank(fileName: String, availableAbis: List<String>): Int {
    val normalized = fileName.lowercase(Locale.US)
        .replace("aarch64", "arm64")
        .replace("x64", "x86_64")
    if (!ABI_MENTION.containsMatchIn(normalized)) return ARCH_RANK_UNIVERSAL
    val rank = archMatchRank(normalized.replace("universal", ""), availableAbis)
    return if (rank != ARCH_RANK_MISMATCH && "universal" in normalized) ARCH_RANK_UNIVERSAL else rank
}

/**
 * The downloadable assets of a release this device can install, best first.
 *
 * Builds for other ABIs are dropped. Among the rest, the device's preferred ABI wins, then
 * a universal build, and a debug build only comes after every other one.
 */
internal fun gitHubInstallableAssets(
    assets: List<GitHubAsset>,
    availableAbis: List<String>
): List<GitHubAsset> =
    assets
        .filter { asset ->
            val name = asset.name.orEmpty()
            name.substringAfterLast('.', "").lowercase(Locale.US) in DOWNLOADABLE_EXTENSIONS &&
                !asset.downloadUrl.isNullOrBlank()
        }
        .map { asset -> asset to gitHubAssetArchRank(asset.name.orEmpty(), availableAbis) }
        .filter { (_, rank) -> rank < ARCH_RANK_MISMATCH }
        .sortedWith(
            compareBy<Pair<GitHubAsset, Int>> { (asset, _) ->
                asset.name.orEmpty().contains("debug", ignoreCase = true)
            }
                .thenBy { (_, rank) -> rank }
        )
        .map { (asset, _) -> asset }

/**
 * The files Fast Mode should let the user choose between, or an empty list when one is
 * clearly the best fit.
 *
 * Repositories name their builds their own way, so several files are only a question when
 * they tie for first place: two builds for this device's ABI (`app-foss-arm64.apk`,
 * `app-play-arm64.apk`), or several that name no ABI at all. A debug build never ties with a
 * release build.
 */
internal fun gitHubTiedChoices(
    candidates: List<DownloadCandidate>,
    availableAbis: List<String>
): List<DownloadCandidate> {
    val files = candidates.filter { it.source == DownloadSource.GITHUB && it.directDownload }
    val placeOf = { candidate: DownloadCandidate ->
        val name = candidate.variantLabel.orEmpty()
        name.contains("debug", ignoreCase = true) to gitHubAssetArchRank(name, availableAbis)
    }
    val best = files.minWithOrNull(
        compareBy<DownloadCandidate> { placeOf(it).first }.thenBy { placeOf(it).second }
    ) ?: return emptyList()
    return files
        .filter { it.versionName == best.versionName && placeOf(it) == placeOf(best) }
        .takeIf { it.size > 1 }
        .orEmpty()
}

/** The hex digest of a `sha256:<hex>` asset digest, or null for any other algorithm. */
internal fun gitHubSha256Of(digest: String?): String? =
    digest?.trim()
        ?.takeIf { it.startsWith("sha256:", ignoreCase = true) }
        ?.substringAfter(':')
        ?.lowercase(Locale.US)
        ?.takeIf { it.length == 64 && it.all { c -> c in '0'..'9' || c in 'a'..'f' } }

internal class GitHubParser(private val ctx: SourceParserContext) : ApkSourceParser {
    override val source: DownloadSource = DownloadSource.GITHUB

    override suspend fun findCandidates(
        request: HelperRequest,
        option: CandidateOption
    ): List<DownloadCandidate> {
        if (option == CandidateOption.MANUAL) return emptyList()
        val link = releaseLink(request) ?: return emptyList()
        return when (option) {
            CandidateOption.REQUESTED -> requestedCandidates(request, link)
            CandidateOption.LATEST -> latestCandidates(request, link)
            CandidateOption.MANUAL -> emptyList()
        }
    }

    override fun requestedFallbackCandidate(request: HelperRequest): DownloadCandidate? =
        releasesPageCandidate(request, CandidateOption.REQUESTED)

    override fun latestFallbackCandidate(request: HelperRequest): DownloadCandidate? =
        releasesPageCandidate(request, CandidateOption.LATEST)

    override suspend fun resolveHistory(request: HelperRequest): List<DownloadCandidate> {
        val link = releaseLink(request) ?: return emptyList()
        return releases(link)
            .mapNotNull { release -> candidatesOf(request, link, release, CandidateOption.LATEST).firstOrNull() }
            .sortedWith { left, right -> compareVersionNames(right.versionName, left.versionName) }
    }

    /** History rows already name their file, so there is nothing left to resolve. */
    override suspend fun resolveHistoryCandidate(
        request: HelperRequest,
        candidate: DownloadCandidate
    ): DownloadCandidate? = candidate.takeIf { it.directDownload }

    // The resolver is the only way to learn the repository, and its answer for a request
    // does not change, so it is asked once per request rather than per tab. A null is not
    // kept: the resolver also answers null when it could not be reached.
    @Volatile
    private var cachedLink: Pair<HelperRequest, GitHubReleaseLink?>? = null

    private fun releaseLink(request: HelperRequest): GitHubReleaseLink? {
        cachedLink?.takeIf { it.first == request }?.let { return it.second }
        val link = request.sourceHintUrlsFor(DownloadSource.GITHUB).firstNotNullOfOrNull(::gitHubReleaseLinkOf)
            ?: ctx.resolveViaMorphe?.invoke(request)?.let(::gitHubReleaseLinkOf)
        if (link != null) cachedLink = request to link
        return link
    }

    private fun requestedCandidates(request: HelperRequest, link: GitHubReleaseLink): List<DownloadCandidate> {
        val tagged = link.tag?.let { tag -> runCatching { release(link, tag) }.getOrNull() }
        if (tagged != null && request.matchesRequestedVersion(versionOf(tagged), null)) {
            return candidatesOf(request, link, tagged, CandidateOption.REQUESTED)
        }
        // The link names another release, or none: look for the requested one by tag.
        val match = releases(link).firstOrNull { request.matchesRequestedVersion(versionOf(it), null) }
            ?: return emptyList()
        return candidatesOf(request, link, match, CandidateOption.REQUESTED)
    }

    private fun latestCandidates(request: HelperRequest, link: GitHubReleaseLink): List<DownloadCandidate> {
        // `/releases/latest` skips pre-releases and answers 404 when a repository only has
        // those, so the newest published release in the list stands in for it.
        val latest = runCatching { fetch<GitHubRelease>("${repoApi(link)}/releases/latest") }.getOrNull()
            ?: releases(link).firstOrNull()
            ?: return emptyList()
        return candidatesOf(request, link, latest, CandidateOption.LATEST)
    }

    private fun candidatesOf(
        request: HelperRequest,
        link: GitHubReleaseLink,
        release: GitHubRelease,
        option: CandidateOption
    ): List<DownloadCandidate> {
        val versionName = versionOf(release) ?: return emptyList()
        // A link straight to one file means Morphe picked it: offer that one first.
        val assets = gitHubInstallableAssets(release.assets, request.availableAbis)
            .sortedBy { asset -> if (link.assetName != null && asset.name == link.assetName) 0 else 1 }
        if (assets.isEmpty()) {
            Log.w(TAG, "GitHub release ${release.tagName} of ${link.owner}/${link.repo} has no installable APK")
        }
        val pageUrl = release.htmlUrl ?: link.releasesPageUrl
        return assets.mapIndexed { index, asset ->
            val fileName = asset.name.orEmpty()
            val fileKind = fileName.substringAfterLast('.').lowercase(Locale.US)
            DownloadCandidate(
                source = DownloadSource.GITHUB,
                name = request.appName,
                packageName = request.packageName,
                versionName = versionName,
                versionCode = null,
                url = pageUrl,
                fileKind = fileKind,
                option = option,
                directDownload = true,
                versionStatus = request.versionStatus(versionName, null),
                formatMatches = request.acceptsFormat(fileKind),
                note = "${link.owner}/${link.repo}" + if (release.prerelease) " (pre-release)" else "",
                variantLabel = fileName,
                variantNumber = (index + 1).takeIf { assets.size > 1 },
                files = listOf(
                    CandidateDownloadFile(
                        url = asset.downloadUrl.orEmpty(),
                        fileName = fileName,
                        size = asset.size?.takeIf { it > 0L },
                        expectedSha256 = gitHubSha256Of(asset.digest)
                    )
                )
            )
        }
    }

    private fun releasesPageCandidate(request: HelperRequest, option: CandidateOption): DownloadCandidate? {
        val link = cachedLink?.takeIf { it.first == request }?.second ?: return null
        return DownloadCandidate(
            source = DownloadSource.GITHUB,
            name = request.appName,
            packageName = request.packageName,
            versionName = request.requestedVersionNames.firstOrNull().takeIf { option == CandidateOption.REQUESTED },
            versionCode = null,
            url = link.tag?.let { "${link.releasesPageUrl}/tag/$it" } ?: link.releasesPageUrl,
            fileKind = "web",
            option = option,
            directDownload = false,
            versionStatus = if (option == CandidateOption.REQUESTED) VersionStatus.REQUESTED else VersionStatus.LATEST,
            formatMatches = true
        )
    }

    private fun versionOf(release: GitHubRelease): String? =
        release.tagName?.takeIf(String::isNotBlank)?.let(::gitHubVersionOfTag)

    private fun release(link: GitHubReleaseLink, tag: String): GitHubRelease =
        fetch("${repoApi(link)}/releases/tags/${tag.encodedPathSegment()}")

    /** The first page of releases, newest first; drafts are only visible to the owner. */
    private fun releases(link: GitHubReleaseLink): List<GitHubRelease> =
        fetch<Array<GitHubRelease>>("${repoApi(link)}/releases?per_page=50")
            .filterNot(GitHubRelease::draft)

    private fun repoApi(link: GitHubReleaseLink): String =
        "$GITHUB_API/repos/${link.owner.encodedPathSegment()}/${link.repo.encodedPathSegment()}"

    private inline fun <reified T> fetch(url: String): T =
        gson.fromJson(ctx.text(url), T::class.java)

    private fun String.encodedPathSegment(): String =
        java.net.URLEncoder.encode(this, "UTF-8").replace("+", "%20")
}
