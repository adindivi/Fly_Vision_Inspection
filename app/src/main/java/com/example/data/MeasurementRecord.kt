package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "measurement_history")
data class MeasurementRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val partName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val gapWidth: Float,
    val flushHeight: Float,
    val isPass: Boolean
) {
    val formattedDate: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}
