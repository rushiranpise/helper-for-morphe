package dev.rushi.apkdownloadhelper.ui.shared.chip

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.ui.shared.icon.ThemedIcon
import dev.rushi.apkdownloadhelper.ui.theme.SemanticTone

/** Sizing shared by every badge, so badges line up wherever they end up side by side. */
private object BadgeDefaults {
    val HorizontalPadding = 10.dp
    val VerticalPadding = 4.dp
    val IconSize = 14.dp
    val ItemSpacing = 5.dp
}

/**
 * Inline status marker, sized to its content.
 *
 * @param text Badge label, or null for a badge that is only its [icon]  dropping the label is
 *   for markers sharing a row with badges that need the room for their own words.
 * @param tone Semantic colour role
 * @param containerColor Background override, for badges drawn over custom artwork
 * @param contentColor Content override, paired with [containerColor]
 * @param onClick Makes the badge act as a control
 */
@Composable
internal fun StatusBadge(
    text: String?,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: SemanticTone = SemanticTone.Neutral,
    containerColor: Color = tone.container,
    contentColor: Color = tone.content,
    onClick: (() -> Unit)? = null
) {
    // Zero-width spaces so long tokens break at "/" and "." instead of overflowing the pill.
    val breakableText = remember(text) {
        text?.replace("/", "/\u200B")?.replace(".", ".\u200B")
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(containerColor)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(
                horizontal = BadgeDefaults.HorizontalPadding,
                vertical = BadgeDefaults.VerticalPadding
            ),
        horizontalArrangement = Arrangement.spacedBy(BadgeDefaults.ItemSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            ThemedIcon(icon = it, tint = contentColor, size = BadgeDefaults.IconSize)
        }
        breakableText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Badges on a line of their own, wrapping onto the next one when they run out of room.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StatusBadgeRow(
    modifier: Modifier = Modifier,
    content: @Composable FlowRowScope.() -> Unit
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(BadgeDefaults.ItemSpacing),
        verticalArrangement = Arrangement.spacedBy(BadgeDefaults.ItemSpacing),
        content = content
    )
}
