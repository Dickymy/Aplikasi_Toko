package com.example.aplikasitokosembakoarkhan.utils

import android.content.Context
import android.net.Uri
import com.example.aplikasitokosembakoarkhan.data.Product
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

object ExcelHelper {

    // Header untuk file CSV
    private const val CSV_HEADER = "Barcode,Nama Barang,Kategori,Satuan,Stok,Harga Beli,Harga Jual,Keterangan"

    // --- EXPORT KE CSV ---
    fun exportProductsToCsv(context: Context, uri: Uri, products: List<Product>): Result<Boolean> {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val writer = outputStream.bufferedWriter()

                // Tulis Header
                writer.write(CSV_HEADER)
                writer.newLine()

                // Tulis Data
                for (p in products) {
                    val line = listOf(
                        escapeCsv(p.barcode),
                        escapeCsv(p.name),
                        escapeCsv(p.category),
                        escapeCsv(p.unit),
                        p.stock.toString(),
                        p.buyPrice.toString(),
                        p.sellPrice.toString(),
                        escapeCsv(p.description)
                    ).joinToString(",")

                    writer.write(line)
                    writer.newLine()
                }
                writer.flush()
            }
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // --- IMPORT DARI CSV ---
    fun importProductsFromCsv(context: Context, uri: Uri): Result<List<Product>> {
        return try {
            val products = mutableListOf<Product>()
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("Gagal membuka file"))

            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line = reader.readLine() // Baca Header dulu (skip)

                while (true) {
                    line = reader.readLine() ?: break
                    if (line.isBlank()) continue

                    // Parse CSV manual (menangani koma dalam kutip)
                    val tokens = parseCsvLine(line)
                    if (tokens.size >= 7) {
                        try {
                            val product = Product(
                                barcode = tokens[0],
                                name = tokens[1],
                                category = tokens[2].ifEmpty { "Umum" },
                                unit = tokens[3].ifEmpty { "Pcs" },
                                stock = tokens[4].toDoubleOrNull() ?: 0.0,
                                buyPrice = tokens[5].toDoubleOrNull() ?: 0.0,
                                sellPrice = tokens[6].toDoubleOrNull() ?: 0.0,
                                description = if (tokens.size > 7) tokens[7] else "",
                                expireDate = 0L // Default
                            )
                            products.add(product)
                        } catch (e: Exception) {
                            // Skip baris yang error formatnya
                        }
                    }
                }
            }
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Fungsi bantu untuk menangani karakter koma dalam nama barang
    private fun escapeCsv(value: String): String {
        var result = value.replace("\"", "\"\"") // Escape double quotes
        if (result.contains(",") || result.contains("\n") || result.contains("\"")) {
            result = "\"$result\""
        }
        return result
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (char in line) {
            if (char == '\"') {
                inQuotes = !inQuotes
            } else if (char == ',' && !inQuotes) {
                result.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(char)
            }
        }
        result.add(sb.toString().trim())
        return result
    }
}