package com.example.aplikasitokosembakoarkhan.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProducts(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    // --- TAMBAHAN BARU: CEK PENGGUNAAN ---
    @Query("SELECT COUNT(*) FROM products WHERE category = :categoryName")
    suspend fun countProductsByCategory(categoryName: String): Int

    @Query("SELECT COUNT(*) FROM products WHERE unit = :unitName")
    suspend fun countProductsByUnit(unitName: String): Int
}