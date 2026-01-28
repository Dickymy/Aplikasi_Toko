package com.example.aplikasitokosembakoarkhan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aplikasitokosembakoarkhan.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SalesViewModel(
    private val productDao: ProductDao,
    private val saleDao: SaleDao
) : ViewModel() {

    val allProducts = productDao.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun checkout(
        cart: Map<Product, Int>,
        paymentMethod: String,
        customerName: String, // Parameter Baru
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis()
                cart.forEach { (product, qty) ->
                    // Kurangi stok
                    productDao.updateProduct(product.copy(stock = product.stock - qty))

                    // Hitung harga final
                    val finalSellPrice = if (product.wholesaleQty > 0 && qty >= product.wholesaleQty && product.wholesalePrice > 0)
                        product.wholesalePrice
                    else
                        product.sellPrice

                    // Simpan Transaksi dengan Nama Pelanggan
                    saleDao.insertSale(
                        Sale(
                            productId = product.id,
                            productName = product.name,
                            quantity = qty,
                            capitalPrice = product.buyPrice * qty,
                            totalPrice = finalSellPrice * qty,
                            date = timestamp,
                            customerName = customerName // Simpan di sini
                        )
                    )
                }
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.message ?: "Gagal Transaksi") }
            }
        }
    }
}