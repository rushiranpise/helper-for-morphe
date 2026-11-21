package dev.rushi.apkdownloadhelper.ui.screen.helper

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.rushi.apkdownloadhelper.BuildConfig
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.storage.HelperSettings
import dev.rushi.apkdownloadhelper.ui.shared.layout.AppWordmark
import dev.rushi.apkdownloadhelper.ui.theme.AppColors
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.glassFill

//========================================================================
// Shared top header (AppWordmark + version + BoltButton) used by both views.
//========================================================================

@Composable
internal fun HelperFloatingHeader(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit,
    isScrolled: Boolean,
    headerHaze: HazeState,
    onHeaderHeightChanged: (Dp) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val headerShape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                onHeaderHeightChanged(with(density) { size.height.toDp() })
            }
            .then(
                if (isScrolled) {
                    Modifier.glassFill(
                        glass = AppColors.glassContainerColor(),
                        shape = headerShape,
                        hazeState = headerHaze
                    )
                } else {
                    Modifier
                }
            )
            .statusBarsPadding()
            .padding(
                horizontal = AppDefaults.ContentPadding,
                vertical = 12.dp
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                AppWordmark(
                    modifier = Modifier.height(26.dp),
                    textColor = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.helper),
                        style = MaterialTheme.typography.bodySmall.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }

            HelperGlassBoltButton(
                fastMode = settings.fastMode,
                onClick = {
                    val enabled = !settings.fastMode
                    onSettingsChange(settings.copy(fastMode = enabled))
                    Toast.makeText(
                        context,
                        if (enabled) "Fast Mode on" else "Fast Mode off",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }
}

@Composable
private fun HelperGlassBoltButton(
    fastMode: Boolean,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val idleAlpha = if (isDark) 0.35f else 0.6f
    val activeAlpha = if (isDark) 0.55f else 0.72f
    val baseContainer = if (fastMode) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = activeAlpha)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = idleAlpha)
    }
    val containerColor by animateColorAsState(
        targetValue = if (pressed) {
            baseContainer.copy(alpha = (baseContainer.alpha * 0.75f).coerceAtLeast(0.18f))
        } else {
            baseContainer
        },
        animationSpec = tween(200),
        label = "helper_bolt_container"
    )
    val contentColor by animateColorAsState(
        targetValue = if (fastMode) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "helper_bolt_content"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = tween(200),
        label = "helper_bolt_scale"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.Bolt,
                contentDescription = if (fastMode) "Fast Mode on" else "Fast Mode off",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}