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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aplikasitokosembakoarkhan.data.Category
import com.example.aplikasitokosembakoarkhan.data.UnitModel

// --- SCREEN KATEGORI ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(viewModel: ProductViewModel) {
    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var currentName by remember { mutableStateOf("") }
    var currentId by remember { mutableStateOf<Int?>(null) } // Jika null berarti Tambah, jika ada berarti Edit

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { currentName = ""; currentId = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Manajemen Kategori", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(categories) { category ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(category.name, fontWeight = FontWeight.Medium)
                            Row {
                                IconButton(onClick = { currentName = category.name; currentId = category.id; showDialog = true }) {
                                    Icon(Icons.Default.Edit, "Edit", tint = Color.Blue)
                                }
                                IconButton(onClick = { viewModel.deleteCategory(category) }) {
                                    Icon(Icons.Default.Delete, "Hapus", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (currentId == null) "Tambah Kategori" else "Edit Kategori") },
            text = { OutlinedTextField(value = currentName, onValueChange = { currentName = it }, label = { Text("Nama Kategori") }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Button(onClick = {
                    if (currentName.isNotEmpty()) {
                        if (currentId == null) viewModel.addCategory(currentName)
                        else viewModel.updateCategory(Category(id = currentId!!, name = currentName))
                        showDialog = false
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Batal") } }
        )
    }
}

// --- SCREEN SATUAN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitScreen(viewModel: ProductViewModel) {
    val units by viewModel.allUnits.collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var currentName by remember { mutableStateOf("") }
    var currentId by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { currentName = ""; currentId = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Manajemen Satuan", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(units) { unit ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(unit.name, fontWeight = FontWeight.Medium)
                            Row {
                                IconButton(onClick = { currentName = unit.name; currentId = unit.id; showDialog = true }) {
                                    Icon(Icons.Default.Edit, "Edit", tint = Color.Blue)
                                }
                                IconButton(onClick = { viewModel.deleteUnit(unit) }) {
                                    Icon(Icons.Default.Delete, "Hapus", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (currentId == null) "Tambah Satuan" else "Edit Satuan") },
            text = { OutlinedTextField(value = currentName, onValueChange = { currentName = it }, label = { Text("Nama Satuan (Pcs, Kg, dll)") }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Button(onClick = {
                    if (currentName.isNotEmpty()) {
                        if (currentId == null) viewModel.addUnit(currentName)
                        else viewModel.updateUnit(UnitModel(id = currentId!!, name = currentName))
                        showDialog = false
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Batal") } }
        )
    }
}