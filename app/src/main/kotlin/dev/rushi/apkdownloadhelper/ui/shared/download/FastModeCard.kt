package dev.rushi.apkdownloadhelper.ui.shared.download

import androidx.activity.compose.BackHandler
import dev.rushi.apkdownloadhelper.ui.shared.dialog.CancelDownloadDialog
import dev.rushi.apkdownloadhelper.ui.shared.dialog.SettingsChoiceDialog
import dev.rushi.apkdownloadhelper.ui.shared.dialog.toSettingsChoice
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.ui.graphics.Shape
import dev.rushi.apkdownloadhelper.ui.shared.text.fittedTextAutoSize
import androidx.compose.ui.unit.sp
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
import dev.rushi.apkdownloadhelper.ui.shared.button.DialogButton
import dev.rushi.apkdownloadhelper.ui.shared.button.IconActionButton

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
    var showCancelConfirm by remember { mutableStateOf(false) }
    val isDirectFastMode = progress.title == null
    val showingTransfer = progress.percent != null && !progress.done &&
            !progress.awaitingDecision && !progress.awaitingVersionChoice

    // Same branch that shows the "Cancel" pill while Fast Mode is checking
    // sources. Back now does exactly what tapping Cancel does: open the
    // "Stop…?" confirmation first, and only leave after the user confirms.
    // (The transfer state has its own BackHandler in DownloadTransferBody.)
    BackHandler(
        enabled = !progress.done &&
                !showingTransfer &&
                !progress.awaitingDecision &&
                !progress.awaitingVersionChoice &&
                !showCancelConfirm
    ) {
        showCancelConfirm = true
    }

    if (showCancelConfirm) {
        CancelDownloadDialog(
            title = if (progress.percent != null) "Stop download?" else "Cancel ${progress.title ?: "Fast Mode"}?",
            message = if (progress.percent != null) {
                "The current download progress will be discarded."
            } else {
                "The active source search will be stopped."
            },
            // Searching sources is cancelled, not "stopped"; a real download keeps "Stop".
            confirmLabel = if (progress.percent != null) "Stop" else "Cancel",
            confirmDestructive = progress.percent != null,
            onKeepDownloading = { showCancelConfirm = false },
            onConfirmStop = {
                showCancelConfirm = false
                onCancel()
            }
        )
    }

    if (showSwitch) {
        val closeSwitch = {
            showSwitch = false
            if (resumeOnDismiss) onResume()
            resumeOnDismiss = false
        }
        // Morphe Link is always offered, first in the list like the Default source picker
        val switchSources = remember(availableSources) {
            if (DownloadSource.MORPHE_LINK in availableSources) availableSources
            else listOf(DownloadSource.MORPHE_LINK) + availableSources
        }
        SettingsChoiceDialog(
            title = "Switch source",
            choices = switchSources.map { it.toSettingsChoice() },
            // The source that is running right now comes pre-selected
            selectedIndex = switchSources.indexOfFirst { it.label == progress.sourceLabel },
            onSelect = { index ->
                val source = switchSources[index]
                if (source.label == progress.sourceLabel) {
                    closeSwitch()
                } else {
                    showSwitch = false
                    resumeOnDismiss = false
                    onSwitchSource(source)
                }
            },
            onDismiss = closeSwitch
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
                DownloadHeader(
                    source = progress.sourceLabel,
                    modeLabel = progress.title ?: stringResource(R.string.fast_mode),
                    modifier = Modifier.weight(1f)
                )
                if (!progress.done && progress.percent == null && !progress.awaitingDecision) {
                    WavyProgressRing(
                        progress = null,
                        modifier = Modifier.size(30.dp),
                        strokeWidth = 3.dp,
                        active = !progress.paused
                    )
                }
            }
            if (!showingTransfer) {
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
            } else if (showingTransfer) {
                DownloadTransferBody(
                    percent = progress.percent ?: 0,
                    paused = progress.paused,
                    statusText = progress.detail,
                    downloadedBytes = progress.downloadedBytes,
                    totalBytes = progress.totalBytes,
                    speedBytesPerSec = progress.speedBytesPerSec,
                    etaMs = progress.etaMs,
                    connections = progress.connections,
                    onPause = onPause,
                    onResume = onResume,
                    onStop = onCancel,
                    onSkipWait = onSkipWait,
                    onSkipScan = onSkipScan,
                    onSwitchSources = if (isDirectFastMode) {
                        {
                            resumeOnDismiss = !progress.paused
                            if (!progress.paused) onPause()
                            showSwitch = true
                        }
                    } else {
                        null
                    }
                )
            } else if (!progress.done) {
                DownloadPillButton(
                    text = "Cancel",
                    icon = Icons.Outlined.Close,
                    onClick = { showCancelConfirm = true },
                    primary = false,
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

@Composable
internal fun DownloadStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppDefaults.CompactCornerRadius)
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = shape,
        color = scheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = scheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                autoSize = fittedTextAutoSize(maxFontSize = 20.sp, minFontSize = 11.sp),
                fontWeight = FontWeight.Bold,
                color = scheme.primary,
                maxLines = 1
            )
        }
    }
}