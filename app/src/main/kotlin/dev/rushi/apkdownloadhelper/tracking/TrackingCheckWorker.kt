package dev.rushi.apkdownloadhelper.tracking

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.rushi.apkdownloadhelper.MainActivity
import dev.rushi.apkdownloadhelper.ui.MorpheTracking
import dev.rushi.apkdownloadhelper.ui.TrackedAppChanges
import dev.rushi.apkdownloadhelper.ui.trackedChanges
import dev.rushi.apkdownloadhelper.updater.ArchiveApp
import dev.rushi.apkdownloadhelper.updater.MorpheArchive
import kotlinx.coroutines.CancellationException
import java.util.Locale

internal const val EXTRA_OPEN_TRACKING = "dev.rushi.apkdownloadhelper.OPEN_TRACKING"

private const val CHANNEL_TRACKED_APPS = "tracked_app_updates"
private const val NOTIFICATION_ID_TRACKED_APPS = 74
private const val MAX_NOTIFICATION_APPS = 5

private data class TrackedAppNotification(
    val app: ArchiveApp,
    val changes: TrackedAppChanges,
    val newlyAvailable: Boolean = false
)

/**
 * Periodic background check for tracked apps.
 *
 * The index is fetched only while something is tracked. A notification is
 * posted for changes since the last notification, then that notification
 * baseline is advanced. The seen snapshot used by the in-app badges is left
 * alone, so opening a tracked app is still what clears its badge.
 */
class TrackingCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val tracked = MorpheTracking.load(context)
        if (tracked.isEmpty()) return Result.success()
        if (!canPostNotifications(context)) return Result.success()

        val index = try {
            MorpheArchive.fetchIndex()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            return Result.retry()
        }

        val apps = index.apps.associateBy { it.packageName }
        val seen = MorpheTracking.loadSnapshots(context, tracked)
        val notified = MorpheTracking.loadNotifiedSnapshots(context, tracked)
        val changes = tracked.mapNotNull { packageName ->
            val app = apps[packageName] ?: return@mapNotNull null
            val baseline = notified[packageName] ?: seen[packageName]
            if (baseline == null) {
                TrackedAppNotification(
                    app = app,
                    changes = TrackedAppChanges(),
                    newlyAvailable = true
                )
            } else {
                val delta = app.trackedChanges(baseline)
                if (delta.updates == 0 && delta.newPatches == 0) {
                    null
                } else {
                    TrackedAppNotification(app, delta)
                }
            }
        }.sortedBy { it.app.name.lowercase(Locale.US) }

        if (changes.isEmpty()) return Result.success()
        if (!showNotification(context, changes)) return Result.success()

        MorpheTracking.markNotified(context, changes.map { it.app })
        MorpheTracking.seedSeen(
            context,
            changes.filter { it.newlyAvailable }.map { it.app }
        )
        return Result.success()
    }
}

private fun showNotification(
    context: Context,
    changes: List<TrackedAppNotification>
): Boolean {
    if (!canPostNotifications(context)) return false

    ensureNotificationChannel(context)
    val title = if (changes.size == 1) {
        val change = changes.single()
        when {
            change.newlyAvailable -> "${change.app.name} is now available"
            change.changes.updates > 0 && change.changes.newPatches > 0 ->
                "${change.app.name} has updates"
            change.changes.updates > 0 -> "${change.app.name} has new versions"
            else -> "${change.app.name} has new patches"
        }
    } else if (changes.all { it.newlyAvailable }) {
        "${changes.size} tracked apps are now available"
    } else {
        "${changes.size} tracked apps have changes"
    }
    val text = if (changes.size == 1) {
        val change = changes.single()
        if (change.newlyAvailable) {
            "It is now in the Morphe index."
        } else {
            change.changes.summary()
        }
    } else {
        "New versions, patches, or apps are available."
    }
    val style = NotificationCompat.InboxStyle()
        .setBigContentTitle(title)
    changes.take(MAX_NOTIFICATION_APPS).forEach { change ->
        style.addLine("${change.app.name}: ${change.summary()}")
    }
    if (changes.size > MAX_NOTIFICATION_APPS) {
        style.setSummaryText("+${changes.size - MAX_NOTIFICATION_APPS} more")
    }

    val open = Intent(context, MainActivity::class.java)
        .putExtra(EXTRA_OPEN_TRACKING, true)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    val pending = PendingIntent.getActivity(
        context,
        11,
        open,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, CHANNEL_TRACKED_APPS)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(style)
        .setContentIntent(pending)
        .setAutoCancel(true)
        .setOnlyAlertOnce(false)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    return runCatching {
        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID_TRACKED_APPS, notification)
    }.isSuccess
}

private fun TrackedAppNotification.summary(): String =
    if (newlyAvailable) "now available" else changes.summary()

private fun TrackedAppChanges.summary(): String {
    val parts = buildList {
        if (updates > 0) {
            add("$updates new version" + if (updates == 1) "" else "s")
        }
        if (newPatches > 0) {
            add("$newPatches new patch" + if (newPatches == 1) "" else "es")
        }
    }
    return parts.joinToString(" and ")
}

private fun canPostNotifications(context: Context): Boolean {
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
    return true
}

private fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    manager.createNotificationChannel(
        NotificationChannel(
            CHANNEL_TRACKED_APPS,
            "Tracked app updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "New versions and patches for tracked apps"
        }
    )
}
