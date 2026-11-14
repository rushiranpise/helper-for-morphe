package dev.rushi.apkdownloadhelper

import android.util.Log
import java.io.File

/**
 * Runs under Shizuku's shell UID. The APK is copied to a world-readable path
 * because package manager reads the staged file as the system user.
 */
class UpdateUserService : IUpdateService.Stub() {
    override fun install(path: String): Int {
        val staged = File("/data/local/tmp/helper-for-morphe-update.apk")
        return try {
            val source = File(path)
            if (!source.isFile || !source.canRead()) {
                Log.e(TAG, "Update APK is not readable: $path")
                return 255
            }
            source.inputStream().use { input ->
                staged.outputStream().use { output -> input.copyTo(output) }
            }
            staged.setReadable(true, false)

            val process = ProcessBuilder(
                "pm", "install", "-r", "--user", "0", staged.absolutePath
            ).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val result = process.waitFor()
            Log.i(TAG, "pm install result=$result $output")
            result
        } catch (error: Exception) {
            Log.e(TAG, "Shizuku update install failed", error)
            -1
        } finally {
            staged.delete()
        }
    }

    private companion object {
        const val TAG = "UpdateUserService"
    }
}
