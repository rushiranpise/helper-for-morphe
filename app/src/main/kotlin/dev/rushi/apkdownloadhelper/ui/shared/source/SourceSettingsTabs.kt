package dev.rushi.apkdownloadhelper.ui.shared.source

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import dev.rushi.apkdownloadhelper.ui.shared.selection.SelectorOption
import dev.rushi.apkdownloadhelper.ui.shared.selection.SelectorRow

/**
 * Settings tabs, mirroring the manager's three-tab organisation: appearance,
 * how files come and go, and the troubleshooting/scanning corner.
 */
internal enum class SettingsTab(
    val label: String,
    val icon: ImageVector
) {
    APPEARANCE("Appearance", Icons.Outlined.Palette),
    SYSTEM("System", Icons.Outlined.PhoneAndroid),
    ADVANCED("Advanced", Icons.Outlined.Tune),
    // Request logs get a tab of their own: in Advanced they sat under the
    // scanning settings and made that tab scroll for pages.
    LOGS("Logs", Icons.Outlined.BugReport)
}

@Composable
internal fun SettingsTabRow(
    selected: SettingsTab,
    onSelect: (SettingsTab) -> Unit
) {
    SelectorRow(
        options = SettingsTab.entries.map { SelectorOption(label = it.label, icon = it.icon) },
        selectedIndex = SettingsTab.entries.indexOf(selected),
        onSelect = { onSelect(SettingsTab.entries[it]) },
        // Four tabs across, so keep the labels on the smaller step.
        labelStyle = MaterialTheme.typography.labelLarge
    )
}
