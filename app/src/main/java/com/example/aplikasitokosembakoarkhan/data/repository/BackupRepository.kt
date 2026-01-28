package com.example.aplikasitokosembakoarkhan.data.repository

import android.content.Context
import android.net.Uri
import com.example.aplikasitokosembakoarkhan.TokoApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupRepository(private val context: Context) {
    private val DB_NAME = "toko_database"

    suspend fun backupData(destUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Checkpoint WAL agar semua data masuk ke file .db utama
            val db = (context.applicationContext as TokoApplication).database
            val supportDb = db.openHelper.writableDatabase
            supportDb.query("PRAGMA wal_checkpoint(FULL)").close()

            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) throw Exception("Database belum dibuat.")

            context.contentResolver.openOutputStream(destUri)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                    // Backup DB
                    FileInputStream(dbFile).use { origin ->
                        zipOut.putNextEntry(ZipEntry(DB_NAME))
                        origin.copyTo(zipOut)
                        zipOut.closeEntry()
                    }
                    // Backup Gambar
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreData(sourceUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
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
                            if (dbFile.exists()) dbFile.delete()
                            if (dbWal.exists()) dbWal.delete()
                            if (dbShm.exists()) dbShm.delete()
                            FileOutputStream(dbFile).use { it.write(zipIn.readBytes()) }
                        } else if (entry.name.startsWith("images/")) {
                            val fileName = entry.name.substringAfter("images/")
                            if (fileName.isNotEmpty()) {
                                FileOutputStream(File(filesDir, fileName)).use { it.write(zipIn.readBytes()) }
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}