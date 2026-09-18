package com.example

import com.example.data.MeasurementRecord
import com.example.domain.SpcCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Rigorous unit tests for SpcCalculator statistical process control calculations.
 * Replaces legacy dummy 2+2 tests with production-grade validation of Bessel's correction,
 * Cpk bounding, zero-division safety, and empty/single-sample edge cases.
 */
class SpcCalculatorUnitTest {

    @Test
    fun calculateStats_emptyList_returnsZeroStatsAndZeroCount() {
        val result = SpcCalculator.calculateStats(emptyList())

        assertEquals(0f, result.mean, 0.0001f)
        assertEquals(0f, result.stdDev, 0.0001f)
        assertEquals(0f, result.cpk, 0.0001f)
        assertEquals(0, result.sampleCount)
    }

    @Test
    fun calculateStats_singleRecordInTolerance_returnsZeroStdDevAndPassCpk() {
        val records = listOf(
            MeasurementRecord(id = 1, partName = "Door Panel", flushHeight = 0.05f, gapWidth = 3.5f, isPass = true)
        )

        val result = SpcCalculator.calculateStats(records, targetValue = 0.0f, tolerance = 0.3f)

        assertEquals(0.05f, result.mean, 0.0001f)
        assertEquals(0f, result.stdDev, 0.0001f)
        assertEquals(1.0f, result.cpk, 0.0001f)
        assertEquals(1, result.sampleCount)
    }

    @Test
    fun calculateStats_singleRecordOutOfTolerance_returnsZeroStdDevAndFailCpk() {
        val records = listOf(
            MeasurementRecord(id = 1, partName = "Door Panel", flushHeight = 0.65f, gapWidth = 3.5f, isPass = false)
        )

        val result = SpcCalculator.calculateStats(records, targetValue = 0.0f, tolerance = 0.3f)

        assertEquals(0.65f, result.mean, 0.0001f)
        assertEquals(0f, result.stdDev, 0.0001f)
        assertEquals(0.0f, result.cpk, 0.0001f)
        assertEquals(1, result.sampleCount)
    }

    @Test
    fun calculateStats_nominalDataset_calculatesCorrectMeanStdDevAndCpk() {
        // 5 samples around 0.0 with small variation
        val records = listOf(
            MeasurementRecord(id = 1, partName = "Door Panel", flushHeight = -0.02f, gapWidth = 3.5f, isPass = true),
            MeasurementRecord(id = 2, partName = "Door Panel", flushHeight = 0.00f, gapWidth = 3.5f, isPass = true),
            MeasurementRecord(id = 3, partName = "Door Panel", flushHeight = 0.02f, gapWidth = 3.5f, isPass = true),
            MeasurementRecord(id = 4, partName = "Door Panel", flushHeight = -0.01f, gapWidth = 3.5f, isPass = true),
            MeasurementRecord(id = 5, partName = "Door Panel", flushHeight = 0.01f, gapWidth = 3.5f, isPass = true)
        )

        val result = SpcCalculator.calculateStats(records, targetValue = 0.0f, tolerance = 0.30f)

        // Mean should be exactly 0.0
        assertEquals(0.0f, result.mean, 0.001f)
        assertEquals(5, result.sampleCount)

        // Standard deviation > 0
        assertTrue("StdDev should be positive", result.stdDev > 0.01f)

        // Process is well within tolerance (0.30), Cpk should be healthy (> 1.33 for Six Sigma standards)
        assertTrue("Cpk should indicate capable process (> 1.33)", result.cpk > 1.33f)
    }

    @Test
    fun calculateStats_extremeOutliers_boundsCpkWithinDisplayableRange() {
        // Extreme defect data that could cause extreme negative Cpk
        val records = listOf(
            MeasurementRecord(id = 1, partName = "Door Panel", flushHeight = 15.0f, gapWidth = 3.5f, isPass = false),
            MeasurementRecord(id = 2, partName = "Door Panel", flushHeight = 16.0f, gapWidth = 3.5f, isPass = false),
            MeasurementRecord(id = 3, partName = "Door Panel", flushHeight = 15.5f, gapWidth = 3.5f, isPass = false)
        )

        val result = SpcCalculator.calculateStats(records, targetValue = 0.0f, tolerance = 0.3f)

        // Mean around 15.5mm
        assertEquals(15.5f, result.mean, 0.1f)

        // Cpk should be bounded to -99.99f minimum to avoid UI crash or NaN/Infinity
        assertTrue("Cpk should be >= -99.99f", result.cpk >= -99.99f)
        assertTrue("Cpk should be <= 99.99f", result.cpk <= 99.99f)
    }

    @Test
    fun calculateStats_besselCorrection_usesNMinusOneDivisor() {
        // Values: 2.0, 4.0 -> Mean = 3.0
        // Variance with Bessel (N-1 = 1): (2-3)^2 + (4-3)^2 = 1 + 1 = 2.0. StdDev = sqrt(2) ≈ 1.4142
        val records = listOf(
            MeasurementRecord(id = 1, partName = "A", flushHeight = 2.0f, gapWidth = 3.5f, isPass = false),
            MeasurementRecord(id = 2, partName = "A", flushHeight = 4.0f, gapWidth = 3.5f, isPass = false)
        )

        val result = SpcCalculator.calculateStats(records, targetValue = 0.0f, tolerance = 0.3f)

        assertEquals(3.0f, result.mean, 0.001f)
        assertEquals(1.4142f, result.stdDev, 0.01f)
        assertEquals(2, result.sampleCount)
    }
}
