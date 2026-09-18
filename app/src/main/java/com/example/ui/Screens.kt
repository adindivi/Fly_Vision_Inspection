package com.example.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MeasurementRecord
import com.example.util.rememberDebouncedClick
import kotlin.math.abs

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
                val step = (nominalFlush / 2f) * kotlin.math.tanh(xNorm).toFloat()
                val wave = 0.03f * kotlin.math.sin(i * 0.4f).toFloat()
                step + wave
            }
        }
    }

    // Flatness / RMS deviation calculation
    val flatnessRms = remember(profilePoints) {
        if (profilePoints.isEmpty()) 0f
        else {
            val mean = profilePoints.average().toFloat()
            kotlin.math.sqrt(profilePoints.map { (it - mean) * (it - mean) }.average()).toFloat()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header & Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Row 1: Title & Pass/Fail Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "🧊 3D 서피스 단차 지형도",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (depthResult.profile.isNotEmpty()) "실시간 광학 플로우 64-Point 메쉬" else "기준면 프리뷰 시뮬레이션 모드",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = if (depthResult.isPass) Color(0xFF10B981) else Color(0xFFEF4444),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (depthResult.isPass) "PASS" else "FAIL",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4-Grid Metric Indicators (Row 1)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MetricBadgeItem(
                            label = "단차 높이",
                            value = "${String.format("%+.3f", depthResult.flushHeight)}mm",
                            highlightColor = if (depthResult.isPass) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadgeItem(
                            label = "틈새 간격",
                            value = "${String.format("%.2f", depthResult.gapWidth)}mm",
                            highlightColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadgeItem(
                            label = "표면 평탄도",
                            value = "${String.format("%.3f", flatnessRms)}mm",
                            highlightColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadgeItem(
                            label = "해상도",
                            value = "${profilePoints.size} Pts",
                            highlightColor = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4-Grid Metric Indicators (Row 2: MaleCNS Neural Firing Rates & Subpixel Ridge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MetricBadgeItem(
                            label = "T4 광택 뉴런",
                            value = "${depthResult.t4Activity.toInt()} Hz",
                            highlightColor = Color(0xFFFBBF24),
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadgeItem(
                            label = "T5 음영 뉴런",
                            value = "${depthResult.t5Activity.toInt()} Hz",
                            highlightColor = Color(0xFFF43F5E),
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadgeItem(
                            label = "서브픽셀 능선",
                            value = "${String.format("%.1f", depthResult.subpixelGapCenter)} px",
                            highlightColor = Color(0xFF38BDF8),
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadgeItem(
                            label = "표면 결함",
                            value = "${depthResult.defects.size} 건",
                            highlightColor = if (depthResult.defects.isEmpty()) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            // 3D Isometric Surface Topology Canvas Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(12.dp)
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
                        val cosA = kotlin.math.cos(angleRad)
                        val sinA = kotlin.math.sin(angleRad)

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

                        // [FR-05.3] Color interpolation helper: Green (0mm) -> Purple (Gap Shadow) -> Red (Step violation)
                        fun getSurfaceColor(zMm: Float): Color {
                            return when {
                                zMm < -0.06f -> {
                                    // 틈새 음영 / 협곡 (Crevasse/Shadow): Dark Violet to Purple (#7C3AED / #4C1D95)
                                    val factor = ((-zMm - 0.06f) / 0.25f).coerceIn(0f, 1f)
                                    Color(
                                        red = (0x7C * (1f - factor) + 0x4C * factor).toInt(),
                                        green = (0x3A * (1f - factor) + 0x1D * factor).toInt(),
                                        blue = (0xED * (1f - factor) + 0x95 * factor).toInt()
                                    )
                                }
                                abs(zMm) <= flushTolerance -> {
                                    // 기준면 (0mm, 녹색 #10B981)
                                    Color(0xFF10B981)
                                }
                                else -> {
                                    // 허용 단차 초과 (+0.5mm / > flushTolerance, 붉은색 #EF4444)
                                    Color(0xFFEF4444)
                                }
                            }
                        }

                        // Draw surface transverse mesh ribbons with continuous color mapping [FR-05.3]
                        for (r in 0 until numRows - 1) {
                            val alpha = 0.25f + (r.toFloat() / numRows) * 0.55f
                            for (c in 1 until numCols) {
                                val segmentColor = getSurfaceColor(profilePoints[c])
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
                            val segmentColor = getSurfaceColor(profilePoints[c])
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

                    // [FR-05] Viewpoint Reset Button
                    Surface(
                        onClick = {
                            viewAngleDeg = 35f
                            zScale = 3.0f
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.9f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "시점 리셋",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "리셋 (${viewAngleDeg.toInt()}°)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color(0xFFE2E8F0),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Bottom-left: FR-05 Color Legend and Gesture Hint
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // [FR-05.3] Color Legend Badge
                        Surface(
                            color = Color(0xFF1E293B).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x33FFFFFF))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                                    Text("기준(0mm)", color = Color(0xFFE2E8F0), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), softWrap = false)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF7C3AED), CircleShape))
                                    Text("틈새음영", color = Color(0xFFE2E8F0), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), softWrap = false)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Box(modifier = Modifier.size(6.dp).background(Color(0xFFEF4444), CircleShape))
                                    Text("단차초과", color = Color(0xFFE2E8F0), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), softWrap = false)
                                }
                            }
                        }

                        // Gesture hint
                        Text(
                            text = "💡 드래그: 3D 회전 | 핀치: 줌",
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            softWrap = false
                        )
                    }

                    // Axis orientation badge
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        color = Color(0xFF1E293B).copy(alpha = 0.85f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "+Z: 고저차 | X: 스윕방향 | Y: 단차폭",
                            color = Color(0xFFCBD5E1),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        item {
            // Interactive Controls: View Angle & Z-Scale Sliders
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3D 시점 및 디스플레이 제어",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "회전 시점 (${viewAngleDeg.toInt()}°)",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(110.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                        Slider(
                            value = viewAngleDeg,
                            onValueChange = { viewAngleDeg = it },
                            valueRange = 15f..75f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "단차 과장 (${String.format("%.1f", zScale)}x)",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(110.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                        Slider(
                            value = zScale,
                            onValueChange = { zScale = it },
                            valueRange = 1.0f..6.0f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SensorCalibrationScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val velocity by viewModel.velocity.collectAsState()
    val accelMagnitude by viewModel.accelMagnitude.collectAsState()
    val gyroMagnitude by viewModel.gyroMagnitude.collectAsState()
    val isCalibrated by viewModel.isCalibrated.collectAsState()
    val calibratedFocalLength by viewModel.calibratedFocalLength.collectAsState()
    val lastCalibrationTime by viewModel.lastCalibrationTime.collectAsState()
    val gapTarget by viewModel.gapTarget.collectAsState()
    val gapTolerance by viewModel.gapTolerance.collectAsState()
    val flushTolerance by viewModel.flushTolerance.collectAsState()

    // Lifecycle auto-start: start HaltereSensorFusion when entering this screen,
    // stop when leaving if not actively measuring in inspection tab.
    DisposableEffect(Unit) {
        viewModel.sensorFusion.start()
        onDispose {
            if (!viewModel.isMeasuring.value) {
                viewModel.sensorFusion.stop()
            }
        }
    }

    val debouncedCalibrate = rememberDebouncedClick {
        viewModel.calibrate(350f)
        Toast.makeText(context, "✅ 300mm 기준면 광학 캘리브레이션 완료 (초점거리 350.0mm)", Toast.LENGTH_SHORT).show()
    }

    val debouncedZeroReset = rememberDebouncedClick {
        viewModel.resetSensorZero()
        Toast.makeText(context, "⚖️ IMU 센서 영점(Zero-Bias) 재보정이 완료되었습니다.", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Card 1: Calibration Status Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "⚖️ IMU 및 광학 센서 캘리브레이션",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "초점거리: ${String.format("%.1f", calibratedFocalLength)}mm" +
                                        (lastCalibrationTime?.let { " (최근: $it)" } ?: " (기본값)"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = if (isCalibrated) Color(0xFF10B981) else Color(0xFFF59E0B),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isCalibrated) "✅ 교정 완료" else "⚠️ 미교정",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        item {
            // Card 2: Live Sweeping Velocity & IMU Telemetry Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "실시간 스마트폰 스위핑 속도",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${String.format("%.1f", velocity)} mm/s",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        color = when {
                            velocity in 80f..160f -> Color(0xFF10B981) // Optimal zone
                            velocity > 160f -> Color(0xFFEF4444) // Over-speed
                            velocity > 10f -> Color(0xFF3B82F6) // Moving
                            else -> MaterialTheme.colorScheme.onSurface // Idle
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sweeping speed progress bar (0 ~ 300 mm/s)
                    val speedProgress = (velocity / 300f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { speedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = when {
                            velocity in 80f..160f -> Color(0xFF10B981)
                            velocity > 160f -> Color(0xFFEF4444)
                            else -> Color(0xFF3B82F6)
                        },
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Speed guide indicator
                    val statusMessage = when {
                        velocity < 10f -> "🛑 정지 상태 (기기를 평평한 곳에 두고 영점을 보정하세요)"
                        velocity < 80f -> "🔄 스위핑 속도 다소 느림 (약간 더 빠르게 이동하세요)"
                        velocity <= 160f -> "🎯 최적 스위핑 속도 유지 중 (80 ~ 160 mm/s)"
                        else -> "⚠️ 속도가 너무 빠릅니다 (천천히 이동하세요)"
                    }
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            velocity in 80f..160f -> Color(0xFF10B981)
                            velocity > 160f -> Color(0xFFEF4444)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (velocity in 80f..160f) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Mini Telemetry: Accel & Gyro
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("3축 가속도 합성 크기", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${String.format("%.2f", accelMagnitude)} m/s²",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("자이로 회전 각속도", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${String.format("%.2f", gyroMagnitude)} rad/s",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        item {
            // Card: Distance Presets & Reference Objects (from run_flyvision.py calibration_dialog)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🎯 실측 눈금 거리별 프리셋 & 기준 물체",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "검사 거리에 맞는 프리셋을 누르면 실측 스케일(mm/px)이 즉시 확정됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("1. 거리별 추천 프리셋", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CalibrationPresetItem(
                            icon = "🔍",
                            title = "매크로",
                            subtitle = "15cm (초근접)",
                            onClick = {
                                viewModel.applyCalibrationPreset(150f, 0.035f)
                                Toast.makeText(context, "🔍 초근접 매크로 프리셋 적용 (15cm / 0.035 mm/px)", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CalibrationPresetItem(
                            icon = "📏",
                            title = "표준",
                            subtitle = "30cm (권장)",
                            onClick = {
                                viewModel.applyCalibrationPreset(300f, 0.085f)
                                Toast.makeText(context, "📏 책상 표준 프리셋 적용 (30cm / 0.085 mm/px)", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CalibrationPresetItem(
                            icon = "🏭",
                            title = "원거리",
                            subtitle = "60cm (생산라인)",
                            onClick = {
                                viewModel.applyCalibrationPreset(600f, 0.180f)
                                Toast.makeText(context, "🏭 공장 원거리 프리셋 적용 (60cm / 0.180 mm/px)", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("2. 기준 물체 빠른 눈금 매핑", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CalibrationPresetItem(
                            icon = "💳",
                            title = "신용카드",
                            subtitle = "85.6mm",
                            onClick = {
                                viewModel.applyCalibrationPreset(300f, 0.085f)
                                Toast.makeText(context, "💳 신용카드(85.6mm) 기준 30cm 표준 스케일 적용", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CalibrationPresetItem(
                            icon = "📄",
                            title = "A4 용지",
                            subtitle = "210mm",
                            onClick = {
                                viewModel.applyCalibrationPreset(300f, 0.085f)
                                Toast.makeText(context, "📄 A4 용지(210mm) 기준 30cm 표준 스케일 적용", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CalibrationPresetItem(
                            icon = "📐",
                            title = "30cm 자",
                            subtitle = "300mm",
                            onClick = {
                                viewModel.applyCalibrationPreset(300f, 0.085f)
                                Toast.makeText(context, "📐 30cm 자 기준 30cm 표준 스케일 적용", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            // Card 3: Calibration Actions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "캘리브레이션 실행 작업",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. 광학 초점 캘리브레이션: 검사 대상과 300mm 기준 거리를 유지하고 실행합니다.\n2. IMU 영점 보정: 스마트폰을 평평한 바닥에 고정하고 영점 바이어스를 리셋합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = debouncedCalibrate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "300mm 기준면 광학 캘리브레이션 실행",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "검사 대상과 300mm 거리 유지 시 초점거리 350.0mm 확정",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = debouncedZeroReset,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "IMU 센서 영점(Zero-Bias) 재보정",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "스마트폰을 평평한 바닥에 고정하고 적분 오차 리셋",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            // Card 4: Tolerance Specification Config [FR-06]
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🎯 차체 검사 합격 공차 규격 (Tolerance Spec)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "차종 및 검사 부위별 합격 기준 틈새 및 단차 허용 오차를 설정합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("1. 주요 부품별 표준 공차 원클릭 프리셋", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CalibrationPresetItem(
                            icon = "🚗",
                            title = "도어 패널",
                            subtitle = "3.5mm (±0.5)",
                            onClick = {
                                viewModel.updateTolerances(3.5f, 0.5f, 0.3f)
                                Toast.makeText(context, "🚗 도어 패널 공차 (틈새 3.5±0.5mm, 단차 ±0.3mm) 적용", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CalibrationPresetItem(
                            icon = "💡",
                            title = "헤드램프",
                            subtitle = "2.0mm (±0.4)",
                            onClick = {
                                viewModel.updateTolerances(2.0f, 0.4f, 0.2f)
                                Toast.makeText(context, "💡 헤드램프 공차 (틈새 2.0±0.4mm, 단차 ±0.2mm) 적용", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CalibrationPresetItem(
                            icon = "🧰",
                            title = "트렁크 리드",
                            subtitle = "4.0mm (±0.6)",
                            onClick = {
                                viewModel.updateTolerances(4.0f, 0.6f, 0.4f)
                                Toast.makeText(context, "🧰 트렁크 리드 공차 (틈새 4.0±0.6mm, 단차 ±0.4mm) 적용", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("2. 공차 정밀 미세 조정", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Slider 1: Target Gap
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "목표 틈새: ${String.format("%.1f", gapTarget)}mm",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(120.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                        Slider(
                            value = gapTarget,
                            onValueChange = { viewModel.updateTolerances(it, gapTolerance, flushTolerance) },
                            valueRange = 1.0f..6.0f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Slider 2: Gap Tolerance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "틈새 공차: ±${String.format("%.2f", gapTolerance)}mm",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(120.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                        Slider(
                            value = gapTolerance,
                            onValueChange = { viewModel.updateTolerances(gapTarget, it, flushTolerance) },
                            valueRange = 0.2f..1.5f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Slider 3: Flush Tolerance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "단차 허용: ±${String.format("%.2f", flushTolerance)}mm",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(120.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                        Slider(
                            value = flushTolerance,
                            onValueChange = { viewModel.updateTolerances(gapTarget, gapTolerance, it) },
                            valueRange = 0.1f..1.0f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalibrationPresetItem(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(icon, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    softWrap = false
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun MetricBadgeItem(
    label: String,
    value: String,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = (-0.3).sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                letterSpacing = (-0.2).sp
            ),
            color = highlightColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpcStatisticsScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val spcResult by viewModel.spcResult.collectAsState()
    val history by viewModel.measurementHistory.collectAsState()
    val aiHistory by viewModel.filteredAiHistory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()
    val selectedAiRecords by viewModel.selectedAiRecords.collectAsState()
    val selectedDetail by viewModel.selectedInspectionDetail.collectAsState()
    val context = LocalContext.current
    
    // Display individual inspection result screen when selected
    if (selectedDetail != null) {
        IndividualInspectionResultDialog(
            record = selectedDetail!!,
            onDismiss = { viewModel.clearInspectionDetail() }
        )
    }
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportSelectedToCsv(context, it) }
    }
    
    val debouncedExportCsv = rememberDebouncedClick {
        exportLauncher.launch("ai_inspection_history_${System.currentTimeMillis()}.csv")
    }

    Column(modifier = modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SPC 공정 능력 통계", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    val cpkBadge = when {
                        spcResult.cpk >= 1.33f -> "✅ 6-Sigma 우수" to Color(0xFF10B981)
                        spcResult.cpk >= 1.00f -> "⚠️ 공정 보통" to Color(0xFFF59E0B)
                        spcResult.sampleCount > 0 -> "❌ 개선 필요" to Color(0xFFEF4444)
                        else -> "데이터 대기" to Color(0xFF94A3B8)
                    }
                    Surface(
                        color = cpkBadge.second.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cpkBadge.second)
                    ) {
                        Text(
                            text = cpkBadge.first,
                            color = cpkBadge.second,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("공정능력 (Cpk)", String.format("%.2f", spcResult.cpk), modifier = Modifier.weight(1f))
                    StatItem("평균 단차 (μ)", "${String.format("%.3f", spcResult.mean)}mm", modifier = Modifier.weight(1f))
                    StatItem("표준편차 (σ)", "${String.format("%.3f", spcResult.stdDev)}mm", modifier = Modifier.weight(1f))
                }
            }
        }
        
        Text(
            "최근 검사 이력",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(history) { record ->
                HistoryItem(record)
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "AI 분석 이력",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("검색어 입력 (예: scratch, anomaly)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = dateFilter == DateFilter.ALL,
                        onClick = { viewModel.updateDateFilter(DateFilter.ALL) },
                        label = { Text("전체") }
                    )
                    FilterChip(
                        selected = dateFilter == DateFilter.TODAY,
                        onClick = { viewModel.updateDateFilter(DateFilter.TODAY) },
                        label = { Text("오늘") }
                    )
                    FilterChip(
                        selected = dateFilter == DateFilter.LAST_7_DAYS,
                        onClick = { viewModel.updateDateFilter(DateFilter.LAST_7_DAYS) },
                        label = { Text("최근 7일") }
                    )
                }
                
                if (selectedAiRecords.isNotEmpty()) {
                    Button(
                        onClick = debouncedExportCsv,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text("선택 항목 CSV 내보내기 (${selectedAiRecords.size}건)")
                    }
                }
            }
            
            if (aiHistory.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "📭" else "🔍",
                                fontSize = 36.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (searchQuery.isBlank()) "저장된 AI 분석 이력이 없습니다" else "일치하는 검색 결과가 없습니다",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (searchQuery.isBlank()) "검사 화면에서 '일괄 분석'을 실행하거나 단일 검사를 진행해보세요."
                                       else "'$searchQuery'와 일치하는 검사 결과가 없습니다. 다른 키워드로 검색해보세요.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(aiHistory) { aiRecord ->
                    AiHistoryItem(
                        record = aiRecord,
                        selected = selectedAiRecords.contains(aiRecord.id),
                        onSelect = { viewModel.toggleSelection(aiRecord.id) },
                        onOpenDetail = { viewModel.selectInspectionDetail(aiRecord) }
                    )
                }
            }
        }
    }
}

@Composable
fun AiHistoryItem(
    record: com.example.data.AiAnalysisRecord,
    selected: Boolean,
    onSelect: (Boolean) -> Unit,
    onOpenDetail: () -> Unit
) {
    val context = LocalContext.current
    val debouncedShareReport = rememberDebouncedClick {
        com.example.util.PdfReportGenerator.generateAndShareReport(context, record)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onOpenDetail() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = onSelect
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(record.formattedDate, style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(record.apiResponse, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = debouncedShareReport
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share PDF Report",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
            maxLines = 1,
            softWrap = false
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun HistoryItem(record: MeasurementRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = record.formattedDate,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "단차: ${String.format("%+.3f", record.flushHeight)}mm",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                softWrap = false
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            color = if (record.isPass) Color(0xFF10B981) else Color(0xFFEF4444),
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = if (record.isPass) "PASS" else "FAIL",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
