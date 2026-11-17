package dev.rushi.apkdownloadhelper.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** How far into a row its edge fades, enough to read as more to come without hiding a label. */
internal val EdgeFadeWidth = 24.dp

/**
 * Clips to bounds and fades the start and end edges out to nothing, each over as many pixels as
 * it hides, up to [length]. An edge fades only while something is cut off behind it.
 *
 * Fades the content itself rather than laying a strip of one color over it, so it works on any
 * surface, tinted ones included, which no single gradient color could match.
 */
internal fun Modifier.edgeFade(
    length: Dp,
    hiddenAtStart: () -> Float,
    hiddenAtEnd: () -> Float,
    orientation: Orientation = Orientation.Horizontal
): Modifier = clipToBounds()
    .graphicsLayer {
        compositingStrategy = if (hiddenAtStart() > 0f || hiddenAtEnd() > 0f) {
            CompositingStrategy.Offscreen
        } else {
            CompositingStrategy.Auto
        }
    }
    .drawWithContent {
        drawContent()
        val extent = if (orientation == Orientation.Horizontal) size.width else size.height
        val maxLength = length.toPx().coerceAtMost(extent / 2)
        if (maxLength <= 0f) return@drawWithContent
        // A row starts on the right in a right-to-left layout, a column always at the top
        val startIsFirst = orientation == Orientation.Vertical ||
            layoutDirection != LayoutDirection.Rtl
        fadeEdge(orientation, atFirst = startIsFirst, length = hiddenAtStart().coerceAtMost(maxLength))
        fadeEdge(orientation, atFirst = !startIsFirst, length = hiddenAtEnd().coerceAtMost(maxLength))
    }

/** [edgeFade] for a row that scrolls sideways, fading whichever end [scrollState] has more past. */
internal fun Modifier.horizontalScrollFade(
    scrollState: ScrollState,
    length: Dp = EdgeFadeWidth
): Modifier = scrollFade(scrollState, length, Orientation.Horizontal)

/** [edgeFade] for a column that scrolls, fading whichever end [scrollState] has more past. */
internal fun Modifier.verticalScrollFade(
    scrollState: ScrollState,
    length: Dp = EdgeFadeWidth
): Modifier = scrollFade(scrollState, length, Orientation.Vertical)

/** [edgeFade] for a lazy column, fading whichever end [state] has more past. */
internal fun Modifier.verticalScrollFade(
    state: LazyListState,
    length: Dp = EdgeFadeWidth
): Modifier = edgeFade(
    length = length,
    hiddenAtStart = { state.hiddenAtStart() },
    hiddenAtEnd = { state.hiddenAtEnd() },
    orientation = Orientation.Vertical
)

/** [edgeFade] for a lazy grid, fading whichever end [state] has more past. */
internal fun Modifier.verticalScrollFade(
    state: LazyGridState,
    length: Dp = EdgeFadeWidth
): Modifier = edgeFade(
    length = length,
    hiddenAtStart = { state.hiddenAtStart() },
    hiddenAtEnd = { state.hiddenAtEnd() },
    orientation = Orientation.Vertical
)

/**
 * How far [this] runs on past its start, for what follows the scroll in from there. A list whose
 * kind is not known here counts as wholly past it once it can scroll back at all.
 */
private fun ScrollableState.hiddenAtStart(): Float = when (this) {
    is ScrollState -> value.toFloat()
    is LazyListState -> hiddenAtStart()
    is LazyGridState -> hiddenAtStart()
    else -> if (canScrollBackward) Float.MAX_VALUE else 0f
}

// A lazy list measures only the rows in view, so a row out of sight counts as more than any fade
private fun LazyListState.hiddenAtStart(): Float =
    if (firstVisibleItemIndex > 0) Float.MAX_VALUE else firstVisibleItemScrollOffset.toFloat()

private fun LazyListState.hiddenAtEnd(): Float {
    val info = layoutInfo
    val last = info.visibleItemsInfo.lastOrNull() ?: return 0f
    if (last.index < info.totalItemsCount - 1) return Float.MAX_VALUE
    return (last.offset + last.size + info.afterContentPadding - info.viewportEndOffset)
        .coerceAtLeast(0)
        .toFloat()
}

// The first visible item always opens a line, so past index 0 the whole first line is out of view
private fun LazyGridState.hiddenAtStart(): Float =
    if (firstVisibleItemIndex > 0) Float.MAX_VALUE else firstVisibleItemScrollOffset.toFloat()

// Cells of the last line may differ in height, so the lowest edge among them is what counts
private fun LazyGridState.hiddenAtEnd(): Float {
    val info = layoutInfo
    val last = info.visibleItemsInfo.lastOrNull() ?: return 0f
    if (last.index < info.totalItemsCount - 1) return Float.MAX_VALUE
    val bottom = info.visibleItemsInfo.maxOf { it.offset.y + it.size.height }
    return (bottom + info.afterContentPadding - info.viewportEndOffset)
        .coerceAtLeast(0)
        .toFloat()
}

private fun Modifier.scrollFade(
    scrollState: ScrollState,
    length: Dp,
    orientation: Orientation
) = edgeFade(
    length = length,
    hiddenAtStart = { scrollState.hiddenAtStart() },
    hiddenAtEnd = { (scrollState.maxValue - scrollState.value).toFloat() },
    orientation = orientation
)

// Masks the fade draws with, kept rather than built on every frame an edge fades
private val FadeInMask = listOf(Color.Transparent, Color.Black)
private val FadeOutMask = listOf(Color.Black, Color.Transparent)

/**
 * Fades one edge out to nothing over [length]: the left or top one when [atFirst], the right or
 * bottom one otherwise.
 */
private fun DrawScope.fadeEdge(
    orientation: Orientation,
    atFirst: Boolean,
    length: Float
) {
    if (length <= 0f) return
    val colors = if (atFirst) FadeInMask else FadeOutMask

    if (orientation == Orientation.Horizontal) {
        val left = if (atFirst) 0f else size.width - length
        drawRect(
            brush = Brush.horizontalGradient(colors, startX = left, endX = left + length),
            topLeft = Offset(left, 0f),
            size = Size(length, size.height),
            blendMode = BlendMode.DstIn
        )
    } else {
        val top = if (atFirst) 0f else size.height - length
        drawRect(
            brush = Brush.verticalGradient(colors, startY = top, endY = top + length),
            topLeft = Offset(0f, top),
            size = Size(size.width, length),
            blendMode = BlendMode.DstIn
        )
    }
}
