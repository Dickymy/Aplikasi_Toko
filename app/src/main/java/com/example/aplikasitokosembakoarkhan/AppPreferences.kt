package com.example.aplikasitokosembakoarkhan

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("toko_prefs", Context.MODE_PRIVATE)

    var storeName: String
        get() = prefs.getString("store_name", "") ?: ""
        set(value) = prefs.edit().putString("store_name", value).apply()

    var storeAddress: String
        get() = prefs.getString("store_address", "") ?: ""
        set(value) = prefs.edit().putString("store_address", value).apply()

    var storePhone: String
        get() = prefs.getString("store_phone", "") ?: ""
        set(value) = prefs.edit().putString("store_phone", value).apply()

    var receiptFooter: String
        get() = prefs.getString("receipt_footer", "") ?: ""
        set(value) = prefs.edit().putString("receipt_footer", value).apply()

    var printerAddress: String
        get() = prefs.getString("printer_address", "") ?: ""
        set(value) = prefs.edit().putString("printer_address", value).apply()

    // --- FITUR BARU: UKURAN KERTAS ---
    var printerPaperSize: Int
        get() = prefs.getInt("printer_paper_size", 58) // Default 58mm
        set(value) = prefs.edit().putInt("printer_paper_size", value).apply()

    var lockedMenus: Set<String>
        get() = prefs.getStringSet("locked_menus", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("locked_menus", value).apply()
}