package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    @Insert
    suspend fun insert(record: MeasurementRecord)

    @Query("SELECT * FROM measurement_history ORDER BY timestamp DESC LIMIT 50")
    fun getRecentMeasurements(): Flow<List<MeasurementRecord>>

    @Query("SELECT * FROM measurement_history ORDER BY timestamp DESC")
    suspend fun getAllMeasurements(): List<MeasurementRecord>
    
    @Query("DELETE FROM measurement_history")
    suspend fun clearAll()
}
