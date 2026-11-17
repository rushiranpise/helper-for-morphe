package dev.rushi.apkdownloadhelper

import dev.rushi.apkdownloadhelper.download.VirusTotalScanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class VirusTotalRateLimiterTest {

    @Test
    fun `first call is immediate and records the slot`() {
        val limiter = VirusTotalScanner.RateLimiter(minGapMs = 16_000L)
        val start = System.currentTimeMillis()
        limiter.awaitSlot { false }
        // First slot should be granted with no sleep.
        assertTrue(System.currentTimeMillis() - start < 1500)
        assertTrue(limiter.millisUntilNextSlot() in 1..16_000L)
    }

    @Test
    fun `successive calls wait for the gap`() {
        val limiter = VirusTotalScanner.RateLimiter(minGapMs = 200L)
        limiter.awaitSlot { false }
        assertTrue(limiter.millisUntilNextSlot() in 1..200L)
        val start = System.currentTimeMillis()
        limiter.awaitSlot { false }
        // Second slot must wait out the gap.
        assertTrue(System.currentTimeMillis() - start >= 180L)
    }

    @Test
    fun `cancellation is honoured promptly`() {
        val limiter = VirusTotalScanner.RateLimiter(minGapMs = 5_000L)
        limiter.awaitSlot { false }
        val start = System.currentTimeMillis()
        var cancel = false
        // Flip cancel shortly after the wait starts.
        Thread {
            Thread.sleep(300)
            cancel = true
        }.start()
        assertTrue(
            try {
                limiter.awaitSlot { cancel }
                false
            } catch (e: CancellationException) {
                true
            }
        )
        // Cancellation should land in well under the full 5s gap.
        assertTrue(System.currentTimeMillis() - start < 4000)
    }

    @Test
    fun `skip request releases a pending wait promptly`() {
        val limiter = VirusTotalScanner.RateLimiter(minGapMs = 5_000L)
        limiter.awaitSlot { false }
        val released = CountDownLatch(1)
        val waiter = Thread {
            limiter.awaitSlot { false }
            released.countDown()
        }
        waiter.start()

        Thread.sleep(150)
        limiter.requestSkip()

        assertTrue(released.await(2, TimeUnit.SECONDS))
        waiter.join(500)
    }

    @Test
    fun `millisUntilNextSlot reports zero when a slot is available`() {
        val limiter = VirusTotalScanner.RateLimiter(minGapMs = 16_000L)
        // Fresh limiter: no calls yet.
        assertEquals(0L, limiter.millisUntilNextSlot())
        limiter.awaitSlot { false }
        assertTrue(limiter.millisUntilNextSlot() > 0L)
    }

    @Test
    fun `pace surfaces the wait as progress`() {
        VirusTotalScanner.rateLimiter.awaitSlot { false }
        val messages = mutableListOf<String>()
        // A second pace call must wait; assert the progress callback fired.
        VirusTotalScanner.pace(onProgress = { messages += it })
        assertEquals(1, messages.size)
        assertTrue(messages.first().contains("rate limit"))
    }

    @Test
    fun `callsInLastMinute counts granted slots`() {
        val limiter = VirusTotalScanner.RateLimiter(minGapMs = 1L)
        assertEquals(0, limiter.callsInLastMinute())
        limiter.awaitSlot { false }
        limiter.awaitSlot { false }
        limiter.awaitSlot { false }
        assertEquals(3, limiter.callsInLastMinute())
    }

    @Test
    fun `restoreCallTimestamps rehydrates recent slots and drops stale ones`() {
        val limiter = VirusTotalScanner.RateLimiter(minGapMs = 1L)
        val now = System.currentTimeMillis()
        limiter.restoreCallTimestamps(
            listOf(
                now - 30_000L, // recent — must survive
                now - 40_000L, // recent — must survive
                now - 90_000L  // older than 60s — must be dropped
            )
        )
        assertEquals(2, limiter.callsInLastMinute())
    }

    @Test
    fun `callsInCurrentMinute resets at the minute boundary`() {
        val limiter = VirusTotalScanner.RateLimiter(minGapMs = 1L)
        val now = System.currentTimeMillis()
        val minuteStart = now / 60_000L * 60_000L
        val msIntoMinute = now - minuteStart
        // Two calls safely inside this wall-clock minute (clamped so neither is
        // ever in the future regardless of where we are in the minute), and one
        // in the previous minute that must NOT count.
        limiter.restoreCallTimestamps(
            listOf(
                minuteStart + (msIntoMinute - 500L).coerceAtLeast(0L),
                now,
                minuteStart - 1L // previous minute — must NOT count
            )
        )
        assertEquals(2, limiter.callsInCurrentMinute())
        // The rolling window still sees the previous-minute edge case that
        // falls inside 60s, so the two helpers genuinely measure different things.
    }

    @Test
    fun `waitForWindowClear waits until the oldest call ages out`() {
        val limiter = VirusTotalScanner.RateLimiter(minGapMs = 1L)
        // Oldest call 2s ago -> window clears in ~58s.
        limiter.restoreCallTimestamps(listOf(System.currentTimeMillis() - 2_000L))
        assertTrue(limiter.millisUntilWindowClears() > 55_000L)

        // A window that is already clear returns quickly (bounded 1s backoff), so
        // a persistent 429 (e.g. daily quota exhausted) can't spin a tight loop.
        val clear = VirusTotalScanner.RateLimiter(minGapMs = 1L)
        val clearStart = System.currentTimeMillis()
        clear.waitForWindowClear()
        assertTrue(System.currentTimeMillis() - clearStart < 2_500L)

        // A near-expiry window fires the countdown once with the seconds
        // remaining, then returns once it clears — without sleeping the full 60s.
        val short = VirusTotalScanner.RateLimiter(minGapMs = 1L)
        short.restoreCallTimestamps(listOf(System.currentTimeMillis() - 59_000L))
        val waits = mutableListOf<Long>()
        val shortStart = System.currentTimeMillis()
        short.waitForWindowClear(onWait = { waits += it })
        assertTrue(waits.isNotEmpty())
        assertTrue(waits.first() in 1..2)
        assertTrue(System.currentTimeMillis() - shortStart < 4_000L)
    }

    @Test
    fun `restoreCallTimestamps resumes pacing from the most recent call`() {
        val limiter = VirusTotalScanner.RateLimiter(minGapMs = 200L)
        limiter.restoreCallTimestamps(listOf(System.currentTimeMillis() - 50L))
        // The 200ms gap should now be measured from the restored timestamp, so
        // the next slot is not granted instantly.
        assertTrue(limiter.millisUntilNextSlot() > 0L)
    }
}