package dev.rushi.apkdownloadhelper.ui.shared.download

import android.content.ClipData
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import dev.rushi.apkdownloadhelper.ui.shared.button.DialogButton
import dev.rushi.apkdownloadhelper.ui.shared.dialog.AppDialogDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import dev.chrisbanes.haze.HazeState
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.download.VirusTotalScanner
import dev.rushi.apkdownloadhelper.ui.shared.card.SurfaceCard
import dev.rushi.apkdownloadhelper.ui.shared.chip.StatusBadge
import dev.rushi.apkdownloadhelper.ui.shared.icon.ThemedIcon
import dev.rushi.apkdownloadhelper.ui.shared.shape.rememberPressShape
import dev.rushi.apkdownloadhelper.ui.shared.text.SectionTitle
import dev.rushi.apkdownloadhelper.ui.shared.text.fittedTextAutoSize
import dev.rushi.apkdownloadhelper.ui.model.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.SemanticTone
import dev.rushi.apkdownloadhelper.ui.theme.glassFill
import dev.rushi.apkdownloadhelper.ui.theme.pressScale
import dev.rushi.apkdownloadhelper.ui.util.formatBytes
import dev.rushi.apkdownloadhelper.ui.util.formatScanDate
import dev.rushi.apkdownloadhelper.ui.util.openVirusTotalPage

@Composable
internal fun ScanAskCard(
    candidate: DownloadCandidate,
    onScan: () -> Unit,
    onSkip: () -> Unit
) {
    SurfaceCard(
        cornerRadius = AppDefaults.SectionCornerRadius,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(AppDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
        ) {
            SectionTitle(text = "Scan with VirusTotal?", icon = Icons.Outlined.Shield)
            Text(
                text = "Check ${candidate.name} with VirusTotal before returning it to Morphe.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Scan / Skip are pinned at the bottom of the screen (ScanActionBar).
        }
    }
}

/**
 * Bottom-pinned two-button bar with frosted glass blur (respecting the user's blur setting):
 * [leftText] outlined glass on the left, [rightText] Morphe-tinted glass on the right.
 */
@Composable
internal fun ScanActionBar(
    leftText: String,
    onLeft: () -> Unit,
    rightText: String,
    onRight: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    rightDestructive: Boolean = false,
    onHeightChanged: (Dp) -> Unit = {}
) {
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme
    val fallbackHaze = remember { HazeState() }
    val effectiveHaze = hazeState ?: fallbackHaze

    val isDark = (0.299 * colors.onSurface.red + 0.587 * colors.onSurface.green + 0.114 * colors.onSurface.blue) >= 0.5
    val leftInteraction = remember { MutableInteractionSource() }
    val leftShape = rememberPressShape(leftInteraction, AppDefaults.PillShape)
    val leftBorderColor = colors.primary.copy(alpha = if (isDark) 0.30f else 0.25f)

    val rightInteraction = remember { MutableInteractionSource() }
    val rightShape = rememberPressShape(rightInteraction, AppDefaults.PillShape)
    val rightFill = if (rightDestructive) {
        AppDialogDefaults.defaultButtonContainer()
    } else {
        AppDialogDefaults.confirmButtonContainer()
    }
    val rightContentColor = if (rightDestructive) {
        if (isDark) Color(0xFFFF6B6B) else Color(0xFFD32F2F)
    } else {
        colors.onSurface
    }
    val labelStyle = MaterialTheme.typography.labelLarge

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { onHeightChanged(with(density) { it.height.toDp() }) }
            .navigationBarsPadding()
            .padding(horizontal = AppDefaults.ContentPadding, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(AppDialogDefaults.ButtonSpacing)
    ) {
        OutlinedButton(
            onClick = onLeft,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = AppDefaults.DialogButtonHeight)
                .pressScale(leftInteraction, pressedScale = 0.96f)
                .glassFill(
                    glass = Color.Transparent,
                    shape = leftShape,
                    hazeState = effectiveHaze
                ),
            interactionSource = leftInteraction,
            shape = leftShape,
            border = BorderStroke(1.dp, leftBorderColor),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = colors.onSurface.copy(alpha = 0.85f)
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = leftText,
                style = labelStyle,
                autoSize = fittedTextAutoSize(labelStyle.fontSize),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Button(
            onClick = onRight,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = AppDefaults.DialogButtonHeight)
                .pressScale(rightInteraction, pressedScale = 0.96f)
                .glassFill(
                    glass = rightFill,
                    shape = rightShape,
                    hazeState = effectiveHaze
                ),
            interactionSource = rightInteraction,
            shape = rightShape,
            border = null,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = rightContentColor
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = rightText,
                style = labelStyle,
                autoSize = fittedTextAutoSize(labelStyle.fontSize),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
    val flagged = when (scanResult) {
        is VirusTotalScanner.ScanResult.Malicious -> scanResult.detections
        else -> 0
    }
    val totalEngines = when (scanResult) {
        is VirusTotalScanner.ScanResult.Malicious -> scanResult.totalEngines
        is VirusTotalScanner.ScanResult.Clean -> scanResult.totalEngines
        else -> 0
    }

    // Borderless card on the app's clean elevated surface
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceColorAtElevation(2.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Hero verdict header + right-aligned detection pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tone.container),
                contentAlignment = Alignment.Center
            ) {
                ThemedIcon(
                    icon = when {
                        isMalicious || isError -> Icons.Outlined.Warning
                        else -> Icons.Outlined.CheckCircle
                    },
                    size = 24.dp,
                    tint = tone.accent
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = when {
                        isMalicious -> "Threats detected"
                        isError -> "Scan error"
                        else -> "No threats found"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = "VirusTotal security report",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
            }
            if (!isError && totalEngines > 0) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(tone.container)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$flagged / $totalEngines",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = tone.content
                    )
                }
            }
        }

        // 2. Status badges row (Cached/Fresh + SHA-256 verified)
        if (!isError || shaVerified) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isError) {
                    StatusBadge(
                        text = if (scanResult.cached) "Cached report" else "Fresh scan",
                        tone = if (scanResult.cached) SemanticTone.Primary else SemanticTone.Neutral
                    )
                }
                if (shaVerified) {
                    val verifiedLabel = stringResource(R.string.sha_256_verified)
                    StatusBadge(
                        text = "SHA-256 verified",
                        icon = Icons.Outlined.Verified,
                        tone = SemanticTone.Primary,
                        modifier = Modifier.semantics { contentDescription = verifiedLabel }
                    )
                }
            }
        }

        // 3. Connected 2x2 metric tiles (matching DownloadStatGrid)
        if (!isError && totalEngines > 0) {
            val notFlagged = (totalEngines - flagged).coerceAtLeast(0)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    ReportStatTile(
                        label = "Not flagged",
                        value = "$notFlagged / $totalEngines",
                        valueColor = if (isMalicious) colors.onSurface else tone.accent,
                        shape = connectedShape(topStart = true, topEnd = false, bottomStart = false, bottomEnd = false),
                        modifier = Modifier.weight(1f)
                    )
                    ReportStatTile(
                        label = "Flagged",
                        value = flagged.toString(),
                        valueColor = if (flagged > 0) colors.error else colors.onSurface,
                        shape = connectedShape(topStart = false, topEnd = true, bottomStart = false, bottomEnd = false),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    ReportStatTile(
                        label = "File size",
                        value = scanResult.sizeBytes?.formatBytes() ?: "—",
                        valueColor = colors.onSurface,
                        shape = connectedShape(topStart = false, topEnd = false, bottomStart = true, bottomEnd = false),
                        modifier = Modifier.weight(1f)
                    )
                    ReportStatTile(
                        label = if (scanResult.scannedFiles != null) "Bundle APKs" else "Reputation",
                        value = scanResult.scannedFiles?.toString()
                            ?: scanResult.reputation?.toString()
                            ?: "—",
                        valueColor = colors.onSurface,
                        shape = connectedShape(topStart = false, topEnd = false, bottomStart = false, bottomEnd = true),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Verdict summary banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(tone.container.copy(alpha = 0.55f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemedIcon(
                icon = if (isMalicious || isError) Icons.Outlined.Warning else Icons.Outlined.Shield,
                size = 16.dp,
                tint = tone.accent
            )
            Text(
                text = when {
                    isMalicious -> detail + "\n" + stringResource(R.string.this_file_was_flagged_by_antivirus) +
                        "Only proceed if you trust the source."
                    isError -> stringResource(R.string.the_scan_did_not_complete_network) +
                        "You can still proceed, or cancel to keep the file out of Morphe."
                    else -> detail
                },
                style = MaterialTheme.typography.bodySmall,
                color = tone.content,
                modifier = Modifier.weight(1f)
            )
        }

        // 5. Malicious engine detections (if any)
        if (scanResult is VirusTotalScanner.ScanResult.Malicious) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.errorContainer.copy(alpha = 0.35f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                scanResult.suggestedThreatLabel?.let { label ->
                    Text(
                        text = "Detected as: $label",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.error
                    )
                }
                if (scanResult.engines.isNotEmpty()) {
                    scanResult.engines.take(8).forEach { hit ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = hit.engine,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface,
                                modifier = Modifier.weight(0.38f)
                            )
                            Text(
                                text = hit.result,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.error,
                                modifier = Modifier.weight(0.62f)
                            )
                        }
                    }
                    if (scanResult.engines.size > 8) {
                        Text(
                            text = "+${scanResult.engines.size - 8} more engines",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 6. Bundle inner APK breakdown (if bundle)
        if (scanResult.apkResults.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surfaceVariant.copy(alpha = 0.32f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.apk_details),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
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
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (!apk.failed) {
                            StatusBadge(
                                text = if (apk.cached) "cached" else "fresh",
                                tone = if (apk.cached) SemanticTone.Primary else SemanticTone.Neutral
                            )
                        }
                        Text(
                            text = if (apk.failed) "failed" else "${apk.detections}/${apk.totalEngines}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = apkVerdictColor
                        )
                        if (apk.sha256 != null) {
                            ThemedIcon(
                                icon = Icons.Outlined.OpenInNew,
                                contentDescription = "Open ${apk.name} in VirusTotal",
                                size = 16.dp,
                                tint = colors.primary,
                                modifier = Modifier
                                    .clip(AppDefaults.PillShape)
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
        }

        // 7. File & VirusTotal API metadata section
        ScanMetaRows(scanResult)
    }
}

@Composable
private fun ReportStatTile(
    label: String,
    value: String,
    valueColor: Color,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun ScanMetaRows(scanResult: VirusTotalScanner.ScanResult) {
    if (scanResult is VirusTotalScanner.ScanResult.Error) return
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
    var copiedKey by remember { mutableStateOf<String?>(null) }
    val sha256 = scanResult.sha256

    val rows = buildList {
        scanResult.meaningfulName?.takeIf { it.isNotBlank() }?.let { add("Known name" to it) }
        scanResult.typeDescription?.let { add("File type" to it) }
        scanResult.timesSubmitted?.let { add("Times submitted" to it.toString()) }
        scanResult.firstSubmissionDate?.let { add("First seen" to formatScanDate(it)) }
        scanResult.lastAnalysisDate?.let { add("Last analyzed" to formatScanDate(it)) }
        if (scanResult is VirusTotalScanner.ScanResult.Malicious) {
            if (scanResult.suspicious > 0) {
                add("Suspicious flags" to scanResult.suspicious.toString())
            }
            if (scanResult.scannedFiles != null && scanResult.fileName.isNotBlank()) {
                add("Flagged APK" to scanResult.fileName)
            }
        }
        if (scanResult.sandboxMalwareNames.isNotEmpty()) {
            add("Sandbox verdicts" to scanResult.sandboxMalwareNames.joinToString(", "))
        }
        scanResult.bundleName?.let { add("Bundle" to it) }
        if (scanResult.votesHarmless > 0 || scanResult.votesMalicious > 0) {
            add("Community votes" to "${scanResult.votesHarmless} harmless · ${scanResult.votesMalicious} malicious")
        }
    }

    val hashes = buildList {
        sha256?.let { add("SHA-256" to it) }
        scanResult.sha1?.let { add("SHA-1" to it) }
        scanResult.md5?.let { add("MD5" to it) }
    }

    if (rows.isEmpty() && hashes.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.28f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "File & report details",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            if (sha256 != null) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.primaryContainer.copy(alpha = 0.65f))
                        .clickable { openVirusTotalPage(context, sha256) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.open_in_virustotal),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onPrimaryContainer
                    )
                    ThemedIcon(
                        icon = Icons.Outlined.OpenInNew,
                        contentDescription = stringResource(R.string.open_in_virustotal),
                        size = 13.dp,
                        tint = colors.onPrimaryContainer
                    )
                }
            }
        }

        rows.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(0.36f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                    modifier = Modifier.weight(0.64f)
                )
            }
        }

        hashes.forEach { (label, fullHash) ->
            val isCopied = copiedKey == label
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceVariant.copy(alpha = 0.35f))
                    .clickable {
                        clipboard?.setPrimaryClip(ClipData.newPlainText(label, fullHash))
                        copiedKey = label
                        Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.width(56.dp)
                )
                Text(
                    text = if (fullHash.length > 24) fullHash.take(20) + "…" else fullHash,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                ThemedIcon(
                    icon = if (isCopied) Icons.Outlined.CheckCircle else Icons.Outlined.ContentCopy,
                    contentDescription = "Copy $label",
                    size = 15.dp,
                    tint = if (isCopied) colors.primary else colors.onSurfaceVariant
                )
            }
        }
    }
}
