package dev.rushi.apkdownloadhelper.sources

import dev.rushi.apkdownloadhelper.DOWNLOAD_FILE_KIND_ORDER
import dev.rushi.apkdownloadhelper.ui.DownloadSource
import dev.rushi.apkdownloadhelper.ui.HelperRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Starts the metadata work that the next likely sources need while Fast Mode is still
 * checking an earlier source. Every result is optional: if a probe is not ready when its
 * source's turn arrives, it is cancelled and the parser fetches normally.
 */
internal class SourcePrefetch(
    private val fetcher: SourceTextFetcher,
    private val apkPureApi: ApkPureApi,
    private val uptodownApi: UptodownApi?,
    private val isSourceEnabled: (DownloadSource) -> Boolean = { true },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val texts = ConcurrentHashMap<String, Deferred<String>>()
    private var requestKey: String? = null
    private var apkPureHistory: Deferred<ApkPureHistoryResponse>? = null
    private var uptodownPrepared: Deferred<UptodownPrepared>? = null
    private var apkCubeNoMatch: Deferred<Boolean>? = null
    private var apkCubeChecked = false

    fun start(request: HelperRequest, preflightApkCube: Boolean = false) {
        cancel()
        requestKey = key(request)
        apkCubeChecked = false

        if (isSourceEnabled(DownloadSource.APK_MIRROR)) {
            val url = apkMirrorSearchUrl(request.packageName)
            texts[url] = scope.async { fetcher.fetchText(url, null) }
        }
        if (isSourceEnabled(DownloadSource.APK_PURE) && request.hasRequestedVersionRequest) {
            apkPureHistory = scope.async {
                apkPureApi.getAppHistoryVersions(
                    packageName = request.packageName,
                    header = apkPurePrefetchHeader()
                )
            }
        }
        if (isSourceEnabled(DownloadSource.UPTODOWN) && uptodownApi != null) {
            val version = request.requestedVersionNames.firstOrNull()
            val fileType = DOWNLOAD_FILE_KIND_ORDER
                .firstOrNull { it in request.requestedFileKinds }
                ?: "apk"
            uptodownPrepared = scope.async {
                uptodownApi.prepare(request.packageName, version, fileType)
            }
        }
        // Start this after the metadata prefetches so the APKCube probe does
        // not take the fetcher's pacing slot from the sources checked first.
        if (preflightApkCube && isSourceEnabled(DownloadSource.APK_CUBE)) {
            val url = apkCubePreflightUrl(request)
            apkCubeNoMatch = scope.async {
                apkCubeHtmlNoMatch(fetcher.fetchText(url, null))
            }
        }
    }

    /** A completed prefetched search page, or null so the parser fetches it itself. */
    fun takeText(url: String, referer: String? = null): String? {
        if (referer != null) return null
        val deferred = texts.remove(url) ?: return null
        if (!deferred.isCompleted) {
            deferred.cancel()
            return null
        }
        return runCatching { deferred.getCompleted() }.getOrNull()
    }

    fun takeApkPureHistory(request: HelperRequest): ApkPureHistoryResponse? {
        if (requestKey != key(request)) return null
        val deferred = apkPureHistory ?: return null
        apkPureHistory = null
        if (!deferred.isCompleted) {
            deferred.cancel()
            return null
        }
        return runCatching { deferred.getCompleted() }.getOrNull()
    }

    fun takeUptodown(request: HelperRequest): UptodownPrepared? {
        if (requestKey != key(request)) return null
        val deferred = uptodownPrepared ?: return null
        uptodownPrepared = null
        if (!deferred.isCompleted) {
            deferred.cancel()
            return null
        }
        return runCatching { deferred.getCompleted() }.getOrNull()
    }

    /**
     * True only when APKCube's page definitively has no matching app. If no
     * background probe was started, check the page now. An unfinished probe is
     * treated as uncertain so the normal WebView can open without waiting.
     */
    suspend fun takeApkCubeNoMatch(request: HelperRequest): Boolean {
        if (requestKey != key(request) || apkCubeChecked) return false
        val deferred = apkCubeNoMatch
        apkCubeNoMatch = null
        apkCubeChecked = true
        if (deferred != null) {
            if (!deferred.isCompleted) {
                deferred.cancel()
                return false
            }
            return runCatching { deferred.getCompleted() }.getOrDefault(false)
        }
        return withContext(Dispatchers.IO) {
            runCatching {
                apkCubeHtmlNoMatch(
                    fetcher.fetchText(apkCubePreflightUrl(request), null)
                )
            }.getOrDefault(false)
        }
    }

    fun cancel() {
        apkPureHistory?.cancel()
        uptodownPrepared?.cancel()
        apkCubeNoMatch?.cancel()
        apkPureHistory = null
        uptodownPrepared = null
        apkCubeNoMatch = null
        apkCubeChecked = false
        texts.values.forEach { it.cancel() }
        texts.clear()
        requestKey = null
    }

    private fun apkPurePrefetchHeader(): String =
        """{"device_info":{"os_ver":"34"}}"""

    private fun key(request: HelperRequest): String = buildString {
        append(request.packageName)
        append('|')
        append(request.requestedVersionNames.joinToString(","))
        append('|')
        append(request.requestedVersionCodes.sorted().joinToString(","))
        append('|')
        append(request.requestedFileKinds.sorted().joinToString(","))
    }
}
