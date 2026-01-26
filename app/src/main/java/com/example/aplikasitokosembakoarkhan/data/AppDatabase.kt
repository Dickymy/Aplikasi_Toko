package com.example.aplikasitokosembakoarkhan.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Product::class, Sale::class, Expense::class, Customer::class, UnitModel::class, Category::class], // Tambahkan Category::class
    version = 3, // Naik ke versi 3
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun customerDao(): CustomerDao
    abstract fun unitDao(): UnitDao
    abstract fun categoryDao(): CategoryDao // Tambahkan ini

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migrasi dari 1 ke 2 (History)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE products ADD COLUMN category TEXT NOT NULL DEFAULT 'Umum'")
            }
        }

        // Migrasi dari 2 ke 3 (Menambahkan Tabel Categories)
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)")
                // Isi kategori default agar tidak kosong
                database.execSQL("INSERT INTO categories (name) VALUES ('Makanan'), ('Minuman'), ('Sembako'), ('Rokok'), ('Obat'), ('Alat Tulis')")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "toko_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // Tambahkan migrasi baru
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}