package com.example

import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader

object ShellUtils {

    fun isRootAvailable(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return try {
            val process = Runtime.getRuntime().exec("which su")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            process.destroy()
            line != null
        } catch (t: Throwable) {
            false
        }
    }

    fun runRootCommand(command: String): Boolean {
        var process: Process? = null
        var os: DataOutputStream? = null
        return try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()
            val exitValue = process.waitFor()
            exitValue == 0
        } catch (e: Exception) {
            Log.e("ShellUtils", "Root execution failed for: $command", e)
            false
        } finally {
            try {
                os?.close()
                process?.destroy()
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
