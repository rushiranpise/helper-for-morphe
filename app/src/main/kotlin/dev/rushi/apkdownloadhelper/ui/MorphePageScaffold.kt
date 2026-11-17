package dev.rushi.apkdownloadhelper.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class PageInsets(val top: Dp, val bottom: Dp, val horizontal: Dp)

/**
 * Pinned-header page. Opaque by default so pushed screens cannot show the page
 * underneath. Only top-level pager pages should opt into a transparent background.
 */
@Composable
fun MorphePinnedHeaderPage(
    header: @Composable ColumnScope.() -> Unit,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    content: @Composable BoxScope.(insets: PageInsets) -> Unit
) {
    val density = LocalDensity.current
    val resolvedBackground = backgroundColor ?: MaterialTheme.colorScheme.background

    SubcomposeLayout(
        modifier = modifier
            .fillMaxSize()
            .background(resolvedBackground)
    ) { constraints ->
        // 1. Measure Header first
        val headerPlaceables = subcompose("Header") {
            Box(modifier = Modifier.fillMaxWidth()) {
                HeaderBand(
                    glass = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    scrollState = scrollState
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    content = header
                )
            }
        }.map { it.measure(constraints.copy(minHeight = 0)) }

        val headerHeightPx = headerPlaceables.maxOfOrNull { it.height } ?: 0

        // 2. Measure Content with the proper top padding
        val insets = PageInsets(
            top = with(density) { headerHeightPx.toDp() },
            bottom = 32.dp,
            horizontal = 16.dp
        )

        val contentPlaceables = subcompose("Content") {
            Box(modifier = Modifier.fillMaxSize()) {
                content(insets)
            }
        }.map { it.measure(constraints) }

        // 3. Layout everything
        layout(constraints.maxWidth, constraints.maxHeight) {
            contentPlaceables.forEach { it.place(0, 0) }
            headerPlaceables.forEach { it.place(0, 0) }
        }
    }
}

@Composable
fun MorpheSubpageScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit
) {
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    MorphePinnedHeaderPage(
        header = {
            MorpheHeaderBar(title = title, onBack = onBack)
        },
        scrollState = scrollState,
        modifier = modifier
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    start = insets.horizontal,
                    top = insets.top + 16.dp,
                    end = insets.horizontal,
                    bottom = navigationBar + 32.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )

        ScrollToTopButton(
            scrollState = scrollState,
            extraBottomPadding = navigationBar
        )
    }
}

@Composable
fun MorpheHeaderBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onBack != null) {
            HelperHeaderIconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                onClick = onBack
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BoxScope.HeaderBand(
    glass: Color,
    shape: Shape,
    scrollState: ScrollState
) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer {
                // Once content starts moving under the header it must be covered
                // completely; a partial fade would leave text visible through it.
                alpha = if (scrollState.value > 0) 1f else 0f
            }
            // Using solid background instead of haze to guarantee text is hidden
            .background(glass, shape)
    )
}