package com.example.aplikasitokosembakoarkhan

import android.Manifest
import android.app.DatePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.aplikasitokosembakoarkhan.data.Product
import com.example.aplikasitokosembakoarkhan.utils.BarcodeScannerView
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ProductScreen(viewModel: ProductViewModel) {
    val productList by viewModel.allProducts.collectAsState()
    val unitList by viewModel.allUnits.collectAsState()
    val context = LocalContext.current

    // State Dialog
    var showDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    // State Input Barang
    var name by remember { mutableStateOf("") }
    var buyPrice by remember { mutableStateOf("") }
    var sellPrice by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("pcs") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // --- INPUT GROSIR (BARU) ---
    var wholesaleQty by remember { mutableStateOf("") }
    var wholesalePrice by remember { mutableStateOf("") }

    // State Tanggal Kadaluwarsa
    var expireDate by remember { mutableStateOf(0L) }

    var showScanner by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) showScanner = true else Toast.makeText(context, "Butuh izin kamera", Toast.LENGTH_SHORT).show()
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> selectedImageUri = uri }

    fun resetInput() {
        name = ""; buyPrice = ""; sellPrice = ""; stock = ""; barcode = ""
        selectedUnit = "pcs"; selectedImageUri = null; expireDate = 0L
        wholesaleQty = ""; wholesalePrice = "" // Reset Grosir
        productToEdit = null
    }

    val filteredList = productList.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.barcode.contains(searchQuery)
    }

    fun formatRupiah(amount: Double): String = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)

    fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        if (expireDate > 0) calendar.timeInMillis = expireDate
        DatePickerDialog(context, { _, y, m, d ->
            val c = Calendar.getInstance(); c.set(y, m, d); expireDate = c.timeInMillis
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { resetInput(); showDialog = true }, containerColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Default.Add, "Tambah") }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            Text("Stok Barang", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                label = { Text("Cari Barang...") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredList) { product ->
                    val isExpiredSoon = product.expireDate > 0 && product.expireDate < (System.currentTimeMillis() + 2592000000L)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = if (isExpiredSoon) CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)) else CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (product.imagePath != null) {
                                AsyncImage(model = File(product.imagePath), null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray), contentScale = ContentScale.Crop)
                            } else {
                                Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray), contentAlignment = Alignment.Center) { Icon(Icons.Default.Image, null, tint = Color.White) }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Stok: ${product.stock} ${product.unit} | Jual: ${formatRupiah(product.sellPrice)}", fontSize = 12.sp)
                                if (product.wholesaleQty > 0) {
                                    Text("Grosir: ${formatRupiah(product.wholesalePrice)} (Min ${product.wholesaleQty})", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                }
                                if (product.expireDate > 0) {
                                    val expStr = SimpleDateFormat("dd MMM yyyy", Locale("id")).format(Date(product.expireDate))
                                    Text("Exp: $expStr", fontSize = 11.sp, color = if(isExpiredSoon) Color.Red else Color.DarkGray)
                                }
                            }
                            IconButton(onClick = {
                                productToEdit = product
                                name = product.name
                                buyPrice = product.buyPrice.toString().replace(".0", "")
                                sellPrice = product.sellPrice.toString().replace(".0", "")
                                stock = product.stock.toString()
                                barcode = product.barcode
                                selectedUnit = product.unit
                                expireDate = product.expireDate
                                wholesaleQty = if(product.wholesaleQty > 0) product.wholesaleQty.toString() else ""
                                wholesalePrice = if(product.wholesalePrice > 0.0) product.wholesalePrice.toString().replace(".0", "") else ""
                                selectedImageUri = null
                                showDialog = true
                            }) { Icon(Icons.Default.Edit, "Edit", tint = Color.Blue) }
                            IconButton(onClick = { productToDelete = product; showDeleteDialog = true }) { Icon(Icons.Default.Delete, "Hapus", tint = Color.Gray) }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (productToEdit == null) "Tambah Barang" else "Edit Barang") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray).clickable { imagePickerLauncher.launch("image/*") }, contentAlignment = Alignment.Center) {
                        if (selectedImageUri != null) AsyncImage(model = selectedImageUri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else if (productToEdit?.imagePath != null) AsyncImage(model = File(productToEdit!!.imagePath!!), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Image, null, tint = Color.White, modifier = Modifier.size(40.dp)); Text("Pilih Foto", color = Color.White) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("Barcode") }, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))) { Icon(Icons.Default.QrCodeScanner, null, tint = Color.White) }
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Barang") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = buyPrice, onValueChange = { if(it.all { c->c.isDigit()}) buyPrice = it }, label = { Text("Harga Beli") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = sellPrice, onValueChange = { if(it.all { c->c.isDigit()}) sellPrice = it }, label = { Text("Harga Jual") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = stock, onValueChange = { if(it.all { c->c.isDigit()}) stock = it }, label = { Text("Stok") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        var expandedUnit by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { expandedUnit = true }) { Text(selectedUnit) }
                            DropdownMenu(expanded = expandedUnit, onDismissRequest = { expandedUnit = false }) {
                                unitList.forEach { u -> DropdownMenuItem(text = { Text(u.name) }, onClick = { selectedUnit = u.name; expandedUnit = false }) }
                                DropdownMenuItem(text = { Text("+ Tambah Unit") }, onClick = { viewModel.addUnit("Box"); expandedUnit = false })
                            }
                        }
                    }

                    // --- INPUTAN GROSIR (BARU) ---
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Opsi Grosir (Kosongkan jika tidak ada)", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = wholesaleQty, onValueChange = { if(it.all { c->c.isDigit()}) wholesaleQty = it }, label = { Text("Min. Qty") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = wholesalePrice, onValueChange = { if(it.all { c->c.isDigit()}) wholesalePrice = it }, label = { Text("Harga Grosir") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Tanggal Kadaluwarsa:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = { showDatePickerDialog() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = if (expireDate > 0) Color.Red else Color.Black)) {
                        Icon(Icons.Default.DateRange, null); Spacer(modifier = Modifier.width(8.dp))
                        Text(if (expireDate > 0L) SimpleDateFormat("dd MMM yyyy", Locale("id")).format(Date(expireDate)) else "Pilih Tanggal Expired")
                    }
                    if (expireDate > 0) TextButton(onClick = { expireDate = 0L }, modifier = Modifier.align(Alignment.End)) { Text("Hapus Tanggal", fontSize = 12.sp, color = Color.Gray) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotEmpty() && sellPrice.isNotEmpty()) {
                        if (productToEdit == null) {
                            viewModel.saveProduct(
                                name = name, buy = buyPrice, sell = sellPrice, stock = stock, barcode = barcode.ifEmpty { System.currentTimeMillis().toString() }, unit = selectedUnit, expireDate = expireDate,
                                wholesaleQtyStr = wholesaleQty, wholesalePriceStr = wholesalePrice, // Save Grosir
                                uri = selectedImageUri, ctx = context,
                                onSuccess = { showDialog = false; Toast.makeText(context, "Disimpan", Toast.LENGTH_SHORT).show() },
                                onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                            )
                        } else {
                            viewModel.updateProductWithImageCheck(
                                original = productToEdit!!, name = name, buy = buyPrice, sell = sellPrice, stock = stock, barcode = barcode, unit = selectedUnit, expireDate = expireDate,
                                wholesaleQtyStr = wholesaleQty, wholesalePriceStr = wholesalePrice, // Update Grosir
                                uri = selectedImageUri, ctx = context,
                                onSuccess = { showDialog = false; Toast.makeText(context, "Diupdate", Toast.LENGTH_SHORT).show() }
                            )
                        }
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Batal") } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(onDismissRequest = { showDeleteDialog = false }, title = { Text("Hapus Barang?") }, text = { Text("Yakin ingin menghapus ${productToDelete?.name}?") }, confirmButton = { Button(onClick = { productToDelete?.let { viewModel.deleteProduct(it, onSuccess = { showDeleteDialog = false }) } }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Hapus") } }, dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") } })
    }
    if (showScanner) {
        Dialog(onDismissRequest = { showScanner = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) { Box { BarcodeScannerView(onBarcodeDetected = { code -> showScanner = false; barcode = code }); IconButton(onClick = { showScanner = false }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) { Icon(Icons.Default.Close, "Tutup", tint = Color.White) } } }
        }
    }
}