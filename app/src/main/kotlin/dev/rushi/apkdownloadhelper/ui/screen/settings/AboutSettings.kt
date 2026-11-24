package dev.rushi.apkdownloadhelper.ui.screen.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.BuildConfig
import dev.rushi.apkdownloadhelper.storage.*
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsGroup
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsItem
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsSection
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsSwitchItem
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsValueText

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
        SettingsSection(title = "App info", icon = Icons.Outlined.Info) {
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Outlined.PhoneAndroid,
                    title = "Version",
                    onClick = { },
                    trailingContent = { SettingsValueText(BuildConfig.VERSION_NAME) }
                )
                SettingsItem(
                    icon = Icons.Outlined.OpenInNew,
                    title = "GitHub repository",
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
