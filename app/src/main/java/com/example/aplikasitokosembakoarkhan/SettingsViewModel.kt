package com.example.aplikasitokosembakoarkhan

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aplikasitokosembakoarkhan.data.Product
import com.example.aplikasitokosembakoarkhan.data.repository.BackupRepository
import com.example.aplikasitokosembakoarkhan.data.repository.BackupStatus
import com.example.aplikasitokosembakoarkhan.utils.PrinterHelper
import com.example.aplikasitokosembakoarkhan.utils.SecurityHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StoreProfile(
    val name: String, val address: String, val phone: String, val footer: String
)

class SettingsViewModel(
    private val app: Application,
    private val backupRepository: BackupRepository
) : AndroidViewModel(app) {

    private val prefs = AppPreferences(app)

    private val _storeProfile = MutableStateFlow(StoreProfile(prefs.storeName, prefs.storeAddress, prefs.storePhone, prefs.receiptFooter))
    val storeProfile = _storeProfile.asStateFlow()

    private val _lockedMenus = MutableStateFlow(prefs.lockedMenus)
    val lockedMenus = _lockedMenus.asStateFlow()

    private val _selectedPrinter = MutableStateFlow(prefs.printerAddress)
    val selectedPrinter = _selectedPrinter.asStateFlow()

    private val _paperSize = MutableStateFlow(prefs.printerPaperSize)
    val paperSize = _paperSize.asStateFlow()

    private val _lastBackupTime = MutableStateFlow(backupRepository.getLastBackupTime())
    val lastBackupTime = _lastBackupTime.asStateFlow()

    private val _backupStatus = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val backupStatus = _backupStatus.asStateFlow()

    fun saveStoreProfile(name: String, address: String, phone: String, footer: String) {
        prefs.storeName = name; prefs.storeAddress = address; prefs.storePhone = phone; prefs.receiptFooter = footer
        _storeProfile.value = StoreProfile(name, address, phone, footer)
    }

    // --- LOGIKA PRINTER ---
    fun savePrinter(address: String) { prefs.printerAddress = address; _selectedPrinter.value = address }
    fun setPaperSize(size: Int) { prefs.printerPaperSize = size; _paperSize.value = size }

    fun getPairedDevices(): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        return try {
            if (adapter != null && adapter.isEnabled) adapter.bondedDevices.toList() else emptyList()
        } catch (e: SecurityException) { emptyList() }
    }

    fun testPrint(context: Context) {
        val dummyCart = mapOf(
            Product(name = "Tes Barang", barcode = "TEST", sellPrice = 15000.0, stock = 10.0, buyPrice = 10000.0, category = "-", unit = "Pcs", wholesaleQty = 0.0, wholesalePrice = 0.0) to 1.0
        )
        PrinterHelper.printReceipt(context, dummyCart, 15000.0, 20000.0, 5000.0, "Test Print")
    }

    fun testConnection(context: Context, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val address = _selectedPrinter.value
            if (address.isEmpty()) { onError(); return@launch }
            val isConnected = withContext(Dispatchers.IO) { PrinterHelper.testConnection(context, address) }
            if (isConnected) onSuccess() else onError()
        }
    }

    // --- KEAMANAN & PIN (LOGIKA BARU DI SINI) ---

    fun isPinSet(): Boolean = SecurityHelper.isPinSet(app)

    fun setPin(newPin: String) {
        // Cek apakah sebelumnya PIN belum diset (Pertama kali)
        val isFirstTime = !SecurityHelper.isPinSet(app)

        // Simpan PIN baru
        SecurityHelper.setPin(app, newPin)

        // Jika ini pertama kali set PIN, otomatis kunci menu tertentu
        if (isFirstTime) {
            val defaultLocked = setOf(
                "categories",   // Kategori
                "units",        // Satuan
                "expense",      // Pengeluaran
                "debt",         // Hutang
                "settings"      // Pengaturan (Wajib)
            )
            updateLockedMenus(defaultLocked)
        }
    }

    fun removePin() {
        SecurityHelper.removePin(app)
        // Opsional: Buka semua kunci saat PIN dihapus
        updateLockedMenus(emptySet())
    }

    fun updateLockedMenus(newSet: Set<String>) {
        prefs.lockedMenus = newSet
        _lockedMenus.value = newSet
    }

    // --- BACKUP & RESTORE ---
    fun backupData(uri: Uri) = viewModelScope.launch {
        backupRepository.backupDataFlow(uri).collect { status ->
            _backupStatus.value = status
            if (status is BackupStatus.Success) _lastBackupTime.value = backupRepository.getLastBackupTime()
        }
    }

    fun restoreData(uri: Uri) = viewModelScope.launch {
        backupRepository.restoreDataFlow(uri).collect { status -> _backupStatus.value = status }
    }

    fun resetBackupStatus() { _backupStatus.value = BackupStatus.Idle }
}