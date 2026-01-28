package com.example.aplikasitokosembakoarkhan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aplikasitokosembakoarkhan.data.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockScreen(
    // Update: Gunakan InventoryViewModel
    viewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val products by viewModel.allProducts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // Dialog State
    var showDialog by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var addQty by remember { mutableStateOf("") }

    val filteredList = products.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Cari Barang untuk Restok...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(filteredList) { product ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            selectedProduct = product
                            addQty = ""
                            showDialog = true
                        },
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(product.name, style = MaterialTheme.typography.titleMedium)
                            Text("Stok Saat Ini: ${product.stock}", color = Color.Gray)
                        }
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    if (showDialog && selectedProduct != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Restok: ${selectedProduct!!.name}") },
            text = {
                Column {
                    Text("Stok Awal: ${selectedProduct!!.stock}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = addQty,
                        onValueChange = { if(it.all { c -> c.isDigit() }) addQty = it },
                        label = { Text("Jumlah Penambahan") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val qty = addQty.toIntOrNull() ?: 0
                    if (qty > 0) {
                        val newStock = selectedProduct!!.stock + qty
                        // Update stok
                        viewModel.update(selectedProduct!!.copy(stock = newStock))
                        showDialog = false
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Batal") } }
        )
    }
}