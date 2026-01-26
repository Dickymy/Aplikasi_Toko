package com.example.aplikasitokosembakoarkhan.utils

import android.content.Context
import android.net.Uri
import com.example.aplikasitokosembakoarkhan.data.Product
import com.example.aplikasitokosembakoarkhan.data.Sale
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelHelper {

    fun parseExcelToProducts(context: Context, uri: Uri): List<Product> {
        val productList = mutableListOf<Product>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = XSSFWorkbook(inputStream)
                val sheet = workbook.getSheetAt(0)

                for (row in sheet) {
                    if (row.rowNum == 0) continue

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

    fun exportProductsToExcel(context: Context, products: List<Product>): File {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Stok Barang")

        val headerRow = sheet.createRow(0)
        val headers = listOf("ID", "Nama Barang", "Harga Beli", "Harga Jual", "Stok", "Satuan", "Barcode")

        headers.forEachIndexed { index, title ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(title)
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

    fun exportSalesToExcel(context: Context, salesList: List<Sale>): File {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Laporan Penjualan")

        // 1. Header (Disesuaikan dengan data yang ada)
        val headerRow = sheet.createRow(0)
        val headers = listOf("No", "Tanggal", "Jam", "Nama Barang", "Qty", "Total Jual", "Keuntungan")

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

            // Hitung Profit Manual
            val profit = sale.totalPrice - sale.capitalPrice

            row.createCell(0).setCellValue((index + 1).toDouble())
            row.createCell(1).setCellValue(dateFormat.format(date))
            row.createCell(2).setCellValue(timeFormat.format(date))
            row.createCell(3).setCellValue(sale.productName) // Gunakan ProductName
            row.createCell(4).setCellValue(sale.quantity.toDouble())
            row.createCell(5).setCellValue(sale.totalPrice)
            row.createCell(6).setCellValue(profit)
        }

        sheet.setColumnWidth(0, 5 * 256)
        sheet.setColumnWidth(1, 15 * 256)
        sheet.setColumnWidth(2, 10 * 256)
        sheet.setColumnWidth(3, 40 * 256)
        sheet.setColumnWidth(4, 10 * 256)
        sheet.setColumnWidth(5, 15 * 256)
        sheet.setColumnWidth(6, 15 * 256)

        val fileName = "Laporan_Penjualan_${System.currentTimeMillis()}.xlsx"
        val file = File(context.cacheDir, fileName)
        val outputStream = FileOutputStream(file)
        workbook.write(outputStream)
        outputStream.close()
        workbook.close()
        return file
    }
}