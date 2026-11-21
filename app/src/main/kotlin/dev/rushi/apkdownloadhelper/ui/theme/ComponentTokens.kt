package dev.rushi.apkdownloadhelper.ui.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.unit.dp

internal val MarkerRadius = 10.dp

internal val MarkerStroke = 3.dp

/**
 * Exactly the handle's own diameter, so it fills the strip end to end. A ring is
 * stroked centred on its radius, which puts only half the width outside it.
 */
internal val HueStripHeight = (MarkerRadius + MarkerStroke / 2) * 2

/** Gap between the rows of a segmented list, the one a connected group of buttons keeps. */
internal val SegmentGap = 2.dp

/** The page's own corner radius, matched by the outer edges of a connected group. */
internal val OuterCorner = CornerSize(AppDefaults.SectionCornerRadius)

/** Corner where two members of a connected group meet, nearly square so they read as one piece. */
internal val InnerCorner = CornerSize(4.dp)

/** How far into a row its edge fades, enough to read as more to come without hiding a label. */
internal val EdgeFadeWidth = 24.dp

internal val ScrollbarWidth = 14.dp

internal val ScrollbarTrackWidth = 4.dp

internal val ScrollbarVerticalPadding = 8.dp

internal val ScrollbarMinThumbHeight = 36.dp

internal const val ScrollbarMinThumbFraction = 0.08f

internal const val ScrollbarIdleTimeout = 650L

/** Test switch: true = picked tile has a border, false = picked tile is shown by its fill alone. */
internal const val SelectedTileBorder = true

// =====================================================================================
// Shapes: connected pill group + animated / pressed corners
// =====================================================================================
internal val PillOuter = CornerSize(50)

internal val PillInner = CornerSize(4.dp)

internal const val PressedCornerScale = 0.5f

// =====================================================================================
// ButtonGroupRow: equally wide buttons, a pressed one widens at its neighbours' expense
// =====================================================================================
internal const val PressedWidthRatio = 0.15f

/** Spacing of Material's connected button group (ButtonGroupDefaults.ConnectedSpaceBetween). */
internal val ConnectedSpaceBetween = 2.dp

/** Width over height of the wordmark's viewport, cut to the edges of the letters. */
internal const val WordmarkAspectRatio = 428f / 106f
