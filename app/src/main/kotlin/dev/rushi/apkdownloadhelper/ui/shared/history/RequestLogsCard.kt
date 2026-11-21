package dev.rushi.apkdownloadhelper.ui.shared.history

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.HelperOutlinedButton
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.storage.AppLog
import dev.rushi.apkdownloadhelper.storage.RequestLogEntry
import dev.rushi.apkdownloadhelper.ui.shared.card.InfoCard
import dev.rushi.apkdownloadhelper.ui.shared.card.SurfaceCard
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.color

@Composable
internal fun RequestLogsCard(
    logs: List<RequestLogEntry>,
    onClearLogs: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.request_logs),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            HelperOutlinedButton(
                text = "Share",
                icon = Icons.Outlined.Share,
                onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Morphe Helper logs")
                        putExtra(Intent.EXTRA_TEXT, AppLog.exportText())
                    }
                    context.startActivity(Intent.createChooser(share, "Share logs"))
                },
                // Icon + text needs more room than the plain "Clear" button.
                modifier = Modifier.widthIn(min = 120.dp)
            )
            HelperOutlinedButton(
                text = "Clear",
                onClick = onClearLogs,
                modifier = Modifier.width(AppDefaults.CompactButtonWidth)
            )
        }

        if (logs.isEmpty()) {
            InfoCard("No logs yet.")
        } else {
            SurfaceCard(cornerRadius = AppDefaults.CompactCornerRadius) {
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDefaults.ContentPadding),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        logs.takeLast(80).forEach { entry ->
                            Text(
                                text = "${entry.time} ${entry.level.badge} ${entry.message}",
                                color = entry.level.color(),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
