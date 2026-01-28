package com.example.aplikasitokosembakoarkhan

import android.app.DatePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.aplikasitokosembakoarkhan.data.ExpenseDao
import com.example.aplikasitokosembakoarkhan.data.Product
import com.example.aplikasitokosembakoarkhan.data.Transaction
import com.example.aplikasitokosembakoarkhan.data.TransactionDao
import com.example.aplikasitokosembakoarkhan.utils.PrinterHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportViewModel(
    private val transactionDao: TransactionDao,
    private val expenseDao: ExpenseDao
) : ViewModel() {

    val allTransactions = transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses = expenseDao.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel = viewModel(factory = AppViewModelProvider.Factory),
    inventoryViewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory),
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val expenses by viewModel.allExpenses.collectAsState()
    val productList by inventoryViewModel.allProducts.collectAsState()
    val storeProfile by settingsViewModel.storeProfile.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }

    var showDetailDialog by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }

    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace("Rp", "Rp ").replace(",00", "")
    }

    // --- HELPER: FORMAT QTY PINTAR (1.0 -> 1, 1.5 -> 1.5) ---
    fun formatQty(qty: Double): String {
        return if (qty % 1.0 == 0.0) qty.toInt().toString() else qty.toString()
    }

    val filteredTransactions = remember(transactions, searchQuery, startDate, endDate) {
        transactions.filter { trans ->
            val matchSearch = trans.customerName.contains(searchQuery, ignoreCase = true) || trans.items.contains(searchQuery, ignoreCase = true)
            val matchDate = (startDate == null || trans.date >= startDate!!) &&
                    (endDate == null || trans.date <= (endDate!! + 86400000L))
            matchSearch && matchDate
        }
    }

    val filteredExpenses = remember(expenses, startDate, endDate) {
        expenses.filter { exp ->
            (startDate == null || exp.date >= startDate!!) &&
                    (endDate == null || exp.date <= (endDate!! + 86400000L))
        }
    }

    val totalOmzet = filteredTransactions.sumOf { it.totalAmount }
    val totalModalEstimasi = remember(filteredTransactions, productList) {
        var modal = 0.0
        filteredTransactions.forEach { trans ->
            val items = trans.items.split(", ")
            items.forEach { itemStr ->
                try {
                    val lastX = itemStr.lastIndexOf(" x")
                    if (lastX != -1) {
                        val name = itemStr.substring(0, lastX).trim()
                        val qty = itemStr.substring(lastX + 2).toDoubleOrNull() ?: 0.0
                        val product = productList.find { it.name.equals(name, ignoreCase = true) }
                        if (product != null) modal += product.buyPrice * qty
                    }
                } catch (e: Exception) { }
            }
        }
        modal
    }
    val totalPengeluaran = filteredExpenses.sumOf { it.amount }
    val labaKotor = totalOmzet - totalModalEstimasi
    val labaBersih = labaKotor - totalPengeluaran

    val topProducts = remember(filteredTransactions) {
        val countMap = mutableMapOf<String, Double>()
        filteredTransactions.forEach { trans ->
            val items = trans.items.split(", ")
            items.forEach { itemStr ->
                try {
                    val lastX = itemStr.lastIndexOf(" x")
                    if (lastX != -1) {
                        val name = itemStr.substring(0, lastX).trim()
                        val qty = itemStr.substring(lastX + 2).toDoubleOrNull() ?: 0.0
                        countMap[name] = (countMap[name] ?: 0.0) + qty
                    }
                } catch (e: Exception) {}
            }
        }
        countMap.toList().sortedByDescending { it.second }.take(5)
    }

    // --- STRUK WA (DENGAN SATUAN & FORMAT PINTAR) ---
    fun generateReceiptText(trans: Transaction): String {
        val sb = StringBuilder()
        val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(trans.date))
        sb.append("*${storeProfile.name.uppercase()}*\n")
        sb.append("Tgl: $date\n")
        sb.append("Plg: ${trans.customerName}\n")
        sb.append("--------------------------------\n")

        // Parsing Item untuk mendapatkan satuan dari database
        trans.items.split(", ").forEach { itemStr ->
            if(itemStr.isNotBlank()) {
                try {
                    val lastX = itemStr.lastIndexOf(" x")
                    if (lastX != -1) {
                        val name = itemStr.substring(0, lastX).trim()
                        val qtyVal = itemStr.substring(lastX + 2).toDoubleOrNull() ?: 0.0

                        // Cari satuan produk
                        val product = productList.find { it.name.equals(name, ignoreCase = true) }
                        val unit = product?.unit ?: "" // Ambil satuan (Pcs/Kg)

                        // Format: "Bawang Merah x 1.5 Kg"
                        sb.append("$name x ${formatQty(qtyVal)} $unit\n")
                    } else {
                        sb.append("$itemStr\n")
                    }
                } catch (e: Exception) {
                    sb.append("$itemStr\n")
                }
            }
        }

        sb.append("--------------------------------\n")
        sb.append("Total: ${formatRupiah(trans.totalAmount)}\n")
        return sb.toString()
    }

    fun shareToWhatsApp(trans: Transaction) {
        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, generateReceiptText(trans)); setPackage("com.whatsapp") }
        try { context.startActivity(intent) } catch (e: Exception) { Toast.makeText(context, "WhatsApp error", Toast.LENGTH_SHORT).show() }
    }

    fun printTransaction(trans: Transaction) {
        val cartMap = mutableMapOf<Product, Double>()
        trans.items.split(", ").forEach { itemStr ->
            try {
                val lastX = itemStr.lastIndexOf(" x")
                if (lastX != -1) {
                    val name = itemStr.substring(0, lastX).trim()
                    val qty = itemStr.substring(lastX + 2).toDoubleOrNull() ?: 0.0
                    val product = productList.find { it.name.equals(name, ignoreCase = true) }
                        ?: Product(name = name, barcode = "", buyPrice = 0.0, sellPrice = 0.0, stock = 0.0, category = "", unit = "")
                    cartMap[product] = qty
                }
            } catch (e: Exception) {}
        }
        PrinterHelper.printReceipt(context, cartMap, trans.totalAmount, trans.totalAmount, 0.0, trans.paymentMethod)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { Toast.makeText(context, "Export Excel Segera Hadir", Toast.LENGTH_SHORT).show() },
            ) { Icon(Icons.Default.Download, null) }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {

            // --- HEADER SEARCH ---
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchQuery, onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari Pelanggan/Barang") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    val isFilterActive = startDate != null
                    FilledTonalButton(
                        onClick = { showFilterDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(56.dp)
                    ) { Icon(Icons.Default.DateRange, null) }
                }

                if (startDate != null) {
                    Text(
                        text = "Periode: ${SimpleDateFormat("dd/MM/yy").format(Date(startDate!!))} - ${if(endDate!=null) SimpleDateFormat("dd/MM/yy").format(Date(endDate!!)) else "Hari ini"}",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
            ) {

                // --- SECTION 1: RINGKASAN KEUANGAN ---
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("RINGKASAN KEUANGAN", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total Omzet"); Text(formatRupiah(totalOmzet), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Estimasi Modal", color = Color.Gray); Text("- ${formatRupiah(totalModalEstimasi)}", color = Color.Gray) }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Laba Kotor"); Text(formatRupiah(labaKotor), fontWeight = FontWeight.SemiBold) }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Operasional", color = Color.Red); Text("- ${formatRupiah(totalPengeluaran)}", color = Color.Red) }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("LABA BERSIH", fontWeight = FontWeight.ExtraBold)
                                Text(formatRupiah(labaBersih), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = if(labaBersih >= 0) Color(0xFF2E7D32) else Color.Red)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // --- SECTION 2: PRODUK TERLARIS ---
                item {
                    if (topProducts.isNotEmpty()) {
                        Text("PRODUK TERLARIS (TOP 5)", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                topProducts.forEachIndexed { index, (name, qty) ->
                                    // Ambil satuan produk
                                    val product = productList.find { it.name.equals(name, ignoreCase = true) }
                                    val unit = product?.unit ?: ""

                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(if(index==0) Color(0xFFFFD700) else Color.LightGray.copy(alpha=0.3f)), contentAlignment = Alignment.Center) {
                                                Text("${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(name, fontWeight = FontWeight.Medium)
                                        }
                                        // Tampilkan: 5 Pcs atau 1.5 Kg
                                        Text("${formatQty(qty)} $unit", fontWeight = FontWeight.Bold, color = Color(0xFF00695C))
                                    }
                                    if(index < topProducts.size - 1) HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // --- SECTION 3: RIWAYAT TRANSAKSI ---
                item {
                    Text("RIWAYAT TRANSAKSI", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (filteredTransactions.isEmpty()) {
                    item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("Tidak ada data transaksi.", color = Color.Gray) } }
                } else {
                    items(filteredTransactions) { trans ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { selectedTransaction = trans; showDetailDialog = true },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ReceiptLong, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = SimpleDateFormat("dd MMM, HH:mm", Locale("id")).format(Date(trans.date)),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(trans.customerName, fontSize = 12.sp, color = Color.Gray)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(formatRupiah(trans.totalAmount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(trans.paymentMethod, fontSize = 11.sp, color = Color.Gray)
                                }

                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG DETAIL TRANSAKSI (DENGAN SATUAN) ---
    if (showDetailDialog && selectedTransaction != null) {
        val trans = selectedTransaction!!
        Dialog(onDismissRequest = { showDetailDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(max = 700.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Detail Transaksi", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id")).format(Date(trans.date)), fontSize = 12.sp, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { showDetailDialog = false }) { Icon(Icons.Default.Close, null) }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Pelanggan", fontSize = 11.sp)
                            Text(trans.customerName, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Daftar Belanja", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        val items = trans.items.split(", ")
                        items.forEach { itemStr ->
                            if (itemStr.isNotBlank()) {
                                var name = itemStr
                                var qtyStr = ""
                                var unitStr = ""
                                try {
                                    val lastX = itemStr.lastIndexOf(" x")
                                    if (lastX != -1) {
                                        name = itemStr.substring(0, lastX).trim()
                                        val rawQty = itemStr.substring(lastX + 2).toDoubleOrNull() ?: 0.0
                                        qtyStr = formatQty(rawQty) // Format 1.0 -> 1
                                    }
                                } catch (e: Exception) {}

                                // Cari Data Produk untuk ambil Unit & Gambar
                                val product = productList.find { it.name.equals(name, ignoreCase = true) }
                                unitStr = product?.unit ?: ""

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (product?.imagePath != null) {
                                        AsyncImage(
                                            model = File(product.imagePath),
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.ShoppingBag, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        // Tampilkan Qty + Satuan (Contoh: "Jumlah: 1 Pcs" atau "Jumlah: 0.5 Kg")
                                        Text("Jumlah: $qtyStr $unitStr", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Total Bayar", fontWeight = FontWeight.Bold)
                        Text(formatRupiah(trans.totalAmount), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("Metode: ${trans.paymentMethod}", fontSize = 12.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { printTransaction(trans) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Print, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Cetak")
                        }
                        Button(onClick = { shareToWhatsApp(trans) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("WA")
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG FILTER ---
    if (showFilterDialog) {
        val calendar = Calendar.getInstance()
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Pilih Periode") },
            text = {
                Column {
                    OutlinedButton(onClick = { DatePickerDialog(context, { _, y, m, d -> val c = Calendar.getInstance(); c.set(y, m, d, 0, 0, 0); startDate = c.timeInMillis }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show() }, modifier = Modifier.fillMaxWidth()) { Text(if(startDate!=null) "Mulai: ${SimpleDateFormat("dd/MM/yy").format(Date(startDate!!))}" else "Pilih Tanggal Mulai") }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { DatePickerDialog(context, { _, y, m, d -> val c = Calendar.getInstance(); c.set(y, m, d, 23, 59, 59); endDate = c.timeInMillis }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show() }, modifier = Modifier.fillMaxWidth()) { Text(if(endDate!=null) "Sampai: ${SimpleDateFormat("dd/MM/yy").format(Date(endDate!!))}" else "Pilih Tanggal Selesai") }
                }
            },
            confirmButton = { Button(onClick = { showFilterDialog = false }) { Text("Terapkan") } },
            dismissButton = { TextButton(onClick = { startDate = null; endDate = null; showFilterDialog = false }) { Text("Reset", color = Color.Red) } }
        )
    }
}