package com.example.aplikasitokosembakoarkhan.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,      // ID Barang (PENTING untuk Laporan)
    val productName: String, // Nama Barang (Snapshot saat transaksi)
    val quantity: Int,       // Jumlah Beli
    val totalPrice: Double,  // Total Harga Jual (Harga x Qty)
    val date: Long,          // Tanggal Transaksi

    // Opsional: Menyimpan harga modal saat transaksi terjadi (untuk akurasi laporan laba jika harga modal berubah nanti)
    val capitalPrice: Double = 0.0
)