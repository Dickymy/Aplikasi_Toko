package com.example.aplikasitokosembakoarkhan.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "units")
data class UnitModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String
)