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

    fun enableAccessibilityServiceWithRoot(context: Context): Boolean {
        val serviceName = "${context.packageName}/${ScreenshotAccessibilityService::class.java.name}"
        val cmd = "settings put secure accessibility_enabled 1 && settings put secure enabled_accessibility_services '$serviceName'"
        return runRootCommand(cmd)
    }

    private fun getScreenshotFilePath(): Pair<File, String> {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Screenshot_${timeStamp}_${System.currentTimeMillis() % 1000}.png"

        val picturesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Screenshots")
        if (!picturesDir.exists()) {
            picturesDir.mkdirs()
        }
        val targetFile = File(picturesDir, fileName)
        return Pair(targetFile, targetFile.absolutePath)
    }

    /**
     * Executes collapse (if requested) and screencap in a SINGLE su process session.
     */
    fun takeRootScreencap(context: Context, method: String, collapseFirst: Boolean = false): Boolean {
        val (targetFile, filePath) = getScreenshotFilePath()
        val parentDir = targetFile.parentFile?.absolutePath ?: "/storage/emulated/0/Pictures/Screenshots"

        val scriptBuilder = StringBuilder()
        if (collapseFirst) {
            scriptBuilder.append("cmd statusbar collapse\n")
            scriptBuilder.append("sleep 0.45\n")
        }

        if (method == "keyevent") {
            // Native UI/Animation triggers:
            // 1. cmd accessibility global-action 9 (Native system screenshot with animation & preview UI)
            // 2. input keycombination 26 25 (Hardware Power+VolDown)
            // 3. input keyevent 120 (SYSRQ)
            // 4. Fallback directly to screencap -p so capture NEVER fails
            scriptBuilder.append(
                """
                cmd accessibility global-action 9 2>/dev/null || \
                input keycombination 26 25 2>/dev/null || \
                input keyevent 120 2>/dev/null || \
                (mkdir -p '$parentDir' && screencap -p '$filePath' && chmod 666 '$filePath' && chown media_rw:media_rw '$filePath' 2>/dev/null && restorecon '$filePath' 2>/dev/null && am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d 'file://$filePath' 2>/dev/null)
                """.trimIndent()
            )
        } else {
            // Direct screencap binary execution (Works everywhere silently)
            scriptBuilder.append("mkdir -p '$parentDir'\n")
            scriptBuilder.append("screencap -p '$filePath'\n")
            scriptBuilder.append("chmod 666 '$filePath'\n")
            scriptBuilder.append("chown media_rw:media_rw '$filePath' 2>/dev/null || true\n")
            scriptBuilder.append("restorecon '$filePath' 2>/dev/null || true\n")
            scriptBuilder.append("am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d 'file://$filePath' 2>/dev/null || true\n")
        }

        val success = runRootCommand(scriptBuilder.toString())

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

        return success
    }
}
