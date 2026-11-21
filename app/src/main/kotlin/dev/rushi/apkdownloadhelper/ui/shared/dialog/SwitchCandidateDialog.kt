package dev.rushi.apkdownloadhelper.ui.shared.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.HelperOutlinedButton
import dev.rushi.apkdownloadhelper.ui.shared.card.SurfaceCard
import dev.rushi.apkdownloadhelper.ui.model.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.model.SwitchCandidatePrompt
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import java.util.Locale

@Composable
internal fun SwitchCandidateDialog(
    prompt: SwitchCandidatePrompt,
    onChoose: (DownloadCandidate) -> Unit,
    onDismiss: () -> Unit
) {
    AppDialog(
        title = "Choose a variant from ${prompt.source.label}",
        onDismiss = onDismiss,
        actions = {
            HelperOutlinedButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
        }
    ) {
        Text(
            text = prompt.detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            prompt.candidates.forEach { candidate ->
                val variant = candidate.variantLabel ?: candidate.variantTag
                SurfaceCard(
                    onClick = { onChoose(candidate) },
                    cornerRadius = AppDefaults.CompactCornerRadius,
                    borderWidth = 1.dp,
                    borderColor = MaterialTheme.colorScheme.outlineVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDefaults.ContentPadding),
                        horizontalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = candidate.versionDisplay,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = listOfNotNull(
                                    candidate.fileKind.uppercase(Locale.US),
                                    variant?.takeIf { it.isNotBlank() }
                                ).joinToString(" · "),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = if (candidate.directDownload) {
                                Icons.Outlined.Download
                            } else {
                                Icons.Outlined.OpenInBrowser
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
