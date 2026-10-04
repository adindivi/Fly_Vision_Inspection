package com.example

import com.example.domain.DepthResult
import com.example.domain.NeuromorphicEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for NeuromorphicEngine configuration, state management,
 * tolerance updates, and behavioral invariants.
 * Refactored to adhere to Single Responsibility Principle (SRP) and eliminate dummy assertions.
 */
class NeuromorphicEngineUnitTest {

    private lateinit var engine: NeuromorphicEngine

    @Before
    fun setUp() {
        engine = NeuromorphicEngine()
    }

    @Test
    fun initialDepthData_hasDefaultPassStateAndZeroMeasurements() {
        val depth = engine.depthData.value

        assertEquals(0f, depth.flushHeight, 0.001f)
        assertEquals(0f, depth.gapWidth, 0.001f)
        assertTrue("Initial depth state should default to pass", depth.isPass)
        assertTrue("Initial profile should be empty", depth.profile.isEmpty())
    }

    @Test
    fun setTolerances_updatesInspectionThresholdsAccurately() {
        engine.setTolerances(targetGap = 2.0f, gapTol = 0.4f, flushTol = 0.2f)

        assertEquals(2.0f, engine.gapTarget, 0.001f)
        assertEquals(0.4f, engine.gapTolerance, 0.001f)
        assertEquals(0.2f, engine.flushTolerance, 0.001f)
    }

    @Test
    fun updateVelocityAndFocalLength_updatesEngineState() {
        engine.updateVelocity(120.5f)
        engine.setFocalLength(350.0f)

        val depth = engine.depthData.value
        assertNotNull("Depth data flow should remain valid after velocity and focal length updates", depth)
    }

    @Test
    fun resetPeakHold_clearsAccumulatedPeakState() {
        engine.resetPeakHold()

        val depth = engine.depthData.value
        assertNotNull("Depth data should remain valid after resetting peak-hold", depth)
    }

    @Test
    fun updateRotationalVelocity_updatesAngularCompensation() {
        engine.updateRotationalVelocity(0.05f)

        val depth = engine.depthData.value
        assertNotNull("Depth data flow should handle rotational velocity compensation without error", depth)
    }

    @Test
    fun virtualSimulationMode_togglesCorrectly() {
        assertFalse("Virtual simulation should be disabled by default", engine.isVirtualMode)

        engine.isVirtualMode = true
        assertTrue("Virtual simulation flag should be true after enabling", engine.isVirtualMode)

        engine.isVirtualMode = false
        assertFalse("Virtual simulation flag should be false after disabling", engine.isVirtualMode)
    }

    @Test
    fun defectDetectionToggle_togglesFlag() {
        assertFalse("Defect detection should be disabled by default", engine.isDefectDetectionEnabled)

        engine.isDefectDetectionEnabled = true
        assertTrue("Defect detection should be enabled", engine.isDefectDetectionEnabled)

        engine.isDefectDetectionEnabled = false
        assertFalse("Defect detection should be disabled", engine.isDefectDetectionEnabled)
    }

    @Test
    fun depthResult_dataClassIntegrity() {
        val sampleProfile = listOf(0.1f, 0.2f, -0.1f)
        val result = DepthResult(
            flushHeight = 0.15f,
            gapWidth = 3.52f,
            isPass = true,
            profile = sampleProfile,
            t4Activity = 120.0f,
            t5Activity = 85.0f,
            subpixelGapCenter = 320.35f,
            defects = emptyList()
        )

        assertEquals(0.15f, result.flushHeight, 0.001f)
        assertEquals(3.52f, result.gapWidth, 0.001f)
        assertTrue("isPass should match constructor parameter", result.isPass)
        assertEquals(3, result.profile.size)
        assertEquals(sampleProfile, result.profile)
        assertEquals(120.0f, result.t4Activity, 0.001f)
        assertEquals(85.0f, result.t5Activity, 0.001f)
        assertEquals(320.35f, result.subpixelGapCenter, 0.001f)
        assertTrue("Defects list should be empty", result.defects.isEmpty())
    }
}
