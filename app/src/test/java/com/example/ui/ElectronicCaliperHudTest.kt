package com.example.ui

import com.example.ui.components.CaliperMath
import com.example.ui.components.CaliperToleranceStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure JVM Unit tests for ElectronicCaliperHud mathematical models and tolerance band gauge calculations.
 */
class ElectronicCaliperHudTest {

    @Test
    fun calculateToleranceProgress_nominalTarget_returnsCenterHalf() {
        val progress = CaliperMath.calculateToleranceProgress(
            value = 3.5f,
            target = 3.5f,
            tolerance = 0.5f
        )
        assertEquals(0.5f, progress, 0.001f)
    }

    @Test
    fun calculateToleranceProgress_atLowerSpecLimit_returnsZero() {
        val progress = CaliperMath.calculateToleranceProgress(
            value = 3.0f, // 3.5 - 0.5 = LSL
            target = 3.5f,
            tolerance = 0.5f
        )
        assertEquals(0.0f, progress, 0.001f)
    }

    @Test
    fun calculateToleranceProgress_atUpperSpecLimit_returnsOne() {
        val progress = CaliperMath.calculateToleranceProgress(
            value = 4.0f, // 3.5 + 0.5 = USL
            target = 3.5f,
            tolerance = 0.5f
        )
        assertEquals(1.0f, progress, 0.001f)
    }

    @Test
    fun calculateToleranceProgress_outOfRange_isCoercedWithinZeroAndOne() {
        val underFlow = CaliperMath.calculateToleranceProgress(
            value = 2.1f,
            target = 3.5f,
            tolerance = 0.5f
        )
        assertEquals(0.0f, underFlow, 0.001f)

        val overFlow = CaliperMath.calculateToleranceProgress(
            value = 5.2f,
            target = 3.5f,
            tolerance = 0.5f
        )
        assertEquals(1.0f, overFlow, 0.001f)
    }

    @Test
    fun determineToleranceStatus_optimalWithinHalfTolerance() {
        val statusExact = CaliperMath.determineToleranceStatus(
            value = 3.5f,
            target = 3.5f,
            tolerance = 0.5f
        )
        assertEquals(CaliperToleranceStatus.OPTIMAL, statusExact)

        val statusHalf = CaliperMath.determineToleranceStatus(
            value = 3.65f, // +0.15mm <= 0.25mm
            target = 3.5f,
            tolerance = 0.5f
        )
        assertEquals(CaliperToleranceStatus.OPTIMAL, statusHalf)
    }

    @Test
    fun determineToleranceStatus_acceptableWithinFullTolerance() {
        val status = CaliperMath.determineToleranceStatus(
            value = 3.85f, // +0.35mm > 0.25mm but <= 0.50mm
            target = 3.5f,
            tolerance = 0.5f
        )
        assertEquals(CaliperToleranceStatus.ACCEPTABLE, status)
    }

    @Test
    fun determineToleranceStatus_outOfSpecWhenViolatingLimits() {
        val statusAbove = CaliperMath.determineToleranceStatus(
            value = 4.15f, // > 4.0mm
            target = 3.5f,
            tolerance = 0.5f
        )
        assertEquals(CaliperToleranceStatus.OUT_OF_SPEC, statusAbove)

        val statusBelow = CaliperMath.determineToleranceStatus(
            value = 2.80f, // < 3.0mm
            target = 3.5f,
            tolerance = 0.5f
        )
        assertEquals(CaliperToleranceStatus.OUT_OF_SPEC, statusBelow)
    }

    @Test
    fun calculateDisplacementVector_calculatesCorrectNormalizedDirectionAndCoercion() {
        val zeroVector = CaliperMath.calculateDisplacementVector(0f, 0.8f)
        assertEquals(0f, zeroVector, 0.001f)

        val positiveVector = CaliperMath.calculateDisplacementVector(0.4f, 0.8f)
        assertEquals(0.5f, positiveVector, 0.001f)

        val negativeVector = CaliperMath.calculateDisplacementVector(-0.4f, 0.8f)
        assertEquals(-0.5f, negativeVector, 0.001f)

        val clampedPositive = CaliperMath.calculateDisplacementVector(1.5f, 0.8f)
        assertEquals(1.0f, clampedPositive, 0.001f)

        val clampedNegative = CaliperMath.calculateDisplacementVector(-1.5f, 0.8f)
        assertEquals(-1.0f, clampedNegative, 0.001f)
    }
}
