package dev.rushi.apkdownloadhelper.ui.shared.source

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import dev.rushi.apkdownloadhelper.ui.model.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.model.HelperRequest
import dev.rushi.apkdownloadhelper.ui.model.ResolveState
import dev.rushi.apkdownloadhelper.ui.model.identityKey
import dev.rushi.apkdownloadhelper.ui.shared.download.CandidateCard

internal fun LazyListScope.candidateResolveItems(
    keyPrefix: String,
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
        ResolveState.Idle,
        ResolveState.Loading -> Unit

        is ResolveState.Done -> {
            if (state.candidates.isEmpty()) {
                item(key = "${keyPrefix}_empty") {
                    ModeNoticeCard(emptyText)
                }
            } else {
                itemsIndexed(
                    items = state.candidates,
                    key = { index, candidate -> "${keyPrefix}_${candidate.identityKey()}_$index" }
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
        }

        is ResolveState.Error -> {
            item(key = "${keyPrefix}_error") {
                ModeNoticeCard(state.message)
            }
            state.fallbackCandidate?.let { candidate ->
                item(key = "${keyPrefix}_fallback_${candidate.identityKey()}") {
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
}