package com.example.aplikasitokosembakoarkhan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    reportViewModel: ReportViewModel = viewModel(factory = AppViewModelProvider.Factory),
    inventoryViewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val todaySales by reportViewModel.todaySales.collectAsState()
    val lowStock by inventoryViewModel.lowStockProducts.collectAsState()
    val expiringProducts by inventoryViewModel.expiringProducts.collectAsState() // Update: Ambil Data Expired

    val totalToday = todaySales.sumOf { it.totalPrice }
    val countToday = todaySales.size

    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Halo, Admin!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // CARD RINGKASAN HARI INI
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Penjualan Hari Ini", fontSize = 14.sp, color = Color.Gray)
                Text(formatRupiah(totalToday), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                Spacer(modifier = Modifier.height(8.dp))
                Text("$countToday Transaksi", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION PERINGATAN (STOK & EXPIRED) ---
        Text("Peringatan Toko", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 1. STOK MENIPIS
            if (lowStock.isNotEmpty()) {
                item {
                    Text("Stok Menipis:", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                }
                items(lowStock) { product ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, null, tint = Color.Red)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(product.name, fontWeight = FontWeight.Bold)
                                Text("Sisa: ${product.stock} ${product.unit}", color = Color.Red, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 2. BARANG AKAN EXPIRED (UPDATE BARU)
            if (expiringProducts.isNotEmpty()) {
                item {
                    Text("Akan Segera Expired (30 Hari):", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                }
                items(expiringProducts) { product ->
                    val expDate = SimpleDateFormat("dd MMM yyyy", Locale("id")).format(Date(product.expireDate))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)) // Warna Orange
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.WarningAmber, null, tint = Color(0xFFEF6C00))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(product.name, fontWeight = FontWeight.Bold)
                                Text("Exp: $expDate", color = Color(0xFFEF6C00), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            if (lowStock.isEmpty() && expiringProducts.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Aman! Stok & Expired aman terkendali.", color = Color(0xFF2E7D32))
                        }
                    }
                }
            }
        }
    }
}