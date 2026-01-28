package com.example.aplikasitokosembakoarkhan.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debt_transactions")
data class DebtTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val customerId: Int,
    val type: String, // "Hutang" atau "Bayar"
    val amount: Double,
    val date: Long
)