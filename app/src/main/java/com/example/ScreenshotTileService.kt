package com.example

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.TileService
import android.widget.Toast

class ScreenshotTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val prefs = PrefsManager(this)

        if (prefs.useRoot) {
            // Collapse notification panel so we don't capture the expanded QS panel
            collapseAndExecute {
                // Short delay to allow the panel to animate closed before screenshot
                Handler(Looper.getMainLooper()).postDelayed({
                    val success = if (prefs.rootMethod == "keyevent") {
                        ShellUtils.runRootCommand("input keyevent 120")
                    } else {
                        val dirPath = "/sdcard/Pictures/Screenshots"
                        ShellUtils.runRootCommand("mkdir -p $dirPath")
                        val path = "$dirPath/Screenshot_${System.currentTimeMillis()}.png"
                        ShellUtils.runRootCommand("screencap -p $path")
                    }

                    if (!success) {
                        // Fallback to accessibility service if root fails
                        if (ScreenshotAccessibilityService.isEnabled()) {
                            ScreenshotAccessibilityService.takeScreenshot()
                        } else {
                            showToast("Root command failed! Please open app to configure.")
                            openApp()
                        }
                    }
                }, 500)
            }
        } else {
            if (ScreenshotAccessibilityService.isEnabled()) {
                collapseAndExecute {
                    Handler(Looper.getMainLooper()).postDelayed({
                        ScreenshotAccessibilityService.takeScreenshot()
                    }, 500)
                }
            } else {
                showToast("Please open the app to enable Accessibility or Root screenshots!")
                openApp()
            }
        }
    }

    private inline fun collapseAndExecute(crossinline block: () -> Unit) {
        // We trigger system collapse
        try {
            val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            sendBroadcast(closeIntent)
        } catch (e: Exception) {
            // Ignore
        }
        block()
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } catch (e: Exception) {
            startActivity(intent)
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }
}
