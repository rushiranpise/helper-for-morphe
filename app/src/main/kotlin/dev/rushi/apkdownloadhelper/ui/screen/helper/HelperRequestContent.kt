package dev.rushi.apkdownloadhelper.ui.screen.helper

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.download.isPackageInstalled
import dev.rushi.apkdownloadhelper.download.orderedFileKinds
import dev.rushi.apkdownloadhelper.storage.FastModePolicy
import dev.rushi.apkdownloadhelper.storage.HelperSettings
import dev.rushi.apkdownloadhelper.storage.orderedSources
import dev.rushi.apkdownloadhelper.ui.model.CandidateOption
import dev.rushi.apkdownloadhelper.ui.model.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.model.HelperRequest
import dev.rushi.apkdownloadhelper.ui.model.UiState
import dev.rushi.apkdownloadhelper.ui.shared.appcard.AppAvatar
import dev.rushi.apkdownloadhelper.ui.shared.button.AnimatedFab
import dev.rushi.apkdownloadhelper.ui.shared.button.GlassHeaderButton
import dev.rushi.apkdownloadhelper.ui.shared.chip.StatusBadge
import dev.rushi.apkdownloadhelper.ui.shared.dialog.AppAlertDialog
import dev.rushi.apkdownloadhelper.ui.shared.download.CheckingPickedFileState
import dev.rushi.apkdownloadhelper.ui.shared.download.DownloadingState
import dev.rushi.apkdownloadhelper.ui.shared.download.FastModeCard
import dev.rushi.apkdownloadhelper.ui.shared.download.ScanAskCard
import dev.rushi.apkdownloadhelper.ui.shared.download.ScanResultCard
import dev.rushi.apkdownloadhelper.ui.shared.download.isScanStatus
import dev.rushi.apkdownloadhelper.ui.shared.icon.ThemedIcon
import dev.rushi.apkdownloadhelper.ui.shared.scroll.LazyListScrollbar
import dev.rushi.apkdownloadhelper.ui.shared.settings.VirusTotalQuotaCard
import dev.rushi.apkdownloadhelper.ui.shared.source.PrimaryAction
import dev.rushi.apkdownloadhelper.ui.shared.source.SourceBottomBar
import dev.rushi.apkdownloadhelper.ui.shared.source.SourcePickerDialog
import dev.rushi.apkdownloadhelper.ui.shared.source.rememberSourcePickerState
import dev.rushi.apkdownloadhelper.ui.shared.source.sourcePickerItems
import dev.rushi.apkdownloadhelper.ui.shared.state.ErrorState
import dev.rushi.apkdownloadhelper.ui.shared.state.LoadingState
import dev.rushi.apkdownloadhelper.ui.theme.AppColors
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.LocalMonochromeTheme
import dev.rushi.apkdownloadhelper.ui.theme.SemanticTone
import dev.rushi.apkdownloadhelper.ui.theme.glassFill
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Stable
private class RequestHeaderCollapse {
    var folded by mutableFloatStateOf(0f)
        private set

    private var range = 0
    private var drags = 0

    val connection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.UserInput) drags++
            return Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            folded = (folded - consumed.y - available.y.coerceAtLeast(0f)).coerceIn(0f, range.toFloat())
            return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (folded > 0f && folded < range) {
                easeTo(if (folded < range / 2f) 0f else range.toFloat())
            }
            return Velocity.Zero
        }
    }

    private suspend fun easeTo(target: Float) {
        val drag = drags
        animate(folded, target, animationSpec = tween(220)) { value, _ ->
            if (drags == drag) folded = value
        }
    }

    suspend fun unfold() {
        if (folded > 0f) easeTo(0f)
    }

    fun measured(height: Int) {
        range = height
        if (folded > height) folded = height.toFloat()
    }
}

private fun Modifier.foldsWith(collapse: RequestHeaderCollapse): Modifier =
    clipToBounds().layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        collapse.measured(placeable.height)
        val height = (placeable.height - collapse.folded.roundToInt()).coerceAtLeast(0)
        layout(placeable.width, height) {
            placeable.place(0, height - placeable.height)
        }
    }

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

    val activeResult = when (state) {
        is UiState.Ready -> state.result
        is UiState.FastMode -> state.progress.result
        else -> null
    }
    LaunchedEffect(activeResult != null) {
        if (activeResult == null) primaryAction = null
    }

    val pickerState = activeResult?.let { result ->
        rememberSourcePickerState(
            request = request,
            result = result,
            selectedPagerPage = selectedPagerPage,
            onPagerPageChanged = onPagerPageChanged,
            onResolve = onResolve,
            onDownload = onDownload,
            onVersionHistory = onVersionHistory,
            onSolveCaptcha = onSolveCaptcha,
            onPrimaryActionChanged = { primaryAction = it }
        )
    }

    var confirmFastModeTarget by remember { mutableStateOf<Boolean?>(null) }

    val context = LocalContext.current
    val density = LocalDensity.current
    val scheme = MaterialTheme.colorScheme
    val monochrome = LocalMonochromeTheme.current
    val listState = rememberLazyListState()
    val pageHaze = remember { HazeState() }
    val collapse = remember(request.packageName) { RequestHeaderCollapse() }
    val scope = rememberCoroutineScope()

    var unfoldedHeaderHeightDp by remember { mutableStateOf(0.dp) }
    var footerHeightDp by remember { mutableStateOf(0.dp) }

    val formatsLocked = state is UiState.Downloading ||
        state is UiState.CheckingPickedFile ||
        state is UiState.ScanAsk ||
        state is UiState.ScanResult ||
        (state is UiState.FastMode && !state.progress.done)
    val formatCardAlpha by animateFloatAsState(
        targetValue = if (formatsLocked) 0.42f else 1f,
        animationSpec = tween(200),
        label = "accepted_formats_alpha"
    )
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

    val isAtTop by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }
    LaunchedEffect(isAtTop) {
        if (isAtTop) collapse.unfold()
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

    pickerState?.let { SourcePickerDialog(state = it) }

    confirmFastModeTarget?.let { enableFastMode ->
        AppAlertDialog(
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
                TextButton(
                    onClick = {
                        confirmFastModeTarget = null
                        onSettingsChange(settings.copy(fastMode = enableFastMode))
                    }
                ) {
                    Text(if (enableFastMode) "Start Fast Mode" else "Turn off")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmFastModeTarget = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(collapse.connection)
    ) {
        // 1. Full-screen LazyColumn scrolling behind the frosted header & bottom bar
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = pageHaze),
            contentPadding = PaddingValues(
                top = unfoldedHeaderHeightDp + 12.dp,
                start = AppDefaults.ContentPadding,
                end = AppDefaults.ContentPadding,
                bottom = (if (primaryAction != null) footerHeightDp else 0.dp) + 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
        ) {
            val allKinds = request.availableFileKinds.orderedFileKinds()
            if (allKinds.size > 1) {
                item(key = "format_selector") {
                    val outerRadius = 20.dp
                    val innerRadius = 6.dp
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(formatCardAlpha)
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
                                    enabled = !formatsLocked,
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
                    pickerState?.let { picker ->
                        sourcePickerItems(
                            state = picker,
                            request = request,
                            onDownload = onDownload,
                            onPickDownloadedFile = onPickDownloadedFile,
                            onUseInstalledApp = onUseInstalledApp,
                            onSolveCaptcha = onSolveCaptcha,
                            onDownloadVersion = onDownloadVersion,
                            installedPackageRefreshToken = installedPackageRefreshToken
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
                    pickerState?.let { picker ->
                        sourcePickerItems(
                            state = picker,
                            request = request,
                            onDownload = onDownload,
                            onPickDownloadedFile = onPickDownloadedFile,
                            onUseInstalledApp = onUseInstalledApp,
                            onSolveCaptcha = onSolveCaptcha,
                            onDownloadVersion = onDownloadVersion,
                            installedPackageRefreshToken = installedPackageRefreshToken
                        )
                    }
                }
            }
        }

        // 2. Smooth scrollbar with fixed top offset so it doesn't expand when the header pills hide
        LazyListScrollbar(
            listState = listState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .padding(
                    top = unfoldedHeaderHeightDp,
                    bottom = if (primaryAction != null) footerHeightDp else 0.dp
                )
                .then(if (primaryAction == null) Modifier.navigationBarsPadding() else Modifier)
        )

        // 3. Go-to-top FAB
        AnimatedFab(
            visible = listState.firstVisibleItemIndex > 1,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .then(if (primaryAction == null) Modifier.navigationBarsPadding() else Modifier)
                .padding(
                    end = 20.dp,
                    bottom = (if (primaryAction != null) footerHeightDp else 0.dp) + 12.dp
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

        // 4. AppDetailScreen-style Frosted Hero Header with scroll-collapsing pill bar
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onSizeChanged { size ->
                    val measuredDp = with(density) { size.height.toDp() }
                    if (collapse.folded == 0f && measuredDp > 0.dp) {
                        unfoldedHeaderHeightDp = measuredDp
                    }
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
            Column(modifier = Modifier.fillMaxWidth()) {
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

                // Single-row horizontally scrollable badges that fold away on scroll
                val badgeFill = if (monochrome) {
                    scheme.surfaceVariant.copy(alpha = 0.65f)
                } else {
                    lerp(scheme.surfaceVariant, scheme.primary, 0.25f).copy(alpha = 0.65f)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .foldsWith(collapse)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadge(
                            text = request.requestedVersionName ?: "Any",
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

        // 6. Pinned Bottom Frosted Action Bar
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