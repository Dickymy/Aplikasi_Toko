package com.example.aplikasitokosembakoarkhan

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aplikasitokosembakoarkhan.data.Customer
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CustomerScreen(
    viewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val customers by viewModel.allCustomers.collectAsState()
    val context = LocalContext.current

    // State
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    // State Dialog Hapus
    var showDeleteErrorDialog by remember { mutableStateOf(false) }
    var showDeleteWarningDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }

    // LAUNCHER IMPORT KONTAK HP
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.importPhoneContacts(context) { count ->
                Toast.makeText(context, "Berhasil import $count kontak HP", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Izin kontak diperlukan untuk import", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredCustomers = customers.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery)
    }

    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace("Rp", "Rp ").replace(",00", "")
    }

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

            // --- HEADER: SEARCH & IMPORT KONTAK ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari Pelanggan...") },
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

                // TOMBOL IMPORT KONTAK HP
                FilledTonalButton(
                    onClick = { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp)
                ) {
                    Icon(Icons.Default.Contacts, null) // Ganti ikon kontak
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import Kontak")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- LIST PELANGGAN ---
            if (filteredCustomers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Data pelanggan tidak ditemukan", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(filteredCustomers) { customer ->
                        CustomerItemCard(
                            customer = customer,
                            debtFormatted = formatRupiah(customer.totalDebt),
                            onEdit = {
                                selectedCustomer = customer
                                nameInput = customer.name
                                phoneInput = customer.phoneNumber
                                showEditDialog = true
                            },
                            onDelete = {
                                selectedCustomer = customer
                                if (customer.totalDebt > 0) {
                                    showDeleteErrorDialog = true
                                } else if (customer.hasHistory) {
                                    showDeleteWarningDialog = true
                                } else {
                                    showDeleteConfirmDialog = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOGS ---
    // (Kode dialog Add, Edit, Delete sama seperti sebelumnya)
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah Pelanggan") },
            text = {
                Column {
                    OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Nama") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = phoneInput, onValueChange = { phoneInput = it }, label = { Text("No. HP") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { Button(onClick = { if (nameInput.isNotEmpty()) { viewModel.addCustomer(nameInput, phoneInput); showAddDialog = false } }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Batal") } }
        )
    }

    if (showEditDialog && selectedCustomer != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Pelanggan") },
            text = {
                Column {
                    OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Nama") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = phoneInput, onValueChange = { phoneInput = it }, label = { Text("No. HP") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (nameInput.isNotEmpty()) {
                        viewModel.updateCustomer(selectedCustomer!!.copy(name = nameInput, phoneNumber = phoneInput))
                        showEditDialog = false
                    }
                }) { Text("Update") }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Batal") } }
        )
    }

    if (showDeleteErrorDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteErrorDialog = false },
            icon = { Icon(Icons.Default.Error, null, tint = Color.Red) },
            title = { Text("Gagal Menghapus") },
            text = { Text("Pelanggan ini masih memiliki hutang sebesar ${formatRupiah(selectedCustomer?.totalDebt ?: 0.0)}.\n\nHarap lunasi hutang terlebih dahulu sebelum menghapus data.") },
            confirmButton = { Button(onClick = { showDeleteErrorDialog = false }) { Text("Mengerti") } }
        )
    }

    if (showDeleteWarningDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteWarningDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = Color(0xFFFFA000)) },
            title = { Text("Hapus Riwayat?") },
            text = { Text("Pelanggan ini sudah lunas, TETAPI memiliki riwayat transaksi.\n\nJika dihapus, SEMUA RIWAYAT hutang/bayar pelanggan ini akan HILANG permanen dari laporan.") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedCustomer?.let { viewModel.deleteCustomer(it) }
                        showDeleteWarningDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Hapus Semuanya") }
            },
            dismissButton = { TextButton(onClick = { showDeleteWarningDialog = false }) { Text("Batal") } }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Hapus Pelanggan?") },
            text = { Text("Yakin ingin menghapus ${selectedCustomer?.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedCustomer?.let { viewModel.deleteCustomer(it) }
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Batal") } }
        )
    }
}

// --- KOMPONEN KARTU ---
@Composable
fun CustomerItemCard(
    customer: Customer,
    debtFormatted: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (customer.totalDebt > 0) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = customer.name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = if (customer.totalDebt > 0) Color.Red else Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (customer.phoneNumber.isNotEmpty()) {
                    Text(customer.phoneNumber, fontSize = 12.sp, color = Color.Gray)
                } else {
                    Text("-", fontSize = 12.sp, color = Color.LightGray)
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (customer.totalDebt > 0) {
                    Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(4.dp)) {
                        Text("Hutang: $debtFormatted", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) {
                        Text("Bebas Hutang", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit", tint = Color.Blue)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Hapus", tint = Color.Red)
                }
            }
        }
    }
}