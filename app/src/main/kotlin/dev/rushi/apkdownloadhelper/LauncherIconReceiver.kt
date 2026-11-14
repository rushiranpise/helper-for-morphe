package dev.rushi.apkdownloadhelper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-applies the saved launcher-alias state after a reboot or app update. */
class LauncherIconReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        context.ensureLauncherIconState()
    }
}
