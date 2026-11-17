package dev.rushi.apkdownloadhelper

import dev.rushi.apkdownloadhelper.download.fileKindFromUrl
import dev.rushi.apkdownloadhelper.download.fileKindFromUrlOrNull
import dev.rushi.apkdownloadhelper.ui.DownloadSource
import dev.rushi.apkdownloadhelper.ui.browserBuildMismatchMessage
import dev.rushi.apkdownloadhelper.ui.isApkCubeAllowedNavigation
import dev.rushi.apkdownloadhelper.ui.isApkCubeBrowserFlow
import dev.rushi.apkdownloadhelper.ui.isApkCubeHostedUrl
import dev.rushi.apkdownloadhelper.ui.isBrowserBuildMismatchUrl
import dev.rushi.apkdownloadhelper.ui.isCapturableDownload
import dev.rushi.apkdownloadhelper.ui.isUptodownFileUrl
import dev.rushi.apkdownloadhelper.ui.reportsBrowserChallenge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the in-app browser treats as the file it was opened to capture.
 *
 * Uptodown's links carry no extension (`dw.uptodown.com/dwn/<token>`), so the extension
 * test alone let the page handle the download itself and nothing was captured.
 */
class BrowserCaptureUrlTest {

    @Test
    fun `an Uptodown file link is recognised without an extension`() {
        assertTrue("https://dw.uptodown.com/dwn/abc123".isUptodownFileUrl())
        assertTrue("https://dw.uptodown.net/dwn/abc123".isUptodownFileUrl())
        assertTrue("https://dw.uptodown.com/dwn/abc123?x=1".isCapturableDownload())
    }

    @Test
    fun `an Uptodown page is not mistaken for a file`() {
        assertFalse("https://spotify.en.uptodown.com/android/download/1234-x".isUptodownFileUrl())
        assertFalse("https://en.uptodown.com/android/search?query=spotify".isUptodownFileUrl())
    }

    @Test
    fun `a lookalike host is not an Uptodown file`() {
        assertFalse("https://dw.uptodown.com.evil.example/dwn/abc".isUptodownFileUrl())
        assertFalse("https://evil.example/dwn/abc".isCapturableDownload())
    }

    @Test
    fun `plain file links still count`() {
        assertTrue("https://cdn.example.com/app-1.0.apk".isCapturableDownload())
        assertTrue("https://cdn.example.com/a/xapk?filename=App_1.0_APKPure.xapk".isCapturableDownload())
    }

    @Test
    fun `a link that names no kind reports none`() {
        assertNull(fileKindFromUrlOrNull("https://dw.uptodown.com/dwn/abc123"))
        assertNull(fileKindFromUrlOrNull("https://example.com/download"))
    }

    @Test
    fun `a link that names a kind reports it`() {
        assertEquals("apk", fileKindFromUrlOrNull("https://cdn.example.com/app-1.0.apk"))
        assertEquals("xapk", fileKindFromUrlOrNull("https://cdn.example.com/files/App_1.0_APKPure.xapk"))
        assertEquals("apkm", fileKindFromUrlOrNull("https://cdn.example.com/b/APKM/com.example.app?version=latest"))
    }

    @Test
    fun `the plain kind helper keeps its APK default`() {
        // Callers that know nothing else must keep getting a usable extension.
        assertEquals("apk", fileKindFromUrl("https://dw.uptodown.com/dwn/abc123"))
    }

    @Test
    fun `only APKCube and Evozi use the APKCube browser guard`() {
        assertTrue(DownloadSource.APK_CUBE.isApkCubeBrowserFlow())
        assertTrue(DownloadSource.EVOZI.isApkCubeBrowserFlow())
        assertFalse(DownloadSource.APK_MIRROR.isApkCubeBrowserFlow())
        assertFalse(DownloadSource.UPTODOWN.isApkCubeBrowserFlow())
    }

    @Test
    fun `the APKCube host check rejects off-site prelanders`() {
        assertTrue("https://apkcube.com/whatsapp/com.whatsapp/download".isApkCubeHostedUrl())
        assertTrue("https://www.apkcube.com/whatsapp/com.whatsapp/download".isApkCubeHostedUrl())
        assertTrue("https://icons.apkcube.com/com.whatsapp".isApkCubeHostedUrl())
        assertFalse("https://apkcube.com.evil.example/download".isApkCubeHostedUrl())
        assertFalse("https://s.shopee.com.my/8pnI7nf4Kp".isApkCubeHostedUrl())
        assertFalse("not a url".isApkCubeHostedUrl())
    }

    @Test
    fun `build mismatch navigation is recognised`() {
        assertTrue("morphe-helper://build-mismatch?code=123".isBrowserBuildMismatchUrl())
        assertFalse("morphe-helper://download?code=123".isBrowserBuildMismatchUrl())
        assertEquals(
            "Download page is build 123, expected 456.",
            "123".browserBuildMismatchMessage("456")
        )
        assertEquals(
            "Download page is build unknown, expected the requested build.",
            (null as String?).browserBuildMismatchMessage(null)
        )
    }

    @Test
    fun `challenge probe results are classified`() {
        assertTrue("\"challenge\"".reportsBrowserChallenge())
        assertFalse("\"ready\"".reportsBrowserChallenge())
        assertFalse((null as String?).reportsBrowserChallenge())
    }

    @Test
    fun `APKCube navigation allows its file hosts and blocks prelanders`() {
        assertTrue("https://apkcube.com/whatsapp/com.whatsapp/download".isApkCubeAllowedNavigation())
        assertTrue(
            "https://bucket.account.cloudflarestorage.com/app.apk?X-Amz-Signature=x"
                .isApkCubeAllowedNavigation()
        )
        assertTrue("https://pub-123.r2.dev/files/build".isApkCubeAllowedNavigation())
        assertTrue("https://cdn.example.com/app-1.0.xapk".isApkCubeAllowedNavigation())
        assertTrue("https://files.example.com/download.php?id=1".isApkCubeAllowedNavigation())

        assertFalse("https://cdn.example.com/download/token".isApkCubeAllowedNavigation())
        assertFalse("https://s.shopee.com.my/8pnI7nf4Kp".isApkCubeAllowedNavigation())
        assertFalse("https://prelander.example/go".isApkCubeAllowedNavigation())
        assertFalse("https://cloudflarestorage.com.evil.example/download".isApkCubeAllowedNavigation())
        assertFalse("https://r2.dev.evil.example/download".isApkCubeAllowedNavigation())
    }
}
