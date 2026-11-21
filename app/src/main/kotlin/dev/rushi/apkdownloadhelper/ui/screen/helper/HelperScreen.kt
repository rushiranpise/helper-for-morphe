package dev.rushi.apkdownloadhelper.ui.screen.helper

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.APK_PICKER_MIME_TYPES
import dev.rushi.apkdownloadhelper.BuildConfig
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.storage.DownloadHistoryEntry
import dev.rushi.apkdownloadhelper.storage.DownloadHistoryStore
import dev.rushi.apkdownloadhelper.storage.FastModePolicy
import dev.rushi.apkdownloadhelper.storage.HelperSettings
import dev.rushi.apkdownloadhelper.storage.RequestLogEntry
import dev.rushi.apkdownloadhelper.storage.orderedSources
import dev.rushi.apkdownloadhelper.ui.shared.appinfo.AppInfoCard
import dev.rushi.apkdownloadhelper.ui.shared.button.BoltButton
import dev.rushi.apkdownloadhelper.ui.shared.download.CheckingPickedFileState
import dev.rushi.apkdownloadhelper.ui.shared.download.DownloadingState
import dev.rushi.apkdownloadhelper.ui.shared.download.FastModeCard
import dev.rushi.apkdownloadhelper.ui.shared.download.ScanAskCard
import dev.rushi.apkdownloadhelper.ui.shared.download.ScanResultCard
import dev.rushi.apkdownloadhelper.ui.shared.download.isScanStatus
import dev.rushi.apkdownloadhelper.ui.shared.layout.AppWordmark
import dev.rushi.apkdownloadhelper.ui.shared.scroll.verticalScrollFade
import dev.rushi.apkdownloadhelper.ui.shared.settings.VirusTotalQuotaCard
import dev.rushi.apkdownloadhelper.ui.shared.source.PrimaryAction
import dev.rushi.apkdownloadhelper.ui.shared.source.SourceBottomBar
import dev.rushi.apkdownloadhelper.ui.shared.source.SourcePickerFlow
import dev.rushi.apkdownloadhelper.ui.shared.state.EmptyLaunchState
import dev.rushi.apkdownloadhelper.ui.shared.state.ErrorState
import dev.rushi.apkdownloadhelper.ui.shared.state.LoadingState
import dev.rushi.apkdownloadhelper.ui.shared.text.SectionTitle
import dev.rushi.apkdownloadhelper.ui.model.CandidateOption
import dev.rushi.apkdownloadhelper.ui.model.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.model.HelperRequest
import dev.rushi.apkdownloadhelper.ui.model.UiState
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults

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
    // VirusTotal API key, threaded into the scan-flow cards (ask, progress,
    // result) so quota is shown where scanning actually happens instead of
    // cluttering the home screen. Empty when VirusTotal is disabled.
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
    var showAppBrowser by remember { mutableStateOf(false) }
    val availableSources = settings.orderedSources.filter { it !in settings.disabledSources }
    var pendingFilePick by remember { mutableStateOf<DownloadCandidate?>(null) }
    var primaryAction by remember { mutableStateOf<PrimaryAction?>(null) }
    val context = LocalContext.current
    var historyEntries by remember { mutableStateOf<List<DownloadHistoryEntry>>(emptyList()) }
    val refreshHistory = {
        historyEntries = DownloadHistoryStore.entries(context)
    }
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val candidate = pendingFilePick
        pendingFilePick = null
        candidate?.let { onPickDownloadedFile(it, uri) }
    }
    val openDownloadedFilePicker: (DownloadCandidate) -> Unit = { candidate ->
        pendingFilePick = candidate
        filePickerLauncher.launch(APK_PICKER_MIME_TYPES)
    }
    BackHandler(enabled = showAppBrowser) {
        showAppBrowser = false
    }

    val flowVisible = request != null &&
        (state is UiState.Ready || (state is UiState.FastMode && state.progress.result != null))
    LaunchedEffect(flowVisible) {
        if (!flowVisible) primaryAction = null
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
        // A pushed screen covers the home, but the focus system has no idea: leaving
        // the home composed let a D-pad keep driving the hidden screen underneath, so
        // on a TV the settings page opened and then ignored every press (issue #22).
        // Fade it out and drop it from the tree, leaving nothing focusable behind.
        AnimatedVisibility(
            visible = !showAppBrowser,
            enter = fadeIn(tween(AppDefaults.ANIMATION_DURATION)),
            exit = fadeOut(tween(AppDefaults.ANIMATION_DURATION))
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val homeListState = rememberLazyListState()
            LazyColumn(
                state = homeListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScrollFade(homeListState)
                    .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = AppDefaults.ContentPadding, vertical = AppDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        AppWordmark(
                            modifier = Modifier.height(26.dp),
                            textColor = MaterialTheme.colorScheme.onBackground
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.helper),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                    BoltButton(
                        fastMode = settings.fastMode,
                        onClick = {
                            val enabled = !settings.fastMode
                            onSettingsChange(settings.copy(fastMode = enabled))
                            Toast.makeText(
                                context,
                                if (enabled) "Fast Mode on" else "Fast Mode off",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }

            if (request == null) {
                item {
                    EmptyLaunchState(onOpenMorphe = onOpenMorphe)
                }
                return@LazyColumn
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle(text = "App info", icon = Icons.Outlined.Smartphone)
                    AppInfoCard(
                        request = request,
                        onFormatToggle = onFormatToggle
                    )
                }
            }
            when (state) {
                UiState.Idle,
                UiState.Loading -> item { LoadingState() }

                is UiState.Ready -> {
                    item {
                        SourcePickerFlow(
                            request = request,
                            result = state.result,
                            selectedPagerPage = selectedPagerPage,
                            onPagerPageChanged = onPagerPageChanged,
                            onResolve = onResolve,
                            onDownload = onDownload,
                            onPickDownloadedFile = openDownloadedFilePicker,
                            onUseInstalledApp = onUseInstalledApp,
                            onSolveCaptcha = onSolveCaptcha,
                            onVersionHistory = onVersionHistory,
                            onDownloadVersion = onDownloadVersion,
                            onRefresh = onRefresh,
                            onCancel = onCancel,
                            installedPackageRefreshToken = installedPackageRefreshToken,
                            onPrimaryActionChanged = { primaryAction = it }
                        )
                    }
                }

                is UiState.CheckingPickedFile -> item { CheckingPickedFileState(state) }
                is UiState.Downloading -> {
                    item {
                        DownloadingState(
                            state = state,
                            availableSources = availableSources,
                            onCancel = onCancelDownload,
                            onPause = onPauseDownload,
                            onResume = onResumeDownload,
                            onSwitchSource = onSwitchDownloadSource,
                            onSkipWait = onSkipScanWait,
                            onSkipScan = onSkipScan
                        )
                    }
                    if (isScanStatus(state.statusMessage)) {
                        item { VirusTotalQuotaCard(virusTotalApiKey) }
                    }
                }
                is UiState.ScanAsk -> {
                    item {
                        ScanAskCard(
                            candidate = state.candidate,
                            onScan = onProceedAfterScan,
                            onSkip = onCancelAfterScan
                        )
                    }
                    item { VirusTotalQuotaCard(virusTotalApiKey) }
                }
                is UiState.ScanResult -> {
                    item {
                        ScanResultCard(
                            scanResult = state.scanResult,
                            detail = state.detail,
                            isMalicious = state.isMalicious,
                            onProceed = onProceedAfterScan,
                            onCancel = onCancelAfterScan,
                            shaVerified = state.shaVerified
                        )
                    }
                    item { VirusTotalQuotaCard(virusTotalApiKey) }
                }
                is UiState.Error -> item {
                    ErrorState(
                        message = state.message,
                        canRetryFailedSources = canRetryFailedSources,
                        onRetryAll = onRetryAll,
                        onRetryFailedSources = onRetryFailedSources,
                        onCopyDetails = onCopyFailureDetails,
                        onCancel = onCancel
                    )
                }

                is UiState.FastMode -> {
                    item {
                        FastModeCard(
                            progress = state.progress,
                            availableSources = availableSources,
                            onCancel = onCancelFastMode,
                            onPause = onPauseDownload,
                            onResume = onResumeDownload,
                            onSwitchSource = onSwitchDownloadSource,
                            onSkipWait = onSkipScanWait,
                            onUseMismatch = onUseFastModeMismatch,
                            onSkipMismatch = onSkipFastModeMismatch,
                            onSkipScan = onSkipScan,
                            onChooseVersion = onChooseVersion,
                            canRetryFailedSources = canRetryFailedSources,
                            onRetryAll = onRetryAll,
                            onRetryFailedSources = onRetryFailedSources,
                            onCopyFailureDetails = onCopyFailureDetails
                        )
                    }
                    if (isScanStatus(state.progress.detail)) {
                        item { VirusTotalQuotaCard(virusTotalApiKey) }
                    }
                    state.progress.result?.let { result ->
                        item {
                            SourcePickerFlow(
                                request = request,
                                result = result,
                                selectedPagerPage = selectedPagerPage,
                                onPagerPageChanged = onPagerPageChanged,
                                onResolve = onResolve,
                                onDownload = onDownload,
                                onPickDownloadedFile = openDownloadedFilePicker,
                                onUseInstalledApp = onUseInstalledApp,
                                onSolveCaptcha = onSolveCaptcha,
                                onVersionHistory = onVersionHistory,
                                onDownloadVersion = onDownloadVersion,
                                onRefresh = onRefresh,
                                onCancel = onCancel,
                                installedPackageRefreshToken = installedPackageRefreshToken,
                                onPrimaryActionChanged = { primaryAction = it }
                            )
                        }
                    }
                }
            }
        }
        val action = primaryAction
        if (action != null) {
            SourceBottomBar(action = action, onRefresh = onRefresh, onCancel = onCancel)
        }
        }
        }
        }
    }
}
