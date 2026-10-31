package dev.rushi.apkdownloadhelper

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * APKPure's direct endpoint: one hop names the file, where the web flow needs three
 * pages. These cover both halves - the request we build and the redirect we read - plus
 * the parser using it, including that a failed shortcut still leaves the web flow working.
 */
class ApkPureDirectTest {

    private val packageName = "com.instagram.android"
    private val signedUrl =
        "https://data.winudf.com/APK/Y29tLmluc3RhZ3JhbS5hbmRyb2lkXzM4NTkxNDU1N19lYWI5ZjEwNA" +
            "?_p=Y29tLmluc3RhZ3JhbS5hbmRyb2lk&filename=Instagram_453.0.0.57.79_APKPure.apk" +
            "&full_size=145026516&is_hot=true&k=abc&package_name=com.instagram.android&source=web"

    @Test
    fun `the latest request asks for the latest build`() {
        assertEquals(
            "https://d.apkpure.com/b/APK/com.example.app?version=latest",
            apkPureDirectUrl("com.example.app", "APK", null)
        )
    }

    @Test
    fun `a pinned build is asked for by version code`() {
        assertEquals(
            "https://d.apkpure.com/b/XAPK/com.example.app?versionCode=385914557",
            apkPureDirectUrl("com.example.app", "XAPK", 385914557L)
        )
    }

    @Test
    fun `the redirect names the file, its version, its build and its size`() {
        val file = apkPureDirectFile(signedUrl, packageName)

        assertNotNull(file)
        assertEquals(signedUrl, file!!.url)
        assertEquals("453.0.0.57.79", file.versionName)
        assertEquals(385914557L, file.versionCode)
        assertEquals("apk", file.fileKind)
        assertEquals(145026516L, file.sizeBytes)
    }

    @Test
    fun `an xapk redirect is recognised as a split archive`() {
        val file = apkPureDirectFile(
            "https://data.winudf.com/XAPK/Y29tLmdvb2dsZS5hbmRyb2lkLnlvdXR1YmVfMTU2MTMxMDE4NV8yNzMwNDNhNQ" +
                "?filename=YouTube_21.44.528-SECONDARY_APKPure.xapk&package_name=com.google.android.youtube",
            "com.google.android.youtube"
        )

        assertEquals("xapk", file?.fileKind)
        // A variant build marker survives, so ranking can still tell builds apart.
        assertEquals("21.44.528-SECONDARY", file?.versionName)
        assertEquals(1561310185L, file?.versionCode)
    }

    @Test
    fun `an unknown package bounces to apkpure and is not a shortcut`() {
        assertNull(apkPureDirectFile("https://apkpure.com", "com.example.app"))
        assertNull(apkPureDirectFile("https://apkpure.com/", "com.example.app"))
        assertNull(apkPureDirectFile(null, "com.example.app"))
        assertNull(apkPureDirectFile("", "com.example.app"))
    }

    @Test
    fun `a redirect for another app is rejected`() {
        // The URL says its own package; a mismatched request must not accept it.
        assertNull(apkPureDirectFile(signedUrl, "com.example.other"))
    }

    @Test
    fun `a lookalike host is not a file URL`() {
        assertNull(
            apkPureDirectFile(
                "https://data.winudf.com.evil.example/APK/abc?filename=A_1.0_APKPure.apk",
                "com.example.app"
            )
        )
    }

    @Test
    fun `a file name whose version cannot be read yields no version`() {
        assertNull(apkPureVersionFromFileName("Instagram.apk"))
        assertNull(apkPureVersionFromFileName(null))
        assertEquals("1.2.3", apkPureVersionFromFileName("Some App_1.2.3_APKPure.apk"))
    }

    @Test
    fun `the parser names a latest file in one request`() = runBlocking {
        val parser = ApkPureParser(
            testParserContext(
                pages = emptyMap(),
                redirects = mapOf("https://d.apkpure.com/b/APK/$packageName?version=latest" to signedUrl)
            )
        )

        val candidates = parser.findCandidates(
            testRequest(packageName = packageName, appName = "Instagram"),
            CandidateOption.LATEST
        )

        assertEquals(1, candidates.size)
        val candidate = candidates.single()
        assertTrue(candidate.directDownload)
        assertEquals("453.0.0.57.79", candidate.versionName)
        assertEquals(385914557L, candidate.versionCode)
        assertEquals("apk", candidate.fileKind)
        assertEquals(signedUrl, candidate.files.single().url)
        assertEquals(145026516L, candidate.files.single().size)
    }

    @Test
    fun `a pinned request returns the asked-for build`() = runBlocking {
        val parser = ApkPureParser(
            testParserContext(
                pages = emptyMap(),
                redirects = mapOf(
                    "https://d.apkpure.com/b/APK/$packageName?versionCode=385914557" to signedUrl
                )
            )
        )

        val candidates = parser.findCandidates(
            testRequest(
                packageName = packageName,
                versionName = "453.0.0.57.79",
                versionCode = 385914557L
            ),
            CandidateOption.REQUESTED
        )

        assertEquals(1, candidates.size)
        assertEquals(385914557L, candidates.single().versionCode)
        assertTrue(candidates.single().directDownload)
    }

    @Test
    fun `a pinned request answered with another build falls back to the web flow`() = runBlocking {
        val infoUrl = "https://apkpure.com/apk-info/$packageName"
        val parser = ApkPureParser(
            testParserContext(
                pages = mapOf(infoUrl to "<html><body><h1>Nothing here</h1></body></html>"),
                redirects = mapOf(
                    "https://d.apkpure.com/b/APK/$packageName?versionCode=999" to signedUrl
                )
            )
        )

        val candidates = parser.findCandidates(
            testRequest(packageName = packageName, versionName = "1.0.0", versionCode = 999L),
            CandidateOption.REQUESTED
        )

        // The signed URL is a 385914557 build, so it is not the 999 build that was asked for.
        assertTrue(candidates.none { it.directDownload })
    }

    @Test
    fun `with no redirect resolver the parser never builds a shortcut`() = runBlocking {
        val infoUrl = "https://apkpure.com/apk-info/$packageName"
        val parser = ApkPureParser(
            testParserContext(pages = mapOf(infoUrl to "<html><body></body></html>"))
        )

        val candidates = parser.findCandidates(
            testRequest(packageName = packageName),
            CandidateOption.LATEST
        )

        assertTrue(candidates.all { !it.directDownload })
    }

    @Test
    fun `an xapk-only request asks the endpoint for the xapk`() = runBlocking {
        val asked = mutableListOf<String>()
        val xapkUrl = signedUrl.replace("/APK/", "/XAPK/").replace(".apk", ".xapk")
        val context = SourceParserContext(
            fetcher = FakeSourceTextFetcher(emptyMap()),
            apkPureApi = FakeApkPureApi(),
            aptoideApi = FakeAptoideApi(),
            resolveRedirect = { url ->
                asked += url
                if (url.contains("/XAPK/")) xapkUrl else null
            }
        )

        val candidates = ApkPureParser(context).findCandidates(
            testRequest(packageName = packageName, requestedFileType = "XAPK"),
            CandidateOption.LATEST
        )

        assertEquals(listOf("https://d.apkpure.com/b/XAPK/$packageName?version=latest"), asked)
        assertEquals("xapk", candidates.single().fileKind)
    }

    @Test
    fun `an apk-only request never asks for the xapk`() = runBlocking {
        val asked = mutableListOf<String>()
        val context = SourceParserContext(
            fetcher = FakeSourceTextFetcher(emptyMap()),
            apkPureApi = FakeApkPureApi(),
            aptoideApi = FakeAptoideApi(),
            resolveRedirect = { url ->
                asked += url
                signedUrl
            }
        )

        ApkPureParser(context).findCandidates(
            testRequest(packageName = packageName, requestedFileType = "APK"),
            CandidateOption.LATEST
        )

        assertTrue(asked.none { it.contains("/XAPK/") })
    }
}
