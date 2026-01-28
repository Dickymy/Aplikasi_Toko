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
        cart: Map<Product, Double>, // <-- UBAH KE DOUBLE
        totalPrice: Double,
        payAmount: Double,
        change: Double,
        paymentMethod: String
    ) {
        val prefs = AppPreferences(context)
        val printerAddress = prefs.printerAddress

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
        if (adapter == null || !adapter.isEnabled) {
            Toast.makeText(context, "Bluetooth error", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val device = adapter.getRemoteDevice(printerAddress)
                val connection = BluetoothConnection(device)
                val printer = EscPosPrinter(connection, 203, 48f, 32)

                fun formatRupiah(amount: Double) = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace("Rp", "Rp ").replace(",00", "")

                // Helper format qty (jika bulat tampil bulat, jika desimal tampil koma)
                fun formatQty(qty: Double): String {
                    return if (qty % 1.0 == 0.0) qty.toInt().toString() else qty.toString()
                }

                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
                val dateNow = dateFormat.format(Date())

                var text = ""
                text += "[C]<b>${prefs.storeName}</b>\n"
                text += "[C]${prefs.storeAddress}\n"
                text += "[C]${prefs.storePhone}\n"
                text += "[C]--------------------------------\n"
                text += "[L]$dateNow\n"
                text += "[L]Metode: <b>$paymentMethod</b>\n"
                text += "[C]--------------------------------\n"

                cart.forEach { (product, qty) ->
                    val totalItemPrice = product.sellPrice * qty
                    text += "[L]<b>${product.name}</b>\n"
                    // Tampilkan Qty desimal
                    text += "[L]  ${formatQty(qty)} x ${formatRupiah(product.sellPrice)}[R]${formatRupiah(totalItemPrice)}\n"
                }

                text += "[C]--------------------------------\n"
                text += "[L]TOTAL :[R]<b>${formatRupiah(totalPrice)}</b>\n"

                if (paymentMethod == "Tunai") {
                    text += "[L]TUNAI :[R]${formatRupiah(payAmount)}\n"
                    text += "[L]KEMBALI :[R]${formatRupiah(change)}\n"
                } else {
                    text += "[L]STATUS :[R]LUNAS\n"
                }

                text += "[C]--------------------------------\n"
                text += "[C]${prefs.receiptFooter}\n"

                printer.printFormattedText(text)

                withContext(Dispatchers.Main) { Toast.makeText(context, "Struk Dicetak", Toast.LENGTH_SHORT).show() }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { Toast.makeText(context, "Gagal Cetak: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }
}