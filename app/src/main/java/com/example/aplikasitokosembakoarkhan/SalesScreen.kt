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
import com.example.aplikasitokosembakoarkhan.data.Customer
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
    // Ambil Data Kategori & Satuan untuk Filter
    val dbCategories by inventoryViewModel.allCategories.collectAsState(initial = emptyList())
    val dbUnits by inventoryViewModel.allUnits.collectAsState(initial = emptyList())

    val storeProfile by settingsViewModel.storeProfile.collectAsState()
    val context = LocalContext.current

    var cart by remember { mutableStateOf(mapOf<Product, Int>()) }
    var showPaymentDialog by remember { mutableStateOf(false) }

    // State Filter
    var selectedFilterCategory by remember { mutableStateOf("Semua") }
    var selectedFilterUnit by remember { mutableStateOf("Semua") }
    var showFilterDialog by remember { mutableStateOf(false) }

    // State Pelanggan
    var selectedCustomerName by remember { mutableStateOf("Umum") }
    var showCustomerDialog by remember { mutableStateOf(false) }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var lastTransactionChange by remember { mutableStateOf(0.0) }
    var lastTransactionPay by remember { mutableStateOf(0.0) }
    var lastCartBackup by remember { mutableStateOf(mapOf<Product, Int>()) }
    var lastPaymentMethod by remember { mutableStateOf("Tunai") }
    var lastCustomerName by remember { mutableStateOf("Umum") }

    var searchQuery by remember { mutableStateOf("") }
    var barcodeInput by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var showScanner by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) showScanner = true else Toast.makeText(context, "Izin kamera ditolak!", Toast.LENGTH_SHORT).show()
    }

    // --- LOGIKA FILTER LENGKAP ---
    val filteredList = productList.filter { product ->
        val matchSearch = product.name.contains(searchQuery, ignoreCase = true) || product.barcode.contains(searchQuery)
        val matchCategory = selectedFilterCategory == "Semua" || product.category == selectedFilterCategory
        val matchUnit = selectedFilterUnit == "Semua" || product.unit == selectedFilterUnit

        matchSearch && matchCategory && matchUnit
    }

    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace("Rp", "Rp ").replace(",00", "")
    }

    fun getProductPrice(product: Product, qty: Int): Double {
        return if (product.wholesaleQty > 0 && qty >= product.wholesaleQty && product.wholesalePrice > 0) {
            product.wholesalePrice
        } else {
            product.sellPrice
        }
    }

    val totalPrice = cart.entries.sumOf { (product, qty) -> getProductPrice(product, qty) * qty }

    fun generateReceiptText(cart: Map<Product, Int>, total: Double, pay: Double, change: Double, method: String, customer: String): String {
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
            sb.append("$qty x ${formatRupiah(finalPrice)} = ${formatRupiah(finalPrice * qty)}\n")
        }

        sb.append("--------------------------------\n")
        sb.append("*Total   : ${formatRupiah(total)}*\n")
        sb.append("Bayar   : ${formatRupiah(pay)}\n")
        if (method == "Tunai") {
            sb.append("Kembali : ${formatRupiah(change)}\n")
        }
        sb.append("Metode  : $method\n")
        sb.append("--------------------------------\n")
        if(storeProfile.footer.isNotEmpty()) sb.append("${storeProfile.footer}\n")
        else sb.append("Terima Kasih!\n")

        return sb.toString()
    }

    fun shareToWhatsApp(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.whatsapp")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                intent.setPackage("com.whatsapp.w4b")
                context.startActivity(intent)
            } catch (e2: Exception) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Kirim Struk via..."))
            }
        }
    }

    fun addToCart(product: Product) {
        val currentQty = cart[product] ?: 0
        if (currentQty < product.stock) {
            cart = cart.toMutableMap().apply { put(product, currentQty + 1) }
        } else {
            Toast.makeText(context, "Stok ${product.name} habis!", Toast.LENGTH_SHORT).show()
        }
    }

    fun processBarcode(code: String) {
        val productFound = productList.find { it.barcode == code }
        if (productFound != null) {
            addToCart(productFound)
            barcodeInput = ""
            Toast.makeText(context, "+1 ${productFound.name}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Barang tidak ditemukan!", Toast.LENGTH_SHORT).show()
        }
    }

    // --- HELPER FORMAT INPUT UANG ---
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
            .padding(horizontal = 16.dp, vertical = 8.dp) // Layout Compact
    ) {
        // --- BARIS 1: PELANGGAN (POJOK KANAN) ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(
                onClick = { showCustomerDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(selectedCustomerName, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- BARIS 2: SCAN BARCODE ---
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = barcodeInput,
                onValueChange = { barcodeInput = it },
                label = { Text("Scan Barcode...") },
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
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
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(8.dp)
            ) { Icon(Icons.Default.QrCodeScanner, null) }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- BARIS 3: CARI & FILTER ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari Barang...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            Spacer(modifier = Modifier.width(8.dp))

            // TOMBOL FILTER
            val isFilterActive = selectedFilterCategory != "Semua" || selectedFilterUnit != "Semua"
            FilledTonalButton(
                onClick = { showFilterDialog = true },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
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
                    AssistChip(
                        onClick = { selectedFilterCategory = "Semua" },
                        label = { Text(selectedFilterCategory) },
                        trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp).clickable { selectedFilterCategory = "Semua" }) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color.White)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (selectedFilterUnit != "Semua") {
                    AssistChip(
                        onClick = { selectedFilterUnit = "Semua" },
                        label = { Text(selectedFilterUnit) },
                        trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp).clickable { selectedFilterUnit = "Semua" }) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color.White)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- LIST BARANG ---
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp)) {
            items(filteredList) { product ->
                val qtyInCart = cart[product] ?: 0

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { addToCart(product) },
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = if (qtyInCart > 0) CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)) else CardDefaults.cardColors(containerColor = Color.White)
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
                            val stockColor = if (product.stock <= 5) Color.Red else Color.Black
                            Text("Stok: ${product.stock} ${product.unit} | ${formatRupiah(product.sellPrice)}", fontSize = 12.sp, color = stockColor)
                            if (product.wholesaleQty > 0) {
                                Text("Grosir: ${formatRupiah(product.wholesalePrice)} (Min ${product.wholesaleQty})", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            }
                        }

                        if (qtyInCart > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { val c = cart[product]?:0; if(c>1) cart=cart.toMutableMap().apply{put(product,c-1)} else cart=cart.toMutableMap().apply{remove(product)} }) { Icon(Icons.Default.Remove, "Kurang") }
                                Text(text = "$qtyInCart", fontWeight = FontWeight.Bold)
                                IconButton(onClick = { addToCart(product) }) { Icon(Icons.Default.Add, "Tambah") }
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
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
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

    // --- DIALOG PILIH PELANGGAN (Update: Cari & No HP) ---
    if (showCustomerDialog) {
        var customerSearchQuery by remember { mutableStateOf("") }
        val filteredCustomers = customers.filter {
            it.name.contains(customerSearchQuery, ignoreCase = true) ||
                    it.phoneNumber.contains(customerSearchQuery)
        }

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

    // --- DIALOG PEMBAYARAN (Update: Auto Format Input) ---
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
            onFormatInput = { formatInput(it) }, // Pass fungsi format
            onCleanInput = { cleanInput(it) }    // Pass fungsi clean
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
                    Button(onClick = { val totalStruk = lastCartBackup.entries.sumOf { getProductPrice(it.key, it.value) * it.value }; val text = generateReceiptText(lastCartBackup, totalStruk, lastTransactionPay, lastTransactionChange, lastPaymentMethod, lastCustomerName); shareToWhatsApp(text) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))) { Icon(Icons.Default.Share, null, tint = Color.White); Spacer(modifier = Modifier.width(8.dp)); Text("Kirim Struk WhatsApp", color = Color.White, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { val totalStruk = lastCartBackup.entries.sumOf { getProductPrice(it.key, it.value) * it.value }; PrinterHelper.printReceipt(context = context, cart = lastCartBackup, totalPrice = totalStruk, payAmount = lastTransactionPay, change = lastTransactionChange, paymentMethod = lastPaymentMethod) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Print, null); Spacer(modifier = Modifier.width(8.dp)); Text("Cetak Printer Thermal") }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showSuccessDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Tutup", color = Color.Gray) }
                }
            }
        }
    }

    if (showScanner) {
        Dialog(onDismissRequest = { showScanner = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                Box { BarcodeScannerView(onBarcodeDetected = { code -> showScanner = false; processBarcode(code) }); IconButton(onClick = { showScanner = false }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) { Icon(Icons.Default.Close, "Tutup", tint = Color.White) } }
            }
        }
    }
}

@Composable
fun PaymentDialog(
    cart: Map<Product, Int>,
    totalPrice: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit,
    formatRupiah: (Double) -> String,
    getPrice: (Product, Int) -> Double,
    onFormatInput: (String) -> String,
    onCleanInput: (String) -> Double
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
                        Column { Text("${product.name} (x$qty)", fontSize = 14.sp); if (isWholesaleActive) Text("Grosir Aktif!", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold) }
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
                        onValueChange = { paidAmountText = onFormatInput(it) }, // Format Auto
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