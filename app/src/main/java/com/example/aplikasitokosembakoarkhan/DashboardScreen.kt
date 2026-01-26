package com.example.aplikasitokosembakoarkhan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: ProductViewModel,
    onNavigateToSales: () -> Unit,
    onNavigateToStock: () -> Unit
) {
    val todaySales by viewModel.todaySales.collectAsState()
    val lowStockList by viewModel.lowStockProducts.collectAsState()
    val expiringList by viewModel.expiringProducts.collectAsState()

    // Hitung Ringkasan Hari Ini
    val todayRevenue = todaySales.sumOf { it.totalPrice }
    val todayCount = todaySales.size

    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Halo, Kasir!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            text = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(Date()),
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- KARTU RINGKASAN HARI INI ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Kartu Omzet
            DashboardCard(
                title = "Omzet Hari Ini",
                value = formatRupiah(todayRevenue),
                icon = Icons.Default.AttachMoney,
                color = Color(0xFF2E7D32), // Hijau
                modifier = Modifier.weight(1f)
            )
            // Kartu Transaksi
            DashboardCard(
                title = "Transaksi",
                value = "$todayCount",
                icon = Icons.Default.ShoppingCart,
                color = Color(0xFF1565C0), // Biru
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- TOMBOL PINTAS (QUICK ACTION) ---
        Text("Menu Cepat", fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onNavigateToSales,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.AddShoppingCart, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kasir")
            }
            OutlinedButton(
                onClick = onNavigateToStock,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Inventory, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stok")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- DAFTAR PERINGATAN (ALERT) ---
        Text("Perlu Perhatian", fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // 1. Peringatan Stok Menipis
            if (lowStockList.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), // Merah Muda
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, null, tint = Color.Red)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Stok Menipis (${lowStockList.size})", fontWeight = FontWeight.Bold, color = Color.Red)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            lowStockList.take(5).forEach { product ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• ${product.name}", fontSize = 14.sp)
                                    Text("${product.stock} ${product.unit}", fontWeight = FontWeight.Bold, color = Color.Red)
                                }
                            }
                            if (lowStockList.size > 5) Text("...dan ${lowStockList.size - 5} lainnya", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // 2. Peringatan Barang Kadaluwarsa
            if (expiringList.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), // Oranye Muda
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, null, tint = Color(0xFFE65100))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Hampir Expired (${expiringList.size})", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            expiringList.take(5).forEach { product ->
                                val dateStr = SimpleDateFormat("dd MMM", Locale("id")).format(Date(product.expireDate))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• ${product.name}", fontSize = 14.sp)
                                    Text("Exp: $dateStr", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                }
                            }
                        }
                    }
                }
            }

            // Jika Aman Semua
            if (lowStockList.isEmpty() && expiringList.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Inventory, null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stok Aman! Tidak ada peringatan.", color = Color(0xFF2E7D32))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontSize = 12.sp, color = Color.Gray)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}