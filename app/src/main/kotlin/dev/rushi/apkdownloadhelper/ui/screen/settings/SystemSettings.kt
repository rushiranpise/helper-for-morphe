package dev.rushi.apkdownloadhelper.ui.screen.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.storage.*
import dev.rushi.apkdownloadhelper.storage.clearAllDownloads
import dev.rushi.apkdownloadhelper.storage.clearLocation
import dev.rushi.apkdownloadhelper.ui.shared.dialog.AppConfirmDialog
import dev.rushi.apkdownloadhelper.ui.shared.dialog.SmallDialogButton
import dev.rushi.apkdownloadhelper.storage.locationSizes
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import dev.rushi.apkdownloadhelper.ui.shared.dialog.AppAlertDialog
import dev.rushi.apkdownloadhelper.ui.shared.dialog.SettingsChoice
import dev.rushi.apkdownloadhelper.ui.shared.dialog.SettingsChoiceDialog
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsGroup
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsItem
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsSection
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsSwitchItem
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsValueText
import dev.rushi.apkdownloadhelper.ui.util.formatBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Color

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
    var retentionDialog by remember { mutableStateOf(false) }
    var fastModePolicyDialog by remember { mutableStateOf(false) }
    var connectionCountDialog by remember { mutableStateOf(false) }
    var pendingLocation by remember { mutableStateOf<DownloadLocation?>(null) }
    var storageRefreshToken by remember { mutableIntStateOf(0) }
    var storageSummary by remember { mutableStateOf("Calculating storage…") }
    var showClearStorageDialog by remember { mutableStateOf(false) }
    var showAfterHandoffConfirmDialog by remember { mutableStateOf(false) }
    var storageSizes by remember { mutableStateOf(Triple(0L, 0L, 0L)) }
    val latestSettings by rememberUpdatedState(settings)

    LaunchedEffect(storageRefreshToken) {
        storageSizes = withContext(Dispatchers.IO) { context.locationSizes() }
        storageSummary = withContext(Dispatchers.IO) {
            "Internal ${context.temporaryDownloadsSize().formatBytes()} \u00b7 " +
            "External ${context.downloadsCopySize().formatBytes()} \u00b7 " +
            "Custom ${context.customDownloadsSize().formatBytes()}"
        }
    }

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
                } else if (loc == DownloadLocation.EXTERNAL && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    pendingLocation = loc
                    storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    onSettingsChange(latestSettings.copy(downloadLocation = loc))
                }
            },
            onDismiss = { locationDialog = false }
        )
    }

    if (retentionDialog) {
        SettingsChoiceDialog(
            title = "Keep downloads",
            choices = DownloadRetention.entries.map { retention ->
                SettingsChoice(
                    icon = when (retention) {
                        DownloadRetention.KEEP -> Icons.Outlined.SdStorage
                        DownloadRetention.AFTER_HANDOFF -> Icons.Outlined.Delete
                        DownloadRetention.HOUR_1,
                        DownloadRetention.HOUR_24,
                        DownloadRetention.DAY_7 -> Icons.Outlined.CleaningServices
                    },
                    title = retention.title,
                    description = retention.description
                )
            },
            selectedIndex = DownloadRetention.entries.indexOf(settings.downloadRetention),
            onSelect = { index ->
                val chosen = DownloadRetention.entries[index]
                retentionDialog = false
                val totalExistingBytes = storageSizes.first + storageSizes.second + storageSizes.third
                if (
                    chosen == DownloadRetention.AFTER_HANDOFF &&
                    settings.downloadRetention != DownloadRetention.AFTER_HANDOFF &&
                    totalExistingBytes > 0L
                ) {
                    showAfterHandoffConfirmDialog = true
                } else {
                    onSettingsChange(settings.copy(downloadRetention = chosen))
                }
            },
            onDismiss = { retentionDialog = false }
        )
    }

    if (showAfterHandoffConfirmDialog) {
        val totalExistingBytes = storageSizes.first + storageSizes.second + storageSizes.third
        AppAlertDialog(
            onDismissRequest = { showAfterHandoffConfirmDialog = false },
            title = { Text("Delete existing downloads?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "You currently have ${totalExistingBytes.formatBytes()} of previously downloaded files. " +
                            "Would you like to clear them all now or keep them and only auto-delete future downloads after hand-off?"
                    )
                    Text(
                        text = storageSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                dev.rushi.apkdownloadhelper.ui.shared.button.DialogButton(
                    text = "Clear all",
                    onClick = {
                        showAfterHandoffConfirmDialog = false
                        onSettingsChange(latestSettings.copy(downloadRetention = DownloadRetention.AFTER_HANDOFF))
                        scope.launch {
                            val freed = withContext(Dispatchers.IO) { context.clearAllDownloads() }
                            storageRefreshToken++
                            Toast.makeText(
                                context,
                                if (freed > 0L) "Cleared ${freed.formatBytes()}" else "Nothing to clear",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    isDestructive = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            dismissButton = {
                dev.rushi.apkdownloadhelper.ui.shared.button.DialogButton(
                    text = "Continue without clearing",
                    onClick = {
                        showAfterHandoffConfirmDialog = false
                        onSettingsChange(latestSettings.copy(downloadRetention = DownloadRetention.AFTER_HANDOFF))
                    },
                    filled = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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

    var showStorageManager by remember { mutableStateOf(false) }
    var pendingClear by remember { mutableStateOf<DownloadLocation?>(null) }

    if (showStorageManager) {
        val total = storageSizes.first + storageSizes.second + storageSizes.third
        val isDark = isSystemInDarkTheme()

        val internalColor = if (isDark) Color(0xFF3B82F6) else Color(0xFF2563EB) // Blue
        val externalColor = if (isDark) Color(0xFFA855F7) else Color(0xFF7E22CE) // Purple
        val customColor   = if (isDark) Color(0xFF10B981) else Color(0xFF059669) // Emerald

        val rows = listOf(
            Triple(DownloadLocation.INTERNAL, storageSizes.first, internalColor),
            Triple(DownloadLocation.EXTERNAL, storageSizes.second, externalColor),
            Triple(DownloadLocation.CUSTOM, storageSizes.third, customColor)
        )
        AppConfirmDialog(
            title = "Storage used",
            message = "",
            dismissText = "Close",
            confirmText = "Clear all",
            onDismiss = { showStorageManager = false },
            onConfirm = {
                showStorageManager = false
                showClearStorageDialog = true
            },
            icon = Icons.Outlined.CleaningServices,
            destructiveConfirm = true,
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        total.formatBytes(),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Adjust these values manually to control inner/outer corner rounding and gap
                    val innerCornerRadius = 2.dp
                    val outerCornerRadius = 12.dp
                    val segmentGap = 2.dp

                    val activeSegments = rows.filter { it.second > 0L }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(outerCornerRadius))
                            .background(
                                if (activeSegments.isEmpty()) MaterialTheme.colorScheme.surfaceVariant
                                else Color.Transparent
                            ),
                        horizontalArrangement = Arrangement.spacedBy(segmentGap)
                    ) {
                        activeSegments.forEachIndexed { index, (_, size, color) ->
                            val isFirst = index == 0
                            val isLast = index == activeSegments.lastIndex

                            val shape = RoundedCornerShape(
                                topStart = if (isFirst) outerCornerRadius else innerCornerRadius,
                                bottomStart = if (isFirst) outerCornerRadius else innerCornerRadius,
                                topEnd = if (isLast) outerCornerRadius else innerCornerRadius,
                                bottomEnd = if (isLast) outerCornerRadius else innerCornerRadius
                            )

                            Box(
                                modifier = Modifier
                                    .weight(size.toFloat())
                                    .fillMaxHeight()
                                    .clip(shape)
                                    .background(color)
                            )
                        }
                    }
                    rows.forEach { (location, size, color) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(location.title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    size.formatBytes(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            SmallDialogButton(
                                text = "Clear",
                                enabled = size > 0L,
                                onClick = { pendingClear = location }
                            )
                        }
                    }
                }
           }
        )
    }

    pendingClear?.let { location ->
        AppConfirmDialog(
            title = "Clear ${location.title}?",
            message = "This permanently deletes every download saved here. This cannot be undone.",
            dismissText = "Cancel",
            confirmText = "Clear",
            onDismiss = { pendingClear = null },
            onConfirm = {
                pendingClear = null
                scope.launch {
                    val freed = withContext(Dispatchers.IO) { context.clearLocation(location) }
                    storageRefreshToken++
                    Toast.makeText(
                        context,
                        if (freed > 0L) "Cleared ${freed.formatBytes()}" else "Nothing to clear",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            icon = Icons.Outlined.CleaningServices,
            destructiveConfirm = true
        )
    }

    if (showClearStorageDialog) {
        AppAlertDialog(
            onDismissRequest = { showClearStorageDialog = false },
            title = { Text("Clear storage?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This permanently deletes internal app files, " +
                                "saved APKs in Downloads/helper-for-morphe, and copies saved to your custom folder. " +
                                "This cannot be undone."
                    )
                    Text(
                        text = storageSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                dev.rushi.apkdownloadhelper.ui.shared.button.DialogButton(
                    text = "Clear",
                    onClick = {
                        showClearStorageDialog = false
                        scope.launch {
                            val freed = withContext(Dispatchers.IO) { context.clearAllDownloads() }
                            storageRefreshToken++
                            Toast.makeText(
                                context,
                                if (freed > 0L) "Cleared ${freed.formatBytes()}" else "Nothing to clear",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    isDestructive = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            dismissButton = {
                dev.rushi.apkdownloadhelper.ui.shared.button.DialogButton(
                    text = "Cancel",
                    onClick = { showClearStorageDialog = false },
                    filled = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsSection(title = "Storage", icon = Icons.Outlined.SdStorage) {
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Outlined.Schedule,
                    title = "Keep downloads",
                    subtitle = settings.downloadRetention.description,
                    onClick = { retentionDialog = true },
                    trailingContent = { SettingsValueText(settings.downloadRetention.title) }
                )
                if (settings.downloadRetention != DownloadRetention.AFTER_HANDOFF) {
                    SettingsItem(
                        icon = settings.downloadLocation.icon(),
                        title = "Save downloads in",
                        subtitle = settings.downloadLocation.description,
                        onClick = { locationDialog = true },
                        trailingContent = { SettingsValueText(downloadLocationLabel(context, settings)) }
                    )
                }
                SettingsItem(
                    icon = Icons.Outlined.CleaningServices,
                    title = "Storage used",
                    subtitle = "Total " + (storageSizes.first + storageSizes.second + storageSizes.third).formatBytes(),
                    onClick = { showStorageManager = true },
                    trailingContent = { SettingsValueText("Manage") }
                )
                SettingsItem(
                    icon = Icons.Outlined.Bolt,
                    title = "Download connections",
                    subtitle = "Maximum parallel connections per download",
                    onClick = { connectionCountDialog = true },
                    trailingContent = { SettingsValueText(settings.downloadConnections.title) }
                )
            }
        }

        SettingsSection(title = "Download behavior", icon = Icons.Outlined.Tune) {
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

        SettingsSection(title = "Fast mode", icon = Icons.Outlined.Explore) {
            SettingsGroup {
                SettingsSwitchItem(
                    icon = Icons.Outlined.Bolt,
                    title = "Enable Fast mode",
                    subtitle = "Auto-fetch a version across sources and return it automatically.",
                    checked = settings.fastMode,
                    onToggle = { onSettingsChange(settings.copy(fastMode = !settings.fastMode)) }
                )
                if (settings.fastMode) {
                    SettingsItem(
                        icon = Icons.Outlined.Tune,
                        title = "Which version to fetch",
                        onClick = { fastModePolicyDialog = true },
                        trailingContent = { SettingsValueText(settings.fastModePolicy.title) }
                    )
                }
            }
        }
    }
}
