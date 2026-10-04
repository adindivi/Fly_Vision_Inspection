package com.example

import com.example.data.MeasurementRecord
import com.example.domain.SpcCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rigorous unit tests for SpcCalculator statistical process control calculations.
 * Validates Bessel's correction, Cpk bounding, zero-division safety, and edge cases.
 * Refactored to eliminate repetitive MeasurementRecord instantiation boilerplate (DRY).
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
        val records = listOf(createRecord(flushHeight = 0.05f, isPass = true))

        val result = SpcCalculator.calculateStats(records, targetValue = 0.0f, tolerance = 0.3f)

        assertEquals(0.05f, result.mean, 0.0001f)
        assertEquals(0f, result.stdDev, 0.0001f)
        assertEquals(1.0f, result.cpk, 0.0001f)
        assertEquals(1, result.sampleCount)
    }

    @Test
    fun calculateStats_singleRecordOutOfTolerance_returnsZeroStdDevAndFailCpk() {
        val records = listOf(createRecord(flushHeight = 0.65f, isPass = false))

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
            createRecord(id = 1, flushHeight = -0.02f, isPass = true),
            createRecord(id = 2, flushHeight = 0.00f, isPass = true),
            createRecord(id = 3, flushHeight = 0.02f, isPass = true),
            createRecord(id = 4, flushHeight = -0.01f, isPass = true),
            createRecord(id = 5, flushHeight = 0.01f, isPass = true)
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
            createRecord(id = 1, flushHeight = 15.0f, isPass = false),
            createRecord(id = 2, flushHeight = 16.0f, isPass = false),
            createRecord(id = 3, flushHeight = 15.5f, isPass = false)
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
            createRecord(id = 1, partName = "A", flushHeight = 2.0f, isPass = false),
            createRecord(id = 2, partName = "A", flushHeight = 4.0f, isPass = false)
        )

        val result = SpcCalculator.calculateStats(records, targetValue = 0.0f, tolerance = 0.3f)

        assertEquals(3.0f, result.mean, 0.001f)
        assertEquals(1.4142f, result.stdDev, 0.01f)
        assertEquals(2, result.sampleCount)
    }

    companion object {
        private fun createRecord(
            flushHeight: Float,
            isPass: Boolean = true,
            id: Int = 1,
            gapWidth: Float = 3.5f,
            partName: String = "Door Panel"
        ): MeasurementRecord = MeasurementRecord(
            id = id,
            partName = partName,
            flushHeight = flushHeight,
            gapWidth = gapWidth,
            isPass = isPass
        )
    }
}
