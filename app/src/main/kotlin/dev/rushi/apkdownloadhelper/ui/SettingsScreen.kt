package dev.rushi.apkdownloadhelper.ui

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


@Composable
internal fun HistoryEntryCard(
    entry: DownloadHistoryEntry,
    usable: Boolean,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    var showScanResult by remember { mutableStateOf(false) }
    SurfaceCard(cornerRadius = MorpheDefaults.CompactCornerRadius) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MorpheDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = entry.appName,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = entry.packageName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatHistoryTimestamp(entry.timestamp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = buildString {
                    entry.versionName?.let { append(it).append(" · ") }
                    append(entry.sourceName)
                    append(" · ")
                    append(entry.fileName)
                    if (!entry.fileKind.equals("web", ignoreCase = true)) {
                        append(" · ")
                        append(entry.fileKind.uppercase(Locale.US))
                    }
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            // A game that refuses to start after patching is usually missing this, so
            // say it was inside the file and where Android expects it.
            entry.obbContents?.let { obb ->
                Text(
                    text = "Includes game data (${obb.summary}). Install it to " +
                        "Android/obb/${entry.packageName} if the app will not start.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            entry.scanVerdict?.let { verdict ->
                val verdictTone = when {
                    verdict.malicious -> SemanticTone.Error
                    verdict.failed -> SemanticTone.Warning
                    else -> SemanticTone.Success
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (verdict.result != null) {
                            Modifier.clickable { showScanResult = true }
                        } else {
                            Modifier
                        }),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ContentPaddingSmall)
                ) {
                    ThemedIcon(
                        icon = when {
                            verdict.malicious -> Icons.Outlined.Warning
                            verdict.failed -> Icons.Outlined.HelpOutline
                            else -> Icons.Outlined.CheckCircle
                        },
                        size = 16.dp,
                        tint = verdictTone.accent
                    )
                    Text(
                        text = verdict.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (verdict.malicious) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    verdict.result?.let { result ->
                        MorpheStatusBadge(
                            text = if (result.cached) "Cached report" else "Fresh scan",
                            tone = if (result.cached) SemanticTone.Primary else SemanticTone.Neutral
                        )
                        ForwardChevronIcon(size = 16.dp)
                    }
                }
            }
            if (usable) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
                ) {
                    HelperButton(
                        text = "Share",
                        onClick = onShare,
                        icon = Icons.Outlined.Share,
                        modifier = Modifier.weight(1f)
                    )
                    HelperOutlinedButton(
                        text = "Open",
                        onClick = onOpen,
                        icon = Icons.Outlined.FolderOpen,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.file_no_longer_available_temporary_hand),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    val savedResult = entry.scanVerdict?.result
    if (showScanResult && savedResult != null) {
        MorpheDialog(
            title = stringResource(R.string.scan_result),
            onDismiss = { showScanResult = false },
            actions = {
                HelperButton(
                    text = "Close",
                    onClick = { showScanResult = false },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                ScanResultCard(
                    scanResult = savedResult,
                    detail = scanResultDetail(savedResult),
                    isMalicious = savedResult is VirusTotalScanner.ScanResult.Malicious,
                    onProceed = {},
                    onCancel = {},
                    readOnly = true
                )
            }
        }
    }
}

@Composable
internal fun HelperSettingsScreen(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit,
    onOpenUpdates: () -> Unit,
    logs: List<RequestLogEntry>,
    onClearLogs: () -> Unit,
    historyEntries: List<DownloadHistoryEntry>,
    onOpenHistoryEntry: (DownloadHistoryEntry) -> Unit,
    onShareHistoryEntry: (DownloadHistoryEntry) -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val swipeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }
    var tab by rememberSaveable { mutableStateOf(SettingsTab.SYSTEM) }
    var showSources by rememberSaveable { mutableStateOf(false) }
    if (showSources) {
        SourcesSettingsScreen(
            settings = settings,
            onSettingsChange = onSettingsChange,
            onBack = { showSources = false }
        )
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = MorpheDefaults.ContentPadding, vertical = MorpheDefaults.ContentPadding)
            .pointerInput(swipeThresholdPx) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        if (totalDrag >= swipeThresholdPx) onBack()
                    },
                    onDragCancel = { totalDrag = 0f }
                )
            },
        verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
    ) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                HelperHeaderIconButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
        }

        item {
            SettingsTabRow(
                selected = tab,
                onSelect = { tab = it }
            )
        }

        when (tab) {
            SettingsTab.APPEARANCE -> item {
                AppearanceTabContent(
                    settings = settings,
                    onSettingsChange = onSettingsChange
                )
            }

            SettingsTab.SYSTEM -> item {
                SystemTabContent(
                    settings = settings,
                    onSettingsChange = onSettingsChange,
                    onOpenUpdates = onOpenUpdates,
                    onOpenSources = { showSources = true }
                )
            }

            SettingsTab.ADVANCED -> item {
                AdvancedTabContent(
                    settings = settings,
                    onSettingsChange = onSettingsChange
                )
            }

            SettingsTab.LOGS -> item {
                LogsTabContent(
                    settings = settings,
                    onSettingsChange = onSettingsChange,
                    logs = logs,
                    onClearLogs = onClearLogs,
                    historyEntries = historyEntries,
                    onOpenHistoryEntry = onOpenHistoryEntry,
                    onShareHistoryEntry = onShareHistoryEntry,
                    onClearHistory = onClearHistory
                )
            }
        }
    }
}

@Composable
internal fun SystemTabContent(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit,
    onOpenUpdates: () -> Unit,
    onOpenSources: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheBytes by remember(context) { mutableStateOf(context.temporaryDownloadsSize()) }
    var downloadsBytes by remember(context) { mutableStateOf(context.downloadsCopySize()) }
    var externalBytes by remember(context) { mutableStateOf(context.externalDownloadsSize()) }
    var customBytes by remember(context) { mutableStateOf(context.customDownloadsSize()) }
    var locationDialog by remember { mutableStateOf(false) }
    var retentionDialog by remember { mutableStateOf(false) }
    var fastModePolicyDialog by remember { mutableStateOf(false) }
    var pendingLocation by remember { mutableStateOf<DownloadLocation?>(null) }
    val latestSettings by rememberUpdatedState(settings)

    val locations = DownloadLocation.entries
    val retentions = DownloadRetention.entries
    val fastPolicies = FastModePolicy.entries

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data ?: return@rememberLauncherForActivityResult
        val uri = data.data ?: return@rememberLauncherForActivityResult
        val requestedFlags = data.flags and (
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        val flags = requestedFlags.takeIf { it != 0 }
            ?: (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        val persisted = runCatching {
            context.contentResolver.takePersistableUriPermission(uri, flags)
        }.isSuccess
        val writable = runCatching {
            DocumentFile.fromTreeUri(context, uri)?.canWrite() == true
        }.getOrDefault(false)
        if (!persisted || !writable) {
            Toast.makeText(
                context,
                "Helper could not keep write access to that folder.",
                Toast.LENGTH_LONG
            ).show()
            return@rememberLauncherForActivityResult
        }
        onSettingsChange(
            latestSettings.copy(
                downloadLocation = DownloadLocation.CUSTOM,
                downloadCustomTreeUri = uri.toString()
            )
        )
    }

    val storagePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val target = pendingLocation
        pendingLocation = null
        if (granted && target != null) {
            onSettingsChange(latestSettings.copy(downloadLocation = target))
        } else if (!granted) {
            Toast.makeText(
                context,
                "Storage permission is needed to save in Downloads on this Android version.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val folderPickerIntent = remember {
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
    }

    fun chooseLocation(location: DownloadLocation) {
        when {
            location == DownloadLocation.CUSTOM -> {
                runCatching { folderPicker.launch(folderPickerIntent) }
                    .onFailure {
                        Toast.makeText(
                            context,
                            "No system folder picker is available.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            location == DownloadLocation.DOWNLOADS &&
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED -> {
                pendingLocation = location
                storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            else -> onSettingsChange(latestSettings.copy(downloadLocation = location))
        }
    }

    if (locationDialog) {
        SettingsChoiceDialog(
            title = stringResource(R.string.save_downloads),
            choices = locations.map { SettingsChoice(it.icon(), it.title, it.description) },
            selectedIndex = locations.indexOf(latestSettings.downloadLocation),
            onSelect = { index ->
                locationDialog = false
                chooseLocation(locations[index])
            },
            onDismiss = { locationDialog = false }
        )
    }

    if (retentionDialog) {
        SettingsChoiceDialog(
            title = "Keep downloads",
            choices = retentions.map { SettingsChoice(it.icon(), it.title, it.description) },
            selectedIndex = retentions.indexOf(latestSettings.downloadRetention),
            onSelect = { index ->
                onSettingsChange(latestSettings.copy(downloadRetention = retentions[index]))
                retentionDialog = false
            },
            onDismiss = { retentionDialog = false }
        )
    }

    if (fastModePolicyDialog) {
        SettingsChoiceDialog(
            title = stringResource(R.string.which_version_to_fetch),
            choices = fastPolicies.map { policy ->
                SettingsChoice(
                    icon = when (policy) {
                        FastModePolicy.REQUESTED -> Icons.Outlined.Tune
                        FastModePolicy.LATEST -> Icons.Outlined.TrendingUp
                        FastModePolicy.ALWAYS_ASK -> Icons.Outlined.HelpOutline
                    },
                    title = policy.title,
                    description = policy.description
                )
            },
            selectedIndex = fastPolicies.indexOf(settings.fastModePolicy),
            onSelect = { index ->
                onSettingsChange(settings.copy(fastModePolicy = fastPolicies[index]))
                fastModePolicyDialog = false
            },
            onDismiss = { fastModePolicyDialog = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ContentPaddingMedium)
    ) {
        SettingsGroup(title = stringResource(R.string.downloads_storage), icon = Icons.Outlined.SdStorage) {
            SettingsRow(
                icon = settings.downloadLocation.icon(),
                title = stringResource(R.string.save_downloads),
                subtitle = settings.downloadLocation.description,
                value = downloadLocationLabel(context, settings),
                onClick = { locationDialog = true }
            )
            MorpheDivider()
            SettingsRow(
                icon = Icons.Outlined.CleaningServices,
                title = stringResource(R.string.storage_used),
                subtitle = "Cache ${cacheBytes.formatBytes()}  ·  External ${externalBytes.formatBytes()}  " +
                    "·  Downloads ${downloadsBytes.formatBytes()}  ·  Custom ${customBytes.formatBytes()}",
                value = "Clear",
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        context.clearAllDownloads()
                        val cache = context.temporaryDownloadsSize()
                        val external = context.externalDownloadsSize()
                        val downloads = context.downloadsCopySize()
                        val custom = context.customDownloadsSize()
                        withContext(Dispatchers.Main) {
                            cacheBytes = cache
                            externalBytes = external
                            downloadsBytes = downloads
                            customBytes = custom
                        }
                    }
                }
            )
            MorpheDivider()
            SettingsRow(
                icon = settings.downloadRetention.icon(),
                title = "Keep downloads",
                subtitle = "${settings.downloadRetention.description} Cleanup runs when Helper opens.",
                value = settings.downloadRetention.title,
                onClick = { retentionDialog = true }
            )
            MorpheDivider()
            SettingsSwitchItem(
                icon = Icons.Outlined.PhoneAndroid,
                title = "Prefer density-specific APKs",
                subtitle = "Use APKMirror's variant for this screen's density instead of nodpi. " +
                    "Some density rows are split APKs that may not patch or install on their own.",
                checked = settings.preferDensitySpecificVariants,
                onToggle = {
                    onSettingsChange(
                        settings.copy(
                            preferDensitySpecificVariants = !settings.preferDensitySpecificVariants
                        )
                    )
                }
            )
        }

        SettingsGroup(title = stringResource(R.string.sources), icon = Icons.Outlined.Dns) {
            val enabledCount = settings.orderedSources.count { it !in settings.disabledSources }
            SettingsRow(
                icon = Icons.Outlined.Dns,
                title = "Manage sources",
                subtitle = "$enabledCount of ${settings.orderedSources.size} enabled, default " +
                    (settings.preferredSource?.label ?: "Automatic"),
                value = "Manage",
                onClick = onOpenSources
            )
        }

        SettingsGroup(title = stringResource(R.string.fast_mode), icon = Icons.Outlined.Bolt) {
            SettingsSwitchItem(
                icon = Icons.Outlined.Bolt,
                title = stringResource(R.string.fast_mode),
                subtitle = "Auto-fetch a version across sources and return it to Morphe automatically. " +
                    "Format differences are allowed.",
                checked = settings.fastMode,
                onToggle = {
                    onSettingsChange(settings.copy(fastMode = !settings.fastMode))
                }
            )
            if (settings.fastMode) {
                MorpheDivider()
                SettingsRow(
                    icon = Icons.Outlined.Tune,
                    title = stringResource(R.string.which_version_to_fetch),
                    value = settings.fastModePolicy.title,
                    onClick = { fastModePolicyDialog = true }
                )
            }
        }

        SettingsGroup(title = "Updates", icon = Icons.Outlined.Refresh) {
            SettingsRow(
                icon = Icons.Outlined.Refresh,
                title = "Updates",
                subtitle = "Check for a new release, read the changelog, or install through Shizuku.",
                value = "v${BuildConfig.VERSION_NAME}",
                onClick = onOpenUpdates
            )
        }
    }
}

/**
 * Dedicated source management screen. The Settings tab keeps one summary row instead
 * of repeating every provider twice (once to enable it, once to reorder it).
 */
@Composable
internal fun SourcesSettingsScreen(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var sourceDialog by remember { mutableStateOf(false) }
    val latestSettings by rememberUpdatedState(settings)
    val orderedSources = settings.orderedSources

    fun moveSourceOrder(source: DownloadSource, delta: Int) {
        val order = latestSettings.orderedSources.toMutableList()
        val index = order.indexOf(source)
        val target = index + delta
        if (index < 0 || target !in order.indices) return
        order.removeAt(index)
        order.add(target, source)
        onSettingsChange(latestSettings.copy(sourceOrder = order))
    }

    if (sourceDialog) {
        val choices = buildList {
            add(
                SettingsChoice(
                    Icons.Outlined.AutoAwesome,
                    "Automatic",
                    "Use the first enabled source in the custom order"
                )
            )
            addAll(orderedSources.map {
                SettingsChoice(
                    icon = null,
                    title = it.label,
                    description = "Prefer this source when it has the file",
                    source = it
                )
            })
        }
        SettingsChoiceDialog(
            title = stringResource(R.string.default_source),
            choices = choices,
            selectedIndex = settings.preferredSource
                ?.let { source -> orderedSources.indexOf(source) + 1 }
                ?.takeIf { it > 0 }
                ?: 0,
            onSelect = { index ->
                onSettingsChange(
                    settings.copy(
                        preferredSource = if (index == 0) null else orderedSources[index - 1]
                    )
                )
                sourceDialog = false
            },
            onDismiss = { sourceDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(
                horizontal = MorpheDefaults.ContentPadding,
                vertical = MorpheDefaults.ContentPadding
            ),
        verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.sources),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Enable providers and set their priority",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                HelperHeaderIconButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
        }

        item {
            SettingsGroup(
                title = stringResource(R.string.default_source),
                icon = Icons.Outlined.Star
            ) {
                SettingsRow(
                    icon = Icons.Outlined.Star,
                    title = stringResource(R.string.default_source),
                    subtitle = "Used first when a request is resolved",
                    value = settings.preferredSource?.label ?: "Automatic",
                    onClick = { sourceDialog = true }
                )
            }
        }

        item {
            SettingsGroup(title = "Providers", icon = Icons.Outlined.Dns) {
                orderedSources.forEachIndexed { index, source ->
                    if (index > 0) MorpheDivider()
                    SettingsSwitchItem(
                        leading = {
                            SourceAvatar(source = source, size = MorpheDefaults.IconSize)
                        },
                        title = source.label,
                        checked = source !in settings.disabledSources,
                        onToggle = {
                            val enabled = source !in settings.disabledSources
                            onSettingsChange(
                                settings.copy(
                                    disabledSources = if (enabled) {
                                        settings.disabledSources + source
                                    } else {
                                        settings.disabledSources - source
                                    }
                                )
                            )
                        }
                    )
                }
            }
        }

        item {
            SettingsGroup(title = "Priority", icon = Icons.Outlined.SortByAlpha) {
                orderedSources.forEachIndexed { index, source ->
                    if (index > 0) MorpheDivider()
                    SettingsRow(
                        leading = {
                            SourceAvatar(source = source, size = MorpheDefaults.IconSize)
                        },
                        title = source.label,
                        subtitle = when {
                            source in settings.disabledSources -> "Disabled"
                            index == 0 -> "Tried first"
                            else -> null
                        },
                        trailing = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (index > 0) {
                                    HelperIconButton(
                                        icon = Icons.Outlined.ExpandLess,
                                        contentDescription = "Move ${source.label} up",
                                        onClick = { moveSourceOrder(source, -1) }
                                    )
                                } else {
                                    Spacer(Modifier.size(MorpheDefaults.GlassButtonHeight))
                                }
                                if (index < orderedSources.lastIndex) {
                                    HelperIconButton(
                                        icon = Icons.Outlined.ExpandMore,
                                        contentDescription = "Move ${source.label} down",
                                        onClick = { moveSourceOrder(source, 1) }
                                    )
                                } else {
                                    Spacer(Modifier.size(MorpheDefaults.GlassButtonHeight))
                                }
                            }
                        }
                    )
                }
                MorpheDivider()
                SettingsRow(
                    icon = Icons.Outlined.Refresh,
                    title = "Reset source order",
                    subtitle = "Restore the default priority.",
                    value = "Reset",
                    onClick = {
                        onSettingsChange(
                            latestSettings.copy(sourceOrder = DEFAULT_SOURCE_ORDER)
                        )
                    }
                )
            }
        }
    }
}

/**
 * Appearance tab: theme mode, colour style, pure black and the accent colour,
 * mirroring the manager's own appearance tab.
 */
@Composable
internal fun AppearanceTabContent(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit
) {
    var showAccentPicker by rememberSaveable { mutableStateOf(false) }
    var confirmHideIcon by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    // Wallpaper colours need a platform palette, so the option is not offered
    // where it could not work. A stored choice still resolves on the other side.
    val styles = ThemeStyle.entries.filter {
        it != ThemeStyle.MATERIAL_YOU || supportsDynamicColor
    }
    val accent = settings.customAccentColor.toAccentColorOrNull()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ContentPaddingMedium)
    ) {
        SettingsGroup(title = stringResource(R.string.theme), icon = Icons.Outlined.Palette) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                if (index > 0) MorpheDivider()
                SettingsRow(
                    icon = mode.icon(),
                    title = mode.title,
                    subtitle = mode.description,
                    onClick = { onSettingsChange(settings.copy(themeMode = mode)) },
                    trailing = { RadioDot(selected = settings.themeMode == mode) }
                )
            }
        }

        SettingsGroup(title = "App icon", icon = Icons.Outlined.Smartphone) {
            SettingsSwitchItem(
                icon = Icons.Outlined.Smartphone,
                title = "Show app icon",
                subtitle = "Keep the app visible in your launcher. When hidden, open it " +
                    "from Android Settings or when Morphe starts a download.",
                checked = settings.showLauncherIcon,
                onToggle = {
                    if (settings.showLauncherIcon) {
                        confirmHideIcon = true
                    } else {
                        onSettingsChange(settings.copy(showLauncherIcon = true))
                        Toast.makeText(context, "App icon shown.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        SettingsGroup(title = stringResource(R.string.colour_style), icon = Icons.Outlined.ColorLens) {
            styles.forEachIndexed { index, style ->
                if (index > 0) MorpheDivider()
                SettingsRow(
                    icon = style.icon(),
                    title = style.title,
                    subtitle = style.description,
                    onClick = { onSettingsChange(settings.copy(themeStyle = style)) },
                    trailing = { RadioDot(selected = settings.themeStyle == style) }
                )
            }
        }

        // Pure black only means anything behind dark content, so it is hidden
        // while the app is pinned to the light theme.
        if (settings.themeMode != ThemeMode.LIGHT) {
            SettingsGroup(title = stringResource(R.string.dark_theme), icon = Icons.Outlined.DarkMode) {
                SettingsSwitchItem(
                    icon = Icons.Outlined.Contrast,
                    title = stringResource(R.string.pure_black),
                    subtitle = "Paint backgrounds fully black instead of the theme's near-black. " +
                        "Saves power on OLED screens.",
                    checked = settings.pureBlackTheme,
                    onToggle = {
                        onSettingsChange(settings.copy(pureBlackTheme = !settings.pureBlackTheme))
                    }
                )
            }
        }

        // Wallpaper colours are the accent, and the neutral style has none to
        // swap, so the grid is offered for the Morphe palette alone.
        if (settings.themeStyle == ThemeStyle.MORPHE) {
            SettingsGroup(title = stringResource(R.string.accent_colour), icon = Icons.Outlined.Colorize) {
                AccentGridSection {
                    AccentSwatchGrid(
                        colors = MorpheAccentPresets,
                        selected = accent,
                        onSelect = { color ->
                            onSettingsChange(settings.copy(customAccentColor = color.toHexString()))
                        },
                        onClear = { onSettingsChange(settings.copy(customAccentColor = "")) },
                        onCustomClick = { showAccentPicker = true }
                    )
                }
            }
        }
    }

    if (confirmHideIcon) {
        MorpheDialog(
            title = "Hide app icon?",
            onDismiss = { confirmHideIcon = false },
            actions = {
                HelperOutlinedButton(
                    text = "Cancel",
                    onClick = { confirmHideIcon = false },
                    modifier = Modifier.weight(1f)
                )
                HelperButton(
                    text = "Hide icon",
                    onClick = {
                        confirmHideIcon = false
                        onSettingsChange(settings.copy(showLauncherIcon = false))
                        Toast.makeText(context, "App icon hidden.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        ) {
            Text(
                text = "The app will disappear from your launcher. You can still open it " +
                    "from Android Settings > Apps > Morphe Helper > Open, or when Morphe " +
                    "starts a download.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    if (showAccentPicker) {
        MorpheAccentPickerDialog(
            currentColorHex = settings.customAccentColor,
            onColorSelected = { color ->
                onSettingsChange(settings.copy(customAccentColor = color.toHexString()))
                showAccentPicker = false
            },
            onDismiss = { showAccentPicker = false }
        )
    }
}

/** Icon for each colour style, mirroring the manager's selector. */
internal fun ThemeStyle.icon(): ImageVector = when (this) {
    ThemeStyle.MORPHE -> Icons.Outlined.Palette
    ThemeStyle.MATERIAL_YOU -> Icons.Outlined.AutoAwesome
    ThemeStyle.MONOCHROME -> Icons.Outlined.Contrast
}

/**
 * Advanced tab: connection, resolver and VirusTotal scanning. What has been
 * handed off, and what the app logged doing it, live on the Logs tab.
 */
@Composable
internal fun AdvancedTabContent(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit
) {
    var policyDialog by remember { mutableStateOf(false) }
    var connectionCountDialog by remember { mutableStateOf(false) }
    val policies = NetworkPolicy.entries
    val connectionCounts = DownloadConnectionCount.entries

    if (policyDialog) {
        SettingsChoiceDialog(
            title = stringResource(R.string.connection),
            choices = policies.map { SettingsChoice(it.icon(), it.title, it.description) },
            selectedIndex = policies.indexOf(settings.networkPolicy),
            onSelect = { index ->
                onSettingsChange(settings.copy(networkPolicy = policies[index]))
                policyDialog = false
            },
            onDismiss = { policyDialog = false }
        )
    }

    if (connectionCountDialog) {
        SettingsChoiceDialog(
            title = "Download connections",
            choices = connectionCounts.map {
                SettingsChoice(
                    icon = Icons.Outlined.Bolt,
                    title = it.title,
                    description = it.description
                )
            },
            selectedIndex = connectionCounts.indexOf(settings.downloadConnections),
            onSelect = { index ->
                onSettingsChange(settings.copy(downloadConnections = connectionCounts[index]))
                connectionCountDialog = false
            },
            onDismiss = { connectionCountDialog = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ContentPaddingMedium)
    ) {
        SettingsGroup(title = stringResource(R.string.connection), icon = Icons.Outlined.NetworkCheck) {
            SettingsRow(
                icon = settings.networkPolicy.icon(),
                title = stringResource(R.string.network_policy),
                subtitle = settings.networkPolicy.description,
                value = settings.networkPolicy.title,
                onClick = { policyDialog = true }
            )
            MorpheDivider()
            SettingsRow(
                icon = Icons.Outlined.Bolt,
                title = "Download connections",
                subtitle = "Maximum parallel connections. Large files are split into 4 MiB chunks, " +
                    "and file size can reduce the count.",
                value = settings.downloadConnections.title,
                onClick = { connectionCountDialog = true }
            )
            MorpheDivider()
            SettingsSwitchItem(
                icon = Icons.Outlined.Dns,
                title = stringResource(R.string.adguard_dns),
                subtitle = "Blocks ads and trackers on download pages and in the captcha browser. " +
                    "Falls back to the system resolver whenever it fails.",
                checked = settings.adGuardDns,
                onToggle = {
                    onSettingsChange(settings.copy(adGuardDns = !settings.adGuardDns))
                }
            )
            MorpheDivider()
            SettingsSwitchItem(
                icon = Icons.Outlined.Explore,
                title = "Use Morphe's resolver",
                subtitle = "Ask Morphe where its own download dialog would go and open that " +
                    "page directly, instead of searching the site. Faster; turn it off to " +
                    "always fetch through this app's own source pages.",
                checked = settings.useMorpheResolver,
                onToggle = {
                    onSettingsChange(settings.copy(useMorpheResolver = !settings.useMorpheResolver))
                }
            )
        }

        SettingsGroup(title = stringResource(R.string.virustotal), icon = Icons.Outlined.Shield) {
            SettingsSwitchItem(
                icon = Icons.Outlined.Shield,
                title = stringResource(R.string.virustotal_scanning),
                subtitle = "Scan downloaded files with VirusTotal before returning them to Morphe.",
                checked = settings.virusTotalEnabled,
                onToggle = {
                    onSettingsChange(settings.copy(virusTotalEnabled = !settings.virusTotalEnabled))
                }
            )
            if (settings.virusTotalEnabled) {
                VirusTotalScanMode.entries.forEach { mode ->
                    MorpheDivider()
                    SettingsRow(
                        icon = when (mode) {
                            VirusTotalScanMode.NEVER -> Icons.Outlined.Block
                            VirusTotalScanMode.ASK -> Icons.Outlined.HelpOutline
                            VirusTotalScanMode.ALWAYS -> Icons.Outlined.CheckCircle
                        },
                        title = mode.title,
                        subtitle = mode.description,
                        onClick = { onSettingsChange(settings.copy(virusTotalScanMode = mode)) },
                        trailing = { RadioDot(selected = settings.virusTotalScanMode == mode) }
                    )
                }
                MorpheDivider()
                ApiKeyEditor(
                    apiKey = settings.virusTotalApiKey,
                    onApiKeyChange = { onSettingsChange(settings.copy(virusTotalApiKey = it)) }
                )
                if (settings.virusTotalScanMode != VirusTotalScanMode.NEVER) {
                    MorpheDivider()
                    SettingsSwitchItem(
                        icon = Icons.Outlined.Verified,
                        title = "Proceed automatically when clean",
                        subtitle = "Hand off without showing the result when VirusTotal finds " +
                            "nothing and every inner APK was scanned.",
                        checked = settings.virusTotalAutoProceed,
                        onToggle = {
                            onSettingsChange(
                                settings.copy(virusTotalAutoProceed = !settings.virusTotalAutoProceed)
                            )
                        }
                    )
                }
                if (settings.virusTotalApiKey.isNotBlank()) {
                    MorpheDivider()
                    Box(modifier = Modifier.padding(MorpheDefaults.ContentPadding)) {
                        VirusTotalQuotaCard(apiKey = settings.virusTotalApiKey)
                    }
                }
            }
        }

    }
}

/**
 * Logs tab: history, Logcat logging and the in-app request log each get a sub-tab
 * so the page no longer stacks all three into one long scroll.
 */
internal enum class LogsSubTab(
    val label: String,
    val icon: ImageVector
) {
    HISTORY("History", Icons.Outlined.History),
    LOGGING("Logging", Icons.Outlined.BugReport),
    REQUEST_LOGS("Request logs", Icons.AutoMirrored.Outlined.ViewList)
}

@Composable
internal fun LogsTabContent(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit,
    logs: List<RequestLogEntry>,
    onClearLogs: () -> Unit,
    historyEntries: List<DownloadHistoryEntry>,
    onOpenHistoryEntry: (DownloadHistoryEntry) -> Unit,
    onShareHistoryEntry: (DownloadHistoryEntry) -> Unit,
    onClearHistory: () -> Unit
) {
    var subTab by rememberSaveable { mutableStateOf(LogsSubTab.HISTORY) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ContentPaddingMedium)
    ) {
        MorpheSelectorRow(
            options = LogsSubTab.entries.map {
                MorpheSelectorOption(label = it.label, icon = it.icon)
            },
            selectedIndex = LogsSubTab.entries.indexOf(subTab),
            onSelect = { subTab = LogsSubTab.entries[it] },
            labelStyle = MaterialTheme.typography.labelLarge
        )

        when (subTab) {
            LogsSubTab.HISTORY -> DownloadHistorySection(
                entries = historyEntries,
                onClear = onClearHistory,
                onOpen = onOpenHistoryEntry,
                onShare = onShareHistoryEntry
            )

            LogsSubTab.LOGGING -> SettingsGroup(
                title = stringResource(R.string.logging),
                icon = Icons.Outlined.BugReport
            ) {
                SettingsSwitchItem(
                    icon = Icons.Outlined.BugReport,
                    title = stringResource(R.string.log_to_logcat),
                    subtitle = "Write request, result, and source HTTP details to the system log (adb logcat) for debugging.",
                    checked = settings.logcatLogging,
                    onToggle = {
                        onSettingsChange(settings.copy(logcatLogging = !settings.logcatLogging))
                    }
                )
            }

            LogsSubTab.REQUEST_LOGS -> RequestLogsCard(
                logs = logs,
                onClearLogs = onClearLogs
            )
        }
    }
}

/** API key entry plus the manager link, laid out as a group row. */
@Composable
internal fun ApiKeyEditor(
    apiKey: String,
    onApiKeyChange: (String) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MorpheDefaults.ContentPadding,
                vertical = MorpheDefaults.ContentPaddingSmall + 4.dp
            ),
        verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ContentPaddingSmall)
    ) {
        Text(
            text = stringResource(R.string.api_key),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            placeholder = {
                Text(
                    stringResource(R.string.enter_api_key),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MorpheDefaults.CompactCornerRadius))
                .clickable {
                    val open = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://docs.virustotal.com/docs/please-give-me-an-api-key")
                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    runCatching { context.startActivity(open) }
                        .onFailure {
                            Toast.makeText(
                                context,
                                "No browser available",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.how_to_get_a_free_api),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Outlined.OpenInNew,
                contentDescription = stringResource(R.string.open_virustotal_api_key_guide),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}



/**
 * One settings section: the manager's `SectionTitle` over a single group card
 * holding compact rows separated by dividers.
 */
@Composable
internal fun SettingsGroup(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ContentPadding)
    ) {
        MorpheSectionTitle(text = title, icon = icon)
        SectionCard {
            Column(content = content)
        }
    }
}

/**
 * Compact settings row in the manager's `SettingsItem` language: icon, title and
 * subtitle, with either a trailing value/chevron (opens a dialog) or a caller
 * supplied trailing control such as a switch.
 */
@Composable
internal fun SettingsRow(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    /** Leading content used instead of [icon], e.g. a source's brand logo. */
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MorpheDefaults.SettingsCornerRadius))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(
                horizontal = MorpheDefaults.ContentPadding,
                vertical = MorpheDefaults.ContentPaddingSmall + 4.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ContentPadding)
    ) {
        if (leading != null) {
            leading()
        } else if (icon != null) {
            ThemedIcon(icon = icon, modifier = Modifier.padding(top = 2.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            value?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.primary
                )
            }
        }

        Box(
            modifier = Modifier.align(Alignment.CenterVertically),
            contentAlignment = Alignment.Center
        ) {
            if (trailing != null) trailing() else if (onClick != null) ForwardChevronIcon(size = MorpheDefaults.IconSizeSmall)
        }
    }
}

/** [SettingsRow] trailed by a switch that reflects [checked]; tapping the row toggles it. */
@Composable
internal fun SettingsSwitchItem(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onToggle: () -> Unit,
    /** Leading content used instead of [icon], e.g. a source's brand logo. */
    leading: (@Composable () -> Unit)? = null
) {
    val enabledLabel = "Enabled"
    val disabledLabel = "Disabled"
    SettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        leading = leading,
        onClick = onToggle,
        trailing = {
            Box(
                modifier = Modifier.semantics { stateDescription = if (checked) enabledLabel else disabledLabel }
            ) {
                MorpheToggleSwitch(checked = checked, onCheckedChange = null)
            }
        }
    )
}

/** One option offered by [SettingsChoiceDialog]. */
internal data class SettingsChoice(
    val icon: ImageVector?,
    val title: String,
    val description: String,
    /** When set, the choice shows this source's brand logo instead of [icon]. */
    val source: DownloadSource? = null
)

/**
 * Multi-choice settings are picked in a dialog rather than as a stack of full
 * width cards, which is what keeps the manager's tabs scannable.
 */
@Composable
internal fun SettingsChoiceDialog(
    title: String,
    choices: List<SettingsChoice>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        // The platform container is an already-elevated tone, which the option rows
        // (elevated surfaces themselves) sit flush against  the two read as one flat
        // block. The manager paints its dialogs on the plain background so the cards on
        // top stand out, so do the same here.
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Text(text = title, fontWeight = FontWeight.Bold)
        },
        text = {
            // The source picker carries one row per source, which overflows a small
            // screen, so the list scrolls inside a capped height instead of being clipped.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ContentPaddingSmall)
            ) {
                choices.forEachIndexed { index, choice ->
                    SettingsOptionCard(
                        icon = choice.icon,
                        title = choice.title,
                        description = choice.description,
                        selected = index == selectedIndex,
                        source = choice.source,
                        onClick = { onSelect(index) }
                    )
                }
            }
        },
        confirmButton = {}
    )
}

internal fun DownloadLocation.icon(): ImageVector = when (this) {
    DownloadLocation.TEMPORARY -> Icons.Outlined.SdStorage
    DownloadLocation.EXTERNAL -> Icons.Outlined.PhoneAndroid
    DownloadLocation.DOWNLOADS -> Icons.Outlined.SaveAlt
    DownloadLocation.CUSTOM -> Icons.Outlined.FolderOpen
}

internal fun DownloadRetention.icon(): ImageVector = when (this) {
    DownloadRetention.KEEP -> Icons.Outlined.SaveAlt
    DownloadRetention.AFTER_HANDOFF -> Icons.Outlined.DeleteOutline
    DownloadRetention.HOUR_1,
    DownloadRetention.HOUR_24,
    DownloadRetention.DAY_7 -> Icons.Outlined.CleaningServices
}

internal fun NetworkPolicy.icon(): ImageVector = when (this) {
    NetworkPolicy.WIFI_ONLY -> Icons.Outlined.Wifi
    NetworkPolicy.MOBILE_DATA_ONLY -> Icons.Outlined.SignalCellularAlt
    NetworkPolicy.WIFI_AND_MOBILE -> Icons.Outlined.NetworkCheck
}

internal fun ThemeMode.icon(): ImageVector = when (this) {
    ThemeMode.SYSTEM -> Icons.Outlined.Smartphone
    ThemeMode.DARK -> Icons.Outlined.DarkMode
    ThemeMode.LIGHT -> Icons.Outlined.LightMode
}

/**
 * Neutral settings row card in the manager's `SettingsItemCard` language: theme
 * surface fill, hairline border only when selected.
 */
@Composable
internal fun SettingsRowSurface(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(MorpheDefaults.SettingsCornerRadius)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = enabled, onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = if (selected) colors.surfaceVariant else colors.surfaceColorAtElevation(3.dp),
        contentColor = colors.onSurface,
        tonalElevation = if (selected) 0.dp else 1.dp,
        border = if (selected) {
            BorderStroke(1.5.dp, colors.onSurface.copy(alpha = 0.5f))
        } else {
            null
        }
    ) {
        content()
    }
}

@Composable
internal fun SettingsOptionCard(
    icon: ImageVector?,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    source: DownloadSource? = null
) {
    val colors = MaterialTheme.colorScheme
    SettingsRowSurface(selected = selected, enabled = enabled, onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MorpheDefaults.ContentPadding),
            horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (source != null) {
                SourceAvatar(source = source, size = MorpheDefaults.IconSize)
            } else if (icon != null) {
                ThemedIcon(
                    icon = icon,
                    size = MorpheDefaults.IconSize,
                    tint = if (selected) colors.primary else colors.onSurfaceVariant
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface
                )
                Text(
                    description,
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            RadioDot(selected = selected)
        }
    }
}

/**
 * Compact dropdown trigger in the same card language as [SettingsOptionCard],
 * but showing the current value on the right and a chevron instead of a radio
 * dot. Tap opens the anchored [DropdownMenu].
 */
@Composable
internal fun SettingsDropdownCard(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    SettingsRowSurface(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MorpheDefaults.ContentPadding),
            horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemedIcon(icon = icon, size = MorpheDefaults.IconSize)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface
                )
                Text(
                    value,
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = stringResource(R.string.change_default_source),
                tint = colors.primary,
                modifier = Modifier.size(MorpheDefaults.IconSize)
            )
        }
    }
}

@Composable
internal fun SettingsStorageCard(
    cacheBytes: Long,
    downloadsBytes: Long,
    onClear: () -> Unit
) {
    SettingsRowSurface {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MorpheDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ContentPaddingSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(MorpheDefaults.IconSize)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        stringResource(R.string.clear_storage_cache_downloads),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.removes_both_the_cache_copies),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                HelperOutlinedButton(
                    text = "Clear",
                    onClick = onClear,
                    modifier = Modifier.widthIn(min = 96.dp)
                )
            }
            MorpheDivider(fullWidth = true)
            StorageInfoLine(
                icon = Icons.Outlined.SdStorage,
                label = stringResource(R.string.helper_cache),
                sizeBytes = cacheBytes
            )
            StorageInfoLine(
                icon = Icons.Outlined.FolderOpen,
                label = stringResource(R.string.downloads_copy),
                sizeBytes = downloadsBytes
            )
        }
    }
}

@Composable
internal fun StorageInfoLine(
    icon: ImageVector,
    label: String,
    sizeBytes: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ContentPaddingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThemedIcon(
            icon = icon,
            size = MorpheDefaults.IconSizeSmall,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = sizeBytes.formatBytes(),
            fontWeight = FontWeight.Bold,
            color = if (sizeBytes > 0L) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

internal fun Long.formatBytes(): String {
    if (this <= 0L) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    var value = toDouble()
    var unit = 0
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    return if (unit == 0) "${toLong()} B" else String.format(Locale.US, "%.1f %s", value, units[unit])
}

@Composable
internal fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MorpheDefaults.SettingsCornerRadius))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        shape = RoundedCornerShape(MorpheDefaults.SettingsCornerRadius),
        color = colors.surfaceColorAtElevation(3.dp),
        contentColor = colors.onSurface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MorpheDefaults.ContentPadding),
            horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemedIcon(icon = icon, size = MorpheDefaults.IconSize)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface
                )
                Text(
                    description,
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            MorpheToggleSwitch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
internal fun SettingTextFieldRow(
    icon: ImageVector,
    title: String,
    description: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    SettingsRowSurface {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MorpheDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ContentPaddingSmall)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemedIcon(icon = icon, size = MorpheDefaults.IconSize)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface
                    )
                    Text(
                        description,
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.onSurface
                ),
                placeholder = {
                    Text(stringResource(R.string.enter_api_key), color = colors.onSurfaceVariant.copy(alpha = 0.5f))
                }
            )
        }
    }
}

/**
 * Detailed VirusTotal quota card shared by the Settings screen and the
 * scan-flow screens. Shows the live per-minute bucket (tracked client-side
 * against the 4/min free tier) plus the hourly/daily/monthly buckets reported
 * by the API. Returns nothing when no API key is set.
 */
@Composable
internal fun VirusTotalQuotaCard(apiKey: String) {
    if (apiKey.isBlank()) return
    val colors = MaterialTheme.colorScheme
    var quota by remember { mutableStateOf<VirusTotalScanner.QuotaUsage?>(null) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var minuteUsed by remember { mutableIntStateOf(0) }
    var rollingUsed by remember { mutableIntStateOf(0) }
    // Seconds until the wall-clock minute boundary ticks over and the per-minute
    // count resets to 0. Ceiled so it reads 60 right after a rollover.
    var secsToRollover by remember { mutableIntStateOf(60) }
    val scope = rememberCoroutineScope()
    val load: () -> Unit = {
        scope.launch {
            loading = true
            failed = false
            quota = withContext(Dispatchers.IO) { VirusTotalScanner.fetchQuotaUsage(apiKey) }
            if (quota == null) failed = true
            loading = false
        }
    }
    LaunchedEffect(apiKey) {
        load()
        // The per-minute count is tracked locally in the rate limiter, so tick
        // it once a second to keep the bar honest while a scan is running.
        while (true) {
            minuteUsed = VirusTotalScanner.rateLimiter.callsInCurrentMinute()
            rollingUsed = VirusTotalScanner.rateLimiter.callsInLastMinute()
            secsToRollover =
                ((60_000L - System.currentTimeMillis() % 60_000L + 999L) / 1000L).toInt()
            delay(1000)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MorpheDefaults.CardCornerRadius),
        color = sourceCardFill(),
        border = BorderStroke(1.dp, sourceCardBorder())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.virustotal_quota),
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                }
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.refresh_quota),
                        tint = colors.primary,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(MorpheDefaults.PillShape)
                            .clickable { load() }
                            .padding(2.dp)
                    )
                }
            }
            // Per minute is always available locally, even while the API quota
            // is still loading. The wall-clock minute count resets at each
            // minute boundary; the rolling 60s count is what the API actually
            // enforces, so show both and flash when either nears the cap.
            QuotaBar(
                "Per minute",
                minuteUsed,
                VirusTotalScanner.MINUTE_LOOKUP_LIMIT,
                flash = maxOf(minuteUsed, rollingUsed) >= VirusTotalScanner.MINUTE_LOOKUP_LIMIT - 1,
                caption = "rolling 60s: $rollingUsed / ${VirusTotalScanner.MINUTE_LOOKUP_LIMIT} · resets in ${secsToRollover}s"
            )
            val current = quota
            when {
                current != null -> {
                    QuotaBar("Per hour", current.hourlyUsed, current.hourlyAllowed)
                    QuotaBar("Per day", current.dailyUsed, current.dailyAllowed)
                    QuotaBar("Per month", current.monthlyUsed, current.monthlyAllowed)
                }
                failed -> Text(
                    text = stringResource(R.string.couldn_t_load_quota_check_your),
                    color = colors.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
internal fun QuotaBar(
    label: String,
    used: Int,
    allowed: Int,
    // Pulsing amber flash used for the per-minute bar when it nears the cap.
    flash: Boolean = false,
    // Optional second line under the bar, e.g. the rolling 60s count next to
    // the wall-clock minute count.
    caption: String? = null
) {
    val colors = MaterialTheme.colorScheme
    val ratio = if (allowed > 0) used.toFloat() / allowed else 0f
    val warningAccent = SemanticTone.Warning.accent
    val baseColor = when {
        ratio >= 0.9f -> colors.error
        ratio >= 0.7f -> warningAccent
        else -> colors.primary
    }
    // When near the cap, pulse the bar between amber and a dim amber so the
    // mid-scan warning is visible even without looking at the number.
    val barColor = if (flash) {
        val transition = rememberInfiniteTransition(label = "quota-flash-$label")
        warningAccent.copy(
            alpha = transition.animateFloat(
                initialValue = 1f,
                targetValue = 0.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "quota-alpha-$label"
            ).value
        )
    } else {
        baseColor
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$used / $allowed",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = if (ratio >= 0.7f) barColor else colors.onSurface
            )
        }
        LinearProgressIndicator(
            progress = { ratio.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = barColor,
            trackColor = colors.surfaceVariant
        )
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun SourceToggleRow(
    source: DownloadSource,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(MorpheDefaults.SettingsCornerRadius)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .toggleable(value = enabled, role = Role.Switch, onValueChange = onToggle),
        shape = shape,
        color = colors.surfaceColorAtElevation(3.dp),
        contentColor = colors.onSurface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MorpheDefaults.ContentPadding, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SourceAvatar(source = source, size = 32.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = source.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) colors.onSurface else colors.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = if (enabled) "Enabled" else "Disabled",
                    color = colors.onSurfaceVariant.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            MorpheToggleSwitch(checked = enabled, onCheckedChange = null)
        }
    }
}




