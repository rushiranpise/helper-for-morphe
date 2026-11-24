package dev.rushi.apkdownloadhelper.ui.model

import dev.rushi.apkdownloadhelper.download.VirusTotalScanner

internal sealed interface UiState {
    data object Idle : UiState
    data object Loading : UiState
    data class Ready(val result: CandidateResult) : UiState
    data class CheckingPickedFile(val candidate: DownloadCandidate) : UiState
    data class Downloading(
        val candidate: DownloadCandidate,
        val percent: Int,
        val speedBytesPerSec: Double = 0.0,
        val etaMs: Long? = null,
        val statusMessage: String? = null,
        val paused: Boolean = false,
        val connections: Int = 1,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = 0L
    ) : UiState
    data class Error(val message: String) : UiState
    data class FastMode(val progress: FastModeProgress) : UiState
    data class ScanResult(
        val candidate: DownloadCandidate,
        val scanResult: VirusTotalScanner.ScanResult,
        val detail: String,
        val isMalicious: Boolean,
        /** True if the downloaded file's bytes matched the source-published SHA-256. */
        val shaVerified: Boolean = false
    ) : UiState
    data class ScanAsk(val candidate: DownloadCandidate) : UiState
}

internal enum class FastModeChoice { USE, NEXT }

internal sealed interface FastModeFindResult {
    data class Exact(val candidate: DownloadCandidate) : FastModeFindResult
    data class VersionMismatch(val candidate: DownloadCandidate) : FastModeFindResult
    object None : FastModeFindResult
}

internal data class SwitchCandidatePrompt(
    val source: DownloadSource,
    val detail: String,
    val candidates: List<DownloadCandidate>
)

internal data class FastModeProgress(
    val sourceLabel: String? = null,
    val detail: String = "Starting…",
    val percent: Int? = null,
    val speedBytesPerSec: Double = 0.0,
    val etaMs: Long? = null,
    val paused: Boolean = false,
    val connections: Int = 1,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val done: Boolean = false,
    val succeeded: Boolean = false,
    // Present once Fast Mode finishes so the regular source list stays reachable.
    val result: CandidateResult? = null,
    // Set while Fast Mode waits for the user to accept a version-code mismatch
    // or to solve a captcha for an exact captcha-gated candidate.
    val awaitingDecision: Boolean = false,
    val awaitingCaptcha: Boolean = false,
    val mismatchDetail: String? = null,
    // True while Fast Mode (ALWAYS_ASK) asks which version to fetch.
    val awaitingVersionChoice: Boolean = false,
    val versionChoiceDetail: String? = null,
    // The requested version shown on the ALWAYS_ASK prompt, so the user can
    // compare it against the newest version each source offers.
    val versionChoiceRequested: String? = null,
    // True once the downloaded file's bytes matched the source-published SHA-256;
    // shown persistently on the card (not just the transient post-download status).
    val shaVerified: Boolean = false,
    // Reused by the automatic retry flow so the card does not claim Fast Mode.
    val title: String? = null
)
