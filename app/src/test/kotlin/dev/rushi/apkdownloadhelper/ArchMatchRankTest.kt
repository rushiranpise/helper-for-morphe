package dev.rushi.apkdownloadhelper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchMatchRankTest {

    private val arm64Phone = listOf("arm64-v8a", "armeabi-v7a", "armeabi")
    private val intelPhone = listOf("x86_64", "x86")

    @Test
    fun `the device's own abi ranks first`() {
        assertEquals(0, archMatchRank("arm64-v8a", arm64Phone))
        assertEquals(1, archMatchRank("armeabi-v7a", arm64Phone))
    }

    @Test
    fun `the device's abi order decides between two runnable labels`() {
        // A 64-bit phone runs both, but prefers the 64-bit build.
        assertTrue(archMatchRank("arm64-v8a", arm64Phone) < archMatchRank("armeabi-v7a", arm64Phone))
    }

    @Test
    fun `a universal build beats one built for another abi`() {
        assertTrue(
            archMatchRank("universal", arm64Phone) < archMatchRank("x86_64", arm64Phone)
        )
    }

    @Test
    fun `another abi is a mismatch`() {
        assertEquals(ARCH_RANK_MISMATCH, archMatchRank("armeabi-v7a", intelPhone))
        assertEquals(ARCH_RANK_MISMATCH, archMatchRank("x86", arm64Phone))
    }

    @Test
    fun `an empty label makes no claim about architecture`() {
        assertEquals(ARCH_RANK_UNIVERSAL, archMatchRank(null, arm64Phone))
        assertEquals(ARCH_RANK_UNIVERSAL, archMatchRank("  ", arm64Phone))
        assertEquals(ARCH_RANK_UNIVERSAL, archMatchRank("all architectures", arm64Phone))
    }

    @Test
    fun `the loose spellings sources use still match`() {
        // APKMirror writes full ABI names, other sources write arm64 or v7a,
        // and bundles can list several.
        assertEquals(0, archMatchRank("arm64-v8a + armeabi-v7a", arm64Phone))
        assertEquals(0, archMatchRank("arm64", arm64Phone))
        assertEquals(1, archMatchRank("v7a", arm64Phone))
        assertEquals(0, archMatchRank("ARM64-V8A", arm64Phone))
    }

    @Test
    fun `a plain armeabi label does not swallow the v7a one`() {
        assertEquals(2, archMatchRank("armeabi", arm64Phone))
        assertEquals(1, archMatchRank("armeabi-v7a", arm64Phone))
    }
}
