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
internal fun UpdateScreen(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit,
    refreshToken: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var latest by remember { mutableStateOf(UpdateManager.cachedLatest(context)) }
    var checking by remember { mutableStateOf(false) }
    var checkToken by remember { mutableIntStateOf(0) }
    var checkMessage by remember { mutableStateOf<String?>(null) }
    var installing by remember { mutableStateOf(false) }
    var installMessage by remember { mutableStateOf<String?>(null) }
    var modeDialog by remember { mutableStateOf(false) }
    var channelDialog by remember { mutableStateOf(false) }
    var packageDialog by remember { mutableStateOf(false) }

    val shizukuStatus = remember(refreshToken, settings.shizukuPackage) {
        ShizukuHelper.status(context)
    }
    val batteryUnrestricted = remember(refreshToken) {
        UpdateManager.isBatteryUnrestricted(context)
    }
    val updateAvailable = latest?.isNewerThan(BuildConfig.VERSION_NAME) == true

    fun checkNow(channel: UpdateChannel = settings.updateChannel) {
        if (checking && channel == settings.updateChannel) return
        val token = checkToken + 1
        checkToken = token
        checking = true
        checkMessage = "Checking for updates…"
        scope.launch {
            val result = UpdateManager.checkForUpdates(
                context = context,
                manual = true,
                settings = settings.copy(updateChannel = channel)
            )
            if (token != checkToken) return@launch
            latest = result.info ?: latest
            checkMessage = result.message
            checking = false
        }
    }

    fun install(useShizuku: Boolean) {
        val info = latest ?: return
        if (installing) return
        if (info.assetUrl.isBlank()) {
            installMessage = "This release has no APK asset."
            return
        }
        scope.launch {
            installing = true
            installMessage = "Downloading v${info.version}…"
            try {
                val file = UpdateManager.download(context, info)
                val verification = UpdateManager.verifyDownloadedApk(context, file, info)
                if (verification != null) {
                    file.delete()
                    installMessage = verification
                } else if (
                    useShizuku &&
                    ShizukuHelper.status(context) == ShizukuHelper.READY
                ) {
                    installMessage = "Installing with Shizuku…"
                    val (ok, detail) = UpdateManager.installWithShizuku(context, file)
                    if (ok) {
                        installMessage = "Update installed. Reopen the helper."
                    } else {
                        installMessage = "Shizuku failed: $detail. Opening the Android installer…"
                        UpdateManager.openInstaller(context, file)
                    }
                } else {
                    val opened = UpdateManager.openInstaller(context, file)
                    installMessage = if (opened) {
                        "Android installer opened. Follow the prompt."
                    } else {
                        "Allow installs from this app, then tap Install again."
                    }
                }
            } catch (error: Throwable) {
                installMessage = "Update failed: ${error.message ?: "unknown error"}"
            } finally {
                installing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        latest = UpdateManager.cachedLatest(context)
        if (latest != null) return@LaunchedEffect
        if (!settings.checkUpdates) {
            checkMessage = "Update checks are disabled."
            return@LaunchedEffect
        }
        checkNow()
    }

    BackHandler(onBack = onBack)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
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
                            text = "Updates",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    HelperHeaderIconButton(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }
            }

            item {
                SettingsGroup(title = "Release", icon = Icons.Outlined.Refresh) {
                    SettingsRow(
                        icon = Icons.Outlined.PhoneAndroid,
                        title = "Current version",
                        value = "v${BuildConfig.VERSION_NAME}"
                    )
                    MorpheDivider()
                    SettingsRow(
                        icon = Icons.Outlined.Refresh,
                        title = if (checking) "Checking for updates…" else "Check for updates",
                        subtitle = checkMessage ?: "Look for a newer release on GitHub.",
                        value = if (checking) null else "Check",
                        onClick = { checkNow() }
                    )
                    latest?.let { info ->
                        MorpheDivider()
                        SettingsRow(
                            icon = Icons.Outlined.Verified,
                            title = "Latest version",
                            value = "v${info.version}",
                            subtitle = if (updateAvailable) {
                                val channel = if (UpdateInfo.isPrerelease(info.version)) {
                                    "Beta"
                                } else {
                                    "Stable"
                                }
                                "$channel update available"
                            } else {
                                "You are up to date"
                            }
                        )
                    }
                }
            }

            latest?.let { info ->
                item {
                    SettingsGroup(title = "Changelog", icon = Icons.Outlined.History) {
                        SelectionContainer {
                            Text(
                                text = cleanReleaseNotes(info.notes)
                                    .ifBlank { "No release notes for this version." },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(MorpheDefaults.ContentPadding),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (info.releaseUrl.isNotBlank()) {
                            MorpheDivider()
                            SettingsRow(
                                icon = Icons.Outlined.OpenInNew,
                                title = "Open release page",
                                onClick = { openExternal(context, info.releaseUrl) }
                            )
                        }
                    }
                }
            }

            if (updateAvailable && latest != null) {
                item {
                    val info = latest!!
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
                    ) {
                        HelperButton(
                            text = if (installing) "Working…" else "Download and install",
                            onClick = { install(false) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (shizukuStatus == ShizukuHelper.READY) {
                            HelperOutlinedButton(
                                text = "Install via Shizuku",
                                onClick = { install(true) },
                                icon = Icons.Outlined.Shield,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (info.assetUrl.isBlank()) {
                            Text(
                                text = "This release has no APK attached.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        installMessage?.let { message ->
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            item {
                SettingsGroup(title = "Update settings", icon = Icons.Outlined.Settings) {
                    SettingsSwitchItem(
                        icon = Icons.Outlined.Refresh,
                        title = "Check for updates",
                        subtitle = "Look for a newer release when the helper starts.",
                        checked = settings.checkUpdates,
                        onToggle = {
                            onSettingsChange(settings.copy(checkUpdates = !settings.checkUpdates))
                        }
                    )
                    MorpheDivider()
                    SettingsSwitchItem(
                        icon = Icons.Outlined.HelpOutline,
                        title = "In-app update popup",
                        subtitle = "Show a dialog on launch when a newer release is available.",
                        checked = settings.updatePopup,
                        onToggle = {
                            onSettingsChange(settings.copy(updatePopup = !settings.updatePopup))
                        }
                    )
                    MorpheDivider()
                    SettingsRow(
                        icon = Icons.Outlined.TrendingUp,
                        title = "Update channel",
                        subtitle = settings.updateChannel.description,
                        value = settings.updateChannel.title,
                        onClick = { channelDialog = true }
                    )
                    MorpheDivider()
                    SettingsRow(
                        icon = Icons.Outlined.Download,
                        title = "Update mode",
                        subtitle = settings.updateMode.description,
                        value = settings.updateMode.title,
                        onClick = { modeDialog = true }
                    )
                    MorpheDivider()
                    SettingsRow(
                        icon = Icons.Outlined.Shield,
                        title = "Shizuku",
                        subtitle = "Silent installs need Shizuku running and this app approved.",
                        value = ShizukuHelper.statusLabel(context, shizukuStatus),
                        onClick = { handleShizukuAction(context, shizukuStatus) }
                    )
                    MorpheDivider()
                    SettingsRow(
                        icon = Icons.Outlined.PhoneAndroid,
                        title = "Shizuku app package",
                        subtitle = "Only needed for a Shizuku fork with a different package name.",
                        value = settings.shizukuPackage.ifBlank { "Default" },
                        onClick = { packageDialog = true }
                    )
                    MorpheDivider()
                    SettingsRow(
                        icon = Icons.Outlined.Settings,
                        title = "Battery optimization",
                        subtitle = "Keep update checks reliable when Android restricts background work.",
                        value = if (batteryUnrestricted) "Unrestricted" else "Optimized",
                        onClick = { UpdateManager.requestBatteryExemption(context) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (channelDialog) {
        val channels = UpdateChannel.entries
        SettingsChoiceDialog(
            title = "Update channel",
            choices = channels.map { channel ->
                SettingsChoice(
                    icon = if (channel == UpdateChannel.BETA) {
                        Icons.Outlined.TrendingUp
                    } else {
                        Icons.Outlined.CheckCircle
                    },
                    title = channel.title,
                    description = channel.description
                )
            },
            selectedIndex = channels.indexOf(settings.updateChannel),
            onSelect = { index ->
                val channel = channels[index]
                channelDialog = false
                if (channel != settings.updateChannel) {
                    UpdateManager.resetForChannelSwitch(context)
                    latest = null
                    checkMessage = null
                    installMessage = null
                    onSettingsChange(settings.copy(updateChannel = channel))
                    checkNow(channel)
                }
            },
            onDismiss = { channelDialog = false }
        )
    }

    if (modeDialog) {
        val modes = UpdateMode.entries
        SettingsChoiceDialog(
            title = "Update mode",
            choices = modes.map { mode ->
                SettingsChoice(
                    icon = when (mode) {
                        UpdateMode.NOTIFY -> Icons.Outlined.HelpOutline
                        UpdateMode.SHIZUKU -> Icons.Outlined.Shield
                    },
                    title = mode.title,
                    description = mode.description
                )
            },
            selectedIndex = modes.indexOf(settings.updateMode),
            onSelect = { index ->
                onSettingsChange(settings.copy(updateMode = modes[index]))
                modeDialog = false
            },
            onDismiss = { modeDialog = false }
        )
    }

    if (packageDialog) {
        ShizukuPackageDialog(
            initial = settings.shizukuPackage,
            onSave = { value ->
                onSettingsChange(settings.copy(shizukuPackage = value))
                packageDialog = false
            },
            onDismiss = { packageDialog = false }
        )
    }
}

@Composable
internal fun ShizukuPackageDialog(
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = { Text("Shizuku app package", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)) {
                Text(
                    text = "Leave blank for the default package. Only change this for a Shizuku fork with a different package name.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        value = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = error != null,
                    placeholder = { Text(ShizukuHelper.DEFAULT_PACKAGE) }
                )
                error?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = value.trim()
                if (trimmed.isNotEmpty() && !PACKAGE_PATTERN.matches(trimmed)) {
                    error = "Enter a valid package name, for example ${ShizukuHelper.DEFAULT_PACKAGE}."
                } else {
                    onSave(trimmed)
                }
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onSave("") }) { Text("Use default") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
internal fun UpdatePromptDialog(
    info: UpdateInfo,
    currentVersion: String,
    onView: () -> Unit,
    onLater: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = { Text("Update available", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Helper v${info.version} is available. You have v$currentVersion."
            )
        },
        confirmButton = {
            TextButton(onClick = onView) { Text("View update") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onSkip) { Text("Skip") }
                TextButton(onClick = onLater) { Text("Later") }
            }
        }
    )
}

internal fun handleShizukuAction(context: Context, status: Int) {
    when (status) {
        ShizukuHelper.READY -> {
            Toast.makeText(context, "Shizuku is ready.", Toast.LENGTH_SHORT).show()
        }
        ShizukuHelper.PERMISSION_REQUIRED -> ShizukuHelper.requestPermission()
        ShizukuHelper.NOT_RUNNING,
        ShizukuHelper.NOT_INSTALLED -> ShizukuHelper.openManager(context)
        else -> {
            Toast.makeText(context, "This Shizuku version is not supported.", Toast.LENGTH_SHORT).show()
        }
    }
}

internal fun openExternal(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

internal fun cleanReleaseNotes(value: String): String =
    value.replace("\r\n", "\n")
        .replace(Regex("""(?m)^#{1,6}\s*"""), "")
        .replace(Regex("""\[([^\]]+)\]\([^)]+\)"""), "\$1")
        .replace("**", "")
        .replace("__", "")
        .replace("`", "")
        .replace(Regex("""(?m)^- """), "• ")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()



