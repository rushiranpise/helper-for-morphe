package dev.rushi.apkdownloadhelper.ui.shared.layout

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults

/** Horizontal divider tinted the way the manager tints its settings dividers. */
@Composable
internal fun AppDivider(
    modifier: Modifier = Modifier,
    fullWidth: Boolean = false
) {
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val surfaceTint = MaterialTheme.colorScheme.surfaceTint
    val color = remember(outlineVariant, surfaceTint) {
        lerp(outlineVariant, surfaceTint, 0.18f).copy(alpha = 0.55f)
    }
    HorizontalDivider(
        modifier = if (fullWidth) modifier else modifier.padding(horizontal = AppDefaults.ContentPadding),
        color = color
    )
}
