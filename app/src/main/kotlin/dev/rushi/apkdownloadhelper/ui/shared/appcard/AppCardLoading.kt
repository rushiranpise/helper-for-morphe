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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.ui.theme.LocalMonochromeTheme

@Composable
internal fun FindAppCardLoading(
    index: Int,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    useGradient: Boolean = true
) {
    val monochrome = LocalMonochromeTheme.current
    val effectiveGradient = useGradient && !monochrome
    val scheme = MaterialTheme.colorScheme
    val palette = FindAppPlaceholderGradients[index % FindAppPlaceholderGradients.size]
    val skeletonColor = if (!effectiveGradient) scheme.onSurface else Color.White

    val transition = rememberInfiniteTransition(label = "skeleton_shimmer_$index")
    val shimmerAlpha by transition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(shape)
            .background(
                if (!effectiveGradient) {
                    SolidColor(scheme.surfaceContainerHigh)
                } else {
                    Brush.linearGradient(
                        colors = palette.map { it.copy(alpha = 0.55f) },
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 300f)
                    )
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(skeletonColor.copy(alpha = shimmerAlpha))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(16.dp)
                        .clip(CircleShape)
                        .background(skeletonColor.copy(alpha = shimmerAlpha + 0.06f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.80f)
                        .height(12.dp)
                        .clip(CircleShape)
                        .background(skeletonColor.copy(alpha = shimmerAlpha))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.42f)
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(skeletonColor.copy(alpha = shimmerAlpha * 0.8f))
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(skeletonColor.copy(alpha = 0.14f))
            )
        }
    }
}
