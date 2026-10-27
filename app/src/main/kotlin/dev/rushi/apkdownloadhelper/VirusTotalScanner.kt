package dev.rushi.apkdownloadhelper

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

/**
 * VirusTotal API v3 client for scanning downloaded APK files.
 *
 * Free-tier limits: 4 lookups/min, 500/day, 15.5K/month.
 * Upload size: up to 32 MB via POST /files; larger files (up to 650 MB)
 * go through the single-use /files/upload_url endpoint.
 *
 * Flow: compute the file's SHA-256 locally and ask VirusTotal for the
 * existing report first (GET /files/{sha256}). If the file was already
 * analysed — which is the common case for popular APKs — that report is
 * returned instantly without uploading, so we never hit the "file already
 * submitted" dead end. Only files unknown to VirusTotal are uploaded, then
 * the analysis is polled until it completes.
 */
internal object VirusTotalScanner {

    private const val TAG = "VirusTotalScanner"
    private const val BASE_URL = "https://www.virustotal.com/api/v3"

    /**
     * Minimum gap enforced between every VirusTotal API call. The free tier
     * allows ~4 lookups/min, so this paces report fetches and uploads globally
     * (across bundle APKs and any batch work) instead of relying on fixed
     * sleeps that either overshoot a fast network or trip 429s on a busy one.
     * 16s ≈ 3.75 calls/min, leaving headroom for the polling retry.
     */
    internal const val MIN_CALL_GAP_MS = 16_000L

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    sealed interface ScanResult {
        val sha256: String?
        val fileName: String
        val typeDescription: String?
        val sizeBytes: Long?
        val timesSubmitted: Int?
        val firstSubmissionDate: Long?
        val lastAnalysisDate: Long?
        val reputation: Int?
        /**
         * True when the verdict came from a report VirusTotal already had on
         * file (no upload); false when the file was freshly uploaded and
         * analysed just now. Null verdicts (Error) are never cached.
         */
        val cached: Boolean
        /** Non-null when a XAPK/APKS/APKM bundle was scanned instead of one file. */
        val scannedFiles: Int?
        val bundleName: String?
        /** Per-inner-APK outcomes for bundle scans (empty for single files). */
        val apkResults: List<BundleApkResult>

        data class Clean(
            override val sha256: String?,
            override val fileName: String,
            override val typeDescription: String?,
            override val sizeBytes: Long?,
            override val timesSubmitted: Int?,
            override val firstSubmissionDate: Long?,
            override val lastAnalysisDate: Long?,
            override val reputation: Int?,
            val totalEngines: Int,
            val votesHarmless: Int,
            val votesMalicious: Int,
            override val cached: Boolean = false,
            // Set when a XAPK/APKS/APKM bundle was scanned: how many inner
            // APKs were scanned and the bundle's file name.
            override val scannedFiles: Int? = null,
            override val bundleName: String? = null,
            override val apkResults: List<BundleApkResult> = emptyList()
        ) : ScanResult

        data class Malicious(
            override val sha256: String?,
            override val fileName: String,
            override val typeDescription: String?,
            override val sizeBytes: Long?,
            override val timesSubmitted: Int?,
            override val firstSubmissionDate: Long?,
            override val lastAnalysisDate: Long?,
            override val reputation: Int?,
            val detections: Int,
            val suspicious: Int,
            val totalEngines: Int,
            val engines: List<EngineDetection>,
            val suggestedThreatLabel: String?,
            val sandboxMalwareNames: List<String>,
            override val cached: Boolean = false,
            // Set when a XAPK/APKS/APKM bundle was scanned: how many inner
            // APKs were scanned, how many were flagged, and the bundle name.
            // [fileName] then names the flagged inner APK.
            override val scannedFiles: Int? = null,
            val flaggedFiles: Int? = null,
            override val bundleName: String? = null,
            override val apkResults: List<BundleApkResult> = emptyList()
        ) : ScanResult

        data class Error(val message: String) : ScanResult {
            override val sha256: String? get() = null
            override val fileName: String get() = ""
            override val typeDescription: String? get() = null
            override val sizeBytes: Long? get() = null
            override val timesSubmitted: Int? get() = null
            override val firstSubmissionDate: Long? get() = null
            override val lastAnalysisDate: Long? get() = null
            override val reputation: Int? get() = null
            override val cached: Boolean get() = false
            override val scannedFiles: Int? get() = null
            override val bundleName: String? get() = null
            override val apkResults: List<BundleApkResult> get() = emptyList()
        }
    }

    /** One antivirus engine's verdict: engine name + human-readable result. */
    data class EngineDetection(
        val engine: String,
        val result: String,
        val category: String
    )

    /**
     * True only when the scan found nothing at all: a clean verdict for the file and,
     * for a bundle, no inner APK that was flagged or failed to scan.
     *
     * Deliberately strict. A "clean" bundle can still carry an inner APK that never
     * got analysed, and deciding automatically on the strength of a partial scan is
     * how a flagged split would reach Morphe without anyone looking.
     */
    fun ScanResult.isEntirelyClean(): Boolean = when (this) {
        is ScanResult.Clean -> apkResults.all { it.detections == 0 && !it.failed }
        is ScanResult.Malicious -> false
        is ScanResult.Error -> false
    }

    /** One inner APK's outcome inside a scanned bundle. */
    data class BundleApkResult(
        val name: String,
        val totalEngines: Int,
        val detections: Int,
        val sha256: String?,
        val engines: List<EngineDetection> = emptyList(),
        val failed: Boolean = false,
        /** True when the verdict came from a report VirusTotal already had
         *  on file; false when the inner APK was freshly uploaded. */
        val cached: Boolean = false
    )

    /** Free-tier request quotas reported by /users/{id}/overall_quotas. */
    data class QuotaUsage(
        val hourlyUsed: Int, val hourlyAllowed: Int,
        val dailyUsed: Int, val dailyAllowed: Int,
        val monthlyUsed: Int, val monthlyAllowed: Int
    ) {
        val remainingDaily: Int get() = (dailyAllowed - dailyUsed).coerceAtLeast(0)
        val remainingHourly: Int get() = (hourlyAllowed - hourlyUsed).coerceAtLeast(0)
    }

    /**
     * Free-tier per-minute lookup cap. The overall_quotas endpoint only reports
     * hourly/daily/monthly buckets, so the "per minute" number is tracked
     * client-side in [RateLimiter.callsInLastMinute] against this limit.
     */
    internal const val MINUTE_LOOKUP_LIMIT = 4

    /**
     * Global flat-rate token gate between VirusTotal calls. Every API call
     * (report fetch, upload, poll, quota) records its timestamp here, so calls
     * spaced across separate files — a bundle's inner APKs today, a bulk queue
     * later — never stampede the 4/min limit. [awaitSlot] sleeps until it is
     * this caller's turn, checking cancellation each second so a cancel lands
     * promptly.
     */
    internal class RateLimiter(private val minGapMs: Long = MIN_CALL_GAP_MS) {
        private val lock = Any()
        private var lastCallAtMs = 0L
        // Timestamps of every consuming API call, pruned to the trailing 60s so
        // the per-minute "N of 4" bar reflects the calls this app actually made.
        private val callTimestamps = ArrayDeque<Long>()

        // Set from ANY thread (e.g. a "Skip wait" button in the UI) to cut the
        // current rate-limit wait short. Consumed by the next awaitSlot that
        // observes it; then reset, so a single tap skips exactly one wait.
        @Volatile
        private var skipRequested = false

        /**
         * Persistence hook: invoked after every consuming call is recorded, with
         * the pruned trailing-60s timestamps, so the per-minute count can survive
         * a process restart. Installed by the Activity/Service that owns a Context.
         */
        @Volatile
        var onCallRecorded: ((List<Long>) -> Unit)? = null

        /** Wake any thread currently waiting out the rate-limit gap. */
        fun requestSkip() {
            skipRequested = true
        }

        /** Wait until at least [minGapMs] has passed since the previous call. */
        fun awaitSlot(checkCancelled: () -> Boolean = { false }) {
            while (true) {
                val waitMs: Long
                synchronized(lock) {
                    waitMs = lastCallAtMs + minGapMs - System.currentTimeMillis()
                    if (waitMs > 0) {
                        // Not our turn yet.
                    } else {
                        lastCallAtMs = System.currentTimeMillis()
                        recordCallLocked()
                        // Just claimed our slot: clear any stale skip signal so
                        // it doesn't leak into the next wait.
                        skipRequested = false
                        return
                    }
                }
                // Sleep in 1s slices so cancellation and a skip both land within
                // a second. requestSkip() breaks out early — attempting the call
                // then may 429, which the caller already backoffs off and retries.
                var remaining = waitMs
                while (remaining > 0) {
                    if (checkCancelled()) throw CancellationException("Scan cancelled")
                    if (skipRequested) {
                        skipRequested = false
                        synchronized(lock) {
                            lastCallAtMs = System.currentTimeMillis()
                            recordCallLocked()
                        }
                        return
                    }
                    Thread.sleep(1000)
                    remaining -= 1000
                }
            }
        }

        /**
         * Number of consuming VirusTotal calls made in the current wall-clock
         * minute. Unlike the rolling 60s window used for pacing, this resets to
         * zero at each minute boundary — so the UI's "N of 4" bar reads 0 right
         * after the clock ticks over and climbs as calls are made, instead of
         * hovering at 3-4 forever during a long scan.
         */
        fun callsInCurrentMinute(): Int {
            synchronized(lock) {
                val minuteStart = System.currentTimeMillis() / 60_000L * 60_000L
                return callTimestamps.count { it >= minuteStart }
            }
        }

        /**
         * Milliseconds until the oldest call in the rolling window ages out,
         * i.e. how long until the free tier's 4/60s window is fully clear. Used
         * after a real 429 so we retry only once the limit has actually lifted
         * instead of guessing with a fixed sleep.
         */
        fun millisUntilWindowClears(): Long {
            synchronized(lock) {
                val oldest = callTimestamps.firstOrNull() ?: return 0L
                val clearAt = oldest + 60_000L
                return (clearAt - System.currentTimeMillis()).coerceAtLeast(0L)
            }
        }

        /**
         * Block until the rolling 60s window is clear, sleeping in 1s slices so
         * a cancel or a Skip-wait tap lands promptly. [onWait] fires with the
         * seconds remaining so the UI can render a countdown.
         */
        fun waitForWindowClear(
            checkCancelled: () -> Boolean = { false },
            onWait: ((Long) -> Unit)? = null
        ) {
            while (true) {
                val remaining = millisUntilWindowClears()
                if (remaining <= 0) {
                    // Window is already clear but we were still 429'd — e.g. the
                    // daily/monthly quota is exhausted, which no 60s window can
                    // fix. Yield once instead of returning instantly, so a
                    // persistent 429 can't spin into a tight retry loop.
                    Thread.sleep(1000)
                    return
                }
                onWait?.invoke((remaining / 1000) + 1)
                if (checkCancelled()) throw CancellationException("Scan cancelled")
                if (skipRequested) {
                    skipRequested = false
                    continue
                }
                Thread.sleep(1000)
            }
        }

        /**
         * Number of consuming VirusTotal calls made in the trailing 60 seconds
         * (the rolling window the free tier actually enforces).
         */
        fun callsInLastMinute(): Int {
            synchronized(lock) {
                pruneCallTimestampsLocked()
                return callTimestamps.size
            }
        }

        /**
         * Rehydrate the trailing call log after a process restart. Timestamps
         * older than 60s are dropped, and the pacing clock resumes from the most
         * recent surviving call so the 16s gap also carries across the restart.
         */
        fun restoreCallTimestamps(timestamps: Collection<Long>) {
            synchronized(lock) {
                val now = System.currentTimeMillis()
                val cutoff = now - 60_000L
                timestamps
                    .filter { it in cutoff..now }
                    .sorted()
                    .forEach { callTimestamps.addLast(it) }
                pruneCallTimestampsLocked()
                callTimestamps.lastOrNull()?.let { lastCallAtMs = it }
            }
        }

        private fun recordCallLocked() {
            callTimestamps.addLast(System.currentTimeMillis())
            pruneCallTimestampsLocked()
            onCallRecorded?.invoke(callTimestamps.toList())
        }

        private fun pruneCallTimestampsLocked() {
            val cutoff = System.currentTimeMillis() - 60_000L
            while (callTimestamps.isNotEmpty() && callTimestamps.first() < cutoff) {
                callTimestamps.removeFirst()
            }
        }

        /**
         * Milliseconds until the next slot is free, or 0 if one is available now.
         * Read-only, used to render "waiting Xs for rate limit" in the UI.
         */
        fun millisUntilNextSlot(): Long {
            synchronized(lock) {
                val wait = lastCallAtMs + minGapMs - System.currentTimeMillis()
                return wait.coerceAtLeast(0)
            }
        }
    }

    /**
     * The scanner's shared rate limiter. Making it the single gate means the
     * pacing carries across every scan in a session, so a batch of files is
     * throttled as a whole rather than each file starting fresh.
     */
    internal val rateLimiter = RateLimiter()

    /** Pace the next VirusTotal call and surface any wait as progress. */
    internal fun pace(
        onProgress: ((String) -> Unit)? = null,
        checkCancelled: () -> Boolean = { false }
    ) {
        val waitMs = rateLimiter.millisUntilNextSlot()
        if (waitMs > 0) {
            Log.d(TAG, "Pacing: waiting ${waitMs / 1000}s for rate limit")
            onProgress?.invoke("Waiting ${(waitMs / 1000) + 1}s for rate limit…")
        }
        rateLimiter.awaitSlot(checkCancelled)
    }

    /**
     * Scan a downloaded file. XAPK/APKS/APKM bundles are split into their
     * inner APKs and each one is scanned individually — engines often skip
     * large container files — while plain APKs are scanned directly.
     */
    suspend fun scanDownloadedFile(
        file: File,
        apiKey: String,
        onProgress: ((String) -> Unit)? = null,
        onPercent: ((Int) -> Unit)? = null,
        checkCancelled: () -> Boolean = { false }
    ): ScanResult {
        val lower = file.name.lowercase()
        val isBundle = lower.endsWith(".xapk") || lower.endsWith(".apks") || lower.endsWith(".apkm")
        return if (isBundle) {
            scanBundle(file, apiKey, onProgress, onPercent, checkCancelled)
        } else {
            scanFile(file, apiKey, onProgress, onPercent, checkCancelled)
        }
    }

    /**
     * Extract the inner APKs of a bundle container and scan each one,
     * returning a combined verdict. Falls back to scanning the container
     * itself if it contains no APK entries.
     */
    private suspend fun scanBundle(
        file: File,
        apiKey: String,
        onProgress: ((String) -> Unit)? = null,
        onPercent: ((Int) -> Unit)? = null,
        checkCancelled: () -> Boolean = { false }
    ): ScanResult {
        // Open the bundle ONCE. For large APKS files the central-directory read
        // is the slow part, so a second ZipFile(file) open — as the old code
        // did for extraction — could block visibility with no progress update,
        // which read as "stuck at Extracting". One open, one pass: enumerate,
        // extract, and emit progress per APK without re-reading the archive.
        onProgress?.invoke("Opening ${file.name}…")
        onPercent?.invoke(1)
        val extractStartTotal = System.currentTimeMillis()
        val tempDir = File.createTempFile("vt-bundle", "").apply { delete(); mkdirs() }
        val innerResults = mutableListOf<Pair<ScanResult, String>>()
        // A corrupt or truncated bundle (a failed split download) makes
        // ZipFile throw. Catch it here and return a proper scan error so the
        // UI always receives a definitive result instead of freezing on the
        // last "Extracting…" progress message — the freeze the user hit.
        return try {
            val apkNames = zipEntriesOf(file)
            if (apkNames.isEmpty()) {
                Log.d(TAG, "${file.name} is not a ZIP of APKs — scanning the container itself.")
                return scanFile(file, apiKey, onProgress, onPercent, checkCancelled)
            }
            ZipFile(file).use { zip ->
                val total = apkNames.size
                if (total > 0) {
                    // Warn ahead of time that extraction is about to run, so the UI
                    // never sits on a stale message during the whole extraction pass.
                    onProgress?.invoke("Extracting $total APKs from ${file.name}…")
                    onPercent?.invoke(5)
                    Log.d(TAG, "Starting extraction of $total APKs from ${file.name}")
                    apkNames.forEachIndexed { index, name ->
                        if (checkCancelled()) throw CancellationException("Scan cancelled")
                        val safeName = name.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_")
                        val out = File(tempDir, safeName)
                        val extractStart = System.currentTimeMillis()
                        zip.getInputStream(zip.getEntry(name)).use { input ->
                            out.outputStream().use { output -> input.copyTo(output) }
                        }
                        // Report each finished extraction so a big bundle shows
                        // movement instead of looking hung on a single APK.
                        onProgress?.invoke(
                            "Extracted ${index + 1}/$total (${name})" +
                                " in ${(System.currentTimeMillis() - extractStart) / 1000}s — next: scan…"
                        )
                        Log.d(TAG, "Extracted ${index + 1}/$total ($name) — starting scan")
                        innerResults.add(
                            // Each APK occupies a slice from 5 to 100% weighted by
                            // its index, so the bar marches as the scan advances.
                            scanFile(
                                out,
                                apiKey,
                                onProgress = { status ->
                                    onProgress?.invoke("APK ${index + 1} of $total ($name): $status")
                                },
                                onPercent = { inner ->
                                    val sliceStart = 5 + (index * 95) / total
                                    val sliceEnd = 5 + ((index + 1) * 95) / total
                                    onPercent?.invoke(sliceStart + ((inner * (sliceEnd - sliceStart)) / 100))
                                },
                                checkCancelled = checkCancelled
                            ) to name
                        )
                    }
                }
            }
            onProgress?.invoke("Aggregating results for ${apkNames.size} APKs…")
            onPercent?.invoke(99)
            aggregateBundle(innerResults, file.name)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Bundle scan failed for ${file.name}", e)
            ScanResult.Error("Bundle scan failed: ${e.message ?: "Unknown error"}")
        } finally {
            // Best effort only: throwing here would mask the real scan outcome.
            runCatching { tempDir.deleteRecursively() }
            Log.d(TAG, "Bundle scan of ${file.name} finished in ${(System.currentTimeMillis() - extractStartTotal) / 1000}s")
        }
    }

    /** Returns the inner APK entry names, or empty on a corrupt/unreadable archive. */
    private fun zipEntriesOf(file: File): List<String> = runCatching {
        ZipFile(file).use { zip ->
            zip.entries().asSequence()
                .filter { !it.isDirectory }
                .map { it.name }
                .filter { it.lowercase().endsWith(".apk") }
                .toList()
        }
    }.getOrDefault(emptyList())

    private fun aggregateBundle(
        results: List<Pair<ScanResult, String>>,
        bundleName: String
    ): ScanResult {
        val total = results.size
        val apkResults = results.map { (result, name) ->
            when (result) {
                is ScanResult.Clean -> BundleApkResult(
                    name = name,
                    totalEngines = result.totalEngines,
                    detections = 0,
                    sha256 = result.sha256,
                    cached = result.cached
                )
                is ScanResult.Malicious -> BundleApkResult(
                    name = name,
                    totalEngines = result.totalEngines,
                    detections = result.detections,
                    sha256 = result.sha256,
                    engines = result.engines,
                    cached = result.cached
                )
                is ScanResult.Error -> BundleApkResult(
                    name = name,
                    totalEngines = 0,
                    detections = 0,
                    sha256 = null,
                    failed = true,
                    cached = false
                )
            }
        }
        val malicious = results.filter { it.first is ScanResult.Malicious }
        val errors = results.filter { it.first is ScanResult.Error }
        val clean = results.filter { it.first is ScanResult.Clean }

        if (malicious.isNotEmpty()) {
            val worst = malicious.maxByOrNull { (r, _) -> (r as ScanResult.Malicious).detections }!!
            val flagged = worst.first as ScanResult.Malicious
            return flagged.copy(
                fileName = worst.second,
                sizeBytes = null, // would be the inner APK, confusing next to the bundle
                cached = results.all { it.first.cached },
                scannedFiles = total,
                flaggedFiles = malicious.size,
                bundleName = bundleName,
                apkResults = apkResults
            )
        }
        if (errors.isNotEmpty()) {
            val firstError = (errors.first().first as ScanResult.Error).message
            val message = if (clean.isEmpty()) {
                "All $total APKs failed to scan: $firstError"
            } else {
                "${clean.size} of $total APKs scanned clean; ${errors.size} failed ($firstError)"
            }
            return ScanResult.Error(message)
        }
        val totalEngines = clean.sumOf { (it.first as ScanResult.Clean).totalEngines }
        val firstClean = clean.first().first as ScanResult.Clean
        return firstClean.copy(
            fileName = bundleName,
            sha256 = null,
            typeDescription = null,
            sizeBytes = null,
            timesSubmitted = null,
            firstSubmissionDate = null,
            lastAnalysisDate = null,
            reputation = null,
            totalEngines = totalEngines,
            votesHarmless = 0,
            votesMalicious = 0,
            cached = results.all { it.first.cached },
            scannedFiles = total,
            bundleName = bundleName,
            apkResults = apkResults
        )
    }

    /**
     * Check VirusTotal for [file] and return a [ScanResult] with the detection
     * summary plus whatever file metadata the API returned.
     */
    suspend fun scanFile(
        file: File,
        apiKey: String,
        onProgress: ((String) -> Unit)? = null,
        onPercent: ((Int) -> Unit)? = null,
        checkCancelled: () -> Boolean = { false }
    ): ScanResult {
        if (checkCancelled()) throw CancellationException("Scan cancelled")
        if (!file.exists()) {
            return ScanResult.Error("File not found: ${file.name}")
        }
        if (file.length() > 650L * 1024 * 1024) {
            return ScanResult.Error("File too large for VirusTotal scan (max 650 MB)")
        }

        return try {
            onPercent?.invoke(5)
            val hashStart = System.currentTimeMillis()
            val sha256 = sha256Of(file)
            Log.d(TAG, "SHA-256 of ${file.name} (${file.length()} bytes) in ${System.currentTimeMillis() - hashStart}ms: $sha256")

            // Fast path: the file is already in VirusTotal's database.
            onProgress?.invoke("Checking VirusTotal…")
            onPercent?.invoke(25)
            val fetchStart = System.currentTimeMillis()
            val report = fetchFileReport(sha256, apiKey, onProgress, checkCancelled)
            Log.d(TAG, "fetchFileReport(${file.name}) returned in ${System.currentTimeMillis() - fetchStart}ms")
            report?.let { r ->
                Log.d(TAG, "File already analysed — using existing report.")
                onPercent?.invoke(100)
                return parseFileReport(r, file.name)
            }

            onProgress?.invoke("Uploading ${file.name} to VirusTotal…")
            onPercent?.invoke(45)
            val analysisId = try {
                uploadFile(file, apiKey, onProgress, checkCancelled)
            } catch (e: AlreadySubmittedException) {
                // Analysed between our check and the upload: fetch the report.
                Log.d(TAG, "Upload rejected (already submitted) — fetching report.")
                val report = fetchFileReport(sha256, apiKey, onProgress, checkCancelled)
                    ?: throw Exception("File already submitted but no report was returned")
                onPercent?.invoke(100)
                return parseFileReport(report, file.name)
            }
            Log.d(TAG, "Upload complete, analysis ID: $analysisId")

            onProgress?.invoke("Waiting for analysis…")
            onPercent?.invoke(60)
            val analysis = pollAnalysis(analysisId, apiKey, onProgress, onPercent, checkCancelled)
            onPercent?.invoke(100)
            parseAnalysis(analysis, file.name)
        } catch (e: Exception) {
            Log.e(TAG, "Scan failed", e)
            ScanResult.Error("Scan failed: ${e.message ?: "Unknown error"}")
        }
    }

    /**
     * Fetch the account's request quotas (free tier: 240/hour, 500/day,
     * 15.5K/month). The API key itself works as the user id, and this
     * endpoint does not consume quota. Returns null on failure.
     */
    fun fetchQuotaUsage(apiKey: String): QuotaUsage? {
        if (apiKey.isBlank()) return null
        return try {
            // Quota lookups are read-only and do not consume quota, so they are
            // NOT paced through the limiter — they must never stall a scan.
            val request = Request.Builder()
                .url("$BASE_URL/users/$apiKey/overall_quotas")
                .addHeader("x-apikey", apiKey)
                .get()
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null
            if (!response.isSuccessful) {
                Log.w(TAG, "Quota lookup failed (${response.code})")
                return null
            }
            val quotas = gson.fromJson(body, QuotasResponse::class.java).data ?: return null
            QuotaUsage(
                hourlyUsed = quotas.hourly?.user?.used ?: 0,
                hourlyAllowed = quotas.hourly?.user?.allowed ?: 0,
                dailyUsed = quotas.daily?.user?.used ?: 0,
                dailyAllowed = quotas.daily?.user?.allowed ?: 0,
                monthlyUsed = quotas.monthly?.user?.used ?: 0,
                monthlyAllowed = quotas.monthly?.user?.allowed ?: 0
            )
        } catch (e: Exception) {
            Log.w(TAG, "Quota lookup failed", e)
            null
        }
    }

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
    }

    /** Returns the stored report for [sha256], or null if VirusTotal has never seen it. */
    private fun fetchFileReport(
        sha256: String,
        apiKey: String,
        onProgress: ((String) -> Unit)? = null,
        checkCancelled: () -> Boolean = { false }
    ): FileReportResponse? {
        // Report lookups are NOT pre-paced through the shared limiter. A cached
        // lookup is a cheap GET and pacing each one ahead of time is what made
        // split bundles stall for minutes (every inner APK already analysed).
        // The 429 retry loop below throttles against the server's real 4/60s
        // window, so lookups fly through when the window is clear and only wait
        // when the server actually rejects, never by a flat 16s guess. Uploads
        // and analysis polls keep their own pacing — they are the heavy calls.
        val request = Request.Builder()
            .url("$BASE_URL/files/$sha256")
            .addHeader("x-apikey", apiKey)
            .get()
            .build()
        var response = client.newCall(request).execute()
        if (response.code == 429) {
            // The 4/60s window is genuinely exhausted: wait until it clears
            // (surfacing a live countdown), then retry — automatically and as
            // many times as needed, instead of one blind 20s sleep + give-up.
            Log.d(TAG, "Report lookup rate limited (429), waiting for the limit to clear.")
            while (response.code == 429) {
                rateLimiter.waitForWindowClear(
                    checkCancelled = checkCancelled,
                    onWait = { secs ->
                        onProgress?.invoke("Rate limit hit — retrying in ${secs}s…")
                    }
                )
                response = client.newCall(request).execute()
            }
        }
        var body = response.body?.string() ?: return null
        if (response.code == 404) return null
        if (!response.isSuccessful) {
            throw Exception("Report lookup failed (${response.code}): $body")
        }
        // A 2xx with a non-JSON body (e.g. an HTML block page or a plain-text
        // proxy response) is a transient upstream blip, not a real verdict. Gson
        // would throw "Expected BEGIN_OBJECT but was STRING" and fail the whole
        // scan for a single bad response — so retry a couple of times before
        // giving up on this file.
        for (attempt in 1..3) {
            if (isJsonObject(body)) {
                return gson.fromJson(body, FileReportResponse::class.java)
            }
            Log.w(TAG, "Report lookup returned non-JSON 2xx body on attempt $attempt; retrying")
            if (checkCancelled()) throw CancellationException("Scan cancelled")
            Thread.sleep(1500L * attempt)
            response = client.newCall(request).execute()
            body = response.body?.string() ?: return null
            if (response.code == 404) return null
            if (!response.isSuccessful) {
                throw Exception("Report lookup failed (${response.code}): $body")
            }
        }
        if (isJsonObject(body)) {
            return gson.fromJson(body, FileReportResponse::class.java)
        }
        throw Exception("Report lookup kept returning a non-JSON body")
    }

    /** True when [s] starts with a JSON object token, so we never feed Gson a
     *  plain string / HTML body and hit "Expected BEGIN_OBJECT but was STRING". */
    private fun isJsonObject(s: String?): Boolean {
        val t = s?.trimStart() ?: return false
        return t.startsWith("{")
    }

    private fun uploadFile(
        file: File,
        apiKey: String,
        onProgress: ((String) -> Unit)? = null,
        checkCancelled: () -> Boolean = { false }
    ): String {
        if (file.length() > 32L * 1024 * 1024) {
            // POST /files rejects anything above 32 MB; use the presigned URL.
            return uploadViaUploadUrl(file, apiKey, onProgress, checkCancelled)
        }
        return try {
            uploadDirect(file, apiKey, onProgress, checkCancelled)
        } catch (e: PayloadTooLargeException) {
            Log.d(TAG, "Direct upload rejected (too large), falling back to upload URL.")
            uploadViaUploadUrl(file, apiKey, onProgress, checkCancelled)
        }
    }

    /** POST the file straight to /files (only admits files up to 32 MB). */
    private fun uploadDirect(
        file: File,
        apiKey: String,
        onProgress: ((String) -> Unit)? = null,
        checkCancelled: () -> Boolean = { false }
    ): String {
        pace(onProgress, checkCancelled)
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("application/octet-stream".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/files")
            .addHeader("x-apikey", apiKey)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) {
            val error = gson.fromJson(body, ErrorResponse::class.java)
            val message = error.error?.message ?: "Upload failed (${response.code})"
            if (response.code == 409) {
                throw AlreadySubmittedException(message)
            }
            if (response.code == 413) {
                throw PayloadTooLargeException(message)
            }
            throw Exception(message)
        }
        if (!isJsonObject(body)) {
            throw Exception("Upload returned a non-JSON body: ${body.take(80)}")
        }

        val uploadResponse = gson.fromJson(body, UploadResponse::class.java)
        return uploadResponse.data?.id ?: throw Exception("No analysis ID in response")
    }

    /**
     * Request a single-use presigned upload URL, then POST the file to it.
     * Admits files up to 650 MB.
     */
    private fun uploadViaUploadUrl(
        file: File,
        apiKey: String,
        onProgress: ((String) -> Unit)? = null,
        checkCancelled: () -> Boolean = { false }
    ): String {
        pace(onProgress, checkCancelled)
        val urlRequest = Request.Builder()
            .url("$BASE_URL/files/upload_url")
            .addHeader("x-apikey", apiKey)
            .get()
            .build()
        val urlResponse = client.newCall(urlRequest).execute()
        val urlBody = urlResponse.body?.string() ?: throw Exception("Empty upload URL response")
        if (!urlResponse.isSuccessful) {
            throw Exception("Getting upload URL failed (${urlResponse.code}): $urlBody")
        }
        if (!isJsonObject(urlBody)) {
            throw Exception("Upload URL response was not JSON: ${urlBody.take(80)}")
        }
        val uploadUrl = gson.fromJson(urlBody, UploadUrlResponse::class.java).data
            ?: throw Exception("No upload URL in response")

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("application/octet-stream".toMediaType())
            )
            .build()
        val request = Request.Builder()
            .url(uploadUrl)
            .addHeader("x-apikey", apiKey)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        if (!response.isSuccessful) {
            val error = gson.fromJson(body, ErrorResponse::class.java)
            throw Exception(error.error?.message ?: "Upload via URL failed (${response.code})")
        }
        if (!isJsonObject(body)) {
            throw Exception("Upload via URL returned a non-JSON body: ${body.take(80)}")
        }
        val uploadResponse = gson.fromJson(body, UploadResponse::class.java)
        return uploadResponse.data?.id ?: throw Exception("No analysis ID in response")
    }

    private fun pollAnalysis(
        analysisId: String,
        apiKey: String,
        onProgress: ((String) -> Unit)?,
        onPercent: ((Int) -> Unit)?,
        checkCancelled: () -> Boolean = { false }
    ): AnalysisResponse {
        val maxAttempts = 90 // Poll every 3s for up to ~4.5 minutes.
        var lastStatus = ""
        repeat(maxAttempts) { attempt ->
            if (checkCancelled()) throw CancellationException("Scan cancelled")
            // Analysis polls (GET /analyses/{id}) are read-only status checks
            // and do NOT consume VT's 4/min quota — no pace() here.
            Thread.sleep(3000)

            val request = Request.Builder()
                .url("$BASE_URL/analyses/$analysisId")
                .addHeader("x-apikey", apiKey)
                .get()
                .build()

            var response = client.newCall(request).execute()
            while (response.code == 429) {
                // Window exhausted: wait until it clears (live countdown), then
                // poll again — this does not consume an attempt.
                Log.d(TAG, "Analysis poll rate limited (429), waiting for the limit to clear.")
                rateLimiter.waitForWindowClear(
                    checkCancelled = checkCancelled,
                    onWait = { secs ->
                        onProgress?.invoke("Rate limit hit — retrying in ${secs}s…")
                    }
                )
                response = client.newCall(request).execute()
            }
            val body = response.body?.string() ?: throw Exception("Empty response")

            if (!response.isSuccessful) {
                throw Exception("Analysis poll failed (${response.code}): $body")
            }
            if (!isJsonObject(body)) {
                throw Exception("Analysis poll returned a non-JSON body: ${body.take(80)}")
            }

            val analysis = gson.fromJson(body, AnalysisResponse::class.java)
            val status = analysis.data?.attributes?.status
            Log.d(TAG, "Analysis poll ${attempt + 1}: status=$status")

            // Rough progress 60→100 while polling (each attempt ≈ a slice).
            onPercent?.invoke(60 + ((attempt + 1) * 40) / maxAttempts)

            if (status == "completed") {
                onPercent?.invoke(100)
                return analysis
            }
            if (status != lastStatus) {
                lastStatus = status ?: ""
                onProgress?.invoke(
                    when (status) {
                        "queued" -> "Queued at VirusTotal — waiting for engines…"
                        "in-progress" -> "VirusTotal engines scanning…"
                        else -> "Waiting for analysis…"
                    }
                )
            }
        }

        throw Exception("Analysis timed out after ${maxAttempts * 3}s (VirusTotal queue is busy)")
    }

    // --- Result parsing ---

    private fun parseFileReport(report: FileReportResponse, fileName: String): ScanResult {
        val attrs = report.data?.attributes
            ?: return ScanResult.Error("No file data in the VirusTotal report")
        val stats = attrs.lastAnalysisStats
            ?: return ScanResult.Error("No analysis stats in the VirusTotal report")
        val totalEngines = stats.total()
        if (totalEngines == 0) {
            return ScanResult.Error("No engine results available")
        }
        val malicious = stats.malicious
        val suspicious = stats.suspicious
        val engineHits = detectionEngines(attrs.lastAnalysisResults)

        return if (malicious + suspicious > 0) {
            ScanResult.Malicious(
                sha256 = attrs.sha256 ?: report.data?.id,
                fileName = fileName,
                typeDescription = attrs.typeDescription,
                sizeBytes = attrs.size,
                timesSubmitted = attrs.timesSubmitted,
                detections = malicious + suspicious,
                suspicious = suspicious,
                totalEngines = totalEngines,
                engines = engineHits,
                suggestedThreatLabel = suggestedLabel(engineHits, attrs),
                sandboxMalwareNames = attrs.sandboxMalwareNames(),
                firstSubmissionDate = attrs.firstSubmissionDate,
                lastAnalysisDate = attrs.lastAnalysisDate,
                reputation = attrs.reputation,
                // The report came from VirusTotal's database, not a new upload.
                cached = true
            )
        } else {
            ScanResult.Clean(
                sha256 = attrs.sha256 ?: report.data?.id,
                fileName = fileName,
                typeDescription = attrs.typeDescription,
                sizeBytes = attrs.size,
                timesSubmitted = attrs.timesSubmitted,
                totalEngines = totalEngines,
                firstSubmissionDate = attrs.firstSubmissionDate,
                lastAnalysisDate = attrs.lastAnalysisDate,
                reputation = attrs.reputation,
                votesHarmless = attrs.totalVotes?.harmless ?: 0,
                votesMalicious = attrs.totalVotes?.malicious ?: 0,
                // The report came from VirusTotal's database, not a new upload.
                cached = true
            )
        }
    }

    private fun parseAnalysis(analysis: AnalysisResponse, fileName: String): ScanResult {
        val attrs = analysis.data?.attributes
            ?: return ScanResult.Error("No analysis data available")
        val stats = attrs.stats
            ?: return ScanResult.Error("No analysis stats available")
        val totalEngines = stats.total()
        if (totalEngines == 0) {
            return ScanResult.Error("No engine results available")
        }
        val fileInfo = attrs.meta?.fileInfo
        val malicious = stats.malicious
        val suspicious = stats.suspicious
        val engineHits = detectionEngines(attrs.results)

        return if (malicious + suspicious > 0) {
            ScanResult.Malicious(
                sha256 = fileInfo?.sha256,
                fileName = fileName,
                typeDescription = fileInfo?.typeDescription,
                sizeBytes = fileInfo?.size,
                timesSubmitted = fileInfo?.timesSubmitted,
                detections = malicious + suspicious,
                suspicious = suspicious,
                totalEngines = totalEngines,
                engines = engineHits,
                suggestedThreatLabel = attrs.threatNames?.firstOrNull() ?: suggestedLabel(engineHits, null),
                sandboxMalwareNames = emptyList(),
                firstSubmissionDate = null,
                lastAnalysisDate = attrs.date,
                reputation = null,
                // Freshly uploaded and analysed just now.
                cached = false
            )
        } else {
            ScanResult.Clean(
                sha256 = fileInfo?.sha256,
                fileName = fileName,
                typeDescription = fileInfo?.typeDescription,
                sizeBytes = fileInfo?.size,
                timesSubmitted = fileInfo?.timesSubmitted,
                totalEngines = totalEngines,
                firstSubmissionDate = null,
                lastAnalysisDate = attrs.date,
                reputation = null,
                votesHarmless = 0,
                votesMalicious = 0,
                // Freshly uploaded and analysed just now.
                cached = false
            )
        }
    }

    private fun AnalysisStats.total(): Int =
        harmless + malicious + suspicious + undetected + timeout +
            `type-unsupported` + failure + `confirmed-timeout`

    private fun detectionEngines(results: Map<String, EngineResult>?): List<EngineDetection> =
        results.orEmpty()
            .filterValues { it?.category == "malicious" || it?.category == "suspicious" }
            .mapNotNull { (engine, result) ->
                val name = result?.engineName ?: engine
                val detail = result?.result
                if (detail.isNullOrBlank()) null else EngineDetection(name, detail, result.category ?: "")
            }
            .sortedWith(
                compareByDescending<EngineDetection> { it.category == "malicious" }
                    .thenBy { it.engine }
            )

    private fun suggestedLabel(
        engines: List<EngineDetection>,
        attrs: FileReportAttributes?
    ): String? {
        // Prefer VirusTotal's own classification of what the file is.
        attrs?.sandboxVerdicts?.values
            ?.flatMap { it.malwareNames.orEmpty() }
            ?.firstOrNull()
            ?.let { return it }
        // Otherwise use the most common detection string from the engines.
        return engines
            .groupBy { it.result }
            .maxByOrNull { it.value.size }
            ?.key
    }

    private fun FileReportAttributes.sandboxMalwareNames(): List<String> =
        sandboxVerdicts?.values
            ?.flatMap { it.malwareNames.orEmpty() }
            ?.distinct()
            ?.take(4)
            ?: emptyList()

    // --- Response data classes ---

    private class AlreadySubmittedException(message: String) : Exception(message)

    /**
     * Gson adapter that round-trips a [ScanResult] (a sealed interface Gson
     * cannot deserialize on its own) by tagging each object with its kind.
     */
    internal class ScanResultTypeAdapter :
        com.google.gson.JsonSerializer<ScanResult>, com.google.gson.JsonDeserializer<ScanResult> {
        override fun serialize(
            src: ScanResult,
            typeOfSrc: java.lang.reflect.Type,
            context: com.google.gson.JsonSerializationContext
        ): com.google.gson.JsonElement {
            val obj = context.serialize(src, src.javaClass).asJsonObject
            obj.addProperty("kind", when (src) {
                is ScanResult.Clean -> "CLEAN"
                is ScanResult.Malicious -> "MALICIOUS"
                is ScanResult.Error -> "ERROR"
            })
            return obj
        }

        override fun deserialize(
            json: com.google.gson.JsonElement,
            typeOfT: java.lang.reflect.Type,
            context: com.google.gson.JsonDeserializationContext
        ): ScanResult {
            val obj = json.asJsonObject
            return when (obj.get("kind")?.asString) {
                "CLEAN" -> context.deserialize(json, ScanResult.Clean::class.java)
                "MALICIOUS" -> context.deserialize(json, ScanResult.Malicious::class.java)
                else -> context.deserialize(json, ScanResult.Error::class.java)
            }
        }
    }

    private class PayloadTooLargeException(message: String) : Exception(message)

    private data class UploadResponse(
        val data: AnalysisData?
    )

    private data class UploadUrlResponse(
        val data: String?
    )

    private data class QuotasResponse(
        val data: QuotasData?
    )

    private data class QuotasData(
        @SerializedName("api_requests_hourly")
        val hourly: QuotaBucket?,
        @SerializedName("api_requests_daily")
        val daily: QuotaBucket?,
        @SerializedName("api_requests_monthly")
        val monthly: QuotaBucket?
    )

    private data class QuotaBucket(
        val user: QuotaValue?
    )

    private data class QuotaValue(
        val allowed: Int = 0,
        val used: Int = 0
    )

    private data class AnalysisResponse(
        val data: AnalysisData?
    )

    private data class AnalysisData(
        val id: String?,
        val attributes: AnalysisAttributes?
    )

    private data class AnalysisAttributes(
        val status: String?,
        val date: Long?,
        val stats: AnalysisStats?,
        val results: Map<String, EngineResult>?,
        @SerializedName("threat_names")
        val threatNames: List<String>?,
        val meta: AnalysisMeta?
    )

    private data class AnalysisMeta(
        @SerializedName("file_info")
        val fileInfo: FileInfo?
    )

    private data class FileInfo(
        val sha256: String?,
        val size: Long?,
        @SerializedName("type_description")
        val typeDescription: String?,
        @SerializedName("times_submitted")
        val timesSubmitted: Int?
    )

    private data class FileReportResponse(
        val data: FileReportData?
    )

    private data class FileReportData(
        val id: String?,
        val attributes: FileReportAttributes?
    )

    private data class FileReportAttributes(
        val sha256: String?,
        @SerializedName("type_description")
        val typeDescription: String?,
        val size: Long?,
        @SerializedName("first_submission_date")
        val firstSubmissionDate: Long?,
        @SerializedName("last_analysis_date")
        val lastAnalysisDate: Long?,
        @SerializedName("times_submitted")
        val timesSubmitted: Int?,
        val reputation: Int?,
        @SerializedName("total_votes")
        val totalVotes: TotalVotes?,
        @SerializedName("last_analysis_stats")
        val lastAnalysisStats: AnalysisStats?,
        @SerializedName("last_analysis_results")
        val lastAnalysisResults: Map<String, EngineResult>?,
        @SerializedName("sandbox_verdicts")
        val sandboxVerdicts: Map<String, SandboxVerdict>?
    )

    private data class TotalVotes(
        val harmless: Int = 0,
        val malicious: Int = 0
    )

    private data class AnalysisStats(
        val harmless: Int = 0,
        val malicious: Int = 0,
        val suspicious: Int = 0,
        val undetected: Int = 0,
        val timeout: Int = 0,
        @SerializedName("type-unsupported")
        val `type-unsupported`: Int = 0,
        // Note: the API key is "failure" (not "failed") in both the analysis
        // stats and the file report's last_analysis_stats.
        @SerializedName("failure")
        val failure: Int = 0,
        @SerializedName("confirmed-timeout")
        val `confirmed-timeout`: Int = 0,
    )

    private data class EngineResult(
        val category: String?,
        val result: String?,
        @SerializedName("engine_name")
        val engineName: String?
    )

    private data class SandboxVerdict(
        val category: String?,
        @SerializedName("malware_names")
        val malwareNames: List<String>?
    )

    private data class ErrorResponse(
        val error: ErrorDetail?
    )

    private data class ErrorDetail(
        val code: String?,
        val message: String?
    )
}
