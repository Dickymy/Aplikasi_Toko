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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ProductViewModel(
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val unitDao: UnitDao,
    private val customerDao: CustomerDao,
    private val expenseDao: ExpenseDao // <--- 1. TAMBAHKAN DAO PENGELUARAN
) : ViewModel() {

    val allProducts = productDao.getAllProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSales = saleDao.getAllSales().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allUnits = unitDao.getAllUnits().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCustomers = customerDao.getAllCustomers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allExpenses = expenseDao.getAllExpenses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) // <--- 2. FLOW PENGELUARAN

    // --- DASHBOARD LOGIC ---
    val todaySales = saleDao.getAllSales().map { sales ->
        val start = getStartOfDay()
        sales.filter { it.date >= start }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts = productDao.getAllProducts().map { it.filter { p -> p.stock <= 5 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expiringProducts = productDao.getAllProducts().map { products ->
        val now = System.currentTimeMillis()
        val warningLimit = now + (30L * 24 * 60 * 60 * 1000)
        products.filter { it.expireDate > 0 && it.expireDate < warningLimit }
            .sortedBy { it.expireDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun getStartOfDay(): Long {
        val c = java.util.Calendar.getInstance()
        c.set(java.util.Calendar.HOUR_OF_DAY, 0); c.set(java.util.Calendar.MINUTE, 0)
        c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    // --- FUNGSI CUSTOMER ---
    fun addCustomer(name: String, phone: String) { viewModelScope.launch { customerDao.insertCustomer(Customer(name = name, phoneNumber = phone, totalDebt = 0.0)) } }
    fun addDebt(c: Customer, amount: Double) {
        viewModelScope.launch {
            customerDao.updateCustomer(c.copy(
                totalDebt = c.totalDebt + amount,
                lastUpdated = System.currentTimeMillis() // <--- Simpan Waktu
            ))
        }
    }
    fun payDebt(c: Customer, amount: Double) {
        viewModelScope.launch {
            customerDao.updateCustomer(c.copy(
                totalDebt = (c.totalDebt - amount).coerceAtLeast(0.0),
                lastUpdated = System.currentTimeMillis() // <--- Simpan Waktu
            ))
        }
    }
    fun deleteCustomer(c: Customer) { viewModelScope.launch { customerDao.deleteCustomer(c) } }

    fun addUnit(name: String) { viewModelScope.launch { unitDao.insertUnit(UnitModel(name = name)) } }

    // --- FUNGSI PENGELUARAN (BARU) ---
    fun addExpense(name: String, amount: Double) {
        viewModelScope.launch {
            // Ganti 'name =' menjadi 'description ='
            expenseDao.insertExpense(Expense(description = name, amount = amount, date = System.currentTimeMillis()))
        }
    }
    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.deleteExpense(expense)
        }
    }

    // --- FUNGSI PRODUK ---
    fun saveProduct(
        id: Int? = null, name: String, buy: String, sell: String, stock: String, barcode: String, unit: String,
        expireDate: Long,
        wholesaleQtyStr: String, // Tambahan
        wholesalePriceStr: String, // Tambahan
        uri: Uri?, ctx: Context, onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // ... (Kode Cek Barcode Lama Tetap Sama) ...
            if (id == null || id == 0) {
                if (productDao.getProductByBarcode(barcode) != null) {
                    withContext(Dispatchers.Main) { onError("Barcode sudah ada!") }
                    return@launch
                }
            }

            // ... (Kode Simpan Gambar Lama Tetap Sama) ...
            var path: String? = null
            if (uri != null) {
                // ... (Logic gambar tetap sama) ...
                try {
                    val file = File(ctx.filesDir, "img_${UUID.randomUUID()}.jpg")
                    ctx.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
                    path = file.absolutePath
                } catch (e: Exception) { e.printStackTrace() }
            }

            // SIMPAN DATA TERMASUK GROSIR
            val p = Product(
                id = id ?: 0,
                name = name,
                buyPrice = buy.toDoubleOrNull() ?: 0.0,
                sellPrice = sell.toDoubleOrNull() ?: 0.0,
                stock = stock.toIntOrNull() ?: 0,
                barcode = barcode,
                unit = unit,
                imagePath = path,
                expireDate = expireDate,
                wholesaleQty = wholesaleQtyStr.toIntOrNull() ?: 0,       // Simpan Qty Grosir
                wholesalePrice = wholesalePriceStr.toDoubleOrNull() ?: 0.0 // Simpan Harga Grosir
            )

            if (id == null || id == 0) productDao.insertProduct(p) else productDao.updateProduct(p)
            withContext(Dispatchers.Main) { onSuccess() }
        }
    }

    // UPDATE FUNGSI UPDATE
    fun updateProductWithImageCheck(
        original: Product, name: String, buy: String, sell: String, stock: String, barcode: String, unit: String,
        expireDate: Long,
        wholesaleQtyStr: String, // Tambahan
        wholesalePriceStr: String, // Tambahan
        uri: Uri?, ctx: Context, onSuccess: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var path = original.imagePath
            if (uri != null) {
                // ... (Logic gambar tetap sama) ...
                try {
                    val file = File(ctx.filesDir, "img_${UUID.randomUUID()}.jpg")
                    ctx.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
                    path = file.absolutePath
                } catch (e: Exception) { e.printStackTrace() }
            }

            // UPDATE DATA TERMASUK GROSIR
            productDao.updateProduct(original.copy(
                name = name,
                buyPrice = buy.toDoubleOrNull() ?: 0.0,
                sellPrice = sell.toDoubleOrNull() ?: 0.0,
                stock = stock.toIntOrNull() ?: 0,
                barcode = barcode,
                unit = unit,
                imagePath = path,
                expireDate = expireDate,
                wholesaleQty = wholesaleQtyStr.toIntOrNull() ?: 0,       // Update Qty Grosir
                wholesalePrice = wholesalePriceStr.toDoubleOrNull() ?: 0.0 // Update Harga Grosir
            ))

            withContext(Dispatchers.Main) { onSuccess() }
        }
    }

    fun addStock(p: Product, amount: Int, onSuccess: () -> Unit) { viewModelScope.launch { productDao.updateProduct(p.copy(stock = p.stock + amount)); onSuccess() } }

    fun checkout(cart: Map<Product, Int>, paymentMethod: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis()

                cart.forEach { (product, qty) ->
                    // 1. Kurangi Stok
                    val newStock = product.stock - qty
                    productDao.updateProduct(product.copy(stock = newStock))

                    // 2. Hitung Harga (Cek Grosir)
                    val finalSellPrice = if (product.wholesaleQty > 0 && qty >= product.wholesaleQty && product.wholesalePrice > 0) {
                        product.wholesalePrice
                    } else {
                        product.sellPrice
                    }

                    // 3. Hitung Total & Modal
                    val totalJual = finalSellPrice * qty
                    val totalModal = product.buyPrice * qty

                    // 4. Simpan ke Riwayat (Tabel Sale)
                    val sale = Sale(
                        productId = product.id,
                        productName = product.name,
                        quantity = qty,
                        capitalPrice = totalModal,
                        totalPrice = totalJual,
                        date = timestamp
                    )
                    saleDao.insertSale(sale)
                }

                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.message ?: "Transaksi Gagal") }
            }
        }
    }

    fun deleteProduct(p: Product, onSuccess: () -> Unit) { viewModelScope.launch { productDao.deleteProduct(p); onSuccess() } }

    fun importExcel(ctx: Context, uri: Uri, onResult: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = ExcelHelper.parseExcelToProducts(ctx, uri)
            if (list.isNotEmpty()) productDao.insertAll(list)
            onResult(list.size)
        }
    }

    // --- FITUR EXPORT LAPORAN ---
    fun exportSales(context: Context, sales: List<Sale>, onSuccess: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = ExcelHelper.exportSalesToExcel(context, sales)
            withContext(Dispatchers.Main) {
                onSuccess(file)
            }
        }
    }

    // --- FITUR BACKUP & RESTORE ---
    private val DB_NAME = "toko-database"

    fun backupDatabase(context: Context, destUri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = (context.applicationContext as TokoApplication).database
                db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)")

                val dbFile = context.getDatabasePath(DB_NAME)
                if (!dbFile.exists()) throw Exception("Database belum dibuat.")

                context.contentResolver.openOutputStream(destUri)?.use { output ->
                    dbFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }

                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.message ?: "Gagal Backup") }
            }
        }
    }

    fun restoreDatabase(context: Context, sourceUri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbFile = context.getDatabasePath(DB_NAME)

                val walFile = File(dbFile.path + "-wal")
                val shmFile = File(dbFile.path + "-shm")
                if (walFile.exists()) walFile.delete()
                if (shmFile.exists()) shmFile.delete()

                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    dbFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.message ?: "Gagal Restore") }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val db = (this[APPLICATION_KEY] as TokoApplication).database
                // 3. UPDATE PARAMETER FACTORY
                ProductViewModel(db.productDao(), db.saleDao(), db.unitDao(), db.customerDao(), db.expenseDao())
            }
        }
    }
}