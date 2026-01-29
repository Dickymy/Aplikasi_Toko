package com.example.aplikasitokosembakoarkhan.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    // Fungsi ini wajib ada agar ViewModel tidak error
    @Query("UPDATE categories SET isPriority = 0")
    suspend fun clearAllPriorities()
}