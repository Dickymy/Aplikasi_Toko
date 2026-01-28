package com.example.aplikasitokosembakoarkhan

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.aplikasitokosembakoarkhan.InventoryApplication

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            InventoryViewModel(
                inventoryApplication().container.productDao,
                inventoryApplication().container.categoryDao,
                inventoryApplication().container.unitDao,
                inventoryApplication().container.customerDao,
                inventoryApplication().container.debtTransactionDao
            )
        }
        initializer {
            SalesViewModel(
                inventoryApplication().container.productDao,
                inventoryApplication().container.transactionDao
            )
        }
        initializer {
            SettingsViewModel(
                inventoryApplication(),
                inventoryApplication().container.backupRepository
            )
        }

        // FIX: ReportViewModel sekarang minta TransactionDao DAN ExpenseDao
        initializer {
            ReportViewModel(
                inventoryApplication().container.transactionDao,
                inventoryApplication().container.expenseDao
            )
        }
    }
}

fun CreationExtras.inventoryApplication(): InventoryApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as InventoryApplication)