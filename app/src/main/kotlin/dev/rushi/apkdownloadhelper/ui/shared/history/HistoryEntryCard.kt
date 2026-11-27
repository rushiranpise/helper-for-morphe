package dev.rushi.apkdownloadhelper.ui.shared.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.HelperButton
import dev.rushi.apkdownloadhelper.HelperOutlinedButton
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.download.VirusTotalScanner
import dev.rushi.apkdownloadhelper.formatHistoryTimestamp
import dev.rushi.apkdownloadhelper.storage.*
import dev.rushi.apkdownloadhelper.ui.shared.card.SurfaceCard
import dev.rushi.apkdownloadhelper.ui.shared.chip.StatusBadge
import dev.rushi.apkdownloadhelper.ui.shared.dialog.AppDialog
import dev.rushi.apkdownloadhelper.ui.shared.download.ScanResultCard
import dev.rushi.apkdownloadhelper.ui.shared.download.scanResultDetail
import dev.rushi.apkdownloadhelper.ui.shared.icon.ForwardChevronIcon
import dev.rushi.apkdownloadhelper.ui.shared.icon.ThemedIcon
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.SemanticTone
import java.util.Locale

@Composable
internal fun HistoryEntryCard(
    entry: DownloadHistoryEntry,
    usable: Boolean,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    var showScanResult by remember { mutableStateOf(false) }
    SurfaceCard(cornerRadius = AppDefaults.CompactCornerRadius) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = entry.appName,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = entry.packageName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatHistoryTimestamp(entry.timestamp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = buildString {
                    entry.versionName?.let { append(it).append(" · ") }
                    append(entry.sourceName)
                    append(" · ")
                    append(entry.fileName)
                    if (!entry.fileKind.equals("web", ignoreCase = true)) {
                        append(" · ")
                        append(entry.fileKind.uppercase(Locale.US))
                    }
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            entry.obbContents?.let { obb ->
                Text(
                    text = "Includes game data (${obb.summary}). Install it to " +
                            "Android/obb/${entry.packageName} if the app will not start.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            entry.scanVerdict?.let { verdict ->
                val verdictTone = when {
                    verdict.malicious -> SemanticTone.Error
                    verdict.failed -> SemanticTone.Warning
                    else -> SemanticTone.Success
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (verdict.result != null) {
                            Modifier.clickable { showScanResult = true }
                        } else {
                            Modifier
                        }),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDefaults.ContentPaddingSmall)
                ) {
                    ThemedIcon(
                        icon = when {
                            verdict.malicious -> Icons.Outlined.Warning
                            verdict.failed -> Icons.Outlined.HelpOutline
                            else -> Icons.Outlined.CheckCircle
                        },
                        size = 16.dp,
                        tint = verdictTone.accent
                    )
                    Text(
                        text = verdict.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (verdict.malicious) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    verdict.result?.let { result ->
                        StatusBadge(
                            text = if (result.cached) "Cached report" else "Fresh scan",
                            tone = if (result.cached) SemanticTone.Primary else SemanticTone.Neutral
                        )
                        ForwardChevronIcon(size = 16.dp)
                    }
                }
            }
            if (usable) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
                ) {
                    HelperButton(
                        text = "Share",
                        onClick = onShare,
                        icon = Icons.Outlined.Share,
                        modifier = Modifier.weight(1f)
                    )
                    HelperOutlinedButton(
                        text = "Open",
                        onClick = onOpen,
                        icon = Icons.Outlined.FolderOpen,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.file_no_longer_available_temporary_hand),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    val savedResult = entry.scanVerdict?.result
    if (showScanResult && savedResult != null) {
        AppDialog(
            title = stringResource(R.string.scan_result),
            onDismiss = { showScanResult = false },
            actions = {
                dev.rushi.apkdownloadhelper.ui.shared.button.DialogButton(
                    text = "Close",
                    onClick = { showScanResult = false },
                    buttonColor = dev.rushi.apkdownloadhelper.ui.shared.dialog.AppDialogDefaults.confirmButtonContainer(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                ScanResultCard(
                    scanResult = savedResult,
                    detail = scanResultDetail(savedResult),
                    isMalicious = savedResult is VirusTotalScanner.ScanResult.Malicious,
                    onProceed = {},
                    onCancel = {},
                    readOnly = true
                )
            }
        }
    }
}
