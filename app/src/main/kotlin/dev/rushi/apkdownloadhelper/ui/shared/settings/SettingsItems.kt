package dev.rushi.apkdownloadhelper.ui.shared.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.ui.shared.card.SurfaceCard
import dev.rushi.apkdownloadhelper.ui.shared.icon.ForwardChevronIcon
import dev.rushi.apkdownloadhelper.ui.shared.icon.ThemedIcon
import dev.rushi.apkdownloadhelper.ui.shared.layout.IconTextRow
import dev.rushi.apkdownloadhelper.ui.shared.shape.LocalSegmentRegistry
import dev.rushi.apkdownloadhelper.ui.shared.shape.SegmentRegistry
import dev.rushi.apkdownloadhelper.ui.shared.shape.rowShape
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.SegmentGap
import dev.rushi.apkdownloadhelper.ui.theme.pressScale

@Composable
internal fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val registry = remember { SegmentRegistry() }
    CompositionLocalProvider(LocalSegmentRegistry provides registry) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .onPlaced(registry::placeGroup),
            content = content
        )
    }
}

@Composable
internal fun SettingsItemCard(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderWidth: Dp = 0.dp,
    borderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
    color: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val group = LocalSegmentRegistry.current
    if (group == null) {
        if (onClick != null) {
            Surface(
                onClick = onClick,
                enabled = enabled,
                interactionSource = interactionSource,
                shape = RoundedCornerShape(AppDefaults.SettingsCornerRadius),
                color = color,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 1.dp,
                border = if (borderWidth > 0.dp) BorderStroke(borderWidth, borderColor) else null,
                modifier = modifier
                    .fillMaxWidth()
                    .pressScale(interactionSource, enabled = enabled, pressedScale = 0.98f)
            ) {
                content()
            }
        } else {
            SurfaceCard(
                onClick = null,
                enabled = enabled,
                elevation = 1.dp,
                cornerRadius = AppDefaults.SettingsCornerRadius,
                borderWidth = borderWidth,
                borderColor = borderColor,
                color = color,
                modifier = modifier
            ) {
                content()
            }
        }
        return
    }

    val row = remember { Any() }
    val shape = group.rowShape(row)
    val rowModifier = modifier
        .padding(vertical = SegmentGap / 2)
        .onPlaced { group.place(row, it) }
        .fillMaxWidth()
        .pressScale(interactionSource, enabled = enabled && onClick != null, pressedScale = 0.98f)
    val rowContent: @Composable () -> Unit = {
        CompositionLocalProvider(LocalSegmentRegistry provides null) {
            content()
        }
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interactionSource,
            modifier = rowModifier,
            shape = shape,
            color = color,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 1.dp,
            border = if (borderWidth > 0.dp) BorderStroke(borderWidth, borderColor) else null,
            content = rowContent
        )
    } else {
        Surface(
            modifier = rowModifier,
            shape = shape,
            color = color,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 1.dp,
            border = if (borderWidth > 0.dp) BorderStroke(borderWidth, borderColor) else null,
            content = rowContent
        )
    }
}

@Composable
internal fun SettingsItem(
    onClick: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    subtitle: String? = null,
    showBorder: Boolean = false,
    statusContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = { ForwardChevronIcon() }
) {
    SettingsItemCard(
        onClick = onClick,
        borderWidth = if (showBorder) 1.dp else 0.dp,
        modifier = modifier
    ) {
        IconTextRow(
            modifier = Modifier.padding(AppDefaults.ContentPadding),
            leadingContent = leadingContent ?: icon?.let { { ThemedIcon(icon = it) } },
            title = title,
            description = subtitle,
            trailingContent = statusContent ?: trailingContent
        )
    }
}