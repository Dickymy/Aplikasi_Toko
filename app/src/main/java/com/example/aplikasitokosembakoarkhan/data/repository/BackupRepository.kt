package com.example.aplikasitokosembakoarkhan.data.repository

import android.content.Context
import android.net.Uri
import com.example.aplikasitokosembakoarkhan.data.InventoryDatabase
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// FIX: Konstruktor menerima (Context, InventoryDatabase)
class BackupRepository(private val context: Context, private val database: InventoryDatabase) {

    suspend fun backupData(uri: Uri): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                    val dbFile = context.getDatabasePath("inventory_database")
                    if (dbFile.exists()) addToZip(zipOut, dbFile, "inventory_database")

                    val dbWal = context.getDatabasePath("inventory_database-wal")
                    if (dbWal.exists()) addToZip(zipOut, dbWal, "inventory_database-wal")

                    val dbShm = context.getDatabasePath("inventory_database-shm")
                    if (dbShm.exists()) addToZip(zipOut, dbShm, "inventory_database-shm")

                    val filesDir = context.filesDir
                    filesDir.listFiles()?.forEach { file ->
                        if (file.isFile && (file.name.endsWith(".jpg") || file.name.endsWith(".png"))) {
                            addToZip(zipOut, file, "images/${file.name}")
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun restoreData(uri: Uri): Result<Unit> {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val fileName = entry.name
                        if (fileName == "inventory_database" || fileName.contains("-wal") || fileName.contains("-shm")) {
                            val dbFile = context.getDatabasePath(fileName)
                            if (dbFile.exists()) dbFile.delete()
                            FileOutputStream(dbFile).use { fos -> zipIn.copyTo(fos) }
                        } else if (fileName.startsWith("images/")) {
                            val imageName = fileName.substringAfter("images/")
                            val imageFile = File(context.filesDir, imageName)
                            FileOutputStream(imageFile).use { fos -> zipIn.copyTo(fos) }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun addToZip(zipOut: ZipOutputStream, file: File, fileName: String) {
        FileInputStream(file).use { fis ->
            val entry = ZipEntry(fileName)
            zipOut.putNextEntry(entry)
            fis.copyTo(zipOut)
            zipOut.closeEntry()
        }
    }
}