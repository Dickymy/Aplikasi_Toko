package com.example.aplikasitokosembakoarkhan.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val capitalPrice: Double,
    val totalPrice: Double,
    val date: Long,
    val customerName: String = "Umum" // Field Baru
)