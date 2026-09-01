package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class ScreenshotAccessibilityService : AccessibilityService() {
    
    companion object {
        @Volatile
        private var instance: ScreenshotAccessibilityService? = null
        
        fun isEnabled(): Boolean {
            return instance != null
        }
        
        fun takeScreenshot(): Boolean {
            val service = instance ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                service.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            } else {
                false
            }
        }

        fun collapseNotificationShade(): Boolean {
            val service = instance ?: return false
            var dismissed = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dismissed = service.performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
            }
            if (!dismissed) {
                // Also trigger BACK or global dismiss
                dismissed = service.performGlobalAction(GLOBAL_ACTION_BACK)
            }
            return dismissed
        }

        /**
         * Cleanly collapses the notification shade and then takes a screenshot
         * after a proper delay for the shade slide-up animation to complete.
         */
        fun collapseAndTakeScreenshot(delayMs: Long = 600L) {
            val service = instance
            if (service == null) return

            // 1. Try Accessibility DISMISS_NOTIFICATION_SHADE (API 31+)
            var shadeDismissed = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                shadeDismissed = service.performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
            }
            
            // 2. Perform BACK action which reliably closes open notification shade / QS panel on all Android versions
            if (!shadeDismissed) {
                service.performGlobalAction(GLOBAL_ACTION_BACK)
            }

            // 3. Also send close system dialogs broadcast as fallback
            try {
                @Suppress("DEPRECATION")
                val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
                service.sendBroadcast(closeIntent)
            } catch (e: Exception) {
                // Ignore
            }

            // 4. Wait for the slide-up animation to completely clear the screen before capturing
            Handler(Looper.getMainLooper()).postDelayed({
                takeScreenshot()
            }, delayMs)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("AccessibilityService", "Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No action needed for events
    }

    override fun onInterrupt() {
        // No action needed
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
