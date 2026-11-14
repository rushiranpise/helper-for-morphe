package dev.rushi.apkdownloadhelper

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MorpheLinkParserTest {

    private val packageName = "com.example.app"

    @Test
    fun `resolved hosts map to the parser that can continue from them`() {
        assertEquals(
            DownloadSource.APK_MIRROR,
            morpheLinkSourceOf("https://www.apkmirror.com/apk/example/example-app/1-0-release/")
        )
        assertEquals(
            DownloadSource.UPTODOWN,
            morpheLinkSourceOf("https://example-app.en.uptodown.com/android/download/123-x")
        )
        assertEquals(
            DownloadSource.APK_PURE,
            morpheLinkSourceOf("https://apkpure.com/example/com.example.app/download/2.0.0")
        )
        assertEquals(
            DownloadSource.APTOIDE,
            morpheLinkSourceOf("https://en.aptoide.com/app/example/com.example.app")
        )
        assertNull(morpheLinkSourceOf("https://www.apkmirror.com.evil.example/apk/x/y/1-0-release/"))
        assertNull(morpheLinkSourceOf("https://example.com/app"))
    }

    @Test
    fun `a direct apk link becomes a direct candidate`() = runBlocking {
        val directUrl = "https://cdn.example.com/Example_2.0.0.apk"
        val parser = MorpheLinkParser(
            testParserContext(pages = emptyMap()),
            resolveLink = { directUrl }
        )
        val request = testRequest(
            packageName = packageName,
            versionName = "2.0.0",
            versionCode = 200L
        )

        val candidate = parser.findCandidates(request, CandidateOption.REQUESTED).single()

        assertEquals(DownloadSource.MORPHE_LINK, candidate.source)
        assertEquals(directUrl, candidate.url)
        assertTrue(candidate.directDownload)
        assertEquals("apk", candidate.fileKind)
        assertEquals(200L, candidate.versionCode)
        assertEquals("Example_2.0.0.apk", candidate.files.single().fileName)
    }

    @Test
    fun `extensionless source endpoints are still direct files`() = runBlocking {
        val uptodown = "https://dw.uptodown.com/dwn/token"
        val parser = MorpheLinkParser(
            testParserContext(pages = emptyMap()),
            resolveLink = { uptodown }
        )

        val candidate = parser.findCandidates(
            testRequest(packageName = packageName),
            CandidateOption.LATEST
        ).single()

        assertTrue(candidate.directDownload)
        assertEquals("apk", candidate.fileKind)
        assertEquals(uptodown, candidate.url)
    }

    @Test
    fun `an APKPure bundle endpoint keeps its archive kind`() = runBlocking {
        val directUrl = "https://d.apkpure.com/b/XAPK/com.example.app?versionCode=200"
        val parser = MorpheLinkParser(
            testParserContext(pages = emptyMap()),
            resolveLink = { directUrl }
        )

        val candidate = parser.findCandidates(
            testRequest(packageName = packageName, requestedFileType = "XAPK", allowSplitArchive = true),
            CandidateOption.LATEST
        ).single()

        assertTrue(candidate.directDownload)
        assertEquals("xapk", candidate.fileKind)
    }

    @Test
    fun `a known page is handed to its own parser and keeps the Morphe identity`() = runBlocking {
        val resolved = "https://www.apkmirror.com/apk/example/example-app/1-0-release/"
        val underlyingCandidate = DownloadCandidate(
            source = DownloadSource.APK_MIRROR,
            name = "Example App",
            packageName = packageName,
            versionName = "2.0.0",
            versionCode = 200L,
            url = "https://www.apkmirror.com/download.php?id=200",
            fileKind = "apk",
            option = CandidateOption.REQUESTED,
            directDownload = true,
            versionStatus = VersionStatus.REQUESTED,
            formatMatches = true
        )
        val mirror = object : ApkSourceParser {
            override val source = DownloadSource.APK_MIRROR
            override suspend fun findCandidates(
                request: HelperRequest,
                option: CandidateOption
            ) = emptyList<DownloadCandidate>()

            override suspend fun findCandidatesFromResolvedUrl(
                request: HelperRequest,
                url: String,
                option: CandidateOption
            ) = listOf(underlyingCandidate)
        }
        val parser = MorpheLinkParser(
            testParserContext(pages = emptyMap()),
            delegates = listOf(mirror),
            resolveLink = { resolved }
        )

        val candidate = parser.findCandidates(
            testRequest(packageName = packageName, versionName = "2.0.0", versionCode = 200L),
            CandidateOption.REQUESTED
        ).single()

        assertEquals(DownloadSource.MORPHE_LINK, candidate.source)
        assertEquals(DownloadSource.APK_MIRROR, candidate.viaSource)
        assertEquals(underlyingCandidate.url, candidate.url)
        assertTrue(candidate.note.orEmpty().contains("APKMirror"))
    }

    @Test
    fun `an unknown resolved page opens in the browser`() = runBlocking {
        val resolved = "https://example.com/download/com.example.app"
        val parser = MorpheLinkParser(
            testParserContext(pages = emptyMap()),
            resolveLink = { resolved }
        )

        val candidate = parser.findCandidates(
            testRequest(packageName = packageName),
            CandidateOption.LATEST
        ).single()

        assertFalse(candidate.directDownload)
        assertEquals("web", candidate.fileKind)
        assertEquals(resolved, candidate.url)
        assertEquals(resolved, candidate.captchaUrl)
        assertNull(candidate.viaSource)
    }

    @Test
    fun `the request fallback page is offered when the resolver has no answer`() {
        val fallback = "https://example.com/fallback"
        val parser = MorpheLinkParser(
            testParserContext(pages = emptyMap()),
            resolveLink = { null }
        )
        val request = testRequest(packageName = packageName).copy(fallbackWebUrl = fallback)

        val candidate = parser.requestedFallbackCandidate(request)

        assertEquals(fallback, candidate?.url)
        assertEquals(DownloadSource.MORPHE_LINK, candidate?.source)
        assertFalse(candidate?.directDownload ?: true)
    }

    @Test
    fun `a direct fallback url is resolved instead of only opened`() = runBlocking {
        val fallback = "https://cdn.example.com/Example_2.0.0.apk"
        val parser = MorpheLinkParser(
            testParserContext(pages = emptyMap()),
            resolveLink = { null }
        )
        val request = testRequest(packageName = packageName).copy(fallbackWebUrl = fallback)

        val candidate = parser.findCandidates(request, CandidateOption.LATEST).single()

        assertEquals(fallback, candidate.url)
        assertTrue(candidate.directDownload)
        assertEquals("apk", candidate.fileKind)
    }

    @Test
    fun `a known fallback page is handed to its parser`() = runBlocking {
        val fallback = "https://www.apkmirror.com/apk/example/example-app/1-0-release/"
        val underlying = DownloadCandidate(
            source = DownloadSource.APK_MIRROR,
            name = "Example App",
            packageName = packageName,
            versionName = "2.0.0",
            versionCode = 200L,
            url = "https://www.apkmirror.com/download.php?id=200",
            fileKind = "apk",
            option = CandidateOption.LATEST,
            directDownload = true,
            versionStatus = VersionStatus.LATEST,
            formatMatches = true
        )
        val mirror = object : ApkSourceParser {
            override val source = DownloadSource.APK_MIRROR
            override suspend fun findCandidates(
                request: HelperRequest,
                option: CandidateOption
            ) = emptyList<DownloadCandidate>()

            override suspend fun findCandidatesFromResolvedUrl(
                request: HelperRequest,
                url: String,
                option: CandidateOption
            ) = listOf(underlying)
        }
        val parser = MorpheLinkParser(
            testParserContext(pages = emptyMap()),
            delegates = listOf(mirror),
            resolveLink = { null }
        )
        val request = testRequest(packageName = packageName).copy(fallbackWebUrl = fallback)

        val candidate = parser.findCandidates(request, CandidateOption.LATEST).single()

        assertEquals(DownloadSource.MORPHE_LINK, candidate.source)
        assertEquals(DownloadSource.APK_MIRROR, candidate.viaSource)
        assertEquals(underlying.url, candidate.url)
    }

    @Test
    fun `a disabled source page is not resolved automatically`() = runBlocking {
        val resolved = "https://www.apkmirror.com/apk/example/example-app/1-0-release/"
        val underlying = DownloadCandidate(
            source = DownloadSource.APK_MIRROR,
            name = "Example App",
            packageName = packageName,
            versionName = "2.0.0",
            versionCode = 200L,
            url = "https://www.apkmirror.com/download.php?id=200",
            fileKind = "apk",
            option = CandidateOption.LATEST,
            directDownload = true,
            versionStatus = VersionStatus.LATEST,
            formatMatches = true
        )
        val mirror = object : ApkSourceParser {
            override val source = DownloadSource.APK_MIRROR
            override suspend fun findCandidates(
                request: HelperRequest,
                option: CandidateOption
            ) = emptyList<DownloadCandidate>()

            override suspend fun findCandidatesFromResolvedUrl(
                request: HelperRequest,
                url: String,
                option: CandidateOption
            ) = listOf(underlying)
        }
        val parser = MorpheLinkParser(
            testParserContext(
                pages = emptyMap(),
                isSourceEnabled = { false }
            ),
            delegates = listOf(mirror),
            resolveLink = { resolved }
        )

        val candidate = parser.findCandidates(
            testRequest(packageName = packageName),
            CandidateOption.LATEST
        ).single()

        assertFalse(candidate.directDownload)
        assertEquals("web", candidate.fileKind)
        assertEquals(DownloadSource.MORPHE_LINK, candidate.source)
    }

    @Test
    fun `a github release page resolves to its matching asset`() = runBlocking {
        val releasesUrl = "https://api.github.com/repos/example/app/releases?per_page=30"
        val releasesJson = """
            [
              {
                "tag_name": "v2.0.0",
                "draft": false,
                "assets": [
                  {
                    "name": "Example-2.0.0-arm64-v8a.apk",
                    "browser_download_url": "https://github.com/example/app/releases/download/v2.0.0/Example-2.0.0-arm64-v8a.apk"
                  }
                ]
              }
            ]
        """.trimIndent()
        val parser = MorpheLinkParser(
            testParserContext(pages = mapOf(releasesUrl to releasesJson)),
            resolveLink = { "https://github.com/example/app/releases/tag/v2.0.0" }
        )
        val request = testRequest(
            packageName = packageName,
            versionName = "2.0.0",
            versionCode = 200L
        )

        val candidate = parser.findCandidates(request, CandidateOption.REQUESTED).single()

        assertEquals(DownloadSource.MORPHE_LINK, candidate.source)
        assertEquals("2.0.0", candidate.versionName)
        assertEquals("apk", candidate.fileKind)
        assertTrue(candidate.directDownload)
        assertEquals(
            "https://github.com/example/app/releases/download/v2.0.0/Example-2.0.0-arm64-v8a.apk",
            candidate.url
        )
        assertEquals("Example-2.0.0-arm64-v8a.apk", candidate.files.single().fileName)
    }

    @Test
    fun `github release metadata is read from release urls only`() {
        val target = githubReleaseTargetOf(
            "https://github.com/example/app/releases/tag/v2.0.0"
        )

        assertEquals("example/app", target?.repo)
        assertEquals("v2.0.0", target?.tag)
        assertNull(githubReleaseTargetOf("https://github.com/example/app"))
        assertNull(githubReleaseTargetOf("https://github.com.evil.example/app/releases/tag/v2.0.0"))
    }

    @Test
    fun `morphe link is first in the default source order`() {
        assertEquals(DownloadSource.MORPHE_LINK, DEFAULT_SOURCE_ORDER.first())
        assertEquals(
            DownloadSource.entries.toSet(),
            DEFAULT_SOURCE_ORDER.toSet()
        )
    }
}
