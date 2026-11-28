package dev.rushi.apkdownloadhelper.storage

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dev.rushi.apkdownloadhelper.download.VirusTotalScanner
import dev.rushi.apkdownloadhelper.ui.model.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.model.HelperRequest
import dev.rushi.apkdownloadhelper.ui.screen.helper.inspectArchiveFile
import java.io.File
import java.lang.reflect.Type

/** Outcome of the VirusTotal scan that ran before a hand-off, if any. */
internal data class ScanVerdict(
    val label: String,
    val malicious: Boolean = false,
    val failed: Boolean = false,
    val sha256: String? = null,
    val scannedFiles: Int? = null,
    /** The full result, so the scan card can be reopened from history. */
    val result: VirusTotalScanner.ScanResult? = null
)

internal data class DownloadHistoryEntry(
    val timestamp: Long,
    val appName: String,
    val packageName: String,
    val versionName: String?,
    val sourceName: String,
    val fileName: String,
    val fileKind: String,
    val uri: String,
    val scanVerdict: ScanVerdict? = null,
    /** Expansion data found inside the handed-off container, when it carried any. */
    val obbContents: ObbContents? = null,
    /** Everything the details page needs, saved once at download time. */
    val details: StoredDetails? = null
)

internal data class StoredDetails(
    val packageName: String? = null,
    val appName: String? = null,
    val versionName: String? = null,
    val versionCode: Long? = null,
    val minSdkVersion: Int? = null,
    val abis: List<String>? = null,
    val sizeBytes: Long = 0L
)

internal object DownloadHistoryStore {
    private const val PREFS_HISTORY = "download_history"
    private const val KEY_ENTRIES = "entries"
    private const val MAX_ENTRIES = 50
    private val gson = GsonBuilder()
        .registerTypeAdapter(
            VirusTotalScanner.ScanResult::class.java,
            VirusTotalScanner.ScanResultTypeAdapter()
        )
        .create()
    private val entriesType: Type = object : TypeToken<List<DownloadHistoryEntry>>() {}.type
    private val mutationLock = Any()

    /** Incremented whenever entries are added, removed, or cleared so UIs stay in sync. */
    var revision by mutableIntStateOf(0)
        private set

    fun notifyChanged() {
        revision++
    }

    fun entries(context: Context): List<DownloadHistoryEntry> {
        val raw = context.getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE)
            .getString(KEY_ENTRIES, null)
            ?: return emptyList()
        return runCatching {
            gson.fromJson<List<DownloadHistoryEntry>>(raw, entriesType).orEmpty()
        }.getOrDefault(emptyList())
    }

    fun add(context: Context, entry: DownloadHistoryEntry) {
        synchronized(mutationLock) {
            val updated = (listOf(entry) + entries(context)).take(MAX_ENTRIES)
            context.getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ENTRIES, gson.toJson(updated))
                .apply()
        }
        revision++
    }

    fun removeByUris(context: Context, uris: Set<String>, fileNames: Set<String> = emptySet()) {
        if (uris.isEmpty() && fileNames.isEmpty()) return
        synchronized(mutationLock) {
            val updated = entries(context).filterNot {
                it.uri in uris || (fileNames.isNotEmpty() && it.fileName in fileNames)
            }
            context.getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ENTRIES, gson.toJson(updated))
                .apply()
        }
        revision++
    }

    fun clear(context: Context) {
        synchronized(mutationLock) {
            context.getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE).edit().clear().apply()
        }
        revision++
    }
}

internal fun Context.recordHandOff(
    request: HelperRequest,
    candidate: DownloadCandidate,
    file: File,
    uri: Uri,
    scanVerdict: ScanVerdict? = null
) {
    val obb = obbContentsIn(file)
    val details = runCatching {
        val d = inspectArchiveFile(this, file)
        if (d.packageName.isNullOrBlank() && d.appName.isNullOrBlank() &&
            d.versionName.isNullOrBlank() && d.versionCode == null &&
            d.minSdkVersion == null && d.abis.isEmpty()
        ) {
            null
        } else {
            StoredDetails(
                packageName = d.packageName,
                appName = d.appName,
                versionName = d.versionName,
                versionCode = d.versionCode,
                minSdkVersion = d.minSdkVersion,
                abis = d.abis,
                sizeBytes = file.length()
            )
        }
    }.getOrNull()
    if (obb != null) {
        // Worth a log line of its own: a game that will not start is otherwise
        // indistinguishable from a bad patch, when the usual cause is this data
        // never reaching Android/obb.
        AppLog.record(
            LogLevel.Info,
            "${file.name} includes game data (${obb.summary}). Android loads it from " +
                "Android/obb/${candidate.packageName}, which this app cannot write."
        )
    }
    DownloadHistoryStore.add(
        this,
        DownloadHistoryEntry(
            timestamp = System.currentTimeMillis(),
            appName = candidate.name.ifBlank { request.appName },
            packageName = candidate.packageName,
            versionName = candidate.versionName,
            sourceName = candidate.source.label,
            fileName = file.name,
            fileKind = candidate.fileKind,
            uri = uri.toString(),
            scanVerdict = scanVerdict,
            obbContents = obb,
            details = details
        )
    )
}

