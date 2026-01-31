package com.example.aplikasitokosembakoarkhan

import android.Manifest
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontStyle
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

// Data Class Pending
data class PendingTransaction(
    val id: Long = System.currentTimeMillis(),
    val customerName: String,
    val cartItems: Map<Product, Double>,
    val time: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
)

@OptIn(ExperimentalMaterial3Api::class)
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

    // --- STATE TRANSAKSI ---
    var cart by remember { mutableStateOf(mapOf<Product, Double>()) }
    var selectedCustomerName by remember { mutableStateOf("Umum") }

    // --- STATE UI LAYOUT ---
    var isFullScreen by remember { mutableStateOf(false) }
    var showFullScreenDialog by remember { mutableStateOf(false) }
    var isInputExpanded by remember { mutableStateOf(true) }

    // FITUR: Handle Tombol Back saat Fullscreen
    BackHandler(enabled = isFullScreen) {
        Toast.makeText(context, "Mode Fokus Aktif! Tekan tombol [Keluar] di pojok kanan atas.", Toast.LENGTH_SHORT).show()
    }

    // State Pending & Dialogs
    val pendingList = remember { mutableStateListOf<PendingTransaction>() }
    var showPendingDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showPendingConfirmDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showEditQtyDialog by remember { mutableStateOf(false) }
    var selectedProductForEdit by remember { mutableStateOf<Product?>(null) }
    var qtyInputText by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedFilterCategory by remember { mutableStateOf("Semua") }
    var selectedFilterUnit by remember { mutableStateOf("Semua") }
    var showCustomerDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // State Last Transaction
    var lastTransactionChange by remember { mutableStateOf(0.0) }
    var lastTransactionPay by remember { mutableStateOf(0.0) }
    var lastCartBackup by remember { mutableStateOf(mapOf<Product, Double>()) }
    var lastPaymentMethod by remember { mutableStateOf("Tunai") }
    var lastCustomerName by remember { mutableStateOf("Umum") }

    // State Input
    var searchQuery by remember { mutableStateOf("") }
    var barcodeInput by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var showScanner by remember { mutableStateOf(false) }

    // State Error/Alert
    var showOutOfStockDialog by remember { mutableStateOf(false) }
    var outOfStockProductName by remember { mutableStateOf("") }
    var showPendingStockErrorDialog by remember { mutableStateOf(false) }
    var pendingStockErrorText by remember { mutableStateOf("") }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> if (isGranted) showScanner = true else Toast.makeText(context, "Izin ditolak", Toast.LENGTH_SHORT).show() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // --- LOGIKA UTAMA ---
    val baseFilteredList = productList.filter { product ->
        val query = searchQuery.lowercase()
        val matchSearch = product.name.lowercase().contains(query) ||
                product.barcode.lowercase().contains(query) ||
                product.category.lowercase().contains(query) ||
                product.unit.lowercase().contains(query)
        val matchCategory = selectedFilterCategory == "Semua" || product.category == selectedFilterCategory
        val matchUnit = selectedFilterUnit == "Semua" || product.unit == selectedFilterUnit
        matchSearch && matchCategory && matchUnit
    }

    val finalDisplayList = baseFilteredList.sortedByDescending { product ->
        if (cart.containsKey(product)) 1 else 0
    }

    fun formatRupiah(amount: Double): String = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace("Rp", "Rp ").replace(",00", "")
    fun formatQty(qty: Double): String = if (qty % 1.0 == 0.0) qty.toInt().toString() else qty.toString()
    fun parseQtyInput(input: String): Double {
        return try {
            if (input.contains("/")) {
                val parts = input.split("/")
                if (parts.size == 2) parts[0].trim().replace(",", ".").toDouble() / parts[1].trim().replace(",", ".").toDouble() else 0.0
            } else input.replace(",", ".").toDoubleOrNull() ?: 0.0
        } catch (e: Exception) { 0.0 }
    }
    fun getProductPrice(product: Product, qty: Double): Double = if (product.wholesaleQty > 0 && qty >= product.wholesaleQty) product.wholesalePrice else product.sellPrice

    val totalPrice = cart.entries.sumOf { (product, qty) -> getProductPrice(product, qty) * qty }

    fun addToCart(product: Product) {
        val currentQty = cart[product] ?: 0.0
        if (currentQty + 1.0 <= product.stock) {
            val newCart = cart.toMutableMap()
            newCart[product] = currentQty + 1.0
            cart = newCart
        } else {
            outOfStockProductName = product.name
            showOutOfStockDialog = true
        }
    }

    fun updateQty(product: Product, newQty: Double) {
        if (newQty <= product.stock) {
            val newCart = cart.toMutableMap()
            if (newQty <= 0.0) newCart.remove(product) else newCart[product] = newQty
            cart = newCart
        } else {
            outOfStockProductName = product.name
            showOutOfStockDialog = true
        }
    }

    fun doPendingTransaction() {
        if (cart.isNotEmpty()) {
            pendingList.add(PendingTransaction(customerName = selectedCustomerName, cartItems = cart))
            cart = emptyMap(); selectedCustomerName = "Umum"
            Toast.makeText(context, "Transaksi dipending...", Toast.LENGTH_SHORT).show()
        }
    }

    fun restorePendingTransaction(pending: PendingTransaction) {
        if (cart.isNotEmpty()) pendingList.add(PendingTransaction(customerName = selectedCustomerName, cartItems = cart))
        val validCart = mutableMapOf<Product, Double>()
        val removedItems = mutableListOf<String>()

        pending.cartItems.forEach { (pendingProduct, qty) ->
            val currentProduct = productList.find { it.id == pendingProduct.id }
            if (currentProduct != null) {
                if (currentProduct.stock >= qty) validCart[currentProduct] = qty
                else removedItems.add("- ${currentProduct.name} (Stok: ${formatQty(currentProduct.stock)}, Diminta: ${formatQty(qty)})")
            } else { removedItems.add("- ${pendingProduct.name} (Data barang hilang)") }
        }

        cart = validCart
        selectedCustomerName = pending.customerName
        pendingList.remove(pending)
        showPendingDialog = false

        if (removedItems.isNotEmpty()) {
            pendingStockErrorText = "Barang berikut dihapus otomatis dari keranjang karena stok habis/tidak cukup:\n\n" + removedItems.joinToString("\n")
            showPendingStockErrorDialog = true
        } else { Toast.makeText(context, "Transaksi dilanjutkan", Toast.LENGTH_SHORT).show() }
    }

    fun processBarcode(code: String) {
        val productFound = productList.find { it.barcode == code }
        if (productFound != null) {
            val isWeighedItem = productFound.unit.equals("Kg", ignoreCase = true) || productFound.unit.equals("Liter", ignoreCase = true) || productFound.unit.equals("Ons", ignoreCase = true)
            if (isWeighedItem) { selectedProductForEdit = productFound; qtyInputText = ""; showEditQtyDialog = true; barcodeInput = "" }
            else { addToCart(productFound); barcodeInput = ""; Toast.makeText(context, "+1 ${productFound.name}", Toast.LENGTH_SHORT).show() }
        } else { Toast.makeText(context, "Barang tidak ditemukan!", Toast.LENGTH_SHORT).show() }
    }

    fun processQuickSearchAdd() {
        if (baseFilteredList.size == 1) {
            val product = baseFilteredList.first()
            val isWeighedItem = product.unit.equals("Kg", ignoreCase = true) || product.unit.equals("Liter", ignoreCase = true)
            if (isWeighedItem) { selectedProductForEdit = product; qtyInputText = ""; showEditQtyDialog = true }
            else { addToCart(product); searchQuery = ""; Toast.makeText(context, "+1 ${product.name}", Toast.LENGTH_SHORT).show() }
        }
    }

    fun generateReceiptText(cart: Map<Product, Double>, total: Double, pay: Double, change: Double, method: String, customer: String): String {
        val sb = StringBuilder()
        val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date())
        sb.append("*${storeProfile.name.uppercase()}*\n")
        if(storeProfile.address.isNotEmpty()) sb.append("${storeProfile.address}\n")
        if(storeProfile.phone.isNotEmpty()) sb.append("Telp: ${storeProfile.phone}\n")
        sb.append("--------------------------------\n")
        sb.append("Tgl: $date\nPlg: $customer\n--------------------------------\n")
        cart.forEach { (product, qty) ->
            val finalPrice = getProductPrice(product, qty)
            sb.append("${product.name}\n${formatQty(qty)} x ${formatRupiah(finalPrice)} = ${formatRupiah(finalPrice * qty)}\n")
        }
        sb.append("--------------------------------\n*Total    : ${formatRupiah(total)}*\nBayar    : ${formatRupiah(pay)}\n")
        if (method == "Tunai") sb.append("Kembali : ${formatRupiah(change)}\n")
        sb.append("Metode  : $method\n--------------------------------\n")
        if(storeProfile.footer.isNotEmpty()) sb.append("${storeProfile.footer}\n") else sb.append("Terima Kasih!\n")
        return sb.toString()
    }

    fun shareToWhatsApp(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text); setPackage("com.whatsapp") }
        try { context.startActivity(intent) } catch (e: Exception) { Toast.makeText(context, "WhatsApp tidak ditemukan", Toast.LENGTH_SHORT).show() }
    }

    fun formatInput(input: String): String {
        val clean = input.replace("[^\\d]".toRegex(), "")
        if (clean.isEmpty()) return ""
        return try { NumberFormat.getInstance(Locale("id", "ID")).format(clean.toLong()) } catch (e: Exception) { clean }
    }
    fun cleanInput(input: String): Double = input.replace(".", "").replace(",", "").toDoubleOrNull() ?: 0.0

    // ================================================================
    // KONTEN UTAMA KASIR
    // ================================================================
    val salesContent = @Composable {
        Scaffold(
            // HEADER
            topBar = {
                Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 4.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // ROW 1: ACTIONS
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            if (pendingList.isNotEmpty()) {
                                FilledTonalButton(onClick = { showPendingDialog = true }, colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFFFF3E0), contentColor = Color(0xFFEF6C00)), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.height(36.dp)) {
                                    Icon(Icons.Default.PendingActions, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Pending (${pendingList.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            } else { Spacer(Modifier.width(1.dp)) }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (cart.isNotEmpty()) {
                                    OutlinedButton(onClick = { showPendingConfirmDialog = true }, border = BorderStroke(1.dp, Color(0xFFFFA000)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFA000)), shape = RoundedCornerShape(8.dp), modifier = Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Tunda", fontSize = 12.sp) }
                                    Spacer(Modifier.width(8.dp)); IconButton(onClick = { showClearConfirmDialog = true }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.DeleteSweep, null, tint = Color.Red) }
                                }
                                Spacer(Modifier.width(4.dp))
                                FilledTonalIconButton(onClick = { showFullScreenDialog = true }, modifier = Modifier.size(36.dp), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = if (isFullScreen) MaterialTheme.colorScheme.primaryContainer else Color.LightGray.copy(alpha = 0.2f))) { Icon(imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, contentDescription = "Mode Layar Penuh", modifier = Modifier.size(20.dp)) }
                                if (isFullScreen) {
                                    Spacer(Modifier.width(4.dp))
                                    FilledTonalIconButton(onClick = { isInputExpanded = !isInputExpanded }, modifier = Modifier.size(36.dp), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Icon(imageVector = if(isInputExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = "Toggle Header", modifier = Modifier.size(20.dp)) }
                                }
                            }
                        }

                        // INPUT FIELDS (Collapsible)
                        val textFieldColors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, disabledContainerColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = Color.LightGray,
                            disabledTextColor = Color.Black, disabledLabelColor = Color.Gray, disabledPlaceholderColor = Color.Gray
                        )
                        val inputShape = RoundedCornerShape(12.dp)
                        val showFullDetails = !isFullScreen || isInputExpanded

                        AnimatedVisibility(visible = showFullDetails, enter = expandVertically(), exit = shrinkVertically()) {
                            Column {
                                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = selectedCustomerName, onValueChange = {}, readOnly = true,
                                        label = { Text("Pelanggan") }, leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                                        modifier = Modifier.weight(1f).clickable { showCustomerDialog = true },
                                        enabled = false, colors = textFieldColors, shape = inputShape, singleLine = true
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FilledIconButton(onClick = { showCustomerDialog = true }, modifier = Modifier.size(56.dp), shape = CircleShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) { Icon(Icons.Default.PersonSearch, null) }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = barcodeInput, onValueChange = { barcodeInput = it },
                                        label = { Text("Ketik Barcode Manual") }, leadingIcon = { Icon(Icons.Default.QrCode, null, tint = MaterialTheme.colorScheme.primary) },
                                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                                        singleLine = true, shape = inputShape, colors = textFieldColors,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { processBarcode(barcodeInput) })
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FilledIconButton(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.size(56.dp), shape = CircleShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)) { Icon(Icons.Default.QrCodeScanner, null) }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        // SEARCH BAR
                        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = searchQuery, onValueChange = { searchQuery = it },
                                placeholder = { Text("Cari Barang / Kategori...") },
                                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                                trailingIcon = if(searchQuery.isNotEmpty()) { { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Cancel, null, tint = Color.Gray) } } } else null,
                                modifier = Modifier.weight(1f),
                                singleLine = true, shape = inputShape, colors = textFieldColors,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { processQuickSearchAdd() })
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (showFullDetails) {
                                FilledTonalIconButton(onClick = { showFilterDialog = true }, modifier = Modifier.size(56.dp), shape = CircleShape, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = if(selectedFilterCategory!="Semua" || selectedFilterUnit!="Semua") MaterialTheme.colorScheme.tertiaryContainer else Color(0xFFF0F0F0), contentColor = if(selectedFilterCategory!="Semua" || selectedFilterUnit!="Semua") MaterialTheme.colorScheme.onTertiaryContainer else Color.Black)) { Icon(Icons.Default.FilterList, null) }
                            } else {
                                FilledIconButton(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.size(56.dp), shape = CircleShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)) { Icon(Icons.Default.QrCodeScanner, null) }
                            }
                        }
                    }
                }
            },

            // --- FOOTER ---
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 24.dp,
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = if (isFullScreen) 60.dp else 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .height(60.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.Center) {
                                Text("Total Tagihan", fontSize = 12.sp, color = Color.Gray)
                                Text(formatRupiah(totalPrice), fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Button(
                                onClick = { if(cart.isNotEmpty()) showPaymentDialog = true else Toast.makeText(context, "Keranjang Kosong", Toast.LENGTH_SHORT).show() },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxHeight().width(160.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                elevation = ButtonDefaults.buttonElevation(8.dp)
                            ) {
                                Text("BAYAR", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.Payment, null, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            // --- LIST BARANG ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(finalDisplayList) { product ->
                        val qtyInCart = cart[product] ?: 0.0
                        val isGrosirApplied = product.wholesaleQty > 0 && qtyInCart >= product.wholesaleQty

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                val isWeighedItem = product.unit.equals("Kg", ignoreCase = true) || product.unit.equals("Liter", ignoreCase = true) || product.unit.equals("Ons", ignoreCase = true)
                                if (isWeighedItem) { selectedProductForEdit = product; qtyInputText = ""; showEditQtyDialog = true }
                                else { addToCart(product) }
                            },
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = if (qtyInCart > 0.0) CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)) else CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (product.imagePath != null) AsyncImage(model = File(product.imagePath), null, Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray), contentScale = ContentScale.Crop)
                                else Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray), contentAlignment = Alignment.Center) { Icon(Icons.Default.Image, null, tint = Color.White) }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Bold)
                                    Text("${product.category} • @ ${formatRupiah(product.sellPrice)} / ${product.unit}", fontSize = 12.sp, color = Color.Gray)
                                    if (product.description.isNotEmpty()) { Text(product.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontStyle = FontStyle.Italic) }
                                    val stockColor = if (product.stock <= 5) Color.Red else Color.Black
                                    Text("Sisa: ${formatQty(product.stock)}", fontSize = 11.sp, color = stockColor)
                                    if (product.wholesaleQty > 0) Text("Grosir: ${formatRupiah(product.wholesalePrice)} (Min ${formatQty(product.wholesaleQty)})", fontSize = 10.sp, color = Color(0xFF2E7D32))
                                }

                                if (qtyInCart > 0.0) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        if (isGrosirApplied) Surface(color = Color(0xFFC8E6C9), shape = RoundedCornerShape(4.dp)) { Text("Grosir Aktif", fontSize = 10.sp, color = Color(0xFF1B5E20), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { updateQty(product, qtyInCart - 1.0) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.RemoveCircle, null, tint = Color.Red) }
                                            Surface(modifier = Modifier.padding(horizontal = 8.dp).clickable { selectedProductForEdit = product; qtyInputText = formatQty(qtyInCart); showEditQtyDialog = true }, shape = RoundedCornerShape(4.dp), color = Color.White, border = BorderStroke(1.dp, Color.LightGray)) { Text(formatQty(qtyInCart), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) }
                                            IconButton(onClick = { updateQty(product, qtyInCart + 1.0) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.AddCircle, null, tint = Color(0xFF2E7D32)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    // --- RENDER MODE ---
    if (isFullScreen) {
        Dialog(
            onDismissRequest = { /* Block dismiss */ },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            LaunchedEffect(Unit) {
                Toast.makeText(context, "Mode Fokus Aktif! Tekan tombol [Keluar] di pojok kanan atas untuk kembali.", Toast.LENGTH_LONG).show()
            }
            Surface(modifier = Modifier.fillMaxSize()) {
                salesContent()
            }
        }
    } else {
        salesContent()
    }

    // --- DIALOG DIALOG ---
    if (showFullScreenDialog) {
        AlertDialog(
            onDismissRequest = { showFullScreenDialog = false },
            icon = { Icon(if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, null) },
            title = { Text(if (isFullScreen) "Keluar Mode Fokus?" else "Masuk Mode Fokus?") },
            text = { Text(if (isFullScreen) "Kembali ke tampilan standar." else "Mode ini akan menyembunyikan menu dan memperluas area kasir.") },
            confirmButton = {
                Button(onClick = { isFullScreen = !isFullScreen; isInputExpanded = true; showFullScreenDialog = false }) { Text("Ya") }
            },
            dismissButton = { TextButton(onClick = { showFullScreenDialog = false }) { Text("Batal") } }
        )
    }

    if (showFilterDialog) {
        var categorySearch by remember { mutableStateOf("") }
        var unitSearch by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Produk", fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                    Text("Kategori", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(value = categorySearch, onValueChange = { categorySearch = it }, placeholder = { Text("Cari Kategori...") }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterChip(selected = selectedFilterCategory=="Semua", onClick = { selectedFilterCategory="Semua" }, label = { Text("Semua") }) }
                        items(dbCategories.filter { it.name.contains(categorySearch, ignoreCase = true) }) { cat ->
                            FilterChip(selected = selectedFilterCategory==cat.name, onClick = { selectedFilterCategory=cat.name }, label = { Text(cat.name) })
                        }
                    }
                    Divider(Modifier.padding(vertical = 16.dp))
                    Text("Satuan", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(value = unitSearch, onValueChange = { unitSearch = it }, placeholder = { Text("Cari Satuan...") }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterChip(selected = selectedFilterUnit=="Semua", onClick = { selectedFilterUnit="Semua" }, label = { Text("Semua") }) }
                        items(dbUnits.filter { it.name.contains(unitSearch, ignoreCase = true) }) { unit ->
                            FilterChip(selected = selectedFilterUnit==unit.name, onClick = { selectedFilterUnit=unit.name }, label = { Text(unit.name) })
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { showFilterDialog = false }) { Text("Terapkan") } }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(onDismissRequest = { showClearConfirmDialog = false }, title = { Text("Hapus Keranjang?") }, text = { Text("Semua barang di keranjang akan dihapus. Anda yakin?") },
            confirmButton = { Button(onClick = { cart = emptyMap(); selectedCustomerName="Umum"; showClearConfirmDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Ya, Hapus") } },
            dismissButton = { TextButton(onClick = { showClearConfirmDialog = false }) { Text("Batal") } })
    }

    if (showPendingConfirmDialog) {
        AlertDialog(onDismissRequest = { showPendingConfirmDialog = false }, title = { Text("Tunda Transaksi?") }, text = { Text("Transaksi ini akan disimpan ke daftar pending. Lanjutkan?") },
            confirmButton = { Button(onClick = { doPendingTransaction(); showPendingConfirmDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000))) { Text("Ya, Tunda") } },
            dismissButton = { TextButton(onClick = { showPendingConfirmDialog = false }) { Text("Batal") } })
    }

    if (showOutOfStockDialog) {
        AlertDialog(onDismissRequest = { showOutOfStockDialog = false }, icon = { Icon(Icons.Default.Warning, null, tint = Color.Red) }, title = { Text("Stok Habis!") }, text = { Text("Maaf, stok untuk barang \"$outOfStockProductName\" sudah habis.") }, confirmButton = { Button(onClick = { showOutOfStockDialog = false }) { Text("OK") } })
    }

    if (showPendingStockErrorDialog) {
        AlertDialog(onDismissRequest = { showPendingStockErrorDialog = false }, icon = { Icon(Icons.Default.ErrorOutline, null, tint = Color.Red) }, title = { Text("Perhatian!") }, text = { Text(pendingStockErrorText) }, confirmButton = { Button(onClick = { showPendingStockErrorDialog = false }) { Text("Mengerti") } })
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

    if (showPendingDialog) {
        AlertDialog(onDismissRequest = { showPendingDialog = false }, title = { Text("Transaksi Pending") }, text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                if (pendingList.isEmpty()) item { Text("Tidak ada transaksi pending.", color = Color.Gray) }
                else { items(pendingList) { pending -> Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { restorePendingTransaction(pending) }, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(pending.customerName, fontWeight = FontWeight.Bold); Text("${pending.cartItems.size} Item • Jam ${pending.time}", fontSize = 12.sp, color = Color.Gray) }; Icon(Icons.Default.PlayCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(32.dp)) } } } }
            }
        }, confirmButton = { TextButton(onClick = { showPendingDialog = false }) { Text("Tutup") } })
    }

    if (showEditQtyDialog && selectedProductForEdit != null) {
        val product = selectedProductForEdit!!
        AlertDialog(onDismissRequest = { showEditQtyDialog = false }, title = { Text("Atur Jumlah: ${product.name}") }, text = {
            Column {
                Text("Satuan: ${product.unit}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = qtyInputText, onValueChange = { qtyInputText = it }, label = { Text("Masukkan Jumlah") }, placeholder = { Text("Contoh: 1.5 atau 0.5") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done))
            }
        }, confirmButton = { Button(onClick = { val newQty = parseQtyInput(qtyInputText); if (newQty > 0 && newQty <= product.stock) { updateQty(product, newQty); showEditQtyDialog = false } else { outOfStockProductName = product.name; showOutOfStockDialog = true } }) { Text("Simpan") } }, dismissButton = { TextButton(onClick = { updateQty(product, 0.0); showEditQtyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) { Text("Hapus Item") } })
    }

    if (showPaymentDialog) {
        PaymentDialog(
            cart = cart,
            totalPrice = totalPrice,
            customerName = selectedCustomerName,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { payAmount, method ->
                viewModel.checkout(
                    cart = cart,
                    paymentMethod = method,
                    customerName = selectedCustomerName,
                    payAmount = payAmount,
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

    if (showCustomerDialog) {
        var customerSearchQuery by remember { mutableStateOf("") }
        val filteredCustomers = customers.filter { it.name.contains(customerSearchQuery, ignoreCase = true) || it.phoneNumber.contains(customerSearchQuery) }
        Dialog(onDismissRequest = { showCustomerDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pilih Pelanggan", fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = customerSearchQuery, onValueChange = { customerSearchQuery = it }, label = { Text("Cari Nama / No. HP") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true); Spacer(modifier = Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedCustomerName = "Umum"; showCustomerDialog = false }, colors = if(selectedCustomerName == "Umum") CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) { Text("Umum (Non-Member)", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) }
                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) { items(filteredCustomers) { customer -> Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedCustomerName = customer.name; showCustomerDialog = false }, colors = if(selectedCustomerName == customer.name) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) { Column(modifier = Modifier.padding(16.dp)) { Text(customer.name, fontWeight = FontWeight.Bold); if(customer.phoneNumber.isNotEmpty()) Text(customer.phoneNumber, fontSize = 12.sp, color = Color.Gray) } } } }
                    Spacer(modifier = Modifier.height(8.dp)); TextButton(onClick = { showCustomerDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Tutup") }
                }
            }
        }
    }

    if (showScanner) {
        Dialog(onDismissRequest = { showScanner = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                Box { BarcodeScannerView { code -> barcodeInput = code; showScanner = false; processBarcode(code) }; IconButton(onClick = { showScanner = false }, modifier = Modifier.align(Alignment.TopStart).padding(24.dp)) { Icon(Icons.Default.Close, "Tutup", tint = Color.White) } }
            }
        }
    }
}

// --- PAYMENT DIALOG (UPDATED WITH ORDER NUMBER & TOTAL ITEMS) ---
@Composable
fun PaymentDialog(
    cart: Map<Product, Double>,
    totalPrice: Double,
    customerName: String,
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

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Konfirmasi Pembayaran") }, text = {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {

            // Info Pelanggan
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary); Spacer(modifier = Modifier.width(12.dp))
                    Column { Text("Pelanggan", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)); Text(customerName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                }
            }

            // Header Rincian + Total Item
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rincian Pesanan", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                // Menghitung Total Quantity Item
                val totalItems = cart.values.sum()
                Surface(color = Color.LightGray.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp)) {
                    Text("${formatQty(totalItems)} Pcs", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.Black)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // LIST BARANG DENGAN NOMOR URUT
            var index = 1 // Counter untuk nomor
            cart.forEach { (product, qty) ->
                val finalPrice = getPrice(product, qty); val isWholesaleActive = finalPrice < product.sellPrice

                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // NOMOR URUT
                    Text("$index.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(20.dp))

                    if (product.imagePath != null) AsyncImage(model = File(product.imagePath), null, Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(Color.LightGray), contentScale = ContentScale.Crop)
                    else Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(Color.LightGray), contentAlignment = Alignment.Center) { Icon(Icons.Default.ShoppingBag, null, tint = Color.White, modifier = Modifier.size(20.dp)) }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${product.category} • ${product.unit}", fontSize = 11.sp, color = Color.Gray)
                        if (isWholesaleActive) Text("Grosir Aktif", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatRupiah(finalPrice * qty), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("x${formatQty(qty)}", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                index++ // Increment nomor
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metode Pembayaran
            Text("Metode Pembayaran:", fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Tunai", "QRIS", "Transfer").forEach { method -> FilterChip(selected = selectedMethod == method, onClick = { selectedMethod = method }, label = { Text(method) }) } }

            Spacer(modifier = Modifier.height(16.dp))

            // Total
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total Tagihan:", fontWeight = FontWeight.Bold); Text(formatRupiah(totalPrice), fontWeight = FontWeight.Bold, color = Color.Red) }

            Spacer(modifier = Modifier.height(16.dp))

            // Input Tunai
            if (selectedMethod == "Tunai") {
                OutlinedTextField(value = paidAmountText, onValueChange = { paidAmountText = onFormatInput(it) }, label = { Text("Uang Tunai") }, prefix = { Text("Rp") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = if (change >= 0) "Kembalian: ${formatRupiah(change)}" else "Kurang: ${formatRupiah(kotlin.math.abs(change))}", color = if (change >= 0) Color(0xFF2E7D32) else Color.Red, fontWeight = FontWeight.Bold)
            }
        }
    }, confirmButton = { Button(onClick = { onConfirm(paidAmount, selectedMethod) }, enabled = paidAmount >= totalPrice) { Text("SELESAI") } }, dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Batal") } })
}