package dev.rushi.apkdownloadhelper.ui.util

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import dev.rushi.apkdownloadhelper.storage.*
import java.util.Locale

internal fun formatScanDate(epochSeconds: Long): String {
    val date = java.util.Date(epochSeconds * 1000L)
    return java.text.SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
}

internal fun formatTransferSpeed(bytesPerSec: Double): String {
    if (bytesPerSec <= 0.0) return ""
    val mb = bytesPerSec / (1024.0 * 1024.0)
    if (mb >= 1.0) return String.format(Locale.US, "%.1f MB/s", mb)
    val kb = bytesPerSec / 1024.0
    if (kb >= 1.0) return String.format(Locale.US, "%.0f KB/s", kb)
    return String.format(Locale.US, "%.0f B/s", bytesPerSec)
}

internal fun formatTransferEta(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(1L)
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%d:%02d", m, s)
    }
}

internal fun Long.formatBytes(): String {
    if (this <= 0L) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    var value = toDouble()
    var unit = 0
    while (value >= 1024.0 && unit < units.lastIndex) { value /= 1024.0; unit++ }
    return if (unit == 0) "${toLong()} B" else String.format(Locale.US, "%.1f %s", value, units[unit])
}
