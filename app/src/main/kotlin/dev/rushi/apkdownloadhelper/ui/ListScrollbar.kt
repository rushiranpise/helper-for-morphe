package dev.rushi.apkdownloadhelper.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun ListScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val viewportHeight = scrollState.viewportSize
    val contentHeight = scrollState.viewportSize + scrollState.maxValue
    val thumbHeightFractionSize = if (contentHeight > 0) viewportHeight.toFloat() / contentHeight else 1f
    val scrollFraction by remember {
        derivedStateOf {
            if (scrollState.maxValue == 0) 0f else scrollState.value.toFloat() / scrollState.maxValue
        }
    }

    // Only show scrollbar if content is scrollable
    if (scrollState.maxValue <= 0) return

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(thumbHeightFractionSize)
                    .graphicsLayer {
                        translationY = scrollFraction * (viewportHeight - (viewportHeight * thumbHeightFractionSize))
                    }
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
            )
        }
    }
}