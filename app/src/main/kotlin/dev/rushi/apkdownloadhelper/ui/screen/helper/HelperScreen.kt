package dev.rushi.apkdownloadhelper.ui.screen.helper

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.rushi.apkdownloadhelper.APK_PICKER_MIME_TYPES
import dev.rushi.apkdownloadhelper.storage.DownloadHistoryEntry
import dev.rushi.apkdownloadhelper.storage.FastModePolicy
import dev.rushi.apkdownloadhelper.storage.HelperSettings
import dev.rushi.apkdownloadhelper.storage.RequestLogEntry
import dev.rushi.apkdownloadhelper.ui.model.CandidateOption
import dev.rushi.apkdownloadhelper.ui.model.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.model.HelperRequest
import dev.rushi.apkdownloadhelper.ui.model.UiState

//==================================================
// Slim entry point that keeps the exact
// signature MainActivity expects and
// routes between the two views.
//==================================================

@Composable
internal fun HelperScreen(
    request: HelperRequest?,
    state: UiState,
    settings: HelperSettings,
    logs: List<RequestLogEntry>,
    installedPackageRefreshToken: Int,
    selectedPagerPage: Int,
    onPagerPageChanged: (Int) -> Unit,
    onSettingsChange: (HelperSettings) -> Unit,
    onOpenUpdates: () -> Unit,
    onRefresh: () -> Unit,
    canRetryFailedSources: Boolean,
    onRetryAll: () -> Unit,
    onRetryFailedSources: () -> Unit,
    onCopyFailureDetails: () -> Unit,
    virusTotalApiKey: String,
    onResolve: (DownloadSource, CandidateOption) -> Unit,
    onDownload: (DownloadCandidate) -> Unit,
    onPickDownloadedFile: (DownloadCandidate, Uri?) -> Unit,
    onUseInstalledApp: (DownloadCandidate) -> Unit,
    onVersionHistory: (DownloadSource) -> Unit,
    onDownloadVersion: (DownloadCandidate) -> Unit,
    onOpenHistoryEntry: (DownloadHistoryEntry) -> Unit,
    onShareHistoryEntry: (DownloadHistoryEntry) -> Unit,
    onClearHistory: () -> Unit,
    onClearLogs: () -> Unit,
    onCancel: () -> Unit,
    onCancelDownload: () -> Unit,
    onPauseDownload: () -> Unit,
    onResumeDownload: () -> Unit,
    onSwitchDownloadSource: (DownloadSource) -> Unit,
    onSkipScanWait: () -> Unit,
    onCancelFastMode: () -> Unit,
    onUseFastModeMismatch: () -> Unit,
    onSkipFastModeMismatch: () -> Unit,
    onChooseVersion: (FastModePolicy) -> Unit,
    onOpenMorphe: () -> Unit,
    onSolveCaptcha: (DownloadCandidate) -> Unit,
    onFormatToggle: (String) -> Unit,
    onProceedAfterScan: () -> Unit,
    onCancelAfterScan: () -> Unit,
    onSkipScan: () -> Unit
) {
    var pendingFilePick by remember { mutableStateOf<DownloadCandidate?>(null) }
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val candidate = pendingFilePick
        pendingFilePick = null
        candidate?.let { onPickDownloadedFile(it, uri) }
    }
    val openDownloadedFilePicker: (DownloadCandidate) -> Unit = { candidate ->
        pendingFilePick = candidate
        filePickerLauncher.launch(APK_PICKER_MIME_TYPES)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        if (request == null) {
            HelperStandaloneContent(
                settings = settings,
                onSettingsChange = onSettingsChange,
                onOpenMorphe = onOpenMorphe
            )
        } else {
            HelperRequestContent(
                request = request,
                state = state,
                settings = settings,
                installedPackageRefreshToken = installedPackageRefreshToken,
                selectedPagerPage = selectedPagerPage,
                onPagerPageChanged = onPagerPageChanged,
                onSettingsChange = onSettingsChange,
                onRefresh = onRefresh,
                canRetryFailedSources = canRetryFailedSources,
                onRetryAll = onRetryAll,
                onRetryFailedSources = onRetryFailedSources,
                onCopyFailureDetails = onCopyFailureDetails,
                virusTotalApiKey = virusTotalApiKey,
                onResolve = onResolve,
                onDownload = onDownload,
                onPickDownloadedFile = openDownloadedFilePicker,
                onUseInstalledApp = onUseInstalledApp,
                onVersionHistory = onVersionHistory,
                onDownloadVersion = onDownloadVersion,
                onCancel = onCancel,
                onCancelDownload = onCancelDownload,
                onPauseDownload = onPauseDownload,
                onResumeDownload = onResumeDownload,
                onSwitchDownloadSource = onSwitchDownloadSource,
                onSkipScanWait = onSkipScanWait,
                onCancelFastMode = onCancelFastMode,
                onUseFastModeMismatch = onUseFastModeMismatch,
                onSkipFastModeMismatch = onSkipFastModeMismatch,
                onChooseVersion = onChooseVersion,
                onSolveCaptcha = onSolveCaptcha,
                onFormatToggle = onFormatToggle,
                onProceedAfterScan = onProceedAfterScan,
                onCancelAfterScan = onCancelAfterScan,
                onSkipScan = onSkipScan
            )
        }
    }
}