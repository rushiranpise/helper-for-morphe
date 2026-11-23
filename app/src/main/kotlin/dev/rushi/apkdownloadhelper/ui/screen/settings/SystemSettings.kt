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
import dev.rushi.apkdownloadhelper.ui.shared.dialog.AppAlertDialog
import dev.rushi.apkdownloadhelper.ui.shared.dialog.SettingsChoice
import dev.rushi.apkdownloadhelper.ui.shared.dialog.SettingsChoiceDialog
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsGroup
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsItem
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsSection
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsSwitchItem
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsValueText
import dev.rushi.apkdownloadhelper.ui.theme.SettingsValueStyle
import dev.rushi.apkdownloadhelper.ui.util.formatBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.interaction.MutableInteractionSource
import dev.rushi.apkdownloadhelper.ui.theme.pressScale

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
    val latestSettings by rememberUpdatedState(settings)

    LaunchedEffect(storageRefreshToken) {
        storageSummary = withContext(Dispatchers.IO) {
            "Cache ${context.temporaryDownloadsSize().formatBytes()} · " +
                "External ${context.externalDownloadsSize().formatBytes()} · " +
                "Downloads ${context.downloadsCopySize().formatBytes()} · " +
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
                onSettingsChange(settings.copy(downloadRetention = DownloadRetention.entries[index]))
                retentionDialog = false
            },
            onDismiss = { retentionDialog = false }
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

    if (showClearStorageDialog) {
        AppAlertDialog(
            onDismissRequest = { showClearStorageDialog = false },
            title = { Text("Clear storage?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This permanently deletes cached hand-off files, app external copies, " +
                                "saved APKs in Downloads/Morphe Helper, and copies saved to your custom folder. " +
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
                val deleteInteraction = remember { MutableInteractionSource() }
                TextButton(
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
                    interactionSource = deleteInteraction,
                    modifier = Modifier.pressScale(deleteInteraction, restCorner = 20.dp, pressedCorner = 10.dp)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                val cancelInteraction = remember { MutableInteractionSource() }
                TextButton(
                    onClick = { showClearStorageDialog = false },
                    interactionSource = cancelInteraction,
                    modifier = Modifier.pressScale(cancelInteraction, restCorner = 20.dp, pressedCorner = 10.dp)
                ) {
                    Text("Cancel")
                }
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
                    icon = settings.downloadLocation.icon(),
                    title = "Save downloads",
                    subtitle = settings.downloadLocation.description,
                    onClick = { locationDialog = true },
                    trailingContent = { SettingsValueText(downloadLocationLabel(context, settings)) }
                )
                SettingsItem(
                    icon = Icons.Outlined.Schedule,
                    title = "Keep downloads",
                    subtitle = settings.downloadRetention.description,
                    onClick = { retentionDialog = true },
                    trailingContent = { SettingsValueText(settings.downloadRetention.title) }
                )
                SettingsItem(
                    icon = Icons.Outlined.CleaningServices,
                    title = "Storage used",
                    subtitle = storageSummary,
                    onClick = { showClearStorageDialog = true },
                    trailingContent = { SettingsValueText("Clear", color = SettingsValueStyle.destructiveColor()) }
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
