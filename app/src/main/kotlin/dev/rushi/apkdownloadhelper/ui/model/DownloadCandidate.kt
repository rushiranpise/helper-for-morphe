package dev.rushi.apkdownloadhelper.ui.model

import dev.rushi.apkdownloadhelper.download.hasVariantBuildMarker
import dev.rushi.apkdownloadhelper.download.versionNameEquals
import dev.rushi.apkdownloadhelper.sources.archMatchRank
import java.util.Locale

internal data class DownloadCandidate(
    val source: DownloadSource,
    val name: String,
    val packageName: String,
    val versionName: String?,
    val versionCode: Long?,
    val url: String,
    val fileKind: String,
    val option: CandidateOption,
    val directDownload: Boolean,
    val versionStatus: VersionStatus,
    val formatMatches: Boolean,
    val note: String? = null,
    val variantLabel: String? = null,
    /** Source's own wording for the variant (APKMirror's arch / DPI), for display. */
    val variantTag: String? = null,
    /** Which variant of the release this is, when the source offered several. */
    val variantNumber: Int? = null,
    val files: List<CandidateDownloadFile> = emptyList(),
    // When set, the source gates the file behind a captcha/JS challenge that
    // only a real browser can pass. The card then offers a "Solve captcha in
    // app" action that opens this URL in an embedded WebView and captures the
    // download the page produces.
    val captchaUrl: String? = null,
    // The source whose page a wrapper such as Morphe Link resolved. It keeps the
    // candidate branded as the wrapper while still selecting that source's browser
    // script for a captcha page.
    val viaSource: DownloadSource? = null,
    // True only for a web link synthesized from the request when a source has no
    // answer. A resolved candidate can share that URL and must still be offered.
    val syntheticFallback: Boolean = false
) {
    val sortIndex: Int get() = source.sortIndex

    /**
     * True when the candidate itself carries a variant marker such as
     * "-SECONDARY"  in the parsed version, the release URL, the variant label
     * or the file name. The version number alone is not enough: APKMirror
     * parses the suffix out of the release slug, so the URL and file name are
     * the only reliable signals.
     */
    val hasVariantBuildMarker: Boolean
        get() = versionName.hasVariantBuildMarker() ||
            url.hasVariantBuildMarker() ||
            variantLabel.hasVariantBuildMarker() ||
            files.any { it.fileName.hasVariantBuildMarker() || it.url.hasVariantBuildMarker() }

    val versionDisplay: String
        get() = when {
            versionName != null && versionCode != null -> "$versionName ($versionCode)"
            versionName != null -> versionName
            option == CandidateOption.MANUAL -> "Manual"
            option == CandidateOption.REQUESTED -> "Requested search"
            else -> "Latest"
        }
}

/**
 * Candidates from a newly picked source that continue the same download target:
 * same format, same version for a requested build, and the ABI/variant already
 * in flight. Fast Mode's latest mode deliberately leaves the version open,
 * because the new source resolves its own newest release.
 */
internal fun matchingSourceSwitchCandidates(
    candidates: List<DownloadCandidate>,
    target: DownloadCandidate,
    request: HelperRequest
): List<DownloadCandidate> =
    candidates.filter { candidate ->
        candidate.directDownload &&
            candidate.fileKind.equals(target.fileKind, ignoreCase = true) &&
            sourceSwitchVersionMatches(target, candidate) &&
            sourceSwitchVariantMatches(target, candidate, request)
    }

internal fun sourceSwitchVersionMatches(
    target: DownloadCandidate,
    candidate: DownloadCandidate
): Boolean {
    if (target.option == CandidateOption.LATEST) return true
    val targetVersion = target.versionName
    if (targetVersion != null) {
        return candidate.versionName?.versionNameEquals(targetVersion) == true
    }
    val targetCode = target.versionCode ?: return true
    return candidate.versionCode == targetCode
}

internal fun sourceSwitchVariantMatches(
    target: DownloadCandidate,
    candidate: DownloadCandidate,
    request: HelperRequest
): Boolean {
    val targetLabel = target.variantLabel ?: target.variantTag
    val candidateLabel = candidate.variantLabel ?: candidate.variantTag

    if (target.source == candidate.source) {
        if (target.variantNumber != null && candidate.variantNumber != null) {
            return target.variantNumber == candidate.variantNumber
        }
        if (targetLabel != null || candidateLabel != null) {
            return targetLabel.normalizedVariantLabel() == candidateLabel.normalizedVariantLabel()
        }
        return true
    }

    // Labels differ between sources ("arm64-v8a", "ARM64", "arm64"), so
    // compare how each one fits the device instead of the wording.
    return archMatchRank(targetLabel, request.availableAbis) ==
        archMatchRank(candidateLabel, request.availableAbis)
}

internal fun String?.normalizedVariantLabel(): String =
    this?.trim()?.lowercase(Locale.US)?.replace(Regex("\\s+"), " ").orEmpty()

internal data class CandidateMatchSummary(
    val matches: Boolean,
    val title: String,
    val details: List<String>
)

internal fun DownloadCandidate.matchSummary(request: HelperRequest): CandidateMatchSummary {
    val requestedVersionNames = request.requestedVersionNames
    val versionNameMatches = requestedVersionNames.isEmpty() ||
        (versionName != null && requestedVersionNames.any { versionName.versionNameEquals(it) })
    val requestedVersionCodes = request.requestedVersionCodes
    val versionCodeMatches = requestedVersionCodes.isEmpty() ||
        versionCode == null ||
        versionCode in requestedVersionCodes
    val formatMatches = fileKind.equals("web", ignoreCase = true) ||
        request.acceptsCandidateFormat(fileKind)

    val mismatchNames = buildList {
        if (!versionNameMatches) add("Version")
        if (!versionCodeMatches) add("Version code")
        if (!formatMatches) add("Format")
    }
    val details = buildList {
        if (!versionNameMatches) {
            add("Version: requested ${requestedVersionNames.joinToString().ifBlank { "Any" }}, found ${versionName ?: "Unknown"}")
        }
        if (!versionCodeMatches) {
            add("Version code: requested ${requestedVersionCodes.joinToString()}, found $versionCode")
        }
        if (!formatMatches) {
            add("Format: requested ${request.requestedFormatLabel}, found ${fileKind.uppercase()}")
        }
    }
    val matches = mismatchNames.isEmpty()

    return CandidateMatchSummary(
        matches = matches,
        title = if (matches) {
            "Same as recommended version"
        } else {
            "${mismatchNames.joinToString(" / ")} mismatch"
        },
        details = details
    )
}

internal fun DownloadCandidate.identityKey(): String =
    "${source.name}:$versionName:$versionCode:$fileKind:$variantLabel:$url"
