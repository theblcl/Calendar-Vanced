package com.example.calendarapp.utils

import android.content.Context
import android.os.Build

object EPaperUtils {
    private const val PREFS_NAME = "epaper_settings"
    private const val KEY_EPAPER_MODE = "epaper_mode_enabled"

    fun isEPaperMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_EPAPER_MODE, detectEPaperDevice())
    }

    fun setEPaperMode(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_EPAPER_MODE, enabled).apply()
    }

    fun detectEPaperDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()

        return when {
            manufacturer.contains("onyx") -> true
            manufacturer.contains("remarkable") -> true
            model.contains("kindle") -> true
            model.contains("mp01") -> true  // Your e-ink device
            else -> false
        }
    }

    fun applyTheme(context: Context) {
        // This will be called to restart the activity with the right theme
        if (context is android.app.Activity) {
            context.recreate()
        }
    }
}