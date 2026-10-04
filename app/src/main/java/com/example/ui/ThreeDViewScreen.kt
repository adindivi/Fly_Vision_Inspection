package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DotBlack
import com.example.ui.theme.DotCardMist
import com.example.ui.theme.DotFailRed
import com.example.ui.theme.DotHeroCharcoal
import com.example.ui.theme.DotNavGraphite
import com.example.ui.theme.DotOffWhite
import com.example.ui.theme.DotPassGreen
import com.example.ui.theme.DotSlate
import com.example.ui.theme.DotTagBorder
import com.example.ui.theme.DotViolet
import com.example.ui.theme.DotWhite
import com.example.ui.theme.TossGray200
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * ThreeDViewScreen: 3D Isometric Surface Topology Visualization
 * Displays multi-angle wireframe mesh of vehicle panel gap/flush profiles.
 */
@Composable
fun ThreeDViewScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val depthResult by viewModel.depthData.collectAsState()
    val flushTolerance by viewModel.flushTolerance.collectAsState()
    var viewAngleDeg by remember { mutableFloatStateOf(35f) }
    var zScale by remember { mutableFloatStateOf(3.0f) }

    // Synthesize profile if empty or all zero so preview is always informative
    val profilePoints = remember(depthResult.profile, depthResult.flushHeight) {
        if (depthResult.profile.isNotEmpty() && depthResult.profile.any { abs(it) > 0.001f }) {
            depthResult.profile
        } else {
            // Generate synthetic baseline step curve based on current flushHeight or nominal 0.35mm step
            val nominalFlush = if (abs(depthResult.flushHeight) > 0.01f) depthResult.flushHeight else 0.35f
            List(64) { i ->
                val xNorm = (i - 32) / 10.0f
                val step = (nominalFlush / 2f) * tanh(xNorm)
                val wave = 0.03f * sin(i * 0.4f)
                step + wave
            }
        }
    }

    // Flatness / RMS deviation calculation
    val flatnessRms = remember(profilePoints) {
        if (profilePoints.isEmpty()) 0f
        else {
            val mean = profilePoints.average().toFloat()
            sqrt(profilePoints.map { (it - mean) * (it - mean) }.average()).toFloat()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DotOffWhite),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header & Status Card - 42dot Flat Tint Standard (0px Radius, Card Mist #f6f6f9)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DotCardMist),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Row 1: Title & Pass/Fail Badge (22px Pill)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "3D Surface Topology",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = DotBlack,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (depthResult.profile.isNotEmpty()) "Optical Flow 64-Point Mesh" else "Baseline Preview Simulation",
                                style = MaterialTheme.typography.bodySmall,
                                color = DotSlate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = if (depthResult.isPass) DotPassGreen else DotFailRed,
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Text(
                                text = if (depthResult.isPass) "PASS" else "FAIL",
                                color = DotWhite,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4-Grid Metric Indicators (Row 1)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MetricBadgeItem(
                            label = "단차 높이",
                            value = "${String.format("%+.3f", depthResult.flushHeight)}mm",
                            highlightColor = if (depthResult.isPass) DotPassGreen else DotFailRed,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadgeItem(
                            label = "틈새 간격",
                            value = "${String.format("%.2f", depthResult.gapWidth)}mm",
                            highlightColor = DotViolet,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadgeItem(
                            label = "표면 평탄도",
                            value = "${String.format("%.3f", flatnessRms)}mm",
                            highlightColor = DotSlate,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadgeItem(
                            label = "해상도",
                            value = "${profilePoints.size} Pts",
                            highlightColor = DotBlack,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4-Grid Metric Indicators (Row 2: MaleCNS Neural Firing Rates & Subpixel Ridge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MetricBadgeItem(
                            label = "T4 광택 뉴런",
                            value = "${depthResult.t4Activity.toInt()} Hz",
                            highlightColor = DotSlate,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadgeItem(
                            label = "T5 음영 뉴런",
                            value = "${depthResult.t5Activity.toInt()} Hz",
                            highlightColor = DotSlate,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadgeItem(
                            label = "서브픽셀 능선",
                            value = "${String.format("%.1f", depthResult.subpixelGapCenter)} px",
                            highlightColor = DotViolet,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadgeItem(
                            label = "표면 결함",
                            value = "${depthResult.defects.size} 건",
                            highlightColor = if (depthResult.defects.isEmpty()) DotPassGreen else DotFailRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            // 3D Isometric Surface Topology Canvas Box - 42dot Dark Hero Charcoal (#282b32) & 0px Square
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                colors = CardDefaults.cardColors(containerColor = DotHeroCharcoal),
                shape = RoundedCornerShape(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                viewAngleDeg = (viewAngleDeg + pan.x * 0.35f).coerceIn(-75f, 75f)
                                zScale = (zScale * zoom).coerceIn(1.0f, 6.0f)
                            }
                        }
                        .padding(12.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val centerX = canvasWidth * 0.5f
                        val centerY = canvasHeight * 0.52f

                        val angleRad = (viewAngleDeg * Math.PI / 180f).toFloat()
                        val cosA = cos(angleRad)
                        val sinA = sin(angleRad)

                        val numRows = 7 // Along depth axis (Y)
                        val numCols = profilePoints.size // 64 points along X axis
                        val xStep = (canvasWidth * 0.65f) / numCols
                        val yStep = (canvasHeight * 0.22f) / numRows

                        // 1. Draw 3D Base Reference Grid (Zero plane)
                        for (r in 0..numRows) {
                            val yOffset = (r - numRows / 2f) * yStep
                            val startX = centerX - (numCols / 2f) * xStep * cosA + yOffset * sinA
                            val startY = centerY + (numCols / 2f) * xStep * sinA * 0.5f + yOffset * cosA * 0.5f
                            val endX = centerX + (numCols / 2f) * xStep * cosA + yOffset * sinA
                            val endY = centerY - (numCols / 2f) * xStep * sinA * 0.5f + yOffset * cosA * 0.5f
                            drawLine(
                                color = Color(0xFF334155).copy(alpha = 0.4f),
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = 1f
                            )
                        }

                        // 2. Draw 3D Isometric Surface Ribbons / Mesh Lines
                        val points2D = Array(numRows) {
                            FloatArray(numCols * 2) // [x0, y0, x1, y1, ...]
                        }

                        for (r in 0 until numRows) {
                            val yOffset = (r - numRows / 2f) * yStep
                            for (c in 0 until numCols) {
                                val zVal = profilePoints[c] * zScale * 25f // scaled Z offset in pixels
                                val colOffset = (c - numCols / 2f) * xStep

                                val px = centerX + colOffset * cosA + yOffset * sinA
                                val py = centerY - colOffset * sinA * 0.5f + yOffset * cosA * 0.5f - zVal

                                points2D[r][c * 2] = px
                                points2D[r][c * 2 + 1] = py
                            }
                        }

                        // Draw surface transverse mesh ribbons with continuous color mapping [FR-05.3]
                        for (r in 0 until numRows - 1) {
                            val alpha = 0.25f + (r.toFloat() / numRows) * 0.55f
                            for (c in 1 until numCols) {
                                val segmentColor = calculateSurfaceColor(profilePoints[c], flushTolerance)
                                drawLine(
                                    color = segmentColor.copy(alpha = alpha),
                                    start = Offset(points2D[r][(c - 1) * 2], points2D[r][(c - 1) * 2 + 1]),
                                    end = Offset(points2D[r][c * 2], points2D[r][c * 2 + 1]),
                                    strokeWidth = if (r == numRows - 2) 2.5f else 1.5f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }

                        // Draw cross-ribbons connecting adjacent rows across step gap
                        val stepCol = numCols / 2
                        val highlightCols = listOf(2, stepCol - 8, stepCol, stepCol + 8, numCols - 3)
                        for (c in highlightCols) {
                            if (c in 0 until numCols) {
                                val crossPath = Path()
                                crossPath.moveTo(points2D[0][c * 2], points2D[0][c * 2 + 1])
                                for (r in 1 until numRows) {
                                    crossPath.lineTo(points2D[r][c * 2], points2D[r][c * 2 + 1])
                                }
                                drawPath(
                                    path = crossPath,
                                    color = Color(0xFF38BDF8).copy(alpha = 0.7f),
                                    style = Stroke(width = 2f, cap = StrokeCap.Round)
                                )
                            }
                        }

                        // Front profile edge highlight (Main step visualization) with continuous color mapping [FR-05.3]
                        val frontR = numRows - 1
                        for (c in 1 until numCols) {
                            val segmentColor = calculateSurfaceColor(profilePoints[c], flushTolerance)
                            drawLine(
                                color = segmentColor,
                                start = Offset(points2D[frontR][(c - 1) * 2], points2D[frontR][(c - 1) * 2 + 1]),
                                end = Offset(points2D[frontR][c * 2], points2D[frontR][c * 2 + 1]),
                                strokeWidth = 4.5f,
                                cap = StrokeCap.Round
                            )
                        }

                        // Step gap drop vertical indicators
                        val dropX = points2D[frontR][stepCol * 2]
                        val dropY = points2D[frontR][stepCol * 2 + 1]
                        val baseDropY = centerY - (stepCol - numCols / 2f) * xStep * sinA * 0.5f + ((frontR - numRows / 2f) * yStep) * cosA * 0.5f
                        drawLine(
                            color = Color(0xFFF43F5E),
                            start = Offset(dropX, dropY),
                            end = Offset(dropX, baseDropY),
                            strokeWidth = 3f
                        )
                    }

                    // Watermark / Coordinate overlay
                    Column(
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "3D ISOMETRIC TOPOLOGY",
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Z-Overdrive: ${String.format("%.1f", zScale)}x | View: ${viewAngleDeg.toInt()}°",
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    // [FR-05] Viewpoint Reset Button - 42dot 22px Pill Tag Standard
                    Surface(
                        onClick = {
                            viewAngleDeg = 35f
                            zScale = 3.0f
                        },
                        shape = RoundedCornerShape(22.dp),
                        color = DotNavGraphite.copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, TossGray200.copy(alpha = 0.35f)),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "리셋 (${viewAngleDeg.toInt()}°)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = DotWhite,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Bottom: Color Legend & Gesture/Axis Guide (Clean 2-row Stacked Layout to eliminate horizontal collision)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // Row 1: Color Legend Capsule
                        Surface(
                            color = DotNavGraphite.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.dp, TossGray200.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Box(modifier = Modifier.size(6.dp).background(DotPassGreen, CircleShape))
                                    Text("기준(0mm)", color = DotWhite, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), softWrap = false)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Box(modifier = Modifier.size(6.dp).background(DotViolet, CircleShape))
                                    Text("틈새음영", color = DotWhite, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), softWrap = false)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Box(modifier = Modifier.size(6.dp).background(DotFailRed, CircleShape))
                                    Text("단차초과", color = DotWhite, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), softWrap = false)
                                }
                            }
                        }

                        // Row 2: Gesture Hint & Axis Orientation Guide (non-overlapping)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "드래그: 3D 회전 · 핀치: 줌",
                                color = DotSlate,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                softWrap = false,
                                maxLines = 1
                            )
                            Surface(
                                color = DotNavGraphite.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(22.dp),
                                border = BorderStroke(1.dp, TossGray200.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = "+Z: 고저차 | X: 스윕 | Y: 단차폭",
                                    color = DotWhite,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            // Interactive Controls: View Angle & Z-Scale Sliders - 42dot Flat Tint Standard (0px Radius, Card Mist #f6f6f9)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DotCardMist),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3D 시점 및 디스플레이 제어",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = DotBlack
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "회전 시점 (${viewAngleDeg.toInt()}°)",
                            style = MaterialTheme.typography.bodySmall,
                            color = DotSlate,
                            modifier = Modifier.widthIn(min = 96.dp, max = 110.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                        Slider(
                            value = viewAngleDeg,
                            onValueChange = { viewAngleDeg = it },
                            valueRange = 15f..75f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = DotViolet,
                                activeTrackColor = DotViolet,
                                inactiveTrackColor = DotTagBorder.copy(alpha = 0.3f)
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "단차 과장 (${String.format("%.1f", zScale)}x)",
                            style = MaterialTheme.typography.bodySmall,
                            color = DotSlate,
                            modifier = Modifier.widthIn(min = 96.dp, max = 110.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                        Slider(
                            value = zScale,
                            onValueChange = { zScale = it },
                            valueRange = 1.0f..6.0f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = DotViolet,
                                activeTrackColor = DotViolet,
                                inactiveTrackColor = DotTagBorder.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * [FR-05.3] Maps surface elevation (Z in mm) to a distinct color gradient:
 * - Crevasse / gap shadow (< -0.06mm): Deep Violet / Purple
 * - Nominal base plane (|Z| <= tolerance): Emerald Green
 * - Step / Flush violation (> tolerance): Crimson Red
 */
private fun calculateSurfaceColor(zMm: Float, flushTolerance: Float): Color {
    return when {
        zMm < -0.06f -> {
            val factor = ((-zMm - 0.06f) / 0.25f).coerceIn(0f, 1f)
            Color(
                red = (0x7C * (1f - factor) + 0x4C * factor).toInt(),
                green = (0x3A * (1f - factor) + 0x1D * factor).toInt(),
                blue = (0xED * (1f - factor) + 0x95 * factor).toInt()
            )
        }
        abs(zMm) <= flushTolerance -> Color(0xFF10B981)
        else -> Color(0xFFEF4444)
    }
}
