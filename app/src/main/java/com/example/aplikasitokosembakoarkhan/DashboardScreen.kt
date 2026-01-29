package com.example.aplikasitokosembakoarkhan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aplikasitokosembakoarkhan.data.Product
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    onNavigateToSales: () -> Unit,
    onNavigateToProduct: () -> Unit,
    onNavigateToReport: () -> Unit,
    inventoryViewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory),
    reportViewModel: ReportViewModel = viewModel(factory = AppViewModelProvider.Factory),
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val products by inventoryViewModel.allProducts.collectAsState(initial = emptyList())
    val transactions by reportViewModel.allTransactions.collectAsState(initial = emptyList())
    val storeProfile by settingsViewModel.storeProfile.collectAsState()

    // --- LOGIKA DATA ---
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val todaySales = transactions.filter { it.date >= todayStart }.sumOf { it.totalAmount }
    val todayTransactionCount = transactions.count { it.date >= todayStart }

    val lowStockProducts = products.filter { it.stock <= 5 }.sortedBy { it.stock }

    val thirtyDaysFromNow = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
    val expiringProducts = products.filter { it.expireDate > 0 && it.expireDate < thirtyDaysFromNow && it.expireDate > System.currentTimeMillis() }.sortedBy { it.expireDate }

    val totalAsset = products.sumOf { it.buyPrice * it.stock }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..10 -> "Selamat Pagi"
        in 11..14 -> "Selamat Siang"
        in 15..18 -> "Selamat Sore"
        else -> "Selamat Malam"
    }

    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace("Rp", "Rp ").replace(",00", "")
    }

    // FUNGSI FORMAT STOK (FIXED)
    fun formatQty(qty: Double): String {
        return if (qty % 1.0 == 0.0) qty.toInt().toString() else qty.toString()
    }

    // --- UI DASHBOARD ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // HEADER
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = greeting, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Text(text = storeProfile.name.ifEmpty { "Toko Sembako" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { /* Navigasi ke Setting jika perlu */ }) {
                Icon(Icons.Default.Store, null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- HERO SECTION ---
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // KIRI: OMZET
            Card(
                modifier = Modifier.weight(1.2f).fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(colors = listOf(Color(0xFF00695C), Color(0xFF4DB6AC))))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxHeight()) {
                        Column {
                            Text("Penjualan Hari Ini", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(formatRupiah(todaySales), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Receipt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("$todayTransactionCount Transaksi", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // KANAN: AKSES CEPAT
            Column(
                modifier = Modifier.weight(0.8f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                QuickAccessButton(
                    icon = Icons.Default.ShoppingCart, label = "Kasir",
                    color = MaterialTheme.colorScheme.primaryContainer, iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onNavigateToSales, modifier = Modifier.weight(1f).fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                QuickAccessButton(
                    icon = Icons.Default.Inventory, label = "Barang",
                    color = MaterialTheme.colorScheme.secondaryContainer, iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = onNavigateToProduct, modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- STATISTIK MINI ---
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatisticItem(Icons.Default.Category, "Total Barang", "${products.size} Jenis")
            StatisticItem(Icons.Default.AttachMoney, "Aset Barang", formatRupiah(totalAsset))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- PERINGATAN (SCROLLABLE) ---
        Text("Peringatan & Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // EXPIRE
            if (expiringProducts.isNotEmpty()) {
                item {
                    WarningCard(
                        title = "Hampir Kadaluarsa (${expiringProducts.size})",
                        color = Color(0xFFFFEBEE), // Pink Muda
                        borderColor = Color(0xFFEF5350), // Merah
                        icon = Icons.Default.Warning
                    ) {
                        expiringProducts.take(5).forEach { p ->
                            val daysLeft = ((p.expireDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(p.name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text("$daysLeft hari lagi", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = Color.Red.copy(alpha = 0.1f))
                        }
                        if (expiringProducts.size > 5) Text("...dan ${expiringProducts.size - 5} lainnya", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            // STOK MENIPIS
            if (lowStockProducts.isNotEmpty()) {
                item {
                    WarningCard(
                        title = "Stok Menipis (${lowStockProducts.size})",
                        color = Color(0xFFFFF3E0), // Orange Muda
                        borderColor = Color(0xFFFF9800), // Orange Manual
                        icon = Icons.Default.ProductionQuantityLimits
                    ) {
                        lowStockProducts.take(5).forEach { p ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(p.name, fontWeight = FontWeight.Medium, fontSize = 13.sp)

                                // FIX: MENGGUNAKAN FORMAT QTY AGAR RAPI (0 bukan 0.0)
                                val unitStr = formatQty(p.stock)

                                Text("Sisa: $unitStr ${p.unit}", color = Color(0xFFE65100), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = Color(0xFFFF9800).copy(alpha = 0.1f))
                        }
                        if (lowStockProducts.size > 5) Text("...dan ${lowStockProducts.size - 5} lainnya", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            if (expiringProducts.isEmpty() && lowStockProducts.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32))
                            Spacer(Modifier.width(12.dp))
                            Text("Semua Aman! Stok cukup & tidak ada barang expired.", color = Color(0xFF1B5E20), fontSize = 13.sp)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun QuickAccessButton(
    icon: ImageVector, label: String, color: Color, iconColor: Color,
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Card(modifier = modifier.clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = color), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = iconColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun StatisticItem(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = Color.Gray)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun WarningCard(
    title: String, color: Color, borderColor: Color, icon: ImageVector, content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = borderColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, color = borderColor, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}