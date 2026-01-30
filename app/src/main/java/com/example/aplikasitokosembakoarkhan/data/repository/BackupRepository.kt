package com.example.aplikasitokosembakoarkhan.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.aplikasitokosembakoarkhan.data.AppDatabase
import com.example.aplikasitokosembakoarkhan.data.Category
import com.example.aplikasitokosembakoarkhan.data.Customer
import com.example.aplikasitokosembakoarkhan.data.Expense
import com.example.aplikasitokosembakoarkhan.data.Product
import com.example.aplikasitokosembakoarkhan.data.Transaction
import com.example.aplikasitokosembakoarkhan.data.UnitModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// Data Class untuk menyimpan Status Backup
sealed class BackupStatus {
    object Idle : BackupStatus()
    data class Loading(val progress: Int, val message: String) : BackupStatus()
    data class Success(val message: String) : BackupStatus()
    data class Error(val message: String) : BackupStatus()
}

data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val products: List<Product>,
    val categories: List<Category>,
    val units: List<UnitModel>,
    val customers: List<Customer>,
    val transactions: List<Transaction>,
    val expenses: List<Expense>
)

class BackupRepository(private val context: Context, private val db: AppDatabase) {
    private val gson = Gson()
    private val imagesDir = File(context.filesDir, "product_images")

    fun backupDataFlow(uri: Uri): Flow<BackupStatus> = flow {
        emit(BackupStatus.Loading(0, "Menyiapkan Data..."))
        try {
            // 1. Ambil Data (10%)
            val products = db.productDao().getAllProductsSync()
            val backupData = BackupData(
                products = products,
                categories = db.categoryDao().getAllCategoriesSync(),
                units = db.unitDao().getAllUnitsSync(),
                customers = db.customerDao().getAllCustomersSync(),
                transactions = db.transactionDao().getAllTransactionsSync(),
                expenses = db.expenseDao().getAllExpensesSync()
            )
            emit(BackupStatus.Loading(10, "Mengonversi Database..."))

            val jsonString = gson.toJson(backupData)
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: throw Exception("Gagal membuka file tujuan")

            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                // 2. Simpan JSON (20%)
                val dataEntry = ZipEntry("database.json")
                zipOut.putNextEntry(dataEntry)
                zipOut.write(jsonString.toByteArray())
                zipOut.closeEntry()
                emit(BackupStatus.Loading(20, "Memproses Gambar..."))

                // 3. Simpan Gambar (20% - 100%)
                val productsWithImg = products.filter { !it.imagePath.isNullOrEmpty() }
                val totalImages = productsWithImg.size

                if (totalImages == 0) {
                    emit(BackupStatus.Loading(100, "Menyelesaikan..."))
                } else {
                    productsWithImg.forEachIndexed { index, product ->
                        val sourceFile = File(product.imagePath!!)
                        if (sourceFile.exists()) {
                            try {
                                val imgEntry = ZipEntry("images/${sourceFile.name}")
                                zipOut.putNextEntry(imgEntry)
                                FileInputStream(sourceFile).use { it.copyTo(zipOut) }
                                zipOut.closeEntry()
                            } catch (e: Exception) {
                                Log.e("Backup", "Skip img: ${product.imagePath}")
                            }
                        }
                        // Hitung Progress: Mulai dari 20% sampai 100%
                        val progress = 20 + ((index + 1) * 80 / totalImages)
                        emit(BackupStatus.Loading(progress, "Mengompres Gambar (${index+1}/$totalImages)"))

                        // Sedikit delay agar animasi UI terlihat smooth jika prosesnya terlalu cepat
                        if (index % 5 == 0) delay(10)
                    }
                }
            }

            val prefs = context.getSharedPreferences("toko_prefs", Context.MODE_PRIVATE)
            prefs.edit().putLong("last_backup_time", System.currentTimeMillis()).apply()

            emit(BackupStatus.Success("Backup Berhasil Disimpan!"))
        } catch (e: Exception) {
            e.printStackTrace()
            emit(BackupStatus.Error("Gagal: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    fun restoreDataFlow(uri: Uri): Flow<BackupStatus> = flow {
        emit(BackupStatus.Loading(0, "Membuka File..."))
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw Exception("Gagal membuka file backup")

            var backupData: BackupData? = null

            if (!imagesDir.exists()) imagesDir.mkdirs()

            // Mode Restore: Progress 0-50% saat Ekstrak, 50-100% saat Insert DB
            ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                var entry = zipIn.nextEntry
                var entryCount = 0

                while (entry != null) {
                    val entryName = entry.name
                    entryCount++

                    // Update progress dummy karena kita tidak tahu total file di zip
                    // Kita buat berputar 0-50%
                    val fakeProgress = (entryCount * 5) % 50
                    emit(BackupStatus.Loading(fakeProgress, "Mengekstrak: $entryName"))

                    if (entryName == "database.json") {
                        val reader = InputStreamReader(zipIn)
                        backupData = gson.fromJson(reader, BackupData::class.java)
                    } else if (entryName.startsWith("images/")) {
                        val fileName = File(entryName).name
                        val destFile = File(imagesDir, fileName)
                        FileOutputStream(destFile).use { fos -> zipIn.copyTo(fos) }
                    }

                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            emit(BackupStatus.Loading(50, "Memulihkan Database..."))

            backupData?.let { data ->
                db.clearAllTables()

                emit(BackupStatus.Loading(60, "Memulihkan Master Data..."))
                if (data.categories.isNotEmpty()) db.categoryDao().insertAll(data.categories)
                if (data.units.isNotEmpty()) db.unitDao().insertAll(data.units)
                if (data.customers.isNotEmpty()) db.customerDao().insertAll(data.customers)

                emit(BackupStatus.Loading(80, "Memulihkan Produk..."))
                val updatedProducts = data.products.map { product ->
                    if (!product.imagePath.isNullOrEmpty()) {
                        val fileName = File(product.imagePath).name
                        val newPath = File(imagesDir, fileName).absolutePath
                        product.copy(imagePath = newPath)
                    } else product
                }
                if (updatedProducts.isNotEmpty()) db.productDao().insertAll(updatedProducts)

                emit(BackupStatus.Loading(90, "Memulihkan Transaksi..."))
                if (data.transactions.isNotEmpty()) db.transactionDao().insertAll(data.transactions)
                if (data.expenses.isNotEmpty()) db.expenseDao().insertAll(data.expenses)

                emit(BackupStatus.Success("Restore Berhasil! Restarting..."))
            } ?: emit(BackupStatus.Error("File backup kosong/rusak"))

        } catch (e: Exception) {
            e.printStackTrace()
            emit(BackupStatus.Error("Gagal: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    fun getLastBackupTime(): String {
        val prefs = context.getSharedPreferences("toko_prefs", Context.MODE_PRIVATE)
        val time = prefs.getLong("last_backup_time", 0)
        return if (time == 0L) "Belum pernah"
        else SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(time))
    }
}