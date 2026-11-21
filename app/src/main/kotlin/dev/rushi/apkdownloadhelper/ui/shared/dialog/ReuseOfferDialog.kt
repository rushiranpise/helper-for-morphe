package dev.rushi.apkdownloadhelper.ui.shared.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.HelperButton
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.storage.DownloadHistoryEntry
import dev.rushi.apkdownloadhelper.ui.shared.icon.ThemedIcon
import dev.rushi.apkdownloadhelper.ui.shared.source.sourceCardBorder
import dev.rushi.apkdownloadhelper.ui.shared.source.sourceCardFill
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.SemanticTone
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppDefaults.ContentPaddingSmall)
            ) {
                Text(
                    text = stringResource(R.string.a_previous_download_for_this_exact),
                    style = MaterialTheme.typography.bodyMedium
                )
                options.forEach { option ->
                    val entry = option.entry
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppDefaults.CompactCornerRadius))
                            .clickable { onUseExisting(option) },
                        shape = RoundedCornerShape(AppDefaults.CompactCornerRadius),
                        color = sourceCardFill(),
                        border = BorderStroke(1.dp, sourceCardBorder())
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = entry.sourceName,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                entry.versionName?.let {
                                    Text(
                                        text = "· $it",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (option.sizeBytes > 0L) {
                                    Text(
                                        text = "· ${option.sizeBytes.formatBytes()}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = entry.fileName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            entry.scanVerdict?.let { verdict ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(AppDefaults.ContentPaddingSmall),
                                    modifier = Modifier.padding(top = 3.dp)
                                ) {
                                    ThemedIcon(
                                        icon = when {
                                            verdict.malicious -> Icons.Outlined.Warning
                                            verdict.failed -> Icons.Outlined.HelpOutline
                                            else -> Icons.Outlined.CheckCircle
                                        },
                                        size = 16.dp,
                                        tint = when {
                                            verdict.malicious -> SemanticTone.Error.accent
                                            verdict.failed -> SemanticTone.Warning.accent
                                            else -> SemanticTone.Success.accent
                                        }
                                    )
                                    Text(
                                        text = verdict.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (verdict.malicious) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                HelperButton(
                    text = "Download new",
                    onClick = onDownloadNew,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {}
    )
}
