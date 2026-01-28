package com.example.aplikasitokosembakoarkhan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.example.aplikasitokosembakoarkhan.data.DebtTransaction
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
    var selectedFilter by remember { mutableStateOf("Semua") } // Semua, Hutang, Lunas, Bersih
    var showFilterDialog by remember { mutableStateOf(false) }

    // Dialog State
    var showAddDialog by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf(false) } // Dialog Detail + Riwayat
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Temp Data
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }

    // Input Fields
    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }

    // --- HELPERS ---
    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace("Rp", "Rp ").replace(",00", "")
    }

    fun formatDate(millis: Long): String {
        return if (millis > 0) SimpleDateFormat("dd MMM yy", Locale("id", "ID")).format(Date(millis)) else "-"
    }

    // --- FILTER LOGIC ---
    val filteredCustomers = customers.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery)
    }

    // Grouping
    val debtors = filteredCustomers.filter { it.totalDebt > 0 }.sortedByDescending { it.totalDebt }
    val paidOff = filteredCustomers.filter { it.totalDebt <= 0 && it.hasHistory }.sortedByDescending { it.lastUpdated }
    val cleanUsers = filteredCustomers.filter { it.totalDebt <= 0 && !it.hasHistory }.sortedBy { it.name }

    val totalPiutang = customers.sumOf { it.totalDebt }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { nameInput = ""; phoneInput = ""; showAddDialog = true }) {
                Icon(Icons.Default.Add, "Tambah")
            }
        }
    ) { padding ->
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
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Tombol Filter
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

            if(selectedFilter != "Semua") {
                Text("Filter: $selectedFilter", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
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
                        CustomerCard(customer, formatRupiah(customer.totalDebt), formatDate(customer.lastUpdated),
                            statusColor = Color(0xFFD32F2F), statusText = "Belum Lunas",
                            onClick = { selectedCustomer = customer; showDetailDialog = true },
                            onDelete = { customerToDelete = customer; showDeleteDialog = true }
                        )
                    }
                    if (selectedFilter == "Semua") item { Divider(Modifier.padding(vertical = 12.dp)) }
                }

                // GROUP 2: LUNAS (Pernah Hutang)
                if ((selectedFilter == "Semua" || selectedFilter == "Lunas") && paidOff.isNotEmpty()) {
                    item { SectionHeader("Lunas / Riwayat Ada (${paidOff.size})", Color(0xFF388E3C)) }
                    items(paidOff) { customer ->
                        CustomerCard(customer, "Rp 0", formatDate(customer.lastUpdated),
                            statusColor = Color(0xFF388E3C), statusText = "Lunas",
                            onClick = { selectedCustomer = customer; showDetailDialog = true },
                            onDelete = { customerToDelete = customer; showDeleteDialog = true }
                        )
                    }
                    if (selectedFilter == "Semua") item { Divider(Modifier.padding(vertical = 12.dp)) }
                }

                // GROUP 3: BERSIH (Tidak Pernah Hutang)
                if ((selectedFilter == "Semua" || selectedFilter == "Bersih") && cleanUsers.isNotEmpty()) {
                    item { SectionHeader("Tidak Punya Hutang (${cleanUsers.size})", Color.Gray) }
                    items(cleanUsers) { customer ->
                        CustomerCard(customer, "-", "-",
                            statusColor = Color.Gray, statusText = "Tidak Hutang",
                            onClick = { selectedCustomer = customer; showDetailDialog = true },
                            onDelete = { customerToDelete = customer; showDeleteDialog = true }
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

    // --- DIALOG DETAIL & RIWAYAT ---
    if (showDetailDialog && selectedCustomer != null) {
        DetailHistoryDialog(
            customer = selectedCustomer!!,
            viewModel = viewModel,
            onDismiss = { showDetailDialog = false },
            formatRupiah = { formatRupiah(it) },
            formatDate = { formatDate(it) }
        )
    }

    // --- DIALOG HAPUS ---
    if (showDeleteDialog && customerToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Pelanggan?") },
            text = { Text("Yakin ingin menghapus '${customerToDelete!!.name}'? Semua data dan riwayat hutang akan hilang permanen.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteCustomer(customerToDelete!!); showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") } }
        )
    }

    // --- DIALOG TAMBAH ---
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Pelanggan Baru") },
            text = {
                Column {
                    OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Nama Pelanggan") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = phoneInput, onValueChange = { phoneInput = it }, label = { Text("No. HP (Opsional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { Button(onClick = { if(nameInput.isNotEmpty()) { viewModel.addCustomer(nameInput, phoneInput); showAddDialog = false } }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Batal") } }
        )
    }
}

// --- KOMPONEN KARTU PELANGGAN (LIST UTAMA) ---
@Composable
fun CustomerCard(
    customer: Customer,
    debtFormatted: String,
    lastUpdated: String,
    statusColor: Color,
    statusText: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(statusText, fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.Bold)
                if(customer.phoneNumber.isNotEmpty()) Text(customer.phoneNumber, fontSize = 12.sp, color = Color.Gray)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(debtFormatted, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = statusColor)
                Text(lastUpdated, fontSize = 10.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // ICON HAPUS MERAH
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, "Hapus", tint = Color.Red)
            }
        }
    }
}

@Composable
fun SectionHeader(text: String, color: Color) {
    Text(text, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.padding(vertical = 8.dp))
}

// --- DIALOG DETAIL & RIWAYAT (KOMPLEKS) ---
@Composable
fun DetailHistoryDialog(
    customer: Customer,
    viewModel: InventoryViewModel,
    onDismiss: () -> Unit,
    formatRupiah: (Double) -> String,
    formatDate: (Long) -> String
) {
    val history by viewModel.getDebtHistory(customer.id).collectAsState(initial = emptyList())
    var showInput by remember { mutableStateOf(false) }
    var inputType by remember { mutableStateOf("Hutang") } // Hutang / Bayar
    var amountText by remember { mutableStateOf("") }

    // Helper Clean Input
    fun cleanInput(input: String): Double = input.replace(".", "").replace(",", "").toDoubleOrNull() ?: 0.0
    fun formatInput(input: String): String {
        val clean = input.replace("[^\\d]".toRegex(), "")
        return if (clean.isNotEmpty()) try {
            NumberFormat.getInstance(Locale("id", "ID")).format(clean.toLong())
        } catch (e: Exception) { clean } else ""
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(if(customer.phoneNumber.isNotEmpty()) customer.phoneNumber else "Tanpa No HP", fontSize = 12.sp, color = Color.Gray)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Total Hutang Besar
                Card(colors = CardDefaults.cardColors(containerColor = if(customer.totalDebt > 0) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Sisa Hutang", fontWeight = FontWeight.SemiBold)
                        Text(formatRupiah(customer.totalDebt), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = if(customer.totalDebt > 0) Color.Red else Color(0xFF2E7D32))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- AREA INPUT TRANSAKSI (Expandable) ---
                if (showInput) {
                    Column(modifier = Modifier.background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)).padding(12.dp)) {
                        Text(if(inputType == "Hutang") "Tambah Hutang Baru" else "Terima Pembayaran", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = amountText, onValueChange = { amountText = formatInput(it) },
                            label = { Text("Nominal (Rp)") },
                            prefix = { Text("Rp ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { showInput = false }) { Text("Batal") }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                val amount = cleanInput(amountText)
                                if (amount > 0) {
                                    if(inputType == "Hutang") viewModel.addDebt(customer, amount) else viewModel.payDebt(customer, amount)
                                    showInput = false
                                }
                            }) { Text("Simpan") }
                        }
                    }
                } else {
                    // Tombol Aksi
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { inputType = "Bayar"; amountText = ""; showInput = true },
                            modifier = Modifier.weight(1f),
                            enabled = customer.totalDebt > 0
                        ) { Text("Bayar") }

                        Button(
                            onClick = { inputType = "Hutang"; amountText = ""; showInput = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) { Text("Ngutang") }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Riwayat Transaksi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // --- LIST RIWAYAT ---
                if (history.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Belum ada riwayat", color = Color.LightGray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(history) { trans ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(if(trans.type == "Hutang") "Ngutang" else "Bayar", fontWeight = FontWeight.Bold, color = if(trans.type == "Hutang") Color.Red else Color(0xFF388E3C))
                                    Text(SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id")).format(Date(trans.date)), fontSize = 10.sp, color = Color.Gray)
                                }
                                Text(
                                    text = (if(trans.type=="Hutang") "+ " else "- ") + formatRupiah(trans.amount),
                                    fontWeight = FontWeight.Bold,
                                    color = if(trans.type == "Hutang") Color.Red else Color(0xFF388E3C)
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