package dev.rushi.apkdownloadhelper.ui.shared.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.storage.DownloadHistoryEntry
import dev.rushi.apkdownloadhelper.ui.shared.appcard.AppAvatar
import dev.rushi.apkdownloadhelper.ui.shared.chip.StatusBadge
import dev.rushi.apkdownloadhelper.ui.shared.chip.StatusBadgeRow
import dev.rushi.apkdownloadhelper.ui.shared.icon.ThemedIcon
import dev.rushi.apkdownloadhelper.ui.theme.SemanticTone
import dev.rushi.apkdownloadhelper.ui.theme.pressScale
import dev.rushi.apkdownloadhelper.ui.util.formatBytes

/** A previously downloaded file offered for reuse, with its size on disk. */
internal data class ReuseOption(
    val entry: DownloadHistoryEntry,
    val sizeBytes: Long
)

@Composable
internal fun ReuseOfferDialog(
    options: List<ReuseOption>,
    onUseExisting: (ReuseOption) -> Unit,
    onDownloadNew: () -> Unit
) {
    AppAlertDialog(
        onDismissRequest = onDownloadNew,
        title = {
            Text(
                text = stringResource(R.string.use_an_existing_apk),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            val scheme = MaterialTheme.colorScheme
            val downloadNewInteraction = remember { MutableInteractionSource() }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.a_previous_download_for_this_exact),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { option ->
                        val entry = option.entry
                        val cardInteraction = remember { MutableInteractionSource() }
                        options.forEach { option ->
                            val entry = option.entry
                            val useItInteraction = remember { MutableInteractionSource() }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(scheme.surfaceColorAtElevation(4.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // App icon + name + package
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppAvatar(
                                        packageName = entry.packageName,
                                        initial = entry.appName.firstOrNull()?.uppercaseChar() ?: '?'
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = entry.appName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = scheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = entry.packageName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = scheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Version, source, format & file size pills
                                StatusBadgeRow(modifier = Modifier.fillMaxWidth()) {
                                    entry.versionName?.let { version ->
                                        StatusBadge(
                                            text = version,
                                            icon = Icons.Outlined.History,
                                            tone = SemanticTone.Primary
                                        )
                                    }
                                    StatusBadge(
                                        text = entry.sourceName,
                                        containerColor = scheme.surfaceVariant.copy(alpha = 0.65f),
                                        contentColor = scheme.onSurface
                                    )
                                    if (entry.fileKind.isNotBlank() && !entry.fileKind.equals("web", ignoreCase = true)) {
                                        StatusBadge(
                                            text = entry.fileKind.uppercase(),
                                            containerColor = scheme.surfaceVariant.copy(alpha = 0.65f),
                                            contentColor = scheme.onSurfaceVariant
                                        )
                                    }
                                    if (option.sizeBytes > 0L) {
                                        StatusBadge(
                                            text = option.sizeBytes.formatBytes(),
                                            containerColor = scheme.surfaceVariant.copy(alpha = 0.65f),
                                            contentColor = scheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // File name strip
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(scheme.surfaceVariant.copy(alpha = 0.35f))
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FolderOpen,
                                        contentDescription = null,
                                        tint = scheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = entry.fileName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Scan verdict (if any)
                                entry.scanVerdict?.let { verdict ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        ThemedIcon(
                                            icon = when {
                                                verdict.malicious -> Icons.Outlined.Warning
                                                verdict.failed -> Icons.Outlined.HelpOutline
                                                else -> Icons.Outlined.CheckCircle
                                            },
                                            size = 15.dp,
                                            tint = when {
                                                verdict.malicious -> SemanticTone.Error.accent
                                                verdict.failed -> SemanticTone.Warning.accent
                                                else -> SemanticTone.Primary.accent
                                            }
                                        )
                                        Text(
                                            text = verdict.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = if (verdict.malicious) {
                                                scheme.error
                                            } else {
                                                scheme.onSurfaceVariant
                                            },
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Button(
                                    onClick = { onUseExisting(option) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .pressScale(useItInteraction),
                                    interactionSource = useItInteraction,
                                    shape = CircleShape,
                                    border = null,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = scheme.primaryContainer,
                                        contentColor = scheme.onPrimaryContainer
                                    ),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Use it",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDownloadNew,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .pressScale(downloadNewInteraction),
                    interactionSource = downloadNewInteraction,
                    shape = CircleShape,
                    border = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.surfaceVariant.copy(alpha = 0.55f),
                        contentColor = scheme.onSurface
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Download new",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        },
        confirmButton = {}
    )
}
