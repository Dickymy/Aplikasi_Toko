package com.example.aplikasitokosembakoarkhan

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aplikasitokosembakoarkhan.utils.SecurityHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.aplikasitokosembakoarkhan.data.repository.BackupStatus
import android.net.Uri // Penting untuk intent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// --- FUNGSI RESTART APP (DIPINDAHKAN KE SINI AGAR TIDAK ERROR) ---
private fun restartApp(context: Context) {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
    val componentName = intent?.component
    val mainIntent = Intent.makeRestartActivityTask(componentName)
    context.startActivity(mainIntent)
    Runtime.getRuntime().exit(0)
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    inventoryViewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    var currentSection by remember { mutableStateOf("menu") }

    BackHandler(enabled = currentSection != "menu") {
        currentSection = "menu"
    }

    when (currentSection) {
        "menu" -> SettingsMenuContent(onNavigate = { section -> currentSection = section })
        "receipt" -> ReceiptSettingsContent(viewModel) { currentSection = "menu" }
        "printer" -> PrinterSettingsContent(viewModel) { currentSection = "menu" }
        "backup" -> BackupSettingsContent(viewModel) { currentSection = "menu" }
        "security" -> SecuritySettingsContent(viewModel) { currentSection = "menu" }
        "about" -> AboutSectionContent(onBack = { currentSection = "menu" })
        "data_management" -> DataManagementContent(inventoryViewModel) { currentSection = "menu" }
    }
}

@Composable
fun SettingsMenuContent(onNavigate: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Pengaturan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        SettingsItemRow("Koneksi Printer", "Sambungkan printer thermal bluetooth", Icons.Default.Print) { onNavigate("printer") }
        SettingsItemRow("Identitas Toko & Struk", "Atur nama toko, alamat, dan footer", Icons.AutoMirrored.Filled.ReceiptLong) { onNavigate("receipt") }
        SettingsItemRow("Export & Import Excel", "Transfer data barang ke PC/Kasir lain", Icons.Default.TableChart) { onNavigate("data_management") }
        SettingsItemRow("Backup & Restore", "Amankan data database & gambar", Icons.Default.Backup) { onNavigate("backup") }
        SettingsItemRow("Keamanan (PIN)", "Atur kunci & menu yang dilindungi", Icons.Default.Lock) { onNavigate("security") }
        SettingsItemRow("Tentang Aplikasi", "Informasi Pengembang", Icons.Default.Info) { onNavigate("about") }
    }
}

@Composable
fun SettingsItemRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(subtitle, fontSize = 12.sp, color = Color.Gray) }
        }
    }
}

@Composable
fun DataManagementContent(viewModel: InventoryViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            isLoading = true
            viewModel.exportDataToCsv(context, uri,
                onSuccess = { isLoading = false; Toast.makeText(context, "Export Berhasil!", Toast.LENGTH_SHORT).show() },
                onError = { msg -> isLoading = false; Toast.makeText(context, "Gagal: $msg", Toast.LENGTH_LONG).show() }
            )
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            isLoading = true
            viewModel.importDataFromCsv(context, uri,
                onSuccess = { count -> isLoading = false; Toast.makeText(context, "Berhasil import $count barang", Toast.LENGTH_LONG).show() },
                onError = { msg -> isLoading = false; Toast.makeText(context, "Gagal: $msg", Toast.LENGTH_LONG).show() }
            )
        }
    }

    Scaffold(topBar = { SettingsHeader("Data Barang (Excel)", onBack) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Gunakan fitur ini untuk memindahkan data barang ke Komputer (Excel) atau aplikasi kasir lain. Format file adalah .csv (Excel Compatible).", fontSize = 12.sp, color = Color.DarkGray)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Kirim Data ke PC", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom=8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable { if(!isLoading) exportLauncher.launch("DataBarang_Arkhan.csv") },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).background(Color(0xFFE8F5E9), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Upload, null, tint = Color(0xFF2E7D32)) }
                    Spacer(Modifier.width(16.dp))
                    Column { Text("Export ke Excel (.csv)", fontWeight = FontWeight.Bold); Text("Simpan daftar barang", fontSize = 11.sp, color = Color.Gray) }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Ambil Data dari PC", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom=8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable { if(!isLoading) importLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "application/vnd.ms-excel")) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).background(Color(0xFFFFF3E0), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Download, null, tint = Color(0xFFE65100)) }
                    Spacer(Modifier.width(16.dp))
                    Column { Text("Import dari Excel (.csv)", fontWeight = FontWeight.Bold); Text("Masukkan data barang massal", fontSize = 11.sp, color = Color.Gray) }
                }
            }
            if (isLoading) {
                Spacer(Modifier.height(32.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Sedang memproses...", modifier = Modifier.align(Alignment.CenterHorizontally), fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun SecuritySettingsContent(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val isPinSet by remember { mutableStateOf(viewModel.isPinSet()) }
    var creationStep by remember { mutableStateOf(if (isPinSet) 2 else 0) }
    var firstPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    val originalLockedMenus by viewModel.lockedMenus.collectAsState()
    val tempLockedMenus = remember { mutableStateListOf<String>() }
    var hasChanges by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(originalLockedMenus) {
        tempLockedMenus.clear()
        tempLockedMenus.addAll(originalLockedMenus)
        if (!tempLockedMenus.contains("settings")) tempLockedMenus.add("settings")
    }

    val menuOptions = remember {
        listOf(
            "sales" to "Kasir", "restock" to "Restok Barang", "debt" to "Hutang / Kasbon",
            "expense" to "Pengeluaran", "products" to "Data Barang", "customers" to "Data Pelanggan",
            "categories" to "Kategori", "units" to "Satuan", "report" to "Laporan Keuangan",
            "settings" to "Pengaturan"
        )
    }

    Scaffold(topBar = { SettingsHeader("Keamanan (PIN)", onBack) }) { p ->
        Column(modifier = Modifier.padding(p).fillMaxSize()) {
            if (creationStep < 2) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Security, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text(text = if (creationStep == 0) "Buat PIN Baru" else "Konfirmasi PIN", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(text = if (creationStep == 0) "Masukkan 6 angka PIN keamanan" else "Masukkan ulang 6 angka yang sama", color = Color.Gray, fontSize = 14.sp)
                    Spacer(Modifier.height(32.dp))
                    val currentInput = if (creationStep == 0) firstPinInput else confirmPinInput
                    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        repeat(6) { i ->
                            Box(modifier = Modifier.padding(8.dp).size(20.dp).background(color = if (i < currentInput.length) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.3f), shape = CircleShape).border(1.dp, if (i < currentInput.length) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape))
                        }
                    }
                    if (errorMsg.isNotEmpty()) { Spacer(Modifier.height(16.dp)); Text(errorMsg, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(32.dp))
                    CustomNumericKeypad(
                        onNumberClick = { num ->
                            if (creationStep == 0) { if (firstPinInput.length < 6) { firstPinInput += num; errorMsg = ""; if (firstPinInput.length == 6) creationStep = 1 } }
                            else { if (confirmPinInput.length < 6) { confirmPinInput += num; errorMsg = ""; if (confirmPinInput.length == 6) { if (firstPinInput == confirmPinInput) { viewModel.setPin(firstPinInput); showRestartDialog = true } else { errorMsg = "PIN tidak sama! Ulangi."; confirmPinInput = ""; firstPinInput = ""; creationStep = 0 } } } }
                        },
                        onDeleteClick = { if (creationStep == 0) { if (firstPinInput.isNotEmpty()) firstPinInput = firstPinInput.dropLast(1) } else { if (confirmPinInput.isNotEmpty()) confirmPinInput = confirmPinInput.dropLast(1) }; errorMsg = "" }
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).background(Color(0xFFC8E6C9), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Shield, null, tint = Color(0xFF2E7D32)) }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column { Text("Perlindungan Aktif", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("Aplikasi dilindungi PIN 6 digit", color = Color(0xFF1B5E20), fontSize = 12.sp) }
                            }
                        }
                        Button(onClick = { showDeleteDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color(0xFFD32F2F)), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(8.dp)) { Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Nonaktifkan / Hapus PIN") }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), thickness = 8.dp, color = Color.Gray.copy(alpha = 0.1f))
                        Text("Kunci Menu Aplikasi", modifier = Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    items(menuOptions) { (route, name) ->
                        val isMandatory = route == "settings"
                        val isChecked = if (isMandatory) true else tempLockedMenus.contains(route)
                        ListItem(
                            headlineContent = { Text(name, fontWeight = if(isMandatory) FontWeight.Bold else FontWeight.Normal) },
                            trailingContent = { Switch(checked = isChecked, onCheckedChange = { checked -> if (!isMandatory) { if (checked) tempLockedMenus.add(route) else tempLockedMenus.remove(route); hasChanges = true } }, enabled = !isMandatory, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MaterialTheme.colorScheme.primary)) },
                            modifier = Modifier.clickable(enabled = !isMandatory) { if (!isMandatory) { if (isChecked) tempLockedMenus.remove(route) else tempLockedMenus.add(route); hasChanges = true } }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))
                    }
                    item {
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { val finalSet = tempLockedMenus.toSet() + "settings"; viewModel.updateLockedMenus(finalSet); showRestartDialog = true }, modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp), enabled = hasChanges, shape = RoundedCornerShape(8.dp)) { Text("SIMPAN PERUBAHAN") }
                    }
                }
            }
        }
    }
    if (showRestartDialog) { AlertDialog(onDismissRequest = {}, icon = { Icon(Icons.Default.Refresh, null) }, title = { Text("Restart Diperlukan") }, text = { Text("Pengaturan keamanan telah disimpan.") }, confirmButton = { Button(onClick = { restartApp(context) }) { Text("OK, Restart Sekarang") } }, properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) }
    if (showDeleteDialog) { AlertDialog(onDismissRequest = { showDeleteDialog = false }, title = { Text("Hapus Keamanan?") }, text = { Text("Semua kunci halaman akan dibuka.") }, confirmButton = { Button(onClick = { viewModel.removePin(); restartApp(context) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Hapus & Restart") } }, dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") } }) }
}

@Composable
fun PrinterSettingsContent(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val currentPrinter by viewModel.selectedPrinter.collectAsState()
    val currentPaperSize by viewModel.paperSize.collectAsState()
    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions -> if (permissions.entries.all { it.value }) { hasPermission = true; pairedDevices = viewModel.getPairedDevices() } }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) { hasPermission = true; pairedDevices = viewModel.getPairedDevices() }
            else permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN))
        } else { hasPermission = true; pairedDevices = viewModel.getPairedDevices() }
    }

    Scaffold(topBar = { SettingsHeader("Koneksi Printer", onBack) }, containerColor = Color(0xFFF5F5F5)) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = if(currentPrinter.isNotEmpty()) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(if(currentPrinter.isNotEmpty()) Icons.Default.CheckCircle else Icons.Default.Error, null, tint = if(currentPrinter.isNotEmpty()) Color(0xFF2E7D32) else Color(0xFFC62828)); Spacer(Modifier.width(12.dp)); Column { Text(if(currentPrinter.isNotEmpty()) "Printer Dipilih" else "Belum Ada Printer", fontWeight = FontWeight.Bold, color = if(currentPrinter.isNotEmpty()) Color(0xFF2E7D32) else Color(0xFFC62828)); if(currentPrinter.isNotEmpty()) Text(currentPrinter, fontSize = 12.sp, color = Color.Gray) } }
                    if (currentPrinter.isNotEmpty()) { Spacer(Modifier.height(12.dp)); Button(onClick = { isTestingConnection = true; viewModel.testConnection(context, { isTestingConnection = false; Toast.makeText(context, "Koneksi OK!", Toast.LENGTH_SHORT).show() }, { isTestingConnection = false; Toast.makeText(context, "Gagal Terhubung!", Toast.LENGTH_LONG).show() }) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF2E7D32)), modifier = Modifier.fillMaxWidth(), enabled = !isTestingConnection) { if (isTestingConnection) { CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Text("Mengecek...") } else { Icon(Icons.Default.Wifi, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Cek Status Koneksi") } } }
                }
            }
            Text("Ukuran Kertas Struk", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = currentPaperSize == 58, onClick = { viewModel.setPaperSize(58) }, label = { Text("58mm (Standar)") }, leadingIcon = if (currentPaperSize == 58) { { Icon(Icons.Default.Check, null) } } else null)
                FilterChip(selected = currentPaperSize == 80, onClick = { viewModel.setPaperSize(80) }, label = { Text("80mm (Lebar)") }, leadingIcon = if (currentPaperSize == 80) { { Icon(Icons.Default.Check, null) } } else null)
            }
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { Text("Pilih Printer Bluetooth:", fontWeight = FontWeight.Bold); IconButton(onClick = { pairedDevices = viewModel.getPairedDevices(); Toast.makeText(context, "Daftar disegarkan", Toast.LENGTH_SHORT).show() }) { Icon(Icons.Default.Refresh, "Refresh") } }
            if (!hasPermission) { Button(onClick = { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)) }) { Text("Izinkan Akses Bluetooth") } }
            else { LazyColumn(modifier = Modifier.weight(1f)) { if (pairedDevices.isEmpty()) item { Text("Tidak ada perangkat yang dipasangkan.", textAlign = TextAlign.Center, color = Color.Gray, modifier = Modifier.fillMaxWidth().padding(20.dp)) } else items(pairedDevices) { device -> val isSelected = device.address == currentPrinter; Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.savePrinter(device.address); Toast.makeText(context, "Printer Dipilih", Toast.LENGTH_SHORT).show() }, colors = CardDefaults.cardColors(containerColor = if(isSelected) Color(0xFFC8E6C9) else Color.White), border = if(isSelected) BorderStroke(2.dp, Color(0xFF2E7D32)) else null) { Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Print, null, tint = if(isSelected) Color(0xFF2E7D32) else Color.Gray); Spacer(modifier = Modifier.width(16.dp)); Column { if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) { Text(device.name ?: "Unknown Device", fontWeight = FontWeight.Bold); Text(device.address, fontSize = 12.sp, color = Color.Gray) } else Text("Perangkat") }; if (isSelected) { Spacer(modifier = Modifier.weight(1f)); Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32)) } } } } } }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.testPrint(context) }, modifier = Modifier.fillMaxWidth().height(50.dp), enabled = currentPrinter.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))) { Icon(Icons.Default.Print, null); Spacer(modifier = Modifier.width(8.dp)); Text("Test Print Struk Sampel") }
        }
    }
}

@Composable
fun BackupSettingsContent(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val lastBackup by viewModel.lastBackupTime.collectAsState()
    val status by viewModel.backupStatus.collectAsState()

    LaunchedEffect(status) {
        when (status) {
            is BackupStatus.Success -> {
                val msg = (status as BackupStatus.Success).message
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                if (msg.contains("Restart")) restartApp(context) else viewModel.resetBackupStatus()
            }
            is BackupStatus.Error -> { Toast.makeText(context, (status as BackupStatus.Error).message, Toast.LENGTH_LONG).show(); viewModel.resetBackupStatus() }
            else -> {}
        }
    }

    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
    val defaultFileName = "Backup_Toko_$timeStamp.zip"
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri -> if (uri != null) viewModel.backupData(uri) }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) viewModel.restoreData(uri) }

    Scaffold(topBar = { SettingsHeader("Backup & Restore", onBack) }) { p ->
        Box(modifier = Modifier.padding(p).fillMaxSize()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(20.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CloudUpload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)); Spacer(Modifier.width(16.dp)); Column { Text("Status Backup", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text("Terakhir: $lastBackup", fontSize = 12.sp, color = Color.Gray) } }; Spacer(Modifier.height(16.dp)); Text("Backup menyimpan database & foto produk ke file ZIP.", fontSize = 12.sp, color = Color(0xFF455A64)) } }
                Text("Amankan Data", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                Card(modifier = Modifier.fillMaxWidth().clickable { if(status is BackupStatus.Idle) backupLauncher.launch(defaultFileName) }, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), border = BorderStroke(1.dp, Color(0xFFE0E0E0))) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(48.dp).background(Color(0xFFE8F5E9), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Save, null, tint = Color(0xFF2E7D32)) }; Spacer(Modifier.width(16.dp)); Column { Text("Buat Backup Baru", fontWeight = FontWeight.Bold); Text("Simpan data ke file .zip", fontSize = 12.sp, color = Color.Gray) }; Spacer(Modifier.weight(1f)); Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray) } }
                Spacer(Modifier.height(24.dp))
                Text("Pulihkan Data", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                Card(modifier = Modifier.fillMaxWidth().clickable { if(status is BackupStatus.Idle) restoreLauncher.launch(arrayOf("application/zip")) }, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), border = BorderStroke(1.dp, Color(0xFFE0E0E0))) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(48.dp).background(Color(0xFFFFF3E0), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Restore, null, tint = Color(0xFFE65100)) }; Spacer(Modifier.width(16.dp)); Column { Text("Restore dari File", fontWeight = FontWeight.Bold); Text("Timpa data saat ini", fontSize = 12.sp, color = Color.Gray) }; Spacer(Modifier.weight(1f)); Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray) } }
            }
            if (status is BackupStatus.Loading) {
                val loadingState = status as BackupStatus.Loading
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(enabled = false){}, contentAlignment = Alignment.Center) { Card(modifier = Modifier.width(280.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("Memproses Data...", fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(Modifier.height(24.dp)); Box(contentAlignment = Alignment.Center) { CircularProgressIndicator(progress = { loadingState.progress / 100f }, modifier = Modifier.size(80.dp), strokeWidth = 6.dp, trackColor = Color(0xFFEEEEEE)); Text("${loadingState.progress}%", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary) }; Spacer(Modifier.height(16.dp)); Text(loadingState.message, fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center) } } }
            }
        }
    }
}

@Composable
fun CustomNumericKeypad(onNumberClick: (String) -> Unit, onDeleteClick: () -> Unit) {
    val keys = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "del"))
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        keys.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { key ->
                    if (key.isEmpty()) Spacer(modifier = Modifier.size(72.dp))
                    else if (key == "del") IconButton(onClick = onDeleteClick, modifier = Modifier.size(72.dp)) { Icon(Icons.AutoMirrored.Filled.Backspace, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurface) }
                    else Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable { onNumberClick(key) }, contentAlignment = Alignment.Center) { Text(key, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

@Composable
fun AboutSectionContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val appVersion = "1.0.0 (Stable)"
    val developerName = "Dicky Muhammad Yahya"
    val developerEmail = "dickymyahya@gmail.com"
    val developerWa = "6281256977124"

    fun openUrl(url: String) {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (e: Exception) { Toast.makeText(context, "Tidak ada browser", Toast.LENGTH_SHORT).show() }
    }

    fun openEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$developerEmail")
            putExtra(Intent.EXTRA_SUBJECT, "Support Toko Arkhan")
        }
        try { context.startActivity(intent) }
        catch (e: Exception) { Toast.makeText(context, "Tidak ada aplikasi email", Toast.LENGTH_SHORT).show() }
    }

    fun openWhatsApp() {
        val url = "https://api.whatsapp.com/send?phone=$developerWa&text=Halo%20Developer"
        openUrl(url)
    }

    fun shareApp() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Aplikasi Toko Arkhan")
            putExtra(Intent.EXTRA_TEXT, "Coba aplikasi Toko Arkhan!")
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan"))
    }

    Scaffold(topBar = { SettingsHeader("Tentang Aplikasi", onBack) }) { p ->
        Column(
            modifier = Modifier
                .padding(p)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()), // Scrollable sekarang aman
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ... (Isi konten About sama seperti sebelumnya) ...
            // Header Logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Store, null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Toko Arkhan", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                    Text("Versi $appVersion", fontSize = 14.sp, color = Color.Gray)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Deskripsi
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Tentang", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Aplikasi Point of Sales (POS) modern untuk toko sembako Anda.", fontSize = 13.sp, color = Color.DarkGray, lineHeight = 20.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Kontak
            Text("Hubungi Pengembang", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp).align(Alignment.Start))
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                Column {
                    ListItem(headlineContent = { Text("Developer") }, supportingContent = { Text(developerName) }, leadingContent = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) })
                    HorizontalDivider(color = Color.LightGray.copy(0.3f))
                    ListItem(headlineContent = { Text("WhatsApp Support") }, leadingContent = { Icon(Icons.Default.Chat, null, tint = Color(0xFF25D366)) }, modifier = Modifier.clickable { openWhatsApp() })
                    HorizontalDivider(color = Color.LightGray.copy(0.3f))
                    ListItem(headlineContent = { Text("Email") }, leadingContent = { Icon(Icons.Default.Email, null, tint = Color.Blue) }, modifier = Modifier.clickable { openEmail() })
                }
            }

            Spacer(Modifier.height(24.dp))

            // Share Button
            OutlinedButton(onClick = { shareApp() }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Bagikan App")
            }

            Spacer(Modifier.height(32.dp))
            Text("© 2024 Toko Arkhan", fontSize = 11.sp, color = Color.Gray)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun ReceiptSettingsContent(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val profile by viewModel.storeProfile.collectAsState()
    val context = LocalContext.current
    var name by remember { mutableStateOf(profile.name) }
    var address by remember { mutableStateOf(profile.address) }
    var phone by remember { mutableStateOf(profile.phone) }
    var footer by remember { mutableStateOf(profile.footer) }

    Scaffold(topBar = { SettingsHeader("Pengaturan Struk", onBack) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Toko") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("No. HP") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = footer, onValueChange = { footer = it }, label = { Text("Footer") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Button(onClick = { viewModel.saveStoreProfile(name, address, phone, footer); Toast.makeText(context, "Disimpan", Toast.LENGTH_SHORT).show(); onBack() }, modifier = Modifier.fillMaxWidth()) { Text("Simpan") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHeader(title: String, onBack: () -> Unit) {
    TopAppBar(title = { Text(title) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") } })
}