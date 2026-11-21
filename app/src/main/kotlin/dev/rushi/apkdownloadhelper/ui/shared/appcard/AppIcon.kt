package dev.rushi.apkdownloadhelper.ui.shared.appcard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.LocalMonochromeTheme
import dev.rushi.apkdownloadhelper.ui.theme.MonochromeThemeDefaults
import dev.rushi.apkdownloadhelper.ui.theme.SemanticTone
import dev.rushi.apkdownloadhelper.updater.ArchiveApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
private fun FindAppIconPlaceholder(
    gradient: List<Color>,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val monochrome = LocalMonochromeTheme.current
    val base = if (monochrome) scheme.surfaceContainerHighest else gradient.getOrElse(0) { scheme.primary }
    val mid = if (monochrome) scheme.surfaceContainerHighest else gradient.getOrElse(1) { base }
    val end = if (monochrome) scheme.surfaceContainerHigh else gradient.lastOrNull() ?: base
    val highlight = if (monochrome) scheme.onSurface.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.50f)
    val borderHighlight = if (monochrome) scheme.outline.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.55f)

    Box(
        modifier = modifier.drawWithCache {
            val corner = CornerRadius(16.dp.toPx())
            val glass = Brush.linearGradient(
                colors = listOf(
                    highlight,
                    base.copy(alpha = 0.22f),
                    end.copy(alpha = 0.20f)
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
            val border = Brush.linearGradient(
                colors = listOf(
                    borderHighlight,
                    mid.copy(alpha = 0.30f),
                    borderHighlight
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
            onDrawBehind {
                drawRoundRect(brush = glass, cornerRadius = corner)
                drawRoundRect(
                    brush = border,
                    cornerRadius = corner,
                    style = Stroke(1.dp.toPx())
                )
            }
        }
    )
}

@Composable
internal fun FindAppIcon(
    app: ArchiveApp,
    gradient: List<Color>,
    installed: Boolean,
    size: Dp = 56.dp,
    cornerRadius: Dp = 16.dp
) {
    val initial = app.name.firstOrNull()?.uppercaseChar() ?: '?'
    if (installed) {
        val icon = rememberInstalledAppIcon(app.packageName)
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(cornerRadius))
            )
        } else {
            AsyncAvatar(
                url = null,
                fallbackText = initial.toString(),
                size = size,
                cornerRadius = cornerRadius
            )
        }
        return
    }
    val archiveIconUrl = app.iconUrl?.takeIf { it.isNotBlank() }
    AsyncAvatar(
        url = archiveIconUrl ?: publicMetadataIconUrl(app.packageName),
        fallbackText = initial.toString(),
        size = size,
        cornerRadius = cornerRadius,
        fallbackGradient = gradient.takeIf { archiveIconUrl == null }
    )
}

@Composable
internal fun AsyncAvatar(
    url: String?,
    fallbackText: String,
    size: Dp = 20.dp,
    cornerRadius: Dp = 6.dp,
    fallbackGradient: List<Color>? = null
) {
    var bitmap by remember(url) { mutableStateOf(url?.let(archiveIconCache::get)) }
    LaunchedEffect(url) {
        val iconUrl = url
        if (iconUrl == null) {
            bitmap = null
            return@LaunchedEffect
        }
        archiveIconCache.get(iconUrl)?.let {
            bitmap = it
            return@LaunchedEffect
        }
        delay(120)
        bitmap = loadArchiveIcon(iconUrl) ?: return@LaunchedEffect
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius))
        )
    } else if (fallbackGradient != null) {
        FindAppIconPlaceholder(
            gradient = fallbackGradient,
            modifier = Modifier.size(size)
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius))
                .background(SemanticTone.Primary.container),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallbackText,
                color = SemanticTone.Primary.content,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
internal fun AppAvatar(packageName: String, initial: Char) {
    val context = LocalContext.current
    // Fetch the installed app's real icon; when the app isn't installed ask the
    // Play metadata service for its store icon; the Android default app icon is
    // the fallback for packages without a listing, and the letter tile the last.
    var iconBitmap by remember(packageName) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(packageName) {
        iconBitmap = withContext(Dispatchers.IO) {
            val installed = runCatching {
                context.packageManager.getApplicationIcon(packageName)
            }.getOrNull()?.let { drawable ->
                runCatching {
                    drawable.toBitmap(width = 176, height = 176).asImageBitmap()
                }.getOrNull()
            }
            if (installed != null) return@withContext installed

            loadArchiveIcon(publicMetadataIconUrl(packageName)) ?: runCatching {
                context.packageManager.getDefaultActivityIcon()
                    .toBitmap(width = 176, height = 176)
                    .asImageBitmap()
            }.getOrNull()
        }
    }

    if (iconBitmap != null) {
        Image(
            bitmap = iconBitmap!!,
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(AppDefaults.CompactCornerRadius))
        )
    } else {
        val colors = listOf(
            Color(0xFF1A73E8),
            Color(0xFF4C8DFF),
            Color(0xFFFBBC04),
            Color(0xFFEA4335),
            Color(0xFF4285F4),
            Color(0xFFF25C1B)
        )
        val color = colors[initial.code % colors.size]
        // Monochrome mode drops the per-app hue and takes the neutral accent, so
        // a list of these tiles does not read as the only colour left on screen.
        val tile = MonochromeThemeDefaults.accentColor(color)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(AppDefaults.CompactCornerRadius))
                .background(tile.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial.toString(),
                color = MonochromeThemeDefaults.iconTint(Color.White),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
