package com.example.aplikasitokosembakoarkhan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aplikasitokosembakoarkhan.data.Expense
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(viewModel: ProductViewModel) {
    val expenseList by viewModel.allExpenses.collectAsState()

    // State untuk Dialog Input (Tambah/Edit)
    var showDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) } // Jika null = Tambah, Jika isi = Edit
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    // State untuk Dialog Hapus
    var showDeleteDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    fun formatRupiah(valAmount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(valAmount)
    }

    val totalExpense = expenseList.sumOf { it.amount }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    expenseToEdit = null // Reset ke mode Tambah
                    name = ""
                    amount = ""
                    showDialog = true
                },
                containerColor = Color(0xFFD32F2F),
                contentColor = Color.White
            ) { Icon(Icons.Default.Add, "Tambah") }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Biaya Operasional", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Pengeluaran", fontSize = 12.sp, color = Color.Red)
                        Text(formatRupiah(totalExpense), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                    }
                    Icon(Icons.Default.TrendingDown, null, tint = Color.Red, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Riwayat Pengeluaran", fontWeight = FontWeight.SemiBold)

            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(expenseList) { expense ->
                    // FORMAT WAKTU LENGKAP
                    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id")).format(Date(expense.date))

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(expense.description, fontWeight = FontWeight.Bold) // PERBAIKAN: use description
                                Text(dateStr, fontSize = 12.sp, color = Color.Gray)
                                Text(formatRupiah(expense.amount), color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            // Tombol Edit & Hapus
                            Row {
                                IconButton(onClick = {
                                    expenseToEdit = expense
                                    name = expense.description // PERBAIKAN: use description
                                    amount = expense.amount.toString().replace(".0", "")
                                    showDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, "Edit", tint = Color.Blue)
                                }
                                IconButton(onClick = {
                                    expenseToDelete = expense
                                    showDeleteDialog = true
                                }) {
                                    Icon(Icons.Default.Delete, "Hapus", tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG INPUT (TAMBAH / EDIT) ---
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (expenseToEdit == null) "Catat Pengeluaran" else "Edit Pengeluaran") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Keterangan") })
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { if(it.all { c -> c.isDigit() }) amount = it },
                        label = { Text("Biaya (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cost = amount.toDoubleOrNull() ?: 0.0
                        if (name.isNotEmpty() && cost > 0) {
                            if (expenseToEdit == null) {
                                // Mode Tambah - pastikan parameter di ViewModel sesuai
                                viewModel.addExpense(name, cost)
                            } else {
                                // Mode Edit - update menggunakan description
                                viewModel.updateExpense(expenseToEdit!!.copy(description = name, amount = cost))
                            }
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Batal") } }
        )
    }

    // --- DIALOG KONFIRMASI HAPUS ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Data?") },
            text = { Text("Yakin ingin menghapus pengeluaran '${expenseToDelete?.description}' senilai ${expenseToDelete?.amount}?") }, // PERBAIKAN: use description
            confirmButton = {
                TextButton(
                    onClick = {
                        expenseToDelete?.let { viewModel.deleteExpense(it) }
                        showDeleteDialog = false
                    }
                ) { Text("Hapus", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") }
            }
        )
    }
}