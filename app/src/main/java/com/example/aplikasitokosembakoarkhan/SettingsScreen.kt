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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aplikasitokosembakoarkhan.utils.SecurityHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ProductViewModel = viewModel(),
    onNavigateToReceipt: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToSecurity: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Pengaturan", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            item { SettingsItem("Pengaturan Struk", "Edit nama toko dan alamat", Icons.Default.Receipt, onNavigateToReceipt) }
            item { SettingsItem("Backup & Restore", "Amankan data ke file ZIP", Icons.Default.CloudUpload, onNavigateToBackup) }
            item { SettingsItem("Keamanan", "Atur PIN dan kunci aplikasi", Icons.Default.Security, onNavigateToSecurity) }
        }

        Spacer(modifier = Modifier.weight(1f))
        Text("Versi Aplikasi 1.0.0", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() }, elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}

@Composable
fun ReceiptSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    var storeName by remember { mutableStateOf(prefs.storeName) }
    var storeAddress by remember { mutableStateOf(prefs.storeAddress) }
    var storePhone by remember { mutableStateOf(prefs.storePhone) }
    var footer by remember { mutableStateOf(prefs.receiptFooter) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Kembali") }; Text("Edit Struk", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = storeName, onValueChange = { storeName = it }, label = { Text("Nama Toko") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = storeAddress, onValueChange = { storeAddress = it }, label = { Text("Alamat Toko") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = storePhone, onValueChange = { storePhone = it }, label = { Text("No. HP") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = footer, onValueChange = { footer = it }, label = { Text("Footer Struk") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { prefs.storeName=storeName; prefs.storeAddress=storeAddress; prefs.storePhone=storePhone; prefs.receiptFooter=footer; Toast.makeText(context, "Disimpan", Toast.LENGTH_SHORT).show(); onBack() }, modifier = Modifier.fillMaxWidth()) { Text("Simpan") }
    }
}

@Composable
fun BackupSettingsScreen(viewModel: ProductViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var showRestoreWarning by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) viewModel.backupData(context, uri, { Toast.makeText(context, "Backup Berhasil!", Toast.LENGTH_SHORT).show() }, { Toast.makeText(context, "Gagal: $it", Toast.LENGTH_SHORT).show() })
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.restoreData(context, uri, {
            Toast.makeText(context, "Restore Sukses! Restart Aplikasi...", Toast.LENGTH_LONG).show()
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)
            exitProcess(0)
        }, { Toast.makeText(context, "Gagal: $it", Toast.LENGTH_SHORT).show() })
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Kembali") }; Text("Backup & Restore", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Backup Data", fontWeight = FontWeight.Bold); Text("Simpan Database & Gambar (ZIP).", fontSize = 12.sp)
                Button(onClick = { backupLauncher.launch("Backup_Toko_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.zip") }) { Text("Backup Sekarang") }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Restore Data", fontWeight = FontWeight.Bold); Text("Kembalikan data dari file backup.", fontSize = 12.sp)
                Button(onClick = { showRestoreWarning = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Pilih File Backup") }
            }
        }
    }

    if (showRestoreWarning) {
        AlertDialog(onDismissRequest = { showRestoreWarning = false }, icon = { Icon(Icons.Default.Warning, null, tint = Color.Red) }, title = { Text("Peringatan Keras!") }, text = { Text("Yakin ingin restore? Data saat ini akan TIMPA dan HILANG selamanya.") }, confirmButton = { Button(onClick = { showRestoreWarning = false; restoreLauncher.launch(arrayOf("application/zip")) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Ya, Timpa Data") } }, dismissButton = { TextButton(onClick = { showRestoreWarning = false }) { Text("Batal") } })
    }
}

@Composable
fun SecuritySettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var isPinSet by remember { mutableStateOf(SecurityHelper.isPinSet(context)) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Kembali") }; Text("Keamanan", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("PIN Pengaman", fontWeight = FontWeight.Bold); Text(if (isPinSet) "Aktif" else "Nonaktif", fontSize = 12.sp, color = if(isPinSet) Color(0xFF2E7D32) else Color.Red) }
                Button(onClick = { showSetPinDialog = true }) { Text(if(isPinSet) "Ubah" else "Buat") }
            }
        }
        if (isPinSet) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = { SecurityHelper.removePin(context); isPinSet = false; Toast.makeText(context, "PIN Dihapus", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)) { Text("Hapus PIN") }
        }
    }

    if (showSetPinDialog) {
        AlertDialog(onDismissRequest = { showSetPinDialog = false }, title = { Text("Set PIN (6 Angka)") }, text = { OutlinedTextField(value = newPin, onValueChange = { if(it.length<=6 && it.all{c->c.isDigit()}) newPin=it }, label = { Text("PIN") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)) }, confirmButton = { Button(onClick = { if(newPin.length>=4) { SecurityHelper.setPin(context, newPin); isPinSet=true; showSetPinDialog=false; Toast.makeText(context, "Disimpan", Toast.LENGTH_SHORT).show() } }) { Text("Simpan") } }, dismissButton = { TextButton(onClick = { showSetPinDialog = false }) { Text("Batal") } })
    }
}