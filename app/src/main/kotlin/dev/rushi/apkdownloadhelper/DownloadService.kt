package dev.rushi.apkdownloadhelper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

internal const val ACTION_START_DOWNLOAD = "dev.rushi.apkdownloadhelper.action.START_DOWNLOAD"
internal const val ACTION_CANCEL_DOWNLOAD = "dev.rushi.apkdownloadhelper.action.CANCEL_DOWNLOAD"
internal const val ACTION_RETRY_DOWNLOAD = "dev.rushi.apkdownloadhelper.action.RETRY_DOWNLOAD"
internal const val ACTION_SCAN_PROCEED = "dev.rushi.apkdownloadhelper.action.SCAN_PROCEED"
internal const val ACTION_SCAN_CANCEL = "dev.rushi.apkdownloadhelper.action.SCAN_CANCEL"
internal const val ACTION_SCAN_SKIP = "dev.rushi.apkdownloadhelper.action.SCAN_SKIP"

private const val CHANNEL_PROGRESS = "download_progress"
private const val CHANNEL_DONE = "download_done"
private const val NOTIFICATION_ID_PROGRESS = 1001
internal const val NOTIFICATION_ID_DONE = 1002
internal const val TEMP_CLEANUP_DELAY_MS = 5 * 60 * 1000L
private const val PREFS_PENDING = "pending_download_result"


internal data class PendingDownload(
    val request: HelperRequest,
    val candidate: DownloadCandidate,
    val settings: HelperSettings
)

internal data class PendingDownloadResult(
    val uri: String,
    val fileName: String,
    val packageName: String,
    val versionName: String?,
    val sourceName: String,
    val requestPackage: String,
    val callerPackage: String
)

/**
 * In-memory state shared between [DownloadService] and [MainActivity] so the
 * activity can keep its UI in sync with a download that survives the activity
 * being stopped or destroyed. Completed results are also persisted to
 * SharedPreferences so a re-created activity can hand the file to the caller.
 */
internal object DownloadJobManager {
    data class DownloadJob(
        val request: HelperRequest,
        val candidate: DownloadCandidate,
        val settings: HelperSettings,
        val requestIntentExtras: Bundle? = null,
        val epoch: Long = 0,
        // Only Fast Mode asks the user about a version-code mismatch; manual
        // downloads deliver the file as before the check existed.
        val fastMode: Boolean = false
    )

    sealed interface Event {
        data class Progress(
            val candidate: DownloadCandidate,
            val percent: Int,
            val speedBytesPerSec: Double = 0.0,
            val etaMs: Long? = null
        ) : Event
        data class Completed(val result: PendingDownloadResult, val epoch: Long = 0) : Event
        data class Failed(val candidate: DownloadCandidate, val message: String) : Event
        data class Cancelled(val candidate: DownloadCandidate) : Event
        // The downloaded file is valid except for its version code (which the
        // parser could not know up front). The file is kept so the UI can ask
        // whether to use it anyway.
        data class ValidationMismatch(
            val candidate: DownloadCandidate,
            val file: File,
            val foundVersionCode: Long?
        ) : Event
        /** Short-lived status shown after download hits 100 % but before completion. */
        data class PostDownloadStatus(
            val candidate: DownloadCandidate,
            val status: String
        ) : Event
        /** VirusTotal scan is in progress. [percent] is 0..100, null when unknown. */
        data class Scanning(
            val candidate: DownloadCandidate,
            /** Human status, e.g. "Extracting 2 of 4…" or "Checking VirusTotal…". */
            val status: String,
            val percent: Int? = null,
            /** Estimated ms until the scan finishes, when knowable (null otherwise). */
            val etaMs: Long? = null,
            /** True once the downloaded file's bytes matched the source-published SHA-256. */
            val shaVerified: Boolean = false
        ) : Event
        /** VirusTotal scan completed with results. */
        data class ScanComplete(
            val candidate: DownloadCandidate,
            val result: VirusTotalScanner.ScanResult,
            /** True if the downloaded file's bytes matched the source-published SHA-256. */
            val shaVerified: Boolean = false
        ) : Event
        /** The download finished and the UI must ask whether to scan (ASK mode). */
        data class ScanAsk(val candidate: DownloadCandidate) : Event
    }

    /**
     * A downloaded file paused for a user decision about VirusTotal scanning.
     * [PendingScan.Ask] = the download finished and the UI is asking
     * "scan or skip?" (Scan mode ASK). [PendingScan.Decided] = a scan has run
     * and the UI is showing the result, asking "proceed or cancel?".
     */
    sealed interface PendingScan {
        data class Ask(val file: File) : PendingScan
        data class Decided(val file: File) : PendingScan
    }

    @Volatile
    var pendingScan: PendingScan? = null
        private set

    fun setPendingScan(pending: PendingScan?) {
        this.pendingScan = pending
    }

    @Volatile
    var activeJob: DownloadJob? = null
        private set

    /**
     * Monotonic session counter, bumped on every [start]. A completion event
     * only belongs to the request session that started it, so a replayed event
     * from an earlier session (e.g. after activity recreation) can never hand
     * an old file to a new request  even when package and version coincide
     * with the new request's pin.
     */
    @Volatile
    var currentEpoch: Long = 0
        private set

    private val _events = MutableStateFlow<Event?>(null)
    val events: StateFlow<Event?> = _events.asStateFlow()

    fun start(job: DownloadJob) {
        currentEpoch++
        activeJob = job.copy(epoch = currentEpoch)
        _events.value = null
    }

    fun clearEvent() {
        _events.value = null
    }

    fun emit(event: Event) {
        _events.value = event
        if (event is Event.Completed || event is Event.Cancelled) {
            activeJob = null
        }
    }

    fun readPendingResult(context: Context): PendingDownloadResult? {
        val prefs = context.getSharedPreferences(PREFS_PENDING, Context.MODE_PRIVATE)
        val uri = prefs.getString("uri", null) ?: return null
        return PendingDownloadResult(
            uri = uri,
            fileName = prefs.getString("file_name", "download") ?: "download",
            packageName = prefs.getString("package_name", "") ?: "",
            versionName = prefs.getString("version_name", null),
            sourceName = prefs.getString("source_name", "") ?: "",
            requestPackage = prefs.getString("request_package", "") ?: "",
            callerPackage = prefs.getString("caller_package", "") ?: ""
        )
    }

    fun persistPendingResult(result: PendingDownloadResult, context: Context) {
        context.getSharedPreferences(PREFS_PENDING, Context.MODE_PRIVATE)
            .edit()
            .putString("uri", result.uri)
            .putString("file_name", result.fileName)
            .putString("package_name", result.packageName)
            .putString("version_name", result.versionName)
            .putString("source_name", result.sourceName)
            .putString("request_package", result.requestPackage)
            .putString("caller_package", result.callerPackage)
            .apply()
    }

    fun clearPendingResult(context: Context) {
        context.getSharedPreferences(PREFS_PENDING, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

internal class DownloadService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null
    // Verdict of the last scan, carried from scanAndHandoff into handleSuccess
    // so the hand-off gets recorded with its VirusTotal outcome.
    private var lastScanVerdict: ScanVerdict? = null
    // Set while a VirusTotal scan is running so the user can abort just the scan
    // and hand the already-downloaded file off to Morphe without a verdict.
    // [skipScanRequested] is flipped by ACTION_SCAN_SKIP; the scanner aborts via
    // its checkCancelled lambda, and the CancellationException handler checks it
    // to route to a hand-off instead of a plain cancel.
    @Volatile private var skipScanRequested = false
    @Volatile private var skipScanFile: File? = null
    // True when the downloaded file's bytes matched the SHA-256 the source
    // published for it (APKMirror single APK, Uptodown). Surfaced in the
    // download status so the user sees integrity was verified, since the check
    // is otherwise silent on success.
    @Volatile private var lastShaVerified = false
    // True once the user cancels the whole request while a scan is running.
    // The scanner polls this via checkCancelled. NOTE: it must NOT be derived
    // from downloadJob.isActive — in ASK mode the scan runs in a fresh
    // serviceScope.launch after the download job has already completed, so
    // downloadJob.isActive is false and the scan would abort instantly.
    @Volatile private var scanAbortRequested = false
    private var lastPostedPercent = -1
    private var lastPostedTime = 0L
    private var lastProgressBytes = 0L
    private var lastProgressTime = 0L
    private var currentSpeedBytesPerSec = 0.0
    private var speedSamples = 0

    private val browserUserAgent =
        "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL}) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36"

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .dns(AdGuardDns)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", browserUserAgent)
                    .build()
            )
        }
        .addInterceptor(httpLoggingInterceptor("Download"))
        .build()

    private val apkPureClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .dns(AdGuardDns)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "APKPure/3.19.39 (Aegon)")
                    .build()
            )
        }
        .addInterceptor(httpLoggingInterceptor("APKPure"))
        .build()

    private val downloader = ApkDownloader(client, apkPureClient, onRetry = ::retryNotification)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        restoreAndPersistRateLimiter()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL_DOWNLOAD -> {
                cancelDownload()
                return START_NOT_STICKY
            }
            ACTION_SCAN_PROCEED -> {
                val job = DownloadJobManager.activeJob
                val pending = DownloadJobManager.pendingScan
                if (job != null && pending != null) {
                    DownloadJobManager.setPendingScan(null)
                    serviceScope.launch {
                        when (pending) {
                            // User said "Scan" at the ask prompt: run the scan.
                            is DownloadJobManager.PendingScan.Ask -> scanAndHandoff(job, pending.file)
                            // User said "Proceed" after seeing the result: hand off.
                            is DownloadJobManager.PendingScan.Decided -> handleSuccess(job, pending.file)
                        }
                    }
                }
                return START_NOT_STICKY
            }
            ACTION_SCAN_CANCEL -> {
                val job = DownloadJobManager.activeJob
                val pending = DownloadJobManager.pendingScan
                DownloadJobManager.setPendingScan(null)
                if (job != null && pending != null) {
                    serviceScope.launch {
                        when (pending) {
                            // User chose to skip scanning: hand off directly.
                            is DownloadJobManager.PendingScan.Ask -> handleSuccess(job, pending.file)
                            // User chose not to hand off after seeing the result.
                            is DownloadJobManager.PendingScan.Decided -> {
                                DownloadJobManager.emit(DownloadJobManager.Event.Cancelled(job.candidate))
                                stopForeground(STOP_FOREGROUND_REMOVE)
                                stopSelf()
                            }
                        }
                    }
                } else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                return START_NOT_STICKY
            }
            ACTION_SCAN_SKIP -> {
                // Opt out of the VirusTotal scan and hand the downloaded file
                // straight to Morphe. Covers every stage: the ASK prompt, the
                // result card, and a scan still in progress (ALWAYS mode).
                val job = DownloadJobManager.activeJob
                val pending = DownloadJobManager.pendingScan
                if (job != null && pending != null) {
                    // Prompt or result already shown: hand off without a scan.
                    DownloadJobManager.setPendingScan(null)
                    serviceScope.launch {
                        when (pending) {
                            is DownloadJobManager.PendingScan.Ask -> handleSuccess(job, pending.file)
                            is DownloadJobManager.PendingScan.Decided -> {
                                // User is done waiting for the verdict — hand off anyway.
                                val f = skipScanFile ?: pending.file
                                lastScanVerdict = skippedVerdict()
                                handleSuccess(job, f)
                            }
                        }
                    }
                } else if (job != null && skipScanFile != null) {
                    // Mid-scan in ALWAYS mode: abort the scan, hand off the file.
                    skipScanRequested = true
                    lastScanVerdict = skippedVerdict()
                    downloadJob?.cancel()
                } else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                return START_NOT_STICKY
            }
            ACTION_RETRY_DOWNLOAD -> {
                val job = DownloadJobManager.activeJob ?: run {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startDownload(job)
                return START_NOT_STICKY
            }
            else -> {
                val job = DownloadJobManager.activeJob ?: run {
                    stopSelf()
                    return START_NOT_STICKY
                }
                // A duplicate START_DOWNLOAD (a double tap, or a stale activity
                // instance starting the same job again) must not cancel and
                // restart the download in flight. Two jobs racing over the same
                // files corrupts the output and lets a zombie job's Cancelled
                // event overwrite the real completion  leaving the app stuck
                // at 100% with the file never returned.
                if (downloadJob?.isActive == true) {
                    Log.i(TAG, "Download already in progress; ignoring duplicate start.")
                    return START_NOT_STICKY
                }
                startDownload(job)
                return START_NOT_STICKY
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun startDownload(job: DownloadJobManager.DownloadJob) {
        val candidate = job.candidate
        lastScanVerdict = null
        skipScanRequested = false
        skipScanFile = null
        lastShaVerified = false
        scanAbortRequested = false
        lastPostedPercent = -1
        lastPostedTime = 0L
        lastProgressBytes = 0L
        lastProgressTime = 0L
        currentSpeedBytesPerSec = 0.0
        speedSamples = 0
        val notification = buildProgressNotification(candidate, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID_PROGRESS,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID_PROGRESS, notification)
        }
        downloadJob?.cancel()
        // Also abort any in-flight blocking body read from the old job; a
        // coroutine cancellation alone cannot interrupt a blocking OkHttp read,
        // which would otherwise keep writing to the same staged file.
        downloader.cancelCurrent()
        downloadJob = serviceScope.launch {
            try {
                val file = runDownload(job)
                // Check if VirusTotal scanning is needed before handoff.
                val settings = job.settings
                if (settings.virusTotalEnabled && settings.virusTotalApiKey.isNotBlank()) {
                    when (settings.virusTotalScanMode) {
                        VirusTotalScanMode.ALWAYS -> {
                            scanAndHandoff(job, file)
                        }
                        VirusTotalScanMode.ASK -> {
                            // Emit event, store file, wait for user decision.
                            DownloadJobManager.setPendingScan(DownloadJobManager.PendingScan.Ask(file))
                            DownloadJobManager.emit(
                                DownloadJobManager.Event.ScanAsk(job.candidate)
                            )
                            notifySafe(
                                NOTIFICATION_ID_PROGRESS,
                                buildProgressNotification(
                                    job.candidate,
                                    100,
                                    "Scan with VirusTotal?"
                                )
                            )
                            // Don't call handleSuccess yet — wait for SCAN_PROCEED or SCAN_CANCEL.
                        }
                        VirusTotalScanMode.NEVER -> {
                            handleSuccess(job, file)
                        }
                    }
                } else {
                    handleSuccess(job, file)
                }
            } catch (error: Throwable) {
                if (error is CancellationException || error.message == "Canceled") {
                    if (skipScanRequested) {
                        // User skipped the scan: hand the downloaded file off
                        // instead of aborting the whole request.
                        val f = skipScanFile
                        skipScanRequested = false
                        skipScanFile = null
                        lastScanVerdict = skippedVerdict()
                        if (f != null) {
                            handleSuccess(job, f)
                        } else {
                            handleCancelled(job)
                        }
                    } else {
                        handleCancelled(job)
                    }
                } else if (error is VersionCodeMismatchException) {
                    // Keep the file; the UI decides whether to use it anyway.
                    DownloadJobManager.emit(
                        DownloadJobManager.Event.ValidationMismatch(
                            candidate = job.candidate,
                            file = error.file,
                            foundVersionCode = error.foundVersionCode
                        )
                    )
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    handleFailure(job, error)
                }
            }
        }
    }

    private suspend fun runDownload(job: DownloadJobManager.DownloadJob): File {
        val candidate = job.candidate
        val downloadsDir = temporaryDownloadsDir().apply { mkdirs() }
        val files = candidate.files.ifEmpty {
            listOf(
                CandidateDownloadFile(
                    url = candidate.url,
                    fileName = "${candidate.packageName}-${candidate.versionName ?: "latest"}.${candidate.fileKind}"
                        .sanitizeFileName()
                )
            )
        }

        val file = if (files.size == 1) {
            downloadSingleFile(candidate, files.single(), downloadsDir)
        } else {
            downloadSplitArchive(candidate, files, downloadsDir)
        }
        emitPostDownloadStatus(
            candidate,
            if (lastShaVerified) "SHA-256 verified \u2713 \u00b7 validating file…" else "Validating downloaded file…"
        )
        validateDownloadedArtifact(
            this,
            job.request,
            candidate,
            file,
            checkVersionCode = job.fastMode
        )
        return file
    }

    private fun handleSuccess(job: DownloadJobManager.DownloadJob, file: File) {
        val request = job.request
        val candidate = job.candidate
        val settings = job.settings

        emitPostDownloadStatus(candidate, "Copying to Downloads…")
        val uri = when (settings.downloadLocation) {
            DownloadLocation.TEMPORARY -> FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.files", file)
            DownloadLocation.DOWNLOADS -> copyToDownloads(file)
        }
        grantUriPermission(request.callerPackage, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        emitPostDownloadStatus(candidate, "Handing off to Morphe…")
        val result = PendingDownloadResult(
            uri = uri.toString(),
            fileName = file.name,
            packageName = candidate.packageName,
            versionName = candidate.versionName,
            sourceName = candidate.source.label,
            requestPackage = request.packageName,
            callerPackage = request.callerPackage
        )
        DownloadJobManager.persistPendingResult(result, applicationContext)
        recordHandOff(request, candidate, file, uri, lastScanVerdict)
        lastScanVerdict = null

        if (
            settings.downloadLocation == DownloadLocation.DOWNLOADS ||
            settings.deleteTemporaryAfterHandoff
        ) {
            scheduleTemporaryDelete(file)
        }

        DownloadJobManager.emit(DownloadJobManager.Event.Completed(result, job.epoch))
        notifyCompletion(job, result)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun scanAndHandoff(job: DownloadJobManager.DownloadJob, file: File) {
        val candidate = job.candidate
        val settings = job.settings
        val apiKey = settings.virusTotalApiKey
        // Track the file so a mid-scan ACTION_SCAN_SKIP can still hand it off.
        skipScanFile = file
        skipScanRequested = false

        DownloadJobManager.emit(
            DownloadJobManager.Event.Scanning(candidate, "Scanning…", shaVerified = lastShaVerified)
        )

        // Capture the latest human status so the percent callback can pair a
        // real 0-100% bar with the right label (e.g. "APK 3 of 4 (split_1.apk):
        // Waiting for analysis… · 82%").
        var scanStatus = "Scanning…"

        // Rate-based remaining-time estimator. Scans are paced by the VT quota
        // (≈16s/APK), so percent doesn't advance linearly — a windowed rate is
        // steadier than an instant one. We smooth over the last few ticks.
        val scanStartNanos = System.nanoTime()
        var lastEtaPercent = 0
        var lastEtaNanos = scanStartNanos
        // In ASK mode this runs in a fresh serviceScope.launch (the download
        // job already completed), so a cancel/skip CancellationException would
        // otherwise be swallowed by the coroutine and leave the service stuck
        // with a stale notification. Handle it here for BOTH modes.
        val scanResult = try {
            VirusTotalScanner.scanDownloadedFile(
                file,
                apiKey,
                onProgress = { status ->
                    scanStatus = status
                    DownloadJobManager.emit(
                        DownloadJobManager.Event.Scanning(candidate, status, shaVerified = lastShaVerified)
                    )
            },
            onPercent = { pct ->
                run {
                    val now = System.nanoTime()
                    val elapsedSinceLast = now - lastEtaNanos
                    val elapsedTotal = now - scanStartNanos
                    var eta: Long? = null
                    if (pct > 0 && pct < 100) {
                        // Progress per nanosecond since the last tick → remaining.
                        val progressed = pct - lastEtaPercent
                        if (progressed > 0) {
                            val ratePctPerMs = progressed.toDouble() / (elapsedSinceLast / 1_000_000.0)
                            // Recompute from the total elapsed for a smoother estimate.
                            val overallRate = pct.toDouble() / (elapsedTotal / 1_000_000.0)
                            val effectiveRate = maxOf(ratePctPerMs, overallRate)
                            if (effectiveRate > 0) {
                                eta = ((100 - pct) / effectiveRate).toLong()
                            }
                        }
                        lastEtaPercent = pct
                        lastEtaNanos = now
                    }
                    // Drive a real 0-100% bar through the scan phases (extraction,
                    // per-APK hashing, report checks, upload, analysis poll) instead
                    // of sitting on a static 100% from the download.
                    DownloadJobManager.emit(
                        DownloadJobManager.Event.Scanning(candidate, scanStatus, percent = pct, etaMs = eta)
                    )
                    notifySafe(
                        NOTIFICATION_ID_PROGRESS,
                        buildProgressNotification(candidate, pct, scanStatus)
                    )
                }
            },
            // Abort promptly when the user cancels OR skips the scan: the
            // scanner checks this between per-APK lookups and during rate-limit
            // pauses, so a cancel lands within a second instead of after a 12s
            // sleep. ACTION_CANCEL flips scanAbortRequested; ACTION_SCAN_SKIP
            // flips skipScanRequested. Never derive this from downloadJob.
            // isActive — in ASK mode the scan runs in a fresh serviceScope
            // .launch after the download job completed, so downloadJob.isActive
            // is false and the scan would abort on its very first check.
            checkCancelled = { scanAbortRequested || skipScanRequested }
            )
        } catch (error: CancellationException) {
            if (skipScanRequested) {
                // User opted out of the scan mid-flight: hand off the
                // already-downloaded file without a verdict.
                val f = skipScanFile
                skipScanRequested = false
                skipScanFile = null
                lastScanVerdict = skippedVerdict()
                if (f != null) {
                    handleSuccess(job, f)
                } else {
                    handleCancelled(job)
                }
            } else {
                // Plain cancel: stop the whole request.
                handleCancelled(job)
            }
            return
        }

        DownloadJobManager.emit(
            DownloadJobManager.Event.ScanComplete(candidate, scanResult, shaVerified = lastShaVerified)
        )

        // Always pause for the user: show the result and let them proceed or
        // cancel. "Always scan" means the scan runs without asking, not that
        // the result is skipped — the user must see it before the file leaves
        // the app.
        lastScanVerdict = when (scanResult) {
            is VirusTotalScanner.ScanResult.Clean -> ScanVerdict(
                label = if (scanResult.scannedFiles != null) {
                    "Clean · 0/${scanResult.totalEngines} engines · ${scanResult.scannedFiles} APKs"
                } else {
                    "Clean · 0/${scanResult.totalEngines} engines"
                },
                malicious = false,
                sha256 = scanResult.sha256,
                scannedFiles = scanResult.scannedFiles,
                result = scanResult
            )
            is VirusTotalScanner.ScanResult.Malicious -> ScanVerdict(
                label = if (scanResult.scannedFiles != null) {
                    "Malicious · ${scanResult.detections}/${scanResult.totalEngines} engines · ${scanResult.fileName}"
                } else {
                    "Malicious · ${scanResult.detections}/${scanResult.totalEngines} engines"
                },
                malicious = true,
                sha256 = scanResult.sha256,
                scannedFiles = scanResult.scannedFiles,
                result = scanResult
            )
            is VirusTotalScanner.ScanResult.Error -> ScanVerdict(
                label = "Scan failed · ${scanResult.message.take(48)}",
                malicious = false,
                failed = true,
                result = scanResult
            )
        }
        DownloadJobManager.setPendingScan(DownloadJobManager.PendingScan.Decided(file))
        notifySafe(
            NOTIFICATION_ID_PROGRESS,
            buildProgressNotification(candidate, 100, "Scan complete — open the app to continue")
        )
    }

    private fun handleFailure(job: DownloadJobManager.DownloadJob, error: Throwable) {
        val message = downloadFailureMessage(job.candidate, error)
        Log.w(TAG, "Download failed for ${job.candidate.packageName}", error)
        DownloadJobManager.emit(DownloadJobManager.Event.Failed(job.candidate, message))
        notifyFailure(job, message)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleCancelled(job: DownloadJobManager.DownloadJob) {
        DownloadJobManager.emit(DownloadJobManager.Event.Cancelled(job.candidate))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** A history verdict for a scan the user chose to skip (handed off unverified). */
    private fun skippedVerdict(): ScanVerdict =
        ScanVerdict(label = "Scan skipped — handed off unverified", scannedFiles = 0)

    private fun emitPostDownloadStatus(candidate: DownloadCandidate, status: String) {
        DownloadJobManager.emit(DownloadJobManager.Event.PostDownloadStatus(candidate, status))
        notifySafe(
            NOTIFICATION_ID_PROGRESS,
            buildProgressNotification(candidate, 100, status)
        )
    }

    private fun cancelDownload() {
        downloader.cancelCurrent()
        downloadJob?.cancel()
        // Flag any in-flight scan (ALWAYS or ASK mode) so its next
        // checkCancelled() poll aborts it even though the scan may be running
        // in a different coroutine than downloadJob.
        scanAbortRequested = true
    }

    private suspend fun downloadSingleFile(
        candidate: DownloadCandidate,
        downloadFile: CandidateDownloadFile,
        downloadsDir: File
    ): File {
        val outputName = if (candidate.source == DownloadSource.AURORA) {
            "${candidate.packageName}-${candidate.versionName ?: "latest"}-aurora.apk"
        } else {
            downloadFile.fileName
        }
        val safeName = outputName.sanitizeFileName()
        val outputFile = File(downloadsDir, safeName)
        val stagedFile = File(downloadsDir, "$safeName.part")
        downloader.downloadToFile(downloadFile, stagedFile) { copied, total ->
            updateDownloadProgress(candidate, copied, total)
        }
        if (outputFile.exists()) outputFile.delete()
        if (!stagedFile.renameTo(outputFile)) {
            stagedFile.copyTo(outputFile, overwrite = true)
            stagedFile.delete()
        }
        verifyExpectedSha256(candidate, downloadFile, outputFile)
        return outputFile
    }

    /**
     * Check the finished file against the SHA-256 the source published for it.
     * When the expected value is present and the bytes don't match, delete the
     * file and fail with a manual-mode hint rather than hand off a corrupted or
     * wrong build. (Only Uptodown and APKMirror set [CandidateDownloadFile.expectedSha256].)
     */
    private fun verifyExpectedSha256(
        candidate: DownloadCandidate,
        downloadFile: CandidateDownloadFile,
        outputFile: File
    ) {
        val expected = downloadFile.expectedSha256?.normalizeHex() ?: return
        val actual = sha256HexOf(outputFile) ?: return
        if (actual.equals(expected, ignoreCase = true)) {
            Log.i(TAG, "SHA-256 verified for ${outputFile.name} expected ${expected}")
            lastShaVerified = true
            return
        }
        outputFile.delete()
        Log.e(TAG, "SHA-256 check FAILED for ${outputFile.name}: expected $expected, got $actual")
        throw IllegalStateException(
            "Downloaded file failed its SHA-256 check (expected $expected, got $actual). " +
                "The file may have been modified in transit. Try the source's Manual mode."
        )
    }

    private suspend fun downloadSplitArchive(
        candidate: DownloadCandidate,
        files: List<CandidateDownloadFile>,
        downloadsDir: File
    ): File {
        val outputFile = File(
            downloadsDir,
            "${candidate.packageName}-${candidate.versionName ?: "latest"}-${candidate.source.label}.apks"
                .sanitizeFileName()
        )
        val knownTotal = files.mapNotNull { it.size }.sum().takeIf { it > 0L }
        var completed = 0L
        val staged = files.mapIndexed { index, file ->
            val stagedFile = File(
                downloadsDir,
                "${candidate.packageName}-${candidate.source.name}-split_$index.part".sanitizeFileName()
            )
            downloader.downloadToFile(file, stagedFile) { fileCopied, total ->
                knownTotal?.let { updateDownloadProgress(candidate, completed + fileCopied, it) }
            }
            completed += stagedFile.length()
            stagedFile to file.fileName.ifBlank { "split_$index.apk" }.sanitizeFileName()
        }

        ZipOutputStream(outputFile.outputStream()).use { zip ->
            staged.forEach { (stagedFile, entryName) ->
                zip.putNextEntry(ZipEntry(entryName))
                stagedFile.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
                stagedFile.delete()
            }
        }
        return outputFile
    }

    private fun retryNotification(attempt: Int, delayMs: Long) {
        val candidate = DownloadJobManager.activeJob?.candidate ?: return
        notifySafe(
            NOTIFICATION_ID_PROGRESS,
            buildProgressNotification(
                candidate,
                lastPostedPercent.coerceAtLeast(0),
                "Connection issue  retrying in ${delayMs / 1000}s (attempt $attempt)"
            )
        )
    }

    private fun downloadClientFor(url: String): OkHttpClient =
        if (url.contains("apkpure", ignoreCase = true)) apkPureClient else client

    private fun updateDownloadProgress(candidate: DownloadCandidate, copied: Long, total: Long) {
        if (total <= 0L) return
        val now = SystemClock.elapsedRealtime()
        if (lastProgressTime == 0L) {
            lastProgressTime = now
            lastProgressBytes = copied
        } else {
            val dt = now - lastProgressTime
            if (dt >= 500L) {
                val delta = copied - lastProgressBytes
                if (delta > 0L) {
                    val instantaneous = delta * 1000.0 / dt
                    // CDN reads are bursty (large chunks arrive in a few ms), so a
                    // raw windowed speed jumps around. Exponential moving average
                    // settles within a couple of seconds and stays stable.
                    currentSpeedBytesPerSec = if (speedSamples == 0) {
                        instantaneous
                    } else {
                        0.3 * instantaneous + 0.7 * currentSpeedBytesPerSec
                    }
                    speedSamples++
                } else if (delta == 0L && speedSamples > 0) {
                    // A stalled window (no bytes read) should drag the average
                    // down so the ETA reflects reality instead of the last burst.
                    currentSpeedBytesPerSec *= 0.5
                }
                lastProgressBytes = copied
                lastProgressTime = now
            }
        }
        val percent = ((copied * 100f) / total).roundToInt().coerceIn(0, 100)
        val etaMs = if (currentSpeedBytesPerSec > 0.0) {
            ((total - copied) / currentSpeedBytesPerSec * 1000.0).toLong()
        } else null
        DownloadJobManager.emit(
            DownloadJobManager.Event.Progress(candidate, percent, currentSpeedBytesPerSec, etaMs)
        )
        // Refresh at least once a second so speed/ETA stay fresh even when
        // percent changes slowly (big files, slow links); percent-gating alone
        // would leave a stale ETA up for many seconds.
        if (percent != lastPostedPercent || now - lastPostedTime >= 1000L) {
            lastPostedPercent = percent
            lastPostedTime = now
            val speed = formatSpeed(currentSpeedBytesPerSec)
            val text = buildString {
                append(percent).append('%')
                if (speed.isNotEmpty()) append(" · ").append(speed)
                if (etaMs != null && etaMs > 0L) {
                    append(" · ").append(formatEta(etaMs)).append(" left")
                }
            }
            notifySafe(NOTIFICATION_ID_PROGRESS, buildProgressNotification(candidate, percent, text))
        }
    }

    private fun formatSpeed(bytesPerSec: Double): String {
        if (bytesPerSec <= 0.0) return ""
        val mb = bytesPerSec / (1024.0 * 1024.0)
        if (mb >= 1.0) return String.format(Locale.US, "%.1f MB/s", mb)
        val kb = bytesPerSec / 1024.0
        if (kb >= 1.0) return String.format(Locale.US, "%.0f KB/s", kb)
        return String.format(Locale.US, "%.0f B/s", bytesPerSec)
    }

    private fun formatEta(ms: Long): String {
        val totalSec = (ms / 1000L).coerceAtLeast(1L)
        val h = totalSec / 3600L
        val m = (totalSec % 3600L) / 60L
        val s = totalSec % 60L
        return if (h > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%d:%02d", m, s)
        }
    }

    private fun temporaryDownloadsDir(): File = File(cacheDir, "downloads")

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_PROGRESS, "Download progress", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows the progress of APK downloads"
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, "Download results", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Alerts when a download finishes or fails"
            }
        )
    }

    private fun buildProgressNotification(
        candidate: DownloadCandidate,
        percent: Int,
        contentText: String? = null
    ): Notification {
        val contentIntent = requestContentIntent(DownloadJobManager.activeJob?.requestIntentExtras)
        val cancelIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, DownloadService::class.java).setAction(ACTION_CANCEL_DOWNLOAD),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_PROGRESS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading ${candidate.name}")
            .setContentText(contentText ?: if (percent <= 0) "Preparing…" else "$percent%")
            .setProgress(100, percent.coerceIn(0, 100), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(0, "Cancel", cancelIntent)
            .build()
    }

    private fun notifyCompletion(job: DownloadJobManager.DownloadJob, result: PendingDownloadResult) {
        val notification = NotificationCompat.Builder(this, CHANNEL_DONE)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download complete: ${job.candidate.name}")
            .setContentText(
                "${job.candidate.versionDisplay} from ${job.candidate.source.label} · " +
                    "tap to return it to ${result.callerPackage}"
            )
            .setContentIntent(requestContentIntent(job.requestIntentExtras))
            .setAutoCancel(true)
            .build()
        notifySafe(NOTIFICATION_ID_DONE, notification)
    }

    private fun notifyFailure(job: DownloadJobManager.DownloadJob, message: String) {
        val retryIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, DownloadService::class.java).setAction(ACTION_RETRY_DOWNLOAD),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_DONE)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Download failed: ${job.candidate.name}")
            .setContentText(message.lineSequence().firstOrNull().orEmpty().take(180))
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(requestContentIntent(job.requestIntentExtras))
            .addAction(0, "Retry", retryIntent)
            .setAutoCancel(true)
            .build()
        notifySafe(NOTIFICATION_ID_DONE, notification)
    }

    private fun requestContentIntent(extras: Bundle?): PendingIntent {
        val launch = Intent(this, MainActivity::class.java).apply {
            action = DownloadHelperContract.ACTION_DOWNLOAD_ORIGINAL_APK
            extras?.let(::putExtras)
        }
        return PendingIntent.getActivity(
            this,
            2,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun notifySafe(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        runCatching {
            NotificationManagerCompat.from(this).notify(id, notification)
        }
    }
}

internal fun Context.scheduleTemporaryDelete(file: File) {
    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    // Record the deletion first so a process death between download and cleanup
    // cannot leave the handed-off file behind: the startup cleanup re-runs any
    // recorded deletion that the in-process thread never got to.
    prefs.edit().putStringSet(
        "pending_temp_deletes",
        prefs.getStringSet("pending_temp_deletes", emptySet()).orEmpty() +
            "${file.absolutePath}|${System.currentTimeMillis()}"
    ).apply()
    Thread {
        Thread.sleep(TEMP_CLEANUP_DELAY_MS)
        Log.i(TAG, "Cleaning up handed-off temp file: ${file.absolutePath}")
        runCatching { file.delete() }
        val remaining = prefs.getStringSet("pending_temp_deletes", emptySet())
            .orEmpty()
            .filterNot { it.startsWith("${file.absolutePath}|") }
            .toSet()
        prefs.edit().putStringSet("pending_temp_deletes", remaining).apply()
    }.apply {
        name = "apk-helper-temp-cleanup"
        isDaemon = true
        start()
    }
}

internal fun Context.copyToDownloads(file: File): Uri {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, file.name)
            put(MediaStore.Downloads.MIME_TYPE, file.mimeType())
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Morphe Helper")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create a Downloads entry.")

        try {
            contentResolver.openOutputStream(uri)?.use { output ->
                file.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Could not write to Downloads.")

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)
            uri
        } catch (error: Throwable) {
            contentResolver.delete(uri, null, null)
            throw error
        }
    } else {
        val downloadsDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Morphe Helper"
        ).apply { mkdirs() }
        val output = downloadsDir.uniqueChild(file.name)
        file.copyTo(output, overwrite = false)
        FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.files", output)
    }
}

internal class VersionCodeMismatchException(
    val file: File,
    val foundVersionCode: Long?
) : Exception("Version code: requested a different build, found $foundVersionCode")

/** Strip whitespace and any common separators/prefix from a published hex digest. */
private fun String.normalizeHex(): String? =
    lowercase(Locale.US)
        .filter { it.isDigit() || it in 'a'..'f' }
        .takeIf { it.length == 64 }

internal fun sha256HexOf(file: File): String? = runCatching {
    java.security.MessageDigest.getInstance("SHA-256")
        .digest(file.inputStream().use { it.readBytes() })
        .joinToString("") { "%02x".format(it) }
}.getOrNull()

internal fun validateDownloadedArtifact(
    context: Context,
    request: HelperRequest,
    candidate: DownloadCandidate,
    file: File,
    checkVersionCode: Boolean = false
) {
    val shouldValidateMetadata = candidate.fileKind.lowercase(Locale.US) in setOf("apk", "apks", "apkm", "xapk") ||
        file.extension.lowercase(Locale.US) in setOf("apk", "apks", "apkm", "xapk")
    val metadata = context.readDownloadedApkMetadata(file) ?: run {
        check(!shouldValidateMetadata) {
            file.delete()
            "Downloaded file could not be read as an APK.".withManualModeHint()
        }
        return
    }
    val hardMismatches = buildList {
        if (metadata.packageName != request.packageName) {
            add("Package: requested ${request.packageName}, found ${metadata.packageName}")
        }

        // A secondary build (e.g. APKMirror's "-SECONDARY") shares its version
        // number with the normal release but is a different package Morphe
        // cannot patch. The source URL/file name betrays it even when the
        // parsed version and the downloaded manifest look normal.
        if (candidate.hasVariantBuildMarker && !request.requestsVariantBuild) {
            add("Variant: the source offered a secondary build Morphe cannot patch")
        }

        if (candidate.option == CandidateOption.REQUESTED) {
            val requestedNames = request.knownVersionNames
            if (
                requestedNames.isNotEmpty() &&
                requestedNames.none { metadata.versionName.versionNameEquals(it) }
            ) {
                add(
                    "Version: requested ${requestedNames.joinToString()}, " +
                        "found ${metadata.versionName ?: "unknown"}"
                )
            }
        }
    }

    // Wrong package or version name: the file is unusable  delete and fail.
    check(hardMismatches.isEmpty()) {
        file.delete()
        "Downloaded file does not match Morphe request.\n${hardMismatches.joinToString("\n")}"
            .withManualModeHint()
    }

    // Version-code-only mismatch: the file is otherwise valid, but its build
    // differs from the request. Only enforced for Fast Mode, which keeps the
    // file and asks the user whether to use it anyway; manual downloads
    // deliver normally.
    if (checkVersionCode && candidate.option == CandidateOption.REQUESTED) {
        val requestedCodes = request.requestedVersionCodes +
            request.compatibleVersionCodes.filter { it > 0L }
        if (
            requestedCodes.isNotEmpty() &&
            metadata.versionCode !in requestedCodes
        ) {
            throw VersionCodeMismatchException(file, metadata.versionCode)
        }
    }

    // Fast Mode "Latest" must never hand over a file that is not actually
    // newer than the requested version. The downloaded APK's real manifest is
    // the ground truth here: the source page's version can be misparsed (e.g.
    // an app slug ending in a digit leaking into the version), which would
    // make an older build pass the "newer than requested" gate. Delete and
    // fail loudly instead of delivering an older APK labelled "latest".
    if (checkVersionCode && candidate.option == CandidateOption.LATEST) {
        val requestedName = request.requestedVersionName
        val foundName = metadata.versionName
        if (
            requestedName != null &&
            foundName != null &&
            compareVersionNames(foundName, requestedName) <= 0
        ) {
            file.delete()
            throw IllegalStateException(
                "Downloaded \"latest\" version $foundName is not newer than " +
                    "requested $requestedName. Deleted the file."
            )
        }
    }
}


