package com.example.aplikasitokosembakoarkhan.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val items: String, // Format: "Nama x Qty, ..."
    val totalAmount: Double,
    val payAmount: Double,      // <--- TAMBAHAN BARU
    val changeAmount: Double,   // <--- TAMBAHAN BARU
    val date: Long,
    val paymentMethod: String,
    val customerName: String
)