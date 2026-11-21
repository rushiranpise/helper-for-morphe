package dev.rushi.apkdownloadhelper.ui.screen.captcha

import android.webkit.JavascriptInterface

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
