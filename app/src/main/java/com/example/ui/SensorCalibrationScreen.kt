package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DotBlack
import com.example.ui.theme.DotCardMist
import com.example.ui.theme.DotFailRed
import com.example.ui.theme.DotOffWhite
import com.example.ui.theme.DotPassGreen
import com.example.ui.theme.DotSlate
import com.example.ui.theme.DotTagBorder
import com.example.ui.theme.DotViolet
import com.example.ui.theme.DotWhite
import com.example.ui.theme.TossGray200
import com.example.ui.theme.TossWhite
import com.example.util.rememberDebouncedClick

/**
 * SensorCalibrationScreen: IMU & Optical Scale Calibration Screen
 * Allows operators to calibrate focal length, reset gyro/accel zero-bias, and configure tolerance thresholds.
 */
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
        Toast.makeText(context, "300mm 기준면 광학 캘리브레이션 완료 (초점거리 350.0mm)", Toast.LENGTH_SHORT).show()
    }

    val debouncedZeroReset = rememberDebouncedClick {
        viewModel.resetSensorZero()
        Toast.makeText(context, "IMU 센서 영점(Zero-Bias) 재보정이 완료되었습니다.", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DotOffWhite),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Card 1: Calibration Status Summary Card - 42dot Flat Tint Standard (0px Radius, Card Mist #f6f6f9)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DotCardMist),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "IMU 및 광학 센서 캘리브레이션",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = DotBlack,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "초점거리: ${String.format("%.1f", calibratedFocalLength)}mm" +
                                        (lastCalibrationTime?.let { " (최근: $it)" } ?: " (기본값)"),
                                style = MaterialTheme.typography.bodySmall,
                                color = DotSlate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = if (isCalibrated) DotPassGreen else DotViolet,
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Text(
                                text = if (isCalibrated) "교정 완료" else "미교정",
                                color = DotWhite,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        item {
            // Card 2: Live Sweeping Velocity & IMU Telemetry Card - 42dot Flat Tint Standard
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DotCardMist),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "실시간 스마트폰 스위핑 속도",
                        style = MaterialTheme.typography.titleSmall,
                        color = DotSlate
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${String.format("%.1f", velocity)} mm/s",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = when {
                            velocity in 80f..160f -> DotPassGreen // Optimal zone
                            velocity > 160f -> DotFailRed // Over-speed
                            velocity > 10f -> DotViolet // Moving
                            else -> DotBlack // Idle
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sweeping speed progress bar (0 ~ 300 mm/s)
                    val speedProgress = (velocity / 300f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { speedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(0.dp)),
                        color = when {
                            velocity in 80f..160f -> DotPassGreen
                            velocity > 160f -> DotFailRed
                            else -> DotViolet
                        },
                        trackColor = DotWhite
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Speed guide indicator
                    val statusMessage = when {
                        velocity < 10f -> "정지 상태 (기기를 평평한 곳에 두고 영점을 보정하세요)"
                        velocity < 80f -> "스위핑 속도 다소 느림 (약간 더 빠르게 이동하세요)"
                        velocity <= 160f -> "최적 스위핑 속도 유지 중 (80 ~ 160 mm/s)"
                        else -> "속도가 너무 빠릅니다 (천천히 이동하세요)"
                    }
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            velocity in 80f..160f -> DotPassGreen
                            velocity > 160f -> DotFailRed
                            else -> DotSlate
                        },
                        fontWeight = if (velocity in 80f..160f) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DotTagBorder.copy(alpha = 0.25f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Mini Telemetry: Accel & Gyro
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("3축 가속도 합성 크기", style = MaterialTheme.typography.labelSmall, color = DotSlate)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${String.format("%.2f", accelMagnitude)} m/s²",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = DotViolet
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("자이로 회전 각속도", style = MaterialTheme.typography.labelSmall, color = DotSlate)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${String.format("%.2f", gyroMagnitude)} rad/s",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = DotBlack
                            )
                        }
                    }
                }
            }
        }

        item {
            // Card 3: Distance Presets & Reference Objects - 42dot Flat Tint Standard
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DotCardMist),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "실측 눈금 거리별 프리셋 & 기준 물체",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = DotBlack
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "검사 거리에 맞는 프리셋을 누르면 실측 스케일(mm/px)이 즉시 확정됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DotSlate
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("1. 거리별 추천 프리셋", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = DotBlack)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CalibrationPresetItem(
                            title = "매크로",
                            subtitle = "15cm (초근접)",
                            onClick = {
                                viewModel.applyCalibrationPreset(150f, 0.035f)
                                Toast.makeText(context, "초근접 매크로 프리셋 적용 (15cm / 0.035 mm/px)", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CalibrationPresetItem(
                            title = "표준",
                            subtitle = "30cm (권장)",
                            onClick = {
                                viewModel.applyCalibrationPreset(300f, 0.085f)
                                Toast.makeText(context, "책상 표준 프리셋 적용 (30cm / 0.085 mm/px)", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CalibrationPresetItem(
                            title = "원거리",
                            subtitle = "60cm (생산라인)",
                            onClick = {
                                viewModel.applyCalibrationPreset(600f, 0.180f)
                                Toast.makeText(context, "공장 원거리 프리셋 적용 (60cm / 0.180 mm/px)", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("2. 기준 물체 빠른 눈금 매핑", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = DotBlack)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CalibrationPresetItem(
                            title = "신용카드",
                            subtitle = "85.6mm",
                            onClick = {
                                viewModel.applyCalibrationPreset(300f, 0.085f)
                                Toast.makeText(context, "신용카드(85.6mm) 기준 30cm 표준 스케일 적용", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CalibrationPresetItem(
                            title = "A4 용지",
                            subtitle = "210mm",
                            onClick = {
                                viewModel.applyCalibrationPreset(300f, 0.085f)
                                Toast.makeText(context, "A4 용지(210mm) 기준 30cm 표준 스케일 적용", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CalibrationPresetItem(
                            title = "30cm 자",
                            subtitle = "300mm",
                            onClick = {
                                viewModel.applyCalibrationPreset(300f, 0.085f)
                                Toast.makeText(context, "30cm 자 기준 30cm 표준 스케일 적용", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            // Card 4: Calibration Actions - 42dot Flat Tint Standard
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DotCardMist),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "캘리브레이션 실행 작업 (Autonomy Calibration)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = DotBlack
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. 광학 초점 캘리브레이션: 검사 대상과 300mm 기준 거리를 유지하고 실행합니다.\n2. IMU 영점 보정: 스마트폰을 평평한 바닥에 고정하고 영점 바이어스를 리셋합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DotSlate
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = debouncedCalibrate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DotViolet, contentColor = DotWhite)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "300mm 광학 캘리브레이션 실행",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "초점거리 350.0mm 확정",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = DotWhite.copy(alpha = 0.85f),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = debouncedZeroReset,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, TossGray200),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = TossWhite,
                            contentColor = DotBlack
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "IMU 센서 영점(Zero-Bias) 보정",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "평평한 바닥 고정 후 적분 오차 리셋",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = DotSlate,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        item {
            // Card 5: Tolerance Specification Config [FR-06] - 42dot Flat Tint Standard
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DotCardMist),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "차체 검사 합격 공차 규격 (Tolerance Spec)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = DotBlack
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "차종 및 검사 부위별 합격 기준 틈새 및 단차 허용 오차를 설정합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DotSlate
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("1. 주요 부품별 표준 공차 원클릭 프리셋", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = DotBlack)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CalibrationPresetItem(
                            title = "도어 패널",
                            subtitle = "3.5mm (±0.5)",
                            onClick = {
                                viewModel.updateTolerances(3.5f, 0.5f, 0.3f)
                                Toast.makeText(context, "도어 패널 공차 (틈새 3.5±0.5mm, 단차 ±0.3mm) 적용", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CalibrationPresetItem(
                            title = "헤드램프",
                            subtitle = "2.0mm (±0.4)",
                            onClick = {
                                viewModel.updateTolerances(2.0f, 0.4f, 0.2f)
                                Toast.makeText(context, "헤드램프 공차 (틈새 2.0±0.4mm, 단차 ±0.2mm) 적용", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CalibrationPresetItem(
                            title = "트렁크 리드",
                            subtitle = "4.0mm (±0.6)",
                            onClick = {
                                viewModel.updateTolerances(4.0f, 0.6f, 0.4f)
                                Toast.makeText(context, "트렁크 리드 공차 (틈새 4.0±0.6mm, 단차 ±0.4mm) 적용", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DotTagBorder.copy(alpha = 0.25f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("2. 공차 정밀 미세 조정", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = DotBlack)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Slider 1: Target Gap
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "목표 틈새: ${String.format("%.1f", gapTarget)}mm",
                            style = MaterialTheme.typography.bodySmall,
                            color = DotBlack,
                            modifier = Modifier.widthIn(min = 96.dp, max = 120.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = gapTarget,
                            onValueChange = { viewModel.updateTolerances(it, gapTolerance, flushTolerance) },
                            valueRange = 1.0f..6.0f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = DotViolet,
                                activeTrackColor = DotViolet,
                                inactiveTrackColor = DotTagBorder.copy(alpha = 0.3f)
                            )
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
                            color = DotBlack,
                            modifier = Modifier.widthIn(min = 96.dp, max = 120.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = gapTolerance,
                            onValueChange = { viewModel.updateTolerances(gapTarget, it, flushTolerance) },
                            valueRange = 0.2f..1.5f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = DotViolet,
                                activeTrackColor = DotViolet,
                                inactiveTrackColor = DotTagBorder.copy(alpha = 0.3f)
                            )
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
                            color = DotBlack,
                            modifier = Modifier.widthIn(min = 96.dp, max = 120.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = flushTolerance,
                            onValueChange = { viewModel.updateTolerances(gapTarget, gapTolerance, it) },
                            valueRange = 0.1f..1.0f,
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
 * CalibrationPresetItem: 22px Rounded Minimal Capsule button for calibration distance presets.
 */
@Composable
fun CalibrationPresetItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Clean White Minimal Capsule (22px Rounded, TossGray200 1px Border)
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 46.dp),
        shape = RoundedCornerShape(22.dp),
        color = TossWhite,
        border = BorderStroke(1.dp, TossGray200)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = DotBlack,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = DotSlate,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
