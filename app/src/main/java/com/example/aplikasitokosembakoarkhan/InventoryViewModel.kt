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
import java.util.UUID

class InventoryViewModel(
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val unitDao: UnitDao,
    private val customerDao: CustomerDao,
    private val debtTransactionDao: DebtTransactionDao
) : ViewModel() {

    // --- STATE FLOWS (DATA UTAMA) ---
    val allProducts = productDao.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUnits = unitDao.getAllUnits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCustomers = customerDao.getAllCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- NOTIFIKASI / DASHBOARD ---
    val lowStockProducts = productDao.getAllProducts().map { it.filter { p -> p.stock <= 5 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expiringProducts = productDao.getAllProducts().map { products ->
        val now = System.currentTimeMillis()
        val warningLimit = now + (30L * 24 * 60 * 60 * 1000) // 30 Hari
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

    // UPDATE PRODUCT (PENTING: DIPERLUKAN OLEH RESTOCK SCREEN)
    fun updateProduct(product: Product) {
        viewModelScope.launch {
            productDao.updateProduct(product)
        }
    }

    // Helper simple update (untuk kompatibilitas)
    fun update(product: Product) = updateProduct(product)

    // --- CRUD MASTER DATA ---
    fun addCategory(name: String) = viewModelScope.launch { categoryDao.insertCategory(Category(name = name)) }
    fun updateCategory(category: Category) = viewModelScope.launch { categoryDao.updateCategory(category) }
    fun deleteCategory(category: Category) = viewModelScope.launch { categoryDao.deleteCategory(category) }

    fun addUnit(name: String) = viewModelScope.launch { unitDao.insertUnit(UnitModel(name = name)) }
    fun updateUnit(unit: UnitModel) = viewModelScope.launch { unitDao.updateUnit(unit) }
    fun deleteUnit(unit: UnitModel) = viewModelScope.launch { unitDao.deleteUnit(unit) }

    // --- MANAJEMEN PELANGGAN & HUTANG ---

    fun addCustomer(name: String, phone: String) = viewModelScope.launch {
        customerDao.insertCustomer(Customer(name = name, phoneNumber = phone, totalDebt = 0.0))
    }

    fun updateCustomer(customer: Customer) = viewModelScope.launch {
        customerDao.updateCustomer(customer)
    }

    fun deleteCustomer(c: Customer) = viewModelScope.launch {
        // Hapus Data Pelanggan & Seluruh Riwayat Transaksinya
        customerDao.deleteCustomer(c)
        debtTransactionDao.deleteAllTransactionsByCustomer(c.id)
    }

    // Transaksi: Bayar Hutang
    fun payDebt(c: Customer, amount: Double) = viewModelScope.launch {
        val newDebt = (c.totalDebt - amount).coerceAtLeast(0.0)
        customerDao.updateCustomer(c.copy(
            totalDebt = newDebt,
            lastUpdated = System.currentTimeMillis(),
            hasHistory = true
        ))
        debtTransactionDao.insertTransaction(DebtTransaction(
            customerId = c.id,
            type = "Bayar",
            amount = amount,
            date = System.currentTimeMillis()
        ))
    }

    // Transaksi: Tambah Hutang (Ngutang)
    fun addDebt(c: Customer, amount: Double) = viewModelScope.launch {
        customerDao.updateCustomer(c.copy(
            totalDebt = c.totalDebt + amount,
            lastUpdated = System.currentTimeMillis(),
            hasHistory = true
        ))
        debtTransactionDao.insertTransaction(DebtTransaction(
            customerId = c.id,
            type = "Hutang",
            amount = amount,
            date = System.currentTimeMillis()
        ))
    }

    // Ambil Riwayat Transaksi (Untuk Dialog Detail)
    fun getDebtHistory(customerId: Int) = debtTransactionDao.getTransactionsByCustomer(customerId)

    // --- IMPORT / EXPORT FEATURES ---

    // 1. Import Produk dari Excel
    fun importExcel(ctx: Context, uri: Uri, onResult: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = ExcelHelper.parseExcelToProducts(ctx, uri)
            if (list.isNotEmpty()) productDao.insertAll(list)
            withContext(Dispatchers.Main) { onResult(list.size) }
        }
    }

    // 2. Import Pelanggan dari Kontak HP
    fun importPhoneContacts(context: Context, onResult: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var count = 0
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null
            )

            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    if (nameIndex != -1 && numberIndex != -1) {
                        val name = it.getString(nameIndex) ?: ""
                        var number = it.getString(numberIndex) ?: ""
                        number = number.replace("[^\\d+]".toRegex(), "")

                        if (name.isNotEmpty()) {
                            val exists = customerDao.getCustomerByName(name)
                            if (exists == null) {
                                customerDao.insertCustomer(Customer(name = name, phoneNumber = number, totalDebt = 0.0))
                                count++
                            }
                        }
                    }
                }
            }
            withContext(Dispatchers.Main) {
                onResult(count)
            }
        }
    }

    // 3. Import Pelanggan dari Excel
    fun importCustomers(context: Context, uri: Uri, onResult: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0)
                var count = 0

                val iter = sheet.iterator()
                if (iter.hasNext()) iter.next() // Skip Header

                while (iter.hasNext()) {
                    val row = iter.next()
                    val nameCell = row.getCell(0)
                    val phoneCell = row.getCell(1)

                    val name = nameCell?.toString() ?: ""
                    val phone = phoneCell?.toString()?.replace("[^\\d]".toRegex(), "") ?: ""

                    if (name.isNotEmpty()) {
                        val exists = customerDao.getCustomerByName(name)
                        if (exists == null) {
                            customerDao.insertCustomer(Customer(name = name, phoneNumber = phone, totalDebt = 0.0))
                            count++
                        }
                    }
                }
                inputStream?.close()
                withContext(Dispatchers.Main) { onResult(count) }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onResult(0) }
            }
        }
    }

    // --- PRIVATE HELPERS ---
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