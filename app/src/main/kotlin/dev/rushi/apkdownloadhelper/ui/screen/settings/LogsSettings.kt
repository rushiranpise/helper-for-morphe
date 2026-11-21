package dev.rushi.apkdownloadhelper.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.storage.*
import dev.rushi.apkdownloadhelper.ui.shared.history.DownloadHistorySection
import dev.rushi.apkdownloadhelper.ui.shared.history.RequestLogsCard
import dev.rushi.apkdownloadhelper.ui.shared.selection.SelectorOption
import dev.rushi.apkdownloadhelper.ui.shared.selection.SelectorRow
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsGroup
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsSection
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsSwitchItem

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
    var displayedHistory by remember(historyEntries) { mutableStateOf(historyEntries) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsSection(title = "Logs & history", icon = Icons.AutoMirrored.Outlined.ViewList) {
            SelectorRow(
                options = LogsSubTab.entries.map { SelectorOption(label = it.label, icon = it.icon) },
                selectedIndex = LogsSubTab.entries.indexOf(subTab),
                onSelect = { subTab = LogsSubTab.entries[it] },
                labelStyle = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(8.dp))

            when (subTab) {
                LogsSubTab.HISTORY -> DownloadHistorySection(
                    entries = displayedHistory,
                    onClear = {
                        onClearHistory()
                        displayedHistory = emptyList()
                    },
                    onOpen = onOpenHistoryEntry,
                    onShare = onShareHistoryEntry
                )
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
