package dev.rushi.apkdownloadhelper.ui.shared.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.HelperButton
import dev.rushi.apkdownloadhelper.HelperOutlinedButton
import dev.rushi.apkdownloadhelper.download.isPackageInstalled
import dev.rushi.apkdownloadhelper.download.openPlayStoreListing
import dev.rushi.apkdownloadhelper.download.versionNameEquals
import dev.rushi.apkdownloadhelper.ui.shared.card.InfoCard
import dev.rushi.apkdownloadhelper.ui.shared.card.SurfaceCard
import dev.rushi.apkdownloadhelper.ui.shared.chip.StatusBadge
import dev.rushi.apkdownloadhelper.ui.shared.chip.StatusBadgeRow
import dev.rushi.apkdownloadhelper.ui.model.CandidateMatchSummary
import dev.rushi.apkdownloadhelper.ui.model.CandidateOption
import dev.rushi.apkdownloadhelper.ui.model.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.model.HelperRequest
import dev.rushi.apkdownloadhelper.ui.model.identityKey
import dev.rushi.apkdownloadhelper.ui.model.matchSummary
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.SemanticTone

@Composable
internal fun CandidateCard(
    request: HelperRequest,
    candidate: DownloadCandidate,
    onDownload: () -> Unit,
    onPickDownloadedFile: () -> Unit,
    onUseInstalledApp: () -> Unit,
    onSolveCaptcha: (DownloadCandidate) -> Unit,
    installedPackageRefreshToken: Int
) {
    val context = LocalContext.current
    val match = candidate.matchSummary(request)
    val hasResolvedCandidateInfo = candidate.versionName != null ||
        candidate.versionCode != null ||
        !candidate.fileKind.equals("web", ignoreCase = true)
    var hasOpenedLink by remember(candidate.identityKey()) { mutableStateOf(false) }
    // Manual-mode links are opened by the bottom-bar primary action, so the
    // "installed app" offer applies as soon as the row renders.
    val linkConsideredOpened = hasOpenedLink || candidate.option == CandidateOption.MANUAL
    val showUseInstalledApp = candidate.source == DownloadSource.PLAY &&
        linkConsideredOpened &&
        remember(candidate.packageName, linkConsideredOpened, installedPackageRefreshToken) {
            context.isPackageInstalled(candidate.packageName)
        }

    // A plain web link with no resolved metadata (manual-mode rows, and
    // info-less Play/Aurora listings) renders as a bare action instead of a
    // filled card around a single button.
    val bareLink = candidate.note == null &&
        !hasResolvedCandidateInfo &&
        !candidate.directDownload &&
        (candidate.option == CandidateOption.MANUAL ||
            candidate.source == DownloadSource.AURORA ||
            candidate.source == DownloadSource.PLAY)

    val body: @Composable ColumnScope.() -> Unit = {
        if (candidate.option != CandidateOption.MANUAL && hasResolvedCandidateInfo) {
            CandidateInfoChips(request, candidate)
        }
        if (candidate.option != CandidateOption.MANUAL && hasResolvedCandidateInfo && !match.matches) {
            CandidateMatchBox(match)
        }
        candidate.note?.let { note ->
            InfoCard(note)
        }

        if (candidate.directDownload) {
            HelperButton(
                text = "Download and return",
                onClick = onDownload,
                icon = Icons.Outlined.Download,
                modifier = Modifier.fillMaxWidth()
            )
        } else if (candidate.option == CandidateOption.MANUAL) {
            HelperButton(
                text = "Open in app",
                onClick = { onSolveCaptcha(candidate) },
                icon = Icons.Outlined.OpenInBrowser,
                modifier = Modifier.fillMaxWidth()
            )
            HelperOutlinedButton(
                text = "Pick downloaded APK",
                onClick = onPickDownloadedFile,
                icon = Icons.Outlined.FolderOpen,
                modifier = Modifier.fillMaxWidth()
            )
            if (showUseInstalledApp) {
                HelperButton(
                    text = "Use installed app",
                    onClick = onUseInstalledApp,
                    icon = Icons.Outlined.CheckCircle,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // Resolved candidates (Recommended/Latest/History) from every source
            // except Aurora and Play can fall back to the in-app captcha
            // findapps: it opens the candidate's page in a real WebView (passing
            // any Cloudflare challenge) and captures the download URL the page
            // produces. The findapps action stays here because the bottom bar
            // for these tabs shows "Find latest/requested" instead.
            if (candidate.source != DownloadSource.AURORA &&
                candidate.source != DownloadSource.PLAY
            ) {
                HelperButton(
                    text = "Solve captcha in app",
                    onClick = { onSolveCaptcha(candidate) },
                    icon = Icons.Outlined.VerifiedUser,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            HelperOutlinedButton(
                text = "Open in app",
                onClick = {
                    if (candidate.source == DownloadSource.PLAY) {
                        context.openPlayStoreListing(candidate.packageName, candidate.url)
                    } else {
                        onSolveCaptcha(candidate)
                    }
                    hasOpenedLink = true
                },
                icon = Icons.Outlined.OpenInBrowser,
                modifier = Modifier.fillMaxWidth()
            )
            if (showUseInstalledApp) {
                HelperButton(
                    text = "Use installed app",
                    onClick = onUseInstalledApp,
                    icon = Icons.Outlined.CheckCircle,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (bareLink) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
        ) {
            body()
        }
    } else {
        SurfaceCard(cornerRadius = AppDefaults.SectionCornerRadius) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDefaults.ContentPadding),
                verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
            ) {
                body()
            }
        }
    }
}

@Composable
internal fun CandidateInfoChips(request: HelperRequest, candidate: DownloadCandidate) {
    val requestedVersionNames = request.requestedVersionNames
    val requestedVersionCodes = request.requestedVersionCodes
    val versionTone = when {
        requestedVersionNames.isEmpty() -> SemanticTone.Success
        candidate.versionName != null && requestedVersionNames.any { candidate.versionName.versionNameEquals(it) } -> {
            SemanticTone.Success
        }
        else -> SemanticTone.Error
    }
    val versionCodeTone = when {
        requestedVersionCodes.isEmpty() -> SemanticTone.Success
        candidate.versionCode in requestedVersionCodes -> SemanticTone.Success
        else -> SemanticTone.Error
    }
    val formatTone = when {
        candidate.fileKind.equals("web", ignoreCase = true) -> SemanticTone.Neutral
        request.acceptsFormat(candidate.fileKind) -> SemanticTone.Success
        else -> SemanticTone.Error
    }

    StatusBadgeRow(modifier = Modifier.fillMaxWidth()) {
        candidate.versionName?.let {
            StatusBadge(text = "Version $it", tone = versionTone)
        }
        if (candidate.versionCode != null) {
            StatusBadge(text = "Code ${candidate.versionCode}", tone = versionCodeTone)
        }
        if (candidate.versionName == null && candidate.versionCode == null) {
            StatusBadge(text = candidate.versionDisplay, tone = versionTone)
        }
        if (!candidate.fileKind.equals("web", ignoreCase = true)) {
            StatusBadge(text = candidate.fileKind.uppercase(), tone = formatTone)
        }
        // APKMirror's own variant wording wins over the generic label: it names the
        // DPI even when that is `nodpi`, which is what the site shows side by side.
        (candidate.variantTag ?: candidate.variantLabel)?.let { label ->
            StatusBadge(text = label, tone = SemanticTone.Neutral)
        }
        // Only set when the release really offers more than one variant, so the
        // badge identifies which of several otherwise identical cards this is.
        candidate.variantNumber?.let { number ->
            StatusBadge(text = "Variant $number", tone = SemanticTone.Neutral)
        }
    }
}

@Composable
internal fun CandidateMatchBox(match: CandidateMatchSummary) {
    val tone = if (match.matches) SemanticTone.Success else SemanticTone.Error

    SurfaceCard(
        cornerRadius = AppDefaults.CompactCornerRadius,
        color = tone.container
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDefaults.ItemSpacing),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(match.title, color = tone.content, fontWeight = FontWeight.Bold)
            match.details.forEach { detail ->
                Text(
                    text = detail,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
