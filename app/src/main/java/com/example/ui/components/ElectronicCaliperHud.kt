package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.DepthResult
import com.example.ui.theme.DotBlack
import com.example.ui.theme.DotFailRed
import com.example.ui.theme.DotHeroCharcoal
import com.example.ui.theme.DotNavGraphite
import com.example.ui.theme.DotPassGreen
import com.example.ui.theme.DotSlate
import com.example.ui.theme.DotTagBorder
import com.example.ui.theme.DotViolet
import com.example.ui.theme.DotWarningOrange
import com.example.ui.theme.DotWhite
import com.example.ui.theme.TossGray200
import com.example.ui.theme.TossWhite
import kotlin.math.abs

/**
 * Industrial Spec tolerance status enum for Caliper HUD.
 */
enum class CaliperToleranceStatus {
    OPTIMAL,      // Within ±50% of tolerance target (Peak precision)
    ACCEPTABLE,   // Within allowable tolerance band (Pass)
    OUT_OF_SPEC   // Exceeds upper (USL) or lower (LSL) specification limits
}

/**
 * Pure mathematical functions for Caliper HUD calculations and unit testing.
 */
object CaliperMath {
    /**
     * Maps measurement value across tolerance band:
     * 0.0f = LSL (Target - Tolerance)
     * 0.5f = Nominal Target Center
     * 1.0f = USL (Target + Tolerance)
     */
    fun calculateToleranceProgress(value: Float, target: Float, tolerance: Float): Float {
        if (tolerance <= 0.0001f) return 0.5f
        val lsl = target - tolerance
        val range = 2f * tolerance
        return ((value - lsl) / range).coerceIn(0f, 1f)
    }

    /**
     * Evaluates tolerance compliance status against spec limits.
     */
    fun determineToleranceStatus(value: Float, target: Float, tolerance: Float): CaliperToleranceStatus {
        val deviation = abs(value - target)
        return when {
            deviation <= tolerance * 0.5f -> CaliperToleranceStatus.OPTIMAL
            deviation <= tolerance -> CaliperToleranceStatus.ACCEPTABLE
            else -> CaliperToleranceStatus.OUT_OF_SPEC
        }
    }

    /**
     * Normalizes flush height displacement (-1.0f to +1.0f) relative to max visual scale.
     */
    fun calculateDisplacementVector(flushHeight: Float, maxDisplacementMm: Float = 0.8f): Float {
        if (maxDisplacementMm <= 0.0001f) return 0f
        return (flushHeight / maxDisplacementMm).coerceIn(-1.0f, 1.0f)
    }
}

/**
 * Keyence-grade AR Interactive Electronic Caliper & Real-time Tolerance Band (HUD).
 *
 * Renders:
 * 1. Physical Electronic Caliper Jaws & Vernier micro-scale expanding/contracting dynamically to gap width.
 * 2. Precision Laser Optical Reticle with center alignment circle.
 * 3. Dynamic +/- Delta Z Flush Displacement vector arrows (indicating surface elevation/depression).
 * 4. Real-time Tolerance Band Gauge Bar (USL/Target/LSL with live indicator needle).
 * 5. Tap-to-toggle Compact vs Pro Full HUD modes.
 */
@Composable
fun ElectronicCaliperHud(
    depthResult: DepthResult,
    gapTarget: Float = 3.5f,
    gapTolerance: Float = 0.5f,
    flushTolerance: Float = 0.3f,
    isMeasuring: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isDetailedMode by remember { mutableStateOf(true) }

    val effectiveGap = if (depthResult.gapWidth > 0.05f) depthResult.gapWidth else gapTarget
    val effectiveFlush = depthResult.flushHeight

    val gapStatus = CaliperMath.determineToleranceStatus(effectiveGap, gapTarget, gapTolerance)
    val flushStatus = CaliperMath.determineToleranceStatus(effectiveFlush, 0f, flushTolerance)

    val isOverallPass = if (isMeasuring) {
        depthResult.isPass
    } else {
        gapStatus != CaliperToleranceStatus.OUT_OF_SPEC && flushStatus != CaliperToleranceStatus.OUT_OF_SPEC
    }

    val caliperColor = when {
        !isMeasuring -> DotSlate
        isOverallPass -> DotPassGreen
        else -> DotFailRed
    }

    val statusBadgeColor = when {
        !isMeasuring -> DotSlate
        gapStatus == CaliperToleranceStatus.OPTIMAL -> DotPassGreen
        gapStatus == CaliperToleranceStatus.ACCEPTABLE -> DotViolet
        else -> DotFailRed
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable { isDetailedMode = !isDetailedMode }
    ) {
        // 1. AR Interactive Electronic Caliper Jaws & Vector Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f

            // Optical scale: 1mm ~ 32px (adaptive to screen width)
            val pxPerMm = (canvasWidth * 0.08f).coerceIn(24f, 42f)
            val halfGapPx = (effectiveGap * pxPerMm / 2f).coerceIn(24f, canvasWidth * 0.42f)

            val leftJawX = centerX - halfGapPx
            val rightJawX = centerX + halfGapPx

            val jawHeight = 90.dp.toPx()
            val jawTop = centerY - jawHeight / 2f
            val jawBottom = centerY + jawHeight / 2f

            // A. Sub-millimeter Crosshair Reticle
            val reticleLength = 36.dp.toPx()
            val reticleColor = Color.White.copy(alpha = 0.45f)

            // Horizontal reticle (Left & Right segments with center gap)
            drawLine(
                color = reticleColor,
                start = Offset(centerX - reticleLength, centerY),
                end = Offset(centerX - 10.dp.toPx(), centerY),
                strokeWidth = 1.2f
            )
            drawLine(
                color = reticleColor,
                start = Offset(centerX + 10.dp.toPx(), centerY),
                end = Offset(centerX + reticleLength, centerY),
                strokeWidth = 1.2f
            )

            // Vertical reticle (Top & Bottom segments)
            drawLine(
                color = reticleColor,
                start = Offset(centerX, centerY - reticleLength),
                end = Offset(centerX, centerY - 10.dp.toPx()),
                strokeWidth = 1.2f
            )
            drawLine(
                color = reticleColor,
                start = Offset(centerX, centerY + 10.dp.toPx()),
                end = Offset(centerX, centerY + reticleLength),
                strokeWidth = 1.2f
            )

            // Center targeting optical circle
            drawCircle(
                color = reticleColor,
                radius = 7.dp.toPx(),
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.2f)
            )

            // B. Caliper Horizontal Beam & Vernier Scale ticks
            val beamY = jawTop
            drawLine(
                color = caliperColor.copy(alpha = 0.75f),
                start = Offset(leftJawX, beamY),
                end = Offset(rightJawX, beamY),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )

            // Micro vernier tick marks along the horizontal beam
            val tickCount = 9
            val tickStep = (rightJawX - leftJawX) / (tickCount + 1)
            for (i in 1..tickCount) {
                val tickX = leftJawX + i * tickStep
                val isMajorTick = (i == (tickCount + 1) / 2)
                val tickLen = if (isMajorTick) 8.dp.toPx() else 4.dp.toPx()
                drawLine(
                    color = caliperColor.copy(alpha = 0.85f),
                    start = Offset(tickX, beamY),
                    end = Offset(tickX, beamY + tickLen),
                    strokeWidth = if (isMajorTick) 1.8f else 1f
                )
            }

            // C. Caliper Left Jaw & Tooth Tip
            // Main vertical blade
            drawLine(
                color = caliperColor,
                start = Offset(leftJawX, jawTop),
                end = Offset(leftJawX, jawBottom),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
            // Inward teeth
            drawLine(
                color = caliperColor,
                start = Offset(leftJawX, jawTop),
                end = Offset(leftJawX + 10.dp.toPx(), jawTop),
                strokeWidth = 2f
            )
            drawLine(
                color = caliperColor,
                start = Offset(leftJawX, jawBottom),
                end = Offset(leftJawX + 10.dp.toPx(), jawBottom),
                strokeWidth = 2f
            )

            // D. Caliper Right Jaw & Tooth Tip
            // Main vertical blade
            drawLine(
                color = caliperColor,
                start = Offset(rightJawX, jawTop),
                end = Offset(rightJawX, jawBottom),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
            // Inward teeth
            drawLine(
                color = caliperColor,
                start = Offset(rightJawX, jawTop),
                end = Offset(rightJawX - 10.dp.toPx(), jawTop),
                strokeWidth = 2f
            )
            drawLine(
                color = caliperColor,
                start = Offset(rightJawX, jawBottom),
                end = Offset(rightJawX - 10.dp.toPx(), jawBottom),
                strokeWidth = 2f
            )

            // E. Dynamic Flush Displacement Vector Arrow (+/- Delta Z)
            if (abs(effectiveFlush) >= 0.03f) {
                val maxArrowLen = 32.dp.toPx()
                val normalizedDisplacement = CaliperMath.calculateDisplacementVector(effectiveFlush, flushTolerance * 1.5f)
                val arrowLen = (abs(normalizedDisplacement) * maxArrowLen).coerceIn(12.dp.toPx(), maxArrowLen)

                val isElevated = effectiveFlush > 0
                val arrowTipY = if (isElevated) centerY - arrowLen else centerY + arrowLen
                val arrowBaseY = centerY
                val arrowColor = if (abs(effectiveFlush) <= flushTolerance) DotPassGreen else DotFailRed

                // Arrow shaft
                drawLine(
                    color = arrowColor,
                    start = Offset(centerX, arrowBaseY),
                    end = Offset(centerX, arrowTipY),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )

                // Arrow head path
                val arrowHead = Path()
                val headSize = 6.dp.toPx()
                if (isElevated) {
                    arrowHead.moveTo(centerX, arrowTipY)
                    arrowHead.lineTo(centerX - headSize, arrowTipY + headSize)
                    arrowHead.lineTo(centerX + headSize, arrowTipY + headSize)
                } else {
                    arrowHead.moveTo(centerX, arrowTipY)
                    arrowHead.lineTo(centerX - headSize, arrowTipY - headSize)
                    arrowHead.lineTo(centerX + headSize, arrowTipY - headSize)
                }
                arrowHead.close()
                drawPath(path = arrowHead, color = arrowColor)
            }
        }

        // 2. Floating Top Caliper Reading Pill (Keyence Standard)
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 120.dp),
            color = DotHeroCharcoal.copy(alpha = 0.90f),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, TossGray200.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(statusBadgeColor, CircleShape)
                )
                Text(
                    text = "GAP ${String.format("%.2f", effectiveGap)}mm",
                    color = DotWhite,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "·",
                    color = DotSlate,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "FLUSH ${String.format("%+.2f", effectiveFlush)}mm",
                    color = if (abs(effectiveFlush) <= flushTolerance) DotPassGreen else DotFailRed,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // 3. Bottom Real-time Tolerance Band HUD Gauge (Detailed Pro HUD)
        AnimatedVisibility(
            visible = isDetailedMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                color = DotHeroCharcoal.copy(alpha = 0.92f),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, TossGray200.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Row 1: Header - Specification Title & Compliance Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "KEYENCE AR CALIPER",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = DotViolet,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "PRO HUD",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = DotSlate,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Surface(
                            color = statusBadgeColor.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.dp, statusBadgeColor.copy(alpha = 0.8f))
                        ) {
                            Text(
                                text = when {
                                    !isMeasuring -> "STANDBY"
                                    isOverallPass -> "IN-SPEC (PASS)"
                                    else -> "OUT-OF-SPEC (FAIL)"
                                },
                                color = statusBadgeColor,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.5.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: Real-time Tolerance Band Gauge Bar
                    val lsl = gapTarget - gapTolerance
                    val usl = gapTarget + gapTolerance
                    val progress = CaliperMath.calculateToleranceProgress(effectiveGap, gapTarget, gapTolerance)

                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Gauge Track & Needle Canvas
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                        ) {
                            val trackWidth = size.width
                            val trackHeight = size.height
                            val trackCorner = trackHeight / 2f

                            // Background track (Out-of-spec dark red zones)
                            drawRoundRect(
                                color = DotFailRed.copy(alpha = 0.25f),
                                size = Size(trackWidth, trackHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackCorner, trackCorner)
                            )

                            // Tolerance In-Spec Green Safety Zone (25% to 75% region of nominal target)
                            val safeLeft = trackWidth * 0.15f
                            val safeWidth = trackWidth * 0.70f
                            drawRect(
                                color = DotPassGreen.copy(alpha = 0.35f),
                                topLeft = Offset(safeLeft, 0f),
                                size = Size(safeWidth, trackHeight)
                            )

                            // Nominal Target Center Notch Line
                            val centerNotchX = trackWidth * 0.5f
                            drawLine(
                                color = Color.White.copy(alpha = 0.8f),
                                start = Offset(centerNotchX, 0f),
                                end = Offset(centerNotchX, trackHeight),
                                strokeWidth = 1.5f
                            )

                            // Dynamic Indicator Needle
                            val needleX = (trackWidth * progress).coerceIn(4.dp.toPx(), trackWidth - 4.dp.toPx())
                            drawCircle(
                                color = statusBadgeColor,
                                radius = trackHeight / 2f + 1.dp.toPx(),
                                center = Offset(needleX, trackHeight / 2f)
                            )
                            drawCircle(
                                color = DotWhite,
                                radius = 2.5.dp.toPx(),
                                center = Offset(needleX, trackHeight / 2f)
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        // Gauge Scale Axis Labels (LSL, TARGET, USL)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LSL ${String.format("%.2f", lsl)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = DotSlate,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "TARGET ${String.format("%.2f", gapTarget)}mm",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = DotWhite.copy(alpha = 0.9f),
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "USL ${String.format("%.2f", usl)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = DotSlate,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 3: Flush Step Metric & Quick Hint
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "단차 변위:",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = DotSlate,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "${String.format("%+.3f", effectiveFlush)} mm",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp
                                ),
                                color = if (abs(effectiveFlush) <= flushTolerance) DotPassGreen else DotFailRed,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "(허용 ±${String.format("%.2f", flushTolerance)})",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = DotSlate,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Text(
                            text = "탭: HUD 토글",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = DotSlate.copy(alpha = 0.8f),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
