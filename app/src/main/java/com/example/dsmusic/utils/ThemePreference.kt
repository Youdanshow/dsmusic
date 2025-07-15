package com.example.dsmusic.utils

import android.content.Context

/**
 * Helper object to persist the selected theme using [android.content.SharedPreferences].
 */
object ThemePreference {
    private const val PREF_NAME = "user_prefs"
    private const val KEY_THEME = "selected_theme"

    /**
     * Save the chosen theme index.
     */
    fun saveTheme(context: Context, theme: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME, theme).apply()
    }

    /**
     * Retrieve the last selected theme. Defaults to `1` if none was stored.
     */
    fun loadTheme(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_THEME, 1)
    }
}
