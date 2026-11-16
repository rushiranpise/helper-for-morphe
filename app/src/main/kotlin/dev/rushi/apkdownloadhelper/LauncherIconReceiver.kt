package dev.rushi.apkdownloadhelper

import dev.rushi.apkdownloadhelper.*
import dev.rushi.apkdownloadhelper.ui.*
import dev.rushi.apkdownloadhelper.sources.*
import dev.rushi.apkdownloadhelper.play.*
import dev.rushi.apkdownloadhelper.download.*
import dev.rushi.apkdownloadhelper.storage.*
import dev.rushi.apkdownloadhelper.updater.*

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-applies the saved launcher-alias state after a reboot or app update. */
class LauncherIconReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        context.ensureLauncherIconState()
    }
}

