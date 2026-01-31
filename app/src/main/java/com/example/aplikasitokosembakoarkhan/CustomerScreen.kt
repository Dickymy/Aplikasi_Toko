package com.example.aplikasitokosembakoarkhan

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class) // Tambahkan ExperimentalMaterial3Api
@Composable
fun CustomerScreen(
    viewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val customers by viewModel.allCustomers.collectAsState()
    val context = LocalContext.current

    // --- STATES ---
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf("A-Z") }
    var showFilterMenu by remember { mutableStateOf(false) }

    // Multi-Selection State
    val selectedCustomers = remember { mutableStateListOf<Customer>() }
    var isSelectionMode by remember { mutableStateOf(false) }

    // Dialogs
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var showBulkDeleteResultDialog by remember { mutableStateOf<String?>(null) } // Pesan hasil hapus

    var selectedCustomerForEdit by remember { mutableStateOf<Customer?>(null) } // Khusus Edit Single
    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }

    // Back Handler untuk keluar dari mode seleksi
    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedCustomers.clear()
    }

    // LAUNCHER IMPORT
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.importPhoneContacts(context) { count -> Toast.makeText(context, "Berhasil import $count kontak", Toast.LENGTH_SHORT).show() }
        else Toast.makeText(context, "Izin kontak diperlukan", Toast.LENGTH_SHORT).show()
    }

    // LOGIKA FILTER
    val filteredCustomers = customers
        .filter { it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery) }
        .let { list ->
            when (sortOption) {
                "A-Z" -> list.sortedBy { it.name }
                "Z-A" -> list.sortedByDescending { it.name }
                "Terbaru" -> list.sortedByDescending { it.id }
                "Hutang Terbanyak" -> list.sortedByDescending { it.totalDebt }
                else -> list
            }
        }

    fun formatRupiah(amount: Double): String = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace("Rp", "Rp ").replace(",00", "")

    fun toggleSelection(customer: Customer) {
        if (selectedCustomers.contains(customer)) {
            selectedCustomers.remove(customer)
            if (selectedCustomers.isEmpty()) isSelectionMode = false
        } else {
            selectedCustomers.add(customer)
        }
    }

    fun selectAll() {
        if (selectedCustomers.size == filteredCustomers.size) {
            selectedCustomers.clear() // Deselect All
            isSelectionMode = false
        } else {
            selectedCustomers.clear()
            selectedCustomers.addAll(filteredCustomers) // Select All
            isSelectionMode = true
        }
    }

    fun deleteSelectedCustomers() {
        val toDelete = selectedCustomers.toList()
        var successCount = 0
        var failCount = 0 // Yang ada hutang

        toDelete.forEach { c ->
            if (c.totalDebt > 0) {
                failCount++
            } else {
                viewModel.deleteCustomer(c)
                successCount++
            }
        }

        selectedCustomers.clear()
        isSelectionMode = false
        showDeleteConfirmDialog = false

        if (failCount > 0) {
            showBulkDeleteResultDialog = "$successCount Pelanggan dihapus.\n$failCount Gagal dihapus karena masih memiliki hutang."
        } else {
            Toast.makeText(context, "$successCount Pelanggan berhasil dihapus", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedCustomers.size} Dipilih") },
                    navigationIcon = {
                        IconButton(onClick = { isSelectionMode = false; selectedCustomers.clear() }) {
                            Icon(Icons.Default.Close, "Batal")
                        }
                    },
                    actions = {
                        // Tombol Select All
                        IconButton(onClick = { selectAll() }) {
                            Icon(
                                if (selectedCustomers.size == filteredCustomers.size) Icons.Default.Deselect else Icons.Default.SelectAll,
                                "Pilih Semua"
                            )
                        }
                        // Tombol Hapus
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, "Hapus", tint = Color.Red)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(onClick = { nameInput = ""; phoneInput = ""; showAddDialog = true }) {
                    Icon(Icons.Default.Add, "Tambah")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            if (!isSelectionMode) {
                Spacer(Modifier.height(8.dp))
                // HEADER NORMAL
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Box {
                        FilledTonalIconButton(onClick = { showFilterMenu = true }, shape = RoundedCornerShape(8.dp), modifier = Modifier.size(56.dp)) { Icon(Icons.Default.FilterList, null) }
                        DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                            DropdownMenuItem(text = { Text("Nama (A-Z)") }, onClick = { sortOption = "A-Z"; showFilterMenu = false })
                            DropdownMenuItem(text = { Text("Hutang Terbanyak") }, onClick = { sortOption = "Hutang Terbanyak"; showFilterMenu = false })
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalIconButton(onClick = { showImportConfirmDialog = true }, shape = RoundedCornerShape(8.dp), modifier = Modifier.size(56.dp)) { Icon(Icons.Default.ImportContacts, null) }
                }

                // INFO TOTAL DATA
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Total Pelanggan: ${customers.size} Data",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // LIST DATA
            if (filteredCustomers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Data tidak ditemukan", color = Color.Gray) }
            } else {
                LazyColumn {
                    items(filteredCustomers) { customer ->
                        val isSelected = selectedCustomers.contains(customer)

                        CustomerItemCardSelectable(
                            customer = customer,
                            debtFormatted = formatRupiah(customer.totalDebt),
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onLongClick = {
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedCustomers.add(customer)
                                }
                            },
                            onClick = {
                                if (isSelectionMode) {
                                    toggleSelection(customer)
                                } else {
                                    // Aksi Klik Biasa (Edit)
                                    selectedCustomerForEdit = customer
                                    nameInput = customer.name
                                    phoneInput = customer.phoneNumber
                                    showEditDialog = true
                                }
                            },
                            onCall = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phoneNumber}"))
                                try { context.startActivity(intent) } catch (e:Exception){}
                            },
                            onWA = {
                                try {
                                    var num = customer.phoneNumber.replace("[^\\d]".toRegex(), "")
                                    if(num.startsWith("0")) num = "62${num.substring(1)}"
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$num")))
                                } catch(e:Exception){}
                            }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    // 1. ADD DIALOG
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah Pelanggan") },
            text = { Column { OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Nama") }); Spacer(Modifier.height(8.dp)); OutlinedTextField(value = phoneInput, onValueChange = { phoneInput = it }, label = { Text("No. HP") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)) } },
            confirmButton = { Button(onClick = { if(nameInput.isNotEmpty()){ viewModel.addCustomer(nameInput, phoneInput); showAddDialog = false } }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Batal") } }
        )
    }

    // 2. EDIT DIALOG
    if (showEditDialog && selectedCustomerForEdit != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Pelanggan") },
            text = { Column { OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Nama") }); Spacer(Modifier.height(8.dp)); OutlinedTextField(value = phoneInput, onValueChange = { phoneInput = it }, label = { Text("No. HP") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)) } },
            confirmButton = { Button(onClick = { if(nameInput.isNotEmpty()){ viewModel.updateCustomer(selectedCustomerForEdit!!.copy(name = nameInput, phoneNumber = phoneInput)); showEditDialog = false } }) { Text("Update") } },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Batal") } }
        )
    }

    // 3. BULK DELETE CONFIRM
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = Color.Red) },
            title = { Text("Hapus ${selectedCustomers.size} Data?") },
            text = { Text("Data yang dipilih akan dihapus permanen.\n\nCatatan: Pelanggan yang masih memiliki hutang TIDAK akan terhapus demi keamanan data.") },
            confirmButton = { Button(onClick = { deleteSelectedCustomers() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Batal") } }
        )
    }

    // 4. BULK DELETE RESULT REPORT
    if (showBulkDeleteResultDialog != null) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteResultDialog = null },
            title = { Text("Laporan Penghapusan") },
            text = { Text(showBulkDeleteResultDialog!!) },
            confirmButton = { Button(onClick = { showBulkDeleteResultDialog = null }) { Text("OK") } }
        )
    }

    // 5. IMPORT CONFIRM
    if (showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showImportConfirmDialog = false },
            title = { Text("Import Kontak?") },
            text = { Text("Ambil data dari kontak HP?") },
            confirmButton = { Button(onClick = { showImportConfirmDialog = false; contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) }) { Text("Ya") } },
            dismissButton = { TextButton(onClick = { showImportConfirmDialog = false }) { Text("Batal") } }
        )
    }
}

// --- ITEM CARD DENGAN DUKUNGAN SELEKSI ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomerItemCardSelectable(
    customer: Customer,
    debtFormatted: String,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onWA: () -> Unit
) {
    val cardColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable( // Fitur Tekan Lama
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(if (isSelected) 8.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, borderColor) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox Mode Seleksi
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() } // Klik checkbox sama dengan klik item
                )
                Spacer(Modifier.width(8.dp))
            }

            // Avatar
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

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (customer.phoneNumber.isNotEmpty()) {
                    Text(customer.phoneNumber, fontSize = 13.sp, color = Color.Gray)
                }
            }

            // Aksi / Info Hutang
            if (!isSelectionMode) {
                Column(horizontalAlignment = Alignment.End) {
                    if (customer.totalDebt > 0) {
                        Text("Hutang", fontSize = 10.sp, color = Color.Gray)
                        Text(debtFormatted, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                        Spacer(Modifier.height(8.dp))
                    }
                    // Tombol WA & Call (Hanya muncul jika tidak mode seleksi)
                    if (customer.phoneNumber.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = CircleShape, color = Color(0xFFE3F2FD), modifier = Modifier.size(32.dp).clickable { onCall() }) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Call, null, tint = Color.Blue, modifier = Modifier.size(16.dp)) }
                            }
                            Surface(shape = CircleShape, color = Color(0xFFE8F5E9), modifier = Modifier.size(32.dp).clickable { onWA() }) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Message, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}