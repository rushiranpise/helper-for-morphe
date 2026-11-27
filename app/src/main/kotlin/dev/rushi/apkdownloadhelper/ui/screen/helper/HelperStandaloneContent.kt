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
import dev.rushi.apkdownloadhelper.storage.temporaryDownloadsDir
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

    // 2. Content URI or split archive (.apkm, .apks, .xapk) via stream
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
                        if (verName == null && (lower == "base.apk" || lower == "$pkg.apk")) {
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

private fun loadAllStoredApkItems(context: Context, settings: HelperSettings): List<StoredApkCardItem> {    val archiveAppsByPkg = runCatching {
        MorpheArchive.loadCachedIndex(context)?.apps?.associateBy { it.packageName }
    }.getOrNull().orEmpty()
    val installedCache = mutableMapOf<String, Boolean>()
    val historyEntries = DownloadHistoryStore.entries(context)
    val historyByUri = historyEntries.associateBy { it.uri }
    val historyByName = historyEntries.associateBy { it.fileName }

    val seenUris = LinkedHashSet<String>()
    val seenNames = HashSet<String>()
    val results = mutableListOf<StoredApkCardItem>()

    fun isInstalled(pkg: String): Boolean =
        installedCache.getOrPut(pkg) {
            runCatching {
                context.packageManager.getPackageInfo(pkg, 0) != null
            }.getOrDefault(false)
        }

    fun addCandidate(
        uri: Uri,
        fileName: String,
        sizeBytes: Long,
        timestamp: Long,
        locationLabel: String,
        localFile: File? = null
    ) {
        val uriKey = uri.toString()
        if (!fileName.hasSupportedArchiveExtension()) return
        if (fileName.endsWith(".part", ignoreCase = true)) return
        if (!seenUris.add(uriKey)) return
        // Avoid showing duplicate copies of the exact same downloaded file across cache & Downloads
        if (!seenNames.add(fileName.lowercase(Locale.US))) return

        val history = historyByUri[uriKey] ?: historyByName[fileName]
        // Saved at download time. Only older history entries fall back to inspecting the file.
        val saved = history?.details
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
            inspectArchiveUri(context, uri, fileName, localFile)
        }
        val isPrivate = localFile?.absolutePath?.let { p ->
            p.startsWith(context.cacheDir.absolutePath) ||
                p.startsWith(context.filesDir.absolutePath) ||
                context.getExternalFilesDir(null)?.absolutePath?.let { p.startsWith(it) } == true
        } ?: (uri.authority == "${BuildConfig.APPLICATION_ID}.files")

        val pkg = history?.packageName?.takeIf { it.isNotBlank() }
            ?: parsedMeta.packageName?.takeIf { it.isNotBlank() }
            ?: fileName.substringBefore('-').substringBefore('_').substringBeforeLast('.')
        val baseArchive = archiveAppsByPkg[pkg]
        val resolvedName = history?.appName?.takeIf { it.isNotBlank() }
            ?: parsedMeta.appName?.takeIf { it.isNotBlank() }
            ?: baseArchive?.name?.takeIf { it.isNotBlank() }
            ?: pkg
        val installedPkgInfo = runCatching { context.packageManager.getPackageInfo(pkg, 0) }.getOrNull()
        val version = history?.versionName?.takeIf { it.isNotBlank() }
            ?: parsedMeta.versionName?.takeIf { it.isNotBlank() }
            ?: installedPkgInfo?.versionName?.takeIf { it.isNotBlank() }
        val sourceOrLocation = history?.sourceName?.takeIf { it.isNotBlank() } ?: locationLabel
        val storageDisplay = when {
            uriKey.contains("/cache/", ignoreCase = true) || locationLabel.equals("Cache", ignoreCase = true) -> "App Cache"
            uriKey.contains("media/external/downloads", ignoreCase = true) || locationLabel.equals("Downloads", ignoreCase = true) -> "Downloads / Morphe Helper"
            uriKey.startsWith("content://com.android.externalstorage", ignoreCase = true) || locationLabel.equals("Custom", ignoreCase = true) -> "Custom Folder"
            else -> locationLabel
        }
        val effectiveTimestamp = history?.timestamp ?: timestamp
        val extKind = fileName.substringAfterLast('.', "").lowercase(Locale.US)
        val kind = extKind.takeIf { it in SUPPORTED_ARCHIVE_EXTENSIONS }
            ?: history?.fileKind?.takeIf { it.isNotBlank() && !it.equals("web", ignoreCase = true) }
            ?: "apk"

        val app = baseArchive?.copy(name = resolvedName) ?: ArchiveApp(
            packageName = pkg,
            name = resolvedName,
            versions = listOfNotNull(version)
        )

        val subtitle = listOfNotNull(
            formatHistoryTimestamp(effectiveTimestamp),
            sourceOrLocation,
            sizeBytes.takeIf { it > 0L }?.formatBytes()
        ).joinToString(" · ")

        results.add(
            StoredApkCardItem(
                id = uriKey,
                uri = uri,
                fileName = fileName,
                fileKind = kind,
                versionName = version,
                versionCode = parsedMeta.versionCode,
                sourceName = sourceOrLocation,
                locationLabel = storageDisplay,
                sizeBytes = sizeBytes,
                timestamp = effectiveTimestamp,
                app = app,
                installed = installedPkgInfo != null,
                subtitleText = subtitle,
                abis = parsedMeta.abis,
                minSdkVersion = parsedMeta.minSdkVersion,
                obbSummary = history?.obbContents?.summary,
                scanVerdict = history?.scanVerdict,
                isPrivate = isPrivate
            )
        )
    }

    // 1. Usable entries recorded in DownloadHistoryStore
    for (entry in historyEntries) {
        if (!context.isHistoryUriUsable(entry.uri)) continue
        val uri = Uri.parse(entry.uri)
        val localFile = if (entry.uri.startsWith("file://")) {
            uri.path?.let(::File)
        } else null
        val size = localFile?.length() ?: runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
        }.getOrDefault(0L)
        addCandidate(
            uri = uri,
            fileName = entry.fileName,
            sizeBytes = size,
            timestamp = entry.timestamp,
            locationLabel = entry.sourceName,
            localFile = localFile
        )
    }

    // 2. Internal cache downloads directory
    context.temporaryDownloadsDir().listFiles()?.forEach { file ->
        if (file.isFile && file.length() > 0L && file.name.hasSupportedArchiveExtension()) {
            val uri = runCatching {
                FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.files", file)
            }.getOrNull() ?: Uri.fromFile(file)
            addCandidate(
                uri = uri,
                fileName = file.name,
                sizeBytes = file.length(),
                timestamp = file.lastModified(),
                locationLabel = "Cache",
                localFile = file
            )
        }
    }

    // 3. External app files/downloads directory
    context.externalDownloadsDir()?.listFiles()?.forEach { file ->
        if (file.isFile && file.length() > 0L && file.name.hasSupportedArchiveExtension()) {
            val uri = runCatching {
                FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.files", file)
            }.getOrNull() ?: Uri.fromFile(file)
            addCandidate(
                uri = uri,
                fileName = file.name,
                sizeBytes = file.length(),
                timestamp = file.lastModified(),
                locationLabel = "External",
                localFile = file
            )
        }
    }

    // 4. Public Downloads/Morphe Helper directory
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        runCatching {
            val projection = arrayOf(
                MediaStore.Downloads._ID,
                MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.SIZE,
                MediaStore.Downloads.DATE_MODIFIED
            )
            context.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Downloads.RELATIVE_PATH} LIKE ?",
                arrayOf("${downloadsCopyRelativePath()}%"),
                "${MediaStore.Downloads.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: continue
                    val size = cursor.getLong(sizeCol)
                    val modifiedMs = cursor.getLong(dateCol) * 1000L
                    val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                    addCandidate(
                        uri = uri,
                        fileName = name,
                        sizeBytes = size,
                        timestamp = modifiedMs,
                        locationLabel = "Downloads"
                    )
                }
            }
        }
    } else {
        @Suppress("DEPRECATION")
        val legacyDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Morphe Helper"
        )
        legacyDir.listFiles()?.forEach { file ->
            if (file.isFile && file.length() > 0L && file.name.hasSupportedArchiveExtension()) {
                val uri = runCatching {
                    FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.files", file)
                }.getOrNull() ?: Uri.fromFile(file)
                addCandidate(
                    uri = uri,
                    fileName = file.name,
                    sizeBytes = file.length(),
                    timestamp = file.lastModified(),
                    locationLabel = "Downloads",
                    localFile = file
                )
            }
        }
    }

    // 5. Custom SAF folder (if configured)
    val customTree = settings.downloadCustomTreeUri
    if (customTree.isNotBlank()) {
        runCatching {
            val root = DocumentFile.fromTreeUri(context, Uri.parse(customTree))
            root?.listFiles()?.forEach { doc ->
                val name = doc.name ?: return@forEach
                if (doc.isFile && doc.exists() && name.hasSupportedArchiveExtension()) {
                    addCandidate(
                        uri = doc.uri,
                        fileName = name,
                        sizeBytes = doc.length().coerceAtLeast(0L),
                        timestamp = doc.lastModified(),
                        locationLabel = "Custom"
                    )
                }
            }
        }
    }

    return results.sortedByDescending { it.timestamp }
}

internal fun deleteStoredApkFromEverywhere(context: Context, item: StoredApkCardItem) {
    // 1. Delete via ContentResolver or direct File path
    runCatching {
        if (item.uri.scheme == "content") {
            context.contentResolver.delete(item.uri, null, null)
        } else {
            item.uri.path?.let { File(it).delete() }
        }
    }
    // 2. Also remove any matching filename copies in cache/external/legacy Downloads so no orphan remains
    runCatching { File(context.temporaryDownloadsDir(), item.fileName).delete() }
    runCatching { context.externalDownloadsDir()?.let { File(it, item.fileName).delete() } }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        runCatching {
            context.contentResolver.delete(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                "${MediaStore.Downloads.RELATIVE_PATH} LIKE ? AND ${MediaStore.Downloads.DISPLAY_NAME}=?",
                arrayOf("${downloadsCopyRelativePath()}%", item.fileName)
            )
        }
    }
    runCatching {
        DocumentFile.fromSingleUri(context, item.uri)?.delete()
    }
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

    LaunchedEffect(historyRevision, settings.downloadCustomTreeUri, settings.downloadLocation) {
        val loaded = withContext(Dispatchers.IO) {
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
            withContext(Dispatchers.IO) {
                toDelete.forEach { deleteStoredApkFromEverywhere(context, it) }
            }
            DownloadHistoryStore.removeByUris(
                context = context,
                uris = toDelete.map { it.id }.toSet(),
                fileNames = toDelete.map { it.fileName }.toSet()
            )
            storedItems = storedItems?.filterNot { target -> toDelete.any { it.id == target.id } }
            Toast.makeText(
                context,
                if (toDelete.size == 1) "Deleted ${toDelete.single().fileName}" else "Deleted ${toDelete.size} files",
                Toast.LENGTH_SHORT
            ).show()
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