package dev.rushi.apkdownloadhelper.storage

import dev.rushi.apkdownloadhelper.*
import dev.rushi.apkdownloadhelper.ui.*
import dev.rushi.apkdownloadhelper.sources.*
import dev.rushi.apkdownloadhelper.play.*
import dev.rushi.apkdownloadhelper.download.*
import dev.rushi.apkdownloadhelper.storage.*
import dev.rushi.apkdownloadhelper.updater.*
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import java.io.File
import kotlinx.coroutines.CancellationException

/** Where a completed download was handed off and whether a saved copy exists. */
internal data class DownloadHandoff(
    val uri: Uri,
    val savedCopy: Boolean
)

/**
 * Picks the hand-off URI and saves the configured visible copy before Morphe
 * receives the result. If saving fails, the cache copy is handed off instead.
 */
internal fun Context.prepareDownloadHandoff(
    file: File,
    settings: HelperSettings
): DownloadHandoff {
    val savedUri = if (settings.downloadRetention == DownloadRetention.AFTER_HANDOFF) {
        null
    } else {
        when (settings.downloadLocation) {
            DownloadLocation.TEMPORARY -> null
            DownloadLocation.EXTERNAL -> storageResult { copyToExternalDownloads(file) }
                .onFailure { logStorageFailure("external storage", it) }
                .getOrNull()
            DownloadLocation.DOWNLOADS -> storageResult { copyToDownloads(file) }
                .onFailure { logStorageFailure("Downloads", it) }
                .getOrNull()
            DownloadLocation.CUSTOM -> storageResult {
                copyToCustomFolder(file, settings.downloadCustomTreeUri)
            }
                .onFailure { logStorageFailure("the custom folder", it) }
                .getOrNull()
        }
    }

    return if (savedUri != null) {
        DownloadHandoff(savedUri, savedCopy = true)
    } else {
        cacheHandoff(file)
    }
}

internal fun Context.cacheHandoff(file: File): DownloadHandoff =
    DownloadHandoff(
        FileProvider.getUriForFile(
            this,
            "${BuildConfig.APPLICATION_ID}.files",
            file
        ),
        savedCopy = false
    )

/** A saved copy can be read by Morphe, so the cache duplicate is safe to remove. */
internal fun Context.scheduleHandoffCleanup(
    file: File,
    handoff: DownloadHandoff,
    settings: HelperSettings
) {
    if (handoff.savedCopy || settings.downloadRetention == DownloadRetention.AFTER_HANDOFF) {
        scheduleTemporaryDelete(file)
    }
}

private inline fun <T> storageResult(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }

private fun logStorageFailure(target: String, error: Throwable) {
    AppLog.record(
        LogLevel.Warning,
        "Could not save the download to $target: " +
            "${error.message ?: "unknown error"}. Handing off the cache copy."
    )
}

internal fun Context.externalDownloadsDir(): File? =
    getExternalFilesDir(null)?.let { File(it, "downloads") }

internal fun Context.externalDownloadsSize(): Long =
    externalDownloadsDir()
        ?.listFiles()
        ?.filter { it.isFile }
        ?.sumOf { it.length() }
        ?: 0L

internal fun Context.copyToExternalDownloads(file: File): Uri {
    val dir = externalDownloadsDir()?.apply { mkdirs() }
        ?: error("External storage is not available")
    val target = dir.uniqueChild(file.name)
    try {
        file.copyTo(target, overwrite = false)
    } catch (error: Throwable) {
        runCatching { target.delete() }
        throw error
    }
    return FileProvider.getUriForFile(
        this,
        "${BuildConfig.APPLICATION_ID}.files",
        target
    )
}

internal fun Context.clearExternalDownloads(): Long {
    val files = externalDownloadsDir()
        ?.listFiles()
        ?.filter { it.isFile }
        .orEmpty()
    val freed = files.sumOf { it.length() }
    files.forEach { file -> runCatching { file.delete() } }
    return freed
}

private const val KEY_CUSTOM_DOWNLOAD_URIS = "custom_download_uris"

private fun Context.customDownloadUris(): Set<String> =
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getStringSet(KEY_CUSTOM_DOWNLOAD_URIS, emptySet())
        .orEmpty()

private fun Context.saveCustomDownloadUris(uris: Set<String>) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putStringSet(KEY_CUSTOM_DOWNLOAD_URIS, uris)
        .apply()
}

internal fun Context.trackCustomDownload(uri: Uri) {
    saveCustomDownloadUris(customDownloadUris() + uri.toString())
}

internal fun Context.untrackCustomDownload(uri: Uri) {
    saveCustomDownloadUris(customDownloadUris() - uri.toString())
}

internal fun customFolderName(context: Context, treeUri: String): String? =
    treeUri.takeIf { it.isNotBlank() }
        ?.let {
            runCatching {
                DocumentFile.fromTreeUri(context, Uri.parse(it))?.name
            }.getOrNull()
        }
        ?.takeIf { it.isNotBlank() }

internal fun Context.copyToCustomFolder(file: File, treeUri: String): Uri {
    if (treeUri.isBlank()) error("No custom download folder is selected")
    val root = DocumentFile.fromTreeUri(this, Uri.parse(treeUri))
        ?: error("The custom download folder is not available")
    if (!root.canWrite()) error("The custom download folder is not writable")
    val target = root.createFile(file.mimeType(), file.name)
        ?: error("Could not create the file in the custom folder")

    trackCustomDownload(target.uri)
    try {
        contentResolver.openOutputStream(target.uri)?.use { output ->
            file.inputStream().use { input -> input.copyTo(output) }
        } ?: error("Could not open the file in the custom folder")
    } catch (error: Throwable) {
        untrackCustomDownload(target.uri)
        runCatching { target.delete() }
        throw error
    }
    return target.uri
}

internal fun Context.customDownloadsSize(): Long {
    var total = 0L
    for (raw in customDownloadUris()) {
        val document = runCatching {
            DocumentFile.fromSingleUri(this, Uri.parse(raw))
        }.getOrNull() ?: continue
        if (runCatching { document.isFile && document.exists() }.getOrDefault(false)) {
            total += runCatching { document.length() }.getOrDefault(0L).coerceAtLeast(0L)
        }
    }
    return total
}

internal fun Context.clearCustomDownloads(): Long {
    var freed = 0L
    val remaining = customDownloadUris().toMutableSet()
    for (raw in remaining.toList()) {
        val document = runCatching {
            DocumentFile.fromSingleUri(this, Uri.parse(raw))
        }.getOrNull()
        if (document == null ||
            !runCatching { document.isFile && document.exists() }.getOrDefault(false)
        ) {
            remaining.remove(raw)
            continue
        }
        val size = runCatching { document.length() }.getOrDefault(0L).coerceAtLeast(0L)
        if (runCatching { document.delete() }.getOrDefault(false)) {
            freed += size
            remaining.remove(raw)
        }
    }
    saveCustomDownloadUris(remaining)
    return freed
}

internal fun Context.cleanupCustomDownloads(cutoff: Long): Long {
    var freed = 0L
    val remaining = customDownloadUris().toMutableSet()
    for (raw in remaining.toList()) {
        val document = runCatching {
            DocumentFile.fromSingleUri(this, Uri.parse(raw))
        }.getOrNull()
        if (document == null ||
            !runCatching { document.isFile && document.exists() }.getOrDefault(false)
        ) {
            remaining.remove(raw)
            continue
        }
        val modified = runCatching { document.lastModified() }.getOrDefault(0L)
        val shouldDelete = cutoff == 0L || (modified > 0L && modified < cutoff)
        if (!shouldDelete) continue
        val size = runCatching { document.length() }.getOrDefault(0L).coerceAtLeast(0L)
        if (runCatching { document.delete() }.getOrDefault(false)) {
            freed += size
            remaining.remove(raw)
        }
    }
    saveCustomDownloadUris(remaining)
    return freed
}

/** Deletes public Downloads copies older than [cutoff], or every copy when it is 0. */
internal fun Context.cleanupDownloadsCopies(cutoff: Long): Long {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var freed = 0L
            val selection = buildString {
                append("${MediaStore.Downloads.RELATIVE_PATH} LIKE ?")
                if (cutoff > 0L) {
                    append(" AND ${MediaStore.MediaColumns.DATE_MODIFIED} < ?")
                }
            }
            val args = buildList {
                add("${downloadsCopyRelativePath()}/%")
                if (cutoff > 0L) add((cutoff / 1000L).toString())
            }.toTypedArray()
            contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.SIZE),
                selection,
                args,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val size = cursor.getLong(sizeIndex)
                    val deleted = runCatching {
                        contentResolver.delete(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            "${MediaStore.Downloads._ID}=?",
                            arrayOf(id.toString())
                        )
                    }.getOrDefault(0)
                    if (deleted > 0) freed += size.coerceAtLeast(0L)
                }
            }
            freed
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Morphe Helper"
            )
            val files = dir.listFiles()
                ?.filter { it.isFile && (cutoff <= 0L || it.lastModified() < cutoff) }
                .orEmpty()
            val freed = files.sumOf { it.length() }
            files.forEach { file -> runCatching { file.delete() } }
            freed
        }
    }.getOrDefault(0L)
}

private fun Context.cleanupCacheFiles(cutoff: Long): Long {
    val files = temporaryDownloadsDir()
        .listFiles()
        ?.filter {
            it.isFile &&
                it.name.endsWith(".part").not() &&
                it.lastModified() < cutoff
        }
        .orEmpty()
    val freed = files.sumOf { it.length() }
    files.forEach { file -> runCatching { file.delete() } }
    return freed
}

private fun Context.cleanupExternalDownloads(cutoff: Long): Long {
    val files = externalDownloadsDir()
        ?.listFiles()
        ?.filter { it.isFile && it.lastModified() < cutoff }
        .orEmpty()
    val freed = files.sumOf { it.length() }
    files.forEach { file -> runCatching { file.delete() } }
    return freed
}

/** Deletes every managed copy in cache, external storage, Downloads and the custom folder. */
internal fun Context.clearAllDownloads(): Long =
    clearTemporaryDownloads() +
        clearExternalDownloads() +
        clearDownloadsCopies() +
        clearCustomDownloads()

/**
 * Applies the configured retention to cache, external, public and custom
 * copies. Pending temporary deletions are always completed first, even when
 * retention has since been switched back to Keep.
 */
internal fun Context.cleanupDownloads(settings: HelperSettings): Long {
    var freed = 0L
    val now = System.currentTimeMillis()
    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val deleteCutoff = now - TEMP_CLEANUP_DELAY_MS

    val pending = prefs.getStringSet("pending_temp_deletes", emptySet()).orEmpty()
    if (pending.isNotEmpty()) {
        val remaining = pending.toMutableSet()
        pending.forEach { entry ->
            val path = entry.substringBeforeLast('|')
            val recordedAt = entry.substringAfterLast('|').toLongOrNull() ?: 0L
            val file = File(path)
            when {
                !file.exists() -> remaining.remove(entry)
                now - recordedAt >= TEMP_CLEANUP_DELAY_MS -> {
                    freed += file.length()
                    runCatching { file.delete() }
                    remaining.remove(entry)
                }
            }
        }
        prefs.edit().putStringSet("pending_temp_deletes", remaining).apply()
    }

    val retention = settings.downloadRetention
    if (retention == DownloadRetention.KEEP) return freed

    if (retention == DownloadRetention.AFTER_HANDOFF) {
        freed += cleanupCacheFiles(deleteCutoff)
        freed += cleanupExternalDownloads(deleteCutoff)
        freed += cleanupDownloadsCopies(0L)
        freed += cleanupCustomDownloads(0L)
        return freed
    }

    val cutoff = now - retention.durationMillis
    freed += cleanupCacheFiles(cutoff)
    freed += cleanupExternalDownloads(cutoff)
    freed += cleanupDownloadsCopies(cutoff)
    freed += cleanupCustomDownloads(cutoff)
    return freed
}

internal fun downloadLocationLabel(context: Context, settings: HelperSettings): String =
    when (settings.downloadLocation) {
        DownloadLocation.CUSTOM -> customFolderName(context, settings.downloadCustomTreeUri)
            ?.let { "Custom: $it" }
            ?: "Custom folder"
        else -> settings.downloadLocation.title
    }

