package com.example.aplikasitokosembakoarkhan

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aplikasitokosembakoarkhan.data.*
import com.example.aplikasitokosembakoarkhan.utils.ExcelHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

class ReportViewModel(
    private val saleDao: SaleDao,
    private val expenseDao: ExpenseDao
) : ViewModel() {

    val allSales = saleDao.getAllSales().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allExpenses = expenseDao.getAllExpenses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Untuk Dashboard
    val todaySales = saleDao.getAllSales().map { sales ->
        val start = getStartOfDay()
        sales.filter { it.date >= start }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- EXPENSES ---
    fun addExpense(name: String, amount: Double) = viewModelScope.launch { expenseDao.insertExpense(Expense(description = name, amount = amount, date = System.currentTimeMillis())) }
    fun updateExpense(expense: Expense) = viewModelScope.launch { expenseDao.updateExpense(expense) }
    fun deleteExpense(expense: Expense) = viewModelScope.launch { expenseDao.deleteExpense(expense) }

    // --- EXPORT ---
    fun exportSales(context: Context, sales: List<Sale>, onSuccess: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = ExcelHelper.exportSalesToExcel(context, sales)
            withContext(Dispatchers.Main) { onSuccess(file) }
        }
    }

    private fun getStartOfDay(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
}