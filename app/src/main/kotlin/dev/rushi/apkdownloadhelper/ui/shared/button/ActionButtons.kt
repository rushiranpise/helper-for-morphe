package dev.rushi.apkdownloadhelper.ui.shared.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.ui.shared.shape.rememberPressShape
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.pressScale

@Composable
internal fun BoltButton(
    fastMode: Boolean,
    onClick: () -> Unit
) {
    IconActionButton(
        icon = Icons.Outlined.Bolt,
        contentDescription = if (fastMode) "Fast Mode on" else "Fast Mode off",
        onClick = onClick,
        selected = fastMode
    )
}

/**
 * Square glass button used by the header row and other single-icon actions,
 * matching the manager's glass-button family (16dp shape, 48dp touch target).
 */
@Composable
internal fun IconActionButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    tint: Color? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    val contentColor = tint ?: if (selected) primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
    val interactionSource = remember { MutableInteractionSource() }
    val shape = rememberPressShape(interactionSource, RoundedCornerShape(AppDefaults.CardCornerRadius))
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(AppDefaults.GlassButtonHeight)
            .clip(RoundedCornerShape(AppDefaults.CardCornerRadius))
            .pressScale(interactionSource),
        shape = shape,
        color = if (selected) primary.copy(alpha = 0.28f) else Color.Transparent,
        contentColor = contentColor,
        border = BorderStroke(1.dp, primary.copy(alpha = if (selected) 0.6f else 0.32f)),
        interactionSource = interactionSource
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(AppDefaults.IconSizeSmall)
            )
        }
    }
}

@Composable
internal fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconActionButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
internal fun ThemeToggleButton(
    dark: Boolean,
    onToggle: () -> Unit
) {
    IconActionButton(
        icon = if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
        contentDescription = if (dark) "Switch to light theme" else "Switch to dark theme",
        onClick = onToggle
    )
}
