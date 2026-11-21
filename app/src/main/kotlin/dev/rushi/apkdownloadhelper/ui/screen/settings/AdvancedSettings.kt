package dev.rushi.apkdownloadhelper.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.storage.*
import dev.rushi.apkdownloadhelper.ui.shared.dialog.SettingsChoice
import dev.rushi.apkdownloadhelper.ui.shared.dialog.SettingsChoiceDialog
import dev.rushi.apkdownloadhelper.ui.shared.selection.RadioDot
import dev.rushi.apkdownloadhelper.ui.shared.settings.ApiKeyEditor
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsGroup
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsItem
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsSection
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsSwitchItem
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsValueText
import dev.rushi.apkdownloadhelper.ui.shared.settings.VirusTotalQuotaCard

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
        SettingsSection(title = "Network routing", icon = Icons.Outlined.NetworkCheck) {
            SettingsGroup {
                SettingsItem(
                    icon = settings.networkPolicy.icon(),
                    title = stringResource(R.string.network_policy),
                    subtitle = settings.networkPolicy.description,
                    onClick = { policyDialog = true },
                    trailingContent = { SettingsValueText(settings.networkPolicy.title) }
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

        SettingsSection(title = "VirusTotal scanning", icon = Icons.Outlined.Shield) {
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
