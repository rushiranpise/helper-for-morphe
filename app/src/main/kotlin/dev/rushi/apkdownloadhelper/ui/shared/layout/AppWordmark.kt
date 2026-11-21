package dev.rushi.apkdownloadhelper.ui.shared.layout

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.ui.theme.WordmarkAspectRatio

/**
 * The Morphe wordmark: the logo's M in its own colors, then the rest of the name in [textColor],
 * so it reads on whatever the theme lays under it.
 */
@Composable
internal fun AppWordmark(
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Box(modifier = modifier.aspectRatio(WordmarkAspectRatio)) {
        Image(
            painter = painterResource(R.drawable.ic_morphe_wordmark_letter),
            contentDescription = null,
            modifier = Modifier.matchParentSize()
        )
        Image(
            painter = painterResource(R.drawable.ic_morphe_wordmark_text),
            contentDescription = null,
            colorFilter = ColorFilter.tint(textColor),
            modifier = Modifier.matchParentSize()
        )
    }
}
