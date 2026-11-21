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
        SelectedSourceBar(source = currentGroup.source)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tapping the title toggles the source cards.
                Row(
                    modifier = Modifier
                        .clip(AppDefaults.PillShape)
                        .clickable { sourcesExpanded = !sourcesExpanded }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle(text = "Download source", icon = Icons.Outlined.Dns)
                    if (!sourcesExpanded) {
                        Text(
                            text = "· ${groups.size}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = if (sourcesExpanded) {
                            Icons.Outlined.ExpandLess
                        } else {
                            Icons.Outlined.ExpandMore
                        },
                        contentDescription = if (sourcesExpanded) "Collapse sources" else "Expand sources",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.how_it_works),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(AppDefaults.PillShape)
                        .clickable { showHowItWorks = !showHowItWorks }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            // Two separate expand blocks: the "How it works" card and the source
            // grid used to share one AnimatedExpand, and when the card toggled while
            // the grid was already expanded, its content stayed at the container's
            // top and the paragraph rendered over the grid (overlapping text).
            // Keeping each in its own block means no block's content height changes
            // mid-animation, so the layout can never go stale.
            AnimatedExpand(visible = showHowItWorks) {
                InfoCard(stringResource(R.string.request_how_it_works))
            }
            Box {
                DropdownMenu(
                    expanded = sourcesExpanded,
                    onDismissRequest = { sourcesExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    groups.forEachIndexed { index, group ->
                        SourceMenuItem(
                            source = group.source,
                            selected = index == currentPage,
                            onClick = {
                                currentPage = index
                                sourcesExpanded = false
                            }
                        )
                    }
                }
            }
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
