package com.example.aplikasitokosembakoarkhan.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.aplikasitokosembakoarkhan.data.UnitModel
import com.example.aplikasitokosembakoarkhan.data.UnitDao


// Update 1: Tambahkan Sale::class di entities dan ubah version jadi 2
@Database(entities = [Product::class, Sale::class, UnitModel::class, Customer::class, Expense::class], version = 10, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun unitDao(): UnitDao
    abstract fun customerDao(): CustomerDao
    abstract fun expenseDao(): ExpenseDao // <--- Tambahkan ini

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "toko_sembako_database"
                )
                    .fallbackToDestructiveMigration() // Hapus data lama jika struktur berubah
                    .build()
                    .also { Instance = it }
            }
        }
    }
}