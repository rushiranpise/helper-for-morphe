package dev.rushi.apkdownloadhelper.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.BuildConfig
import dev.rushi.apkdownloadhelper.HelperButton
import dev.rushi.apkdownloadhelper.HelperOutlinedButton
import dev.rushi.apkdownloadhelper.download.PACKAGE_PATTERN
import dev.rushi.apkdownloadhelper.storage.HelperSettings
import dev.rushi.apkdownloadhelper.storage.UpdateChannel
import dev.rushi.apkdownloadhelper.storage.UpdateMode
import dev.rushi.apkdownloadhelper.updater.ShizukuHelper
import dev.rushi.apkdownloadhelper.updater.UpdateInfo
import dev.rushi.apkdownloadhelper.updater.UpdateManager
import kotlinx.coroutines.launch

@Composable
internal fun UpdateScreen(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit,
    refreshToken: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var latest by remember { mutableStateOf(UpdateManager.cachedLatest(context)) }
    var checking by remember { mutableStateOf(false) }
    var checkToken by remember { mutableIntStateOf(0) }
    var checkMessage by remember { mutableStateOf<String?>(null) }
    var installing by remember { mutableStateOf(false) }
    var installMessage by remember { mutableStateOf<String?>(null) }
    var modeDialog by remember { mutableStateOf(false) }
    var channelDialog by remember { mutableStateOf(false) }
    var packageDialog by remember { mutableStateOf(false) }

    val shizukuStatus = remember(refreshToken, settings.shizukuPackage) {
        ShizukuHelper.status(context)
    }
    val batteryUnrestricted = remember(refreshToken) {
        UpdateManager.isBatteryUnrestricted(context)
    }
    val updateAvailable = latest?.isNewerThan(BuildConfig.VERSION_NAME) == true

    fun checkNow(channel: UpdateChannel = settings.updateChannel) {
        if (installing) {
            checkMessage = "Finish the current install first."
            return
        }
        if (checking && channel == settings.updateChannel) return
        val token = checkToken + 1
        checkToken = token
        checking = true
        checkMessage = "Checking for updates…"
        scope.launch {
            val result = UpdateManager.checkForUpdates(
                context = context,
                manual = true,
                settings = settings.copy(updateChannel = channel)
            )
            if (token != checkToken) return@launch
            latest = result.info ?: latest
            checkMessage = result.message
            checking = false
        }
    }

    fun install(useShizuku: Boolean) {
        val info = latest ?: return
        if (installing) return
        if (checking) {
            installMessage = "Wait for the update check to finish."
            return
        }
        if (info.assetUrl.isBlank()) {
            installMessage = "This release has no APK asset."
            return
        }
        installing = true
        scope.launch {
            installMessage = "Downloading v${info.version}…"
            try {
                val file = UpdateManager.download(context, info)
                val verification = UpdateManager.verifyDownloadedApk(context, file, info)
                if (verification != null) {
                    file.delete()
                    installMessage = verification
                } else if (
                    useShizuku &&
                    ShizukuHelper.status(context) == ShizukuHelper.READY
                ) {
                    installMessage = "Installing with Shizuku…"
                    val (ok, detail) = UpdateManager.installWithShizuku(context, file)
                    if (ok) {
                        installMessage = "Update installed. Reopen the helper."
                    } else {
                        installMessage = "Shizuku failed: $detail. Opening the Android installer…"
                        UpdateManager.openInstaller(context, file)
                    }
                } else {
                    val opened = UpdateManager.openInstaller(context, file)
                    installMessage = if (opened) {
                        "Android installer opened. Follow the prompt."
                    } else {
                        "Allow installs from this app, then tap Install again."
                    }
                }
            } catch (error: Throwable) {
                installMessage = "Update failed: ${error.message ?: "unknown error"}"
            } finally {
                installing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        latest = UpdateManager.cachedLatest(context)
        if (latest != null) return@LaunchedEffect
        if (!settings.checkUpdates) {
            checkMessage = "Update checks are disabled."
            return@LaunchedEffect
        }
        checkNow()
    }

    BackHandler(onBack = onBack)

    // Use MorpheSubpageScaffold to get the exact sticky header and bottom padding
    MorpheSubpageScaffold(
        title = "Updates",
        onBack = onBack
    ) {
        SettingsSection(title = "Release", icon = Icons.Outlined.Refresh) {
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Outlined.PhoneAndroid,
                    title = "Current version",
                    onClick = { },
                    trailingContent = {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
                SettingsItem(
                    icon = Icons.Outlined.Refresh,
                    title = if (checking) "Checking for updates…" else "Check for updates",
                    subtitle = checkMessage ?: "Look for a newer release on GitHub.",
                    onClick = { checkNow() },
                    trailingContent = {
                        Text(
                            text = if (checking) "" else "Check",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
                latest?.let { info ->
                    SettingsItem(
                        icon = Icons.Outlined.Verified,
                        title = "Latest version",
                        onClick = { },
                        subtitle = if (updateAvailable) {
                            val channel = if (UpdateInfo.isPrerelease(info.version)) "Beta" else "Stable"
                            "$channel update available"
                        } else {
                            "You are up to date"
                        },
                        trailingContent = {
                            Text(
                                text = "v${info.version}",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )
                }
            }
        }

        if (latest == null && checking) {
            SettingsSection(title = "Changelog", icon = Icons.Outlined.History) {
                SettingsGroup {
                    SettingsItemCard(onClick = null) {
                        ShimmerReleaseNotes()
                    }
                }
            }
        }

        latest?.let { info ->
            SettingsSection(title = "Changelog", icon = Icons.Outlined.History) {
                SettingsGroup {
                    SettingsItemCard(onClick = null) {
                        SelectionContainer(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = cleanReleaseNotes(info.notes).ifBlank { "No release notes for this version." },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(MorpheDefaults.ContentPadding),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (info.releaseUrl.isNotBlank()) {
                        SettingsItem(
                            icon = Icons.Outlined.OpenInNew,
                            title = "Open release page",
                            onClick = { openExternal(context, info.releaseUrl) }
                        )
                    }
                }
            }
        }

        if (updateAvailable && latest != null) {
            val info = latest!!
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
            ) {
                HelperButton(
                    text = if (installing) "Working…" else "Download and install",
                    onClick = { install(false) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (shizukuStatus == ShizukuHelper.READY) {
                    HelperOutlinedButton(
                        text = "Install via Shizuku",
                        onClick = { install(true) },
                        icon = Icons.Outlined.Shield,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (info.assetUrl.isBlank()) {
                    Text(
                        text = "This release has no APK attached.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                installMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        SettingsSection(title = "Update settings", icon = Icons.Outlined.Settings) {
            SettingsGroup {
                SettingsSwitchItem(
                    icon = Icons.Outlined.Refresh,
                    title = "Check for updates",
                    subtitle = "Look for a newer release when the helper starts.",
                    checked = settings.checkUpdates,
                    onToggle = { onSettingsChange(settings.copy(checkUpdates = !settings.checkUpdates)) }
                )
                SettingsSwitchItem(
                    icon = Icons.Outlined.HelpOutline,
                    title = "In-app update popup",
                    subtitle = "Show a dialog on launch when a newer release is available.",
                    checked = settings.updatePopup,
                    onToggle = { onSettingsChange(settings.copy(updatePopup = !settings.updatePopup)) }
                )
                SettingsItem(
                    icon = Icons.Outlined.TrendingUp,
                    title = "Update channel",
                    subtitle = settings.updateChannel.description,
                    onClick = { channelDialog = true },
                    trailingContent = {
                        Text(
                            text = settings.updateChannel.title,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
                SettingsItem(
                    icon = Icons.Outlined.Download,
                    title = "Update mode",
                    subtitle = settings.updateMode.description,
                    onClick = { modeDialog = true },
                    trailingContent = {
                        Text(
                            text = settings.updateMode.title,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
                SettingsItem(
                    icon = Icons.Outlined.Shield,
                    title = "Shizuku",
                    subtitle = "Silent installs need Shizuku running and this app approved.",
                    onClick = { handleShizukuAction(context, shizukuStatus) },
                    trailingContent = {
                        Text(
                            text = ShizukuHelper.statusLabel(context, shizukuStatus),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
                SettingsItem(
                    icon = Icons.Outlined.PhoneAndroid,
                    title = "Shizuku app package",
                    subtitle = "Only needed for a Shizuku fork with a different package name.",
                    onClick = { packageDialog = true },
                    trailingContent = {
                        Text(
                            text = settings.shizukuPackage.ifBlank { "Default" },
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
                SettingsItem(
                    icon = Icons.Outlined.Settings,
                    title = "Battery optimization",
                    subtitle = "Keep update checks reliable when Android restricts background work.",
                    onClick = { UpdateManager.requestBatteryExemption(context) },
                    trailingContent = {
                        Text(
                            text = if (batteryUnrestricted) "Unrestricted" else "Optimized",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }
        }
    }

    if (channelDialog) {
        val channels = UpdateChannel.entries
        SettingsChoiceDialog(
            title = "Update channel",
            choices = channels.map { channel ->
                SettingsChoice(
                    icon = if (channel == UpdateChannel.BETA) Icons.Outlined.TrendingUp else Icons.Outlined.CheckCircle,
                    title = channel.title,
                    description = channel.description
                )
            },
            selectedIndex = channels.indexOf(settings.updateChannel),
            onSelect = { index ->
                val channel = channels[index]
                channelDialog = false
                if (channel != settings.updateChannel) {
                    UpdateManager.resetForChannelSwitch(context)
                    latest = null
                    checkMessage = null
                    installMessage = null
                    onSettingsChange(settings.copy(updateChannel = channel))
                    checkNow(channel)
                }
            },
            onDismiss = { channelDialog = false }
        )
    }

    if (modeDialog) {
        val modes = UpdateMode.entries
        SettingsChoiceDialog(
            title = "Update mode",
            choices = modes.map { mode ->
                SettingsChoice(
                    icon = when (mode) {
                        UpdateMode.NOTIFY -> Icons.Outlined.HelpOutline
                        UpdateMode.SHIZUKU -> Icons.Outlined.Shield
                    },
                    title = mode.title,
                    description = mode.description
                )
            },
            selectedIndex = modes.indexOf(settings.updateMode),
            onSelect = { index ->
                onSettingsChange(settings.copy(updateMode = modes[index]))
                modeDialog = false
            },
            onDismiss = { modeDialog = false }
        )
    }

    if (packageDialog) {
        ShizukuPackageDialog(
            initial = settings.shizukuPackage,
            onSave = { value ->
                onSettingsChange(settings.copy(shizukuPackage = value))
                packageDialog = false
            },
            onDismiss = { packageDialog = false }
        )
    }
}

@Composable
internal fun ShizukuPackageDialog(
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = { Text("Shizuku app package", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)) {
                Text(
                    text = "Leave blank for the default package. Only change this for a Shizuku fork with a different package name.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        value = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = error != null,
                    placeholder = { Text(ShizukuHelper.DEFAULT_PACKAGE) }
                )
                error?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = value.trim()
                if (trimmed.isNotEmpty() && !PACKAGE_PATTERN.matches(trimmed)) {
                    error = "Enter a valid package name, for example ${ShizukuHelper.DEFAULT_PACKAGE}."
                } else {
                    onSave(trimmed)
                }
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onSave("") }) { Text("Use default") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
internal fun UpdatePromptDialog(
    info: UpdateInfo,
    currentVersion: String,
    onView: () -> Unit,
    onLater: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = { Text("Update available", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Helper v${info.version} is available. You have v$currentVersion."
            )
        },
        confirmButton = {
            TextButton(onClick = onView) { Text("View update") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onSkip) { Text("Skip") }
                TextButton(onClick = onLater) { Text("Later") }
            }
        }
    )
}

internal fun handleShizukuAction(context: Context, status: Int) {
    when (status) {
        ShizukuHelper.READY -> {
            Toast.makeText(context, "Shizuku is ready.", Toast.LENGTH_SHORT).show()
        }
        ShizukuHelper.PERMISSION_REQUIRED -> ShizukuHelper.requestPermission()
        ShizukuHelper.NOT_RUNNING,
        ShizukuHelper.NOT_INSTALLED -> ShizukuHelper.openManager(context)
        else -> {
            Toast.makeText(context, "This Shizuku version is not supported.", Toast.LENGTH_SHORT).show()
        }
    }
}

internal fun openExternal(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

internal fun cleanReleaseNotes(value: String): String =
    value.replace("\r\n", "\n")
        .replace(Regex("""(?m)^#{1,6}\s*"""), "")
        .replace(Regex("""\[([^\]]+)\]\([^)]+\)"""), "\$1")
        .replace("**", "")
        .replace("__", "")
        .replace("`", "")
        .replace(Regex("""(?m)^- """), "• ")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()