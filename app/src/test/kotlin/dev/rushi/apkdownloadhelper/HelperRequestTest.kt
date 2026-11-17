package dev.rushi.apkdownloadhelper

import dev.rushi.apkdownloadhelper.download.belongsTo
import dev.rushi.apkdownloadhelper.download.belongsToCurrentSession
import dev.rushi.apkdownloadhelper.download.fileKindFromUrl
import dev.rushi.apkdownloadhelper.download.hasVariantBuildMarker
import dev.rushi.apkdownloadhelper.download.versionNameEquals
import dev.rushi.apkdownloadhelper.ui.CandidateOption
import dev.rushi.apkdownloadhelper.ui.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.DownloadSource
import dev.rushi.apkdownloadhelper.ui.VersionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HelperRequestTest {

    private fun multiFormatRequest() = testRequest(
        packageName = "com.zhiliaoapp.musically",
        appName = "TikTok",
        versionName = "46.2.3",
        requestedFileType = "APK/APKM/APKS/XAPK",
        allowSplitArchive = true
    )

    @Test
    fun acceptsFormat_acceptsAnySingleKindFromMultiKindRequest() {
        val request = multiFormatRequest()
        assertTrue(request.acceptsFormat("apk"))
        assertTrue(request.acceptsFormat("apkm"))
        assertTrue(request.acceptsFormat("apks"))
        assertTrue(request.acceptsFormat("xapk"))
    }

    @Test
    fun acceptsFormat_acceptsWholeMultiKindLabel() {
        val request = multiFormatRequest()
        assertTrue(request.acceptsFormat("APK/APKM/APKS/XAPK"))
        assertTrue(request.acceptsFormat("apk/apkm/apks/xapk"))
    }

    @Test
    fun acceptsFormat_rejectsKindsOutsideRequest() {
        val request = multiFormatRequest()
        assertFalse(request.acceptsFormat("web"))
        assertFalse(request.acceptsFormat("zip"))
    }

    @Test
    fun acceptsFormat_rejectsKindsOutsideSingleKindRequest() {
        val request = testRequest(requestedFileType = "APKS", allowSplitArchive = true)
        assertTrue(request.acceptsFormat("apks"))
        assertFalse(request.acceptsFormat("apk"))
        assertFalse(request.acceptsFormat("xapk"))
        // A candidate labeled with the generic multi-kind tag still covers APKS.
        assertTrue(request.acceptsFormat("APK/APKM/APKS/XAPK"))
    }

    @Test
    fun selectedFormats_narrowResolutionWithoutLosingAvailableKinds() {
        val request = testRequest(requestedFileType = "APK", allowSplitArchive = true)
        assertEquals(setOf("apk", "apkm", "apks", "xapk"), request.availableFileKinds)

        val narrowed = request.copy(requestedFileKindsOverride = setOf("apkm", "apk"))
        assertEquals(setOf("apkm", "apk"), narrowed.requestedFileKinds)
        assertTrue(narrowed.acceptsFormat("apk"))
        assertTrue(narrowed.acceptsFormat("apkm"))
        assertFalse(narrowed.acceptsFormat("apks"))
        assertEquals(0, narrowed.fileKindPriority("apk"))
        assertEquals(1, narrowed.fileKindPriority("apkm"))
        assertEquals(Int.MAX_VALUE, narrowed.fileKindPriority("apks"))
        assertEquals(setOf("apk", "apkm", "apks", "xapk"), narrowed.availableFileKinds)
    }

    @Test
    fun fileKindPriority_usesPickerOrderForMultiKindLabels() {
        val request = multiFormatRequest()
        assertEquals(0, request.fileKindPriority("APK/APKM/APKS/XAPK"))
        assertEquals(1, request.fileKindPriority("APKM"))
        assertEquals(3, request.fileKindPriority("XAPK"))
    }

    @Test
    fun acceptsCandidateFormat_acceptsXapkAsSplitSubstitute() {
        for (kind in listOf("APKS", "APKM")) {
            val request = testRequest(requestedFileType = kind, allowSplitArchive = true)
            assertTrue(request.acceptsCandidateFormat("xapk"))
        }

        // Without permission to install split archives, XAPK is not a substitute.
        val noSplit = testRequest(requestedFileType = "APKS", allowSplitArchive = false)
        assertFalse(noSplit.acceptsCandidateFormat("xapk"))
        assertFalse(testRequest(requestedFileType = "APK").acceptsCandidateFormat("xapk"))
    }

    // ---- Fast Mode strict version matching (exact name AND code) ----

    @Test
    fun strictMatch_acceptsSameNameAndCode() {
        val request = testRequest(versionName = "2.5.0.6", versionCode = 723)
        assertTrue(request.matchesRequestedVersionStrict("2.5.0.6", 723))
    }

    @Test
    fun strictMatch_rejectsNameMatchWithDifferentCode() {
        // Battery Guru case: request wants build 723, source only has 721.
        val request = testRequest(versionName = "2.5.0.6", versionCode = 723)
        assertFalse(request.matchesRequestedVersionStrict("2.5.0.6", 721))
        // ...even though the lenient matcher (used by the Recommended tab)
        // still treats it as a name match.
        assertTrue(request.matchesRequestedVersion("2.5.0.6", 721))
    }

    @Test
    fun strictMatch_acceptsNameMatchWhenSourceDoesNotReportCode() {
        val request = testRequest(versionName = "2.5.0.6", versionCode = 723)
        assertTrue(request.matchesRequestedVersionStrict("2.5.0.6", null))
    }

    @Test
    fun strictMatch_rejectsDifferentNameWithSameCode() {
        val request = testRequest(versionName = "2.5.0.6", versionCode = 723)
        assertFalse(request.matchesRequestedVersionStrict("2.5.1.0", 723))
    }

    @Test
    fun strictMatch_nameOnlyRequestIgnoresCandidateCode() {
        val request = testRequest(versionName = "2.5.0.6", versionCode = null)
        assertTrue(request.matchesRequestedVersionStrict("2.5.0.6", 721))
        assertTrue(request.matchesRequestedVersionStrict("2.5.0.6", 723))
    }

    @Test
    fun strictMatch_rejectsWhenRequestHasNoVersionAtAll() {
        val request = testRequest(versionName = null, versionCode = null)
        assertFalse(request.matchesRequestedVersionStrict("2.5.0.6", 721))
    }

    // ---- secondary / variant builds ----

    @Test
    fun variantBuildMarker_detectsSecondarySuffixInVersionUrlAndFileName() {
        assertTrue("21.36.45-SECONDARY".hasVariantBuildMarker())
        assertTrue("21.36.45-secondary".hasVariantBuildMarker())
        assertTrue("21_36_45_SECONDARY".hasVariantBuildMarker())
        assertTrue(
            "https://www.apkmirror.com/apk/whatsapp/whatsapp/whatsapp-21-36-45-secondary-release/"
                .hasVariantBuildMarker()
        )
        assertTrue("com.whatsapp_21.36.45-SECONDARY-apkmirror.apk".hasVariantBuildMarker())
    }

    @Test
    fun variantBuildMarker_ignoresPlainBuildsAndUnrelatedNames() {
        assertFalse("21.36.45".hasVariantBuildMarker())
        assertFalse((null as String?).hasVariantBuildMarker())
        // An app that merely has "secondary" in its slug is not a variant build.
        assertFalse(
            "https://example.com/apk/secondary/secondary-1-2-3-release/".hasVariantBuildMarker()
        )
    }

    @Test
    fun versionNameEquals_rejectsSecondaryBuildOfPlainVersion() {
        assertFalse("21.36.45-SECONDARY".versionNameEquals("21.36.45"))
        assertFalse("21.36.45".versionNameEquals("21.36.45-SECONDARY"))
        assertTrue("21.36.45-SECONDARY".versionNameEquals("21.36.45-secondary"))
    }

    @Test
    fun requestedMatch_rejectsSecondaryBuildUnlessRequested() {
        val plain = testRequest(versionName = "21.36.45")
        assertFalse(plain.matchesRequestedVersion("21.36.45-SECONDARY", null))
        assertFalse(plain.matchesRequestedVersionStrict("21.36.45-SECONDARY", null))

        val secondary = testRequest(versionName = "21.36.45-SECONDARY")
        assertTrue(secondary.matchesRequestedVersionStrict("21.36.45-SECONDARY", null))
    }

    @Test
    fun isRequestedMatch_rejectsSecondaryBuildSharingVersionNumber() {
        // APKMirror parses the suffix out of the release slug, so the version
        // number looks normal  the release URL is the only signal.
        val request = testRequest(versionName = "21.36.45")
        val secondary = secondaryCandidate()

        assertTrue(secondary.hasVariantBuildMarker)
        assertFalse(request.isRequestedMatch(secondary))
        assertFalse(request.matchesRequestedVersionStrict(secondary))
    }

    @Test
    fun requestedVariantBuild_letsAnExplicitSecondaryRequestThrough() {
        val request = testRequest(versionName = "21.36.45-SECONDARY")
        val secondary = secondaryCandidate()

        assertTrue(request.requestsVariantBuild)
        assertTrue(request.isRequestedMatch(secondary))
        assertTrue(request.matchesRequestedVersionStrict(secondary))
    }

    private fun secondaryCandidate() = DownloadCandidate(
        source = DownloadSource.APK_MIRROR,
        name = "WhatsApp",
        packageName = "com.example.app",
        versionName = "21.36.45",
        versionCode = null,
        url = "https://www.apkmirror.com/apk/whatsapp/whatsapp/whatsapp-21-36-45-secondary-release/",
        fileKind = "web",
        option = CandidateOption.REQUESTED,
        directDownload = false,
        versionStatus = VersionStatus.REQUESTED,
        formatMatches = true
    )

    // ---- stale-result scoping (PendingDownloadResult.belongsTo) ----

    private fun pendingResult(
        requestPackage: String = "com.example.app",
        versionName: String? = "1.2.3"
    ) = PendingDownloadResult(
        uri = "content://x/file.apk",
        fileName = "file.apk",
        packageName = "com.example.app",
        versionName = versionName,
        sourceName = "APKMirror",
        requestPackage = requestPackage,
        callerPackage = "app.morphe.manager"
    )

    @Test
    fun belongsTo_matchesSamePackageAndVersion() {
        val request = testRequest(packageName = "com.example.app", versionName = "1.2.3")
        assertTrue(pendingResult(requestPackage = "com.example.app", versionName = "1.2.3").belongsTo(request))
    }

    @Test
    fun belongsTo_rejectsDifferentPackage() {
        val request = testRequest(packageName = "com.new.app", versionName = "1.2.3")
        assertFalse(pendingResult(requestPackage = "com.example.app").belongsTo(request))
    }

    @Test
    fun belongsTo_rejectsDifferentRequestedVersionOfSamePackage() {
        val request = testRequest(packageName = "com.example.app", versionName = "9.9.9")
        assertFalse(pendingResult(requestPackage = "com.example.app", versionName = "1.2.3").belongsTo(request))
    }

    @Test
    fun belongsTo_acceptsWhenRequestPinsNoVersion() {
        val request = testRequest(packageName = "com.example.app", versionName = null)
        assertTrue(pendingResult(requestPackage = "com.example.app", versionName = "1.2.3").belongsTo(request))
    }

    @Test
    fun belongsTo_rejectsNullRequest() {
        assertFalse(pendingResult().belongsTo(null))
    }

    // ---- live-event scoping (PendingDownloadResult.belongsToCurrentSession) ----

    @Test
    fun belongsToCurrentSession_matchesSamePackageAndEpoch() {
        val request = testRequest(packageName = "com.example.app", versionName = "1.2.3")
        assertTrue(
            pendingResult(requestPackage = "com.example.app", versionName = "1.2.3")
                .belongsToCurrentSession(request, epoch = DownloadJobManager.currentEpoch)
        )
    }

    @Test
    fun belongsToCurrentSession_acceptsDifferentVersionSameSession() {
        // The user may deliberately download a different version (Latest tab)
        // in the same session; it must still be returned, not dropped.
        val request = testRequest(packageName = "com.example.app", versionName = "1.2.3")
        assertTrue(
            pendingResult(requestPackage = "com.example.app", versionName = "9.9.9")
                .belongsToCurrentSession(request, epoch = DownloadJobManager.currentEpoch)
        )
    }

    @Test
    fun belongsToCurrentSession_rejectsDifferentPackage() {
        val request = testRequest(packageName = "com.new.app", versionName = "1.2.3")
        assertFalse(
            pendingResult(requestPackage = "com.example.app")
                .belongsToCurrentSession(request, epoch = DownloadJobManager.currentEpoch)
        )
    }

    @Test
    fun belongsToCurrentSession_rejectsStaleEpoch() {
        val request = testRequest(packageName = "com.example.app", versionName = "1.2.3")
        assertFalse(
            pendingResult(requestPackage = "com.example.app")
                .belongsToCurrentSession(request, epoch = DownloadJobManager.currentEpoch - 1)
        )
    }

    @Test
    fun belongsToCurrentSession_rejectsNullRequest() {
        assertFalse(
            pendingResult().belongsToCurrentSession(null, epoch = DownloadJobManager.currentEpoch)
        )
    }

    // ---- fileKindFromUrl ----

    @Test
    fun fileKindFromUrl_apkmirrorCdnApkIsNotMistakenForBundle() {
        // The CDN file name embeds the host domain; "apkm" must not match "apkmirror".
        val url = "https://eb5e7388c3df147b74dd2379b7cf8323.r2.cloudflarestorage.com/downloadprod/" +
            "wp-content/uploads/2026/06/10/6a1d61d704300/com.accuweather.android.tablet_1.2.7-32_" +
            "minAPI11%28nodpi%29_apkmirror.com.apk?X-Amz-Expires=3600"
        assertEquals("apk", fileKindFromUrl(url))
    }

    @Test
    fun fileKindFromUrl_downloadPhpIsNotMistakenForBundle() {
        // APKMirror's download.php path itself contains "apkmirror".
        val url = "https://www.apkmirror.com/wp-content/themes/APKMirror/download.php?id=14072439&key=2b218113dda68435162f39e99e820c5525641192"
        assertEquals("apk", fileKindFromUrl(url))
    }

    @Test
    fun fileKindFromUrl_realBundleUrl() {
        assertEquals("apkm", fileKindFromUrl("https://cdn.example.com/apps/com.example.app-2.0_apkmirror.com.apkm"))
        assertEquals("apkm", fileKindFromUrl("https://cdn.example.com/download/12345.bundle-apkm.bin"))
    }

    @Test
    fun fileKindFromUrl_splitAndXapk() {
        assertEquals("apks", fileKindFromUrl("https://cdn.example.com/apps/com.example.app_apkmirror.com.apks"))
        assertEquals("xapk", fileKindFromUrl("https://cdn.example.com/apps/com.example.app.xapk"))
    }

    @Test
    fun fileKindFromUrl_filenameQueryParam() {
        assertEquals(
            "xapk",
            fileKindFromUrl("https://example.com/download.php?id=14072439&filename=app.xapk&key=abc")
        )
    }

    @Test
    fun fileKindFromUrl_noExtensionDefaultsToApk() {
        assertEquals("apk", fileKindFromUrl("https://example.com/download?id=1"))
    }

    @Test
    fun fileKindFromUrl_apkPurePathSegmentXapk() {
        // APKPure's download link carries the bundle marker as a path segment,
        // not in the file name (which is the package). Must be XAPK, not APK.
        val url = "https://d.apkpure.com/b/XAPK/com.proxyman.proxymanandroid?versionCode=54"
        assertEquals("xapk", fileKindFromUrl(url))
    }

    @Test
    fun fileKindFromUrl_packageOnlyPathStillDefaultsToApk() {
        // No bundle marker anywhere: stays a plain APK.
        assertEquals("apk", fileKindFromUrl("https://d.apkpure.com/b/APK/com.example.app?versionCode=5"))
    }

    @Test
    fun sha256HexOf_matchesExpectedDigest() {
        val file = java.io.File.createTempFile("helper-sha", ".apk")
        try {
            file.writeText("hello world")
            // SHA-256 of "hello world" (no trailing newline via writeText? writeText adds none).
            assertEquals(
                "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
                sha256HexOf(file)
            )
        } finally {
            file.delete()
        }
    }
}
