package com.example.aplikasitokosembakoarkhan

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("TokoArkhanPrefs", Context.MODE_PRIVATE)

    // Property Toko
    var storeName: String
        get() = prefs.getString("store_name", "Toko Sembako Arkhan") ?: "Toko Sembako Arkhan"
        set(value) = prefs.edit().putString("store_name", value).apply()

    var storeAddress: String
        get() = prefs.getString("store_address", "Alamat Toko Belum Diatur") ?: "Alamat Toko Belum Diatur"
        set(value) = prefs.edit().putString("store_address", value).apply()

    var storePhone: String
        get() = prefs.getString("store_phone", "08xx-xxxx-xxxx") ?: "08xx-xxxx-xxxx"
        set(value) = prefs.edit().putString("store_phone", value).apply()

    // Property Footer Struk (BARU DITAMBAHKAN)
    var receiptFooter: String
        get() = prefs.getString("receipt_footer", "Terima Kasih atas Kunjungan Anda") ?: "Terima Kasih"
        set(value) = prefs.edit().putString("receipt_footer", value).apply()

    // Property Printer
    var printerAddress: String
        get() = prefs.getString("printer_address", "") ?: ""
        set(value) = prefs.edit().putString("printer_address", value).apply()
}