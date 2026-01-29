package com.example.aplikasitokosembakoarkhan

import android.app.DatePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    // --- STRUK WA ---
    fun generateReceiptText(trans: Transaction): String {
        val sb = StringBuilder()
        val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(trans.date))
        sb.append("*${storeProfile.name.uppercase()}*\n")
        sb.append("Tgl: $date\n")
        sb.append("Plg: ${trans.customerName}\n")
        sb.append("--------------------------------\n")

        trans.items.split(", ").forEach { itemStr ->
            if(itemStr.isNotBlank()) {
                try {
                    val lastX = itemStr.lastIndexOf(" x")
                    if (lastX != -1) {
                        val name = itemStr.substring(0, lastX).trim()
                        val qtyVal = itemStr.substring(lastX + 2).toDoubleOrNull() ?: 0.0

                        val product = productList.find { it.name.equals(name, ignoreCase = true) }
                        val unit = product?.unit ?: ""
                        val desc = product?.description ?: ""

                        sb.append("$name x ${formatQty(qtyVal)} $unit\n")
                        if (desc.isNotEmpty()) {
                            sb.append("($desc)\n")
                        }
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
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) { Icon(Icons.Default.Download, null) }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
        ) {
            // --- HEADER SEARCH & FILTER ---
            Column(modifier = Modifier.background(Color.White).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchQuery, onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari Transaksi...") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFFAFAFA),
                            unfocusedContainerColor = Color(0xFFFAFAFA),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    FilledTonalIconButton(
                        onClick = { showFilterDialog = true },
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if(startDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if(startDate != null) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) { Icon(Icons.Default.DateRange, null) }
                }

                if (startDate != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Periode: ${SimpleDateFormat("dd MMM").format(Date(startDate!!))} - ${if(endDate!=null) SimpleDateFormat("dd MMM").format(Date(endDate!!)) else "Hari ini"}",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp).clickable { startDate=null; endDate=null }, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {

                // --- SECTION 1: RINGKASAN KEUANGAN (TENGAH & RAPI) ---
                item {
                    Text(
                        text = "RINGKASAN",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SummaryCard(title = "Total Omzet", amount = totalOmzet, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        SummaryCard(title = "Total Pengeluaran", amount = totalPengeluaran, color = Color(0xFFD32F2F), modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if(labaBersih >= 0) Color(0xFF388E3C) else Color(0xFFC62828)
                        ),
                        elevation = CardDefaults.cardElevation(6.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "ESTIMASI LABA BERSIH",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = formatRupiah(labaBersih),
                                color = Color.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(50),
                            ) {
                                Text(
                                    text = "Omzet - Modal - Operasional",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // --- SECTION 2: PRODUK TERLARIS ---
                if (topProducts.isNotEmpty()) {
                    item {
                        Text("PRODUK TERLARIS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFFF0F0F0))) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                topProducts.forEachIndexed { index, (name, qty) ->
                                    val product = productList.find { it.name.equals(name, ignoreCase = true) }
                                    val unit = product?.unit ?: ""

                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(if(index==0) Color(0xFFFFC107) else Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) {
                                                Text("${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if(index==0) Color.White else Color.Black)
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        }
                                        Text("${formatQty(qty)} $unit", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    if(index < topProducts.size - 1) HorizontalDivider(color = Color(0xFFEEEEEE))
                                }
                            }
                        }
                    }
                }

                // --- SECTION 3: RIWAYAT TRANSAKSI ---
                item {
                    Text("RIWAYAT TRANSAKSI (${filteredTransactions.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
                }

                if (filteredTransactions.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.History, null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                                Spacer(Modifier.height(8.dp))
                                Text("Tidak ada data transaksi", color = Color.Gray)
                            }
                        }
                    }
                } else {
                    items(filteredTransactions) { trans ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable { selectedTransaction = trans; showDetailDialog = true },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(1.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFF0F0F0))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE3F2FD)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ReceiptLong, null, tint = MaterialTheme.colorScheme.primary)
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(trans.customerName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(SimpleDateFormat("dd MMM • HH:mm", Locale("id")).format(Date(trans.date)), fontSize = 12.sp, color = Color.Gray)
                                    Surface(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(top=4.dp)) {
                                        Text(trans.paymentMethod, fontSize = 10.sp, color = Color.DarkGray, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(formatRupiah(trans.totalAmount), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG DETAIL TRANSAKSI ---
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
                            Text(SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("id")).format(Date(trans.date)), fontSize = 12.sp, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { showDetailDialog = false }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, null)
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Pelanggan", fontSize = 10.sp, color = Color.Gray)
                            Text(trans.customerName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Item Belanja", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        val itemsList = trans.items.split(", ")
                        itemsList.forEach { itemStr ->
                            if (itemStr.isNotBlank()) {
                                var itemName = itemStr
                                var itemQty = ""
                                var itemUnit = ""
                                var itemDesc = ""
                                try {
                                    val lastX = itemStr.lastIndexOf(" x")
                                    if (lastX != -1) {
                                        itemName = itemStr.substring(0, lastX).trim()
                                        val rawQty = itemStr.substring(lastX + 2).toDoubleOrNull() ?: 0.0
                                        itemQty = formatQty(rawQty)
                                    }
                                } catch (e: Exception) {}

                                val prod = productList.find { it.name.equals(itemName, ignoreCase = true) }
                                itemUnit = prod?.unit ?: ""
                                itemDesc = prod?.description ?: ""

                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    if (prod?.imagePath != null) {
                                        AsyncImage(model = File(prod.imagePath), contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray), contentScale = ContentScale.Crop)
                                    } else {
                                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF0F0F0)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.ShoppingBag, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(itemName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        if (itemDesc.isNotEmpty()) {
                                            Text(itemDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontStyle = FontStyle.Italic)
                                        }
                                        Text("Jumlah: $itemQty $itemUnit", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Metode Bayar", color = Color.Gray)
                                Text(trans.paymentMethod, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Transaksi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(formatRupiah(trans.totalAmount), fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { printTransaction(trans) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.Print, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Cetak")
                        }
                        Button(onClick = { shareToWhatsApp(trans) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("WA")
                        }
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        val cal = Calendar.getInstance()
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Periode", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedButton(onClick = { DatePickerDialog(context, { _, y, m, d -> val c = Calendar.getInstance(); c.set(y, m, d, 0, 0, 0); startDate = c.timeInMillis }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.DateRange, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if(startDate!=null) "Mulai: ${SimpleDateFormat("dd MMM yyyy").format(Date(startDate!!))}" else "Tanggal Mulai")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = { DatePickerDialog(context, { _, y, m, d -> val c = Calendar.getInstance(); c.set(y, m, d, 23, 59, 59); endDate = c.timeInMillis }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.DateRange, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if(endDate!=null) "Sampai: ${SimpleDateFormat("dd MMM yyyy").format(Date(endDate!!))}" else "Tanggal Selesai")
                    }
                }
            },
            confirmButton = { Button(onClick = { showFilterDialog = false }) { Text("Terapkan") } },
            dismissButton = { TextButton(onClick = { startDate = null; endDate = null; showFilterDialog = false }) { Text("Reset", color = Color.Red) } }
        )
    }
}

@Composable
fun SummaryCard(title: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(
                text = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace("Rp", "Rp ").replace(",00", ""),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}