package dev.rushi.apkdownloadhelper.ui.shared.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.download.VirusTotalScanner
import dev.rushi.apkdownloadhelper.ui.shared.source.sourceCardFill
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.SemanticTone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
internal fun ApiKeyEditor(
    apiKey: String,
    onApiKeyChange: (String) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.api_key), style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.enter_api_key)) }
        )
        Row(
            modifier = Modifier.fillMaxWidth().clickable {
                val open = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.virustotal.com/docs/please-give-me-an-api-key"))
                runCatching { context.startActivity(open) }
            }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.how_to_get_a_free_api),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        }
    }
}

private fun formatReset(ms: Long): String {
    val s = (ms / 1000L).coerceAtLeast(0L)
    return when {
        s < 60L -> "${s}s"
        s < 3_600L -> "${s / 60L} min"
        s < 86_400L -> "${s / 3_600L} h ${(s % 3_600L) / 60L} min"
        else -> "${s / 86_400L} d ${(s % 86_400L) / 3_600L} h"
    }
}

private fun millisToNextUtcMonth(now: Long): Long {
    val c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    c.timeInMillis = now
    c.add(java.util.Calendar.MONTH, 1)
    c.set(java.util.Calendar.DAY_OF_MONTH, 1)
    c.set(java.util.Calendar.HOUR_OF_DAY, 0)
    c.set(java.util.Calendar.MINUTE, 0)
    c.set(java.util.Calendar.SECOND, 0)
    c.set(java.util.Calendar.MILLISECOND, 0)
    return c.timeInMillis - now
}

private val sessionQuotaCache = mutableMapOf<String, VirusTotalScanner.QuotaUsage>()

@Composable
internal fun VirusTotalQuotaCard(apiKey: String) {
    if (apiKey.isBlank()) return
    val colors = MaterialTheme.colorScheme
    var quota by remember(apiKey) { mutableStateOf(sessionQuotaCache[apiKey]) }
    var loading by remember(apiKey) { mutableStateOf(false) }
    var failed by remember(apiKey) { mutableStateOf(false) }
    var minuteUsed by remember { mutableIntStateOf(VirusTotalScanner.rateLimiter.callsInCurrentMinute()) }
    var rollingUsed by remember { mutableIntStateOf(VirusTotalScanner.rateLimiter.callsInLastMinute()) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var refreshKey by androidx.compose.runtime.saveable.rememberSaveable(apiKey) { mutableIntStateOf(0) }
    var lastFetchedRefreshKey by androidx.compose.runtime.saveable.rememberSaveable(apiKey) { mutableIntStateOf(-1) }
    LaunchedEffect(apiKey, refreshKey) {
        if (quota == null || refreshKey != lastFetchedRefreshKey) {
            loading = true
            failed = false
            val fetched = withContext(Dispatchers.IO) { VirusTotalScanner.fetchQuotaUsage(apiKey) }
            if (fetched != null) {
                sessionQuotaCache[apiKey] = fetched
                quota = fetched
                lastFetchedRefreshKey = refreshKey
            } else if (quota == null) {
                failed = true
            }
            loading = false
        }
        while (true) {
            minuteUsed = VirusTotalScanner.rateLimiter.callsInCurrentMinute()
            rollingUsed = VirusTotalScanner.rateLimiter.callsInLastMinute()
            nowMs = System.currentTimeMillis()
            delay(1000)
        }
    }
    val limit = VirusTotalScanner.MINUTE_LOOKUP_LIMIT
    val minuteReset = "Resets in " + formatReset(60_000L - nowMs % 60_000L)
    val hourReset = "Resets in " + formatReset(3_600_000L - nowMs % 3_600_000L)
    val dayReset = "Resets in " + formatReset(86_400_000L - nowMs % 86_400_000L)
    val monthReset = "Resets in " + formatReset(millisToNextUtcMonth(nowMs))
    SettingsItemCard(onClick = null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Shield, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.virustotal_quota), fontWeight = FontWeight.Bold, color = colors.onSurface)
                    Text("Lookups used before each limit resets", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Refresh",
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp).clip(AppDefaults.PillShape).clickable { refreshKey++ }.padding(2.dp)
                    )
                }
            }
            val current = quota
            if (current != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuotaTile("Per minute", minuteUsed, limit, minuteReset, Modifier.weight(1f), flash = maxOf(minuteUsed, rollingUsed) >= limit - 1)
                    QuotaTile("Per hour", current.hourlyUsed, current.hourlyAllowed, hourReset, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuotaTile("Per day", current.dailyUsed, current.dailyAllowed, dayReset, Modifier.weight(1f))
                    QuotaTile("Per month", current.monthlyUsed, current.monthlyAllowed, monthReset, Modifier.weight(1f))
                }
            } else {
                QuotaTile("Per minute", minuteUsed, limit, minuteReset, Modifier.fillMaxWidth(), flash = maxOf(minuteUsed, rollingUsed) >= limit - 1)
                if (failed) {
                    Text(stringResource(R.string.couldn_t_load_quota_check_your), color = colors.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun QuotaTile(
    label: String,
    used: Int,
    allowed: Int,
    resetText: String,
    modifier: Modifier = Modifier,
    flash: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val ratio = if (allowed > 0) used.toFloat() / allowed else 0f
    val warning = SemanticTone.Warning.accent
    val base = when { ratio >= 0.9f -> colors.error; ratio >= 0.7f -> warning; else -> colors.primary }
    val barColor = if (flash) {
        val t = rememberInfiniteTransition(label = "quota-flash-$label")
        warning.copy(alpha = t.animateFloat(1f, 0.3f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "quota-alpha-$label").value)
    } else base
    val shownUsed = used.coerceAtLeast(0)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(AppDefaults.CardCornerRadius))
            .background(colors.surfaceVariant.copy(alpha = 0.35f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "$shownUsed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (ratio >= 0.7f) barColor else colors.onSurface
            )
            Text(
                text = "/ $allowed",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
        LinearProgressIndicator(
            progress = { ratio.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = barColor,
            trackColor = colors.surfaceVariant
        )
        Text(resetText, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
    }
}

@Composable
internal fun VirusTotalApiKeyDialog(
    initialApiKey: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var draftKey by remember(initialApiKey) { mutableStateOf(initialApiKey) }

    dev.rushi.apkdownloadhelper.ui.shared.dialog.AppAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "VirusTotal API key",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = draftKey,
                    onValueChange = { draftKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start
                    ),
                    label = {
                        Text(
                            text = "API key",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                        )
                    },
                    placeholder = {
                        Text(
                            text = "Paste your 64-character API key",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                        )
                    },
                    trailingIcon = {
                        if (draftKey.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear",
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .clickable { draftKey = "" }
                            )
                        }
                    }
                )
                Surface(
                    onClick = {
                        dev.rushi.apkdownloadhelper.ui.util.openExternal(
                            context,
                            "https://docs.virustotal.com/docs/please-give-me-an-api-key"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppDefaults.CompactCornerRadius),
                    color = scheme.surfaceVariant.copy(alpha = 0.45f),
                    contentColor = scheme.primary
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.how_to_get_a_free_api),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                            color = scheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = scheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            dev.rushi.apkdownloadhelper.ui.shared.button.DialogButton(
                text = "Save",
                onClick = { onSave(draftKey.trim()) },
                buttonColor = dev.rushi.apkdownloadhelper.ui.shared.dialog.AppDialogDefaults.confirmButtonContainer(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            dev.rushi.apkdownloadhelper.ui.shared.button.DialogButton(
                text = "Cancel",
                onClick = onDismiss,
                filled = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
private fun VirusTotalQuotaCardOld(apiKey: String) {
    if (apiKey.isBlank()) return
    val colors = MaterialTheme.colorScheme
    var quota by remember { mutableStateOf<VirusTotalScanner.QuotaUsage?>(null) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var minuteUsed by remember { mutableIntStateOf(0) }
    var rollingUsed by remember { mutableIntStateOf(0) }
    var secsToRollover by remember { mutableIntStateOf(60) }
    var refreshKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(apiKey, refreshKey) {
        loading = true
        failed = false
        quota = withContext(Dispatchers.IO) { VirusTotalScanner.fetchQuotaUsage(apiKey) }
        if (quota == null) failed = true
        loading = false
        while (true) {
            minuteUsed = VirusTotalScanner.rateLimiter.callsInCurrentMinute()
            rollingUsed = VirusTotalScanner.rateLimiter.callsInLastMinute()
            secsToRollover = ((60_000L - System.currentTimeMillis() % 60_000L + 999L) / 1000L).toInt()
            delay(1000)
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDefaults.CardCornerRadius),
        color = sourceCardFill(),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Shield, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
                Column(modifier = Modifier.weight(1f)) { Text(stringResource(R.string.virustotal_quota), fontWeight = FontWeight.Bold, color = colors.onSurface) }
                if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = colors.primary, modifier = Modifier.size(20.dp).clip(AppDefaults.PillShape).clickable { refreshKey++ }.padding(2.dp))
            }
            QuotaBar("Per minute", minuteUsed, VirusTotalScanner.MINUTE_LOOKUP_LIMIT, flash = maxOf(minuteUsed, rollingUsed) >= VirusTotalScanner.MINUTE_LOOKUP_LIMIT - 1, caption = "rolling 60s: $rollingUsed / ${VirusTotalScanner.MINUTE_LOOKUP_LIMIT} · resets in ${secsToRollover}s")
            val current = quota
            when {
                current != null -> {
                    QuotaBar("Per hour", current.hourlyUsed, current.hourlyAllowed)
                    QuotaBar("Per day", current.dailyUsed, current.dailyAllowed)
                    QuotaBar("Per month", current.monthlyUsed, current.monthlyAllowed)
                }
                failed -> Text(stringResource(R.string.couldn_t_load_quota_check_your), color = colors.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun QuotaBar(label: String, used: Int, allowed: Int, flash: Boolean = false, caption: String? = null) {
    val colors = MaterialTheme.colorScheme
    val ratio = if (allowed > 0) used.toFloat() / allowed else 0f
    val warningAccent = SemanticTone.Warning.accent
    val baseColor = when { ratio >= 0.9f -> colors.error; ratio >= 0.7f -> warningAccent; else -> colors.primary }
    val barColor = if (flash) {
        val transition = rememberInfiniteTransition(label = "quota-flash-$label")
        warningAccent.copy(alpha = transition.animateFloat(1f, 0.3f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "quota-alpha-$label").value)
    } else baseColor
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text("$used / $allowed", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = if (ratio >= 0.7f) barColor else colors.onSurface)
        }
        LinearProgressIndicator(progress = { ratio.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = barColor, trackColor = colors.surfaceVariant)
        if (caption != null) Text(caption, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
    }
}
