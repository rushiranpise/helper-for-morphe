package dev.rushi.apkdownloadhelper.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import dev.rushi.apkdownloadhelper.BuildConfig
import dev.rushi.apkdownloadhelper.HelperButton
import dev.rushi.apkdownloadhelper.HelperOutlinedButton
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.download.VirusTotalScanner
import dev.rushi.apkdownloadhelper.formatHistoryTimestamp
import dev.rushi.apkdownloadhelper.storage.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.background
import androidx.compose.material3.surfaceColorAtElevation
import androidx.core.content.ContextCompat
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView

// ==========================================
// 1. APPEARANCE TAB
// ==========================================
@Composable
internal fun AppearanceTabContent(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit
) {
    var showAccentPicker by rememberSaveable { mutableStateOf(false) }
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val styles = ThemeStyle.entries.filter { it != ThemeStyle.MATERIAL_YOU || supportsDynamicColor }
    val accent = settings.customAccentColor.toAccentColorOrNull()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // THEME SECTION (Restored spacing)
        SettingsSection(title = "Theme", icon = Icons.Outlined.DarkMode) {
            SettingsGroup {
                ThemeMode.entries.forEach { mode ->
                    SettingsItem(
                        icon = mode.icon(),
                        title = mode.title,
                        subtitle = mode.description,
                        onClick = { onSettingsChange(settings.copy(themeMode = mode)) },
                        trailingContent = { RadioDot(selected = settings.themeMode == mode) }
                    )
                }
            }
        }

        // COLOR STYLE SECTION
        SettingsSection(title = "Color Style", icon = Icons.Outlined.Palette) {
            SettingsGroup {
                styles.forEach { style ->
                    SettingsItem(
                        icon = style.icon(),
                        title = style.title,
                        subtitle = style.description,
                        onClick = { onSettingsChange(settings.copy(themeStyle = style)) },
                        trailingContent = { RadioDot(selected = settings.themeStyle == style) }
                    )
                }
            }
        }

        // PURE BLACK SECTION
        if (settings.themeMode != ThemeMode.LIGHT) {
            SettingsSection(title = "Dark Theme", icon = Icons.Outlined.Contrast) {
                SettingsGroup {
                    SettingsSwitchItem(
                        icon = Icons.Outlined.Contrast,
                        title = stringResource(R.string.pure_black),
                        subtitle = "Paint backgrounds fully black. Saves power on OLED screens.",
                        checked = settings.pureBlackTheme,
                        onToggle = { onSettingsChange(settings.copy(pureBlackTheme = !settings.pureBlackTheme)) }
                    )
                }
            }
        }

        // ACCENT COLOR SECTION (Restored grid + reset button)
        if (settings.themeStyle == ThemeStyle.MORPHE) {
            SettingsSection(title = "Accent Color", icon = Icons.Outlined.Colorize) {
                SettingsGroup {
                    SettingsItemCard(onClick = null) {
                        AccentGridSection {
                            AccentSwatchGrid(
                                colors = MorpheAccentPresets,
                                selected = accent,
                                onSelect = { color ->
                                    onSettingsChange(settings.copy(customAccentColor = color.toHexString()))
                                },
                                onClear = { onSettingsChange(settings.copy(customAccentColor = "")) },
                                onCustomClick = { showAccentPicker = true }
                            )
                        }
                    }
                    SettingsItem(
                        icon = Icons.Outlined.Refresh,
                        title = "Reset to default",
                        subtitle = "Restore the app's default blue accent",
                        onClick = { onSettingsChange(settings.copy(customAccentColor = "")) },
                        trailingContent = { Text("Reset", color = MaterialTheme.colorScheme.primary) }
                    )
                }
            }
        }

        // BACKGROUND SECTION
        SettingsSection(title = "Background", icon = Icons.Outlined.Wallpaper) {
            SettingsGroup {
                SettingsSwitchItem(
                    icon = Icons.Outlined.Wallpaper,
                    title = "Animated circle backdrop",
                    subtitle = "Drift softly behind the helper. Holds still when animations are off or battery saver is on.",
                    checked = settings.animatedBackground,
                    onToggle = {
                        onSettingsChange(settings.copy(animatedBackground = !settings.animatedBackground))
                    }
                )
            }
        }

    }

    if (showAccentPicker) {
        MorpheAccentPickerDialog(
            currentColorHex = settings.customAccentColor,
            onColorSelected = { color ->
                onSettingsChange(settings.copy(customAccentColor = color.toHexString()))
                showAccentPicker = false
            },
            onDismiss = { showAccentPicker = false }
        )
    }
}

// ==========================================
// 2. DOWNLOADS TAB
// ==========================================
@Composable
internal fun SystemTabContent(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var locationDialog by remember { mutableStateOf(false) }
    var fastModePolicyDialog by remember { mutableStateOf(false) }
    var connectionCountDialog by remember { mutableStateOf(false) }
    var pendingLocation by remember { mutableStateOf<DownloadLocation?>(null) }
    val latestSettings by rememberUpdatedState(settings)

    val locations = DownloadLocation.entries
    val fastPolicies = FastModePolicy.entries
    val connectionCounts = DownloadConnectionCount.entries

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data ?: return@rememberLauncherForActivityResult
        val uri = data.data ?: return@rememberLauncherForActivityResult
        val flags = (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        val persisted = runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }.isSuccess
        if (!persisted) {
            Toast.makeText(context, "Could not keep write access.", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        onSettingsChange(latestSettings.copy(downloadLocation = DownloadLocation.CUSTOM, downloadCustomTreeUri = uri.toString()))
    }

    val storagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val target = pendingLocation
        pendingLocation = null
        if (granted && target != null) onSettingsChange(latestSettings.copy(downloadLocation = target))
    }

    if (locationDialog) {
        SettingsChoiceDialog(
            title = stringResource(R.string.save_downloads),
            choices = locations.map { SettingsChoice(it.icon(), it.title, it.description) },
            selectedIndex = locations.indexOf(latestSettings.downloadLocation),
            onSelect = { index ->
                locationDialog = false
                val loc = locations[index]
                if (loc == DownloadLocation.CUSTOM) {
                    folderPicker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply { addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) })
                } else if (loc == DownloadLocation.DOWNLOADS && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    pendingLocation = loc
                    storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    onSettingsChange(latestSettings.copy(downloadLocation = loc))
                }
            },
            onDismiss = { locationDialog = false }
        )
    }

    if (fastModePolicyDialog) {
        SettingsChoiceDialog(
            title = stringResource(R.string.which_version_to_fetch),
            choices = fastPolicies.map { policy -> SettingsChoice(Icons.Outlined.Tune, policy.title, policy.description) },
            selectedIndex = fastPolicies.indexOf(settings.fastModePolicy),
            onSelect = { index ->
                onSettingsChange(settings.copy(fastModePolicy = fastPolicies[index]))
                fastModePolicyDialog = false
            },
            onDismiss = { fastModePolicyDialog = false }
        )
    }

    if (connectionCountDialog) {
        SettingsChoiceDialog(
            title = "Download connections",
            choices = connectionCounts.map { SettingsChoice(Icons.Outlined.Bolt, it.title, it.description) },
            selectedIndex = connectionCounts.indexOf(settings.downloadConnections),
            onSelect = { index ->
                onSettingsChange(settings.copy(downloadConnections = connectionCounts[index]))
                connectionCountDialog = false
            },
            onDismiss = { connectionCountDialog = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsSection(title = "Storage", icon = Icons.Outlined.SdStorage) {
            SettingsGroup {
                SettingsItem(
                    icon = settings.downloadLocation.icon(),
                    title = "Save downloads",
                    subtitle = settings.downloadLocation.description,
                    onClick = { locationDialog = true },
                    trailingContent = { Text(downloadLocationLabel(context, settings), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
                )
                SettingsItem(
                    icon = Icons.Outlined.CleaningServices,
                    title = "Storage used",
                    subtitle = "Clear temporary files",
                    onClick = { scope.launch(Dispatchers.IO) { context.clearTemporaryDownloads() } },
                    trailingContent = { Text("Clear", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                )
                SettingsItem(
                    icon = Icons.Outlined.Bolt,
                    title = "Download connections",
                    subtitle = "Maximum parallel connections per download",
                    onClick = { connectionCountDialog = true },
                    trailingContent = { Text(settings.downloadConnections.title, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
                )
            }
        }

        SettingsSection(title = "Download Behavior", icon = Icons.Outlined.Tune) {
            SettingsGroup {
                SettingsSwitchItem(
                    icon = Icons.Outlined.PhoneAndroid,
                    title = "Prefer density-specific APKs",
                    subtitle = "Use APKMirror's variant for this screen's density instead of nodpi.",
                    checked = settings.preferDensitySpecificVariants,
                    onToggle = { onSettingsChange(settings.copy(preferDensitySpecificVariants = !settings.preferDensitySpecificVariants)) }
                )
            }
        }

        SettingsSection(title = "Fast Mode", icon = Icons.Outlined.Explore) {
            SettingsGroup {
                SettingsSwitchItem(
                    icon = Icons.Outlined.Bolt,
                    title = "Enable Fast Mode",
                    subtitle = "Auto-fetch a version across sources and return it automatically.",
                    checked = settings.fastMode,
                    onToggle = { onSettingsChange(settings.copy(fastMode = !settings.fastMode)) }
                )
                if (settings.fastMode) {
                    SettingsItem(
                        icon = Icons.Outlined.Tune,
                        title = "Which version to fetch",
                        onClick = { fastModePolicyDialog = true },
                        trailingContent = { Text(settings.fastModePolicy.title, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
                    )
                }
            }
        }
    }
}

// ==========================================
// 3. SECURITY & NETWORK TAB
// ==========================================
@Composable
internal fun AdvancedTabContent(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit
) {
    var policyDialog by remember { mutableStateOf(false) }
    val policies = NetworkPolicy.entries

    if (policyDialog) {
        SettingsChoiceDialog(
            title = stringResource(R.string.connection),
            choices = policies.map { SettingsChoice(it.icon(), it.title, it.description) },
            selectedIndex = policies.indexOf(settings.networkPolicy),
            onSelect = { index ->
                onSettingsChange(settings.copy(networkPolicy = policies[index]))
                policyDialog = false
            },
            onDismiss = { policyDialog = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsSection(title = "Network Routing", icon = Icons.Outlined.NetworkCheck) {
            SettingsGroup {
                SettingsItem(
                    icon = settings.networkPolicy.icon(),
                    title = stringResource(R.string.network_policy),
                    subtitle = settings.networkPolicy.description,
                    onClick = { policyDialog = true },
                    trailingContent = { Text(settings.networkPolicy.title, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
                )
                SettingsSwitchItem(
                    icon = Icons.Outlined.Dns,
                    title = stringResource(R.string.adguard_dns),
                    subtitle = "Blocks ads and trackers on download pages.",
                    checked = settings.adGuardDns,
                    onToggle = { onSettingsChange(settings.copy(adGuardDns = !settings.adGuardDns)) }
                )
                SettingsSwitchItem(
                    icon = Icons.Outlined.Explore,
                    title = "Use Morphe's resolver",
                    subtitle = "Ask Morphe where its own download dialog would go.",
                    checked = settings.useMorpheResolver,
                    onToggle = { onSettingsChange(settings.copy(useMorpheResolver = !settings.useMorpheResolver)) }
                )
            }
        }

        SettingsSection(title = "VirusTotal Scanning", icon = Icons.Outlined.Shield) {
            SettingsGroup {
                SettingsSwitchItem(
                    icon = Icons.Outlined.Shield,
                    title = stringResource(R.string.virustotal_scanning),
                    subtitle = "Scan downloaded files before returning them to Morphe.",
                    checked = settings.virusTotalEnabled,
                    onToggle = { onSettingsChange(settings.copy(virusTotalEnabled = !settings.virusTotalEnabled)) }
                )
                if (settings.virusTotalEnabled) {
                    VirusTotalScanMode.entries.forEach { mode ->
                        SettingsItem(
                            icon = when (mode) {
                                VirusTotalScanMode.NEVER -> Icons.Outlined.Block
                                VirusTotalScanMode.ASK -> Icons.Outlined.HelpOutline
                                VirusTotalScanMode.ALWAYS -> Icons.Outlined.CheckCircle
                            },
                            title = mode.title,
                            subtitle = mode.description,
                            onClick = { onSettingsChange(settings.copy(virusTotalScanMode = mode)) },
                            trailingContent = { RadioDot(selected = settings.virusTotalScanMode == mode) }
                        )
                    }
                    ApiKeyEditor(apiKey = settings.virusTotalApiKey, onApiKeyChange = { onSettingsChange(settings.copy(virusTotalApiKey = it)) })
                    if (settings.virusTotalScanMode != VirusTotalScanMode.NEVER) {
                        SettingsSwitchItem(
                            icon = Icons.Outlined.Verified,
                            title = "Proceed automatically when clean",
                            subtitle = "Hand off without showing the result when VirusTotal finds nothing.",
                            checked = settings.virusTotalAutoProceed,
                            onToggle = { onSettingsChange(settings.copy(virusTotalAutoProceed = !settings.virusTotalAutoProceed)) }
                        )
                    }
                    if (settings.virusTotalApiKey.isNotBlank()) {
                        Box(modifier = Modifier.padding(16.dp)) { VirusTotalQuotaCard(apiKey = settings.virusTotalApiKey) }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. SYSTEM & LOGS TAB
// ==========================================
internal enum class LogsSubTab(val label: String, val icon: ImageVector) {
    HISTORY("History", Icons.Outlined.History),
    LOGGING("Logging", Icons.Outlined.BugReport),
    REQUEST_LOGS("Request logs", Icons.AutoMirrored.Outlined.ViewList)
}

@Composable
internal fun LogsTabContent(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit,
    logs: List<RequestLogEntry>,
    onClearLogs: () -> Unit,
    historyEntries: List<DownloadHistoryEntry>,
    onOpenHistoryEntry: (DownloadHistoryEntry) -> Unit,
    onShareHistoryEntry: (DownloadHistoryEntry) -> Unit,
    onClearHistory: () -> Unit
) {
    var subTab by rememberSaveable { mutableStateOf(LogsSubTab.HISTORY) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsSection(title = "Logs & History", icon = Icons.AutoMirrored.Outlined.ViewList) {
            MorpheSelectorRow(
                options = LogsSubTab.entries.map { MorpheSelectorOption(label = it.label, icon = it.icon) },
                selectedIndex = LogsSubTab.entries.indexOf(subTab),
                onSelect = { subTab = LogsSubTab.entries[it] },
                labelStyle = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(8.dp))

            when (subTab) {
                LogsSubTab.HISTORY -> DownloadHistorySection(entries = historyEntries, onClear = onClearHistory, onOpen = onOpenHistoryEntry, onShare = onShareHistoryEntry)
                LogsSubTab.LOGGING -> SettingsGroup {
                    SettingsSwitchItem(
                        icon = Icons.Outlined.BugReport,
                        title = stringResource(R.string.log_to_logcat),
                        subtitle = "Write logs to the system log (adb logcat).",
                        checked = settings.logcatLogging,
                        onToggle = { onSettingsChange(settings.copy(logcatLogging = !settings.logcatLogging)) }
                    )
                }
                LogsSubTab.REQUEST_LOGS -> RequestLogsCard(logs = logs, onClearLogs = onClearLogs)
            }
        }
    }
}

// ==========================================
// 5. ABOUT TAB
// ==========================================
@Composable
internal fun AboutTabContent(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit,
    onOpenUpdates: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsSection(title = "App Info", icon = Icons.Outlined.Info) {
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Outlined.PhoneAndroid,
                    title = "Version",
                    onClick = { },
                    trailingContent = { Text(BuildConfig.VERSION_NAME, color = MaterialTheme.colorScheme.primary) }
                )
                SettingsItem(
                    icon = Icons.Outlined.OpenInNew,
                    title = "GitHub Repository",
                    subtitle = "View source code and report issues",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rushiranpise/helper-for-morphe"))
                        context.startActivity(intent)
                    }
                )
            }
        }

        SettingsSection(title = "Updates", icon = Icons.Outlined.Refresh) {
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Outlined.SystemUpdate,
                    title = "Check for updates & changelog",
                    subtitle = "View latest release and install",
                    onClick = onOpenUpdates
                )
                SettingsSwitchItem(
                    icon = Icons.Outlined.Refresh,
                    title = "Check automatically",
                    subtitle = "Look for a newer release when the helper starts.",
                    checked = settings.checkUpdates,
                    onToggle = { onSettingsChange(settings.copy(checkUpdates = !settings.checkUpdates)) }
                )
            }
        }
    }
}

// ==========================================
// SHARED COMPONENTS
// ==========================================

@Composable
internal fun SourcesSettingsScreen(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit,
    parentScrollState: ScrollState? = null
) {
    var sourceDialog by remember { mutableStateOf(false) }
    val latestSettings by rememberUpdatedState(settings)
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current

    val localOrderState = remember(settings.sourceOrder) {
        mutableStateOf(settings.orderedSources)
    }
    var localOrder by localOrderState
    var draggingSource by remember { mutableStateOf<DownloadSource?>(null) }
    var startIndex by remember { mutableIntStateOf(-1) }
    var targetIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var fingerRootY by remember { mutableFloatStateOf(0f) }
    val rowHeights = remember { mutableStateMapOf<DownloadSource, Int>() }

    fun updateTargetIndex() {
        if (startIndex !in localOrder.indices || localOrder.isEmpty()) return
        val draggedHeight = (rowHeights[localOrder[startIndex]] ?: 180).toFloat()
        var accumulated = 0f
        var newTarget = startIndex

        if (dragOffsetY > 0f) {
            for (i in (startIndex + 1)..localOrder.lastIndex) {
                val h = (rowHeights[localOrder[i]] ?: 180).toFloat()
                if (dragOffsetY > accumulated + h * 0.5f) {
                    accumulated += h
                    newTarget = i
                } else break
            }
        } else if (dragOffsetY < 0f) {
            for (i in (startIndex - 1) downTo 0) {
                val h = (rowHeights[localOrder[i]] ?: 180).toFloat()
                if (-dragOffsetY > accumulated + h * 0.5f) {
                    accumulated += h
                    newTarget = i
                } else break
            }
        }

        if (newTarget != targetIndex) {
            targetIndex = newTarget
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // Auto-scroll loop when dragging near screen edges
    LaunchedEffect(draggingSource, parentScrollState) {
        val activeSource = draggingSource ?: return@LaunchedEffect
        val scroll = parentScrollState ?: return@LaunchedEffect
        val screenHeight = view.height.toFloat().coerceAtLeast(1f)
        val topThreshold = screenHeight * 0.22f
        val bottomThreshold = screenHeight * 0.82f

        while (draggingSource == activeSource) {
            delay(16L)
            val y = fingerRootY
            val delta = when {
                y < topThreshold && scroll.canScrollBackward -> {
                    val ratio = ((topThreshold - y) / topThreshold).coerceIn(0.25f, 1.2f)
                    -20f * ratio
                }
                y > bottomThreshold && scroll.canScrollForward -> {
                    val ratio = ((y - bottomThreshold) / (screenHeight - bottomThreshold)).coerceIn(0.25f, 1.2f)
                    20f * ratio
                }
                else -> 0f
            }

            if (delta != 0f && startIndex in localOrder.indices) {
                val consumed = scroll.scrollBy(delta)
                if (consumed != 0f) {
                    val maxUp = -localOrder.take(startIndex).sumOf { rowHeights[it] ?: 180 }.toFloat()
                    val maxDown = localOrder.drop(startIndex + 1).sumOf { rowHeights[it] ?: 180 }.toFloat()
                    dragOffsetY = (dragOffsetY + consumed).coerceIn(maxUp, maxDown)
                    updateTargetIndex()
                }
            }
        }
    }

    if (sourceDialog) {
        val choices = buildList {
            add(SettingsChoice(Icons.Outlined.AutoAwesome, "Automatic", "Use the first enabled source in the custom order"))
            addAll(localOrder.map { SettingsChoice(icon = null, title = it.label, description = "Prefer this source when it has the file", source = it) })
        }
        SettingsChoiceDialog(
            title = stringResource(R.string.default_source),
            choices = choices,
            selectedIndex = settings.preferredSource?.let { source -> localOrder.indexOf(source) + 1 }?.takeIf { it > 0 } ?: 0,
            onSelect = { index ->
                onSettingsChange(settings.copy(preferredSource = if (index == 0) null else localOrder[index - 1]))
                sourceDialog = false
            },
            onDismiss = { sourceDialog = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsSection(title = stringResource(R.string.default_source), icon = Icons.Outlined.Star) {
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Outlined.Star,
                    title = stringResource(R.string.default_source),
                    subtitle = "Used first when a request is resolved",
                    onClick = { sourceDialog = true },
                    trailingContent = {
                        Text(
                            text = settings.preferredSource?.label ?: "Automatic",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }
        }

        SettingsSection(title = "Providers & Priority", icon = Icons.Outlined.Dns) {
            SettingsGroup {
                val draggedHeight = if (startIndex in localOrder.indices) {
                    (rowHeights[localOrder[startIndex]] ?: 180).toFloat()
                } else 0f

                localOrder.forEachIndexed { index, source ->
                    key(source) {
                        val enabled = source !in settings.disabledSources
                        val isDragging = draggingSource == source

                        val targetShiftY = when {
                            isDragging || startIndex !in localOrder.indices || targetIndex !in localOrder.indices -> 0f
                            startIndex < targetIndex && index in (startIndex + 1)..targetIndex -> -draggedHeight
                            startIndex > targetIndex && index in targetIndex until startIndex -> draggedHeight
                            else -> 0f
                        }
                        val animatedShiftY by animateFloatAsState(
                            targetValue = targetShiftY,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "neighbor_shift"
                        )

                        val scale by animateFloatAsState(
                            targetValue = if (isDragging) 1.03f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "drag_scale"
                        )
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 10.dp else 0.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "drag_elevation"
                        )
                        val rowFill = if (isDragging) {
                            MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                        } else {
                            Color.Transparent
                        }

                        var handleRootY by remember { mutableFloatStateOf(0f) }

                        Box(
                            modifier = Modifier
                                .zIndex(if (isDragging) 10f else 0f)
                                .onSizeChanged { rowHeights[source] = it.height }
                                .graphicsLayer {
                                    translationY = if (isDragging) dragOffsetY else animatedShiftY
                                    scaleX = scale
                                    scaleY = scale
                                    shadowElevation = elevation.toPx()
                                    shape = RoundedCornerShape(16.dp)
                                    clip = isDragging
                                }
                                .background(
                                    color = rowFill,
                                    shape = if (isDragging) RoundedCornerShape(16.dp) else RoundedCornerShape(0.dp)
                                )
                        ) {
                            SettingsItem(
                                leadingContent = { SourceAvatar(source = source, size = MorpheDefaults.IconSize) },
                                title = source.label,
                                subtitle = when {
                                    !enabled -> "Disabled"
                                    index == 0 && draggingSource == null -> "Tried first · Hold ≡ to reorder"
                                    else -> "Hold ≡ to reorder"
                                },
                                onClick = {
                                    onSettingsChange(
                                        settings.copy(
                                            disabledSources = if (enabled) settings.disabledSources + source else settings.disabledSources - source
                                        )
                                    )
                                },
                                trailingContent = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Switch(
                                            checked = enabled,
                                            onCheckedChange = { isChecked ->
                                                onSettingsChange(
                                                    settings.copy(
                                                        disabledSources = if (isChecked) settings.disabledSources - source else settings.disabledSources + source
                                                    )
                                                )
                                            },
                                            modifier = Modifier.semantics {
                                                stateDescription = if (enabled) "Enabled" else "Disabled"
                                            },
                                            thumbContent = {
                                                Icon(
                                                    imageVector = if (enabled) Icons.Outlined.Check else Icons.Outlined.Close,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                                )
                                            }
                                        )
                                        Icon(
                                            imageVector = Icons.Outlined.DragHandle,
                                            contentDescription = "Hold and drag to reorder ${source.label}",
                                            tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .onGloballyPositioned { coords ->
                                                    handleRootY = coords.positionInRoot().y + coords.size.height / 2f
                                                }
                                                .pointerInput(source, localOrderState) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = {
                                                            val idx = localOrder.indexOf(source)
                                                            if (idx != -1) {
                                                                draggingSource = source
                                                                startIndex = idx
                                                                targetIndex = idx
                                                                dragOffsetY = 0f
                                                                fingerRootY = handleRootY
                                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            }
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            if (startIndex in localOrder.indices) {
                                                                fingerRootY += dragAmount.y
                                                                val maxUp = -localOrder.take(startIndex).sumOf { rowHeights[it] ?: 180 }.toFloat()
                                                                val maxDown = localOrder.drop(startIndex + 1).sumOf { rowHeights[it] ?: 180 }.toFloat()
                                                                dragOffsetY = (dragOffsetY + dragAmount.y).coerceIn(maxUp, maxDown)
                                                                updateTargetIndex()
                                                            }
                                                        },
                                                        onDragEnd = {
                                                            if (startIndex in localOrder.indices && targetIndex in localOrder.indices && startIndex != targetIndex) {
                                                                val updated = localOrder.toMutableList().apply {
                                                                    val item = removeAt(startIndex)
                                                                    add(targetIndex, item)
                                                                }
                                                                localOrder = updated
                                                                onSettingsChange(latestSettings.copy(sourceOrder = updated))
                                                            }
                                                            draggingSource = null
                                                            startIndex = -1
                                                            targetIndex = -1
                                                            dragOffsetY = 0f
                                                        },
                                                        onDragCancel = {
                                                            draggingSource = null
                                                            startIndex = -1
                                                            targetIndex = -1
                                                            dragOffsetY = 0f
                                                        }
                                                    )
                                                }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
                SettingsItem(
                    icon = Icons.Outlined.Refresh,
                    title = "Reset source order",
                    subtitle = "Restore default priority.",
                    onClick = {
                        localOrder = DEFAULT_SOURCE_ORDER
                        onSettingsChange(latestSettings.copy(sourceOrder = DEFAULT_SOURCE_ORDER))
                    },
                    trailingContent = {
                        Text("Reset", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                    }
                )
            }
        }
    }
}

internal data class SettingsChoice(
    val icon: ImageVector?,
    val title: String,
    val description: String,
    val source: DownloadSource? = null
)

@Composable
internal fun SettingsChoiceDialog(
    title: String,
    choices: List<SettingsChoice>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ContentPaddingSmall)
            ) {
                choices.forEachIndexed { index, choice ->
                    SettingsOptionCard(
                        icon = choice.icon,
                        title = choice.title,
                        description = choice.description,
                        selected = index == selectedIndex,
                        source = choice.source,
                        onClick = { onSelect(index) }
                    )
                }
            }
        },
        confirmButton = {}
    )
}

internal fun DownloadLocation.icon(): ImageVector = when (this) {
    DownloadLocation.TEMPORARY -> Icons.Outlined.SdStorage
    DownloadLocation.EXTERNAL -> Icons.Outlined.PhoneAndroid
    DownloadLocation.DOWNLOADS -> Icons.Outlined.SaveAlt
    DownloadLocation.CUSTOM -> Icons.Outlined.FolderOpen
}

internal fun NetworkPolicy.icon(): ImageVector = when (this) {
    NetworkPolicy.WIFI_ONLY -> Icons.Outlined.Wifi
    NetworkPolicy.MOBILE_DATA_ONLY -> Icons.Outlined.SignalCellularAlt
    NetworkPolicy.WIFI_AND_MOBILE -> Icons.Outlined.NetworkCheck
}

internal fun ThemeMode.icon(): ImageVector = when (this) {
    ThemeMode.SYSTEM -> Icons.Outlined.Smartphone
    ThemeMode.DARK -> Icons.Outlined.DarkMode
    ThemeMode.LIGHT -> Icons.Outlined.LightMode
}

internal fun ThemeStyle.icon(): ImageVector = when (this) {
    ThemeStyle.MORPHE -> Icons.Outlined.Palette
    ThemeStyle.MATERIAL_YOU -> Icons.Outlined.AutoAwesome
    ThemeStyle.MONOCHROME -> Icons.Outlined.Contrast
}

internal fun Long.formatBytes(): String {
    if (this <= 0L) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    var value = toDouble()
    var unit = 0
    while (value >= 1024.0 && unit < units.lastIndex) { value /= 1024.0; unit++ }
    return if (unit == 0) "${toLong()} B" else String.format(Locale.US, "%.1f %s", value, units[unit])
}

@Composable
internal fun SettingsOptionCard(
    icon: ImageVector?,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    source: DownloadSource? = null
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(MorpheDefaults.SettingsCornerRadius)).clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(MorpheDefaults.SettingsCornerRadius),
        color = if (selected) colors.surfaceVariant else colors.surfaceColorAtElevation(3.dp),
        contentColor = colors.onSurface,
        tonalElevation = if (selected) 0.dp else 1.dp,
        border = if (selected) BorderStroke(1.5.dp, colors.onSurface.copy(alpha = 0.5f)) else null
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(MorpheDefaults.ContentPadding), horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing), verticalAlignment = Alignment.CenterVertically) {
            if (source != null) SourceAvatar(source = source, size = MorpheDefaults.IconSize)
            else if (icon != null) ThemedIcon(icon = icon, size = MorpheDefaults.IconSize, tint = if (selected) colors.primary else colors.onSurfaceVariant)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = colors.onSurface)
                Text(description, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            RadioDot(selected = selected)
        }
    }
}

@Composable
internal fun ApiKeyEditor(
    apiKey: String,
    onApiKeyChange: (String) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.api_key), style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.enter_api_key)) }
        )
        Row(
            modifier = Modifier.fillMaxWidth().clickable {
                val open = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.virustotal.com/docs/please-give-me-an-api-key"))
                runCatching { context.startActivity(open) }
            }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.how_to_get_a_free_api),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
internal fun VirusTotalQuotaCard(apiKey: String) {
    if (apiKey.isBlank()) return
    val colors = MaterialTheme.colorScheme
    var quota by remember { mutableStateOf<VirusTotalScanner.QuotaUsage?>(null) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var minuteUsed by remember { mutableIntStateOf(0) }
    var rollingUsed by remember { mutableIntStateOf(0) }
    var secsToRollover by remember { mutableIntStateOf(60) }
    var refreshKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(apiKey, refreshKey) {
        loading = true
        failed = false
        quota = withContext(Dispatchers.IO) { VirusTotalScanner.fetchQuotaUsage(apiKey) }
        if (quota == null) failed = true
        loading = false
        while (true) {
            minuteUsed = VirusTotalScanner.rateLimiter.callsInCurrentMinute()
            rollingUsed = VirusTotalScanner.rateLimiter.callsInLastMinute()
            secsToRollover = ((60_000L - System.currentTimeMillis() % 60_000L + 999L) / 1000L).toInt()
            delay(1000)
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MorpheDefaults.CardCornerRadius),
        color = sourceCardFill(),
        border = BorderStroke(1.dp, sourceCardBorder())
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Shield, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
                Column(modifier = Modifier.weight(1f)) { Text(stringResource(R.string.virustotal_quota), fontWeight = FontWeight.Bold, color = colors.onSurface) }
                if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = colors.primary, modifier = Modifier.size(20.dp).clip(MorpheDefaults.PillShape).clickable { refreshKey++ }.padding(2.dp))
            }
            QuotaBar("Per minute", minuteUsed, VirusTotalScanner.MINUTE_LOOKUP_LIMIT, flash = maxOf(minuteUsed, rollingUsed) >= VirusTotalScanner.MINUTE_LOOKUP_LIMIT - 1, caption = "rolling 60s: $rollingUsed / ${VirusTotalScanner.MINUTE_LOOKUP_LIMIT} · resets in ${secsToRollover}s")
            val current = quota
            when {
                current != null -> {
                    QuotaBar("Per hour", current.hourlyUsed, current.hourlyAllowed)
                    QuotaBar("Per day", current.dailyUsed, current.dailyAllowed)
                    QuotaBar("Per month", current.monthlyUsed, current.monthlyAllowed)
                }
                failed -> Text(stringResource(R.string.couldn_t_load_quota_check_your), color = colors.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun QuotaBar(label: String, used: Int, allowed: Int, flash: Boolean = false, caption: String? = null) {
    val colors = MaterialTheme.colorScheme
    val ratio = if (allowed > 0) used.toFloat() / allowed else 0f
    val warningAccent = SemanticTone.Warning.accent
    val baseColor = when { ratio >= 0.9f -> colors.error; ratio >= 0.7f -> warningAccent; else -> colors.primary }
    val barColor = if (flash) {
        val transition = rememberInfiniteTransition(label = "quota-flash-$label")
        warningAccent.copy(alpha = transition.animateFloat(1f, 0.3f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "quota-alpha-$label").value)
    } else baseColor
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text("$used / $allowed", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = if (ratio >= 0.7f) barColor else colors.onSurface)
        }
        LinearProgressIndicator(progress = { ratio.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = barColor, trackColor = colors.surfaceVariant)
        if (caption != null) Text(caption, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
    }
}

@Composable
internal fun HistoryEntryCard(
    entry: DownloadHistoryEntry,
    usable: Boolean,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    var showScanResult by remember { mutableStateOf(false) }
    SurfaceCard(cornerRadius = MorpheDefaults.CompactCornerRadius) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MorpheDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = entry.appName,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = entry.packageName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatHistoryTimestamp(entry.timestamp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = buildString {
                    entry.versionName?.let { append(it).append(" · ") }
                    append(entry.sourceName)
                    append(" · ")
                    append(entry.fileName)
                    if (!entry.fileKind.equals("web", ignoreCase = true)) {
                        append(" · ")
                        append(entry.fileKind.uppercase(Locale.US))
                    }
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            entry.obbContents?.let { obb ->
                Text(
                    text = "Includes game data (${obb.summary}). Install it to " +
                            "Android/obb/${entry.packageName} if the app will not start.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            entry.scanVerdict?.let { verdict ->
                val verdictTone = when {
                    verdict.malicious -> SemanticTone.Error
                    verdict.failed -> SemanticTone.Warning
                    else -> SemanticTone.Success
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (verdict.result != null) {
                            Modifier.clickable { showScanResult = true }
                        } else {
                            Modifier
                        }),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ContentPaddingSmall)
                ) {
                    ThemedIcon(
                        icon = when {
                            verdict.malicious -> Icons.Outlined.Warning
                            verdict.failed -> Icons.Outlined.HelpOutline
                            else -> Icons.Outlined.CheckCircle
                        },
                        size = 16.dp,
                        tint = verdictTone.accent
                    )
                    Text(
                        text = verdict.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (verdict.malicious) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    verdict.result?.let { result ->
                        MorpheStatusBadge(
                            text = if (result.cached) "Cached report" else "Fresh scan",
                            tone = if (result.cached) SemanticTone.Primary else SemanticTone.Neutral
                        )
                        ForwardChevronIcon(size = 16.dp)
                    }
                }
            }
            if (usable) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
                ) {
                    HelperButton(
                        text = "Share",
                        onClick = onShare,
                        icon = Icons.Outlined.Share,
                        modifier = Modifier.weight(1f)
                    )
                    HelperOutlinedButton(
                        text = "Open",
                        onClick = onOpen,
                        icon = Icons.Outlined.FolderOpen,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.file_no_longer_available_temporary_hand),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    val savedResult = entry.scanVerdict?.result
    if (showScanResult && savedResult != null) {
        MorpheDialog(
            title = stringResource(R.string.scan_result),
            onDismiss = { showScanResult = false },
            actions = {
                HelperButton(
                    text = "Close",
                    onClick = { showScanResult = false },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                ScanResultCard(
                    scanResult = savedResult,
                    detail = scanResultDetail(savedResult),
                    isMalicious = savedResult is VirusTotalScanner.ScanResult.Malicious,
                    onProceed = {},
                    onCancel = {},
                    readOnly = true
                )
            }
        }
    }
}
