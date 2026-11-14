package dev.rushi.apkdownloadhelper

import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApkDownloaderTest {

    private fun mib(value: Long): Long = value * 1024L * 1024L

    @Test
    fun segmentedConnectionCount_skipsSmallFiles() {
        assertEquals(0, segmentedConnectionCount(mib(7), 32))
        assertEquals(0, segmentedConnectionCount(mib(1), 2))
    }

    @Test
    fun segmentedConnectionCount_isCappedByChunkCount() {
        assertEquals(2, segmentedConnectionCount(mib(8), 32))
        assertEquals(4, segmentedConnectionCount(mib(16), 32))
        assertEquals(8, segmentedConnectionCount(mib(32), 32))
    }

    @Test
    fun segmentedConnectionCount_respectsTheConfiguredMaximum() {
        assertEquals(2, segmentedConnectionCount(mib(128), 2))
        assertEquals(4, segmentedConnectionCount(mib(128), 4))
        assertEquals(8, segmentedConnectionCount(mib(128), 8))
        assertEquals(16, segmentedConnectionCount(mib(128), 16))
        assertEquals(32, segmentedConnectionCount(mib(512), 32))
    }

    @Test
    fun segmentedConnectionCount_clampsInvalidMaximums() {
        assertEquals(2, segmentedConnectionCount(mib(128), 1))
        assertEquals(32, segmentedConnectionCount(mib(1024), 64))
    }

    @Test
    fun segmentedDownload_usesChunkRangesAndAssemblesTheFile() = runBlocking {
        val total = mib(8) + 123L
        val payload = ByteArray(total.toInt()) { index -> (index % 251).toByte() }
        payload[0] = 'P'.code.toByte()
        payload[1] = 'K'.code.toByte()
        val chunkRequests = AtomicInteger(0)

        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val range = request.getHeader("Range")
                val match = Regex("""bytes=(\d+)-(\d+)""")
                    .matchEntire(range.orEmpty())
                    ?: return MockResponse().setResponseCode(400)
                val start = match.groupValues[1].toLong()
                val requestedEnd = match.groupValues[2].toLong()
                val end = minOf(requestedEnd, total - 1)
                if (range != "bytes=0-0") chunkRequests.incrementAndGet()
                val length = (end - start + 1).toInt()
                return MockResponse()
                    .setResponseCode(206)
                    .setHeader("Content-Range", "bytes $start-$end/$total")
                    .setHeader("Content-Type", "application/vnd.android.package-archive")
                    .setBody(Buffer().write(payload, start.toInt(), length))
            }
        }
        server.start()

        val directory = File(
            System.getProperty("java.io.tmpdir"),
            "apk-downloader-test-${System.nanoTime()}"
        ).apply { mkdirs() }
        val target = File(directory, "app.apk")
        var copied = 0L
        var connections = 0
        try {
            ApkDownloader(OkHttpClient(), OkHttpClient()).downloadToFile(
                download = CandidateDownloadFile(
                    url = server.url("/app.apk").toString(),
                    fileName = "app.apk"
                ),
                target = target,
                maxConnections = 4,
                onProgress = { done, _ -> copied = done },
                onConnections = { connections = it }
            )
        } finally {
            server.shutdown()
        }

        assertEquals(3, connections)
        assertEquals(total, copied)
        assertTrue("expected multiple chunk requests", chunkRequests.get() >= 3)
        assertArrayEquals(payload, target.readBytes())
        assertTrue(directory.deleteRecursively())
    }
}
