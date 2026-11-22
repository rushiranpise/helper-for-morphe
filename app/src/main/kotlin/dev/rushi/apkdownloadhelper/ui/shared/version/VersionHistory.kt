package dev.rushi.apkdownloadhelper.ui.shared.version

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.VerifiedUser
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.download.openPlayStoreListing
import dev.rushi.apkdownloadhelper.ui.model.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.model.VersionHistoryState
import dev.rushi.apkdownloadhelper.ui.model.identityKey
import dev.rushi.apkdownloadhelper.ui.shared.source.ModeNoticeCard
import dev.rushi.apkdownloadhelper.ui.theme.pressScale
import java.util.Locale

internal fun LazyListScope.versionHistoryItems(
    state: VersionHistoryState,
    onDownloadVersion: (DownloadCandidate) -> Unit,
    onSolveCaptcha: (DownloadCandidate) -> Unit
) {
    when (state) {
        VersionHistoryState.Idle,
        VersionHistoryState.Loading -> Unit

        is VersionHistoryState.Error -> {
            item(key = "history_error") {
                ModeNoticeCard(state.message)
            }
        }

        is VersionHistoryState.Done -> {
            if (state.candidates.isEmpty()) {
                item(key = "history_empty") {
                    ModeNoticeCard("No version list was available for this source.")
                }
            } else {
                itemsIndexed(
                    items = state.candidates,
                    key = { index, candidate -> "history_${candidate.identityKey()}_$index" }
                ) { _, candidate ->
                    VersionHistoryRow(
                        candidate = candidate,
                        showOpenLink = candidate.identityKey() in state.noDirectDownloadKeys,
                        onDownloadVersion = { onDownloadVersion(candidate) },
                        onSolveCaptcha = { onSolveCaptcha(candidate) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun VersionHistoryRow(
    candidate: DownloadCandidate,
    showOpenLink: Boolean,
    onDownloadVersion: () -> Unit,
    onSolveCaptcha: (DownloadCandidate) -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.surfaceColorAtElevation(2.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = candidate.versionDisplay,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = when {
                    candidate.captchaUrl != null && !candidate.directDownload ->
                        "Browsable in the in-app browser"
                    showOpenLink -> "No direct download · open version page"
                    candidate.fileKind.equals("web", ignoreCase = true) ->
                        "Direct download · format resolved on download"
                    else -> candidate.fileKind.uppercase(Locale.US)
                },
                color = scheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        when {
            (showOpenLink || (candidate.captchaUrl != null && !candidate.directDownload)) &&
                    candidate.source != DownloadSource.AURORA &&
                    candidate.source != DownloadSource.PLAY -> {
                HistoryActionPill(
                    text = "Captcha",
                    icon = Icons.Outlined.VerifiedUser,
                    primary = true,
                    onClick = { onSolveCaptcha(candidate) }
                )
            }
            showOpenLink -> {
                HistoryActionPill(
                    text = "Open",
                    icon = Icons.Outlined.OpenInBrowser,
                    primary = false,
                    onClick = {
                        if (candidate.source == DownloadSource.PLAY) {
                            context.openPlayStoreListing(candidate.packageName, candidate.url)
                        } else {
                            onSolveCaptcha(candidate)
                        }
                    }
                )
            }
            else -> {
                HistoryActionPill(
                    text = "Download",
                    icon = Icons.Outlined.Download,
                    primary = true,
                    onClick = onDownloadVersion
                )
            }
        }
    }
}

@Composable
private fun HistoryActionPill(
    text: String,
    icon: ImageVector,
    primary: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(40.dp)
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
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}