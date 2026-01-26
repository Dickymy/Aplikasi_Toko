package com.example.aplikasitokosembakoarkhan

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.aplikasitokosembakoarkhan.data.*
import com.example.aplikasitokosembakoarkhan.utils.ExcelHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ProductViewModel(
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val unitDao: UnitDao,
    private val customerDao: CustomerDao,
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao
) : ViewModel() {

    val allProducts = productDao.getAllProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSales = saleDao.getAllSales().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allUnits = unitDao.getAllUnits().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCategories = categoryDao.getAllCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCustomers = customerDao.getAllCustomers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allExpenses = expenseDao.getAllExpenses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todaySales = saleDao.getAllSales().map { sales ->
        val start = getStartOfDay()
        sales.filter { it.date >= start }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts = productDao.getAllProducts().map { it.filter { p -> p.stock <= 5 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expiringProducts = productDao.getAllProducts().map { products ->
        val now = System.currentTimeMillis()
        val warningLimit = now + (30L * 24 * 60 * 60 * 1000)
        products.filter { it.expireDate > 0 && it.expireDate < warningLimit }.sortedBy { it.expireDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- LOGIKA GAMBAR ---
    fun insertProductWithImage(product: Product, imageUri: Uri?, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalPath = saveImageToInternalStorage(context, imageUri)
            productDao.insertProduct(product.copy(imagePath = finalPath))
        }
    }

    fun updateProductWithImage(product: Product, imageUri: Uri?, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalPath = if (imageUri != null) {
                if (!product.imagePath.isNullOrEmpty()) {
                    val oldFile = File(product.imagePath)
                    if (oldFile.exists()) oldFile.delete()
                }
                saveImageToInternalStorage(context, imageUri)
            } else product.imagePath
            productDao.updateProduct(product.copy(imagePath = finalPath))
        }
    }

    private fun saveImageToInternalStorage(context: Context, uri: Uri?): String? {
        if (uri == null) return null
        return try {
            val fileName = "img_${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
            file.absolutePath
        } catch (e: Exception) { null }
    }

    fun delete(product: Product) = viewModelScope.launch {
        if (!product.imagePath.isNullOrEmpty()) {
            val file = File(product.imagePath)
            if (file.exists()) file.delete()
        }
        productDao.deleteProduct(product)
    }

    // --- BASIC CRUD ---
    fun update(product: Product) = viewModelScope.launch { productDao.updateProduct(product) }
    fun addCategory(name: String) = viewModelScope.launch { categoryDao.insertCategory(Category(name = name)) }
    fun updateCategory(category: Category) = viewModelScope.launch { categoryDao.updateCategory(category) }
    fun deleteCategory(category: Category) = viewModelScope.launch { categoryDao.deleteCategory(category) }
    fun addUnit(name: String) = viewModelScope.launch { unitDao.insertUnit(UnitModel(name = name)) }
    fun updateUnit(unit: UnitModel) = viewModelScope.launch { unitDao.updateUnit(unit) }
    fun deleteUnit(unit: UnitModel) = viewModelScope.launch { unitDao.deleteUnit(unit) }
    fun addExpense(name: String, amount: Double) = viewModelScope.launch { expenseDao.insertExpense(Expense(description = name, amount = amount, date = System.currentTimeMillis())) }
    fun updateExpense(expense: Expense) = viewModelScope.launch { expenseDao.updateExpense(expense) }
    fun deleteExpense(expense: Expense) = viewModelScope.launch { expenseDao.deleteExpense(expense) }
    fun addCustomer(name: String, phone: String) = viewModelScope.launch { customerDao.insertCustomer(Customer(name = name, phoneNumber = phone, totalDebt = 0.0)) }
    fun deleteCustomer(c: Customer) = viewModelScope.launch { customerDao.deleteCustomer(c) }
    fun addDebt(c: Customer, amount: Double) = viewModelScope.launch { customerDao.updateCustomer(c.copy(totalDebt = c.totalDebt + amount, lastUpdated = System.currentTimeMillis())) }
    fun payDebt(c: Customer, amount: Double) = viewModelScope.launch { customerDao.updateCustomer(c.copy(totalDebt = (c.totalDebt - amount).coerceAtLeast(0.0), lastUpdated = System.currentTimeMillis())) }

    fun checkout(cart: Map<Product, Int>, paymentMethod: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis()
                cart.forEach { (product, qty) ->
                    productDao.updateProduct(product.copy(stock = product.stock - qty))
                    val finalSellPrice = if (product.wholesaleQty > 0 && qty >= product.wholesaleQty && product.wholesalePrice > 0) product.wholesalePrice else product.sellPrice
                    saleDao.insertSale(Sale(productId = product.id, productName = product.name, quantity = qty, capitalPrice = product.buyPrice * qty, totalPrice = finalSellPrice * qty, date = timestamp))
                }
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) { withContext(Dispatchers.Main) { onError(e.message ?: "Gagal") } }
        }
    }

    fun importExcel(ctx: Context, uri: Uri, onResult: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = ExcelHelper.parseExcelToProducts(ctx, uri)
            if (list.isNotEmpty()) productDao.insertAll(list)
            withContext(Dispatchers.Main) { onResult(list.size) }
        }
    }

    fun exportSales(context: Context, sales: List<Sale>, onSuccess: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = ExcelHelper.exportSalesToExcel(context, sales)
            withContext(Dispatchers.Main) { onSuccess(file) }
        }
    }

    // --- BACKUP & RESTORE YANG DIOPTIMALKAN ---
    private val DB_NAME = "toko_database"

    fun backupData(context: Context, destUri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. PENTING: Pindahkan semua data dari WAL ke file utama (.db)
                val db = (context.applicationContext as TokoApplication).database
                val supportDb = db.openHelper.writableDatabase
                supportDb.query("PRAGMA wal_checkpoint(FULL)").close()

                val dbFile = context.getDatabasePath(DB_NAME)
                if (!dbFile.exists()) throw Exception("Database belum dibuat.")

                context.contentResolver.openOutputStream(destUri)?.use { outputStream ->
                    ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->

                        // A. Backup File Database Utama Saja (Karena sudah di-checkpoint)
                        FileInputStream(dbFile).use { origin ->
                            zipOut.putNextEntry(ZipEntry(DB_NAME))
                            origin.copyTo(zipOut)
                            zipOut.closeEntry()
                        }

                        // B. Backup Semua Gambar
                        val filesDir = context.filesDir
                        filesDir.listFiles()?.forEach { file ->
                            if (file.isFile && (file.name.endsWith(".jpg") || file.name.endsWith(".png"))) {
                                FileInputStream(file).use { origin ->
                                    zipOut.putNextEntry(ZipEntry("images/${file.name}"))
                                    origin.copyTo(zipOut)
                                    zipOut.closeEntry()
                                }
                            }
                        }
                    }
                }
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.message ?: "Gagal Backup") }
            }
        }
    }

    fun restoreData(context: Context, sourceUri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbFile = context.getDatabasePath(DB_NAME)
                val dbWal = File(dbFile.path + "-wal")
                val dbShm = File(dbFile.path + "-shm")
                val filesDir = context.filesDir

                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                        var entry = zipIn.nextEntry
                        while (entry != null) {
                            if (entry.name == DB_NAME) {
                                // 1. RESTORE DATABASE
                                // Hapus SEMUA file database lama agar bersih
                                if (dbFile.exists()) dbFile.delete()
                                if (dbWal.exists()) dbWal.delete()
                                if (dbShm.exists()) dbShm.delete()

                                // Tulis file .db baru
                                FileOutputStream(dbFile).use { output ->
                                    zipIn.copyTo(output)
                                }
                            } else if (entry.name.startsWith("images/")) {
                                // 2. RESTORE GAMBAR
                                val fileName = entry.name.substringAfter("images/")
                                if (fileName.isNotEmpty()) {
                                    val targetFile = File(filesDir, fileName)
                                    FileOutputStream(targetFile).use { output ->
                                        zipIn.copyTo(output)
                                    }
                                }
                            }
                            zipIn.closeEntry()
                            entry = zipIn.nextEntry
                        }
                    }
                }
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.message ?: "Gagal Restore") }
            }
        }
    }

    private fun getStartOfDay(): Long {
        val c = java.util.Calendar.getInstance()
        c.set(java.util.Calendar.HOUR_OF_DAY, 0); c.set(java.util.Calendar.MINUTE, 0)
        c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val db = (this[APPLICATION_KEY] as TokoApplication).database
                ProductViewModel(db.productDao(), db.saleDao(), db.unitDao(), db.customerDao(), db.expenseDao(), db.categoryDao())
            }
        }
    }
}