package com.example.aplikasitokosembakoarkhan.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Pastikan semua Entity terdaftar di sini
@Database(
    entities = [
        Product::class,
        Category::class,
        UnitModel::class,
        Customer::class,
        Transaction::class,
        DebtTransaction::class,
        Sale::class,
        Expense::class
    ],
    version = 2, // Naikkan versi jika perlu (tapi sebaiknya uninstall app dulu)
    exportSchema = false
)
abstract class InventoryDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun unitDao(): UnitDao
    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun debtTransactionDao(): DebtTransactionDao
    abstract fun saleDao(): SaleDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var Instance: InventoryDatabase? = null

        fun getDatabase(context: Context): InventoryDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, InventoryDatabase::class.java, "inventory_database")
                    // Mengizinkan penghancuran data lama jika skema berubah (Untuk Dev Mode)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}