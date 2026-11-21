package dev.rushi.apkdownloadhelper.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import dev.rushi.apkdownloadhelper.MORPHE_MANAGER_PACKAGES
import dev.rushi.apkdownloadhelper.updater.ShizukuHelper

internal fun openAddSource(context: Context, addUrl: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(addUrl))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val manager = MORPHE_MANAGER_PACKAGES.firstOrNull { pkg ->
        runCatching {
            context.packageManager.queryIntentActivities(intent.setPackage(pkg), 0).isNotEmpty()
        }.getOrDefault(false)
    }
    if (manager != null) intent.setPackage(manager)
    runCatching { context.startActivity(intent) }
}

internal fun openVirusTotalPage(context: Context, sha256: String) {
    val open = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://www.virustotal.com/gui/file/$sha256")
    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    runCatching { context.startActivity(open) }
        .onFailure {
            Toast.makeText(context, "No findapps available", Toast.LENGTH_SHORT).show()
        }
}

internal fun handleShizukuAction(context: Context, status: Int) {
    when (status) {
        ShizukuHelper.READY -> {
            Toast.makeText(context, "Shizuku is ready.", Toast.LENGTH_SHORT).show()
        }
        ShizukuHelper.PERMISSION_REQUIRED -> ShizukuHelper.requestPermission()
        ShizukuHelper.NOT_RUNNING,
        ShizukuHelper.NOT_INSTALLED -> ShizukuHelper.openManager(context)
        else -> {
            Toast.makeText(context, "This Shizuku version is not supported.", Toast.LENGTH_SHORT).show()
        }
    }
}

internal fun openExternal(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
