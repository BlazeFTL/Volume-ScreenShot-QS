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
                tile.state = Tile.STATE_ACTIVE
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
            // Collapse notification panel before screenshot
            collapseAndExecute(prefs) {
                // Short delay to allow the panel to animate closed before screenshot
                Handler(Looper.getMainLooper()).postDelayed({
                    Thread {
                        var success = false
                        try {
                            success = if (prefs.rootMethod == "keyevent") {
                                ShellUtils.runRootCommand("input keyevent 120")
                            } else {
                                val dirPath = "/sdcard/Pictures/Screenshots"
                                ShellUtils.runRootCommand("mkdir -p $dirPath")
                                val path = "$dirPath/Screenshot_${System.currentTimeMillis()}.png"
                                ShellUtils.runRootCommand("screencap -p $path")
                            }
                        } catch (t: Throwable) {
                            success = false
                        }

                        if (!success) {
                            Handler(Looper.getMainLooper()).post {
                                // Fallback to accessibility service if root fails
                                if (ScreenshotAccessibilityService.isEnabled()) {
                                    ScreenshotAccessibilityService.takeScreenshot()
                                } else {
                                    showToast("Root capture failed! Please configure within the application.")
                                    openApp()
                                }
                            }
                        }
                    }.start()
                }, 500)
            }
        } else {
            if (ScreenshotAccessibilityService.isEnabled()) {
                collapseAndExecute(prefs) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        ScreenshotAccessibilityService.takeScreenshot()
                    }, 500)
                }
            } else {
                showToast("Please enable Accessibility or Root screenshots in the app first!")
                openApp()
            }
        }
    }

    private fun collapseAndExecute(prefs: PrefsManager, block: () -> Unit) {
        var collapsed = false
        // 1. Accessibility collapse first (incredibly reliable and official)
        if (ScreenshotAccessibilityService.isEnabled()) {
            collapsed = ScreenshotAccessibilityService.collapseNotificationShade()
        }
        // 2. Root collapse command next (flawless if root is enabled)
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
