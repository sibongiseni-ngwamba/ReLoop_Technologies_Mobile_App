package com.example.reloop_technologies_mobile_app.database

import android.content.Context

object UserManager {
    private const val PREF_NAME = "reloop_user_prefs"
    private const val KEY_UID = "user_uid"
    private const val KEY_NAME = "user_name"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_PHONE = "user_phone"
    private const val KEY_ADDRESS = "user_address"
    private const val KEY_ROLE = "user_role"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    fun saveUserProfile(context: Context, userData: UserData) {
        if (userData.uid.isBlank()) return
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_UID, userData.uid)
            putString(KEY_NAME, userData.name)
            putString(KEY_EMAIL, userData.email)
            putString(KEY_PHONE, userData.phone)
            putString(KEY_ADDRESS, userData.address)
            putString(KEY_ROLE, userData.role)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun getUserProfile(context: Context, expectedUid: String? = null): UserData {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val uid = prefs.getString(KEY_UID, "").orEmpty()
        if (expectedUid != null && uid != expectedUid) return UserData(uid = expectedUid)
        return UserData(
            uid = uid,
            name = prefs.getString(KEY_NAME, "").orEmpty(),
            email = prefs.getString(KEY_EMAIL, "").orEmpty(),
            phone = prefs.getString(KEY_PHONE, "").orEmpty(),
            address = prefs.getString(KEY_ADDRESS, "").orEmpty(),
            role = prefs.getString(KEY_ROLE, "resident").orEmpty().ifEmpty { "resident" }
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
