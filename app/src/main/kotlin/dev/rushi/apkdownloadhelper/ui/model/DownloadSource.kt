package dev.rushi.apkdownloadhelper.ui.model



internal enum class DownloadSource(
    val label: String,
    val sortIndex: Int,
    val supportsManualArtifactPicker: Boolean = true
) {
    APK_MIRROR("APKMirror", 0),
    UPTODOWN("Uptodown", 1),
    APK_PURE("APKPure", 2),
    APK_CUBE("APKCube", 3),
    APTOIDE("Aptoide", 4),
    MORPHE_LINK("Morphe Link", 5, supportsManualArtifactPicker = false),
    AURORA("Aurora", 6, supportsManualArtifactPicker = false),
    PLAY("Play", 7, supportsManualArtifactPicker = false)
}

internal fun DownloadSource.searchDomain(): String? = when (this) {
    DownloadSource.APK_MIRROR -> "apkmirror.com"
    DownloadSource.UPTODOWN -> "uptodown.com"
    DownloadSource.APK_PURE -> "apkpure.com"
    DownloadSource.APK_CUBE -> "apkcube.com"
    DownloadSource.APTOIDE -> "aptoide.com"
    DownloadSource.MORPHE_LINK -> null
    DownloadSource.PLAY -> "play.google.com"
    DownloadSource.AURORA -> null
}
