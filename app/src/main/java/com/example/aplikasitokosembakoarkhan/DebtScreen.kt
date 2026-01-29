package com.example.aplikasitokosembakoarkhan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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

    // --- STATE ---
    var searchQuery by remember { mutableStateOf("") }

    // Filter & Sort State
    var selectedFilter by remember { mutableStateOf("Hutang") }
    var sortOption by remember { mutableStateOf("Tertinggi") }
    var showSortDialog by remember { mutableStateOf(false) }

    // Dialog State
    var showTransactionDialog by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf(false) }

    // Temp Data for Logic
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var selectedCustomerForDetail by remember { mutableStateOf<Customer?>(null) }
    var transactionType by remember { mutableStateOf("Hutang") }
    var amountInput by remember { mutableStateOf("") }

    // --- HELPERS (LOGIKA FORMAT RUPIAH REAL-TIME) ---

    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace("Rp", "Rp ").replace(",00", "")
    }

    // Fungsi untuk memformat input saat diketik (10000 -> 10.000)
    fun formatInput(input: String): String {
        // 1. Hapus semua karakter yang bukan angka
        val clean = input.replace("[^\\d]".toRegex(), "")

        // 2. Jika kosong, kembalikan kosong
        if (clean.isEmpty()) return ""

        // 3. Format ke ribuan Indonesia
        return try {
            val number = clean.toLong()
            NumberFormat.getInstance(Locale("id", "ID")).format(number)
        } catch (e: Exception) {
            clean // Fallback jika angka terlalu besar
        }
    }

    // Fungsi untuk membersihkan format rupiah kembali ke Double agar bisa disimpan (10.000 -> 10000.0)
    fun cleanInput(input: String): Double {
        return input.replace(".", "").replace(",", "").toDoubleOrNull() ?: 0.0
    }

    fun formatDate(millis: Long): String = if (millis > 0) SimpleDateFormat("dd MMM yy", Locale("id", "ID")).format(Date(millis)) else "-"

    // --- LOGIKA FILTER & SORTIR ---
    val filteredList = customers.filter {
        (it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery)) &&
                when (selectedFilter) {
                    "Hutang" -> it.totalDebt > 0
                    "Lunas" -> it.totalDebt <= 0 && it.hasHistory
                    "Bersih" -> it.totalDebt <= 0 && !it.hasHistory
                    else -> true // Semua
                }
    }.let { list ->
        when (sortOption) {
            "Tertinggi" -> list.sortedByDescending { it.totalDebt }
            "Terendah" -> list.sortedBy { it.totalDebt }
            "Terlama" -> list.sortedBy { if(it.lastUpdated > 0) it.lastUpdated else Long.MAX_VALUE }
            "Terbaru" -> list.sortedByDescending { it.lastUpdated }
            "Abjad A-Z" -> list.sortedBy { it.name }
            else -> list
        }
    }

    val totalPiutang = customers.sumOf { it.totalDebt }
    val debtorCount = customers.count { it.totalDebt > 0 }

    Scaffold(
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // --- HEADER TOTAL ---
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Piutang Toko", fontSize = 12.sp, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(formatRupiah(totalPiutang), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color(0xFFC62828))
                        Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(8.dp)) {
                            Text("$debtorCount Orang", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color(0xFFC62828), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- SEARCH & FILTER BAR ---
            Column(modifier = Modifier.padding(16.dp)) {
                // Search & Sort Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari Pelanggan...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalIconButton(onClick = { showSortDialog = true }) {
                        Icon(Icons.Default.Sort, "Urutkan")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Chips (Scrollable Horizontal)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filters = listOf("Semua", "Hutang", "Lunas", "Bersih")
                    items(filters.size) { index ->
                        val filter = filters[index]
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            leadingIcon = if (selectedFilter == filter) { { Icon(Icons.Default.Check, null) } } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if(filter == "Hutang") Color(0xFFFFEBEE) else MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = if(filter == "Hutang") Color(0xFFC62828) else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // --- LIST PELANGGAN ---
            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Text("Data tidak ditemukan", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList) { customer ->
                        CustomerDebtCardImproved(
                            customer = customer,
                            debtFormatted = formatRupiah(customer.totalDebt),
                            lastUpdated = formatDate(customer.lastUpdated),
                            onClick = {
                                selectedCustomerForDetail = customer
                                showDetailDialog = true
                            },
                            onTransact = { c, type ->
                                selectedCustomer = c
                                transactionType = type
                                amountInput = ""
                                showTransactionDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOG SORTIR ---
    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Urutkan Berdasarkan") },
            text = {
                Column {
                    listOf("Tertinggi", "Terendah", "Terbaru", "Terlama", "Abjad A-Z").forEach { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { sortOption = option; showSortDialog = false }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = sortOption == option, onClick = null)
                            Text(option, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSortDialog = false }) { Text("Tutup") } }
        )
    }

    // --- DIALOG DETAIL RIWAYAT (TIMELINE LOOK) ---
    if (showDetailDialog && selectedCustomerForDetail != null) {
        val detailCustomer = selectedCustomerForDetail!!
        val history by viewModel.getDebtHistory(detailCustomer.id).collectAsState(initial = emptyList())

        Dialog(onDismissRequest = { showDetailDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().heightIn(max = 650.dp)) {
                Column {
                    // Header Detail
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(detailCustomer.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(if(detailCustomer.phoneNumber.isNotEmpty()) detailCustomer.phoneNumber else "-", fontSize = 12.sp)
                        }
                        IconButton(onClick = { showDetailDialog = false }) { Icon(Icons.Default.Close, "Tutup") }
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        // Sisa Hutang Big Card
                        Card(colors = CardDefaults.cardColors(containerColor = if (detailCustomer.totalDebt > 0) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Sisa Hutang Saat Ini", fontSize = 12.sp, color = Color.Gray)
                                Text(formatRupiah(detailCustomer.totalDebt), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = if (detailCustomer.totalDebt > 0) Color(0xFFD32F2F) else Color(0xFF388E3C))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Riwayat Transaksi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (history.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Text("Belum ada riwayat", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(history) { trans ->
                                    val isHutang = trans.type == "Hutang"
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                                        // Icon Indicator
                                        Box(
                                            modifier = Modifier.size(32.dp).clip(CircleShape).background(if (isHutang) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isHutang) Icons.Default.Remove else Icons.Default.Add,
                                                contentDescription = null,
                                                tint = if (isHutang) Color.Red else Color.Green,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(if (isHutang) "Tambah Hutang" else "Pembayaran", fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = (if (isHutang) "+ " else "- ") + formatRupiah(trans.amount),
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isHutang) Color(0xFFD32F2F) else Color(0xFF388E3C)
                                                )
                                            }
                                            Text(SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("id")).format(Date(trans.date)), fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                    Divider(color = Color.LightGray.copy(alpha = 0.2f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG INPUT TRANSAKSI (AUTO FORMAT RUPIAH) ---
    if (showTransactionDialog && selectedCustomer != null) {
        AlertDialog(
            onDismissRequest = { showTransactionDialog = false },
            title = { Text(if(transactionType == "Bayar") "Terima Pembayaran" else "Catat Hutang Baru") },
            text = {
                Column {
                    Text(selectedCustomer!!.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Sisa Hutang: ${formatRupiah(selectedCustomer!!.totalDebt)}", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    // INPUT DENGAN FORMAT REAL-TIME
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = {
                            // Terapkan formatInput setiap kali user mengetik
                            amountInput = formatInput(it)
                        },
                        label = { Text("Nominal (Rp)") },
                        prefix = { Text("Rp ") },
                        placeholder = { Text("Contoh: 10.000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = cleanInput(amountInput)
                        if (amount > 0) {
                            if (transactionType == "Bayar") viewModel.payDebt(selectedCustomer!!, amount) else viewModel.addDebt(selectedCustomer!!, amount)
                            showTransactionDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if(transactionType == "Bayar") Color(0xFF388E3C) else Color(0xFFD32F2F))
                ) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showTransactionDialog = false }) { Text("Batal") } }
        )
    }
}

// --- KOMPONEN KARTU PELANGGAN ---
@Composable
fun CustomerDebtCardImproved(
    customer: Customer,
    debtFormatted: String,
    lastUpdated: String,
    onClick: () -> Unit,
    onTransact: (Customer, String) -> Unit
) {
    val isPaidOff = customer.totalDebt <= 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }, // Indikasi klik di seluruh kartu
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // HEADER KARTU
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Inisial
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isPaidOff) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = customer.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = if (isPaidOff) Color(0xFF388E3C) else Color(0xFFD32F2F)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Info Nama
                Column(modifier = Modifier.weight(1f)) {
                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (customer.phoneNumber.isNotEmpty()) {
                        Text(customer.phoneNumber, fontSize = 12.sp, color = Color.Gray)
                    }
                    Text("Update: $lastUpdated", fontSize = 10.sp, color = Color.LightGray)
                }

                // Indikator Klik (Chevron)
                Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.LightGray)
            }

            Divider(color = Color.LightGray.copy(alpha = 0.2f))

            // BODY KARTU (Nominal Hutang)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Hutang", fontSize = 11.sp, color = Color.Gray)
                    if (isPaidOff) {
                        Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) {
                            Text("LUNAS", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color(0xFF388E3C), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    } else {
                        Text(debtFormatted, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFFD32F2F))
                    }
                }
            }

            // FOOTER KARTU (Tombol Aksi)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFAFAFA))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tombol Ngutang
                OutlinedButton(
                    onClick = { onTransact(customer, "Hutang") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                ) {
                    Icon(Icons.Default.AddCircleOutline, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ngutang", fontSize = 13.sp)
                }

                // Tombol Bayar
                Button(
                    onClick = { onTransact(customer, "Bayar") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isPaidOff,
                    contentPadding = PaddingValues(vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                ) {
                    Icon(Icons.Default.AttachMoney, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bayar", fontSize = 13.sp)
                }
            }
        }
    }
}