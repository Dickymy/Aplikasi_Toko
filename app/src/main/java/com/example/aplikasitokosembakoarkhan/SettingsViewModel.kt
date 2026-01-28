package com.example.aplikasitokosembakoarkhan

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aplikasitokosembakoarkhan.data.repository.BackupRepository
import com.example.aplikasitokosembakoarkhan.utils.SecurityHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Model Data Sederhana untuk Info Toko
data class StoreProfile(
    val name: String,
    val address: String,
    val phone: String,
    val footer: String
)

class SettingsViewModel(
    private val app: Application,
    private val backupRepository: BackupRepository
) : AndroidViewModel(app) {

    private val prefs = AppPreferences(app)

    // --- STATE INFO TOKO ---
    private val _storeProfile = MutableStateFlow(
        StoreProfile(prefs.storeName, prefs.storeAddress, prefs.storePhone, prefs.receiptFooter)
    )
    val storeProfile = _storeProfile.asStateFlow()

    fun saveStoreProfile(name: String, address: String, phone: String, footer: String) {
        prefs.storeName = name
        prefs.storeAddress = address
        prefs.storePhone = phone
        prefs.receiptFooter = footer

        // Update State
        _storeProfile.value = StoreProfile(name, address, phone, footer)
    }

    // --- KEAMANAN (PIN) ---
    fun isPinSet(): Boolean = SecurityHelper.isPinSet(app)

    fun setPin(newPin: String) {
        SecurityHelper.setPin(app, newPin)
    }

    fun removePin() {
        SecurityHelper.removePin(app)
    }

    fun checkPin(input: String): Boolean {
        return SecurityHelper.checkPin(app, input)
    }

    // --- BACKUP & RESTORE (Logika Lama) ---
    fun backupData(destUri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = backupRepository.backupData(destUri)
            if (result.isSuccess) onSuccess() else onError(result.exceptionOrNull()?.message ?: "Gagal Backup")
        }
    }

    fun restoreData(sourceUri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = backupRepository.restoreData(sourceUri)
            if (result.isSuccess) onSuccess() else onError(result.exceptionOrNull()?.message ?: "Gagal Restore")
        }
    }
}