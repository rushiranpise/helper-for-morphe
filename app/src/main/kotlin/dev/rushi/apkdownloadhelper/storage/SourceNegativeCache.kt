package dev.rushi.apkdownloadhelper.storage

import android.content.Context
import dev.rushi.apkdownloadhelper.BuildConfig
import dev.rushi.apkdownloadhelper.ui.CandidateOption
import dev.rushi.apkdownloadhelper.ui.DownloadSource
import dev.rushi.apkdownloadhelper.ui.HelperRequest

/**
 * Remembers explicit "this source does not have it" answers for a short time.
 * Empty candidate lists, transient failures and cancellations are not cached,
 * and a successful lookup clears the entry so a source gets another chance as
 * soon as it recovers.
 */
internal object SourceNegativeCache {
    private const val PREFS = "source_negative_cache"
    private const val TTL_MS = 2L * 60L * 60L * 1000L

    private val cacheableSources = setOf(
        DownloadSource.APK_MIRROR,
        DownloadSource.APK_PURE,
        DownloadSource.APK_CUBE,
        DownloadSource.UPTODOWN,
        DownloadSource.APTOIDE
    )

    fun get(
        context: Context,
        source: DownloadSource,
        request: HelperRequest,
        option: CandidateOption
    ): String? {
        if (source !in cacheableSources) return null
        val key = key(source, request, option)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(key, null) ?: return null
        val time = raw.substringBefore('|').toLongOrNull() ?: return null
        if (System.currentTimeMillis() - time > TTL_MS) {
            prefs.edit().remove(key).apply()
            return null
        }
        return raw.substringAfter('|', "")
    }

    fun recordNotFound(
        context: Context,
        source: DownloadSource,
        request: HelperRequest,
        option: CandidateOption,
        reason: String
    ) {
        if (source !in cacheableSources || reason.isBlank()) return
        val key = key(source, request, option)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val editor = prefs.edit()
        prefs.all.forEach { (storedKey, value) ->
            val time = (value as? String)?.substringBefore('|')?.toLongOrNull()
            if (time != null && now - time > TTL_MS) editor.remove(storedKey)
        }
        editor.putString(key, "$now|${reason.trim().take(180)}").apply()
    }

    fun clear(
        context: Context,
        source: DownloadSource,
        request: HelperRequest,
        option: CandidateOption
    ) {
        if (source !in cacheableSources) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(key(source, request, option))
            .apply()
    }

    private fun key(
        source: DownloadSource,
        request: HelperRequest,
        option: CandidateOption
    ): String = buildString {
        append(BuildConfig.VERSION_NAME)
        append('|').append(source.name)
        append('|').append(option.name)
        append('|').append(request.packageName)
        append('|').        append(request.requestedVersionNames.sorted().joinToString(","))
        append('|').append(request.requestedVersionCodes.sorted().joinToString(","))
        append('|').append(request.requestedFileKinds.sorted().joinToString(","))
    }
}
