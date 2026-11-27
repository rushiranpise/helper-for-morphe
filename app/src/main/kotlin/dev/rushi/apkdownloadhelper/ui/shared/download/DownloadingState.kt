package dev.rushi.apkdownloadhelper.ui.shared.download

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
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
import dev.rushi.apkdownloadhelper.ui.shared.dialog.CancelDownloadDialog
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
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
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
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import dev.rushi.apkdownloadhelper.ui.shared.shape.rememberPressShape

// Same look as the "Accepted formats" pills: small gap, round outer corners, tight inner ones.
private val ConnectGap = 3.dp
private val ConnectOuter = 20.dp
private val ConnectInner = 6.dp
private val ConnectButtonOuter = 23.dp // half of the 46.dp pill buttons, so outer ends stay fully round

internal fun connectedShape(
    topStart: Boolean,
    topEnd: Boolean,
    bottomStart: Boolean,
    bottomEnd: Boolean,
    outer: Dp = ConnectOuter
): CornerBasedShape = RoundedCornerShape(
    topStart = if (topStart) outer else ConnectInner,
    topEnd = if (topEnd) outer else ConnectInner,
    bottomStart = if (bottomStart) outer else ConnectInner,
    bottomEnd = if (bottomEnd) outer else ConnectInner
)

@Composable
internal fun DownloadingState(
    state: UiState.Downloading,
    onCancel: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkipWait: () -> Unit,
    onSkipScan: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceColorAtElevation(2.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DownloadHeader(source = state.candidate.source.label, modeLabel = null)
        DownloadTransferBody(
            percent = state.percent,
            paused = state.paused,
            statusText = state.statusMessage.orEmpty(),
            downloadedBytes = state.downloadedBytes,
            totalBytes = state.totalBytes,
            speedBytesPerSec = state.speedBytesPerSec,
            etaMs = state.etaMs,
            connections = state.connections,
            onPause = onPause,
            onResume = onResume,
            onStop = onCancel,
            onSkipWait = onSkipWait,
            onSkipScan = onSkipScan
        )
    }
}

/** Shared card header: big source name, small mode label (e.g. "Fast Mode") below it. */
@Composable
internal fun DownloadHeader(
    source: String?,
    modeLabel: String?,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        source?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        modeLabel?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Everything under the header for an active transfer, shared by the normal download card and
 * the Fast Mode card. Pass [onSwitchSources] only for Fast Mode to get the extra button.
 */
@Composable
internal fun DownloadTransferBody(
    percent: Int,
    paused: Boolean,
    statusText: String,
    downloadedBytes: Long,
    totalBytes: Long,
    speedBytesPerSec: Double,
    etaMs: Long?,
    connections: Int,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSkipWait: () -> Unit,
    onSkipScan: () -> Unit,
    modifier: Modifier = Modifier,
    onSwitchSources: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val isScan = isScanStatus(statusText)
    var showStopConfirm by remember { mutableStateOf(false) }

    // Back behaves exactly like tapping Stop: ask first, then stop or keep downloading.
    BackHandler(enabled = !isScan && percent < 100) { showStopConfirm = true }

    if (showStopConfirm) {
        CancelDownloadDialog(
            onKeepDownloading = { showStopConfirm = false },
            onConfirmStop = {
                showStopConfirm = false
                onStop()
            }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
    ) {
        if (isScan) {
            val (phase, detail) = scanPhaseSplit(statusText)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = phase,
                    style = MaterialTheme.typography.bodyLarge,
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
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                DownloadPercentPill(percent = percent, paused = paused)
            }
        } else {
            DownloadStatGrid(
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes,
                speedBytesPerSec = speedBytesPerSec,
                etaMs = etaMs,
                connections = connections,
                showConnections = percent < 100
            )
            DownloadStatusRow(percent = percent, paused = paused)
        }

        DownloadProgressBar(percent = percent, paused = paused)

        if (!isScan && percent < 100) {
            val pauseResumeText = if (paused) "Resume" else "Pause"
            val pauseResumeIcon = if (paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause
            val pauseResumeClick = if (paused) onResume else onPause
            if (onSwitchSources != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ConnectGap)
                ) {
                    DownloadPillButton(
                        text = pauseResumeText,
                        icon = pauseResumeIcon,
                        onClick = pauseResumeClick,
                        primary = true,
                        shape = connectedShape(
                            topStart = true,
                            topEnd = false,
                            bottomStart = true,
                            bottomEnd = false,
                            outer = ConnectButtonOuter
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    DownloadPillButton(
                        text = "Switch Sources",
                        icon = Icons.Outlined.SwapHoriz,
                        onClick = onSwitchSources,
                        primary = false,
                        shape = connectedShape(
                            topStart = false,
                            topEnd = true,
                            bottomStart = false,
                            bottomEnd = true,
                            outer = ConnectButtonOuter
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                DownloadPillButton(
                    text = pauseResumeText,
                    icon = pauseResumeIcon,
                    onClick = pauseResumeClick,
                    primary = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            DownloadPillButton(
                text = "Stop",
                icon = Icons.Outlined.Close,
                onClick = { showStopConfirm = true },
                primary = false,
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
internal fun DownloadStatGrid(
    downloadedBytes: Long,
    totalBytes: Long,
    speedBytesPerSec: Double,
    etaMs: Long?,
    connections: Int,
    showConnections: Boolean,
    modifier: Modifier = Modifier
) {
    val sizeText = when {
        totalBytes > 0L -> "${downloadedBytes.formatBytes()} / ${totalBytes.formatBytes()}"
        downloadedBytes > 0L -> downloadedBytes.formatBytes()
        else -> "—"
    }
    val speedText = formatTransferSpeed(speedBytesPerSec)
        .takeIf { it.isNotEmpty() } ?: "—"
    val etaText = etaMs
        ?.takeIf { it > 0L }
        ?.let { formatTransferEta(it) }
        ?: "—"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ConnectGap)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ConnectGap)
        ) {
            DownloadStatCard(
                label = "Downloaded",
                value = sizeText,
                shape = connectedShape(topStart = true, topEnd = false, bottomStart = false, bottomEnd = false),
                modifier = Modifier.weight(1f)
            )
            DownloadStatCard(
                label = "Speed",
                value = speedText,
                shape = connectedShape(topStart = false, topEnd = true, bottomStart = false, bottomEnd = false),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ConnectGap)
        ) {
            // When Connections is hidden, Time left fills the row and rounds both bottom corners.
            DownloadStatCard(
                label = "Time left",
                value = etaText,
                shape = connectedShape(
                    topStart = false,
                    topEnd = false,
                    bottomStart = true,
                    bottomEnd = !showConnections
                ),
                modifier = Modifier.weight(1f)
            )
            if (showConnections) {
                DownloadStatCard(
                    label = "Connections",
                    value = connections.toString(),
                    shape = connectedShape(topStart = false, topEnd = false, bottomStart = false, bottomEnd = true),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
internal fun DownloadStatusRow(
    percent: Int,
    paused: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (paused) scheme.surfaceVariant.copy(alpha = 0.7f)
                    else scheme.primaryContainer.copy(alpha = 0.7f)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (paused) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (paused) scheme.onSurfaceVariant else scheme.onPrimaryContainer
            )
            Text(
                text = if (paused) "Paused" else "Downloading",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (paused) scheme.onSurfaceVariant else scheme.onPrimaryContainer,
                maxLines = 1
            )
        }
        Spacer(Modifier.weight(1f))
        DownloadPercentPill(percent = percent, paused = paused)
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
internal fun DownloadPillButton(
    text: String,
    icon: ImageVector,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape? = null
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val interaction = remember { MutableInteractionSource() }
    val morphShape = rememberMorphePillShape(interaction)
    val pressedShape = rememberPressShape(interaction, shape ?: morphShape)
    Button(
        onClick = onClick,
        modifier = modifier
            .height(46.dp)
            .pressScale(interaction),
        interactionSource = interaction,
        shape = if (shape != null) pressedShape else morphShape,
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