package com.example.aplikasitokosembakoarkhan

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aplikasitokosembakoarkhan.utils.SecurityHelper // Pastikan sudah buat file ini
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ProductViewModel = viewModel()) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }

    // State Info Toko
    var name by remember { mutableStateOf(prefs.storeName) }
    var address by remember { mutableStateOf(prefs.storeAddress) }
    var phone by remember { mutableStateOf(prefs.storePhone) }

    // State Printer
    var selectedPrinterAddress by remember { mutableStateOf(prefs.printerAddress) }
    var pairedDevices by remember { mutableStateOf(listOf<BluetoothDeviceModel>()) }

    // State Keamanan (PIN) - FITUR BARU
    var showPinDialog by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }
    var isPinActive by remember { mutableStateOf(SecurityHelper.isPinSet(context)) }

    // --- LAUNCHER BACKUP & RESTORE ---
    val createDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-sqlite3")) { uri ->
        uri?.let {
            viewModel.backupDatabase(context, it,
                onSuccess = { Toast.makeText(context, "Backup Berhasil Disimpan!", Toast.LENGTH_LONG).show() },
                onError = { msg -> Toast.makeText(context, "Gagal: $msg", Toast.LENGTH_LONG).show() }
            )
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            viewModel.restoreDatabase(context, it,
                onSuccess = {
                    Toast.makeText(context, "Restore Berhasil! Aplikasi akan restart...", Toast.LENGTH_LONG).show()
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    context.startActivity(intent)
                    exitProcess(0)
                },
                onError = { msg -> Toast.makeText(context, "Gagal: $msg", Toast.LENGTH_LONG).show() }
            )
        }
    }

    // Permission Launcher (Android 12+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    fun loadPairedDevices() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter

        if (adapter == null) {
            Toast.makeText(context, "Bluetooth tidak didukung", Toast.LENGTH_SHORT).show()
            return
        }

        if (!adapter.isEnabled) {
            Toast.makeText(context, "Aktifkan Bluetooth dulu!", Toast.LENGTH_SHORT).show()
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            context.startActivity(intent)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN))
                return
            }
        }

        val devices = adapter.bondedDevices
        pairedDevices = devices.map { BluetoothDeviceModel(it.name, it.address) }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN))
        }
        loadPairedDevices()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Pengaturan Toko", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // --- BAGIAN 1: INFO TOKO ---
        Text("Info Struk", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Toko") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat Toko") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("No. HP / Footer") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                prefs.storeName = name
                prefs.storeAddress = address
                prefs.storePhone = phone
                Toast.makeText(context, "Info Toko Disimpan!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.align(Alignment.End)
        ) { Icon(Icons.Default.Save, null); Spacer(modifier = Modifier.width(8.dp)); Text("Simpan") }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // --- BAGIAN 2: KEAMANAN PIN (FITUR BARU) ---
        Text("Keamanan Aplikasi", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Text("Kunci menu Laporan & Stok dengan PIN.", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isPinActive) Icons.Default.Lock else Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isPinActive) "PIN Aktif" else "PIN Belum Diatur", fontWeight = FontWeight.Bold)
                }

                if (isPinActive) {
                    Button(
                        onClick = {
                            SecurityHelper.removePin(context)
                            isPinActive = false
                            Toast.makeText(context, "PIN Dihapus", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) { Text("Hapus PIN") }
                } else {
                    Button(onClick = { showPinDialog = true }) { Text("Buat PIN") }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // --- BAGIAN 3: BACKUP & RESTORE ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudUpload, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Backup & Restore Data", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
        Text("Simpan data agar aman saat ganti HP.", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Tombol Backup
            Button(
                onClick = {
                    val fileName = "Backup_Toko_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.db"
                    createDocumentLauncher.launch(fileName)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.CloudUpload, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Backup")
            }

            // Tombol Restore
            Button(
                onClick = { openDocumentLauncher.launch(arrayOf("application/*")) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) {
                Icon(Icons.Default.CloudDownload, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restore")
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp)) {
                Icon(Icons.Default.Warning, null, tint = Color(0xFFF57C00), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Peringatan: Restore akan menimpa/menghapus data yang ada sekarang dengan data dari file backup.", fontSize = 11.sp, color = Color(0xFFE65100))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // --- BAGIAN 4: PRINTER ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Print, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Printer Bluetooth", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            context.startActivity(intent)
        }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray), modifier = Modifier.fillMaxWidth()) {
            Text("Buka Pengaturan Bluetooth HP")
        }

        OutlinedButton(onClick = { loadPairedDevices() }, modifier = Modifier.fillMaxWidth()) {
            Text("Refresh Daftar Perangkat")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // List Printer
        pairedDevices.forEach { device ->
            val isSelected = device.address == selectedPrinterAddress
            Card(
                colors = CardDefaults.cardColors(containerColor = if(isSelected) Color(0xFFE3F2FD) else Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                    selectedPrinterAddress = device.address
                    prefs.printerAddress = device.address
                    Toast.makeText(context, "Printer Dipilih!", Toast.LENGTH_SHORT).show()
                }
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bluetooth, null, tint = if(isSelected) Color.Blue else Color.Gray)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(device.name ?: "Unknown", fontWeight = FontWeight.Bold)
                        Text(device.address, fontSize = 12.sp, color = Color.Gray)
                    }
                    if (isSelected) Icon(Icons.Default.Check, null, tint = Color.Blue)
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp)) // Padding bawah
    }

    // --- DIALOG BUAT PIN (BARU) ---
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Buat PIN Baru") },
            text = {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
                    label = { Text("Masukkan 4-6 Angka") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newPin.length >= 4) {
                        SecurityHelper.setPin(context, newPin)
                        isPinActive = true
                        showPinDialog = false
                        newPin = ""
                        Toast.makeText(context, "PIN Diaktifkan!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Minimal 4 angka", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("Batal") } }
        )
    }
}

data class BluetoothDeviceModel(val name: String?, val address: String)