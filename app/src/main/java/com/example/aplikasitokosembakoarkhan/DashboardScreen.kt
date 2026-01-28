package com.example.aplikasitokosembakoarkhan

import androidx.compose.foundation.clickable // <-- Import Wajib
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aplikasitokosembakoarkhan.data.ProductDao
import com.example.aplikasitokosembakoarkhan.data.TransactionDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

// ViewModel Dashboard
class DashboardViewModel(
    productDao: ProductDao,
    transactionDao: TransactionDao
) : ViewModel() {

    val lowStockProducts = productDao.getAllProducts().map { it.filter { p -> p.stock <= 5.0 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions = transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as InventoryApplication

    // Inisialisasi Manual
    val viewModel = remember {
        DashboardViewModel(app.container.productDao, app.container.transactionDao)
    }

    val lowStock by viewModel.lowStockProducts.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()

    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val todayTransactions = transactions.filter { it.date >= today }

    // FIX: Gunakan totalAmount (bukan totalPrice) dan return Double eksplisit
    val todayOmzet: Double = todayTransactions.sumOf { it.totalAmount }
    val todayCount = todayTransactions.size

    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace("Rp", "Rp ").replace(",00", "")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // KARTU OMZET
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Penjualan Hari Ini", fontSize = 14.sp)
                Text(formatRupiah(todayOmzet), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Receipt, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("$todayCount Transaksi", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // STOK MENIPIS
        Text("Stok Menipis (${lowStock.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))

        if (lowStock.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stok aman!", color = Color(0xFF2E7D32))
                }
            }
        } else {
            lowStock.take(5).forEach { product ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(product.name, fontWeight = FontWeight.Bold)
                            val stockDisplay = if(product.stock % 1.0 == 0.0) product.stock.toInt().toString() else product.stock.toString()
                            Text("Sisa: $stockDisplay ${product.unit}", fontSize = 12.sp, color = Color.Red)
                        }
                        Icon(Icons.Default.Warning, null, tint = Color.Red)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Akses Cepat", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAccessCard("Kasir", Icons.Default.ShoppingCart, Color(0xFFE3F2FD)) {}
            QuickAccessCard("Laporan", Icons.Default.Analytics, Color(0xFFF3E5F5)) {}
        }
    }
}

@Composable
fun RowScope.QuickAccessCard(title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.weight(1f).height(100.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, null, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(title, fontWeight = FontWeight.Bold)
            }
        }
    }
}