package dev.rushi.apkdownloadhelper.ui.shared.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

@Composable
internal fun VirusTotalQuotaCard(apiKey: String) {
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
