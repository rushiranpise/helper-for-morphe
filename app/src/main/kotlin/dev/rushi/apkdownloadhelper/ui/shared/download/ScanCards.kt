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
import dev.rushi.apkdownloadhelper.HelperButton
import dev.rushi.apkdownloadhelper.HelperOutlinedButton
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.download.VirusTotalScanner
import dev.rushi.apkdownloadhelper.ui.shared.card.SectionCard
import dev.rushi.apkdownloadhelper.ui.shared.card.SurfaceCard
import dev.rushi.apkdownloadhelper.ui.shared.chip.StatusBadge
import dev.rushi.apkdownloadhelper.ui.shared.icon.ThemedIcon
import dev.rushi.apkdownloadhelper.ui.shared.layout.AppDivider
import dev.rushi.apkdownloadhelper.ui.shared.text.SectionTitle
import dev.rushi.apkdownloadhelper.ui.model.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.SemanticTone
import dev.rushi.apkdownloadhelper.ui.util.formatBytes
import dev.rushi.apkdownloadhelper.ui.util.formatScanDate
import dev.rushi.apkdownloadhelper.ui.util.openVirusTotalPage

@Composable
internal fun ScanAskCard(
    candidate: DownloadCandidate,
    onScan: () -> Unit,
    onSkip: () -> Unit
) {
    SectionCard {
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
        cornerRadius = AppDefaults.SectionCornerRadius,
        borderWidth = 1.dp,
        borderColor = tone.accent.copy(alpha = 0.45f),
        color = tone.container.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.padding(AppDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
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
                    StatusBadge(
                        text = if (scanResult.cached) "Cached report" else "Fresh scan",
                        tone = if (scanResult.cached) SemanticTone.Primary else SemanticTone.Neutral
                    )
                }
                if (shaVerified) {
                    // As above: Modifier.semantics takes a plain lambda, so the resource is read
                    // before it rather than inside it.
                    val verifiedLabel = stringResource(R.string.sha_256_verified)
                    StatusBadge(
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
                AppDivider(modifier = Modifier.padding(vertical = 4.dp), fullWidth = true)
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
                            StatusBadge(
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
                    .clip(RoundedCornerShape(AppDefaults.CompactCornerRadius))
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
                    .clip(RoundedCornerShape(AppDefaults.CompactCornerRadius))
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
