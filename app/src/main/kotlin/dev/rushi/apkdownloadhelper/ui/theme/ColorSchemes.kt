package dev.rushi.apkdownloadhelper.ui.theme

import androidx.compose.foundation.background
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import dev.rushi.apkdownloadhelper.ui.theme.MonochromeThemeDefaults.iconBackground

// ---------------------------------------------------------------------------
// Colour scheme  exact values from morphe-manager's ui/theme/Color.kt
// ---------------------------------------------------------------------------
internal fun appDarkColorScheme() = darkColorScheme(
    primary = Color(0xFFA4C9FF),
    onPrimary = Color(0xFF00315D),
    primaryContainer = Color(0xFF004884),
    onPrimaryContainer = Color(0xFFD4E3FF),
    secondary = Color(0xFFBCC7DB),
    onSecondary = Color(0xFF263141),
    secondaryContainer = Color(0xFF3D4758),
    onSecondaryContainer = Color(0xFFD8E3F8),
    tertiary = Color(0xFFD9BDE3),
    onTertiary = Color(0xFF3D2946),
    tertiaryContainer = Color(0xFF543F5E),
    onTertiaryContainer = Color(0xFFF6D9FF),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C6CF),
    outline = Color(0xFF8D9199),
    inverseOnSurface = Color(0xFF1A1C1E),
    inverseSurface = Color(0xFFE3E2E6),
    inversePrimary = Color(0xFF005FAC),
    surfaceTint = Color(0xFFA4C9FF),
    outlineVariant = Color(0xFF43474E),
    scrim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF0C0E11),
    surfaceContainerLow = Color(0xFF1A1C1E),
    surfaceContainer = Color(0xFF1E2022),
    surfaceContainerHigh = Color(0xFF282A2C),
    surfaceContainerHighest = Color(0xFF333537),
    surfaceBright = Color(0xFF37393C),
    surfaceDim = Color(0xFF111416),
)

internal fun appLightColorScheme() = lightColorScheme(
    primary = Color(0xFF005FAC),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4E3FF),
    onPrimaryContainer = Color(0xFF001C39),
    secondary = Color(0xFF545F71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD8E3F8),
    onSecondaryContainer = Color(0xFF111C2B),
    tertiary = Color(0xFF6D5677),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF6D9FF),
    onTertiaryContainer = Color(0xFF271430),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFDFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFDFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F),
    inverseOnSurface = Color(0xFFF1F0F4),
    inverseSurface = Color(0xFF2F3033),
    inversePrimary = Color(0xFFA4C9FF),
    surfaceTint = Color(0xFF005FAC),
    outlineVariant = Color(0xFFC3C6CF),
    scrim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F3F7),
    surfaceContainer = Color(0xFFEEEDF1),
    surfaceContainerHigh = Color(0xFFE9E8EC),
    surfaceContainerHighest = Color(0xFFE3E2E6),
    surfaceBright = Color(0xFFFAF9FD),
    surfaceDim = Color(0xFFDAD9DD),
)

/**
 * Neutral palette for [ThemeStyle.MONOCHROME], exactly the manager's
 * `monochrome_*` values. Overlaid on the Morphe scheme rather than built from
 * scratch so the roles the manager leaves alone (error, scrim) stay the same.
 */
internal fun monochromeDarkColorScheme() = appDarkColorScheme().copy(
    primary = Color(0xFFC8CED8),
    onPrimary = Color(0xFF29313A),
    primaryContainer = Color(0xFF3E4650),
    onPrimaryContainer = Color(0xFFE3E8EF),
    secondary = Color(0xFFC4C9D8),
    onSecondary = Color(0xFF2D323D),
    secondaryContainer = Color(0xFF434956),
    onSecondaryContainer = Color(0xFFE0E5F2),
    tertiary = Color(0xFFC8CBD3),
    onTertiary = Color(0xFF2F3138),
    tertiaryContainer = Color(0xFF464852),
    onTertiaryContainer = Color(0xFFE5E7EF),
    background = Color(0xFF0B0C10),
    onBackground = Color(0xFFE4E6EC),
    surface = Color(0xFF131418),
    onSurface = Color(0xFFE4E6EC),
    surfaceVariant = Color(0xFF444852),
    onSurfaceVariant = Color(0xFFC5C9D2),
    outline = Color(0xFF8E929C),
    outlineVariant = Color(0xFF444852),
    inverseOnSurface = Color(0xFF191B20),
    inverseSurface = Color(0xFFE4E6EC),
    inversePrimary = Color(0xFF565E68),
    surfaceTint = Color(0xFFC8CED8),
    surfaceContainerLowest = Color(0xFF06070A),
    surfaceContainerLow = Color(0xFF111216),
    surfaceContainer = Color(0xFF17191E),
    surfaceContainerHigh = Color(0xFF202228),
    surfaceContainerHighest = Color(0xFF2B2E35),
    surfaceBright = Color(0xFF383B43),
    surfaceDim = Color(0xFF0B0C10)
)

internal fun monochromeLightColorScheme() = appLightColorScheme().copy(
    primary = Color(0xFF565E68),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE3EA),
    onPrimaryContainer = Color(0xFF141B22),
    secondary = Color(0xFF5B6170),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E4EA),
    onSecondaryContainer = Color(0xFF181D28),
    tertiary = Color(0xFF60616A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE4E5EC),
    onTertiaryContainer = Color(0xFF1B1D24),
    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF191B20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191B20),
    surfaceVariant = Color(0xFFE7EAF1),
    onSurfaceVariant = Color(0xFF444852),
    outline = Color(0xFF747984),
    outlineVariant = Color(0xFFC5C9D2),
    inverseOnSurface = Color(0xFFF1F2F7),
    inverseSurface = Color(0xFF2E3035),
    inversePrimary = Color(0xFFC8CED8),
    surfaceTint = Color(0xFF565E68),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F4F9),
    surfaceContainer = Color(0xFFEEF1F7),
    surfaceContainerHigh = Color(0xFFE9ECF3),
    surfaceContainerHighest = Color(0xFFE3E6EE),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFD9DDE5)
)

/**
 * True while [ThemeStyle.MONOCHROME] is active, so decorative colour the scheme
 * cannot reach (gradients, generated tiles) can be flattened too.
 */
internal val LocalMonochromeTheme = staticCompositionLocalOf { false }

/**
 * Adapters that flatten accent-heavy decoration into neutral tokens in
 * monochrome mode and pass the originals through otherwise, mirroring the
 * manager's `MonochromeThemeDefaults`.
 */
internal object MonochromeThemeDefaults {
    @Composable
    fun accentColor(base: Color): Color =
        if (LocalMonochromeTheme.current) MaterialTheme.colorScheme.primary else base

    /** Solid neutral fill in monochrome mode, the caller's gradient otherwise. */
    @Composable
    fun iconBackground(gradient: List<Color>): Brush =
        if (LocalMonochromeTheme.current) {
            SolidColor(MaterialTheme.colorScheme.primaryContainer)
        } else {
            Brush.linearGradient(gradient)
        }

    /**
     * The tint paired with [iconBackground]. Primary sits too close to that fill
     * to read against it, so the icon takes the container's own on-colour.
     */
    @Composable
    fun iconTint(base: Color): Color =
        if (LocalMonochromeTheme.current) MaterialTheme.colorScheme.onPrimaryContainer else base
}

/**
 * Parses a stored accent (`#RRGGBB`, `#AARRGGBB` or either without the hash),
 * returning null for anything that is not a colour so a bad value falls back to
 * the style's own accent instead of painting the app black.
 */
internal fun String?.toAccentColorOrNull(): Color? {
    val trimmed = this?.trim()?.removePrefix("#") ?: return null
    val parsed = trimmed.toLongOrNull(16) ?: return null
    return when (trimmed.length) {
        6 -> Color(0xFF000000L or parsed)
        8 -> Color(parsed)
        else -> null
    }
}

internal fun Color.toHexString(): String =
    String.format("#%06X", 0xFFFFFF and toArgb())

/**
 * Re-derives the accent roles around [accent] the way the manager does: the
 * containers are the accent lightened for dark surfaces and darkened for light
 * ones, and every role takes the black-or-white foreground its own fill needs.
 */
internal fun ColorScheme.withCustomAccent(accent: Color, darkTheme: Boolean): ColorScheme {
    val primaryContainer = accent.adjustLightness(if (darkTheme) 0.25f else -0.25f)
    val secondary = accent.adjustLightness(if (darkTheme) 0.15f else -0.15f)
    val secondaryContainer = accent.adjustLightness(if (darkTheme) 0.35f else -0.35f)
    val tertiary = accent.adjustLightness(if (darkTheme) -0.1f else 0.1f)
    val tertiaryContainer = accent.adjustLightness(if (darkTheme) 0.4f else -0.4f)
    return copy(
        primary = accent,
        onPrimary = accent.contrastingForeground(),
        primaryContainer = primaryContainer,
        onPrimaryContainer = primaryContainer.contrastingForeground(),
        secondary = secondary,
        onSecondary = secondary.contrastingForeground(),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = secondaryContainer.contrastingForeground(),
        tertiary = tertiary,
        onTertiary = tertiary.contrastingForeground(),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = tertiaryContainer.contrastingForeground(),
        surfaceTint = accent,
        inversePrimary = accent.adjustLightness(if (darkTheme) -0.4f else 0.4f)
    )
}

/**
 * Paints the dark scheme's backgrounds pure black instead of its near-black, for
 * OLED screens that save power on black pixels. Only the backgrounds move: cards
 * keep their own elevated tone, or every surface would flatten into one block.
 */
internal fun ColorScheme.withPureBlack(): ColorScheme {
    val black = Color.Black
    return copy(background = black, surface = black, surfaceDim = black)
}

private fun Color.adjustLightness(delta: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    hsl[2] = (hsl[2] + delta).coerceIn(0f, 1f)
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun Color.contrastingForeground(): Color =
    if (ColorUtils.calculateLuminance(toArgb()) > 0.5) Color.Black else Color.White
