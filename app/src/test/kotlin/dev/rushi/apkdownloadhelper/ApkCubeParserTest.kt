package dev.rushi.apkdownloadhelper

import dev.rushi.apkdownloadhelper.sources.*
import dev.rushi.apkdownloadhelper.ui.model.*

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ApkCubeParserTest {

    private val packageName = "com.paget96.batteryguru"

    // Mirrors the Next.js flight payload apkcube serves: escaped quotes live
    // inside the script content verbatim.
    private fun downloaderPage(): String = """
        <!DOCTYPE html><html><head><title>APK Downloader</title></head><body>
        <script>self.__next_f.push([1,"39:[\"R\",\"RL38\",null,{\"as\":\"p\",\"className\":\"truncate font-mono\",\"children\":[\"2.5.0.6\",\" · \",\"\",\"Paget96\"]}]"])</script>
        <script>self.__next_f.push([1,"3a:[\"R\",\"RL3\",null,{\"href\":\"/battery-guru-battery-health/com.paget96.batteryguru/download\",\"className\":\"mt-4 flex\"}]"])</script>
        </body></html>
    """.trimIndent()

    private fun downloadPage(): String = """
        <!DOCTYPE html><html><head>
        <title>Download Battery Guru 2.5.0.6 APK for Android - APKCube</title>
        </head></html>
    """.trimIndent()

    private fun oldVersionsPage(): String = """
        <!DOCTYPE html><html><head><title>Old versions</title></head><body>
        <script>self.__next_f.push([1,"39:[\"R\",\"RL38\",null,{\"versionName\":\"2.5.0.6\",\"versionCode\":2506,\"arch\":\"arm64-v8a\"}]"])</script>
        <script>self.__next_f.push([1,"3a:[\"R\",\"RL38\",null,{\"versionName\":\"2.5.0.5\",\"versionCode\":2505,\"arch\":\"armeabi-v7a\"}]"])</script>
        <a href="/battery-guru-battery-health/com.paget96.batteryguru/download?version=2.5.0.6">2.5.0.6</a>
        <a href="/battery-guru-battery-health/com.paget96.batteryguru/download?version=2.5.0.5">2.5.0.5</a>
        </body></html>
    """.trimIndent()

    private fun parser(pages: Map<String, String>) =
        ApkCubeParser(testParserContext(pages))

    private fun parserWithFetcher(fetcher: SourceTextFetcher) =
        ApkCubeParser(
            SourceParserContext(
                fetcher = fetcher,
                apkPureApi = FakeApkPureApi(),
                aptoideApi = FakeAptoideApi()
            )
        )

    private val downloaderUrl = "https://apkcube.com/apk-downloader?url=$packageName"
    private val downloadUrl = "https://apkcube.com/battery-guru-battery-health/$packageName/download"
    private val oldVersionsUrl = "https://apkcube.com/battery-guru-battery-health/$packageName/old-versions"

    private fun latestRequest() = testRequest(
        packageName = packageName,
        appName = "Battery Guru",
        versionName = null
    )

    private fun requestedRequest(version: String) = testRequest(
        packageName = packageName,
        appName = "Battery Guru",
        versionName = version
    )

    @Test
    fun apkcube_usesTheSameCatalogue() {
        val result = runBlocking {
            ApkCubeParser(
                testParserContext(
                    mapOf(
                        downloaderUrl to downloaderPage(),
                        downloadUrl to downloadPage()
                    )
                )
            ).findCandidates(latestRequest(), CandidateOption.LATEST)
        }
        assertEquals(1, result.size)
        assertEquals(DownloadSource.APK_CUBE, result.first().source)
        assertEquals("2.5.0.6", result.first().versionName)
    }

    @Test
    fun latest_readsVersionFromTheDownloadPageTitle() {
        // The live downloader page can name the app without carrying a version;
        // the download page title still has it.
        val downloaderWithoutVersion = """
            <script>self.__next_f.push([1,"3a:[\"R\",\"RL3\",null,{\"href\":\"/battery-guru-battery-health/com.paget96.batteryguru/download\"}]"])</script>
        """.trimIndent()
        val downloadPage = """
            <!DOCTYPE html><html><head>
            <title>Download Battery Guru 2.5.0.6 APK for Android - APKCube</title>
            <script>self.__next_f.push([1,"39:[\"R\",\"RL38\",null,{\"playVersionCode\":2506}]"])</script>
            </head></html>
        """.trimIndent()
        val result = runBlocking {
            parser(
                mapOf(
                    downloaderUrl to downloaderWithoutVersion,
                    downloadUrl to downloadPage
                )
            ).findCandidates(latestRequest(), CandidateOption.LATEST)
        }
        assertEquals(1, result.size)
        assertEquals("2.5.0.6", result.first().versionName)
        assertEquals(2506L, result.first().versionCode)
        assertEquals(
            "$downloadUrl?target=version&vc=2506&arch=arm64-v8a&langs=standard&dpi=all",
            result.first().url
        )
    }

    @Test
    fun latest_findsVersionAndDownloadPage() {
        val result = runBlocking {
            parser(
                mapOf(
                    downloaderUrl to downloaderPage(),
                    downloadUrl to downloadPage()
                )
            ).findCandidates(latestRequest(), CandidateOption.LATEST)
        }
        assertEquals(1, result.size)
        val candidate = result.first()
        assertEquals("2.5.0.6", candidate.versionName)
        assertEquals(
            "https://apkcube.com/battery-guru-battery-health/$packageName/download?arch=arm64-v8a&langs=standard&dpi=all",
            candidate.url
        )
        assertFalse(candidate.directDownload)
        assertTrue(candidate.note.orEmpty().contains("captcha", ignoreCase = true))
        assertEquals(candidate.url, candidate.captchaUrl)
        assertEquals(CandidateOption.LATEST, candidate.option)
    }

    @Test
    fun latest_skipsNavLabelsBeforeVersion() {
        // The real page has `children` arrays for nav labels ("apk-downloader")
        // before the app-version row; the version must still win.
        val page = """
            <script>self.__next_f.push([1,"39:[\"R\",\"RL38\",null,{\"children\":[\"apk-downloader\"]}]"])</script>
            <script>self.__next_f.push([1,"3a:[\"R\",\"RL3\",null,{\"children\":[\"2.5.0.6\",\" · \",\"\",\"Paget96\"]}]"])</script>
            <script>self.__next_f.push([1,"3a:[\"R\",\"RL3\",null,{\"href\":\"/battery-guru-battery-health/com.paget96.batteryguru/download\"}]"])</script>
        """.trimIndent()
        val result = runBlocking {
            parser(
                mapOf(
                    downloaderUrl to page,
                    downloadUrl to downloadPage()
                )
            ).findCandidates(latestRequest(), CandidateOption.LATEST)
        }
        assertEquals(1, result.size)
        assertEquals("2.5.0.6", result.first().versionName)
    }

    @Test
    fun requestedFallbackTargetsTheBuildAndAbi() {
        val request = testRequest(
            packageName = packageName,
            appName = "Battery Guru",
            versionName = "2.5.0.6",
            versionCode = 2506L
        )

        val candidate = parser(emptyMap()).requestedFallbackCandidate(request)

        assertEquals(
            "$downloaderUrl&target=version&vc=2506&arch=arm64-v8a&langs=standard&dpi=all",
            candidate?.url
        )
        assertEquals(candidate?.url, candidate?.captchaUrl)
        assertEquals(CandidateOption.REQUESTED, candidate?.option)
        assertTrue(candidate?.syntheticFallback ?: false)
    }

    @Test
    fun latestFallbackKeepsTheAbiAndLanguageParameters() {
        val candidate = parser(emptyMap()).latestFallbackCandidate(latestRequest())

        assertEquals(
            "$downloaderUrl&arch=arm64-v8a&langs=standard&dpi=all",
            candidate?.url
        )
        assertEquals(CandidateOption.LATEST, candidate?.option)
    }

    @Test
    fun requested_matchesListedVersion() {
        val result = runBlocking {
            parser(
                mapOf(
                    downloaderUrl to downloaderPage(),
                    oldVersionsUrl to oldVersionsPage()
                )
            ).findCandidates(requestedRequest("2.5.0.6"), CandidateOption.REQUESTED)
        }
        assertEquals(1, result.size)
        val candidate = result.first()
        assertEquals("2.5.0.6", candidate.versionName)
        assertEquals(2506L, candidate.versionCode)
        assertEquals(
            "https://apkcube.com/battery-guru-battery-health/$packageName/download?version=2.5.0.6&target=version&vc=2506&arch=arm64-v8a&langs=standard&dpi=all",
            candidate.url
        )
        assertEquals(VersionStatus.REQUESTED, candidate.versionStatus)
        assertFalse(candidate.directDownload)
        assertEquals(candidate.url, candidate.captchaUrl)
    }

    @Test
    fun requested_explicitBuildCodeWins() {
        val request = testRequest(
            packageName = packageName,
            appName = "Battery Guru",
            versionName = "2.5.0.6",
            versionCode = 999L
        )
        val result = runBlocking {
            parser(
                mapOf(
                    downloaderUrl to downloaderPage(),
                    oldVersionsUrl to oldVersionsPage()
                )
            ).findCandidates(request, CandidateOption.REQUESTED)
        }
        assertEquals(1, result.size)
        assertEquals(999L, result.first().versionCode)
        assertTrue(
            result.first().url.endsWith(
                "target=version&vc=999&arch=arm64-v8a&langs=standard&dpi=all"
            )
        )
    }

    @Test
    fun requested_targetsAnArmv7Device() {
        val request = testRequest(
            packageName = packageName,
            appName = "Battery Guru",
            versionName = "2.5.0.6"
        ).copy(supportedAbis = listOf("armeabi-v7a"))
        val result = runBlocking {
            parser(
                mapOf(
                    downloaderUrl to downloaderPage(),
                    oldVersionsUrl to oldVersionsPage()
                )
            ).findCandidates(request, CandidateOption.REQUESTED)
        }
        assertEquals(1, result.size)
        assertTrue(
            result.first().url.endsWith(
                "target=version&vc=2506&arch=armeabi-v7a&langs=standard&dpi=all"
            )
        )
    }

    @Test
    fun requested_fallsBackToUniversalForNonArmDevices() {
        val request = testRequest(
            packageName = packageName,
            appName = "Battery Guru",
            versionName = "2.5.0.6"
        ).copy(supportedAbis = listOf("x86_64"))
        val result = runBlocking {
            parser(
                mapOf(
                    downloaderUrl to downloaderPage(),
                    oldVersionsUrl to oldVersionsPage()
                )
            ).findCandidates(request, CandidateOption.REQUESTED)
        }
        assertEquals(1, result.size)
        assertTrue(
            result.first().url.endsWith(
                "target=version&vc=2506&arch=universal&langs=standard&dpi=all"
            )
        )
    }

    @Test
    fun requested_notListed_returnsEmpty() {
        val result = runBlocking {
            parser(
                mapOf(
                    downloaderUrl to downloaderPage(),
                    oldVersionsUrl to oldVersionsPage()
                )
            ).findCandidates(requestedRequest("9.9.9"), CandidateOption.REQUESTED)
        }
        assertTrue(result.isEmpty())
    }

    @Test
    fun latest_noMatchPage_throwsNotFound() {
        val html = "<div hidden><p>No match</p>" +
            "<p>We don't hold that package</p>" +
            "<p>Package isn't in the catalog.</p></div>"
        val error = runCatching {
            runBlocking {
                parser(mapOf(downloaderUrl to html))
                    .findCandidates(latestRequest(), CandidateOption.LATEST)
            }
        }.exceptionOrNull()
        assertTrue("expected SourceAppNotFoundException, got $error", error is SourceAppNotFoundException)
    }

    @Test
    fun explicit404_throwsNotFound() {
        val error = runCatching {
            runBlocking {
                parserWithFetcher(SourceTextFetcher { _, _ ->
                    throw HttpNotFoundException(downloaderUrl)
                }).findCandidates(latestRequest(), CandidateOption.LATEST)
            }
        }.exceptionOrNull()
        assertTrue("expected SourceAppNotFoundException, got $error", error is SourceAppNotFoundException)
    }

    @Test
    fun transientFailuresAreRethrown() {
        val failures = listOf(
            IOException("network failed"),
            IllegalStateException("parse failed"),
            CancellationException("cancelled")
        )
        failures.forEach { failure ->
            val error = runCatching {
                runBlocking {
                    parserWithFetcher(SourceTextFetcher { _, _ -> throw failure })
                        .findCandidates(latestRequest(), CandidateOption.LATEST)
                }
            }.exceptionOrNull()
            assertSame(failure, error)
        }
    }

    @Test
    fun noMatchPageIsDefinitive() {
        val html = "<div hidden><p>No match</p>" +
            "<h2>We don&#x27;t hold that package</h2>" +
            "<p>Package isn&#x27;t in the catalog.</p></div>"
        assertTrue(apkCubeHtmlNoMatch(html))
    }

    @Test
    fun resultPageIsNotSkipped() {
        val html = "<p>Your file</p><a href='/download?version=1.2.3'>Download</a>"
        assertFalse(apkCubeHtmlNoMatch(html))
    }

    @Test
    fun verificationPageIsNotSkipped() {
        val html = "<p>No match</p><div>Verify to download</div>"
        assertFalse(apkCubeHtmlNoMatch(html))
    }

    @Test
    fun emptyPageIsNotSkipped() {
        assertFalse(apkCubeHtmlNoMatch(null))
        assertFalse(apkCubeHtmlNoMatch(""))
    }

}
