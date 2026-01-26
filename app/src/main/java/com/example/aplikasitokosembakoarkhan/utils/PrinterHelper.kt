package com.example.aplikasitokosembakoarkhan.utils

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.example.aplikasitokosembakoarkhan.AppPreferences
import com.example.aplikasitokosembakoarkhan.data.Product
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrinterHelper {

    @SuppressLint("MissingPermission")
    fun printReceipt(
        context: Context,
        cart: Map<Product, Int>,
        totalPrice: Double,
        payAmount: Double,
        change: Double,
        paymentMethod: String // <--- PARAMETER BARU
    ) {
        val prefs = AppPreferences(context)
        val printerAddress = prefs.printerAddress

        // 1. Cek Validasi Dasar
        if (printerAddress.isEmpty()) {
            Toast.makeText(context, "Printer belum diatur di Pengaturan!", Toast.LENGTH_LONG).show()
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Izin Bluetooth ditolak", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            Toast.makeText(context, "Bluetooth tidak didukung", Toast.LENGTH_SHORT).show()
            return
        }

        if (!adapter.isEnabled) {
            Toast.makeText(context, "Bluetooth belum aktif!", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. JALANKAN PROSES DI BACKGROUND (Agar tidak Crash/Hang)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Mencari Device
                val device = adapter.getRemoteDevice(printerAddress)

                if (device == null) {
                    throw Exception("Printer tidak ditemukan di daftar paired device")
                }

                // Mencoba Koneksi
                val connection = BluetoothConnection(device)

                // Inisialisasi Printer
                val printer = EscPosPrinter(connection, 203, 48f, 32)

                // --- SIAPKAN TEXT ---
                fun formatRupiah(amount: Double) = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace("Rp", "Rp ").replace(",00", "")
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
                val dateNow = dateFormat.format(Date())

                var text = ""
                // Header
                text += "[C]<b>${prefs.storeName}</b>\n"
                text += "[C]${prefs.storeAddress}\n"
                text += "[C]${prefs.storePhone}\n"
                text += "[C]--------------------------------\n"

                // Info Transaksi
                text += "[L]$dateNow\n"
                text += "[L]Metode: <b>$paymentMethod</b>\n" // <--- TAMPILKAN METODE BAYAR
                text += "[C]--------------------------------\n"

                // Daftar Barang
                cart.forEach { (product, qty) ->
                    val totalItemPrice = product.sellPrice * qty
                    text += "[L]<b>${product.name}</b>\n"
                    text += "[L]  $qty x ${formatRupiah(product.sellPrice)}[R]${formatRupiah(totalItemPrice)}\n"
                }

                text += "[C]--------------------------------\n"

                // Total
                text += "[L]TOTAL :[R]<b>${formatRupiah(totalPrice)}</b>\n"

                // Logika Tampilan Pembayaran
                if (paymentMethod == "Tunai") {
                    text += "[L]TUNAI :[R]${formatRupiah(payAmount)}\n"
                    text += "[L]KEMBALI :[R]${formatRupiah(change)}\n"
                } else {
                    text += "[L]STATUS :[R]LUNAS ($paymentMethod)\n"
                }

                // Footer
                text += "[C]--------------------------------\n"
                text += "[C]Terima Kasih\n"
                text += "[C]Barang yang dibeli tidak dapat\n"
                text += "[C]dikembalikan\n"

                // Cetak
                printer.printFormattedText(text)

                // Beritahu User Sukses
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Struk Berhasil Dicetak", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val message = when {
                        e.message?.contains("socket closed") == true -> "Koneksi printer terputus/mati."
                        e.message?.contains("connect") == true -> "Gagal terhubung. Cek nyala printer."
                        else -> "Gagal Cetak: ${e.message}"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}