package dev.rushi.apkdownloadhelper.ui.screen.captcha

import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import dev.rushi.apkdownloadhelper.HelperOutlinedButton
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.TAG
import dev.rushi.apkdownloadhelper.download.AdGuardDns
import dev.rushi.apkdownloadhelper.ui.shared.card.InfoBox
import dev.rushi.apkdownloadhelper.ui.shared.card.SectionCard
import dev.rushi.apkdownloadhelper.ui.model.BrowserDownloadCapture
import dev.rushi.apkdownloadhelper.ui.model.DownloadCandidate
import dev.rushi.apkdownloadhelper.ui.model.DownloadSource
import dev.rushi.apkdownloadhelper.ui.model.HelperRequest
import dev.rushi.apkdownloadhelper.ui.theme.AppDefaults
import java.io.ByteArrayInputStream
import java.util.Locale
import kotlinx.coroutines.delay
import dev.rushi.apkdownloadhelper.ui.model.CandidateOption

@Composable
internal fun CaptchaBrowserScreen(
    candidate: DownloadCandidate,
    request: HelperRequest?,
    requiredAppName: String?,
    requiredVersion: String?,
    onClose: () -> Unit,
    onDownloadCaptured: (BrowserDownloadCapture) -> Unit,
    onBuildMismatch: (String?) -> Unit,
    onChallengeTimeout: () -> Unit
) {
    val context = LocalContext.current
    val isManualMode = candidate.option == CandidateOption.MANUAL
    var progress by remember { mutableIntStateOf(0) }
    var verificationHintVisible by remember { mutableStateOf(false) }
    var challengeActive by remember { mutableStateOf(false) }
    var challengeProbeKnown by remember { mutableStateOf(false) }
    var challengeStartedAt by remember { mutableStateOf(0L) }
    var pageFinishedCount by remember { mutableIntStateOf(0) }
    var injectedPage by remember { mutableIntStateOf(-1) }
    var readyLogged by remember { mutableStateOf(false) }
    var lastProgressAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var reloadAttempts by remember { mutableIntStateOf(0) }
    val webFlowStartedAt = remember { System.currentTimeMillis() }

    fun logWebFlowState(state: String, detail: String = "") {
        val source = (candidate.viaSource ?: candidate.source).name.lowercase(Locale.US)
        val elapsed = System.currentTimeMillis() - webFlowStartedAt
        Log.i(
            TAG,
            "webflow state $source $state" +
                (detail.takeIf(String::isNotBlank)?.let { " $it" } ?: "") +
                " (+${elapsed}ms)"
        )
    }
    // The page that produced the link is the referer APKMirror and APKPure
    // expect. It changes as their scripts walk search -> release -> download.
    val browserPageUrl = remember { mutableStateOf(candidate.captchaUrl ?: candidate.url) }
    val webViewUserAgent = remember { WebSettings.getDefaultUserAgent(context) }
    val bridge = remember {
        CaptchaCaptureBridge { url ->
            Handler(Looper.getMainLooper()).post {
                val referer = browserPageUrl.value
                onDownloadCaptured(
                    BrowserDownloadCapture(
                        downloadUrl = url,
                        refererUrl = referer,
                        cookieHeader = browserCookieHeader(url, referer),
                        userAgent = webViewUserAgent
                    )
                )
            }
        }
    }
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString = webViewUserAgent
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            addJavascriptInterface(bridge, CAPTCHA_BRIDGE_NAME)
            fun handleBrowserNavigation(view: WebView?, rawUrl: String?): Boolean {
                val url = rawUrl ?: return false
                if (url.isBrowserBuildMismatchUrl()) {
                    onBuildMismatch(
                        runCatching { Uri.parse(url).getQueryParameter("code") }.getOrNull()
                    )
                    return true
                }
                if (url.isCapturableDownload()) {
                    val referer = view?.url ?: view?.originalUrl ?: browserPageUrl.value
                    onDownloadCaptured(
                        BrowserDownloadCapture(
                            downloadUrl = url,
                            refererUrl = referer,
                            cookieHeader = browserCookieHeader(url, referer),
                            userAgent = webViewUserAgent
                        )
                    )
                    return true
                }
                val browserSource = candidate.viaSource ?: candidate.source
                if (browserSource.isApkCubeBrowserFlow() &&
                    !url.isApkCubeAllowedNavigation()
                ) {
                    Log.i(TAG, "Blocked off-site APKCube navigation: $url")
                    return true
                }
                return false
            }
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    // Apply AdGuard DNS filtering to the in-app browser: when the
                    // setting is on, drop subresources whose host AdGuard filters
                    // (ads/trackers answer 0.0.0.0). Any resolver failure means
                    // "not blocked", so the page still loads.
                    val host = request?.url?.host ?: return null
                    if (AdGuardDns.isBlocked(host)) {
                        Log.i(TAG, "AdGuard DNS: blocked ${request.url} in the in-app browser")
                        return WebResourceResponse(
                            "text/plain",
                            "utf-8",
                            ByteArrayInputStream(ByteArray(0))
                        )
                    }
                    return null
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean = handleBrowserNavigation(view, request?.url?.toString())

                @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    url: String?
                ): Boolean = handleBrowserNavigation(view, url)

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    browserPageUrl.value = url ?: browserPageUrl.value
                    readyLogged = false
                    challengeProbeKnown = false
                    lastProgressAt = System.currentTimeMillis()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    browserPageUrl.value = url ?: browserPageUrl.value
                    pageFinishedCount++
                    lastProgressAt = System.currentTimeMillis()
                    view?.evaluateJavascript(CAPTCHA_CAPTURE_JS, null)
                }
            }
            setDownloadListener { url, _, _, _, _ ->
                // Authoritative signal: the page started a real download.
                val referer = browserPageUrl.value
                onDownloadCaptured(
                    BrowserDownloadCapture(
                        downloadUrl = url,
                        refererUrl = referer,
                        cookieHeader = browserCookieHeader(url, referer),
                        userAgent = webViewUserAgent
                    )
                )
            }
        }
    }
    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }
    LaunchedEffect(candidate.captchaUrl ?: candidate.url) {
        val startUrl = candidate.captchaUrl ?: candidate.url
        browserPageUrl.value = startUrl
        webView.loadUrl(startUrl)
    }
    LaunchedEffect(webView, isManualMode) {
        if (isManualMode) return@LaunchedEffect
        while (true) {
            delay(1_200)
            webView.evaluateJavascript(BROWSER_CHALLENGE_PROBE_JS) { value ->
                val detected = value.reportsBrowserChallenge()
                challengeProbeKnown = true
                if (!detected && !readyLogged && pageFinishedCount > 0) {
                    readyLogged = true
                    logWebFlowState("READY")
                }
                if (detected != challengeActive) {
                    challengeActive = detected
                    verificationHintVisible = detected
                    if (detected) {
                        challengeStartedAt = System.currentTimeMillis()
                        logWebFlowState("CHALLENGE")
                    } else {
                        logWebFlowState("CHALLENGE_CLEARED")
                    }
                }
            }
        }
    }
    LaunchedEffect(webView, pageFinishedCount, challengeActive, challengeProbeKnown, isManualMode) {
        if (isManualMode ||
            pageFinishedCount == 0 ||
            challengeActive ||
            !challengeProbeKnown ||
            injectedPage == pageFinishedCount
        ) {
            return@LaunchedEffect
        }
        injectedPage = pageFinishedCount
        val currentUrl = browserPageUrl.value
        val host = runCatching { Uri.parse(currentUrl).host.orEmpty().lowercase(Locale.US) }.getOrDefault("")
        val effectiveSource = when {
            host == "apkmirror.com" || host.endsWith(".apkmirror.com") -> DownloadSource.APK_MIRROR
            host == "apkpure.com" || host.endsWith(".apkpure.com") -> DownloadSource.APK_PURE
            host == "uptodown.com" || host.endsWith(".uptodown.com") || host.endsWith(".uptodown.net") -> DownloadSource.UPTODOWN
            else -> candidate.viaSource ?: candidate.source
        }
        when (effectiveSource) {
            DownloadSource.APK_MIRROR -> webView.evaluateJavascript(
                apkMirrorBrowserScript(
                    candidate = candidate,
                    request = request,
                    appName = requiredAppName ?: candidate.name,
                    version = requiredVersion ?: candidate.versionName
                ),
                null
            )
            DownloadSource.APK_PURE -> webView.evaluateJavascript(
                apkPureBrowserScript(
                    candidate = candidate,
                    appName = requiredAppName ?: candidate.name,
                    version = requiredVersion ?: candidate.versionName
                ),
                null
            )
            DownloadSource.UPTODOWN -> webView.evaluateJavascript(CAPTCHA_AUTOPRESS_JS, null)
            else -> Unit
        }
    }
    LaunchedEffect(challengeActive, challengeStartedAt, isManualMode) {
        if (isManualMode || !challengeActive || challengeStartedAt <= 0L) return@LaunchedEffect
        delay(BROWSER_CHALLENGE_TIMEOUT_MS)
        if (challengeActive) {
            logWebFlowState("CHALLENGE_TIMEOUT")
            onChallengeTimeout()
        }
    }
    LaunchedEffect(webView, pageFinishedCount, challengeActive, isManualMode) {
        val browserSource = candidate.viaSource ?: candidate.source
        if (isManualMode ||
            browserSource != DownloadSource.UPTODOWN ||
            pageFinishedCount == 0 ||
            challengeActive ||
            reloadAttempts >= 2
        ) {
            return@LaunchedEffect
        }
        delay(10_000)
        if (!challengeActive && reloadAttempts < 2) {
            val idle = System.currentTimeMillis() - lastProgressAt
            reloadAttempts++
            logWebFlowState("STALLED", "idle=${idle}ms reload $reloadAttempts/2")
            webView.reload()
        }
    }

    fun verifyNow() {
        webView.requestFocus()
        webView.evaluateJavascript(
            """
            (function () {
              var el = document.querySelector(
                'iframe[src*=turnstile], .cf-turnstile, #cf-turnstile, ' +
                'iframe[src*=captcha], [class*=captcha], input[name*=captcha]');
              if (el) el.scrollIntoView({ block: 'center', behavior: 'smooth' });
            })();
            """.trimIndent(),
            null
        )
    }
    BackHandler {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            onClose()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = AppDefaults.ContentPadding, vertical = AppDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDefaults.ItemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val browserTitle = when {
                        isManualMode -> "Open in app · ${candidate.source.label}"
                        candidate.source == DownloadSource.APK_MIRROR ||
                            candidate.source == DownloadSource.APK_PURE -> candidate.source.label
                        candidate.captchaUrl != null && !candidate.directDownload ->
                            "Solve captcha · ${candidate.source.label}"
                        else -> "Open in app · ${candidate.source.label}"
                    }
                    Text(
                        text = browserTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // The version Morphe actually needs, because on the captcha and
                    // manual routes the candidate carries none of its own (it reads
                    // "Manual"), and the page has to be searched for an exact version
                    // string that is usually too long to remember.
                    val wanted = requiredVersion?.takeIf { it.isNotBlank() }
                    // Morphe names the app ("YouTube"); a manual candidate only knows
                    // its package name, which is the fallback when no request came in.
                    val appLabel = requiredAppName?.takeIf { it.isNotBlank() } ?: candidate.name
                    Text(
                        text = if (wanted != null) {
                            "$appLabel · needs $wanted"
                        } else {
                            "$appLabel · ${candidate.versionDisplay}"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        // Two lines, so a long version string stays readable rather
                        // than being cut down to its first few digits.
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                HelperOutlinedButton(
                    text = "Close",
                    onClick = onClose,
                    icon = Icons.Outlined.Close,
                    modifier = Modifier.widthIn(min = AppDefaults.CompactButtonWidth)
                )
            }
            if (!isManualMode && (candidate.source == DownloadSource.APK_MIRROR ||
                candidate.source == DownloadSource.APK_PURE)
            ) {
                InfoBox(title = "Download from ${candidate.source.label}", icon = Icons.Outlined.OpenInBrowser) {
                    Text(
                        text = "Solve any captcha on the page. Once cleared, the app will handle " +
                            "everything else automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (candidate.captchaUrl != null && !candidate.directDownload) {
                InfoBox(title = stringResource(R.string.solve_the_captcha), icon = Icons.Outlined.Shield) {
                    Text(
                        text = stringResource(R.string.when_the_page_starts_the_download) +
                            "returned to Morphe automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                InfoBox(title = stringResource(R.string.find_the_download_link), icon = Icons.Outlined.OpenInBrowser) {
                    Text(
                        text = stringResource(R.string.the_app_downloads_the_file_once),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "package \"${candidate.packageName}\" · version " +
                            "\"${candidate.versionDisplay}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (progress > 0 && progress < 100) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (verificationHintVisible) {
                InfoBox(
                    title = stringResource(R.string.download_verification_title),
                    modifier = Modifier.clickable { verifyNow() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    titleColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    icon = Icons.Outlined.Warning,
                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(
                        text = stringResource(R.string.download_verification_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    HelperOutlinedButton(
                        text = stringResource(R.string.download_verify_now),
                        onClick = { verifyNow() },
                        icon = Icons.Outlined.Shield,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            SectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    factory = { webView },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
