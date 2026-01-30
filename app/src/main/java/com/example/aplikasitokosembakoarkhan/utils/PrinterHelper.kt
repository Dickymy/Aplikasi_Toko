package com.example.aplikasitokosembakoarkhan.utils

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.aplikasitokosembakoarkhan.AppPreferences
import com.example.aplikasitokosembakoarkhan.data.Product
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object PrinterHelper {

    @SuppressLint("MissingPermission")
    fun printReceipt(
        context: Context,
        cart: Map<Product, Double>,
        totalPrice: Double,
        payAmount: Double,
        change: Double,
        paymentMethod: String,
        transactionDate: Long = System.currentTimeMillis()
    ) {
        val prefs = AppPreferences(context)
        val printerAddress = prefs.printerAddress
        val paperSize = prefs.printerPaperSize // 58 atau 80

        // KONFIGURASI KARAKTER PER BARIS
        // 58mm biasanya 32 chars, 80mm biasanya 48 chars
        val maxChars = if (paperSize == 80) 48 else 32

        if (printerAddress.isEmpty()) {
            Toast.makeText(context, "Printer belum diatur!", Toast.LENGTH_SHORT).show()
            return
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter

        if (adapter == null || !adapter.isEnabled) {
            Toast.makeText(context, "Bluetooth mati", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val device = adapter.getRemoteDevice(printerAddress)
            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            val socket = device.createRfcommSocketToServiceRecord(uuid)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return
            }

            socket.connect()
            val outputStream = socket.outputStream
            val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())

            // COMMANDS
            val RESET = byteArrayOf(0x1B, 0x40)
            val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
            val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
            val ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
            val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
            val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
            val TEXT_NORMAL = byteArrayOf(0x1D, 0x21, 0x00)
            val TEXT_LARGE = byteArrayOf(0x1D, 0x21, 0x11)
            val FEED_LINE = byteArrayOf(0x0A)

            // FUNGSI GARIS PEMISAH DINAMIS
            fun printDivider() {
                outputStream.write(("-".repeat(maxChars) + "\n").toByteArray())
            }

            // HEADER
            outputStream.write(RESET)
            outputStream.write(ALIGN_CENTER)
            outputStream.write(BOLD_ON)
            outputStream.write(TEXT_LARGE)
            outputStream.write("${prefs.storeName.ifEmpty { "Toko Arkhan" }}\n".toByteArray())
            outputStream.write(TEXT_NORMAL)
            outputStream.write(BOLD_OFF)
            outputStream.write("${prefs.storeAddress}\n".toByteArray())
            printDivider()

            outputStream.write(ALIGN_LEFT)
            outputStream.write("Tgl: ${dateFormat.format(Date(transactionDate))}\n".toByteArray())
            printDivider()

            // ITEMS
            for ((product, qty) in cart) {
                val subtotal = product.sellPrice * qty
                outputStream.write("${product.name}\n".toByteArray())

                val qtyStr = if(qty % 1.0 == 0.0) qty.toInt().toString() else qty.toString()
                val priceStr = formatRp.format(product.sellPrice).replace("Rp", "").trim()
                val subtotalStr = formatRp.format(subtotal).replace("Rp", "").trim()

                // Layout: "2 x 5.000        10.000"
                val leftText = "$qtyStr x $priceStr"
                val spaceNeeded = maxChars - leftText.length - subtotalStr.length
                val spaces = if (spaceNeeded > 0) " ".repeat(spaceNeeded) else " "

                outputStream.write("$leftText$spaces$subtotalStr\n".toByteArray())
            }

            printDivider()

            // TOTAL
            val totalStr = formatRp.format(totalPrice).replace("Rp", "Rp ")
            outputStream.write(ALIGN_RIGHT)
            outputStream.write(BOLD_ON)
            outputStream.write("TOTAL : $totalStr\n".toByteArray())
            outputStream.write(BOLD_OFF)

            outputStream.write("Bayar : ${formatRp.format(payAmount).replace("Rp", "Rp ")}\n".toByteArray())
            outputStream.write("Kembali : ${formatRp.format(change).replace("Rp", "Rp ")}\n".toByteArray())
            outputStream.write("Metode : $paymentMethod\n".toByteArray())

            // FOOTER
            outputStream.write(ALIGN_CENTER)
            printDivider()
            outputStream.write("${prefs.receiptFooter}\n".toByteArray())
            outputStream.write(FEED_LINE)
            outputStream.write(FEED_LINE)
            outputStream.write(FEED_LINE)

            outputStream.close()
            socket.close()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error Print: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // FUNGSI CEK KONEKSI (PING)
    @SuppressLint("MissingPermission")
    fun testConnection(context: Context, address: String): Boolean {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val device = bluetoothManager.adapter.getRemoteDevice(address)
            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            val socket = device.createRfcommSocketToServiceRecord(uuid)
            socket.connect()
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}