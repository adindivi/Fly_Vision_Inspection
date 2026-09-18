package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AiAnalysisDao {
    @Insert
    suspend fun insert(record: AiAnalysisRecord): Long

    @Query("SELECT * FROM ai_analysis_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<AiAnalysisRecord>>
    
    @Query("DELETE FROM ai_analysis_history")
    suspend fun clearAll()
}
