package com.example.aplikasitokosembakoarkhan

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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

// --- VIEWMODEL ---
class ExpenseViewModel(private val expenseDao: ExpenseDao) : ViewModel() {
    val allExpenses = expenseDao.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addExpense(desc: String, amount: Double, date: Long) {
        viewModelScope.launch { expenseDao.insertExpense(Expense(description = desc, amount = amount, date = date)) }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch { expenseDao.updateExpense(expense) }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { expenseDao.deleteExpense(expense) }
    }
}

// --- SCREEN UTAMA ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExpenseScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as InventoryApplication
    val viewModel = remember { ExpenseViewModel(app.container.expenseDao) }
    val expenses by viewModel.allExpenses.collectAsState()

    // State Filter Waktu
    var selectedFilterType by remember { mutableStateOf("Bulanan") }
    var displayedMonth by remember { mutableStateOf(Calendar.getInstance()) }

    // State Dialog Input/Edit
    var showDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var currentId by remember { mutableStateOf(0) }
    var description by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }

    // State Dialog Hapus
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    // --- LOGIKA FILTER ---
    val sixMonthsAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -6) }.timeInMillis
    val oneYearAgo = Calendar.getInstance().apply { add(Calendar.YEAR, -1) }.timeInMillis

    val filteredExpenses = expenses.filter { item ->
        when (selectedFilterType) {
            "Bulanan" -> {
                val c = Calendar.getInstance()
                c.timeInMillis = item.date
                c.get(Calendar.MONTH) == displayedMonth.get(Calendar.MONTH) &&
                        c.get(Calendar.YEAR) == displayedMonth.get(Calendar.YEAR)
            }
            "6 Bulan" -> item.date >= sixMonthsAgo
            "1 Tahun" -> item.date >= oneYearAgo
            else -> true
        }
    }.sortedByDescending { it.date }

    val groupedExpenses = filteredExpenses.groupBy {
        SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID")).format(Date(it.date))
    }

    val totalExpense: Double = filteredExpenses.sumOf { it.amount }

    // Helpers
    fun formatRupiah(amount: Double): String = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount).replace("Rp", "Rp ").replace(",00", "")
    fun formatInput(input: String): String {
        val clean = input.replace("[^\\d]".toRegex(), "")
        if (clean.isEmpty()) return ""
        return try { NumberFormat.getInstance(Locale("id", "ID")).format(clean.toLong()) } catch (e: Exception) { clean }
    }
    fun cleanInput(input: String): Double = input.replace(".", "").replace(",", "").toDoubleOrNull() ?: 0.0

    fun changeMonth(amount: Int) {
        val newCal = displayedMonth.clone() as Calendar
        newCal.add(Calendar.MONTH, amount)
        displayedMonth = newCal
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    isEditing = false; description = ""; amountInput = ""; selectedDate = System.currentTimeMillis()
                    showDialog = true
                },
                containerColor = Color(0xFFD32F2F),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Tambah", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // --- HEADER FILTER (CHIPS) ---
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Column {
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filters = listOf("Bulanan", "6 Bulan", "1 Tahun")
                        items(filters) { filter ->
                            val isSelected = selectedFilterType == filter

                            // FIX: Menggunakan implementasi Chip yang paling dasar dan aman
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFilterType = filter },
                                label = { Text(filter, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFEBEE),
                                    selectedLabelColor = Color(0xFFD32F2F)
                                )
                                // border dihapus agar menggunakan default yang aman
                            )
                        }
                    }

                    if (selectedFilterType == "Bulanan") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { changeMonth(-1) }) {
                                Icon(Icons.Default.ChevronLeft, null, tint = Color.Gray)
                            }
                            Text(
                                text = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(displayedMonth.time),
                                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121)
                            )
                            IconButton(onClick = { changeMonth(1) }) {
                                Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- LIST PENGELUARAN ---
            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    // TOTAL CARD
                    Box(modifier = Modifier.padding(16.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFFD32F2F), Color(0xFFEF5350))
                                        )
                                    )
                                    .padding(24.dp)
                            ) {
                                Column {
                                    val title = when(selectedFilterType) {
                                        "Bulanan" -> "Total Bulan Ini"
                                        "6 Bulan" -> "Total 6 Bulan"
                                        else -> "Total 1 Tahun"
                                    }
                                    Text(title, fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatRupiah(totalExpense),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 32.sp,
                                        color = Color.White
                                    )
                                }
                                Icon(
                                    Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.align(Alignment.CenterEnd).size(64.dp)
                                )
                            }
                        }
                    }
                }

                if (filteredExpenses.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Savings, null, tint = Color.LightGray, modifier = Modifier.size(80.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Tidak ada pengeluaran", color = Color.Gray, fontSize = 16.sp)
                        }
                    }
                } else {
                    groupedExpenses.forEach { (dateHeader, items) ->
                        stickyHeader {
                            Surface(
                                color = Color(0xFFF8F9FA),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(text = dateHeader, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            }
                        }

                        items(items) { item ->
                            // ITEM CARD
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(0.5.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(shape = CircleShape, color = Color(0xFFFFEBEE), modifier = Modifier.size(42.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.ShoppingBag, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.description, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF212121))
                                        Text(SimpleDateFormat("HH:mm", Locale("id")).format(Date(item.date)), fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "- ${formatRupiah(item.amount)}", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row {
                                            IconButton(onClick = {
                                                isEditing = true; currentId = item.id; description = item.description
                                                amountInput = formatInput(item.amount.toLong().toString()); selectedDate = item.date
                                                showDialog = true
                                            }, modifier = Modifier.size(28.dp)) {
                                                Icon(Icons.Outlined.Edit, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = {
                                                expenseToDelete = item
                                                showDeleteConfirmDialog = true
                                            }, modifier = Modifier.size(28.dp)) {
                                                Icon(Icons.Outlined.Delete, null, tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- ALERT DIALOG ---
    if (showDeleteConfirmDialog && expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = Color.Red, modifier = Modifier.size(32.dp)) },
            title = { Text("Hapus Pengeluaran?", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Hapus data ini?", fontSize = 14.sp)
                    Text(expenseToDelete!!.description, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteExpense(expenseToDelete!!); showDeleteConfirmDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { Text("Hapus") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) { Text("Batal") }
            }
        )
    }

    // --- INPUT DIALOG ---
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if(isEditing) "Edit Data" else "Catat Baru", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Kategori Cepat:", fontSize = 12.sp, color = Color.Gray)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        val suggestions = listOf("Kulakan", "Listrik", "Air", "Gaji", "Makan", "Transport", "Lainnya")
                        items(suggestions) { label ->
                            val isSelected = description == label
                            // FIX: Menggunakan SuggestionChip yang aman
                            SuggestionChip(
                                onClick = { description = label },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if(isSelected) Color(0xFFFFEBEE) else Color.Transparent,
                                    labelColor = if(isSelected) Color(0xFFD32F2F) else Color.Black
                                ),
                                border = null // Disable border to avoid type mismatch
                            )
                        }
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Keperluan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = formatInput(it) },
                        label = { Text("Nominal (Rp)") },
                        prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = SimpleDateFormat("dd MMMM yyyy", Locale("id")).format(Date(selectedDate)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tanggal") },
                        trailingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Black,
                            disabledLabelColor = Color.Gray,
                            disabledBorderColor = Color.Gray,
                            disabledTrailingIconColor = Color.Gray
                        )
                    )
                    Surface(
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .offset(y = (-56).dp)
                            .clickable {
                                val c = Calendar.getInstance(); c.timeInMillis = selectedDate
                                DatePickerDialog(context, { _, y, m, d ->
                                    val newC = Calendar.getInstance(); newC.set(y, m, d)
                                    selectedDate = newC.timeInMillis
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            }
                    ) {}
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = cleanInput(amountInput)
                        if (description.isNotEmpty() && amount > 0) {
                            if (isEditing) viewModel.updateExpense(Expense(id = currentId, description = description, amount = amount, date = selectedDate))
                            else viewModel.addExpense(description, amount, selectedDate)
                            showDialog = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Simpan Transaksi") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Batal", color = Color.Gray) }
            }
        )
    }
}