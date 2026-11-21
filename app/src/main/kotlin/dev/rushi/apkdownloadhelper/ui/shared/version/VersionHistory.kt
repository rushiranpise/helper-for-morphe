package dev.rushi.apkdownloadhelper.ui.shared.version

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.HelperButton
import dev.rushi.apkdownloadhelper.HelperOutlinedButton
import dev.rushi.apkdownloadhelper.download.openPlayStoreListing
import dev.rushi.apkdownloadhelper.ui.shared.card.InfoCard
import dev.rushi.apkdownloadhelper.ui.shared.card.SurfaceCard
import dev.rushi.apkdownloadhelper.ui.model.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.model.VersionHistoryState
import dev.rushi.apkdownloadhelper.ui.model.identityKey
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import java.util.Locale

@Composable
internal fun VersionHistorySection(
    state: VersionHistoryState,
    onDownloadVersion: (DownloadCandidate) -> Unit,
    onSolveCaptcha: (DownloadCandidate) -> Unit
) {
    when (state) {
        // Idle and Loading are driven entirely by the bottom-bar primary
        // action button; the content area shows results only.
        VersionHistoryState.Idle,
        VersionHistoryState.Loading -> Unit

        is VersionHistoryState.Error -> {
            InfoCard(state.message)
        }

        is VersionHistoryState.Done -> {
            if (state.candidates.isEmpty()) {
                InfoCard("No version list was available for this source.")
            } else {
                state.candidates.forEach { candidate ->
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
    SurfaceCard(cornerRadius = AppDefaults.CompactCornerRadius) {
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
                    text = when {
                        // Captcha-gated rows are browsed in the in-app captcha
                        // findapps, not downloaded.
                        candidate.captchaUrl != null && !candidate.directDownload ->
                            "Browsable in the in-app findapps (version history)"
                        showOpenLink -> "No direct download  open the version page"
                        // History rows know only the version page until the
                        // user taps Download, which resolves the real format.
                        // "web" is a placeholder, not an actual file type, so
                        // don't render it as if the row just links out.
                        candidate.fileKind.equals("web", ignoreCase = true) ->
                            "Direct download  format resolved on download"
                        else -> candidate.fileKind.uppercase(Locale.US)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            when {
                (showOpenLink || (candidate.captchaUrl != null && !candidate.directDownload)) &&
                    candidate.source != DownloadSource.AURORA &&
                    candidate.source != DownloadSource.PLAY -> {
                    HelperButton(
                        text = "Solve captcha",
                        onClick = { onSolveCaptcha(candidate) },
                        icon = Icons.Outlined.VerifiedUser,
                        modifier = Modifier.widthIn(min = 140.dp)
                    )
                }
                showOpenLink -> {
                    HelperOutlinedButton(
                        text = "Open in app",
                        onClick = {
                            if (candidate.source == DownloadSource.PLAY) {
                                context.openPlayStoreListing(candidate.packageName, candidate.url)
                            } else {
                                onSolveCaptcha(candidate)
                            }
                        },
                        icon = Icons.Outlined.OpenInBrowser,
                        modifier = Modifier.widthIn(min = 120.dp)
                    )
                }
                else -> {
                    HelperButton(
                        text = "Download",
                        onClick = onDownloadVersion,
                        icon = Icons.Outlined.Download,
                        modifier = Modifier.widthIn(min = 120.dp)
                    )
                }
            }
        }
    }
}
