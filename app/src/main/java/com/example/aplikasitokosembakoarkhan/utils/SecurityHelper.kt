package com.example.aplikasitokosembakoarkhan.utils

import android.content.Context

object SecurityHelper {
    private const val PREF_NAME = "toko_security_prefs"
    private const val KEY_PIN = "admin_pin"

    // Simpan PIN Baru
    fun setPin(context: Context, pin: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PIN, pin).apply()
    }

    // Cek apakah PIN sudah diatur?
    fun isPinSet(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(KEY_PIN, null).isNullOrEmpty()
    }

    // Verifikasi PIN (Benar/Salah)
    fun checkPin(context: Context, inputPin: String): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val storedPin = prefs.getString(KEY_PIN, "")
        return storedPin == inputPin
    }

    // Hapus PIN (Matikan Keamanan)
    fun removePin(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_PIN).apply()
    }
}