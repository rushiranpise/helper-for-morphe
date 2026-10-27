package dev.rushi.apkdownloadhelper

import java.io.File
import java.util.Locale
import java.util.zip.ZipFile

/**
 * OBB (expansion/game data) files carried inside a downloaded container.
 *
 * Android serves these only from `Android/obb/<package>/`, a directory no other app
 * may write on Android 11 and later, so the helper cannot install them for the user.
 * It can still say that the file it just handed over carries them, which is the
 * difference between a patched game that starts and one that closes on launch with
 * no explanation.
 */
internal data class ObbContents(
    val fileCount: Int,
    val totalBytes: Long
) {
    /** "2 files · 1.2 GB", for a single line of UI. */
    val summary: String
        get() = buildString {
            append(fileCount)
            append(if (fileCount == 1) " file" else " files")
            if (totalBytes > 0L) {
                append(" · ")
                append(readableSize(totalBytes))
            }
        }
}

private fun readableSize(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val units = listOf("KB", "MB", "GB")
    var value = bytes.toDouble() / 1024.0
    var unit = 0
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    return if (value >= 10.0) {
        "${value.toInt()} ${units[unit]}"
    } else {
        "${String.format(Locale.US, "%.1f", value)} ${units[unit]}"
    }
}

/**
 * The `.obb` entries inside [file], or null when it is a plain APK, carries none, or
 * cannot be read as a container. Never throws: a missing answer is not an error here.
 */
internal fun obbContentsIn(file: File): ObbContents? = runCatching {
    if (!file.isFile) return null
    ZipFile(file).use { zip ->
        var count = 0
        var bytes = 0L
        for (entry in zip.entries()) {
            if (entry.isDirectory) continue
            if (!entry.name.lowercase(Locale.US).endsWith(".obb")) continue
            count++
            // A zip entry's size can be unknown (-1) for streamed archives.
            bytes += entry.size.coerceAtLeast(0L)
        }
        if (count == 0) null else ObbContents(fileCount = count, totalBytes = bytes)
    }
}.getOrNull()
