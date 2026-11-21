package dev.rushi.apkdownloadhelper.ui.shared.release

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

internal data class NoteGroup(
    val title: String,
    val entries: List<String>
)

private val HeadingPattern = Regex("""^#{1,6}\s*(.+)$""")

private val BulletPattern = Regex("""^(?:[-*+]\s+|\d+[.)]\s+)(.+)$""")

private val ImagePattern = Regex("""!\[([^\]]*)]\([^)]+\)""")

private val HtmlTagPattern = Regex("""<[^>]+>""")

internal fun String.changelogIcon(): ImageVector {
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

internal fun parseReleaseNotes(value: String): List<NoteGroup> {
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

internal fun cleanReleaseNotes(value: String): String =
    value.replace("\r\n", "\n")
        .replace(Regex("""(?m)^#{1,6}\s*"""), "")
        .replace(Regex("""\[([^\]]+)\]\([^)]+\)"""), "\$1")
        .replace("**", "")
        .replace("__", "")
        .replace("`", "")
        .replace(Regex("""(?m)^- """), "• ")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()
