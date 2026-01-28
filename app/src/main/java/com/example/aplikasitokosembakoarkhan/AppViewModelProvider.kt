package com.example.aplikasitokosembakoarkhan

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.aplikasitokosembakoarkhan.data.repository.BackupRepository

object AppViewModelProvider {
    val Factory = viewModelFactory {
        // 1. InventoryViewModel
        initializer {
            val db = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TokoApplication).database
            // Tambahkan db.debtTransactionDao() di akhir
            InventoryViewModel(db.productDao(), db.categoryDao(), db.unitDao(), db.customerDao(), db.debtTransactionDao())
        }

        // 2. SalesViewModel
        initializer {
            val db = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TokoApplication).database
            SalesViewModel(db.productDao(), db.saleDao())
        }

        // 3. ReportViewModel
        initializer {
            val db = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TokoApplication).database
            ReportViewModel(db.saleDao(), db.expenseDao())
        }

        // 4. SettingsViewModel (UPDATE: Tambah Application Context)
        initializer {
            val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TokoApplication)
            val repo = BackupRepository(app)
            SettingsViewModel(app, repo) // Pass 'app' di sini
        }
    }
}