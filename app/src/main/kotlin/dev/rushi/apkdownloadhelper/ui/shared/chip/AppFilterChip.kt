package dev.rushi.apkdownloadhelper.ui.shared.chip

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.rushi.apkdownloadhelper.ui.shared.shape.rememberPressShape
import dev.rushi.apkdownloadhelper.ui.shared.text.fittedTextAutoSize
import dev.rushi.apkdownloadhelper.ui.theme.pressScale

/**
 * Filter chip for the lists that narrow down  the manager's `AppFilterChip`. Carries a fill of
 * its own rather than the platform's transparent one, which would show the raised surface back
 * and leave only a hairline to say a button is there.
 */
@Composable
internal fun AppFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    selectedIcon: ImageVector = Icons.Outlined.Done
) {
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }

    FilterChip(
        selected = selected,
        onClick = onClick,
        interactionSource = interactionSource,
        shape = rememberPressShape(interactionSource, FilterChipDefaults.shape),
        label = {
            val labelStyle = MaterialTheme.typography.labelMedium
            Text(
                text = label,
                style = labelStyle,
                autoSize = fittedTextAutoSize(labelStyle.fontSize),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier.pressScale(interactionSource, restCorner = 16.dp, pressedCorner = 8.dp),
        leadingIcon = if (selected) {
            { Icon(selectedIcon, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = scheme.surfaceColorAtElevation(2.dp),
            labelColor = scheme.onSurfaceVariant,
            selectedContainerColor = scheme.primaryContainer,
            selectedLabelColor = scheme.onPrimaryContainer,
            selectedLeadingIconColor = scheme.onPrimaryContainer
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = scheme.outline.copy(alpha = 0.5f),
            selectedBorderColor = scheme.primary,
            selectedBorderWidth = 1.dp
        )
    )
}
