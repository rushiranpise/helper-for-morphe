package dev.rushi.apkdownloadhelper.ui

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/** Gap between the rows of a segmented list, the one a connected group of buttons keeps. */
internal val SegmentGap = 2.dp

/** The page's own corner radius, matched by the outer edges of a connected group. */
private val OuterCorner = CornerSize(MorpheDefaults.SectionCornerRadius)

/** Corner where two members of a connected group meet, nearly square so they read as one piece. */
private val InnerCorner = CornerSize(4.dp)

/** Shapes of the members of a connected group, as Material's segmented lists use. */
@Immutable
private class ConnectedShapes {
    private val single = RoundedCornerShape(OuterCorner)
    private val top = RoundedCornerShape(
        topStart = OuterCorner,
        topEnd = OuterCorner,
        bottomStart = InnerCorner,
        bottomEnd = InnerCorner
    )
    private val middle = RoundedCornerShape(InnerCorner)
    private val bottom = RoundedCornerShape(
        topStart = InnerCorner,
        topEnd = InnerCorner,
        bottomStart = OuterCorner,
        bottomEnd = OuterCorner
    )

    /** Shape of the member at [index] of a column of [count], rounded all round for one on its own. */
    fun inColumn(index: Int, count: Int): RoundedCornerShape = when {
        count == 1 -> single
        index == 0 -> top
        index == count - 1 -> bottom
        else -> middle
    }

    companion object {
        val Card = ConnectedShapes()
    }
}

/**
 * The group a settings row belongs to, null outside one. Each member is rounded on the group's
 * outer edges and nearly square where it meets the next.
 */
internal val LocalSegmentRegistry = staticCompositionLocalOf<SegmentRegistry?> { null }

/**
 * Shape of [row] in this group, its corners read where it is drawn: its place is known only once
 * the group is laid out, which comes before the first frame is drawn.
 */
@Composable
internal fun SegmentRegistry.rowShape(row: Any): CornerBasedShape {
    DisposableEffect(this, row) {
        onDispose { remove(row) }
    }
    return remember(this, row) {
        RoundedCornerShape(
            topStart = PlacedCorner { shape(row).topStart },
            topEnd = PlacedCorner { shape(row).topEnd },
            bottomEnd = PlacedCorner { shape(row).bottomEnd },
            bottomStart = PlacedCorner { shape(row).bottomStart }
        )
    }
}

/** A corner of a row's shape, taken from its place in the group each time its outline is made. */
private class PlacedCorner(private val corner: () -> CornerSize) : CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float = corner().toPx(shapeSize, density)
}

/**
 * The rows of a settings group in the order they show within it, so each takes the shape of its
 * place among those showing. Positions are only compared, so scrolling never rewrites the order.
 */
@Stable
internal class SegmentRegistry {
    private val rows = mutableMapOf<Any, LayoutCoordinates>()
    private var group: LayoutCoordinates? = null
    private var order by mutableStateOf(emptyList<Any>())

    /** Shape of [row] by its place among the showing rows, middle until it first shows. */
    fun shape(row: Any): RoundedCornerShape {
        val index = order.indexOf(row)
        return if (index < 0) {
            ConnectedShapes.Card.inColumn(1, 3)
        } else {
            ConnectedShapes.Card.inColumn(index, order.size)
        }
    }

    fun placeGroup(coordinates: LayoutCoordinates) {
        group = coordinates
        reorder()
    }

    fun place(row: Any, coordinates: LayoutCoordinates) {
        rows[row] = coordinates
        reorder()
    }

    fun remove(row: Any) {
        if (rows.remove(row) != null) reorder()
    }

    private fun reorder() {
        val group = group?.takeIf { it.isAttached } ?: return
        val sorted = rows.mapNotNull { (row, coordinates) ->
            if (!coordinates.isAttached) return@mapNotNull null
            val shown = group.localBoundingBoxOf(coordinates, clipBounds = true)
            if (shown.height > 0f) row to shown.top else null
        }.sortedBy { (_, top) -> top }.map { (row, _) -> row }
        if (sorted != order) order = sorted
    }
}
