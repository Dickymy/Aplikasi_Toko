package com.example.aplikasitokosembakoarkhan

import android.Manifest
import android.content.Intent
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.aplikasitokosembakoarkhan.data.Product
import com.example.aplikasitokosembakoarkhan.utils.BarcodeScannerView
import com.example.aplikasitokosembakoarkhan.utils.PrinterHelper
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SalesScreen(
    viewModel: SalesViewModel = viewModel(factory = AppViewModelProvider.Factory),
    inventoryViewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory),
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val productList by viewModel.allProducts.collectAsState()
    val customers by inventoryViewModel.allCustomers.collectAsState()
    val dbCategories by inventoryViewModel.allCategories.collectAsState(initial = emptyList())
    val dbUnits by inventoryViewModel.allUnits.collectAsState(initial = emptyList())

    val storeProfile by settingsViewModel.storeProfile.collectAsState()
    val context = LocalContext.current

    var cart by remember { mutableStateOf(mapOf<Product, Double>()) }
    var showPaymentDialog by remember { mutableStateOf(false) }

    // State Filter
    var selectedFilterCategory by remember { mutableStateOf("Semua") }
    var selectedFilterUnit by remember { mutableStateOf("Semua") }
    var showFilterDialog by remember { mutableStateOf(false) }

    // State Pelanggan
    var selectedCustomerName by remember { mutableStateOf("Umum") }
    var showCustomerDialog by remember { mutableStateOf(false) }

    // State Transaksi Sukses
    var showSuccessDialog by remember { mutableStateOf(false) }
    var lastTransactionChange by remember { mutableStateOf(0.0) }
    var lastTransactionPay by remember { mutableStateOf(0.0) }
    var lastCartBackup by remember { mutableStateOf(mapOf<Product, Double>()) }
    var lastPaymentMethod by remember { mutableStateOf("Tunai") }
    var lastCustomerName by remember { mutableStateOf("Umum") }

    // State Edit Qty
    var showEditQtyDialog by remember { mutableStateOf(false) }
    var selectedProductForEdit by remember { mutableStateOf<Product?>(null) }
    var qtyInputText by remember { mutableStateOf("") }

    var searchQuery by remember { mutableStateOf("") }
    var barcodeInput by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var showScanner by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) showScanner = true else Toast.makeText(context, "Izin kamera ditolak!", Toast.LENGTH_SHORT).show()
    }

    // Auto Focus Barcode saat layar dibuka (Support alat scan fisik)
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // --- LOGIKA FILTER ---
    val filteredList = productList.filter { product ->
        val matchSearch = product.name.contains(searchQuery, ignoreCase = true) || product.barcode.contains(searchQuery)
        val matchCategory = selectedFilterCategory == "Semua" || product.category == selectedFilterCategory
        val matchUnit = selectedFilterUnit == "Semua" || product.unit == selectedFilterUnit
        matchSearch && matchCategory && matchUnit
    }

    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace("Rp", "Rp ").replace(",00", "")
    }

    fun formatQty(qty: Double): String {
        return if (qty % 1.0 == 0.0) qty.toInt().toString() else qty.toString()
    }

    fun parseQtyInput(input: String): Double {
        return try {
            if (input.contains("/")) {
                val parts = input.split("/")
                if (parts.size == 2) {
                    val num = parts[0].trim().replace(",", ".").toDouble()
                    val den = parts[1].trim().replace(",", ".").toDouble()
                    if (den != 0.0) num / den else 0.0
                } else 0.0
            } else {
                input.replace(",", ".").toDoubleOrNull() ?: 0.0
            }
        } catch (e: Exception) { 0.0 }
    }

    fun getProductPrice(product: Product, qty: Double): Double {
        return if (product.wholesaleQty > 0 && qty >= product.wholesaleQty && product.wholesalePrice > 0) {
            product.wholesalePrice
        } else {
            product.sellPrice
        }
    }

    val totalPrice = cart.entries.sumOf { (product, qty) -> getProductPrice(product, qty) * qty }

    fun generateReceiptText(cart: Map<Product, Double>, total: Double, pay: Double, change: Double, method: String, customer: String): String {
        val sb = StringBuilder()
        val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date())

        sb.append("*${storeProfile.name.uppercase()}*\n")
        if(storeProfile.address.isNotEmpty()) sb.append("${storeProfile.address}\n")
        if(storeProfile.phone.isNotEmpty()) sb.append("Telp: ${storeProfile.phone}\n")
        sb.append("--------------------------------\n")
        sb.append("Tgl: $date\n")
        sb.append("Plg: $customer\n")
        sb.append("--------------------------------\n")
        cart.forEach { (product, qty) ->
            val finalPrice = getProductPrice(product, qty)
            sb.append("${product.name}\n")
            sb.append("${formatQty(qty)} x ${formatRupiah(finalPrice)} = ${formatRupiah(finalPrice * qty)}\n")
        }
        sb.append("--------------------------------\n")
        sb.append("*Total    : ${formatRupiah(total)}*\n")
        sb.append("Bayar    : ${formatRupiah(pay)}\n")
        if (method == "Tunai") sb.append("Kembali : ${formatRupiah(change)}\n")
        sb.append("Metode  : $method\n")
        sb.append("--------------------------------\n")
        if(storeProfile.footer.isNotEmpty()) sb.append("${storeProfile.footer}\n") else sb.append("Terima Kasih!\n")
        return sb.toString()
    }

    fun shareToWhatsApp(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.whatsapp")
        }
        try { context.startActivity(intent) }
        catch (e: Exception) { Toast.makeText(context, "WhatsApp tidak ditemukan", Toast.LENGTH_SHORT).show() }
    }

    fun addToCart(product: Product) {
        val currentQty = cart[product] ?: 0.0
        if (currentQty + 1.0 <= product.stock) {
            val newCart = cart.toMutableMap()
            newCart[product] = currentQty + 1.0
            cart = newCart
        } else {
            Toast.makeText(context, "Stok ${product.name} habis!", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateQty(product: Product, newQty: Double) {
        if (newQty > product.stock) {
            Toast.makeText(context, "Melebihi stok tersedia!", Toast.LENGTH_SHORT).show()
            return
        }
        val newCart = cart.toMutableMap()
        if (newQty <= 0.0) newCart.remove(product) else newCart[product] = newQty
        cart = newCart
    }

    fun processBarcode(code: String) {
        val productFound = productList.find { it.barcode == code }
        if (productFound != null) {
            val isWeighedItem = productFound.unit.equals("Kg", ignoreCase = true) ||
                    productFound.unit.equals("Liter", ignoreCase = true) ||
                    productFound.unit.equals("Ons", ignoreCase = true)

            if (isWeighedItem) {
                selectedProductForEdit = productFound
                qtyInputText = ""
                showEditQtyDialog = true
                barcodeInput = ""
            } else {
                addToCart(productFound)
                barcodeInput = ""
                Toast.makeText(context, "+1 ${productFound.name}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Barang tidak ditemukan!", Toast.LENGTH_SHORT).show()
        }
    }

    fun formatInput(input: String): String {
        val clean = input.replace("[^\\d]".toRegex(), "")
        if (clean.isEmpty()) return ""
        return try {
            val number = clean.toLong()
            NumberFormat.getInstance(Locale("id", "ID")).format(number)
        } catch (e: Exception) { clean }
    }
    fun cleanInput(input: String): Double {
        return input.replace(".", "").replace(",", "").toDoubleOrNull() ?: 0.0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // --- HEADER: Clear Cart & Title (Opsional) ---
        if (cart.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { cart = emptyMap() }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                    Icon(Icons.Default.DeleteSweep, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Hapus Keranjang")
                }
            }
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // --- BARIS 1: PELANGGAN (TAMPILAN BARU: RAPI & SEJAJAR) ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = selectedCustomerName,
                onValueChange = {}, // Read Only
                readOnly = true,
                label = { Text("Pelanggan") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.weight(1f).clickable { showCustomerDialog = true },
                enabled = false, // Agar tidak muncul keyboard, tapi klik ditangani di Modifier
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color.Black,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledContainerColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = { showCustomerDialog = true },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(56.dp) // Ukuran Tombol Kotak
            ) {
                Icon(Icons.Default.Edit, "Ganti")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- BARIS 2: SCAN BARCODE (TAMPILAN BARU: SEJAJAR PELANGGAN) ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = barcodeInput,
                onValueChange = { barcodeInput = it },
                label = { Text("Scan Barcode / Ketik Kode") },
                leadingIcon = { Icon(Icons.Default.QrCode, null) },
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { processBarcode(barcodeInput) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(56.dp), // Tinggi sama dengan TextField
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.QrCodeScanner, null)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- BARIS 3: CARI NAMA & FILTER ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari Nama Barang...") },
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

            val isFilterActive = selectedFilterCategory != "Semua" || selectedFilterUnit != "Semua"
            FilledTonalButton(
                onClick = { showFilterDialog = true },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(56.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if(isFilterActive) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0),
                    contentColor = if(isFilterActive) Color.White else Color.Black
                )
            ) {
                Icon(Icons.Default.FilterList, null)
            }
        }

        // --- CHIP FILTER AKTIF ---
        if (selectedFilterCategory != "Semua" || selectedFilterUnit != "Semua") {
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                if (selectedFilterCategory != "Semua") {
                    AssistChip(onClick = { selectedFilterCategory = "Semua" }, label = { Text(selectedFilterCategory) }, trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) })
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (selectedFilterUnit != "Semua") {
                    AssistChip(onClick = { selectedFilterUnit = "Semua" }, label = { Text(selectedFilterUnit) }, trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) })
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- LIST BARANG (DENGAN INDIKATOR GROSIR) ---
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp)) {
            items(filteredList) { product ->
                val qtyInCart = cart[product] ?: 0.0
                val isGrosirApplied = product.wholesaleQty > 0 && qtyInCart >= product.wholesaleQty

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                        val isWeighedItem = product.unit.equals("Kg", ignoreCase = true) || product.unit.equals("Liter", ignoreCase = true) || product.unit.equals("Ons", ignoreCase = true)
                        if (isWeighedItem) { selectedProductForEdit = product; qtyInputText = ""; showEditQtyDialog = true }
                        else { addToCart(product) }
                    },
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = if (qtyInCart > 0.0) CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)) else CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (product.imagePath != null) {
                            AsyncImage(model = File(product.imagePath), null, Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray), contentScale = ContentScale.Crop)
                        } else {
                            Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray), contentAlignment = Alignment.Center) { Icon(Icons.Default.Image, null, tint = Color.White) }
                        }
                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.Bold)
                            // Info Harga Normal
                            Text("@ ${formatRupiah(product.sellPrice)} / ${product.unit}", fontSize = 12.sp, color = Color.Gray)

                            // Info Stok
                            val stockColor = if (product.stock <= 5) Color.Red else Color.Black
                            Text("Sisa: ${formatQty(product.stock)}", fontSize = 11.sp, color = stockColor)

                            // Info Grosir (Bonus Fitur)
                            if (product.wholesaleQty > 0) {
                                Text("Grosir: ${formatRupiah(product.wholesalePrice)} (Min ${formatQty(product.wholesaleQty)})", fontSize = 10.sp, color = Color(0xFF2E7D32))
                            }
                        }

                        if (qtyInCart > 0.0) {
                            Column(horizontalAlignment = Alignment.End) {
                                // Badge Grosir Aktif
                                if (isGrosirApplied) {
                                    Surface(color = Color(0xFFC8E6C9), shape = RoundedCornerShape(4.dp)) {
                                        Text("Harga Grosir", fontSize = 10.sp, color = Color(0xFF1B5E20), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { updateQty(product, qtyInCart - 1.0) }) { Icon(Icons.Default.Remove, "Kurang") }
                                    Text(
                                        text = formatQty(qtyInCart),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { selectedProductForEdit = product; qtyInputText = formatQty(qtyInCart); showEditQtyDialog = true }.padding(horizontal = 8.dp)
                                    )
                                    IconButton(onClick = { updateQty(product, qtyInCart + 1.0) }) { Icon(Icons.Default.Add, "Tambah") }
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // --- FOOTER TOTAL & BAYAR ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Total Bayar:", fontSize = 14.sp, color = Color.Gray)
                Text(formatRupiah(totalPrice), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
            }
            Button(
                onClick = { if (cart.isNotEmpty()) showPaymentDialog = true else Toast.makeText(context, "Keranjang kosong!", Toast.LENGTH_SHORT).show() },
                enabled = cart.isNotEmpty(),
                shape = RoundedCornerShape(12.dp), // Konsisten
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.ShoppingCart, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("BAYAR", fontWeight = FontWeight.Bold)
            }
        }
    }

    // --- DIALOG FILTER ---
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Barang") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Kategori:", fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth().clickable { selectedFilterCategory = "Semua" }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedFilterCategory == "Semua", onClick = null)
                        Text("Semua Kategori", modifier = Modifier.padding(start = 8.dp))
                    }
                    dbCategories.forEach { cat ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { selectedFilterCategory = cat.name }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedFilterCategory == cat.name, onClick = null)
                            Text(cat.name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Satuan:", fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth().clickable { selectedFilterUnit = "Semua" }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedFilterUnit == "Semua", onClick = null)
                        Text("Semua Satuan", modifier = Modifier.padding(start = 8.dp))
                    }
                    dbUnits.forEach { unit ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { selectedFilterUnit = unit.name }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedFilterUnit == unit.name, onClick = null)
                            Text(unit.name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFilterDialog = false }) { Text("Tutup") } }
        )
    }

    // --- DIALOG EDIT QTY ---
    if (showEditQtyDialog && selectedProductForEdit != null) {
        val product = selectedProductForEdit!!
        AlertDialog(
            onDismissRequest = { showEditQtyDialog = false },
            title = { Text("Ubah Jumlah: ${product.name}") },
            text = {
                Column {
                    Text("Masukkan jumlah (bisa desimal, cth: 0.5 atau 1/5)", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = qtyInputText,
                        onValueChange = { qtyInputText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Jumlah (${product.unit})") },
                        placeholder = { Text("1/5 atau 0.2") },
                        singleLine = true
                    )

                    val previewQty = parseQtyInput(qtyInputText)
                    if (previewQty > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val previewPrice = getProductPrice(product, previewQty) * previewQty
                        Text("Estimasi: ${formatQty(previewQty)} x ${formatRupiah(getProductPrice(product, previewQty))} = ${formatRupiah(previewPrice)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val cleanQty = parseQtyInput(qtyInputText)
                    updateQty(product, cleanQty)
                    showEditQtyDialog = false
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showEditQtyDialog = false }) { Text("Batal") } }
        )
    }

    // --- DIALOG PILIH PELANGGAN (Updated Layout) ---
    if (showCustomerDialog) {
        var customerSearchQuery by remember { mutableStateOf("") }
        val filteredCustomers = customers.filter { it.name.contains(customerSearchQuery, ignoreCase = true) || it.phoneNumber.contains(customerSearchQuery) }

        Dialog(onDismissRequest = { showCustomerDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pilih Pelanggan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customerSearchQuery, onValueChange = { customerSearchQuery = it },
                        label = { Text("Cari Nama / No. HP") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedCustomerName = "Umum"; showCustomerDialog = false },
                        colors = if(selectedCustomerName == "Umum") CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                    ) { Text("Umum (Non-Member)", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) }

                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        items(filteredCustomers) { customer ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedCustomerName = customer.name; showCustomerDialog = false },
                                colors = if(selectedCustomerName == customer.name) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(customer.name, fontWeight = FontWeight.Bold)
                                    if(customer.phoneNumber.isNotEmpty()) Text(customer.phoneNumber, fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showCustomerDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Tutup") }
                }
            }
        }
    }

    // --- DIALOG PEMBAYARAN ---
    if (showPaymentDialog) {
        PaymentDialog(
            cart = cart,
            totalPrice = totalPrice,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { payAmount, method ->
                viewModel.checkout(
                    cart = cart,
                    paymentMethod = method,
                    customerName = selectedCustomerName,
                    onSuccess = {
                        lastTransactionChange = payAmount - totalPrice
                        lastTransactionPay = payAmount
                        lastCartBackup = cart
                        lastPaymentMethod = method
                        lastCustomerName = selectedCustomerName
                        cart = emptyMap()
                        selectedCustomerName = "Umum"
                        showPaymentDialog = false
                        showSuccessDialog = true
                    },
                    onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                )
            },
            formatRupiah = { formatRupiah(it) },
            getPrice = { p, q -> getProductPrice(p, q) },
            onFormatInput = { formatInput(it) },
            onCleanInput = { cleanInput(it) },
            formatQty = { formatQty(it) }
        )
    }

    if (showSuccessDialog) {
        Dialog(onDismissRequest = { showSuccessDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
                    Text("Transaksi Berhasil!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Pelanggan: $lastCustomerName", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (lastPaymentMethod == "Tunai") {
                        Text("Kembalian:", color = Color.Gray)
                        Text(formatRupiah(lastTransactionChange), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))
                    } else { Text("Metode: $lastPaymentMethod", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.Blue) }
                    Spacer(modifier = Modifier.height(24.dp))

                    val totalStruk = lastCartBackup.entries.sumOf { getProductPrice(it.key, it.value) * it.value }

                    Button(onClick = { val text = generateReceiptText(lastCartBackup, totalStruk, lastTransactionPay, lastTransactionChange, lastPaymentMethod, lastCustomerName); shareToWhatsApp(text) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))) { Icon(Icons.Default.Share, null, tint = Color.White); Spacer(modifier = Modifier.width(8.dp)); Text("Kirim Struk WhatsApp", color = Color.White, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(onClick = { PrinterHelper.printReceipt(context = context, cart = lastCartBackup, totalPrice = totalStruk, payAmount = lastTransactionPay, change = lastTransactionChange, paymentMethod = lastPaymentMethod) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Print, null); Spacer(modifier = Modifier.width(8.dp)); Text("Cetak Printer Thermal") }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showSuccessDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Tutup", color = Color.Gray) }
                }
            }
        }
    }

    // --- SCANNER BARCODE (FULLSCREEN MODERN) ---
    if (showScanner) {
        Dialog(onDismissRequest = { showScanner = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                Box {
                    BarcodeScannerView(onBarcodeDetected = { code ->
                        showScanner = false
                        processBarcode(code)
                    })
                    // TOMBOL CLOSE (KIRI ATAS - AGAR TIDAK NUMPUK FLASH)
                    IconButton(onClick = { showScanner = false }, modifier = Modifier.align(Alignment.TopStart).padding(24.dp)) {
                        Icon(Icons.Default.Close, "Tutup", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentDialog(
    cart: Map<Product, Double>,
    totalPrice: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit,
    formatRupiah: (Double) -> String,
    getPrice: (Product, Double) -> Double,
    onFormatInput: (String) -> String,
    onCleanInput: (String) -> Double,
    formatQty: (Double) -> String
) {
    var paidAmountText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("Tunai") }

    val paidAmount = if (selectedMethod == "Tunai") onCleanInput(paidAmountText) else totalPrice
    val change = paidAmount - totalPrice

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Konfirmasi Pembayaran") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text("Rincian Pesanan:", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                cart.forEach { (product, qty) ->
                    val finalPrice = getPrice(product, qty)
                    val isWholesaleActive = finalPrice < product.sellPrice
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("${product.name} (x${formatQty(qty)})", fontSize = 14.sp); if (isWholesaleActive) Text("Grosir Aktif!", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold) }
                        Text(formatRupiah(finalPrice * qty), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text("Metode Pembayaran:", fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Tunai", "QRIS", "Transfer").forEach { method -> FilterChip(selected = selectedMethod == method, onClick = { selectedMethod = method }, label = { Text(method) }) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total Tagihan:", fontWeight = FontWeight.Bold); Text(formatRupiah(totalPrice), fontWeight = FontWeight.Bold, color = Color.Red) }
                Spacer(modifier = Modifier.height(16.dp))
                if (selectedMethod == "Tunai") {
                    OutlinedTextField(
                        value = paidAmountText,
                        onValueChange = { paidAmountText = onFormatInput(it) },
                        label = { Text("Uang Tunai") },
                        prefix = { Text("Rp") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = if (change >= 0) "Kembalian: ${formatRupiah(change)}" else "Kurang: ${formatRupiah(kotlin.math.abs(change))}", color = if (change >= 0) Color(0xFF2E7D32) else Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(paidAmount, selectedMethod) }, enabled = paidAmount >= totalPrice) { Text("SELESAI") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Batal") } }
    )
}