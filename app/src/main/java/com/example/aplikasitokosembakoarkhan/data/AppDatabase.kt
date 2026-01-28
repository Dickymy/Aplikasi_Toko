package com.example.aplikasitokosembakoarkhan.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Product::class, Sale::class, Category::class, UnitModel::class, Expense::class, Customer::class],
    version = 2, // Naikkan versi ke 2
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun categoryDao(): CategoryDao
    abstract fun unitDao(): UnitDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun customerDao(): CustomerDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        // Migrasi dari Versi 1 ke 2 (Tambah kolom customerName)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE sales ADD COLUMN customerName TEXT NOT NULL DEFAULT 'Umum'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "toko_database")
                    .addMigrations(MIGRATION_1_2) // Tambahkan migrasi
                    .fallbackToDestructiveMigration() // Jaga-jaga jika migrasi gagal
                    .build()
                    .also { Instance = it }
            }
        }
    }
}