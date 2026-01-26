package com.example.aplikasitokosembakoarkhan.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val barcode: String,
    val buyPrice: Double, // Modal
    val sellPrice: Double, // Jual
    val stock: Int,
    val unit: String,
    val imagePath: String? = null, // Kembalikan fitur gambar

    // Fitur Grosir & Expired Asli
    val expireDate: Long = 0,
    val wholesaleQty: Int = 0,
    val wholesalePrice: Double = 0.0,

    // --- BARU: KATEGORI ---
    val category: String = "Umum"
)