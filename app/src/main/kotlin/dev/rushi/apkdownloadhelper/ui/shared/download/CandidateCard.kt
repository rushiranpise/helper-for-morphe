package dev.rushi.apkdownloadhelper.ui.shared.download

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.download.isPackageInstalled
import dev.rushi.apkdownloadhelper.download.openPlayStoreListing
import dev.rushi.apkdownloadhelper.download.versionNameEquals
import dev.rushi.apkdownloadhelper.ui.model.CandidateMatchSummary
import dev.rushi.apkdownloadhelper.ui.model.CandidateOption
import dev.rushi.apkdownloadhelper.ui.model.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.model.HelperRequest
import dev.rushi.apkdownloadhelper.ui.model.identityKey
import dev.rushi.apkdownloadhelper.ui.model.matchSummary
import dev.rushi.apkdownloadhelper.ui.shared.chip.StatusBadge
import dev.rushi.apkdownloadhelper.ui.shared.chip.StatusBadgeRow
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.SemanticTone
import dev.rushi.apkdownloadhelper.ui.theme.pressScale

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
    val scheme = MaterialTheme.colorScheme
    val match = candidate.matchSummary(request)
    val hasResolvedCandidateInfo = candidate.versionName != null ||
            candidate.versionCode != null ||
            !candidate.fileKind.equals("web", ignoreCase = true)
    var hasOpenedLink by remember(candidate.identityKey()) { mutableStateOf(false) }
    val linkConsideredOpened = hasOpenedLink || candidate.option == CandidateOption.MANUAL
    val showUseInstalledApp = candidate.source == DownloadSource.PLAY &&
            linkConsideredOpened &&
            remember(candidate.packageName, linkConsideredOpened, installedPackageRefreshToken) {
                context.isPackageInstalled(candidate.packageName)
            }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.surfaceVariant.copy(alpha = 0.42f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (candidate.directDownload) {
            ModePrimaryPillButton(
                text = "Download and return",
                icon = Icons.Outlined.Download,
                onClick = onDownload,
                modifier = Modifier.fillMaxWidth()
            )
        } else if (candidate.option == CandidateOption.MANUAL) {
            ModePrimaryPillButton(
                text = "Pick downloaded APK",
                icon = Icons.Outlined.FolderOpen,
                onClick = onPickDownloadedFile,
                modifier = Modifier.fillMaxWidth()
            )
            if (showUseInstalledApp) {
                ModeSecondaryPillButton(
                    text = "Use installed app",
                    icon = Icons.Outlined.CheckCircle,
                    onClick = onUseInstalledApp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            if (candidate.source != DownloadSource.AURORA &&
                candidate.source != DownloadSource.PLAY
            ) {
                ModePrimaryPillButton(
                    text = "Solve captcha in app",
                    icon = Icons.Outlined.VerifiedUser,
                    onClick = { onSolveCaptcha(candidate) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ModeSecondaryPillButton(
                text = "Open in app",
                icon = Icons.Outlined.OpenInBrowser,
                onClick = {
                    if (candidate.source == DownloadSource.PLAY) {
                        context.openPlayStoreListing(candidate.packageName, candidate.url)
                    } else {
                        onSolveCaptcha(candidate)
                    }
                    hasOpenedLink = true
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (showUseInstalledApp) {
                ModePrimaryPillButton(
                    text = "Use installed app",
                    icon = Icons.Outlined.CheckCircle,
                    onClick = onUseInstalledApp,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(scheme.surfaceColorAtElevation(2.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            body()
        }
    }
}

@Composable
private fun ModePrimaryPillButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .pressScale(interaction),
        interactionSource = interaction,
        shape = CircleShape,
        border = null,
        colors = ButtonDefaults.buttonColors(
            containerColor = scheme.primaryContainer,
            contentColor = scheme.onPrimaryContainer
        ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ModeSecondaryPillButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .pressScale(interaction),
        interactionSource = interaction,
        shape = CircleShape,
        border = null,
        colors = ButtonDefaults.buttonColors(
            containerColor = scheme.surfaceVariant.copy(alpha = 0.55f),
            contentColor = scheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun CandidateInfoChips(request: HelperRequest, candidate: DownloadCandidate) {
    val requestedVersionNames = request.requestedVersionNames
    val requestedVersionCodes = request.requestedVersionCodes
    val versionTone = when {
        requestedVersionNames.isEmpty() -> SemanticTone.Primary
        candidate.versionName != null && requestedVersionNames.any { candidate.versionName.versionNameEquals(it) } -> {
            SemanticTone.Primary
        }
        else -> SemanticTone.Error
    }
    val versionCodeTone = when {
        requestedVersionCodes.isEmpty() -> SemanticTone.Primary
        candidate.versionCode in requestedVersionCodes -> SemanticTone.Primary
        else -> SemanticTone.Error
    }
    val formatTone = when {
        candidate.fileKind.equals("web", ignoreCase = true) -> SemanticTone.Neutral
        request.acceptsFormat(candidate.fileKind) -> SemanticTone.Primary
        else -> SemanticTone.Error
    }

    StatusBadgeRow(modifier = Modifier.fillMaxWidth()) {
        candidate.versionName?.let {
            StatusBadge(text = it, icon = Icons.Outlined.History, tone = versionTone)
        }
        if (candidate.versionCode != null) {
            StatusBadge(text = "Build ${candidate.versionCode}", tone = versionCodeTone)
        }
        if (candidate.versionName == null && candidate.versionCode == null) {
            StatusBadge(text = candidate.versionDisplay, icon = Icons.Outlined.History, tone = versionTone)
        }
        if (!candidate.fileKind.equals("web", ignoreCase = true)) {
            StatusBadge(text = candidate.fileKind.uppercase(), tone = formatTone)
        }
        (candidate.variantTag ?: candidate.variantLabel)?.let { label ->
            StatusBadge(text = label, tone = SemanticTone.Neutral)
        }
        candidate.variantNumber?.let { number ->
            StatusBadge(text = "Variant $number", tone = SemanticTone.Neutral)
        }
    }
}

@Composable
internal fun CandidateMatchBox(match: CandidateMatchSummary) {
    val tone = if (match.matches) SemanticTone.Primary else SemanticTone.Error

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tone.container.copy(alpha = 0.72f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = match.title,
            style = MaterialTheme.typography.labelLarge,
            color = tone.content,
            fontWeight = FontWeight.Bold
        )
        match.details.forEach { detail ->
            Text(
                text = detail,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}