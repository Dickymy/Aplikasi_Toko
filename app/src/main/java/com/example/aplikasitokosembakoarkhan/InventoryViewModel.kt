package com.example.aplikasitokosembakoarkhan

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aplikasitokosembakoarkhan.data.*
import com.example.aplikasitokosembakoarkhan.utils.ExcelHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class InventoryViewModel(
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val unitDao: UnitDao,
    private val customerDao: CustomerDao // Tambahan untuk DebtScreen
) : ViewModel() {

    val allProducts = productDao.getAllProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCategories = categoryDao.getAllCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allUnits = unitDao.getAllUnits().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCustomers = customerDao.getAllCustomers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) // Tambahan

    // Notifikasi Stok
    val lowStockProducts = productDao.getAllProducts().map { it.filter { p -> p.stock <= 5 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expiringProducts = productDao.getAllProducts().map { products ->
        val now = System.currentTimeMillis()
        val warningLimit = now + (30L * 24 * 60 * 60 * 1000)
        products.filter { it.expireDate > 0 && it.expireDate < warningLimit }.sortedBy { it.expireDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- CRUD PRODUK ---
    fun insertProductWithImage(product: Product, imageUri: Uri?, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalPath = saveImageToInternalStorage(context, imageUri)
            productDao.insertProduct(product.copy(imagePath = finalPath))
        }
    }

    fun updateProductWithImage(product: Product, imageUri: Uri?, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalPath = if (imageUri != null) {
                if (!product.imagePath.isNullOrEmpty()) File(product.imagePath).delete()
                saveImageToInternalStorage(context, imageUri)
            } else product.imagePath
            productDao.updateProduct(product.copy(imagePath = finalPath))
        }
    }

    fun delete(product: Product) = viewModelScope.launch(Dispatchers.IO) {
        if (!product.imagePath.isNullOrEmpty()) File(product.imagePath).delete()
        productDao.deleteProduct(product)
    }

    // --- CRUD MASTER LAINNYA ---
    fun addCategory(name: String) = viewModelScope.launch { categoryDao.insertCategory(Category(name = name)) }
    fun deleteCategory(category: Category) = viewModelScope.launch { categoryDao.deleteCategory(category) }
    fun addUnit(name: String) = viewModelScope.launch { unitDao.insertUnit(UnitModel(name = name)) }
    fun deleteUnit(unit: UnitModel) = viewModelScope.launch { unitDao.deleteUnit(unit) }

    // --- PELANGGAN & HUTANG (Untuk DebtScreen) ---
    fun addCustomer(name: String, phone: String) = viewModelScope.launch { customerDao.insertCustomer(Customer(name = name, phoneNumber = phone, totalDebt = 0.0)) }
    fun deleteCustomer(c: Customer) = viewModelScope.launch { customerDao.deleteCustomer(c) }
    fun addDebt(c: Customer, amount: Double) = viewModelScope.launch { customerDao.updateCustomer(c.copy(totalDebt = c.totalDebt + amount, lastUpdated = System.currentTimeMillis())) }
    fun payDebt(c: Customer, amount: Double) = viewModelScope.launch { customerDao.updateCustomer(c.copy(totalDebt = (c.totalDebt - amount).coerceAtLeast(0.0), lastUpdated = System.currentTimeMillis())) }

    // --- IMPORT EXCEL ---
    fun importExcel(ctx: Context, uri: Uri, onResult: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = ExcelHelper.parseExcelToProducts(ctx, uri)
            if (list.isNotEmpty()) productDao.insertAll(list)
            withContext(Dispatchers.Main) { onResult(list.size) }
        }
    }
    fun updateCustomer(customer: Customer) = viewModelScope.launch { customerDao.updateCustomer(customer) } // TAMBAHKAN INI

    private fun saveImageToInternalStorage(context: Context, uri: Uri?): String? {
        if (uri == null) return null
        return try {
            val fileName = "img_${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
            file.absolutePath
        } catch (e: Exception) { null }
    }

    // Helper simple update (untuk restock dll)
    fun update(product: Product) = viewModelScope.launch { productDao.updateProduct(product) }
    fun updateCategory(category: Category) = viewModelScope.launch { categoryDao.updateCategory(category) }
    fun updateUnit(unit: UnitModel) = viewModelScope.launch { unitDao.updateUnit(unit) }
}