package dev.rushi.apkdownloadhelper

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Uptodown shortcut: the resolver already names one build's page, so the parser goes
 * straight there instead of walking search -> app -> versions -> files.
 *
 * The page it lands on is still behind Uptodown's captcha, so these also pin the rule that
 * a page without a download link becomes a "solve the captcha" candidate rather than a
 * broken one, and that a page for another app or another version is refused.
 */
class UptodownResolverPathTest {

    private val packageName = "com.example.app"
    private val resolvedUrl = "https://example-app.en.uptodown.com/android/download/987-x"

    private fun filePage(
        version: String = "2.0.0",
        playPackage: String = packageName,
        fileType: String? = null,
        /** Set on a page that offers an XAPK but serves a plain APK - see data-only-xapk. */
        xapkFlagOnly: Boolean = false,
        dataUrl: String? = null
    ): String = """
        <html>
          <head><title>Download Example App $version for Android | Uptodown.com</title></head>
          <body>
            <h1>Example App</h1>
            <div id="gplay-url"
                 data-url="https://play.google.com/store/apps/details?id=$playPackage"></div>
            <table>
              ${if (fileType != null) "<tr><th>File type</th><td>$fileType</td></tr>" else ""}
            </table>
            <script type="application/ld+json">
              {"@type":"SoftwareApplication","softwareVersion":"$version"}
            </script>
            <a id="detail-download-button"
               data-file-id="987"
               ${if (xapkFlagOnly) "data-only-xapk=\"1\"" else ""}
               ${if (dataUrl != null) "data-url=\"$dataUrl\"" else ""}>Download</a>
            <div id="download-turnstile-widget" data-sitekey="solved-in-browser"></div>
          </body>
        </html>
    """.trimIndent()

    @Test
    fun `the resolved page is used directly`() = runBlocking {
        val parser = UptodownParser(
            testParserContext(
                pages = mapOf(resolvedUrl to filePage()),
                resolveViaMorphe = { resolvedUrl }
            )
        )

        val candidates = parser.findCandidates(
            testRequest(packageName = packageName),
            CandidateOption.LATEST
        )

        val candidate = candidates.single()
        assertEquals("2.0.0", candidate.versionName)
        assertEquals(resolvedUrl, candidate.url)
        // No link on the page: the candidate asks the user to solve the captcha, exactly
        // as the scraped path does.
        assertFalse(candidate.directDownload)
        assertTrue(candidate.note.orEmpty().contains("captcha", ignoreCase = true))
        // And it points the in-app browser at the page that holds the button, which is
        // the only way past Turnstile.
        assertEquals(resolvedUrl, candidate.captchaUrl)
    }

    @Test
    fun `a page that reveals its link offers no captcha route`() = runBlocking {
        val direct = "https://dw.uptodown.com/dwn/abc123"
        val parser = UptodownParser(
            testParserContext(
                pages = mapOf(resolvedUrl to filePage(dataUrl = direct)),
                resolveViaMorphe = { resolvedUrl }
            )
        )

        val candidate = parser.findCandidates(
            testRequest(packageName = packageName),
            CandidateOption.LATEST
        ).single()

        assertEquals(null, candidate.captchaUrl)
        assertEquals(null, candidate.note)
    }

    @Test
    fun `a page that does reveal its link is a direct candidate`() = runBlocking {
        val direct = "https://dw.uptodown.com/dwn/abc123"
        val parser = UptodownParser(
            testParserContext(
                pages = mapOf(resolvedUrl to filePage(dataUrl = direct)),
                resolveViaMorphe = { resolvedUrl }
            )
        )

        val candidate = parser.findCandidates(
            testRequest(packageName = packageName),
            CandidateOption.LATEST
        ).single()

        assertTrue(candidate.directDownload)
        assertEquals(direct, candidate.url)
        assertEquals(direct, candidate.files.single().url)
        assertEquals(resolvedUrl, candidate.files.single().referer)
    }

    @Test
    fun `a page that states an xapk is labelled as a split archive`() = runBlocking {
        val parser = UptodownParser(
            testParserContext(
                pages = mapOf(resolvedUrl to filePage(fileType = "XAPK")),
                resolveViaMorphe = { resolvedUrl }
            )
        )

        val candidate = parser.findCandidates(
            testRequest(packageName = packageName, requestedFileType = "XAPK"),
            CandidateOption.LATEST
        ).single()

        assertEquals("xapk", candidate.fileKind)
        assertTrue(candidate.formatMatches)
    }

    @Test
    fun `the xapk-only button flag alone does not relabel a plain apk`() = runBlocking {
        // Spotify 9.1.94.1929: data-only-xapk was set, and the button served a .apk.
        val parser = UptodownParser(
            testParserContext(
                pages = mapOf(resolvedUrl to filePage(xapkFlagOnly = true)),
                resolveViaMorphe = { resolvedUrl }
            )
        )

        val candidate = parser.findCandidates(
            testRequest(packageName = packageName, requestedFileType = "APK"),
            CandidateOption.LATEST
        ).single()

        assertEquals("apk", candidate.fileKind)
        assertTrue(candidate.formatMatches)
    }

    @Test
    fun `a page for another app is refused and the site is walked instead`() = runBlocking {
        // Clones share an app name; only the Play id proves which app this is.
        val searchUrl = uptodownSearchUrlFor(packageName)
        val detailUrl = "https://example-app.en.uptodown.com/android"
        val parser = UptodownParser(
            testParserContext(
                pages = mapOf(
                    resolvedUrl to filePage(playPackage = "com.example.clone"),
                    searchUrl to "<html><body></body></html>",
                    "$detailUrl/download" to """
                        <html><body>
                          <div id="detail-app-name" data-code="1">Example App</div>
                          <table><tr><th>Package Name</th><td>$packageName</td></tr></table>
                        </body></html>
                    """.trimIndent(),
                    "$detailUrl/versions" to """{"data": []}"""
                ),
                resolveViaMorphe = { resolvedUrl }
            )
        )

        val candidates = parser.findCandidates(
            testRequest(packageName = packageName),
            CandidateOption.LATEST
        )

        // The resolver's page was rejected, so the candidate came from the walked path.
        assertTrue(candidates.all { it.url != resolvedUrl })
    }

    @Test
    fun `a requested version that the page does not offer falls back`() = runBlocking {
        val searchUrl = uptodownSearchUrlFor(packageName)
        val detailUrl = "https://example-app.en.uptodown.com/android"
        val parser = UptodownParser(
            testParserContext(
                pages = mapOf(
                    resolvedUrl to filePage(version = "2.0.0"),
                    searchUrl to "<html><body></body></html>",
                    "$detailUrl/download" to """
                        <html><body>
                          <div id="detail-app-name" data-code="1">Example App</div>
                          <table><tr><th>Package Name</th><td>$packageName</td></tr></table>
                        </body></html>
                    """.trimIndent(),
                    "$detailUrl/versions" to """{"data": []}"""
                ),
                resolveViaMorphe = { resolvedUrl }
            )
        )

        val candidates = parser.findCandidates(
            testRequest(packageName = packageName, versionName = "1.5.0"),
            CandidateOption.REQUESTED
        )

        assertTrue(candidates.all { it.versionName != "2.0.0" })
    }

    @Test
    fun `with the resolver off nothing is fetched from it`() = runBlocking {
        val searchUrl = uptodownSearchUrlFor(packageName)
        val detailUrl = "https://example-app.en.uptodown.com/android"
        val parser = UptodownParser(
            testParserContext(
                pages = mapOf(
                    searchUrl to "<html><body></body></html>",
                    "$detailUrl/download" to """
                        <html><body>
                          <div id="detail-app-name" data-code="1">Example App</div>
                          <table><tr><th>Package Name</th><td>$packageName</td></tr></table>
                        </body></html>
                    """.trimIndent(),
                    "$detailUrl/versions" to """{"data": []}"""
                )
            )
        )

        val candidates = parser.findCandidates(
            testRequest(packageName = packageName),
            CandidateOption.LATEST
        )

        assertTrue(candidates.none { it.url == resolvedUrl })
    }

    private fun uptodownSearchUrlFor(packageName: String): String =
        "https://en.uptodown.com/android/search?query=${java.net.URLEncoder.encode(packageName, "UTF-8")}"
}
