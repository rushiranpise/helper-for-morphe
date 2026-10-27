package dev.rushi.apkdownloadhelper

import dev.rushi.apkdownloadhelper.VirusTotalScanner.BundleApkResult
import dev.rushi.apkdownloadhelper.VirusTotalScanner.ScanResult
import dev.rushi.apkdownloadhelper.VirusTotalScanner.isEntirelyClean
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanAutoProceedTest {

    private fun clean(vararg inner: BundleApkResult) = ScanResult.Clean(
        sha256 = "abc",
        fileName = "app.apk",
        typeDescription = "Android",
        sizeBytes = 1024,
        timesSubmitted = 1,
        firstSubmissionDate = null,
        lastAnalysisDate = null,
        reputation = 0,
        totalEngines = 70,
        votesHarmless = 70,
        votesMalicious = 0,
        apkResults = inner.toList()
    )

    private fun inner(name: String, detections: Int = 0, failed: Boolean = false) =
        BundleApkResult(
            name = name,
            totalEngines = 70,
            detections = detections,
            sha256 = "abc",
            failed = failed
        )

    @Test
    fun `a clean file may be handed off without asking`() {
        assertTrue(clean().isEntirelyClean())
    }

    @Test
    fun `a clean bundle whose inner APKs all came back clean may be handed off`() {
        assertTrue(clean(inner("base.apk"), inner("split_config.arm64_v8a.apk")).isEntirelyClean())
    }

    @Test
    fun `a bundle with an inner APK that failed still needs the user`() {
        // The overall verdict can be clean while one split was never analysed; that is
        // not "all clean", and deciding alone could pass an unexamined file to Morphe.
        assertFalse(clean(inner("base.apk"), inner("split.apk", failed = true)).isEntirelyClean())
    }

    @Test
    fun `a bundle with a flagged inner APK still needs the user`() {
        assertFalse(clean(inner("base.apk"), inner("split.apk", detections = 2)).isEntirelyClean())
    }

    @Test
    fun `a malicious verdict never proceeds on its own`() {
        val malicious = ScanResult.Malicious(
            sha256 = "abc",
            fileName = "app.apk",
            typeDescription = "Android",
            sizeBytes = 1024,
            timesSubmitted = 1,
            firstSubmissionDate = null,
            lastAnalysisDate = null,
            reputation = -5,
            detections = 3,
            suspicious = 0,
            totalEngines = 70,
            engines = emptyList(),
            suggestedThreatLabel = null,
            sandboxMalwareNames = emptyList()
        )

        assertFalse(malicious.isEntirelyClean())
    }

    @Test
    fun `a scan error never proceeds on its own`() {
        assertFalse(ScanResult.Error("network unreachable").isEntirelyClean())
    }
}
