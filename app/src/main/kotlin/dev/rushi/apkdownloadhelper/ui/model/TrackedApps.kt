package dev.rushi.apkdownloadhelper.ui.model

import dev.rushi.apkdownloadhelper.updater.ArchiveApp
import java.util.Locale

/** One identity for a package everywhere: trimmed and lowercased. */
internal fun trackingPackageKey(packageName: String): String =
    packageName.trim().lowercase(Locale.US)

/** What an app looked like when the user last saw it. */
internal data class TrackedAppSnapshot(
    val versions: Set<String> = emptySet(),
    val patches: Set<String> = emptySet()
)

/** New versions and patch names since [TrackedAppSnapshot]. */
internal data class TrackedAppChanges(
    val updates: Int = 0,
    val newPatches: Int = 0
)

private fun cleanNames(values: Iterable<String?>): Set<String> = values
    .mapNotNull { it?.trim() }
    .filter { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
    .toSet()

internal fun ArchiveApp.trackingSnapshot(): TrackedAppSnapshot = TrackedAppSnapshot(
    versions = cleanNames(versions + patchDetails.flatMap { it.versions }),
    patches = cleanNames(patches + patchDetails.map { it.name })
)

internal fun ArchiveApp.trackedChanges(snapshot: TrackedAppSnapshot?): TrackedAppChanges {
    if (snapshot == null) return TrackedAppChanges()
    val current = trackingSnapshot()
    return TrackedAppChanges(
        updates = (current.versions - snapshot.versions).size,
        newPatches = (current.patches - snapshot.patches).size
    )
}
