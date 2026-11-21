package dev.rushi.apkdownloadhelper

import dev.rushi.apkdownloadhelper.updater.*

import org.junit.Assert.assertEquals
import org.junit.Test

class MorpheArchiveSourcesTest {

    private fun source(
        repo: String,
        patchedDate: String? = null,
        patches: List<String> = listOf("Unlock Pro")
    ) = ArchiveSource(
        repo = repo,
        patches = patches.map { ArchivePatch(name = it) },
        latestChanges = patchedDate?.let { ArchiveChanges(title = "x", date = it) }
    )

    @Test
    fun `sources with the identical patch set collapse into one entry`() {
        val groups = groupArchiveSources(
            listOf(
                source("upstream/patches", "2026-09-08"),
                source("fork/patches", "2026-08-19")
            )
        )

        assertEquals(1, groups.size)
        assertEquals("upstream/patches", groups[0].primary.repo)
        assertEquals(listOf("fork/patches"), groups[0].mirrors.map { it.repo })
    }

    @Test
    fun `the order of the patch names does not split a group`() {
        val groups = groupArchiveSources(
            listOf(
                source("a/patches", patches = listOf("One", "Two")),
                source("b/patches", patches = listOf("Two", "One"))
            )
        )

        assertEquals(1, groups.size)
        assertEquals(1, groups[0].mirrors.size)
    }

    @Test
    fun `a different patch set stays its own entry`() {
        val groups = groupArchiveSources(
            listOf(
                source("a/patches", "2026-09-06", patches = listOf("Security Bypass", "Unlock Pro")),
                source("b/patches", "2026-08-19", patches = listOf("Security Bypass", "Unlock Pro")),
                source("c/patches", "2026-09-08", patches = listOf("AAAD Premium"))
            )
        )

        // Two sets, and the newest release leads the list as well as its own group.
        assertEquals(2, groups.size)
        assertEquals(listOf("c/patches", "a/patches"), groups.map { it.primary.repo })
        assertEquals(listOf("b/patches"), groups[1].mirrors.map { it.repo })
    }

    @Test
    fun `sources without a release date keep the index order`() {
        // Distinct patch sets, or the three would collapse into one group instead.
        val groups = groupArchiveSources(
            listOf(
                source("first/patches", "2026-07-01", patches = listOf("A")),
                source("second/patches", null, patches = listOf("B")),
                source("third/patches", null, patches = listOf("C"))
            )
        )

        assertEquals(
            listOf("first/patches", "second/patches", "third/patches"),
            groups.map { it.primary.repo }
        )
    }

    @Test
    fun `a single source produces no mirrors`() {
        val groups = groupArchiveSources(listOf(source("only/patches")))

        assertEquals(1, groups.size)
        assertEquals("only/patches", groups[0].primary.repo)
        assertEquals(emptyList<String>(), groups[0].mirrors.map { it.repo })
    }
}
