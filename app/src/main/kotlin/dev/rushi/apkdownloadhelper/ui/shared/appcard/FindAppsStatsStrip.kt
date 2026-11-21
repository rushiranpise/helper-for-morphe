package dev.rushi.apkdownloadhelper.ui.shared.appcard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rushi.apkdownloadhelper.updater.MorpheArchiveIndex

@Composable
internal fun FindAppsStatsStrip(index: MorpheArchiveIndex?) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "stats_shimmer")
    val shimmerAlpha by transition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stats_shimmer_alpha"
    )

    val stats = listOf(
        Triple("SOURCES", index?.repoCount, Icons.Outlined.Dns),
        Triple("APPS", index?.appCount, Icons.Outlined.Apps),
        Triple("PATCHES", index?.patchCount, Icons.Outlined.Extension),
        Triple("UNIVERSAL", index?.universalPatchCount, Icons.Outlined.Public)
    )

    val outerRadius = 20.dp
    val innerRadius = 6.dp
    val statCount = stats.size
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        stats.forEachIndexed { i, (label, value, icon) ->
            val isFirst = i == 0
            val isLast = i == statCount - 1
            val cardShape = RoundedCornerShape(
                topStart = if (isFirst) outerRadius else innerRadius,
                bottomStart = if (isFirst) outerRadius else innerRadius,
                topEnd = if (isLast) outerRadius else innerRadius,
                bottomEnd = if (isLast) outerRadius else innerRadius
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(cardShape)
                    .background(scheme.surfaceColorAtElevation(2.dp))
                    .padding(vertical = 10.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (value == null) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(16.dp)
                            .clip(CircleShape)
                            .background(scheme.onSurface.copy(alpha = shimmerAlpha))
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = scheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
