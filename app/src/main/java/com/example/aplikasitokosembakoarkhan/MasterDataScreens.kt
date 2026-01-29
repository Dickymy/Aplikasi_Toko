package com.example.aplikasitokosembakoarkhan

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aplikasitokosembakoarkhan.data.Category
import com.example.aplikasitokosembakoarkhan.data.UnitModel

// --- CATEGORY SCREEN MODERN & SAFE ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    viewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val categories by viewModel.allCategories.collectAsState()
    val context = LocalContext.current

    // State
    var showDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // State Edit & Delete
    var editTarget by remember { mutableStateOf<Category?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var targetToDelete by remember { mutableStateOf<Category?>(null) }

    // Filter Logic
    val filteredCategories = categories.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }.sortedByDescending { it.isPriority }

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editTarget = null; showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Kategori Baru") }
            )
        }
    ) { p ->
        Column(modifier = Modifier.padding(p).fillMaxSize()) {

            // --- HEADER SEARCH ---
            Surface(color = Color.White, shadowElevation = 2.dp, shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery, onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari Kategori...") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFF5F7FA), unfocusedContainerColor = Color(0xFFF5F7FA), focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = Color.Transparent)
                    )
                }
            }

            // --- LIST DATA ---
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                if (filteredCategories.isEmpty()) {
                    item { Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text("Data tidak ditemukan", color = Color.Gray) } }
                } else {
                    items(filteredCategories) { cat ->
                        MasterDataItemCard(
                            title = cat.name,
                            isPriority = cat.isPriority,
                            icon = Icons.Default.Category,
                            onEdit = { editTarget = cat; showDialog = true },
                            onDelete = { targetToDelete = cat; showDeleteConfirm = true }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOG INPUT/EDIT ---
    if (showDialog) {
        var nameInput by remember { mutableStateOf(editTarget?.name ?: "") }
        var isPriority by remember { mutableStateOf(editTarget?.isPriority ?: false) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editTarget == null) "Tambah Kategori" else "Edit Kategori", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Nama Kategori") }, singleLine = true, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    PriorityOptionCard(isPriority = isPriority, onClick = { isPriority = !isPriority })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (nameInput.isNotEmpty()) {
                        if (editTarget == null) viewModel.addCategory(nameInput, isPriority)
                        else viewModel.updateCategory(editTarget!!.copy(name = nameInput, isPriority = isPriority))
                        showDialog = false
                    }
                }, enabled = nameInput.isNotBlank(), shape = RoundedCornerShape(8.dp)) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Batal") } }
        )
    }

    // --- DIALOG HAPUS ---
    if (showDeleteConfirm && targetToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Default.Warning, null, tint = Color.Red) },
            title = { Text("Hapus Kategori?") },
            text = { Text("Yakin hapus '${targetToDelete!!.name}'? Sistem akan mengecek apakah kategori ini digunakan.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteCategorySafe(
                        category = targetToDelete!!,
                        onSuccess = { showDeleteConfirm = false; Toast.makeText(context, "Berhasil dihapus", Toast.LENGTH_SHORT).show() },
                        onError = { msg -> showDeleteConfirm = false; Toast.makeText(context, msg, Toast.LENGTH_LONG).show() } // Alert Error
                    )
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red), shape = RoundedCornerShape(8.dp)) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") } }
        )
    }
}

// -----------------------------------------------------------
// UNIT SCREEN (MODERN & SAFE DELETE)
// -----------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitScreen(
    viewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val units by viewModel.allUnits.collectAsState()
    val context = LocalContext.current

    // State
    var showDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // State Edit & Delete
    var editTarget by remember { mutableStateOf<UnitModel?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var targetToDelete by remember { mutableStateOf<UnitModel?>(null) }

    val filteredUnits = units.filter { it.name.contains(searchQuery, ignoreCase = true) }.sortedByDescending { it.isPriority }

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editTarget = null; showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Satuan Baru") }
            )
        }
    ) { p ->
        Column(modifier = Modifier.padding(p).fillMaxSize()) {

            // --- HEADER SEARCH ---
            Surface(color = Color.White, shadowElevation = 2.dp, shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery, onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari Satuan...") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFF5F7FA), unfocusedContainerColor = Color(0xFFF5F7FA), focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = Color.Transparent)
                    )
                }
            }

            // --- LIST DATA ---
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                if (filteredUnits.isEmpty()) {
                    item { Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text("Data tidak ditemukan", color = Color.Gray) } }
                } else {
                    items(filteredUnits) { unit ->
                        MasterDataItemCard(
                            title = unit.name,
                            isPriority = unit.isPriority,
                            icon = Icons.Default.Scale,
                            onEdit = { editTarget = unit; showDialog = true },
                            onDelete = { targetToDelete = unit; showDeleteConfirm = true }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOG INPUT/EDIT ---
    if (showDialog) {
        var nameInput by remember { mutableStateOf(editTarget?.name ?: "") }
        var isPriority by remember { mutableStateOf(editTarget?.isPriority ?: false) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editTarget == null) "Tambah Satuan" else "Edit Satuan", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Nama Satuan") }, singleLine = true, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    PriorityOptionCard(isPriority = isPriority, onClick = { isPriority = !isPriority })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (nameInput.isNotEmpty()) {
                        if (editTarget == null) viewModel.addUnit(nameInput, isPriority)
                        else viewModel.updateUnit(editTarget!!.copy(name = nameInput, isPriority = isPriority))
                        showDialog = false
                    }
                }, enabled = nameInput.isNotBlank(), shape = RoundedCornerShape(8.dp)) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Batal") } }
        )
    }

    // --- DIALOG HAPUS ---
    if (showDeleteConfirm && targetToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Default.Warning, null, tint = Color.Red) },
            title = { Text("Hapus Satuan?") },
            text = { Text("Yakin hapus '${targetToDelete!!.name}'? Sistem akan mengecek ketersediaan barang.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteUnitSafe(
                        unit = targetToDelete!!,
                        onSuccess = { showDeleteConfirm = false; Toast.makeText(context, "Berhasil dihapus", Toast.LENGTH_SHORT).show() },
                        onError = { msg -> showDeleteConfirm = false; Toast.makeText(context, msg, Toast.LENGTH_LONG).show() } // Alert Error
                    )
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red), shape = RoundedCornerShape(8.dp)) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") } }
        )
    }
}

// --- SHARED COMPONENTS ---

@Composable
fun MasterDataItemCard(title: String, isPriority: Boolean, icon: ImageVector, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if(isPriority) MaterialTheme.colorScheme.primary.copy(0.3f) else Color(0xFFEEEEEE))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if (isPriority) Color(0xFFFFF8E1) else Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) {
                Icon(if (isPriority) Icons.Default.Star else icon, null, tint = if (isPriority) Color(0xFFFFC107) else Color.Gray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (isPriority) Text("Default Input Barang", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.7f), modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

@Composable
fun PriorityOptionCard(isPriority: Boolean, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isPriority) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        border = if (isPriority) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isPriority, onCheckedChange = null)
            Column {
                Text("Jadikan Default", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Otomatis terpilih saat tambah barang", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}