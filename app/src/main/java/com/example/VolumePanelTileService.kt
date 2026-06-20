package com.example

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.service.quicksettings.TileService

class VolumePanelTileService : TileService() {
    override fun onClick() {
        super.onClick()
        
        // Collapse the system QS panel to display the volume slider properly
        try {
            val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            sendBroadcast(closeIntent)
        } catch (e: Exception) {
            // Ignore
        }

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
    }
}
