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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aplikasitokosembakoarkhan.utils.SecurityHelper

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    var currentSection by remember { mutableStateOf("menu") }

    BackHandler(enabled = currentSection != "menu") {
        currentSection = "menu"
    }

    when (currentSection) {
        "menu" -> SettingsMenuContent(onNavigate = { section -> currentSection = section })
        "receipt" -> ReceiptSettingsContent(viewModel) { currentSection = "menu" }
        "printer" -> PrinterSettingsContent(viewModel) { currentSection = "menu" } // HALAMAN BARU
        "backup" -> BackupSettingsContent(viewModel) { currentSection = "menu" }
        "security" -> SecuritySettingsContent(viewModel) { currentSection = "menu" }
        "about" -> AboutSectionContent(onBack = { currentSection = "menu" })
    }
}

// --- HELPER FUNGSI RESTART APLIKASI ---
fun restartApp(context: Context) {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
    val componentName = intent?.component
    val mainIntent = Intent.makeRestartActivityTask(componentName)
    context.startActivity(mainIntent)
    Runtime.getRuntime().exit(0)
}

@Composable
fun SettingsMenuContent(onNavigate: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Pengaturan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        SettingsItemRow("Koneksi Printer", "Sambungkan printer thermal bluetooth", Icons.Default.Print) { onNavigate("printer") } // MENU BARU
        SettingsItemRow("Identitas Toko & Struk", "Atur nama toko, alamat, dan footer", Icons.AutoMirrored.Filled.ReceiptLong) { onNavigate("receipt") }
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

// --- HALAMAN PENGATURAN PRINTER (BARU) ---
@Composable
fun PrinterSettingsContent(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val currentPrinter by viewModel.selectedPrinter.collectAsState()
    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(false) }

    // Launcher Izin Bluetooth
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        hasPermission = granted
        if (granted) {
            pairedDevices = viewModel.getPairedDevices()
        } else {
            Toast.makeText(context, "Izin Bluetooth Diperlukan", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        // Cek Izin saat dibuka
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                hasPermission = true
                pairedDevices = viewModel.getPairedDevices()
            } else {
                permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN))
            }
        } else {
            hasPermission = true
            pairedDevices = viewModel.getPairedDevices()
        }
    }

    Scaffold(topBar = { SettingsHeader("Koneksi Printer", onBack) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {

            // Info Header
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cara Menghubungkan:", fontWeight = FontWeight.Bold)
                    Text("1. Hidupkan Printer Thermal Bluetooth.", fontSize = 12.sp)
                    Text("2. Buka Pengaturan Bluetooth HP -> Pasangkan (Pair) dengan Printer.", fontSize = 12.sp)
                    Text("3. Kembali ke sini, lalu pilih nama printer dari daftar di bawah.", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!hasPermission) {
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN))
                    }
                }) { Text("Izinkan Akses Bluetooth") }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Perangkat Terpasang:", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { pairedDevices = viewModel.getPairedDevices(); Toast.makeText(context, "Daftar disegarkan", Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }

                LazyColumn {
                    if (pairedDevices.isEmpty()) {
                        item { Text("Tidak ada perangkat bluetooth yang terpasang.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp)) }
                    } else {
                        items(pairedDevices) { device ->
                            val isSelected = device.address == currentPrinter
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.savePrinter(device.address)
                                        Toast.makeText(context, "Printer Dipilih: ${device.name}", Toast.LENGTH_SHORT).show()
                                    },
                                colors = CardDefaults.cardColors(containerColor = if(isSelected) Color(0xFFC8E6C9) else Color.White),
                                border = if(isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Print, null, tint = if(isSelected) Color(0xFF2E7D32) else Color.Gray)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        // PENTING: Permission check sebelum akses .name
                                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                            Text(device.name ?: "Unknown Device", fontWeight = FontWeight.Bold)
                                            Text(device.address, fontSize = 12.sp, color = Color.Gray)
                                        } else {
                                            Text("Perangkat (Izin Ditolak)")
                                        }
                                    }
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // TOMBOL TEST PRINT
                Button(
                    onClick = { viewModel.testPrint(context) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = currentPrinter.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Icon(Icons.Default.Print, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tes Cetak Struk")
                }
            }
        }
    }
}

// --- BAGIAN LAIN TETAP SAMA (SECURITY, RECEIPT, DLL) ---
// (Pastikan Anda tetap menyertakan fungsi SecuritySettingsContent, ReceiptSettingsContent, dll yang ada di file sebelumnya)

@Composable
fun SecuritySettingsContent(viewModel: SettingsViewModel, onBack: () -> Unit) {
    var isPinSet by remember { mutableStateOf(viewModel.isPinSet()) }
    var pinInput by remember { mutableStateOf("") }

    val originalLockedMenus by viewModel.lockedMenus.collectAsState()
    val tempLockedMenus = remember { mutableStateListOf<String>() }

    var showRestartDialog by remember { mutableStateOf(false) }
    var showPinCreateDialog by remember { mutableStateOf(false) }
    var showPinDeleteDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(originalLockedMenus) {
        tempLockedMenus.clear()
        tempLockedMenus.addAll(originalLockedMenus)
        if (!tempLockedMenus.contains("settings")) {
            tempLockedMenus.add("settings")
        }
    }

    val menuOptions = remember {
        listOf(
            "sales" to "Kasir",
            "restock" to "Restok Barang",
            "debt" to "Hutang / Kasbon",
            "expense" to "Pengeluaran",
            "products" to "Data Barang",
            "customers" to "Data Pelanggan",
            "categories" to "Kategori",
            "units" to "Satuan",
            "report" to "Laporan Keuangan",
            "settings" to "Pengaturan"
        )
    }

    Scaffold(topBar = { SettingsHeader("Keamanan (PIN)", onBack) }) { p ->
        LazyColumn(modifier = Modifier.padding(p).padding(16.dp)) {
            item {
                if (isPinSet) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Aplikasi dilindungi PIN", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showPinDeleteDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red), modifier = Modifier.fillMaxWidth()
                    ) { Text("Hapus PIN") }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    Text("Pilih Menu yang Dikunci:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Centang menu, lalu tekan tombol Simpan di bawah.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Text("Pasang PIN Baru (6 Digit):")
                    OutlinedTextField(
                        value = pinInput, onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pinInput = it },
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { if (pinInput.length >= 4) showPinCreateDialog = true else Toast.makeText(context, "Minimal 4 digit", Toast.LENGTH_SHORT).show() },
                        modifier = Modifier.fillMaxWidth(), enabled = pinInput.length >= 4
                    ) { Text("Simpan PIN") }
                }
            }

            if (isPinSet) {
                items(menuOptions) { (route, name) ->
                    val isMandatory = route == "settings"
                    val isChecked = if (isMandatory) true else tempLockedMenus.contains(route)

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !isMandatory) { if (isChecked) tempLockedMenus.remove(route) else tempLockedMenus.add(route) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isChecked, onCheckedChange = { checked -> if (!isMandatory) { if (checked) tempLockedMenus.add(route) else tempLockedMenus.remove(route) } }, enabled = !isMandatory)
                        Column {
                            Text(name, fontWeight = if(isMandatory) FontWeight.Bold else FontWeight.Normal)
                            if (isMandatory) Text("(Wajib dikunci)", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val finalSet = tempLockedMenus.toSet() + "settings"
                            viewModel.updateLockedMenus(finalSet)
                            showRestartDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Save, null); Spacer(modifier = Modifier.width(8.dp)); Text("SIMPAN & TERAPKAN", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Pengaturan Disimpan") },
            text = { Text("Aplikasi akan dimuat ulang untuk menerapkan perubahan keamanan.\n\nKlik OK untuk melanjutkan.") },
            confirmButton = { Button(onClick = { restartApp(context) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("OK, Restart Sekarang") } }
        )
    }

    if (showPinCreateDialog) {
        AlertDialog(
            onDismissRequest = { showPinCreateDialog = false },
            title = { Text("Aktifkan Keamanan PIN?") },
            text = { Text("Setelah PIN aktif, Anda dapat mengunci halaman penting.\n\nAplikasi akan di-restart untuk mengaktifkan sistem keamanan ini.") },
            confirmButton = { Button(onClick = { viewModel.setPin(pinInput); restartApp(context) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("Ya, Simpan & Restart") } },
            dismissButton = { TextButton(onClick = { showPinCreateDialog = false }) { Text("Batal") } }
        )
    }

    if (showPinDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showPinDeleteDialog = false },
            title = { Text("Hapus PIN Keamanan?") },
            text = { Text("PERINGATAN: Menghapus PIN akan membuka semua kunci halaman.\n\nAplikasi akan di-restart setelah PIN dihapus.") },
            confirmButton = { Button(onClick = { viewModel.removePin(); restartApp(context) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Ya, Hapus & Restart") } },
            dismissButton = { TextButton(onClick = { showPinDeleteDialog = false }) { Text("Batal") } }
        )
    }
}

@Composable
fun AboutSectionContent(onBack: () -> Unit) {
    Scaffold(topBar = { SettingsHeader("Tentang Aplikasi", onBack) }) { p ->
        Column(
            modifier = Modifier.padding(p).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(100.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Store, null, modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Aplikasi Toko Sembako Arkhan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Versi 1.0", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(32.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Dikembangkan oleh:", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Dicky Muhammad Yahya", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("© 2024 Hak Cipta Dilindungi", fontSize = 10.sp, color = Color.LightGray)
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
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Toko") }, modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat Toko") }, modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("No. HP") }, modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = footer, onValueChange = { footer = it }, label = { Text("Footer Struk") }, modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { viewModel.saveStoreProfile(name, address, phone, footer); Toast.makeText(context, "Disimpan!", Toast.LENGTH_SHORT).show(); onBack() }, modifier = Modifier.fillMaxWidth()) { Text("SIMPAN") }
        }
    }
}

@Composable
fun BackupSettingsContent(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri -> if (uri != null) { isLoading = true; viewModel.backupData(uri, { isLoading = false; Toast.makeText(context, "Sukses!", Toast.LENGTH_SHORT).show() }, { isLoading = false; Toast.makeText(context, "Gagal: $it", Toast.LENGTH_LONG).show() }) } }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) { isLoading = true; viewModel.restoreData(uri, { isLoading = false; Toast.makeText(context, "Sukses! Restart App.", Toast.LENGTH_LONG).show() }, { isLoading = false; Toast.makeText(context, "Gagal: $it", Toast.LENGTH_LONG).show() }) } }

    Scaffold(topBar = { SettingsHeader("Backup & Restore", onBack) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (isLoading) CircularProgressIndicator() else {
                Button(onClick = { backupLauncher.launch("Backup_${System.currentTimeMillis()}.zip") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Upload, null); Spacer(Modifier.width(8.dp)); Text("Backup Data") }
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/zip")) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text("Restore Data") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHeader(title: String, onBack: () -> Unit) {
    TopAppBar(title = { Text(title) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") } })
}