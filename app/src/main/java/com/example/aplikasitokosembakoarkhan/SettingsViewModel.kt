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
import com.example.aplikasitokosembakoarkhan.data.repository.BackupStatus // Import ini penting
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

    // STATE UKURAN KERTAS
    private val _paperSize = MutableStateFlow(prefs.printerPaperSize)
    val paperSize = _paperSize.asStateFlow()

    // STATE BACKUP & RESTORE
    private val _lastBackupTime = MutableStateFlow(backupRepository.getLastBackupTime())
    val lastBackupTime = _lastBackupTime.asStateFlow()

    // State Status Backup (Menggantikan boolean loading lama)
    private val _backupStatus = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val backupStatus = _backupStatus.asStateFlow()

    fun saveStoreProfile(name: String, address: String, phone: String, footer: String) {
        prefs.storeName = name; prefs.storeAddress = address; prefs.storePhone = phone; prefs.receiptFooter = footer
        _storeProfile.value = StoreProfile(name, address, phone, footer)
    }

    // --- LOGIKA PRINTER BARU ---

    fun savePrinter(address: String) {
        prefs.printerAddress = address
        _selectedPrinter.value = address
    }

    fun setPaperSize(size: Int) {
        prefs.printerPaperSize = size
        _paperSize.value = size
    }

    fun getPairedDevices(): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        return try {
            if (adapter != null && adapter.isEnabled) {
                adapter.bondedDevices.toList()
            } else {
                emptyList()
            }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    fun testPrint(context: Context) {
        val dummyCart = mapOf(
            Product(name = "Tes Barang 58mm/80mm", barcode = "TEST", sellPrice = 15000.0, stock = 10.0, buyPrice = 10000.0, category = "-", unit = "Pcs", wholesaleQty = 0.0, wholesalePrice = 0.0) to 1.0,
            Product(name = "Kopi Hitam", barcode = "KOPI", sellPrice = 5000.0, stock = 10.0, buyPrice = 2500.0, category = "-", unit = "Cup", wholesaleQty = 0.0, wholesalePrice = 0.0) to 2.0
        )
        PrinterHelper.printReceipt(context, dummyCart, 25000.0, 50000.0, 25000.0, "Test Print")
    }

    // FUNGSI PING KONEKSI (Async)
    fun testConnection(context: Context, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val address = _selectedPrinter.value
            if (address.isEmpty()) { onError(); return@launch }

            val isConnected = withContext(Dispatchers.IO) {
                PrinterHelper.testConnection(context, address)
            }

            if (isConnected) onSuccess() else onError()
        }
    }

    // --- KEAMANAN & BACKUP (DISESUAIKAN DENGAN FLOW BARU) ---

    fun isPinSet(): Boolean = SecurityHelper.isPinSet(app)
    fun setPin(newPin: String) = SecurityHelper.setPin(app, newPin)
    fun removePin() = SecurityHelper.removePin(app)
    fun updateLockedMenus(newSet: Set<String>) { prefs.lockedMenus = newSet; _lockedMenus.value = newSet }

    // Fungsi Backup Menggunakan Flow (Untuk Progress Bar)
    fun backupData(uri: Uri) = viewModelScope.launch {
        // Mengumpulkan status (Loading 10%... 50%... Sukses)
        backupRepository.backupDataFlow(uri).collect { status ->
            _backupStatus.value = status

            // Jika sukses, update waktu backup terakhir
            if (status is BackupStatus.Success) {
                _lastBackupTime.value = backupRepository.getLastBackupTime()
            }
        }
    }

    // Fungsi Restore Menggunakan Flow
    fun restoreData(uri: Uri) = viewModelScope.launch {
        backupRepository.restoreDataFlow(uri).collect { status ->
            _backupStatus.value = status
        }
    }

    // Reset status agar loading hilang setelah selesai/error
    fun resetBackupStatus() {
        _backupStatus.value = BackupStatus.Idle
    }
}