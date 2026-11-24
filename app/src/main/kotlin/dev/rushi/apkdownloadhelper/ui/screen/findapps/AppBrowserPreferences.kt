package dev.rushi.apkdownloadhelper.ui.screen.findapps

import android.content.Context

/**
 * Remembers the filter and sort each browser mode was left on. Find Apps and
 * Tracking use separate keys so changing one never changes the other, and the
 * values survive leaving the pager or restarting the app.
 */
internal object AppBrowserPreferences {
    private const val PREFS = "app_browser_view_state"
    private const val FIND_TAB = "find_tab"
    private const val FIND_SORT = "find_sort"
    private const val TRACKING_TAB = "tracking_tab"
    private const val TRACKING_SORT = "tracking_sort"

    fun loadTab(context: Context, trackedOnly: Boolean): AppListTab {
        val fallback = if (trackedOnly) AppListTab.Tracked else AppListTab.All
        val saved = prefs(context).getString(tabKey(trackedOnly), null)
        val tab = saved
            ?.let { runCatching { AppListTab.valueOf(it) }.getOrNull() }
            ?: fallback
        // Tracking has no "All" filter; treat an old or hand-edited value as its default.
        return tab.takeUnless { trackedOnly && it == AppListTab.All } ?: fallback
    }

    fun loadSort(context: Context, trackedOnly: Boolean): AppSort {
        val fallback = if (trackedOnly) AppSort.Updates else AppSort.AZ
        val saved = prefs(context).getString(sortKey(trackedOnly), null)
        return saved
            ?.let { runCatching { AppSort.valueOf(it) }.getOrNull() }
            ?: fallback
    }

    fun saveTab(context: Context, trackedOnly: Boolean, tab: AppListTab) {
        prefs(context).edit().putString(tabKey(trackedOnly), tab.name).apply()
    }

    fun saveSort(context: Context, trackedOnly: Boolean, sort: AppSort) {
        prefs(context).edit().putString(sortKey(trackedOnly), sort.name).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun tabKey(trackedOnly: Boolean) = if (trackedOnly) TRACKING_TAB else FIND_TAB

    private fun sortKey(trackedOnly: Boolean) = if (trackedOnly) TRACKING_SORT else FIND_SORT
}
