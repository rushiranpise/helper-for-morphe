package dev.rushi.apkdownloadhelper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApkMirrorDpiRankTest {

    @Test
    fun rangeCoveringTheScreenBeatsNodpi() {
        assertEquals(0, apkMirrorDpiRank("120-640dpi", 420))
        assertEquals(0, apkMirrorDpiRank("480dpi", 480))
        assertEquals(1, apkMirrorDpiRank("nodpi", 420))
        assertEquals(1, apkMirrorDpiRank("anydpi", 420))
    }

    @Test
    fun densitySpecificRowsBeatNodpiWithoutADeviceDensity() {
        assertEquals(1, apkMirrorDpiRank("nodpi", null))
        assertEquals(2, apkMirrorDpiRank("480dpi", null))
    }

    @Test
    fun closestRangeWinsWhenNoRangeCoversTheScreen() {
        assertEquals(62, apkMirrorDpiRank("480-640dpi", 420))
        assertEquals(22, apkMirrorDpiRank("320dpi", 300))
        assertEquals(24, apkMirrorDpiRank("320dpi", 298))
    }

    @Test
    fun unknownDensityRanksLast() {
        assertTrue(apkMirrorDpiRank(null, 420) > apkMirrorDpiRank("nodpi", 420))
        assertTrue(apkMirrorDpiRank("", 420) > apkMirrorDpiRank("nodpi", 420))
    }
}
