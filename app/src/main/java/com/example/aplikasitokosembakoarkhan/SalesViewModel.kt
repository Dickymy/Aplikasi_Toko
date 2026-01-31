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
        payAmount: Double, // <--- PARAMETER BARU (Uang yang dibayar)
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                var totalAmount = 0.0
                val itemsSummary = StringBuilder()

                // 1. Cek Stok & Hitung Total Dulu
                cart.forEach { (product, qty) ->
                    if (product.stock < qty) {
                        onError("Stok ${product.name} kurang! (Sisa: ${product.stock})")
                        return@launch
                    }

                    // Cek Harga (Grosir/Ecer)
                    val price = if (product.wholesaleQty > 0 && qty >= product.wholesaleQty) {
                        product.wholesalePrice
                    } else {
                        product.sellPrice
                    }

                    totalAmount += price * qty

                    if (itemsSummary.isNotEmpty()) itemsSummary.append(", ")
                    itemsSummary.append("${product.name} x $qty")
                }

                // 2. Update Stok (Kurangi)
                cart.forEach { (product, qty) ->
                    val newStock = product.stock - qty
                    productDao.updateProduct(product.copy(stock = newStock))
                }

                // 3. Hitung Kembalian
                val changeAmount = payAmount - totalAmount

                // 4. Simpan Transaksi Lengkap
                val transaction = Transaction(
                    date = System.currentTimeMillis(),
                    items = itemsSummary.toString(),
                    totalAmount = totalAmount,
                    payAmount = payAmount,       // <--- SIMPAN KE DB
                    changeAmount = changeAmount, // <--- SIMPAN KE DB
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