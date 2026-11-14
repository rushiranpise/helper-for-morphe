package dev.rushi.apkdownloadhelper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import kotlin.coroutines.resume

internal data class UpdateCheckResult(
    val info: UpdateInfo?,
    val message: String,
    val available: Boolean
)

/**
 * Owns update checks, the cached release, APK downloads and the optional
 * Shizuku install. Checking is throttled and serialized; a manually opened
 * Updates screen can always force a fresh check.
 */
internal object UpdateManager {
    const val EXTRA_OPEN_UPDATES = "dev.rushi.apkdownloadhelper.OPEN_UPDATES"

    private const val PREFS = "update_state"
    private const val KEY_LAST_CHECK = "last_check"
    private const val KEY_SNOOZE_UNTIL = "snooze_until"
    private const val KEY_SKIPPED_VERSION = "skipped_version"
    private const val KEY_VERSION = "latest_version"
    private const val KEY_RELEASE_URL = "release_url"
    private const val KEY_ASSET_URL = "asset_url"
    private const val KEY_ASSET_NAME = "asset_name"
    private const val KEY_ASSET_SIZE = "asset_size"
    private const val KEY_NOTES = "latest_notes"

    private const val CHANNEL_UPDATES = "updates"
    private const val NOTIFICATION_ID = 73
    private const val CHECK_INTERVAL = 6L * 60L * 60L * 1000L
    private const val SNOOZE_INTERVAL = 24L * 60L * 60L * 1000L

    private val checkMutex = Mutex()
    private val checkGeneration = AtomicLong(0L)
    private val HTTP: OkHttpClient = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.MINUTES)
        .build()

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Drops the cached release after the user switches update channels. */
    fun clearCachedLatest(context: Context) {
        prefs(context).edit()
            .remove(KEY_LAST_CHECK)
            .remove(KEY_VERSION)
            .remove(KEY_RELEASE_URL)
            .remove(KEY_ASSET_URL)
            .remove(KEY_ASSET_NAME)
            .remove(KEY_ASSET_SIZE)
            .remove(KEY_NOTES)
            .apply()
    }

    /** Invalidates in-flight checks and clears the old channel before a recheck. */
    fun resetForChannelSwitch(context: Context) {
        checkGeneration.incrementAndGet()
        clearCachedLatest(context)
        cancelNotification(context)
    }

    fun cachedLatest(context: Context): UpdateInfo? {
        val store = prefs(context)
        val version = store.getString(KEY_VERSION, "").orEmpty()
        if (version.isBlank()) return null
        return UpdateInfo(
            version = version,
            releaseUrl = store.getString(KEY_RELEASE_URL, "").orEmpty(),
            assetUrl = store.getString(KEY_ASSET_URL, "").orEmpty(),
            assetName = store.getString(KEY_ASSET_NAME, "").orEmpty(),
            assetSize = store.getLong(KEY_ASSET_SIZE, 0L),
            notes = store.getString(KEY_NOTES, "").orEmpty()
        )
    }

    fun lastCheck(context: Context): Long = prefs(context).getLong(KEY_LAST_CHECK, 0L)

    fun skipVersion(context: Context, version: String) {
        prefs(context).edit().putString(KEY_SKIPPED_VERSION, version).apply()
        cancelNotification(context)
    }

    fun snooze(context: Context, hours: Long = 24L) {
        val until = System.currentTimeMillis() + hours * 60L * 60L * 1000L
        prefs(context).edit().putLong(KEY_SNOOZE_UNTIL, until).apply()
        cancelNotification(context)
    }

    suspend fun checkForUpdates(
        context: Context,
        manual: Boolean,
        settings: HelperSettings = context.loadHelperSettings()
    ): UpdateCheckResult {
        if (!manual && !settings.checkUpdates) {
            return UpdateCheckResult(null, "Update checks are disabled.", false)
        }
        val generation = checkGeneration.incrementAndGet()
        return checkMutex.withLock {
            if (generation != checkGeneration.get()) {
                return@withLock supersededCheck()
            }

            try {
                val currentVersion = UpdateChecker.installedVersion(context)
                val includePrereleases = settings.updateChannel == UpdateChannel.BETA
                val info = withContext(Dispatchers.IO) {
                    UpdateChecker.check(currentVersion, includePrereleases)
                }
                if (generation != checkGeneration.get()) {
                    return@withLock supersededCheck()
                }
                cacheInfo(context, info)
                if (generation != checkGeneration.get()) {
                    return@withLock supersededCheck()
                }

                val result = if (!info.isNewerThan(currentVersion)) {
                    cancelNotification(context)
                    UpdateCheckResult(info, "You are up to date (v$currentVersion).", false)
                } else if (info.version == skippedVersion(context)) {
                    cancelNotification(context)
                    UpdateCheckResult(info, "Update v${info.version} is skipped.", false)
                } else if (!manual && System.currentTimeMillis() < snoozedUntil(context)) {
                    UpdateCheckResult(info, "Update reminder is snoozed.", false)
                } else if (
                    settings.updateMode == UpdateMode.SHIZUKU &&
                    ShizukuHelper.status(context) == ShizukuHelper.READY &&
                    info.assetUrl.isNotBlank()
                ) {
                    val (ok, detail) = installSilently(context, info)
                    if (ok) {
                        cancelNotification(context)
                        UpdateCheckResult(info, "Update v${info.version} installed.", true)
                    } else {
                        notifyUpdate(context, info)
                        UpdateCheckResult(
                            info,
                            "Update v${info.version} available. Automatic install failed: $detail",
                            true
                        )
                    }
                } else {
                    notifyUpdate(context, info)
                    UpdateCheckResult(info, "Update v${info.version} available.", true)
                }
                if (generation != checkGeneration.get()) supersededCheck() else result
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (generation != checkGeneration.get()) {
                    supersededCheck()
                } else {
                    Log.w(TAG, "Update check failed", error)
                    UpdateCheckResult(
                        cachedLatest(context),
                        "Update check failed: ${error.message ?: "network error"}",
                        false
                    )
                }
            }
        }
    }

    private fun supersededCheck(): UpdateCheckResult =
        UpdateCheckResult(null, "Update check superseded by a newer one.", false)

    /**
     * Called once when the main activity starts. Returns a newer release when
     * the in-app popup should be offered; notifications and silent installs
     * are handled by [checkForUpdates].
     */
    suspend fun checkOnLaunch(context: Context): UpdateInfo? {
        val settings = context.loadHelperSettings()
        if (!settings.checkUpdates) return null
        if (System.currentTimeMillis() < snoozedUntil(context)) return null

        val currentVersion = UpdateChecker.installedVersion(context)
        if (System.currentTimeMillis() - lastCheck(context) < CHECK_INTERVAL) {
            return cachedLatest(context)?.takeIf {
                it.isNewerThan(currentVersion) &&
                    it.version != skippedVersion(context)
            }
        }

        val result = checkForUpdates(context, manual = false, settings = settings)
        return result.info?.takeIf { result.available }
    }

    suspend fun download(context: Context, info: UpdateInfo): File = withContext(Dispatchers.IO) {
        if (info.assetUrl.isBlank()) throw IOException("This release has no APK asset")
        val dir = File(context.filesDir, "updates")
        if (!dir.exists() && !dir.mkdirs()) throw IOException("Cannot create update directory")
        dir.listFiles()?.forEach { it.delete() }

        val target = File(dir, "update-v${info.version}.apk")
        var last: IOException? = null
        for (attempt in 0 until 3) {
            target.delete()
            val request = Request.Builder()
                .url(info.assetUrl)
                .header("User-Agent", "HelperForMorphe/${BuildConfig.VERSION_NAME}")
                .header("Accept", "application/octet-stream")
                .build()
            try {
                HTTP.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Update download HTTP ${response.code}")
                    }
                    val input = response.body.byteStream()
                    FileOutputStream(target).use { output ->
                        input.use { it.copyTo(output) }
                    }
                }
                last = null
                break
            } catch (error: IOException) {
                last = error
                if (attempt < 2) {
                    runCatching { Thread.sleep(1500L * (attempt + 1)) }
                }
            }
        }
        if (last != null) {
            target.delete()
            throw last
        }
        if (info.assetSize > 0L && target.length() != info.assetSize) {
            target.delete()
            throw IOException("Downloaded update is incomplete")
        }
        target
    }

    /**
     * Refuses an APK unless its package, version and signing certificate match
     * the installed helper. This is the gate that makes automatic install safe.
     */
    fun verifyDownloadedApk(context: Context, file: File, info: UpdateInfo): String? {
        val flags = packageFlags()
        val archive = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: return "Could not read the downloaded APK"
        if (archive.packageName != context.packageName) {
            return "Downloaded update does not match this helper"
        }
        if (normalizeVersion(archive.versionName) != normalizeVersion(info.version)) {
            return "Downloaded update version does not match the release"
        }
        val installed = try {
            context.packageManager.getPackageInfo(context.packageName, flags)
        } catch (error: Exception) {
            return "Could not read the installed helper"
        }
        val archiveSigners = signerHashes(archive)
        val installedSigners = signerHashes(installed)
        if (archiveSigners.isEmpty() || installedSigners.isEmpty() || archiveSigners != installedSigners) {
            return "Downloaded update is signed by a different key than this build"
        }
        return null
    }

    suspend fun installWithShizuku(context: Context, file: File): Pair<Boolean, String> {
        val staged = runCatching {
            withContext(Dispatchers.IO) { stageForShizuku(context, file) }
        }.getOrElse { error ->
            return false to (error.message ?: "Could not stage the update for Shizuku")
        }
        return suspendCancellableCoroutine { continuation ->
            ShizukuHelper.install(context, staged) { ok, message ->
                deleteStaged(context, staged)
                if (continuation.isActive) continuation.resume(ok to message)
            }
        }
    }

    private fun deleteStaged(context: Context, staged: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                context.contentResolver.delete(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
                    arrayOf(staged.name)
                )
            }
        }
        staged.delete()
    }

    /**
     * Shizuku's user service runs as shell, outside this app's sandbox. Stage
     * the APK where that UID can read it: public Downloads on Android 10+,
     * the app's external files directory on older releases.
     */
    @Suppress("DEPRECATION")
    private fun stageForShizuku(context: Context, file: File): File {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val displayName = "${file.nameWithoutExtension}-${System.currentTimeMillis()}.apk"
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/Helper updates"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Could not create the public update file")
            try {
                file.inputStream().use { input ->
                    resolver.openOutputStream(uri)?.use { output ->
                        input.copyTo(output)
                    } ?: throw IOException("Could not open the public update file")
                }
                val done = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(uri, done, null, null)
            } catch (error: Exception) {
                runCatching { resolver.delete(uri, null, null) }
                throw error
            }
            val actualName = resolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else ""
            }?.takeIf { it.isNotBlank() } ?: displayName
            val publicDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Helper updates"
            )
            return File(publicDir, actualName)
        }

        val dir = File(
            context.getExternalFilesDir("updates") ?: context.filesDir,
            "shizuku"
        )
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException("Could not create the Shizuku staging directory")
        }
        val staged = File(dir, file.name)
        file.copyTo(staged, overwrite = true)
        staged.setReadable(true, false)
        return staged
    }

    /**
     * Opens the system installer. Returns false when the user first has to
     * allow installs from this app, or when no installer could be opened.
     */
    fun openInstaller(context: Context, file: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val settings = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(settings) }
            return false
        }
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.files",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (error: Exception) {
            Log.e(TAG, "Could not open the Android installer", error)
            false
        }
    }

    fun isBatteryUnrestricted(context: Context): Boolean {
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestBatteryExemption(context: Context) {
        val request = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(request) }.isSuccess) return
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private suspend fun installSilently(context: Context, info: UpdateInfo): Pair<Boolean, String> {
        return try {
            val file = download(context, info)
            val verification = verifyDownloadedApk(context, file, info)
            if (verification != null) {
                file.delete()
                false to verification
            } else {
                installWithShizuku(context, file)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            false to (error.message ?: "Update download failed")
        }
    }

    private fun cacheInfo(context: Context, info: UpdateInfo) {
        prefs(context).edit()
            .putLong(KEY_LAST_CHECK, System.currentTimeMillis())
            .putString(KEY_VERSION, info.version)
            .putString(KEY_RELEASE_URL, info.releaseUrl)
            .putString(KEY_ASSET_URL, info.assetUrl)
            .putString(KEY_ASSET_NAME, info.assetName)
            .putLong(KEY_ASSET_SIZE, info.assetSize)
            .putString(KEY_NOTES, info.notes)
            .apply()
    }

    private fun skippedVersion(context: Context): String =
        prefs(context).getString(KEY_SKIPPED_VERSION, "").orEmpty()

    private fun snoozedUntil(context: Context): Long =
        prefs(context).getLong(KEY_SNOOZE_UNTIL, 0L)

    private fun notifyUpdate(context: Context, info: UpdateInfo) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_UPDATES,
                    "Helper updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "A newer release of the helper is available"
                }
            )
        }
        val open = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_OPEN_UPDATES, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context,
            10,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = "Helper v${info.version} is available. You have v${BuildConfig.VERSION_NAME}."
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Update available")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    private fun cancelNotification(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
    }

    @Suppress("DEPRECATION")
    private fun packageFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }

    private fun signerHashes(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signing = info.signingInfo ?: return emptySet()
            if (signing.hasMultipleSigners()) {
                signing.apkContentsSigners
            } else {
                signing.signingCertificateHistory
            }
        } else {
            @Suppress("DEPRECATION")
            info.signatures
        }
        return signatures.orEmpty().map { signature ->
            MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
                .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        }.toSet()
    }

    private fun normalizeVersion(value: String?): String =
        value.orEmpty().trim().removePrefix("v").removePrefix("V")

    private const val TAG = "UpdateManager"
}
