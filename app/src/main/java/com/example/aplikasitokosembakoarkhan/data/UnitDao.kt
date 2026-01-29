package com.example.aplikasitokosembakoarkhan.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitDao {
    @Query("SELECT * FROM unit_models ORDER BY id ASC")
    fun getAllUnits(): Flow<List<UnitModel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: UnitModel)

    @Update
    suspend fun updateUnit(unit: UnitModel)

    @Delete
    suspend fun deleteUnit(unit: UnitModel)

    // --- TAMBAHAN BARU ---
    @Query("UPDATE unit_models SET isPriority = 0")
    suspend fun clearAllPriorities()
}