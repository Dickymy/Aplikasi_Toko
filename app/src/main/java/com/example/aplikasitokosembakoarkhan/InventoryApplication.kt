package com.example.aplikasitokosembakoarkhan

import android.app.Application

class InventoryApplication : Application() {

    // Properti ini WAJIB ada agar bisa diakses AppViewModelProvider
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
    }
}