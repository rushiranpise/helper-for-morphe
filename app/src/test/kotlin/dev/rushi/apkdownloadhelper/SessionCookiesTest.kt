package dev.rushi.apkdownloadhelper

import dev.rushi.apkdownloadhelper.download.*
import dev.rushi.apkdownloadhelper.sources.*
import dev.rushi.apkdownloadhelper.storage.*
import dev.rushi.apkdownloadhelper.ui.*
import dev.rushi.apkdownloadhelper.updater.*

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
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
    fun `imported cookies from www subdomain are available on download subdomain for the same site`() {
        val downloadPage = "https://www.apkmirror.com/apk/google-inc/youtube/".toHttpUrl()
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

