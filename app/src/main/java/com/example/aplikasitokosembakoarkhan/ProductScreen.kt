package com.example.aplikasitokosembakoarkhan

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.aplikasitokosembakoarkhan.data.Category
import com.example.aplikasitokosembakoarkhan.data.Product
import com.example.aplikasitokosembakoarkhan.data.UnitModel
import com.example.aplikasitokosembakoarkhan.utils.BarcodeScannerView
import java.io.File
import java.text.DecimalFormat
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
    var selectedFilterCategory by remember { mutableStateOf("Semua") }
    var selectedFilterUnit by remember { mutableStateOf("Semua") }

    var showCategoryFilterDialog by remember { mutableStateOf(false) }
    var showUnitFilterDialog by remember { mutableStateOf(false) }

    // --- STATE DIALOGS ---
    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }

    // State Restok & Hapus
    var showRestockDialog by remember { mutableStateOf(false) }
    var restockProduct by remember { mutableStateOf<Product?>(null) }
    var restockQty by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf<Product?>(null) }

    // STATE BARU: PREVIEW GAMBAR FULL
    var selectedImageForPreview by remember { mutableStateOf<File?>(null) }

    // State Scanner
    var showSearchScanner by remember { mutableStateOf(false) }
    val searchScannerPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) showSearchScanner = true else Toast.makeText(context, "Izin kamera diperlukan", Toast.LENGTH_SHORT).show()
    }

    // --- FILTER LOGIC ---
    val filteredProducts = products.filter { product ->
        val matchName = product.name.contains(searchQuery, ignoreCase = true) || product.barcode.contains(searchQuery)
        val matchCategory = selectedFilterCategory == "Semua" || product.category == selectedFilterCategory
        val matchUnit = selectedFilterUnit == "Semua" || product.unit == selectedFilterUnit
        matchName && matchCategory && matchUnit
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
                    // Search Bar
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

                    // FILTER ROW (Kategori & Satuan)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Filter Kategori
                        FilterChip(
                            selected = selectedFilterCategory != "Semua",
                            onClick = { showCategoryFilterDialog = true },
                            label = { Text(if(selectedFilterCategory == "Semua") "Kategori" else selectedFilterCategory) },
                            leadingIcon = { Icon(Icons.Default.Category, null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
                        )

                        // Filter Satuan
                        FilterChip(
                            selected = selectedFilterUnit != "Semua",
                            onClick = { showUnitFilterDialog = true },
                            label = { Text(if(selectedFilterUnit == "Semua") "Satuan" else selectedFilterUnit) },
                            leadingIcon = { Icon(Icons.Default.Straighten, null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer)
                        )

                        if (selectedFilterCategory != "Semua" || selectedFilterUnit != "Semua") {
                            IconButton(onClick = { selectedFilterCategory = "Semua"; selectedFilterUnit = "Semua" }) {
                                Icon(Icons.Default.Close, "Reset Filter", tint = Color.Red)
                            }
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
                            formatQty = { formatQty(it) },
                            onImageClick = { file -> selectedImageForPreview = file } // Trigger Preview
                        )
                    }
                }
            }
        }
    }

    // --- DIALOG PREVIEW FOTO FULL SCREEN (BARU) ---
    if (selectedImageForPreview != null) {
        Dialog(
            onDismissRequest = { selectedImageForPreview = null },
            properties = DialogProperties(usePlatformDefaultWidth = false) // Full Screen
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // Gambar
                AsyncImage(
                    model = selectedImageForPreview,
                    contentDescription = "Full Image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().align(Alignment.Center)
                )

                // Tombol Tutup (X) di pojok kanan atas
                IconButton(
                    onClick = { selectedImageForPreview = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                }
            }
        }
    }

    // --- DIALOG FILTER KATEGORI ---
    if (showCategoryFilterDialog) {
        SearchableFilterDialog(
            title = "Pilih Kategori",
            items = listOf("Semua") + categories.map { it.name },
            onDismiss = { showCategoryFilterDialog = false },
            onSelected = { selectedFilterCategory = it; showCategoryFilterDialog = false }
        )
    }

    // --- DIALOG FILTER SATUAN ---
    if (showUnitFilterDialog) {
        SearchableFilterDialog(
            title = "Pilih Satuan",
            items = listOf("Semua") + units.map { it.name },
            onDismiss = { showUnitFilterDialog = false },
            onSelected = { selectedFilterUnit = it; showUnitFilterDialog = false }
        )
    }

    // --- DIALOG INPUT/EDIT ---
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
                    Toast.makeText(context, "Berhasil menambahkan ${newProduct.name}", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.updateProductWithImage(newProduct, uri, context)
                    Toast.makeText(context, "Berhasil mengubah ${newProduct.name}", Toast.LENGTH_SHORT).show()
                }
                showAddDialog = false
            },
            onAddCategory = { name -> viewModel.addCategory(name, false) },
            onAddUnit = { name -> viewModel.addUnit(name, false) }
        )
    }

    // --- DIALOG LAINNYA ---
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
                        Toast.makeText(context, "Stok berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                        showRestockDialog = false
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showRestockDialog = false }) { Text("Batal") } }
        )
    }

    if (showDeleteConfirm != null) {
        val product = showDeleteConfirm!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            icon = { Icon(Icons.Default.Warning, null, tint = Color.Red) },
            title = { Text("Hapus Barang?") },
            text = { Text("Anda yakin ingin menghapus '${product.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.delete(product)
                        Toast.makeText(context, "Barang berhasil dihapus", Toast.LENGTH_SHORT).show()
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("Batal") } }
        )
    }

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

// --- DIALOG FILTER PENCARIAN (REUSABLE) ---
@Composable
fun SearchableFilterDialog(
    title: String,
    items: List<String>,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredItems = items.filter { it.contains(searchQuery, ignoreCase = true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(filteredItems) { item ->
                        TextButton(
                            onClick = { onSelected(item) },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Text(item, color = Color.Black, modifier = Modifier.weight(1f))
                        }
                        Divider(color = Color.LightGray.copy(alpha = 0.3f))
                    }
                }

                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Tutup") }
            }
        }
    }
}

// --- DROPDOWN SEARCHABLE (UNTUK FORM INPUT) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableDropdownInput(
    label: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    onAddNew: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var newText by remember { mutableStateOf("") }

    Box {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { IconButton(onClick = { expanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = Color.Black,
                disabledBorderColor = Color.Gray,
                disabledLabelColor = Color.Gray
            ),
            enabled = false // Agar klik tembus ke Box
        )

        if (expanded) {
            Dialog(onDismissRequest = { expanded = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Pilih $label", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = searchQuery, onValueChange = { searchQuery = it },
                            placeholder = { Text("Cari...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))

                        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                            item {
                                TextButton(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text("+ Tambah $label Baru", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }

                            val filtered = options.filter { it.contains(searchQuery, ignoreCase = true) }
                            items(filtered) { opt ->
                                TextButton(onClick = { onOptionSelected(opt); expanded = false }, modifier = Modifier.fillMaxWidth()) {
                                    Text(opt, color = Color.Black, modifier = Modifier.fillMaxWidth())
                                }
                                Divider(color = Color.LightGray.copy(0.3f))
                            }
                        }
                        TextButton(onClick = { expanded = false }, modifier = Modifier.fillMaxWidth()) { Text("Batal") }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah $label Baru") },
            text = { OutlinedTextField(value = newText, onValueChange = { newText = it }, singleLine = true) },
            confirmButton = {
                Button(onClick = {
                    if(newText.isNotEmpty()) { onAddNew(newText); onOptionSelected(newText); showAddDialog = false; expanded = false }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Batal") } }
        )
    }
}

// --- DIALOG ENTRY (FIX FORMAT UANG & STOK) ---
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
    // FORMATTER UANG
    val numberFormat = DecimalFormat("#,###")

    // FORMATTER STOK (Hapus .0 jika bulat)
    fun formatStokEdit(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
    }

    // STATE FORM
    var name by remember { mutableStateOf(product?.name ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }

    // State Harga (Simpan String terformat)
    var buyPriceRaw by remember { mutableStateOf(if (product != null) product.buyPrice else 0.0) }
    var sellPriceRaw by remember { mutableStateOf(if (product != null) product.sellPrice else 0.0) }
    var buyPriceText by remember { mutableStateOf(if (product != null) numberFormat.format(product.buyPrice) else "") }
    var sellPriceText by remember { mutableStateOf(if (product != null) numberFormat.format(product.sellPrice) else "") }

    var stock by remember { mutableStateOf(if (product != null) formatStokEdit(product.stock) else "") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var expireDate by remember { mutableStateOf(product?.expireDate ?: 0L) }

    var isWholesale by remember { mutableStateOf(product?.wholesaleQty ?: 0.0 > 0) }
    var wholesaleMinQty by remember { mutableStateOf(if (product != null && product.wholesaleQty > 0) formatStokEdit(product.wholesaleQty) else "") }

    var wholesalePriceRaw by remember { mutableStateOf(if (product != null && product.wholesaleQty > 0) product.wholesalePrice else 0.0) }
    var wholesalePriceText by remember { mutableStateOf(if (product != null && product.wholesaleQty > 0) numberFormat.format(product.wholesalePrice) else "") }

    var selectedCategory by remember { mutableStateOf(product?.category ?: categories.firstOrNull { it.isPriority }?.name ?: "") }
    var selectedUnit by remember { mutableStateOf(product?.unit ?: units.firstOrNull { it.isPriority }?.name ?: "") }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    fun createImageUri(ctx: Context): Uri {
        val file = File(ctx.filesDir, "IMG_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success -> if (success && tempCameraUri != null) selectedImageUri = tempCameraUri }
    fun launchCamera() { val uri = createImageUri(context); tempCameraUri = uri; cameraLauncher.launch(uri) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) launchCamera() else Toast.makeText(context, "Izin Ditolak", Toast.LENGTH_SHORT).show() }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { selectedImageUri = it }

    var showScanner by remember { mutableStateOf(false) }
    val datePickerDialog = DatePickerDialog(context, { _, y, m, d -> val c=Calendar.getInstance(); c.set(y,m,d); expireDate=c.timeInMillis }, Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DAY_OF_MONTH))

    // Helper Input Uang
    fun onPriceChange(input: String, updateRaw: (Double) -> Unit, updateText: (String) -> Unit) {
        val cleanString = input.replace(".", "").replace(",", "")
        if (cleanString.isEmpty()) {
            updateRaw(0.0)
            updateText("")
        } else {
            val parsed = cleanString.toDoubleOrNull() ?: 0.0
            updateRaw(parsed)
            updateText(numberFormat.format(parsed))
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 650.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = if (product == null) "Tambah Barang" else "Edit Barang", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(16.dp))

                Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    // IMAGE
                    Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF5F5F5)).clickable { showImageSourceDialog = true }, contentAlignment = Alignment.Center) {
                        if (selectedImageUri != null) AsyncImage(model = selectedImageUri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else if (product?.imagePath != null) AsyncImage(model = File(product.imagePath), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.AddAPhoto, null, tint = Color.Gray); Text("Foto", fontSize = 10.sp) }
                    }
                    Spacer(Modifier.height(16.dp))

                    // BARCODE & NAMA
                    OutlinedTextField(
                        value = barcode, onValueChange = { barcode = it }, label = { Text("Barcode") },
                        trailingIcon = { IconButton(onClick = { showScanner = true }) { Icon(Icons.Default.QrCodeScanner, null) } },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Barang") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Keterangan") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))

                    // HARGA (TERFORMAT)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = buyPriceText, onValueChange = { onPriceChange(it, { v -> buyPriceRaw = v }, { t -> buyPriceText = t }) },
                            label = { Text("Modal (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = sellPriceText, onValueChange = { onPriceChange(it, { v -> sellPriceRaw = v }, { t -> sellPriceText = t }) },
                            label = { Text("Jual (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // STOK & SATUAN (DROPDOWN SEARCHABLE)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stock, onValueChange = { if(it.all { c -> c.isDigit() || c=='.' }) stock = it },
                            label = { Text("Stok") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)
                        )
                        Box(Modifier.weight(1f)) {
                            SearchableDropdownInput("Satuan", selectedUnit, units.map { it.name }, { selectedUnit = it }, onAddUnit)
                        }
                    }

                    // KATEGORI & EXPIRED
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            SearchableDropdownInput("Kategori", selectedCategory, categories.map { it.name }, { selectedCategory = it }, onAddCategory)
                        }
                        OutlinedTextField(
                            value = if (expireDate > 0) SimpleDateFormat("dd/MM/yy").format(Date(expireDate)) else "",
                            onValueChange = {}, readOnly = true, label = { Text("Expired") },
                            trailingIcon = { if (expireDate > 0) IconButton(onClick = { expireDate = 0L }) { Icon(Icons.Default.Close, null, tint = Color.Red) } else IconButton(onClick = { datePickerDialog.show() }) { Icon(Icons.Default.DateRange, null) } },
                            modifier = Modifier.weight(1f).clickable { datePickerDialog.show() }, shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // GROSIR
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = isWholesale, onCheckedChange = { isWholesale = it }); Text("Grosir") }
                    if (isWholesale) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = wholesaleMinQty, onValueChange = { wholesaleMinQty = it }, label = { Text("Min Qty") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
                            OutlinedTextField(
                                value = wholesalePriceText, onValueChange = { onPriceChange(it, { v -> wholesalePriceRaw = v }, { t -> wholesalePriceText = t }) },
                                label = { Text("Hrg Grosir (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        if (name.isNotEmpty() && sellPriceRaw > 0) {
                            val currentId = product?.id ?: 0
                            val isDuplicate = barcode.isNotEmpty() && existingProducts.any { it.barcode == barcode && it.id != currentId }
                            if (isDuplicate) {
                                Toast.makeText(context, "Barcode sudah ada!", Toast.LENGTH_LONG).show()
                            } else {
                                val p = product?.copy(
                                    name = name, barcode = barcode, buyPrice = buyPriceRaw, sellPrice = sellPriceRaw,
                                    stock = stock.toDoubleOrNull() ?: 0.0, unit = selectedUnit.ifEmpty { "Pcs" }, category = selectedCategory.ifEmpty { "Umum" },
                                    expireDate = expireDate, description = description, wholesaleQty = if(isWholesale) wholesaleMinQty.toDoubleOrNull()?:0.0 else 0.0, wholesalePrice = if(isWholesale) wholesalePriceRaw else 0.0
                                ) ?: Product(
                                    name = name, barcode = if(barcode.isEmpty()) System.currentTimeMillis().toString() else barcode,
                                    buyPrice = buyPriceRaw, sellPrice = sellPriceRaw, stock = stock.toDoubleOrNull() ?: 0.0,
                                    unit = selectedUnit.ifEmpty { "Pcs" }, category = selectedCategory.ifEmpty { "Umum" },
                                    expireDate = expireDate, description = description, wholesaleQty = if(isWholesale) wholesaleMinQty.toDoubleOrNull()?:0.0 else 0.0, wholesalePrice = if(isWholesale) wholesalePriceRaw else 0.0
                                )
                                onSave(p, selectedImageUri)
                            }
                        } else { Toast.makeText(context, "Nama & Harga Jual Wajib", Toast.LENGTH_SHORT).show() }
                    }, shape = RoundedCornerShape(8.dp)) { Text("Simpan") }
                }
            }
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(onDismissRequest = { showImageSourceDialog = false }, title = { Text("Pilih Gambar") }, text = { Column { ListItem(headlineContent = { Text("Kamera") }, leadingContent = { Icon(Icons.Default.CameraAlt, null) }, modifier = Modifier.clickable { if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchCamera() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA); showImageSourceDialog = false }); ListItem(headlineContent = { Text("Galeri") }, leadingContent = { Icon(Icons.Default.Image, null) }, modifier = Modifier.clickable { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)); showImageSourceDialog = false }) } }, confirmButton = {})
    }
    if (showScanner) {
        Dialog(onDismissRequest = { showScanner = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) { Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) { Box { BarcodeScannerView { code -> barcode = code; showScanner = false }; IconButton(onClick = { showScanner = false }, modifier = Modifier.align(Alignment.TopStart).padding(24.dp)) { Icon(Icons.Default.Close, null, tint = Color.White) } } } }
    }
}

// --- ITEM CARD PRODUK ---
@Composable
fun ProductItemCard(
    product: Product,
    imageFile: File?,
    onEdit: () -> Unit,
    onRestock: () -> Unit,
    onDelete: () -> Unit,
    formatQty: (Double) -> String,
    onImageClick: (File?) -> Unit // CALLBACK BARU UNTUK KLIK FOTO
) {
    val isLowStock = product.stock <= 5
    Card(modifier = Modifier.fillMaxWidth().clickable { onEdit() }, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(3.dp), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row {
                // BOX IMAGE DENGAN KLIK KHUSUS
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF5F5F5))
                        .clickable { onImageClick(imageFile) }, // BUKA FULL SCREEN SAAT DIKLIK
                    contentAlignment = Alignment.Center
                ) {
                    if (imageFile != null) Image(rememberAsyncImagePainter(imageFile), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else if (product.imagePath != null) AsyncImage(model = File(product.imagePath), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Icon(Icons.Outlined.Image, null, tint = Color.LightGray, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (product.barcode.isNotEmpty()) Text("Kode: ${product.barcode}", fontSize = 11.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(4.dp)) { Text(product.category, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("• ${product.unit}", fontSize = 12.sp, color = Color.Gray)
                    }

                    // --- PERBAIKAN: TAMPILKAN KETERANGAN DI SINI ---
                    if (product.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = product.description,
                            style = TextStyle(fontSize = 11.sp, fontStyle = FontStyle.Italic, color = Color.Gray),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp)); Divider(color = Color(0xFFF0F0F0)); Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Rp ${NumberFormat.getInstance(Locale("id", "ID")).format(product.sellPrice)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
                    Text("Stok: ${formatQty(product.stock)}", color = if (isLowStock) Color.Red else Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row { IconButton(onClick = onRestock) { Icon(Icons.Default.AddBox, "Restok", tint = Color(0xFF2E7D32)) }; IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit", tint = Color.Blue) }; IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Hapus", tint = Color(0xFFE53935)) } }
            }
        }
    }
}