package dev.rushi.apkdownloadhelper.storage

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dev.rushi.apkdownloadhelper.BuildConfig
import dev.rushi.apkdownloadhelper.download.VirusTotalScanner
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.updater.UpdateInfo
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
internal var adGuardDnsEnabled = true

/**
 * Sources that start switched off.
 *
 * The six the helper is built around (APKMirror, Uptodown, APKPure, APKCube,
 * Morphe Link and Aurora) are enabled; the rest are theirs to discover in Settings.
 * Like the Fast Mode default, this only decides what somebody who never touched the
 * source toggles gets - a stored set is used as it is.
 */
/**
 * Bumped when a default changes in a way existing installs should feel.
 *
 * A stored value cannot say whether it was chosen or merely written while it was the
 * default - every settings change saves every key - so a changed default is applied once
 * by version rather than inferred from what is on disk.
 */
private const val SETTINGS_VERSION_KEY = "settings_version"
private const val SETTINGS_VERSION = 7

internal val DEFAULT_DISABLED_SOURCES: Set<DownloadSource> = setOf(
    DownloadSource.APTOIDE,
    DownloadSource.PLAY
)

/** Old source names that now point at the source serving the same catalogue. */
private val LEGACY_SOURCE_NAMES = mapOf("EVOZI" to DownloadSource.APK_CUBE)

private fun sourceFromStoredName(name: String): DownloadSource? =
    DownloadSource.entries.firstOrNull { it.name == name } ?: LEGACY_SOURCE_NAMES[name]

/**
 * The old default kept Uptodown before APKPure. Existing installs that saved
 * that exact list are moved to the new default below.
 */
private val LEGACY_DEFAULT_SOURCE_ORDER: List<DownloadSource> = listOf(
    DownloadSource.MORPHE_LINK,
    DownloadSource.APK_MIRROR,
    DownloadSource.UPTODOWN,
    DownloadSource.APK_PURE,
    DownloadSource.APK_CUBE,
    DownloadSource.APTOIDE,
    DownloadSource.AURORA,
    DownloadSource.PLAY
)

/**
 * Morphe Link is the resolver-backed fallback, so try it before walking the
 * individual sites. APKPure and APKCube run before Uptodown because they can
 * resolve many requests without a captcha, while Uptodown often cannot.
 */
private val PREFERRED_DEFAULT_SOURCE_ORDER: List<DownloadSource> = listOf(
    DownloadSource.MORPHE_LINK,
    DownloadSource.APK_MIRROR,
    DownloadSource.APK_PURE,
    DownloadSource.APK_CUBE,
    DownloadSource.UPTODOWN,
    DownloadSource.APTOIDE,
    DownloadSource.AURORA,
    DownloadSource.PLAY
)

internal val DEFAULT_SOURCE_ORDER: List<DownloadSource> =
    PREFERRED_DEFAULT_SOURCE_ORDER +
        DownloadSource.entries.filterNot { it in PREFERRED_DEFAULT_SOURCE_ORDER }

internal data class HelperSettings(
    val downloadLocation: DownloadLocation = DownloadLocation.TEMPORARY,
    val downloadRetention: DownloadRetention = DownloadRetention.KEEP,
    val downloadCustomTreeUri: String = "",
    /** APKMirror only: pick a density-specific row instead of nodpi. */
    val preferDensitySpecificVariants: Boolean = false,
    val networkPolicy: NetworkPolicy = NetworkPolicy.WIFI_AND_MOBILE,
    /** Maximum workers for a segmented download. File size can lower this. */
    val downloadConnections: DownloadConnectionCount = DownloadConnectionCount.FOUR,
    val logcatLogging: Boolean = true,
    // On by default: the helper exists to hand a file back to Morphe, and Fast Mode is
    // the path that does that in one step. An explicit off is stored and respected, so
    // this only decides what somebody who never touched the switch gets.
    val fastMode: Boolean = true,
    // Which version Fast Mode should auto-fetch when multiple are available.
    val fastModePolicy: FastModePolicy = FastModePolicy.REQUESTED,
    /**
     * Ask Morphe's resolver for the page its own dialog would use, instead of walking the
     * site. On by default because it is a single HEAD chain against a text index rather
     * than several hundred kilobytes of HTML; off means every source is scraped directly.
     */
    val useMorpheResolver: Boolean = true,
    val disabledSources: Set<DownloadSource> = DEFAULT_DISABLED_SOURCES,
    /** Source priority, first tried first. Missing sources are appended in enum order. */
    val sourceOrder: List<DownloadSource> = DEFAULT_SOURCE_ORDER,
    val themeMode: ThemeMode = ThemeMode.DARK,
    /** Where the palette comes from: the app's own, the wallpaper's, or neutral. */
    val themeStyle: ThemeStyle = ThemeStyle.MORPHE,
    /** Force pure black behind content in dark mode (OLED-friendly). */
    val pureBlackTheme: Boolean = false,
    /** Custom accent as `#RRGGBB`; empty keeps the style's own accent. */
    val customAccentColor: String = "",
    /** Drifting circles behind the pager; off removes the backdrop entirely. */
    val animatedBackground: Boolean = true,
    val hideFindAppsStats: Boolean = false,
    val hideCommunityNotice: Boolean = false,
    val useGradientCardColors: Boolean = true,
    val adGuardDns: Boolean = true,
    val virusTotalEnabled: Boolean = false,
    val virusTotalApiKey: String = "",
    val virusTotalScanMode: VirusTotalScanMode = VirusTotalScanMode.ASK,
    /** Hand the file to Morphe without showing the result when a scan comes back clean. */
    val virusTotalAutoProceed: Boolean = false,
    // Source opened by default when Helper launches (null = first enabled source).
    val preferredSource: DownloadSource? = null,
    val checkUpdates: Boolean = true,
    val updateChannel: UpdateChannel = UpdateChannel.STABLE,
    val updateMode: UpdateMode = UpdateMode.NOTIFY,
    val updatePopup: Boolean = true,
    /** Empty keeps Shizuku's default package; set for forks with another name. */
    val shizukuPackage: String = ""
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

internal enum class UpdateChannel(
    val title: String,
    val description: String
) {
    STABLE(
        title = "Stable",
        description = "Only stable releases are offered."
    ),
    BETA(
        title = "Beta",
        description = "Include pre-release builds tagged beta or rc."
    )
}

internal enum class UpdateMode(
    val title: String,
    val description: String
) {
    NOTIFY(
        title = "Notify me",
        description = "Show a notification when a newer release is available."
    ),
    SHIZUKU(
        title = "Automatic via Shizuku",
        description = "Download and install updates silently when Shizuku is running and approved. Falls back to a notification."
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
        title = "Internal app storage",
        description = "Keep the hand-off file in Helper's private storage."
    ),
    EXTERNAL(
        title = "App external storage",
        description = "Save a copy in the app's external files folder."
    ),
    DOWNLOADS(
        title = "Downloads",
        description = "Save a visible copy in your Downloads folder."
    ),
    CUSTOM(
        title = "Custom folder",
        description = "Save a copy in a folder you choose with the system picker."
    )
}

internal enum class DownloadRetention(
    val title: String,
    val description: String,
    val durationMillis: Long = 0L
) {
    KEEP(
        title = "Keep forever",
        description = "Keep downloads until you clear storage."
    ),
    AFTER_HANDOFF(
        title = "After hand-off",
        description = "Skip the saved copy and remove the temporary file after a short delay."
    ),
    HOUR_1(
        title = "1 hour",
        description = "Delete cache and saved copies after one hour.",
        durationMillis = 60L * 60L * 1000L
    ),
    HOUR_24(
        title = "24 hours",
        description = "Delete cache and saved copies after 24 hours.",
        durationMillis = 24L * 60L * 60L * 1000L
    ),
    DAY_7(
        title = "7 days",
        description = "Delete cache and saved copies after 7 days.",
        durationMillis = 7L * 24L * 60L * 60L * 1000L
    )
}

internal enum class DownloadConnectionCount(val count: Int) {
    TWO(2),
    FOUR(4),
    EIGHT(8),
    SIXTEEN(16),
    THIRTY_TWO(32);

    val title: String
        get() = "$count connections"

    val description: String
        get() = "Use up to $count parallel connections. File size can reduce this."
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
        downloadRetention = enumValueOrDefault(
            prefs.getString("download_retention", null),
            DownloadRetention.KEEP
        ),
        downloadCustomTreeUri = prefs.getString("download_custom_tree_uri", "") ?: "",
        preferDensitySpecificVariants = prefs.getBoolean("prefer_density_specific_variants", false),
        networkPolicy = enumValueOrDefault(
            prefs.getString("network_policy", null),
            NetworkPolicy.WIFI_AND_MOBILE
        ),
        downloadConnections = enumValueOrDefault(
            prefs.getString("download_connections", null),
            DownloadConnectionCount.FOUR
        ),
        logcatLogging = prefs.getBoolean("logcat_logging", true),
        fastMode = prefs.getBoolean("fast_mode", true),
        useMorpheResolver = prefs.getBoolean("use_morphe_resolver", true),
        fastModePolicy = enumValueOrDefault(
            prefs.getString("fast_mode_policy", null),
            FastModePolicy.REQUESTED
        ),
        disabledSources = prefs.getStringSet("disabled_sources", emptySet())
            .orEmpty()
            .mapNotNull { name -> DownloadSource.entries.firstOrNull { it.name == name } }
            .toSet(),
        sourceOrder = prefs.getString("source_order", null)
            ?.split(',')
            ?.mapNotNull(::sourceFromStoredName)
            ?.distinct()
            ?.takeIf { it.isNotEmpty() }
            ?: DEFAULT_SOURCE_ORDER,
        themeMode = enumValueOrDefault(
            prefs.getString("theme_mode", null),
            ThemeMode.DARK
        ),
        themeStyle = themeStyle(prefs),
        pureBlackTheme = prefs.getBoolean("pure_black_theme", false),
        customAccentColor = prefs.getString("custom_accent_color", "") ?: "",
        animatedBackground = prefs.getBoolean("animated_background", true),
        hideFindAppsStats = prefs.getBoolean("hide_find_apps_stats", false),
        hideCommunityNotice = prefs.getBoolean("hide_community_notice", false),
        useGradientCardColors = prefs.getBoolean("use_gradient_card_colors", true),
        adGuardDns = prefs.getBoolean("adguard_dns", true),
        virusTotalEnabled = prefs.getBoolean("virus_total_enabled", false),
        virusTotalApiKey = prefs.getString("virus_total_api_key", "") ?: "",
        virusTotalAutoProceed = prefs.getBoolean("virus_total_auto_proceed", false),
        virusTotalScanMode = enumValueOrDefault(
            prefs.getString("virus_total_scan_mode", null),
            VirusTotalScanMode.ASK
        ),
        preferredSource = prefs.getString("preferred_source", null)
            ?.let(::sourceFromStoredName),
        checkUpdates = prefs.getBoolean("check_updates", true),
        updateChannel = enumValueOrDefault(
            prefs.getString("update_channel", null),
            if (UpdateInfo.isPrerelease(BuildConfig.VERSION_NAME)) {
                UpdateChannel.BETA
            } else {
                UpdateChannel.STABLE
            }
        ),
        updateMode = enumValueOrDefault(
            prefs.getString("update_mode", null),
            UpdateMode.NOTIFY
        ),
        updatePopup = prefs.getBoolean("update_popup", true),
        shizukuPackage = prefs.getString("shizuku_package", "") ?: ""
    )

    // Version 2 turned Fast Mode on and narrowed the enabled sources to the ones the
    // helper is built around. Version 4 moved Morphe Link to the front of the default
    // order, but only when the saved order was still the old unmodified default.
    // Version 5 enables APKCube by default, removing the old default-off entry. Version
    // 6 enables AdGuard DNS by default. Version 7 moves Uptodown after APKPure and
    // APKCube for installs that still had the old default order. Each step is applied
    // once and saved, so from the next launch on the stored values win.
    val storedVersion = prefs.getInt(SETTINGS_VERSION_KEY, 1)
    var migrated = settings
    if (storedVersion < 2) {
        migrated = migrated.copy(fastMode = true, disabledSources = DEFAULT_DISABLED_SOURCES)
    }
    if (storedVersion < 4 &&
        migrated.sourceOrder == DownloadSource.entries.toList()
    ) {
        migrated = migrated.copy(sourceOrder = DEFAULT_SOURCE_ORDER)
    }
    if (storedVersion < 5) {
        migrated = migrated.copy(
            disabledSources = migrated.disabledSources - DownloadSource.APK_CUBE
        )
    }
    if (storedVersion < 6) {
        migrated = migrated.copy(adGuardDns = true)
    }
    if (storedVersion < 7 && migrated.sourceOrder == LEGACY_DEFAULT_SOURCE_ORDER) {
        migrated = migrated.copy(sourceOrder = DEFAULT_SOURCE_ORDER)
    }
    if (migrated !== settings) {
        saveHelperSettings(migrated)
    }

    logcatLoggingEnabled = migrated.logcatLogging
    adGuardDnsEnabled = migrated.adGuardDns
    return migrated
}

internal fun Context.saveHelperSettings(settings: HelperSettings) {
    logcatLoggingEnabled = settings.logcatLogging
    adGuardDnsEnabled = settings.adGuardDns
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString("download_location", settings.downloadLocation.name)
        .putString("download_retention", settings.downloadRetention.name)
        .putString("download_custom_tree_uri", settings.downloadCustomTreeUri)
        .putBoolean("prefer_density_specific_variants", settings.preferDensitySpecificVariants)
        .remove("show_launcher_icon")
        .remove("delete_temporary_after_handoff")
        .putString("network_policy", settings.networkPolicy.name)
        .putString("download_connections", settings.downloadConnections.name)
        .putBoolean("logcat_logging", settings.logcatLogging)
        .putInt(SETTINGS_VERSION_KEY, SETTINGS_VERSION)
        .putBoolean("fast_mode", settings.fastMode)
        .putBoolean("use_morphe_resolver", settings.useMorpheResolver)
        .putString("fast_mode_policy", settings.fastModePolicy.name)
        .putStringSet("disabled_sources", settings.disabledSources.map { it.name }.toSet())
        .putString("source_order", settings.orderedSources.joinToString(",") { it.name })
        .putString("theme_mode", settings.themeMode.name)
        .putString("theme_style", settings.themeStyle.name)
        .putBoolean("pure_black_theme", settings.pureBlackTheme)
        .putString("custom_accent_color", settings.customAccentColor)
        .putBoolean("animated_background", settings.animatedBackground)
        .putBoolean("adguard_dns", settings.adGuardDns)
        .putBoolean("virus_total_enabled", settings.virusTotalEnabled)
        .putString("virus_total_api_key", settings.virusTotalApiKey)
        .putString("virus_total_scan_mode", settings.virusTotalScanMode.name)
        .putBoolean("virus_total_auto_proceed", settings.virusTotalAutoProceed)
        .putString("preferred_source", settings.preferredSource?.name)
        .putBoolean("check_updates", settings.checkUpdates)
        .putString("update_channel", settings.updateChannel.name)
        .putString("update_mode", settings.updateMode.name)
        .putBoolean("update_popup", settings.updatePopup)
        .putBoolean("hide_find_apps_stats", settings.hideFindAppsStats)
        .putBoolean("hide_community_notice", settings.hideCommunityNotice)
        .putBoolean("use_gradient_card_colors", settings.useGradientCardColors)
        .putString("shizuku_package", settings.shizukuPackage)
        .apply()
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(name: String?, fallback: T): T =
    name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback

/** Saved priority order with any newly added sources appended. */
internal val HelperSettings.orderedSources: List<DownloadSource>
    get() {
        val saved = sourceOrder.filter { it in DownloadSource.entries }.distinct()
        return saved + DownloadSource.entries.filterNot { it in saved }
    }

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

// Private files rather than cacheDir: Android may clear the cache while Morphe
// is still reading the returned URI, which made the hand-off fail.
internal fun Context.temporaryDownloadsDir(): File =
    File(filesDir, "downloads").apply { mkdirs() }

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
    return files.sumOf { file ->
        val size = file.length()
        if (runCatching { file.delete() }.getOrDefault(false)) size else 0L
    }
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
            arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.SIZE),
            "${MediaStore.Downloads.RELATIVE_PATH} LIKE ?",
            arrayOf("${downloadsCopyRelativePath()}/%"),
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val size = cursor.getLong(sizeIndex).coerceAtLeast(0L)
                val deleted = runCatching {
                    contentResolver.delete(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        "${MediaStore.Downloads._ID}=?",
                        arrayOf(id.toString())
                    )
                }.getOrDefault(0)
                if (deleted > 0) freed += size
            }
        }
        return freed
    }
    val dir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "Morphe Helper"
    )
    val files = dir.listFiles()?.filter { it.isFile }.orEmpty()
    return files.sumOf { file ->
        val size = file.length()
        if (runCatching { file.delete() }.getOrDefault(false)) size else 0L
    }
}

