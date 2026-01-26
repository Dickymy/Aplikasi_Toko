package com.example.aplikasitokosembakoarkhan.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val phoneNumber: String,
    val totalDebt: Double,
    val lastUpdated: Long = System.currentTimeMillis() // <--- KOLOM BARU (Waktu Update)
)