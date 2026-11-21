package dev.rushi.apkdownloadhelper.ui.shared.source

import androidx.compose.runtime.Composable
import dev.rushi.apkdownloadhelper.ui.shared.card.InfoCard
import dev.rushi.apkdownloadhelper.ui.shared.download.CandidateCard
import dev.rushi.apkdownloadhelper.ui.model.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.model.HelperRequest
import dev.rushi.apkdownloadhelper.ui.model.ResolveState

@Composable
internal fun CandidateResolveSection(
    request: HelperRequest,
    state: ResolveState,
    emptyText: String,
    onDownload: (DownloadCandidate) -> Unit,
    onPickDownloadedFile: (DownloadCandidate) -> Unit,
    onUseInstalledApp: (DownloadCandidate) -> Unit,
    onSolveCaptcha: (DownloadCandidate) -> Unit,
    installedPackageRefreshToken: Int
) {
    when (state) {
        // Idle and Loading are driven entirely by the bottom-bar primary
        // action button; the content area shows results only.
        ResolveState.Idle,
        ResolveState.Loading -> Unit

        is ResolveState.Done -> {
            if (state.candidates.isEmpty()) {
                InfoCard(emptyText)
            } else {
                state.candidates.forEach { candidate ->
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
        }

        is ResolveState.Error -> {
            InfoCard(state.message)
            state.fallbackCandidate?.let { candidate ->
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
    }
}
