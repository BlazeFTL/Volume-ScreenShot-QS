package com.example

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
