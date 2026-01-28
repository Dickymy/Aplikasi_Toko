package com.example.aplikasitokosembakoarkhan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(
    viewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val customers by viewModel.allCustomers.collectAsState()

    // State
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var currentCustomer by remember { mutableStateOf<Customer?>(null) }

    // Input State
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    // Alert Hapus
    var showDeleteDialog by remember { mutableStateOf(false) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }

    // Filter Logic
    val filteredCustomers = customers.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery)
    }.sortedBy { it.name }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                name = ""; phone = ""; isEditing = false; showDialog = true
            }) { Icon(Icons.Default.Add, "Tambah") }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Data Pelanggan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            // --- SEARCH BAR (NEW) ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Cari Nama / No HP...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (filteredCustomers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Data tidak ditemukan", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(filteredCustomers) { customer ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = customer.name.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(customer.name, fontWeight = FontWeight.Bold)
                                    if (customer.phoneNumber.isNotEmpty()) {
                                        Text(customer.phoneNumber, fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                                IconButton(onClick = {
                                    name = customer.name
                                    phone = customer.phoneNumber
                                    currentCustomer = customer
                                    isEditing = true
                                    showDialog = true
                                }) { Icon(Icons.Default.Edit, "Edit", tint = Color.Blue) }
                                IconButton(onClick = {
                                    customerToDelete = customer
                                    showDeleteDialog = true
                                }) { Icon(Icons.Default.Delete, "Hapus", tint = Color.Gray) }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG INPUT
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (isEditing) "Edit Pelanggan" else "Pelanggan Baru") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        label = { Text("Nomor HP (Opsional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotEmpty()) {
                        if (isEditing && currentCustomer != null) {
                            viewModel.updateCustomer(currentCustomer!!.copy(name = name, phoneNumber = phone))
                        } else {
                            viewModel.addCustomer(name, phone)
                        }
                        showDialog = false
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Batal") } }
        )
    }

    // DIALOG HAPUS
    if (showDeleteDialog && customerToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Pelanggan?") },
            text = { Text("Hapus '${customerToDelete!!.name}' dari daftar?") },
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
}