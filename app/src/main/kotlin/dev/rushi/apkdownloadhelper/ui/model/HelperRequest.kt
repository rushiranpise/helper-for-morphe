package dev.rushi.apkdownloadhelper.ui.model

import android.content.Intent
import android.os.Build
import dev.rushi.apkdownloadhelper.download.hasVariantBuildMarker
import dev.rushi.apkdownloadhelper.download.normalizedVersionName
import dev.rushi.apkdownloadhelper.download.orderedFileKinds
import dev.rushi.apkdownloadhelper.download.requestedFileKindsFrom
import dev.rushi.apkdownloadhelper.download.trailingVersionCode
import dev.rushi.apkdownloadhelper.download.versionNameEquals
import dev.rushi.apkdownloadhelper.download.withoutTrailingVersionCode
import java.util.Locale

internal data class HelperRequest(
    val callerPackage: String,
    val packageName: String,
    val appName: String,
    val versionName: String?,
    val versionCode: Long?,
    val versionCodes: Set<Long>,
    val compatibleVersionNames: Set<String>,
    val compatibleVersionCodes: Set<Long>,
    val supportedAbis: List<String>,
    val requestedFileType: String?,
    val allowSplitArchive: Boolean,
    /** Formats the on-screen picker narrowed the request to, null for all allowed kinds. */
    val requestedFileKindsOverride: Set<String>? = null,
    val stockInstallRequired: Boolean,
    val fallbackWebUrl: String,
    val sourceHintUrls: List<String>,
    /** Screen density in dpi, used to rank density-specific source variants. */
    val deviceDpi: Int? = null,
    /** APKMirror only: opt in to density-specific rows instead of nodpi. */
    val preferDensitySpecificVariants: Boolean = false
) {
    fun withDeviceDpi(
        dpi: Int,
        preferDensitySpecificVariants: Boolean = this.preferDensitySpecificVariants
    ): HelperRequest = copy(
        deviceDpi = dpi.takeIf { it > 0 },
        preferDensitySpecificVariants = preferDensitySpecificVariants
    )

    val availableAbis: List<String>
        get() = supportedAbis
            .ifEmpty { Build.SUPPORTED_ABIS.toList() }
            .mapNotNull { it.trim().takeIf(String::isNotBlank) }
            .distinct()

    val requestedVersionName: String?
        get() = versionName
            ?.withoutTrailingVersionCode()
            ?.takeIf(String::isNotBlank)

    val embeddedVersionCode: Long?
        get() = versionName?.trailingVersionCode()

    val abiSummary: String
        get() = availableAbis.joinToString().ifBlank { "Default" }

    val requestedFileKinds: Set<String>
        get() = requestedFileKindsOverride?.takeIf { it.isNotEmpty() }
            ?: requestedFileKindsFrom(requestedFileType, allowSplitArchive)

    /** Every format the caller allows, before the format picker narrows them. */
    val availableFileKinds: Set<String>
        get() = requestedFileKindsFrom(requestedFileType, allowSplitArchive)

    val requestedFormatLabel: String
        get() = requestedFileKinds
            .orderedFileKinds()
            .joinToString("/") { it.uppercase(Locale.US) }

    val versionCodeSummary: String?
        get() = when {
            requestedVersionCodes.isEmpty() -> null
            requestedVersionCodes.size == 1 -> requestedVersionCodes.first().toString()
            else -> requestedVersionCodes.joinToString(limit = 3, truncated = "+${requestedVersionCodes.size - 3} more")
        }

    val requestedVersionCodes: Set<Long>
        get() = buildSet {
            versionCode?.takeIf { it > 0L }?.let(::add)
            embeddedVersionCode?.takeIf { it > 0L }?.let(::add)
            addAll(versionCodes.filter { it > 0L })
        }

    val knownVersionNames: List<String>
        get() = (listOfNotNull(requestedVersionName) + compatibleVersionNames.map { it.withoutTrailingVersionCode() })
            .mapNotNull { it.trim().takeIf(String::isNotBlank) }
            .distinctBy { it.normalizedVersionName() }

    val requestedVersionNames: List<String>
        get() = listOfNotNull(requestedVersionName)
            .mapNotNull { it.trim().takeIf(String::isNotBlank) }
            .distinctBy { it.normalizedVersionName() }

    val hasRequestedVersionRequest: Boolean
        get() = requestedVersionName != null || requestedVersionCodes.isNotEmpty()

    /** True when the request itself asks for a variant build (e.g. "...-SECONDARY"). */
    val requestsVariantBuild: Boolean
        get() = requestedVersionName.hasVariantBuildMarker()

    val hasKnownVersionRequest: Boolean
        get() = requestedVersionName != null ||
            requestedVersionCodes.isNotEmpty() ||
            compatibleVersionNames.isNotEmpty() ||
            compatibleVersionCodes.any { it > 0L }

    val requestedVersionLabel: String
        get() = listOfNotNull(
            requestedVersionName,
            versionCodeSummary?.let { "build $it" }
        ).joinToString(" ").ifBlank { "any compatible version" }

    fun isRequestedMatch(candidate: DownloadCandidate): Boolean {
        // A secondary build must not satisfy a plain-version request just
        // because its version number or code happens to match.
        if (candidate.hasVariantBuildMarker && !requestsVariantBuild) return false
        return matchesRequestedVersion(candidate.versionName, candidate.versionCode)
    }

    fun versionStatus(candidateVersionName: String?, candidateVersionCode: Long?): VersionStatus {
        if (matchesRequestedVersion(candidateVersionName, candidateVersionCode)) return VersionStatus.REQUESTED

        val compatibleName = candidateVersionName != null &&
            compatibleVersionNames.any { candidateVersionName.versionNameEquals(it.withoutTrailingVersionCode()) }
        val compatibleCode = candidateVersionCode != null &&
            candidateVersionCode > 0L &&
            candidateVersionCode in compatibleVersionCodes
        return if (compatibleName || compatibleCode) VersionStatus.COMPATIBLE else VersionStatus.LATEST
    }

    fun acceptsFormat(fileKind: String): Boolean {
        // Some parsers tag a candidate with the whole requested label (e.g.
        // "APK/APKM/APKS/XAPK") instead of a single kind. Split on '/' so any
        // listed kind counts as a match instead of a false "Format mismatch".
        val kinds = fileKind.lowercase(Locale.US).split('/')
        return kinds.any { it in requestedFileKinds }
    }

    /** Position of [fileKind] in the selected format order, or Int.MAX_VALUE when unselected. */
    fun fileKindPriority(fileKind: String): Int {
        val order = requestedFileKinds.orderedFileKinds()
        return fileKind.lowercase(Locale.US)
            .split('/')
            .map { it to order.indexOf(it) }
            .filter { it.second >= 0 }
            .minOfOrNull { it.second }
            ?: Int.MAX_VALUE
    }

    /**
     * A split request can use APKPure's XAPK asset: it is the same kind of
     * split archive under a different name, and Morphe accepts it whenever
     * split archives are allowed.
     */
    fun acceptsCandidateFormat(fileKind: String): Boolean =
        acceptsFormat(fileKind) ||
            (fileKind.equals("xapk", ignoreCase = true) &&
                allowSplitArchive &&
                requestedFileKinds.any { it == "apks" || it == "apkm" })

    fun matchesKnownVersion(candidateVersionName: String?, candidateVersionCode: Long?): Boolean =
        if (!hasKnownVersionRequest) {
            false
        } else {
            matchesRequestedVersion(candidateVersionName, candidateVersionCode) ||
                (
                    candidateVersionName != null &&
                        compatibleVersionNames.any { candidateVersionName.versionNameEquals(it.withoutTrailingVersionCode()) }
                    ) ||
                (
                    candidateVersionCode != null &&
                        candidateVersionCode > 0L &&
                        candidateVersionCode in compatibleVersionCodes
                    )
        }

    fun matchesRequestedVersion(candidateVersionName: String?, candidateVersionCode: Long?): Boolean {
        val requestedCodes = requestedVersionCodes
        if (versionName == null && requestedCodes.isEmpty()) return false
        if (candidateVersionName.hasVariantBuildMarker() && !requestsVariantBuild) return false

        val nameMatches = requestedVersionName != null && candidateVersionName
            .versionNameEquals(requestedVersionName, ignoreVariantMarker = requestsVariantBuild)
        val codeMatches = requestedCodes.isNotEmpty() &&
            candidateVersionCode != null &&
            candidateVersionCode > 0L &&
            candidateVersionCode in requestedCodes
        return nameMatches || codeMatches
    }

    /**
     * Strict matcher used by Fast Mode, where the exact version name AND the
     * exact version code are both required. Unlike [matchesRequestedVersion]
     * (which accepts a name-only match), a candidate that reports a version
     * code outside the requested set is rejected up front  so Fast Mode
     * doesn't download a file that validation would reject anyway. When the
     * source doesn't report a code, the name match is accepted (validation is
     * still the backstop for the downloaded artifact).
     */
    fun matchesRequestedVersionStrict(candidateVersionName: String?, candidateVersionCode: Long?): Boolean {
        if (versionName == null && requestedVersionCodes.isEmpty()) return false
        if (candidateVersionName.hasVariantBuildMarker() && !requestsVariantBuild) return false

        val nameRequired = requestedVersionName != null
        val nameMatches = candidateVersionName != null &&
            candidateVersionName.versionNameEquals(
                requestedVersionName,
                ignoreVariantMarker = requestsVariantBuild
            )
        if (nameRequired && !nameMatches) return false

        if (requestedVersionCodes.isEmpty()) return true
        val candidateCode = candidateVersionCode ?: return nameMatches
        return candidateCode > 0L && candidateCode in requestedVersionCodes
    }

    /**
     * [matchesRequestedVersionStrict] for a whole candidate: a variant build
     * ("-SECONDARY") the request didn't ask for is rejected as well, which the
     * parsed version number alone can't reveal.
     */
    fun matchesRequestedVersionStrict(candidate: DownloadCandidate): Boolean =
        matchesRequestedVersionStrict(candidate.versionName, candidate.versionCode) &&
            (requestsVariantBuild || !candidate.hasVariantBuildMarker)

    fun sourceHintUrlsFor(source: DownloadSource): List<String> {
        val needles = when (source) {
            DownloadSource.AURORA,
            DownloadSource.PLAY -> listOf("play.google.com")
            DownloadSource.APK_MIRROR -> listOf("apkmirror.com", "google.com/search")
            DownloadSource.APTOIDE -> listOf("aptoide.com")
            DownloadSource.APK_PURE -> listOf("apkpure.com")
            DownloadSource.UPTODOWN -> listOf("uptodown.com")
            DownloadSource.APK_CUBE -> listOf("apkcube.com")
            DownloadSource.MORPHE_LINK -> listOf("morphe.software")
        }

        return (sourceHintUrls + fallbackWebUrl).distinct().filter { url ->
            needles.any { needle -> url.contains(needle, ignoreCase = true) }
        }
    }

    companion object {
        fun from(intent: Intent): HelperRequest? {
            if (intent.action != DownloadHelperContract.ACTION_DOWNLOAD_ORIGINAL_APK) return null
            val packageName = intent.getStringExtra(DownloadHelperContract.EXTRA_PACKAGE_NAME)
                ?.takeIf { it.isNotBlank() }
                ?: return null

            return HelperRequest(
                callerPackage = intent.getStringExtra(DownloadHelperContract.EXTRA_CALLER_PACKAGE)
                    ?: "app.morphe.manager",
                packageName = packageName,
                appName = intent.getStringExtra(DownloadHelperContract.EXTRA_APP_NAME) ?: packageName,
                versionName = intent.getStringExtra(DownloadHelperContract.EXTRA_VERSION_NAME),
                versionCode = if (intent.hasExtra(DownloadHelperContract.EXTRA_VERSION_CODE)) {
                    // Callers send the version code as either an Int (am --ei, some
                    // app stores) or a Long. getLongExtra silently returns the
                    // default for Int extras, so try Long first and fall back to Int.
                    intent.getLongExtra(DownloadHelperContract.EXTRA_VERSION_CODE, 0L)
                        .takeIf { it > 0L }
                        ?: intent.getIntExtra(DownloadHelperContract.EXTRA_VERSION_CODE, 0)
                            .toLong()
                            .takeIf { it > 0L }
                } else {
                    null
                },
                versionCodes = intent
                    .getLongArrayExtra(DownloadHelperContract.EXTRA_VERSION_CODES)
                    ?.filter { it > 0L }
                    ?.toSet()
                    .orEmpty(),
                compatibleVersionNames = intent
                    .getStringArrayListExtra(DownloadHelperContract.EXTRA_COMPATIBLE_VERSION_NAMES)
                    ?.toSet()
                    .orEmpty(),
                compatibleVersionCodes = intent
                    .getLongArrayExtra(DownloadHelperContract.EXTRA_COMPATIBLE_VERSION_CODES)
                    ?.filter { it > 0L }
                    ?.toSet()
                    .orEmpty(),
                supportedAbis = intent
                    .getStringArrayExtra(DownloadHelperContract.EXTRA_SUPPORTED_ABIS)
                    ?.toList()
                    .orEmpty(),
                requestedFileType = intent.getStringExtra(DownloadHelperContract.EXTRA_FILE_TYPE)
                    ?: intent.getStringExtra(DownloadHelperContract.EXTRA_REQUESTED_FILE_TYPE),
                allowSplitArchive = intent.getBooleanExtra(DownloadHelperContract.EXTRA_ALLOW_SPLIT_ARCHIVE, false),
                stockInstallRequired = intent.getBooleanExtra(
                    DownloadHelperContract.EXTRA_STOCK_INSTALL_REQUIRED,
                    intent.getBooleanExtra(DownloadHelperContract.EXTRA_INSTALL_STOCK_AFTER_DOWNLOAD, false)
                ),
                fallbackWebUrl = intent.getStringExtra(DownloadHelperContract.EXTRA_FALLBACK_WEB_URL)
                    ?: "https://www.apkmirror.com/?post_type=app_release&searchtype=app&s=$packageName",
                sourceHintUrls = intent
                    .getStringArrayListExtra(DownloadHelperContract.EXTRA_SOURCE_HINT_URLS)
                    .orEmpty()
            )
        }
    }
}
