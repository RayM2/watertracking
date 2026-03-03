package com.example.watertracking

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<WaterEntry>>

    @Insert
    suspend fun insert(entry: WaterEntry)

    @Query("DELETE FROM water_entries")
    suspend fun deleteAll()
}
