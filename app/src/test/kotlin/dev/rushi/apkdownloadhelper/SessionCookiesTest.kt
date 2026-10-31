package dev.rushi.apkdownloadhelper

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
    fun `clearing drops every host`() {
        SessionCookies.saveFromResponse(variantPage, listOf(cookie()))
        assertEquals(1, SessionCookies.hostCount)

        SessionCookies.clear()

        assertEquals(0, SessionCookies.hostCount)
        assertTrue(SessionCookies.loadForRequest(variantPage).isEmpty())
    }
}
