package dev.rushi.apkdownloadhelper

import dev.rushi.apkdownloadhelper.sources.ApkPureHistoryResponse
import dev.rushi.apkdownloadhelper.sources.SourcePrefetch
import dev.rushi.apkdownloadhelper.sources.SourceTextFetcher
import dev.rushi.apkdownloadhelper.sources.UptodownApp
import dev.rushi.apkdownloadhelper.sources.UptodownDownload
import dev.rushi.apkdownloadhelper.sources.UptodownPrepared
import dev.rushi.apkdownloadhelper.sources.UptodownRelease
import dev.rushi.apkdownloadhelper.sources.apkMirrorSearchUrl
import dev.rushi.apkdownloadhelper.ui.HelperRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SourcePrefetchTest {

    @Test
    fun `completed prefetches are handed to the parser once`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val request = request()
        val history = ApkPureHistoryResponse()
        val prepared = prepared()
        val prefetch = SourcePrefetch(
            fetcher = ImmediateSourceFetcher("search-page"),
            apkPureApi = FakeApkPureApi(historyResponse = history),
            uptodownApi = FakeUptodownApi(prepared),
            scope = scope
        )

        prefetch.start(request)

        val searchUrl = apkMirrorSearchUrl(request.packageName)
        assertEquals("search-page", prefetch.takeText(searchUrl))
        assertNull(prefetch.takeText(searchUrl))
        assertEquals(history, prefetch.takeApkPureHistory(request))
        assertNull(prefetch.takeApkPureHistory(request))
        assertEquals(prepared, prefetch.takeUptodown(request))
        assertNull(prefetch.takeUptodown(request))

        prefetch.cancel()
        scope.cancel()
    }

    @Test
    fun `an in-flight prefetch is cancelled instead of being handed over`() = runBlocking {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val fetcher = object : SourceTextFetcher {
            override fun fetchText(url: String, referer: String?): String {
                started.countDown()
                release.await(2, TimeUnit.SECONDS)
                finished.countDown()
                return "late search page"
            }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val request = request()
        val prefetch = SourcePrefetch(
            fetcher = fetcher,
            apkPureApi = FakeApkPureApi(),
            uptodownApi = null,
            scope = scope
        )

        try {
            prefetch.start(request)
            assertTrue(started.await(2, TimeUnit.SECONDS))

            val searchUrl = apkMirrorSearchUrl(request.packageName)
            assertNull(prefetch.takeText(searchUrl))
            prefetch.cancel()
            release.countDown()
            assertTrue(finished.await(2, TimeUnit.SECONDS))
            assertNull(prefetch.takeText(searchUrl))
        } finally {
            release.countDown()
            prefetch.cancel()
            scope.cancel()
        }
    }

    private fun request(): HelperRequest = HelperRequest(
        callerPackage = "dev.rushi.test",
        packageName = "com.example.app",
        appName = "Example",
        versionName = "1.2.3",
        versionCode = 123L,
        versionCodes = setOf(123L),
        compatibleVersionNames = emptySet(),
        compatibleVersionCodes = emptySet(),
        supportedAbis = listOf("arm64-v8a"),
        requestedFileType = "apk",
        allowSplitArchive = false,
        stockInstallRequired = false,
        fallbackWebUrl = "",
        sourceHintUrls = emptyList()
    )

    private fun prepared(): UptodownPrepared = UptodownPrepared(
        app = UptodownApp(
            appId = "app-id",
            slug = "example",
            name = "Example",
            icon = "",
            packageName = "com.example.app",
            lastVersion = "1.2.3"
        ),
        release = UptodownRelease(
            version = "1.2.3",
            fileId = 1L,
            kindFile = "apk"
        ),
        download = UptodownDownload(
            url = "https://example.com/example.apk",
            sha256 = "00"
        )
    )

    private class ImmediateSourceFetcher(private val page: String) : SourceTextFetcher {
        override fun fetchText(url: String, referer: String?): String = page
    }
}
