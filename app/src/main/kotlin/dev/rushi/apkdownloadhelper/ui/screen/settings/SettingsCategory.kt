package dev.rushi.apkdownloadhelper.ui.screen.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector

internal enum class SettingsCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    SOURCES("Sources", "Enable, disable and reorder providers", Icons.Outlined.Dns),
    APPEARANCE("Appearance", "Theme, colors, and app icon", Icons.Outlined.Palette),
    CUSTOMIZATION("More customization", "Find Apps layout, cards & preview", Icons.Outlined.Tune),
    DOWNLOADS("Downloads", "Save location, storage & Fast Mode", Icons.Outlined.SdStorage),
    SECURITY_NETWORK("Security & network", "Network policy, DNS & VirusTotal", Icons.Outlined.Security),
    SYSTEM_LOGS("System & logs", "History, logs & diagnostics", Icons.AutoMirrored.Outlined.ViewList),
    ABOUT("About", "Version, updates & GitHub", Icons.Outlined.Info)
}
