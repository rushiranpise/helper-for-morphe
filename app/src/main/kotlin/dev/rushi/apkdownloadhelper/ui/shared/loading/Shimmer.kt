package dev.rushi.apkdownloadhelper.ui.shared.loading

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.ui.shared.card.SectionCard
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults

// progress in [0, 1]: 0 = band fully off top-left, 1 = band fully off bottom-right
private fun DrawScope.drawDiagonalShimmer(progress: Float, color: Color) {
    val totalDiag = size.width + size.height
    val bandWidth = totalDiag * 0.4f
    val center = progress * (totalDiag + bandWidth * 2f) - bandWidth
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color.Transparent, color, Color.Transparent),
            start = Offset((center - bandWidth) / 2f, (center - bandWidth) / 2f),
            end = Offset((center + bandWidth) / 2f, (center + bandWidth) / 2f)
        )
    )
}

/** Base shimmer box with the manager's animated diagonal glint and soft pulse. */
@Composable
internal fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    baseColor: Color = Color.Unspecified,
    shimmerColor: Color = Color.Unspecified,
    baseAlpha: Float = 0.2f
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface
    val resolvedBase = if (baseColor == Color.Unspecified) onSurface else baseColor
    val resolvedShimmer = if (shimmerColor == Color.Unspecified) {
        if (surface.luminance() > onSurface.luminance()) {
            surface.copy(alpha = 0.7f)
        } else {
            onSurface.copy(alpha = 0.35f)
        }
    } else {
        shimmerColor
    }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerProgress: State<Float> = transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_progress"
    )
    val pulseAlpha: State<Float> = transition.animateFloat(
        initialValue = baseAlpha,
        targetValue = baseAlpha + 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                drawRect(color = resolvedBase.copy(alpha = pulseAlpha.value))
                drawDiagonalShimmer(shimmerProgress.value % 1f, resolvedShimmer)
            }
    )
}

/** Simple shimmer element for text-like loading states. */
@Composable
internal fun ShimmerText(
    modifier: Modifier = Modifier,
    widthFraction: Float = 0.6f,
    height: Dp = 16.dp,
    cornerRadius: Dp = 6.dp
) {
    ShimmerBox(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height),
        shape = RoundedCornerShape(cornerRadius)
    )
}

/**
 * A bar on a line of [height], the height of the text or badge it stands in for, so the row keeps
 * its height when the real content replaces it.
 */
@Composable
internal fun ShimmerLine(
    height: Dp,
    widthFraction: Float,
    baseColor: Color = Color.Unspecified
) {
    Box(modifier = Modifier.height(height), contentAlignment = Alignment.CenterStart) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .height(height * 0.7f),
            shape = RoundedCornerShape(6.dp),
            baseColor = baseColor
        )
    }
}

/** Placeholder for an app row while a list is being read. */
@Composable
internal fun ShimmerListRow() {
    SectionCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDefaults.ContentPadding),
            horizontalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBox(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(AppDefaults.CompactCornerRadius)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShimmerLine(height = 18.dp, widthFraction = 0.55f)
                ShimmerLine(height = 14.dp, widthFraction = 0.8f)
                ShimmerLine(height = 14.dp, widthFraction = 0.35f)
            }
        }
    }
}

/** Placeholder for release notes while an update check is running. */
@Composable
internal fun ShimmerReleaseNotes() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppDefaults.ContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerLine(height = 18.dp, widthFraction = 0.42f)
        repeat(4) { index ->
            ShimmerLine(
                height = 14.dp,
                widthFraction = when (index) {
                    0 -> 0.9f
                    1 -> 0.76f
                    2 -> 0.84f
                    else -> 0.5f
                }
            )
        }
    }
}
