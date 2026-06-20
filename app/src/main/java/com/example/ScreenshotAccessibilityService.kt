package com.example

import android.accessibilityservice.AccessibilityService
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
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                service.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            } else {
                false
            }
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

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
