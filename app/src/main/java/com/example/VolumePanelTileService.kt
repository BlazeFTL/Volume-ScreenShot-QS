package com.example

import android.service.quicksettings.TileService
import android.media.AudioManager
import android.content.Context
import android.util.Log

class VolumePanelTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
        Log.d("VolumePanelTile", "Volume panel shown.")
    }
}
