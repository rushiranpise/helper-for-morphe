package dev.rushi.apkdownloadhelper

import dev.rushi.apkdownloadhelper.ui.model.*
import dev.rushi.apkdownloadhelper.updater.*
import org.junit.Assert.assertEquals
import org.junit.Test

class AppTrackingTest {

    private fun app(
        versions: List<String> = listOf("1.0"),
        patches: List<String> = listOf("Base patch")
    ) = ArchiveApp(
        packageName = "com.example",
        name = "Example",
        versions = versions,
        patches = patches
    )

    @Test
    fun `a saved snapshot sees new versions and patches`() {
        val saved = app().trackingSnapshot()

        val changes = app(
            versions = listOf("1.0", "2.0"),
            patches = listOf("Base patch", "Extra patch")
        ).trackedChanges(saved)

        assertEquals(1, changes.updates)
        assertEquals(1, changes.newPatches)
    }

    @Test
    fun `a missing snapshot does not report every version as new`() {
        val changes = app().trackedChanges(null)

        assertEquals(0, changes.updates)
        assertEquals(0, changes.newPatches)
    }

    @Test
    fun `removed versions and patches are not counted as new`() {
        val saved = app(
            versions = listOf("1.0", "2.0"),
            patches = listOf("Base patch", "Extra patch")
        ).trackingSnapshot()

        val changes = app(versions = listOf("1.0"), patches = listOf("Base patch"))
            .trackedChanges(saved)

        assertEquals(0, changes.updates)
        assertEquals(0, changes.newPatches)
    }

    @Test
    fun `patch detail versions are part of the update baseline`() {
        val withDetail = ArchiveApp(
            packageName = "com.example",
            name = "Example",
            versions = listOf("1.0"),
            patchDetails = listOf(
                ArchivePatch(name = "Base patch", versions = listOf("1.0", "1.1"))
            )
        )
        val saved = withDetail.trackingSnapshot()

        val changes = withDetail.copy(
            patchDetails = listOf(
                ArchivePatch(name = "Base patch", versions = listOf("1.0", "1.1", "1.2"))
            )
        ).trackedChanges(saved)

        assertEquals(1, changes.updates)
        assertEquals(0, changes.newPatches)
    }
}
