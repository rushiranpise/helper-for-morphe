package dev.rushi.apkdownloadhelper

import java.util.Locale
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

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
