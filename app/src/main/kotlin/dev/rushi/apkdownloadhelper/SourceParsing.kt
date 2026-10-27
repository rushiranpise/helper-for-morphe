package dev.rushi.apkdownloadhelper

import android.content.Context
import dev.rushi.apkdownloadhelper.play.PlayHttpClient
import java.util.Locale
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/** Abstraction over network text fetching so parsers can be unit-tested with fixtures. */
internal fun interface SourceTextFetcher {
    fun fetchText(url: String, referer: String?): String
}

/**
 * Thrown when a source rate-limits us (HTTP 429), so multi-page walks can abort
 * instead of hammering the site further. The message keeps the "HTTP 429" token
 * that [sourceFailureMessage] maps to a friendly rate-limit explanation.
 */
internal class HttpRateLimitedException(message: String) : Exception(message)

/**
 * Thrown when a source answers a probe with a Cloudflare legal-interstitial /
 * challenge page instead of the real content. Callers surface it as a distinct
 * "blocked " state rather than silently reading the page as "no version found".
 */
internal class SourceChallengeException(message: String) : Exception(message)

/**
 * Real-network fetcher shared by all parsers. Enforces a minimum gap between
 * requests so source resolution never bursts (the main cause of site rate
 * limits), and surfaces HTTP 429 as [HttpRateLimitedException].
 */
internal class OkHttpSourceTextFetcher(
    private val client: OkHttpClient,
    private val minRequestGapMillis: Long = 800L,
    /**
     * Per-host minimum gaps, overriding [minRequestGapMillis] for sites that
     * are stricter than the app's general default. APKMirror declares
     * `Crawl-delay: 3` in its robots.txt and answers 429 to denser runs, so it
     * must stay below ~1 request every 2-3s instead of the flat 800ms the
     * other sources tolerate.
     */
    private val hostGapsMillis: Map<String, Long> = emptyMap()
) : SourceTextFetcher {
    private val paceGate = Any()
    private var nextAllowedAtNanos = 0L

    override fun fetchText(url: String, referer: String?): String {
        pace(gapMillisFor(url))
        val builder = Request.Builder().url(url)
        referer?.let { builder.header("Referer", it) }
        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                if (response.code == 429) {
                    throw HttpRateLimitedException("HTTP 429 Too Many Requests for $url")
                }
                throw IllegalStateException("HTTP ${response.code} for $url")
            }
            return response.body.string()
        }
    }

    private fun gapMillisFor(url: String): Long {
        val host = runCatching { java.net.URI(url).host }?.getOrNull() ?: return minRequestGapMillis
        return hostGapsMillis[host.lowercase(Locale.US)] ?: minRequestGapMillis
    }

    /** Enforce a minimum gap between request starts so fetches never burst. */
    private fun pace(gapMillis: Long) {
        synchronized(paceGate) {
            val now = System.nanoTime()
            val waitMillis = (nextAllowedAtNanos - now) / 1_000_000
            if (waitMillis > 0) {
                try {
                    Thread.sleep(waitMillis)
                } catch (ignored: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
            nextAllowedAtNanos = System.nanoTime() + gapMillis * 1_000_000
        }
    }
}

/**
 * Everything a per-source parser needs. `apkPureApi`/`aptoideApi` are non-null
 * so parsers can call them directly; `playHttpClient`/`appContext` are only
 * needed by the Aurora parser and are nullable for fixture-based tests.
 */
internal class SourceParserContext(
    val fetcher: SourceTextFetcher,
    val apkPureApi: ApkPureApi,
    val aptoideApi: AptoideApi,
    val playHttpClient: PlayHttpClient? = null,
    val appContext: Context? = null
) {
    fun document(url: String, referer: String? = null): Document =
        Jsoup.parse(fetcher.fetchText(url, referer), url)

    fun text(url: String, referer: String? = null): String = fetcher.fetchText(url, referer)
}

internal fun String.archDisplayLabel(): String =
    if (isUniversalArchLabel()) {
        "Universal"
    } else {
        trim()
    }

/**
 * Rank of an architecture label that names only ABIs this device cannot run.
 *
 * Lower ranks are better: an index into the device's own ABI preference comes first,
 * then [ARCH_RANK_UNIVERSAL], then this.
 */
internal const val ARCH_RANK_MISMATCH = 100

/** Rank of a label that does not pick an architecture at all. */
internal const val ARCH_RANK_UNIVERSAL = 50

/**
 * How well a source's architecture label fits this device.
 *
 * A source that publishes `armeabi-v7a` and `arm64-v8a` builds of one version must
 * not hand an arm64 phone the 32-bit one, so the caller ranks candidates with this
 * instead of taking whichever row came first.
 *
 * Returns the ABI's position in [availableAbis] when the label names one the device
 * runs, [ARCH_RANK_UNIVERSAL] when it names none in particular, and
 * [ARCH_RANK_MISMATCH] when it names only others.
 */
internal fun archMatchRank(label: String?, availableAbis: List<String>): Int {
    val normalized = label?.trim()?.lowercase(Locale.US).orEmpty()
    if (normalized.isEmpty() || normalized.isUniversalArchLabel()) return ARCH_RANK_UNIVERSAL
    val index = availableAbis
        .map { it.trim().lowercase(Locale.US) }
        .indexOfFirst { abi -> abi.isNotBlank() && normalized.namesAbi(abi) }
    return if (index >= 0) index else ARCH_RANK_MISMATCH
}

/**
 * Whether a free-text architecture cell mentions [abi].
 *
 * Sources spell these loosely (`arm64`, `v8a`, `+` joined lists), so the aliases
 * matter more than an exact match.
 */
private fun String.namesAbi(abi: String): Boolean {
    if (contains(abi)) return true
    return when (abi) {
        "arm64-v8a" -> contains("arm64") || contains("v8a")
        "armeabi-v7a" -> contains("armeabi-v7a") || contains("v7a") || contains("armv7")
        // Plain `armeabi` must not swallow the v7a wording above it.
        "armeabi" -> contains("armeabi") && !contains("v7a")
        "x86_64" -> contains("x86_64") || contains("x86-64")
        "x86" -> !contains("x86_64") && !contains("x86-64") && contains("x86")
        else -> false
    }
}

internal fun String.isUniversalArchLabel(): Boolean {
    val normalizedLabel = lowercase(Locale.US)
    return "all architectures" in normalizedLabel ||
        "universal" in normalizedLabel ||
        normalizedLabel == "all" ||
        normalizedLabel == "noarch"
}

/**
 * Thrown by a source parser when the app is genuinely absent from the source
 * (e.g. the search page returns no matching listing), as opposed to a transient
 * resolution failure. Callers render this as "not found" instead of an error
 * with a fallback candidate.
 */
internal class SourceAppNotFoundException(packageName: String) :
    Exception("No listing found on source for $packageName")

/** Common contract every APK source parser implements. */
internal interface ApkSourceParser {
    val source: DownloadSource

    suspend fun findCandidates(
        request: HelperRequest,
        option: CandidateOption
    ): List<DownloadCandidate>

    /** Web search/info URL for manual mode; null when the source has no web page. */
    fun searchUrl(packageName: String): String? = null

    /** Web-page fallback shown when no direct candidate resolves for the requested version. */
    fun requestedFallbackCandidate(request: HelperRequest): DownloadCandidate? = null

    /** Web-page fallback shown when no direct candidate resolves for the latest version. */
    fun latestFallbackCandidate(request: HelperRequest): DownloadCandidate? = null

    suspend fun resolveHistory(request: HelperRequest): List<DownloadCandidate> = emptyList()

    suspend fun resolveHistoryCandidate(
        request: HelperRequest,
        candidate: DownloadCandidate
    ): DownloadCandidate? = null
}
