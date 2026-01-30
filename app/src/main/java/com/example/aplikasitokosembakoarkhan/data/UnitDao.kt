package com.example.aplikasitokosembakoarkhan.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitDao {
    @Query("SELECT * FROM unit_models ORDER BY isPriority DESC, name ASC")
    fun getAllUnits(): Flow<List<UnitModel>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnit(unit: UnitModel)

    @Update
    suspend fun updateUnit(unit: UnitModel)

    @Delete
    suspend fun deleteUnit(unit: UnitModel)

    @Query("SELECT * FROM unit_models")
    fun getAllUnitsSync(): List<UnitModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(units: List<UnitModel>)

    // --- TAMBAHAN BARU ---
    @Query("UPDATE unit_models SET isPriority = 0")
    suspend fun clearAllPriorities()
}