package com.example.aplikasitokosembakoarkhan

import android.content.Context
import com.example.aplikasitokosembakoarkhan.data.*
import com.example.aplikasitokosembakoarkhan.data.repository.BackupRepository

interface AppContainer {
    val productDao: ProductDao
    val categoryDao: CategoryDao
    val unitDao: UnitDao
    val customerDao: CustomerDao
    val transactionDao: TransactionDao
    val debtTransactionDao: DebtTransactionDao
    val saleDao: SaleDao
    val expenseDao: ExpenseDao
    val backupRepository: BackupRepository
}

class AppDataContainer(private val context: Context) : AppContainer {

    private val database: InventoryDatabase by lazy {
        InventoryDatabase.getDatabase(context)
    }

    override val productDao: ProductDao by lazy { database.productDao() }
    override val categoryDao: CategoryDao by lazy { database.categoryDao() }
    override val unitDao: UnitDao by lazy { database.unitDao() }
    override val customerDao: CustomerDao by lazy { database.customerDao() }
    override val transactionDao: TransactionDao by lazy { database.transactionDao() }
    override val debtTransactionDao: DebtTransactionDao by lazy { database.debtTransactionDao() }
    override val saleDao: SaleDao by lazy { database.saleDao() }
    override val expenseDao: ExpenseDao by lazy { database.expenseDao() }

    // FIX: Memanggil konstruktor dengan 2 parameter
    override val backupRepository: BackupRepository by lazy {
        BackupRepository(context, database)
    }
}