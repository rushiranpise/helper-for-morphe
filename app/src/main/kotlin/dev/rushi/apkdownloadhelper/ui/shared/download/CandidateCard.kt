package dev.rushi.apkdownloadhelper.ui.shared.download

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Warning
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
import dev.rushi.apkdownloadhelper.ui.theme.rememberMorphePillShape

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
                text = "Download",
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
            } else {
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
            }
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
    val primaryColor = MaterialTheme.colorScheme.primary
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .pressScale(interaction),
        interactionSource = interaction,
        shape = rememberMorphePillShape(interaction),
        border = null,
        colors = ButtonDefaults.buttonColors(
            containerColor = primaryColor.copy(alpha = 0.28f),
            contentColor = MaterialTheme.colorScheme.onSurface
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
    val primaryColor = MaterialTheme.colorScheme.primary
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .pressScale(interaction),
        interactionSource = interaction,
        shape = rememberMorphePillShape(interaction),
        border = null,
        colors = ButtonDefaults.buttonColors(
            containerColor = primaryColor.copy(alpha = 0.28f),
            contentColor = MaterialTheme.colorScheme.onSurface
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
    val scheme = MaterialTheme.colorScheme
    val requestedVersionNames = request.requestedVersionNames
    val requestedVersionCodes = request.requestedVersionCodes
    val versionMatches = requestedVersionNames.isEmpty() ||
            (candidate.versionName != null && requestedVersionNames.any { candidate.versionName.versionNameEquals(it) })
    val versionCodeMatches = requestedVersionCodes.isEmpty() ||
            candidate.versionCode == null ||
            candidate.versionCode in requestedVersionCodes
    val formatMatches = candidate.fileKind.equals("web", ignoreCase = true) ||
            request.acceptsFormat(candidate.fileKind)

    val versionTint = if (versionMatches) scheme.primary else scheme.error
    val versionLabel = candidate.versionName ?: candidate.versionDisplay
    val variantText = candidate.variantTag ?: candidate.variantLabel

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = versionTint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = versionLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (!candidate.fileKind.equals("web", ignoreCase = true)) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (formatMatches) scheme.primaryContainer
                            else scheme.errorContainer
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = candidate.fileKind.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (formatMatches) scheme.onPrimaryContainer else scheme.onErrorContainer,
                        maxLines = 1
                    )
                }
            }
        }

        val hasMeta = candidate.versionCode != null || variantText != null || candidate.variantNumber != null
        if (hasMeta) {
            StatusBadgeRow(modifier = Modifier.fillMaxWidth()) {
                candidate.versionCode?.let { code ->
                    StatusBadge(
                        text = "Build $code",
                        containerColor = if (versionCodeMatches) {
                            scheme.surfaceVariant.copy(alpha = 0.55f)
                        } else {
                            scheme.errorContainer.copy(alpha = 0.7f)
                        },
                        contentColor = if (versionCodeMatches) {
                            scheme.onSurfaceVariant
                        } else {
                            scheme.onErrorContainer
                        }
                    )
                }
                variantText?.let { label ->
                    StatusBadge(
                        text = label,
                        containerColor = scheme.surfaceVariant.copy(alpha = 0.55f),
                        contentColor = scheme.onSurfaceVariant
                    )
                }
                candidate.variantNumber?.let { number ->
                    StatusBadge(
                        text = "Variant $number",
                        containerColor = scheme.surfaceVariant.copy(alpha = 0.55f),
                        contentColor = scheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private val MISMATCH_DETAIL_REGEX = Regex("^([^:]+):\\s*requested\\s+(.+),\\s*found\\s+(.+)$")

@Composable
internal fun CandidateMatchBox(match: CandidateMatchSummary) {
    val scheme = MaterialTheme.colorScheme
    val isError = !match.matches
    val containerColor = if (isError) {
        scheme.errorContainer.copy(alpha = 0.42f)
    } else {
        scheme.primaryContainer.copy(alpha = 0.35f)
    }
    val rowSurfaceColor = if (isError) {
        scheme.errorContainer.copy(alpha = 0.45f)
    } else {
        scheme.primaryContainer.copy(alpha = 0.4f)
    }
    val titleColor = if (isError) scheme.error else scheme.primary
    val contentColor = if (isError) scheme.onErrorContainer else scheme.onPrimaryContainer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isError) Icons.Outlined.Warning else Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = titleColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = match.title,
                color = titleColor,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        match.details.forEach { detail ->
            val parsed = MISMATCH_DETAIL_REGEX.matchEntire(detail.trim())
            if (parsed != null) {
                val fieldLabel = parsed.groupValues[1].trim()
                val requestedValue = parsed.groupValues[2].trim()
                val foundValue = parsed.groupValues[3].trim()
                val showFieldSuffix = match.details.size > 1
                val requestedLabel = if (showFieldSuffix) "Requested ${fieldLabel.lowercase()}" else "Requested"
                val foundLabel = if (showFieldSuffix) "Found ${fieldLabel.lowercase()}" else "Found"

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(rowSurfaceColor)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = requestedLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.72f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = requestedValue,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = foundLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.72f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = foundValue,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                Text(
                    text = detail,
                    color = contentColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}