package com.example

import com.example.domain.DefectDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Prophesee Metavision-inspired real-time surface micro-defect detector.
 */
class DefectDetectorUnitTest {

    @Test
    fun defectDetector_cleanFrame_returnsNoDefects() {
        val detector = DefectDetector(pixelToMm = 0.05f)
        val w = 100
        val h = 100
        val frame1 = ByteArray(w * h) { 128.toByte() }
        val frame2 = ByteArray(w * h) { 128.toByte() }

        detector.detect(frame1, w, h)
        val defects = detector.detect(frame2, w, h)

        assertTrue("Clean uniform frame should produce zero defects", defects.isEmpty())
    }

    @Test
    fun defectDetector_linearScratch_classifiesAsScratch() {
        val detector = DefectDetector(pixelToMm = 0.05f)
        val w = 100
        val h = 100
        val frame1 = ByteArray(w * h) { 100.toByte() }
        val frame2 = ByteArray(w * h) { 100.toByte() }

        // Inject a linear scratch: 20 pixels tall, 2 pixels wide (aspect ratio = 10.0 >= 2.2)
        for (y in 40 until 60) {
            for (x in 50..51) {
                frame2[y * w + x] = 220.toByte()
            }
        }

        detector.detect(frame1, w, h)
        val defects = detector.detect(frame2, w, h)

        assertFalse("Elongated defect should be detected", defects.isEmpty())
        val defect = defects.first()
        assertEquals("SCRATCH", defect.defectType)
        assertTrue("Scratch length should be greater than 0.5mm", defect.lengthMm >= 0.5f)
    }

    @Test
    fun defectDetector_pointDent_classifiesAsPit() {
        val detector = DefectDetector(pixelToMm = 0.05f)
        val w = 100
        val h = 100
        val frame1 = ByteArray(w * h) { 100.toByte() }
        val frame2 = ByteArray(w * h) { 100.toByte() }

        // Inject a square pit/dent: 4x4 pixels (aspect ratio = 1.0 < 2.2, area = 16px >= 6px)
        for (y in 48..51) {
            for (x in 48..51) {
                frame2[y * w + x] = 230.toByte()
            }
        }

        detector.detect(frame1, w, h)
        val defects = detector.detect(frame2, w, h)

        assertFalse("Point dent should be detected", defects.isEmpty())
        val defect = defects.first()
        assertEquals("PIT", defect.defectType)
    }

    @Test
    fun defectDetector_largeDefect_classifiesAsCritical() {
        val detector = DefectDetector(pixelToMm = 0.05f)
        val w = 100
        val h = 100
        val frame1 = ByteArray(w * h) { 80.toByte() }
        val frame2 = ByteArray(w * h) { 80.toByte() }

        // Inject large scratch: 30 pixels tall (30 * 0.05 = 1.5mm >= 1.0mm -> CRITICAL)
        for (y in 30 until 60) {
            for (x in 30..31) {
                frame2[y * w + x] = 240.toByte()
            }
        }

        detector.detect(frame1, w, h)
        val defects = detector.detect(frame2, w, h)

        assertFalse(defects.isEmpty())
        assertEquals("CRITICAL", defects.first().severity)
    }

    @Test
    fun defectDetector_excludeGap_masksOutGapArea() {
        val detector = DefectDetector(pixelToMm = 0.05f)
        val w = 100
        val h = 100
        val frame1 = ByteArray(w * h) { 120.toByte() }
        val frame2 = ByteArray(w * h) { 120.toByte() }

        // Inject a dark slit at x = 50 (gap center)
        for (y in 20 until 80) {
            for (x in 48..52) {
                frame2[y * w + x] = 10.toByte()
            }
        }

        detector.detect(frame1, w, h)
        // With excludeGapX = 50, excludeGapWidth = 10 -> masked out
        val defects = detector.detect(frame2, w, h, excludeGapX = 50, excludeGapWidth = 10)

        assertTrue("Defect in the masked gap region must be excluded", defects.isEmpty())
    }
}
