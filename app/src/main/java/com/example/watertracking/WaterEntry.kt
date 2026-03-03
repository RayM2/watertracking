package com.example.watertracking

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_entries")
data class WaterEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Int, // in ml
    val drinkType: String, // Water, Juice, Alcohol, Milk, etc.
    val timestamp: Long = System.currentTimeMillis()
)
