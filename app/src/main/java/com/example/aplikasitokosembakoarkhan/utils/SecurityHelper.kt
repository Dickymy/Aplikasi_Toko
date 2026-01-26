package com.example.aplikasitokosembakoarkhan.utils

import android.content.Context

object SecurityHelper {
    private const val PREF_NAME = "TokoArkhanSecurity"
    private const val KEY_PIN = "app_pin"

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // Cek apakah PIN sudah diatur (Dipakai di SettingsScreen)
    fun isPinSet(context: Context): Boolean {
        return getPrefs(context).contains(KEY_PIN)
    }

    // Simpan PIN Baru (Dipakai di SettingsScreen)
    fun setPin(context: Context, pin: String) {
        getPrefs(context).edit().putString(KEY_PIN, pin).apply()
    }

    // Cek kecocokan PIN (Dipakai di PinLockScreen)
    fun checkPin(context: Context, inputPin: String): Boolean {
        val savedPin = getPrefs(context).getString(KEY_PIN, "")
        return savedPin == inputPin
    }

    // Hapus PIN (Dipakai di SettingsScreen)
    fun removePin(context: Context) {
        getPrefs(context).edit().remove(KEY_PIN).apply()
    }
}