package com.example.aplikasitokosembakoarkhan

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("toko_prefs", Context.MODE_PRIVATE)

    var storeName: String
        get() = prefs.getString("store_name", "Toko Sembako Arkhan") ?: "Toko Sembako Arkhan"
        set(value) = prefs.edit().putString("store_name", value).apply()

    var storeAddress: String
        get() = prefs.getString("store_address", "Jl. Contoh No. 1, Kota") ?: "Jl. Contoh No. 1, Kota"
        set(value) = prefs.edit().putString("store_address", value).apply()

    var storePhone: String
        get() = prefs.getString("store_phone", "0812-3456-7890") ?: "0812-3456-7890"
        set(value) = prefs.edit().putString("store_phone", value).apply()

    // Alamat Bluetooth Printer (MAC Address)
    var printerAddress: String
        get() = prefs.getString("printer_address", "") ?: ""
        set(value) = prefs.edit().putString("printer_address", value).apply()
}