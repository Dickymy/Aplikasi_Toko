package com.example.aplikasitokosembakoarkhan

import android.app.DatePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ArrowDropDown // Ganti ArrowDownward jadi ArrowDropDown untuk menu
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.aplikasitokosembakoarkhan.data.Sale
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: ProductViewModel) {
    val salesList by viewModel.allSales.collectAsState()
    val expenseList by viewModel.allExpenses.collectAsState()
    val productList by viewModel.allProducts.collectAsState() // Kita butuh ini untuk mengambil data satuan
    val context = LocalContext.current

    // --- STATE FILTER ---
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Filter Values
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var selectedUnit by remember { mutableStateOf("Semua") }

    // --- LOGIC FILTERING ---
    val filteredSales = remember(salesList, searchQuery, startDate, endDate, selectedUnit) {
        salesList.filter { sale ->
            // 1. Filter Nama (Search)
            val matchName = sale.productName.contains(searchQuery, ignoreCase = true)

            // 2. Filter Tanggal (Range)
            val matchDate = (startDate == null || sale.date >= startDate!!) &&
                    (endDate == null || sale.date <= (endDate!! + 86400000L)) // +1 hari agar inklusif

            // 3. Filter Satuan (Cari produk dulu untuk tahu satuannya)
            val product = productList.find { it.id == sale.productId }
            val matchUnit = selectedUnit == "Semua" || (product?.unit == selectedUnit)

            matchName && matchDate && matchUnit
        }
    }

    val filteredExpenses = remember(expenseList, startDate, endDate) {
        expenseList.filter { expense ->
            (startDate == null || expense.date >= startDate!!) &&
                    (endDate == null || expense.date <= (endDate!! + 86400000L))
        }
    }

    // --- HITUNG KEUANGAN ---
    val totalOmzet = filteredSales.sumOf { it.totalPrice }
    val totalModal = filteredSales.sumOf { it.capitalPrice }
    val labaKotor = totalOmzet - totalModal
    val totalOperasional = filteredExpenses.sumOf { it.amount }
    val labaBersih = labaKotor - totalOperasional

    // Helper Format
    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace("Rp", "Rp ").replace(",00", "")
    }
    fun formatDate(millis: Long): String = SimpleDateFormat("dd MMM yyyy", Locale("id")).format(Date(millis))

    // List Satuan Unik untuk Dropdown
    val availableUnits = remember(productList) { listOf("Semua") + productList.map { it.unit }.distinct() }

    // --- UI UTAMA ---
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (filteredSales.isNotEmpty()) {
                        viewModel.exportSales(context, filteredSales) { file ->
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Kirim Excel"))
                        }
                    } else {
                        Toast.makeText(context, "Data Kosong (Cek Filter)", Toast.LENGTH_SHORT).show()
                    }
                },
                containerColor = Color(0xFF00695C),
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Download, null) },
                text = { Text("Export Laporan") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            // --- 1. TOP BAR (SEARCH & FILTER) ---
            Surface(shadowElevation = 4.dp, color = Color.White) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Laporan Keuangan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Cari Barang...") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Tombol Filter
                        val isFilterActive = startDate != null || endDate != null || selectedUnit != "Semua"
                        FilledTonalButton(
                            onClick = { showFilterDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if(isFilterActive) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0),
                                contentColor = if(isFilterActive) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp)
                        ) {
                            Icon(Icons.Default.FilterList, null)
                        }
                    }

                    // Indikator Filter Aktif
                    if (startDate != null || endDate != null) {
                        Text(
                            text = "Periode: ${startDate?.let { formatDate(it) } ?: "?"} - ${endDate?.let { formatDate(it) } ?: "?"}",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // --- 2. KARTU LABA BERSIH ---
                item {
                    val isProfit = labaBersih >= 0
                    val bgColor = if (isProfit) Color(0xFF2E7D32) else Color(0xFFC62828)

                    Card(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Laba Bersih (Sesuai Filter)", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                            Text(
                                formatRupiah(labaBersih),
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // --- 3. GRID RINGKASAN ---
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Omzet", formatRupiah(totalOmzet), Icons.Default.ShoppingBag, Color(0xFF1976D2), Modifier.weight(1f))
                        StatCard("Laba Kotor", formatRupiah(labaKotor), Icons.Default.AttachMoney, Color(0xFFFBC02D), Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Modal", formatRupiah(totalModal), Icons.Default.Inventory, Color.Gray, Modifier.weight(1f))
                        StatCard("Biaya Ops", "- ${formatRupiah(totalOperasional)}", Icons.Default.MoneyOff, Color(0xFFD32F2F), Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // --- 4. LIST TRANSAKSI ---
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rincian Transaksi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("${filteredSales.size} Transaksi", fontSize = 12.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (filteredSales.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Tidak ada data sesuai filter.", color = Color.Gray)
                        }
                    }
                } else {
                    items(filteredSales.sortedByDescending { it.date }) { sale ->
                        // AMBIL SATUAN DARI PRODUK LIST
                        val unit = productList.find { it.id == sale.productId }?.unit ?: "unit"

                        TransactionItemView(sale, unit) { formatRupiah(it) }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // --- DIALOG FILTER ---
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Laporan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Plih Tanggal
                    Text("Rentang Tanggal:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(context, { _, y, m, d ->
                                    val c = Calendar.getInstance(); c.set(y, m, d, 0, 0, 0); startDate = c.timeInMillis
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(startDate?.let { formatDate(it) } ?: "Dari Tgl")
                        }
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(context, { _, y, m, d ->
                                    val c = Calendar.getInstance(); c.set(y, m, d, 23, 59, 59); endDate = c.timeInMillis
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(endDate?.let { formatDate(it) } ?: "Sampai")
                        }
                    }

                    HorizontalDivider()

                    // Pilih Satuan
                    Text("Filter Satuan Barang:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Satuan: $selectedUnit")
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(24.dp))
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            availableUnits.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit) },
                                    onClick = { selectedUnit = unit; expanded = false }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showFilterDialog = false }) { Text("Terapkan") }
            },
            dismissButton = {
                TextButton(onClick = {
                    // Reset Filter
                    startDate = null
                    endDate = null
                    selectedUnit = "Semua"
                    showFilterDialog = false
                }) { Text("Reset", color = Color.Red) }
            }
        )
    }
}

// --- KOMPONEN KARTU KECIL ---
@Composable
fun StatCard(title: String, value: String, icon: ImageVector, iconColor: Color, modifier: Modifier) {
    Card(
        modifier = modifier.height(90.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontSize = 12.sp, color = Color.Gray)
            }
            Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
        }
    }
}

// --- ITEM TRANSAKSI ---
@Composable
fun TransactionItemView(sale: Sale, unit: String, formatRupiah: (Double) -> String) {
    val profit = sale.totalPrice - sale.capitalPrice
    val dateStr = SimpleDateFormat("dd/MM HH:mm", Locale("id")).format(Date(sale.date))

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.5.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(sale.productName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                // MENAMPILKAN SATUAN DI SINI
                Text("${sale.quantity} $unit • $dateStr", fontSize = 11.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatRupiah(sale.totalPrice), fontSize = 13.sp, color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)
                Text("+${formatRupiah(profit)}", fontSize = 11.sp, color = Color(0xFF388E3C))
            }
        }
    }
}