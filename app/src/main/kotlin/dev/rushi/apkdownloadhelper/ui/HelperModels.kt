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


internal data class HelperRequest(
    val callerPackage: String,
    val packageName: String,
    val appName: String,
    val versionName: String?,
    val versionCode: Long?,
    val versionCodes: Set<Long>,
    val compatibleVersionNames: Set<String>,
    val compatibleVersionCodes: Set<Long>,
    val supportedAbis: List<String>,
    val requestedFileType: String?,
    val allowSplitArchive: Boolean,
    /** Formats the on-screen picker narrowed the request to, null for all allowed kinds. */
    val requestedFileKindsOverride: Set<String>? = null,
    val stockInstallRequired: Boolean,
    val fallbackWebUrl: String,
    val sourceHintUrls: List<String>,
    /** Screen density in dpi, used to rank density-specific source variants. */
    val deviceDpi: Int? = null,
    /** APKMirror only: opt in to density-specific rows instead of nodpi. */
    val preferDensitySpecificVariants: Boolean = false
) {
    fun withDeviceDpi(
        dpi: Int,
        preferDensitySpecificVariants: Boolean = this.preferDensitySpecificVariants
    ): HelperRequest = copy(
        deviceDpi = dpi.takeIf { it > 0 },
        preferDensitySpecificVariants = preferDensitySpecificVariants
    )

    val availableAbis: List<String>
        get() = supportedAbis
            .ifEmpty { Build.SUPPORTED_ABIS.toList() }
            .mapNotNull { it.trim().takeIf(String::isNotBlank) }
            .distinct()

    val requestedVersionName: String?
        get() = versionName
            ?.withoutTrailingVersionCode()
            ?.takeIf(String::isNotBlank)

    val embeddedVersionCode: Long?
        get() = versionName?.trailingVersionCode()

    val abiSummary: String
        get() = availableAbis.joinToString().ifBlank { "Default" }

    val requestedFileKinds: Set<String>
        get() = requestedFileKindsOverride?.takeIf { it.isNotEmpty() }
            ?: requestedFileKindsFrom(requestedFileType, allowSplitArchive)

    /** Every format the caller allows, before the format picker narrows them. */
    val availableFileKinds: Set<String>
        get() = requestedFileKindsFrom(requestedFileType, allowSplitArchive)

    val requestedFormatLabel: String
        get() = requestedFileKinds
            .orderedFileKinds()
            .joinToString("/") { it.uppercase(Locale.US) }

    val versionCodeSummary: String?
        get() = when {
            requestedVersionCodes.isEmpty() -> null
            requestedVersionCodes.size == 1 -> requestedVersionCodes.first().toString()
            else -> requestedVersionCodes.joinToString(limit = 3, truncated = "+${requestedVersionCodes.size - 3} more")
        }

    val requestedVersionCodes: Set<Long>
        get() = buildSet {
            versionCode?.takeIf { it > 0L }?.let(::add)
            embeddedVersionCode?.takeIf { it > 0L }?.let(::add)
            addAll(versionCodes.filter { it > 0L })
        }

    val knownVersionNames: List<String>
        get() = (listOfNotNull(requestedVersionName) + compatibleVersionNames.map { it.withoutTrailingVersionCode() })
            .mapNotNull { it.trim().takeIf(String::isNotBlank) }
            .distinctBy { it.normalizedVersionName() }

    val requestedVersionNames: List<String>
        get() = listOfNotNull(requestedVersionName)
            .mapNotNull { it.trim().takeIf(String::isNotBlank) }
            .distinctBy { it.normalizedVersionName() }

    val hasRequestedVersionRequest: Boolean
        get() = requestedVersionName != null || requestedVersionCodes.isNotEmpty()

    /** True when the request itself asks for a variant build (e.g. "...-SECONDARY"). */
    val requestsVariantBuild: Boolean
        get() = requestedVersionName.hasVariantBuildMarker()

    val hasKnownVersionRequest: Boolean
        get() = requestedVersionName != null ||
            requestedVersionCodes.isNotEmpty() ||
            compatibleVersionNames.isNotEmpty() ||
            compatibleVersionCodes.any { it > 0L }

    val requestedVersionLabel: String
        get() = listOfNotNull(
            requestedVersionName,
            versionCodeSummary?.let { "build $it" }
        ).joinToString(" ").ifBlank { "any compatible version" }

    fun isRequestedMatch(candidate: DownloadCandidate): Boolean {
        // A secondary build must not satisfy a plain-version request just
        // because its version number or code happens to match.
        if (candidate.hasVariantBuildMarker && !requestsVariantBuild) return false
        return matchesRequestedVersion(candidate.versionName, candidate.versionCode)
    }

    fun versionStatus(candidateVersionName: String?, candidateVersionCode: Long?): VersionStatus {
        if (matchesRequestedVersion(candidateVersionName, candidateVersionCode)) return VersionStatus.REQUESTED

        val compatibleName = candidateVersionName != null &&
            compatibleVersionNames.any { candidateVersionName.versionNameEquals(it.withoutTrailingVersionCode()) }
        val compatibleCode = candidateVersionCode != null &&
            candidateVersionCode > 0L &&
            candidateVersionCode in compatibleVersionCodes
        return if (compatibleName || compatibleCode) VersionStatus.COMPATIBLE else VersionStatus.LATEST
    }

    fun acceptsFormat(fileKind: String): Boolean {
        // Some parsers tag a candidate with the whole requested label (e.g.
        // "APK/APKM/APKS/XAPK") instead of a single kind. Split on '/' so any
        // listed kind counts as a match instead of a false "Format mismatch".
        val kinds = fileKind.lowercase(Locale.US).split('/')
        return kinds.any { it in requestedFileKinds }
    }

    /** Position of [fileKind] in the selected format order, or Int.MAX_VALUE when unselected. */
    fun fileKindPriority(fileKind: String): Int {
        val order = requestedFileKinds.orderedFileKinds()
        return fileKind.lowercase(Locale.US)
            .split('/')
            .map { it to order.indexOf(it) }
            .filter { it.second >= 0 }
            .minOfOrNull { it.second }
            ?: Int.MAX_VALUE
    }

    /**
     * A split request can use APKPure's XAPK asset: it is the same kind of
     * split archive under a different name, and Morphe accepts it whenever
     * split archives are allowed.
     */
    fun acceptsCandidateFormat(fileKind: String): Boolean =
        acceptsFormat(fileKind) ||
            (fileKind.equals("xapk", ignoreCase = true) &&
                allowSplitArchive &&
                requestedFileKinds.any { it == "apks" || it == "apkm" })

    fun matchesKnownVersion(candidateVersionName: String?, candidateVersionCode: Long?): Boolean =
        if (!hasKnownVersionRequest) {
            false
        } else {
            matchesRequestedVersion(candidateVersionName, candidateVersionCode) ||
                (
                    candidateVersionName != null &&
                        compatibleVersionNames.any { candidateVersionName.versionNameEquals(it.withoutTrailingVersionCode()) }
                    ) ||
                (
                    candidateVersionCode != null &&
                        candidateVersionCode > 0L &&
                        candidateVersionCode in compatibleVersionCodes
                    )
        }

    fun matchesRequestedVersion(candidateVersionName: String?, candidateVersionCode: Long?): Boolean {
        val requestedCodes = requestedVersionCodes
        if (versionName == null && requestedCodes.isEmpty()) return false
        if (candidateVersionName.hasVariantBuildMarker() && !requestsVariantBuild) return false

        val nameMatches = requestedVersionName != null && candidateVersionName
            .versionNameEquals(requestedVersionName, ignoreVariantMarker = requestsVariantBuild)
        val codeMatches = requestedCodes.isNotEmpty() &&
            candidateVersionCode != null &&
            candidateVersionCode > 0L &&
            candidateVersionCode in requestedCodes
        return nameMatches || codeMatches
    }

    /**
     * Strict matcher used by Fast Mode, where the exact version name AND the
     * exact version code are both required. Unlike [matchesRequestedVersion]
     * (which accepts a name-only match), a candidate that reports a version
     * code outside the requested set is rejected up front  so Fast Mode
     * doesn't download a file that validation would reject anyway. When the
     * source doesn't report a code, the name match is accepted (validation is
     * still the backstop for the downloaded artifact).
     */
    fun matchesRequestedVersionStrict(candidateVersionName: String?, candidateVersionCode: Long?): Boolean {
        if (versionName == null && requestedVersionCodes.isEmpty()) return false
        if (candidateVersionName.hasVariantBuildMarker() && !requestsVariantBuild) return false

        val nameRequired = requestedVersionName != null
        val nameMatches = candidateVersionName != null &&
            candidateVersionName.versionNameEquals(
                requestedVersionName,
                ignoreVariantMarker = requestsVariantBuild
            )
        val codeRequired = requestedVersionCodes.isNotEmpty() && candidateVersionCode != null
        val codeMatches = requestedVersionCodes.isEmpty() ||
            candidateVersionCode == null ||
            (candidateVersionCode > 0L && candidateVersionCode in requestedVersionCodes)

        return (!nameRequired || nameMatches) && (!codeRequired || codeMatches)
    }

    /**
     * [matchesRequestedVersionStrict] for a whole candidate: a variant build
     * ("-SECONDARY") the request didn't ask for is rejected as well, which the
     * parsed version number alone can't reveal.
     */
    fun matchesRequestedVersionStrict(candidate: DownloadCandidate): Boolean =
        matchesRequestedVersionStrict(candidate.versionName, candidate.versionCode) &&
            (requestsVariantBuild || !candidate.hasVariantBuildMarker)

    fun sourceHintUrlsFor(source: DownloadSource): List<String> {
        val needles = when (source) {
            DownloadSource.AURORA,
            DownloadSource.PLAY -> listOf("play.google.com")
            DownloadSource.APK_MIRROR -> listOf("apkmirror.com", "google.com/search")
            DownloadSource.APTOIDE -> listOf("aptoide.com")
            DownloadSource.APK_PURE -> listOf("apkpure.com")
            DownloadSource.UPTODOWN -> listOf("uptodown.com")
            DownloadSource.APK_CUBE -> listOf("apkcube.com")
            DownloadSource.EVOZI -> listOf("apkcube.com", "evozi.com")
            DownloadSource.MORPHE_LINK -> listOf("morphe.software")
        }

        return (sourceHintUrls + fallbackWebUrl).distinct().filter { url ->
            needles.any { needle -> url.contains(needle, ignoreCase = true) }
        }
    }

    companion object {
        fun from(intent: Intent): HelperRequest? {
            if (intent.action != DownloadHelperContract.ACTION_DOWNLOAD_ORIGINAL_APK) return null
            val packageName = intent.getStringExtra(DownloadHelperContract.EXTRA_PACKAGE_NAME)
                ?.takeIf { it.isNotBlank() }
                ?: return null

            return HelperRequest(
                callerPackage = intent.getStringExtra(DownloadHelperContract.EXTRA_CALLER_PACKAGE)
                    ?: "app.morphe.manager",
                packageName = packageName,
                appName = intent.getStringExtra(DownloadHelperContract.EXTRA_APP_NAME) ?: packageName,
                versionName = intent.getStringExtra(DownloadHelperContract.EXTRA_VERSION_NAME),
                versionCode = if (intent.hasExtra(DownloadHelperContract.EXTRA_VERSION_CODE)) {
                    // Callers send the version code as either an Int (am --ei, some
                    // app stores) or a Long. getLongExtra silently returns the
                    // default for Int extras, so try Long first and fall back to Int.
                    intent.getLongExtra(DownloadHelperContract.EXTRA_VERSION_CODE, 0L)
                        .takeIf { it > 0L }
                        ?: intent.getIntExtra(DownloadHelperContract.EXTRA_VERSION_CODE, 0)
                            .toLong()
                            .takeIf { it > 0L }
                } else {
                    null
                },
                versionCodes = intent
                    .getLongArrayExtra(DownloadHelperContract.EXTRA_VERSION_CODES)
                    ?.filter { it > 0L }
                    ?.toSet()
                    .orEmpty(),
                compatibleVersionNames = intent
                    .getStringArrayListExtra(DownloadHelperContract.EXTRA_COMPATIBLE_VERSION_NAMES)
                    ?.toSet()
                    .orEmpty(),
                compatibleVersionCodes = intent
                    .getLongArrayExtra(DownloadHelperContract.EXTRA_COMPATIBLE_VERSION_CODES)
                    ?.filter { it > 0L }
                    ?.toSet()
                    .orEmpty(),
                supportedAbis = intent
                    .getStringArrayExtra(DownloadHelperContract.EXTRA_SUPPORTED_ABIS)
                    ?.toList()
                    .orEmpty(),
                requestedFileType = intent.getStringExtra(DownloadHelperContract.EXTRA_FILE_TYPE)
                    ?: intent.getStringExtra(DownloadHelperContract.EXTRA_REQUESTED_FILE_TYPE),
                allowSplitArchive = intent.getBooleanExtra(DownloadHelperContract.EXTRA_ALLOW_SPLIT_ARCHIVE, false),
                stockInstallRequired = intent.getBooleanExtra(
                    DownloadHelperContract.EXTRA_STOCK_INSTALL_REQUIRED,
                    intent.getBooleanExtra(DownloadHelperContract.EXTRA_INSTALL_STOCK_AFTER_DOWNLOAD, false)
                ),
                fallbackWebUrl = intent.getStringExtra(DownloadHelperContract.EXTRA_FALLBACK_WEB_URL)
                    ?: "https://www.apkmirror.com/?post_type=app_release&searchtype=app&s=$packageName",
                sourceHintUrls = intent
                    .getStringArrayListExtra(DownloadHelperContract.EXTRA_SOURCE_HINT_URLS)
                    .orEmpty()
            )
        }
    }
}

internal data class FailedAttempt(
    val source: DownloadSource,
    val option: CandidateOption
)

internal data class CandidateResult(
    val sourceGroups: List<SourceCandidateGroup>
) {
    fun withResolveState(
        source: DownloadSource,
        option: CandidateOption,
        state: ResolveState
    ): CandidateResult = copy(
        sourceGroups = sourceGroups.map { group ->
            if (group.source != source) {
                group
            } else {
                when (option) {
                    CandidateOption.REQUESTED -> group.copy(recommended = state)
                    CandidateOption.LATEST -> group.copy(latest = state)
                    CandidateOption.MANUAL -> group
                }
            }
        }
    )

    fun withHistoryState(
        source: DownloadSource,
        state: VersionHistoryState
    ): CandidateResult = copy(
        sourceGroups = sourceGroups.map { group ->
            if (group.source == source) group.copy(history = state) else group
        }
    )

    fun markHistoryCandidateNoDirectDownload(
        source: DownloadSource,
        candidateKey: String
    ): CandidateResult = copy(
        sourceGroups = sourceGroups.map { group ->
            if (group.source == source && group.history is VersionHistoryState.Done) {
                val done = group.history as VersionHistoryState.Done
                group.copy(
                    history = done.copy(
                        noDirectDownloadKeys = done.noDirectDownloadKeys + candidateKey
                    )
                )
            } else {
                group
            }
        }
    )
}

internal data class SourceCandidateGroup(
    val source: DownloadSource,
    val manual: List<DownloadCandidate>,
    val recommended: ResolveState,
    val latest: ResolveState,
    val history: VersionHistoryState = VersionHistoryState.Idle
)

internal data class ResolveOutcome(
    val candidates: List<DownloadCandidate>,
    val errorMessage: String? = null,
    val fallbackCandidate: DownloadCandidate? = null,
    val notFoundMessage: String? = null
)

internal sealed interface ResolveState {
    data object Idle : ResolveState
    data object Loading : ResolveState
    data class Done(val candidates: List<DownloadCandidate>) : ResolveState
    data class Error(
        val message: String,
        val fallbackCandidate: DownloadCandidate? = null
    ) : ResolveState
}

internal sealed interface VersionHistoryState {
    data object Idle : VersionHistoryState
    data object Loading : VersionHistoryState
    data class Done(
        val candidates: List<DownloadCandidate>,
        // Identity keys of candidates whose direct download could not be
        // resolved; those rows render an "Open link" action instead of
        // "Download" so the user is not stuck in a resolve-then-error loop.
        val noDirectDownloadKeys: Set<String> = emptySet()
    ) : VersionHistoryState
    data class Error(val message: String) : VersionHistoryState
}



internal data class ApkMirrorLatestInfo(
    val versionName: String?,
    val openUrl: String
)

internal data class ApkMirrorVariant(
    val url: String,
    val type: String,
    val fileKind: String,
    val arch: String?,
    val dpi: String?,
    val isBundle: Boolean,
    /** Build code APKMirror prints in the variant row, when present. */
    val versionCode: Long? = null,
    /** Position of this row in the release page's variant table, 1-based. */
    val number: Int = 0
)

internal data class UptodownVersionResponse(
    val data: List<UptodownVersionEntry> = emptyList()
)

internal data class UptodownVersionEntry(
    @SerializedName("fileID")
    val fileId: Long? = null,
    val version: String? = null,
    @SerializedName("kindFile")
    val kindFile: String? = null,
    @SerializedName("titleKindFile")
    val titleKindFile: String? = null,
    @SerializedName("versionURL")
    val versionUrl: UptodownVersionUrl? = null
)

internal data class UptodownVersionUrl(
    val url: String? = null,
    @SerializedName("extraURL")
    val extraUrl: String? = null,
    @SerializedName("versionID")
    val versionId: Long? = null
)

internal data class UptodownVariantResponse(
    val content: String? = null
)

internal data class UptodownVariantFile(
    val fileId: String,
    val fileKind: String,
    val archLabel: String?
)

internal data class DownloadCandidate(
    val source: DownloadSource,
    val name: String,
    val packageName: String,
    val versionName: String?,
    val versionCode: Long?,
    val url: String,
    val fileKind: String,
    val option: CandidateOption,
    val directDownload: Boolean,
    val versionStatus: VersionStatus,
    val formatMatches: Boolean,
    val note: String? = null,
    val variantLabel: String? = null,
    /** Source's own wording for the variant (APKMirror's arch / DPI), for display. */
    val variantTag: String? = null,
    /** Which variant of the release this is, when the source offered several. */
    val variantNumber: Int? = null,
    val files: List<CandidateDownloadFile> = emptyList(),
    // When set, the source gates the file behind a captcha/JS challenge that
    // only a real browser can pass. The card then offers a "Solve captcha in
    // app" action that opens this URL in an embedded WebView and captures the
    // download the page produces.
    val captchaUrl: String? = null,
    // The source whose page a wrapper such as Morphe Link resolved. It keeps the
    // candidate branded as the wrapper while still selecting that source's browser
    // script for a captcha page.
    val viaSource: DownloadSource? = null
) {
    val sortIndex: Int get() = source.sortIndex

    /**
     * True when the candidate itself carries a variant marker such as
     * "-SECONDARY"  in the parsed version, the release URL, the variant label
     * or the file name. The version number alone is not enough: APKMirror
     * parses the suffix out of the release slug, so the URL and file name are
     * the only reliable signals.
     */
    val hasVariantBuildMarker: Boolean
        get() = versionName.hasVariantBuildMarker() ||
            url.hasVariantBuildMarker() ||
            variantLabel.hasVariantBuildMarker() ||
            files.any { it.fileName.hasVariantBuildMarker() || it.url.hasVariantBuildMarker() }

    val versionDisplay: String
        get() = when {
            versionName != null && versionCode != null -> "$versionName ($versionCode)"
            versionName != null -> versionName
            option == CandidateOption.MANUAL -> "Manual"
            option == CandidateOption.REQUESTED -> "Requested search"
            else -> "Latest"
        }
}

/**
 * Candidates from a newly picked source that continue the same download target:
 * same format, same version for a requested build, and the ABI/variant already
 * in flight. Fast Mode's latest mode deliberately leaves the version open,
 * because the new source resolves its own newest release.
 */
internal fun matchingSourceSwitchCandidates(
    candidates: List<DownloadCandidate>,
    target: DownloadCandidate,
    request: HelperRequest
): List<DownloadCandidate> =
    candidates.filter { candidate ->
        candidate.directDownload &&
            candidate.fileKind.equals(target.fileKind, ignoreCase = true) &&
            sourceSwitchVersionMatches(target, candidate) &&
            sourceSwitchVariantMatches(target, candidate, request)
    }

internal fun sourceSwitchVersionMatches(
    target: DownloadCandidate,
    candidate: DownloadCandidate
): Boolean {
    if (target.option == CandidateOption.LATEST) return true
    val targetVersion = target.versionName ?: return true
    return candidate.versionName?.versionNameEquals(targetVersion) == true
}

internal fun sourceSwitchVariantMatches(
    target: DownloadCandidate,
    candidate: DownloadCandidate,
    request: HelperRequest
): Boolean {
    val targetLabel = target.variantLabel ?: target.variantTag
    val candidateLabel = candidate.variantLabel ?: candidate.variantTag

    if (target.source == candidate.source) {
        if (target.variantNumber != null && candidate.variantNumber != null) {
            return target.variantNumber == candidate.variantNumber
        }
        if (targetLabel != null || candidateLabel != null) {
            return targetLabel.normalizedVariantLabel() == candidateLabel.normalizedVariantLabel()
        }
        return true
    }

    // Labels differ between sources ("arm64-v8a", "ARM64", "arm64"), so
    // compare how each one fits the device instead of the wording.
    return archMatchRank(targetLabel, request.availableAbis) ==
        archMatchRank(candidateLabel, request.availableAbis)
}

internal fun String?.normalizedVariantLabel(): String =
    this?.trim()?.lowercase(Locale.US)?.replace(Regex("\\s+"), " ").orEmpty()

internal data class CandidateMatchSummary(
    val matches: Boolean,
    val title: String,
    val details: List<String>
)

internal fun DownloadCandidate.matchSummary(request: HelperRequest): CandidateMatchSummary {
    val requestedVersionNames = request.requestedVersionNames
    val versionNameMatches = requestedVersionNames.isEmpty() ||
        (versionName != null && requestedVersionNames.any { versionName.versionNameEquals(it) })
    val requestedVersionCodes = request.requestedVersionCodes
    val versionCodeMatches = requestedVersionCodes.isEmpty() ||
        versionCode == null ||
        versionCode in requestedVersionCodes
    val formatMatches = fileKind.equals("web", ignoreCase = true) ||
        request.acceptsCandidateFormat(fileKind)

    val mismatchNames = buildList {
        if (!versionNameMatches) add("Version")
        if (!versionCodeMatches) add("Version code")
        if (!formatMatches) add("Format")
    }
    val details = buildList {
        if (!versionNameMatches) {
            add("Version: requested ${requestedVersionNames.joinToString().ifBlank { "Any" }}, found ${versionName ?: "Unknown"}")
        }
        if (!versionCodeMatches) {
            add("Version code: requested ${requestedVersionCodes.joinToString()}, found $versionCode")
        }
        if (!formatMatches) {
            add("Format: requested ${request.requestedFormatLabel}, found ${fileKind.uppercase()}")
        }
    }
    val matches = mismatchNames.isEmpty()

    return CandidateMatchSummary(
        matches = matches,
        title = if (matches) {
            "Same as recommended version"
        } else {
            "${mismatchNames.joinToString(" / ")} mismatch"
        },
        details = details
    )
}

internal fun DownloadCandidate.identityKey(): String =
    "${source.name}:$versionName:$versionCode:$fileKind:$variantLabel:$url"

internal data class CandidateDownloadFile(
    val url: String,
    val fileName: String,
    val size: Long? = null,
    val referer: String? = null,
    val cookieHeader: String? = null,
    // The SHA-256 the source publishes for the file, when it does. When set,
    // the downloaded bytes are checked against it before handoff, catching
    // corrupted or wrong builds outright instead of only a package/version
    // mismatch. (Sources that publish it: APKMirror variant page, Uptodown
    // download page.)
    val expectedSha256: String? = null
)

internal data class BrowserDownloadCapture(
    val downloadUrl: String,
    val refererUrl: String? = null,
    val cookieHeader: String? = null
)

internal fun capturedDownloadFileName(
    candidate: DownloadCandidate,
    downloadUrl: String,
    fileKind: String
): String {
    val decodedUrl = runCatching { Uri.decode(downloadUrl) }.getOrDefault(downloadUrl)
    val path = runCatching { java.net.URI(decodedUrl).path }.getOrDefault("")
    val fileName = path.substringAfterLast('/').takeIf { it.isNotBlank() && it != "/" }
    val extension = fileName?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() } ?: fileKind
    val baseName = fileName?.takeIf { it.contains('.') }
        ?: "${candidate.packageName}-${candidate.versionName ?: "download"}.$extension"
    return baseName.sanitizeFileName()
}

internal data class DownloadedApkMetadata(
    val packageName: String,
    val versionName: String?,
    val versionCode: Long?,
    val minSdkVersion: Int? = null
)

internal enum class DownloadSource(
    val label: String,
    val sortIndex: Int,
    val supportsManualArtifactPicker: Boolean = true
) {
    APK_MIRROR("APKMirror", 0),
    UPTODOWN("Uptodown", 1),
    APK_PURE("APKPure", 2),
    APK_CUBE("APKCube", 3),
    APTOIDE("Aptoide", 4),
    EVOZI("Evozi", 5),
    MORPHE_LINK("Morphe Link", 6, supportsManualArtifactPicker = false),
    AURORA("Aurora", 7, supportsManualArtifactPicker = false),
    PLAY("Play", 8, supportsManualArtifactPicker = false)
}

internal fun DownloadSource.searchDomain(): String? = when (this) {
    DownloadSource.APK_MIRROR -> "apkmirror.com"
    DownloadSource.UPTODOWN -> "uptodown.com"
    DownloadSource.APK_PURE -> "apkpure.com"
    DownloadSource.APK_CUBE -> "apkcube.com"
    DownloadSource.APTOIDE -> "aptoide.com"
    DownloadSource.EVOZI -> "apkcube.com"
    DownloadSource.MORPHE_LINK -> null
    DownloadSource.PLAY -> "play.google.com"
    DownloadSource.AURORA -> null
}

internal enum class CandidateOption {
    MANUAL,
    REQUESTED,
    LATEST
}

internal val CandidateOption.labelForLogs: String
    get() = when (this) {
        CandidateOption.MANUAL -> "manual"
        CandidateOption.REQUESTED -> "recommended"
        CandidateOption.LATEST -> "latest"
    }

internal enum class VersionStatus(val label: String) {
    REQUESTED("Requested"),
    COMPATIBLE("Compatible"),
    LATEST("Latest")
}

@Composable
internal fun LogLevel.color(): Color = when (this) {
    LogLevel.Info -> MaterialTheme.colorScheme.onSurfaceVariant
    LogLevel.Warning -> SemanticTone.Warning.accent
    LogLevel.Error -> MaterialTheme.colorScheme.error
}

internal sealed interface UiState {
    data object Idle : UiState
    data object Loading : UiState
    data class Ready(val result: CandidateResult) : UiState
    data class CheckingPickedFile(val candidate: DownloadCandidate) : UiState
    data class Downloading(
        val candidate: DownloadCandidate,
        val percent: Int,
        val speedBytesPerSec: Double = 0.0,
        val etaMs: Long? = null,
        val statusMessage: String? = null,
        val paused: Boolean = false,
        val connections: Int = 1
    ) : UiState
    data class Error(val message: String) : UiState
    data class FastMode(val progress: FastModeProgress) : UiState
    data class ScanResult(
        val candidate: DownloadCandidate,
        val scanResult: VirusTotalScanner.ScanResult,
        val detail: String,
        val isMalicious: Boolean,
        /** True if the downloaded file's bytes matched the source-published SHA-256. */
        val shaVerified: Boolean = false
    ) : UiState
    data class ScanAsk(val candidate: DownloadCandidate) : UiState
}

internal enum class FastModeChoice { USE, NEXT }

internal sealed interface FastModeFindResult {
    data class Exact(val candidate: DownloadCandidate) : FastModeFindResult
    data class VersionMismatch(val candidate: DownloadCandidate) : FastModeFindResult
    object None : FastModeFindResult
}

internal data class SwitchCandidatePrompt(
    val source: DownloadSource,
    val detail: String,
    val candidates: List<DownloadCandidate>
)

internal data class FastModeProgress(
    val sourceLabel: String? = null,
    val detail: String = "Starting…",
    val percent: Int? = null,
    val speedBytesPerSec: Double = 0.0,
    val etaMs: Long? = null,
    val paused: Boolean = false,
    val connections: Int = 1,
    val done: Boolean = false,
    val succeeded: Boolean = false,
    // Present once Fast Mode finishes so the regular source list stays reachable.
    val result: CandidateResult? = null,
    // Set while Fast Mode waits for the user to accept a version-code mismatch
    // or to solve a captcha for an exact captcha-gated candidate.
    val awaitingDecision: Boolean = false,
    val awaitingCaptcha: Boolean = false,
    val mismatchDetail: String? = null,
    // True while Fast Mode (ALWAYS_ASK) asks which version to fetch.
    val awaitingVersionChoice: Boolean = false,
    val versionChoiceDetail: String? = null,
    // The requested version shown on the ALWAYS_ASK prompt, so the user can
    // compare it against the newest version each source offers.
    val versionChoiceRequested: String? = null,
    // True once the downloaded file's bytes matched the source-published SHA-256;
    // shown persistently on the card (not just the transient post-download status).
    val shaVerified: Boolean = false,
    // Reused by the automatic retry flow so the card does not claim Fast Mode.
    val title: String? = null
)

internal object DownloadHelperContract {
    const val ACTION_DOWNLOAD_ORIGINAL_APK = "app.morphe.manager.action.DOWNLOAD_ORIGINAL_APK"
    const val EXTRA_PROTOCOL_VERSION = "app.morphe.manager.extra.PROTOCOL_VERSION"
    const val EXTRA_CALLER_PACKAGE = "app.morphe.manager.extra.CALLER_PACKAGE"
    const val EXTRA_PACKAGE_NAME = "app.morphe.manager.extra.PACKAGE_NAME"
    const val EXTRA_APP_NAME = "app.morphe.manager.extra.APP_NAME"
    const val EXTRA_VERSION_NAME = "app.morphe.manager.extra.VERSION_NAME"
    const val EXTRA_VERSION_CODE = "app.morphe.manager.extra.VERSION_CODE"
    const val EXTRA_VERSION_CODES = "app.morphe.manager.extra.VERSION_CODES"
    const val EXTRA_COMPATIBLE_VERSION_NAMES = "app.morphe.manager.extra.COMPATIBLE_VERSION_NAMES"
    const val EXTRA_COMPATIBLE_VERSION_CODES = "app.morphe.manager.extra.COMPATIBLE_VERSION_CODES"
    const val EXTRA_SUPPORTED_ABIS = "app.morphe.manager.extra.SUPPORTED_ABIS"
    const val EXTRA_FILE_TYPE = "app.morphe.manager.extra.FILE_TYPE"
    const val EXTRA_REQUESTED_FILE_TYPE = "app.morphe.manager.extra.REQUESTED_FILE_TYPE"
    const val EXTRA_ALLOW_SPLIT_ARCHIVE = "app.morphe.manager.extra.ALLOW_SPLIT_ARCHIVE"
    const val EXTRA_STOCK_INSTALL_REQUIRED = "app.morphe.manager.extra.STOCK_INSTALL_REQUIRED"
    const val EXTRA_INSTALL_STOCK_AFTER_DOWNLOAD = "app.morphe.manager.extra.INSTALL_STOCK_AFTER_DOWNLOAD"
    const val EXTRA_FALLBACK_WEB_URL = "app.morphe.manager.extra.FALLBACK_WEB_URL"
    const val EXTRA_SOURCE_HINT_URLS = "app.morphe.manager.extra.SOURCE_HINT_URLS"
    const val EXTRA_RESULT_USE_INSTALLED_APP = "app.morphe.manager.extra.RESULT_USE_INSTALLED_APP"
    const val EXTRA_RESULT_PACKAGE_NAME = "app.morphe.manager.extra.RESULT_PACKAGE_NAME"
    const val EXTRA_RESULT_VERSION_NAME = "app.morphe.manager.extra.RESULT_VERSION_NAME"
    const val EXTRA_RESULT_SOURCE_NAME = "app.morphe.manager.extra.RESULT_SOURCE_NAME"
    const val EXTRA_RESULT_FILE_NAME = "app.morphe.manager.extra.RESULT_FILE_NAME"
}

/**
 * APKMirror's own app-exists endpoint. It returns the latest stable release and its
 * variant pages as JSON, so the latest flow does not have to walk the search, app and
 * uploads pages. The endpoint is private and authenticated with APKUpdater's public
 * client credential; every caller treats a failure as "scrape as before".
 */



