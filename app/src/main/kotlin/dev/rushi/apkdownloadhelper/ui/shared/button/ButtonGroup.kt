package dev.rushi.apkdownloadhelper.ui.shared.button

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import dev.rushi.apkdownloadhelper.ui.shared.shape.rememberPressProgress
import dev.rushi.apkdownloadhelper.ui.theme.ConnectedSpaceBetween
import dev.rushi.apkdownloadhelper.ui.theme.PressedWidthRatio
import kotlin.math.roundToInt

@Composable
internal fun ButtonGroupRow(
    modifier: Modifier = Modifier,
    spacing: Dp = ConnectedSpaceBetween,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier.fillMaxWidth()) { measurables, constraints ->
        if (measurables.isEmpty()) return@Layout layout(constraints.maxWidth, 0) {}

        val items = measurables.map { it.parentData as? ButtonGroupItemNode }
        val spacingPx = spacing.roundToPx()
        val fixed = items.map { it?.width?.roundToPx() }
        val sharing = measurables.indices.filter { fixed[it] == null }
        val room = (constraints.maxWidth - spacingPx * (measurables.size - 1) - fixed.sumOf { it ?: 0 })
            .coerceAtLeast(0)

        val widths = IntArray(measurables.size) { fixed[it] ?: 0 }
        sharing.forEachIndexed { position, index ->
            widths[index] = room / sharing.size + if (position < room % sharing.size) 1 else 0
        }

        val floor = IntArray(measurables.size) { index ->
            items[index]?.minWidth?.roundToPx()
                ?: minOf(widths[index], measurables[index].maxIntrinsicWidth(constraints.maxHeight))
        }
        val press = FloatArray(measurables.size) { (items[it]?.press?.invoke() ?: 0f).coerceAtLeast(0f) }
        widenPressed(widths, press, floor)

        val height = measurables.indices.maxOf { measurables[it].minIntrinsicHeight(widths[it]) }
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        val placeables = measurables.mapIndexed { index, measurable ->
            measurable.measure(Constraints.fixed(widths[index], height))
        }

        layout(constraints.maxWidth, height) {
            var x = 0
            placeables.forEach { placeable ->
                placeable.placeRelative(x, 0)
                x += placeable.width + spacingPx
            }
        }
    }
}

@Composable
internal fun Modifier.buttonGroupItem(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    width: Dp? = null,
    minWidth: Dp? = null
): Modifier {
    val press = rememberPressProgress(interactionSource, enabled, label = "button_group_press")
    val amount = remember(press) { { press.value } }
    return this then ButtonGroupItemElement(amount, width, minWidth)
}

private data class ButtonGroupItemElement(
    val press: () -> Float,
    val width: Dp?,
    val minWidth: Dp?
) : ModifierNodeElement<ButtonGroupItemNode>() {
    override fun create() = ButtonGroupItemNode(press, width, minWidth)

    override fun update(node: ButtonGroupItemNode) {
        node.press = press
        node.width = width
        node.minWidth = minWidth
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "buttonGroupItem"
        properties["press"] = press()
        properties["width"] = width
        properties["minWidth"] = minWidth
    }
}

private class ButtonGroupItemNode(
    var press: () -> Float,
    var width: Dp?,
    var minWidth: Dp?
) : Modifier.Node(), ParentDataModifierNode {
    override fun Density.modifyParentData(parentData: Any?): Any = this@ButtonGroupItemNode
}

private fun widenPressed(widths: IntArray, press: FloatArray, floor: IntArray) {
    press.forEachIndexed { index, amount ->
        if (amount == 0f) return@forEachIndexed
        val neighbors = listOf(index - 1, index + 1).filter { it in widths.indices }
        if (neighbors.isEmpty()) return@forEachIndexed
        val wanted = (widths[index] * PressedWidthRatio * amount).roundToInt()
        neighbors.forEach { neighbor ->
            val cut = minOf(wanted / neighbors.size, (widths[neighbor] - floor[neighbor]).coerceAtLeast(0))
            widths[neighbor] -= cut
            widths[index] += cut
        }
    }
}
