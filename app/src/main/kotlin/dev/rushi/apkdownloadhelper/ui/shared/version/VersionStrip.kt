package dev.rushi.apkdownloadhelper.ui.shared.version

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Displays version pills in a single horizontally scrollable row by default.
 * Automatically detects when the pills overflow past the screen edge and shows
 * a right-aligned down-arrow button to expand/collapse all pills into a wrapped FlowRow.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ExpandableVersionRow(
    versions: List<String>,
    chipShape: RoundedCornerShape,
    chipHorizontalPadding: Dp,
    chipVerticalPadding: Dp,
    chipSpacing: Dp,
    containerColorFor: @Composable (String) -> Color,
    contentColorFor: @Composable (String) -> Color,
    borderColorFor: ((String) -> Color)? = null,
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var internalExpanded by remember(versions) { mutableStateOf(false) }
    val isExpanded = expanded ?: internalExpanded
    val setExpanded: (Boolean) -> Unit = onExpandedChange ?: { internalExpanded = it }

    val scrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val hasOverflow = if (versions.isEmpty()) {
            false
        } else {
            var requiredWidth = chipSpacing * (versions.size - 1).coerceAtLeast(0)
            versions.forEach { ver ->
                val textWidth = textMeasurer.measure(
                    text = ver,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    softWrap = false
                ).size.width
                requiredWidth += with(density) { textWidth.toDp() } + chipHorizontalPadding * 2
            }
            requiredWidth > maxWidth
        }
        val showToggle = isExpanded || hasOverflow

        if (isExpanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(chipSpacing),
                    verticalArrangement = Arrangement.spacedBy(chipSpacing)
                ) {
                    versions.forEach { ver ->
                        VersionPillChip(
                            text = ver,
                            shape = chipShape,
                            horizontalPadding = chipHorizontalPadding,
                            verticalPadding = chipVerticalPadding,
                            containerColor = containerColorFor(ver),
                            contentColor = contentColorFor(ver),
                            borderColor = borderColorFor?.invoke(ver),
                            wrap = true
                        )
                    }
                }
                VersionExpandToggleButton(
                    expanded = true,
                    onClick = { setExpanded(false) }
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(chipSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    versions.forEach { ver ->
                        VersionPillChip(
                            text = ver,
                            shape = chipShape,
                            horizontalPadding = chipHorizontalPadding,
                            verticalPadding = chipVerticalPadding,
                            containerColor = containerColorFor(ver),
                            contentColor = contentColorFor(ver),
                            borderColor = borderColorFor?.invoke(ver)
                        )
                    }
                }
                if (showToggle) {
                    VersionExpandToggleButton(
                        expanded = false,
                        onClick = { setExpanded(true) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VersionPillChip(
    text: String,
    shape: RoundedCornerShape,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color? = null,
    wrap: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(shape)
            .background(containerColor)
            .then(
                if (borderColor != null) {
                    Modifier.border(width = 1.dp, color = borderColor, shape = shape)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            maxLines = if (wrap) Int.MAX_VALUE else 1,
            overflow = if (wrap) TextOverflow.Clip else TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VersionExpandToggleButton(
    expanded: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(scheme.surfaceVariant.copy(alpha = 0.65f))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = if (expanded) "Collapse versions" else "Show all versions",
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun AppVersionStrip(versions: List<String>) {
    val scheme = MaterialTheme.colorScheme
    ExpandableVersionRow(
        versions = versions,
        chipShape = RoundedCornerShape(10.dp),
        chipHorizontalPadding = 12.dp,
        chipVerticalPadding = 8.dp,
        chipSpacing = 8.dp,
        containerColorFor = { scheme.surfaceVariant.copy(alpha = 0.45f) },
        contentColorFor = { scheme.onSurface },
        borderColorFor = { scheme.outline.copy(alpha = 0.22f) }
    )
}
