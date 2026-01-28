package com.example.aplikasitokosembakoarkhan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aplikasitokosembakoarkhan.data.Expense
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    viewModel: ReportViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val expenses by viewModel.allExpenses.collectAsState()

    var showDialog by remember { mutableStateOf(false) }

    // State HAPUS
    var showDeleteDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var currentExpense by remember { mutableStateOf<Expense?>(null) }

    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
    }

    val totalExpense = expenses.sumOf { it.amount }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                description = ""; amount = ""; isEditing = false; showDialog = true
            }) { Icon(Icons.Default.Add, "Tambah") }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Pengeluaran Operasional", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Total: ${formatRupiah(totalExpense)}", color = Color.Red, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(expenses.sortedByDescending { it.date }) { expense ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(expense.description, fontWeight = FontWeight.Bold)
                                Text(SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(expense.date)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Text(formatRupiah(expense.amount), color = Color.Red, fontWeight = FontWeight.Bold)
                            Row {
                                IconButton(onClick = { description = expense.description; amount = expense.amount.toInt().toString(); currentExpense = expense; isEditing = true; showDialog = true }) { Icon(Icons.Default.Edit, "Edit", tint = Color.Blue) }
                                IconButton(onClick = {
                                    expenseToDelete = expense
                                    showDeleteDialog = true
                                }) { Icon(Icons.Default.Delete, "Hapus", tint = Color.Gray) }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG KONFIRMASI HAPUS
    if (showDeleteDialog && expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Data?") },
            text = { Text("Yakin hapus pengeluaran '${expenseToDelete!!.description}'?") },
            confirmButton = {
                Button(onClick = { viewModel.deleteExpense(expenseToDelete!!); showDeleteDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") } }
        )
    }

    // DIALOG INPUT
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (isEditing) "Edit Pengeluaran" else "Tambah Pengeluaran") },
            text = {
                Column {
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Keterangan") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = amount, onValueChange = { if (it.all { char -> char.isDigit() }) amount = it }, label = { Text("Jumlah (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (description.isNotEmpty() && amount.isNotEmpty()) {
                        val amountVal = amount.toDouble()
                        if (isEditing && currentExpense != null) viewModel.updateExpense(currentExpense!!.copy(description = description, amount = amountVal))
                        else viewModel.addExpense(description, amountVal)
                        showDialog = false
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Batal") } }
        )
    }
}