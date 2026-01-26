package com.example.aplikasitokosembakoarkhan

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.aplikasitokosembakoarkhan.data.Product
import com.example.aplikasitokosembakoarkhan.utils.BarcodeScannerView

@Composable
fun RestockScreen(viewModel: ProductViewModel) {
    val productList by viewModel.allProducts.collectAsState(initial = emptyList())
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    var showRestockDialog by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var addedQty by remember { mutableStateOf("") }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> if (isGranted) showScanner = true else Toast.makeText(context, "Izin kamera ditolak", Toast.LENGTH_SHORT).show() }

    val filteredList = productList.filter { it.name.contains(searchQuery, ignoreCase = true) || it.barcode.contains(searchQuery) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Restok Barang Masuk", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                label = { Text("Cari / Scan Barcode...") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.weight(1f), singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            ) { Icon(Icons.Default.QrCodeScanner, null, tint = Color.White) }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredList) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedProduct = product; addedQty = ""; showRestockDialog = true },
                    elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Stok: ${product.stock} ${product.unit}", fontSize = 14.sp, color = Color.Gray)
                        }
                        Icon(Icons.Default.Add, "Tambah", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    if (showRestockDialog && selectedProduct != null) {
        val currentStock = selectedProduct!!.stock
        val inputVal = addedQty.toIntOrNull() ?: 0
        val newTotal = currentStock + inputVal
        AlertDialog(
            onDismissRequest = { showRestockDialog = false },
            title = { Text("Tambah Stok") },
            text = {
                Column {
                    Text("Barang: ${selectedProduct!!.name}"); Spacer(modifier = Modifier.height(8.dp))
                    Text("Stok Awal: $currentStock")
                    OutlinedTextField(value = addedQty, onValueChange = { if (it.all { c -> c.isDigit() }) addedQty = it }, label = { Text("Jumlah Masuk") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    Text("Total Baru: $newTotal ${selectedProduct!!.unit}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (inputVal > 0) {
                        // PERBAIKAN: Gunakan viewModel.update()
                        viewModel.update(selectedProduct!!.copy(stock = newTotal))
                        Toast.makeText(context, "Stok ditambah!", Toast.LENGTH_SHORT).show()
                        showRestockDialog = false; searchQuery = ""
                    }
                }) { Text("SIMPAN") }
            },
            dismissButton = { TextButton(onClick = { showRestockDialog = false }) { Text("Batal") } }
        )
    }

    if (showScanner) {
        Dialog(onDismissRequest = { showScanner = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                Box {
                    BarcodeScannerView(onBarcodeDetected = { code -> showScanner = false; searchQuery = code })
                    IconButton(onClick = { showScanner = false }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) { Icon(Icons.Default.Close, null, tint = Color.White) }
                }
            }
        }
    }
}