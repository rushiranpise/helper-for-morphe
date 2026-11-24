package dev.rushi.apkdownloadhelper.ui.shared.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.HelperOutlinedButton
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.isHistoryUriUsable
import dev.rushi.apkdownloadhelper.storage.DownloadHistoryEntry
import dev.rushi.apkdownloadhelper.ui.shared.card.InfoCard
import dev.rushi.apkdownloadhelper.ui.shared.chip.AppFilterChip
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults

internal enum class HistoryFilter(val label: String) {
    All("All"),
    Scanned("Scanned"),
    Flagged("Flagged")
}

@Composable
internal fun DownloadHistorySection(
    entries: List<DownloadHistoryEntry>,
    onClear: () -> Unit,
    onOpen: (DownloadHistoryEntry) -> Unit,
    onShare: (DownloadHistoryEntry) -> Unit
) {
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf(HistoryFilter.All) }
    val filtered = entries.filter { entry ->
        when (filter) {
            HistoryFilter.All -> true
            HistoryFilter.Scanned -> entry.scanVerdict != null
            HistoryFilter.Flagged -> entry.scanVerdict?.malicious == true
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.download_history),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            HelperOutlinedButton(
                text = "Clear",
                onClick = onClear,
                modifier = Modifier.width(AppDefaults.CompactButtonWidth)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistoryFilter.entries.forEach { f ->
                val count = when (f) {
                    HistoryFilter.All -> entries.size
                    HistoryFilter.Scanned -> entries.count { it.scanVerdict != null }
                    HistoryFilter.Flagged -> entries.count { it.scanVerdict?.malicious == true }
                }
                AppFilterChip(
                    selected = f == filter,
                    onClick = { filter = f },
                    label = "${f.label} · $count",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (entries.isEmpty()) {
            InfoCard("No hand-offs recorded yet. Downloads and picked files you return to Morphe show up here.")
        } else if (filtered.isEmpty()) {
            InfoCard(
                when (filter) {
                    HistoryFilter.All -> "No hand-offs recorded yet."
                    HistoryFilter.Scanned -> "None of these downloads were scanned by VirusTotal."
                    HistoryFilter.Flagged -> "No flagged downloads — everything came back clean."
                }
            )
        } else {
            filtered.forEach { entry ->
                val usable = remember(entry.uri) { context.isHistoryUriUsable(entry.uri) }
                HistoryEntryCard(
                    entry = entry,
                    usable = usable,
                    onOpen = { onOpen(entry) },
                    onShare = { onShare(entry) }
                )
            }
        }
    }
}
