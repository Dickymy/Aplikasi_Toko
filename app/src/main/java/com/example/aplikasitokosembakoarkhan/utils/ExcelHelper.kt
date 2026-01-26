package com.example.aplikasitokosembakoarkhan.utils

import android.content.Context
import android.net.Uri
import com.example.aplikasitokosembakoarkhan.data.Product
import com.example.aplikasitokosembakoarkhan.data.Sale
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelHelper {

    // --- FUNGSI 1: IMPORT DARI EXCEL (Sama seperti sebelumnya) ---
    fun parseExcelToProducts(context: Context, uri: Uri): List<Product> {
        val productList = mutableListOf<Product>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = XSSFWorkbook(inputStream)
                val sheet = workbook.getSheetAt(0)

                for (row in sheet) {
                    if (row.rowNum == 0) continue // Skip Header

                    val name = row.getCell(1)?.stringCellValue ?: ""
                    val buyPrice = row.getCell(2)?.numericCellValue ?: 0.0
                    val sellPrice = row.getCell(3)?.numericCellValue ?: 0.0
                    val stock = row.getCell(4)?.numericCellValue?.toInt() ?: 0
                    val unit = row.getCell(5)?.stringCellValue ?: "pcs"
                    val barcode = try {
                        row.getCell(6)?.stringCellValue ?: ""
                    } catch (e: Exception) {
                        row.getCell(6)?.numericCellValue?.toLong()?.toString() ?: ""
                    }

                    if (name.isNotEmpty()) {
                        productList.add(
                            Product(
                                name = name,
                                buyPrice = buyPrice,
                                sellPrice = sellPrice,
                                stock = stock,
                                unit = unit,
                                barcode = barcode,
                                expireDate = 0L
                            )
                        )
                    }
                }
                workbook.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return productList
    }

    // --- FUNGSI 2: EXPORT STOK BARANG (Sama seperti sebelumnya) ---
    fun exportProductsToExcel(context: Context, products: List<Product>): File {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Stok Barang")

        val headerRow = sheet.createRow(0)
        val headers = listOf("ID", "Nama Barang", "Harga Beli", "Harga Jual", "Stok", "Satuan", "Barcode")

        headers.forEachIndexed { index, title ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(title)
            // Style Header Bold
            val style = workbook.createCellStyle()
            val font = workbook.createFont()
            font.bold = true
            style.setFont(font)
            cell.cellStyle = style
        }

        products.forEachIndexed { index, product ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(product.id.toDouble())
            row.createCell(1).setCellValue(product.name)
            row.createCell(2).setCellValue(product.buyPrice)
            row.createCell(3).setCellValue(product.sellPrice)
            row.createCell(4).setCellValue(product.stock.toDouble())
            row.createCell(5).setCellValue(product.unit)
            row.createCell(6).setCellValue(product.barcode)
        }

        // --- PERBAIKAN: JANGAN PAKAI autoSizeColumn (Bikin Crash di Android) ---
        // Ganti dengan setColumnWidth manual
        // Angka 15 * 256 artinya lebar kira-kira 15 karakter
        for (i in headers.indices) {
            sheet.setColumnWidth(i, 20 * 256)
        }

        val fileName = "Stok_Barang_${System.currentTimeMillis()}.xlsx"
        val file = File(context.cacheDir, fileName)
        val outputStream = FileOutputStream(file)
        workbook.write(outputStream)
        outputStream.close()
        workbook.close()
        return file
    }

    // --- FUNGSI 3: EXPORT LAPORAN PENJUALAN (DIPERBAIKI) ---
    fun exportSalesToExcel(context: Context, salesList: List<Sale>): File {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Laporan Penjualan")

        // 1. Header
        val headerRow = sheet.createRow(0)
        val headers = listOf("No", "Tanggal", "Jam", "Rincian Barang", "Total Omzet", "Keuntungan", "Metode Bayar")

        headers.forEachIndexed { index, title ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(title)
            val style = workbook.createCellStyle()
            val font = workbook.createFont()
            font.bold = true
            style.setFont(font)
            cell.cellStyle = style
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        // 2. Isi Data
        salesList.forEachIndexed { index, sale ->
            val row = sheet.createRow(index + 1)
            val date = Date(sale.date)

            row.createCell(0).setCellValue((index + 1).toDouble())
            row.createCell(1).setCellValue(dateFormat.format(date))
            row.createCell(2).setCellValue(timeFormat.format(date))
            row.createCell(3).setCellValue(sale.itemsSummary)
            row.createCell(4).setCellValue(sale.totalPrice)
            row.createCell(5).setCellValue(sale.totalProfit)
            row.createCell(6).setCellValue(sale.paymentMethod)
        }

        // --- BAGIAN INI YANG BIKIN CRASH SEBELUMNYA ---
        // HAPUS: sheet.autoSizeColumn(i)
        // GANTI DENGAN MANUAL WIDTH:

        sheet.setColumnWidth(0, 5 * 256)  // No (Kecil)
        sheet.setColumnWidth(1, 15 * 256) // Tanggal
        sheet.setColumnWidth(2, 10 * 256) // Jam
        sheet.setColumnWidth(3, 40 * 256) // Rincian Barang (Lebar)
        sheet.setColumnWidth(4, 15 * 256) // Omzet
        sheet.setColumnWidth(5, 15 * 256) // Profit
        sheet.setColumnWidth(6, 15 * 256) // Metode

        val fileName = "Laporan_Penjualan_${System.currentTimeMillis()}.xlsx"
        val file = File(context.cacheDir, fileName) // Simpan di Cache dulu
        val outputStream = FileOutputStream(file)
        workbook.write(outputStream)
        outputStream.close()
        workbook.close()
        return file
    }
}