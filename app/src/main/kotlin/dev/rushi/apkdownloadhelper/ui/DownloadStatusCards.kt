package dev.rushi.apkdownloadhelper.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.HelperButton
import dev.rushi.apkdownloadhelper.HelperOutlinedButton
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.download.VirusTotalScanner
import dev.rushi.apkdownloadhelper.download.isPackageInstalled
import dev.rushi.apkdownloadhelper.download.openPlayStoreListing
import dev.rushi.apkdownloadhelper.download.versionNameEquals
import dev.rushi.apkdownloadhelper.storage.FastModePolicy
import kotlinx.coroutines.delay
import java.util.Locale


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
    val match = candidate.matchSummary(request)
    val hasResolvedCandidateInfo = candidate.versionName != null ||
        candidate.versionCode != null ||
        !candidate.fileKind.equals("web", ignoreCase = true)
    var hasOpenedLink by remember(candidate.identityKey()) { mutableStateOf(false) }
    // Manual-mode links are opened by the bottom-bar primary action, so the
    // "installed app" offer applies as soon as the row renders.
    val linkConsideredOpened = hasOpenedLink || candidate.option == CandidateOption.MANUAL
    val showUseInstalledApp = candidate.source == DownloadSource.PLAY &&
        linkConsideredOpened &&
        remember(candidate.packageName, linkConsideredOpened, installedPackageRefreshToken) {
            context.isPackageInstalled(candidate.packageName)
        }

    // A plain web link with no resolved metadata (manual-mode rows, and
    // info-less Play/Aurora listings) renders as a bare action instead of a
    // filled card around a single button.
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
            InfoCard(note)
        }

        if (candidate.directDownload) {
            HelperButton(
                text = "Download and return",
                onClick = onDownload,
                icon = Icons.Outlined.Download,
                modifier = Modifier.fillMaxWidth()
            )
        } else if (candidate.option == CandidateOption.MANUAL) {
            HelperButton(
                text = "Open in app",
                onClick = { onSolveCaptcha(candidate) },
                icon = Icons.Outlined.OpenInBrowser,
                modifier = Modifier.fillMaxWidth()
            )
            HelperOutlinedButton(
                text = "Pick downloaded APK",
                onClick = onPickDownloadedFile,
                icon = Icons.Outlined.FolderOpen,
                modifier = Modifier.fillMaxWidth()
            )
            if (showUseInstalledApp) {
                HelperButton(
                    text = "Use installed app",
                    onClick = onUseInstalledApp,
                    icon = Icons.Outlined.CheckCircle,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // Resolved candidates (Recommended/Latest/History) from every source
            // except Aurora and Play can fall back to the in-app captcha
            // browser: it opens the candidate's page in a real WebView (passing
            // any Cloudflare challenge) and captures the download URL the page
            // produces. The browser action stays here because the bottom bar
            // for these tabs shows "Find latest/requested" instead.
            if (candidate.source != DownloadSource.AURORA &&
                candidate.source != DownloadSource.PLAY
            ) {
                HelperButton(
                    text = "Solve captcha in app",
                    onClick = { onSolveCaptcha(candidate) },
                    icon = Icons.Outlined.VerifiedUser,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            HelperOutlinedButton(
                text = "Open in app",
                onClick = {
                    if (candidate.source == DownloadSource.PLAY) {
                        context.openPlayStoreListing(candidate.packageName, candidate.url)
                    } else {
                        onSolveCaptcha(candidate)
                    }
                    hasOpenedLink = true
                },
                icon = Icons.Outlined.OpenInBrowser,
                modifier = Modifier.fillMaxWidth()
            )
            if (showUseInstalledApp) {
                HelperButton(
                    text = "Use installed app",
                    onClick = onUseInstalledApp,
                    icon = Icons.Outlined.CheckCircle,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (bareLink) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
        ) {
            body()
        }
    } else {
        SurfaceCard(cornerRadius = MorpheDefaults.SectionCornerRadius) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MorpheDefaults.ContentPadding),
                verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
            ) {
                body()
            }
        }
    }
}

@Composable
internal fun CandidateInfoChips(request: HelperRequest, candidate: DownloadCandidate) {
    val requestedVersionNames = request.requestedVersionNames
    val requestedVersionCodes = request.requestedVersionCodes
    val versionTone = when {
        requestedVersionNames.isEmpty() -> SemanticTone.Success
        candidate.versionName != null && requestedVersionNames.any { candidate.versionName.versionNameEquals(it) } -> {
            SemanticTone.Success
        }
        else -> SemanticTone.Error
    }
    val versionCodeTone = when {
        requestedVersionCodes.isEmpty() -> SemanticTone.Success
        candidate.versionCode in requestedVersionCodes -> SemanticTone.Success
        else -> SemanticTone.Error
    }
    val formatTone = when {
        candidate.fileKind.equals("web", ignoreCase = true) -> SemanticTone.Neutral
        request.acceptsFormat(candidate.fileKind) -> SemanticTone.Success
        else -> SemanticTone.Error
    }

    MorpheStatusBadgeRow(modifier = Modifier.fillMaxWidth()) {
        candidate.versionName?.let {
            MorpheStatusBadge(text = "Version $it", tone = versionTone)
        }
        if (candidate.versionCode != null) {
            MorpheStatusBadge(text = "Code ${candidate.versionCode}", tone = versionCodeTone)
        }
        if (candidate.versionName == null && candidate.versionCode == null) {
            MorpheStatusBadge(text = candidate.versionDisplay, tone = versionTone)
        }
        if (!candidate.fileKind.equals("web", ignoreCase = true)) {
            MorpheStatusBadge(text = candidate.fileKind.uppercase(), tone = formatTone)
        }
        // APKMirror's own variant wording wins over the generic label: it names the
        // DPI even when that is `nodpi`, which is what the site shows side by side.
        (candidate.variantTag ?: candidate.variantLabel)?.let { label ->
            MorpheStatusBadge(text = label, tone = SemanticTone.Neutral)
        }
        // Only set when the release really offers more than one variant, so the
        // badge identifies which of several otherwise identical cards this is.
        candidate.variantNumber?.let { number ->
            MorpheStatusBadge(text = "Variant $number", tone = SemanticTone.Neutral)
        }
    }
}

@Composable
internal fun CandidateMatchBox(match: CandidateMatchSummary) {
    val tone = if (match.matches) SemanticTone.Success else SemanticTone.Error

    SurfaceCard(
        cornerRadius = MorpheDefaults.CompactCornerRadius,
        color = tone.container
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MorpheDefaults.ItemSpacing),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(match.title, color = tone.content, fontWeight = FontWeight.Bold)
            match.details.forEach { detail ->
                Text(
                    text = detail,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
internal fun CheckingPickedFileState(state: UiState.CheckingPickedFile) {
    SurfaceCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MorpheDefaults.ContentPadding),
            horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.checking_selected_file))
                Text(
                    text = state.candidate.source.label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
internal fun FastModeCard(
    progress: FastModeProgress,
    availableSources: List<DownloadSource>,
    onCancel: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSwitchSource: (DownloadSource) -> Unit,
    onSkipWait: () -> Unit,
    onUseMismatch: () -> Unit,
    onSkipMismatch: () -> Unit,
    onSkipScan: () -> Unit,
    onChooseVersion: (FastModePolicy) -> Unit,
    canRetryFailedSources: Boolean,
    onRetryAll: () -> Unit,
    onRetryFailedSources: () -> Unit,
    onCopyFailureDetails: () -> Unit
) {
    var showSwitch by remember { mutableStateOf(false) }
    var resumeOnDismiss by remember { mutableStateOf(false) }

    if (showSwitch) {
        AlertDialog(
            onDismissRequest = {
                showSwitch = false
                if (resumeOnDismiss) onResume()
                resumeOnDismiss = false
            },
            title = { Text("Switch source") },
            text = {
                Column {
                    availableSources
                        .filter { it.label != progress.sourceLabel }
                        .forEach { source ->
                            TextButton(
                                onClick = {
                                    showSwitch = false
                                    resumeOnDismiss = false
                                    onSwitchSource(source)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(source.label)
                            }
                        }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showSwitch = false
                        if (resumeOnDismiss) onResume()
                        resumeOnDismiss = false
                    }
                ) { Text("Cancel") }
            }
        )
    }

    SurfaceCard(cornerRadius = MorpheDefaults.SectionCornerRadius) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MorpheDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        progress.title ?: stringResource(R.string.fast_mode),
                        fontWeight = FontWeight.Bold
                    )
                    progress.sourceLabel?.let { source ->
                        Text(
                            text = source,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                if (!progress.done && progress.percent == null && !progress.awaitingDecision) {
                    MorpheWavyProgressRing(
                        progress = null,
                        modifier = Modifier.size(30.dp),
                        strokeWidth = 3.dp
                    )
                }
            }
            Text(
                text = progress.detail,
                color = when {
                    progress.done && !progress.succeeded -> MaterialTheme.colorScheme.error
                    progress.done -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.bodyMedium
            )
            // Persist the SHA-256 verification on the card once the file's
            // bytes matched the source-published hash (not just the transient
            // post-download status line).
            if (progress.shaVerified) {
                // Read outside the semantics block: that lambda is not a composable one, so a
                // resource cannot be resolved inside it.
                val verifiedLabel = stringResource(R.string.sha_256_verified)
                MorpheStatusBadge(
                    text = null,
                    icon = Icons.Outlined.Verified,
                    tone = SemanticTone.Primary,
                    modifier = Modifier.semantics { contentDescription = verifiedLabel }
                )
            }
            if (progress.awaitingVersionChoice) {
                Surface(
                    color = SemanticTone.Primary.container,
                    contentColor = SemanticTone.Primary.content,
                    shape = RoundedCornerShape(MorpheDefaults.CompactCornerRadius)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MorpheDefaults.ContentPadding),
                        verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
                    ) {
                        IconTextRow(
                            leadingContent = {
                                ThemedIcon(icon = Icons.Outlined.Tune, size = 20.dp)
                            },
                            title = progress.versionChoiceDetail
                                ?: "Which version should Fast Mode fetch?",
                            titleStyle = MaterialTheme.typography.bodyMedium,
                            titleWeight = FontWeight.Normal,
                            titleColor = SemanticTone.Primary.content
                        )
                        progress.versionChoiceRequested?.let { requested ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.requested),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SemanticTone.Primary.content.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = requested,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
                ) {
                    HelperButton(
                        text = "Requested version",
                        onClick = { onChooseVersion(FastModePolicy.REQUESTED) },
                        modifier = Modifier.weight(1f)
                    )
                    HelperButton(
                        text = "Latest version",
                        onClick = { onChooseVersion(FastModePolicy.LATEST) },
                        modifier = Modifier.weight(1f)
                    )
                }
                HelperOutlinedButton(
                    text = "Cancel",
                    onClick = onCancel,
                    icon = Icons.Outlined.Close,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (progress.awaitingDecision) {
                val captchaDecision = progress.awaitingCaptcha
                val decisionTone = if (captchaDecision) SemanticTone.Primary else SemanticTone.Error
                Surface(
                    color = decisionTone.container,
                    contentColor = decisionTone.content,
                    shape = RoundedCornerShape(MorpheDefaults.CompactCornerRadius)
                ) {
                    IconTextRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MorpheDefaults.ContentPadding),
                        leadingContent = {
                            ThemedIcon(
                                icon = if (captchaDecision) Icons.Outlined.Shield else Icons.Outlined.Warning,
                                size = 20.dp,
                                tint = decisionTone.accent
                            )
                        },
                        title = progress.mismatchDetail ?: "Version code mismatch.",
                        titleStyle = MaterialTheme.typography.bodyMedium,
                        titleWeight = FontWeight.Normal,
                        titleColor = decisionTone.content
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
                ) {
                    HelperButton(
                        text = if (captchaDecision) "Solve captcha" else "Use this version",
                        onClick = onUseMismatch,
                        modifier = Modifier.weight(1f)
                    )
                    HelperOutlinedButton(
                        text = "Skip to next source",
                        onClick = onSkipMismatch,
                        modifier = Modifier.weight(1f)
                    )
                }
                HelperOutlinedButton(
                    text = "Cancel",
                    onClick = onCancel,
                    icon = Icons.Outlined.Close,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (progress.percent != null && !progress.done) {
                val percent = progress.percent
                val isScan = isScanStatus(progress.detail)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DownloadProgressGauge(
                        percent = percent,
                        paused = progress.paused,
                        modifier = Modifier.size(104.dp)
                    )
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTransferStats(
                                connections = progress.connections,
                                speedBytesPerSec = progress.speedBytesPerSec,
                                etaMs = progress.etaMs,
                                paused = progress.paused,
                                includeConnections = !isScan && percent < 100
                            ).ifBlank { if (progress.paused) "Paused" else "Downloading" },
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Medium
                        )
                        HelperIconButton(
                            icon = Icons.Outlined.Close,
                            contentDescription = "Cancel download",
                            onClick = onCancel
                        )
                    }
                }
                if (!isScanStatus(progress.detail) && progress.percent < 100) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HelperOutlinedButton(
                            text = if (progress.paused) "Resume" else "Pause",
                            onClick = if (progress.paused) onResume else onPause,
                            modifier = Modifier.weight(1f),
                            icon = if (progress.paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause
                        )
                        HelperOutlinedButton(
                            text = "Switch source",
                            onClick = {
                                resumeOnDismiss = !progress.paused
                                if (!progress.paused) onPause()
                                showSwitch = true
                            },
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.SwapHoriz
                        )
                    }
                }
                SkipWaitButton(
                    active = isRateLimitWait(progress.detail),
                    onSkipWait = onSkipWait
                )
                if (isScanStatus(progress.detail)) {
                    HelperButton(
                        text = "Skip scan & hand off",
                        onClick = onSkipScan,
                        icon = Icons.Outlined.Shield,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else if (!progress.done) {
                HelperOutlinedButton(
                    text = "Cancel",
                    onClick = onCancel,
                    icon = Icons.Outlined.Close,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (!progress.succeeded) {
                FailureActions(
                    canRetryFailedSources = canRetryFailedSources,
                    onRetryAll = onRetryAll,
                    onRetryFailedSources = onRetryFailedSources,
                    onCopyDetails = onCopyFailureDetails,
                    onCancel = onCancel
                )
            }
        }
    }
}

// True when the scan is paused on the app's own pacing gap ("Waiting Ns for
// rate limit…") — the only wait the Skip button can meaningfully cut short.
// Match the pause anywhere in the status: the bundle scanner prefixes inner
// APK statuses with "APK 2 of 4 (split_1.apk): …", so a plain prefix check
// would never see the "Waiting Ns for rate limit…" inside.
//
// A genuine 429 ("Rate limit hit — retrying in Ns…") is deliberately NOT
// matched: skipping it is futile (the server keeps rejecting until the 60s
// window clears) and its countdown comes from the window, not the gap, so the
// Skip button would show a wrong number. That wait auto-retries instead.
internal fun isRateLimitWait(status: String): Boolean =
    status.contains("waiting", ignoreCase = true) &&
        status.contains("rate limit", ignoreCase = true)

/** True when a status message belongs to the VirusTotal scan phase (as opposed
 *  to the download phase that reuses the same card). */
internal fun isScanStatus(status: String?): Boolean =
    status?.let {
        it.startsWith("APK ") || it.startsWith("Extract") ||
            it.startsWith("Opening") || it.startsWith("Upload") ||
            it.startsWith("Waiting") || it.startsWith("Aggregat") ||
            it.startsWith("Scanning") || it.startsWith("Checking VirusTotal") ||
            it.startsWith("Queued") || it.startsWith("VirusTotal engines")
    } == true

/**
 * "Skip wait" with a live countdown of the seconds left in the current
 * rate-limit pause. While the scan status shows a wait, this re-reads the
 * shared limiter's remaining gap each second and shows "Skip wait · 12s";
 * once the pause ends the button disappears. Tapping skips the wait early.
 */
@Composable
internal fun SkipWaitButton(active: Boolean, onSkipWait: () -> Unit) {
    var secondsLeft by remember { mutableIntStateOf(0) }
    // Flips when the user taps: hides the button immediately so the tap gives
    // visible feedback even though the parent's "Waiting…" status (and hence
    // `active`) stays true until the scan's next network result arrives.
    var skipped by remember { mutableStateOf(false) }
    LaunchedEffect(active) {
        if (!active) {
            skipped = false
            return@LaunchedEffect
        }
        while (!skipped) {
            // Re-read from the limiter each tick so the countdown stays honest
            // even if the status message lags a second behind the real pause.
            secondsLeft = ((VirusTotalScanner.rateLimiter.millisUntilNextSlot() + 999) / 1000).toInt()
            if (secondsLeft <= 0) break
            delay(1000)
        }
    }
    if (active && !skipped) {
        HelperOutlinedButton(
            text = if (secondsLeft > 0) "Skip wait · ${secondsLeft}s" else "Skip wait",
            onClick = {
                skipped = true
                onSkipWait()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

internal fun scanPhaseSplit(status: String): Pair<String, String> {
    val apk = Regex("""APK (\d+) of (\d+) \(([^)]+)\):? ?(.*)""").find(status)
    if (apk != null) {
        val (_, _, name, rest) = apk.destructured
        val phase = "Scanning APK ${apk.groupValues[1]} of ${apk.groupValues[2]}"
        return phase to rest.ifBlank { name }
    }
    val extract = Regex("""Extract(ing|ed) ([^:]+)""").find(status)
    if (extract != null) {
        val action = extract.groupValues[1]  // ing | ed
        val body = extract.groupValues[2]
        return "Extract$action ${body.take(30)}" to (status.removePrefix(extract.value).trim())
    }
    // Everything else: first clause is the phase (before "·"/":"), rest is detail.
    val sep = status.indexOfFirst { it == '·' || it == ':' }
    return if (sep > 0 && sep < status.length - 1) {
        (status.substring(0, sep).trim().replaceFirstChar { it.uppercase() }) to
            status.substring(sep + 1).trim()
    } else {
        status to ""
    }
}

@Composable
internal fun DownloadingState(
    state: UiState.Downloading,
    availableSources: List<DownloadSource>,
    onCancel: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSwitchSource: (DownloadSource) -> Unit,
    onSkipWait: () -> Unit,
    onSkipScan: () -> Unit
) {
    var showSwitch by remember { mutableStateOf(false) }
    var resumeOnDismiss by remember { mutableStateOf(false) }

    if (showSwitch) {
        AlertDialog(
            onDismissRequest = {
                showSwitch = false
                if (resumeOnDismiss) onResume()
                resumeOnDismiss = false
            },
            title = { Text("Switch source") },
            text = {
                Column {
                    availableSources
                        .filter { it != state.candidate.source }
                        .forEach { source ->
                            TextButton(
                                onClick = {
                                    showSwitch = false
                                    resumeOnDismiss = false
                                    onSwitchSource(source)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(source.label)
                            }
                        }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showSwitch = false
                        if (resumeOnDismiss) onResume()
                        resumeOnDismiss = false
                    }
                ) { Text("Cancel") }
            }
        )
    }

    SurfaceCard {
        Column(
            modifier = Modifier.padding(MorpheDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
        ) {
            val statusText = state.statusMessage
                ?: if (state.paused) "Paused" else "Downloading from ${state.candidate.source.label}"
            val isScan = isScanStatus(statusText)
            val (phase, detail) = if (isScan) scanPhaseSplit(statusText) else (statusText to "")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DownloadProgressGauge(
                    percent = state.percent,
                    paused = state.paused,
                    modifier = Modifier.size(104.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = phase,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        HelperIconButton(
                            icon = Icons.Outlined.Close,
                            contentDescription = "Cancel download",
                            onClick = onCancel
                        )
                    }
                    if (detail.isNotBlank()) {
                        Text(
                            text = detail,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    val stats = formatTransferStats(
                        connections = state.connections,
                        speedBytesPerSec = state.speedBytesPerSec,
                        etaMs = state.etaMs,
                        paused = state.paused,
                        includeConnections = !isScan && state.percent < 100
                    )
                    if (stats.isNotBlank()) {
                        Text(
                            text = stats,
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            if (!isScan && state.percent < 100) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HelperOutlinedButton(
                        text = if (state.paused) "Resume" else "Pause",
                        onClick = if (state.paused) onResume else onPause,
                        modifier = Modifier.weight(1f),
                        icon = if (state.paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause
                    )
                    HelperOutlinedButton(
                        text = "Switch source",
                        onClick = {
                            resumeOnDismiss = !state.paused
                            if (!state.paused) onPause()
                            showSwitch = true
                        },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.SwapHoriz
                    )
                }
            }
            SkipWaitButton(
                active = isRateLimitWait(statusText),
                onSkipWait = onSkipWait
            )
            if (isScan) {
                HelperButton(
                    text = "Skip scan & hand off",
                    onClick = onSkipScan,
                    icon = Icons.Outlined.Shield,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.hand_the_downloaded_file_to_morphe),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun DownloadProgressGauge(
    percent: Int,
    paused: Boolean,
    modifier: Modifier = Modifier
) {
    val shown = percent.coerceIn(0, 100)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        MorpheWavyProgressRing(
            progress = { shown / 100f },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 7.dp
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$shown%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (paused) {
                Text(
                    text = "Paused",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatTransferStats(
    connections: Int,
    speedBytesPerSec: Double,
    etaMs: Long?,
    paused: Boolean,
    includeConnections: Boolean
): String {
    val parts = buildList {
        if (includeConnections) {
            add(
                "$connections " +
                    if (connections == 1) "connection" else "connections"
            )
        }
        if (!paused) {
            formatTransferSpeed(speedBytesPerSec)
                .takeIf { it.isNotEmpty() }
                ?.let(::add)
            if (etaMs != null && etaMs > 0L) {
                add("${formatTransferEta(etaMs)} left")
            }
        }
    }
    return parts.joinToString("  ·  ")
}

@Composable
internal fun ScanAskCard(
    candidate: DownloadCandidate,
    onScan: () -> Unit,
    onSkip: () -> Unit
) {
    SectionCard {
        Column(
            modifier = Modifier.padding(MorpheDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
        ) {
            MorpheSectionTitle(text = "Scan with VirusTotal?", icon = Icons.Outlined.Shield)
            Text(
                text = "Check ${candidate.name} with VirusTotal before returning it to Morphe.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HelperButton(
                    text = "Scan",
                    onClick = onScan,
                    modifier = Modifier.weight(1f)
                )
                HelperOutlinedButton(
                    text = "Skip",
                    onClick = onSkip,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

internal fun scanResultDetail(scanResult: VirusTotalScanner.ScanResult): String = when (scanResult) {
    is VirusTotalScanner.ScanResult.Clean ->
        if (scanResult.scannedFiles != null) {
            "All ${scanResult.scannedFiles} APKs in the bundle are clean — " +
                "0 of ${scanResult.totalEngines} engines flagged them"
        } else {
            "0 of ${scanResult.totalEngines} antivirus engines flagged this file"
        }
    is VirusTotalScanner.ScanResult.Malicious ->
        if (scanResult.scannedFiles != null) {
            "${scanResult.flaggedFiles ?: 1} of ${scanResult.scannedFiles} APKs in the " +
                "bundle flagged — ${scanResult.detections} of ${scanResult.totalEngines} engines"
        } else {
            "${scanResult.detections} of ${scanResult.totalEngines} antivirus engines flagged this file"
        }
    is VirusTotalScanner.ScanResult.Error ->
        "Scan error: ${scanResult.message}"
}

internal fun openVirusTotalPage(context: Context, sha256: String) {
    val open = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://www.virustotal.com/gui/file/$sha256")
    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    runCatching { context.startActivity(open) }
        .onFailure {
            Toast.makeText(context, "No browser available", Toast.LENGTH_SHORT).show()
        }
}

@Composable
internal fun ScanResultCard(
    scanResult: VirusTotalScanner.ScanResult,
    detail: String,
    isMalicious: Boolean,
    onProceed: () -> Unit,
    onCancel: () -> Unit,
    readOnly: Boolean = false,
    // True if the downloaded file's bytes matched the source-published SHA-256.
    shaVerified: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val isError = scanResult is VirusTotalScanner.ScanResult.Error
    val tone = when {
        isMalicious -> SemanticTone.Error
        isError -> SemanticTone.Warning
        else -> SemanticTone.Success
    }

    SurfaceCard(
        cornerRadius = MorpheDefaults.SectionCornerRadius,
        borderWidth = 1.dp,
        borderColor = tone.accent.copy(alpha = 0.45f),
        color = tone.container.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.padding(MorpheDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemedIcon(
                    icon = when {
                        isMalicious || isError -> Icons.Outlined.Warning
                        else -> Icons.Outlined.CheckCircle
                    },
                    size = 20.dp,
                    tint = tone.accent
                )
                Text(
                    text = when {
                        isMalicious -> "VirusTotal — Threats detected"
                        isError -> "VirusTotal — Scan error"
                        else -> "VirusTotal — Clean"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (!isError) {
                    MorpheStatusBadge(
                        text = if (scanResult.cached) "Cached report" else "Fresh scan",
                        tone = if (scanResult.cached) SemanticTone.Primary else SemanticTone.Neutral
                    )
                }
                if (shaVerified) {
                    // As above: Modifier.semantics takes a plain lambda, so the resource is read
                    // before it rather than inside it.
                    val verifiedLabel = stringResource(R.string.sha_256_verified)
                    MorpheStatusBadge(
                        text = null,
                        icon = Icons.Outlined.Verified,
                        tone = SemanticTone.Primary,
                        modifier = Modifier.semantics { contentDescription = verifiedLabel }
                    )
                }
            }
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (scanResult is VirusTotalScanner.ScanResult.Malicious) {
                scanResult.suggestedThreatLabel?.let { label ->
                    Text(
                        text = "Detected as: $label",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (scanResult.engines.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        scanResult.engines.take(8).forEach { hit ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = hit.engine,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(0.38f)
                                )
                                Text(
                                    text = hit.result,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(0.62f)
                                )
                            }
                        }
                        if (scanResult.engines.size > 8) {
                            Text(
                                text = "+${scanResult.engines.size - 8} more engines",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (isMalicious) {
                Text(
                    text = stringResource(R.string.this_file_was_flagged_by_antivirus) +
                        "Only proceed if you trust the source.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (isError) {
                Text(
                    text = stringResource(R.string.the_scan_did_not_complete_network) +
                        "You can still proceed, or cancel to keep the file out of Morphe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (scanResult.apkResults.isNotEmpty()) {
                MorpheDivider(modifier = Modifier.padding(vertical = 4.dp), fullWidth = true)
                Text(
                    text = stringResource(R.string.apk_details),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                scanResult.apkResults.forEach { apk ->
                    val apkVerdictColor = when {
                        apk.failed -> colors.onSurfaceVariant
                        apk.detections > 0 -> colors.error
                        else -> colors.primary
                    }
                    val apkIcon = when {
                        apk.failed -> Icons.Outlined.HelpOutline
                        apk.detections > 0 -> Icons.Outlined.Warning
                        else -> Icons.Outlined.CheckCircle
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemedIcon(
                            icon = apkIcon,
                            size = 16.dp,
                            tint = apkVerdictColor
                        )
                        Text(
                            text = apk.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        // Distinguish a cached report (VirusTotal already had it) from a
                        // fresh upload so users can tell why an APK's scan was instant.
                        if (!apk.failed) {
                            MorpheStatusBadge(
                                text = if (apk.cached) "cached" else "fresh",
                                tone = if (apk.cached) SemanticTone.Primary else SemanticTone.Neutral
                            )
                        }
                        Text(
                            text = if (apk.failed) "failed" else "${apk.detections}/${apk.totalEngines}",
                            style = MaterialTheme.typography.bodySmall,
                            color = apkVerdictColor
                        )
                        if (apk.sha256 != null) {
                            ThemedIcon(
                                icon = Icons.Outlined.OpenInNew,
                                contentDescription = "Open ${apk.name} in VirusTotal",
                                size = 16.dp,
                                tint = colors.primary,
                                modifier = Modifier
                                    .clip(MorpheDefaults.PillShape)
                                    .clickable { openVirusTotalPage(context, apk.sha256) }
                                    .padding(2.dp)
                            )
                        }
                    }
                    if (apk.detections > 0 && apk.engines.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 22.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            apk.engines.take(3).forEach { hit ->
                                Text(
                                    text = "${hit.engine} → ${hit.result}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.error,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (apk.engines.size > 3) {
                                Text(
                                    text = "+${apk.engines.size - 3} more engines",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            ScanMetaRows(scanResult)
            if (!readOnly) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HelperButton(
                        text = "Proceed",
                        onClick = onProceed,
                        modifier = Modifier.weight(1f)
                    )
                    HelperOutlinedButton(
                        text = "Cancel",
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
internal fun ScanMetaRows(scanResult: VirusTotalScanner.ScanResult) {
    if (scanResult is VirusTotalScanner.ScanResult.Error) return
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
    var hashCopied by remember { mutableStateOf(false) }
    val sha256 = scanResult.sha256
    val rows = buildList {
        scanResult.typeDescription?.let { add("Type" to it) }
        scanResult.sizeBytes?.let { add("Size" to it.formatBytes()) }
        scanResult.timesSubmitted?.let { add("Times submitted" to it.toString()) }
        scanResult.firstSubmissionDate?.let { add("First seen" to formatScanDate(it)) }
        scanResult.lastAnalysisDate?.let { add("Last analyzed" to formatScanDate(it)) }
        if (scanResult is VirusTotalScanner.ScanResult.Malicious) {
            if (scanResult.scannedFiles != null && scanResult.fileName.isNotBlank()) {
                add("Flagged APK" to scanResult.fileName)
            }
        }
        scanResult.scannedFiles?.let { add("APKs scanned" to it.toString()) }
        scanResult.bundleName?.let { add("Bundle" to it) }
        if (scanResult is VirusTotalScanner.ScanResult.Clean) {
            if (scanResult.votesHarmless > 0 || scanResult.votesMalicious > 0) {
                add("Community votes" to "${scanResult.votesHarmless} harmless / ${scanResult.votesMalicious} malicious")
            }
            scanResult.reputation?.let { add("Reputation" to it.toString()) }
        }
    }
    if (rows.isEmpty() && sha256 == null) return
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        sha256?.let { hash ->
            // Tap to copy the full hash.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MorpheDefaults.CompactCornerRadius))
                    .clickable {
                        clipboard?.setPrimaryClip(ClipData.newPlainText("SHA-256", hash))
                        hashCopied = true
                        Toast.makeText(context, "SHA-256 copied", Toast.LENGTH_SHORT).show()
                    }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.sha_256),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.38f)
                )
                Text(
                    text = hash.take(16) + "…",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(0.5f)
                )
                ThemedIcon(
                    icon = if (hashCopied) Icons.Outlined.CheckCircle else Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.copy_sha_256),
                    size = 16.dp,
                    tint = if (hashCopied) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            // Jump to the full report on virustotal.com.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MorpheDefaults.CompactCornerRadius))
                    .clickable { openVirusTotalPage(context, hash) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.open_in_virustotal),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(0.38f)
                )
                Spacer(modifier = Modifier.weight(0.5f))
                ThemedIcon(
                    icon = Icons.Outlined.OpenInNew,
                    contentDescription = stringResource(R.string.open_in_virustotal),
                    size = 16.dp
                )
            }
        }
        rows.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.38f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(0.62f)
                )
            }
        }
    }
}

internal fun formatScanDate(epochSeconds: Long): String {
    val date = java.util.Date(epochSeconds * 1000L)
    return java.text.SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
}

internal fun formatTransferSpeed(bytesPerSec: Double): String {
    if (bytesPerSec <= 0.0) return ""
    val mb = bytesPerSec / (1024.0 * 1024.0)
    if (mb >= 1.0) return String.format(Locale.US, "%.1f MB/s", mb)
    val kb = bytesPerSec / 1024.0
    if (kb >= 1.0) return String.format(Locale.US, "%.0f KB/s", kb)
    return String.format(Locale.US, "%.0f B/s", bytesPerSec)
}

internal fun formatTransferEta(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(1L)
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%d:%02d", m, s)
    }
}

@Composable
internal fun FailureActions(
    canRetryFailedSources: Boolean,
    onRetryAll: () -> Unit,
    onRetryFailedSources: () -> Unit,
    onCopyDetails: () -> Unit,
    onCancel: () -> Unit
) {
    HelperButton(
        text = "Retry all",
        onClick = onRetryAll,
        modifier = Modifier.fillMaxWidth()
    )
    if (canRetryFailedSources) {
        HelperOutlinedButton(
            text = "Retry failed sources",
            onClick = onRetryFailedSources,
            modifier = Modifier.fillMaxWidth()
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
    ) {
        HelperOutlinedButton(
            text = "Copy details",
            onClick = onCopyDetails,
            modifier = Modifier.weight(1f)
        )
        HelperOutlinedButton(
            text = "Cancel",
            onClick = onCancel,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
internal fun ErrorState(
    message: String,
    canRetryFailedSources: Boolean,
    onRetryAll: () -> Unit,
    onRetryFailedSources: () -> Unit,
    onCopyDetails: () -> Unit,
    onCancel: () -> Unit
) {
    SurfaceCard {
        Column(
            modifier = Modifier.padding(MorpheDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
        ) {
            Text(text = message, color = MaterialTheme.colorScheme.error)
            FailureActions(
                canRetryFailedSources = canRetryFailedSources,
                onRetryAll = onRetryAll,
                onRetryFailedSources = onRetryFailedSources,
                onCopyDetails = onCopyDetails,
                onCancel = onCancel
            )
        }
    }
}




