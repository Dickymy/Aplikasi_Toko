package com.example.aplikasitokosembakoarkhan

import androidx.compose.foundation.background
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

    // State
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showTransactionDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Temp Data
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }
    var transactionType by remember { mutableStateOf("Hutang") } // "Hutang" atau "Bayar"

    // Input Fields
    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
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
        return if (millis > 0) SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date(millis)) else "-"
    }

    // --- FILTER & SORTING ---
    // 1. Filter nama/hp
    val filteredCustomers = customers.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery)
    }

    // 2. Pisahkan Hutang vs Lunas
    val debtors = filteredCustomers.filter { it.totalDebt > 0 }.sortedByDescending { it.totalDebt }
    val nonDebtors = filteredCustomers.filter { it.totalDebt <= 0 }.sortedBy { it.name }

    val totalPiutang = customers.sumOf { it.totalDebt }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { nameInput = ""; phoneInput = ""; showAddDialog = true }) {
                Icon(Icons.Default.Add, "Tambah Pelanggan")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp) // Layout Rapat ke Atas
        ) {

            // --- HEADER SEARCH (Compact) ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari Nama / No HP...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- KARTU RINGKASAN TOTAL ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), // Merah muda lembut
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

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // --- SECTION: YANG PUNYA HUTANG ---
                if (debtors.isNotEmpty()) {
                    item {
                        Text("Belum Lunas (${debtors.size})", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    items(debtors) { customer ->
                        CustomerDebtCard(
                            customer = customer,
                            debtFormatted = formatRupiah(customer.totalDebt),
                            lastUpdated = formatDate(customer.lastUpdated),
                            isPaidOff = false,
                            onDelete = { customerToDelete = customer; showDeleteDialog = true },
                            onTransact = { c, type -> selectedCustomer = c; transactionType = type; amountInput = ""; showTransactionDialog = true }
                        )
                    }
                }

                // --- PEMBATAS / DIVIDER ---
                if (debtors.isNotEmpty() && nonDebtors.isNotEmpty()) {
                    item {
                        Divider(modifier = Modifier.padding(vertical = 16.dp), thickness = 1.dp, color = Color.LightGray)
                    }
                }

                // --- SECTION: LUNAS / TIDAK ADA HUTANG ---
                if (nonDebtors.isNotEmpty()) {
                    item {
                        Text("Lunas / Bersih (${nonDebtors.size})", color = Color(0xFF388E3C), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    items(nonDebtors) { customer ->
                        CustomerDebtCard(
                            customer = customer,
                            debtFormatted = "Lunas",
                            lastUpdated = formatDate(customer.lastUpdated),
                            isPaidOff = true,
                            onDelete = { customerToDelete = customer; showDeleteDialog = true },
                            onTransact = { c, type -> selectedCustomer = c; transactionType = type; amountInput = ""; showTransactionDialog = true }
                        )
                    }
                }

                if (debtors.isEmpty() && nonDebtors.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Belum ada data pelanggan", color = Color.Gray)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // --- DIALOGS ---

    // 1. DIALOG KONFIRMASI HAPUS
    if (showDeleteDialog && customerToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Pelanggan?") },
            text = { Text("Yakin ingin menghapus '${customerToDelete!!.name}'? Riwayat hutang akan hilang.") },
            confirmButton = {
                Button(onClick = { viewModel.deleteCustomer(customerToDelete!!); showDeleteDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") } }
        )
    }

    // 2. DIALOG TAMBAH PELANGGAN
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Pelanggan Baru") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nameInput, onValueChange = { nameInput = it },
                        label = { Text("Nama Pelanggan") }, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phoneInput, onValueChange = { phoneInput = it },
                        label = { Text("No. HP (Opsional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = { Button(onClick = { if(nameInput.isNotEmpty()) { viewModel.addCustomer(nameInput, phoneInput); showAddDialog = false } }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Batal") } }
        )
    }

    // 3. DIALOG TRANSAKSI (HUTANG / BAYAR)
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
                        onValueChange = { amountInput = formatInput(it) }, // Auto Format
                        label = { Text("Nominal (Rp)") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = cleanInput(amountInput) // Bersihkan Format
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

// --- KOMPONEN KARTU PELANGGAN ---
@Composable
fun CustomerDebtCard(
    customer: Customer,
    debtFormatted: String,
    lastUpdated: String,
    isPaidOff: Boolean,
    onDelete: () -> Unit,
    onTransact: (Customer, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Nama & Delete
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = if(isPaidOff) Icons.Default.CheckCircle else Icons.Default.Person,
                    contentDescription = null,
                    tint = if(isPaidOff) Color(0xFF388E3C) else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if(customer.phoneNumber.isNotEmpty()) {
                        Text(customer.phoneNumber, fontSize = 12.sp, color = Color.Gray)
                    }
                    Text("Update: $lastUpdated", fontSize = 10.sp, color = Color.LightGray)
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, "Hapus", tint = Color.LightGray)
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Info Hutang
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

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                // Tombol Ngutang
                OutlinedButton(
                    onClick = { onTransact(customer, "Hutang") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ngutang")
                }

                // Tombol Bayar
                Button(
                    onClick = { onTransact(customer, "Bayar") },
                    modifier = Modifier.weight(1f),
                    enabled = !isPaidOff // Disable jika sudah lunas
                ) {
                    Text("Bayar")
                }
            }
        }
    }
}