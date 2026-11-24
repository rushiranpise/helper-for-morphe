package dev.rushi.apkdownloadhelper.ui.shared.download

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.model.UiState
import dev.rushi.apkdownloadhelper.ui.shared.button.SkipWaitButton
import dev.rushi.apkdownloadhelper.ui.shared.dialog.AppAlertDialog
import dev.rushi.apkdownloadhelper.ui.shared.progress.WavyLinearProgressBar
import dev.rushi.apkdownloadhelper.ui.theme.pressScale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button

import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import dev.rushi.apkdownloadhelper.ui.shared.dialog.AppDialogProperties
import dev.rushi.apkdownloadhelper.ui.shared.dialog.DialogContentPadding
import dev.rushi.apkdownloadhelper.ui.shared.dialog.DialogSurface
import dev.rushi.apkdownloadhelper.ui.theme.pressScale
import dev.rushi.apkdownloadhelper.ui.theme.rememberMorphePillShape
import dev.rushi.apkdownloadhelper.ui.util.formatBytes
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
    var showCancelConfirm by remember { mutableStateOf(false) }

    if (showCancelConfirm) {
        CancelDownloadDialog(
            onKeepDownloading = { showCancelConfirm = false },
            onConfirmStop = {
                showCancelConfirm = false
                onCancel()
            }
        )
    }

    val scheme = MaterialTheme.colorScheme
    val statusText = state.statusMessage
        ?: if (state.paused) "Paused" else "Downloading from ${state.candidate.source.label}"
    val isScan = isScanStatus(statusText)
    val (phase, detail) = if (isScan) scanPhaseSplit(statusText) else (statusText to "")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceColorAtElevation(2.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header: status + stats on left, percentage pill + circular cancel on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = phase,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (detail.isNotBlank()) {
                    Text(
                        text = detail,
                        color = scheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!isScan) {
                    val candidateMeta = buildList {
                        state.candidate.versionName?.takeIf { it.isNotBlank() }?.let(::add)
                        state.candidate.fileKind
                            .takeIf { !it.equals("web", ignoreCase = true) }
                            ?.uppercase()
                            ?.let(::add)
                    }.joinToString(" · ")
                    if (candidateMeta.isNotBlank()) {
                        Text(
                            text = candidateMeta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                }
            }
            DownloadPercentPill(percent = state.percent, paused = state.paused)
            DownloadCancelButton(onClick = { showCancelConfirm = true })
        }

        DownloadProgressBar(
            percent = state.percent,
            paused = state.paused
        )

        if (!isScan) {
            DownloadDetailsRow(
                downloadedBytes = state.downloadedBytes,
                totalBytes = state.totalBytes,
                speedBytesPerSec = state.speedBytesPerSec,
                etaMs = state.etaMs,
                connections = state.connections,
                paused = state.paused,
                showConnections = state.percent < 100
            )
        }

        if (!isScan && state.percent < 100) {
            DownloadPillButton(
                text = if (state.paused) "Resume" else "Pause",
                icon = if (state.paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                onClick = if (state.paused) onResume else onPause,
                primary = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SkipWaitButton(
            active = isRateLimitWait(statusText),
            onSkipWait = onSkipWait
        )

        if (isScan) {
            DownloadPillButton(
                text = "Skip scan & hand off",
                icon = Icons.Outlined.Shield,
                primary = true,
                onClick = onSkipScan,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.hand_the_downloaded_file_to_morphe),
                color = scheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
internal fun DownloadProgressBar(
    percent: Int,
    paused: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val target = percent.coerceIn(0, 100) / 100f
    val animatedProgress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(220),
        label = "download_linear_progress"
    )
    WavyLinearProgressBar(
        progress = { animatedProgress },
        active = !paused && percent < 100,
        color = if (paused) {
            scheme.onSurfaceVariant.copy(alpha = 0.65f)
        } else {
            scheme.primary
        },
        trackColor = scheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = modifier.height(16.dp)
    )
}

@Composable
internal fun DownloadPercentPill(
    percent: Int,
    paused: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    val shown = percent.coerceIn(0, 100)
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (paused) {
                    scheme.surfaceVariant.copy(alpha = 0.7f)
                } else {
                    scheme.primaryContainer.copy(alpha = 0.7f)
                }
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$shown%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (paused) scheme.onSurfaceVariant else scheme.onPrimaryContainer,
            maxLines = 1
        )
    }
}

@Composable
internal fun DownloadCancelButton(onClick: () -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .pressScale(interaction),
        interactionSource = interaction,
        shape = rememberMorphePillShape(interaction),
        color = primaryColor.copy(alpha = 0.28f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Cancel download",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
internal fun DownloadPillButton(
    text: String,
    icon: ImageVector,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier
            .height(46.dp)
            .pressScale(interaction),
        interactionSource = interaction,
        shape = rememberMorphePillShape(interaction),
        border = null,
        colors = ButtonDefaults.buttonColors(
            containerColor = primaryColor.copy(alpha = 0.28f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (primary) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
    val summary = if (lines.isEmpty()) {
        if (paused) "Paused" else "Downloading"
    } else {
        lines.joinToString(" · ")
    }
    Text(
        text = summary,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
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

@Composable
internal fun DownloadDetailsRow(
    downloadedBytes: Long,
    totalBytes: Long,
    speedBytesPerSec: Double,
    etaMs: Long?,
    connections: Int,
    paused: Boolean,
    showConnections: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val sizeText = when {
        totalBytes > 0L -> "${downloadedBytes.formatBytes()} / ${totalBytes.formatBytes()}"
        downloadedBytes > 0L -> downloadedBytes.formatBytes()
        else -> null
    }
    val rightParts = buildList {
        if (paused) {
            add("Paused")
        } else {
            formatTransferSpeed(speedBytesPerSec).takeIf { it.isNotEmpty() }?.let(::add)
            if (etaMs != null && etaMs > 0L) add("${formatTransferEta(etaMs)} left")
        }
        if (showConnections) {
            add("$connections ${if (connections == 1) "conn" else "conns"}")
        }
    }

    if (sizeText == null && rightParts.isEmpty()) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (sizeText != null) {
            Text(
                text = sizeText,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        if (rightParts.isNotEmpty()) {
            Text(
                text = rightParts.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun CancelDownloadDialog(
    title: String = "Stop download?",
    message: String = "The current download progress will be discarded.",
    onKeepDownloading: () -> Unit,
    onConfirmStop: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Dialog(
        onDismissRequest = onKeepDownloading,
        properties = AppDialogProperties
    ) {
        DialogSurface(modifier = Modifier.fillMaxWidth(0.90f)) {
            Column(
                modifier = Modifier.padding(DialogContentPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(scheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null,
                            tint = scheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )
                }

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val keepInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    Button(
                        onClick = onKeepDownloading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .pressScale(keepInteraction),
                        interactionSource = keepInteraction,
                        shape = rememberMorphePillShape(keepInteraction),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor.copy(alpha = 0.28f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = "Keep downloading",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val stopInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    Button(
                        onClick = onConfirmStop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .pressScale(stopInteraction),
                        interactionSource = stopInteraction,
                        shape = rememberMorphePillShape(stopInteraction),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.errorContainer.copy(alpha = 0.85f),
                            contentColor = scheme.onErrorContainer
                        )
                    ) {
                        Text(
                            text = "Stop",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
