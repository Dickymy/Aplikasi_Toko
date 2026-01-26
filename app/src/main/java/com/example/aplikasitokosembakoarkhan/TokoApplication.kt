package com.example.aplikasitokosembakoarkhan

import android.app.Application
import com.example.aplikasitokosembakoarkhan.data.AppDatabase

class TokoApplication : Application() {
    // Menginisialisasi database secara LAZY (hanya dibuat saat pertama kali butuh)
    // agar aplikasi tidak berat saat start-up.
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }
}