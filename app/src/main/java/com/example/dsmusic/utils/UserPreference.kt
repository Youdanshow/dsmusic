package com.example.dsmusic.utils

import android.content.Context

/**
 * Helper to persist the user's first and last name using [android.content.SharedPreferences].
 * Only the first name is required and will be displayed in the interface.
 */
object UserPreference {
    private const val PREF_NAME = "user_prefs"
    private const val KEY_FIRST_NAME = "first_name"
    private const val KEY_LAST_NAME = "last_name"

    /** Save user names. */
    fun saveUser(context: Context, firstName: String, lastName: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_FIRST_NAME, firstName)
            .putString(KEY_LAST_NAME, lastName)
            .apply()
    }

    /** Retrieve stored first name or null if absent. */
    fun loadFirstName(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_FIRST_NAME, null)
    }

    /** Retrieve stored last name or null if absent. */
    fun loadLastName(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_NAME, null)
    }
}
