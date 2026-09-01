package com.example

import android.content.Intent
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

        if (prefs.useRoot) {
            // Collapse notification panel before taking screenshot of underlying screen/app
            collapseAndExecute(prefs) {
                Handler(Looper.getMainLooper()).postDelayed({
                    Thread {
                        val success = ShellUtils.takeRootScreencap(applicationContext, prefs.rootMethod)
                        if (!success) {
                            Handler(Looper.getMainLooper()).post {
                                if (ScreenshotAccessibilityService.isEnabled()) {
                                    ScreenshotAccessibilityService.takeScreenshot()
                                } else {
                                    showToast("Root capture failed! Please open app to configure.")
                                    openApp()
                                }
                            }
                        }
                    }.start()
                }, 550)
            }
        } else {
            if (ScreenshotAccessibilityService.isEnabled()) {
                // Collapse notification panel before taking screenshot of underlying screen/app
                collapseAndExecute(prefs) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        ScreenshotAccessibilityService.takeScreenshot()
                    }, 550)
                }
            } else {
                showToast("Please enable Accessibility Helper service in the app first!")
                openApp()
            }
        }
    }

    private fun collapseAndExecute(prefs: PrefsManager, block: () -> Unit) {
        var collapsed = false
        // 1. Accessibility collapse first (cleanest and official API)
        if (ScreenshotAccessibilityService.isEnabled()) {
            collapsed = ScreenshotAccessibilityService.collapseNotificationShade()
        }
        // 2. Root collapse command next (if root enabled)
        if (!collapsed && prefs.useRoot) {
            Thread {
                ShellUtils.runRootCommand("cmd statusbar collapse")
            }.start()
        }
        // 3. Fallback broadcast
        try {
            @Suppress("DEPRECATION")
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
