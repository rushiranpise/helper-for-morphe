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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.chrisbanes.haze.hazeSource
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.ui.shared.appcard.AppAvatar
import dev.rushi.apkdownloadhelper.ui.shared.button.AnimatedFab
import dev.rushi.apkdownloadhelper.ui.shared.button.GlassHeaderButton
import dev.rushi.apkdownloadhelper.ui.shared.chip.StatusBadge
import dev.rushi.apkdownloadhelper.ui.shared.icon.ThemedIcon
import dev.rushi.apkdownloadhelper.ui.shared.scroll.LazyListScrollbar
import dev.rushi.apkdownloadhelper.ui.theme.AppColors
import dev.rushi.apkdownloadhelper.ui.theme.LocalMonochromeTheme
import dev.rushi.apkdownloadhelper.ui.theme.SemanticTone
import dev.rushi.apkdownloadhelper.ui.theme.glassFill
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.surfaceColorAtElevation
import dev.rushi.apkdownloadhelper.download.isPackageInstalled
import dev.rushi.apkdownloadhelper.download.orderedFileKinds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

//==================================================
// Morphe Request Flow (Screen when morphe requests)
//==================================================
@OptIn(ExperimentalLayoutApi::class)
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

    var confirmFastModeTarget by remember { mutableStateOf<Boolean?>(null) }

    val context = LocalContext.current
    val density = LocalDensity.current
    val scheme = MaterialTheme.colorScheme
    val monochrome = LocalMonochromeTheme.current
    val listState = rememberLazyListState()
    val pageHaze = remember { HazeState() }
    val scope = rememberCoroutineScope()

    var headerHeightDp by remember { mutableStateOf(0.dp) }
    var footerHeightDp by remember { mutableStateOf(0.dp) }
    var copied by remember(request.packageName) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }
    var installed by remember(request.packageName) { mutableStateOf(false) }
    LaunchedEffect(request.packageName, installedPackageRefreshToken) {
        installed = withContext(Dispatchers.IO) {
            context.isPackageInstalled(request.packageName)
        }
    }

    val isScrolled by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    val headerShape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
    val tintAlpha = if (isScrolled) 0.52f else 0.76f
    val bandTint = if (monochrome) {
        scheme.surfaceColorAtElevation(3.dp).copy(alpha = tintAlpha)
    } else {
        lerp(scheme.surfaceColorAtElevation(2.dp), scheme.primary, 0.18f).copy(alpha = tintAlpha)
    }

    confirmFastModeTarget?.let { enableFastMode ->
        dev.rushi.apkdownloadhelper.ui.shared.dialog.AppAlertDialog(
            onDismissRequest = { confirmFastModeTarget = null },
            title = {
                Text(
                    text = if (enableFastMode) "Switch to Fast Mode?" else "Turn off Fast Mode?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (enableFastMode) {
                        "Current background actions will stop and Fast Mode will immediately search enabled sources for this app."
                    } else {
                        "Current Fast Mode search or download will stop and switch back to manual source selection."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        confirmFastModeTarget = null
                        onSettingsChange(settings.copy(fastMode = enableFastMode))
                    }
                ) {
                    Text(if (enableFastMode) "Start Fast Mode" else "Turn off")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { confirmFastModeTarget = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Full-screen LazyColumn scrolling behind the frosted header & bottom bar
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = pageHaze),
            contentPadding = PaddingValues(
                top = headerHeightDp + 12.dp,
                start = AppDefaults.ContentPadding,
                end = AppDefaults.ContentPadding,
                bottom = (if (primaryAction != null) footerHeightDp else 0.dp) + 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
        ) {
            // Interactive Format Selector Card (replaces duplicated AppInfoCard)
            val allKinds = request.availableFileKinds.orderedFileKinds()
            if (allKinds.size > 1) {
                item(key = "format_selector") {
                    val outerRadius = 20.dp
                    val innerRadius = 6.dp
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(scheme.surfaceColorAtElevation(2.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Accepted formats",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onSurface
                            )
                            val selectedCount = allKinds.count { it in request.requestedFileKinds }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(scheme.primaryContainer.copy(alpha = 0.55f))
                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (selectedCount == allKinds.size) "All ($selectedCount)" else "$selectedCount of ${allKinds.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onPrimaryContainer
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            allKinds.forEachIndexed { idx, kind ->
                                val selected = kind in request.requestedFileKinds
                                val isFirst = idx == 0
                                val isLast = idx == allKinds.lastIndex
                                val pillShape = RoundedCornerShape(
                                    topStart = if (isFirst) outerRadius else innerRadius,
                                    bottomStart = if (isFirst) outerRadius else innerRadius,
                                    topEnd = if (isLast) outerRadius else innerRadius,
                                    bottomEnd = if (isLast) outerRadius else innerRadius
                                )
                                Surface(
                                    onClick = { onFormatToggle(kind) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    shape = pillShape,
                                    color = if (selected) {
                                        scheme.primaryContainer
                                    } else {
                                        scheme.surfaceVariant.copy(alpha = 0.42f)
                                    },
                                    contentColor = if (selected) {
                                        scheme.onPrimaryContainer
                                    } else {
                                        scheme.onSurfaceVariant
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (selected) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Text(
                                            text = kind.uppercase(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            when (state) {
                UiState.Idle,
                UiState.Loading -> item(key = "loading") { LoadingState() }

                is UiState.Ready -> {
                    item(key = "source_picker") {
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

                is UiState.CheckingPickedFile -> item(key = "checking_file") {
                    CheckingPickedFileState(state)
                }

                is UiState.Downloading -> {
                    item(key = "downloading") {
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
                        item(key = "vt_quota_downloading") {
                            VirusTotalQuotaCard(virusTotalApiKey)
                        }
                    }
                }

                is UiState.ScanAsk -> {
                    item(key = "scan_ask") {
                        ScanAskCard(
                            candidate = state.candidate,
                            onScan = onProceedAfterScan,
                            onSkip = onCancelAfterScan
                        )
                    }
                    item(key = "vt_quota_ask") {
                        VirusTotalQuotaCard(virusTotalApiKey)
                    }
                }

                is UiState.ScanResult -> {
                    item(key = "scan_result") {
                        ScanResultCard(
                            scanResult = state.scanResult,
                            detail = state.detail,
                            isMalicious = state.isMalicious,
                            onProceed = onProceedAfterScan,
                            onCancel = onCancelAfterScan,
                            shaVerified = state.shaVerified
                        )
                    }
                    item(key = "vt_quota_result") {
                        VirusTotalQuotaCard(virusTotalApiKey)
                    }
                }

                is UiState.Error -> item(key = "error") {
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
                    item(key = "fast_mode") {
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
                        item(key = "vt_quota_fastmode") {
                            VirusTotalQuotaCard(virusTotalApiKey)
                        }
                    }
                    state.progress.result?.let { result ->
                        item(key = "fast_mode_fallback_picker") {
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

        // 2. Scrollbar matching AppBrowserScreen
        LazyListScrollbar(
            listState = listState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .padding(top = headerHeightDp)
                .navigationBarsPadding()
        )

        // 3. Go-to-top FAB matching AppBrowserScreen
        AnimatedFab(
            visible = listState.firstVisibleItemIndex > 0,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(
                    end = 20.dp,
                    bottom = (if (primaryAction != null) footerHeightDp else 0.dp) + 20.dp
                )
        ) {
            Surface(
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                shape = CircleShape,
                color = SemanticTone.Primary.container,
                contentColor = SemanticTone.Primary.content
            ) {
                ThemedIcon(
                    icon = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.go_to_top),
                    tint = SemanticTone.Primary.content,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // 4. AppDetailScreen-style Frosted Hero Header (No format pill here—Version, Build & ABI only)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onSizeChanged { size ->
                    headerHeightDp = with(density) { size.height.toDp() }
                }
                .clip(headerShape)
                .glassFill(
                    glass = AppColors.glassContainerColor(),
                    shape = headerShape,
                    hazeState = pageHaze
                )
                .background(bandTint)
                .statusBarsPadding()
                .padding(
                    horizontal = AppDefaults.ContentPadding,
                    vertical = 12.dp
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppAvatar(
                        packageName = request.packageName,
                        initial = request.appName.firstOrNull()?.uppercaseChar() ?: '?'
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = request.appName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .basicMarquee()
                            )
                            if (installed) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = stringResource(R.string.installed),
                                    tint = scheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("package", request.packageName))
                                    copied = true
                                    Toast.makeText(context, "Package copied", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = request.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Icon(
                                imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(R.string.copy_package),
                                tint = if (copied) scheme.primary else scheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    GlassHeaderButton(
                        icon = Icons.Outlined.Bolt,
                        contentDescription = if (settings.fastMode) "Fast Mode on" else "Fast Mode off",
                        active = settings.fastMode,
                        onClick = { confirmFastModeTarget = !settings.fastMode }
                    )
                }

                // Single-row horizontally scrollable StatusBadges (Version, Build, ABIs)
                val badgeFill = if (monochrome) {
                    scheme.surfaceVariant.copy(alpha = 0.65f)
                } else {
                    lerp(scheme.surfaceVariant, scheme.primary, 0.25f).copy(alpha = 0.65f)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(
                        text = "Version ${request.requestedVersionName ?: "Any"}",
                        icon = Icons.Outlined.History,
                        containerColor = badgeFill,
                        contentColor = scheme.onSurface
                    )
                    request.versionCodeSummary?.let { code ->
                        StatusBadge(
                            text = "Build $code",
                            containerColor = badgeFill,
                            contentColor = scheme.onSurface
                        )
                    }
                    request.availableAbis.firstOrNull()?.let { primaryAbi ->
                        StatusBadge(
                            text = primaryAbi,
                            icon = Icons.Outlined.Memory,
                            containerColor = badgeFill,
                            contentColor = scheme.onSurface
                        )
                    }
                }
            }
        }

        // 5. Telegram-style bottom dim scrim
        val navBarHeight = with(density) {
            WindowInsets.navigationBars.getBottom(this).toDp()
        }
        if (primaryAction != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(navBarHeight + 76.dp)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.45f to scheme.background.copy(alpha = 0.55f),
                            0.80f to scheme.background.copy(alpha = 0.85f),
                            1.0f to scheme.background.copy(alpha = 0.96f)
                        )
                    )
            )
        }

        // 6. Pinned Bottom Frosted Action Bar (Haze blur on Refresh, Close & Primary pill)
        primaryAction?.let { action ->
            SourceBottomBar(
                action = action,
                onRefresh = onRefresh,
                onCancel = onCancel,
                hazeState = pageHaze,
                onHeightChanged = { footerHeightDp = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}