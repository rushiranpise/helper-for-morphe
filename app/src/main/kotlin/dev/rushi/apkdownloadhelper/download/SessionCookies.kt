package dev.rushi.apkdownloadhelper.download

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.Locale

/**
 * Session cookies kept for the life of the process.
 *
 * APKMirror and similar sites hand out a session cookie with the first response and
 * expect it back on the follow-ups; without it every hop can look like a fresh visitor and
 * re-trigger a Cloudflare challenge. The scraping client and the download client are
 * separate instances in the same process, so the jar is shared rather than per client -
 * that is what lets a cookie from the page hop travel to the file hop.
 *
 * In memory only: cookies here are session state, and persisting them would keep a stale
 * clearance cookie alive across launches, which is worse than asking for a new one.
 */
internal object SessionCookies : CookieJar {

    /** Enough for a site with several paths, small enough that a bad actor cannot grow it. */
    private const val MAX_COOKIES_PER_HOST = 64

    private val byHost = HashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        val host = url.host.lowercase(Locale.US)
        val now = System.currentTimeMillis()
        synchronized(byHost) {
            byHost.getOrPut(host) { mutableListOf() }
                .removeAll { it.expiresAt < now }
            cookies.forEach { cookie ->
                // A re-set cookie replaces the old one of the same name and path instead
                // of piling up, which is what a session refresh looks like.
                byHost[host]?.removeAll { existing ->
                    existing.name == cookie.name && existing.path == cookie.path
                }
                byHost[host]?.add(cookie)
            }
            val stored = byHost[host]
            if (stored != null && stored.size > MAX_COOKIES_PER_HOST) {
                byHost[host] = stored.takeLast(MAX_COOKIES_PER_HOST).toMutableList()
            }
        }
    }

    /**
     * Adds the cookies a WebView already holds for [url] to this jar.
     *
     * The in-app browser keeps its cookies in Android's [android.webkit.CookieManager],
     * not in this jar, so a download that follows a solved challenge would otherwise
     * start without the clearance cookie. [header] is the Cookie request header
     * format returned by `CookieManager.getCookie`, not a Set-Cookie header.
     */
    fun importCookieHeader(url: HttpUrl, header: String?) {
        if (header.isNullOrBlank()) return
        val host = url.host.lowercase(Locale.US)
        val imported = header.split(';').mapNotNull { raw ->
            val pair = raw.trim()
            val separator = pair.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            val name = pair.substring(0, separator).trim()
            val value = pair.substring(separator + 1).trim()
            if (name.isEmpty()) return@mapNotNull null
            runCatching {
                Cookie.Builder()
                    .name(name)
                    .value(value)
                    .hostOnlyDomain(host)
                    .path("/")
                    .expiresAt(Long.MAX_VALUE)
                    .build()
            }.getOrNull()
        }
        if (imported.isEmpty()) return
        synchronized(byHost) {
            val stored = byHost.getOrPut(host) { mutableListOf() }
            imported.forEach { cookie ->
                stored.removeAll { existing ->
                    existing.name == cookie.name && existing.path == cookie.path
                }
                stored.add(cookie)
            }
            if (stored.size > MAX_COOKIES_PER_HOST) {
                byHost[host] = stored.takeLast(MAX_COOKIES_PER_HOST).toMutableList()
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host.lowercase(Locale.US)
        val now = System.currentTimeMillis()
        synchronized(byHost) {
            val stored = byHost[host] ?: return emptyList()
            stored.removeAll { it.expiresAt < now }
            if (stored.isEmpty()) {
                byHost.remove(host)
                return emptyList()
            }
            // Cookies are per host in this jar, but still honours the path they were set
            // on, so a cookie set for /apk/ is not sent to an unrelated path.
            return stored.filter { it.matches(url) }
        }
    }

    /** Drops everything; used by tests and when a challenge means the session is dead. */
    fun clear() {
        synchronized(byHost) { byHost.clear() }
    }

    /** How many hosts currently hold cookies, for diagnostics. */
    val hostCount: Int
        get() = synchronized(byHost) { byHost.size }
}

