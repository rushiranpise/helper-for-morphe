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
import dev.rushi.apkdownloadhelper.ui.theme.pressScale
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
                    TransferStats(
                        connections = state.connections,
                        speedBytesPerSec = state.speedBytesPerSec,
                        etaMs = state.etaMs,
                        paused = state.paused,
                        includeConnections = state.percent < 100
                    )
                }
            }

            DownloadPercentPill(percent = state.percent, paused = state.paused)

            DownloadCancelButton(onClick = onCancel)
        }

        // Linear progress bar
        DownloadProgressBar(
            percent = state.percent,
            paused = state.paused,
            modifier = Modifier.fillMaxWidth()
        )

        if (!isScan && state.percent < 100) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DownloadPillButton(
                    text = if (state.paused) "Resume" else "Pause",
                    icon = if (state.paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                    primary = true,
                    onClick = if (state.paused) onResume else onPause,
                    modifier = Modifier.weight(1f)
                )
                DownloadPillButton(
                    text = "Switch source",
                    icon = Icons.Outlined.SwapHoriz,
                    primary = false,
                    onClick = {
                        resumeOnDismiss = !state.paused
                        if (!state.paused) onPause()
                        showSwitch = true
                    },
                    modifier = Modifier.weight(1f)
                )
            }
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
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(CircleShape)
            .background(scheme.surfaceVariant.copy(alpha = 0.65f))
    ) {
        if (animatedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(
                        if (paused) {
                            scheme.onSurfaceVariant.copy(alpha = 0.6f)
                        } else {
                            scheme.primary
                        }
                    )
            )
        }
    }
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
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .pressScale(interaction),
        interactionSource = interaction,
        shape = CircleShape,
        color = scheme.surfaceVariant.copy(alpha = 0.55f),
        contentColor = scheme.onSurface
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
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier
            .height(46.dp)
            .pressScale(interaction),
        interactionSource = interaction,
        shape = CircleShape,
        border = null,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) {
                scheme.primaryContainer
            } else {
                scheme.surfaceVariant.copy(alpha = 0.55f)
            },
            contentColor = if (primary) {
                scheme.onPrimaryContainer
            } else {
                scheme.onSurface
            }
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
