package com.example.reloop_technologies_mobile_app.database

import android.content.Context

object UserManager {
    private const val PREF_NAME = "reloop_user_prefs"
    private const val KEY_UID = "user_uid"
    private const val KEY_NAME = "user_name"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_PHONE = "user_phone"
    private const val KEY_ADDRESS = "user_address"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    fun saveUserProfile(context: Context, userData: UserData) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_UID, userData.uid)
            putString(KEY_NAME, userData.name)
            putString(KEY_EMAIL, userData.email)
            putString(KEY_PHONE, userData.phone)
            putString(KEY_ADDRESS, userData.address)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun getUserProfile(context: Context): UserData {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return UserData(
            uid = prefs.getString(KEY_UID, "") ?: "",
            name = prefs.getString(KEY_NAME, "Alex Morgan") ?: "Alex Morgan",
            email = prefs.getString(KEY_EMAIL, "alex.morgan@example.com") ?: "alex.morgan@example.com",
            phone = prefs.getString(KEY_PHONE, "+1 555-0192") ?: "+1 555-0192",
            address = prefs.getString(KEY_ADDRESS, "123 Eco Green St, Silicon Valley") ?: "123 Eco Green St, Silicon Valley"
        )
    }

    fun isLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
