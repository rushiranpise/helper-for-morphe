package dev.rushi.apkdownloadhelper.sources

import dev.rushi.apkdownloadhelper.*
import dev.rushi.apkdownloadhelper.ui.*
import dev.rushi.apkdownloadhelper.sources.*
import dev.rushi.apkdownloadhelper.play.*
import dev.rushi.apkdownloadhelper.download.*
import dev.rushi.apkdownloadhelper.storage.*
import dev.rushi.apkdownloadhelper.updater.*
import com.google.gson.annotations.SerializedName
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

private const val GITHUB_API = "https://api.github.com"
private const val GITHUB_RELEASES_PER_PAGE = 30

/** A GitHub repository and, when the URL names one, a release tag. */
internal data class GitHubReleaseTarget(val repo: String, val tag: String?)

/**
 * Reads owner/repo (and an optional release tag) from a GitHub release URL.
 *
 * Only the release paths matter here. A direct asset URL is handled as a
 * direct file before this is called, and ordinary repository pages are not a
 * release the helper can resolve.
 */
internal fun githubReleaseTargetOf(url: String): GitHubReleaseTarget? {
    val uri = runCatching { URI(url) }.getOrNull() ?: return null
    val host = uri.host?.lowercase(Locale.US) ?: return null
    if (host != "github.com" && host != "www.github.com") return null
    val segments = uri.path.orEmpty()
        .trim('/')
        .split('/')
        .filter(String::isNotBlank)
    if (segments.size < 4 || !segments[2].equals("releases", ignoreCase = true)) return null
    val tag = when {
        segments[3].equals("tag", ignoreCase = true) -> segments.drop(4)
            .joinToString("/")
            .takeIf(String::isNotBlank)
        segments[3].equals("download", ignoreCase = true) -> segments.getOrNull(4)
        else -> null
    }
    return GitHubReleaseTarget(
        repo = "${segments[0]}/${segments[1]}",
        tag = tag?.let(::urlDecode)
    )
}

/**
 * Resolves a GitHub release page to one of its APK-family assets.
 *
 * The helper already verifies the downloaded package and version, so this only
 * has to pick the release that names the request and the asset that matches its
 * format and ABI. The public releases endpoint needs no token.
 */
internal suspend fun githubReleaseCandidates(
    ctx: SourceParserContext,
    request: HelperRequest,
    url: String,
    option: CandidateOption
): List<DownloadCandidate> {
    val target = githubReleaseTargetOf(url) ?: return emptyList()
    val apiUrl = "$GITHUB_API/repos/${target.repo}/releases?per_page=$GITHUB_RELEASES_PER_PAGE"
    val body = runCatching { ctx.text(apiUrl) }.getOrNull() ?: return emptyList()
    val releases = runCatching {
        gson.fromJson(body, Array<GitHubRelease>::class.java).orEmpty().toList()
    }.getOrDefault(emptyList())
    if (releases.isEmpty()) return emptyList()

    val requestedVersion = request.requestedVersionNames.firstOrNull()
    for (release in releases) {
        if (release.draft == true) continue
        if (target.tag != null) {
            if (!release.namesVersion(target.tag)) continue
        } else if (requestedVersion != null && !release.namesVersion(requestedVersion)) {
            continue
        }
        val selected = selectGitHubAsset(request, release) ?: continue
        val (fileKind, asset) = selected
        val assetUrl = asset.url ?: continue
        val tagVersion = release.displayVersion()
        val versionName = if (option == CandidateOption.REQUESTED) {
            requestedVersion ?: tagVersion
        } else {
            tagVersion
        }
        val versionCode = (request.versionCode ?: request.versionCodes.singleOrNull())
            ?.takeIf { option == CandidateOption.REQUESTED && it > 0L }
        val assetName = asset.name.orEmpty().sanitizeFileName()
            .ifBlank { "${request.packageName}-${versionName ?: "latest"}-github.$fileKind" }

        return listOf(
            DownloadCandidate(
                source = DownloadSource.MORPHE_LINK,
                name = request.appName,
                packageName = request.packageName,
                versionName = versionName,
                versionCode = versionCode,
                url = assetUrl,
                fileKind = fileKind,
                option = option,
                directDownload = true,
                versionStatus = request.versionStatus(versionName, versionCode),
                formatMatches = request.acceptsCandidateFormat(fileKind),
                note = "GitHub release asset resolved by Morphe.",
                files = listOf(CandidateDownloadFile(url = assetUrl, fileName = assetName))
            )
        )
    }
    return emptyList()
}

private data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String? = null,
    val name: String? = null,
    val draft: Boolean? = null,
    val assets: List<GitHubAsset>? = null
) {
    fun displayVersion(): String? =
        (tagName ?: name)
            ?.trim()
            ?.removePrefix("v")
            ?.removePrefix("V")
            ?.takeIf(String::isNotBlank)

    fun namesVersion(version: String): Boolean {
        val wanted = version.normalizedGitHubVersion()
        if (wanted.isEmpty()) return true
        return tagName.normalizedGitHubVersion() == wanted ||
            name.normalizedGitHubVersion() == wanted
    }
}

private data class GitHubAsset(
    val name: String? = null,
    @SerializedName("browser_download_url") val url: String? = null
)

private fun String?.normalizedGitHubVersion(): String =
    this?.trim()?.lowercase(Locale.US)?.trimStart('v').orEmpty()

private fun selectGitHubAsset(
    request: HelperRequest,
    release: GitHubRelease
): Pair<String, GitHubAsset>? {
    val requestedKinds = DOWNLOAD_FILE_KIND_ORDER.filter { it in request.requestedFileKinds }
    val kinds = (requestedKinds + DOWNLOAD_FILE_KIND_ORDER).distinct()
    for (kind in kinds) {
        if (!request.acceptsCandidateFormat(kind)) continue
        val asset = release.assets.orEmpty()
            .asSequence()
            .filter { asset ->
                asset.url.orEmpty().startsWith("https://") &&
                    asset.name.orEmpty().lowercase(Locale.US).endsWith(".$kind")
            }
            .map { asset ->
                archMatchRank(asset.name, request.availableAbis) to asset
            }
            .filter { (rank, _) -> rank < ARCH_RANK_MISMATCH }
            .minByOrNull { (rank, _) -> rank }
            ?.second
            ?: continue
        return kind to asset
    }
    return null
}

private fun urlDecode(value: String): String =
    runCatching { URLDecoder.decode(value, StandardCharsets.UTF_8.name()) }.getOrDefault(value)

