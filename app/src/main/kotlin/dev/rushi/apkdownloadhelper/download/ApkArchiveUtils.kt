package dev.rushi.apkdownloadhelper.download

import dev.rushi.apkdownloadhelper.*
import dev.rushi.apkdownloadhelper.ui.*
import dev.rushi.apkdownloadhelper.sources.*
import dev.rushi.apkdownloadhelper.play.*
import dev.rushi.apkdownloadhelper.download.*
import dev.rushi.apkdownloadhelper.storage.*
import dev.rushi.apkdownloadhelper.updater.*
import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.widget.Toast
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationManagerCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.models.PlayFile
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dev.rushi.apkdownloadhelper.download.VirusTotalScanner.isEntirelyClean
import dev.rushi.apkdownloadhelper.play.NativeDeviceInfoProvider
import dev.rushi.apkdownloadhelper.play.PlayHttpClient
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.SequenceInputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import net.dongliu.apk.parser.parser.BinaryXmlParser
import net.dongliu.apk.parser.parser.XmlStreamer
import net.dongliu.apk.parser.struct.xml.XmlCData
import net.dongliu.apk.parser.struct.xml.XmlNamespaceEndTag
import net.dongliu.apk.parser.struct.xml.XmlNamespaceStartTag
import net.dongliu.apk.parser.struct.xml.XmlNodeEndTag
import net.dongliu.apk.parser.struct.xml.XmlNodeStartTag
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query


internal fun playStoreUrl(packageName: String): String =
    "https://play.google.com/store/apps/details?id=${URLEncoder.encode(packageName, "UTF-8")}"

internal fun fileKindFromTags(tags: List<String>, request: HelperRequest): String {
    val available = tags
        .map { it.trim().lowercase(Locale.US) }
        .filter { it in DOWNLOAD_FILE_KIND_SET }
        .distinct()
    val requested = available.firstOrNull { it in request.requestedFileKinds }

    return requested
        ?: available.firstOrNull { request.acceptsFormat(it) }
        ?: available.firstOrNull()
        ?: "web"
}

internal fun Context.openPlayStoreListing(packageName: String, fallbackUrl: String) {
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=${Uri.encode(packageName)}")
    ).apply {
        setPackage("com.android.vending")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    runCatching { startActivity(marketIntent) }
        .onFailure { startActivity(webIntent) }
}

internal fun Context.isPackageInstalled(packageName: String): Boolean =
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
    }.isSuccess

internal fun String.normalizedHttpUrlOrNull(): String? {
    val normalized = trim().let { url ->
        when {
            url.startsWith("//") -> "https:$url"
            url.startsWith("http://", ignoreCase = true) ||
                url.startsWith("https://", ignoreCase = true) -> url
            else -> return null
        }
    }

    return runCatching {
        Request.Builder().url(normalized)
        normalized
    }.getOrNull()
}

internal fun validateApkLikeStream(
    input: java.io.InputStream,
    contentType: String?
): java.io.InputStream {
    val header = ByteArray(4)
    val headerSize = input.read(header)
    val isZip = headerSize >= 2 &&
        header[0] == 'P'.code.toByte() &&
        header[1] == 'K'.code.toByte()

    check(isZip) {
        val type = contentType?.let { " ($it)" }.orEmpty()
        "Source did not return a valid APK/APKS/XAPK$type."
    }

    return SequenceInputStream(ByteArrayInputStream(header, 0, headerSize), input)
}

internal fun Context.readDownloadedApkMetadata(
    file: File,
    expectedPackage: String? = null,
    expectedVersionNames: List<String> = emptyList(),
    expectedVersionCodes: List<Long> = emptyList()
): DownloadedApkMetadata? {
    if (file.extension.equals("apk", ignoreCase = true)) {
        // A container can arrive named .apk (a source served the bundle under an APK
        // name, or a candidate's kind was a guess), so fall back to reading it as one.
        return readApkMetadata(file)
            ?: readStandaloneManifest(file)
            ?: readContainerMetadata(file, expectedPackage, expectedVersionNames, expectedVersionCodes)
    }

    // Only the extension says "container" - the bytes decide. A page that serves a
    // single APK from a bundle-looking page would otherwise fail validation outright.
    return readContainerMetadata(file, expectedPackage, expectedVersionNames, expectedVersionCodes)
        ?: readApkMetadata(file)
}

/** The base APK's metadata from inside a split container, or null when it is not one. */
internal fun Context.readContainerMetadata(
    file: File,
    expectedPackage: String?,
    expectedVersionNames: List<String>,
    expectedVersionCodes: List<Long>
): DownloadedApkMetadata? {
    return runCatching {
        val validationDir = File(cacheDir, "validation").apply { mkdirs() }
        ZipFile(file).use { zip ->
            val manifest = readXapkManifest(zip)
            // The base APK inside split containers is not always named "base.apk":
            // some sources (e.g. APKPure XAPKs) name it after the package or give
            // every split a random name. Prefer the real base, but take the first
            // entry whose package/version matches the request.
            val packageHint = (expectedPackage ?: file.nameWithoutExtension.substringBefore('-'))
                .lowercase(Locale.US)
            val apkEntries = zip.entries()
                .asSequence()
                .filter { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) }
                .sortedWith(
                    compareBy<ZipEntry> { entry ->
                        val name = entry.name.substringAfterLast('/').lowercase(Locale.US)
                        when {
                            name == "base.apk" -> 0
                            packageHint.isNotEmpty() && name.contains(packageHint) -> 1
                            name.contains("base") -> 2
                            else -> 3
                        }
                    }
                        .thenByDescending { it.size }
                        .thenBy { it.name }
                )
                .toList()
            if (apkEntries.isEmpty()) return@use null

            var firstParsed: DownloadedApkMetadata? = null
            for (entry in apkEntries) {
                val metadata = readEntryMetadata(
                    context = this@readContainerMetadata,
                    zip = zip,
                    entry = entry,
                    validationDir = validationDir,
                    stem = file.nameWithoutExtension
                ) ?: continue
                if (firstParsed == null) firstParsed = metadata
                if (
                    matchesExpectedMetadata(
                        metadata,
                        expectedPackage,
                        expectedVersionNames,
                        expectedVersionCodes
                    )
                ) {
                    return@use metadata.withManifestMinSdk(manifest)
                }
            }

            // A readable APK always wins over manifest.json. Only fall back to the
            // manifest when no APK metadata could be parsed at all.
            firstParsed?.withManifestMinSdk(manifest) ?: manifest
        }
    }.getOrNull()
}

internal fun readEntryMetadata(
    context: Context,
    zip: ZipFile,
    entry: ZipEntry,
    validationDir: File,
    stem: String
): DownloadedApkMetadata? {
    // Android's package parser refuses split APKs that are not a standalone
    // base, and some XAPKs name every split randomly. Reading the nested
    // AndroidManifest.xml directly gives us the package and version anyway,
    // without writing the APK to the cache. If the direct read is incomplete
    // (for example versionName is a resource reference), extract the APK and
    // let PackageManager resolve it before falling back to the direct result.
    val direct = readNestedManifest(zip, entry)
    if (
        direct != null &&
        direct.versionName != null &&
        direct.versionCode != null &&
        direct.minSdkVersion != null
    ) {
        return direct
    }

    val extracted = File(
        validationDir,
        "$stem-${entry.name.hashCode()}.apk".sanitizeFileName()
    )
    return try {
        zip.getInputStream(entry).use { input ->
            extracted.outputStream().use { output -> input.copyTo(output) }
        }
        context.readApkMetadata(extracted)
            ?: direct
            ?: readStandaloneManifest(extracted)
    } catch (error: Exception) {
        null
    } finally {
        extracted.delete()
    }
}

/** Reads package metadata from a nested APK's AndroidManifest.xml. */
internal fun readNestedManifest(zip: ZipFile, entry: ZipEntry): DownloadedApkMetadata? {
    return try {
        zip.getInputStream(entry).use { input ->
            ZipInputStream(BufferedInputStream(input)).use { nested ->
                var child = nested.nextEntry
                while (child != null) {
                    if (child.name == "AndroidManifest.xml") {
                        val manifest = nested.readBytesLimited(MAX_APK_MANIFEST_BYTES)
                            ?: return null
                        return parseManifestBytes(manifest)
                    }
                    nested.closeEntry()
                    child = nested.nextEntry
                }
            }
        }
        null
    } catch (error: Exception) {
        null
    }
}

/** Reads AndroidManifest.xml from a standalone APK whose package could not be parsed. */
internal fun readStandaloneManifest(file: File): DownloadedApkMetadata? {
    return runCatching {
        ZipFile(file).use { apk ->
            val entry = apk.getEntry("AndroidManifest.xml") ?: return@use null
            val manifest = apk.getInputStream(entry).use {
                it.readBytesLimited(MAX_APK_MANIFEST_BYTES)
            } ?: return@use null
            parseManifestBytes(manifest)
        }
    }.getOrNull()
}

internal fun parseManifestBytes(manifest: ByteArray): DownloadedApkMetadata? {
    return runCatching {
        val streamer = ApkManifestStreamer()
        BinaryXmlParser(ByteBuffer.wrap(manifest), null).apply {
            setXmlStreamer(streamer)
            parse()
        }
        val packageName = streamer.packageName?.takeIf { it.isNotBlank() }
            ?: return@runCatching null
        DownloadedApkMetadata(
            packageName = packageName,
            versionName = streamer.versionName?.takeIf { it.isNotBlank() },
            versionCode = streamer.versionCode.takeIf { it > 0L },
            minSdkVersion = streamer.minSdkVersion.takeIf { it > 0 }
        )
    }.getOrNull()
}

/** Package/version/minSdk attributes from the root manifest and its uses-sdk node. */
internal class ApkManifestStreamer : XmlStreamer {
    var packageName: String? = null
    var versionName: String? = null
    var versionCode: Long = -1L
    var minSdkVersion: Int = -1

    override fun onStartTag(tag: XmlNodeStartTag) {
        val attributes = tag.attributes ?: return
        if (tag.name == "manifest" && packageName == null) {
            packageName = attributes.getString("package")
            versionName = attributes.getString("versionName")
            versionCode = attributes.getString("versionCode")?.toLongOrNull() ?: -1L
            if (versionName?.startsWith("resourceId:") == true) {
                versionName = null
            }
        } else if (tag.name == "uses-sdk") {
            minSdkVersion = attributes.getString("minSdkVersion")?.toIntOrNull()
                ?: minSdkVersion
        }
    }

    override fun onEndTag(tag: XmlNodeEndTag) = Unit

    override fun onCData(cData: XmlCData) = Unit

    override fun onNamespaceStart(tag: XmlNamespaceStartTag) = Unit

    override fun onNamespaceEnd(tag: XmlNamespaceEndTag) = Unit
}

/** Package/version metadata from an XAPK's manifest.json. */
internal fun readXapkManifest(zip: ZipFile): DownloadedApkMetadata? {
    val entry = zip.getEntry("manifest.json") ?: return null
    return runCatching {
        val bytes = zip.getInputStream(entry).use {
            it.readBytesLimited(MAX_XAPK_MANIFEST_BYTES)
        } ?: return@runCatching null
        val text = String(bytes, Charsets.UTF_8).trimStart('\uFEFF')
        val root = JSONObject(text)
        val packageName = root.optString("package_name", "").trim()
        if (packageName.isEmpty()) return@runCatching null
        DownloadedApkMetadata(
            packageName = packageName,
            versionName = root.optString("version_name", "").trim()
                .takeIf { it.isNotEmpty() },
            versionCode = parseJsonLong(root.opt("version_code")),
            minSdkVersion = parseJsonInt(root.opt("min_sdk_version"))
        )
    }.getOrNull()
}

internal fun parseJsonLong(value: Any?): Long? = when (value) {
    is Number -> value.toLong().takeIf { it > 0L }
    is String -> value.trim().toLongOrNull()?.takeIf { it > 0L }
    else -> null
}

internal fun parseJsonInt(value: Any?): Int? = when (value) {
    is Number -> value.toInt().takeIf { it > 0 }
    is String -> value.trim().toIntOrNull()?.takeIf { it > 0 }
    else -> null
}

internal fun InputStream.readBytesLimited(maxBytes: Int): ByteArray? {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        if (total > maxBytes) return null
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

internal fun matchesExpectedMetadata(
    metadata: DownloadedApkMetadata,
    expectedPackage: String?,
    expectedVersionNames: List<String>,
    expectedVersionCodes: List<Long>
): Boolean {
    if (expectedPackage != null && metadata.packageName != expectedPackage) return false
    if (expectedVersionCodes.isNotEmpty()) {
        val code = metadata.versionCode ?: return false
        if (code !in expectedVersionCodes) return false
    }
    if (expectedVersionNames.isNotEmpty()) {
        val name = metadata.versionName ?: return false
        if (expectedVersionNames.none { name.versionNameEquals(it) }) return false
    }
    return true
}

internal fun DownloadedApkMetadata.withManifestMinSdk(
    manifest: DownloadedApkMetadata?
): DownloadedApkMetadata {
    val manifestSdk = manifest?.minSdkVersion ?: return this
    return copy(minSdkVersion = maxOf(minSdkVersion ?: 0, manifestSdk))
}

internal const val MAX_APK_MANIFEST_BYTES = 8 * 1024 * 1024
internal const val MAX_XAPK_MANIFEST_BYTES = 512 * 1024

@Suppress("DEPRECATION")
internal fun Context.readApkMetadata(file: File): DownloadedApkMetadata? {
    val info = packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_META_DATA)
        ?: return null
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.longVersionCode
    } else {
        info.versionCode.toLong()
    }.takeIf { it > 0L }

    val minSdkVersion = info.applicationInfo?.minSdkVersion?.takeIf { it > 0 }
        ?: readStandaloneManifest(file)?.minSdkVersion

    return DownloadedApkMetadata(
        packageName = info.packageName,
        versionName = info.versionName,
        versionCode = versionCode,
        minSdkVersion = minSdkVersion
    )
}

internal fun parseInfoTableValue(doc: Document, label: String): String? =
    doc.select("tr")
        .firstOrNull { it.select("th").text().equals(label, ignoreCase = true) }
        ?.select("td")
        ?.lastOrNull()
        ?.text()
        ?.trim()
        ?.takeIf(String::isNotBlank)

internal fun fileKindFromUrl(url: String): String = fileKindFromUrlOrNull(url) ?: "apk"

/**
 * The kind a URL names, or null when it names none of them.
 *
 * Callers that already know a source's own label (a page stating "XAPK") can keep it
 * instead of falling back to a plain APK for a link that says nothing, such as
 * Uptodown's extension-less `dw.uptodown.com/dwn/<token>`.
 */
internal fun fileKindFromUrlOrNull(url: String): String? {
    val decoded = URLDecoder.decode(url, StandardCharsets.UTF_8.name()).lowercase(Locale.US)
    // Kind detection must be scoped to the file name (final path segment): CDN
    // hosts and APKMirror's own download.php path embed "apkmirror", so a bare
    // substring check for "apkm" wrongly matches and labels plain APKs as APKM
    // bundles, which then fail validation as containers.
    val path = runCatching { java.net.URI(decoded).path }.getOrDefault("")
    val fileName = path.substringAfterLast('/')
    val extension = fileName.substringAfterLast('.', "")
    val fileNameKind = Regex("""filename[^.]*\.(apk|apks|apkm|xapk)""")
        .find(decoded)
        ?.groupValues
        ?.getOrNull(1)
    // Some CDNs expose a bundle only in the path segment rather than the file
    // name (e.g. APKPure's d.apkpure.com/b/XAPK/<pkg>?versionCode=N), where the
    // last segment is the package, not the archive. Match those segments too so
    // an XAPK/APKS/APKM link isn't mislabelled as a plain APK (which later
    // fails archive validation). Scoped to whole segments to avoid the
    // apkmirror/dowload.php substring false-positives the extension check
    // already avoids.
    val segments = path.split('/').filter { it.isNotBlank() }
    val segmentKinds = setOf("apks", "apkm", "xapk")
    val segmentKind = segments.asReversed().firstOrNull { it in segmentKinds }
    return when {
        extension in setOf("apk", "apks", "apkm", "xapk") -> extension
        fileNameKind != null -> fileNameKind
        segmentKind != null -> segmentKind
        "xapk" in fileName -> "xapk"
        "apks" in fileName -> "apks"
        "apkm" in fileName -> "apkm"
        else -> null
    }
}

internal fun String.slugForUrl(): String =
    lowercase(Locale.US)
        .replace("&", " and ")
        .replace("'", "")
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "app" }

internal fun String.apkMirrorVersionSlug(): String =
    lowercase(Locale.US)
        .replace(".", "-")
        .replace("_", "-")
        .replace(Regex("[^a-z0-9-]+"), "-")
        .replace(Regex("-+"), "-")
        .trim('-')

/**
 * Variant suffixes some sources publish next to a normal release  e.g.
 * APKMirror's "21.36.45-SECONDARY" build. Such a build shares the version
 * number with the normal release but is an alternate package Morphe cannot
 * patch, so it must never satisfy a request for the plain version (nor the
 * other way around).
 */
internal val VERSION_VARIANT_MARKERS = listOf("secondary")

/** Matches a variant marker that directly follows a version number ("21.36.45-SECONDARY"). */
internal val VERSION_VARIANT_SUFFIX_REGEX = Regex(
    """(?i)(?<=\d)[\s._-]*(?:${VERSION_VARIANT_MARKERS.joinToString("|")})\b"""
)

/**
 * True when a version string, release URL or file name carries a known variant
 * marker such as "-SECONDARY". The marker has to follow a number-bearing token
 * so release slugs like ".../whatsapp-21-36-45-secondary-release/" and file
 * names like "app_21.36.45-SECONDARY.apk" are detected, while an app that is
 * merely called "Secondary" is not.
 */
internal fun String?.hasVariantBuildMarker(): Boolean {
    if (this.isNullOrBlank()) return false
    val tokens = lowercase(Locale.US)
        .split(Regex("""[^a-z0-9]+"""))
        .filter(String::isNotEmpty)
    return tokens.withIndex().any { (index, token) ->
        index > 0 &&
            token in VERSION_VARIANT_MARKERS &&
            tokens[index - 1].any(Char::isDigit)
    }
}

/**
 * The version name with any variant marker suffix removed
 * ("21.36.45-SECONDARY"  "21.36.45").
 */
internal fun String.withoutVariantMarker(): String =
    replace(VERSION_VARIANT_SUFFIX_REGEX, "")

internal fun String?.versionNameEquals(
    other: String?,
    ignoreVariantMarker: Boolean = false
): Boolean {
    if (this == null || other == null) return false
    // A variant build shares its version number with the normal release but is
    // a different package: the marker must match on both sides before the
    // numbers are compared. Pass [ignoreVariantMarker] when the request itself
    // asked for the variant, so it also matches a source that parses the
    // suffix out of its release slug and reports only the plain number.
    val leftSource = if (ignoreVariantMarker) withoutVariantMarker() else this
    val rightSource = if (ignoreVariantMarker) other.withoutVariantMarker() else other
    if (leftSource.hasVariantBuildMarker() != rightSource.hasVariantBuildMarker()) return false
    val left = leftSource.normalizedVersionName()
    val right = rightSource.normalizedVersionName()
    if (left.isBlank() || right.isBlank()) return false
    if (left == right) return true

    val leftParts = left.versionNumberParts()
    val rightParts = right.versionNumberParts()
    return leftParts.isNotEmpty() && leftParts == rightParts
}

/**
 * A downloaded result belongs to a request only when the package matches and
 * the file's version satisfies the request (or the request didn't pin one).
 * Used to stop a stale result  from a previous session's pending file or a
 * replayed [DownloadJobManager.Event.Completed]  from being handed to a
 * different request.
 */
internal fun PendingDownloadResult.belongsTo(request: HelperRequest?): Boolean {
    if (request == null) return false
    if (requestPackage != request.packageName) return false
    val requestedName = request.requestedVersionName ?: return true
    val candidateName = versionName ?: return true
    return candidateName.versionNameEquals(requestedName)
}

/**
 * A live completion event belongs to the current session when the package
 * matches the request on screen and the event was produced by the session
 * currently in flight (see [DownloadJobManager.currentEpoch]). Unlike
 * [belongsTo], the version is deliberately not compared: within the same
 * session the user may have chosen a different version (Latest/History tab)
 * and that file must still be returned to the caller instead of being
 * silently discarded, which previously left the app frozen at 100%.
 */
internal fun PendingDownloadResult.belongsToCurrentSession(
    request: HelperRequest?,
    epoch: Long
): Boolean =
    request != null &&
        requestPackage == request.packageName &&
        epoch == DownloadJobManager.currentEpoch

internal fun String.withoutTrailingVersionCode(): String =
    replace(Regex("""\s*\(\s*\d+\s*\)\s*$"""), "")
        .trim()

internal fun String.trailingVersionCode(): Long? =
    Regex("""\(\s*(\d+)\s*\)\s*$""")
        .find(this)
        ?.groupValues
        ?.getOrNull(1)
        ?.toLongOrNull()

internal fun String.normalizedVersionName(): String =
    withoutTrailingVersionCode()
        .lowercase(Locale.US)
        .replace(Regex("""\b(version|ver|v|release|stable|apk|xapk|apkm|apks|bundle)\b"""), " ")
        .replace(Regex("""[^\p{Alnum}]+"""), ".")
        .trim('.')

internal fun String.withManualModeHint(): String {
    if (contains("Manual mode", ignoreCase = true)) return this
    val message = trimEnd()
    val hint = "Use Manual mode for this source instead."
    return if (message.contains('\n')) "$message\n$hint" else "$message $hint"
}

internal fun sourceFailureMessage(source: DownloadSource, error: Throwable): String =
    sourceFailureMessage(source.label, error, action = "check")

internal fun downloadFailureMessage(candidate: DownloadCandidate, error: Throwable): String =
    sourceFailureMessage(candidate.source.label, error, action = "download")

internal fun sourceFailureMessage(sourceLabel: String, error: Throwable, action: String): String {
    val details = error.failureDetails()
    val httpCode = Regex("""\bHTTP\s+(\d{3})\b""", RegexOption.IGNORE_CASE)
        .find(details)
        ?.groupValues
        ?.getOrNull(1)
    val actionText = if (action == "download") "download" else "check"

    return when {
        httpCode == "403" -> {
            "$sourceLabel blocked automated access (HTTP 403), likely due to bot protection. Open the link and download manually."
        }
        httpCode == "429" -> {
            "$sourceLabel rate-limited the helper (HTTP 429). Try again later or use Manual mode."
        }
        httpCode == "404" -> {
            "$sourceLabel did not have the requested page (HTTP 404). Use Manual mode for this source instead."
        }
        httpCode != null -> {
            "$sourceLabel returned HTTP $httpCode during $actionText. Use Manual mode for this source instead."
        }
        details.contains("requires Android SDK", ignoreCase = true) &&
            details.contains("device is SDK", ignoreCase = true) -> {
            "$sourceLabel has no build compatible with this device: $details"
        }
        details.contains("cloudflare", ignoreCase = true) -> {
            "$sourceLabel showed a browser verification page, so direct access is blocked. Open the link and download manually."
        }
        details.contains("timeout", ignoreCase = true) -> {
            "$sourceLabel took too long to respond. Try again or use Manual mode."
        }
        details.contains("Unable to resolve host", ignoreCase = true) ||
            details.contains("failed to connect", ignoreCase = true) -> {
            "Could not connect to $sourceLabel. Check your connection or use Manual mode."
        }
        else -> {
            "Could not $actionText $sourceLabel: ${details.ifBlank { "unknown error" }}".withManualModeHint()
        }
    }
}

internal fun Throwable.failureDetails(): String =
    generateSequence(this) { it.cause }
        .mapNotNull { it.message?.trim()?.takeIf(String::isNotBlank) }
        .firstOrNull()
        ?: javaClass.simpleName

internal fun sourceVersionFromText(text: String): String? =
    Regex("""\b(v?\d+(?:[._-]\d+)+(?:[-.][A-Za-z0-9]+)?)\b""", RegexOption.IGNORE_CASE)
        .find(text)
        ?.value
        ?.trim()

internal fun compareVersionNames(left: String?, right: String?): Int {
    if (left == right) return 0
    if (left == null) return -1
    if (right == null) return 1

    val leftParts = left.versionNumberParts()
    val rightParts = right.versionNumberParts()
    val size = maxOf(leftParts.size, rightParts.size)
    for (index in 0 until size) {
        val leftPart = leftParts.getOrElse(index) { 0 }
        val rightPart = rightParts.getOrElse(index) { 0 }
        if (leftPart != rightPart) return leftPart.compareTo(rightPart)
    }

    return left.compareTo(right, ignoreCase = true)
}

internal fun String.versionNumberParts(): List<Int> =
    Regex("""\d+""")
        .findAll(this)
        .mapNotNull { it.value.toIntOrNull() }
        .toList()

internal fun requestedFileKindsFrom(rawFileType: String?, allowSplitArchive: Boolean): Set<String> {
    val normalized = rawFileType?.lowercase(Locale.US).orEmpty()
    val explicitKinds = DOWNLOAD_FILE_KIND_REGEX
        .findAll(normalized)
        .map { it.value.lowercase(Locale.US) }
        .toMutableSet()
    if (explicitKinds.isEmpty() && "package-archive" in normalized) {
        explicitKinds.add("apk")
    }

    if (explicitKinds.isEmpty()) {
        return if (allowSplitArchive) DOWNLOAD_FILE_KIND_SET else setOf("apk")
    }

    if (allowSplitArchive && "apk" in explicitKinds) {
        explicitKinds.addAll(SPLIT_ARCHIVE_FILE_KINDS)
    }

    return explicitKinds
}

internal fun Collection<String>.orderedFileKinds(): List<String> {
    val knownKinds = DOWNLOAD_FILE_KIND_ORDER.filter { it in this }
    val extraKinds = filter { it !in DOWNLOAD_FILE_KIND_SET }.distinct()
    return knownKinds + extraKinds
}

internal fun String?.variantFileSuffix(): String =
    this
        ?.lowercase(Locale.US)
        ?.replace(Regex("""[^a-z0-9._-]+"""), "-")
        ?.trim('-')
        ?.takeIf(String::isNotBlank)
        ?.let { "-$it" }
        .orEmpty()

internal fun String.sanitizeFileName(): String =
    replace(Regex("[^A-Za-z0-9._-]"), "_")

internal val PACKAGE_PATTERN = Regex("""^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$""")



