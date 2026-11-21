package dev.rushi.apkdownloadhelper.ui.shared.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.ui.shared.text.fittedTextAutoSize
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.pressScale

/**
 * Semi-transparent dialog action button (the manager's `AppDialogButton` family), shared by
 * every confirmation surface in the helper.
 */
@Composable
internal fun DialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    isDestructive: Boolean = false,
    filled: Boolean = true,
    textSuffix: String? = null
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val isDark = !textColor.isDarkColor()
    val primaryColor = MaterialTheme.colorScheme.primary
    val destructiveDark = Color(0xFFFF6B6B)
    val destructiveLight = Color(0xFFD32F2F)

    val containerColor = when {
        isDestructive && filled -> Color.Red.copy(alpha = if (isDark) 0.25f else 0.2f)
        isDestructive -> Color.Transparent
        filled -> primaryColor.copy(alpha = if (isDark) 0.3f else 0.25f)
        else -> Color.Transparent
    }
    val contentColor = when {
        isDestructive -> if (isDark) destructiveDark else destructiveLight
        filled -> textColor
        else -> textColor.copy(alpha = 0.85f)
    }
    val borderColor = when {
        isDestructive -> Color.Red.copy(alpha = if (isDark) 0.4f else 0.35f)
        filled -> primaryColor.copy(alpha = if (isDark) 0.5f else 0.4f)
        else -> primaryColor.copy(alpha = if (isDark) 0.3f else 0.25f)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val buttonModifier = modifier
        .height(AppDefaults.DialogButtonHeight)
        .pressScale(interactionSource = interactionSource, enabled = enabled)
    val shape = RoundedCornerShape(AppDefaults.CardCornerRadius)
    val border = BorderStroke(1.dp, borderColor)
    val contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    val labelStyle = MaterialTheme.typography.labelLarge
    val content: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(AppDefaults.IconSizeSmall)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = labelStyle,
            autoSize = fittedTextAutoSize(labelStyle.fontSize),
            maxLines = 1,
            overflow = if (textSuffix == null) TextOverflow.Ellipsis else TextOverflow.Clip
        )
        if (textSuffix != null) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = textSuffix,
                style = labelStyle,
                autoSize = fittedTextAutoSize(labelStyle.fontSize),
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }

    if (filled) {
        Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            interactionSource = interactionSource,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = containerColor.copy(alpha = 0.5f),
                disabledContentColor = contentColor.copy(alpha = 0.5f)
            ),
            border = border,
            contentPadding = contentPadding,
            content = content
        )
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            interactionSource = interactionSource,
            shape = shape,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = contentColor,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = contentColor.copy(alpha = 0.4f)
            ),
            border = border,
            contentPadding = contentPadding,
            content = content
        )
    }
}

/** Approximate luminance test for picking destructive accents. */
private fun Color.isDarkColor(): Boolean =
    (0.299 * red + 0.587 * green + 0.114 * blue) < 0.5
