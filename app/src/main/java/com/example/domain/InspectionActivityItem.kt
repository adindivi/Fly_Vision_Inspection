package com.example.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ActivityType {
    FEATURE,
    ANOMALY
}

enum class ActivityStatus {
    PROCESSING,
    PASS,
    DEFECT,
    WARNING
}

data class InspectionActivityItem(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: ActivityType,
    val anatomicalRegion: String,
    val status: ActivityStatus,
    val confidence: Int = 90,
    val details: String,
    val specimenSource: String = "Live Capture"
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}
