package com.example

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class ScreenshotLongPressActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PrefsManager(this)

        // Holding the tile takes a screenshot of the Quick Settings panel directly (WITHOUT closing it)
        if (prefs.useRoot) {
            Thread {
                val success = ShellUtils.takeRootScreencap(this, prefs.rootMethod)
                Handler(Looper.getMainLooper()).post {
                    if (success) {
                        Toast.makeText(applicationContext, "Quick Settings screenshot captured!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(applicationContext, "Root capture failed!", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                }
            }.start()
        } else {
            if (ScreenshotAccessibilityService.isEnabled()) {
                val success = ScreenshotAccessibilityService.takeScreenshot()
                if (success) {
                    Toast.makeText(applicationContext, "Quick Settings screenshot captured!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, "Could not capture screenshot.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(applicationContext, "Please enable Accessibility Helper service in the app!", Toast.LENGTH_LONG).show()
            }
            finish()
        }
    }
}
