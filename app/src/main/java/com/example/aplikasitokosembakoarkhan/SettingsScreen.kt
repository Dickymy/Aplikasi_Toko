package com.example.aplikasitokosembakoarkhan

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    var currentSection by remember { mutableStateOf("menu") }

    BackHandler(enabled = currentSection != "menu") {
        currentSection = "menu"
    }

    when (currentSection) {
        "menu" -> SettingsMenu(onNavigate = { section -> currentSection = section })
        "receipt" -> ReceiptSettings(viewModel) { currentSection = "menu" }
        "backup" -> BackupSettings(viewModel) { currentSection = "menu" }
        "security" -> SecuritySettings(viewModel) { currentSection = "menu" }
        "about" -> AboutSection(onBack = { currentSection = "menu" }) // Section Baru
    }
}

@Composable
fun SettingsMenu(onNavigate: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Pengaturan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        SettingsItem(
            title = "Identitas Toko & Struk",
            subtitle = "Atur nama toko, alamat, dan footer struk",
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            onClick = { onNavigate("receipt") }
        )
        SettingsItem(
            title = "Backup & Restore",
            subtitle = "Amankan data database & gambar",
            icon = Icons.Default.Backup,
            onClick = { onNavigate("backup") }
        )
        SettingsItem(
            title = "Keamanan (PIN)",
            subtitle = "Atur kunci masuk untuk menu admin",
            icon = Icons.Default.Lock,
            onClick = { onNavigate("security") }
        )
        SettingsItem(
            title = "Tentang Aplikasi",
            subtitle = "Informasi Pengembang",
            icon = Icons.Default.Info,
            onClick = { onNavigate("about") }
        )
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun AboutSection(onBack: () -> Unit) {
    Scaffold(topBar = { SettingsTopBar("Tentang Aplikasi", onBack) }) { p ->
        Column(
            modifier = Modifier.padding(p).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon Logo
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Store,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Aplikasi Toko Sembako Arkhan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Versi 1.0", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
fun ReceiptSettings(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val profile by viewModel.storeProfile.collectAsState()
    val context = LocalContext.current

    var name by remember { mutableStateOf(profile.name) }
    var address by remember { mutableStateOf(profile.address) }
    var phone by remember { mutableStateOf(profile.phone) }
    var footer by remember { mutableStateOf(profile.footer) }

    Scaffold(
        topBar = { SettingsTopBar("Pengaturan Struk", onBack) }
    ) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Toko") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat Toko") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("No. HP / Telp") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = footer, onValueChange = { footer = it }, label = { Text("Footer Struk (Pesan Bawah)") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.saveStoreProfile(name, address, phone, footer)
                    Toast.makeText(context, "Disimpan!", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("SIMPAN PERUBAHAN") }
        }
    }
}

@Composable
fun BackupSettings(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        if (uri != null) {
            isLoading = true
            viewModel.backupData(uri,
                onSuccess = { isLoading = false; Toast.makeText(context, "Backup Berhasil!", Toast.LENGTH_SHORT).show() },
                onError = { isLoading = false; Toast.makeText(context, "Gagal: $it", Toast.LENGTH_LONG).show() }
            )
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            isLoading = true
            viewModel.restoreData(uri,
                onSuccess = { isLoading = false; Toast.makeText(context, "Restore Berhasil! Restart Aplikasi.", Toast.LENGTH_LONG).show() },
                onError = { isLoading = false; Toast.makeText(context, "Gagal: $it", Toast.LENGTH_LONG).show() }
            )
        }
    }

    Scaffold(topBar = { SettingsTopBar("Backup & Restore", onBack) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (isLoading) CircularProgressIndicator() else {
                Button(onClick = { backupLauncher.launch("Backup_Toko_${System.currentTimeMillis()}.zip") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Upload, null); Spacer(modifier = Modifier.width(8.dp)); Text("Backup Data")
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/zip")) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)) {
                    Icon(Icons.Default.Download, null); Spacer(modifier = Modifier.width(8.dp)); Text("Restore Data")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Restore akan menimpa data lama!", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SecuritySettings(viewModel: SettingsViewModel, onBack: () -> Unit) {
    var isPinSet by remember { mutableStateOf(viewModel.isPinSet()) }
    var pinInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(topBar = { SettingsTopBar("Keamanan (PIN)", onBack) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            if (isPinSet) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Lock, null, tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Aplikasi dilindungi PIN", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.removePin()
                        isPinSet = false
                        Toast.makeText(context, "PIN Dihapus", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Hapus PIN") }
            } else {
                Text("Pasang PIN Baru (6 Digit):")
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pinInput = it },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (pinInput.length >= 4) {
                            viewModel.setPin(pinInput)
                            isPinSet = true
                            pinInput = ""
                            Toast.makeText(context, "PIN Disimpan", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Minimal 4 digit", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = pinInput.length >= 4
                ) { Text("Simpan PIN") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") }
        }
    )
}