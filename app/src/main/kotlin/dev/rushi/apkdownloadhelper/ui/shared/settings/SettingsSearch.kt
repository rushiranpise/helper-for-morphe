package dev.rushi.apkdownloadhelper.ui.shared.settings

import androidx.compose.runtime.*

@Stable
internal class SettingsSearch(private val query: () -> String) {
    val isActive: Boolean get() = query().isNotBlank()
    fun matches(texts: List<String?>): Boolean {
        val q = query().trim()
        return q.isEmpty() || texts.any { it != null && it.contains(q, ignoreCase = true) }
    }
}

internal val LocalSettingsSearch = staticCompositionLocalOf<SettingsSearch?> { null }
