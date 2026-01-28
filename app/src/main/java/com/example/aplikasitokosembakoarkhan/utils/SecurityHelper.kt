package com.example.aplikasitokosembakoarkhan.utils

import android.content.Context
import android.content.SharedPreferences

object SecurityHelper {
    private const val PREF_NAME = "toko_security_prefs"
    private const val KEY_PIN = "app_pin"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isPinSet(context: Context): Boolean {
        val pin = getPrefs(context).getString(KEY_PIN, null)
        return !pin.isNullOrEmpty()
    }

    fun setPin(context: Context, pin: String) {
        // GANTI .apply() JADI .commit() AGAR TERSIMPAN SEBELUM RESTART
        getPrefs(context).edit().putString(KEY_PIN, pin).commit()
    }

    fun validatePin(context: Context, inputPin: String): Boolean {
        val storedPin = getPrefs(context).getString(KEY_PIN, "")
        return storedPin == inputPin
    }

    fun removePin(context: Context) {
        // GANTI .apply() JADI .commit()
        getPrefs(context).edit().remove(KEY_PIN).commit()
    }
}