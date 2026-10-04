package com.example

import com.example.domain.DefectDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for Prophesee Metavision-inspired real-time surface micro-defect detector.
 * Refactored to eliminate repetitive pixel-injection loops (DRY) and enforce clean SRP test structure.
 */
class DefectDetectorUnitTest {

    private lateinit var detector: DefectDetector

    @Before
    fun setUp() {
        detector = DefectDetector(pixelToMm = PIXEL_TO_MM)
    }

    @Test
    fun defectDetector_cleanFrame_returnsNoDefects() {
        val frame1 = createUniformFrame(value = 128)
        val frame2 = createUniformFrame(value = 128)

        detector.detect(frame1, FRAME_WIDTH, FRAME_HEIGHT)
        val defects = detector.detect(frame2, FRAME_WIDTH, FRAME_HEIGHT)

        assertTrue("Clean uniform frame should produce zero defects", defects.isEmpty())
    }

    @Test
    fun defectDetector_linearScratch_classifiesAsScratch() {
        val frame1 = createUniformFrame(value = 100)
        val frame2 = createUniformFrame(value = 100).also {
            // Inject a linear scratch: 20 pixels tall, 2 pixels wide (aspect ratio = 10.0 >= 2.2)
            injectRegion(it, yRange = 40 until 60, xRange = 50..51, intensity = 220)
        }

        detector.detect(frame1, FRAME_WIDTH, FRAME_HEIGHT)
        val defects = detector.detect(frame2, FRAME_WIDTH, FRAME_HEIGHT)

        assertFalse("Elongated defect should be detected", defects.isEmpty())
        val defect = defects.first()
        assertEquals("SCRATCH", defect.defectType)
        assertTrue("Scratch length should be greater than 0.5mm", defect.lengthMm >= 0.5f)
    }

    @Test
    fun defectDetector_pointDent_classifiesAsPit() {
        val frame1 = createUniformFrame(value = 100)
        val frame2 = createUniformFrame(value = 100).also {
            // Inject a square pit/dent: 4x4 pixels (aspect ratio = 1.0 < 2.2, area = 16px >= 6px)
            injectRegion(it, yRange = 48..51, xRange = 48..51, intensity = 230)
        }

        detector.detect(frame1, FRAME_WIDTH, FRAME_HEIGHT)
        val defects = detector.detect(frame2, FRAME_WIDTH, FRAME_HEIGHT)

        assertFalse("Point dent should be detected", defects.isEmpty())
        val defect = defects.first()
        assertEquals("PIT", defect.defectType)
    }

    @Test
    fun defectDetector_largeDefect_classifiesAsCritical() {
        val frame1 = createUniformFrame(value = 80)
        val frame2 = createUniformFrame(value = 80).also {
            // Inject large scratch: 30 pixels tall (30 * 0.05 = 1.5mm >= 1.0mm -> CRITICAL)
            injectRegion(it, yRange = 30 until 60, xRange = 30..31, intensity = 240)
        }

        detector.detect(frame1, FRAME_WIDTH, FRAME_HEIGHT)
        val defects = detector.detect(frame2, FRAME_WIDTH, FRAME_HEIGHT)

        assertFalse("Large defect should be detected", defects.isEmpty())
        assertEquals("CRITICAL", defects.first().severity)
    }

    @Test
    fun defectDetector_excludeGap_masksOutGapArea() {
        val frame1 = createUniformFrame(value = 120)
        val frame2 = createUniformFrame(value = 120).also {
            // Inject a dark slit at x = 50 (gap center)
            injectRegion(it, yRange = 20 until 80, xRange = 48..52, intensity = 10)
        }

        detector.detect(frame1, FRAME_WIDTH, FRAME_HEIGHT)
        // With excludeGapX = 50, excludeGapWidth = 10 -> masked out
        val defects = detector.detect(frame2, FRAME_WIDTH, FRAME_HEIGHT, excludeGapX = 50, excludeGapWidth = 10)

        assertTrue("Defect in the masked gap region must be excluded", defects.isEmpty())
    }

    companion object {
        private const val FRAME_WIDTH = 100
        private const val FRAME_HEIGHT = 100
        private const val PIXEL_TO_MM = 0.05f

        private fun createUniformFrame(
            width: Int = FRAME_WIDTH,
            height: Int = FRAME_HEIGHT,
            value: Int = 128
        ): ByteArray = ByteArray(width * height) { value.toByte() }

        private fun injectRegion(
            frame: ByteArray,
            width: Int = FRAME_WIDTH,
            yRange: IntRange,
            xRange: IntRange,
            intensity: Int
        ) {
            for (y in yRange) {
                for (x in xRange) {
                    frame[y * width + x] = intensity.toByte()
                }
            }
        }
    }
}
