package com.example.aplikasitokosembakoarkhan

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.aplikasitokosembakoarkhan.data.Product
import java.io.File
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RestockScreen(
    viewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val productList by viewModel.allProducts.collectAsState(initial = emptyList())

    // State UI
    var searchQuery by remember { mutableStateOf("") }
    var isLowStockOnly by remember { mutableStateOf(false) } // Filter Stok Menipis

    // State Dialog
    var showRestockDialog by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }

    val context = LocalContext.current

    // --- LOGIKA FILTER ---
    val filteredList = productList.filter { product ->
        val matchesSearch = product.name.contains(searchQuery, ignoreCase = true) || product.barcode.contains(searchQuery)
        val matchesStock = if (isLowStockOnly) product.stock <= 5 else true
        matchesSearch && matchesStock
    }.sortedBy { it.stock } // Urutkan dari stok paling sedikit agar yang butuh restok muncul di atas

    // Helper Format: Menghilangkan .0 jika bilangan bulat
    fun formatQty(qty: Double): String {
        return if (qty % 1.0 == 0.0) qty.toInt().toString() else qty.toString()
    }

    // Helper Format Rupiah
    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace("Rp", "Rp ").replace(",00", "")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // --- HEADER & SEARCH ---

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Cari Barang...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            Spacer(modifier = Modifier.width(8.dp))

            // TOMBOL FILTER STOK MENIPIS (Sangat Berguna!)
            FilterChip(
                selected = isLowStockOnly,
                onClick = { isLowStockOnly = !isLowStockOnly },
                label = { Text("Stok Tipis") },
                leadingIcon = {
                    if (isLowStockOnly) Icon(Icons.Default.Check, null)
                    else Icon(Icons.Default.FilterList, null)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFFE0B2), // Orange Muda
                    selectedLabelColor = Color(0xFFE65100)
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- LIST BARANG ---
        if (filteredList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inventory2, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Barang tidak ditemukan", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList) { product ->
                    val isCritical = product.stock <= 2
                    val isLow = product.stock <= 5

                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedProduct = product
                            showRestockDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = if (isCritical) BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Gambar Produk
                            if (product.imagePath != null) {
                                AsyncImage(
                                    model = File(product.imagePath),
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEEEEEE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Image, null, tint = Color.Gray)
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Info Barang
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                // Menampilkan Harga Beli untuk referensi restok
                                Text("Modal: ${formatRupiah(product.buyPrice)}", fontSize = 12.sp, color = Color.Gray)

                                // Badge Kategori
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = product.category,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Stok & Tombol Tambah
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    // Stok otomatis 1 (bukan 1.0)
                                    text = "${formatQty(product.stock)} ${product.unit}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = if (isCritical) Color.Red else if (isLow) Color(0xFFEF6C00) else Color(0xFF2E7D32)
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                FilledTonalButton(
                                    onClick = { selectedProduct = product; showRestockDialog = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Tambah", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG RESTOK (Dengan Estimasi Modal) ---
    if (showRestockDialog && selectedProduct != null) {
        val product = selectedProduct!!
        var addStockText by remember { mutableStateOf("") }

        // Logic parsing angka aman (support pecahan)
        val addStock = try {
            if (addStockText.contains("/")) {
                val parts = addStockText.split("/"); if(parts.size==2) parts[0].trim().toDouble()/parts[1].trim().toDouble() else 0.0
            } else addStockText.replace(",", ".").toDoubleOrNull() ?: 0.0
        } catch(e:Exception){ 0.0 }

        Dialog(onDismissRequest = { showRestockDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Tambah Stok", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(product.name, color = Color.Gray)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input Jumlah
                    OutlinedTextField(
                        value = addStockText,
                        onValueChange = { addStockText = it },
                        label = { Text("Jumlah Tambah (${product.unit})") },
                        placeholder = { Text("Cth: 10 atau 0.5") },
                        singleLine = true,
                        // Keyboard Decimal untuk Kg/Liter
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // FITUR BARU: Kalkulasi Preview & Estimasi Modal
                    if (addStock > 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Stok Awal:", fontSize = 12.sp)
                                    Text(formatQty(product.stock), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Ditambah:", fontSize = 12.sp, color = Color(0xFF2E7D32))
                                    Text("+ ${formatQty(addStock)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2E7D32))
                                }
                                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Stok Akhir:", fontWeight = FontWeight.Bold)
                                    Text(formatQty(product.stock + addStock), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.height(8.dp))

                                // ESTIMASI MODAL BELANJA
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Estimasi Modal:", fontSize = 12.sp, color = Color.Gray)
                                    Text(formatRupiah(addStock * product.buyPrice), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Red)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { showRestockDialog = false }) { Text("Batal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (addStock > 0) {
                                    // Update menggunakan copy object
                                    viewModel.updateProduct(product.copy(stock = product.stock + addStock))
                                    Toast.makeText(context, "Stok berhasil ditambah!", Toast.LENGTH_SHORT).show()
                                    showRestockDialog = false
                                }
                            },
                            enabled = addStock > 0,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }
}