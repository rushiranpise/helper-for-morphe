package dev.rushi.apkdownloadhelper.ui.shared.appcard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import dev.rushi.apkdownloadhelper.ui.theme.toAccentColorOrNull
import dev.rushi.apkdownloadhelper.updater.ArchiveApp

internal val FindAppPlaceholderGradients = listOf(
    listOf(Color(0xFF1E5AA8), Color(0xFF00AFAE)),
    listOf(Color(0xFF6A3AB2), Color(0xFF3D5AFE)),
    listOf(Color(0xFFB3541E), Color(0xFFE0A030)),
    listOf(Color(0xFF2E7D32), Color(0xFF00AFAE)),
    listOf(Color(0xFF00796B), Color(0xFF1E5AA8))
)

data class FindAppCardColors(
    val gradient: List<Color>,
    val title: Color,
    val subtitle: Color
)

private val FindAppFallbackColors = listOf(
    Color(0xFF1E5AA8),
    Color(0xFF00796B),
    Color(0xFF6A3AB2),
    Color(0xFFB3541E),
    Color(0xFFB3261E),
    Color(0xFF2E7D32)
)

internal fun ArchiveApp.findAppCardColors(): FindAppCardColors {
    val base = iconColor.toAccentColorOrNull()
        ?: FindAppFallbackColors[(packageName.hashCode() and 0x7FFFFFFF) % FindAppFallbackColors.size]
    val luminance = base.luminance()
    val safe = if (luminance > 0.4f) {
        lerp(base, Color.Black, ((luminance - 0.4f) / luminance).coerceIn(0f, 0.65f))
    } else {
        base
    }
    return FindAppCardColors(
        gradient = listOf(
            lerp(safe, Color.Black, 0.42f),
            safe,
            lerp(safe, Color.White, 0.12f)
        ),
        title = Color.White,
        subtitle = Color.White.copy(alpha = 0.82f)
    )
}
