package dev.rushi.apkdownloadhelper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultDisabledSourcesTest {

    @Test
    fun `the five built-around sources start enabled`() {
        val enabled = DownloadSource.entries.filterNot { it in DEFAULT_DISABLED_SOURCES }

        assertEquals(
            listOf(
                DownloadSource.APK_MIRROR,
                DownloadSource.UPTODOWN,
                DownloadSource.APK_PURE,
                DownloadSource.APK_COMBO,
                DownloadSource.AURORA
            ),
            enabled.sortedBy { it.sortIndex }
        )
    }

    @Test
    fun `the remaining sources start disabled`() {
        listOf(
            DownloadSource.APTOIDE,
            DownloadSource.EVOZI,
            DownloadSource.MI9,
            DownloadSource.APK_DOWNLOADER,
            DownloadSource.PLAY
        ).forEach { source ->
            assertTrue("$source should start disabled", source in DEFAULT_DISABLED_SOURCES)
        }
    }

    @Test
    fun `a fresh install still has sources to try`() {
        // A default set that disabled everything would leave the resolver with nothing,
        // which is the one thing these defaults must never do.
        assertFalse(DownloadSource.entries.all { it in DEFAULT_DISABLED_SOURCES })
        assertTrue(DownloadSource.entries.count { it !in DEFAULT_DISABLED_SOURCES } > 1)
    }
}
