package com.example

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.service.quicksettings.TileService

class VolumePanelTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val prefs = PrefsManager(this)
        
        // Collapse the system QS panel to display the volume slider properly
        collapseNotificationShade(prefs)

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
    }

    private fun collapseNotificationShade(prefs: PrefsManager) {
        var collapsed = false
        // 1. Accessibility collapse
        if (ScreenshotAccessibilityService.isEnabled()) {
            collapsed = ScreenshotAccessibilityService.collapseNotificationShade()
        }
        // 2. Root collapse command
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
    }
}
