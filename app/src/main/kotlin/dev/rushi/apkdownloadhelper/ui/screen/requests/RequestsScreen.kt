package dev.rushi.apkdownloadhelper.ui.screen.requests

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.rushi.apkdownloadhelper.ui.screen.home.LocalPagerPageSelected
import dev.rushi.apkdownloadhelper.ui.util.openExternal
import kotlin.math.roundToInt

private const val REQUESTS_HOST = "morphe-patches.software"
private const val REQUESTS_URL = "https://$REQUESTS_HOST/requests"

/** The site's board and its vote calls expect the same client. */
private const val REQUESTS_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/125.0 Mobile Safari/537.36"

private val DarkPageBackground = Color(0xFF18181B)
private val LightPageBackground = Color(0xFFFFFFFF)

/**
 * Hosts the site's own request board. Search, voting, the Turnstile check and the
 * app detail sheet all stay exactly as they are on the web.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun RequestsScreen(
    bottomContentPadding: Dp = 160.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val dark = rememberUpdatedState(isDark)
    val bottomPadding = rememberUpdatedState(bottomContentPadding.value.roundToInt())
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var hasLoadedOnce by remember { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(
                (if (isDark) DarkPageBackground else LightPageBackground).toArgb()
            )
            val cookies = CookieManager.getInstance()
            cookies.setAcceptCookie(true)
            cookies.setAcceptThirdPartyCookies(this, true)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString = REQUESTS_USER_AGENT
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.setSupportZoom(false)
            settings.javaScriptCanOpenWindowsAutomatically = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.allowFileAccess = false
            settings.allowContentAccess = false

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val host = request.url.host.orEmpty()
                    if (host == REQUESTS_HOST || host.endsWith(".$REQUESTS_HOST")) {
                        return false
                    }
                    openExternal(context, request.url.toString())
                    return true
                }

                override fun onPageStarted(
                    view: WebView,
                    url: String?,
                    favicon: Bitmap?
                ) {
                    loading = true
                    failed = false
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    loading = false
                    hasLoadedOnce = true
                    canGoBack = view.canGoBack()
                    view.evaluateJavascript(
                        requestsPageScript(dark.value, bottomPadding.value),
                        null
                    )
                }

                override fun doUpdateVisitedHistory(
                    view: WebView,
                    url: String?,
                    isReload: Boolean
                ) {
                    canGoBack = view.canGoBack()
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    if (request.isForMainFrame) {
                        failed = true
                        loading = false
                    }
                }
            }
            loadUrl(REQUESTS_URL)
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    LaunchedEffect(isDark, bottomContentPadding) {
        webView.setBackgroundColor(
            (if (isDark) DarkPageBackground else LightPageBackground).toArgb()
        )
        webView.evaluateJavascript(
            requestsPageScript(isDark, bottomContentPadding.value.roundToInt()),
            null
        )
    }

    // The page stays composed while another tab is shown, so only take Back
    // when the board is visible and has somewhere to return to.
    val pageSelected = LocalPagerPageSelected.current
    BackHandler(enabled = pageSelected && canGoBack) {
        webView.goBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AndroidView(
            factory = { webView },
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .statusBarsPadding()
        )

        if (loading && !failed && !hasLoadedOnce) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        if (hasLoadedOnce && !failed) {
            Surface(
                onClick = {
                    loading = true
                    webView.reload()
                },
                enabled = !loading,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 12.dp)
                    .size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Reload the request board",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (failed) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Could not load the request board.",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Check the connection and try again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                TextButton(
                    onClick = {
                        failed = false
                        loading = true
                        webView.reload()
                    }
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

/**
 * Keeps the board's own theme in step with the app, leaves room for the app's
 * bottom bar, and gives the captcha card its natural height. The site's modal
 * otherwise collapses to max-height 0 in a WebView and clips the widget.
 */
private fun requestsPageScript(dark: Boolean, bottomPaddingPx: Int): String = """
(function () {
  var theme = '${if (dark) "dark" else "light"}';
  try { localStorage.setItem('mp-theme', theme); } catch (error) {}
  document.documentElement.setAttribute('data-theme', theme);

  var style = document.getElementById('helper-requests-style');
  if (!style) {
    style = document.createElement('style');
    style.id = 'helper-requests-style';
    style.textContent =
      '#rq-captcha{padding:8px !important;align-items:center !important;' +
      'justify-content:center !important;}' +
      '#rq-captcha .rq-captcha-modal{max-height:none !important;height:auto !important;' +
      'overflow:visible !important;padding:16px !important;gap:12px !important;' +
      'max-width:336px !important;}' +
      '#rq-captcha .rq-captcha-widget{width:304px !important;max-width:100% !important;' +
      'min-height:78px !important;}' +
      // The board animates every row in with fill-mode both. WebView can leave
      // rows inserted after the first paint stuck at the animation's opacity 0,
      // so skip the entry animations and keep the rows visible.
      '*,*::before,*::after{animation:none !important;' +
      'transition:none !important;}' +
      '.app-row,.rq-row,.rq-card,.bundle-card{opacity:1 !important;' +
      'transform:none !important;}' +
      // The app already has its own navigation; the site header only eats
      // vertical space and hides itself on scroll.
      'nav#u3c4db{display:none !important;}' +
      // Sticky header and modal blurs are expensive to recomposite per frame.
      '.u3c4db,.u60f3e,.modal-backdrop{backdrop-filter:none !important;' +
      '-webkit-backdrop-filter:none !important;}' +
      'html{scroll-behavior:auto !important;' +
      'overscroll-behavior:none !important;}';
    document.head.appendChild(style);
  }

  if (document.body) {
    document.body.style.paddingBottom = '${bottomPaddingPx}px';
  }
})();
""".trimIndent()
