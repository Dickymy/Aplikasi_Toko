package com.example.aplikasitokosembakoarkhan.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProduct(product: Product)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("SELECT * FROM products")
    fun getAllProductsSync(): List<Product>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    // --- TAMBAHAN BARU UNTUK MEMPERBAIKI ERROR ---
    @Query("SELECT COUNT(*) FROM products WHERE category = :categoryName")
    suspend fun countProductsByCategory(categoryName: String): Int

    @Query("SELECT COUNT(*) FROM products WHERE unit = :unitName")
    suspend fun countProductsByUnit(unitName: String): Int
}