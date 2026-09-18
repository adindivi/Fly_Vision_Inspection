package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "ai_analysis_history")
data class AiAnalysisRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imageUri: String,
    val timestamp: Long = System.currentTimeMillis(),
    val apiResponse: String
) {
    val formattedDate: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}
