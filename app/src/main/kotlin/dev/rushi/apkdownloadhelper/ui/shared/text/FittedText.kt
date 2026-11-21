package dev.rushi.apkdownloadhelper.ui.shared.text

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Shrink a single-line control label only as far as it needs to fit, never below
 * [minFontSize]. The style's own size is the ceiling, so short labels keep their
 * designed size and nothing grows beyond the theme's scale.
 */
internal fun fittedTextAutoSize(
    maxFontSize: TextUnit,
    minFontSize: TextUnit = 10.sp
): TextAutoSize {
    val ceiling = if (maxFontSize == TextUnit.Unspecified) 14.sp else maxFontSize
    val floor = if (minFontSize.value > ceiling.value) ceiling else minFontSize
    return TextAutoSize.StepBased(
        minFontSize = floor,
        maxFontSize = ceiling
    )
}
