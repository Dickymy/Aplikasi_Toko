package com.example.aplikasitokosembakoarkhan.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

// --- TAMBAHKAN DAO BARU DI SINI (Internal File) ---
@Dao
interface DebtTransactionDao {
    @Query("SELECT * FROM debt_transactions WHERE customerId = :custId ORDER BY date DESC")
    fun getTransactionsByCustomer(custId: Int): Flow<List<DebtTransaction>>

    @Insert
    suspend fun insertTransaction(transaction: DebtTransaction)

    @Query("DELETE FROM debt_transactions WHERE customerId = :custId")
    suspend fun deleteAllTransactionsByCustomer(custId: Int)
}

// --- UPDATE DATABASE ---
@Database(
    entities = [Product::class, Sale::class, Category::class, UnitModel::class, Expense::class, Customer::class, DebtTransaction::class], // Tambah DebtTransaction
    version = 3, // Naik ke Versi 3
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun categoryDao(): CategoryDao
    abstract fun unitDao(): UnitDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun customerDao(): CustomerDao
    abstract fun debtTransactionDao(): DebtTransactionDao // Tambah ini

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        // Migrasi 1->2 (Yang lama)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE sales ADD COLUMN customerName TEXT NOT NULL DEFAULT 'Umum'")
            }
        }

        // Migrasi 2->3 (Baru: Tambah kolom hasHistory & Tabel debt_transactions)
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Update Customer Table
                database.execSQL("ALTER TABLE customers ADD COLUMN hasHistory INTEGER NOT NULL DEFAULT 0")
                // 2. Create Debt Transaction Table
                database.execSQL("CREATE TABLE IF NOT EXISTS `debt_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `customerId` INTEGER NOT NULL, `type` TEXT NOT NULL, `amount` REAL NOT NULL, `date` INTEGER NOT NULL)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "toko_database")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // Tambahkan migrasi baru
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}