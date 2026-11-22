package dev.rushi.apkdownloadhelper.ui.shared.source

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import dev.rushi.apkdownloadhelper.ui.model.CandidateOption
import dev.rushi.apkdownloadhelper.ui.model.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.model.HelperRequest
import dev.rushi.apkdownloadhelper.ui.model.ResolveState
import dev.rushi.apkdownloadhelper.ui.model.SourceCandidateGroup
import dev.rushi.apkdownloadhelper.ui.model.VersionHistoryState

internal data class PrimaryAction(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val loading: Boolean = false,
    val run: () -> Unit
)

internal fun subTabsFor(group: SourceCandidateGroup, request: HelperRequest): List<SourceSubTab> =
    buildList {
        if (group.manual.isNotEmpty()) add(SourceSubTab.Manual)
        if (request.hasKnownVersionRequest && group.source.supportsRecommended) {
            add(SourceSubTab.Recommended)
        }
        add(SourceSubTab.Latest)
        if (group.source.supportsHistory) {
            add(SourceSubTab.History)
        }
    }

internal fun defaultSubTab(group: SourceCandidateGroup, request: HelperRequest): SourceSubTab =
    subTabsFor(group, request).firstOrNull() ?: SourceSubTab.Latest

internal fun buildPrimaryAction(
    group: SourceCandidateGroup,
    tab: SourceSubTab,
    onResolve: (DownloadSource, CandidateOption) -> Unit,
    onDownload: (DownloadCandidate) -> Unit,
    onVersionHistory: (DownloadSource) -> Unit,
    openLink: (DownloadCandidate) -> Unit
): PrimaryAction = when (tab) {
    SourceSubTab.Manual -> {
        val first = group.manual.firstOrNull()
        PrimaryAction(
            label = if (first == null) "No manual link" else "Open source site",
            icon = Icons.Outlined.OpenInBrowser,
            enabled = first != null,
            run = { first?.let(openLink) }
        )
    }

    SourceSubTab.Recommended,
    SourceSubTab.Latest -> {
        val isLatest = tab == SourceSubTab.Latest
        val state = if (isLatest) group.latest else group.recommended
        val option = if (isLatest) CandidateOption.LATEST else CandidateOption.REQUESTED
        val resolveLabel = if (isLatest) "Find latest version" else "Find requested version"
        when (state) {
            ResolveState.Loading -> PrimaryAction(
                label = if (isLatest) "Checking latest..." else "Checking requested version...",
                icon = Icons.Outlined.Search,
                enabled = false,
                loading = true,
                run = {}
            )
            is ResolveState.Done -> PrimaryAction(
                label = if (isLatest) "Reload latest" else "Reload requested",
                icon = Icons.Outlined.Refresh,
                run = { onResolve(group.source, option) }
            )
            else -> PrimaryAction(
                label = resolveLabel,
                icon = Icons.Outlined.Search,
                run = { onResolve(group.source, option) }
            )
        }
    }

    SourceSubTab.History -> when (group.history) {
        VersionHistoryState.Idle -> PrimaryAction(
            label = "Load versions",
            icon = Icons.Outlined.History,
            run = { onVersionHistory(group.source) }
        )
        VersionHistoryState.Loading -> PrimaryAction(
            label = "Loading versions...",
            icon = Icons.Outlined.History,
            enabled = false,
            loading = true,
            run = {}
        )
        else -> PrimaryAction(
            label = "Reload versions",
            icon = Icons.Outlined.Refresh,
            run = { onVersionHistory(group.source) }
        )
    }
}

internal enum class SourceSubTab(
    val label: String,
    val icon: ImageVector
) {
    Manual("Manual", Icons.Outlined.Tune),
    Recommended("Recommended", Icons.Outlined.CheckCircle),
    Latest("Latest", Icons.Outlined.Star),
    History("History", Icons.Outlined.History)
}

/** Sources that can resolve the exact requested version from their own data. */
internal val DownloadSource.supportsRecommended: Boolean
    get() = when (this) {
        DownloadSource.AURORA,
        DownloadSource.PLAY -> false
        else -> true
    }

/** Sources that expose a version history list. */
internal val DownloadSource.supportsHistory: Boolean
    get() = when (this) {
        // APKCube has an old-versions page but no history list. Aurora/Play never
        // offer originals.
        DownloadSource.APK_CUBE,
        DownloadSource.AURORA,
        DownloadSource.PLAY -> false
        else -> true
    }
