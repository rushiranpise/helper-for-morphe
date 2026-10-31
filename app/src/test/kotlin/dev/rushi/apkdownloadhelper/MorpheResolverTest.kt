package dev.rushi.apkdownloadhelper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MorpheResolverTest {

    private val variant =
        "https://www.apkmirror.com/apk/google-inc/youtube/youtube-21-20-405-release/youtube-21-20-405-4-android-apk-download/"
    private val release =
        "https://www.apkmirror.com/apk/google-inc/youtube/youtube-21-20-405-release/"

    @Test
    fun `the query is package, version and abi`() {
        val request = testRequest(
            packageName = "com.example.app",
            versionName = "2.0.0"
        )

        assertEquals("com.example.app~2.0.0~arm64-v8a", morpheResolveQuery(request))
    }

    @Test
    fun `with no requested version the resolver is asked for any`() {
        val request = testRequest(packageName = "com.example.app", versionName = null)

        assertEquals("com.example.app~any~arm64-v8a", morpheResolveQuery(request))
    }

    @Test
    fun `a variant page is recognised and mapped to its release page`() {
        assertEquals(ApkMirrorPage.VARIANT, apkMirrorPageOf(variant))
        assertEquals(release, apkMirrorReleaseUrlOfVariantPage(variant))
    }

    @Test
    fun `a release page is recognised and not mistaken for a variant`() {
        assertEquals(ApkMirrorPage.RELEASE, apkMirrorPageOf(release))
        assertNull(apkMirrorReleaseUrlOfVariantPage(release))
    }

    @Test
    fun `the app page is derived from the release page`() {
        assertEquals(
            "https://www.apkmirror.com/apk/google-inc/youtube/",
            apkMirrorAppPageOfReleaseUrl(release)
        )
    }

    @Test
    fun `pages this helper cannot continue from are rejected`() {
        // The resolver falls back to a Google search when it finds nothing, and it can
        // also land on an app page or another site; none of those is a shortcut.
        assertNull(apkMirrorPageOf("https://google.com/search?q=com.example.app+apkmirror.com"))
        assertNull(apkMirrorPageOf("https://www.apkmirror.com/apk/google-inc/youtube/"))
        assertNull(apkMirrorPageOf("https://instagram.en.uptodown.com/android/download/1-x"))
        assertNull(apkMirrorPageOf("not a url"))
    }

    @Test
    fun `a lookalike host is not treated as APKMirror`() {
        assertNull(apkMirrorPageOf("https://www.apkmirror.com.evil.example/apk/a/b/1-0-release/"))
    }
}
