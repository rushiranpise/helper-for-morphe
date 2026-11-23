package dev.rushi.apkdownloadhelper.ui

import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import dev.rushi.apkdownloadhelper.DOWNLOAD_FILE_KIND_SET
import dev.rushi.apkdownloadhelper.HelperOutlinedButton
import dev.rushi.apkdownloadhelper.R
import dev.rushi.apkdownloadhelper.TAG
import dev.rushi.apkdownloadhelper.download.AdGuardDns
import dev.rushi.apkdownloadhelper.download.orderedFileKinds
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale


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
    LaunchedEffect(webView) {
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
    LaunchedEffect(webView, pageFinishedCount, challengeActive, challengeProbeKnown) {
        if (pageFinishedCount == 0 ||
            challengeActive ||
            !challengeProbeKnown ||
            injectedPage == pageFinishedCount
        ) {
            return@LaunchedEffect
        }
        injectedPage = pageFinishedCount
        when (candidate.viaSource ?: candidate.source) {
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
            else -> webView.evaluateJavascript(CAPTCHA_AUTOPRESS_JS, null)
        }
    }
    LaunchedEffect(challengeActive, challengeStartedAt) {
        if (!challengeActive || challengeStartedAt <= 0L) return@LaunchedEffect
        delay(BROWSER_CHALLENGE_TIMEOUT_MS)
        if (challengeActive) {
            logWebFlowState("CHALLENGE_TIMEOUT")
            onChallengeTimeout()
        }
    }
    LaunchedEffect(webView, pageFinishedCount, challengeActive) {
        val browserSource = candidate.viaSource ?: candidate.source
        if (browserSource != DownloadSource.UPTODOWN ||
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
                .padding(horizontal = MorpheDefaults.ContentPadding, vertical = MorpheDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val browserTitle = when {
                        candidate.source == DownloadSource.APK_MIRROR ||
                            candidate.source == DownloadSource.APK_PURE -> candidate.source.label
                        candidate.captchaUrl != null && !candidate.directDownload ->
                            "Solve captcha  ${candidate.source.label}"
                        else -> "Open in app  ${candidate.source.label}"
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
                    modifier = Modifier.widthIn(min = MorpheDefaults.CompactButtonWidth)
                )
            }
            if (candidate.source == DownloadSource.APK_MIRROR ||
                candidate.source == DownloadSource.APK_PURE
            ) {
                InfoBox(title = "Download from ${candidate.source.label}", icon = Icons.Outlined.OpenInBrowser) {
                    Text(
                        text = "Complete any browser check on the page. The file returns to " +
                            "Morphe automatically when its download starts.",
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

/** Posts captured URLs from the WebView's JS bridge back to Kotlin. */
internal class CaptchaCaptureBridge(
    private val onUrl: (String) -> Unit
) {
    @JavascriptInterface
    fun capture(url: String) {
        onUrl(url)
    }
}

internal const val CAPTCHA_BRIDGE_NAME = "Android"

/**
 * Injected into every page the captcha browser finishes loading. Captures
 * clicks on download-looking links and window.open calls that the download
 * listener would not see (e.g. links opened in a new tab).
 */
internal const val CAPTCHA_CAPTURE_JS = """
(function () {
  if (window.__captureInstalled) return;
  window.__captureInstalled = true;
  var re = /\.(apk|apks|apkm|xapk)(\?|#|$)/i;
  function isDw(u) {
    return /^https?:\/\/[^\/]*uptodown\.(com|net)\//i.test(u) && /\/dwn\//i.test(u);
  }
  function isApkMirrorFile(u) {
    return /^https?:\/\/([^\/]*\.)?apkmirror\.com\//i.test(u) && /download\.php/i.test(u);
  }
  function isApkPureFile(u) {
    return /^https?:\/\/([^\/]*\.)?apkpure\.com\/b\//i.test(u);
  }
  function looksLike(u) {
    if (!u) return false;
    try { u = decodeURIComponent(u); } catch (e) {}
    return re.test(u) || /filename[^.]*\.(apk|apks|apkm|xapk)/i.test(u)
      || isDw(u) || isApkMirrorFile(u) || isApkPureFile(u);
  }
  function capture(u) {
    window.__captured = true;
    window.Android && window.Android.capture(u);
  }
  document.addEventListener('click', function (e) {
    var el = e.target;
    while (el && el !== document && !(el.tagName === 'A' && el.href)) el = el.parentNode;
    if (el && el.tagName === 'A' && el.href && looksLike(el.href)) capture(el.href);
  }, true);
  var origOpen = window.open;
  window.open = function (u) {
    if (u && looksLike(u)) { capture(u); return null; }
    return origOpen ? origOpen.apply(window, arguments) : null;
  };
})();
"""

internal const val BROWSER_CHALLENGE_TIMEOUT_MS = 5 * 60_000L

/**
 * Reports whether the page is currently showing a Cloudflare-style challenge.
 * The probe is polled while the browser is open so a stalled page can be
 * reloaded only after its challenge clears.
 */
internal const val BROWSER_CHALLENGE_PROBE_JS = """
(function () {
  var frames = document.querySelectorAll('iframe');
  for (var i = 0; i < frames.length; i++) {
    var src = (frames[i].src || '').toLowerCase();
    if (src.indexOf('challenges.cloudflare.com') < 0) continue;
    var rect = frames[i].getBoundingClientRect();
    if (rect.width > 0 && rect.height > 0) return 'challenge';
  }
  var text = ((document.title || '') + ' ' +
    (document.body ? document.body.innerText : '')).toLowerCase();
  if (text.indexOf('verify you are human') >= 0
      || text.indexOf('security verification') >= 0
      || text.indexOf('checking your browser') >= 0
      || text.indexOf('just a moment') >= 0
      || text.indexOf('attention required') >= 0
      || text.indexOf('enable javascript and cookies to continue') >= 0) {
    return 'challenge';
  }
  return 'ready';
})();
"""

/** The probe returns the JSON-quoted string `"challenge"` or `"ready"`. */
internal fun String?.reportsBrowserChallenge(): Boolean =
    this?.contains("challenge", ignoreCase = true) == true

/**
 * Uptodown keeps its file behind a Turnstile widget: the page's own download button only
 * starts the file once that is solved. This clears the decoys and presses the real button
 * a few times, so solving the captcha is the only thing left for the user to do.
 *
 * Runs on every page load (the flow normally lands on the app page before the file page),
 * and stops trying once a download has been captured.
 */
internal const val CAPTCHA_AUTOPRESS_JS = """
(function () {
  if (!/(^|\.)uptodown\.(com|net)$/i.test(location.hostname)) return;
  var decoys = '#detail-download-button-know-native, [data-url*="uptodown-app-store"]';
  document.querySelectorAll(decoys).forEach(function (el) { el.style.display = 'none'; });

  // Uptodown's cookie banner can sit over the download button. Reject it
  // automatically, including inside the banner's same-origin iframe.
  function rejectCookies(doc) {
    var selectors = ['#onetrust-reject-all-handler', '.onetrust-reject-all-handler',
      '.cky-btn-reject', '.fc-cta-do-not-consent', '.osano-cm-deny',
      '[data-testid=uc-reject-all-button]'];
    for (var s = 0; s < selectors.length; s++) {
      var direct = doc.querySelector(selectors[s]);
      if (direct) {
        try { direct.click(); return true; } catch (e) {}
      }
    }
    var els = doc.querySelectorAll('button,a,[role=button],[role=link]');
    for (var i = 0; i < els.length; i++) {
      var el = els[i];
      var text = (el.innerText || el.textContent || '').replace(/\s+/g, ' ').trim().toLowerCase();
      if (!text || text.length > 40) continue;
      if (text === 'reject all' || text === 'reject' || text === 'decline'
          || text === 'refuse' || text === 'only necessary'
          || text === 'necessary only' || text.indexOf('reject all') >= 0
          || text.indexOf('only necessary') >= 0) {
        try { el.click(); return true; } catch (e) {}
      }
    }
    var frames = doc.querySelectorAll('iframe');
    for (var j = 0; j < frames.length; j++) {
      try {
        if (frames[j].contentDocument && rejectCookies(frames[j].contentDocument)) return true;
      } catch (e) {}
    }
    return false;
  }

  var consentTries = 0;
  var consentTimer = setInterval(function () {
    consentTries++;
    rejectCookies(document);
    if (consentTries > 60 || window.__captured) clearInterval(consentTimer);
  }, 500);

  var tries = 0;
  function press() {
    if (window.__captured || tries >= 4) return;
    tries++;
    var b = document.getElementById('detail-download-button');
    if (!b) return;
    b.scrollIntoView({ block: 'center' });
    b.click();
  }
  setTimeout(press, 2000);
  setTimeout(press, 6000);
  setTimeout(press, 12000);
})();
"""

/**
 * Walks APKMirror's search -> release -> variant -> download chain in the
 * in-app browser. The HTTP parser already does this with plain requests; this
 * is the same walk on a page that can pass Cloudflare, with the requested
 * version, ABI and build code used to pick the right variant.
 */
internal fun apkMirrorBrowserScript(
    candidate: DownloadCandidate,
    request: HelperRequest?,
    appName: String?,
    version: String?
): String {
    val fileType = candidate.fileKind.lowercase(Locale.US)
        .takeIf { it in DOWNLOAD_FILE_KIND_SET }
        ?: request?.requestedFileKinds?.orderedFileKinds()?.firstOrNull()
        ?: "apk"
    return """
        (function () {
        if (window.__mhApkMirrorStarted) return;
        window.__mhApkMirrorStarted = true;
        var pkg=__MH_PKG__,ver=__MH_VER__,name=__MH_NAME__,fileType=__MH_TYPE__,abi=__MH_ABI__;
        var codes=__MH_CODES__;
        var nameLower=(name||'').toLowerCase();
        var nameSlug=nameLower.replace(/[^a-z0-9]+/g,'-').replace(/^-+|-+$/g,'');
        var pkgParts=(pkg||'').toLowerCase().split('.').filter(function(p){
            return p.length>2&&p!=='com'&&p!=='org'&&p!=='net'&&p!=='android';
        });
        var verSlug=(ver||'').toLowerCase().split('.').join('-');
        var verCore=(ver||'').split('-')[0];
        var verCoreSlug=(verCore||'').toLowerCase().split('.').join('-');
        function txt(el){return (el&&el.innerText||'').trim();}
        function digits(s){return (s||'').replace(/[^0-9]/g,'');}
        function hasVer(s){
            var d=digits(s);
            if(ver&&d.indexOf(digits(ver))>=0)return true;
            return verCore&&verCore!==ver&&d.indexOf(digits(verCore))>=0;
        }
        function pathOf(href){
            try{return new URL(href,location.href).pathname.toLowerCase();}
            catch(e){return (href||'').toLowerCase();}
        }
        function segments(p){return p.split('/').filter(function(s){return s.length>0;});}
        function isApkPath(p){var s=segments(p);return s.length>=3&&s[0]==='apk';}
        function isRelease(p){return /-release\/?$/.test(p);}
        function isVariant(p){
            return /-android-(apk|apkm|apks)-download\/?$/.test(p);
        }
        function isAppPage(p){var s=segments(p);return s.length===3&&s[0]==='apk';}
        function rowCode(row){
            if(!row)return -1;
            var spans=row.querySelectorAll('span.colorLightBlack');
            for(var i=0;i<spans.length;i++){
                var span=spans[i];
                if(span.classList.contains('wrapText'))continue;
                if(span.querySelector('.dateyear_utc'))continue;
                var t=txt(span);
                if(/^\d+$/.test(t)){
                    var v=parseInt(t,10);
                    if(!isNaN(v)&&v>0)return v;
                }
            }
            var cells=row.querySelectorAll('.table-cell');
            for(var c=0;c<cells.length;c++){
                var cellText=txt(cells[c]);
                if(/^\d+$/.test(cellText)){
                    var cv=parseInt(cellText,10);
                    if(!isNaN(cv)&&cv>0)return cv;
                }
            }
            var parts=(row.innerText||'').split(/[^0-9]+/);
            for(var k=0;k<codes.length;k++){
                if(parts.indexOf(String(codes[k]))>=0)return codes[k];
            }
            var m=(row.innerText||'').match(/\b\d{6,}\b/g);
            if(!m)return -1;
            for(var j=0;j<m.length;j++){
                var mv=parseInt(m[j],10);
                if(!isNaN(mv)&&mv>0)return mv;
            }
            return -1;
        }
        function pageCode(){
            var vals=document.querySelectorAll('.appspec-value');
            for(var i=0;i<vals.length;i++){
                var m=txt(vals[i]).match(/\((\d{1,18})\)\s*$/);
                if(m){
                    var v=parseInt(m[1],10);
                    if(!isNaN(v)&&v>0)return v;
                }
            }
            var rows=document.querySelectorAll('tr');
            for(var r=0;r<rows.length;r++){
                var th=rows[r].querySelector('th'),td=rows[r].querySelector('td');
                if(!th||!td)continue;
                if(txt(th).toLowerCase()!=='version')continue;
                var tm=txt(td).match(/\((\d{1,18})\)\s*$/);
                if(tm){
                    var tv=parseInt(tm[1],10);
                    if(!isNaN(tv)&&tv>0)return tv;
                }
            }
            return -1;
        }
        function pickSearch(){
            var bestRelease=null,bestReleaseScore=-1,bestApp=null,bestAppScore=-1;
            var firstRelease=null,ls=document.querySelectorAll('a[href]');
            for(var i=0;i<ls.length;i++){
                var a=ls[i],h=a.href||'';
                if(!h||h.indexOf('#')>=0)continue;
                var hp=pathOf(h);
                if(!isApkPath(hp))continue;
                var rel=isRelease(hp),appPage=isAppPage(hp);
                if(!rel&&!appPage)continue;
                var t=txt(a).toLowerCase();
                if(!t)continue;
                var seg=segments(hp);
                var app=seg.length>2?seg[2]:'';
                var score=0;
                if(nameSlug&&app===nameSlug)score+=120;
                else if(nameSlug&&app.indexOf(nameSlug)>=0)score+=60;
                for(var j=0;j<pkgParts.length;j++){
                    if(app===pkgParts[j])score+=80;
                    else if(app.indexOf(pkgParts[j])>=0)score+=30;
                }
                if(nameLower&&t.indexOf(nameLower)>=0)score+=60;
                if(rel){
                    if(!firstRelease)firstRelease=a;
                    if(ver&&!(hp.indexOf(verSlug)>=0||hp.indexOf(verCoreSlug)>=0
                        ||hasVer(t)||hasVer(hp)))continue;
                    score+=20;
                    if(score>bestReleaseScore){bestReleaseScore=score;bestRelease=a;}
                }else if(appPage){
                    if(score>bestAppScore){bestAppScore=score;bestApp=a;}
                }
            }
            if(bestRelease&&bestReleaseScore>=60)return bestRelease;
            if(bestApp&&bestAppScore>=60)return bestApp;
            if(!ver&&bestRelease)return bestRelease;
            if(firstRelease){
                var relPath=pathOf(firstRelease.href);
                var slash=relPath.lastIndexOf('/');
                if(slash>0)return relPath.substring(0,slash+1);
            }
            return null;
        }
        function pickRelease(){
            if(ver&&!(location.pathname.toLowerCase().indexOf(verSlug)>=0
                ||location.pathname.toLowerCase().indexOf(verCoreSlug)>=0
                ||hasVer(location.pathname)))return null;
            var vs=document.querySelectorAll('a[href]');
            var fallback=null,plain=null,plainAbi=null,plainExact=null;
            var plainExactAbi=null,plainUniversal=null,bundle=null,bundleAbi=null;
            var bundleExact=null,bundleExactAbi=null;
            for(var v=0;v<vs.length;v++){
                var va=vs[v],vh=va.href||'';
                if(!vh||vh.indexOf('#')>=0)continue;
                var vp=pathOf(vh);
                if(!isVariant(vp))continue;
                var row=va.closest('.table-row'),rt=txt(row).toLowerCase();
                if(!rt){
                    var par=va.parentElement;
                    for(var u=0;u<4&&par;u++){
                        rt=txt(par).toLowerCase();
                        if(rt.indexOf('apk')>=0||rt.indexOf('bundle')>=0)break;
                        par=par.parentElement;
                    }
                }
                var isBundle=rt.indexOf('bundle')>=0||rt.indexOf('apkm')>=0
                    ||rt.indexOf('apks')>=0;
                var isPlain=!isBundle;
                var universal=rt.indexOf('universal')>=0;
                var hasAbi=abi&&rt.indexOf(abi.toLowerCase())>=0;
                var rc=rowCode(row);
                if(codes.length&&(rc<=0||codes.indexOf(rc)<0))continue;
                var exact=rc>0&&codes.indexOf(rc)>=0;
                if(!fallback)fallback=va;
                if(isPlain&&exact&&hasAbi&&!plainExactAbi)plainExactAbi=va;
                if(isPlain&&exact&&!plainExact)plainExact=va;
                if(isPlain&&universal&&!plainUniversal)plainUniversal=va;
                if(isPlain&&hasAbi&&!plainAbi)plainAbi=va;
                if(isPlain&&!plain)plain=va;
                if(isBundle&&exact&&hasAbi&&!bundleExactAbi)bundleExactAbi=va;
                if(isBundle&&exact&&!bundleExact)bundleExact=va;
                if(isBundle&&hasAbi&&!bundleAbi)bundleAbi=va;
                if(isBundle&&!bundle)bundle=va;
            }
            var wantBundle=fileType&&fileType.toLowerCase()!=='apk';
            if(wantBundle){
                if(bundleExactAbi)return bundleExactAbi;
                if(bundleExact)return bundleExact;
                if(plainExactAbi)return plainExactAbi;
                if(plainExact)return plainExact;
                return bundleAbi||bundle||plain||fallback;
            }
            if(plainExactAbi)return plainExactAbi;
            if(plainExact)return plainExact;
            return plainUniversal||plainAbi||plain||fallback;
        }
        function pickApp(){
            var rs=document.querySelectorAll('a[href]'),picked=null,first=null;
            for(var r=0;r<rs.length;r++){
                var ra=rs[r],rh=ra.href||'';
                if(!rh||rh.indexOf('#')>=0)continue;
                if(!isRelease(pathOf(rh)))continue;
                if(!first)first=ra;
                if(ver&&(rh.toLowerCase().indexOf(verSlug)>=0
                    ||rh.toLowerCase().indexOf(verCoreSlug)>=0
                    ||hasVer(txt(ra)))){picked=ra;break;}
            }
            return picked||(ver?null:first);
        }
        function pickDownload(){
            if(codes.length){
                var pc=pageCode();
                if(pc>0&&codes.indexOf(pc)<0){
                    if(!window.__mhBuildMismatch){
                        window.__mhBuildMismatch=true;
                        location.href='morphe-helper://build-mismatch?code='
                            +encodeURIComponent(pc);
                    }
                    return null;
                }
            }
            var ds=document.querySelectorAll('a,button');
            for(var d=0;d<ds.length;d++){
                var dh=ds[d].href||'';
                if(dh.indexOf('download.php')>=0
                    ||dh.indexOf('/download/?key=')>=0)return ds[d];
                var dt=txt(ds[d]).toLowerCase();
                if(dt.indexOf('download apk')>=0)return ds[d];
            }
            return null;
        }
        function pick(){
            var p=location.pathname.toLowerCase();
            var q=location.search.toLowerCase();
            if(q.indexOf('searchtype=')>=0)return pickSearch();
            if(isRelease(p))return pickRelease();
            if(isAppPage(p))return pickApp();
            if(isVariant(p))return pickDownload();
            return pickDownload();
        }
        var n=0,t=setInterval(function(){
            n++;
            var target=pick();
            if(target){
                clearInterval(t);
                if(typeof target==='string'){location.href=target;return;}
                try{target.scrollIntoView({block:'center'});}catch(e){}
                setTimeout(function(){try{target.click();}catch(e){}},300);
                return;
            }
            if(n>900)clearInterval(t);
        },400);
        })();
    """.trimIndent()
        .replace("__MH_PKG__", JSONObject.quote(candidate.packageName))
        .replace("__MH_VER__", JSONObject.quote(version.orEmpty()))
        .replace("__MH_NAME__", JSONObject.quote(appName.orEmpty()))
        .replace("__MH_TYPE__", JSONObject.quote(fileType))
        .replace(
            "__MH_ABI__",
            JSONObject.quote(request?.availableAbis?.firstOrNull().orEmpty())
        )
        .replace(
            "__MH_CODES__",
            request?.requestedVersionCodes
                ?.joinToString(separator = ",", prefix = "[", postfix = "]")
                ?: "[]"
        )
}

/**
 * Walks APKPure's search -> versions -> app -> download chain in the in-app
 * browser, the last resort after the direct endpoint and web parsers fail.
 */
internal fun apkPureBrowserScript(
    candidate: DownloadCandidate,
    appName: String?,
    version: String?
): String = """
    (function () {
      if (window.__mhApkPureStarted) return;
      window.__mhApkPureStarted = true;
      var pkg=__MH_PKG__,ver=__MH_VER__,name=__MH_NAME__;
      var nameLower=(name||'').toLowerCase();
      var pkgLower=(pkg||'').toLowerCase();
      function txt(el){return (el&&el.innerText||'').trim();}
      function digits(s){return (s||'').replace(/[^0-9]/g,'');}
      function hasVer(s){
        var d=digits(s);
        return !!ver&&d.indexOf(digits(ver))>=0;
      }
      function pathOf(href){
        try{return new URL(href,location.href).pathname.toLowerCase();}
        catch(e){return (href||'').toLowerCase();}
      }
      function pickSearch(){
        var ls=document.querySelectorAll('a');
        for(var i=0;i<ls.length;i++){
          var h=ls[i].href||'',t=txt(ls[i]).toLowerCase();
          if(!h||h.indexOf('/search')>=0)continue;
          if(h.toLowerCase().indexOf(pkgLower)>=0
            ||(nameLower.length>2&&t.indexOf(nameLower)>=0&&t.length<100))return ls[i];
        }
        return null;
      }
      function pickVersions(){
        var vs=document.querySelectorAll('a');
        for(var j=0;j<vs.length;j++){
          var vt=txt(vs[j]),vh=vs[j].href||'';
          if(hasVer(vt)||hasVer(vh))return vs[j];
        }
        return null;
      }
      function pickInfo(){
        var canonical=document.querySelector('link[rel=canonical]');
        if(canonical&&canonical.href
          &&canonical.href.toLowerCase().indexOf(pkgLower)>=0
          &&canonical.href.toLowerCase().indexOf('/apk-info')<0)return canonical.href;
        var ls=document.querySelectorAll('a[href]');
        for(var i=0;i<ls.length;i++){
          var h=ls[i].href||'';
          if(h.toLowerCase().indexOf(pkgLower)>=0
            &&h.toLowerCase().indexOf('/apk-info')<0)return ls[i];
        }
        return null;
      }
      function pickDownloadLink(){
        var a=document.querySelector('a[href*="d.apkpure.com"]');
        if(a)return a;
        var vs=document.querySelectorAll(
          'a.go-version-btn[href],a.dt-version-name-link[href],a#download_link[href]');
        for(var v=0;v<vs.length;v++){
          var vh=vs[v].href||'';
          if(!ver||hasVer(txt(vs[v]))||hasVer(vh))return vs[v];
        }
        var bs=document.querySelectorAll('a,button');
        for(var k=0;k<bs.length;k++){
          var bt=txt(bs[k]).toLowerCase();
          if(bt.indexOf('download apk')>=0&&bt.indexOf('apkpure app')<0)return bs[k];
        }
        return null;
      }
      function pick(){
        var p=location.pathname.toLowerCase();
        if(p.indexOf('/search')>=0)return pickSearch();
        if(p.indexOf('/apk-info')>=0)return pickInfo();
        if(p.indexOf('/versions')>=0)return pickVersions();
        var direct=pickDownloadLink();
        if(direct)return direct;
        if(p.indexOf('/download')<0&&ver&&!hasVer(document.body.innerText)){
          var old=document.querySelector('a[href*="/versions"]');
          if(old)return old;
        }
        return null;
      }
      var n=0,t=setInterval(function(){
        n++;
        var target=pick();
        if(target){
          clearInterval(t);
          if(typeof target==='string'){location.href=target;return;}
          try{target.scrollIntoView({block:'center'});}catch(e){}
          setTimeout(function(){try{target.click();}catch(e){}},500);
          return;
        }
        if(n>80)clearInterval(t);
      },700);
    })();
""".trimIndent()
    .replace("__MH_PKG__", JSONObject.quote(candidate.packageName))
    .replace("__MH_VER__", JSONObject.quote(version.orEmpty()))
    .replace("__MH_NAME__", JSONObject.quote(appName.orEmpty()))

/**
 * Percent-decodes a URL without touching `+` (which is a literal plus in a path) and
 * without `Uri.decode`, so these predicates stay usable off-device in unit tests.
 */
internal fun String.urlDecoded(): String = runCatching {
    URLDecoder.decode(replace("+", "%2B"), StandardCharsets.UTF_8.name())
}.getOrDefault(this)

/** True for URLs that point at an APK-family file (extension or filename hint). */
internal fun String.looksLikeApkDownload(): Boolean {
    val decoded = urlDecoded().lowercase(Locale.US)
    return Regex("""\.(apk|apks|apkm|xapk)(\?|#|$)""").containsMatchIn(decoded) ||
        Regex("""filename[^.]*\.(apk|apks|apkm|xapk)""").containsMatchIn(decoded)
}

/**
 * Uptodown's file links carry no extension at all (`dw.uptodown.com/dwn/<token>`), so the
 * extension test alone misses them: the in-app browser saw a navigation to a URL it could
 * not recognise as a file and let the page handle it.
 */
internal fun String.isUptodownFileUrl(): Boolean {
    val uri = runCatching { java.net.URI(urlDecoded()) }.getOrNull() ?: return false
    val host = uri.host?.lowercase(Locale.US) ?: return false
    if (!host.endsWith("uptodown.com") && !host.endsWith("uptodown.net")) return false
    return uri.path.orEmpty().contains("/dwn/", ignoreCase = true)
}

internal fun mergeCookieHeaders(first: String, second: String): String {
    val cookies = LinkedHashMap<String, String>()
    listOf(second, first).forEach { header ->
        header.split(';').forEach { pair ->
            val sep = pair.indexOf('=')
            if (sep > 0) {
                val name = pair.substring(0, sep).trim()
                val value = pair.substring(sep + 1).trim()
                if (name.isNotEmpty()) {
                    cookies[name] = value
                }
            }
        }
    }
    return cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
}

/**
 * Cookies the browser holds for a captured file URL, combining cookies from both
 * the download URL and the referer page that produced it (which carries the Cloudflare clearance).
 */
private fun browserCookieHeader(downloadUrl: String, referer: String?): String? {
    val manager = CookieManager.getInstance()
    val downloadCookies = manager.getCookie(downloadUrl)?.takeIf(String::isNotBlank)
    val refererCookies = referer?.let { manager.getCookie(it) }?.takeIf(String::isNotBlank)
    return when {
        downloadCookies != null && refererCookies != null -> mergeCookieHeaders(downloadCookies, refererCookies)
        downloadCookies != null -> downloadCookies
        else -> refererCookies
    }
}

/**
 * APKMirror's final file endpoint. The link carries no extension; the site
 * answers it with a redirect to the CDN, so it is safe to capture here.
 */
internal fun String.isApkMirrorFileUrl(): Boolean {
    val uri = runCatching { java.net.URI(urlDecoded()) }.getOrNull() ?: return false
    val host = uri.host?.lowercase(Locale.US) ?: return false
    val apkMirrorHost = host == "apkmirror.com" || host.endsWith(".apkmirror.com")
    return apkMirrorHost && uri.path.orEmpty().contains("download.php", ignoreCase = true)
}

/**
 * APKPure's direct file endpoint (`d.apkpure.com/b/APK/<pkg>`). Capturing it
 * lets the downloader follow the signed CDN redirect with the page's cookies.
 */
internal fun String.isApkPureFileUrl(): Boolean {
    val uri = runCatching { java.net.URI(urlDecoded()) }.getOrNull() ?: return false
    val host = uri.host?.lowercase(Locale.US) ?: return false
    val apkPureHost = host == "apkpure.com" || host.endsWith(".apkpure.com")
    return apkPureHost && uri.path.orEmpty().trim('/').startsWith("b/", ignoreCase = true)
}

/** A URL the in-app browser should treat as the file it was opened to capture. */
internal fun String.isCapturableDownload(): Boolean =
    looksLikeApkDownload() || isUptodownFileUrl() || isApkMirrorFileUrl() || isApkPureFileUrl()

/** Navigation sent by a page script when its build code is not the requested one. */
internal fun String.isBrowserBuildMismatchUrl(): Boolean =
    startsWith("morphe-helper://build-mismatch", ignoreCase = true)

internal fun String?.browserBuildMismatchMessage(expected: String?): String =
    "Download page is build ${this?.takeIf(String::isNotBlank) ?: "unknown"}, " +
        "expected ${expected?.takeIf(String::isNotBlank) ?: "the requested build"}."

/** APKCube uses its own catalogue and browser flow. */
internal fun DownloadSource.isApkCubeBrowserFlow(): Boolean =
    this == DownloadSource.APK_CUBE

/**
 * APKCube's download flow stays on its own site. Off-site links on the download
 * page are prelander ads, so the in-app browser must not follow them.
 */
internal fun String.isApkCubeHostedUrl(): Boolean {
    val uri = runCatching { java.net.URI(urlDecoded()) }.getOrNull() ?: return false
    val host = uri.host?.lowercase(Locale.US) ?: return false
    return host == "apkcube.com" || host.endsWith(".apkcube.com")
}

/**
 * Navigation allowed during an APKCube download: the catalogue itself, its
 * file hosts, and direct file links. Every other host is a prelander ad.
 */
internal fun String.isApkCubeAllowedNavigation(): Boolean {
    if (isApkCubeHostedUrl() || looksLikeApkDownload()) return true
    val uri = runCatching { java.net.URI(urlDecoded()) }.getOrNull() ?: return false
    val host = uri.host?.lowercase(Locale.US) ?: return false
    if (
        host == "cloudflarestorage.com" ||
        host.endsWith(".cloudflarestorage.com") ||
        host == "r2.dev" ||
        host.endsWith(".r2.dev")
    ) {
        return true
    }
    // APKCube's file endpoint is not always on a CDN host and can carry no
    // extension. Only a real file endpoint is allowed here; generic `/download`
    // paths on unknown hosts are prelander pages.
    val path = uri.path.orEmpty().lowercase(Locale.US)
    return path.endsWith(".apk") ||
        path.endsWith(".apks") ||
        path.endsWith(".apkm") ||
        path.endsWith(".xapk") ||
        path.contains("download.php")
}




