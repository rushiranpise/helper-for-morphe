package dev.rushi.apkdownloadhelper.ui.screen.helper

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import dev.rushi.apkdownloadhelper.ui.shared.button.BottomCloseBar
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.GppBad
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import dev.chrisbanes.haze.HazeState
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.download.VirusTotalScanner
import dev.rushi.apkdownloadhelper.formatHistoryTimestamp
import dev.rushi.apkdownloadhelper.ui.shared.appcard.FindAppIcon
import dev.rushi.apkdownloadhelper.ui.shared.appcard.findAppCardColors
import dev.rushi.apkdownloadhelper.ui.shared.button.DialogButton
import dev.rushi.apkdownloadhelper.ui.shared.chip.StatusBadge
import dev.rushi.apkdownloadhelper.ui.shared.dialog.AppAlertDialog
import dev.rushi.apkdownloadhelper.ui.shared.download.ScanResultCard
import dev.rushi.apkdownloadhelper.ui.shared.download.scanResultDetail
import dev.rushi.apkdownloadhelper.ui.shared.layout.AppDivider
import dev.rushi.apkdownloadhelper.ui.shared.shape.rememberPressShape
import dev.rushi.apkdownloadhelper.ui.theme.AppColors
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.LocalMonochromeTheme
import dev.rushi.apkdownloadhelper.ui.theme.SemanticTone
import dev.rushi.apkdownloadhelper.ui.theme.glassFill
import dev.rushi.apkdownloadhelper.ui.theme.hazeSourceIfEnabled
import dev.rushi.apkdownloadhelper.ui.theme.pressScale
import dev.rushi.apkdownloadhelper.ui.theme.rememberMorphePillShape
import dev.rushi.apkdownloadhelper.ui.util.formatBytes
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private val LocalStoredDetailAccent = compositionLocalOf<Color?> { null }

@Stable
private class StoredDetailHeaderCollapse {
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

private fun Modifier.foldsWith(collapse: StoredDetailHeaderCollapse): Modifier =
    clipToBounds().layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        collapse.measured(placeable.height)
        val height = (placeable.height - collapse.folded.roundToInt()).coerceAtLeast(0)
        layout(placeable.width, height) {
            placeable.place(0, height - placeable.height)
        }
    }

/** Like statusBarsPadding(), but never shrinks when the system temporarily reports a 0 inset
 *  (e.g. while the share sheet or another window has focus). */
@Composable
private fun Modifier.stableStatusBarsPadding(): Modifier {
    val density = LocalDensity.current
    val current = WindowInsets.statusBars.getTop(density)
    val stable = remember { intArrayOf(0) }
    if (current > stable[0]) stable[0] = current
    return this.padding(top = with(density) { stable[0].toDp() })
}
@Composable
internal fun StoredAppDetailScreen(
    item: StoredApkCardItem,
    useGradientHeader: Boolean = true,
    onOpenFile: (StoredApkCardItem) -> Unit,
    onOpenFolder: (StoredApkCardItem) -> Unit,
    onExportFile: (StoredApkCardItem) -> Unit,
    onShareFile: (StoredApkCardItem) -> Unit,
    onDeleteFile: (StoredApkCardItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val monochrome = LocalMonochromeTheme.current
    val darkTheme = isSystemInDarkTheme()
    val scheme = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val headerHaze = remember { HazeState() }
    val collapse = remember(item.id) { StoredDetailHeaderCollapse() }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showScanDialog by remember(item.id) { mutableStateOf(false) }

    BackHandler {
        when {
            showScanDialog -> showScanDialog = false
            showDeleteConfirm -> showDeleteConfirm = false
            else -> onBack()
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

    var unfoldedHeaderHeightDp by remember { mutableStateOf(0.dp) }
    var footerHeightDp by remember { mutableStateOf(0.dp) }

    // Morphe DialogHeaderEntrance clocks
    var headerEntered by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(item.id) { headerEntered = true }
    val entranceTarget = if (headerEntered) 1f else 0f

    val iconEntrance by animateFloatAsState(
        targetValue = entranceTarget,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 320f),
        label = "stored_header_icon_entrance"
    )
    val textEntrance by animateFloatAsState(
        targetValue = entranceTarget,
        animationSpec = tween(durationMillis = 260, delayMillis = 60, easing = EaseOutCubic),
        label = "stored_header_text_entrance"
    )
    val badgesEntrance by animateFloatAsState(
        targetValue = entranceTarget,
        animationSpec = tween(durationMillis = 240, delayMillis = 160, easing = EaseOutBack),
        label = "stored_header_badges_entrance"
    )

    var pkgCopied by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(pkgCopied) {
        if (pkgCopied) {
            delay(1500)
            pkgCopied = false
        }
    }

    val appPalette = remember(item.app.iconColor, item.app.packageName) { item.app.findAppCardColors() }
    val useAppAccent = useGradientHeader && !monochrome
    val accent = if (useAppAccent) appPalette.gradient[1] else scheme.primary

    val headerBandShape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
    val isScrolled by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    val tintAlpha = if (isScrolled) 0.48f else 0.72f
    val bandTint = when {
        monochrome -> scheme.surfaceColorAtElevation(3.dp).copy(alpha = tintAlpha)
        !useGradientHeader -> lerp(scheme.surfaceColorAtElevation(2.dp), scheme.primary, 0.24f).copy(alpha = tintAlpha)
        else -> lerp(scheme.surfaceColorAtElevation(2.dp), accent, 0.22f).copy(alpha = tintAlpha)
    }

    if (showDeleteConfirm) {
        AppAlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "Delete file?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text("Delete ${item.fileName} and its saved details?") },
            confirmButton = {
                DialogButton(
                    text = "Delete",
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteFile(item)
                    },
                    isDestructive = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            dismissButton = {
                DialogButton(
                    text = "Cancel",
                    onClick = { showDeleteConfirm = false },
                    filled = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }

    val savedScanResult = item.scanVerdict?.result

    CompositionLocalProvider(LocalStoredDetailAccent provides accent) {
      Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.background)
                .nestedScroll(collapse.connection)
        ) {
            // 1. Scrollable Morphe Details Body
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSourceIfEnabled(headerHaze),
                contentPadding = PaddingValues(
                    top = unfoldedHeaderHeightDp + 12.dp,
                    start = AppDefaults.ContentPadding,
                    end = AppDefaults.ContentPadding,
                    bottom = footerHeightDp + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Morphe InfoPanel card (CornerLargeIncreased = 20.dp, 1.dp border)
                item(key = "info_panel") {
                    val infoRows = buildList {
                        add(
                            StoredInfoRowSpec(
                                icon = Icons.Outlined.Android,
                                label = "Package name",
                                value = item.app.packageName,
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("package", item.app.packageName))
                                    pkgCopied = true
                                    Toast.makeText(context, "Package copied", Toast.LENGTH_SHORT).show()
                                },
                                trailingIcon = if (pkgCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy
                            )
                        )
                        val versionText = buildString {
                            append(item.versionName ?: "Unknown")
                            item.versionCode?.let { append(" ($it)") }
                        }
                        add(StoredInfoRowSpec(Icons.Outlined.History, "Version", versionText))
                        add(
                            StoredInfoRowSpec(
                                Icons.Outlined.Description,
                                "Format",
                                item.fileKind.uppercase(Locale.US)
                            )
                        )
                        if (item.sizeBytes > 0L) {
                            add(
                                StoredInfoRowSpec(
                                    Icons.Outlined.Storage,
                                    if (item.fileKind == "apk") "APK size" else "Bundle size",
                                    item.sizeBytes.formatBytes()
                                )
                            )
                        }
                        add(StoredInfoRowSpec(Icons.Outlined.CloudDownload, "Source", item.sourceName))
                        add(StoredInfoRowSpec(Icons.Outlined.SdStorage, "Storage location", item.locationLabel))
                        add(
                            StoredInfoRowSpec(
                                Icons.Outlined.CalendarToday,
                                "Downloaded on",
                                formatHistoryTimestamp(item.timestamp)
                            )
                        )
                        add(StoredInfoRowSpec(Icons.Outlined.FolderOpen, "File name", item.fileName))
                        if (item.abis.isNotEmpty()) {
                            add(
                                StoredInfoRowSpec(
                                    Icons.Outlined.Memory,
                                    "CPU architecture",
                                    item.abis.joinToString(" · ")
                                )
                            )
                        }
                        item.minSdkVersion?.let { sdk ->
                            add(StoredInfoRowSpec(Icons.Outlined.Android, "Minimum Android", "API $sdk"))
                        }
                        item.obbSummary?.let { obb ->
                            add(StoredInfoRowSpec(Icons.Outlined.SdStorage, "Game data (OBB)", obb))
                        }
                        add(
                            StoredInfoRowSpec(
                                Icons.Outlined.CheckCircle,
                                "Device status",
                                if (item.installed) "Installed on device" else "Not installed"
                            )
                        )
                        val scanVerdict = item.scanVerdict
                        if (scanVerdict != null) {
                            add(
                                StoredInfoRowSpec(
                                    icon = when {
                                        scanVerdict.malicious -> Icons.Outlined.GppBad
                                        scanVerdict.failed -> Icons.Outlined.WarningAmber
                                        else -> Icons.Outlined.VerifiedUser
                                    },
                                    label = "VirusTotal scan",
                                    value = when {
                                        scanVerdict.malicious -> "Threats detected"
                                        scanVerdict.failed -> "Scan error"
                                        else -> "No threats found"
                                    },
                                    actions = if (scanVerdict.result != null) {
                                        listOf(
                                            StoredRowAction(
                                                Icons.AutoMirrored.Outlined.List,
                                                "View scan report"
                                            ) { showScanDialog = true }
                                        )
                                    } else emptyList()
                                )
                            )
                        }
                    }

                    // Morphe SurfaceCard: CornerLarge 16.dp, cardFill, accent border
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = scheme.surfaceColorAtElevation(3.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            infoRows.forEachIndexed { index, row ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = scheme.outlineVariant
                                    )
                                }
                                StoredDetailInfoRow(row, accent = accent)
                            }
                        }
                    }
                }

                // Connected action pill groups
                item(key = "morphe_action_pills") {
                    val neutralFill = scheme.primary.copy(alpha = 0.30f)
                    val destructiveRed = if (darkTheme) Color(0xFFFF6B6B) else Color(0xFFD32F2F)
                    val launchIntent = remember(item.app.packageName, item.installed) {
                        if (item.installed) {
                            context.packageManager.getLaunchIntentForPackage(item.app.packageName)
                        } else null
                    }
                    val destructiveVeil = scheme.onBackground.copy(alpha = if (darkTheme) 0.08f else 0.10f)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Public file -> open its folder. Private file -> export it.
                            ConnectedPill(
                                onClick = {
                                    if (item.isPrivate) onExportFile(item) else onOpenFolder(item)
                                },
                                icon = if (item.isPrivate) Icons.Outlined.FileUpload else Icons.Outlined.FolderOpen,
                                label = if (item.isPrivate) "Export" else "Open folder",
                                slot = PillSlot.First,
                                containerColor = neutralFill,
                                contentColor = scheme.onBackground,
                                modifier = Modifier.weight(2.4f)
                            )
                            if (launchIntent != null) {
                                ConnectedPill(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        }
                                    },
                                    icon = Icons.Outlined.OpenInNew,
                                    description = "Open app",
                                    slot = PillSlot.Middle,
                                    containerColor = neutralFill,
                                    contentColor = scheme.onBackground,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            ConnectedPill(
                                onClick = { onShareFile(item) },
                                icon = Icons.Outlined.Share,
                                description = "Share",
                                slot = PillSlot.Last,
                                containerColor = neutralFill,
                                contentColor = scheme.onBackground,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (item.installed) {
                                ConnectedPill(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_DELETE, Uri.parse("package:${item.app.packageName}"))
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        }
                                    },
                                    icon = Icons.Outlined.RemoveCircleOutline,
                                    label = "Uninstall",
                                    slot = PillSlot.First,
                                    containerColor = destructiveVeil,
                                    contentColor = destructiveRed,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            ConnectedPill(
                                onClick = { showDeleteConfirm = true },
                                icon = Icons.Outlined.DeleteOutline,
                                label = "Delete",
                                slot = if (item.installed) PillSlot.Last else PillSlot.Only,
                                containerColor = destructiveVeil,
                                contentColor = destructiveRed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }


            }

            // 2. Morphe ListDialogHeader Band
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
                    .clip(headerBandShape)
                    .glassFill(
                        glass = AppColors.glassContainerColor(),
                        shape = headerBandShape,
                        hazeState = headerHaze
                    )
                    .background(bandTint)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .stableStatusBarsPadding()
                        .padding(
                            horizontal = AppDefaults.ContentPadding,
                            vertical = 14.dp
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .graphicsLayer {
                                    val s = lerp(0.6f, 1f, iconEntrance)
                                    scaleX = s
                                    scaleY = s
                                    alpha = iconEntrance.coerceIn(0f, 1f)
                                }
                        ) {
                            FindAppIcon(
                                app = item.app,
                                gradient = appPalette.gradient,
                                installed = item.installed,
                                size = 48.dp,
                                cornerRadius = 14.dp
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = item.app.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier
                                    .graphicsLayer {
                                        translationX = lerp(40f, 0f, textEntrance)
                                        alpha = textEntrance.coerceIn(0f, 1f)
                                    }
                                    .basicMarquee()
                            )
                            val subtitleProgress = ((textEntrance - 0.15f) / 0.85f).coerceIn(0f, 1f)
                            val headerSubtitle = item.versionName?.let { ver ->
                                if (ver.startsWith("v", ignoreCase = true)) ver else "v$ver"
                            } ?: item.app.packageName
                            Text(
                                text = headerSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier
                                    .graphicsLayer {
                                        translationX = lerp(40f, 0f, subtitleProgress)
                                        alpha = subtitleProgress
                                    }
                                    .basicMarquee()
                            )
                        }
                    }

                    // Collapsible status badge strip
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .foldsWith(collapse)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .horizontalScroll(rememberScrollState())
                                .graphicsLayer {
                                    translationY = lerp(20f, 0f, badgesEntrance)
                                    alpha = badgesEntrance.coerceIn(0f, 1f)
                                },
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val badgeFill = if (monochrome) {
                                scheme.surfaceVariant.copy(alpha = 0.65f)
                            } else {
                                lerp(scheme.surfaceVariant, accent, 0.25f).copy(alpha = 0.65f)
                            }
                            StatusBadge(
                                text = item.fileKind.uppercase(Locale.US),
                                icon = Icons.Outlined.Description,
                                containerColor = badgeFill,
                                contentColor = scheme.onSurface
                            )
                            StatusBadge(
                                text = android.text.format.DateUtils.getRelativeTimeSpanString(
                                    item.timestamp,
                                    System.currentTimeMillis(),
                                    android.text.format.DateUtils.MINUTE_IN_MILLIS
                                ).toString(),
                                icon = Icons.Outlined.History,
                                containerColor = badgeFill,
                                contentColor = scheme.onSurface
                            )

                        }
                    }
                }
            }

            // Bottom gradient scrim
            val navHeight = with(density) {
                WindowInsets.navigationBars.getBottom(this).toDp()
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(navHeight + 80.dp)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.45f to scheme.background.copy(alpha = 0.55f),
                            0.80f to scheme.background.copy(alpha = 0.85f),
                            1.0f to scheme.background.copy(alpha = 0.96f)
                        )
                    )
            )

            // 3. Morphe Pinned Frosted Close Footer (52.dp DialogButtonHeight, AccentAlpha.BORDER outline)
            val closeInteraction = remember { MutableInteractionSource() }
            val closeShape = rememberMorphePillShape(closeInteraction)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        footerHeightDp = with(density) { size.height.toDp() }
                    }
                    .navigationBarsPadding()
                    .padding(horizontal = AppDefaults.ContentPadding, vertical = 14.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .pressScale(closeInteraction, pressedScale = 0.96f)
                        .glassFill(
                            glass = Color.Transparent,
                            shape = closeShape,
                            hazeState = headerHaze
                        ),
                    interactionSource = closeInteraction,
                    shape = closeShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = scheme.onSurface.copy(alpha = 0.85f)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.close),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Full-screen VirusTotal report
        if (showScanDialog && savedScanResult != null) {
            StoredScanReportScreen(
                item = item,
                result = savedScanResult,
                onClose = { showScanDialog = false },
                modifier = Modifier.fillMaxSize()
            )
        }
      }
    }
}

private enum class PillSlot { Only, First, Middle, Last }

private val PillGroupHeight = 52.dp

/** Pill in a connected group: round outer corners, small inner corners, morphs on press. */
@Composable
private fun ConnectedPill(
    onClick: () -> Unit,
    icon: ImageVector,
    slot: PillSlot,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    label: String? = null,
    description: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val startOuter = slot == PillSlot.Only || slot == PillSlot.First
    val endOuter = slot == PillSlot.Only || slot == PillSlot.Last
    val restOuter = 26.dp
    val restInner = 6.dp
    val pressOuter = 16.dp
    val pressInner = 4.dp

    val start by animateDpAsState(
        targetValue = if (startOuter) (if (pressed) pressOuter else restOuter)
        else (if (pressed) pressInner else restInner),
        animationSpec = tween(140),
        label = "pill_start_corner"
    )
    val end by animateDpAsState(
        targetValue = if (endOuter) (if (pressed) pressOuter else restOuter)
        else (if (pressed) pressInner else restInner),
        animationSpec = tween(140),
        label = "pill_end_corner"
    )
    val shape = RoundedCornerShape(
        topStart = start,
        bottomStart = start,
        topEnd = end,
        bottomEnd = end
    )
    val animatedContainer by animateColorAsState(containerColor, label = "pill_container")
    val animatedContent by animateColorAsState(contentColor, label = "pill_content")

    Surface(
        onClick = onClick,
        shape = shape,
        color = animatedContainer,
        contentColor = animatedContent,
        interactionSource = interactionSource,
        modifier = modifier
            .height(PillGroupHeight)
            .semantics { role = Role.Button }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                modifier = Modifier.size(20.dp)
            )
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Full-screen scan report: blur-on-scroll header + the universal BottomCloseBar. */
@Composable
private fun StoredScanReportScreen(
    item: StoredApkCardItem,
    result: VirusTotalScanner.ScanResult,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scheme = MaterialTheme.colorScheme
    val monochrome = LocalMonochromeTheme.current
    val haze = remember { HazeState() }
    val listState = rememberLazyListState()
    val palette = remember(item.app.iconColor, item.app.packageName) { item.app.findAppCardColors() }
    val isScrolled by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    // Estimates close to the real heights so the first frame is already laid out correctly
    val statusTop = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    val navBottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    var headerHeight by remember { mutableStateOf(statusTop + 76.dp) }
    var footerHeight by remember { mutableStateOf(navBottom + 72.dp) }
    var headerMeasured by remember { mutableStateOf(false) }
    val headerShape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)

    Box(
        modifier = modifier
            .background(scheme.background)
            // swallow touches so nothing underneath receives them
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (headerMeasured) 1f else 0f }
                .hazeSourceIfEnabled(haze),
            contentPadding = PaddingValues(
                top = headerHeight + 12.dp,
                start = AppDefaults.ContentPadding,
                end = AppDefaults.ContentPadding,
                bottom = footerHeight + 16.dp
            )
        ) {
            item(key = "scan_card") {
                CompositionLocalProvider(LocalContentColor provides scheme.onSurface) {
                    ScanResultCard(
                        scanResult = result,
                        detail = scanResultDetail(result),
                        isMalicious = result is VirusTotalScanner.ScanResult.Malicious,
                        onProceed = {},
                        onCancel = {},
                        readOnly = true
                    )
                }
            }
        }

        // Header: transparent at top, glass blur once scrolled
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onSizeChanged {
                    headerHeight = with(density) { it.height.toDp() }
                    headerMeasured = true
                }
                .then(
                    if (isScrolled) {
                        Modifier
                            .clip(headerShape)
                            .glassFill(
                                glass = AppColors.glassContainerColor(),
                                shape = headerShape,
                                hazeState = haze
                            )
                    } else Modifier
                )
                .stableStatusBarsPadding()
                .padding(horizontal = AppDefaults.ContentPadding, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FindAppIcon(
                    app = item.app,
                    gradient = palette.gradient,
                    installed = item.installed,
                    size = 48.dp,
                    cornerRadius = 14.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "VirusTotal scan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = item.app.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Universal close button
        BottomCloseBar(
            onClose = onClose,
            hazeState = haze,
            showBackground = true,
            onHeightChanged = { footerHeight = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/** Morphe ActionPillButton surface: pill shape that rounds down on press, 36/40.dp tall. */
@Composable
private fun MorphePillSurface(
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    height: Dp,
    minWidth: Dp,
    border: BorderStroke?,
    shape: Shape,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val animatedContainer by animateColorAsState(containerColor, label = "action_pill_container")
    val animatedContent by animateColorAsState(contentColor, label = "action_pill_content")

    Surface(
        onClick = onClick,
        shape = rememberPressShape(interactionSource, shape),
        color = animatedContainer,
        contentColor = animatedContent,
        border = border,
        interactionSource = interactionSource,
        modifier = modifier
            .widthIn(min = minWidth)
            .height(height)
            .semantics { role = Role.Button }
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

/** Labeled pill: Morphe PillHeightLarge (40.dp), 16.dp text inset, 4.dp icon gap. */
@Composable
private fun MorpheActionPill(
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    large: Boolean = true,
    containerColor: Color,
    contentColor: Color,
    border: BorderStroke? = null,
    shape: Shape = AppDefaults.PillShape
) {
    MorphePillSurface(
        onClick = onClick,
        containerColor = containerColor,
        contentColor = contentColor,
        height = if (large) 40.dp else 36.dp,
        minWidth = if (large) 80.dp else 72.dp,
        border = border,
        shape = shape,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(if (large) 20.dp else 18.dp)
            )
            Text(
                text = label,
                style = if (large) MaterialTheme.typography.labelLarge
                else MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Icon-only pill: Morphe PillHeight (36.dp), PillMinWidth (72.dp), 18.dp icon. */
@Composable
private fun MorpheIconPill(
    onClick: () -> Unit,
    icon: ImageVector,
    description: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null
) {
    MorphePillSurface(
        onClick = onClick,
        containerColor = containerColor,
        contentColor = contentColor,
        height = 36.dp,
        minWidth = 72.dp,
        border = border,
        shape = AppDefaults.PillShape,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** A pill at the end of an info row (Morphe InfoRowAction). */
private class StoredRowAction(
    val icon: ImageVector,
    val description: String,
    val onClick: () -> Unit
)

private data class StoredInfoRowSpec(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val onClick: (() -> Unit)? = null,
    val trailingIcon: ImageVector? = null,
    val actions: List<StoredRowAction> = emptyList()
)

/** Morphe InfoRow + InfoRowWithActions. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StoredDetailInfoRow(spec: StoredInfoRowSpec, accent: Color) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val rowShape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .then(
                if (spec.onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = spec.onClick)
                } else {
                    // Long press copies the value, as Morphe's InfoRow does
                    Modifier.combinedClickable(
                        onLongClickLabel = "Copy",
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText(spec.label, spec.value))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        },
                        onClick = {}
                    )
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = spec.icon,
            contentDescription = null,
            tint = scheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = spec.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = scheme.onSurface.copy(alpha = 0.45f)
            )
            Text(
                text = spec.value,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface
            )
        }
        when {
            spec.actions.isNotEmpty() -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                spec.actions.forEach { action ->
                    MorpheIconPill(
                        onClick = action.onClick,
                        icon = action.icon,
                        description = action.description,
                        containerColor = scheme.primary.copy(alpha = 0.30f),
                        contentColor = scheme.onBackground
                    )
                }
            }
            spec.trailingIcon != null -> Icon(
                imageVector = spec.trailingIcon,
                contentDescription = null,
                tint = scheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
            spec.onClick != null -> Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = scheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}