package com.example.aplikasitokosembakoarkhan.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val barcode: String,
    val buyPrice: Double,
    val sellPrice: Double,
    val stock: Double, // Wajib Double
    val category: String,
    val unit: String,
    val imagePath: String? = null,
    val expireDate: Long = 0,
    val wholesaleQty: Double = 0.0, // Wajib Double agar konsisten
    val wholesalePrice: Double = 0.0
)