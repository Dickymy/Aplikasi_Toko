package com.example.aplikasitokosembakoarkhan

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("toko_prefs", Context.MODE_PRIVATE)

    var storeName: String
        get() = prefs.getString("store_name", "Toko Arkhan") ?: "Toko Arkhan"
        set(value) = prefs.edit().putString("store_name", value).apply()

    var storeAddress: String
        get() = prefs.getString("store_address", "") ?: ""
        set(value) = prefs.edit().putString("store_address", value).apply()

    var storePhone: String
        get() = prefs.getString("store_phone", "") ?: ""
        set(value) = prefs.edit().putString("store_phone", value).apply()

    var receiptFooter: String
        get() = prefs.getString("receipt_footer", "Terima Kasih!") ?: "Terima Kasih!"
        set(value) = prefs.edit().putString("receipt_footer", value).apply()

    var printerAddress: String
        get() = prefs.getString("printer_address", "") ?: ""
        set(value) = prefs.edit().putString("printer_address", value).apply()

    // --- PERBAIKAN: GUNAKAN COMMIT() DISINI ---
    var lockedMenus: Set<String>
        get() = prefs.getStringSet("locked_menus", setOf("report", "expense", "settings")) ?: setOf("report", "expense", "settings")
        set(value) {
            val editor = prefs.edit()
            editor.remove("locked_menus")
            editor.commit() // Pastikan hapus dulu tersimpan
            editor.putStringSet("locked_menus", value).commit() // Pastikan data baru tersimpan SEKARANG
        }
}