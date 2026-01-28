package com.example.aplikasitokosembakoarkhan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtScreen(
    viewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val customers by viewModel.allCustomers.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // State Dialog Transaksi
    var showTransactionDialog by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var transactionType by remember { mutableStateOf("Hutang") }

    // State Dialog HAPUS (Konfirmasi)
    var showDeleteDialog by remember { mutableStateOf(false) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }

    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }

    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    }

    val totalPiutang = customers.sumOf { it.totalDebt }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { nameInput = ""; phoneInput = ""; showAddDialog = true }) {
                Icon(Icons.Default.Add, "Tambah Pelanggan")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Buku Kasbon / Hutang", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Total Piutang Toko: ${formatRupiah(totalPiutang)}", color = Color.Red, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(customers) { customer ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, null, tint = Color.Gray)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(customer.phoneNumber, fontSize = 12.sp, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    customerToDelete = customer
                                    showDeleteDialog = true // Trigger Dialog
                                }) {
                                    Icon(Icons.Default.Delete, "Hapus", tint = Color.Gray)
                                }
                            }
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Sisa Hutang:", fontSize = 14.sp)
                                Text(formatRupiah(customer.totalDebt), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if(customer.totalDebt > 0) Color.Red else Color(0xFF2E7D32))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = {
                                    selectedCustomer = customer
                                    transactionType = "Bayar"
                                    amountInput = ""
                                    showTransactionDialog = true
                                }, modifier = Modifier.weight(1f)) { Text("Bayar") }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = {
                                    selectedCustomer = customer
                                    transactionType = "Hutang"
                                    amountInput = ""
                                    showTransactionDialog = true
                                }, modifier = Modifier.weight(1f)) { Text("Ngutang") }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG KONFIRMASI HAPUS
    if (showDeleteDialog && customerToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Pelanggan?") },
            text = { Text("Yakin ingin menghapus '${customerToDelete!!.name}'? Data hutang akan hilang permanen.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(customerToDelete!!)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") } }
        )
    }

    // DIALOG TAMBAH PELANGGAN
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Pelanggan Baru") },
            text = {
                Column {
                    OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Nama Pelanggan") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = phoneInput, onValueChange = { phoneInput = it }, label = { Text("No. HP (Opsional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                }
            },
            confirmButton = { Button(onClick = { if(nameInput.isNotEmpty()) { viewModel.addCustomer(nameInput, phoneInput); showAddDialog = false } }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Batal") } }
        )
    }

    // DIALOG TRANSAKSI
    if (showTransactionDialog && selectedCustomer != null) {
        AlertDialog(
            onDismissRequest = { showTransactionDialog = false },
            title = { Text(if(transactionType == "Bayar") "Terima Pembayaran" else "Tambah Catatan Hutang") },
            text = {
                Column {
                    Text("Pelanggan: ${selectedCustomer!!.name}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { if(it.all { c -> c.isDigit() }) amountInput = it },
                        label = { Text("Nominal (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = amountInput.toDoubleOrNull() ?: 0.0
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