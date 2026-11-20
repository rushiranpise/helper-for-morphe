package dev.rushi.apkdownloadhelper.ui

import android.content.res.Configuration
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import dev.rushi.apkdownloadhelper.storage.ThemeMode
import dev.rushi.apkdownloadhelper.storage.ThemeStyle
import kotlin.math.roundToInt

/** Test switch: true = picked tile has a border, false = picked tile is shown by its fill alone. */
private const val SelectedTileBorder = true

// =====================================================================================
// Theme mode + Color style selector (ported from morphe-manager's ThemeSelector,
// OptionTiles, SelectionTile, ButtonGroupRow, PressFeedback and ConnectedShapes)
// =====================================================================================

/**
 * Theme mode and color style, each a single row of compact tiles in one card.
 */
@Composable
internal fun ThemeSelectorCard(
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    themeStyle: ThemeStyle,
    supportsDynamicColor: Boolean,
    onThemeStyleSelected: (ThemeStyle) -> Unit
) {
    // Same order as Morphe: System, Light, Dark
    val modes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
    val styles = ThemeStyle.entries.filter { it != ThemeStyle.MATERIAL_YOU || supportsDynamicColor }

    // SurfaceCard draws no border by default; SectionCard is the one with the hairline border
    SurfaceCard(cornerRadius = MorpheDefaults.SectionCornerRadius) {
        Column(
            modifier = Modifier.padding(MorpheDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
        ) {
            ThemeOptionRow(
                title = "Theme mode",
                options = modes.map { ThemeOption(it, it.icon(), it.title) },
                selected = themeMode,
                onSelected = onThemeModeSelected
            )
            ThemeOptionRow(
                title = "Color style",
                options = styles.map { ThemeOption(it, it.icon(), it.title) },
                selected = themeStyle,
                onSelected = onThemeStyleSelected
            )
        }
    }
}

private data class ThemeOption<T>(val value: T, val icon: ImageVector, val label: String)

@Composable
private fun <T> ThemeOptionRow(
    title: String,
    options: List<ThemeOption<T>>,
    selected: T,
    onSelected: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ContentPaddingSmall)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        OptionGroup(items = options) { option, shape, interactionSource, itemModifier ->
            IconOptionCard(
                selected = option.value == selected,
                onClick = { onSelected(option.value) },
                icon = option.icon,
                label = option.label,
                modifier = itemModifier,
                shape = shape,
                interactionSource = interactionSource
            )
        }
    }
}

// =====================================================================================
// Option group + tile
// =====================================================================================

/** A single choice among a few [items], as one connected group whose buttons widen under a press. */
@Composable
internal fun <T> OptionGroup(
    items: List<T>,
    modifier: Modifier = Modifier,
    item: @Composable (item: T, shape: CornerBasedShape, interactionSource: MutableInteractionSource, modifier: Modifier) -> Unit
) {
    ButtonGroupRow(modifier = modifier) {
        items.forEachIndexed { index, option ->
            val interactionSource = remember { MutableInteractionSource() }
            item(
                option,
                PillGroupShapes.inRow(index, items.size),
                interactionSource,
                Modifier.buttonGroupItem(interactionSource)
            )
        }
    }
}

/** Compact icon-over-label tile, the one Morphe's theme selector uses. */
@Composable
internal fun IconOptionCard(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: CornerBasedShape = RoundedCornerShape(MorpheDefaults.CardCornerRadius),
    interactionSource: MutableInteractionSource? = null
) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    // A floor rather than a fixed height, so a label under a large font scale grows the tile
    val minHeight = if (landscape) 72.dp else 64.dp

    SelectionTile(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = minHeight),
        shape = shape,
        interactionSource = interactionSource
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MorpheDefaults.ItemSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) LocalContentColor.current else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) LocalContentColor.current else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodySmall.fontSize * 1.2
            )
        }
    }
}

/**
 * Selectable glass tile. The picked tile rounds out fully (pill) and gets a thicker border;
 * corners also shrink slightly while pressed.
 */
@Composable
internal fun SelectionTile(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: CornerBasedShape = RoundedCornerShape(MorpheDefaults.CardCornerRadius),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val tileShape = rememberPressShape(
        source,
        animateCornersAsState(if (selected) MorpheDefaults.PillShape else shape),
        enabled
    )
    // Unselected tiles never draw a border. The picked tile's border follows SelectedTileBorder
    val border = if (selected && SelectedTileBorder) {
        BorderStroke(2.dp, MorpheColors.glassBorderColor(selected = true))
    } else {
        null
    }

    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {
            role = Role.RadioButton
            this.selected = selected
        },
        shape = tileShape,
        color = MorpheColors.glassContainerColor(selected),
        contentColor = MorpheColors.glassContentColor(selected),
        border = border,
        onClick = onClick,
        enabled = enabled,
        interactionSource = source
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

// =====================================================================================
// Shapes: connected pill group + animated / pressed corners
// =====================================================================================

private val PillOuter = CornerSize(50)
private val PillInner = CornerSize(4.dp)

/** Shapes of the members of a connected row of buttons: round at the ends, nearly square between. */
internal object PillGroupShapes {
    private val single = RoundedCornerShape(PillOuter)
    private val leading = RoundedCornerShape(
        topStart = PillOuter, bottomStart = PillOuter, topEnd = PillInner, bottomEnd = PillInner
    )
    private val middle = RoundedCornerShape(PillInner)
    private val trailing = RoundedCornerShape(
        topStart = PillInner, bottomStart = PillInner, topEnd = PillOuter, bottomEnd = PillOuter
    )

    fun inRow(index: Int, count: Int): RoundedCornerShape = when {
        count == 1 -> single
        index == 0 -> leading
        index == count - 1 -> trailing
        else -> middle
    }
}

/** How far into a press [interactionSource] is, from 0 at rest to 1 held. */
@Composable
private fun rememberPressProgress(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    label: String = "press_progress"
): State<Float> {
    val isPressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (enabled && isPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = label
    )
}

/** [shape] with its corners rounding down while pressed. */
@Composable
internal fun rememberPressShape(
    interactionSource: InteractionSource,
    shape: Shape,
    enabled: Boolean = true
): Shape {
    val progress by rememberPressProgress(interactionSource, enabled, label = "press_shape")
    return remember(shape, progress) { shape.pressed(progress) }
}

private const val PressedCornerScale = 0.5f

private fun Shape.pressed(progress: Float): Shape {
    if (this !is CornerBasedShape || progress == 0f) return this
    val scale = lerp(1f, PressedCornerScale, progress)
    return copy(
        topStart = ScaledCornerSize(topStart, scale),
        topEnd = ScaledCornerSize(topEnd, scale),
        bottomEnd = ScaledCornerSize(bottomEnd, scale),
        bottomStart = ScaledCornerSize(bottomStart, scale)
    )
}

private data class ScaledCornerSize(val base: CornerSize, val scale: Float) : CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float = base.toPx(shapeSize, density) * scale
}

/**
 * [target] with its corners easing over from the shape it had before, so a picked button
 * rounds out rather than jumping. The first shape is taken at once.
 */
@Composable
internal fun animateCornersAsState(target: CornerBasedShape): CornerBasedShape {
    val corners = remember { AnimatedCorners(target) }
    LaunchedEffect(target) { corners.animateTo(target) }
    return corners.shape
}

@Stable
private class AnimatedCorners(initial: CornerBasedShape) {
    private class Leg(val from: CornerBasedShape, val to: CornerBasedShape)

    private var leg by mutableStateOf(Leg(initial, initial))
    private var progress by mutableFloatStateOf(1f)

    /** The same shape throughout; its corners are resolved where it is drawn. */
    val shape: CornerBasedShape = initial.copy(
        topStart = Corner(CornerBasedShape::topStart),
        topEnd = Corner(CornerBasedShape::topEnd),
        bottomEnd = Corner(CornerBasedShape::bottomEnd),
        bottomStart = Corner(CornerBasedShape::bottomStart)
    )

    suspend fun animateTo(target: CornerBasedShape) {
        val current = leg
        if (target == current.to) return
        leg = Leg(current.at(progress), target)
        progress = 0f
        animate(0f, 1f, animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)) { value, _ ->
            progress = value
        }
    }

    private fun Leg.at(fraction: Float): CornerBasedShape = when (fraction) {
        0f -> from
        1f -> to
        else -> to.copy(
            topStart = LerpCornerSize(from.topStart, to.topStart, fraction),
            topEnd = LerpCornerSize(from.topEnd, to.topEnd, fraction),
            bottomEnd = LerpCornerSize(from.bottomEnd, to.bottomEnd, fraction),
            bottomStart = LerpCornerSize(from.bottomStart, to.bottomStart, fraction)
        )
    }

    private inner class Corner(private val of: (CornerBasedShape) -> CornerSize) : CornerSize {
        override fun toPx(shapeSize: Size, density: Density): Float {
            val leg = leg
            val end = of(leg.to).toPx(shapeSize, density)
            val fraction = progress
            return if (fraction == 1f) end else lerp(of(leg.from).toPx(shapeSize, density), end, fraction)
        }
    }
}

private data class LerpCornerSize(val start: CornerSize, val stop: CornerSize, val fraction: Float) : CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float =
        lerp(start.toPx(shapeSize, density), stop.toPx(shapeSize, density), fraction)
}

// =====================================================================================
// ButtonGroupRow: equally wide buttons, a pressed one widens at its neighbours' expense
// =====================================================================================

private const val PressedWidthRatio = 0.15f

/** Spacing of Material's connected button group (ButtonGroupDefaults.ConnectedSpaceBetween). */
private val ConnectedSpaceBetween = 2.dp

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