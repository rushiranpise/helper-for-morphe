package dev.rushi.apkdownloadhelper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadJobManagerTest {

    private val candidate = DownloadCandidate(
        source = DownloadSource.APK_MIRROR,
        name = "Example App",
        packageName = "com.example.app",
        versionName = "1.0.0",
        versionCode = null,
        url = "https://example.invalid/app.apk",
        fileKind = "apk",
        option = CandidateOption.REQUESTED,
        directDownload = true,
        versionStatus = VersionStatus.REQUESTED,
        formatMatches = true
    )

    private fun job() = DownloadJobManager.DownloadJob(
        request = testRequest(versionName = "1.0.0"),
        candidate = candidate,
        settings = HelperSettings()
    )

    private fun result() = PendingDownloadResult(
        uri = "content://x/file.apk",
        fileName = "file.apk",
        packageName = "com.example.app",
        versionName = "1.0.0",
        sourceName = "APKMirror",
        requestPackage = "com.example.app",
        callerPackage = "app.morphe.manager"
    )

    @Test
    fun `a replaced download's late cancellation keeps the new job`() {
        DownloadJobManager.start(job())
        val replaced = checkNotNull(DownloadJobManager.activeJob)
        DownloadJobManager.start(job())
        val current = checkNotNull(DownloadJobManager.activeJob)

        // The replaced job reports its cancellation after the new job has started.
        DownloadJobManager.emit(DownloadJobManager.Event.Cancelled(candidate, replaced.epoch))

        assertEquals(current, DownloadJobManager.activeJob)
    }

    @Test
    fun `a replaced download finishing late keeps the new job`() {
        DownloadJobManager.start(job())
        val replaced = checkNotNull(DownloadJobManager.activeJob)
        DownloadJobManager.start(job())
        val current = checkNotNull(DownloadJobManager.activeJob)

        DownloadJobManager.emit(DownloadJobManager.Event.Completed(result(), replaced.epoch))

        assertEquals(current, DownloadJobManager.activeJob)
    }

    @Test
    fun `the current job's completion clears it`() {
        DownloadJobManager.start(job())
        val current = checkNotNull(DownloadJobManager.activeJob)

        DownloadJobManager.emit(DownloadJobManager.Event.Completed(result(), current.epoch))

        assertNull(DownloadJobManager.activeJob)
    }

    @Test
    fun `a retired download is no longer live, and the next one is`() {
        DownloadJobManager.start(job())
        val retired = checkNotNull(DownloadJobManager.activeJob)

        DownloadJobManager.retireCurrent()

        assertFalse(DownloadJobManager.isLive(retired.epoch))
        DownloadJobManager.start(job())
        assertTrue(DownloadJobManager.isLive(checkNotNull(DownloadJobManager.activeJob).epoch))
        assertFalse(DownloadJobManager.isLive(retired.epoch))
    }

    @Test
    fun `a retired download's completion is not handed to the request that replaced it`() {
        DownloadJobManager.start(job())
        val retired = checkNotNull(DownloadJobManager.activeJob)
        DownloadJobManager.retireCurrent()

        // Completes before the replacing run has started a download of its own.
        assertFalse(result().belongsToCurrentSession(testRequest(versionName = "1.0.0"), retired.epoch))

        DownloadJobManager.start(job())
    }

    @Test
    fun `a result or prompt is only published while its download is live`() {
        DownloadJobManager.start(job())
        val epoch = checkNotNull(DownloadJobManager.activeJob).epoch
        var published = 0

        assertTrue(DownloadJobManager.ifLive(epoch) { published++ })
        DownloadJobManager.retireCurrent()
        assertFalse(DownloadJobManager.ifLive(epoch) { published++ })

        assertEquals(1, published)
        DownloadJobManager.start(job())
    }

    @Test
    fun `the current job's cancellation clears it`() {
        DownloadJobManager.start(job())
        val current = checkNotNull(DownloadJobManager.activeJob)

        DownloadJobManager.emit(DownloadJobManager.Event.Cancelled(candidate, current.epoch))

        assertNull(DownloadJobManager.activeJob)
    }
}
