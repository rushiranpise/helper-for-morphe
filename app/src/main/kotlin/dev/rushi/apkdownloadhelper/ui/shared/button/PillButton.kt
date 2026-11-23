package dev.rushi.apkdownloadhelper.ui.shared.button

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.ui.shared.shape.rememberPressShape
import dev.rushi.apkdownloadhelper.ui.shared.text.fittedTextAutoSize
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.SemanticTone
import dev.rushi.apkdownloadhelper.ui.theme.pressScale

/**
 * Pill-shaped action button with an icon and optional label  the manager's `ActionPillButton`,
 * shared by the compact card actions.
 */
@Composable
internal fun PillButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
    tall: Boolean = false,
    tone: SemanticTone = SemanticTone.Neutral
) {
    val interactionSource = remember { MutableInteractionSource() }
    val height = if (tall) AppDefaults.PillHeightLarge else AppDefaults.PillHeight
    val iconSize = if (tall) 20.dp else 18.dp
    // Without a label the pill has nothing to stretch for, so it stays a circle. A
    // filled row here would otherwise take every pixel left in the row it sits in and
    // push whatever follows it off the edge.
    val iconOnly = label == null

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = rememberPressShape(interactionSource, AppDefaults.PillShape, enabled),
        color = if (enabled) tone.container else tone.container.copy(alpha = 0.4f),
        contentColor = if (enabled) tone.content else tone.content.copy(alpha = 0.5f),
        interactionSource = interactionSource,
        modifier = modifier
            .height(height)
            .then(if (iconOnly) Modifier.width(height) else Modifier)
            .pressScale(interactionSource = interactionSource, enabled = enabled)
            .semantics { role = Role.Button }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(
                modifier = if (iconOnly) {
                    Modifier
                } else {
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDefaults.ContentPadding)
                },
                // Centred rather than start-aligned: these pills stretch to fill a card
                // row, and a start-aligned icon+label would sit against the left edge with
                // dead space beside it.
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(iconSize)
                )
                label?.let {
                    val labelStyle = if (tall) {
                        MaterialTheme.typography.labelLarge
                    } else {
                        MaterialTheme.typography.labelSmall
                    }
                    Text(
                        text = it,
                        style = labelStyle,
                        autoSize = fittedTextAutoSize(labelStyle.fontSize),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
