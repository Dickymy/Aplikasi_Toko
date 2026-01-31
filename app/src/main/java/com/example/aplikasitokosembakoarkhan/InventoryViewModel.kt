package com.example.aplikasitokosembakoarkhan

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class InventoryViewModel(
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val unitDao: UnitDao,
    private val customerDao: CustomerDao,
    private val debtTransactionDao: DebtTransactionDao
) : ViewModel() {

    // --- STATE FLOWS ---
    val allProducts = productDao.getAllProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCategories = categoryDao.getAllCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allUnits = unitDao.getAllUnits().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCustomers = customerDao.getAllCustomers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- DASHBOARD DATA ---
    val lowStockProducts = productDao.getAllProducts().map { it.filter { p -> p.stock <= 5 } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
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

    fun updateProduct(product: Product) = viewModelScope.launch { productDao.updateProduct(product) }
    // fun update(product: Product) = updateProduct(product) // Hapus duplikat agar lebih rapi

    // --- CRUD KATEGORI ---
    fun addCategory(name: String, isPriority: Boolean) = viewModelScope.launch {
        if (isPriority) categoryDao.clearAllPriorities()
        categoryDao.insertCategory(Category(name = name, isPriority = isPriority))
    }

    fun updateCategory(category: Category) = viewModelScope.launch {
        if (category.isPriority) categoryDao.clearAllPriorities()
        categoryDao.updateCategory(category)
    }

    fun deleteCategorySafe(category: Category, onSuccess: () -> Unit, onError: (String) -> Unit) = viewModelScope.launch {
        val count = productDao.countProductsByCategory(category.name)
        if (count > 0) {
            withContext(Dispatchers.Main) { onError("Gagal! Kategori ini dipakai oleh $count barang.") }
        } else {
            categoryDao.deleteCategory(category)
            withContext(Dispatchers.Main) { onSuccess() }
        }
    }

    // --- CRUD SATUAN ---
    fun addUnit(name: String, isPriority: Boolean) = viewModelScope.launch {
        if (isPriority) unitDao.clearAllPriorities()
        unitDao.insertUnit(UnitModel(name = name, isPriority = isPriority))
    }

    fun updateUnit(unit: UnitModel) = viewModelScope.launch {
        if (unit.isPriority) unitDao.clearAllPriorities()
        unitDao.updateUnit(unit)
    }

    fun deleteUnitSafe(unit: UnitModel, onSuccess: () -> Unit, onError: (String) -> Unit) = viewModelScope.launch {
        val count = productDao.countProductsByUnit(unit.name)
        if (count > 0) {
            withContext(Dispatchers.Main) { onError("Gagal! Satuan ini dipakai oleh $count barang.") }
        } else {
            unitDao.deleteUnit(unit)
            withContext(Dispatchers.Main) { onSuccess() }
        }
    }

    // --- MANAJEMEN PELANGGAN ---
    fun addCustomer(name: String, phone: String) = viewModelScope.launch {
        if(customerDao.getCustomerByName(name) == null) {
            customerDao.insertCustomer(Customer(name = name, phoneNumber = phone, totalDebt = 0.0))
        }
    }
    fun updateCustomer(customer: Customer) = viewModelScope.launch { customerDao.updateCustomer(customer) }
    fun deleteCustomer(c: Customer) = viewModelScope.launch { customerDao.deleteCustomer(c); debtTransactionDao.deleteAllTransactionsByCustomer(c.id) }

    fun payDebt(c: Customer, amount: Double) = viewModelScope.launch {
        val newDebt = (c.totalDebt - amount).coerceAtLeast(0.0)
        customerDao.updateCustomer(c.copy(totalDebt = newDebt, lastUpdated = System.currentTimeMillis(), hasHistory = true))
        debtTransactionDao.insertTransaction(DebtTransaction(customerId = c.id, type = "Bayar", amount = amount, date = System.currentTimeMillis()))
    }

    fun addDebt(c: Customer, amount: Double) = viewModelScope.launch {
        customerDao.updateCustomer(c.copy(totalDebt = c.totalDebt + amount, lastUpdated = System.currentTimeMillis(), hasHistory = true))
        debtTransactionDao.insertTransaction(DebtTransaction(customerId = c.id, type = "Hutang", amount = amount, date = System.currentTimeMillis()))
    }

    fun getDebtHistory(customerId: Int) = debtTransactionDao.getTransactionsByCustomer(customerId)

    // --- EXPORT & IMPORT ---

    // 1. Export Data Barang ke CSV
    fun exportDataToCsv(context: Context, uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val products = productDao.getAllProductsSync()
            val result = ExcelHelper.exportProductsToCsv(context, uri, products)
            withContext(Dispatchers.Main) {
                if (result.isSuccess) onSuccess() else onError(result.exceptionOrNull()?.message ?: "Gagal Export")
            }
        }
    }

    // 2. Import Data Barang dari CSV
    fun importDataFromCsv(context: Context, uri: Uri, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = ExcelHelper.importProductsFromCsv(context, uri)
            if (result.isSuccess) {
                val newProducts = result.getOrNull() ?: emptyList()
                newProducts.forEach { p ->
                    val existing = productDao.getProductByBarcode(p.barcode)
                    if (existing != null) {
                        val updated = p.copy(id = existing.id, imagePath = existing.imagePath)
                        productDao.updateProduct(updated)
                    } else {
                        productDao.insertProduct(p)
                    }
                }
                withContext(Dispatchers.Main) { onSuccess(newProducts.size) }
            } else {
                withContext(Dispatchers.Main) { onError(result.exceptionOrNull()?.message ?: "Gagal Import") }
            }
        }
    }

    // 3. Export Transaksi ke CSV
    fun exportTransactionsToCsv(context: Context, uri: Uri, transactions: List<Transaction>, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val writer = outputStream.bufferedWriter()
                    val DELIMITER = ","
                    val header = "\"ID\"$DELIMITER\"Tanggal\"$DELIMITER\"Waktu\"$DELIMITER\"Pelanggan\"$DELIMITER\"Metode Bayar\"$DELIMITER\"Total Belanja\"$DELIMITER\"Detail Barang\"\n"
                    writer.write(header)

                    transactions.forEach { trans ->
                        val dateDate = Date(trans.date)
                        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateDate)
                        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(dateDate)
                        val itemsClean = trans.items.replace("\"", "\"\"").replace("\n", " | ").replace(",", " - ")
                        val totalClean = trans.totalAmount.toLong().toString()
                        val row = "\"${trans.id}\"$DELIMITER\"$dateStr\"$DELIMITER\"$timeStr\"$DELIMITER\"${trans.customerName}\"$DELIMITER\"${trans.paymentMethod}\"$DELIMITER\"$totalClean\"$DELIMITER\"$itemsClean\"\n"
                        writer.write(row)
                    }
                    writer.flush()
                }
                withContext(Dispatchers.Main) { onResult("Berhasil! Silakan buka file di Excel.") }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onResult("Gagal export: ${e.message}") }
            }
        }
    }

    // 4. Import Kontak HP ke Pelanggan
    fun importPhoneContacts(context: Context, onResult: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var count = 0
            val cursor = context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    if (nameIndex != -1 && numberIndex != -1) {
                        val name = it.getString(nameIndex) ?: ""
                        val number = it.getString(numberIndex) ?: "".replace("[^\\d+]".toRegex(), "")
                        if (name.isNotEmpty() && customerDao.getCustomerByName(name) == null) {
                            customerDao.insertCustomer(Customer(name = name, phoneNumber = number, totalDebt = 0.0))
                            count++
                        }
                    }
                }
            }
            withContext(Dispatchers.Main) { onResult(count) }
        }
    }

    // 5. Import Customer dari Excel (Optional: Memerlukan Library POI yang berat,
    // jika ingin CSV saja, fungsi ini bisa dihapus/diganti)
    // Untuk saat ini saya comment agar tidak error jika library POI tidak ada
    /*
    fun importCustomers(context: Context, uri: Uri, onResult: (Int) -> Unit) {
        // ... (Kode import POI) ...
    }
    */

    // --- HELPER INTERNAL ---
    private fun saveImageToInternalStorage(context: Context, uri: Uri?): String? {
        if (uri == null) return null
        return try {
            val fileName = "img_${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
            file.absolutePath
        } catch (e: Exception) { null }
    }
}