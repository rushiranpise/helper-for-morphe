package dev.rushi.apkdownloadhelper.tracking

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.rushi.apkdownloadhelper.storage.loadHelperSettings
import dev.rushi.apkdownloadhelper.ui.model.TrackingStore
import java.util.concurrent.TimeUnit

private const val UNIQUE_WORK_NAME = "tracked_app_index_check"
private const val CHECK_INTERVAL_HOURS = 6L

/** Keeps the periodic tracking check in step with the tracked package set. */
internal object TrackingCheckScheduler {

    fun sync(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val settings = context.loadHelperSettings()
        if (settings.hideFindAppsTab || settings.hideTracking) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }
        if (TrackingStore.load(context).isEmpty()) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }

        val request = PeriodicWorkRequestBuilder<TrackingCheckWorker>(
            CHECK_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun checkNow(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val settings = context.loadHelperSettings()
        if (settings.hideFindAppsTab || settings.hideTracking) return
        if (TrackingStore.load(context).isEmpty()) return

        val request = androidx.work.OneTimeWorkRequestBuilder<TrackingCheckWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueue(request)
    }
}
