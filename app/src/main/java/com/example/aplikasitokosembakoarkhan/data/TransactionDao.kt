package com.example.aplikasitokosembakoarkhan.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy // <--- INI YANG SERING KETINGGALAN
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    // --- FITUR BARU UNTUK BACKUP ---
    @Query("SELECT * FROM transactions")
    fun getAllTransactionsSync(): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)
}