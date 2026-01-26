package com.example.aplikasitokosembakoarkhan

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aplikasitokosembakoarkhan.data.Customer
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtScreen(viewModel: ProductViewModel) {
    val customerList by viewModel.allCustomers.collectAsState()
    val context = LocalContext.current

    // State Dialog
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showTransactionDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // State Data Sementara
    var tempName by remember { mutableStateOf("") }
    var tempPhone by remember { mutableStateOf("") }
    var tempAmount by remember { mutableStateOf("") }

    // Untuk mengetahui pelanggan mana yang sedang diproses
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var isPaying by remember { mutableStateOf(false) } // True = Bayar, False = Ngutang Lagi

    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    tempName = ""
                    tempPhone = ""
                    showAddCustomerDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Pelanggan")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            Text("Buku Kasbon", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Catatan hutang pelanggan", fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(16.dp))

            // Total Piutang Toko (Uang toko di luar)
            val totalPiutang = customerList.sumOf { it.totalDebt }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Uang di Luar", fontSize = 12.sp, color = Color.Red)
                        Text(formatRupiah(totalPiutang), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                    }
                    Icon(Icons.Default.MoneyOff, null, tint = Color.Red)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // List Pelanggan
            if (customerList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada data pelanggan.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(customerList) { customer ->
                        CustomerItem(
                            customer = customer,
                            formatRupiah = { formatRupiah(it) },
                            onAddDebt = {
                                selectedCustomer = it
                                isPaying = false
                                tempAmount = ""
                                showTransactionDialog = true
                            },
                            onPayDebt = {
                                selectedCustomer = it
                                isPaying = true
                                tempAmount = ""
                                showTransactionDialog = true
                            },
                            onDelete = {
                                selectedCustomer = it
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOG 1: TAMBAH PELANGGAN BARU ---
    if (showAddCustomerDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            title = { Text("Pelanggan Baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = tempName, onValueChange = { tempName = it }, label = { Text("Nama Pelanggan") }, leadingIcon = { Icon(Icons.Default.Person, null) })
                    OutlinedTextField(value = tempPhone, onValueChange = { tempPhone = it }, label = { Text("No. HP (Opsional)") }, leadingIcon = { Icon(Icons.Default.Phone, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (tempName.isNotEmpty()) {
                        viewModel.addCustomer(tempName, tempPhone)
                        showAddCustomerDialog = false
                        Toast.makeText(context, "Pelanggan ditambahkan", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showAddCustomerDialog = false }) { Text("Batal") } }
        )
    }

    // --- DIALOG 2: TRANSAKSI (TAMBAH HUTANG / BAYAR) ---
    if (showTransactionDialog && selectedCustomer != null) {
        AlertDialog(
            onDismissRequest = { showTransactionDialog = false },
            title = { Text(if (isPaying) "Bayar Hutang" else "Tambah Hutang") },
            text = {
                Column {
                    Text("Pelanggan: ${selectedCustomer?.name}", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempAmount,
                        onValueChange = { if (it.all { c -> c.isDigit() }) tempAmount = it },
                        label = { Text("Nominal (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = tempAmount.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            if (isPaying) {
                                viewModel.payDebt(selectedCustomer!!, amount)
                                Toast.makeText(context, "Pembayaran diterima!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.addDebt(selectedCustomer!!, amount)
                                Toast.makeText(context, "Hutang dicatat!", Toast.LENGTH_SHORT).show()
                            }
                            showTransactionDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isPaying) Color(0xFF4CAF50) else Color(0xFFE53935))
                ) { Text(if (isPaying) "TERIMA BAYARAN" else "CATAT HUTANG") }
            },
            dismissButton = { TextButton(onClick = { showTransactionDialog = false }) { Text("Batal") } }
        )
    }

    // --- DIALOG 3: HAPUS PELANGGAN ---
    if (showDeleteDialog && selectedCustomer != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Pelanggan?") },
            text = { Text("Yakin ingin menghapus '${selectedCustomer?.name}'? Riwayat hutang akan hilang.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCustomer(selectedCustomer!!)
                    showDeleteDialog = false
                    Toast.makeText(context, "Dihapus", Toast.LENGTH_SHORT).show()
                }) { Text("Hapus", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") } }
        )
    }
}

@Composable
fun CustomerItem(
    customer: Customer,
    formatRupiah: (Double) -> String,
    onAddDebt: (Customer) -> Unit,
    onPayDebt: (Customer) -> Unit,
    onDelete: (Customer) -> Unit
) {
    // Format Waktu Terakhir Update
    val lastUpdateStr = if (customer.lastUpdated > 0L) {
        SimpleDateFormat("dd MMM, HH:mm", Locale("id")).format(Date(customer.lastUpdated))
    } else {
        "-"
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (customer.phoneNumber.isNotEmpty()) {
                        Text(customer.phoneNumber, fontSize = 12.sp, color = Color.Gray)
                    }
                }
                IconButton(onClick = { onDelete(customer) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, "Hapus", tint = Color.LightGray)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sisa Hutang:", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        formatRupiah(customer.totalDebt),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (customer.totalDebt > 0) Color.Red else Color(0xFF388E3C)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Update: $lastUpdateStr", fontSize = 10.sp, color = Color.Gray)
                }

                // Tombol Aksi
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onAddDebt(customer) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("+ Ngutang", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { onPayDebt(customer) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        enabled = customer.totalDebt > 0
                    ) {
                        Text("Bayar", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}