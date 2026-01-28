package com.example.aplikasitokosembakoarkhan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aplikasitokosembakoarkhan.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SalesViewModel(
    private val productDao: ProductDao,
    private val transactionDao: TransactionDao
) : ViewModel() {

    val allProducts = productDao.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun checkout(
        cart: Map<Product, Double>,
        paymentMethod: String,
        customerName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                var totalAmount = 0.0
                val itemsSummary = StringBuilder()

                cart.forEach { (product, qty) ->
                    if (product.stock < qty) {
                        onError("Stok ${product.name} kurang!")
                        return@launch
                    }

                    val price = if (product.wholesaleQty > 0 && qty >= product.wholesaleQty) {
                        product.wholesalePrice
                    } else {
                        product.sellPrice
                    }

                    totalAmount += price * qty
                    itemsSummary.append("${product.name} x$qty, ")

                    val newStock = product.stock - qty
                    productDao.updateProduct(product.copy(stock = newStock))
                }

                val transaction = Transaction(
                    date = System.currentTimeMillis(),
                    totalAmount = totalAmount,
                    items = itemsSummary.toString(),
                    paymentMethod = paymentMethod,
                    customerName = customerName
                )
                transactionDao.insertTransaction(transaction)
                onSuccess()
            } catch (e: Exception) {
                onError("Gagal: ${e.message}")
            }
        }
    }
}