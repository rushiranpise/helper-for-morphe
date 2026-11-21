package dev.rushi.apkdownloadhelper.ui.shared.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.HelperButton
import dev.rushi.apkdownloadhelper.HelperOutlinedButton
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.ui.shared.button.IconActionButton
import dev.rushi.apkdownloadhelper.ui.shared.button.SkipWaitButton
import dev.rushi.apkdownloadhelper.ui.shared.card.SurfaceCard
import dev.rushi.apkdownloadhelper.ui.shared.dialog.AppAlertDialog
import dev.rushi.apkdownloadhelper.ui.shared.progress.WavyProgressRing
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.model.UiState
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.util.formatTransferEta
import dev.rushi.apkdownloadhelper.ui.util.formatTransferSpeed

@Composable
internal fun DownloadingState(
    state: UiState.Downloading,
    availableSources: List<DownloadSource>,
    onCancel: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSwitchSource: (DownloadSource) -> Unit,
    onSkipWait: () -> Unit,
    onSkipScan: () -> Unit
) {
    var showSwitch by remember { mutableStateOf(false) }
    var resumeOnDismiss by remember { mutableStateOf(false) }

    if (showSwitch) {
        AppAlertDialog(
            onDismissRequest = {
                showSwitch = false
                if (resumeOnDismiss) onResume()
                resumeOnDismiss = false
            },
            title = { Text("Switch source") },
            text = {
                Column {
                    availableSources
                        .filter { it != state.candidate.source }
                        .forEach { source ->
                            TextButton(
                                onClick = {
                                    showSwitch = false
                                    resumeOnDismiss = false
                                    onSwitchSource(source)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(source.label)
                            }
                        }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showSwitch = false
                        if (resumeOnDismiss) onResume()
                        resumeOnDismiss = false
                    }
                ) { Text("Cancel") }
            }
        )
    }

    SurfaceCard {
        Column(
            modifier = Modifier.padding(AppDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
        ) {
            val statusText = state.statusMessage
                ?: if (state.paused) "Paused" else "Downloading from ${state.candidate.source.label}"
            val isScan = isScanStatus(statusText)
            val (phase, detail) = if (isScan) scanPhaseSplit(statusText) else (statusText to "")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DownloadProgressGauge(
                    percent = state.percent,
                    paused = state.paused,
                    modifier = Modifier.size(104.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = phase,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        IconActionButton(
                            icon = Icons.Outlined.Close,
                            contentDescription = "Cancel download",
                            onClick = onCancel
                        )
                    }
                    if (detail.isNotBlank()) {
                        Text(
                            text = detail,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (!isScan) {
                        TransferStats(
                            connections = state.connections,
                            speedBytesPerSec = state.speedBytesPerSec,
                            etaMs = state.etaMs,
                            paused = state.paused,
                            includeConnections = state.percent < 100
                        )
                    }
                }
            }
            if (!isScan && state.percent < 100) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HelperOutlinedButton(
                        text = if (state.paused) "Resume" else "Pause",
                        onClick = if (state.paused) onResume else onPause,
                        modifier = Modifier.weight(1f),
                        icon = if (state.paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause
                    )
                    HelperOutlinedButton(
                        text = "Switch source",
                        onClick = {
                            resumeOnDismiss = !state.paused
                            if (!state.paused) onPause()
                            showSwitch = true
                        },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.SwapHoriz
                    )
                }
            }
            SkipWaitButton(
                active = isRateLimitWait(statusText),
                onSkipWait = onSkipWait
            )
            if (isScan) {
                HelperButton(
                    text = "Skip scan & hand off",
                    onClick = onSkipScan,
                    icon = Icons.Outlined.Shield,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.hand_the_downloaded_file_to_morphe),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
internal fun DownloadProgressGauge(
    percent: Int,
    paused: Boolean,
    modifier: Modifier = Modifier
) {
    val shown = percent.coerceIn(0, 100)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        WavyProgressRing(
            progress = { shown / 100f },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 7.dp,
            active = !paused
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$shown%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (paused) {
                Text(
                    text = "Paused",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun TransferStats(
    connections: Int,
    speedBytesPerSec: Double,
    etaMs: Long?,
    paused: Boolean,
    includeConnections: Boolean,
    modifier: Modifier = Modifier
) {
    val lines = transferStatLines(
        connections = connections,
        speedBytesPerSec = speedBytesPerSec,
        etaMs = etaMs,
        paused = paused,
        includeConnections = includeConnections
    )
    if (lines.isEmpty()) {
        Text(
            text = if (paused) "Paused" else "Downloading",
            modifier = modifier,
            fontWeight = FontWeight.Medium
        )
        return
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        lines.forEach { line ->
            Text(
                text = line,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        }
    }
}

private fun transferStatLines(
    connections: Int,
    speedBytesPerSec: Double,
    etaMs: Long?,
    paused: Boolean,
    includeConnections: Boolean
): List<String> = buildList {
    if (includeConnections) {
        add(
            "$connections " +
                if (connections == 1) "connection" else "connections"
        )
    }
    if (!paused) {
        formatTransferSpeed(speedBytesPerSec)
            .takeIf { it.isNotEmpty() }
            ?.let(::add)
        if (etaMs != null && etaMs > 0L) {
            add("${formatTransferEta(etaMs)} left")
        }
    }
}
