package com.example.aplikasitokosembakoarkhan

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Message
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
    var filterType by remember { mutableStateOf("Semua") }
    var sortOption by remember { mutableStateOf("A-Z") }
    var showFilterMenu by remember { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    // State Dialog Hapus
    var showDeleteErrorDialog by remember { mutableStateOf(false) }
    var showDeleteWarningDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // State Dialog Import
    var showImportConfirmDialog by remember { mutableStateOf(false) }

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

    // LOGIKA FILTER & SORTIR
    val filteredCustomers = customers
        .filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery)
        }
        .let { list ->
            when (sortOption) {
                "A-Z" -> list.sortedBy { it.name }
                "Z-A" -> list.sortedByDescending { it.name }
                "Terbaru" -> list.sortedByDescending { it.id }
                "Hutang Terbanyak" -> list.sortedByDescending { it.totalDebt }
                else -> list
            }
        }

    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace("Rp", "Rp ").replace(",00", "")
    }

    // Fungsi Helper Call/WA
    fun openDialer(phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka telepon", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsApp(phone: String) {
        try {
            var cleanPhone = phone.replace("[^\\d]".toRegex(), "")
            if (cleanPhone.startsWith("0")) cleanPhone = "62" + cleanPhone.substring(1)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanPhone"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp error/tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
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

            // --- HEADER ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 1. Search Bar
                // FIX: Menghapus properti colors yang bermasalah, menggunakan default
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari Pelanggan...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 2. Tombol Filter
                Box {
                    FilledTonalIconButton(
                        onClick = { showFilterMenu = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.FilterList, null)
                    }

                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        Text("Urutkan:", modifier = Modifier.padding(12.dp), fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        DropdownMenuItem(
                            text = { Text("Nama (A-Z)") },
                            onClick = { sortOption = "A-Z"; showFilterMenu = false },
                            leadingIcon = { if(sortOption == "A-Z") Icon(Icons.Default.Check, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Nama (Z-A)") },
                            onClick = { sortOption = "Z-A"; showFilterMenu = false },
                            leadingIcon = { if(sortOption == "Z-A") Icon(Icons.Default.Check, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Terbaru Ditambahkan") },
                            onClick = { sortOption = "Terbaru"; showFilterMenu = false },
                            leadingIcon = { if(sortOption == "Terbaru") Icon(Icons.Default.Check, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Hutang Terbanyak") },
                            onClick = { sortOption = "Hutang Terbanyak"; showFilterMenu = false },
                            leadingIcon = { if(sortOption == "Hutang Terbanyak") Icon(Icons.Default.Check, null) }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 3. Tombol Import
                FilledTonalIconButton(
                    onClick = { showImportConfirmDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.ImportContacts, null)
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
                            },
                            onCall = { openDialer(customer.phoneNumber) },
                            onWA = { openWhatsApp(customer.phoneNumber) }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOG ALERT IMPORT ---
    if (showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showImportConfirmDialog = false },
            icon = { Icon(Icons.Default.Contacts, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Import Kontak HP?") },
            text = { Text("Aplikasi akan membaca kontak di HP Anda dan menambahkannya ke daftar pelanggan.\n\nLanjutkan?") },
            confirmButton = {
                Button(onClick = {
                    showImportConfirmDialog = false
                    contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }) { Text("Ya, Import") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirmDialog = false }) { Text("Batal") }
            }
        )
    }

    // --- DIALOGS LAINNYA ---
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

// --- KOMPONEN KARTU (TATA LETAK BARU) ---
@Composable
fun CustomerItemCard(
    customer: Customer,
    debtFormatted: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit,
    onWA: () -> Unit
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
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // 1. Avatar (Kiri)
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

            Spacer(modifier = Modifier.width(16.dp))

            // 2. Info Tengah
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )

                if (customer.phoneNumber.isNotEmpty()) {
                    Text(
                        text = customer.phoneNumber,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tombol Call & WA (Di Bawah Nomor HP)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE3F2FD),
                            modifier = Modifier.size(32.dp).clickable { onCall() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Call, "Call", tint = Color(0xFF1565C0), modifier = Modifier.size(16.dp))
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.size(32.dp).clickable { onWA() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Message, "WA", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else {
                    Text("-", fontSize = 12.sp, color = Color.LightGray)
                }
            }

            // 3. Info Kanan
            Column(horizontalAlignment = Alignment.End) {
                if (customer.totalDebt > 0) {
                    Text(
                        text = "Hutang:",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = debtFormatted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tombol Edit & Delete
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, "Edit", tint = Color.Blue, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, "Hapus", tint = Color.Red, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}