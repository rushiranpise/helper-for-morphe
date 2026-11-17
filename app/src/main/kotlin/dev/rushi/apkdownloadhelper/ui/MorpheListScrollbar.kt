package dev.rushi.apkdownloadhelper.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ScrollbarWidth = 14.dp
private val ScrollbarTrackWidth = 4.dp
private val ScrollbarVerticalPadding = 8.dp
private val ScrollbarMinThumbHeight = 36.dp
private const val ScrollbarMinThumbFraction = 0.08f
private const val ScrollbarIdleTimeout = 650L

/** A list's position and visible share of its content, for drawing and seeking. */
private data class ScrollbarMetrics(
    val progress: Float,
    val thumbFraction: Float,
    val averageItemSize: Float = 0f,
    val maxScrollOffset: Float = 1f
)

/**
 * Grabbable scrollbar for a [LazyListState], rendered in a narrow gutter beside the list. The
 * track and thumb fade in while scrolling, and dragging the thumb jumps proportionally across the
 * whole list instead of one row per finger pixel.
 */
@Composable
internal fun LazyListScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    // Keep every row height measured during this scroll session, so a tall card still contributes
    // its real size after it leaves the viewport and the thumb does not drift.
    val sizeCache = remember(listState) { mutableMapOf<Int, Int>() }
    val metrics by remember(listState) {
        derivedStateOf { listState.scrollbarMetrics(sizeCache) }
    }
    val canScroll by remember(listState) {
        derivedStateOf { listState.canScrollBackward || listState.canScrollForward }
    }
    val scope = rememberCoroutineScope()
    MorpheScrollbar(
        canScroll = canScroll,
        isScrolling = listState.isScrollInProgress,
        progress = { metrics.progress },
        thumbFraction = { metrics.thumbFraction },
        modifier = modifier,
        onSeek = { fraction ->
            scope.launch {
                val target = listState.seekTarget(fraction, sizeCache, metrics)
                listState.scrollToItem(target.first, target.second)
            }
        }
    )
}

/** Same fading indicator for a plain vertical [ScrollState] (patch lists). */
@Composable
internal fun ScrollStateScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val canScroll = scrollState.maxValue > 0
    val metrics by remember(scrollState) {
        derivedStateOf {
            if (scrollState.maxValue <= 0) {
                ScrollbarMetrics(progress = 0f, thumbFraction = 1f)
            } else {
                val content = (scrollState.viewportSize + scrollState.maxValue).coerceAtLeast(1)
                ScrollbarMetrics(
                    progress = (scrollState.value.toFloat() / scrollState.maxValue)
                        .coerceIn(0f, 1f),
                    thumbFraction = (scrollState.viewportSize.toFloat() / content)
                        .coerceIn(ScrollbarMinThumbFraction, 1f)
                )
            }
        }
    }
    val scope = rememberCoroutineScope()
    MorpheScrollbar(
        canScroll = canScroll,
        isScrolling = scrollState.isScrollInProgress,
        progress = { metrics.progress },
        thumbFraction = { metrics.thumbFraction },
        modifier = modifier,
        onSeek = { fraction ->
            scope.launch {
                scrollState.scrollTo((fraction * scrollState.maxValue).roundToInt())
            }
        }
    )
}

@Composable
private fun MorpheScrollbar(
    canScroll: Boolean,
    isScrolling: Boolean,
    progress: () -> Float,
    thumbFraction: () -> Float,
    modifier: Modifier,
    onSeek: suspend (Float) -> Unit
) {
    var dragging by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    val currentSeek by rememberUpdatedState(onSeek)
    val scope = rememberCoroutineScope()
    var seekJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(canScroll, isScrolling, dragging) {
        if (!canScroll) {
            visible = false
            return@LaunchedEffect
        }
        if (isScrolling || dragging) {
            visible = true
        } else {
            delay(ScrollbarIdleTimeout)
            visible = false
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (canScroll && visible) 1f else 0f,
        animationSpec = tween(160),
        label = "list_scrollbar_alpha"
    )

    if (!canScroll) return

    val colors = MaterialTheme.colorScheme
    val trackColor = colors.outlineVariant.copy(alpha = 0.34f)
    val idleThumbColor = colors.onSurfaceVariant.copy(alpha = 0.55f)
    val draggedThumbColor = colors.primary.copy(alpha = 0.9f)

    Box(
        modifier = modifier
            .width(ScrollbarWidth)
            .fillMaxHeight()
            .padding(vertical = ScrollbarVerticalPadding)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clearAndSetSemantics { }
                .drawBehind {
                    val shown = alpha
                    if (shown <= 0f) return@drawBehind

                    val trackWidth = ScrollbarTrackWidth.toPx()
                    val left = (size.width - trackWidth) / 2f
                    val corner = CornerRadius(trackWidth / 2f)
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(left, 0f),
                        size = Size(trackWidth, size.height),
                        cornerRadius = corner,
                        alpha = shown
                    )

                    val thumbHeight = (size.height * thumbFraction())
                        .coerceIn(
                            ScrollbarMinThumbHeight.toPx().coerceAtMost(size.height),
                            size.height
                        )
                    val travel = (size.height - thumbHeight).coerceAtLeast(0f)
                    drawRoundRect(
                        color = if (dragging) draggedThumbColor else idleThumbColor,
                        topLeft = Offset(left, travel * progress()),
                        size = Size(trackWidth, thumbHeight),
                        cornerRadius = corner,
                        alpha = shown
                    )
                }
                .pointerInput(Unit) {
                    fun seek(fraction: Float) {
                        seekJob?.cancel()
                        seekJob = scope.launch { currentSeek(fraction) }
                    }
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            dragging = true
                            seek((offset.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f))
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            seek((change.position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f))
                        },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false }
                    )
                }
        )
    }
}

/**
 * Turns a 0..1 drag position into the row and in-row offset that land at that fraction of the
 * scrollable range, using measured heights and the average only for rows not seen yet.
 */
private fun LazyListState.seekTarget(
    fraction: Float,
    sizeCache: Map<Int, Int>,
    metrics: ScrollbarMetrics
): Pair<Int, Int> {
    val total = layoutInfo.totalItemsCount
    if (total <= 0) return 0 to 0

    val lastIndex = total - 1
    // Aim past the last row at the very end and let the list clamp, since rows never measured may
    // turn out taller than the estimate.
    if (fraction >= 1f) {
        val lastSize = sizeCache[lastIndex]?.toFloat() ?: metrics.averageItemSize
        return lastIndex to (lastSize + layoutInfo.afterContentPadding).roundToInt().coerceAtLeast(0)
    }

    var remaining = fraction * metrics.maxScrollOffset
    for (index in 0 until lastIndex) {
        val itemSize = sizeCache[index]?.toFloat() ?: metrics.averageItemSize
        if (itemSize <= 0f || remaining < itemSize) {
            return index to remaining.roundToInt().coerceAtLeast(0)
        }
        remaining -= itemSize
    }
    return lastIndex to remaining.roundToInt().coerceAtLeast(0)
}

private fun LazyListState.scrollbarMetrics(
    sizeCache: MutableMap<Int, Int>
): ScrollbarMetrics {
    val info = layoutInfo
    val visible = info.visibleItemsInfo
    val total = info.totalItemsCount
    if (visible.isEmpty() || total <= 0) {
        return ScrollbarMetrics(progress = 0f, thumbFraction = 1f)
    }
    // Stale tail entries would keep inflating the estimate after the list shrinks
    if (sizeCache.keys.any { it >= total }) {
        sizeCache.keys.retainAll { it < total }
    }
    visible.forEach { sizeCache[it.index] = it.size }

    val viewport = (info.viewportEndOffset - info.viewportStartOffset).coerceAtLeast(1)
    val average = (sizeCache.values.sum().toFloat() / sizeCache.size).coerceAtLeast(1f)
    // Use measured heights where available and the average only for rows not yet seen, so a tall
    // card advances the thumb by exactly its own height as it scrolls past.
    var scrollOffset = firstVisibleItemScrollOffset.toFloat()
    for (index in 0 until firstVisibleItemIndex) {
        scrollOffset += sizeCache[index]?.toFloat() ?: average
    }
    var estimatedItemsSize = 0f
    for (index in 0 until total) {
        estimatedItemsSize += sizeCache[index]?.toFloat() ?: average
    }
    val content = (estimatedItemsSize + info.beforeContentPadding + info.afterContentPadding)
        .coerceAtLeast(viewport.toFloat())
    val maxScrollOffset = (content - viewport).coerceAtLeast(1f)
    return ScrollbarMetrics(
        progress = (scrollOffset / maxScrollOffset).coerceIn(0f, 1f),
        thumbFraction = (viewport / content).coerceIn(ScrollbarMinThumbFraction, 1f),
        averageItemSize = average,
        maxScrollOffset = maxScrollOffset
    )
}
