package dev.rushi.apkdownloadhelper.ui.shared.source

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.ui.shared.card.InfoCard
import dev.rushi.apkdownloadhelper.ui.shared.download.CandidateCard
import dev.rushi.apkdownloadhelper.ui.shared.selection.SelectorOption
import dev.rushi.apkdownloadhelper.ui.shared.selection.SelectorRow
import dev.rushi.apkdownloadhelper.ui.shared.text.SectionTitle
import dev.rushi.apkdownloadhelper.ui.shared.version.VersionHistorySection
import dev.rushi.apkdownloadhelper.ui.model.CandidateOption
import dev.rushi.apkdownloadhelper.ui.model.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.model.HelperRequest
import dev.rushi.apkdownloadhelper.ui.model.SourceCandidateGroup
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.rushi.apkdownloadhelper.ui.shared.selection.IconOptionCard
import dev.rushi.apkdownloadhelper.ui.shared.selection.OptionGroup

@Composable
internal fun SourcePageContent(
    request: HelperRequest,
    group: SourceCandidateGroup,
    selectedTab: SourceSubTab,
    onSelectTab: (SourceSubTab) -> Unit,
    onResolve: (DownloadSource, CandidateOption) -> Unit,
    onDownload: (DownloadCandidate) -> Unit,
    onPickDownloadedFile: (DownloadCandidate) -> Unit,
    onUseInstalledApp: (DownloadCandidate) -> Unit,
    onSolveCaptcha: (DownloadCandidate) -> Unit,
    onVersionHistory: (DownloadSource) -> Unit,
    onDownloadVersion: (DownloadCandidate) -> Unit,
    installedPackageRefreshToken: Int
) {
    val subTabs = remember(group, request) { subTabsFor(group, request) }
    val safeTab = subTabs.firstOrNull { it == selectedTab } ?: subTabs.firstOrNull()
        ?: SourceSubTab.Latest

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(text = "Version selection", icon = Icons.Outlined.Tune)
            VersionTypeRow(
                tabs = subTabs,
                selected = safeTab,
                onSelect = onSelectTab
            )
        }

        AboutModeCard(tab = safeTab)

        when (safeTab) {
            SourceSubTab.Manual -> {
                group.manual.forEach { candidate ->
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
                CandidateResolveSection(
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
                    DownloadSource.AURORA -> {
                        InfoCard("Aurora only provides the latest Play Store version. Use Manual mode if you need a specific version.")
                    }
                    DownloadSource.PLAY -> {
                        InfoCard("Play opens the official Play Store listing for this app. Use Manual mode if you need a specific version.")
                    }
                    else -> Unit
                }
                CandidateResolveSection(
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
                VersionHistorySection(
                    state = group.history,
                    onDownloadVersion = onDownloadVersion,
                    onSolveCaptcha = onSolveCaptcha
                )
            }
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
