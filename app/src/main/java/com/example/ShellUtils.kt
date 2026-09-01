package com.example

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader

object ShellUtils {

    fun isRootAvailable(): Boolean {
        var process: Process? = null
        var os: DataOutputStream? = null
        var reader: BufferedReader? = null
        return try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process.outputStream)
            os.writeBytes("id\n")
            os.writeBytes("exit\n")
            os.flush()
            
            reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine() ?: ""
            val exitValue = process.waitFor()
            
            // True root is only when exit code is 0 and output indicates uid=0 (root)
            exitValue == 0 && (output.contains("uid=0") || output.contains("root"))
        } catch (t: Throwable) {
            false
        } finally {
            try {
                reader?.close()
                os?.close()
                process?.destroy()
            } catch (e: Exception) {
                // ignore
            }
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
        } catch (t: Throwable) {
            Log.e("ShellUtils", "Root execution failed for: $command", t)
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

    fun takeRootScreencap(context: Context, method: String): Boolean {
        val time = System.currentTimeMillis()
        
        // 1. Try simulated hardware keyevent / statusbar command if selected
        if (method == "keyevent") {
            val keyeventCmd = "input keyevent 120 || cmd statusbar screenshot"
            val keySuccess = runRootCommand(keyeventCmd)
            if (keySuccess) {
                return true
            }
        }

        // 2. Direct screencap binary execution
        val picturesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Screenshots")
        if (!picturesDir.exists()) {
            picturesDir.mkdirs()
        }
        val targetFile = File(picturesDir, "Screenshot_${time}.png")
        val filePath = targetFile.absolutePath

        // Screencap + permission fix for media providers
        val screencapCmd = "mkdir -p '$picturesDir' && screencap -p '$filePath' && chmod 666 '$filePath'"
        val success = runRootCommand(screencapCmd)

        if (success && targetFile.exists() && targetFile.length() > 0) {
            // Index file into Android Gallery / Google Photos
            try {
                MediaScannerConnection.scanFile(
                    context.applicationContext,
                    arrayOf(filePath),
                    arrayOf("image/png"),
                    null
                )
            } catch (e: Exception) {
                Log.e("ShellUtils", "MediaScanner failed", e)
            }
            return true
        }

        // Fallback: Try DCIM folder
        val dcimDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Screenshots")
        if (!dcimDir.exists()) {
            dcimDir.mkdirs()
        }
        val dcimFile = File(dcimDir, "Screenshot_${time}.png")
        val dcimPath = dcimFile.absolutePath
        val dcimCmd = "mkdir -p '$dcimDir' && screencap -p '$dcimPath' && chmod 666 '$dcimPath'"
        val dcimSuccess = runRootCommand(dcimCmd)

        if (dcimSuccess && dcimFile.exists() && dcimFile.length() > 0) {
            try {
                MediaScannerConnection.scanFile(
                    context.applicationContext,
                    arrayOf(dcimPath),
                    arrayOf("image/png"),
                    null
                )
            } catch (e: Exception) {
                // ignore
            }
            return true
        }

        return false
    }
}
