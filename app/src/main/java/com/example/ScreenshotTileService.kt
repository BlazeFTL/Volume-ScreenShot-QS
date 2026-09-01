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
            // If the user selected the native UI simulation and Accessibility service is already enabled,
            // using the Accessibility Global Action provides the authentic native system screenshot UI/animation!
            if (prefs.rootMethod == "keyevent" && ScreenshotAccessibilityService.isEnabled()) {
                ScreenshotAccessibilityService.collapseNotificationShade()
                Handler(Looper.getMainLooper()).postDelayed({
                    ScreenshotAccessibilityService.takeScreenshot()
                }, 450)
                return
            }

            // Otherwise execute root command
            Thread {
                val success = ShellUtils.takeRootScreencap(
                    context = applicationContext,
                    method = prefs.rootMethod,
                    collapseFirst = true
                )
                Handler(Looper.getMainLooper()).post {
                    if (!success) {
                        if (ScreenshotAccessibilityService.isEnabled()) {
                            ScreenshotAccessibilityService.collapseNotificationShade()
                            Handler(Looper.getMainLooper()).postDelayed({
                                ScreenshotAccessibilityService.takeScreenshot()
                            }, 450)
                        } else {
                            showToast("Root capture failed! Please open app to configure.")
                            openApp()
                        }
                    }
                }
            }.start()
        } else {
            if (ScreenshotAccessibilityService.isEnabled()) {
                // Collapse notification panel first, then take screenshot of underlying screen/app
                ScreenshotAccessibilityService.collapseNotificationShade()
                Handler(Looper.getMainLooper()).postDelayed({
                    ScreenshotAccessibilityService.takeScreenshot()
                }, 450)
            } else {
                showToast("Please enable Accessibility Helper service in the app first!")
                openApp()
            }
        }
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
