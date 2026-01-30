package com.example.aplikasitokosembakoarkhan

import android.app.DatePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

// --- VIEW MODEL ---
class ReportViewModel(
    private val transactionDao: TransactionDao,
    private val expenseDao: ExpenseDao
) : ViewModel() {

    val allTransactions = transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses = expenseDao.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

// --- COLOR PALETTE ---
val PrimaryBlue = Color(0xFF1565C0)
val IncomeColor = Color(0xFF2E7D32)
val ExpenseColor = Color(0xFFC62828)
val BackgroundColor = Color(0xFFF3F4F6)

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

    // --- STATES ---
    var showDateDialog by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }

    var selectedPaymentFilter by remember { mutableStateOf("Semua") }
    val paymentOptions = listOf("Semua", "Tunai", "Transfer", "QRIS")
    var sortOption by remember { mutableStateOf("Terbaru") }

    var showDetailDialog by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }

    // --- HELPERS ---
    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace("Rp", "Rp ").replace(",00", "")
    }

    fun formatQty(qty: Double): String {
        return if (qty % 1.0 == 0.0) qty.toInt().toString() else qty.toString()
    }

    // --- FILTER & SORT LOGIC ---
    val filteredTransactions = remember(transactions, selectedPaymentFilter, startDate, endDate, sortOption) {
        var result = transactions
        if (startDate != null) result = result.filter { it.date >= startDate!! }
        if (endDate != null) result = result.filter { it.date <= (endDate!! + 86400000L) }

        if (selectedPaymentFilter != "Semua") {
            if (selectedPaymentFilter == "Non-Tunai") result = result.filter { !it.paymentMethod.equals("Tunai", ignoreCase = true) }
            else result = result.filter { it.paymentMethod.equals(selectedPaymentFilter, ignoreCase = true) }
        }

        if (sortOption == "Terbaru") result.sortedByDescending { it.date } else result.sortedByDescending { it.totalAmount }
    }

    val filteredExpenses = remember(expenses, startDate, endDate) {
        expenses.filter { exp -> (startDate == null || exp.date >= startDate!!) && (endDate == null || exp.date <= (endDate!! + 86400000L)) }
    }

    val totalOmzet = filteredTransactions.sumOf { it.totalAmount }
    val totalPengeluaran = filteredExpenses.sumOf { it.amount }

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
    val labaBersih = totalOmzet - totalModalEstimasi - totalPengeluaran

    // --- LAUNCHER EXPORT CSV ---
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && filteredTransactions.isNotEmpty()) {
            inventoryViewModel.exportTransactionsToCsv(context, uri, filteredTransactions) { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        } else if (filteredTransactions.isEmpty()) {
            Toast.makeText(context, "Tidak ada data untuk diexport", Toast.LENGTH_SHORT).show()
        }
    }

    // --- FIX: FUNGSI SHARE WA ---
    fun shareToWhatsApp(trans: Transaction) {
        val sb = StringBuilder()

        // 1. HEADER (Nama & Alamat Toko)
        sb.append("*${storeProfile.name}*\n")
        if (storeProfile.address.isNotEmpty()) {
            sb.append("${storeProfile.address}\n")
        }
        sb.append("----------------\n")

        // 2. Info Transaksi
        sb.append("Tgl: ${SimpleDateFormat("dd MMM yyyy, HH:mm").format(Date(trans.date))}\n")
        sb.append("Plg: ${trans.customerName}\n")
        sb.append("----------------\n")

        // 3. Detail Item
        trans.items.split(", ").forEach { itemStr ->
            if (itemStr.isNotBlank()) {
                try {
                    val lastX = itemStr.lastIndexOf(" x")
                    if (lastX != -1) {
                        val name = itemStr.substring(0, lastX).trim()
                        val qtyVal = itemStr.substring(lastX + 2).toDoubleOrNull() ?: 0.0
                        val product = productList.find { it.name.equals(name, ignoreCase = true) }
                        sb.append("$name x ${formatQty(qtyVal)} ${product?.unit ?: ""}\n")
                    } else sb.append("$itemStr\n")
                } catch (e: Exception) { sb.append("$itemStr\n") }
            }
        }

        // 4. Total & Footer
        sb.append("----------------\n")
        sb.append("Total: ${formatRupiah(trans.totalAmount)}\n")

        if (storeProfile.footer.isNotEmpty()) {
            sb.append("\n${storeProfile.footer}\n")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            setPackage("com.whatsapp")
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val shareIntent = Intent.createChooser(intent.apply { setPackage(null) }, "Bagikan Struk")
            context.startActivity(shareIntent)
        }
    }

    // --- FIX: FUNGSI CETAK STRUK ---
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

        // PERBAIKAN: Karena database tidak punya payAmount & changeAmount,
        // kita gunakan asumsi bayar pas (Total = Bayar, Kembali = 0)
        // agar tidak error saat kompilasi.
        val payAmount = trans.totalAmount
        val changeAmount = 0.0

        PrinterHelper.printReceipt(
            context = context,
            cart = cartMap,
            totalPrice = trans.totalAmount,
            payAmount = payAmount,
            change = changeAmount,
            paymentMethod = trans.paymentMethod,
            transactionDate = trans.date
        )
    }

    Scaffold(
        containerColor = BackgroundColor,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (filteredTransactions.isNotEmpty()) {
                        val dateFormat = SimpleDateFormat("ddMMMyy", Locale.getDefault())
                        val startStr = if (startDate != null) dateFormat.format(Date(startDate!!)) else "Semua"
                        val endStr = if (endDate != null) dateFormat.format(Date(endDate!!)) else "Data"
                        val fileName = "Laporan_${startStr}_sd_${endStr}.csv"
                        exportLauncher.launch(fileName)
                    } else {
                        Toast.makeText(context, "Tidak ada data untuk diexport", Toast.LENGTH_SHORT).show()
                    }
                },
                containerColor = Color(0xFF1B5E20),
                contentColor = Color.White,
                icon = { Icon(Icons.Default.FileDownload, null) },
                text = { Text("Export Excel") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // HEADER CONTROL PANEL
            Surface(color = Color.White, shadowElevation = 2.dp, shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Filter Tanggal
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { showDateDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FA)),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.DateRange, null, tint = PrimaryBlue); Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Periode Laporan", fontSize = 10.sp, color = Color.Gray)
                                Text(
                                    text = if (startDate != null) "${SimpleDateFormat("dd MMM").format(Date(startDate!!))} - ${if (endDate != null) SimpleDateFormat("dd MMM yyyy").format(Date(endDate!!)) else "Hari Ini"}" else "Semua Waktu",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }
                            Spacer(Modifier.weight(1f)); Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filter Pembayaran
                    Text("Metode Pembayaran:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            items(paymentOptions) { method ->
                                FilterChip(
                                    selected = selectedPaymentFilter == method,
                                    onClick = { selectedPaymentFilter = method },
                                    label = { Text(method, fontSize = 12.sp) },
                                    enabled = true,
                                    leadingIcon = if (selectedPaymentFilter == method) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) } } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.15f),
                                        selectedLabelColor = PrimaryBlue,
                                        selectedLeadingIconColor = PrimaryBlue
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(borderColor = if(selectedPaymentFilter == method) PrimaryBlue else Color.LightGray, enabled = true, selected = selectedPaymentFilter == method)
                                )
                            }
                        }
                        IconButton(onClick = { sortOption = if(sortOption == "Terbaru") "Tertinggi" else "Terbaru" }) {
                            Icon(if(sortOption == "Terbaru") Icons.Default.AccessTime else Icons.Default.AttachMoney, null, tint = PrimaryBlue)
                        }
                    }
                }
            }

            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {

                item {
                    Text("Ringkasan Keuangan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniSummaryCard("Pemasukan", totalOmzet, IncomeColor, Icons.Default.ArrowUpward, Modifier.weight(1f))
                        MiniSummaryCard("Modal", totalModalEstimasi, Color(0xFF607D8B), Icons.Default.Inventory, Modifier.weight(1f))
                        MiniSummaryCard("Biaya", totalPengeluaran, ExpenseColor, Icons.Default.MoneyOff, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if(labaBersih >= 0) IncomeColor else ExpenseColor), elevation = CardDefaults.cardElevation(4.dp)) {
                        Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("LABA BERSIH", fontSize = 12.sp, color = Color.White.copy(0.8f), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(formatRupiah(labaBersih), fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Text("Riwayat Transaksi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("${filteredTransactions.size} Data", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                if (filteredTransactions.isEmpty()) {
                    item { Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text("Tidak ada transaksi", color = Color.Gray) } }
                } else {
                    items(filteredTransactions) { trans ->
                        TransactionItemNew(trans, onClick = { selectedTransaction = trans; showDetailDialog = true })
                    }
                }
            }
        }
    }

    // --- DIALOG FILTER TANGGAL ---
    if (showDateDialog) {
        val cal = Calendar.getInstance()

        fun setRange(daysAgo: Int) {
            val end = Calendar.getInstance()
            end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59)
            val start = Calendar.getInstance()
            start.add(Calendar.DAY_OF_YEAR, -daysAgo)
            start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0)

            startDate = start.timeInMillis
            endDate = end.timeInMillis
            showDateDialog = false
        }

        fun setThisMonth() {
            val start = Calendar.getInstance()
            start.set(Calendar.DAY_OF_MONTH, 1)
            start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0)
            val end = Calendar.getInstance()
            end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59)
            startDate = start.timeInMillis
            endDate = end.timeInMillis
            showDateDialog = false
        }

        AlertDialog(
            onDismissRequest = { showDateDialog = false },
            title = { Text("Filter Periode") },
            text = {
                Column {
                    Text("Pilih Cepat:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SuggestionChip(onClick = { setRange(0) }, label = { Text("Hari Ini") }, modifier = Modifier.weight(1f))
                        SuggestionChip(onClick = { setRange(6) }, label = { Text("7 Hari") }, modifier = Modifier.weight(1f))
                        SuggestionChip(onClick = { setThisMonth() }, label = { Text("Bulan Ini") }, modifier = Modifier.weight(1f))
                    }
                    Divider(Modifier.padding(vertical = 12.dp))
                    Text("Custom Tanggal:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { DatePickerDialog(context, { _, y, m, d -> val c = Calendar.getInstance(); c.set(y, m, d, 0, 0, 0); startDate = c.timeInMillis }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show() }, modifier = Modifier.fillMaxWidth()) { Text(if(startDate!=null) "Mulai: ${SimpleDateFormat("dd/MM/yy").format(Date(startDate!!))}" else "Pilih Tanggal Mulai") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { DatePickerDialog(context, { _, y, m, d -> val c = Calendar.getInstance(); c.set(y, m, d, 23, 59, 59); endDate = c.timeInMillis }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show() }, modifier = Modifier.fillMaxWidth()) { Text(if(endDate!=null) "Sampai: ${SimpleDateFormat("dd/MM/yy").format(Date(endDate!!))}" else "Pilih Tanggal Selesai") }
                }
            },
            confirmButton = { Button(onClick = { showDateDialog = false }) { Text("Tutup") } },
            dismissButton = { TextButton(onClick = { startDate = null; endDate = null; showDateDialog = false }) { Text("Reset") } }
        )
    }

    // --- DIALOG DETAIL ---
    if (showDetailDialog && selectedTransaction != null) {
        val trans = selectedTransaction!!
        Dialog(onDismissRequest = { showDetailDialog = false }) {
            Card(modifier = Modifier.fillMaxWidth().heightIn(max = 700.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(50.dp).background(PrimaryBlue.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Receipt, null, tint = PrimaryBlue, modifier = Modifier.size(28.dp)) }
                        Spacer(Modifier.height(12.dp))
                        Text(formatRupiah(trans.totalAmount), fontSize = 24.sp, fontWeight = FontWeight.Black, color = PrimaryBlue)
                        Text("Total Transaksi", fontSize = 12.sp, color = Color.Gray)
                    }
                    Spacer(Modifier.height(20.dp)); Divider(color = Color(0xFFEEEEEE)); Spacer(Modifier.height(16.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Waktu", color = Color.Gray, fontSize = 12.sp); Text(SimpleDateFormat("dd MMM, HH:mm").format(Date(trans.date)), fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    Spacer(Modifier.height(8.dp))

                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Pelanggan", fontSize = 10.sp, color = Color.Gray)
                                Text(trans.customerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Metode", fontSize = 10.sp, color = Color.Gray)
                                Text(trans.paymentMethod, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if(trans.paymentMethod=="Tunai") IncomeColor else Color(0xFFE65100))
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Detail Item", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))

                    Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        val items = trans.items.split(", ")
                        items.forEach { itemStr ->
                            if (itemStr.isNotBlank()) {
                                var name = itemStr
                                var qty = ""
                                try {
                                    val lastX = itemStr.lastIndexOf(" x")
                                    if(lastX != -1) {
                                        name = itemStr.substring(0, lastX).trim()
                                        val rawQty = itemStr.substring(lastX + 2).toDoubleOrNull() ?: 0.0
                                        qty = formatQty(rawQty)
                                    }
                                } catch(e: Exception){}

                                val product = productList.find { it.name.equals(name, ignoreCase = true) }
                                val desc = product?.description ?: ""
                                val unit = product?.unit ?: ""
                                val category = product?.category ?: "-"

                                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (product?.imagePath != null) {
                                        AsyncImage(model = File(product.imagePath), null, Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                    } else {
                                        Box(Modifier.size(40.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.ShoppingBag, null, tint = Color.LightGray) }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        if (desc.isNotEmpty()) Text(desc, fontSize = 11.sp, color = Color.Gray, fontStyle = FontStyle.Italic)
                                        Text("$category • $unit", fontSize = 10.sp, color = PrimaryBlue)
                                    }
                                    if(qty.isNotEmpty()) {
                                        Text("x$qty", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                                Divider(color = Color(0xFFF5F5F5))
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { printTransaction(trans) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Icon(Icons.Default.Print, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Cetak") }
                        Button(onClick = { shareToWhatsApp(trans) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))) { Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("WA") }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniSummaryCard(title: String, amount: Double, color: Color, icon: ImageVector, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.background(color.copy(0.1f), CircleShape).padding(6.dp)) { Icon(icon, null, tint = color, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 11.sp, color = Color.Gray)
            Text(NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace(",00", ""), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun TransactionItemNew(trans: Transaction, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(if(trans.paymentMethod=="Tunai") Color(0xFFE8F5E9) else Color(0xFFE3F2FD)), contentAlignment = Alignment.Center) {
                Text(trans.customerName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = if(trans.paymentMethod=="Tunai") Color(0xFF2E7D32) else PrimaryBlue)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(trans.customerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(SimpleDateFormat("dd MMM • HH:mm").format(Date(trans.date)), fontSize = 11.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(trans.totalAmount).replace(",00", ""), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryBlue)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(trans.paymentMethod, fontSize = 10.sp, color = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}