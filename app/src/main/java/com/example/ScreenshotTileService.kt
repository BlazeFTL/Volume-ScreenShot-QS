package com.example

import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class ScreenshotTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        try {
            val tile = qsTile
            if (tile != null) {
                tile.state = Tile.STATE_INACTIVE
                tile.updateTile()
            }
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    override fun onClick() {
        super.onClick()
        val prefs = PrefsManager(this)

        // Ensure the tile panel collapse is initiated immediately by TileService as well
        dismissTileWindow()

        if (prefs.useRoot) {
            if (prefs.rootMethod == "keyevent" && ScreenshotAccessibilityService.isEnabled()) {
                ScreenshotAccessibilityService.collapseAndTakeScreenshot(600L)
                return
            }

            // Root Mode: execute collapse and screencap
            Thread {
                val success = ShellUtils.takeRootScreencap(
                    context = applicationContext,
                    method = prefs.rootMethod,
                    collapseFirst = true
                )
                Handler(Looper.getMainLooper()).post {
                    if (!success) {
                        if (ScreenshotAccessibilityService.isEnabled()) {
                            ScreenshotAccessibilityService.collapseAndTakeScreenshot(600L)
                        } else {
                            showToast("Root capture failed! Please open app to configure.")
                            openApp()
                        }
                    }
                }
            }.start()
        } else {
            if (ScreenshotAccessibilityService.isEnabled()) {
                // Accessibility Mode (Single Click):
                // 1. Closes the Quick Settings panel completely
                // 2. Waits for the collapse transition
                // 3. Captures a clean screenshot of the underlying app / screen
                ScreenshotAccessibilityService.collapseAndTakeScreenshot(600L)
            } else {
                showToast("Please enable Accessibility Helper service in the app first!")
                openApp()
            }
        }
    }

    private fun dismissTileWindow() {
        try {
            @Suppress("DEPRECATION")
            val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            sendBroadcast(closeIntent)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
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
