package dev.rushi.apkdownloadhelper.ui.shared.icon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.MonochromeThemeDefaults

// ---------------------------------------------------------------------------
// Shared primitives
// ---------------------------------------------------------------------------
/** Reusable icon with the manager's standard sizing and primary tint. */
@Composable
internal fun ThemedIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = AppDefaults.IconSize,
    tint: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size)
    )
}

/** Circle filled with the manager's brand gradient around an icon. */
@Composable
internal fun GradientCircleIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = AppDefaults.IconSize,
    contentDescription: String? = null,
    gradientColors: List<Color> = AppDefaults.DefaultGradientColors
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(brush = MonochromeThemeDefaults.iconBackground(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        ThemedIcon(
            icon = icon,
            contentDescription = contentDescription,
            tint = MonochromeThemeDefaults.iconTint(Color.White),
            size = iconSize
        )
    }
}

/** Chevron pointing at whatever a row navigates to (mirrored for RTL). */
@Composable
internal fun ForwardChevronIcon(
    modifier: Modifier = Modifier,
    size: Dp = AppDefaults.IconSize,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    ThemedIcon(
        icon = if (androidx.compose.ui.platform.LocalConfiguration.current.layoutDirection ==
            android.util.LayoutDirection.RTL
        ) {
            Icons.Outlined.ChevronLeft
        } else {
            Icons.Outlined.ChevronRight
        },
        modifier = modifier,
        size = size,
        tint = tint
    )
}
