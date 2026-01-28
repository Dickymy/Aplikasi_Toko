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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.aplikasitokosembakoarkhan.data.Product
import com.example.aplikasitokosembakoarkhan.data.Sale
import com.example.aplikasitokosembakoarkhan.utils.PrinterHelper
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel = viewModel(factory = AppViewModelProvider.Factory),
    inventoryViewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory),
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val salesList by viewModel.allSales.collectAsState()
    val expenseList by viewModel.allExpenses.collectAsState()
    val productList by inventoryViewModel.allProducts.collectAsState()
    val storeProfile by settingsViewModel.storeProfile.collectAsState()
    val context = LocalContext.current

    // --- STATE FILTER ---
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Filter Values
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var selectedUnit by remember { mutableStateOf("Semua") }

    // --- STATE DETAIL DIALOG ---
    var showDetailDialog by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<List<Sale>>(emptyList()) }

    // --- DATA UNIQUE UNTUK DROPDOWN ---
    val availableCategories = remember(productList) { listOf("Semua") + productList.map { it.category }.distinct().sorted() }
    val availableUnits = remember(productList) { listOf("Semua") + productList.map { it.unit }.distinct().sorted() }

    // --- LOGIC FORMATTING ---
    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace("Rp", "Rp ").replace(",00", "")
    }

    // --- LOGIC FILTERING ---
    val filteredSales = remember(salesList, searchQuery, startDate, endDate, selectedCategory, selectedUnit) {
        salesList.filter { sale ->
            val product = productList.find { it.id == sale.productId }

            val matchName = sale.productName.contains(searchQuery, ignoreCase = true)
            val matchDate = (startDate == null || sale.date >= startDate!!) &&
                    (endDate == null || sale.date <= (endDate!! + 86400000L))
            val matchCategory = selectedCategory == "Semua" || (product?.category == selectedCategory)
            val matchUnit = selectedUnit == "Semua" || (product?.unit == selectedUnit)

            matchName && matchDate && matchCategory && matchUnit
        }
    }

    val filteredExpenses = remember(expenseList, startDate, endDate) {
        expenseList.filter { expense ->
            (startDate == null || expense.date >= startDate!!) &&
                    (endDate == null || expense.date <= (endDate!! + 86400000L))
        }
    }

    // --- GROUPING TRANSAKSI ---
    val groupedTransactions = remember(filteredSales) {
        filteredSales.groupBy { it.date }.toList().sortedByDescending { it.first }
    }

    // --- HITUNG KEUANGAN ---
    val totalOmzet = filteredSales.sumOf { it.totalPrice }
    val totalModal = filteredSales.sumOf { it.capitalPrice }
    val totalOperasional = filteredExpenses.sumOf { it.amount }
    val labaBersih = (totalOmzet - totalModal) - totalOperasional

    // --- FUNGSI STRUK & SHARE ---
    fun generateReceiptText(sales: List<Sale>): String {
        val sb = StringBuilder()
        val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(sales.first().date))
        val total = sales.sumOf { it.totalPrice }

        sb.append("*${storeProfile.name.uppercase()}*\n")
        if (storeProfile.address.isNotEmpty()) sb.append("${storeProfile.address}\n")
        if (storeProfile.phone.isNotEmpty()) sb.append("Telp: ${storeProfile.phone}\n")
        sb.append("--------------------------------\n")
        sb.append("Riwayat Transaksi\n")
        sb.append("Tgl: $date\n")
        sb.append("--------------------------------\n")

        sales.forEach { sale ->
            val product = productList.find { it.id == sale.productId }
            val unit = product?.unit ?: ""
            val pricePerItem = sale.totalPrice / sale.quantity
            sb.append("${sale.productName}\n")
            sb.append("${sale.quantity} $unit x ${formatRupiah(pricePerItem)} = ${formatRupiah(sale.totalPrice)}\n")
        }

        sb.append("--------------------------------\n")
        sb.append("*Total   : ${formatRupiah(total)}*\n")
        sb.append("--------------------------------\n")
        if (storeProfile.footer.isNotEmpty()) sb.append("${storeProfile.footer}\n")
        else sb.append("Terima Kasih!\n")

        return sb.toString()
    }

    fun shareToWhatsApp(sales: List<Sale>) {
        val text = generateReceiptText(sales)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.whatsapp")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                intent.setPackage("com.whatsapp.w4b")
                context.startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(context, "WhatsApp tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun printReceipt(sales: List<Sale>) {
        val cartMap = mutableMapOf<Product, Int>()
        sales.forEach { sale ->
            val product = productList.find { it.id == sale.productId }
            if (product != null) {
                val pricePerItem = sale.totalPrice / sale.quantity
                val historyProduct = product.copy(sellPrice = pricePerItem, wholesalePrice = pricePerItem)
                cartMap[historyProduct] = sale.quantity
            }
        }
        PrinterHelper.printReceipt(context, cartMap, sales.sumOf { it.totalPrice }, sales.sumOf { it.totalPrice }, 0.0, "Riwayat")
    }

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
                    } else Toast.makeText(context, "Data Kosong", Toast.LENGTH_SHORT).show()
                },
                containerColor = Color(0xFF00695C), contentColor = Color.White,
                icon = { Icon(Icons.Default.Download, null) }, text = { Text("Export Excel") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5))) {
            // HEADER & FILTER
            Surface(shadowElevation = 2.dp, color = Color.White) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // --- PERBAIKAN: Definisi variabel dipindah ke sini (sebelum Row) ---
                    val isFilterActive = startDate != null || selectedCategory != "Semua" || selectedUnit != "Semua"

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Laporan Keuangan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))

                        // Tombol Filter
                        FilledTonalButton(
                            onClick = { showFilterDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if(isFilterActive) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0),
                                contentColor = if(isFilterActive) Color.White else Color.Black
                            )
                        ) {
                            Icon(Icons.Default.FilterList, null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Filter")
                        }
                    }

                    // Info Filter Aktif
                    if (isFilterActive) {
                        Text(
                            text = "Filter: ${if(selectedCategory!="Semua") "$selectedCategory, " else ""}${if(selectedUnit!="Semua") "$selectedUnit, " else ""}${if(startDate!=null) "Tgl Aktif" else ""}",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            LazyColumn(contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxSize()) {
                // RINGKASAN
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Omzet:", color = Color.Gray); Text(formatRupiah(totalOmzet), fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Laba Kotor:", color = Color.Gray); Text(formatRupiah(totalOmzet - totalModal), fontWeight = FontWeight.Bold, color = Color(0xFFFBC02D))
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Operasional:", color = Color.Gray); Text("- ${formatRupiah(totalOperasional)}", fontWeight = FontWeight.Bold, color = Color.Red)
                            }
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Laba Bersih:", fontWeight = FontWeight.Bold); Text(formatRupiah(labaBersih), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (labaBersih >= 0) Color(0xFF2E7D32) else Color.Red)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Riwayat Transaksi (${filteredSales.size} Item)", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                }

                // LIST TRANSAKSI
                if (groupedTransactions.isEmpty()) {
                    item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("Data tidak ditemukan.", color = Color.Gray) } }
                } else {
                    items(groupedTransactions) { (date, sales) ->
                        val totalTrans = sales.sumOf { it.totalPrice }
                        val itemCount = sales.sumOf { it.quantity }

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedTransaction = sales; showDetailDialog = true },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id")).format(Date(date)), fontWeight = FontWeight.Bold)
                                    Text("$itemCount Barang", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Text(formatRupiah(totalTrans), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // --- DIALOG DETAIL TRANSAKSI ---
    if (showDetailDialog && selectedTransaction.isNotEmpty()) {
        Dialog(onDismissRequest = { showDetailDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Detail Transaksi", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id")).format(Date(selectedTransaction.first().date)), fontSize = 12.sp, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { showDetailDialog = false }) { Icon(Icons.Default.Close, null) }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        selectedTransaction.forEach { sale ->
                            val product = productList.find { it.id == sale.productId }
                            val unit = product?.unit ?: ""
                            val imgFile = product?.imagePath?.let { File(it) }

                            Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                                if (imgFile != null && imgFile.exists()) AsyncImage(model = imgFile, null, Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray), contentScale = ContentScale.Crop)
                                else Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray), contentAlignment = Alignment.Center) { Icon(Icons.Default.Image, null, tint = Color.White) }

                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sale.productName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("${sale.quantity} $unit @ ${formatRupiah(sale.totalPrice / sale.quantity)}", fontSize = 12.sp, color = Color.Gray)
                                }
                                Text(formatRupiah(sale.totalPrice), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Divider(color = Color.LightGray.copy(alpha = 0.3f))
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Total", fontWeight = FontWeight.Bold)
                        Text(formatRupiah(selectedTransaction.sumOf { it.totalPrice }), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { printReceipt(selectedTransaction) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Print, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Cetak") }
                        Button(onClick = { shareToWhatsApp(selectedTransaction) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))) { Icon(Icons.Default.Share, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("WA") }
                    }
                }
            }
        }
    }

    // --- DIALOG FILTER LENGKAP (TANGGAL, KATEGORI, SATUAN) ---
    if (showFilterDialog) {
        var expandedCat by remember { mutableStateOf(false) }
        var expandedUnit by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Laporan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 1. Pilih Tanggal
                    Text("Tanggal:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { val c = Calendar.getInstance(); DatePickerDialog(context, { _, y, m, d -> val cal = Calendar.getInstance(); cal.set(y, m, d, 0, 0, 0); startDate = cal.timeInMillis }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show() }, modifier = Modifier.weight(1f)) { Text(if(startDate != null) SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(startDate!!)) else "Mulai") }
                        OutlinedButton(onClick = { val c = Calendar.getInstance(); DatePickerDialog(context, { _, y, m, d -> val cal = Calendar.getInstance(); cal.set(y, m, d, 23, 59, 59); endDate = cal.timeInMillis }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show() }, modifier = Modifier.weight(1f)) { Text(if(endDate != null) SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(endDate!!)) else "Sampai") }
                    }

                    Divider()

                    // 2. Pilih Kategori
                    Text("Kategori Barang:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { expandedCat = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(if(selectedCategory=="Semua") "Semua Kategori" else selectedCategory)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                            availableCategories.forEach { cat ->
                                DropdownMenuItem(text = { Text(cat) }, onClick = { selectedCategory = cat; expandedCat = false })
                            }
                        }
                    }

                    // 3. Pilih Satuan
                    Text("Satuan Barang:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { expandedUnit = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(if(selectedUnit=="Semua") "Semua Satuan" else selectedUnit)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = expandedUnit, onDismissRequest = { expandedUnit = false }) {
                            availableUnits.forEach { unit ->
                                DropdownMenuItem(text = { Text(unit) }, onClick = { selectedUnit = unit; expandedUnit = false })
                            }
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { showFilterDialog = false }) { Text("Terapkan") } },
            dismissButton = {
                TextButton(onClick = {
                    startDate = null; endDate = null; selectedCategory = "Semua"; selectedUnit = "Semua"
                    showFilterDialog = false
                }) { Text("Reset", color = Color.Red) }
            }
        )
    }
}