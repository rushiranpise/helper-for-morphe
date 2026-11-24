package dev.rushi.apkdownloadhelper

import dev.rushi.apkdownloadhelper.download.*
import dev.rushi.apkdownloadhelper.ui.screen.captcha.mergeCookieHeaders
import dev.rushi.apkdownloadhelper.ui.screen.captcha.sameCookieSite

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionCookiesTest {

    private val variantPage = "https://www.apkmirror.com/apk/google-inc/youtube/x-2-android-apk-download/".toHttpUrl()
    private val otherPath = "https://www.apkmirror.com/uploads/".toHttpUrl()
    private val otherHost = "https://apkpure.com/apk/".toHttpUrl()

    private fun cookie(
        name: String = "cf_clearance",
        value: String = "session",
        path: String = "/",
        host: String = "www.apkmirror.com",
        expiresAt: Long = System.currentTimeMillis() + 60_000
    ) = Cookie.Builder()
        .name(name)
        .value(value)
        .hostOnlyDomain(host)
        .path(path)
        .expiresAt(expiresAt)
        .build()

    @Before
    fun clear() = SessionCookies.clear()

    @Test
    fun `a cookie set on the page hop is sent on the next one`() {
        // The point of the jar: the answer to the variant page must reach the file hop.
        SessionCookies.saveFromResponse(variantPage, listOf(cookie()))

        val sent = SessionCookies.loadForRequest(variantPage)

        assertEquals(listOf("cf_clearance"), sent.map { it.name })
        assertEquals("session", sent.single().value)
    }

    @Test
    fun `cookies with the same name on different paths are both sent`() {
        SessionCookies.saveFromResponse(
            variantPage,
            listOf(
                cookie(name = "session", value = "root", path = "/"),
                cookie(name = "session", value = "apk", path = "/apk/")
            )
        )

        val sent = SessionCookies.loadForRequest(variantPage)

        assertEquals(listOf("/apk/", "/"), sent.map { it.path })
        assertEquals(listOf("apk", "root"), sent.map { it.value })
    }

    @Test
    fun `cookie domains never widen to a public suffix`() {
        assertEquals("192.168.1.5", rootDomain("192.168.1.5"))
        assertEquals("apkmirror.com", rootDomain("apkmirror.com"))
        // The unit-test JVM has no Android asset database, so the safe fallback
        // is host-only; the device resolves the registrable domain instead.
        val coUkDomain = rootDomain("www.example.co.uk")
        assertTrue(coUkDomain != "co.uk" && coUkDomain != "uk")
    }

    @Test
    fun `cookies stay on their own host`() {
        SessionCookies.saveFromResponse(variantPage, listOf(cookie()))

        assertTrue(SessionCookies.loadForRequest(otherHost).isEmpty())
    }

    @Test
    fun `a refreshed cookie replaces the old one instead of piling up`() {
        SessionCookies.saveFromResponse(variantPage, listOf(cookie(value = "first")))
        SessionCookies.saveFromResponse(variantPage, listOf(cookie(value = "second")))

        val sent = SessionCookies.loadForRequest(variantPage)

        assertEquals(1, sent.size)
        assertEquals("second", sent.single().value)
    }

    @Test
    fun `an expired cookie is never sent`() {
        SessionCookies.saveFromResponse(variantPage, listOf(cookie(expiresAt = System.currentTimeMillis() - 1)))

        assertTrue(SessionCookies.loadForRequest(variantPage).isEmpty())
        assertEquals(0, SessionCookies.hostCount)
    }

    @Test
    fun `a cookie set for a path is not sent to an unrelated path`() {
        SessionCookies.saveFromResponse(variantPage, listOf(cookie(path = "/apk/")))

        assertEquals(1, SessionCookies.loadForRequest(variantPage).size)
        assertTrue(SessionCookies.loadForRequest(otherPath).isEmpty())
    }

    @Test
    fun `cookies captured by the browser are imported for the file hop`() {
        val download = "https://www.apkmirror.com/wp-content/themes/APKMirror/download.php?id=1"
            .toHttpUrl()

        SessionCookies.importCookieHeader(download, "cf_clearance=browser; session=abc")

        val sent = SessionCookies.loadForRequest(download)
        assertEquals(listOf("cf_clearance", "session"), sent.map { it.name })
        assertEquals("browser", sent.first().value)
    }

    @Test
    fun `an unchanged browser cookie keeps the jar's secure flag`() {
        val secure = Cookie.Builder()
            .name("cf_clearance")
            .value("same")
            .hostOnlyDomain("www.apkmirror.com")
            .path("/")
            .secure()
            .expiresAt(System.currentTimeMillis() + 60_000)
            .build()
        SessionCookies.saveFromResponse(variantPage, listOf(secure))

        SessionCookies.importCookieHeader(variantPage, "cf_clearance=same")

        val sent = SessionCookies.loadForRequest(variantPage)
        assertEquals(1, sent.size)
        assertTrue(sent.single().secure)
        assertEquals("same", sent.single().value)
    }

    @Test
    fun `an imported cookie replaces the jar's stale value`() {
        SessionCookies.saveFromResponse(variantPage, listOf(cookie(value = "stale")))

        SessionCookies.importCookieHeader(variantPage, "cf_clearance=fresh")

        assertEquals("fresh", SessionCookies.loadForRequest(variantPage).single().value)
    }

    @Test
    fun `clearing drops every host`() {
        SessionCookies.saveFromResponse(variantPage, listOf(cookie()))
        assertEquals(1, SessionCookies.hostCount)

        SessionCookies.clear()

        assertEquals(0, SessionCookies.hostCount)
        assertTrue(SessionCookies.loadForRequest(variantPage).isEmpty())
    }

    @Test
    fun `an imported cookie is available on the sibling download subdomain`() {
        val downloadPage = "https://apkmirror.com/apk/google-inc/youtube/".toHttpUrl()
        val cdnDownload = "https://download.apkmirror.com/files/youtube.apk".toHttpUrl()

        SessionCookies.importCookieHeader(downloadPage, "cf_clearance=cf_pass_123; session=sess_abc")

        val sentToCdn = SessionCookies.loadForRequest(cdnDownload)
        assertEquals(listOf("cf_clearance", "session"), sentToCdn.map { it.name })
        assertEquals("cf_pass_123", sentToCdn.first().value)

        // Ensure other domains never receive them
        val apkPureRequest = "https://download.apkpure.com/b/APK/test".toHttpUrl()
        assertTrue(SessionCookies.loadForRequest(apkPureRequest).isEmpty())
    }

    @Test
    fun `referer cookies only merge within the same site`() {
        assertTrue(
            sameCookieSite(
                "https://www.apkmirror.com/wp-content/download.php?id=1",
                "https://www.apkmirror.com/apk/example/"
            )
        )
        assertFalse(
            sameCookieSite(
                "https://files.example.net/app.apk",
                "https://www.apkmirror.com/apk/example/"
            )
        )
        assertFalse(
            sameCookieSite(
                "https://www.example.co.uk/app.apk",
                "https://other.co.uk/page"
            )
        )
    }

    @Test
    fun `mergeCookieHeaders combines cookies from download url and referer`() {
        val downloadCookies = "download_token=token456; common=from_download"
        val refererCookies = "cf_clearance=cf_clear_999; common=from_referer; session=sess_orig"

        val merged = mergeCookieHeaders(downloadCookies, refererCookies)

        assertTrue(merged.contains("cf_clearance=cf_clear_999"))
        assertTrue(merged.contains("session=sess_orig"))
        assertTrue(merged.contains("download_token=token456"))
        assertTrue(merged.contains("common=from_download"))
    }
}

