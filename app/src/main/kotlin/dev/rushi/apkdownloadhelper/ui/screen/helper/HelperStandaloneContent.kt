package dev.rushi.apkdownloadhelper.ui.screen.helper

import android.content.ClipData
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import dev.chrisbanes.haze.HazeState
import dev.rushi.apkdownloadhelper.BuildConfig
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.download.readDownloadedApkMetadata
import dev.rushi.apkdownloadhelper.formatHistoryTimestamp
import dev.rushi.apkdownloadhelper.isHistoryUriUsable
import dev.rushi.apkdownloadhelper.storage.DownloadHistoryStore
import dev.rushi.apkdownloadhelper.storage.ScanVerdict
import dev.rushi.apkdownloadhelper.storage.HelperSettings
import dev.rushi.apkdownloadhelper.storage.downloadsCopyRelativePath
import dev.rushi.apkdownloadhelper.storage.externalDownloadsDir
import dev.rushi.apkdownloadhelper.storage.DownloadLocation
import dev.rushi.apkdownloadhelper.storage.cleanupDownloads
import dev.rushi.apkdownloadhelper.storage.customFolderName
import dev.rushi.apkdownloadhelper.storage.storageLocation
import dev.rushi.apkdownloadhelper.storage.temporaryDownloadsDir
import dev.rushi.apkdownloadhelper.storage.untrackCustomDownload
import dev.rushi.apkdownloadhelper.ui.shared.appcard.AppListCard
import dev.rushi.apkdownloadhelper.ui.shared.appcard.FindAppCardLoading
import dev.rushi.apkdownloadhelper.ui.shared.button.DialogButton
import dev.rushi.apkdownloadhelper.ui.shared.button.GlassHeaderButton
import dev.rushi.apkdownloadhelper.ui.shared.dialog.AppAlertDialog
import dev.rushi.apkdownloadhelper.ui.shared.layout.AppWordmark
import dev.rushi.apkdownloadhelper.ui.shared.state.EmptyLaunchState
import dev.rushi.apkdownloadhelper.ui.theme.AppColors
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import dev.rushi.apkdownloadhelper.ui.theme.animatedListItem
import dev.rushi.apkdownloadhelper.ui.theme.glassFill
import dev.rushi.apkdownloadhelper.ui.theme.hazeSourceIfEnabled
import dev.rushi.apkdownloadhelper.ui.theme.pressScale
import dev.rushi.apkdownloadhelper.ui.theme.rememberMorphePillShape
import dev.rushi.apkdownloadhelper.ui.util.formatBytes
import dev.rushi.apkdownloadhelper.updater.ArchiveApp
import dev.rushi.apkdownloadhelper.updater.MorpheArchive
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val SUPPORTED_ARCHIVE_EXTENSIONS = setOf("apk", "apks", "apkm", "xapk")

internal data class StoredApkCardItem(
    val id: String,
    val uri: Uri,
    val fileName: String,
    val fileKind: String,
    val versionName: String?,
    val versionCode: Long?,
    val sourceName: String,
    val locationLabel: String,
    val sizeBytes: Long,
    val timestamp: Long,
    val app: ArchiveApp,
    val installed: Boolean,
    val subtitleText: String,
    val abis: List<String> = emptyList(),
    val minSdkVersion: Int? = null,
    val obbSummary: String? = null,
    val scanVerdict: ScanVerdict? = null,
    /** True when the file lives in the app's cache or app-specific storage. */
    val isPrivate: Boolean = false
)

private fun String.hasSupportedArchiveExtension(): Boolean =
    substringAfterLast('.', "").lowercase(Locale.US) in SUPPORTED_ARCHIVE_EXTENSIONS

internal data class ResolvedArchiveDetails(
    val packageName: String? = null,
    val appName: String? = null,
    val versionName: String? = null,
    val versionCode: Long? = null,
    val minSdkVersion: Int? = null,
    val abis: List<String> = emptyList()
)

internal fun inspectArchiveFile(context: Context, file: File): ResolvedArchiveDetails =
    inspectArchiveUri(context, Uri.fromFile(file), file.name, file)

private fun abiFromName(lower: String): String? = when {
    "arm64_v8a" in lower || "arm64-v8a" in lower -> "arm64-v8a"
    "armeabi_v7a" in lower || "armeabi-v7a" in lower -> "armeabi-v7a"
    "x86_64" in lower -> "x86_64"
    "x86" in lower -> "x86"
    else -> null
}

/** Random-access read of APKM / APKS / XAPK: no full-bundle streaming. */
private fun inspectBundleFast(context: Context, file: File): ResolvedArchiveDetails? = runCatching {
    java.util.zip.ZipFile(file).use { zip ->
        var pkg: String? = null
        var label: String? = null
        var verName: String? = null
        var verCode: Long? = null
        var minSdk: Int? = null
        val abis = LinkedHashSet<String>()
        val names = zip.entries().asSequence().map { it.name }.toList()

        zip.getEntry("info.json")?.let { e ->
            val json = JSONObject(zip.getInputStream(e).use { it.readBytes() }.toString(Charsets.UTF_8))
            pkg = json.optString("pname").takeIf { it.isNotBlank() } ?: pkg
            label = json.optString("app_name").takeIf { it.isNotBlank() } ?: label
            verName = json.optString("release_version").takeIf { it.isNotBlank() }
                ?: json.optString("version").takeIf { it.isNotBlank() }
            val vc = json.optLong("version_code", -1L)
            if (vc > 0L) verCode = vc
            json.optString("min_api").toIntOrNull()?.takeIf { it > 0 }?.let { minSdk = it }
            json.optJSONArray("arches")?.let { arr ->
                for (i in 0 until arr.length()) arr.optString(i).takeIf { it.isNotBlank() }?.let(abis::add)
            }
        }
        zip.getEntry("manifest.json")?.let { e ->
            val json = JSONObject(zip.getInputStream(e).use { it.readBytes() }.toString(Charsets.UTF_8))
            pkg = json.optString("package_name").takeIf { it.isNotBlank() } ?: pkg
            label = json.optString("name").takeIf { it.isNotBlank() } ?: label
            verName = json.optString("version_name").takeIf { it.isNotBlank() } ?: verName
            val vc = json.optLong("version_code", -1L)
            if (vc > 0L) verCode = vc
            json.optString("min_sdk_version").toIntOrNull()?.takeIf { it > 0 }?.let { minSdk = it }
        }
        for (name in names) {
            val lower = name.lowercase(Locale.US)
            if (lower.startsWith("lib/")) {
                name.split('/').getOrNull(1)?.takeIf { it.isNotBlank() }?.let(abis::add)
            } else if (lower.endsWith(".apk")) {
                abiFromName(lower)?.let(abis::add)
            }
        }
        // APKS (bundletool) has no metadata file, so read the base split.
        if (verName == null || pkg == null) {
            val base = names.firstOrNull {
                val l = it.lowercase(Locale.US)
                l.endsWith("base-master.apk") || l.endsWith("/base.apk") || l == "base.apk"
            } ?: names.firstOrNull {
                val l = it.lowercase(Locale.US)
                l.endsWith(".apk") && "config." !in l && "split_" !in l
            }
            val entry = base?.let { zip.getEntry(it) }
            if (entry != null) {
                val tmp = File.createTempFile("inspect_", ".apk", context.cacheDir)
                try {
                    zip.getInputStream(entry).use { input -> tmp.outputStream().use { input.copyTo(it) } }
                    val meta = context.readDownloadedApkMetadata(tmp)
                    pkg = meta?.packageName ?: pkg
                    verName = meta?.versionName ?: verName
                    verCode = meta?.versionCode ?: verCode
                    minSdk = meta?.minSdkVersion ?: minSdk
                } finally {
                    tmp.delete()
                }
            }
        }
        ResolvedArchiveDetails(pkg, label, verName, verCode, minSdk, abis.toList())
    }
}.getOrNull()

private fun inspectArchiveUri(
    context: Context,
    uri: Uri,
    fileName: String,
    localFile: File?
): ResolvedArchiveDetails {
    val ext = fileName.substringAfterLast('.', "").lowercase(Locale.US)

    if (ext != "apk" && localFile != null && localFile.isFile) {
        inspectBundleFast(context, localFile)?.let { return it }
    }

    // 1. Direct local APK file
    if (ext == "apk" && localFile != null && localFile.isFile) {
        val meta = runCatching { context.readDownloadedApkMetadata(localFile) }.getOrNull()
        val abis = extractAbisFromApkFile(localFile)
        return ResolvedArchiveDetails(
            packageName = meta?.packageName,
            versionName = meta?.versionName,
            versionCode = meta?.versionCode,
            minSdkVersion = meta?.minSdkVersion,
            abis = abis
        )
    }

    // 2. Single APK behind a content URI: copy it out so the normal APK reader
    // can parse its package and version, then delete the temporary copy.
    if (ext == "apk" && localFile == null) {
        return runCatching {
            val tmp = File.createTempFile("inspect_", ".apk", context.cacheDir)
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tmp.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Could not open the APK")
                val meta = context.readDownloadedApkMetadata(tmp)
                ResolvedArchiveDetails(
                    packageName = meta?.packageName,
                    versionName = meta?.versionName,
                    versionCode = meta?.versionCode,
                    minSdkVersion = meta?.minSdkVersion,
                    abis = extractAbisFromApkFile(tmp)
                )
            } finally {
                tmp.delete()
            }
        }.getOrDefault(ResolvedArchiveDetails())
    }

    // 3. Split archive (.apkm, .apks, .xapk) via stream
    return runCatching {
        val input = if (localFile != null && localFile.isFile) {
            localFile.inputStream()
        } else {
            context.contentResolver.openInputStream(uri)
        } ?: return ResolvedArchiveDetails()

        var pkg: String? = null
        var label: String? = null
        var verName: String? = null
        var verCode: Long? = null
        var minSdk: Int? = null
        val abis = LinkedHashSet<String>()

        ZipInputStream(input.buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val entryName = entry.name
                val lower = entryName.lowercase(Locale.US)
                when {
                    // APKMirror .apkm metadata
                    lower == "info.json" -> {
                        val json = JSONObject(zis.readBytes().toString(Charsets.UTF_8))
                        pkg = json.optString("pname").takeIf { it.isNotBlank() } ?: pkg
                        label = json.optString("app_name").takeIf { it.isNotBlank() } ?: label
                        verName = json.optString("release_version").takeIf { it.isNotBlank() }
                            ?: json.optString("version").takeIf { it.isNotBlank() }
                            ?: verName
                        val vc = json.optLong("version_code", -1L)
                        if (vc > 0L) verCode = vc
                        val minApi = json.optString("min_api").toIntOrNull()
                        if (minApi != null && minApi > 0) minSdk = minApi
                        val arches = json.optJSONArray("arches")
                        if (arches != null) {
                            for (i in 0 until arches.length()) {
                                arches.optString(i).takeIf { it.isNotBlank() }?.let(abis::add)
                            }
                        }
                    }
                    // XAPK manifest.json
                    lower == "manifest.json" -> {
                        val json = JSONObject(zis.readBytes().toString(Charsets.UTF_8))
                        pkg = json.optString("package_name").takeIf { it.isNotBlank() } ?: pkg
                        label = json.optString("name").takeIf { it.isNotBlank() } ?: label
                        verName = json.optString("version_name").takeIf { it.isNotBlank() } ?: verName
                        val vc = json.optLong("version_code", -1L)
                        if (vc > 0L) verCode = vc
                        val minApi = json.optString("min_sdk_version").toIntOrNull()
                        if (minApi != null && minApi > 0) minSdk = minApi
                    }
                    // Native libs inside a single APK
                    lower.startsWith("lib/") -> {
                        val parts = entryName.split('/')
                        if (parts.size >= 3 && parts[1].isNotBlank()) {
                            abis.add(parts[1])
                        }
                    }
                    // Split APK filenames like split_config.arm64_v8a.apk
                    lower.endsWith(".apk") -> {
                        when {
                            "arm64_v8a" in lower || "arm64-v8a" in lower -> abis.add("arm64-v8a")
                            "armeabi_v7a" in lower || "armeabi-v7a" in lower -> abis.add("armeabi-v7a")
                            "x86_64" in lower -> abis.add("x86_64")
                            "x86" in lower -> abis.add("x86")
                        }
                        if (verName == null && (
                                lower == "base.apk" ||
                                    lower == "$pkg.apk" ||
                                    lower.endsWith("base-master.apk")
                                )
                        ) {
                            val tmp = File.createTempFile("inspect_", ".apk", context.cacheDir)
                            try {
                                tmp.outputStream().use { out -> zis.copyTo(out) }
                                val meta = context.readDownloadedApkMetadata(tmp)
                                pkg = meta?.packageName ?: pkg
                                verName = meta?.versionName ?: verName
                                verCode = meta?.versionCode ?: verCode
                                minSdk = meta?.minSdkVersion ?: minSdk
                            } finally {
                                tmp.delete()
                            }
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        ResolvedArchiveDetails(
            packageName = pkg,
            appName = label,
            versionName = verName,
            versionCode = verCode,
            minSdkVersion = minSdk,
            abis = abis.toList()
        )
    }.getOrDefault(ResolvedArchiveDetails())
}

private fun extractAbisFromApkFile(file: File): List<String> = runCatching {
    val abis = LinkedHashSet<String>()
    java.util.zip.ZipFile(file).use { zip ->
        val entries = zip.entries()
        while (entries.hasMoreElements()) {
            val name = entries.nextElement().name
            if (name.startsWith("lib/")) {
                val parts = name.split('/')
                if (parts.size >= 3 && parts[1].isNotBlank()) {
                    abis.add(parts[1])
                }
            }
        }
    }
    abis.toList()
}.getOrDefault(emptyList())

private fun loadAllStoredApkItems(context: Context, settings: HelperSettings): List<StoredApkCardItem> {
    val archiveAppsByPkg = runCatching {
        MorpheArchive.loadCachedIndex(context)?.apps?.associateBy { it.packageName }
    }.getOrNull().orEmpty()
    val seenUris = HashSet<String>()
    val results = mutableListOf<StoredApkCardItem>()

    for (entry in DownloadHistoryStore.entries(context)) {
        if (!entry.fileName.hasSupportedArchiveExtension()) continue
        if (!seenUris.add(entry.uri)) continue
        if (!context.isHistoryUriUsable(entry.uri)) continue
        val uri = Uri.parse(entry.uri)
        val size = contentLength(context, uri)
        if (size <= 0L) continue

        val location = entry.storageLocation()
        val fullLabel = when {
            entry.isCached -> "Cached (temporary)"
            location == DownloadLocation.INTERNAL -> "Internal app storage"
            location == DownloadLocation.EXTERNAL -> "Downloads/helper-for-morphe"
            else -> dev.rushi.apkdownloadhelper.storage.customFolderRelativePath(
                context,
                uri,
                settings.downloadCustomTreeUri
            ) ?: "Custom folder"
        }
        val shortLabel = when {
            entry.isCached -> "Cached"
            location == DownloadLocation.INTERNAL -> "Internal storage"
            location == DownloadLocation.EXTERNAL -> "Downloads folder"
            else -> "Custom folder"
        }

        val saved = entry.details
        val parsedMeta = if (saved != null) {
            ResolvedArchiveDetails(
                packageName = saved.packageName,
                appName = saved.appName,
                versionName = saved.versionName,
                versionCode = saved.versionCode,
                minSdkVersion = saved.minSdkVersion,
                abis = saved.abis.orEmpty()
            )
        } else {
            inspectArchiveUri(context, uri, entry.fileName, null)
        }
        val pkg = entry.packageName.takeIf { it.isNotBlank() }
            ?: parsedMeta.packageName?.takeIf { it.isNotBlank() }
            ?: entry.fileName.substringBefore('-').substringBefore('_').substringBeforeLast('.')
        val baseArchive = archiveAppsByPkg[pkg]
        val resolvedName = entry.appName.takeIf { it.isNotBlank() }
            ?: parsedMeta.appName?.takeIf { it.isNotBlank() }
            ?: baseArchive?.name?.takeIf { it.isNotBlank() }
            ?: pkg
        val installedPkgInfo = runCatching { context.packageManager.getPackageInfo(pkg, 0) }.getOrNull()
        val version = entry.versionName?.takeIf { it.isNotBlank() }
            ?: parsedMeta.versionName?.takeIf { it.isNotBlank() }
            ?: installedPkgInfo?.versionName?.takeIf { it.isNotBlank() }
        val extKind = entry.fileName.substringAfterLast('.', "").lowercase(Locale.US)
        val kind = extKind.takeIf { it in SUPPORTED_ARCHIVE_EXTENSIONS }
            ?: entry.fileKind.takeIf { it.isNotBlank() && !it.equals("web", ignoreCase = true) }
            ?: "apk"
        val app = baseArchive?.copy(name = resolvedName) ?: ArchiveApp(
            packageName = pkg,
            name = resolvedName,
            versions = listOfNotNull(version)
        )

        results.add(
            StoredApkCardItem(
                id = entry.uri,
                uri = uri,
                fileName = entry.fileName,
                fileKind = kind,
                versionName = version,
                versionCode = parsedMeta.versionCode,
                sourceName = entry.sourceName,
                locationLabel = fullLabel,
                sizeBytes = size,
                timestamp = entry.timestamp,
                app = app,
                installed = installedPkgInfo != null,
                subtitleText = listOf(
                    formatHistoryTimestamp(entry.timestamp),
                    shortLabel,
                    size.formatBytes()
                ).joinToString(" \u00b7 "),
                abis = parsedMeta.abis,
                minSdkVersion = parsedMeta.minSdkVersion,
                obbSummary = entry.obbContents?.summary,
                scanVerdict = entry.scanVerdict,
                isPrivate = location == DownloadLocation.INTERNAL
            )
        )
    }
    return results.sortedByDescending { it.timestamp }
}

internal fun deleteStoredApkFromEverywhere(context: Context, item: StoredApkCardItem): Boolean {
    // Delete only the exact selected URI so identical files in other storage
    // locations (or duplicate downloads in the same location) remain untouched.
    runCatching {
        if (item.uri.scheme == "content") {
            context.contentResolver.delete(item.uri, null, null)
        } else {
            item.uri.path?.let { File(it).delete() }
        }
    }
    if (uriExists(context, item.uri)) {
        runCatching {
            DocumentFile.fromSingleUri(context, item.uri)?.delete()
        }
    }
    runCatching { context.untrackCustomDownload(item.uri) }

    // Report success only when the selected file is actually gone.
    return !uriExists(context, item.uri)
}

private fun contentLength(context: Context, uri: Uri): Long = runCatching {
    when {
        uri.scheme == "file" -> uri.path?.let { File(it).length() } ?: 0L
        else -> context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
    }
}.getOrDefault(0L)

private fun uriExists(context: Context, uri: Uri): Boolean {
    if (uri.scheme == "file") return uri.path?.let { File(it).exists() } == true
    // FileProvider answers metadata queries for deleted files, so DocumentFile.exists()
    // reported "still there". Opening the stream fails for a missing file.
    return runCatching {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
    }.getOrDefault(false)
}

//==================================================
// Normal App Open
//==================================================

@Composable
internal fun HelperStandaloneContent(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit,
    onOpenMorphe: () -> Unit,
    onOpenTracking: () -> Unit = {},
    hideTracking: Boolean = false,
    onOpenStoredItem: (StoredApkCardItem) -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val headerHaze = remember { HazeState() }
    var headerHeightDp by remember { mutableStateOf(0.dp) }
    val isScrolled by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    val historyRevision = DownloadHistoryStore.revision
    var storedItems by remember { mutableStateOf<List<StoredApkCardItem>?>(null) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteSelectedConfirm by remember { mutableStateOf(false) }

    val selectionMode = selectedIds.isNotEmpty()

    BackHandler(enabled = selectionMode) {
        selectedIds = emptySet()
    }

    LaunchedEffect(
        historyRevision,
        settings.downloadCustomTreeUri,
        settings.downloadLocation
    ) {
        val loaded = withContext(Dispatchers.IO) {
            // Apply the Keep downloads timer to internal, Downloads and custom copies first.
            runCatching { context.cleanupDownloads(settings) }
            loadAllStoredApkItems(context, settings)
        }
        storedItems = loaded
        val validIds = loaded.map { it.id }.toSet()
        selectedIds = selectedIds.intersect(validIds)
    }

    fun toggleSelection(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    fun shareSelectedItems() {
        val items = storedItems.orEmpty().filter { it.id in selectedIds }
        if (items.isEmpty()) return
        if (items.size == 1) {
            val item = items.single()
            val mime = if (item.fileName.endsWith(".apk", ignoreCase = true)) {
                "application/vnd.android.package-archive"
            } else {
                "application/octet-stream"
            }
            val share = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, item.uri)
                clipData = ClipData.newUri(context.contentResolver, item.fileName, item.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching {
                context.startActivity(Intent.createChooser(share, "Share ${item.fileName}"))
            }
        } else {
            val uris = ArrayList(items.map { it.uri })
            val share = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "application/octet-stream"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching {
                context.startActivity(Intent.createChooser(share, "Share ${items.size} files"))
            }
        }
        selectedIds = emptySet()
    }

    fun deleteSelectedItems() {
        val toDelete = storedItems.orEmpty().filter { it.id in selectedIds }
        if (toDelete.isEmpty()) return
        selectedIds = emptySet()
        scope.launch {
            val results = withContext(Dispatchers.IO) {
                toDelete.map { item -> item to deleteStoredApkFromEverywhere(context, item) }
            }
            val deleted = results.filter { it.second }.map { it.first }
            val failed = results.filterNot { it.second }.map { it.first }
            if (deleted.isNotEmpty()) {
                DownloadHistoryStore.removeByUris(
                    context = context,
                    uris = deleted.map { it.id }.toSet()
                )
            }
            storedItems = storedItems?.filterNot { target -> deleted.any { it.id == target.id } }
            val message = when {
                failed.isEmpty() && deleted.size == 1 -> "Deleted ${deleted.single().fileName}"
                failed.isEmpty() -> "Deleted ${deleted.size} files"
                deleted.isEmpty() && failed.size == 1 -> "Could not delete ${failed.single().fileName}"
                deleted.isEmpty() -> "Could not delete ${failed.size} files"
                else -> "Deleted ${deleted.size} files. Could not delete ${failed.size}."
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    if (showDeleteSelectedConfirm) {
        val count = selectedIds.size
        val singleName = storedItems.orEmpty().firstOrNull { it.id in selectedIds }?.fileName
        AppAlertDialog(
            onDismissRequest = { showDeleteSelectedConfirm = false },
            title = {
                Text(
                    text = if (count == 1) "Delete file?" else "Delete $count files?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (count == 1 && singleName != null) {
                        "Delete $singleName from storage?"
                    } else {
                        "Delete $count selected files from storage?"
                    }
                )
            },
            confirmButton = {
                DialogButton(
                    text = "Delete",
                    onClick = {
                        showDeleteSelectedConfirm = false
                        deleteSelectedItems()
                    },
                    isDestructive = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            dismissButton = {
                DialogButton(
                    text = "Cancel",
                    onClick = { showDeleteSelectedConfirm = false },
                    filled = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val items = storedItems
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceIfEnabled(headerHaze),
            contentPadding = PaddingValues(
                top = headerHeightDp + 10.dp,
                start = AppDefaults.ContentPadding,
                end = AppDefaults.ContentPadding,
                bottom = 160.dp
            ),
            verticalArrangement = Arrangement.spacedBy(
                if (items == null || items.isNotEmpty()) 3.dp else AppDefaults.ItemSpacing
            )
        ) {
            if (items == null) {
                val skeletonCount = 5
                items(skeletonCount, key = { "stored_skeleton_$it" }) { index ->
                    val isFirst = index == 0
                    val isLast = index == skeletonCount - 1
                    val outerRadius = 22.dp
                    val innerRadius = 10.dp
                    val cardShape = RoundedCornerShape(
                        topStart = if (isFirst) outerRadius else innerRadius,
                        topEnd = if (isFirst) outerRadius else innerRadius,
                        bottomStart = if (isLast) outerRadius else innerRadius,
                        bottomEnd = if (isLast) outerRadius else innerRadius
                    )
                    FindAppCardLoading(
                        index = index,
                        shape = cardShape,
                        useGradient = settings.useGradientCardColors
                    )
                }
            } else if (items.isEmpty()) {
                item(key = "empty_launch") {
                    EmptyLaunchState(onOpenMorphe = onOpenMorphe)
                }
            } else {
                itemsIndexed(
                    items = items,
                    key = { _, item -> item.id }
                ) { index, item ->
                    val isFirst = index == 0
                    val isLast = index == items.lastIndex
                    val outerRadius = 22.dp
                    val innerRadius = 10.dp
                    val cardShape = RoundedCornerShape(
                        topStart = if (isFirst) outerRadius else innerRadius,
                        topEnd = if (isFirst) outerRadius else innerRadius,
                        bottomStart = if (isLast) outerRadius else innerRadius,
                        bottomEnd = if (isLast) outerRadius else innerRadius
                    )
                    val isSelected = item.id in selectedIds

                    AppListCard(
                        app = item.app,
                        installed = item.installed,
                        favourite = false,
                        shape = cardShape,
                        useGradient = settings.useGradientCardColors,
                        customBottomText = item.subtitleText,
                        customSubtitleText = item.versionName?.let {
                            if (it.startsWith("v", ignoreCase = true)) it else "v$it"
                        },
                        selected = isSelected,
                        selectionMode = selectionMode,
                        onToggleFavourite = {},
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            toggleSelection(item.id)
                        },
                        onClick = {
                            if (selectionMode) {
                                toggleSelection(item.id)
                            } else {
                                onOpenStoredItem(item)
                            }
                        },
                        modifier = Modifier.animatedListItem(this)
                    )
                }
            }
        }

        HelperFloatingHeader(
            settings = settings,
            onSettingsChange = onSettingsChange,
            onOpenTracking = onOpenTracking,
            hideTracking = hideTracking,
            isScrolled = isScrolled,
            headerHaze = headerHaze,
            selectedCount = selectedIds.size,
            totalCount = items?.size ?: 0,
            onClearSelection = { selectedIds = emptySet() },
            onSelectAll = {
                val allIds = items.orEmpty().map { it.id }.toSet()
                selectedIds = if (selectedIds.size == allIds.size) emptySet() else allIds
            },
            onShareSelected = ::shareSelectedItems,
            onDeleteSelected = { showDeleteSelectedConfirm = true },
            onHeaderHeightChanged = { headerHeightDp = it },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun HelperFloatingHeader(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit,
    onOpenTracking: () -> Unit,
    hideTracking: Boolean,
    isScrolled: Boolean,
    headerHaze: HazeState,
    selectedCount: Int,
    totalCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onShareSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onHeaderHeightChanged: (Dp) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val headerShape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
    val selectionMode = selectedCount > 0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                onHeaderHeightChanged(with(density) { size.height.toDp() })
            }
            .then(
                if (isScrolled || selectionMode) {
                    Modifier.glassFill(
                        glass = AppColors.glassContainerColor(),
                        shape = headerShape,
                        hazeState = headerHaze
                    )
                } else {
                    Modifier
                }
            )
            .statusBarsPadding()
            .padding(
                horizontal = AppDefaults.ContentPadding,
                vertical = 12.dp
            )
    ) {
        AnimatedContent(
            targetState = selectionMode,
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
            label = "helper_header_selection"
        ) { isSelecting ->
            if (isSelecting) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassHeaderButton(
                        icon = Icons.Outlined.Close,
                        contentDescription = "Clear selection",
                        onClick = onClearSelection
                    )
                    Text(
                        text = "$selectedCount selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (totalCount > 1) {
                        GlassHeaderButton(
                            icon = Icons.Outlined.SelectAll,
                            contentDescription = "Select all",
                            active = selectedCount == totalCount,
                            onClick = onSelectAll
                        )
                    }
                    // The two requested header actions for selected cards: Share & Delete
                    GlassHeaderButton(
                        icon = Icons.Outlined.Share,
                        contentDescription = "Share selected",
                        onClick = onShareSelected
                    )
                    GlassHeaderButton(
                        icon = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete selected",
                        onClick = onDeleteSelected
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        AppWordmark(
                            modifier = Modifier.height(26.dp),
                            textColor = MaterialTheme.colorScheme.onBackground
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.helper),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }

                    if (!hideTracking) {
                        GlassHeaderButton(
                            icon = Icons.Outlined.Notifications,
                            contentDescription = "Tracking",
                            onClick = onOpenTracking
                        )
                    }

                    HelperGlassBoltButton(
                        fastMode = settings.fastMode,
                        onClick = {
                            val enabled = !settings.fastMode
                            onSettingsChange(settings.copy(fastMode = enabled))
                            Toast.makeText(
                                context,
                                if (enabled) "Fast Mode on" else "Fast Mode off",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HelperGlassBoltButton(
    fastMode: Boolean,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val idleAlpha = if (isDark) 0.35f else 0.6f
    val activeAlpha = if (isDark) 0.55f else 0.72f
    val baseContainer = if (fastMode) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = activeAlpha)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = idleAlpha)
    }
    val containerColor by animateColorAsState(
        targetValue = if (pressed) {
            baseContainer.copy(alpha = (baseContainer.alpha * 0.75f).coerceAtLeast(0.18f))
        } else {
            baseContainer
        },
        animationSpec = tween(200),
        label = "helper_bolt_container"
    )
    val contentColor by animateColorAsState(
        targetValue = if (fastMode) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "helper_bolt_content"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .pressScale(interactionSource, restCorner = 20.dp, pressedCorner = 10.dp, pressedScale = 0.90f),
        shape = rememberMorphePillShape(interactionSource),
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.Bolt,
                contentDescription = if (fastMode) "Fast Mode on" else "Fast Mode off",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}