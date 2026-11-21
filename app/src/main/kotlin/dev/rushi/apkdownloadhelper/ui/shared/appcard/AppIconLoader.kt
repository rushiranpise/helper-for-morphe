package dev.rushi.apkdownloadhelper.ui.shared.appcard

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import dev.rushi.apkdownloadhelper.updater.MorpheArchive
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response

private val installedAppIconCache = ConcurrentHashMap<String, ImageBitmap>()

@Composable
internal fun rememberInstalledAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    var icon by remember(packageName) { mutableStateOf(installedAppIconCache[packageName]) }
    LaunchedEffect(packageName) {
        installedAppIconCache[packageName]?.let {
            icon = it
            return@LaunchedEffect
        }
        icon = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName)
                    .toBitmap(width = 176, height = 176)
                    .asImageBitmap()
            }.getOrNull()
        }?.also { installedAppIconCache[packageName] = it }
    }
    return icon
}

/** Decoded archive icons, bounded so a long scroll cannot fill the heap. */
private const val ARCHIVE_ICON_CACHE_BYTES = 12 * 1024 * 1024

private const val ARCHIVE_ICON_MAX_PX = 256

internal val archiveIconCache = object : LruCache<String, ImageBitmap>(ARCHIVE_ICON_CACHE_BYTES) {
    override fun sizeOf(key: String, value: ImageBitmap): Int =
        value.width * value.height * 4
}

/** Decode at avatar scale instead of retaining full-resolution source bitmaps. */
private fun decodeArchiveIcon(bytes: ByteArray): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val maxDimension = maxOf(bounds.outWidth, bounds.outHeight)
    var sampleSize = 1
    while (maxDimension / (sampleSize * 2) >= ARCHIVE_ICON_MAX_PX) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
}

private const val PUBLIC_METADATA_ICON_BASE = "https://publicmetadata.co.uk/"

/**
 * Play Store icon lookup by package name. The service redirects to the store's
 * 128px WebP and answers 404 for packages with no Play listing.
 */
internal fun publicMetadataIconUrl(packageName: String): String =
    PUBLIC_METADATA_ICON_BASE + packageName

/** Public metadata 404s, kept so a missing Play listing is looked up once per process. */
private val failedPublicMetadataLookups = object : LruCache<String, Boolean>(512) {
    override fun sizeOf(key: String, value: Boolean): Int = 1
}

private sealed interface IconFetchResult {
    class Loaded(val bitmap: ImageBitmap) : IconFetchResult
    object NotFound : IconFetchResult
    object Failed : IconFetchResult
}

/** Icon fetch that is cancelled when the row leaves the viewport. */
private suspend fun fetchIcon(iconUrl: String): IconFetchResult {
    val request = runCatching {
        Request.Builder()
            .url(iconUrl)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Mobile Safari/537.36"
            )
            .build()
    }.getOrNull() ?: return IconFetchResult.Failed
    val call = MorpheArchive.http.newCall(request)
    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resume(IconFetchResult.Failed)
            }

            override fun onResponse(call: Call, response: Response) {
                val result = runCatching {
                    response.use {
                        when {
                            it.code == 404 -> IconFetchResult.NotFound
                            !it.isSuccessful -> IconFetchResult.Failed
                            else -> it.body.bytes()
                                .let(::decodeArchiveIcon)
                                ?.let { bitmap -> IconFetchResult.Loaded(bitmap) }
                                ?: IconFetchResult.Failed
                        }
                    }
                }.getOrDefault(IconFetchResult.Failed)
                if (continuation.isActive) continuation.resume(result)
            }
        })
    }
}

/**
 * Decoded icons from the shared cache, fetched once per URL. A public metadata
 * 404 is remembered so rows without a Play listing do not retry while scrolling.
 */
internal suspend fun loadArchiveIcon(iconUrl: String): ImageBitmap? {
    archiveIconCache.get(iconUrl)?.let { return it }
    val isPublicMetadata = iconUrl.startsWith(PUBLIC_METADATA_ICON_BASE)
    if (isPublicMetadata && failedPublicMetadataLookups.get(iconUrl) != null) return null
    return when (val result = fetchIcon(iconUrl)) {
        is IconFetchResult.Loaded -> result.bitmap.also { archiveIconCache.put(iconUrl, it) }
        IconFetchResult.NotFound -> {
            if (isPublicMetadata) failedPublicMetadataLookups.put(iconUrl, true)
            null
        }
        IconFetchResult.Failed -> null
    }
}
