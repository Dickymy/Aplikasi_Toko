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
                    val stock = row.getCell(4)?.numericCellValue ?: 0.0
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
                                expireDate = 0L,
                                category = "Umum"
                            )
                        )
                    }
                }
                workbook.close()
            }
        } catch (e: Exception) { e.printStackTrace() }
        return productList
    }

    // UPDATE: Mengembalikan File agar MainActivity tidak error
    fun exportProductsToExcel(context: Context, products: List<Product>): File {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Stok Barang")
        val headerRow = sheet.createRow(0)
        val headers = listOf("ID", "Nama Barang", "Kategori", "Harga Beli", "Harga Jual", "Stok", "Satuan", "Barcode")

        headers.forEachIndexed { index, title ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(title)
        }

        products.forEachIndexed { index, product ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(product.id.toDouble())
            row.createCell(1).setCellValue(product.name)
            row.createCell(2).setCellValue(product.category)
            row.createCell(3).setCellValue(product.buyPrice)
            row.createCell(4).setCellValue(product.sellPrice)
            row.createCell(5).setCellValue(product.stock.toDouble())
            row.createCell(6).setCellValue(product.unit)
            row.createCell(7).setCellValue(product.barcode)
        }

        val fileName = "Stok_Barang_${System.currentTimeMillis()}.xlsx"
        val file = File(context.getExternalFilesDir(null), fileName)
        val outputStream = FileOutputStream(file)
        workbook.write(outputStream)
        outputStream.close()
        workbook.close()
        return file
    }

    fun exportSalesToExcel(context: Context, salesList: List<Sale>): File {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Laporan Penjualan")
        val headerRow = sheet.createRow(0)
        val headers = listOf("No", "Tanggal", "Jam", "Nama Barang", "Qty", "Total Jual", "Keuntungan")

        headers.forEachIndexed { index, title ->
            headerRow.createCell(index).setCellValue(title)
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        salesList.forEachIndexed { index, sale ->
            val row = sheet.createRow(index + 1)
            val date = Date(sale.date)
            val profit = sale.totalPrice - sale.capitalPrice
            row.createCell(0).setCellValue((index + 1).toDouble())
            row.createCell(1).setCellValue(dateFormat.format(date))
            row.createCell(2).setCellValue(timeFormat.format(date))
            row.createCell(3).setCellValue(sale.productName)
            row.createCell(4).setCellValue(sale.quantity.toDouble())
            row.createCell(5).setCellValue(sale.totalPrice)
            row.createCell(6).setCellValue(profit)
        }

        val fileName = "Laporan_Penjualan_${System.currentTimeMillis()}.xlsx"
        val file = File(context.getExternalFilesDir(null), fileName)
        val outputStream = FileOutputStream(file)
        workbook.write(outputStream)
        outputStream.close()
        workbook.close()
        return file
    }
}