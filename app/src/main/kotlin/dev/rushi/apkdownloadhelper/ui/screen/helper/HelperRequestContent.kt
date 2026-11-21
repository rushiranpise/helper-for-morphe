package dev.rushi.apkdownloadhelper.ui.screen.helper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.storage.FastModePolicy
import dev.rushi.apkdownloadhelper.storage.HelperSettings
import dev.rushi.apkdownloadhelper.storage.orderedSources
import dev.rushi.apkdownloadhelper.ui.model.CandidateOption
import dev.rushi.apkdownloadhelper.ui.model.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.model.HelperRequest
import dev.rushi.apkdownloadhelper.ui.model.UiState
import dev.rushi.apkdownloadhelper.ui.shared.appinfo.AppInfoCard
import dev.rushi.apkdownloadhelper.ui.shared.download.CheckingPickedFileState
import dev.rushi.apkdownloadhelper.ui.shared.download.DownloadingState
import dev.rushi.apkdownloadhelper.ui.shared.download.FastModeCard
import dev.rushi.apkdownloadhelper.ui.shared.download.ScanAskCard
import dev.rushi.apkdownloadhelper.ui.shared.download.ScanResultCard
import dev.rushi.apkdownloadhelper.ui.shared.download.isScanStatus
import dev.rushi.apkdownloadhelper.ui.shared.scroll.verticalScrollFade
import dev.rushi.apkdownloadhelper.ui.shared.settings.VirusTotalQuotaCard
import dev.rushi.apkdownloadhelper.ui.shared.source.PrimaryAction
import dev.rushi.apkdownloadhelper.ui.shared.source.SourceBottomBar
import dev.rushi.apkdownloadhelper.ui.shared.source.SourcePickerFlow
import dev.rushi.apkdownloadhelper.ui.shared.state.ErrorState
import dev.rushi.apkdownloadhelper.ui.shared.state.LoadingState
import dev.rushi.apkdownloadhelper.ui.shared.text.SectionTitle
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults

//==================================================
// Morphe Request Flow (Screen when morphe requests)
//==================================================
@Composable
internal fun HelperRequestContent(
    request: HelperRequest,
    state: UiState,
    settings: HelperSettings,
    installedPackageRefreshToken: Int,
    selectedPagerPage: Int,
    onPagerPageChanged: (Int) -> Unit,
    onSettingsChange: (HelperSettings) -> Unit,
    onRefresh: () -> Unit,
    canRetryFailedSources: Boolean,
    onRetryAll: () -> Unit,
    onRetryFailedSources: () -> Unit,
    onCopyFailureDetails: () -> Unit,
    virusTotalApiKey: String,
    onResolve: (DownloadSource, CandidateOption) -> Unit,
    onDownload: (DownloadCandidate) -> Unit,
    onPickDownloadedFile: (DownloadCandidate) -> Unit,
    onUseInstalledApp: (DownloadCandidate) -> Unit,
    onVersionHistory: (DownloadSource) -> Unit,
    onDownloadVersion: (DownloadCandidate) -> Unit,
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
    onSolveCaptcha: (DownloadCandidate) -> Unit,
    onFormatToggle: (String) -> Unit,
    onProceedAfterScan: () -> Unit,
    onCancelAfterScan: () -> Unit,
    onSkipScan: () -> Unit
) {
    val availableSources = settings.orderedSources.filter { it !in settings.disabledSources }
    var primaryAction by remember { mutableStateOf<PrimaryAction?>(null) }

    val flowVisible = state is UiState.Ready || (state is UiState.FastMode && state.progress.result != null)
    LaunchedEffect(flowVisible) {
        if (!flowVisible) primaryAction = null
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScrollFade(listState)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = AppDefaults.ContentPadding, vertical = AppDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
        ) {
            item {
                HelperTopHeader(
                    settings = settings,
                    onSettingsChange = onSettingsChange
                )
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
                            onPickDownloadedFile = onPickDownloadedFile,
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
                                onPickDownloadedFile = onPickDownloadedFile,
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

        primaryAction?.let { action ->
            SourceBottomBar(action = action, onRefresh = onRefresh, onCancel = onCancel)
        }
    }
}