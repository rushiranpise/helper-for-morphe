package dev.rushi.apkdownloadhelper.ui.model



internal enum class CandidateOption {
    MANUAL,
    REQUESTED,
    LATEST
}

internal val CandidateOption.labelForLogs: String
    get() = when (this) {
        CandidateOption.MANUAL -> "manual"
        CandidateOption.REQUESTED -> "recommended"
        CandidateOption.LATEST -> "latest"
    }

internal enum class VersionStatus(val label: String) {
    REQUESTED("Requested"),
    COMPATIBLE("Compatible"),
    LATEST("Latest")
}
