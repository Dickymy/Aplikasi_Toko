package com.example.aplikasitokosembakoarkhan

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aplikasitokosembakoarkhan.data.Product
import com.example.aplikasitokosembakoarkhan.data.repository.BackupRepository
import com.example.aplikasitokosembakoarkhan.utils.PrinterHelper
import com.example.aplikasitokosembakoarkhan.utils.SecurityHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    // State Printer Terpilih
    private val _selectedPrinter = MutableStateFlow(prefs.printerAddress)
    val selectedPrinter = _selectedPrinter.asStateFlow()

    fun saveStoreProfile(name: String, address: String, phone: String, footer: String) {
        prefs.storeName = name; prefs.storeAddress = address; prefs.storePhone = phone; prefs.receiptFooter = footer
        _storeProfile.value = StoreProfile(name, address, phone, footer)
    }

    // --- LOGIKA PRINTER ---

    fun savePrinter(address: String) {
        prefs.printerAddress = address
        _selectedPrinter.value = address
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
        // Data Dummy untuk Tes (PERBAIKAN: Menambahkan barcode="")
        val dummyCart = mapOf(
            Product(name = "Tes Barang 1", barcode = "001", sellPrice = 10000.0, stock = 10, buyPrice = 5000.0, category = "Umum", unit = "Pcs") to 1,
            Product(name = "Tes Barang 2", barcode = "002", sellPrice = 5000.0, stock = 10, buyPrice = 2500.0, category = "Umum", unit = "Pcs") to 2
        )

        PrinterHelper.printReceipt(
            context = context,
            cart = dummyCart,
            totalPrice = 20000.0,
            payAmount = 50000.0,
            change = 30000.0,
            paymentMethod = "Tunai (Tes)"
        )
    }

    // --- KEAMANAN ---
    fun isPinSet(): Boolean = SecurityHelper.isPinSet(app)
    fun setPin(newPin: String) = SecurityHelper.setPin(app, newPin)
    fun removePin() = SecurityHelper.removePin(app)

    fun updateLockedMenus(newSet: Set<String>) {
        prefs.lockedMenus = newSet
        _lockedMenus.value = newSet
    }

    // --- BACKUP RESTORE ---
    fun backupData(uri: Uri, onSuccess: ()->Unit, onError: (String)->Unit) = viewModelScope.launch {
        val res = backupRepository.backupData(uri)
        if(res.isSuccess) onSuccess() else onError(res.exceptionOrNull()?.message ?: "Error")
    }
    fun restoreData(uri: Uri, onSuccess: ()->Unit, onError: (String)->Unit) = viewModelScope.launch {
        val res = backupRepository.restoreData(uri)
        if(res.isSuccess) onSuccess() else onError(res.exceptionOrNull()?.message ?: "Error")
    }
}