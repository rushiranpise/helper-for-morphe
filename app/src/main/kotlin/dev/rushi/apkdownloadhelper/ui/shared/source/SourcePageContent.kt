package dev.rushi.apkdownloadhelper.ui.shared.source

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.ui.model.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.model.HelperRequest
import dev.rushi.apkdownloadhelper.ui.model.SourceCandidateGroup
import dev.rushi.apkdownloadhelper.ui.model.identityKey
import dev.rushi.apkdownloadhelper.ui.shared.download.CandidateCard
import dev.rushi.apkdownloadhelper.ui.shared.selection.IconOptionCard
import dev.rushi.apkdownloadhelper.ui.shared.selection.OptionGroup
import dev.rushi.apkdownloadhelper.ui.shared.text.SectionTitle
import dev.rushi.apkdownloadhelper.ui.shared.version.versionHistoryItems

internal fun LazyListScope.sourcePageItems(
    request: HelperRequest,
    group: SourceCandidateGroup,
    selectedTab: SourceSubTab,
    onSelectTab: (SourceSubTab) -> Unit,
    onDownload: (DownloadCandidate) -> Unit,
    onPickDownloadedFile: (DownloadCandidate) -> Unit,
    onUseInstalledApp: (DownloadCandidate) -> Unit,
    onSolveCaptcha: (DownloadCandidate) -> Unit,
    onDownloadVersion: (DownloadCandidate) -> Unit,
    installedPackageRefreshToken: Int
) {
    val subTabs = subTabsFor(group, request)
    val safeTab = subTabs.firstOrNull { it == selectedTab }
        ?: subTabs.firstOrNull()
        ?: SourceSubTab.Latest

    item(key = "version_selection") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(text = "Version selection", icon = Icons.Outlined.Tune)
            VersionTypeRow(
                tabs = subTabs,
                selected = safeTab,
                onSelect = onSelectTab
            )
        }
    }

    item(key = "about_mode_${safeTab.name}") {
        AboutModeCard(tab = safeTab)
    }

    when (safeTab) {
        SourceSubTab.Manual -> {
            itemsIndexed(
                items = group.manual,
                key = { index, candidate -> "manual_${candidate.identityKey()}_$index" }
            ) { _, candidate ->
                CandidateCard(
                    request = request,
                    candidate = candidate,
                    onDownload = { onDownload(candidate) },
                    onPickDownloadedFile = { onPickDownloadedFile(candidate) },
                    onUseInstalledApp = { onUseInstalledApp(candidate) },
                    onSolveCaptcha = { onSolveCaptcha(candidate) },
                    installedPackageRefreshToken = installedPackageRefreshToken
                )
            }
        }

        SourceSubTab.Recommended -> {
            candidateResolveItems(
                keyPrefix = "rec",
                request = request,
                state = group.recommended,
                emptyText = "Requested version was not found on this source. Use Manual mode for this source instead.",
                onDownload = onDownload,
                onPickDownloadedFile = onPickDownloadedFile,
                onUseInstalledApp = onUseInstalledApp,
                onSolveCaptcha = onSolveCaptcha,
                installedPackageRefreshToken = installedPackageRefreshToken
            )
        }

        SourceSubTab.Latest -> {
            when (group.source) {
                DownloadSource.AURORA -> item(key = "latest_hint_aurora") {
                    ModeNoticeCard("Aurora only provides the latest Play Store version. Use Manual mode if you need a specific version.")
                }
                DownloadSource.PLAY -> item(key = "latest_hint_play") {
                    ModeNoticeCard("Play opens the official Play Store listing for this app. Use Manual mode if you need a specific version.")
                }
                else -> Unit
            }
            candidateResolveItems(
                keyPrefix = "latest",
                request = request,
                state = group.latest,
                emptyText = "Latest version was not found on this source. Use Manual mode for this source instead.",
                onDownload = onDownload,
                onPickDownloadedFile = onPickDownloadedFile,
                onUseInstalledApp = onUseInstalledApp,
                onSolveCaptcha = onSolveCaptcha,
                installedPackageRefreshToken = installedPackageRefreshToken
            )
        }

        SourceSubTab.History -> {
            versionHistoryItems(
                state = group.history,
                onDownloadVersion = onDownloadVersion,
                onSolveCaptcha = onSolveCaptcha
            )
        }
    }
}

@Composable
internal fun VersionTypeRow(
    tabs: List<SourceSubTab>,
    selected: SourceSubTab,
    onSelect: (SourceSubTab) -> Unit
) {
    OptionGroup(items = tabs) { tab, shape, interactionSource, itemModifier ->
        val displayLabel = if (tab == SourceSubTab.Recommended) "Requested" else tab.label
        IconOptionCard(
            selected = tab == selected,
            onClick = { onSelect(tab) },
            icon = tab.icon,
            label = displayLabel,
            modifier = itemModifier,
            shape = shape,
            interactionSource = interactionSource
        )
    }
}