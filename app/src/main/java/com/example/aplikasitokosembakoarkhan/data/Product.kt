package com.example.aplikasitokosembakoarkhan.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val buyPrice: Double,
    val sellPrice: Double,
    val stock: Int,
    val barcode: String,
    val unit: String,
    val imagePath: String? = null,
    val expireDate: Long = 0 // <--- KOLOM BARU (0 = Tidak ada expired)
)