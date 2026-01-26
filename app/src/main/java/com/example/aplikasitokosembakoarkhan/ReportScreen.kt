package com.example.aplikasitokosembakoarkhan

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.aplikasitokosembakoarkhan.data.Sale
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportScreen(viewModel: ProductViewModel) { // <--- NAMA FUNGSI SUDAH DIPERBAIKI (BUKAN ProductScreen LAGI)
    val salesList by viewModel.allSales.collectAsState()
    val context = LocalContext.current

    // Hitung Ringkasan
    val totalRevenue = salesList.sumOf { it.totalPrice }
    val totalProfit = salesList.sumOf { it.totalPrice - it.capitalPrice }

    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    }

    fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(timestamp))
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Laporan Penjualan", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Ringkasan kinerja toko", fontSize = 14.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(16.dp))

        // Kartu Ringkasan (Omzet & Laba)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Omzet", fontSize = 12.sp)
                    Text(formatRupiah(totalRevenue), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Keuntungan", fontSize = 12.sp)
                    Text(formatRupiah(totalProfit), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tombol Export Excel
        Button(
            onClick = {
                if (salesList.isNotEmpty()) {
                    viewModel.exportSales(context, salesList) { file ->
                        // Share File Excel
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Kirim Laporan Excel"))
                    }
                } else {
                    Toast.makeText(context, "Belum ada data penjualan", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Download, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export Laporan ke Excel")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Riwayat Transaksi Terakhir", fontWeight = FontWeight.SemiBold)

        // List Riwayat Transaksi
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Urutkan dari yang terbaru
            items(salesList.sortedByDescending { it.date }) { sale ->
                Card(
                    modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatDate(sale.date), fontSize = 12.sp, color = Color.Gray)
                            // Badge Keuntungan per Transaksi
                            val profit = sale.totalPrice - sale.capitalPrice
                            Text("Laba: ${formatRupiah(profit)}", fontSize = 10.sp, color = Color(0xFF388E3C))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(sale.productName, fontWeight = FontWeight.Bold)
                                Text("${sale.quantity} item", fontSize = 12.sp)
                            }
                            Text(formatRupiah(sale.totalPrice), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}