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

    fun saveStoreProfile(name: String, address: String, phone: String, footer: String) {
        prefs.storeName = name; prefs.storeAddress = address; prefs.storePhone = phone; prefs.receiptFooter = footer
        _storeProfile.value = StoreProfile(name, address, phone, footer)
    }

    fun isPinSet(): Boolean = SecurityHelper.isPinSet(app)
    fun setPin(newPin: String) = SecurityHelper.setPin(app, newPin)
    fun removePin() = SecurityHelper.removePin(app)

    // --- PERBAIKAN: TOGGLE MENU LOCK ---
    fun toggleMenuLock(route: String, isLocked: Boolean) {
        // Buat Set BARU (Penting agar StateFlow mendeteksi perubahan)
        val currentSet = _lockedMenus.value.toMutableSet()

        if (isLocked) {
            currentSet.add(route)
        } else {
            currentSet.remove(route)
        }

        // Simpan ke Preferences & Update State
        prefs.lockedMenus = currentSet.toSet()
        _lockedMenus.value = currentSet.toSet()
    }

    fun backupData(uri: Uri, onSuccess: ()->Unit, onError: (String)->Unit) = viewModelScope.launch {
        val res = backupRepository.backupData(uri)
        if(res.isSuccess) onSuccess() else onError(res.exceptionOrNull()?.message ?: "Error")
    }
    fun restoreData(uri: Uri, onSuccess: ()->Unit, onError: (String)->Unit) = viewModelScope.launch {
        val res = backupRepository.restoreData(uri)
        if(res.isSuccess) onSuccess() else onError(res.exceptionOrNull()?.message ?: "Error")
    }
    fun updateLockedMenus(newSet: Set<String>) {
        // Simpan ke SharedPreferences
        prefs.lockedMenus = newSet
        // Update StateFlow agar MainActivity langsung mendeteksi perubahan
        _lockedMenus.value = newSet
    }
}