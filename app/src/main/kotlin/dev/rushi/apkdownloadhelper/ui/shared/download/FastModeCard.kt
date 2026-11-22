package dev.rushi.apkdownloadhelper.ui.shared.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.HelperButton
import dev.rushi.apkdownloadhelper.HelperOutlinedButton
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.storage.FastModePolicy
import dev.rushi.apkdownloadhelper.ui.shared.button.IconActionButton
import dev.rushi.apkdownloadhelper.ui.shared.button.SkipWaitButton
import dev.rushi.apkdownloadhelper.ui.shared.card.SurfaceCard
import dev.rushi.apkdownloadhelper.ui.shared.chip.StatusBadge
import dev.rushi.apkdownloadhelper.ui.shared.dialog.AppAlertDialog
import dev.rushi.apkdownloadhelper.ui.shared.icon.ThemedIcon
import dev.rushi.apkdownloadhelper.ui.shared.layout.IconTextRow
import dev.rushi.apkdownloadhelper.ui.shared.progress.WavyProgressRing
import dev.rushi.apkdownloadhelper.ui.shared.state.FailureActions
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.model.FastModeProgress
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.SemanticTone

@Composable
internal fun FastModeCard(
    progress: FastModeProgress,
    availableSources: List<DownloadSource>,
    onCancel: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSwitchSource: (DownloadSource) -> Unit,
    onSkipWait: () -> Unit,
    onUseMismatch: () -> Unit,
    onSkipMismatch: () -> Unit,
    onSkipScan: () -> Unit,
    onChooseVersion: (FastModePolicy) -> Unit,
    canRetryFailedSources: Boolean,
    onRetryAll: () -> Unit,
    onRetryFailedSources: () -> Unit,
    onCopyFailureDetails: () -> Unit
) {
    var showSwitch by remember { mutableStateOf(false) }
    var resumeOnDismiss by remember { mutableStateOf(false) }
    val isScan = isScanStatus(progress.detail)
    val scanInProgress = isScan && progress.percent != null && !progress.done

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
                        .filter { it.label != progress.sourceLabel }
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

    SurfaceCard(cornerRadius = AppDefaults.SectionCornerRadius) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        progress.title ?: stringResource(R.string.fast_mode),
                        fontWeight = FontWeight.Bold
                    )
                    progress.sourceLabel?.let { source ->
                        Text(
                            text = source,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                if (!progress.done && progress.percent == null && !progress.awaitingDecision) {
                    WavyProgressRing(
                        progress = null,
                        modifier = Modifier.size(30.dp),
                        strokeWidth = 3.dp,
                        active = !progress.paused
                    )
                }
            }
            if (!scanInProgress) {
                Text(
                    text = progress.detail,
                    color = when {
                        progress.done && !progress.succeeded -> MaterialTheme.colorScheme.error
                        progress.done -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            // Persist the SHA-256 verification on the card once the file's
            // bytes matched the source-published hash (not just the transient
            // post-download status line).
            if (progress.shaVerified) {
                // Read outside the semantics block: that lambda is not a composable one, so a
                // resource cannot be resolved inside it.
                val verifiedLabel = stringResource(R.string.sha_256_verified)
                StatusBadge(
                    text = null,
                    icon = Icons.Outlined.Verified,
                    tone = SemanticTone.Primary,
                    modifier = Modifier.semantics { contentDescription = verifiedLabel }
                )
            }
            if (progress.awaitingVersionChoice) {
                Surface(
                    color = SemanticTone.Primary.container,
                    contentColor = SemanticTone.Primary.content,
                    shape = RoundedCornerShape(AppDefaults.CompactCornerRadius)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDefaults.ContentPadding),
                        verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
                    ) {
                        IconTextRow(
                            leadingContent = {
                                ThemedIcon(icon = Icons.Outlined.Tune, size = 20.dp)
                            },
                            title = progress.versionChoiceDetail
                                ?: "Which version should Fast Mode fetch?",
                            titleStyle = MaterialTheme.typography.bodyMedium,
                            titleWeight = FontWeight.Normal,
                            titleColor = SemanticTone.Primary.content
                        )
                        progress.versionChoiceRequested?.let { requested ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.requested),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SemanticTone.Primary.content.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = requested,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
                ) {
                    HelperButton(
                        text = "Requested version",
                        onClick = { onChooseVersion(FastModePolicy.REQUESTED) },
                        modifier = Modifier.weight(1f)
                    )
                    HelperButton(
                        text = "Latest version",
                        onClick = { onChooseVersion(FastModePolicy.LATEST) },
                        modifier = Modifier.weight(1f)
                    )
                }
                HelperOutlinedButton(
                    text = "Cancel",
                    onClick = onCancel,
                    icon = Icons.Outlined.Close,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (progress.awaitingDecision) {
                val captchaDecision = progress.awaitingCaptcha
                val decisionTone = if (captchaDecision) SemanticTone.Primary else SemanticTone.Error
                Surface(
                    color = decisionTone.container,
                    contentColor = decisionTone.content,
                    shape = RoundedCornerShape(AppDefaults.CompactCornerRadius)
                ) {
                    IconTextRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDefaults.ContentPadding),
                        leadingContent = {
                            ThemedIcon(
                                icon = if (captchaDecision) Icons.Outlined.Shield else Icons.Outlined.Warning,
                                size = 20.dp,
                                tint = decisionTone.accent
                            )
                        },
                        title = progress.mismatchDetail ?: "Version code mismatch.",
                        titleStyle = MaterialTheme.typography.bodyMedium,
                        titleWeight = FontWeight.Normal,
                        titleColor = decisionTone.content
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
                ) {
                    HelperButton(
                        text = if (captchaDecision) "Solve captcha" else "Use this version",
                        onClick = onUseMismatch,
                        modifier = Modifier.weight(1f)
                    )
                    HelperOutlinedButton(
                        text = "Skip to next source",
                        onClick = onSkipMismatch,
                        modifier = Modifier.weight(1f)
                    )
                }
                HelperOutlinedButton(
                    text = "Cancel",
                    onClick = onCancel,
                    icon = Icons.Outlined.Close,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (progress.percent != null && !progress.done) {
                val percent = progress.percent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isScan) {
                        val (phase, detail) = scanPhaseSplit(progress.detail)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = phase,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (detail.isNotBlank()) {
                                Text(
                                    text = detail,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        TransferStats(
                            connections = progress.connections,
                            speedBytesPerSec = progress.speedBytesPerSec,
                            etaMs = progress.etaMs,
                            paused = progress.paused,
                            includeConnections = percent < 100,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    DownloadPercentPill(percent = percent, paused = progress.paused)
                    DownloadCancelButton(onClick = onCancel)
                }
                DownloadProgressBar(
                    percent = percent,
                    paused = progress.paused,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isScanStatus(progress.detail) && progress.percent < 100) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DownloadPillButton(
                            text = if (progress.paused) "Resume" else "Pause",
                            icon = if (progress.paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                            primary = true,
                            onClick = if (progress.paused) onResume else onPause,
                            modifier = Modifier.weight(1f)
                        )
                        DownloadPillButton(
                            text = "Switch source",
                            icon = Icons.Outlined.SwapHoriz,
                            primary = false,
                            onClick = {
                                resumeOnDismiss = !progress.paused
                                if (!progress.paused) onPause()
                                showSwitch = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                SkipWaitButton(
                    active = isRateLimitWait(progress.detail),
                    onSkipWait = onSkipWait
                )
                if (isScanStatus(progress.detail)) {
                    DownloadPillButton(
                        text = "Skip scan & hand off",
                        icon = Icons.Outlined.Shield,
                        primary = true,
                        onClick = onSkipScan,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else if (!progress.done) {
                HelperOutlinedButton(
                    text = "Cancel",
                    onClick = onCancel,
                    icon = Icons.Outlined.Close,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (!progress.succeeded) {
                FailureActions(
                    canRetryFailedSources = canRetryFailedSources,
                    onRetryAll = onRetryAll,
                    onRetryFailedSources = onRetryFailedSources,
                    onCopyDetails = onCopyFailureDetails,
                    onCancel = onCancel
                )
            }
        }
    }
}
