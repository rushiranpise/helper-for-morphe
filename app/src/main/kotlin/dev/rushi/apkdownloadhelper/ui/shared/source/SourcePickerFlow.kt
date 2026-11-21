package dev.rushi.apkdownloadhelper.ui.shared.source

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.download.openPlayStoreListing
import dev.rushi.apkdownloadhelper.ui.shared.card.InfoCard
import dev.rushi.apkdownloadhelper.ui.shared.layout.AnimatedExpand
import dev.rushi.apkdownloadhelper.ui.shared.text.SectionTitle
import dev.rushi.apkdownloadhelper.ui.model.CandidateOption
import dev.rushi.apkdownloadhelper.ui.model.CandidateResult
import dev.rushi.apkdownloadhelper.ui.model.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.model.HelperRequest
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import dev.rushi.apkdownloadhelper.ui.shared.dialog.SettingsChoice
import dev.rushi.apkdownloadhelper.ui.shared.dialog.SettingsChoiceDialog
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import dev.rushi.apkdownloadhelper.ui.shared.dialog.AppAlertDialog
import dev.rushi.apkdownloadhelper.ui.shared.selection.RadioDot
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsGroup
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsItem
import androidx.compose.foundation.border
import androidx.compose.material.icons.outlined.Check
import androidx.compose.ui.graphics.Color

@Composable
internal fun SourcePickerFlow(
    request: HelperRequest,
    result: CandidateResult,
    selectedPagerPage: Int,
    onPagerPageChanged: (Int) -> Unit,
    onResolve: (DownloadSource, CandidateOption) -> Unit,
    onDownload: (DownloadCandidate) -> Unit,
    onPickDownloadedFile: (DownloadCandidate) -> Unit,
    onUseInstalledApp: (DownloadCandidate) -> Unit,
    onSolveCaptcha: (DownloadCandidate) -> Unit,
    onVersionHistory: (DownloadSource) -> Unit,
    onDownloadVersion: (DownloadCandidate) -> Unit,
    onRefresh: () -> Unit,
    onCancel: () -> Unit,
    installedPackageRefreshToken: Int,
    onPrimaryActionChanged: (PrimaryAction?) -> Unit
) {
    val groups = result.sourceGroups

    // All sources disabled: show a hint instead of crashing on an empty pager.
    if (groups.isEmpty()) {
        SideEffect { onPrimaryActionChanged(null) }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
        ) {
            InfoCard(
                "All sources are disabled. Enable at least one source in " +
                    "Settings → Sources to resolve or download."
            )
        }
        return
    }

    val initialPage = selectedPagerPage.coerceIn(0, (groups.size - 1).coerceAtLeast(0))
    // Plain state instead of a HorizontalPager: swiping on the page content used
    // to change the source, which users found accidental. Sources are now only
    // switched via the dropdown below.
    var currentPage by rememberSaveable { mutableIntStateOf(initialPage) }
    SideEffect { onPagerPageChanged(currentPage) }
    var showHowItWorks by remember { mutableStateOf(false) }
    // The source cards collapse by default so the page stays focused on the
    // selected source's content; the SelectedSourceBar below always shows
    // what's selected.
    var sourcesExpanded by rememberSaveable { mutableStateOf(false) }
    // Version type per source, so switching sources keeps the chosen mode.
    var subTabBySource by remember { mutableStateOf<Map<DownloadSource, SourceSubTab>>(emptyMap()) }

    // Clamp in case a source was disabled while this screen was showing.
    val currentGroup = groups[currentPage.coerceIn(0, groups.lastIndex)]
    val currentSubTab = subTabBySource[currentGroup.source]
        ?: defaultSubTab(currentGroup, request)

    val context = LocalContext.current
    val openCandidateLink: (DownloadCandidate) -> Unit = { candidate ->
        if (candidate.source == DownloadSource.PLAY) {
            context.openPlayStoreListing(candidate.packageName, candidate.url)
        } else {
            onSolveCaptcha(candidate)
        }
    }

    val action = remember(currentGroup, currentSubTab) {
        buildPrimaryAction(
            group = currentGroup,
            tab = currentSubTab,
            onResolve = onResolve,
            onDownload = onDownload,
            onVersionHistory = onVersionHistory,
            openLink = openCandidateLink
        )
    }
    SideEffect { onPrimaryActionChanged(action) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val scheme = MaterialTheme.colorScheme
        val chevronRotation by animateFloatAsState(
            targetValue = if (sourcesExpanded) 180f else 0f,
            animationSpec = tween(200),
            label = "source_dropdown_chevron"
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Top segment: Source Switcher
            Surface(
                onClick = { sourcesExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = 6.dp,
                    bottomEnd = 6.dp
                ),
                color = scheme.surfaceColorAtElevation(2.dp),
                contentColor = scheme.onSurface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SourceAvatar(source = currentGroup.source, size = 38.dp)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Download source · ${groups.size} available",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = currentGroup.source.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface,
                            maxLines = 1
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = "Choose source",
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(22.dp)
                            .rotate(chevronRotation)
                    )
                }
            }

            // Bottom segment: "How it works" collapsible strip
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            topStart = 6.dp,
                            topEnd = 6.dp,
                            bottomStart = 20.dp,
                            bottomEnd = 20.dp
                        )
                    )
                    .background(scheme.surfaceColorAtElevation(2.dp))
                    .clickable { showHowItWorks = !showHowItWorks }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.how_it_works),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (showHowItWorks) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                AnimatedExpand(visible = showHowItWorks) {
                    Text(
                        text = stringResource(R.string.request_how_it_works),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }

        if (sourcesExpanded) {
            AppAlertDialog(
                onDismissRequest = { sourcesExpanded = false },
                title = {
                    Text(
                        text = "Select download source",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        SettingsGroup {
                            groups.forEachIndexed { index, group ->
                                val isSelected = index == currentPage
                                SettingsItem(
                                    onClick = {
                                        currentPage = index
                                        sourcesExpanded = false
                                    },
                                    leadingContent = {
                                        SourceAvatar(source = group.source, size = AppDefaults.IconSize)
                                    },
                                    title = group.source.label,
                                    subtitle = null,
                                    trailingContent = {
                                        SourceSelectCircle(selected = isSelected)
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        SourcePageContent(
            request = request,
            group = currentGroup,
            selectedTab = subTabBySource[currentGroup.source] ?: defaultSubTab(currentGroup, request),
            onSelectTab = { tab ->
                subTabBySource = subTabBySource + (currentGroup.source to tab)
            },
            onResolve = onResolve,
            onDownload = onDownload,
            onPickDownloadedFile = onPickDownloadedFile,
            onUseInstalledApp = onUseInstalledApp,
            onSolveCaptcha = onSolveCaptcha,
            onVersionHistory = onVersionHistory,
            onDownloadVersion = onDownloadVersion,
            installedPackageRefreshToken = installedPackageRefreshToken
        )
    }
}

@Composable
private fun SourceSelectCircle(selected: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.background(Color(0xFF22C55E))
                } else {
                    Modifier.border(
                        width = 2.dp,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}