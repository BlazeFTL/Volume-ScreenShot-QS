package com.example

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("quick_tiles_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USE_ROOT = "use_root"
        private const val KEY_ROOT_METHOD = "root_method" // "screencap" (direct) or "keyevent" (simulated keys)
    }

    var useRoot: Boolean
        get() = prefs.getBoolean(KEY_USE_ROOT, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_ROOT, value).apply()

    var rootMethod: String
        get() = prefs.getString(KEY_ROOT_METHOD, "screencap") ?: "screencap"
        set(value) = prefs.edit().putString(KEY_ROOT_METHOD, value).apply()
}
