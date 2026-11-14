package dev.rushi.apkdownloadhelper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceSwitchMatchTest {

    private val request = testRequest(versionName = "2.0.0", versionCode = 20)

    private fun candidate(
        source: DownloadSource = DownloadSource.APK_MIRROR,
        option: CandidateOption = CandidateOption.LATEST,
        versionName: String? = "2.0.0",
        versionCode: Long? = 20,
        fileKind: String = "apk",
        variantLabel: String? = null,
        variantTag: String? = null,
        variantNumber: Int? = null,
        directDownload: Boolean = true
    ) = DownloadCandidate(
        source = source,
        name = "Example App",
        packageName = request.packageName,
        versionName = versionName,
        versionCode = versionCode,
        url = "https://example.com/${source.name.lowercase()}/$versionName",
        fileKind = fileKind,
        option = option,
        directDownload = directDownload,
        versionStatus = if (option == CandidateOption.LATEST) {
            VersionStatus.LATEST
        } else {
            VersionStatus.REQUESTED
        },
        formatMatches = true,
        variantLabel = variantLabel,
        variantTag = variantTag,
        variantNumber = variantNumber
    )

    @Test
    fun latestSwitchKeepsVariantAndLetsTheNewSourcePickItsLatestVersion() {
        val target = candidate(
            option = CandidateOption.LATEST,
            variantLabel = "arm64-v8a",
            variantNumber = 2
        )
        val candidates = listOf(
            candidate(
                source = DownloadSource.APK_PURE,
                option = CandidateOption.LATEST,
                versionName = "3.0.0",
                variantLabel = "ARM64"
            ),
            candidate(
                source = DownloadSource.APK_PURE,
                option = CandidateOption.LATEST,
                versionName = "3.0.0",
                variantLabel = "x86_64"
            ),
            candidate(
                source = DownloadSource.APK_PURE,
                option = CandidateOption.LATEST,
                versionName = "3.0.0",
                fileKind = "xapk",
                variantLabel = "ARM64"
            )
        )

        val matches = matchingSourceSwitchCandidates(candidates, target, request)

        assertEquals(1, matches.size)
        assertEquals("ARM64", matches.single().variantLabel)
    }

    @Test
    fun requestedSwitchKeepsTheRequestedVersion() {
        val target = candidate(option = CandidateOption.REQUESTED, variantLabel = "arm64-v8a")
        val candidates = listOf(
            candidate(
                source = DownloadSource.APK_PURE,
                option = CandidateOption.REQUESTED,
                versionName = "2.0.0",
                variantLabel = "ARM64"
            ),
            candidate(
                source = DownloadSource.APK_PURE,
                option = CandidateOption.REQUESTED,
                versionName = "2.1.0",
                variantLabel = "ARM64"
            )
        )

        val matches = matchingSourceSwitchCandidates(candidates, target, request)

        assertEquals(1, matches.size)
        assertEquals("2.0.0", matches.single().versionName)
    }

    @Test
    fun sameSourceMatchesTheExactVariantNumber() {
        val target = candidate(variantLabel = "arm64-v8a", variantNumber = 2)
        val candidates = listOf(
            candidate(variantLabel = "arm64-v8a", variantNumber = 1),
            candidate(variantLabel = "arm64-v8a", variantNumber = 2),
            candidate(variantLabel = "arm64-v8a", variantNumber = 3)
        )

        val matches = matchingSourceSwitchCandidates(candidates, target, request)

        assertEquals(1, matches.size)
        assertEquals(2, matches.single().variantNumber)
    }

    @Test
    fun crossSourceMatchUsesTheArchRankRatherThanTheLabelWording() {
        val target = candidate(variantLabel = "arm64-v8a")
        val candidates = listOf(
            candidate(source = DownloadSource.APK_PURE, variantLabel = "ARM64"),
            candidate(source = DownloadSource.APK_PURE, variantLabel = "x86_64")
        )

        val matches = matchingSourceSwitchCandidates(candidates, target, request)

        assertEquals(1, matches.size)
        assertEquals("ARM64", matches.single().variantLabel)
    }

    @Test
    fun aUniversalTargetDoesNotSilentlyTakeAnArchSpecificBuild() {
        val target = candidate(source = DownloadSource.APK_PURE, variantLabel = "Universal")
        val candidates = listOf(
            candidate(source = DownloadSource.APK_MIRROR, variantLabel = "arm64-v8a"),
            candidate(source = DownloadSource.APK_MIRROR, variantLabel = "Universal")
        )

        val matches = matchingSourceSwitchCandidates(candidates, target, request)

        assertEquals(1, matches.size)
        assertEquals("Universal", matches.single().variantLabel)
    }

    @Test
    fun ambiguousMatchesAreLeftForThePickerInsteadOfTakingTheFirstOne() {
        val target = candidate(variantLabel = "arm64-v8a")
        val candidates = listOf(
            candidate(source = DownloadSource.APK_PURE, variantLabel = "ARM64"),
            candidate(source = DownloadSource.APK_COMBO, variantLabel = "arm64")
        )

        val matches = matchingSourceSwitchCandidates(candidates, target, request)

        assertEquals(2, matches.size)
    }

    @Test
    fun nonDirectAndDifferentFormatCandidatesAreNotMatches() {
        val target = candidate(variantLabel = "arm64-v8a")
        val candidates = listOf(
            candidate(source = DownloadSource.APK_PURE, variantLabel = "ARM64", directDownload = false),
            candidate(source = DownloadSource.APK_PURE, variantLabel = "ARM64", fileKind = "xapk")
        )

        assertTrue(matchingSourceSwitchCandidates(candidates, target, request).isEmpty())
    }
}
