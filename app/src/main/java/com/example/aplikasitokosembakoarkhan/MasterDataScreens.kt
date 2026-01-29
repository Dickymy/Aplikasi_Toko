package com.example.aplikasitokosembakoarkhan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // IMPORT INI WAJIB AGAR TIDAK ERROR
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aplikasitokosembakoarkhan.data.Category
import com.example.aplikasitokosembakoarkhan.data.UnitModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    viewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val categories by viewModel.allCategories.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }

    // Variabel ini yang menyimpan data saat Edit
    var editTarget by remember { mutableStateOf<Category?>(null) }

    // State Hapus
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var targetToDelete by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { nameInput=""; editTarget=null; showDialog=true }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { p ->
        LazyColumn(modifier = Modifier.padding(p).padding(16.dp)) {
            items(categories) { cat ->
                Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cat.name)
                            if (cat.isPriority) {
                                // Teks diperbarui agar sesuai fungsi
                                Text("Prioritas (Default Tambah Barang)", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        IconButton(onClick = { nameInput=cat.name; editTarget=cat; showDialog=true }) { Icon(Icons.Default.Edit,null, tint=Color.Blue) }
                        IconButton(onClick = { targetToDelete=cat; showDeleteConfirm=true }) { Icon(Icons.Default.Delete,null, tint=Color.Red) }
                    }
                }
            }
        }
    }

    if(showDeleteConfirm && targetToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Kategori?") },
            text = { Text("Hapus '${targetToDelete!!.name}'?") },
            confirmButton = { Button(onClick = { viewModel.deleteCategory(targetToDelete!!); showDeleteConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") } }
        )
    }

    if(showDialog) {
        // PERBAIKAN: Gunakan editTarget, bukan category
        var isPriority by remember { mutableStateOf(editTarget?.isPriority ?: false) }

        AlertDialog(
            onDismissRequest = { showDialog=false },
            title = { Text(if(editTarget==null) "Tambah Kategori" else "Edit Kategori") },
            text = {
                Column {
                    OutlinedTextField(value = nameInput, onValueChange = { nameInput=it }, label={Text("Nama")})
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPriority, onCheckedChange = { isPriority = it })
                        Text("Prioritaskan sebagai Default", fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if(nameInput.isNotEmpty()) {
                        if(editTarget==null) {
                            // Panggil fungsi tambah dengan parameter prioritas
                            viewModel.addCategory(nameInput, isPriority)
                        } else {
                            // Panggil fungsi update dengan parameter prioritas
                            viewModel.updateCategory(editTarget!!.copy(name = nameInput, isPriority = isPriority))
                        }
                        showDialog=false
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick={showDialog=false}) { Text("Batal") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitScreen(
    viewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val units by viewModel.allUnits.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var editTarget by remember { mutableStateOf<UnitModel?>(null) }

    // State Hapus
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var targetToDelete by remember { mutableStateOf<UnitModel?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { nameInput=""; editTarget=null; showDialog=true }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { p ->
        LazyColumn(modifier = Modifier.padding(p).padding(16.dp)) {
            items(units) { unit ->
                Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(unit.name, modifier = Modifier.weight(1f))
                        IconButton(onClick = { nameInput=unit.name; editTarget=unit; showDialog=true }) { Icon(Icons.Default.Edit,null, tint=Color.Blue) }
                        IconButton(onClick = { targetToDelete=unit; showDeleteConfirm=true }) { Icon(Icons.Default.Delete,null, tint=Color.Red) }
                    }
                }
            }
        }
    }

    if(showDeleteConfirm && targetToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Satuan?") },
            text = { Text("Hapus '${targetToDelete!!.name}'?") },
            confirmButton = { Button(onClick = { viewModel.deleteUnit(targetToDelete!!); showDeleteConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") } }
        )
    }

    if(showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog=false },
            title = { Text(if(editTarget==null) "Tambah Satuan" else "Edit Satuan") },
            text = { OutlinedTextField(value = nameInput, onValueChange = { nameInput=it }, label={Text("Nama")}) },
            confirmButton = { Button(onClick = { if(nameInput.isNotEmpty()) { if(editTarget==null) viewModel.addUnit(nameInput) else viewModel.updateUnit(editTarget!!.copy(name = nameInput)); showDialog=false } }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick={showDialog=false}) { Text("Batal") } }
        )
    }
}