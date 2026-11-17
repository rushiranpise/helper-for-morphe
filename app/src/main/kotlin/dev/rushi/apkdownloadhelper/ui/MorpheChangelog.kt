package dev.rushi.apkdownloadhelper.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private data class NoteGroup(
    val title: String,
    val entries: List<String>
)

private val HeadingPattern = Regex("""^#{1,6}\s*(.+)$""")
private val BulletPattern = Regex("""^(?:[-*+]\s+|\d+[.)]\s+)(.+)$""")
private val ImagePattern = Regex("""!\[([^\]]*)]\([^)]+\)""")
private val HtmlTagPattern = Regex("""<[^>]+>""")

/**
 * Release notes as the manager lays them out: a node on a thin rail, the release version, then
 * each group of changes in its own card with a matching icon. Markdown headings, bullets, bold
 * text, inline code and links are understood rather than shown as punctuation.
 */
@Composable
internal fun MorpheReleaseNotes(
    version: String,
    notes: String,
    prerelease: Boolean
) {
    val groups = remember(notes) { parseReleaseNotes(notes) }
    val colors = MaterialTheme.colorScheme
    val railColor = colors.outlineVariant.copy(alpha = 0.55f)
    val nodeColor = colors.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(MorpheDefaults.ContentPadding)
    ) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight()
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val x = 11.dp.toPx()
                drawLine(
                    color = railColor,
                    start = Offset(x, 16.dp.toPx()),
                    end = Offset(x, size.height),
                    strokeWidth = 2.dp.toPx()
                )
                drawCircle(
                    color = nodeColor.copy(alpha = 0.18f),
                    radius = 10.dp.toPx(),
                    center = Offset(x, 16.dp.toPx())
                )
                drawCircle(
                    color = nodeColor,
                    radius = 5.dp.toPx(),
                    center = Offset(x, 16.dp.toPx())
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "v$version",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val badgeColor = if (prerelease) {
                    colors.tertiaryContainer
                } else {
                    colors.primaryContainer
                }
                val badgeContent = if (prerelease) {
                    colors.onTertiaryContainer
                } else {
                    colors.onPrimaryContainer
                }
                Surface(
                    shape = MorpheDefaults.PillShape,
                    color = badgeColor,
                    contentColor = badgeContent
                ) {
                    Text(
                        text = if (prerelease) "BETA" else "STABLE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(
                text = "Release notes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (groups.isEmpty()) {
                Text(
                    text = "No release notes for this version.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                groups.forEach { group ->
                    ReleaseNoteGroup(group)
                }
            }
        }
    }
}

@Composable
private fun ReleaseNoteGroup(group: NoteGroup) {
    val colors = MaterialTheme.colorScheme
    val icon = group.title.changelogIcon()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MorpheDefaults.CompactCornerRadius),
        color = colors.surfaceColorAtElevation(2.dp),
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = colors.primary.copy(alpha = 0.14f),
                    contentColor = colors.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
            }

            group.entries.forEach { entry ->
                HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .size(5.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = colors.primary.copy(alpha = 0.75f))
                        }
                    }
                    Text(
                        text = inlineNote(entry),
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Builds one note's bold, inline-code and link spans, leaving the rest as written. */
@Composable
private fun inlineNote(text: String): AnnotatedString {
    val colors = MaterialTheme.colorScheme
    return remember(text, colors.primary, colors.surfaceVariant) {
        buildAnnotatedString {
            var index = 0
            while (index < text.length) {
                when {
                    text.startsWith("**", index) -> {
                        val end = text.indexOf("**", index + 2)
                        if (end < 0) {
                            append(text.substring(index))
                            index = text.length
                        } else {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(text.substring(index + 2, end))
                            }
                            index = end + 2
                        }
                    }

                    text[index] == '`' -> {
                        val end = text.indexOf('`', index + 1)
                        if (end < 0) {
                            append(text[index])
                            index++
                        } else {
                            withStyle(
                                SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    background = colors.surfaceVariant,
                                    fontSize = 13.sp
                                )
                            ) {
                                append(" ${text.substring(index + 1, end)} ")
                            }
                            index = end + 1
                        }
                    }

                    text.startsWith("[", index) -> {
                        val close = text.indexOf(']', index + 1)
                        val openUrl = if (close >= 0) text.indexOf('(', close + 1) else -1
                        val end = if (openUrl >= 0) text.indexOf(')', openUrl + 1) else -1
                        if (close < 0 || openUrl != close + 1 || end < 0) {
                            append(text[index])
                            index++
                        } else {
                            val url = text.substring(openUrl + 1, end)
                            withLink(
                                LinkAnnotation.Url(
                                    url = url,
                                    styles = TextLinkStyles(
                                        style = SpanStyle(
                                            color = colors.primary,
                                            textDecoration = TextDecoration.Underline
                                        )
                                    )
                                )
                            ) {
                                append(text.substring(index + 1, close))
                            }
                            index = end + 1
                        }
                    }

                    else -> {
                        append(text[index])
                        index++
                    }
                }
            }
        }
    }
}

private fun String.changelogIcon(): ImageVector {
    val lower = lowercase(Locale.ROOT)
    return when {
        listOf("fix", "bug", "crash").any { it in lower } -> Icons.Outlined.BugReport
        listOf("security", "vulnerab").any { it in lower } -> Icons.Outlined.Security
        listOf("breaking", "deprecat").any { it in lower } -> Icons.Outlined.Warning
        listOf("feature", "new", "add").any { it in lower } -> Icons.Outlined.AutoAwesome
        listOf("improve", "change", "update", "performance").any { it in lower } ->
            Icons.Outlined.Tune
        listOf("note", "other").any { it in lower } -> Icons.Outlined.Info
        else -> Icons.Outlined.Star
    }
}

/**
 * Turns the handful of Markdown shapes GitHub releases use into sections of notes. A heading opens
 * a section, bullets become its entries, and loose paragraphs become entries of their own so no
 * text is lost.
 */
/** The trailing GitHub footer, plain or bold, with or without its link. */
private fun String.isFullChangelogFooter(): Boolean {
    val stripped = trim().trim('*').trim()
    return stripped.startsWith("Full Changelog", ignoreCase = true) ||
        stripped.startsWith("[Full Changelog]", ignoreCase = true)
}

private fun parseReleaseNotes(value: String): List<NoteGroup> {
    val groups = mutableListOf<MutableList<String>>()
    val titles = mutableListOf<String?>()
    var current = -1
    val paragraph = StringBuilder()

    fun startGroup(title: String?) {
        groups.add(mutableListOf())
        titles.add(title)
        current = groups.lastIndex
    }

    fun flushParagraph() {
        val text = paragraph.toString().trim()
        paragraph.clear()
        if (text.isEmpty()) return
        if (current < 0) startGroup(null)
        groups[current] += text
    }

    value.replace("\r\n", "\n").lines().forEach { raw ->
        val line = raw.trim()
        when {
            line.isBlank() -> flushParagraph()
            line.startsWith("<!--") || line.startsWith("![") && line.endsWith(")") -> Unit
            line == "---" || line.isFullChangelogFooter() -> Unit
            HeadingPattern.matches(line) -> {
                flushParagraph()
                val heading = HeadingPattern.matchEntire(line)?.groupValues?.get(1)
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                startGroup(
                    if (heading == null || heading.equals("What's Changed", ignoreCase = true)) {
                        "Changes"
                    } else {
                        heading
                    }
                )
            }
            BulletPattern.matches(line) -> {
                flushParagraph()
                val bullet = BulletPattern.matchEntire(line)?.groupValues?.get(1)
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() } ?: return@forEach
                if (current < 0) startGroup("Changes")
                groups[current] += bullet
            }
            else -> {
                if (paragraph.isNotEmpty()) paragraph.append(' ')
                paragraph.append(
                    ImagePattern.replace(line, "$1")
                        .replace(HtmlTagPattern, "")
                )
            }
        }
    }
    flushParagraph()

    return groups.mapIndexedNotNull { index, entries ->
        val cleaned = entries
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (cleaned.isEmpty()) {
            null
        } else {
            NoteGroup(title = titles[index] ?: "Changes", entries = cleaned)
        }
    }
}
