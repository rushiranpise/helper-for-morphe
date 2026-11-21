package dev.rushi.apkdownloadhelper

import dev.rushi.apkdownloadhelper.updater.*

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class MorpheArchiveFreshnessTest {

    private fun utc(text: String): Long =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse(text)!!
            .time

    @Test
    fun `a just-built index reads as just now`() {
        val fresh = archiveFreshness("2026-09-13 03:23 UTC", now = utc("2026-09-13 03:23"))!!

        assertEquals("just now", fresh.label)
        assertFalse(fresh.stale)
    }

    @Test
    fun `an index built minutes ago reads in minutes`() {
        val fresh = archiveFreshness("2026-09-13 03:23 UTC", now = utc("2026-09-13 03:28"))!!

        assertEquals("5 min ago", fresh.label)
        assertFalse(fresh.stale)
    }

    @Test
    fun `an index built this morning reads in hours`() {
        // The case that prompted this: a build from before a source changed its patches.
        val fresh = archiveFreshness("2026-09-13 03:23 UTC", now = utc("2026-09-13 11:30"))!!

        assertEquals("8 h ago", fresh.label)
        assertFalse(fresh.stale)
    }

    @Test
    fun `past the daily rebuild the label names the day and flags it`() {
        val fresh = archiveFreshness("2026-09-06 03:23 UTC", now = utc("2026-09-13 12:00"))!!

        assertEquals("6 Sep", fresh.label)
        assertTrue(fresh.stale)
    }

    @Test
    fun `exactly two days old is not yet stale`() {
        val fresh = archiveFreshness("2026-09-11 12:00 UTC", now = utc("2026-09-13 12:00"))!!

        assertEquals("48 h ago", fresh.label)
        assertFalse(fresh.stale)
    }

    @Test
    fun `a timestamp from the future is treated as just now`() {
        // Clock skew between the runner and the phone should not produce a negative age.
        val fresh = archiveFreshness("2026-09-13 12:05 UTC", now = utc("2026-09-13 12:00"))!!

        assertEquals("just now", fresh.label)
        assertFalse(fresh.stale)
    }

    @Test
    fun `a missing or unreadable stamp shows nothing`() {
        assertNull(archiveFreshness(null, now = utc("2026-09-13 12:00")))
        assertNull(archiveFreshness("", now = utc("2026-09-13 12:00")))
        assertNull(archiveFreshness("not a date", now = utc("2026-09-13 12:00")))
    }
}
