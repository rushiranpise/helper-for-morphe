package dev.rushi.apkdownloadhelper.download

import android.util.Log
import dev.rushi.apkdownloadhelper.TAG
import dev.rushi.apkdownloadhelper.download.AdGuardDns.isBlocked
import dev.rushi.apkdownloadhelper.download.AdGuardDns.lookup
import dev.rushi.apkdownloadhelper.storage.adGuardDnsEnabled
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * OkHttp [Dns] that routes lookups through AdGuard DNS
 * (https://dns.adguard.com) when the user has the setting enabled.
 *
 * AdGuard answers filtered hosts (ads, trackers, malware) with `0.0.0.0`.
 *
 * - Downloads ([lookup]) treat that answer as a failure and fall back to the
 *   system resolver, so a filtered domain can never kill a download.
 * - The in-app browser ([isBlocked]) treats it as a block signal, so ad and
 *   tracker subresources are dropped before they load.
 * - Any failure reaching the DoH endpoint also falls back to system DNS /
 *   no blocking, per the "fallback to default when it fails" rule.
 *
 * The enabled flag is a volatile mirror kept in sync by
 * [SettingsRepository], so every OkHttp client can be built once with
 * `dns(AdGuardDns)` and toggling the setting takes effect on the next lookup
 * without rebuilding the clients. The DoH query itself runs on a separate
 * plain client so it can never recurse back into this resolver.
 */
internal object AdGuardDns : Dns {

    private val dohClient by lazy { OkHttpClient() }

    private val doh by lazy {
        DnsOverHttps.Builder()
            .client(dohClient)
            .url("https://dns.adguard.com/dns-query".toHttpUrl())
            .build()
    }

    /** Last known blocked/not-blocked state per host, for the in-app browser. */
    private val blockedCache = ConcurrentHashMap<String, Boolean>()

    // [isBlocked] runs on the WebView's request thread inside
    // shouldInterceptRequest, so a slow resolver must never stall page loads:
    // the lookup runs here and is abandoned after [BLOCK_CHECK_TIMEOUT_S].
    // "Abandoned" reads as not-blocked, so browsing keeps working.
    private val blockCheckExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "adguard-block-check").apply { isDaemon = true }
    }

    private fun isBlockedAnswer(addresses: List<InetAddress>): Boolean =
        addresses.isNotEmpty() && addresses.all { it.isAnyLocalAddress }

    override fun lookup(hostname: String): List<InetAddress> {
        if (!adGuardDnsEnabled) return Dns.SYSTEM.lookup(hostname)
        val addresses = try {
            doh.lookup(hostname)
        } catch (t: Throwable) {
            Log.w(TAG, "AdGuard DNS lookup failed for $hostname, falling back to system DNS", t)
            return Dns.SYSTEM.lookup(hostname)
        }
        if (isBlockedAnswer(addresses)) {
            Log.w(TAG, "AdGuard DNS filtered $hostname, falling back to system DNS for the download")
            return Dns.SYSTEM.lookup(hostname)
        }
        return addresses
    }

    /**
     * True when the in-app WebView should drop requests to [hostname] because
     * AdGuard DNS filters it. Never throws: any failure to reach the resolver
     * means "not blocked" so browsing keeps working.
     */
    fun isBlocked(hostname: String): Boolean {
        if (!adGuardDnsEnabled) return false
        blockedCache[hostname]?.let { return it }
        val blocked = try {
            blockCheckExecutor.submit(
                Callable { runCatching { isBlockedAnswer(doh.lookup(hostname)) }.getOrDefault(false) }
            ).get(BLOCK_CHECK_TIMEOUT_S, TimeUnit.SECONDS)
        } catch (e: Exception) {
            // Timeout or resolver failure: don't block, don't stall the page.
            false
        }
        if (blockedCache.size >= 1024) blockedCache.clear()
        blockedCache[hostname] = blocked
        return blocked
    }

    private const val BLOCK_CHECK_TIMEOUT_S = 3L
}

