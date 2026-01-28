package com.example.aplikasitokosembakoarkhan

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aplikasitokosembakoarkhan.data.Expense
import com.example.aplikasitokosembakoarkhan.data.ExpenseDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ViewModel didefinisikan di sini saja untuk kemudahan
class ExpenseViewModel(private val expenseDao: ExpenseDao) : ViewModel() {
    val allExpenses = expenseDao.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addExpense(desc: String, amount: Double) {
        viewModelScope.launch { expenseDao.insertExpense(Expense(description = desc, amount = amount, date = System.currentTimeMillis())) }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch { expenseDao.updateExpense(expense) }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { expenseDao.deleteExpense(expense) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as InventoryApplication

    // Inisialisasi Manual
    val viewModel = remember { ExpenseViewModel(app.container.expenseDao) }

    val expenses by viewModel.allExpenses.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var currentId by remember { mutableStateOf(0) }
    var description by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }

    fun formatRupiah(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace("Rp", "Rp ").replace(",00", "")
    }

    fun formatInput(input: String): String {
        val clean = input.replace("[^\\d]".toRegex(), "")
        if (clean.isEmpty()) return ""
        return try {
            val number = clean.toLong()
            NumberFormat.getInstance(Locale("id", "ID")).format(number)
        } catch (e: Exception) { clean }
    }

    fun cleanInput(input: String): Double {
        return input.replace(".", "").replace(",", "").toDoubleOrNull() ?: 0.0
    }

    val currentMonth = Calendar.getInstance().get(Calendar.MONTH)

    val filteredExpenses = expenses.filter {
        val c = Calendar.getInstance()
        c.timeInMillis = it.date
        c.get(Calendar.MONTH) == currentMonth
    }.sortedByDescending { it.date }

    // FIX: Tentukan tipe Double secara eksplisit agar compiler tidak bingung
    val totalExpense: Double = filteredExpenses.sumOf { it.amount }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                isEditing = false
                description = ""
                amountInput = ""
                selectedDate = System.currentTimeMillis()
                showDialog = true
            }) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pengeluaran Bulan Ini", fontSize = 14.sp, color = Color.Gray)
                    Text(formatRupiah(totalExpense), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.Red)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                if (filteredExpenses.isEmpty()) {
                    item { Text("Belum ada pengeluaran.", color = Color.Gray) }
                } else {
                    items(filteredExpenses) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.description, fontWeight = FontWeight.Bold)
                                    Text(SimpleDateFormat("dd MMM yyyy", Locale("id")).format(Date(item.date)), fontSize = 12.sp, color = Color.Gray)
                                }
                                Text("- ${formatRupiah(item.amount)}", color = Color.Red, fontWeight = FontWeight.Bold)

                                IconButton(onClick = {
                                    isEditing = true
                                    currentId = item.id
                                    description = item.description
                                    amountInput = formatInput(item.amount.toLong().toString())
                                    selectedDate = item.date
                                    showDialog = true
                                }) { Icon(Icons.Default.Edit, null, tint = Color.Gray) }

                                IconButton(onClick = { viewModel.deleteExpense(item) }) {
                                    Icon(Icons.Default.Delete, null, tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if(isEditing) "Edit Pengeluaran" else "Tambah Pengeluaran") },
            text = {
                Column {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Keterangan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = formatInput(it) },
                        label = { Text("Jumlah (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = SimpleDateFormat("dd MMM yyyy", Locale("id")).format(Date(selectedDate)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tanggal") },
                        trailingIcon = {
                            IconButton(onClick = {
                                val c = Calendar.getInstance()
                                c.timeInMillis = selectedDate
                                DatePickerDialog(context, { _, y, m, d ->
                                    val newC = Calendar.getInstance()
                                    newC.set(y, m, d)
                                    selectedDate = newC.timeInMillis
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            }) { Icon(Icons.Default.Edit, null) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = cleanInput(amountInput)
                    if (description.isNotEmpty() && amount > 0) {
                        if (isEditing) {
                            viewModel.updateExpense(Expense(id = currentId, description = description, amount = amount, date = selectedDate))
                        } else {
                            viewModel.addExpense(description, amount)
                        }
                        showDialog = false
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Batal") } }
        )
    }
}