package dev.rushi.apkdownloadhelper.updater

import dev.rushi.apkdownloadhelper.*
import dev.rushi.apkdownloadhelper.ui.*
import dev.rushi.apkdownloadhelper.sources.*
import dev.rushi.apkdownloadhelper.play.*
import dev.rushi.apkdownloadhelper.download.*
import dev.rushi.apkdownloadhelper.storage.*
import dev.rushi.apkdownloadhelper.updater.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import rikka.shizuku.Shizuku

/**
 * Thin wrapper around the Shizuku API. The package is configurable because
 * forks such as Shizuku Next may use a different package name.
 */
internal object ShizukuHelper {
    const val NOT_INSTALLED = 0
    const val NOT_RUNNING = 1
    const val PERMISSION_REQUIRED = 2
    const val READY = 3
    const val UNSUPPORTED = 4

    const val DEFAULT_PACKAGE = "moe.shizuku.privileged.api"
    const val PERMISSION_REQUEST_CODE = 1001

    fun configuredPackage(context: Context): String =
        context.loadHelperSettings().shizukuPackage.trim().ifBlank { DEFAULT_PACKAGE }

    fun managerPackage(context: Context): String {
        val configured = configuredPackage(context)
        if (packageExists(context, configured)) return configured
        return DEFAULT_PACKAGE
    }

    fun isInstalled(context: Context): Boolean {
        if (pingBinder()) return true
        val configured = configuredPackage(context)
        if (packageExists(context, configured)) return true
        return configured != DEFAULT_PACKAGE && packageExists(context, DEFAULT_PACKAGE)
    }

    fun status(context: Context): Int {
        if (!pingBinder()) {
            return if (isInstalled(context)) NOT_RUNNING else NOT_INSTALLED
        }
        return try {
            if (Shizuku.isPreV11()) {
                UNSUPPORTED
            } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                READY
            } else {
                PERMISSION_REQUIRED
            }
        } catch (error: Throwable) {
            Log.w(TAG, "Could not read Shizuku status", error)
            NOT_RUNNING
        }
    }

    fun statusLabel(context: Context, status: Int): String = when (status) {
        READY -> "Ready"
        PERMISSION_REQUIRED -> "Permission required"
        NOT_RUNNING -> "Not running"
        NOT_INSTALLED -> "Not installed"
        else -> "Unsupported"
    }

    fun requestPermission() {
        try {
            Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
        } catch (error: Throwable) {
            Log.w(TAG, "Could not request Shizuku permission", error)
        }
    }

    fun openManager(context: Context) {
        val packageName = managerPackage(context)
        val launch = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(launch) }
            return
        }
        val market = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(market) }.isSuccess) return
        runCatching {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/RikkaApps/Shizuku")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun install(context: Context, apk: File, callback: (Boolean, String) -> Unit) {
        val status = status(context)
        if (status != READY) {
            callback(false, statusLabel(context, status))
            return
        }

        val main = Handler(Looper.getMainLooper())
        main.post {
            val finished = AtomicBoolean(false)
            val args = Shizuku.UserServiceArgs(
                ComponentName(context, UpdateUserService::class.java)
            )
                .daemon(false)
                .version(1)
                .processNameSuffix("update")
                .tag("helper-update")

            fun finish(ok: Boolean, message: String, connection: ServiceConnection) {
                if (!finished.compareAndSet(false, true)) return
                main.post {
                    runCatching { Shizuku.unbindUserService(args, connection, true) }
                    callback(ok, message)
                }
            }

            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    if (finished.get()) return
                    val self = this
                    Thread {
                        val result = runCatching {
                            val updateService = IUpdateService.Stub.asInterface(service)
                                ?: error("Shizuku service returned no binder")
                            Log.i(TAG, "Installing update through Shizuku: ${apk.absolutePath}")
                            updateService.install(apk.absolutePath)
                        }
                        val ok = result.getOrNull() == 0
                        val message = result.exceptionOrNull()?.message
                            ?: if (ok) "Update installed" else "pm install exited with ${result.getOrNull()}"
                        finish(ok, message, self)
                    }.start()
                }

                override fun onServiceDisconnected(name: ComponentName?) = Unit
            }

            try {
                Shizuku.bindUserService(args, connection)
                main.postDelayed({
                    finish(false, "Shizuku install timed out", connection)
                }, 60_000L)
            } catch (error: Throwable) {
                Log.e(TAG, "Could not bind Shizuku update service", error)
                finish(false, error.message ?: "Could not bind Shizuku", connection)
            }
        }
    }

    private fun pingBinder(): Boolean = try {
        Shizuku.pingBinder()
    } catch (error: Throwable) {
        false
    }

    private fun packageExists(context: Context, packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (error: Exception) {
        false
    }

    private const val TAG = "ShizukuHelper"
}

