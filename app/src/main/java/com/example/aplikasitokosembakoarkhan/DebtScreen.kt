package com.example.aplikasitokosembakoarkhan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aplikasitokosembakoarkhan.data.Customer
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtScreen(
    viewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val customers by viewModel.allCustomers.collectAsState()

    // State Filter & Search
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Semua") }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Dialog State
    var showTransactionDialog by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf(false) } // State untuk Dialog Detail

    // Temp Data
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var selectedCustomerForDetail by remember { mutableStateOf<Customer?>(null) } // Pelanggan yang diklik untuk detail
    var transactionType by remember { mutableStateOf("Hutang") }
    var amountInput by remember { mutableStateOf("") }

    // --- HELPERS ---
    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace("Rp", "Rp ").replace(",00", "")
    }

    fun formatInput(input: String): String {
        val clean = input.replace("[^\\d]".toRegex(), "")
        if (clean.isEmpty()) return ""
        return try {
            val number = clean.toLong()
            NumberFormat.getInstance(Locale("id", "ID")).format(number)
        } catch (e: Exception) { clean }
    }

    fun cleanInput(input: String): Double {
        return input.replace(".", "").replace(",", "").toDoubleOrNull() ?: 0.0
    }

    fun formatDate(millis: Long): String {
        return if (millis > 0) SimpleDateFormat("dd MMM yy", Locale("id", "ID")).format(Date(millis)) else "-"
    }

    val filteredCustomers = customers.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery)
    }

    val debtors = filteredCustomers.filter { it.totalDebt > 0 }.sortedByDescending { it.totalDebt }
    val paidOff = filteredCustomers.filter { it.totalDebt <= 0 && it.hasHistory }.sortedByDescending { it.lastUpdated }
    val cleanUsers = filteredCustomers.filter { it.totalDebt <= 0 && !it.hasHistory }.sortedBy { it.name }

    val totalPiutang = customers.sumOf { it.totalDebt }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            // --- HEADER SEARCH & FILTER ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari Nama / No HP...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = { showFilterDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if(selectedFilter != "Semua") MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0),
                        contentColor = if(selectedFilter != "Semua") Color.White else Color.Black
                    )
                ) {
                    Icon(Icons.Default.FilterList, null)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- KARTU RINGKASAN TOTAL ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Piutang Toko", fontSize = 12.sp, color = Color(0xFFC62828))
                        Text(formatRupiah(totalPiutang), fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 20.sp)
                    }
                    Icon(Icons.Default.AccountBalanceWallet, null, tint = Color(0xFFE57373), modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- LIST PELANGGAN ---
            LazyColumn(modifier = Modifier.fillMaxSize()) {

                // GROUP 1: SEDANG HUTANG
                if ((selectedFilter == "Semua" || selectedFilter == "Hutang") && debtors.isNotEmpty()) {
                    item { SectionHeader("Sedang Hutang (${debtors.size})", Color(0xFFD32F2F)) }
                    items(debtors) { customer ->
                        CustomerDebtCard(
                            customer = customer,
                            debtFormatted = formatRupiah(customer.totalDebt),
                            lastUpdated = formatDate(customer.lastUpdated),
                            isPaidOff = false,
                            onClick = { // KLIK UNTUK DETAIL
                                selectedCustomerForDetail = customer
                                showDetailDialog = true
                            },
                            onTransact = { c, type -> selectedCustomer = c; transactionType = type; amountInput = ""; showTransactionDialog = true }
                        )
                    }
                    if (selectedFilter == "Semua") item { Divider(Modifier.padding(vertical = 12.dp)) }
                }

                // GROUP 2: LUNAS (Pernah Hutang)
                if ((selectedFilter == "Semua" || selectedFilter == "Lunas") && paidOff.isNotEmpty()) {
                    item { SectionHeader("Lunas / Riwayat Ada (${paidOff.size})", Color(0xFF388E3C)) }
                    items(paidOff) { customer ->
                        CustomerDebtCard(
                            customer = customer,
                            debtFormatted = "Lunas",
                            lastUpdated = formatDate(customer.lastUpdated),
                            isPaidOff = true,
                            onClick = { // KLIK UNTUK DETAIL
                                selectedCustomerForDetail = customer
                                showDetailDialog = true
                            },
                            onTransact = { c, type -> selectedCustomer = c; transactionType = type; amountInput = ""; showTransactionDialog = true }
                        )
                    }
                    if (selectedFilter == "Semua") item { Divider(Modifier.padding(vertical = 12.dp)) }
                }

                // GROUP 3: BERSIH
                if ((selectedFilter == "Semua" || selectedFilter == "Bersih") && cleanUsers.isNotEmpty()) {
                    item { SectionHeader("Tidak Punya Hutang (${cleanUsers.size})", Color.Gray) }
                    items(cleanUsers) { customer ->
                        CustomerDebtCard(
                            customer = customer,
                            debtFormatted = "-",
                            lastUpdated = "-",
                            isPaidOff = true,
                            onClick = { // KLIK UNTUK DETAIL
                                selectedCustomerForDetail = customer
                                showDetailDialog = true
                            },
                            onTransact = { c, type -> selectedCustomer = c; transactionType = type; amountInput = ""; showTransactionDialog = true }
                        )
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
            title = { Text("Filter Pelanggan") },
            text = {
                Column {
                    listOf("Semua", "Hutang", "Lunas", "Bersih").forEach { filter ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedFilter = filter; showFilterDialog = false }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedFilter == filter, onClick = null)
                            Text(text = filter, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFilterDialog = false }) { Text("Tutup") } }
        )
    }

    // --- DIALOG DETAIL RIWAYAT (BARU) ---
    if (showDetailDialog && selectedCustomerForDetail != null) {
        val detailCustomer = selectedCustomerForDetail!!
        // Mengambil data riwayat dari ViewModel
        val history by viewModel.getDebtHistory(detailCustomer.id).collectAsState(initial = emptyList())

        Dialog(onDismissRequest = { showDetailDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Detail
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(detailCustomer.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(if(detailCustomer.phoneNumber.isNotEmpty()) detailCustomer.phoneNumber else "Tanpa No HP", fontSize = 12.sp, color = Color.Gray)
                        }
                        IconButton(onClick = { showDetailDialog = false }) {
                            Icon(Icons.Default.Close, "Tutup")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Box Sisa Hutang
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (detailCustomer.totalDebt > 0) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Sisa Hutang Saat Ini", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = formatRupiah(detailCustomer.totalDebt),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (detailCustomer.totalDebt > 0) Color(0xFFD32F2F) else Color(0xFF388E3C)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Riwayat Transaksi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // List Riwayat
                    if (history.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text("Belum ada riwayat transaksi", color = Color.Gray, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                            items(history) { trans ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        val typeLabel = if (trans.type == "Hutang") "Ngutang" else "Bayar"
                                        val typeColor = if (trans.type == "Hutang") Color.Red else Color(0xFF388E3C)

                                        Text(typeLabel, fontWeight = FontWeight.Bold, color = typeColor)
                                        Text(SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id")).format(Date(trans.date)), fontSize = 10.sp, color = Color.Gray)
                                    }

                                    val amountPrefix = if (trans.type == "Hutang") "+ " else "- "
                                    val amountColor = if (trans.type == "Hutang") Color.Red else Color(0xFF388E3C)

                                    Text(
                                        text = amountPrefix + formatRupiah(trans.amount),
                                        fontWeight = FontWeight.Bold,
                                        color = amountColor
                                    )
                                }
                                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG TRANSAKSI (Hutang / Bayar) ---
    if (showTransactionDialog && selectedCustomer != null) {
        AlertDialog(
            onDismissRequest = { showTransactionDialog = false },
            title = { Text(if(transactionType == "Bayar") "Terima Pembayaran" else "Tambah Catatan Hutang") },
            text = {
                Column {
                    Text("Pelanggan: ${selectedCustomer!!.name}", fontWeight = FontWeight.Bold)
                    Text("Sisa Hutang: ${formatRupiah(selectedCustomer!!.totalDebt)}", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = formatInput(it) },
                        label = { Text("Nominal (Rp)") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = cleanInput(amountInput)
                    if (amount > 0) {
                        if (transactionType == "Bayar") viewModel.payDebt(selectedCustomer!!, amount) else viewModel.addDebt(selectedCustomer!!, amount)
                        showTransactionDialog = false
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showTransactionDialog = false }) { Text("Batal") } }
        )
    }
}

// --- KOMPONEN PENDUKUNG ---

@Composable
fun SectionHeader(text: String, color: Color) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun CustomerDebtCard(
    customer: Customer,
    debtFormatted: String,
    lastUpdated: String,
    isPaidOff: Boolean,
    onClick: () -> Unit, // Callback untuk klik kartu
    onTransact: (Customer, String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }, // KLIK DIAKTIFKAN DISINI
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = if(isPaidOff) Icons.Default.CheckCircle else Icons.Default.Person,
                    contentDescription = null,
                    tint = if(isPaidOff) Color(0xFF388E3C) else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Info, null, modifier = Modifier.size(14.dp), tint = Color.LightGray) // Indikator info
                    }

                    if(customer.phoneNumber.isNotEmpty()) {
                        Text(customer.phoneNumber, fontSize = 12.sp, color = Color.Gray)
                    }
                    Text("Update: $lastUpdated", fontSize = 10.sp, color = Color.LightGray)
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sisa Hutang:", fontSize = 14.sp, color = Color.Gray)
                Text(
                    text = debtFormatted,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if(isPaidOff) Color(0xFF388E3C) else Color(0xFFD32F2F)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { onTransact(customer, "Hutang") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ngutang")
                }

                Button(
                    onClick = { onTransact(customer, "Bayar") },
                    modifier = Modifier.weight(1f),
                    enabled = !isPaidOff
                ) {
                    Text("Bayar")
                }
            }
        }
    }
}