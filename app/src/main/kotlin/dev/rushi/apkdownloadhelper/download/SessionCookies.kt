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
        val now = System.currentTimeMillis()
        synchronized(byHost) {
            cookies.forEach { cookie ->
                val key = cookie.domain.lowercase(Locale.US)
                val list = byHost.getOrPut(key) { mutableListOf() }
                list.removeAll { it.expiresAt < now }
                list.removeAll { existing ->
                    existing.name == cookie.name && existing.path == cookie.path
                }
                list.add(cookie)
                if (list.size > MAX_COOKIES_PER_HOST) {
                    byHost[key] = list.takeLast(MAX_COOKIES_PER_HOST).toMutableList()
                }
            }
        }
        runCatching {
            val cm = android.webkit.CookieManager.getInstance() ?: return@runCatching
            cookies.forEach { cookie ->
                cm.setCookie(url.toString(), cookie.toString())
            }
            cm.flush()
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
        val imported = parseCookieHeader(url, header, rootDomain(host))
        if (imported.isEmpty()) return
        synchronized(byHost) {
            val keys = candidateDomainKeys(host)
            imported.forEach { cookie ->
                // A Cookie request header carries no attributes. If the value is
                // unchanged, keep the structured cookie and its Secure/expiry
                // metadata instead of replacing it with a reconstructed one.
                val unchanged = keys.asSequence()
                    .mapNotNull { byHost[it] }
                    .flatten()
                    .any { existing ->
                        existing.name == cookie.name &&
                            existing.path == cookie.path &&
                            existing.value == cookie.value
                    }
                if (unchanged) return@forEach
                keys.forEach { key ->
                    byHost[key]?.removeAll { existing ->
                        existing.name == cookie.name && existing.path == cookie.path
                    }
                }
                val targetKey = cookie.domain.lowercase(Locale.US)
                val stored = byHost.getOrPut(targetKey) { mutableListOf() }
                stored.removeAll { existing ->
                    existing.name == cookie.name && existing.path == cookie.path
                }
                stored.add(cookie)
                if (stored.size > MAX_COOKIES_PER_HOST) {
                    byHost[targetKey] = stored.takeLast(MAX_COOKIES_PER_HOST).toMutableList()
                }
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        // The browser's Cookie header has no attributes, so using it to replace
        // the jar's cookies would drop their Secure, path and expiry metadata.
        // Keep it request-scoped instead; CookieManager has already matched it
        // to this URL, and structured cookies fill in the rest.
        val browserCookies = runCatching {
            val cm = android.webkit.CookieManager.getInstance()
            cm?.getCookie(url.toString())
        }.getOrNull()?.let { header ->
            parseCookieHeader(url, header, rootDomain(url.host))
        }.orEmpty()

        val keys = candidateDomainKeys(url.host)
        val now = System.currentTimeMillis()
        val cookiesByName = LinkedHashMap<String, Cookie>()
        browserCookies.forEach { cookie -> cookiesByName[cookie.name] = cookie }
        synchronized(byHost) {
            keys.forEach { key ->
                val stored = byHost[key] ?: return@forEach
                stored.removeAll { it.expiresAt < now }
                if (stored.isEmpty()) {
                    byHost.remove(key)
                } else {
                    stored.filter { it.matches(url) }.forEach { cookie ->
                        cookiesByName.putIfAbsent(cookie.name, cookie)
                    }
                }
            }
        }
        return cookiesByName.values.toList()
    }

    /**
     * Parses a `Cookie` request header into session cookies for [url].
     * The header has no path or expiration, so [domain] is supplied by the caller.
     */
    private fun parseCookieHeader(url: HttpUrl, header: String?, domain: String): List<Cookie> {
        if (header.isNullOrBlank()) return emptyList()
        val host = url.host.lowercase(Locale.US)
        return header.split(';').mapNotNull { raw ->
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
                    .domain(domain)
                    .path("/")
                    .build()
            }.getOrElse {
                runCatching {
                    Cookie.Builder()
                        .name(name)
                        .value(value)
                        .hostOnlyDomain(host)
                        .path("/")
                        .build()
                }.getOrNull()
            }
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

internal fun rootDomain(host: String): String {
    val parts = host.lowercase(Locale.US).split('.').filter { it.isNotBlank() }
    if (parts.size <= 2) return host
    return parts.takeLast(2).joinToString(".")
}

internal fun candidateDomainKeys(host: String): List<String> {
    val parts = host.lowercase(Locale.US).split('.').filter { it.isNotBlank() }
    if (parts.size <= 1) return listOf(host.lowercase(Locale.US))
    val keys = mutableListOf<String>()
    for (i in 0 until parts.size - 1) {
        keys.add(parts.subList(i, parts.size).joinToString("."))
    }
    return keys
}

