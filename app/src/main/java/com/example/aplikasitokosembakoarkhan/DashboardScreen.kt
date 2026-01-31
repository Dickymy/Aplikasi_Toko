package com.example.aplikasitokosembakoarkhan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aplikasitokosembakoarkhan.data.Product
import java.text.NumberFormat
import java.util.Calendar
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

    // --- STATE UI ---
    var showStoreProfileDialog by remember { mutableStateOf(false) }

    // --- LOGIKA DATA ---
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val todaySales = transactions.filter { it.date >= todayStart }.sumOf { it.totalAmount }
    val todayTransactionCount = transactions.count { it.date >= todayStart }

    // Filter Data Warning
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
        // HEADER (DENGAN TOMBOL PROFIL TOKO)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = greeting, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Text(text = storeProfile.name.ifEmpty { "Toko Sembako" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            // ICON TOKO SEKARANG BERFUNGSI
            IconButton(
                onClick = { showStoreProfileDialog = true },
                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            ) {
                Icon(Icons.Default.Store, null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- HERO SECTION (OMZET & QUICK ACCESS) ---
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

        // --- PERINGATAN (EXPANDABLE) ---
        Text("Peringatan & Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. EXPIRE WARNING (EXPANDABLE)
            if (expiringProducts.isNotEmpty()) {
                item {
                    ExpandableWarningCard(
                        title = "Hampir Kadaluarsa (${expiringProducts.size})",
                        color = Color(0xFFFFEBEE),
                        borderColor = Color(0xFFEF5350),
                        icon = Icons.Default.Warning,
                        items = expiringProducts,
                        content = { p ->
                            val daysLeft = ((p.expireDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(p.name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text("$daysLeft hari lagi", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }

            // 2. STOK WARNING (EXPANDABLE)
            if (lowStockProducts.isNotEmpty()) {
                item {
                    ExpandableWarningCard(
                        title = "Stok Menipis (${lowStockProducts.size})",
                        color = Color(0xFFFFF3E0),
                        borderColor = Color(0xFFFF9800),
                        icon = Icons.Default.ProductionQuantityLimits,
                        items = lowStockProducts,
                        content = { p ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(p.name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text("Sisa: ${formatQty(p.stock)} ${p.unit}", color = Color(0xFFE65100), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
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

    // --- DIALOG PROFIL TOKO (FITUR BARU UNTUK IKON TOKO) ---
    if (showStoreProfileDialog) {
        AlertDialog(
            onDismissRequest = { showStoreProfileDialog = false },
            icon = { Icon(Icons.Default.Store, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary) },
            title = { Text(storeProfile.name.ifEmpty { "Profil Toko" }) },
            text = {
                Column {
                    Text("Alamat:", fontSize = 12.sp, color = Color.Gray)
                    Text(storeProfile.address.ifEmpty { "-" }, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Text("Telepon:", fontSize = 12.sp, color = Color.Gray)
                    Text(storeProfile.phone.ifEmpty { "-" }, fontWeight = FontWeight.Medium)

                    Divider(Modifier.padding(vertical = 12.dp))

                    // Statistik Ringkas
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("Total Aset", fontSize = 11.sp); Text(formatRupiah(totalAsset), fontWeight = FontWeight.Bold) }
                        Column { Text("Total Barang", fontSize = 11.sp); Text("${products.size} Item", fontWeight = FontWeight.Bold) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStoreProfileDialog = false }) { Text("Tutup") }
            },
            // Opsional: Tombol Edit mengarah ke Settings
            dismissButton = {
                // Di sini Anda bisa menambahkan navigasi ke Settings jika mau,
                // tapi karena ini komponen Composable mandiri, kita cukup tutup saja.
            }
        )
    }
}

// --- KOMPONEN WARNING YANG BISA EXPAND ---
@Composable
fun <T> ExpandableWarningCard(
    title: String,
    color: Color,
    borderColor: Color,
    icon: ImageVector,
    items: List<T>,
    content: @Composable (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayItems = if (expanded) items else items.take(5)

    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = borderColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, color = borderColor, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))

            // List Items
            Column {
                displayItems.forEachIndexed { index, item ->
                    content(item)
                    if (index < displayItems.size - 1) {
                        HorizontalDivider(color = borderColor.copy(alpha = 0.1f))
                    }
                }
            }

            // Tombol Expand / Collapse
            if (items.size > 5) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = borderColor)
                ) {
                    Text(if (expanded) "Sembunyikan" else "Lihat Semua (${items.size})")
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null,
                        modifier = Modifier.padding(start = 4.dp).size(16.dp)
                    )
                }
            }
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