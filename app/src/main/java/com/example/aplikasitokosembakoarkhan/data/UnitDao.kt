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
    @Query("SELECT * FROM units ORDER BY name ASC")
    fun getAllUnits(): Flow<List<UnitModel>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnit(unit: UnitModel)

    // --- TAMBAHAN YANG SEBELUMNYA HILANG ---
    @Update
    suspend fun updateUnit(unit: UnitModel)

    @Delete
    suspend fun deleteUnit(unit: UnitModel)
}