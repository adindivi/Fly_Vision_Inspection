package com.example.domain

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Prophesee Metavision-inspired real-time surface micro-defect metadata.
 */
data class SurfaceDefect(
    val defectType: String,        // "SCRATCH" (linear scratch) or "PIT" (point dent)
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val centerX: Int,
    val centerY: Int,
    val lengthMm: Float,
    val areaMm2: Float,
    val severity: String,          // "CRITICAL" or "MINOR"
    val contrastPeak: Float
)

/**
 * High-performance on-device surface micro-defect detector.
 * Uses spatiotemporal event bursts, exponential trail decay, and geometric shape analysis.
 */
class DefectDetector(
    var pixelToMm: Float = 0.05f,
    private val trailDecay: Float = 0.65f,
    private val minDefectAreaPx: Int = 6,
    private val burstThreshold: Float = 14.0f
) {
    private var prevFrame: FloatArray? = null
    private var trailMap: FloatArray? = null
    private var bufferWidth = 0
    private var bufferHeight = 0

    fun reset() {
        prevFrame = null
        trailMap = null
    }

    /**
     * Analyzes a downscaled grayscale frame for surface defects.
     * @param grayBytes 8-bit grayscale pixel array
     * @param width frame width
     * @param height frame height
     * @param excludeGapX center X of the panel gap to exclude from defect detection
     * @param excludeGapWidth half-width in pixels around the gap to mask out
     */
    fun detect(
        grayBytes: ByteArray,
        width: Int,
        height: Int,
        excludeGapX: Int = -1,
        excludeGapWidth: Int = 20
    ): List<SurfaceDefect> {
        val size = width * height
        if (size == 0) return emptyList()

        if (prevFrame == null || bufferWidth != width || bufferHeight != height) {
            prevFrame = FloatArray(size) { (grayBytes[it].toInt() and 0xFF).toFloat() }
            trailMap = FloatArray(size)
            bufferWidth = width
            bufferHeight = height
            return emptyList()
        }

        val prev = prevFrame!!
        val trail = trailMap!!

        val binaryMask = BooleanArray(size)

        // 1. Spatiotemporal difference & Exponential trail decay
        val oneMinusDecay = 1.0f - trailDecay
        val excludeLeft = if (excludeGapX >= 0) max(0, excludeGapX - excludeGapWidth) else -1
        val excludeRight = if (excludeGapX >= 0) min(width - 1, excludeGapX + excludeGapWidth) else -1

        for (y in 0 until height) {
            val rowOffset = y * width
            val isGapRow = excludeLeft in 0..excludeRight

            for (x in 0 until width) {
                val idx = rowOffset + x

                // Mask out gap slit area so the gap itself is not flagged as a defect
                if (isGapRow && x in excludeLeft..excludeRight) {
                    trail[idx] = 0f
                    prev[idx] = (grayBytes[idx].toInt() and 0xFF).toFloat()
                    continue
                }

                val currVal = (grayBytes[idx].toInt() and 0xFF).toFloat()
                val diff = abs(currVal - prev[idx])
                prev[idx] = currVal

                // Noise gating: ignore thermal camera noise < 3.5
                val burst = if (diff < 3.5f) 0.0f else diff

                // Prophesee-style exponential trail decay
                val updatedTrail = trail[idx] * trailDecay + burst * oneMinusDecay
                trail[idx] = updatedTrail

                if (updatedTrail >= burstThreshold) {
                    binaryMask[idx] = true
                }
            }
        }

        // 2. Connected component labeling (Lightweight Breadth-First-Search flood fill)
        val visited = BooleanArray(size)
        val defects = mutableListOf<SurfaceDefect>()

        val qx = IntArray(size)
        val qy = IntArray(size)

        for (y in 2 until height - 2) {
            val rowOffset = y * width
            for (x in 2 until width - 2) {
                val idx = rowOffset + x
                if (binaryMask[idx] && !visited[idx]) {
                    // Flood fill connected component
                    visited[idx] = true
                    var head = 0
                    var tail = 0
                    qx[tail] = x
                    qy[tail] = y
                    tail++

                    var minX = x
                    var maxX = x
                    var minY = y
                    var maxY = y
                    var peakContrast = trail[idx]

                    while (head < tail) {
                        val cx = qx[head]
                        val cy = qy[head]
                        head++

                        val cPeak = trail[cy * width + cx]
                        if (cPeak > peakContrast) peakContrast = cPeak

                        // 4-neighborhood
                        val nxArr = intArrayOf(cx - 1, cx + 1, cx, cx)
                        val nyArr = intArrayOf(cy, cy, cy - 1, cy + 1)

                        for (i in 0 until 4) {
                            val nx = nxArr[i]
                            val ny = nyArr[i]
                            if (nx in 1 until width - 1 && ny in 1 until height - 1) {
                                val nIdx = ny * width + nx
                                if (binaryMask[nIdx] && !visited[nIdx]) {
                                    visited[nIdx] = true
                                    if (nx < minX) minX = nx
                                    if (nx > maxX) maxX = nx
                                    if (ny < minY) minY = ny
                                    if (ny > maxY) maxY = ny

                                    if (tail < size) {
                                        qx[tail] = nx
                                        qy[tail] = ny
                                        tail++
                                    }
                                }
                            }
                        }
                    }

                    val areaPx = tail
                    val bw = maxX - minX + 1
                    val bh = maxY - minY + 1

                    // Filter out microscopic noise or screen-spanning artifacts
                    if (areaPx >= minDefectAreaPx && bw < width * 0.7f && bh < height * 0.7f) {
                        val maxDim = max(bw, bh)
                        val minDim = max(1, min(bw, bh))
                        val aspectRatio = maxDim.toFloat() / minDim.toFloat()

                        val lengthMm = maxDim * pixelToMm
                        val areaMm2 = areaPx * (pixelToMm * pixelToMm)

                        // Linear scratch vs point pit classification
                        val defectType = if (aspectRatio >= 2.2f || maxDim > 12) "SCRATCH" else "PIT"
                        val isCritical = lengthMm >= 1.0f || areaMm2 >= 0.30f
                        val severity = if (isCritical) "CRITICAL" else "MINOR"

                        defects.add(
                            SurfaceDefect(
                                defectType = defectType,
                                x = minX,
                                y = minY,
                                width = bw,
                                height = bh,
                                centerX = minX + bw / 2,
                                centerY = minY + bh / 2,
                                lengthMm = lengthMm,
                                areaMm2 = areaMm2,
                                severity = severity,
                                contrastPeak = peakContrast
                            )
                        )
                    }
                }
            }
        }

        return defects
    }
}
