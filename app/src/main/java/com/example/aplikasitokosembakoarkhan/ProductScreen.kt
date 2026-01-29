package com.example.aplikasitokosembakoarkhan

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.aplikasitokosembakoarkhan.data.Category
import com.example.aplikasitokosembakoarkhan.data.Product
import com.example.aplikasitokosembakoarkhan.data.UnitModel
import com.example.aplikasitokosembakoarkhan.utils.BarcodeScannerView
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScreen(
    viewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val products by viewModel.allProducts.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val units by viewModel.allUnits.collectAsState()

    // --- STATE FILTER & SEARCH ---
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf<String?>(null) }

    // --- STATE DIALOGS ---
    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }

    // State Restok
    var showRestockDialog by remember { mutableStateOf(false) }
    var restockProduct by remember { mutableStateOf<Product?>(null) }
    var restockQty by remember { mutableStateOf("") }

    // State Hapus
    var showDeleteConfirm by remember { mutableStateOf<Product?>(null) }

    // State Scanner (Search)
    var showSearchScanner by remember { mutableStateOf(false) }
    val searchScannerPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) showSearchScanner = true else Toast.makeText(context, "Izin kamera diperlukan", Toast.LENGTH_SHORT).show()
    }

    // --- FILTER LOGIC ---
    val filteredProducts = products.filter { product ->
        val matchName = product.name.contains(searchQuery, ignoreCase = true) || product.barcode.contains(searchQuery)
        val matchCategory = selectedFilterCategory == null || product.category == selectedFilterCategory
        matchName && matchCategory
    }

    // --- HELPER FUNCTION ---
    fun formatQty(qty: Double): String {
        return if (qty % 1.0 == 0.0) qty.toInt().toString() else qty.toString()
    }

    fun getProductImageFile(path: String?): File? {
        if (path.isNullOrEmpty()) return null
        val file = File(path)
        if (file.exists()) return file
        val localFile = File(context.filesDir, file.name)
        return if (localFile.exists()) localFile else null
    }

    Scaffold(
        // Hapus containerColor agar kembali ke default background aplikasi (biasanya putih/tema sistem)
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { productToEdit = null; showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Tambah Barang") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // --- HEADER SEARCH & FILTER ---
            Surface(
                shadowElevation = 2.dp,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Search Bar dengan Scanner
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari nama / barcode...") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                        trailingIcon = {
                            IconButton(onClick = { searchScannerPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                                Icon(Icons.Default.QrCodeScanner, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filter Kategori (Chips)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedFilterCategory == null,
                                onClick = { selectedFilterCategory = null },
                                label = { Text("Semua") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), selectedLabelColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedFilterCategory == cat.name,
                                onClick = { selectedFilterCategory = cat.name },
                                label = { Text(cat.name) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), selectedLabelColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }

            // --- LIST PRODUK ---
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (filteredProducts.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("Barang tidak ditemukan", color = Color.Gray)
                        }
                    }
                } else {
                    items(filteredProducts) { product ->
                        ProductItemCard(
                            product = product,
                            imageFile = getProductImageFile(product.imagePath),
                            onEdit = { productToEdit = product; showAddDialog = true },
                            onRestock = { restockProduct = product; restockQty = ""; showRestockDialog = true },
                            onDelete = { showDeleteConfirm = product },
                            formatQty = { formatQty(it) }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOG INPUT/EDIT (TAMPILAN DIPERBAIKI: CARD + STICKY BUTTON) ---
    if (showAddDialog) {
        ProductEntryDialog(
            product = productToEdit,
            existingProducts = products,
            categories = categories,
            units = units,
            context = context,
            onDismiss = { showAddDialog = false },
            onSave = { newProduct, uri ->
                if (productToEdit == null) {
                    viewModel.insertProductWithImage(newProduct, uri, context)
                } else {
                    viewModel.updateProductWithImage(newProduct, uri, context)
                }
                showAddDialog = false
            },
            onAddCategory = { name -> viewModel.addCategory(name, false) },
            onAddUnit = { name -> viewModel.addUnit(name, false) }
        )
    }

    // --- DIALOG RESTOK ---
    if (showRestockDialog && restockProduct != null) {
        AlertDialog(
            onDismissRequest = { showRestockDialog = false },
            title = { Text("Restok: ${restockProduct!!.name}") },
            text = {
                Column {
                    Text("Stok Saat Ini: ${formatQty(restockProduct!!.stock)} ${restockProduct!!.unit}")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restockQty,
                        onValueChange = { if(it.all { c -> c.isDigit() || c == '.' }) restockQty = it },
                        label = { Text("Tambah Stok") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val addQty = restockQty.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (addQty > 0) {
                        val newStock = restockProduct!!.stock + addQty
                        viewModel.updateProductWithImage(restockProduct!!.copy(stock = newStock), null, context)
                        showRestockDialog = false
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showRestockDialog = false }) { Text("Batal") } }
        )
    }

    // --- DIALOG DELETE ---
    if (showDeleteConfirm != null) {
        val product = showDeleteConfirm!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            icon = { Icon(Icons.Default.Warning, null, tint = Color.Red) },
            title = { Text("Hapus Barang?") },
            text = { Text("Anda yakin ingin menghapus '${product.name}'?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.delete(product); showDeleteConfirm = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("Batal") } }
        )
    }

    // --- SCANNER DIALOG (MAIN SEARCH) ---
    if (showSearchScanner) {
        Dialog(onDismissRequest = { showSearchScanner = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                Box {
                    BarcodeScannerView { code ->
                        searchQuery = code
                        showSearchScanner = false
                    }
                    IconButton(onClick = { showSearchScanner = false }, modifier = Modifier.align(Alignment.TopStart).padding(24.dp)) { Icon(Icons.Default.Close, null, tint = Color.White) }
                }
            }
        }
    }
}

// --- KOMPONEN CARD ITEM (CARD PUTIH BERSIH) ---
@Composable
fun ProductItemCard(
    product: Product,
    imageFile: File?,
    onEdit: () -> Unit,
    onRestock: () -> Unit,
    onDelete: () -> Unit,
    formatQty: (Double) -> String
) {
    val isLowStock = product.stock <= 5
    val expiredInfo = if (product.expireDate > 0) {
        val diff = product.expireDate - System.currentTimeMillis()
        val days = diff / (1000 * 60 * 60 * 24)
        if (days < 0) "Expired" else if (days < 30) "$days hari lagi" else ""
    } else ""

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = Color.White), // WARNA CARD PUTIH
        elevation = CardDefaults.cardElevation(3.dp), // Shadow agar menonjol
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row {
                // Image Box
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageFile != null) Image(rememberAsyncImagePainter(imageFile), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else if (product.imagePath != null) AsyncImage(model = File(product.imagePath), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Icon(Icons.Outlined.Image, null, tint = Color.LightGray, modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Detail Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)

                    if (product.barcode.isNotEmpty()) {
                        Text("Kode: ${product.barcode}", fontSize = 11.sp, color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Kategori & Unit Label
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = product.category,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("• ${product.unit}", fontSize = 12.sp, color = Color.Gray)
                    }

                    // Keterangan
                    if (product.description.isNotEmpty()) {
                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray,
                            fontStyle = FontStyle.Italic,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(8.dp))

            // Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Harga & Stok
                Column {
                    Text(
                        "Rp ${NumberFormat.getInstance(Locale("id", "ID")).format(product.sellPrice)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp
                    )

                    Row(modifier = Modifier.padding(top = 2.dp)) {
                        Text(
                            text = "Stok: ${formatQty(product.stock)}",
                            color = if (isLowStock) Color.Red else Color(0xFF2E7D32),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (expiredInfo.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(expiredInfo, color = Color(0xFFEF6C00), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Tombol Aksi
                Row {
                    IconButton(onClick = onRestock) { Icon(Icons.Default.AddBox, "Restok", tint = Color(0xFF2E7D32)) }
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit", tint = Color.Blue) }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Hapus", tint = Color(0xFFE53935)) }
                }
            }
        }
    }
}

// --- DIALOG ENTRY (FIX: BUTTON TIDAK TERTUTUP & TIDAK FULL SCREEN) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEntryDialog(
    product: Product?,
    existingProducts: List<Product>,
    categories: List<Category>,
    units: List<UnitModel>,
    context: Context,
    onDismiss: () -> Unit,
    onSave: (Product, Uri?) -> Unit,
    onAddCategory: (String) -> Unit,
    onAddUnit: (String) -> Unit
) {
    // Form States
    var name by remember { mutableStateOf(product?.name ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var buyPrice by remember { mutableStateOf(if (product != null) product.buyPrice.toLong().toString() else "") }
    var sellPrice by remember { mutableStateOf(if (product != null) product.sellPrice.toLong().toString() else "") }
    var stock by remember { mutableStateOf(if (product != null) product.stock.toString() else "") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var expireDate by remember { mutableStateOf(product?.expireDate ?: 0L) }

    var isWholesale by remember { mutableStateOf(product?.wholesaleQty ?: 0.0 > 0) }
    var wholesalePrice by remember { mutableStateOf(if (product != null && product.wholesaleQty > 0) product.wholesalePrice.toLong().toString() else "") }
    var wholesaleMinQty by remember { mutableStateOf(if (product != null && product.wholesaleQty > 0) product.wholesaleQty.toString() else "") }

    var selectedCategory by remember { mutableStateOf(product?.category ?: categories.firstOrNull { it.isPriority }?.name ?: "") }
    var selectedUnit by remember { mutableStateOf(product?.unit ?: units.firstOrNull { it.isPriority }?.name ?: "") }
    var expandCat by remember { mutableStateOf(false) }
    var expandUnit by remember { mutableStateOf(false) }
    var showQuickCat by remember { mutableStateOf(false) }
    var showQuickUnit by remember { mutableStateOf(false) }
    var newText by remember { mutableStateOf("") }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    fun cleanInput(s: String) = s.replace(".", "").replace(",", "").toDoubleOrNull() ?: 0.0
    fun formatInput(s: String): String {
        return try {
            val num = s.replace("[^\\d]".toRegex(), "").toLong()
            NumberFormat.getInstance(Locale("id", "ID")).format(num)
        } catch (e: Exception) { s }
    }
    fun createImageUri(ctx: Context): Uri {
        val file = File(ctx.filesDir, "IMG_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val uri = createImageUri(context); tempCameraUri = uri
            Toast.makeText(context, "Izin diberikan, silakan coba lagi", Toast.LENGTH_SHORT).show()
        } else { Toast.makeText(context, "Izin kamera ditolak", Toast.LENGTH_SHORT).show() }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { if(it) selectedImageUri = tempCameraUri }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { selectedImageUri = it }

    var showScanner by remember { mutableStateOf(false) }
    val scannerPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if(it) showScanner = true }

    val datePickerDialog = DatePickerDialog(context, { _, y, m, d -> val c=Calendar.getInstance(); c.set(y,m,d); expireDate=c.timeInMillis }, Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DAY_OF_MONTH))

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 650.dp), // BATAS TINGGI DIALOG
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 1. HEADER
                Text(
                    text = if (product == null) "Tambah Barang" else "Edit Barang",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(16.dp))

                // 2. KONTEN SCROLLABLE (Menggunakan weight agar sisa ruang dipakai list)
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Image Picker
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F5))
                            .clickable { showImageSourceDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) AsyncImage(model = selectedImageUri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else if (product?.imagePath != null) AsyncImage(model = File(product.imagePath), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.AddAPhoto, null, tint = Color.Gray); Text("Foto", fontSize = 10.sp) }
                    }
                    Spacer(Modifier.height(16.dp))

                    // Fields
                    OutlinedTextField(
                        value = barcode, onValueChange = { barcode = it }, label = { Text("Barcode (Scan)") },
                        trailingIcon = { IconButton(onClick = { scannerPermissionLauncher.launch(Manifest.permission.CAMERA) }) { Icon(Icons.Default.QrCodeScanner, null) } },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Barang") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Keterangan") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))

                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = buyPrice, onValueChange = { buyPrice = formatInput(it) }, label = { Text("Modal") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
                        OutlinedTextField(value = sellPrice, onValueChange = { sellPrice = formatInput(it) }, label = { Text("Jual") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = stock, onValueChange = { if(it.all { c -> c.isDigit() || c=='.' }) stock = it }, label = { Text("Stok") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
                        // Unit Dropdown
                        Box(Modifier.weight(1f).padding(top=8.dp)) {
                            OutlinedTextField(
                                value = selectedUnit, onValueChange = {}, readOnly = true, label = { Text("Satuan") },
                                trailingIcon = { IconButton(onClick = { expandUnit = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)
                            )
                            DropdownMenu(expanded = expandUnit, onDismissRequest = { expandUnit = false }) {
                                DropdownMenuItem(text = { Text("+ Baru", color = MaterialTheme.colorScheme.primary) }, onClick = { expandUnit = false; newText=""; showQuickUnit = true })
                                units.forEach { u -> DropdownMenuItem(text = { Text(u.name) }, onClick = { selectedUnit = u.name; expandUnit = false }) }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Category Dropdown
                        Box(Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = selectedCategory, onValueChange = {}, readOnly = true, label = { Text("Kategori") },
                                trailingIcon = { IconButton(onClick = { expandCat = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)
                            )
                            DropdownMenu(expanded = expandCat, onDismissRequest = { expandCat = false }) {
                                DropdownMenuItem(text = { Text("+ Baru", color = MaterialTheme.colorScheme.primary) }, onClick = { expandCat = false; newText=""; showQuickCat = true })
                                categories.forEach { cat -> DropdownMenuItem(text = { Text(cat.name) }, onClick = { selectedCategory = cat.name; expandCat = false }) }
                            }
                        }

                        // Expired Date dengan Tombol Hapus (X)
                        OutlinedTextField(
                            value = if (expireDate > 0) SimpleDateFormat("dd/MM/yy").format(Date(expireDate)) else "",
                            onValueChange = {}, readOnly = true, label = { Text("Expired") },
                            trailingIcon = {
                                if (expireDate > 0) {
                                    IconButton(onClick = { expireDate = 0L }) { Icon(Icons.Default.Close, null, tint = Color.Red) }
                                } else {
                                    IconButton(onClick = { datePickerDialog.show() }) { Icon(Icons.Default.DateRange, null) }
                                }
                            },
                            modifier = Modifier.weight(1f).clickable { datePickerDialog.show() }, shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isWholesale, onCheckedChange = { isWholesale = it })
                        Text("Grosir")
                    }
                    if (isWholesale) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = wholesaleMinQty, onValueChange = { wholesaleMinQty = it }, label = { Text("Min Qty") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
                            OutlinedTextField(value = wholesalePrice, onValueChange = { wholesalePrice = formatInput(it) }, label = { Text("Hrg Grosir") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // 3. FOOTER (STICKY BUTTONS) - Selalu terlihat di bawah
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        if (name.isNotEmpty() && sellPrice.isNotEmpty()) {
                            val currentId = product?.id ?: 0
                            val isDuplicate = barcode.isNotEmpty() && existingProducts.any { it.barcode == barcode && it.id != currentId }

                            if (isDuplicate) {
                                Toast.makeText(context, "GAGAL: Barcode sudah digunakan!", Toast.LENGTH_LONG).show()
                            } else {
                                val p = product?.copy(
                                    name = name, barcode = barcode,
                                    buyPrice = cleanInput(buyPrice), sellPrice = cleanInput(sellPrice),
                                    stock = stock.toDoubleOrNull() ?: 0.0,
                                    unit = selectedUnit.ifEmpty { "Pcs" }, category = selectedCategory.ifEmpty { "Umum" },
                                    expireDate = expireDate, description = description,
                                    wholesaleQty = if(isWholesale) wholesaleMinQty.toDoubleOrNull()?:0.0 else 0.0,
                                    wholesalePrice = if(isWholesale) cleanInput(wholesalePrice) else 0.0
                                ) ?: Product(
                                    name = name, barcode = if(barcode.isEmpty()) System.currentTimeMillis().toString() else barcode,
                                    buyPrice = cleanInput(buyPrice), sellPrice = cleanInput(sellPrice),
                                    stock = stock.toDoubleOrNull() ?: 0.0,
                                    unit = selectedUnit.ifEmpty { "Pcs" }, category = selectedCategory.ifEmpty { "Umum" },
                                    expireDate = expireDate, description = description,
                                    wholesaleQty = if(isWholesale) wholesaleMinQty.toDoubleOrNull()?:0.0 else 0.0,
                                    wholesalePrice = if(isWholesale) cleanInput(wholesalePrice) else 0.0
                                )
                                onSave(p, selectedImageUri)
                            }
                        } else { Toast.makeText(context, "Nama & Harga Jual Wajib", Toast.LENGTH_SHORT).show() }
                    }, shape = RoundedCornerShape(8.dp)) { Text("Simpan") }
                }
            }
        }
    }

    // Helper Dialogs
    if (showQuickCat) {
        AlertDialog(onDismissRequest = { showQuickCat = false }, title = { Text("Tambah Kategori") }, text = { OutlinedTextField(value = newText, onValueChange = { newText = it }, singleLine = true) }, confirmButton = { Button(onClick = { if (newText.isNotEmpty()) { onAddCategory(newText); selectedCategory = newText; showQuickCat = false } }) { Text("Simpan") } })
    }
    if (showQuickUnit) {
        AlertDialog(onDismissRequest = { showQuickUnit = false }, title = { Text("Tambah Satuan") }, text = { OutlinedTextField(value = newText, onValueChange = { newText = it }, singleLine = true) }, confirmButton = { Button(onClick = { if (newText.isNotEmpty()) { onAddUnit(newText); selectedUnit = newText; showQuickUnit = false } }) { Text("Simpan") } })
    }
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Pilih Gambar") },
            text = {
                Column {
                    ListItem(headlineContent = { Text("Kamera") }, leadingContent = { Icon(Icons.Default.CameraAlt, null) }, modifier = Modifier.clickable {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        val uri = createImageUri(context); tempCameraUri = uri; cameraLauncher.launch(uri); showImageSourceDialog = false
                    })
                    ListItem(headlineContent = { Text("Galeri") }, leadingContent = { Icon(Icons.Default.Image, null) }, modifier = Modifier.clickable {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)); showImageSourceDialog = false
                    })
                }
            }, confirmButton = {}
        )
    }
    if (showScanner) {
        Dialog(onDismissRequest = { showScanner = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                Box {
                    BarcodeScannerView { code -> barcode = code; showScanner = false }
                    IconButton(onClick = { showScanner = false }, modifier = Modifier.align(Alignment.TopStart).padding(24.dp)) { Icon(Icons.Default.Close, null, tint = Color.White) }
                }
            }
        }
    }
}