package dev.rushi.apkdownloadhelper.download

import dev.rushi.apkdownloadhelper.download.*
import dev.rushi.apkdownloadhelper.ui.CandidateDownloadFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.cancellation.CancellationException

private const val MAX_DOWNLOAD_ATTEMPTS = 3
private const val DOWNLOAD_RETRY_BASE_MS = 2_000L
private const val HTTP_PARTIAL_CONTENT = 206

/** Files below this size are not worth splitting into connections. */
private const val MIN_SEGMENTED_BYTES = 8L * 1024L * 1024L

/** The queue hands out ranges in these units; workers take the next free one. */
private const val SEGMENT_CHUNK_BYTES = 4L * 1024L * 1024L

private const val MAX_SEGMENT_CONNECTIONS = 32

private const val SEGMENT_BUFFER_SIZE = 64 * 1024
private const val PAUSE_POLL_MS = 100L

/**
 * Streaming download engine with partial-range resume, optional parallel
 * segments, automatic retries, and a pause gate.
 *
 * A fresh file is probed with a one-byte Range request. When the server
 * supports byte ranges and the file is large enough, it is split across two or
 * four workers, each writing its own byte range into a `.segN` file and
 * resuming from whatever that file already holds. Anything else (small file,
 * no range support, an existing `.part`, a probe failure) goes through the
 * original single-stream path.
 *
 * Transient failures (network I/O, HTTP 408/429/5xx) retry up to
 * [MAX_DOWNLOAD_ATTEMPTS] times with exponential backoff, reporting each retry
 * through [onRetry]. A paused download keeps its sockets open and stops reading
 * until [resume] is called.
 */
internal class ApkDownloader(
    private val client: OkHttpClient,
    private val apkPureClient: OkHttpClient,
    private val onRetry: (attempt: Int, delayMs: Long) -> Unit = { _, _ -> }
) {
    private val activeCalls = mutableSetOf<Call>()

    @Volatile
    private var paused = false

    @Volatile
    private var cancelled = false

    private val pauseMonitor = Object()

    /** Resets the cancel/pause flags before a fresh job starts. */
    fun prepare() {
        cancelled = false
        paused = false
    }

    /** Stops the read loops after the current buffer; the sockets stay open. */
    fun pause() {
        paused = true
    }

    /** Lets the read loops continue from where they stopped. */
    fun resume() {
        paused = false
        synchronized(pauseMonitor) { pauseMonitor.notifyAll() }
    }

    fun cancelCurrent() {
        cancelled = true
        paused = false
        synchronized(pauseMonitor) { pauseMonitor.notifyAll() }
        cancelActiveCalls()
    }

    suspend fun downloadToFile(
        download: CandidateDownloadFile,
        target: File,
        maxConnections: Int = 4,
        onProgress: (copied: Long, total: Long) -> Unit,
        onConnections: (Int) -> Unit = {}
    ) {
        val url = download.url.normalizedHttpUrlOrNull()
            ?: error("Source returned an invalid download URL.".withManualModeHint())
        // The in-app browser keeps its session in CookieManager rather than in
        // this HTTP client, so import it before the first request. A cleared
        // challenge is only valid for the cookies that solved it.
        url.toHttpUrlOrNull()?.let { SessionCookies.importCookieHeader(it, download.cookieHeader) }
        download.referer?.normalizedHttpUrlOrNull()?.toHttpUrlOrNull()?.let {
            SessionCookies.importCookieHeader(it, download.cookieHeader)
        }

        // Resume a partial single-stream file with the existing path. Mixing a
        // byte offset with a segment layout would corrupt the output.
        if (target.exists() && target.length() > 0L) {
            onConnections(1)
            downloadSingleStream(download, target, url, onProgress)
            return
        }

        val segmentedClient = segmentedDownloadClientFor(url)
        val plan = try {
            probeSegments(download, url, maxConnections, segmentedClient)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            null
        }
        if (plan != null && plan.total >= MIN_SEGMENTED_BYTES) {
            onConnections(plan.connections)
            val complete = try {
                downloadSegmented(download, target, plan, segmentedClient, onProgress)
            } catch (error: CancellationException) {
                throw error
            }
            if (complete) return
            if (cancelled) throw CancellationException("Canceled")
            target.delete()
        }

        onConnections(1)
        target.delete()
        downloadSingleStream(download, target, url, onProgress)
    }

    private data class SegmentPlan(
        val url: String,
        val total: Long,
        val connections: Int,
        val etag: String?,
        val lastModified: String?
    )

    private class HttpStatusException(val code: Int) : IOException("HTTP $code")

    /**
     * Asks for one byte with a Range header. A `206` with a total tells us the
     * file can be split; a `200` means the server ignored the range and the
     * single-stream path should be used.
     */
    private fun probeSegments(
        download: CandidateDownloadFile,
        url: String,
        maxConnections: Int,
        client: OkHttpClient
    ): SegmentPlan? {
        val builder = Request.Builder()
            .url(url)
            .header("Range", "bytes=0-0")
            .header("Accept-Encoding", "identity")
        builder.withCapturedBrowserHeaders(download)

        val call = client.newCall(builder.build())
        synchronized(activeCalls) { activeCalls.add(call) }
        try {
            call.execute().use { response ->
                if (cancelled) throw CancellationException("Canceled")
                if (response.code != HTTP_PARTIAL_CONTENT) return null

                val contentType = response.header("Content-Type")
                check(contentType?.contains("text/html", ignoreCase = true) != true) {
                    "Source did not return a valid APK/APKS/XAPK ($contentType)."
                }

                val encoding = response.header("Content-Encoding")
                if (encoding != null && !encoding.equals("identity", ignoreCase = true)) {
                    return null
                }
                val contentRange = response.header("Content-Range")
                val total = contentRangeTotal(contentRange) ?: return null
                if (!contentRangeStartsAt(contentRange, 0L)) return null
                val connections = segmentedConnectionCount(total, maxConnections)
                if (connections < 2) return null

                return SegmentPlan(
                    url = response.request.url.toString(),
                    total = total,
                    connections = connections,
                    // A weak ETag is not a valid If-Range validator; prefer a
                    // strong one and otherwise fall back to Last-Modified.
                    etag = response.header("ETag")
                        ?.takeUnless { it.trim().startsWith("W/", ignoreCase = true) },
                    lastModified = response.header("Last-Modified")
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            return null
        } finally {
            synchronized(activeCalls) { activeCalls.remove(call) }
        }
    }

    /**
     * Downloads [plan] across its workers and assembles the target file.
     *
     * Returns false when the segmented attempt could not finish, so the caller
     * can fall back to a single stream. Cancellation is rethrown.
     */
    private suspend fun downloadSegmented(
        download: CandidateDownloadFile,
        target: File,
        plan: SegmentPlan,
        client: OkHttpClient,
        onProgress: (copied: Long, total: Long) -> Unit
    ): Boolean = coroutineScope {
        val infoFile = File(target.parentFile, "${target.name}.seginfo")
        val chunkCount = segmentChunkCount(plan.total)
        val chunkFiles = (0 until chunkCount).map { chunkFile(target, it) }
        val validator = plan.etag ?: plan.lastModified

        // The first line guards against resuming a different file. The second
        // fixes the 4 MiB unit, so an old equal-range layout is discarded, and
        // the validator catches a source that replaced the APK at the same size.
        val info = buildString {
            append(plan.total).append('\n')
            append(SEGMENT_CHUNK_BYTES).append('\n')
            append(validator.orEmpty())
        }
        val savedInfo = if (infoFile.exists()) infoFile.readLines() else emptyList()
        val savedLayoutMatches = savedInfo.size >= 2 &&
            savedInfo[0].toLongOrNull() == plan.total &&
            savedInfo[1].toLongOrNull() == SEGMENT_CHUNK_BYTES
        val savedValidator = savedInfo.getOrNull(2)?.takeIf { it.isNotEmpty() }
        val validatorChanged = savedLayoutMatches && savedValidator != validator
        if (!savedLayoutMatches || validatorChanged) {
            allSegmentFiles(target).forEach { it.delete() }
            infoFile.writeText(info)
        }
        allSegmentFiles(target)
            .filterNot { file -> chunkFiles.any { it.name == file.name } }
            .forEach { it.delete() }

        // A chunk file is trusted only up to its 4 MiB slot. Trim leftovers
        // from another layout before counting resumed bytes.
        var completedBytes = 0L
        chunkFiles.forEachIndexed { index, file ->
            val expectedSize = segmentChunkSize(plan.total, index)
            if (file.exists() && file.length() > expectedSize) {
                RandomAccessFile(file, "rw").use { it.setLength(expectedSize) }
            }
            completedBytes += file.length().coerceAtLeast(0L).coerceAtMost(expectedSize)
        }

        val completed = AtomicLong(completedBytes)
        onProgress(completed.get(), plan.total)
        val nextChunk = AtomicLong(0L)
        val workers = (0 until plan.connections).map {
            async(Dispatchers.IO) {
                while (true) {
                    val index = nextChunk.getAndIncrement()
                    if (index >= chunkCount) return@async
                    val chunkIndex = index.toInt()
                    val chunk = chunkFiles[chunkIndex]
                    val expectedSize = segmentChunkSize(plan.total, chunkIndex)
                    if (chunk.length() >= expectedSize) continue
                    downloadChunk(
                        download = download,
                        plan = plan,
                        client = client,
                        chunkIndex = chunkIndex,
                        chunk = chunk,
                        completed = completed,
                        onProgress = onProgress
                    )
                }
            }
        }

        val failure = runCatching { workers.awaitAll() }.exceptionOrNull()
        if (failure != null) {
            cancelActiveCalls()
            allSegmentFiles(target).forEach { it.delete() }
            infoFile.delete()
            if (cancelled || failure is CancellationException) {
                throw CancellationException("Canceled")
            }
            return@coroutineScope false
        }
        val allChunksComplete = chunkFiles.withIndex().all { (index, file) ->
            file.length() == segmentChunkSize(plan.total, index)
        }
        if (!allChunksComplete) {
            allSegmentFiles(target).forEach { it.delete() }
            infoFile.delete()
            return@coroutineScope false
        }

        // Validate the container before assembling, so an HTML page or an error
        // body is rejected before it reaches the validator.
        val header = ByteArray(4)
        val headerSize = chunkFiles.firstOrNull()?.inputStream()?.use { it.read(header) } ?: 0
        val isZip = headerSize >= 2 &&
            header[0] == 'P'.code.toByte() &&
            header[1] == 'K'.code.toByte()
        if (!isZip) {
            allSegmentFiles(target).forEach { it.delete() }
            infoFile.delete()
            error("Source did not return a valid APK/APKS/XAPK.")
        }

        FileOutputStream(target).use { output ->
            chunkFiles.forEach { chunk ->
                chunk.inputStream().use { input -> input.copyTo(output) }
            }
        }
        if (target.length() != plan.total) {
            target.delete()
            allSegmentFiles(target).forEach { it.delete() }
            infoFile.delete()
            return@coroutineScope false
        }

        allSegmentFiles(target).forEach { it.delete() }
        infoFile.delete()
        true
    }

    private fun downloadChunk(
        download: CandidateDownloadFile,
        plan: SegmentPlan,
        client: OkHttpClient,
        chunkIndex: Int,
        chunk: File,
        completed: AtomicLong,
        onProgress: (copied: Long, total: Long) -> Unit
    ) {
        val start = chunkIndex * SEGMENT_CHUNK_BYTES
        val end = minOf(start + SEGMENT_CHUNK_BYTES, plan.total) - 1
        val expectedSize = end - start + 1
        var position = start + chunk.length()
        var attempt = 0

        while (position <= end) {
            awaitResume()
            if (cancelled) throw CancellationException("Canceled")

            val from = position
            val builder = Request.Builder()
                .url(plan.url)
                .header("Range", "bytes=$from-$end")
                .header("Accept-Encoding", "identity")
            builder.withCapturedBrowserHeaders(download)
            // Only a resumed range needs the guard; a fresh chunk must not
            // risk a server rejecting a weak Last-Modified validator.
            if (from > start) {
                val validator = plan.etag ?: plan.lastModified
                if (validator != null) builder.header("If-Range", validator)
            }

            val call = client.newCall(builder.build())
            synchronized(activeCalls) { activeCalls.add(call) }
            var written = 0L
            try {
                call.execute().use { response ->
                    if (cancelled) throw CancellationException("Canceled")
                    if (response.code != HTTP_PARTIAL_CONTENT) {
                        throw HttpStatusException(response.code)
                    }
                    val contentRange = response.header("Content-Range")
                    if (!contentRangeMatches(contentRange, from, end, plan.total)) {
                        throw IllegalStateException("Unexpected Content-Range")
                    }
                    val encoding = response.header("Content-Encoding")
                    if (encoding != null && !encoding.equals("identity", ignoreCase = true)) {
                        throw IllegalStateException("Unexpected Content-Encoding $encoding")
                    }

                    RandomAccessFile(chunk, "rw").use { output ->
                        output.seek(from - start)
                        response.body.byteStream().use { input ->
                            val buffer = ByteArray(SEGMENT_BUFFER_SIZE)
                            while (true) {
                                awaitResume()
                                if (cancelled) throw CancellationException("Canceled")
                                val read = input.read(buffer)
                                if (read < 0) break
                                val remaining = end - position + 1
                                if (remaining <= 0L) break
                                val allowed = minOf(read.toLong(), remaining).toInt()
                                output.write(buffer, 0, allowed)
                                position += allowed
                                written += allowed
                                completed.addAndGet(allowed.toLong())
                                onProgress(completed.get(), plan.total)
                            }
                        }
                    }
                }
                if (position <= end) {
                    throw IOException("Short chunk ${position - start}/$expectedSize")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                completed.addAndGet(-written)
                if (cancelled) throw CancellationException("Canceled")
                if (attempt >= MAX_DOWNLOAD_ATTEMPTS - 1 || !isTransientDownloadError(error)) {
                    throw error
                }
                attempt++
                val delayMs = DOWNLOAD_RETRY_BASE_MS * (1L shl (attempt - 1))
                onRetry(attempt, delayMs)
                sleepBeforeRetry(delayMs)
                position = start + chunk.length()
            } finally {
                synchronized(activeCalls) { activeCalls.remove(call) }
            }
        }
    }

    private fun downloadSingleStream(
        download: CandidateDownloadFile,
        target: File,
        url: String,
        onProgress: (copied: Long, total: Long) -> Unit
    ) {
        var attempt = 0
        while (true) {
            try {
                performSingleStreamAttempt(download, target, url, onProgress)
                return
            } catch (error: Throwable) {
                if (cancelled) throw CancellationException("Canceled")
                if (error is CancellationException || error.message == "Canceled") throw error
                if (attempt >= MAX_DOWNLOAD_ATTEMPTS - 1 || !isTransientDownloadError(error)) throw error
                attempt++
                val delayMs = DOWNLOAD_RETRY_BASE_MS * (1L shl (attempt - 1))
                onRetry(attempt, delayMs)
                sleepBeforeRetry(delayMs)
            }
        }
    }

    private fun performSingleStreamAttempt(
        download: CandidateDownloadFile,
        target: File,
        url: String,
        onProgress: (copied: Long, total: Long) -> Unit
    ) {
        val partial = if (target.exists()) target.length() else 0L
        val builder = Request.Builder().url(url)
        builder.withCapturedBrowserHeaders(download)
        builder.header("Accept-Encoding", "identity")
        if (partial > 0L) builder.header("Range", "bytes=$partial-")

        val call = downloadClientFor(url).newCall(builder.build())
        synchronized(activeCalls) { activeCalls.add(call) }
        try {
            call.execute().use { response ->
                check(response.isSuccessful) { "HTTP ${response.code}" }
                val resumed = response.code == HTTP_PARTIAL_CONTENT && partial > 0L
                if (resumed && !contentRangeStartsAt(response.header("Content-Range"), partial)) {
                    target.delete()
                    throw IOException("Unexpected Content-Range")
                }
                val total: Long = if (resumed) {
                    partial + response.body.contentLength().coerceAtLeast(0L)
                } else {
                    download.size ?: response.body.contentLength().coerceAtLeast(0L)
                }
                val contentType = response.body.contentType()?.toString()
                response.body.byteStream().use { input ->
                    val source = if (resumed) {
                        input
                    } else {
                        validateApkLikeStream(input, contentType)
                    }
                    FileOutputStream(target, resumed).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var copied = if (resumed) partial else 0L
                        while (true) {
                            awaitResume()
                            if (cancelled) throw CancellationException("Canceled")
                            val read = source.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            onProgress(copied, total)
                        }
                    }
                }
            }
        } finally {
            synchronized(activeCalls) { activeCalls.remove(call) }
        }
    }

    private fun awaitResume() {
        while (paused && !cancelled) {
            try {
                Thread.sleep(PAUSE_POLL_MS)
            } catch (error: InterruptedException) {
                Thread.currentThread().interrupt()
                throw CancellationException("Canceled")
            }
        }
        if (cancelled) throw CancellationException("Canceled")
    }

    private fun sleepBeforeRetry(delayMs: Long) {
        try {
            Thread.sleep(delayMs)
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            throw CancellationException("Canceled")
        }
    }

    private fun cancelActiveCalls() {
        val calls = synchronized(activeCalls) { activeCalls.toList() }
        calls.forEach { call ->
            runCatching { call.cancel() }
        }
    }

    private fun chunkFile(target: File, index: Int): File =
        File(target.parentFile, "${target.name}.seg$index")

    /** Every chunk file for [target], including leftovers from another layout. */
    private fun allSegmentFiles(target: File): List<File> =
        target.parentFile?.listFiles()
            ?.filter { file ->
                file.name.startsWith("${target.name}.seg") &&
                    file.name.removePrefix("${target.name}.seg").toIntOrNull() != null
            }
            .orEmpty()

    private fun contentRangeTotal(value: String?): Long? {
        val match = CONTENT_RANGE.matchEntire(value?.trim().orEmpty()) ?: return null
        return match.groupValues[3].toLongOrNull()
    }

    private fun contentRangeStartsAt(value: String?, expectedStart: Long): Boolean {
        val match = CONTENT_RANGE.matchEntire(value?.trim().orEmpty()) ?: return false
        return match.groupValues[1].toLongOrNull() == expectedStart
    }

    private fun contentRangeMatches(
        value: String?,
        expectedStart: Long,
        expectedEnd: Long,
        expectedTotal: Long
    ): Boolean {
        val match = CONTENT_RANGE.matchEntire(value?.trim().orEmpty()) ?: return false
        return match.groupValues[1].toLongOrNull() == expectedStart &&
            match.groupValues[2].toLongOrNull() == expectedEnd &&
            match.groupValues[3].toLongOrNull() == expectedTotal
    }

    private fun Request.Builder.withCapturedBrowserHeaders(download: CandidateDownloadFile) {
        download.referer?.let { header("Referer", it) }
        download.cookieHeader?.let { header("Cookie", it) }
        download.userAgent?.let { header("User-Agent", it) }
    }

    private fun downloadClientFor(url: String): OkHttpClient =
        if (url.contains("apkpure", ignoreCase = true)) apkPureClient else client

    private fun segmentedDownloadClientFor(url: String): OkHttpClient =
        downloadClientFor(url).newBuilder()
            // HTTP/2 multiplexes every range over one connection, which
            // defeats the point of a parallel segmented download.
            .protocols(listOf(Protocol.HTTP_1_1))
            .build()

    private fun isTransientDownloadError(error: Throwable): Boolean {
        if (error is HttpStatusException) {
            return error.code == 403 || error.code == 408 ||
                error.code == 429 || error.code in 500..599
        }
        if (error is IOException) return true
        val message = error.message.orEmpty()
        return Regex("""HTTP (403|408|429|5\d\d)\b""").containsMatchIn(message)
    }

    private companion object {
        val CONTENT_RANGE = Regex("""bytes\s+(\d+)-(\d+)/(\d+)""")
    }
}

/**
 * Worker count for a segmented download, capped by the configured maximum,
 * the file size, and one 4 MiB chunk per worker.
 */
internal fun segmentedConnectionCount(total: Long, configuredMax: Int): Int {
    if (total < MIN_SEGMENTED_BYTES) return 0
    val max = configuredMax.coerceIn(2, MAX_SEGMENT_CONNECTIONS)
    val chunks = segmentChunkCount(total)
    return minOf(max, chunks).coerceAtLeast(2)
}

private fun segmentChunkCount(total: Long): Int =
    ((total + SEGMENT_CHUNK_BYTES - 1) / SEGMENT_CHUNK_BYTES)
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt()

private fun segmentChunkSize(total: Long, index: Int): Long =
    minOf(SEGMENT_CHUNK_BYTES, total - index.toLong() * SEGMENT_CHUNK_BYTES)
        .coerceAtLeast(0L)

