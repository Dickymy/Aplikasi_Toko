package com.example.aplikasitokosembakoarkhan

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.aplikasitokosembakoarkhan.data.Product
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

    val products by viewModel.allProducts.collectAsState(initial = emptyList())
    val dbCategories by viewModel.allCategories.collectAsState(initial = emptyList())
    val dbUnits by viewModel.allUnits.collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf("Semua") }
    var selectedFilterUnit by remember { mutableStateOf("Semua") }
    var showFilterDialog by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var sellPrice by remember { mutableStateOf("") }
    var buyPrice by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }

    // IMAGE STATE
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var currentImagePath by remember { mutableStateOf<String?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    var expireDate by remember { mutableStateOf<Long?>(null) }

    var isWholesale by remember { mutableStateOf(false) }
    var wholesalePrice by remember { mutableStateOf("") }
    var wholesaleMinQty by remember { mutableStateOf("") }

    var showDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var currentProductId by remember { mutableStateOf(0) }

    var showRestockDialog by remember { mutableStateOf(false) }
    var restockProduct by remember { mutableStateOf<Product?>(null) }
    var restockQty by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    var showQuickAddCategory by remember { mutableStateOf(false) }
    var newQuickCategoryName by remember { mutableStateOf("") }
    var showQuickAddUnit by remember { mutableStateOf(false) }
    var newQuickUnitName by remember { mutableStateOf("") }

    var showScanner by remember { mutableStateOf(false) }
    val scannerPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) showScanner = true }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) selectedImageUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            // Gunakan tempCameraUri yang sudah disiapkan
            tempCameraUri?.let { uri -> selectedImageUri = uri }
        }
    }

    var expandedUnit by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf(false) }

    val filteredProducts = products.filter { product ->
        val matchSearch = product.name.contains(searchQuery, ignoreCase = true) || product.barcode.contains(searchQuery)
        val matchCategory = selectedFilterCategory == "Semua" || product.category == selectedFilterCategory
        val matchUnit = selectedFilterUnit == "Semua" || product.unit == selectedFilterUnit
        matchSearch && matchCategory && matchUnit
    }

    fun createImageUri(context: Context): Uri {
        val directory = File(context.filesDir, "camera_images")
        if (!directory.exists()) directory.mkdirs()
        val file = File(directory, "IMG_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
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

    fun showDatePicker() {
        val calendar = Calendar.getInstance()
        if (expireDate != null && expireDate!! > 0) calendar.timeInMillis = expireDate!!
        DatePickerDialog(context, { _, y, m, d -> val newDate = Calendar.getInstance(); newDate.set(y, m, d); expireDate = newDate.timeInMillis }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                name=""; barcode=""; sellPrice=""; buyPrice=""; stock=""
                selectedUnit = if(dbUnits.isNotEmpty()) dbUnits[0].name else ""
                selectedCategory = if(dbCategories.isNotEmpty()) dbCategories[0].name else ""
                isWholesale=false; wholesalePrice=""; wholesaleMinQty=""; expireDate=null; selectedImageUri=null; currentImagePath=null
                isEditing=false; showDialog=true
            }) { Icon(Icons.Default.Add, "Tambah") }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari Barang / Barcode...") },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))

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

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (filteredProducts.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Barang tidak ditemukan", color = Color.Gray)
                        }
                    }
                } else {
                    items(filteredProducts) { product ->
                        ProductItem(product,
                            onRestock = { restockProduct = product; restockQty = ""; showRestockDialog = true },
                            onEdit = {
                                name = product.name
                                barcode = product.barcode
                                sellPrice = formatInput(product.sellPrice.toInt().toString())
                                buyPrice = formatInput(product.buyPrice.toInt().toString())
                                stock = formatQty(product.stock)
                                selectedUnit = product.unit
                                selectedCategory = product.category
                                isWholesale = product.wholesaleQty > 0
                                wholesalePrice = formatInput(product.wholesalePrice.toInt().toString())
                                wholesaleMinQty = product.wholesaleQty.toString()
                                expireDate = if(product.expireDate > 0) product.expireDate else null
                                currentProductId = product.id
                                currentImagePath = product.imagePath
                                selectedImageUri = null
                                isEditing = true
                                showDialog = true
                            },
                            onDelete = { productToDelete = product; showDeleteConfirm = true },
                            imageFile = getProductImageFile(product.imagePath),
                            formatQty = { formatQty(it) }
                        )
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Data Barang") },
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

    if (showDialog) {
        AlertDialog(onDismissRequest = { showDialog = false }, title = { Text(if (isEditing) "Edit Barang" else "Tambah Barang") }, text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray)
                        .clickable { showImageSourceDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) Image(rememberAsyncImagePainter(selectedImageUri), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else if (getProductImageFile(currentImagePath) != null) Image(rememberAsyncImagePainter(getProductImageFile(currentImagePath)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Text("Tambah Gambar", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("Barcode (Unik)") }, trailingIcon = { IconButton(onClick = { scannerPermissionLauncher.launch(Manifest.permission.CAMERA) }) { Icon(Icons.Default.QrCodeScanner, null) } }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Barang") }, modifier = Modifier.fillMaxWidth())

                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    OutlinedTextField(value = selectedCategory, onValueChange = {}, readOnly = true, label = { Text("Kategori") }, trailingIcon = { IconButton(onClick = { expandedCategory = true }) { Icon(Icons.Default.ArrowDropDown, null) } }, modifier = Modifier.fillMaxWidth())
                    DropdownMenu(expanded = expandedCategory, onDismissRequest = { expandedCategory = false }) {
                        dbCategories.forEach { cat -> DropdownMenuItem(text = { Text(cat.name) }, onClick = { selectedCategory = cat.name; expandedCategory = false }) }
                        Divider()
                        DropdownMenuItem(text = { Text("+ Kategori Baru", color = MaterialTheme.colorScheme.primary) }, onClick = { expandedCategory = false; newQuickCategoryName = ""; showQuickAddCategory = true })
                    }
                }

                Row {
                    OutlinedTextField(value = sellPrice, onValueChange = { sellPrice = formatInput(it) }, label = { Text("Jual") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(value = buyPrice, onValueChange = { buyPrice = formatInput(it) }, label = { Text("Modal") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }

                Row {
                    OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stok") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f).padding(top=8.dp)) {
                        OutlinedTextField(value = selectedUnit, onValueChange = {}, readOnly = true, label = { Text("Satuan") }, trailingIcon = { IconButton(onClick = { expandedUnit = true }) { Icon(Icons.Default.ArrowDropDown, null) } }, modifier = Modifier.fillMaxWidth())
                        DropdownMenu(expanded = expandedUnit, onDismissRequest = { expandedUnit = false }) {
                            dbUnits.forEach { unit -> DropdownMenuItem(text = { Text(unit.name) }, onClick = { selectedUnit = unit.name; expandedUnit = false }) }
                            Divider()
                            DropdownMenuItem(text = { Text("+ Satuan Baru", color = MaterialTheme.colorScheme.primary) }, onClick = { expandedUnit = false; newQuickUnitName = ""; showQuickAddUnit = true })
                        }
                    }
                }
                OutlinedTextField(value = if (expireDate != null && expireDate!! > 0) SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(expireDate!!)) else "", onValueChange = {}, label = { Text("Expired (Opsional)") }, readOnly = true, trailingIcon = { IconButton(onClick = { showDatePicker() }) { Icon(Icons.Default.DateRange, null) } }, modifier = Modifier.fillMaxWidth().clickable { showDatePicker() })

                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = isWholesale, onCheckedChange = { isWholesale = it }); Text("Grosir") }
                if (isWholesale) {
                    Row {
                        OutlinedTextField(value = wholesaleMinQty, onValueChange = { wholesaleMinQty = it }, label = { Text("Min Qty") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f));
                        Spacer(modifier = Modifier.width(8.dp));
                        OutlinedTextField(value = wholesalePrice, onValueChange = { wholesalePrice = formatInput(it) }, label = { Text("Harga Grosir") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    }
                }
            }
        }, confirmButton = {
            Button(onClick = {
                if (name.isNotEmpty() && sellPrice.isNotEmpty()) {
                    val isDuplicate = if (isEditing) products.any { it.barcode == barcode && it.id != currentProductId && barcode.isNotEmpty() } else products.any { it.barcode == barcode && barcode.isNotEmpty() }
                    if (isDuplicate) {
                        Toast.makeText(context, "Gagal: Barcode '$barcode' sudah digunakan!", Toast.LENGTH_LONG).show()
                    } else {
                        val p = Product(
                            id = if (isEditing) currentProductId else 0,
                            name = name,
                            barcode = if(barcode.isEmpty()) System.currentTimeMillis().toString() else barcode,
                            sellPrice = cleanInput(sellPrice),
                            buyPrice = cleanInput(buyPrice),
                            stock = stock.replace(",", ".").toDoubleOrNull() ?: 0.0,
                            unit = if(selectedUnit.isEmpty()) "Pcs" else selectedUnit,
                            category = if(selectedCategory.isEmpty()) "Umum" else selectedCategory,
                            imagePath = currentImagePath,
                            expireDate = expireDate?:0L,
                            wholesaleQty = if(isWholesale) wholesaleMinQty.replace(",", ".").toDoubleOrNull() ?: 0.0 else 0.0,
                            wholesalePrice = if(isWholesale) cleanInput(wholesalePrice) else 0.0
                        )
                        if (isEditing) viewModel.updateProductWithImage(p, selectedImageUri, context) else viewModel.insertProductWithImage(p, selectedImageUri, context)
                        showDialog = false
                    }
                } else { Toast.makeText(context, "Nama dan Harga Jual wajib diisi", Toast.LENGTH_SHORT).show() }
            }) { Text("Simpan") }
        }, dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Batal") } })
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Pilih Gambar") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Ambil Foto (Kamera)") },
                        leadingContent = { Icon(Icons.Default.CameraAlt, null) },
                        modifier = Modifier.clickable {
                            showImageSourceDialog = false
                            val uri = createImageUri(context)
                            tempCameraUri = uri // 1. Simpan URI ke state
                            cameraLauncher.launch(uri) // 2. Gunakan URI lokal
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Pilih dari Galeri") },
                        leadingContent = { Icon(Icons.Default.Image, null) },
                        modifier = Modifier.clickable {
                            showImageSourceDialog = false
                            galleryLauncher.launch("image/*")
                        }
                    )
                    if (selectedImageUri != null || currentImagePath != null) {
                        ListItem(
                            headlineContent = { Text("Hapus Gambar", color = Color.Red) },
                            leadingContent = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                            modifier = Modifier.clickable {
                                selectedImageUri = null
                                currentImagePath = null
                                showImageSourceDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showImageSourceDialog = false }) { Text("Batal") } }
        )
    }

    if (showQuickAddCategory) {
        AlertDialog(
            onDismissRequest = { showQuickAddCategory = false },
            title = { Text("Tambah Kategori Baru") },
            text = { OutlinedTextField(value = newQuickCategoryName, onValueChange = { newQuickCategoryName = it }, label = { Text("Nama Kategori") }, singleLine = true) },
            confirmButton = { Button(onClick = { if (newQuickCategoryName.isNotEmpty()) { viewModel.addCategory(newQuickCategoryName); selectedCategory = newQuickCategoryName; showQuickAddCategory = false } }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { showQuickAddCategory = false }) { Text("Batal") } }
        )
    }
    if (showQuickAddUnit) {
        AlertDialog(
            onDismissRequest = { showQuickAddUnit = false },
            title = { Text("Tambah Satuan Baru") },
            text = { OutlinedTextField(value = newQuickUnitName, onValueChange = { newQuickUnitName = it }, label = { Text("Nama Satuan") }, singleLine = true) },
            confirmButton = { Button(onClick = { if (newQuickUnitName.isNotEmpty()) { viewModel.addUnit(newQuickUnitName); selectedUnit = newQuickUnitName; showQuickAddUnit = false } }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { showQuickAddUnit = false }) { Text("Batal") } }
        )
    }

    if (showRestockDialog && restockProduct != null) {
        AlertDialog(onDismissRequest = { showRestockDialog = false }, title = { Text("Restok: ${restockProduct!!.name}") }, text = {
            Column {
                Text("Stok Saat Ini: ${formatQty(restockProduct!!.stock)} ${restockProduct!!.unit}")
                OutlinedTextField(value = restockQty, onValueChange = { restockQty = it }, label = { Text("Tambah Stok") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }, confirmButton = {
            Button(onClick = {
                val addQty = restockQty.replace(",", ".").toDoubleOrNull() ?: 0.0
                val newStock = restockProduct!!.stock + addQty
                viewModel.updateProductWithImage(restockProduct!!.copy(stock = newStock), null, context)
                showRestockDialog = false
            }) { Text("Simpan") }
        }, dismissButton = { TextButton(onClick = { showRestockDialog = false }) { Text("Batal") } })
    }

    if (showDeleteConfirm && productToDelete != null) {
        AlertDialog(onDismissRequest = { showDeleteConfirm = false }, title = { Text("Hapus?") }, text = { Text("Yakin hapus '${productToDelete!!.name}'?") },
            confirmButton = { Button(onClick = { viewModel.delete(productToDelete!!); showDeleteConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") } })
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

@Composable
fun ProductItem(
    product: Product,
    onRestock: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    imageFile: File?,
    formatQty: (Double) -> String
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                if (imageFile != null) {
                    Image(rememberAsyncImagePainter(imageFile), "Img", modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray), contentScale = ContentScale.Crop)
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Kode: ${product.barcode}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) { Text(product.category, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stok: ${formatQty(product.stock)} ${product.unit}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    if (product.expireDate > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Exp: ${SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(product.expireDate))}", style = MaterialTheme.typography.labelSmall, color = Color.Red)
                    }
                }
                val formattedPrice = NumberFormat.getInstance(Locale("id", "ID")).format(product.sellPrice)
                Text("Rp $formattedPrice", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Row(horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onRestock) { Icon(Icons.Default.AddBox, "Restok", tint = Color(0xFF2E7D32)) }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit", tint = Color.Blue) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Hapus", tint = Color.Red) }
            }
        }
    }
}