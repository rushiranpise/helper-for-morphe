package dev.rushi.apkdownloadhelper

import dev.rushi.apkdownloadhelper.sources.runSourceLookup
import dev.rushi.apkdownloadhelper.ui.ResolveOutcome
import dev.rushi.apkdownloadhelper.ui.definitiveNotFoundReason
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceNegativeCacheTest {

    @Test
    fun `an explicit not-found outcome is cacheable`() {
        val outcome = ResolveOutcome(
            candidates = emptyList(),
            notFoundMessage = "no listing for com.example.app"
        )

        assertEquals("no listing for com.example.app", outcome.definitiveNotFoundReason())
    }

    @Test
    fun `an empty filtered candidate list is not a cacheable miss`() {
        assertNull(ResolveOutcome(candidates = emptyList()).definitiveNotFoundReason())
    }

    @Test
    fun `lookup errors are not cacheable misses`() {
        assertNull(
            ResolveOutcome(
                candidates = emptyList(),
                errorMessage = "network connection failed"
            ).definitiveNotFoundReason()
        )
        assertNull(
            ResolveOutcome(
                candidates = emptyList(),
                errorMessage = "lookup threw",
                notFoundMessage = "no listing for com.example.app"
            ).definitiveNotFoundReason()
        )
    }

    @Test
    fun `a cancelled lookup is rethrown instead of becoming a result`() {
        assertThrows(CancellationException::class.java) {
            runBlocking {
                runSourceLookup<Unit> {
                    throw CancellationException("request cancelled")
                }
            }
        }
    }

    @Test
    fun `a thrown lookup stays a failure`() = runBlocking {
        val result = runSourceLookup<Unit> {
            throw IllegalStateException("lookup failed")
        }

        assertTrue(result.isFailure)
    }
}
