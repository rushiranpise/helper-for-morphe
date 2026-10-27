package dev.rushi.apkdownloadhelper

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

internal const val PREFS_NAME = "helper_settings"

/** Dedicated prefs for the VirusTotal rate limiter's trailing call log. */
internal const val RATE_LIMITER_PREFS = "rate_limiter"
private const val KEY_CALL_TIMESTAMPS = "call_timestamps"

/**
 * Mirrors the user's Logcat preference so long-lived clients (built before
 * settings load) can check it per request without holding a Context.
 */
@Volatile
internal var logcatLoggingEnabled = true

/**
 * Mirrors the user's AdGuard DNS preference so OkHttp clients (built before
 * settings load) can route lookups per request without holding a Context.
 */
@Volatile
internal var adGuardDnsEnabled = false

internal data class HelperSettings(
    val downloadLocation: DownloadLocation = DownloadLocation.TEMPORARY,
    val networkPolicy: NetworkPolicy = NetworkPolicy.WIFI_AND_MOBILE,
    val deleteTemporaryAfterHandoff: Boolean = true,
    val logcatLogging: Boolean = true,
    val fastMode: Boolean = false,
    // Which version Fast Mode should auto-fetch when multiple are available.
    val fastModePolicy: FastModePolicy = FastModePolicy.REQUESTED,
    val disabledSources: Set<DownloadSource> = emptySet(),
    val themeMode: ThemeMode = ThemeMode.DARK,
    /** Where the palette comes from: the app's own, the wallpaper's, or neutral. */
    val themeStyle: ThemeStyle = ThemeStyle.MORPHE,
    /** Force pure black behind content in dark mode (OLED-friendly). */
    val pureBlackTheme: Boolean = false,
    /** Custom accent as `#RRGGBB`; empty keeps the style's own accent. */
    val customAccentColor: String = "",
    val adGuardDns: Boolean = false,
    val virusTotalEnabled: Boolean = false,
    val virusTotalApiKey: String = "",
    val virusTotalScanMode: VirusTotalScanMode = VirusTotalScanMode.ASK,
    /** Hand the file to Morphe without showing the result when a scan comes back clean. */
    val virusTotalAutoProceed: Boolean = false,
    // Source opened by default when Helper launches (null = first enabled source).
    val preferredSource: DownloadSource? = null
)

internal enum class VirusTotalScanMode(
    val title: String,
    val description: String
) {
    NEVER(
        title = "Never scan",
        description = "Skip VirusTotal scanning entirely."
    ),
    ASK(
        title = "Ask before scanning",
        description = "Prompt to scan each download before handing off."
    ),
    ALWAYS(
        title = "Always scan",
        description = "Automatically scan every download before handing off."
    )
}

internal enum class FastModePolicy(
    val title: String,
    val description: String
) {
    REQUESTED(
        title = "Requested version",
        description = "Find the exact requested version + version code across sources (current behaviour)."
    ),
    LATEST(
        title = "Latest version",
        description = "Fetch the newest version any source offers, even if it differs from the requested one."
    ),
    ALWAYS_ASK(
        title = "Always ask",
        description = "Ask once per request which version to fetch."
    )
}

internal enum class ThemeMode(
    val title: String,
    val description: String
) {
    SYSTEM(
        title = "System",
        description = "Follow the device's dark / light setting."
    ),
    DARK(
        title = "Dark",
        description = "Always use the dark theme."
    ),
    LIGHT(
        title = "Light",
        description = "Always use the light theme."
    )
}

/**
 * How the app derives its palette, mirroring the manager's own three styles.
 *
 * A custom accent belongs to [MORPHE] alone: the other two own their colours,
 * [MATERIAL_YOU] taking them from the wallpaper and [MONOCHROME] having none.
 */
internal enum class ThemeStyle(
    val title: String,
    val description: String
) {
    MORPHE(
        title = "Morphe",
        description = "The app's own blue palette, light or dark. Pick your own accent below."
    ),
    MATERIAL_YOU(
        title = "Material You",
        description = "Tint the whole app from your wallpaper (Android 12+)."
    ),
    MONOCHROME(
        title = "Monochrome",
        description = "Neutral greys with no colour accent."
    )
}

internal enum class DownloadLocation(
    val title: String,
    val description: String
) {
    TEMPORARY(
        title = "Store in cache",
        description = "Keep the file in Helper's cache and hand it off to Morphe  no visible copy is left behind."
    ),
    DOWNLOADS(
        title = "Store in downloads",
        description = "Save a visible copy in Downloads/Morphe Helper after the file checks out."
    )
}

internal enum class NetworkPolicy(
    val title: String,
    val description: String
) {
    WIFI_ONLY(
        title = "Wi-Fi only",
        description = "Use Wi-Fi for source checks and downloads."
    ),
    MOBILE_DATA_ONLY(
        title = "Mobile data only",
        description = "Use cellular data and pause when Wi-Fi is active."
    ),
    WIFI_AND_MOBILE(
        title = "Wi-Fi or mobile data",
        description = "Use whichever connection Android is already using."
    );

    fun blockReason(context: Context): String? {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
            ?: return "Network status is unavailable. Change Helper settings or try Manual mode."
        val activeNetwork = connectivity.activeNetwork
            ?: return "No active network is available. Connect to an allowed network or use Manual mode."
        val capabilities = connectivity.getNetworkCapabilities(activeNetwork)
            ?: return "Network status is unavailable. Change Helper settings or try Manual mode."
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return "The active network has no internet access. Connect to another network or use Manual mode."
        }

        return when (this) {
            WIFI_ONLY -> if (connectivity.isActiveNetworkMetered) {
                "Helper is set to Wi-Fi only. Connect to Wi-Fi or change Helper settings."
            } else {
                null
            }
            MOBILE_DATA_ONLY -> if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                "Helper is set to mobile data only. Switch to mobile data or change Helper settings."
            } else {
                null
            }
            WIFI_AND_MOBILE -> null
        }
    }
}

internal fun Context.loadHelperSettings(): HelperSettings {
    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val settings = HelperSettings(
        downloadLocation = enumValueOrDefault(
            prefs.getString("download_location", null),
            DownloadLocation.TEMPORARY
        ),
        networkPolicy = enumValueOrDefault(
            prefs.getString("network_policy", null),
            NetworkPolicy.WIFI_AND_MOBILE
        ),
        deleteTemporaryAfterHandoff = prefs.getBoolean("delete_temporary_after_handoff", true),
        logcatLogging = prefs.getBoolean("logcat_logging", true),
        fastMode = prefs.getBoolean("fast_mode", false),
        fastModePolicy = enumValueOrDefault(
            prefs.getString("fast_mode_policy", null),
            FastModePolicy.REQUESTED
        ),
        disabledSources = prefs.getStringSet("disabled_sources", emptySet())
            .orEmpty()
            .mapNotNull { name -> DownloadSource.entries.firstOrNull { it.name == name } }
            .toSet(),
        themeMode = enumValueOrDefault(
            prefs.getString("theme_mode", null),
            ThemeMode.DARK
        ),
        themeStyle = themeStyle(prefs),
        pureBlackTheme = prefs.getBoolean("pure_black_theme", false),
        customAccentColor = prefs.getString("custom_accent_color", "") ?: "",
        adGuardDns = prefs.getBoolean("adguard_dns", false),
        virusTotalEnabled = prefs.getBoolean("virus_total_enabled", false),
        virusTotalApiKey = prefs.getString("virus_total_api_key", "") ?: "",
        virusTotalAutoProceed = prefs.getBoolean("virus_total_auto_proceed", false),
        virusTotalScanMode = enumValueOrDefault(
            prefs.getString("virus_total_scan_mode", null),
            VirusTotalScanMode.ASK
        ),
        preferredSource = prefs.getString("preferred_source", null)
            ?.let { name -> DownloadSource.entries.firstOrNull { it.name == name } }
    )
    logcatLoggingEnabled = settings.logcatLogging
    adGuardDnsEnabled = settings.adGuardDns
    return settings
}

internal fun Context.saveHelperSettings(settings: HelperSettings) {
    logcatLoggingEnabled = settings.logcatLogging
    adGuardDnsEnabled = settings.adGuardDns
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString("download_location", settings.downloadLocation.name)
        .putString("network_policy", settings.networkPolicy.name)
        .putBoolean("delete_temporary_after_handoff", settings.deleteTemporaryAfterHandoff)
        .putBoolean("logcat_logging", settings.logcatLogging)
        .putBoolean("fast_mode", settings.fastMode)
        .putString("fast_mode_policy", settings.fastModePolicy.name)
        .putStringSet("disabled_sources", settings.disabledSources.map { it.name }.toSet())
        .putString("theme_mode", settings.themeMode.name)
        .putString("theme_style", settings.themeStyle.name)
        .putBoolean("pure_black_theme", settings.pureBlackTheme)
        .putString("custom_accent_color", settings.customAccentColor)
        .putBoolean("adguard_dns", settings.adGuardDns)
        .putBoolean("virus_total_enabled", settings.virusTotalEnabled)
        .putString("virus_total_api_key", settings.virusTotalApiKey)
        .putString("virus_total_scan_mode", settings.virusTotalScanMode.name)
        .putBoolean("virus_total_auto_proceed", settings.virusTotalAutoProceed)
        .putString("preferred_source", settings.preferredSource?.name)
        .apply()
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(name: String?, fallback: T): T =
    name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback

/**
 * The stored colour style, or one derived from the retired `dynamic_colors`
 * toggle so installs that predate the style selector keep the look they had.
 */
private fun themeStyle(prefs: android.content.SharedPreferences): ThemeStyle =
    prefs.getString("theme_style", null)
        ?.let { name -> ThemeStyle.entries.firstOrNull { it.name == name } }
        ?: if (prefs.getBoolean("dynamic_colors", false)) {
            ThemeStyle.MATERIAL_YOU
        } else {
            ThemeStyle.MORPHE
        }

/**
 * Rehydrate the VirusTotal rate limiter's per-minute call log from storage so
 * the "N of 4" bar is accurate after a process restart, and install the sink
 * that persists each future call. Call once at process start from any component
 * with a Context.
 */
internal fun Context.restoreAndPersistRateLimiter() {
    val prefs = getSharedPreferences(RATE_LIMITER_PREFS, Context.MODE_PRIVATE)
    val saved = prefs.getString(KEY_CALL_TIMESTAMPS, null)
        ?.split(',')
        ?.mapNotNull { it.trim().toLongOrNull() }
        .orEmpty()
    VirusTotalScanner.rateLimiter.restoreCallTimestamps(saved)
    VirusTotalScanner.rateLimiter.onCallRecorded = { timestamps ->
        getSharedPreferences(RATE_LIMITER_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CALL_TIMESTAMPS, timestamps.joinToString(","))
            .apply()
    }
}

internal fun Context.temporaryDownloadsDir(): File = File(cacheDir, "downloads")

internal fun Context.temporaryDownloadsSize(): Long =
    temporaryDownloadsDir()
        .listFiles()
        ?.filter { it.isFile }
        ?.sumOf { it.length() }
        ?: 0L

/** Deletes every temporary hand-off file and returns the freed bytes. */
internal fun Context.clearTemporaryDownloads(): Long {
    val dir = temporaryDownloadsDir()
    val files = dir.listFiles()?.filter { it.isFile }.orEmpty()
    val freed = files.sumOf { it.length() }
    files.forEach { file -> runCatching { file.delete() } }
    return freed
}

/** Path of the visible copy folder inside Downloads. */
internal fun downloadsCopyRelativePath(): String =
    "${Environment.DIRECTORY_DOWNLOADS}/Morphe Helper"

/** Total size of the visible copies saved to Downloads. */
internal fun Context.downloadsCopySize(): Long {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        var total = 0L
        contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Downloads.SIZE),
            "${MediaStore.Downloads.RELATIVE_PATH} LIKE ?",
            arrayOf("${downloadsCopyRelativePath()}/%"),
            null
        )?.use { cursor ->
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
            while (cursor.moveToNext()) {
                total += cursor.getLong(sizeIndex)
            }
        }
        return total
    }
    val dir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "Morphe Helper"
    )
    return dir.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
}

/** Deletes every visible copy in Downloads and returns the freed bytes. */
internal fun Context.clearDownloadsCopies(): Long {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        var freed = 0L
        contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Downloads._ID),
            "${MediaStore.Downloads.RELATIVE_PATH} LIKE ?",
            arrayOf("${downloadsCopyRelativePath()}/%"),
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                freed += runCatching {
                    contentResolver.delete(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        "${MediaStore.Downloads._ID}=?",
                        arrayOf(id.toString())
                    )
                }.getOrDefault(0)
            }
        }
        return freed
    }
    val dir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "Morphe Helper"
    )
    val files = dir.listFiles()?.filter { it.isFile }.orEmpty()
    val freed = files.sumOf { it.length() }
    files.forEach { file -> runCatching { file.delete() } }
    return freed
}

/**
 * Cleans up handed-off and stale temporary files when auto-clear is enabled,
 * returning the freed bytes (0 if nothing was removed).
 *
 * Two sources of leftovers are handled:
 *  1. Handed-off files whose in-process deleter died with the process (they were
 *     recorded in `pending_temp_deletes` at hand-off time).
 *  2. Older handed-off files that predate that record: once past the same grace
 *     period, anything in the temp dir is dropped unless it is the file the
 *     pending hand-off result still points at (Morphe can re-request it) or an
 *     in-progress download's `.part` staging file.
 */
internal fun Context.cleanupTemporaryDownloads(settings: HelperSettings): Long {
    if (!settings.deleteTemporaryAfterHandoff) return 0L
    var freed = 0L
    val now = System.currentTimeMillis()
    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val deleteCutoff = now - TEMP_CLEANUP_DELAY_MS

    // The file the pending hand-off result still points at must survive.
    val pendingFile = DownloadJobManager.readPendingResult(this)
        ?.fileName
        ?.let { name -> File(temporaryDownloadsDir(), name).canonicalPath }

    // 1) Recorded handed-off deletions the in-process deleter never ran.
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

    // 2) Handed-off leftovers with no record: past the grace period, drop every
    //    temp file that is not the pending re-request target or an in-flight
    //    download's staging file.
    temporaryDownloadsDir()
        .listFiles()
        ?.filter {
            it.isFile &&
                it.lastModified() < deleteCutoff &&
                it.name.endsWith(".part").not() &&
                it.canonicalPath != pendingFile
        }
        ?.forEach { file ->
            freed += file.length()
            runCatching { file.delete() }
        }

    return freed
}
