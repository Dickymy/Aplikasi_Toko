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

@Dao
interface DebtTransactionDao {
    @Query("SELECT * FROM debt_transactions WHERE customerId = :custId ORDER BY date DESC")
    fun getTransactionsByCustomer(custId: Int): Flow<List<DebtTransaction>>
    @Insert
    suspend fun insertTransaction(transaction: DebtTransaction)
    @Query("DELETE FROM debt_transactions WHERE customerId = :custId")
    suspend fun deleteAllTransactionsByCustomer(custId: Int)
}

// --- DATABASE UPDATE ---
@Database(
    // PERBAIKAN: Tambahkan Transaction::class ke dalam entities
    entities = [
        Product::class,
        Sale::class,
        Transaction::class, // <--- TAMBAHAN PENTING
        Category::class,
        UnitModel::class,
        Expense::class,
        Customer::class,
        DebtTransaction::class
    ],
    version = 7, // NAIKKAN VERSI KE 7 agar ter-update otomatis
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun transactionDao(): TransactionDao // <--- TAMBAHAN PENTING
    abstract fun categoryDao(): CategoryDao
    abstract fun unitDao(): UnitDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun customerDao(): CustomerDao
    abstract fun debtTransactionDao(): DebtTransactionDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE sales ADD COLUMN customerName TEXT NOT NULL DEFAULT 'Umum'") } }
        val MIGRATION_2_3 = object : Migration(2, 3) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE customers ADD COLUMN hasHistory INTEGER NOT NULL DEFAULT 0"); db.execSQL("CREATE TABLE IF NOT EXISTS `debt_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `customerId` INTEGER NOT NULL, `type` TEXT NOT NULL, `amount` REAL NOT NULL, `date` INTEGER NOT NULL)") } }
        val MIGRATION_3_4 = object : Migration(3, 4) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE categories ADD COLUMN isPriority INTEGER NOT NULL DEFAULT 0") } }
        val MIGRATION_4_5 = object : Migration(4, 5) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE products ADD COLUMN description TEXT NOT NULL DEFAULT ''") } }
        val MIGRATION_5_6 = object : Migration(5, 6) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE unit_models ADD COLUMN isPriority INTEGER NOT NULL DEFAULT 0") } }

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "toko_database")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration() // Ini akan menghapus data lama JIKA migrasi gagal, aman untuk dev
                    .build()
                    .also { Instance = it }
            }
        }
    }
}