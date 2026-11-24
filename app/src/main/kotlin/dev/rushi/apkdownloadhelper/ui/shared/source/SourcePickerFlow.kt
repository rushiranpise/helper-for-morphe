package dev.rushi.apkdownloadhelper.ui.shared.source

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.download.openPlayStoreListing
import dev.rushi.apkdownloadhelper.ui.model.CandidateOption
import dev.rushi.apkdownloadhelper.ui.model.CandidateResult
import dev.rushi.apkdownloadhelper.ui.model.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.model.HelperRequest
import dev.rushi.apkdownloadhelper.ui.model.SourceCandidateGroup
import dev.rushi.apkdownloadhelper.ui.shared.dialog.AppAlertDialog
import dev.rushi.apkdownloadhelper.ui.shared.layout.AnimatedExpand
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsGroup
import dev.rushi.apkdownloadhelper.ui.shared.settings.SettingsItem
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults

internal class SourcePickerState(
    val groups: List<SourceCandidateGroup>,
    val currentPage: Int,
    val currentGroup: SourceCandidateGroup?,
    val currentSubTab: SourceSubTab,
    val showHowItWorks: Boolean,
    val sourcesExpanded: Boolean,
    val onSelectPage: (Int) -> Unit,
    val onToggleHowItWorks: () -> Unit,
    val onSourcesExpandedChange: (Boolean) -> Unit,
    val onSelectSubTab: (SourceSubTab) -> Unit
)

@Composable
internal fun rememberSourcePickerState(
    request: HelperRequest,
    result: CandidateResult,
    selectedPagerPage: Int,
    onPagerPageChanged: (Int) -> Unit,
    onResolve: (DownloadSource, CandidateOption) -> Unit,
    onDownload: (DownloadCandidate) -> Unit,
    onVersionHistory: (DownloadSource) -> Unit,
    onSolveCaptcha: (DownloadCandidate) -> Unit,
    onPrimaryActionChanged: (PrimaryAction?) -> Unit
): SourcePickerState {
    val groups = result.sourceGroups
    val initialPage = selectedPagerPage.coerceIn(0, (groups.size - 1).coerceAtLeast(0))
    var currentPage by rememberSaveable { mutableIntStateOf(initialPage) }
    var showHowItWorks by remember { mutableStateOf(false) }
    var sourcesExpanded by rememberSaveable { mutableStateOf(false) }
    var subTabBySource by remember { mutableStateOf<Map<DownloadSource, SourceSubTab>>(emptyMap()) }

    val safePage = currentPage.coerceIn(0, (groups.size - 1).coerceAtLeast(0))
    val currentGroup = groups.getOrNull(safePage)
    val currentSubTab = currentGroup?.let { group ->
        subTabBySource[group.source] ?: defaultSubTab(group, request)
    } ?: SourceSubTab.Latest

    SideEffect {
        if (groups.isNotEmpty()) onPagerPageChanged(safePage)
    }

    val context = LocalContext.current
    val openCandidateLink: (DownloadCandidate) -> Unit = { candidate ->
        if (candidate.source == DownloadSource.PLAY) {
            context.openPlayStoreListing(candidate.packageName, candidate.url)
        } else {
            onSolveCaptcha(candidate)
        }
    }

    val currentOnResolve by rememberUpdatedState(onResolve)
    val currentOnDownload by rememberUpdatedState(onDownload)
    val currentOnVersionHistory by rememberUpdatedState(onVersionHistory)
    val currentOpenLink by rememberUpdatedState(openCandidateLink)

    val action = remember(currentGroup, currentSubTab) {
        currentGroup?.let { group ->
            buildPrimaryAction(
                group = group,
                tab = currentSubTab,
                onResolve = { s, o -> currentOnResolve(s, o) },
                onDownload = { c -> currentOnDownload(c) },
                onVersionHistory = { s -> currentOnVersionHistory(s) },
                openLink = { c -> currentOpenLink(c) }
            )
        }
    }
    SideEffect { onPrimaryActionChanged(action) }

    return SourcePickerState(
        groups = groups,
        currentPage = safePage,
        currentGroup = currentGroup,
        currentSubTab = currentSubTab,
        showHowItWorks = showHowItWorks,
        sourcesExpanded = sourcesExpanded,
        onSelectPage = { index ->
            currentPage = index
            sourcesExpanded = false
        },
        onToggleHowItWorks = { showHowItWorks = !showHowItWorks },
        onSourcesExpandedChange = { sourcesExpanded = it },
        onSelectSubTab = { tab ->
            currentGroup?.let { group ->
                subTabBySource = subTabBySource + (group.source to tab)
            }
        }
    )
}

internal fun LazyListScope.sourcePickerItems(
    state: SourcePickerState,
    request: HelperRequest,
    onDownload: (DownloadCandidate) -> Unit,
    onPickDownloadedFile: (DownloadCandidate) -> Unit,
    onUseInstalledApp: (DownloadCandidate) -> Unit,
    onSolveCaptcha: (DownloadCandidate) -> Unit,
    onDownloadVersion: (DownloadCandidate) -> Unit,
    installedPackageRefreshToken: Int
) {
    val currentGroup = state.currentGroup
    if (state.groups.isEmpty() || currentGroup == null) {
        item(key = "all_sources_disabled") {
            ModeNoticeCard(
                "All sources are disabled. Enable at least one source in " +
                        "Settings → Sources to resolve or download."
            )
        }
        return
    }

    item(key = "source_switcher") {
        SourceSwitcherCard(
            currentGroup = currentGroup,
            sourceCount = state.groups.size,
            sourcesExpanded = state.sourcesExpanded,
            showHowItWorks = state.showHowItWorks,
            onOpenSources = { state.onSourcesExpandedChange(true) },
            onToggleHowItWorks = state.onToggleHowItWorks
        )
    }

    sourcePageItems(
        request = request,
        group = currentGroup,
        selectedTab = state.currentSubTab,
        onSelectTab = state.onSelectSubTab,
        onDownload = onDownload,
        onPickDownloadedFile = onPickDownloadedFile,
        onUseInstalledApp = onUseInstalledApp,
        onSolveCaptcha = onSolveCaptcha,
        onDownloadVersion = onDownloadVersion,
        installedPackageRefreshToken = installedPackageRefreshToken
    )
}

@Composable
internal fun SourcePickerDialog(state: SourcePickerState) {
    if (!state.sourcesExpanded || state.groups.isEmpty()) return

    AppAlertDialog(
        onDismissRequest = { state.onSourcesExpandedChange(false) },
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
                    state.groups.forEachIndexed { index, group ->
                        val isSelected = index == state.currentPage
                        SettingsItem(
                            onClick = { state.onSelectPage(index) },
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

@Composable
private fun SourceSwitcherCard(
    currentGroup: SourceCandidateGroup,
    sourceCount: Int,
    sourcesExpanded: Boolean,
    showHowItWorks: Boolean,
    onOpenSources: () -> Unit,
    onToggleHowItWorks: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val chevronRotation by animateFloatAsState(
        targetValue = if (sourcesExpanded) 180f else 0f,
        animationSpec = tween(200),
        label = "source_dropdown_chevron"
    )
    val howItWorksChevron by animateFloatAsState(
        targetValue = if (showHowItWorks) 180f else 0f,
        animationSpec = tween(200),
        label = "how_it_works_chevron"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Surface(
            onClick = onOpenSources,
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
                        text = "Download source · $sourceCount available",
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
                .clickable(onClick = onToggleHowItWorks)
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
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(howItWorksChevron)
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