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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
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
    // Update: Inject SettingsViewModel untuk ambil data toko
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val productList by viewModel.allProducts.collectAsState()
    val storeProfile by settingsViewModel.storeProfile.collectAsState() // Data Toko dari Pengaturan

    val context = LocalContext.current

    var cart by remember { mutableStateOf(mapOf<Product, Int>()) }
    var showPaymentDialog by remember { mutableStateOf(false) }

    // State Transaksi Sukses
    var showSuccessDialog by remember { mutableStateOf(false) }
    var lastTransactionChange by remember { mutableStateOf(0.0) }
    var lastTransactionPay by remember { mutableStateOf(0.0) }
    var lastCartBackup by remember { mutableStateOf(mapOf<Product, Int>()) }
    var lastPaymentMethod by remember { mutableStateOf("Tunai") }

    var searchQuery by remember { mutableStateOf("") }
    var barcodeInput by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // State Scanner
    var showScanner by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) showScanner = true else Toast.makeText(context, "Izin kamera ditolak!", Toast.LENGTH_SHORT).show()
    }

    val filteredList = productList.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.barcode.contains(searchQuery)
    }

    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    }

    fun getProductPrice(product: Product, qty: Int): Double {
        return if (product.wholesaleQty > 0 && qty >= product.wholesaleQty && product.wholesalePrice > 0) {
            product.wholesalePrice
        } else {
            product.sellPrice
        }
    }

    val totalPrice = cart.entries.sumOf { (product, qty) -> getProductPrice(product, qty) * qty }

    // --- FUNGSI GENERATE STRUK TEXT (UPDATE: PAKAI DATA TOKO) ---
    fun generateReceiptText(cart: Map<Product, Int>, total: Double, pay: Double, change: Double, method: String): String {
        val sb = StringBuilder()
        val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date())

        // Menggunakan Profile Toko dari Settings
        sb.append("*${storeProfile.name.uppercase()}*\n")
        if(storeProfile.address.isNotEmpty()) sb.append("${storeProfile.address}\n")
        if(storeProfile.phone.isNotEmpty()) sb.append("Telp: ${storeProfile.phone}\n")

        sb.append("--------------------------------\n")
        sb.append("Tgl: $date\n")
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

        // Footer dari Settings
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
                intent.setPackage("com.whatsapp.w4b") // WA Business
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Kasir / Penjualan", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = barcodeInput,
                onValueChange = { barcodeInput = it },
                label = { Text("Scan Barcode...") },
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { processBarcode(barcodeInput) })
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(8.dp)
            ) { Icon(Icons.Default.QrCodeScanner, null) }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Cari Nama Barang...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(filteredList) { product ->
                val qtyInCart = cart[product] ?: 0
                val displayQty = if(qtyInCart > 0) qtyInCart else 1

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { addToCart(product) },
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = if (qtyInCart > 0) CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)) else CardDefaults.cardColors()
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

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Total Bayar:", fontSize = 14.sp, color = Color.Gray)
                Text(formatRupiah(totalPrice), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
            }
            Button(
                onClick = { if (cart.isNotEmpty()) showPaymentDialog = true else Toast.makeText(context, "Keranjang kosong!", Toast.LENGTH_SHORT).show() },
                enabled = cart.isNotEmpty()
            ) {
                Icon(Icons.Default.ShoppingCart, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("BAYAR")
            }
        }
    }

    if (showPaymentDialog) {
        PaymentDialog(
            cart = cart,
            totalPrice = totalPrice,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { payAmount, method ->
                viewModel.checkout(
                    cart = cart,
                    paymentMethod = method,
                    onSuccess = {
                        lastTransactionChange = payAmount - totalPrice
                        lastTransactionPay = payAmount
                        lastCartBackup = cart
                        lastPaymentMethod = method

                        cart = emptyMap()
                        showPaymentDialog = false
                        showSuccessDialog = true
                    },
                    onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                )
            },
            formatRupiah = { formatRupiah(it) },
            getPrice = { p, q -> getProductPrice(p, q) }
        )
    }

    if (showSuccessDialog) {
        Dialog(onDismissRequest = { showSuccessDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
                    Text("Transaksi Berhasil!", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    if (lastPaymentMethod == "Tunai") {
                        Text("Kembalian:", color = Color.Gray)
                        Text(formatRupiah(lastTransactionChange), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))
                    } else {
                        Text("Metode: $lastPaymentMethod", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.Blue)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val totalStruk = lastCartBackup.entries.sumOf { getProductPrice(it.key, it.value) * it.value }
                            val text = generateReceiptText(lastCartBackup, totalStruk, lastTransactionPay, lastTransactionChange, lastPaymentMethod)
                            shareToWhatsApp(text)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Icon(Icons.Default.Share, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kirim Struk WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val totalStruk = lastCartBackup.entries.sumOf { getProductPrice(it.key, it.value) * it.value }
                            PrinterHelper.printReceipt(
                                context = context,
                                cart = lastCartBackup,
                                totalPrice = totalStruk,
                                payAmount = lastTransactionPay,
                                change = lastTransactionChange,
                                paymentMethod = lastPaymentMethod
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Print, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cetak Printer Thermal")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showSuccessDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Tutup", color = Color.Gray) }
                }
            }
        }
    }

    if (showScanner) {
        Dialog(
            onDismissRequest = { showScanner = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                Box {
                    BarcodeScannerView(
                        onBarcodeDetected = { code ->
                            showScanner = false
                            processBarcode(code)
                        }
                    )
                    IconButton(
                        onClick = { showScanner = false },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                    ) { Icon(Icons.Default.Close, "Tutup", tint = Color.White) }
                }
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
    getPrice: (Product, Int) -> Double
) {
    var paidAmountText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("Tunai") }

    val paidAmount = if (selectedMethod == "Tunai") paidAmountText.toDoubleOrNull() ?: 0.0 else totalPrice
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
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("${product.name} (x$qty)", fontSize = 14.sp)
                            if (isWholesaleActive) {
                                Text("Grosir Aktif!", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(formatRupiah(finalPrice * qty), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text("Metode Pembayaran:", fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Tunai", "QRIS", "Transfer").forEach { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(method) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Tagihan:", fontWeight = FontWeight.Bold)
                    Text(formatRupiah(totalPrice), fontWeight = FontWeight.Bold, color = Color.Red)
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (selectedMethod == "Tunai") {
                    OutlinedTextField(
                        value = paidAmountText,
                        onValueChange = { if (it.all { c -> c.isDigit() }) paidAmountText = it },
                        label = { Text("Uang Tunai") },
                        prefix = { Text("Rp") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (change >= 0) "Kembalian: ${formatRupiah(change)}" else "Kurang: ${formatRupiah(kotlin.math.abs(change))}",
                        color = if (change >= 0) Color(0xFF2E7D32) else Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(paidAmount, selectedMethod) }, enabled = paidAmount >= totalPrice) { Text("SELESAI") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Batal") } }
    )
}