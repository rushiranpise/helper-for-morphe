package dev.rushi.apkdownloadhelper

import dev.rushi.apkdownloadhelper.storage.SourceNegativeCache
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceNegativeCacheTest {

    @Test
    fun `rate limits and timeouts are not definitive misses`() {
        assertFalse(SourceNegativeCache.isDefinitive("HTTP 429 from APKMirror"))
        assertFalse(SourceNegativeCache.isDefinitive("read timeout"))
        assertFalse(SourceNegativeCache.isDefinitive("Cloudflare challenge"))
        assertFalse(SourceNegativeCache.isDefinitive("invalid json"))
    }

    @Test
    fun `missing listings and apps are definitive misses`() {
        assertTrue(SourceNegativeCache.isDefinitive("no listing for com.example.app"))
        assertTrue(SourceNegativeCache.isDefinitive("app not found"))
        assertTrue(SourceNegativeCache.isDefinitive("no versions matched 1.2.3"))
        assertTrue(SourceNegativeCache.isDefinitive("HTTP 404"))
    }
}
