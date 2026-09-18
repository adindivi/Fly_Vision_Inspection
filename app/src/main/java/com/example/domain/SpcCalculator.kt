package com.example.domain

import com.example.data.MeasurementRecord
import kotlin.math.pow
import kotlin.math.sqrt

object SpcCalculator {
    fun calculateStats(
        records: List<MeasurementRecord>,
        targetValue: Float = 0f,
        tolerance: Float = 0.3f
    ): SpcResult {
        if (records.isEmpty()) return SpcResult(0f, 0f, 0f, sampleCount = 0)

        val flushValues = records.map { it.flushHeight }
        val count = flushValues.size
        val mean = flushValues.average().toFloat()

        if (count < 2) {
            val isPass = kotlin.math.abs(mean - targetValue) <= tolerance
            val simpleCpk = if (isPass) 1.0f else 0.0f
            return SpcResult(mean = mean, stdDev = 0f, cpk = simpleCpk, sampleCount = count)
        }

        // Sample variance with N-1 Bessel's correction
        val variance = flushValues.sumOf { (it - mean).toDouble().pow(2) } / (count - 1)
        val stdDev = sqrt(variance).toFloat().coerceAtLeast(0.0001f)

        val usl = targetValue + tolerance
        val lsl = targetValue - tolerance

        val cpu = (usl - mean) / (3 * stdDev)
        val cpl = (mean - lsl) / (3 * stdDev)
        val rawCpk = minOf(cpu, cpl)

        // Bound Cpk to standard displayable range (-99.99f .. 99.99f) to avoid UI overflow
        val cpk = rawCpk.coerceIn(-99.99f, 99.99f)

        return SpcResult(mean = mean, stdDev = stdDev, cpk = cpk, sampleCount = count)
    }
}

data class SpcResult(
    val mean: Float,
    val stdDev: Float,
    val cpk: Float,
    val sampleCount: Int = 0
)
