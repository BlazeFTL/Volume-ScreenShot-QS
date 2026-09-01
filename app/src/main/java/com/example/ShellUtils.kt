package com.example

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ShellUtils {

    private const val TAG = "ShellUtils"

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
            Log.e(TAG, "Root execution failed for: $command", t)
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

    private fun getScreenshotFilePath(): Pair<File, String> {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Screenshot_${timeStamp}_${System.currentTimeMillis() % 1000}.png"

        // Prefer standard Pictures/Screenshots path
        val picturesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Screenshots")
        if (!picturesDir.exists()) {
            picturesDir.mkdirs()
        }
        val targetFile = File(picturesDir, fileName)
        return Pair(targetFile, targetFile.absolutePath)
    }

    /**
     * Executes collapse (if requested) and screencap in a SINGLE su process session.
     * This avoids multiple "Superuser Granted" toasts from Magisk/KernelSU.
     */
    fun takeRootScreencap(context: Context, method: String, collapseFirst: Boolean = false): Boolean {
        // 1. If keyevent method is explicitly selected
        if (method == "keyevent") {
            val cmd = if (collapseFirst) {
                "cmd statusbar collapse; sleep 0.45; input keyevent 120"
            } else {
                "input keyevent 120"
            }
            return runRootCommand(cmd)
        }

        // 2. Direct screencap binary execution (Works everywhere including Firefox Nightly & secure apps)
        val (targetFile, filePath) = getScreenshotFilePath()
        val parentDir = targetFile.parentFile?.absolutePath ?: "/storage/emulated/0/Pictures/Screenshots"

        val scriptBuilder = StringBuilder()
        if (collapseFirst) {
            scriptBuilder.append("cmd statusbar collapse\n")
            scriptBuilder.append("sleep 0.45\n")
        }
        scriptBuilder.append("mkdir -p '$parentDir'\n")
        scriptBuilder.append("screencap -p '$filePath'\n")
        scriptBuilder.append("chmod 666 '$filePath'\n")
        scriptBuilder.append("chown media_rw:media_rw '$filePath' 2>/dev/null || true\n")
        scriptBuilder.append("restorecon '$filePath' 2>/dev/null || true\n")
        scriptBuilder.append("am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d 'file://$filePath' 2>/dev/null || true\n")

        val success = runRootCommand(scriptBuilder.toString())

        // Index with Android MediaScannerConnection as well
        if (targetFile.exists() && targetFile.length() > 0) {
            try {
                MediaScannerConnection.scanFile(
                    context.applicationContext,
                    arrayOf(filePath),
                    arrayOf("image/png")
                ) { path, uri ->
                    Log.d(TAG, "MediaScanner indexed $path -> $uri")
                }
            } catch (e: Exception) {
                Log.e(TAG, "MediaScannerConnection failed", e)
            }
            return true
        }

        // Fallback: If primary target file not created, try DCIM path
        val dcimDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Screenshots")
        if (!dcimDir.exists()) {
            dcimDir.mkdirs()
        }
        val fallbackFile = File(dcimDir, targetFile.name)
        val fallbackPath = fallbackFile.absolutePath
        val fallbackScript = "mkdir -p '$dcimDir' && screencap -p '$fallbackPath' && chmod 666 '$fallbackPath'"
        val fallbackSuccess = runRootCommand(fallbackScript)

        if (fallbackSuccess && fallbackFile.exists() && fallbackFile.length() > 0) {
            try {
                MediaScannerConnection.scanFile(
                    context.applicationContext,
                    arrayOf(fallbackPath),
                    arrayOf("image/png"),
                    null
                )
            } catch (e: Exception) {
                // ignore
            }
            return true
        }

        return success && (targetFile.exists() || fallbackFile.exists())
    }
}
