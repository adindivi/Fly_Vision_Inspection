package com.example

import com.example.domain.DepthResult
import com.example.domain.NeuromorphicEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for NeuromorphicEngine configuration and state management.
 */
class NeuromorphicEngineUnitTest {

    @Test
    fun engine_initialDepthData_hasDefaultPassState() {
        val engine = NeuromorphicEngine()
        val depth = engine.depthData.value

        assertEquals(0f, depth.flushHeight, 0.001f)
        assertEquals(0f, depth.gapWidth, 0.001f)
        assertTrue("Initial depth state should default to pass", depth.isPass)
        assertTrue("Initial profile should be empty", depth.profile.isEmpty())
    }

    @Test
    fun engine_updateVelocityAndFocalLength_doesNotThrow() {
        val engine = NeuromorphicEngine()

        engine.updateVelocity(120.5f)
        engine.setFocalLength(350.0f)

        assertNotNull(engine.depthData.value)
    }

    @Test
    fun depthResult_dataClassIntegrity() {
        val sampleProfile = listOf(0.1f, 0.2f, -0.1f)
        val result = DepthResult(
            flushHeight = 0.15f,
            gapWidth = 3.52f,
            isPass = true,
            profile = sampleProfile
        )

        assertEquals(0.15f, result.flushHeight, 0.001f)
        assertEquals(3.52f, result.gapWidth, 0.001f)
        assertTrue(result.isPass)
        assertEquals(3, result.profile.size)
        assertEquals(sampleProfile, result.profile)
    }

    @Test
    fun engine_resetPeakHold_doesNotThrow() {
        val engine = NeuromorphicEngine()
        engine.resetPeakHold()
        assertNotNull(engine.depthData.value)
    }

    @Test
    fun engine_setTolerances_updatesValues() {
        val engine = NeuromorphicEngine()
        engine.setTolerances(2.0f, 0.4f, 0.2f)

        assertEquals(2.0f, engine.gapTarget, 0.001f)
        assertEquals(0.4f, engine.gapTolerance, 0.001f)
        assertEquals(0.2f, engine.flushTolerance, 0.001f)
    }

    @Test
    fun engine_updateRotationalVelocity_doesNotThrow() {
        val engine = NeuromorphicEngine()
        engine.updateRotationalVelocity(0.05f)
        assertNotNull(engine.depthData.value)
    }
}
