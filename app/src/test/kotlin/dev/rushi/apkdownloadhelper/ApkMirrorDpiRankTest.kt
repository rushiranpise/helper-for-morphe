package dev.rushi.apkdownloadhelper

import dev.rushi.apkdownloadhelper.sources.*

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApkMirrorDpiRankTest {

    @Test
    fun nodpiStaysPreferredByDefault() {
        assertEquals(0, apkMirrorDpiRank("nodpi", 420))
        assertEquals(0, apkMirrorDpiRank("anydpi", 480))
        assertEquals(1, apkMirrorDpiRank("120-640dpi", 420))
        assertEquals(1, apkMirrorDpiRank("480dpi", 480))
    }

    @Test
    fun densityRowsNeedAnExplicitOptIn() {
        assertEquals(0, apkMirrorDpiRank("120-640dpi", 420, preferDensitySpecific = true))
        assertEquals(0, apkMirrorDpiRank("480dpi", 480, preferDensitySpecific = true))
        assertTrue(
            apkMirrorDpiRank("nodpi", 420, preferDensitySpecific = true) >
                apkMirrorDpiRank("480-640dpi", 420, preferDensitySpecific = true)
        )
    }

    @Test
    fun closestRangeWinsWhenNoRangeCoversTheScreen() {
        assertEquals(61, apkMirrorDpiRank("480-640dpi", 420, preferDensitySpecific = true))
        assertEquals(21, apkMirrorDpiRank("320dpi", 300, preferDensitySpecific = true))
        assertEquals(23, apkMirrorDpiRank("320dpi", 298, preferDensitySpecific = true))
    }

    @Test
    fun withoutADeviceDensityNodpiStaysDefault() {
        assertEquals(0, apkMirrorDpiRank("nodpi", null))
        assertEquals(1, apkMirrorDpiRank("480dpi", null))
        assertTrue(
            apkMirrorDpiRank("480dpi", null, preferDensitySpecific = true) <
                apkMirrorDpiRank("nodpi", null, preferDensitySpecific = true)
        )
    }

    @Test
    fun unknownDensityRanksLast() {
        assertTrue(apkMirrorDpiRank(null, 420) > apkMirrorDpiRank("nodpi", 420))
        assertTrue(apkMirrorDpiRank("", 420) > apkMirrorDpiRank("nodpi", 420))
    }
}
