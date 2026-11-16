package dev.rushi.apkdownloadhelper

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.CookieManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import dev.rushi.apkdownloadhelper.download.AdGuardDns
import dev.rushi.apkdownloadhelper.download.SessionCookies
import dev.rushi.apkdownloadhelper.download.VirusTotalScanner
import dev.rushi.apkdownloadhelper.download.VirusTotalScanner.isEntirelyClean
import dev.rushi.apkdownloadhelper.download.belongsTo
import dev.rushi.apkdownloadhelper.download.belongsToCurrentSession
import dev.rushi.apkdownloadhelper.download.compareVersionNames
import dev.rushi.apkdownloadhelper.download.downloadFailureMessage
import dev.rushi.apkdownloadhelper.download.fileKindFromUrlOrNull
import dev.rushi.apkdownloadhelper.download.httpLoggingInterceptor
import dev.rushi.apkdownloadhelper.download.isPackageInstalled
import dev.rushi.apkdownloadhelper.download.orderedFileKinds
import dev.rushi.apkdownloadhelper.download.playStoreUrl
import dev.rushi.apkdownloadhelper.download.sanitizeFileName
import dev.rushi.apkdownloadhelper.download.sourceFailureMessage
import dev.rushi.apkdownloadhelper.download.versionNameEquals
import dev.rushi.apkdownloadhelper.download.withManualModeHint
import dev.rushi.apkdownloadhelper.play.PlayHttpClient
import dev.rushi.apkdownloadhelper.sources.ARCH_RANK_MISMATCH
import dev.rushi.apkdownloadhelper.sources.ApkCubeParser
import dev.rushi.apkdownloadhelper.sources.ApkMirrorApi
import dev.rushi.apkdownloadhelper.sources.ApkMirrorParser
import dev.rushi.apkdownloadhelper.sources.ApkPureApi
import dev.rushi.apkdownloadhelper.sources.ApkPureParser
import dev.rushi.apkdownloadhelper.sources.ApkSourceParser
import dev.rushi.apkdownloadhelper.sources.AptoideApi
import dev.rushi.apkdownloadhelper.sources.AptoideParser
import dev.rushi.apkdownloadhelper.sources.AuroraParser
import dev.rushi.apkdownloadhelper.sources.EvoziParser
import dev.rushi.apkdownloadhelper.sources.MorpheLinkParser
import dev.rushi.apkdownloadhelper.sources.MorpheResolver
import dev.rushi.apkdownloadhelper.sources.OkHttpSourceTextFetcher
import dev.rushi.apkdownloadhelper.sources.SourceAppNotFoundException
import dev.rushi.apkdownloadhelper.sources.SourceParserContext
import dev.rushi.apkdownloadhelper.sources.UptodownParser
import dev.rushi.apkdownloadhelper.sources.archMatchRank
import dev.rushi.apkdownloadhelper.storage.AppLog
import dev.rushi.apkdownloadhelper.storage.DownloadHistoryEntry
import dev.rushi.apkdownloadhelper.storage.DownloadHistoryStore
import dev.rushi.apkdownloadhelper.storage.FastModePolicy
import dev.rushi.apkdownloadhelper.storage.HelperSettings
import dev.rushi.apkdownloadhelper.storage.LogLevel
import dev.rushi.apkdownloadhelper.storage.ThemeMode
import dev.rushi.apkdownloadhelper.storage.ThemeStyle
import dev.rushi.apkdownloadhelper.storage.UpdateMode
import dev.rushi.apkdownloadhelper.storage.cacheHandoff
import dev.rushi.apkdownloadhelper.storage.cleanupDownloads
import dev.rushi.apkdownloadhelper.storage.ensureLauncherIconState
import dev.rushi.apkdownloadhelper.storage.loadHelperSettings
import dev.rushi.apkdownloadhelper.storage.logcatLoggingEnabled
import dev.rushi.apkdownloadhelper.storage.orderedSources
import dev.rushi.apkdownloadhelper.storage.prepareDownloadHandoff
import dev.rushi.apkdownloadhelper.storage.recordHandOff
import dev.rushi.apkdownloadhelper.storage.restoreAndPersistRateLimiter
import dev.rushi.apkdownloadhelper.storage.saveHelperSettings
import dev.rushi.apkdownloadhelper.storage.scheduleHandoffCleanup
import dev.rushi.apkdownloadhelper.storage.setLauncherIconVisible
import dev.rushi.apkdownloadhelper.storage.temporaryDownloadsDir
import dev.rushi.apkdownloadhelper.ui.AdvancedTabContent
import dev.rushi.apkdownloadhelper.ui.AppBrowserScreen
import dev.rushi.apkdownloadhelper.ui.AppearanceTabContent
import dev.rushi.apkdownloadhelper.ui.BrowserDownloadCapture
import dev.rushi.apkdownloadhelper.ui.CandidateDownloadFile
import dev.rushi.apkdownloadhelper.ui.CandidateOption
import dev.rushi.apkdownloadhelper.ui.CandidateResult
import dev.rushi.apkdownloadhelper.ui.CaptchaBrowserScreen
import dev.rushi.apkdownloadhelper.ui.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.DownloadHelperContract
import dev.rushi.apkdownloadhelper.ui.DownloadSource
import dev.rushi.apkdownloadhelper.ui.FailedAttempt
import dev.rushi.apkdownloadhelper.ui.FastModeChoice
import dev.rushi.apkdownloadhelper.ui.FastModeFindResult
import dev.rushi.apkdownloadhelper.ui.FastModeProgress
import dev.rushi.apkdownloadhelper.ui.HelperRequest
import dev.rushi.apkdownloadhelper.ui.HelperScreen
import dev.rushi.apkdownloadhelper.ui.HomePagerScreen
import dev.rushi.apkdownloadhelper.ui.LocalMonochromeTheme
import dev.rushi.apkdownloadhelper.ui.LogsTabContent
import dev.rushi.apkdownloadhelper.ui.MorpheAnimations
import dev.rushi.apkdownloadhelper.ui.MorpheDefaults
import dev.rushi.apkdownloadhelper.ui.MorpheDialogButton
import dev.rushi.apkdownloadhelper.ui.MorphePushedScreen
import dev.rushi.apkdownloadhelper.ui.MorpheTypography
import dev.rushi.apkdownloadhelper.ui.ResolveOutcome
import dev.rushi.apkdownloadhelper.ui.ResolveState
import dev.rushi.apkdownloadhelper.ui.ReuseOfferDialog
import dev.rushi.apkdownloadhelper.ui.ReuseOption
import dev.rushi.apkdownloadhelper.ui.SettingsCategory
import dev.rushi.apkdownloadhelper.ui.SettingsPage
import dev.rushi.apkdownloadhelper.ui.SettingsPageDetail
import dev.rushi.apkdownloadhelper.ui.SourceCandidateGroup
import dev.rushi.apkdownloadhelper.ui.SourcesSettingsScreen
import dev.rushi.apkdownloadhelper.ui.SwitchCandidateDialog
import dev.rushi.apkdownloadhelper.ui.SwitchCandidatePrompt
import dev.rushi.apkdownloadhelper.ui.SystemTabContent
import dev.rushi.apkdownloadhelper.ui.UiState
import dev.rushi.apkdownloadhelper.ui.UpdatePromptDialog
import dev.rushi.apkdownloadhelper.ui.UpdateScreen
import dev.rushi.apkdownloadhelper.ui.VersionHistoryState
import dev.rushi.apkdownloadhelper.ui.VersionStatus
import dev.rushi.apkdownloadhelper.ui.browserBuildMismatchMessage
import dev.rushi.apkdownloadhelper.ui.capturedDownloadFileName
import dev.rushi.apkdownloadhelper.ui.formatBytes
import dev.rushi.apkdownloadhelper.ui.identityKey
import dev.rushi.apkdownloadhelper.ui.labelForLogs
import dev.rushi.apkdownloadhelper.ui.matchingSourceSwitchCandidates
import dev.rushi.apkdownloadhelper.ui.monochromeDarkColorScheme
import dev.rushi.apkdownloadhelper.ui.monochromeLightColorScheme
import dev.rushi.apkdownloadhelper.ui.morpheDarkColorScheme
import dev.rushi.apkdownloadhelper.ui.morpheLightColorScheme
import dev.rushi.apkdownloadhelper.ui.scanResultDetail
import dev.rushi.apkdownloadhelper.ui.searchDomain
import dev.rushi.apkdownloadhelper.ui.toAccentColorOrNull
import dev.rushi.apkdownloadhelper.ui.withCustomAccent
import dev.rushi.apkdownloadhelper.ui.withPureBlack
import dev.rushi.apkdownloadhelper.updater.UpdateInfo
import dev.rushi.apkdownloadhelper.updater.UpdateManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


internal const val AURORA_AUTH_URL = "https://auroraoss.com/api/auth"
internal const val TAG = "ApkDownloadHelper"
internal val DOWNLOAD_FILE_KIND_ORDER = listOf("apk", "apkm", "apks", "xapk")
internal val DOWNLOAD_FILE_KIND_SET = DOWNLOAD_FILE_KIND_ORDER.toSet()
internal val SPLIT_ARCHIVE_FILE_KINDS = setOf("apkm", "apks", "xapk")
internal val DOWNLOAD_FILE_KIND_REGEX = Regex("""apkm|apks|xapk|apk""", RegexOption.IGNORE_CASE)
internal val gson = Gson()

// Morphe Manager installs to test against: release first, debug as fallback.
internal val MORPHE_MANAGER_PACKAGES = listOf(
    "app.morphe.manager",
    "app.morphe.manager.debug"
)

// Where to point the user when Morphe Manager is not installed at all.
internal const val MORPHE_MANAGER_SITE_URL =
    "https://morphe.software/"

internal val APK_PICKER_MIME_TYPES = arrayOf(
    "application/vnd.android.package-archive",
    "application/zip",
    "application/octet-stream",
    "*/*"
)

class MainActivity : ComponentActivity() {
    /** APKMirror declares `Crawl-delay: 3`; stay below ~1 request/2.5s. */
    companion object {
        const val APKMIRROR_REQUEST_GAP_MS = 2500L
    }

    private val browserUserAgent =
        "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL}) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36"

    /** APKMirror challenges browser user agents but serves this one normally. */
    private val apkMirrorUserAgent = "HelperForMorphe/1.0"

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .dns(AdGuardDns)
        .cookieJar(SessionCookies)
        .addInterceptor { chain ->
            val host = chain.request().url.host.lowercase(Locale.US)
            val userAgent = if (host == "apkmirror.com" || host.endsWith(".apkmirror.com")) {
                apkMirrorUserAgent
            } else {
                browserUserAgent
            }
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", userAgent)
                    .build()
            )
        }
        .addInterceptor(httpLoggingInterceptor("Web"))
        .build()
    private val apkPureClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .dns(AdGuardDns)
        .cookieJar(SessionCookies)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "APKPure/3.19.39 (Aegon)")
                    .build()
            )
        }
        .addInterceptor(httpLoggingInterceptor("APKPure"))
        .build()

    private val apkPureApi = Retrofit.Builder()
        .client(apkPureClient)
        .baseUrl("https://tapi.pureapk.com/")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(ApkPureApi::class.java)

    /** One hop only: APKPure's direct endpoint answers with the file URL in its Location. */
    private val apkPureDirectClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .dns(AdGuardDns)
        .cookieJar(SessionCookies)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", browserUserAgent)
                    .build()
            )
        }
        .addInterceptor(httpLoggingInterceptor("APKPure"))
        .build()

    private val aptoideApi = Retrofit.Builder()
        .client(client)
        .baseUrl("https://ws75.aptoide.com/api/7/")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(AptoideApi::class.java)

    private val apkMirrorApi = Retrofit.Builder()
        .client(client)
        .baseUrl("https://www.apkmirror.com/")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(ApkMirrorApi::class.java)

    private val parsers: Map<DownloadSource, ApkSourceParser> by lazy {
        val playHttpClient = PlayHttpClient(Cache(File(cacheDir, "play-cache"), 64L * 1024L * 1024L))
        val parserContext = SourceParserContext(
            fetcher = OkHttpSourceTextFetcher(
                client,
                // APKMirror declares `Crawl-delay: 3` and answers 429 to denser
                // probe runs, so give it a slower pace than the app-wide default.
                hostGapsMillis = mapOf("www.apkmirror.com" to APKMIRROR_REQUEST_GAP_MS)
            ),
            apkPureApi = apkPureApi,
            aptoideApi = aptoideApi,
            apkMirrorApi = apkMirrorApi,
            playHttpClient = playHttpClient,
            appContext = this,
            // Read through the settings each time, so flipping the switch takes effect on
            // the next request instead of needing a restart.
            resolveViaMorphe = { request ->
                if (helperSettings.useMorpheResolver) MorpheResolver.resolve(request) else null
            },
            isSourceEnabled = { source -> source !in effectiveDisabledSources },
            // APKPure's direct endpoint names the file itself in one hop.
            resolveRedirect = { url ->
                apkPureDirectClient.newCall(
                    Request.Builder().url(url).head().build()
                ).execute().use { response -> response.header("Location") }
            }
        )
        val apkMirrorParser = ApkMirrorParser(parserContext)
        val uptodownParser = UptodownParser(parserContext)
        val apkPureParser = ApkPureParser(parserContext)
        val apkCubeParser = ApkCubeParser(parserContext)
        val aptoideParser = AptoideParser(parserContext)
        val evoziParser = EvoziParser(parserContext)
        listOf(
            apkMirrorParser,
            uptodownParser,
            apkPureParser,
            apkCubeParser,
            aptoideParser,
            evoziParser,
            MorpheLinkParser(
                ctx = parserContext,
                delegates = listOf(
                    apkMirrorParser,
                    uptodownParser,
                    apkPureParser,
                    apkCubeParser,
                    aptoideParser,
                    evoziParser
                )
            ),
            AuroraParser(parserContext)
        ).associateBy { it.source }
    }

    private var request by mutableStateOf<HelperRequest?>(null)
    private var uiState by mutableStateOf<UiState>(UiState.Idle)
    private var helperSettings by mutableStateOf(HelperSettings())
    private var installedPackageRefreshToken by mutableIntStateOf(0)
    private var showUpdates by mutableStateOf(false)
    private var updatePrompt by mutableStateOf<UpdateInfo?>(null)
    private var updateRefreshToken by mutableIntStateOf(0)
    private val historyTimeFormat = SimpleDateFormat("MMM d, HH:mm", Locale.US)
    private var pendingDownload: PendingDownload? = null
    // Source picked from the download screen; the next Cancelled event starts it.
    private var pendingSwitchSource: DownloadSource? = null
    // The file the current download was fetching, kept so a source switch can
    // resolve the same version mode and variant instead of starting over from
    // whatever the new source lists first.
    private var activeDownloadCandidate: DownloadCandidate? = null
    private var pendingSwitchTarget: DownloadCandidate? = null
    private var pendingSwitchOption: CandidateOption? = null
    private var switchCandidatePrompt by mutableStateOf<SwitchCandidatePrompt?>(null)
    // Non-null while the in-app browser is open for a candidate whose source
    // needs a real browser to reach the APK download.
    private var captchaBrowser by mutableStateOf<DownloadCandidate?>(null)
    // Set when a repeat request matches previous downloads that still exist;
    // the user picks one to reuse or chooses to download a fresh copy.
    private var reuseOffer by mutableStateOf<List<ReuseOption>?>(null)
    private var selectedPagerPage by mutableIntStateOf(0)
    private var failedAttempts by mutableStateOf<Set<FailedAttempt>>(emptySet())
    private var lastFailureMessage by mutableStateOf<String?>(null)
    private var lastFailureOption by mutableStateOf<CandidateOption?>(null)
    // Set while Retry all / Retry failed sources is walking sources and
    // downloading. Fast Mode failures keep using fastModeActive instead.
    private var autoRetryActive = false
    private var autoRetryQueue: MutableList<FailedAttempt>? = null
    private var autoRetryTarget: DownloadCandidate? = null
    private var fastModeActive = false
    private var fastModeQueue: MutableList<DownloadSource>? = null
    private var fastModeDecision: CompletableDeferred<FastModeChoice?>? = null
    // ALWAYS_ASK: which version the user picked for the current request.
    private var fastModeVersionDecision: CompletableDeferred<FastModePolicy?>? = null
    // Effective policy for the current run (chosen interactively for ALWAYS_ASK).
    private var fastModeRunPolicy = FastModePolicy.REQUESTED

    /**
     * The effective disabled-source set.  If the user disables *every*
     * source, Play Store is forced back on so the app always has at least
     * one fallback source available.
     */
    private val effectiveDisabledSources: Set<DownloadSource>
        get() {
            val disabled = helperSettings.disabledSources
            return if (disabled.size >= DownloadSource.entries.size) {
                // All sources disabled – keep Play Store as the sole fallback.
                disabled - DownloadSource.PLAY
            } else {
                disabled
            }
        }
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> startPendingDownload() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoreAndPersistRateLimiter()
        helperSettings = loadHelperSettings()
        ensureLauncherIconState()
        request = HelperRequest.from(intent)?.withDeviceDpi(
            resources.displayMetrics.densityDpi,
            helperSettings.preferDensitySpecificVariants
        )
        if (intent?.getBooleanExtra(UpdateManager.EXTRA_OPEN_UPDATES, false) == true) {
            showUpdates = true
        }
        // A Morphe request takes over the screen as soon as it arrives. Its
        // update check still runs and can post a notification, but it must not
        // put an update popup on top of the download flow.
        val normalLaunch = request == null &&
            intent?.action != DownloadHelperContract.ACTION_DOWNLOAD_ORIGINAL_APK
        lifecycleScope.launch {
            val prompt = UpdateManager.checkOnLaunch(applicationContext)
            if (prompt != null &&
                normalLaunch &&
                helperSettings.updatePopup &&
                helperSettings.updateMode == UpdateMode.NOTIFY &&
                !showUpdates
            ) {
                updatePrompt = prompt
            }
        }
        startRequestLog(request)
        lifecycleScope.launch(Dispatchers.IO) {
            val freed = cleanupDownloads(helperSettings)
            if (freed > 0L) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Cleared ${freed.formatBytes()} of old downloads",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        lifecycleScope.launch {
            DownloadJobManager.events.collect(::handleDownloadEvent)
        }
        if (deliverPendingResultIfPresent(request)) return
        if (request != null) offerExistingDownloadIfPresent(request!!)

        setContent {
            var isSettingsSubpageOpen by remember { mutableStateOf(false) }
            var activeSettingsCategory by rememberSaveable { mutableStateOf<SettingsCategory?>(null) }
            var showSourcesScreen by rememberSaveable { mutableStateOf(false) }
            val focusManager = LocalFocusManager.current
            LaunchedEffect(showUpdates) {
                if (showUpdates) focusManager.clearFocus(force = true)
            }
            HelperTheme(
                themeMode = helperSettings.themeMode,
                themeStyle = helperSettings.themeStyle,
                pureBlackTheme = helperSettings.pureBlackTheme,
                accentColorHex = helperSettings.customAccentColor
            ) {
                // Source reachability is checked on demand from Settings, never at
                // launch: a ping to every source's home page is another request in
                // each site's rate-limit pool, spent before the user has asked for
                // anything, and it competes with the download that prompted the app to
                // open. The Settings card keeps a Re-check button for when it is wanted.
                Box(modifier = Modifier.fillMaxSize()) {
                val captcha = captchaBrowser
                if (captcha != null) {
                    CaptchaBrowserScreen(
                        candidate = captcha,
                        request = request,
                        requiredAppName = request?.appName,
                        requiredVersion = request?.requestedVersionName,
                        onClose = ::closeCaptchaBrowser,
                        onDownloadCaptured = ::onBrowserDownloadCaptured,
                        onBuildMismatch = ::onBrowserBuildMismatch,
                        onChallengeTimeout = ::onBrowserChallengeTimeout
                    )
                } else {
                    // Updates is pushed over this screen instead of replacing it, so
                    // tab order and screen readers must not reach the covered Settings
                    // UI. Clearing semantics removes the whole subtree while leaving it
                    // composed, which keeps its tab, scroll position and dialogs intact.
                    val backgroundModifier = if (showUpdates) {
                        Modifier
                            .focusProperties { canFocus = false }
                            .clearAndSetSemantics {}
                    } else {
                        Modifier
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(backgroundModifier)
                    ) {
                                        var bottomBarCovered by remember { mutableStateOf(false) }

                    HomePagerScreen(
                        findAppsContent = { bottomBarHeight ->
                            AppBrowserScreen(
                                onBack = null,
                                modifier = Modifier.padding(bottom = bottomBarHeight)
                            )
                        },
                        homeContent = { bottomBarHeight ->
                            HelperScreen(
                                request = request,
                                state = uiState,
                                settings = helperSettings,
                                virusTotalApiKey = if (helperSettings.virusTotalEnabled) {
                                    helperSettings.virusTotalApiKey
                                } else {
                                    ""
                                },
                                logs = AppLog.entries,
                                installedPackageRefreshToken = installedPackageRefreshToken,
                                selectedPagerPage = selectedPagerPage,
                                onPagerPageChanged = { selectedPagerPage = it },
                                onSettingsChange = ::updateHelperSettings,
                                onOpenUpdates = { showUpdates = true },
                                onRefresh = ::loadCandidates,
                                canRetryFailedSources = failedAttempts.isNotEmpty(),
                                onRetryAll = ::retryAllSources,
                                onRetryFailedSources = ::retryFailedSources,
                                onCopyFailureDetails = ::copyFailureDetails,
                                onResolve = ::resolveCandidates,
                                onDownload = ::downloadAndReturn,
                                onPickDownloadedFile = ::returnPickedFile,
                                onUseInstalledApp = ::returnInstalledApp,
                                onVersionHistory = ::loadVersionHistory,
                                onDownloadVersion = ::downloadVersion,
                                onOpenHistoryEntry = ::openHistoryEntry,
                                onShareHistoryEntry = ::shareHistoryEntry,
                                onClearHistory = { DownloadHistoryStore.clear(applicationContext) },
                                onClearLogs = { AppLog.clear() },
                                onCancel = {
                                    appendLog("Query canceled by user.", LogLevel.Warning)
                                    setResult(Activity.RESULT_CANCELED)
                                    finish()
                                },
                                onCancelDownload = ::cancelDownload,
                                onPauseDownload = ::pauseDownload,
                                onResumeDownload = ::resumeDownload,
                                onSwitchDownloadSource = ::switchDownloadSource,
                                onSkipScanWait = { VirusTotalScanner.rateLimiter.requestSkip() },
                                onCancelFastMode = ::cancelFastMode,
                                onUseFastModeMismatch = { fastModeChoose(FastModeChoice.USE) },
                                onSkipFastModeMismatch = { fastModeChoose(FastModeChoice.NEXT) },
                                onChooseVersion = ::fastModeChooseVersion,
                                onOpenMorphe = ::openMorpheManager,
                                onSolveCaptcha = ::openCaptchaBrowser,
                                onFormatToggle = ::toggleRequestedFileKind,
                                onProceedAfterScan = ::proceedAfterScan,
                                onCancelAfterScan = ::cancelAfterScan,
                                onSkipScan = ::skipScanAndHandoff
                            )
                        },
                        settingsContent = { bottomBarHeight ->
                            SettingsPage(
                                versionName = BuildConfig.VERSION_NAME,
                                onOpenCategory = { activeSettingsCategory = it },
                                onOpenUpdates = { showUpdates = true }
                            )
                        },
                        navigationCovered = {
                            showUpdates ||
                            activeSettingsCategory != null ||
                            showSourcesScreen ||
                            uiState is UiState.Downloading ||
                            uiState is UiState.FastMode ||
                            uiState is UiState.ScanAsk ||
                            uiState is UiState.ScanResult ||
                            (uiState is UiState.Ready && request != null)
                        }
                    )
                    }
                }
                val offer = reuseOffer
                if (offer != null) {
                    ReuseOfferDialog(
                        options = offer,
                        onUseExisting = ::useReuseOffer,
                        // "Download new" (or dismissing) dismisses the offer and,
                        // when Fast Mode is on, starts it only now  never while
                        // the dialog is up, so it cannot download behind it.
                        onDownloadNew = {
                            reuseOffer = null
                            request?.let(::startFastModeIfEnabled)
                        }
                    )
                }
                // Keep the screen beneath Updates composed so Back returns to
                // the exact Settings tab, scroll position and dialogs the user
                // left, instead of rebuilding the main screen.
                AnimatedVisibility(
                    visible = showUpdates,
                    enter = MorpheAnimations.pushEnter,
                    exit = MorpheAnimations.pushExit
                ) {
                    MorphePushedScreen {
                        UpdateScreen(
                            settings = helperSettings,
                            onSettingsChange = ::updateHelperSettings,
                            refreshToken = updateRefreshToken,
                            onBack = { showUpdates = false }
                        )
                    }
                }
                }
                // Overlay for Settings Categories
                AnimatedVisibility(
                    visible = activeSettingsCategory != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    val cat = activeSettingsCategory
                    if (cat != null) {
                        SettingsPageDetail(
                            title = cat.title,
                            onBack = { activeSettingsCategory = null }
                        ) {
                            when (cat) {
                                SettingsCategory.DOWNLOADS -> SystemTabContent(
                                    settings = helperSettings,
                                    onSettingsChange = ::updateHelperSettings,
                                    onOpenUpdates = { showUpdates = true },
                                    onOpenSources = { showSourcesScreen = true }
                                )
                                SettingsCategory.APPEARANCE -> AppearanceTabContent(
                                    settings = helperSettings,
                                    onSettingsChange = ::updateHelperSettings
                                )
                                SettingsCategory.ADVANCED -> AdvancedTabContent(
                                    settings = helperSettings,
                                    onSettingsChange = ::updateHelperSettings
                                )
                                SettingsCategory.LOGS -> LogsTabContent(
                                    settings = helperSettings,
                                    onSettingsChange = ::updateHelperSettings,
                                    logs = AppLog.entries,
                                    onClearLogs = { AppLog.clear() },
                                    historyEntries = DownloadHistoryStore.entries(applicationContext),
                                    onOpenHistoryEntry = ::openHistoryEntry,
                                    onShareHistoryEntry = ::shareHistoryEntry,
                                    onClearHistory = { DownloadHistoryStore.clear(applicationContext) }
                                )
                            }
                        }
                    }
                }

                // Overlay for Sources sub-page
                AnimatedVisibility(
                    visible = showSourcesScreen,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    SourcesSettingsScreen(
                        settings = helperSettings,
                        onSettingsChange = ::updateHelperSettings,
                        onBack = { showSourcesScreen = false }
                    )
                }
                val prompt = updatePrompt
                if (prompt != null) {
                    UpdatePromptDialog(
                        info = prompt,
                        currentVersion = BuildConfig.VERSION_NAME,
                        onView = {
                            updatePrompt = null
                            showUpdates = true
                        },
                        onLater = {
                            UpdateManager.snooze(applicationContext)
                            updatePrompt = null
                        },
                        onSkip = {
                            UpdateManager.skipVersion(applicationContext, prompt.version)
                            updatePrompt = null
                        },
                        onDismiss = {
                            UpdateManager.snooze(applicationContext)
                            updatePrompt = null
                        }
                    )
                }
                val switchPrompt = switchCandidatePrompt
                if (switchPrompt != null) {
                    SwitchCandidateDialog(
                        prompt = switchPrompt,
                        onChoose = ::chooseSwitchCandidate,
                        onDismiss = { switchCandidatePrompt = null }
                    )
                }
            }
        }

        val activeRequest = request
        if (activeRequest != null) {
            loadCandidates()
            if (reuseOffer == null) startFastModeIfEnabled(activeRequest)
        }
    }

    override fun onResume() {
        super.onResume()
        installedPackageRefreshToken++
        updateRefreshToken++
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(UpdateManager.EXTRA_OPEN_UPDATES, false)) {
            showUpdates = true
            updatePrompt = null
            installedPackageRefreshToken++
            return
        }
        // singleTask routes a launcher-icon tap (bare MAIN intent) to this
        // existing instance instead of creating a second task. Ignore it and
        // keep the in-flight request on screen: only a fresh Morphe request
        // replaces the current session.
        if (intent.action != DownloadHelperContract.ACTION_DOWNLOAD_ORIGINAL_APK) {
            installedPackageRefreshToken++
            return
        }
        request = HelperRequest.from(intent)?.withDeviceDpi(
            resources.displayMetrics.densityDpi,
            helperSettings.preferDensitySpecificVariants
        )
        startRequestLog(request)
        val activeRequest = request
        if (activeRequest != null) {
            if (!deliverPendingResultIfPresent(activeRequest)) {
                offerExistingDownloadIfPresent(activeRequest)
                loadCandidates()
                if (reuseOffer == null) startFastModeIfEnabled(activeRequest)
            }
        } else {
            uiState = UiState.Idle
        }
        installedPackageRefreshToken++
    }

    private fun loadCandidates() {
        val activeRequest = request ?: return
        uiState = UiState.Ready(initialCandidateResult(activeRequest))
        appendLog(
            "Ready. Manual links prepared for " +
                "${DownloadSource.entries.count { it !in effectiveDisabledSources }} sources."
        )
    }

    private fun resolveCandidates(source: DownloadSource, option: CandidateOption) {
        val activeRequest = request ?: return
        helperSettings.networkPolicy.blockReason(this)?.let { message ->
            appendLog(message, LogLevel.Warning)
            val state = ResolveState.Error(message)
            recordResolveState(source, option, state)
            updateResolveState(source, option, state)
            return
        }
        appendLog("Checking ${option.labelForLogs} from ${source.label}.")
        updateResolveState(source, option, ResolveState.Loading)
        lifecycleScope.launch {
            val resolved = withContext(Dispatchers.IO) {
                resolveSourceSection(activeRequest, source, option)
            }
            logResolveOutcome(source, option, resolved)

            val state = resolved.notFoundMessage
                ?.takeIf { resolved.candidates.isEmpty() }
                ?.let { ResolveState.Done(emptyList()) }
                ?: resolved.errorMessage
                    ?.takeIf { resolved.candidates.isEmpty() }
                    ?.let { message ->
                        ResolveState.Error(
                            message = message,
                            fallbackCandidate = resolved.fallbackCandidate
                        )
                    }
                    ?: ResolveState.Done(resolved.candidates)
            recordResolveState(source, option, state)
            updateResolveState(source, option, state)
        }
    }

    private fun recordResolveState(
        source: DownloadSource,
        option: CandidateOption,
        state: ResolveState
    ) {
        val attempt = FailedAttempt(source, option)
        when (state) {
            is ResolveState.Error -> recordFailedAttempt(source, option)
            is ResolveState.Done -> failedAttempts = failedAttempts - attempt
            ResolveState.Idle,
            ResolveState.Loading -> Unit
        }
    }

    private fun recordFailedAttempt(source: DownloadSource, option: CandidateOption) {
        val retryOption = if (option == CandidateOption.MANUAL) {
            defaultRetryOption()
        } else {
            option
        }
        failedAttempts = failedAttempts + FailedAttempt(source, retryOption)
        lastFailureOption = retryOption
    }

    private fun updateResolveState(
        source: DownloadSource,
        option: CandidateOption,
        state: ResolveState
    ) {
        val activeRequest = request ?: return
        val current = (uiState as? UiState.Ready)?.result ?: initialCandidateResult(activeRequest)
        uiState = UiState.Ready(current.withResolveState(source, option, state))
    }

    private fun showFailure(
        message: String,
        source: DownloadSource? = null,
        option: CandidateOption? = null
    ) {
        autoRetryActive = false
        autoRetryQueue = null
        autoRetryTarget = null
        lastFailureMessage = message
        if (source != null) {
            recordFailedAttempt(source, option ?: defaultRetryOption())
        }
        uiState = UiState.Error(message)
    }

    private fun defaultRetryOption(): CandidateOption =
        if (request?.hasRequestedVersionRequest == true) {
            CandidateOption.REQUESTED
        } else {
            CandidateOption.LATEST
        }

    private fun retryAllSources() {
        val option = sequenceOf(
            activeDownloadCandidate?.option,
            lastFailureOption,
            defaultRetryOption()
        ).filterNotNull().first { it != CandidateOption.MANUAL }
        val attempts = helperSettings.orderedSources
            .filter { it !in effectiveDisabledSources }
            .map { source -> FailedAttempt(source, option) }
        retrySources(attempts, "all sources")
    }

    private fun retryFailedSources() {
        val attempts = failedAttempts.toList()
        if (attempts.isEmpty()) {
            Toast.makeText(this, "No failed sources to retry.", Toast.LENGTH_SHORT).show()
            return
        }
        retrySources(attempts, "failed sources")
    }

    private fun retrySources(attempts: List<FailedAttempt>, label: String) {
        val activeRequest = request ?: return
        val unique = attempts
            .filter { it.source !in effectiveDisabledSources }
            .distinct()
        if (unique.isEmpty()) return

        if (uiState is UiState.FastMode) {
            retryInFastMode(unique, label, activeRequest)
            return
        }
        retryNormally(unique, label, activeRequest)
    }

    private fun retryInFastMode(
        attempts: List<FailedAttempt>,
        label: String,
        activeRequest: HelperRequest
    ) {
        val sources = attempts.map { it.source }.distinct()
        if (sources.isEmpty()) return
        autoRetryActive = false
        autoRetryQueue = null
        autoRetryTarget = null
        fastModeActive = true
        fastModeQueue = sources.toMutableList()
        failedAttempts = emptySet()
        lastFailureMessage = null
        lastFailureOption = null
        DownloadJobManager.clearEvent()
        val policy = if (fastModeRunPolicy == FastModePolicy.LATEST) {
            FastModePolicy.LATEST
        } else {
            FastModePolicy.REQUESTED
        }
        fastModeRunPolicy = policy
        appendLog(
            "Fast Mode retry for $label: " + sources.joinToString { it.label } + "."
        )
        uiState = UiState.FastMode(FastModeProgress(detail = "Retrying $label…"))
        lifecycleScope.launch {
            fastModeRun(activeRequest, policy)
        }
    }

    private fun retryNormally(
        attempts: List<FailedAttempt>,
        label: String,
        activeRequest: HelperRequest
    ) {
        autoRetryActive = true
        autoRetryQueue = attempts.toMutableList()
        autoRetryTarget = activeDownloadCandidate
        fastModeActive = false
        fastModeQueue = null
        failedAttempts = emptySet()
        lastFailureMessage = null
        lastFailureOption = null
        DownloadJobManager.clearEvent()
        appendLog(
            "Retrying $label: " +
                attempts.joinToString { "${it.source.label} ${it.option.labelForLogs}" } + "."
        )
        uiState = UiState.FastMode(
            FastModeProgress(title = "Retrying", detail = "Retrying $label…")
        )
        lifecycleScope.launch {
            retryNextNormally(activeRequest)
        }
    }

    private suspend fun retryNextNormally(activeRequest: HelperRequest) {
        val queue = autoRetryQueue ?: return
        while (autoRetryActive && queue.isNotEmpty()) {
            val attempt = queue.removeAt(0)
            if (attempt.source in effectiveDisabledSources) continue
            val option = when (attempt.option) {
                CandidateOption.LATEST,
                CandidateOption.REQUESTED -> attempt.option
                CandidateOption.MANUAL -> defaultRetryOption()
            }
            appendLog("Retrying ${attempt.source.label} for ${option.labelForLogs}…")
            uiState = UiState.FastMode(
                FastModeProgress(
                    title = "Retrying",
                    sourceLabel = attempt.source.label,
                    detail = "Checking ${attempt.source.label}…"
                )
            )
            val outcome = withContext(Dispatchers.IO) {
                resolveSourceSection(activeRequest, attempt.source, option)
            }
            if (!autoRetryActive) return
            logResolveOutcome(attempt.source, option, outcome)

            val targetMatches = autoRetryTarget
                ?.let { target ->
                    matchingSourceSwitchCandidates(outcome.candidates, target, activeRequest)
                }
                .orEmpty()
            val candidate = targetMatches.singleOrNull()
                ?: retryCandidateForSource(outcome.candidates, activeRequest, option)
            if (candidate == null) {
                recordFailedAttempt(attempt.source, option)
                appendLog(
                    "Retry: ${attempt.source.label} did not offer a direct file.",
                    LogLevel.Warning
                )
                continue
            }
            appendLog(
                "Retry: downloading ${candidate.versionDisplay} from " +
                    "${attempt.source.label} (${candidate.fileKind.uppercase(Locale.US)})."
            )
            downloadAndReturn(candidate)
            return
        }

        if (!autoRetryActive) return
        autoRetryActive = false
        autoRetryQueue = null
        autoRetryTarget = null
        val message = "No source could download ${activeRequest.appName} after retrying."
        appendLog("Retry failed: $message", LogLevel.Error)
        showFailure(message)
    }

    /**
     * Best direct file to start when a retry source has no exact match for the
     * failed target. Requested retries stay on the exact version and build;
     * latest retries prefer the newest candidate the source returned.
     */
    private fun retryCandidateForSource(
        candidates: List<DownloadCandidate>,
        request: HelperRequest,
        option: CandidateOption
    ): DownloadCandidate? {
        val direct = candidates.filter {
            it.directDownload && request.acceptsFormat(it.fileKind)
        }
        val usable = direct.filter {
            archMatchRank(it.variantLabel ?: it.variantTag, request.availableAbis) !=
                ARCH_RANK_MISMATCH
        }
        if (usable.isEmpty()) return null
        fun archRank(candidate: DownloadCandidate): Int =
            archMatchRank(candidate.variantLabel ?: candidate.variantTag, request.availableAbis)

        return when (option) {
            CandidateOption.REQUESTED -> usable
                .filter { candidate -> request.matchesRequestedVersionStrict(candidate) }
                .minWithOrNull(compareBy({ archRank(it) }, { it.sortIndex }))

            CandidateOption.LATEST -> usable
                .filter { candidate ->
                    candidate.versionName != null &&
                        (request.requestsVariantBuild || !candidate.hasVariantBuildMarker)
                }
                .maxWithOrNull(
                    Comparator { a, b ->
                        val byVersion = compareVersionNames(b.versionName, a.versionName)
                        if (byVersion != 0) byVersion else a.sortIndex.compareTo(b.sortIndex)
                    }
                )
                ?: usable.minWithOrNull(compareBy({ archRank(it) }, { it.sortIndex }))

            CandidateOption.MANUAL -> usable
                .minWithOrNull(compareBy({ archRank(it) }, { it.sortIndex }))
        }
    }

    private fun copyFailureDetails() {
        val clipboard = getSystemService(ClipboardManager::class.java)
        if (clipboard == null) {
            Toast.makeText(this, "Clipboard is unavailable.", Toast.LENGTH_SHORT).show()
            return
        }
        val details = buildString {
            lastFailureMessage?.let { message ->
                append("Failure: ").append(message).append("\n\n")
            }
            if (failedAttempts.isNotEmpty()) {
                append("Failed sources:\n")
                failedAttempts
                    .sortedBy { it.source.ordinal }
                    .forEach { attempt ->
                        append("- ").append(attempt.source.label)
                            .append(" (").append(attempt.option.labelForLogs).append(")\n")
                    }
                append("\n")
            }
            append(AppLog.exportText())
        }
        clipboard.setPrimaryClip(
            ClipData.newPlainText("Morphe Helper failure details", details)
        )
        Toast.makeText(this, "Failure details copied.", Toast.LENGTH_SHORT).show()
    }

    private fun loadVersionHistory(source: DownloadSource) {
        val activeRequest = request ?: return
        helperSettings.networkPolicy.blockReason(this)?.let { message ->
            appendLog(message, LogLevel.Warning)
            updateHistoryState(source, VersionHistoryState.Error(message))
            return
        }
        updateHistoryState(source, VersionHistoryState.Loading)
        appendLog("Loading ${source.label} version history.")
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { resolveVersionHistory(activeRequest, source) }
            }
            result
                .onSuccess { candidates ->
                    appendLog("${source.label} version history loaded: ${candidates.size} versions.")
                    updateHistoryState(source, VersionHistoryState.Done(candidates))
                }
                .onFailure { error ->
                    val message = sourceFailureMessage(source, error)
                    appendLog(message, LogLevel.Error)
                    updateHistoryState(source, VersionHistoryState.Error(message))
                }
        }
    }

    private fun updateHistoryState(source: DownloadSource, state: VersionHistoryState) {
        val activeRequest = request ?: return
        val current = (uiState as? UiState.Ready)?.result ?: initialCandidateResult(activeRequest)
        uiState = UiState.Ready(current.withHistoryState(source, state))
    }

    private fun downloadVersion(candidate: DownloadCandidate) {
        val activeRequest = request ?: return
        // Capture the current result before Loading replaces it, so a failed
        // resolution can restore the history list with this row flipped to
        // "Open link" instead of wiping back to a fresh screen.
        val currentResult = (uiState as? UiState.Ready)?.result
            ?: initialCandidateResult(activeRequest)
        appendLog("Resolving ${candidate.versionDisplay} from ${candidate.source.label} for download...")
        uiState = UiState.Loading
        lifecycleScope.launch {
            val resolved = withContext(Dispatchers.IO) {
                runCatching { resolveHistoryCandidate(activeRequest, candidate) }
            }
            resolved
                .onSuccess { direct ->
                    if (direct == null) {
                        // No direct download exists for this version. Keep the
                        // history list usable by flipping this row to an
                        // "Open link" action instead of erroring the whole screen.
                        val message =
                            "No direct download was available for ${candidate.versionDisplay} " +
                                "on ${candidate.source.label}. Open the version page manually."
                        appendLog(message, LogLevel.Warning)
                        uiState = UiState.Ready(
                            currentResult.markHistoryCandidateNoDirectDownload(
                                source = candidate.source,
                                candidateKey = candidate.identityKey()
                            )
                        )
                    } else {
                        downloadAndReturn(direct)
                    }
                }
                .onFailure { error ->
                    val message = downloadFailureMessage(candidate, error)
                    appendLog(message, LogLevel.Error)
                    showFailure(message, candidate.source, candidate.option)
                }
        }
    }

    private suspend fun resolveVersionHistory(
        request: HelperRequest,
        source: DownloadSource
    ): List<DownloadCandidate> =
        parsers[source]?.resolveHistory(request).orEmpty()

    private suspend fun resolveHistoryCandidate(
        request: HelperRequest,
        candidate: DownloadCandidate
    ): DownloadCandidate? =
        parsers[candidate.source]?.resolveHistoryCandidate(request, candidate)

    private fun startRequestLog(request: HelperRequest?) {
        failedAttempts = emptySet()
        lastFailureMessage = null
        lastFailureOption = null
        pendingSwitchSource = null
        pendingSwitchTarget = null
        pendingSwitchOption = null
        activeDownloadCandidate = null
        switchCandidatePrompt = null
        autoRetryActive = false
        autoRetryQueue = null
        autoRetryTarget = null
        AppLog.clear()
        if (request == null) {
            AppLog.setRequestSummary(null)
            appendLog("Opened without a Morphe request.", LogLevel.Warning)
        } else {
            AppLog.setRequestSummary(
                buildString {
                    append("App: ${request.appName}\n")
                    append("Package: ${request.packageName}\n")
                    append("Version: ${request.requestedVersionName ?: "any compatible"}\n")
                    append("Build: ${request.versionCodeSummary ?: "any"}\n")
                    append("Format: ${request.requestedFormatLabel.ifBlank { "any" }}\n")
                    if (request.availableAbis.isNotEmpty()) {
                        append("ABI: ${request.abiSummary}\n")
                    }
                    request.sourceHintUrls.takeIf { it.isNotEmpty() }?.let { urls ->
                        append("Source hints: ${urls.joinToString()}\n")
                    }
                }.trimEnd()
            )
            appendLog(
                "Request for ${request.appName} (${request.packageName}), " +
                    "version ${request.versionName ?: "any compatible"}, " +
                    "build ${request.versionCodeSummary ?: "any"}, format ${request.requestedFormatLabel}."
            )
            logQueryIntentExtras()
        }
    }

    private fun logQueryIntentExtras() {
        val extras = intent.extras ?: return
        val dump = extras.keySet().sorted().joinToString(", ") { key ->
            val value = when (val raw = extras.get(key)) {
                null -> "null"
                is Array<*> -> raw.joinToString("|")
                is LongArray -> raw.joinToString("|")
                is IntArray -> raw.joinToString("|")
                else -> raw.toString()
            }
            "$key=$value"
        }
        if (logcatLoggingEnabled) Log.i(TAG, "Query intent extras: $dump")
    }

    private fun logResolveOutcome(
        source: DownloadSource,
        option: CandidateOption,
        outcome: ResolveOutcome
    ) {
        when {
            outcome.notFoundMessage != null -> {
                appendLog("${source.label} ${option.labelForLogs}: ${outcome.notFoundMessage}.", LogLevel.Warning)
            }
            outcome.errorMessage != null && outcome.candidates.isEmpty() -> {
                appendLog("${source.label} ${option.labelForLogs} failed: ${outcome.errorMessage}", LogLevel.Error)
            }
            outcome.candidates.isEmpty() -> {
                appendLog("${source.label} ${option.labelForLogs} found no candidates.", LogLevel.Warning)
            }
            else -> {
                val summary = outcome.candidates.joinToString(limit = 3, truncated = "…") { candidate ->
                    candidate.versionDisplay
                }
                appendLog("${source.label} ${option.labelForLogs} found ${outcome.candidates.size}: $summary")
            }
        }
    }

    private fun appendLog(message: String, level: LogLevel = LogLevel.Info) {
        // Single shared sink: the Logs tab and the HTTP interceptor both write
        // here, and Logcat mirroring is handled by the sink per the setting.
        AppLog.record(level, message)
    }

    private fun toggleRequestedFileKind(kind: String) {
        val active = request ?: return
        val normalized = kind.lowercase(Locale.US)
        val available = active.availableFileKinds
        if (normalized !in available) return

        val selected = active.requestedFileKinds
        val updated = if (normalized in selected) {
            // Keep at least one format selected; an empty set would accept anything.
            if (selected.size == 1) return
            selected - normalized
        } else {
            selected + normalized
        }
        if (updated == selected) return

        request = active.copy(
            requestedFileKindsOverride = updated.takeUnless { it == available }
        )
        appendLog(
            "Request formats: ${updated.orderedFileKinds().joinToString("/") { it.uppercase(Locale.US) }}.",
            LogLevel.Info
        )
        loadCandidates()
    }

    private fun updateHelperSettings(settings: HelperSettings) {
        val sourcesChanged = helperSettings.disabledSources != settings.disabledSources ||
            helperSettings.sourceOrder != settings.sourceOrder ||
            helperSettings.preferredSource != settings.preferredSource
        val launcherChanged = helperSettings.showLauncherIcon != settings.showLauncherIcon
        val densityPreferenceChanged =
            helperSettings.preferDensitySpecificVariants != settings.preferDensitySpecificVariants
        helperSettings = settings
        saveHelperSettings(settings)
        if (launcherChanged) {
            setLauncherIconVisible(settings.showLauncherIcon)
        }
        if (densityPreferenceChanged) {
            request = request?.copy(
                preferDensitySpecificVariants = settings.preferDensitySpecificVariants
            )
        }
        // Rebuild the picker so enabled/disabled sources take effect immediately.
        if (
            (sourcesChanged || densityPreferenceChanged) &&
            request != null &&
            uiState is UiState.Ready
        ) {
            uiState = UiState.Ready(initialCandidateResult(request!!))
        }
    }

    private fun initialCandidateResult(request: HelperRequest): CandidateResult {
        val manual = manualCandidates(request)
        val enabled = helperSettings.orderedSources
            .filter { it !in effectiveDisabledSources }
        // Put the user's preferred source first so the picker's pager opens on
        // it (page 0) instead of the first source in the custom order. Falls
        // back to that order when no preference is set or it's disabled.
        val ordered = helperSettings.preferredSource
            ?.takeIf { it in enabled }
            ?.let { preferred -> listOf(preferred) + enabled.filterNot { it == preferred } }
            ?: enabled
        return CandidateResult(
            sourceGroups = ordered.map { source ->
                SourceCandidateGroup(
                    source = source,
                    manual = manual.filter { it.source == source },
                    recommended = ResolveState.Idle,
                    latest = ResolveState.Idle
                )
            }
        )
    }

    private suspend fun resolveSourceSection(
        request: HelperRequest,
        source: DownloadSource,
        option: CandidateOption
    ): ResolveOutcome {
        val lookup = runCatching { findSourceCandidates(request, source, option) }
            .onFailure { error ->
                if (error !is SourceAppNotFoundException) {
                    Log.w(TAG, "${source.label} ${option.name.lowercase(Locale.US)} lookup failed", error)
                }
            }
        lookup.exceptionOrNull()
            ?.takeIf { it is SourceAppNotFoundException }
            ?.let {
                return ResolveOutcome(
                    candidates = emptyList(),
                    notFoundMessage = "no listing for ${request.packageName}"
                )
            }
        lookup.exceptionOrNull()?.let { error ->
            return ResolveOutcome(
                candidates = emptyList(),
                errorMessage = sourceFailureMessage(source, error),
                fallbackCandidate = sourceErrorFallbackCandidate(request, source, option)
            )
        }
        val sourceCandidates = lookup
            .getOrDefault(emptyList())
            .distinctBy(DownloadCandidate::identityKey)

        val candidates = when (option) {
            CandidateOption.REQUESTED -> recommendedCandidatesForSource(request, source, sourceCandidates)
            CandidateOption.LATEST -> latestCandidatesForSource(request, source, sourceCandidates)
            CandidateOption.MANUAL -> emptyList()
        }

        return ResolveOutcome(
            candidates = candidates,
            errorMessage = lookup.exceptionOrNull()?.let { sourceFailureMessage(source, it) }
        )
    }

    private suspend fun findSourceCandidates(
        request: HelperRequest,
        source: DownloadSource,
        option: CandidateOption
    ): List<DownloadCandidate> =
        parsers[source]?.findCandidates(request, option).orEmpty()

    private fun recommendedCandidatesForSource(
        request: HelperRequest,
        source: DownloadSource,
        candidates: List<DownloadCandidate>
    ): List<DownloadCandidate> {
        if (!request.hasRequestedVersionRequest || source == DownloadSource.AURORA || source == DownloadSource.PLAY) {
            return emptyList()
        }

        return buildList {
            addAll(candidates.filter(request::isRequestedMatch))
            if (none { it.option == CandidateOption.REQUESTED }) {
                parsers[source]?.requestedFallbackCandidate(request)?.let(::add)
            }
        }
            .distinctBy(DownloadCandidate::identityKey)
            .sortedBy { it.sortIndex }
    }

    private fun latestCandidatesForSource(
        request: HelperRequest,
        source: DownloadSource,
        candidates: List<DownloadCandidate>
    ): List<DownloadCandidate> {
        if (source == DownloadSource.PLAY) {
            return listOf(playStoreCandidate(request))
        }

        return buildList {
            addAll(
                candidates
                    .filter { it.option == CandidateOption.LATEST }
                    // "-SECONDARY" builds are alternate packages Morphe cannot
                    // patch: don't offer them unless the request asked for one.
                    .filter { request.requestsVariantBuild || !it.hasVariantBuildMarker }
            )
            if (none { it.option == CandidateOption.LATEST }) {
                parsers[source]?.latestFallbackCandidate(request)?.let(::add)
            }
            if (none { it.option == CandidateOption.LATEST }) {
                latestWebFallback(request, source)?.let(::add)
            }
        }
            .distinctBy(DownloadCandidate::identityKey)
            .sortedBy { it.sortIndex }
    }

    private fun sourceErrorFallbackCandidate(
        request: HelperRequest,
        source: DownloadSource,
        option: CandidateOption
    ): DownloadCandidate? {
        if (option == CandidateOption.MANUAL || source == DownloadSource.AURORA) return null

        val manual = manualCandidates(request).firstOrNull { it.source == source } ?: return null
        val requestedVersionName = request.requestedVersionNames.firstOrNull()
        val requestedVersionCode = request.versionCode ?: request.versionCodes.singleOrNull()
        // Keep the browser on the source itself when its search page is the
        // thing that hit Cloudflare, instead of sending the user to a Google
        // result for the app.
        val browserUrl = when (source) {
            DownloadSource.APK_MIRROR -> request.sourceHintUrlsFor(source)
                .firstOrNull { it.contains("apkmirror.com", ignoreCase = true) }
                ?: parsers[source]?.searchUrl(request.packageName)
            DownloadSource.APK_PURE -> request.sourceHintUrlsFor(source)
                .firstOrNull { it.contains("apkpure.com", ignoreCase = true) }
                ?: parsers[source]?.searchUrl(request.packageName)
            DownloadSource.APK_CUBE, DownloadSource.EVOZI -> {
                val parser = parsers[source]
                if (option == CandidateOption.REQUESTED) {
                    parser?.requestedFallbackCandidate(request)?.url
                } else {
                    parser?.latestFallbackCandidate(request)?.url
                }
            }
            else -> null
        }
        val url = browserUrl ?: when (option) {
            CandidateOption.REQUESTED -> request.sourceHintUrlsFor(source).firstOrNull()
                ?: sourceVersionSearchUrl(request, source)
                ?: manual.url
            CandidateOption.LATEST -> manual.url
            CandidateOption.MANUAL -> manual.url
        }

        return manual.copy(
            versionName = requestedVersionName.takeIf { option == CandidateOption.REQUESTED },
            versionCode = requestedVersionCode.takeIf { option == CandidateOption.REQUESTED },
            url = url,
            fileKind = "web",
            option = option,
            directDownload = false,
            versionStatus = if (option == CandidateOption.REQUESTED) VersionStatus.REQUESTED else VersionStatus.LATEST,
            formatMatches = true,
            note = null,
            variantLabel = null,
            files = emptyList(),
            captchaUrl = browserUrl
        )
    }

    private fun sourceVersionSearchUrl(request: HelperRequest, source: DownloadSource): String? {
        val domain = source.searchDomain() ?: return null
        val version = request.requestedVersionLabel.takeIf { it != "any compatible version" } ?: return null
        val query = buildList {
            add(request.packageName)
            add("\"$version\"")
            request.requestedFileKinds
                .orderedFileKinds()
                .firstOrNull()
                ?.let(::add)
            add("site:$domain")
        }.joinToString(" ")
        return "https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}"
    }

    private fun manualCandidates(request: HelperRequest): List<DownloadCandidate> =
        manualSourceUrls(request).map { (source, url) ->
            DownloadCandidate(
                source = source,
                name = request.appName,
                packageName = request.packageName,
                versionName = null,
                versionCode = null,
                url = url,
                fileKind = "web",
                option = CandidateOption.MANUAL,
                directDownload = false,
                versionStatus = VersionStatus.LATEST,
                formatMatches = true
            )
        }.sortedBy { it.sortIndex }

    private fun latestWebFallback(request: HelperRequest, source: DownloadSource): DownloadCandidate? =
        manualSourceUrls(request)
            .firstOrNull { (candidateSource, _) -> candidateSource == source }
            ?.let { (_, url) ->
                DownloadCandidate(
                    source = source,
                    name = request.appName,
                    packageName = request.packageName,
                    versionName = null,
                    versionCode = null,
                    url = url,
                    fileKind = "web",
                    option = CandidateOption.LATEST,
                    directDownload = false,
                    versionStatus = VersionStatus.LATEST,
                    formatMatches = true
                )
            }

    private fun manualSourceUrls(request: HelperRequest): List<Pair<DownloadSource, String>> =
        parsers.values
            .filter { it.source !in effectiveDisabledSources }
            .mapNotNull { parser ->
                parser.searchUrl(request.packageName)?.let { parser.source to it }
            }
            .sortedBy { it.first.ordinal }

    private fun playStoreCandidate(request: HelperRequest) = DownloadCandidate(
        source = DownloadSource.PLAY,
        name = request.appName,
        packageName = request.packageName,
        versionName = null,
        versionCode = null,
        url = playStoreUrl(request.packageName),
        fileKind = "web",
        option = CandidateOption.LATEST,
        directDownload = false,
        versionStatus = VersionStatus.LATEST,
        formatMatches = true
    )

    private fun downloadAndReturn(candidate: DownloadCandidate) {
        val activeRequest = request ?: return
        val settings = helperSettings
        settings.networkPolicy.blockReason(this)?.let { message ->
            appendLog(message, LogLevel.Warning)
            showFailure(message)
            return
        }
        activeDownloadCandidate = candidate
        appendLog(
            "Downloading ${candidate.source.label} ${candidate.option.labelForLogs} " +
                "${candidate.versionDisplay} (${candidate.fileKind.uppercase(Locale.US)})."
        )
        uiState = if (fastModeActive) {
            UiState.FastMode(
                FastModeProgress(
                    sourceLabel = candidate.source.label,
                    detail = "Downloading from ${candidate.source.label}…",
                    percent = 0
                )
            )
        } else {
            UiState.Downloading(candidate, 0)
        }
        pendingDownload = PendingDownload(activeRequest, candidate, settings)
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startPendingDownload()
        }
    }

    private fun startPendingDownload() {
        val pending = pendingDownload ?: return
        pendingDownload = null
        DownloadJobManager.start(
            DownloadJobManager.DownloadJob(
                request = pending.request,
                candidate = pending.candidate,
                settings = pending.settings,
                requestIntentExtras = intent.getExtras(),
                fastMode = fastModeActive
            )
        )
        startForegroundService(
            Intent(this, DownloadService::class.java).setAction(ACTION_START_DOWNLOAD)
        )
    }

    private fun cancelDownload() {
        appendLog("Cancelling download…", LogLevel.Warning)
        runCatching {
            startService(
                Intent(this, DownloadService::class.java).setAction(ACTION_CANCEL_DOWNLOAD)
            )
        }.onFailure {
            appendLog("Could not cancel the download.", LogLevel.Error)
        }
    }

    private fun pauseDownload() {
        appendLog("Pausing download…", LogLevel.Info)
        runCatching {
            startService(
                Intent(this, DownloadService::class.java).setAction(ACTION_PAUSE_DOWNLOAD)
            )
        }.onFailure {
            appendLog("Could not pause the download.", LogLevel.Error)
        }
    }

    private fun resumeDownload() {
        appendLog("Resuming download…", LogLevel.Info)
        runCatching {
            startService(
                Intent(this, DownloadService::class.java).setAction(ACTION_RESUME_DOWNLOAD)
            )
        }.onFailure {
            appendLog("Could not resume the download.", LogLevel.Error)
        }
    }

    private fun switchDownloadSource(source: DownloadSource) {
        if (pendingSwitchSource != null) return
        if (request == null) return
        pendingSwitchSource = source
        val currentCandidate = (uiState as? UiState.Downloading)?.candidate
            ?: activeDownloadCandidate
        pendingSwitchTarget = currentCandidate
        pendingSwitchOption = when (currentCandidate?.option) {
            CandidateOption.LATEST -> CandidateOption.LATEST
            CandidateOption.REQUESTED -> CandidateOption.REQUESTED
            else -> when {
                fastModeActive && fastModeRunPolicy == FastModePolicy.LATEST -> CandidateOption.LATEST
                fastModeActive -> defaultRetryOption()
                lastFailureOption != null -> lastFailureOption
                else -> defaultRetryOption()
            }
        }
        appendLog("Switching source to ${source.label}…", LogLevel.Warning)
        fastModeActive = false
        fastModeQueue = null
        fastModeDecision?.complete(null)
        fastModeDecision = null
        fastModeVersionDecision?.complete(null)
        fastModeVersionDecision = null
        if (DownloadJobManager.activeJob != null) {
            cancelDownload()
            // If the service already stopped and no Cancelled event comes back,
            // start the new source without waiting forever.
            lifecycleScope.launch {
                delay(2_000)
                if (pendingSwitchSource == source) startPendingSwitch()
            }
        } else {
            startPendingSwitch()
        }
    }

    private fun startPendingSwitch() {
        val source = pendingSwitchSource ?: return
        pendingSwitchSource = null
        val activeRequest = request ?: return
        val target = pendingSwitchTarget
        val option = pendingSwitchOption ?: defaultRetryOption()
        pendingSwitchTarget = null
        pendingSwitchOption = null
        appendLog("Resolving ${source.label} for ${option.labelForLogs} again…", LogLevel.Info)
        uiState = UiState.Loading
        lifecycleScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                resolveSourceSection(activeRequest, source, option)
            }
            if (outcome.candidates.isEmpty()) {
                val message = outcome.notFoundMessage
                    ?: outcome.errorMessage
                    ?: "No downloadable file from ${source.label}."
                appendLog(message, LogLevel.Error)
                showFailure(message, source, option)
                return@launch
            }

            val matching = if (target == null) {
                outcome.candidates.filter { it.directDownload }
            } else {
                matchingSourceSwitchCandidates(outcome.candidates, target, activeRequest)
            }
            val candidate = matching.singleOrNull()
            if (candidate != null) {
                appendLog(
                    "Continuing on ${source.label} with ${candidate.versionDisplay} " +
                        "(${candidate.variantLabel ?: candidate.variantTag ?: candidate.fileKind.uppercase(Locale.US)})."
                )
                downloadAndReturn(candidate)
                return@launch
            }

            val detail = when {
                target == null -> "Pick a file from ${source.label} to continue the download."
                matching.isEmpty() ->
                    "No matching variant was found on ${source.label}. Pick a file to continue."
                else -> "More than one file matched on ${source.label}. Pick the one you want."
            }
            showSwitchCandidatePrompt(outcome.candidates, source, option, detail)
        }
    }

    /**
     * A source switch should keep the file kind and ABI/variant of the download
     * in flight. If no candidate matches, the caller shows the resolved list so
     * the user can pick a variant instead of silently taking the first one.
     */
    private fun showSwitchCandidatePrompt(
        candidates: List<DownloadCandidate>,
        source: DownloadSource,
        option: CandidateOption,
        detail: String
    ) {
        val activeRequest = request ?: return
        val choices = candidates
            .filter { it.directDownload || it.captchaUrl != null }
            .distinctBy(DownloadCandidate::identityKey)
        if (choices.isEmpty()) {
            appendLog(
                "${source.label} has no selectable file for ${option.labelForLogs}.",
                LogLevel.Warning
            )
            uiState = UiState.Ready(initialCandidateResult(activeRequest))
            return
        }
        appendLog(
            "${source.label} needs a choice: ${choices.size} candidate(s) for ${option.labelForLogs}.",
            LogLevel.Warning
        )
        uiState = UiState.Ready(initialCandidateResult(activeRequest))
        switchCandidatePrompt = SwitchCandidatePrompt(
            source = source,
            detail = detail,
            candidates = choices
        )
    }

    private fun chooseSwitchCandidate(candidate: DownloadCandidate) {
        switchCandidatePrompt = null
        if (candidate.directDownload) {
            downloadAndReturn(candidate)
        } else {
            openCaptchaBrowser(candidate)
        }
    }

    // ---- In-app captcha browser: let the user solve a captcha in a real
    // browser and capture the download URL it produces ----

    private fun openCaptchaBrowser(candidate: DownloadCandidate) {
        if (candidate.source == DownloadSource.AURORA || candidate.source == DownloadSource.PLAY) return
        appendLog(
            "Opening in-app browser for ${candidate.source.label} to continue the download.",
            LogLevel.Info
        )
        captchaBrowser = candidate
    }

    private fun closeCaptchaBrowser() {
        val candidate = captchaBrowser
        captchaBrowser = null
        if (candidate == null || !fastModeActive) return
        abandonFastModeCaptcha(
            candidate,
            "Fast Mode: browser closed before ${candidate.source.label} started a download."
        )
    }

    /**
     * A Fast Mode captcha route that did not produce a file (closed browser, challenge
     * timeout or build mismatch) must release the source queue instead of leaving the
     * card waiting forever. The source is marked failed and the next one is tried.
     */
    private fun abandonFastModeCaptcha(
        candidate: DownloadCandidate,
        message: String
    ) {
        val activeRequest = request
        val hasNextSource = activeRequest != null && !fastModeQueue.isNullOrEmpty()
        appendLog(
            if (hasNextSource) "$message Trying the next source." else message,
            LogLevel.Warning
        )
        recordFailedAttempt(candidate.source, candidate.option)
        if (activeRequest != null && hasNextSource) {
            lifecycleScope.launch { fastModeNext(activeRequest) }
            return
        }
        fastModeActive = false
        fastModeQueue = null
        lastFailureMessage = message
        uiState = if (activeRequest != null) {
            UiState.FastMode(
                FastModeProgress(
                    sourceLabel = candidate.source.label,
                    detail = message,
                    done = true,
                    succeeded = false,
                    result = initialCandidateResult(activeRequest)
                )
            )
        } else {
            UiState.Idle
        }
    }

    /**
     * The browser rejected a page whose build does not match the request.
     * Mark the wrapper source as failed, not the provider whose page it was.
     */
    private fun onBrowserBuildMismatch(code: String?) {
        val candidate = captchaBrowser ?: return
        captchaBrowser = null
        val message = code.browserBuildMismatchMessage(request?.versionCodeSummary)
        if (fastModeActive) {
            abandonFastModeCaptcha(candidate, message)
        } else {
            appendLog(message, LogLevel.Warning)
            showFailure(message, candidate.source, candidate.option)
        }
    }

    private fun onBrowserChallengeTimeout() {
        val candidate = captchaBrowser ?: return
        captchaBrowser = null
        val message = "Verification on ${candidate.source.label} did not finish within five minutes."
        if (fastModeActive) {
            abandonFastModeCaptcha(candidate, message)
        } else {
            appendLog(message, LogLevel.Warning)
            showFailure(message, candidate.source, candidate.option)
        }
    }

    private fun onBrowserDownloadCaptured(capture: BrowserDownloadCapture) {
        val candidate = captchaBrowser ?: return
        captchaBrowser = null
        // A captured link often names no extension (Uptodown's `/dwn/<token>`), so fall
        // back to what the source said the file is rather than calling every one an APK.
        val fileKind = fileKindFromUrlOrNull(capture.downloadUrl)
            ?: candidate.fileKind.lowercase(Locale.US).takeIf { it in DOWNLOAD_FILE_KIND_SET }
            ?: "apk"
        val referer = capture.refererUrl
            ?.takeIf(String::isNotBlank)
            ?: candidate.captchaUrl
            ?: candidate.url
        val cookieHeader = capture.cookieHeader
            ?: CookieManager.getInstance().getCookie(referer)
        appendLog(
            "Captured download link from ${candidate.source.label} in the in-app browser " +
                "($fileKind): ${capture.downloadUrl}",
            LogLevel.Info
        )
        downloadAndReturn(
            candidate.copy(
                url = capture.downloadUrl,
                fileKind = fileKind,
                directDownload = true,
                note = null,
                files = listOf(
                    CandidateDownloadFile(
                        url = capture.downloadUrl,
                        fileName = capturedDownloadFileName(candidate, capture.downloadUrl, fileKind),
                        referer = referer,
                        cookieHeader = cookieHeader
                    )
                )
            )
        )
    }

    private fun openMorpheManager() {
        // Prefer the release build of Morphe Manager, then the debug build.
        val launchIntent = MORPHE_MANAGER_PACKAGES
            .asSequence()
            .mapNotNull { packageName -> packageManager.getLaunchIntentForPackage(packageName) }
            .firstOrNull()
        if (launchIntent != null) {
            appendLog("Opening Morphe Manager (${launchIntent.component?.packageName ?: "Morphe Manager"}).")
            runCatching {
                startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.onFailure {
                appendLog("Could not open Morphe Manager.", LogLevel.Error)
            }
        } else {
            appendLog("Morphe Manager is not installed  opening morphe.software.", LogLevel.Warning)
            val releases = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(MORPHE_MANAGER_SITE_URL)
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            runCatching { startActivity(releases) }
                .onFailure {
                    appendLog("Could not open Morphe Manager releases page.", LogLevel.Error)
                }
        }
    }

    // ---- Fast Mode: auto-find a version and return it ----

    private fun startFastModeIfEnabled(request: HelperRequest) {
        if (!helperSettings.fastMode) return
        if (fastModeActive) return
        // REQUESTED mode needs a concrete version to match against; LATEST and
        // ALWAYS_ASK can resolve the newest version even with no request fields.
        val policy = helperSettings.fastModePolicy
        if (policy == FastModePolicy.REQUESTED && !request.hasRequestedVersionRequest) return
        failedAttempts = emptySet()
        lastFailureMessage = null
        lastFailureOption = null
        fastModeActive = true
        fastModeRunPolicy = if (policy == FastModePolicy.ALWAYS_ASK) FastModePolicy.REQUESTED else policy
        // Walk whatever the user has enabled globally; sources that cannot
        // deliver a direct APK (Play/Aurora links, captcha-gated services)
        // simply resolve to nothing and the walk moves on.
        fastModeQueue = helperSettings.orderedSources
            .filter { it !in effectiveDisabledSources }
            .toMutableList()
        if (policy == FastModePolicy.ALWAYS_ASK) {
            appendLog("Fast Mode: asking which version to fetch.", LogLevel.Info)
            val gate = CompletableDeferred<FastModePolicy?>()
            fastModeVersionDecision = gate
            uiState = UiState.FastMode(
                FastModeProgress(
                    detail = "Auto-searching sources…",
                    awaitingVersionChoice = true,
                    versionChoiceRequested = request.requestedVersionLabel,
                    versionChoiceDetail =
                    "Retrieve the requested version or fetch the latest available version?"
                )
            )
            // Ask first, resolve later: the run stays parked on the gate and
            // only starts searching sources after the user picks, so the
            // prompt appears instantly instead of waiting on a background scan.
            lifecycleScope.launch {
                val chosen = gate.await()
                fastModeVersionDecision = null
                if (!fastModeActive || chosen == null) return@launch
                fastModeRunPolicy = chosen
                appendLog("Fast Mode: resolving ${chosen.name.lowercase(Locale.US)} version.", LogLevel.Info)
                uiState = UiState.FastMode(FastModeProgress(detail = "Auto-searching sources…"))
                fastModeRun(request, chosen)
            }
        } else {
            appendLog(
                "Fast Mode: auto-searching sources for the ${policy.name.lowercase(Locale.US)} version.",
                LogLevel.Info
            )
            uiState = UiState.FastMode(FastModeProgress(detail = "Auto-searching sources…"))
            lifecycleScope.launch {
                fastModeRun(request, policy)
            }
        }
    }

    private fun fastModeChooseVersion(policy: FastModePolicy) {
        fastModeVersionDecision?.complete(policy)
    }

    private suspend fun fastModeRun(request: HelperRequest, policy: FastModePolicy) {
        if (policy == FastModePolicy.LATEST) {
            fastModeNextLatest(request)
        } else {
            fastModeNext(request)
        }
    }

    private fun cancelFastMode() {
        if (autoRetryActive) {
            appendLog("Retry cancelled  taking over manually.", LogLevel.Warning)
            autoRetryActive = false
            autoRetryQueue = null
            autoRetryTarget = null
        } else {
            appendLog("Fast Mode cancelled  taking over manually.", LogLevel.Warning)
        }
        fastModeActive = false
        fastModeQueue = null
        // Unblock a pending "use this version?" question so the loop exits.
        fastModeDecision?.complete(null)
        fastModeDecision = null
        // If a fast-mode download is in flight, stop it too; its Cancelled
        // event also restores the source list.
        if (DownloadJobManager.activeJob != null) {
            cancelDownload()
        }
        val activeRequest = request
        uiState = if (activeRequest != null) {
            UiState.Ready(initialCandidateResult(activeRequest))
        } else {
            UiState.Idle
        }
    }

    private fun fastModeChoose(choice: FastModeChoice) {
        fastModeDecision?.complete(choice)
    }

    private fun proceedAfterScan() {
        startService(
            Intent(this, DownloadService::class.java).setAction(ACTION_SCAN_PROCEED)
        )
        uiState = UiState.Loading
    }

    private fun cancelAfterScan() {
        startService(
            Intent(this, DownloadService::class.java).setAction(ACTION_SCAN_CANCEL)
        )
        uiState = UiState.Loading
    }

    private fun skipScanAndHandoff() {
        appendLog("User skipped the VirusTotal scan — handing off unverified.", LogLevel.Warning)
        startService(
            Intent(this, DownloadService::class.java).setAction(ACTION_SCAN_SKIP)
        )
        uiState = UiState.Loading
    }

    private suspend fun fastModeNext(request: HelperRequest) {
        val queue = fastModeQueue ?: return
        while (queue.isNotEmpty()) {
            // The user may have cancelled while a source was resolving.
            if (!fastModeActive) return
            val source = queue.removeAt(0)
            appendLog("Fast Mode: checking ${source.label} for ${request.requestedVersionLabel}...")
            uiState = UiState.FastMode(
                FastModeProgress(sourceLabel = source.label, detail = "Checking ${source.label}…")
            )
            val result = withContext(Dispatchers.IO) {
                runCatching { fastModeFindCandidate(request, source) }
                    .getOrDefault(FastModeFindResult.None)
            }
            if (!fastModeActive) return
            when (result) {
                is FastModeFindResult.Exact -> {
                    val candidate = result.candidate
                    if (!candidate.directDownload) {
                        appendLog(
                            "Fast Mode: ${source.label} has ${candidate.versionDisplay} behind a captcha. " +
                                "Waiting for the user to solve it.",
                            LogLevel.Info
                        )
                        val decisionGate = CompletableDeferred<FastModeChoice?>()
                        fastModeDecision = decisionGate
                        uiState = UiState.FastMode(
                            FastModeProgress(
                                sourceLabel = source.label,
                                detail = "Exact version found, captcha needed",
                                awaitingDecision = true,
                                awaitingCaptcha = true,
                                mismatchDetail = "${source.label} has ${candidate.versionDisplay}, but the " +
                                    "download needs a captcha. Solve it in the in-app browser to continue."
                            )
                        )
                        val decision = decisionGate.await()
                        fastModeDecision = null
                        if (!fastModeActive || decision == null) return
                        if (decision == FastModeChoice.USE) {
                            appendLog(
                                "Fast Mode: opening ${source.label} to solve the captcha.",
                                LogLevel.Info
                            )
                            openCaptchaBrowser(candidate)
                            return
                        }
                        appendLog(
                            "Fast Mode: user skipped the ${source.label} captcha  trying the next source.",
                            LogLevel.Info
                        )
                        recordFailedAttempt(source, CandidateOption.REQUESTED)
                        continue
                    }
                    appendLog(
                        "Fast Mode: found ${candidate.versionDisplay} on ${source.label} " +
                            "(${candidate.fileKind.uppercase(Locale.US)})  downloading.",
                        LogLevel.Info
                    )
                    uiState = UiState.FastMode(
                        FastModeProgress(
                            sourceLabel = source.label,
                            detail = "Found ${candidate.versionDisplay}  downloading…",
                            percent = 0
                        )
                    )
                    downloadAndReturn(candidate)
                    return
                }
                is FastModeFindResult.VersionMismatch -> {
                    val candidate = result.candidate
                    appendLog(
                        "Fast Mode: ${source.label} has ${candidate.versionDisplay}  " +
                            "build differs from requested ${request.versionCodeSummary ?: "version"}. Asking the user.",
                        LogLevel.Warning
                    )
                    val decisionGate = CompletableDeferred<FastModeChoice?>()
                    fastModeDecision = decisionGate
                    uiState = UiState.FastMode(
                        FastModeProgress(
                            sourceLabel = source.label,
                            detail = "Version code mismatch",
                            awaitingDecision = true,
                            mismatchDetail = "Requested build ${request.versionCodeSummary ?: "?"}  " +
                                "${source.label} has ${candidate.versionDisplay}."
                        )
                    )
                    val decision = decisionGate.await()
                    if (!fastModeActive || decision == null) return
                    if (decision == FastModeChoice.USE) {
                        appendLog(
                            "Fast Mode: using ${candidate.versionDisplay} from ${source.label} " +
                                "despite the code mismatch.",
                            LogLevel.Info
                        )
                        uiState = UiState.FastMode(
                            FastModeProgress(
                                sourceLabel = source.label,
                                detail = "Found ${candidate.versionDisplay}  downloading…",
                                percent = 0
                            )
                        )
                        downloadAndReturn(candidate)
                        return
                    }
                    appendLog("Fast Mode: user skipped ${source.label}  trying the next source.", LogLevel.Info)
                    recordFailedAttempt(source, CandidateOption.REQUESTED)
                    continue
                }
                FastModeFindResult.None -> {
                    recordFailedAttempt(source, CandidateOption.REQUESTED)
                    appendLog("Fast Mode: no exact match on ${source.label}.", LogLevel.Info)
                }
            }
        }
        fastModeActive = false
        fastModeQueue = null
        val message = "No source had the exact version ${request.requestedVersionLabel}."
        lastFailureMessage = message
        appendLog("Fast Mode: no source had the exact version. Use the sources below.", LogLevel.Warning)
        val activeRequest = request
        uiState = if (activeRequest != null) {
            UiState.FastMode(
                FastModeProgress(
                    detail = message,
                    done = true,
                    succeeded = false,
                    result = initialCandidateResult(activeRequest)
                )
            )
        } else {
            UiState.Idle
        }
    }

    private suspend fun fastModeNextLatest(request: HelperRequest) {
        val queue = fastModeQueue ?: return
        while (queue.isNotEmpty()) {
            if (!fastModeActive) return
            val source = queue.removeAt(0)
            appendLog("Fast Mode: fetching latest from ${source.label}...", LogLevel.Info)
            uiState = UiState.FastMode(
                FastModeProgress(sourceLabel = source.label, detail = "Checking ${source.label}…")
            )
            val candidates = withContext(Dispatchers.IO) {
                runCatching {
                    resolveSourceSection(request, source, CandidateOption.LATEST).candidates
                }.getOrDefault(emptyList())
            }
            if (!fastModeActive) return
            val best = fastModeLatestCandidate(request, candidates)
            if (best != null) {
                appendLog(
                    "Fast Mode: latest ${best.versionDisplay} on ${source.label} " +
                        "(${best.fileKind.uppercase(Locale.US)})  downloading.",
                    LogLevel.Info
                )
                uiState = UiState.FastMode(
                    FastModeProgress(
                        sourceLabel = source.label,
                        detail = "Found latest ${best.versionDisplay}  downloading…",
                        percent = 0
                    )
                )
                downloadAndReturn(best)
                return
            }
            recordFailedAttempt(source, CandidateOption.LATEST)
            appendLog("Fast Mode: no newer latest on ${source.label}.", LogLevel.Info)
        }
        fastModeActive = false
        fastModeQueue = null
        val message = "No source offered a newer version than ${request.requestedVersionLabel}."
        lastFailureMessage = message
        appendLog("Fast Mode: no source offered a newer version than requested. Use the sources below.", LogLevel.Warning)
        val activeRequest = request
        uiState = if (activeRequest != null) {
            UiState.FastMode(
                FastModeProgress(
                    detail = message,
                    done = true,
                    succeeded = false,
                    result = initialCandidateResult(activeRequest)
                )
            )
        } else {
            UiState.Idle
        }
    }

    /**
     * The newest direct-download candidate a source resolved for [option] LATEST.
     * Keeps the version strictly newer than the request when a requested version
     * name is known (so "latest" is only reported when it actually is newer); with
     * no requested version, returns the newest candidate regardless.
     */
    private fun fastModeLatestCandidate(
        request: HelperRequest,
        candidates: List<DownloadCandidate>
    ): DownloadCandidate? {
        val requested = request.requestedVersionName
        val newer = candidates
            .filter { it.directDownload && it.versionName != null }
            // A "-SECONDARY" build ties the requested version on its numbers
            // and then wins the string tiebreak, so it would be reported as
            // "latest"  but Morphe cannot patch those builds. Never
            // auto-select one unless the request itself asked for the variant.
            .filter { request.requestsVariantBuild || !it.hasVariantBuildMarker }
            .filter { candidate ->
                requested == null || compareVersionNames(candidate.versionName, requested) > 0
            }
        return newer.maxWithOrNull(
            Comparator<DownloadCandidate> { a, b ->
                val byVersion = compareVersionNames(b.versionName, a.versionName)
                if (byVersion != 0) byVersion else a.sortIndex.compareTo(b.sortIndex)
            }
        )
    }

    private suspend fun fastModeFindCandidate(
        request: HelperRequest,
        source: DownloadSource
    ): FastModeFindResult {
                val outcome = resolveSourceSection(request, source, CandidateOption.REQUESTED)
        // Multiple formats can be selected now, so take the first one in the
        // picker's order (APK before APKM/APKS/XAPK) instead of whichever order
        // the source happened to list.
        val exact = outcome.candidates.withIndex()
            .filter { (_, candidate) ->
                // Format differences are fine, but the exact version name AND
                // version code are both required. A captcha-gated page is still an
                // exact match: the in-app browser can reveal its file after the user
                // solves the challenge. A candidate that reports a code outside the
                // request is surfaced as a mismatch the user can accept or skip 
                // never silently downloaded.
                (candidate.directDownload || candidate.captchaUrl != null) &&
                    request.matchesRequestedVersionStrict(candidate)
            }
            .minWithOrNull(
                compareBy(
                    { request.fileKindPriority(it.value.fileKind) },
                    { it.index }
                )
            )
            ?.value
        if (exact != null) return FastModeFindResult.Exact(exact)

        // The version name exists on this source but with a different build 
        // hand it to the loop so it can ask the user before proceeding.
        val mismatch = outcome.candidates.firstOrNull { candidate ->
            candidate.directDownload &&
                // A secondary build is a different package, not a build
                // mismatch the user should be asked to accept.
                (request.requestsVariantBuild || !candidate.hasVariantBuildMarker) &&
                request.requestedVersionName != null &&
                candidate.versionName != null &&
                candidate.versionName.versionNameEquals(request.requestedVersionName) &&
                candidate.versionCode != null &&
                !request.matchesRequestedVersionStrict(candidate.versionName, candidate.versionCode)
        }
        return if (mismatch != null) {
            FastModeFindResult.VersionMismatch(mismatch)
        } else {
            FastModeFindResult.None
        }
    }

    private fun handleDownloadEvent(event: DownloadJobManager.Event?) {
        when (event) {
            null -> Unit
            is DownloadJobManager.Event.Paused -> {
                val current = uiState
                uiState = when (current) {
                    is UiState.Downloading -> current.copy(paused = event.paused)
                    is UiState.FastMode -> current.copy(
                        progress = current.progress.copy(paused = event.paused)
                    )
                    else -> current
                }
            }
            is DownloadJobManager.Event.Progress -> {
                val wasPaused = (uiState as? UiState.Downloading)?.paused
                    ?: (uiState as? UiState.FastMode)?.progress?.paused
                    ?: false
                uiState = if (fastModeActive) {
                    UiState.FastMode(
                        FastModeProgress(
                            sourceLabel = event.candidate.source.label,
                            detail = "Downloading from ${event.candidate.source.label}…",
                            percent = event.percent,
                            speedBytesPerSec = event.speedBytesPerSec,
                            etaMs = event.etaMs,
                            paused = wasPaused,
                            connections = event.connections
                        )
                    )
                } else {
                    UiState.Downloading(
                        event.candidate,
                        event.percent,
                        event.speedBytesPerSec,
                        event.etaMs,
                        paused = wasPaused,
                        connections = event.connections
                    )
                }
            }
            is DownloadJobManager.Event.PostDownloadStatus -> {
                uiState = if (fastModeActive) {
                    UiState.FastMode(
                        FastModeProgress(
                            sourceLabel = event.candidate.source.label,
                            detail = event.status,
                            percent = 100
                        )
                    )
                } else {
                    UiState.Downloading(
                        event.candidate,
                        percent = 100,
                        statusMessage = event.status
                    )
                }
            }
            is DownloadJobManager.Event.Scanning -> {
                val pct = event.percent ?: 100
                uiState = if (fastModeActive) {
                    UiState.FastMode(
                        FastModeProgress(
                            sourceLabel = event.candidate.source.label,
                            detail = event.status,
                            percent = pct,
                            etaMs = event.etaMs,
                            shaVerified = event.shaVerified
                        )
                    )
                } else {
                    UiState.Downloading(
                        event.candidate,
                        percent = pct,
                        etaMs = event.etaMs,
                        statusMessage = event.status
                    )
                }
            }
            is DownloadJobManager.Event.ScanAsk -> {
                // Only the activity that owns this request may show the scan
                // prompt. A stale instance (no request, or a different
                // package) must ignore it  and must NOT clear it first, or
                // the owner's collector would never see it (StateFlow
                // conflates: a clear before the owner processes the event
                // swallows it).
                val activeRequest = request
                if (activeRequest == null || event.candidate.packageName != activeRequest.packageName) return
                uiState = UiState.ScanAsk(event.candidate)
            }
            is DownloadJobManager.Event.ScanComplete -> {
                val activeRequest = request
                if (activeRequest == null || event.candidate.packageName != activeRequest.packageName) return
                val scanResult = event.result
                val isMalicious = scanResult is VirusTotalScanner.ScanResult.Malicious
                val detail = scanResultDetail(scanResult)
                // With the setting on, a scan that found nothing at all hands off on
                // its own: the result card exists to make the user judge a verdict, and
                // there is nothing to judge when every engine came back clean. Anything
                // short of that still shows the card.
                if (helperSettings.virusTotalAutoProceed && scanResult.isEntirelyClean()) {
                    appendLog("VirusTotal found nothing; handing off without asking.", LogLevel.Info)
                    proceedAfterScan()
                    return
                }
                uiState = UiState.ScanResult(
                    candidate = event.candidate,
                    scanResult = scanResult,
                    detail = detail,
                    isMalicious = isMalicious,
                    shaVerified = event.shaVerified
                )
            }
            is DownloadJobManager.Event.Completed -> {
                // StateFlow replays its last value to every new collector, so a
                // freshly created activity can receive the Completed event of a
                // previous request session. Only act when the event comes from
                // the session currently in flight (same package, same epoch);
                // otherwise discard it so the old file is never handed to a
                // different request. The version is intentionally NOT checked
                // here  the user may have deliberately downloaded a different
                // version (e.g. the Latest tab) in this session, and that file
                // must still be returned instead of leaving the UI stuck at
                // 100%.
                if (event.result.belongsToCurrentSession(request, event.epoch)) {
                    autoRetryActive = false
                    autoRetryQueue = null
                    autoRetryTarget = null
                    appendLog("Download validated: ${event.result.fileName}.")
                    if (!deliverResult(event.result)) {
                        appendLog(
                            "Download is ready; ${event.result.callerPackage} can request it again to receive the file.",
                            LogLevel.Warning
                        )
                        // Opened standalone with no caller to return to  keep the
                        // Fast Mode card showing the result and the source list
                        // reachable beneath it.
                        val wasFastMode = fastModeActive
                        fastModeActive = false
                        fastModeQueue = null
                        val activeRequest = request
                        uiState = if (wasFastMode) {
                            UiState.FastMode(
                                FastModeProgress(
                                    sourceLabel = event.result.sourceName,
                                    detail = "Download ready: ${event.result.fileName}. " +
                                        "Request it again from Morphe to receive it.",
                                    done = true,
                                    succeeded = true,
                                    result = activeRequest?.let { initialCandidateResult(it) }
                                )
                            )
                        } else if (activeRequest != null) {
                            UiState.Ready(initialCandidateResult(activeRequest))
                        } else {
                            UiState.Idle
                        }
                    }
                    // The owner has observed the event; clear it so a future
                    // activity recreation cannot replay it into a new request
                    // session.
                    DownloadJobManager.clearEvent()
                } else if (event.epoch != DownloadJobManager.currentEpoch) {
                    // A stale epoch belongs to an earlier session that no live
                    // activity owns any more. Safe to drop entirely.
                    appendLog(
                        "Ignoring download completion for a stale request session " +
                            "(epoch ${event.epoch}, current ${DownloadJobManager.currentEpoch}).",
                        LogLevel.Warning
                    )
                    if (uiState is UiState.Downloading) {
                        val activeRequest = request
                        uiState = if (activeRequest != null) {
                            UiState.Ready(initialCandidateResult(activeRequest))
                        } else {
                            UiState.Idle
                        }
                    }
                    DownloadJobManager.clearEvent()
                } else {
                    // Same epoch as the current session, but this instance does
                    // not own the request (no request, or a different package):
                    // for example a stale activity living in a separate task
                    // created from a bare launcher intent. Must ignore WITHOUT
                    // clearing, or the real owner's collector would never see
                    // the event (StateFlow conflates: a clear before the owner
                    // processes it swallows the completion and the file is
                    // never returned to the caller).
                    appendLog(
                        "Ignoring download completion for a different request session " +
                            "(epoch ${event.epoch}, current ${DownloadJobManager.currentEpoch}).",
                        LogLevel.Warning
                    )
                    if (uiState is UiState.Downloading) {
                        val activeRequest = request
                        uiState = if (activeRequest != null) {
                            UiState.Ready(initialCandidateResult(activeRequest))
                        } else {
                            UiState.Idle
                        }
                    }
                }
            }
            is DownloadJobManager.Event.Failed -> {
                if (event.epoch != 0L && event.epoch != DownloadJobManager.currentEpoch) return
                if (pendingSwitchSource != null) {
                    startPendingSwitch()
                    return
                }
                // Only the owning activity may act on a failure. A stale
                // instance (no request, or a different package) must ignore
                // it  and must NOT clear it first, or the owner's collector
                // would never see it (StateFlow conflates: a clear before
                // the owner processes the event swallows it).
                val failedRequest = request
                if (failedRequest == null || event.candidate.packageName != failedRequest.packageName) return
                appendLog(event.message, LogLevel.Error)
                if (autoRetryActive) {
                    recordFailedAttempt(event.candidate.source, event.candidate.option)
                    if (autoRetryQueue.isNullOrEmpty()) {
                        autoRetryActive = false
                        autoRetryQueue = null
                        autoRetryTarget = null
                        showFailure(
                            event.message,
                            event.candidate.source,
                            event.candidate.option
                        )
                    } else {
                        appendLog(
                            "Retry: ${event.candidate.source.label} did not work  trying the next source.",
                            LogLevel.Warning
                        )
                        DownloadJobManager.clearEvent()
                        lifecycleScope.launch {
                            retryNextNormally(failedRequest)
                        }
                    }
                    return
                }
                if (fastModeActive) {
                    recordFailedAttempt(event.candidate.source, event.candidate.option)
                    val activeRequest = request
                    if (activeRequest != null && !fastModeQueue.isNullOrEmpty()) {
                        appendLog(
                            "Fast Mode: ${event.candidate.source.label} did not work  trying the next source.",
                            LogLevel.Warning
                        )
                        lifecycleScope.launch {
                            if (fastModeRunPolicy == FastModePolicy.LATEST) {
                                fastModeNextLatest(activeRequest)
                            } else {
                                fastModeNext(activeRequest)
                            }
                        }
                    } else {
                        fastModeActive = false
                        fastModeQueue = null
                        val failureDetail = "Fast Mode failed: " +
                            event.message.lineSequence().firstOrNull().orEmpty().take(160)
                        lastFailureMessage = event.message
                        if (activeRequest != null) {
                            uiState = UiState.FastMode(
                                FastModeProgress(
                                    sourceLabel = event.candidate.source.label,
                                    detail = failureDetail,
                                    done = true,
                                    succeeded = false,
                                    result = initialCandidateResult(activeRequest)
                                )
                            )
                        } else {
                            showFailure(
                                event.message,
                                event.candidate.source,
                                event.candidate.option
                            )
                        }
                    }
                } else {
                    showFailure(
                        event.message,
                        event.candidate.source,
                        event.candidate.option
                    )
                }
            }
            is DownloadJobManager.Event.ValidationMismatch -> {
                // The download itself succeeded; only the version code differs
                // from the request (the parser couldn't know it up front). The
                // file is kept. Only Fast Mode asks the user; the check itself
                // is gated to Fast Mode, so this else branch is defensive only.
                val activeRequest = request
                // Only the activity that owns this request may act on the
                // event. A stale instance (no request, or a different request)
                // must ignore it  and must NOT clear it first, or the owner's
                // collector would never see it (StateFlow conflates: a clear
                // before the owner processes the event swallows it).
                if (activeRequest == null || event.candidate.packageName != activeRequest.packageName) return
                if (fastModeActive) {
                    appendLog(
                        "Fast Mode: ${event.candidate.source.label} downloaded " +
                            "${event.candidate.versionDisplay}  build differs from requested " +
                            "${activeRequest.versionCodeSummary ?: "version"}. Asking the user.",
                        LogLevel.Warning
                    )
                    val decisionGate = CompletableDeferred<FastModeChoice?>()
                    fastModeDecision = decisionGate
                    uiState = UiState.FastMode(
                        FastModeProgress(
                            sourceLabel = event.candidate.source.label,
                            detail = "Version code mismatch",
                            awaitingDecision = true,
                            mismatchDetail = "Requested build ${activeRequest.versionCodeSummary ?: "?"}  " +
                                "${event.candidate.source.label} downloaded " +
                                "${event.candidate.versionDisplay}, found build " +
                                "${event.foundVersionCode ?: "unknown"}."
                        )
                    )
                    lifecycleScope.launch {
                        val decision = decisionGate.await()
                        DownloadJobManager.clearEvent()
                        if (!fastModeActive || decision == null) {
                            event.file.delete()
                            return@launch
                        }
                        if (decision == FastModeChoice.USE) {
                            appendLog(
                                "Fast Mode: using downloaded ${event.candidate.versionDisplay} from " +
                                    "${event.candidate.source.label} despite the code mismatch.",
                                LogLevel.Info
                            )
                            runCatching {
                                returnPreparedFile(activeRequest, event.candidate, event.file, helperSettings)
                            }.onFailure { error ->
                                appendLog(error.message ?: "Could not return the file.", LogLevel.Error)
                                event.file.delete()
                            }
                            fastModeActive = false
                            fastModeQueue = null
                            uiState = UiState.Ready(initialCandidateResult(activeRequest))
                        } else {
                            event.file.delete()
                            appendLog(
                                "Fast Mode: user skipped ${event.candidate.source.label}  " +
                                    "trying the next source.",
                                LogLevel.Info
                            )
                            fastModeNext(activeRequest)
                        }
                    }
                } else {
                    DownloadJobManager.clearEvent()
                    event.file.delete()
                    val message = "Downloaded version code does not match the request " +
                        "(found ${event.foundVersionCode ?: "unknown"})."
                    appendLog(message, LogLevel.Error)
                    showFailure(
                        message,
                        event.candidate.source,
                        event.candidate.option
                    )
                }
            }
            is DownloadJobManager.Event.Cancelled -> {
                if (event.epoch != 0L && event.epoch != DownloadJobManager.currentEpoch) return
                if (pendingSwitchSource != null) {
                    startPendingSwitch()
                    return
                }
                // Only the owning activity may act on a cancellation. A stale
                // instance (no request, or a different package) must ignore
                // it  and must NOT clear it first, or the owner's collector
                // would never see it (StateFlow conflates: a clear before
                // the owner processes the event swallows it).
                val cancelledRequest = request
                if (cancelledRequest == null || event.candidate.packageName != cancelledRequest.packageName) return
                appendLog("Download cancelled.", LogLevel.Warning)
                autoRetryActive = false
                autoRetryQueue = null
                autoRetryTarget = null
                fastModeActive = false
                fastModeQueue = null
                val activeRequest = request
                uiState = if (activeRequest != null) {
                    UiState.Ready(initialCandidateResult(activeRequest))
                } else {
                    UiState.Idle
                }
            }
        }
    }

    private fun deliverPendingResultIfPresent(request: HelperRequest?): Boolean {
        if (request == null) return false
        val pending = DownloadJobManager.readPendingResult(applicationContext) ?: return false
        // The stored result belongs to a previous request session. A new request
        // for a different app (or for a different version of the same app)
        // invalidates it  drop the pending file and its stale "return to
        // Morphe" notification so the old APK can never be handed back for the
        // new request (e.g. when the old completion notification is tapped
        // while the new request is on screen).
        if (!pending.belongsTo(request)) {
            DownloadJobManager.clearPendingResult(applicationContext)
            cancelCompletionNotification()
            return false
        }
        return deliverResult(pending)
    }

    private fun cancelCompletionNotification() {
        runCatching {
            NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID_DONE)
        }
    }

    /**
     * When Morphe re-requests a package and version that were already
     * downloaded (e.g. a patch failed and the user retries, or the same
     * version needs patching again) and the file still exists because
     * auto-clear is off or a visible copy was saved to Downloads, offer the
     * user the choice: reuse the existing APK or download a fresh copy.
     * Only matches an exact package + version, so a different app or version
     * never gets an offer.
     */
    private fun offerExistingDownloadIfPresent(request: HelperRequest) {
        if (request.callerPackage.isEmpty() || request.requestedVersionName == null) return
        val existing = DownloadHistoryStore.entries(applicationContext)
            .filter { history ->
                history.packageName == request.packageName &&
                    request.matchesRequestedVersion(history.versionName, null)
            }
            // The same file may be recorded twice (cache copy + Downloads
            // copy); show each distinct file once, newest record first.
            .distinctBy { it.fileName }
            .filter { entry -> historyFileExists(entry) }
            .map { entry -> ReuseOption(entry, historyFileSize(entry)) }
        if (existing.isNotEmpty()) reuseOffer = existing
    }

    private fun historyFileExists(entry: DownloadHistoryEntry): Boolean {
        val uri = Uri.parse(entry.uri)
        return runCatching {
            if (entry.uri.startsWith("content://")) {
                contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
            } else {
                File(uri.path ?: return@runCatching false).exists()
            }
        }.getOrDefault(false)
    }

    private fun historyFileSize(entry: DownloadHistoryEntry): Long {
        val uri = Uri.parse(entry.uri)
        return runCatching {
            if (entry.uri.startsWith("content://")) {
                contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
            } else {
                File(uri.path ?: return@runCatching 0L).length()
            }
        }.getOrDefault(0L)
    }

    /** Hand the selected offered file back to Morphe instead of downloading again. */
    private fun useReuseOffer(option: ReuseOption) {
        reuseOffer = null
        val request = request ?: return
        val entry = option.entry
        appendLog(
            "Reusing previously downloaded ${entry.fileName} for ${request.packageName} " +
                "instead of re-downloading.",
            LogLevel.Info
        )
        if (logcatLoggingEnabled) {
            Log.i(TAG, "Reusing cached download for ${request.packageName}: ${entry.fileName}")
        }
        val pending = PendingDownloadResult(
            uri = entry.uri,
            fileName = entry.fileName,
            packageName = entry.packageName,
            versionName = entry.versionName,
            sourceName = entry.sourceName,
            requestPackage = request.packageName,
            callerPackage = request.callerPackage
        )
        DownloadJobManager.persistPendingResult(pending, applicationContext)
        deliverResult(pending)
    }

    private fun deliverResult(pending: PendingDownloadResult): Boolean {
        if (getCallingActivity() == null) return false
        val uri = Uri.parse(pending.uri)
        val result = Intent().apply {
            data = uri
            clipData = ClipData.newUri(contentResolver, pending.fileName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(DownloadHelperContract.EXTRA_RESULT_PACKAGE_NAME, pending.packageName)
            putExtra(DownloadHelperContract.EXTRA_RESULT_VERSION_NAME, pending.versionName)
            putExtra(DownloadHelperContract.EXTRA_RESULT_SOURCE_NAME, pending.sourceName)
            putExtra(DownloadHelperContract.EXTRA_RESULT_FILE_NAME, pending.fileName)
        }
        setResult(Activity.RESULT_OK, result)
        appendLog("Returned ${pending.fileName} to ${pending.callerPackage}.")
        if (logcatLoggingEnabled) {
            Log.i(
                TAG,
                "Query result: OK package=${pending.packageName}, " +
                    "version=${pending.versionName ?: "any"}, source=${pending.sourceName}, " +
                    "file=${pending.fileName}, uri=${pending.uri}"
            )
        }
        DownloadJobManager.clearPendingResult(applicationContext)
        cancelCompletionNotification()
        finish()
        return true
    }

    private fun shareHistoryEntry(entry: DownloadHistoryEntry) {
        val uri = Uri.parse(entry.uri)
        val share = Intent(Intent.ACTION_SEND).apply {
            type = fileNameMimeType(entry.fileName)
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(contentResolver, entry.fileName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            startActivity(Intent.createChooser(share, "Share ${entry.fileName}"))
        }.onFailure {
            appendLog("Could not share ${entry.fileName}.", LogLevel.Warning)
        }
    }

    private fun openHistoryEntry(entry: DownloadHistoryEntry) {
        val uri = Uri.parse(entry.uri)
        val open = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, fileNameMimeType(entry.fileName))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            startActivity(open)
        }.onFailure {
            appendLog("Could not open ${entry.fileName}.", LogLevel.Warning)
        }
    }

    private suspend fun returnPreparedFile(
        request: HelperRequest,
        candidate: DownloadCandidate,
        file: File,
        settings: HelperSettings
    ) {
        val handoff = withContext(Dispatchers.IO) {
            var prepared = prepareDownloadHandoff(file, settings)
            val canGrant = runCatching {
                grantUriPermission(
                    request.callerPackage,
                    prepared.uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }.isSuccess
            if (!canGrant) {
                AppLog.record(
                    LogLevel.Warning,
                    "Morphe could not be granted access to the saved copy; handing off the cache file."
                )
                prepared = cacheHandoff(file)
            }
            recordHandOff(request, candidate, file, prepared.uri)
            prepared
        }
        val pending = PendingDownloadResult(
            uri = handoff.uri.toString(),
            fileName = file.name,
            packageName = candidate.packageName,
            versionName = candidate.versionName,
            sourceName = candidate.source.label,
            requestPackage = request.packageName,
            callerPackage = request.callerPackage
        )
        DownloadJobManager.persistPendingResult(pending, applicationContext)
        scheduleHandoffCleanup(file, handoff, settings)
        if (!deliverResult(pending)) {
            appendLog(
                "File is ready; ${request.callerPackage} can request it again to receive it.",
                LogLevel.Warning
            )
        }
    }

    private fun returnPickedFile(candidate: DownloadCandidate, uri: Uri?) {
        val activeRequest = request ?: return
        val settings = helperSettings

        if (uri == null) {
            appendLog("File selection canceled.", LogLevel.Warning)
            return
        }

        appendLog("Checking manually selected file for ${candidate.source.label}.")
        uiState = UiState.CheckingPickedFile(candidate)

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val file = copyPickedFileToTemporary(candidate, uri)
                    validateDownloadedArtifact(this@MainActivity, activeRequest, candidate, file)
                    file
                }
            }

            result
                .onSuccess { file ->
                    appendLog("Selected file validated: ${file.name} (${file.length()} bytes).")
                    runCatching {
                        returnPreparedFile(activeRequest, candidate, file, settings)
                    }.onFailure { error ->
                        val message = (error.message ?: "Could not return selected APK to Morphe.")
                            .withManualModeHint()
                        appendLog(message, LogLevel.Error)
                        showFailure(message, candidate.source, candidate.option)
                    }
                }
                .onFailure { error ->
                    // A code-mismatch keeps the file (so Fast Mode can ask); the
                    // picked-file flow has no ask, so clean it up here.
                    if (error is VersionCodeMismatchException) {
                        error.file.delete()
                    }
                    val message = (error.message ?: "Selected file could not be used.")
                        .withManualModeHint()
                    appendLog(message, LogLevel.Error)
                    showFailure(message, candidate.source, candidate.option)
                }
        }
    }

    private fun returnInstalledApp(candidate: DownloadCandidate) {
        val activeRequest = request ?: return
        if (!isPackageInstalled(candidate.packageName)) {
            appendLog("${candidate.packageName} is not installed yet.", LogLevel.Warning)
            installedPackageRefreshToken++
            return
        }

        val result = Intent().apply {
            putExtra(DownloadHelperContract.EXTRA_RESULT_USE_INSTALLED_APP, true)
            putExtra(DownloadHelperContract.EXTRA_RESULT_PACKAGE_NAME, candidate.packageName)
            putExtra(DownloadHelperContract.EXTRA_RESULT_VERSION_NAME, candidate.versionName)
            putExtra(DownloadHelperContract.EXTRA_RESULT_SOURCE_NAME, candidate.source.label)
        }

        setResult(Activity.RESULT_OK, result)
        appendLog("Returned installed ${candidate.packageName} to ${activeRequest.callerPackage}.")
        if (logcatLoggingEnabled) {
            Log.i(
                TAG,
                "Query result: OK package=${candidate.packageName}, " +
                    "version=${candidate.versionName ?: "any"}, source=${candidate.source.label}, " +
                    "useInstalledApp=true"
            )
        }
        finish()
    }

    private fun copyPickedFileToTemporary(candidate: DownloadCandidate, uri: Uri): File {
        val displayName = displayNameForUri(uri)
        val extension = displayName
            ?.substringAfterLast('.', "")
            ?.takeIf { it.isNotBlank() && it != displayName }
            ?: candidate.fileKind.takeUnless { it.equals("web", ignoreCase = true) }
            ?: request?.requestedFileKinds?.orderedFileKinds()?.firstOrNull()
            ?: "apk"
        val outputName = displayName
            ?.takeIf(String::isNotBlank)
            ?: "${candidate.packageName}-${candidate.versionName ?: "manual"}.$extension"
        val outputFile = temporaryDownloadsDir().apply { mkdirs() }.uniqueChild(outputName)

        try {
            val bytesCopied = contentResolver.openInputStream(uri)?.use { input ->
                outputFile.outputStream().use { output -> input.copyTo(output) }
            } ?: error("Selected file could not be opened.")
            check(bytesCopied > 0L) { "Selected file was empty." }
            return outputFile
        } catch (error: Throwable) {
            outputFile.delete()
            throw error
        }
    }

    private fun displayNameForUri(uri: Uri): String? =
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
            ?.takeIf(String::isNotBlank)


}

internal val historyTimeFormat = SimpleDateFormat("MMM d, HH:mm", Locale.US)

internal fun formatHistoryTimestamp(timestamp: Long): String =
    synchronized(historyTimeFormat) { historyTimeFormat.format(Date(timestamp)) }

internal fun fileNameMimeType(fileName: String): String =
    when (fileName.substringAfterLast('.', "").lowercase(Locale.US)) {
        "apk" -> "application/vnd.android.package-archive"
        // Not application/zip: MediaStore and SAF append .zip when the display
        // name does not already end in the MIME type's extension, turning
        // app.apkm into app.apkm.zip. The generic type keeps the real suffix.
        else -> "application/octet-stream"
    }

internal fun Context.isHistoryUriUsable(uriString: String): Boolean {
    val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return false
    return runCatching {
        contentResolver.openInputStream(uri)?.use { } != null
    }.getOrDefault(false)
}

internal fun File.mimeType(): String = when (extension.lowercase(Locale.US)) {
    "apk" -> "application/vnd.android.package-archive"
    // Keep the archive's own extension. Using application/zip here makes the
    // Downloads provider save app.apkm as app.apkm.zip.
    else -> "application/octet-stream"
}

internal fun File.uniqueChild(fileName: String): File {
    val safeName = fileName.sanitizeFileName()
    val base = safeName.substringBeforeLast('.', safeName)
    val extension = safeName.substringAfterLast('.', "")
        .takeIf { it != safeName && it.isNotBlank() }
        ?.let { ".$it" }
        .orEmpty()
    var candidate = File(this, safeName)
    var index = 1
    while (candidate.exists()) {
        candidate = File(this, "$base ($index)$extension")
        index++
    }
    return candidate
}

@Composable
internal fun HelperTheme(
    themeMode: ThemeMode,
    themeStyle: ThemeStyle,
    pureBlackTheme: Boolean,
    accentColorHex: String,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    // A stored Material You choice is downgraded rather than dropped on devices
    // that do not expose the platform palette, so the setting survives a move
    // back to a newer phone.
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val style = if (themeStyle == ThemeStyle.MATERIAL_YOU && !supportsDynamicColor) {
        ThemeStyle.MORPHE
    } else {
        themeStyle
    }
    val base = when (style) {
        ThemeStyle.MONOCHROME -> if (dark) monochromeDarkColorScheme() else monochromeLightColorScheme()
        ThemeStyle.MATERIAL_YOU -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (dark) morpheDarkColorScheme() else morpheLightColorScheme()
        }
        ThemeStyle.MORPHE -> if (dark) morpheDarkColorScheme() else morpheLightColorScheme()
    }
    val colorScheme = base
        .let { if (dark && pureBlackTheme) it.withPureBlack() else it }
        // A custom accent belongs to the Morphe style alone: the other two own
        // their colours, so leaving one applied there would contradict the choice.
        .let {
            if (style == ThemeStyle.MORPHE) {
                accentColorHex.toAccentColorOrNull()?.let { accent -> it.withCustomAccent(accent, dark) } ?: it
            } else {
                it
            }
        }
    // The manager draws edge-to-edge behind fully transparent bars and lets each
    // screen apply its own status/navigation bar insets, so the helper adopts the
    // same setup. The appearance flags still decide whether the transparent bars
    // render light or dark icons.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.setDecorFitsSystemWindows(window, false)
            @Suppress("DEPRECATION")
            run {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
            }
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
            // Otherwise the system paints an opaque contrast scrim behind the
            // bars (set by default on Android 10+), which does not follow the
            // app background and shows as a lighter status/nav bar border in
            // dark mode on some devices. Mirrors the PotHelper fix.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
        }
    }
    // Decorations the scheme cannot reach (the brand gradient, generated letter
    // tiles) read this to flatten themselves in monochrome mode.
    CompositionLocalProvider(LocalMonochromeTheme provides (style == ThemeStyle.MONOCHROME)) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MorpheTypography,
            content = content
        )
    }
}

@Composable
internal fun HelperButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    MorpheDialogButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        filled = true
    )
}

@Composable
internal fun HelperOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    MorpheDialogButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        filled = false
    )
}

// ---- In-app captcha browser: a real WebView that passes Cloudflare-style
// challenges, captures the download URL the page produces, and hands it to the
// normal download pipeline. Any source that gates its file behind a captcha can
// opt in by setting DownloadCandidate.captchaUrl.





