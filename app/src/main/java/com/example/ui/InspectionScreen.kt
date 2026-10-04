package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.domain.DepthResult
import com.example.ui.components.CameraPermissionFallbackCard
import com.example.ui.components.ElectronicCaliperHud
import com.example.ui.components.ModernPermissionDialog
import com.example.ui.components.openAppSettings
import com.example.ui.theme.DotBlack
import com.example.ui.theme.DotCardMist
import com.example.ui.theme.DotFailRed
import com.example.ui.theme.DotHeroCharcoal
import com.example.ui.theme.DotNavGraphite
import com.example.ui.theme.DotPassGreen
import com.example.ui.theme.DotSlate
import com.example.ui.theme.DotViolet
import com.example.ui.theme.DotWhite
import com.example.ui.theme.TossGray100
import com.example.ui.theme.TossGray200
import com.example.ui.theme.TossGray600
import com.example.ui.theme.TossGray700
import com.example.ui.theme.TossGray800
import com.example.ui.theme.TossGray900
import com.example.ui.theme.TossWhite
import com.example.util.rememberDebouncedClick
import java.util.concurrent.Executors
import kotlin.math.abs

@Composable
fun InspectionScreen(
    viewModel: MainViewModel,
    isCameraActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val depthResult by viewModel.depthData.collectAsState()
    val isMeasuring by viewModel.isMeasuring.collectAsState()
    val aiConfidenceThreshold by viewModel.aiConfidenceThreshold.collectAsState()
    val isMacroMode by viewModel.isMacroMode.collectAsState()
    val macroFocusDistance by viewModel.macroFocusDistance.collectAsState()
    val isArtificialLight by viewModel.isArtificialLight.collectAsState()
    val isVirtualSimulation by viewModel.isVirtualSimulation.collectAsState()
    val isDefectDetectionEnabled by viewModel.isDefectDetectionEnabled.collectAsState()
    val gapTarget by viewModel.gapTarget.collectAsState()
    val gapTolerance by viewModel.gapTolerance.collectAsState()
    val flushTolerance by viewModel.flushTolerance.collectAsState()
    val context = LocalContext.current
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var showPermissionDialog by remember { mutableStateOf(!hasCameraPermission) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            showPermissionDialog = false
        }
    }

    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.analyzeImageUris(uris, context)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            showPermissionDialog = true
        }
    }
    
    // [FR-06.4] Haptic feedback: PASS confirmation & immediate 30ms tactile pulse on tolerance FAIL
    var lastPassState by remember { mutableStateOf(true) }
    LaunchedEffect(depthResult.isPass, isMeasuring) {
        if (isMeasuring) {
            if (depthResult.isPass) {
                // Analysis threshold met (PASS confirmation)
                HapticFeedbackHelper.triggerThresholdMet(context)
            } else if (lastPassState) {
                // [FR-06.4] 30ms immediate haptic tactile vibration on tolerance FAIL
                HapticFeedbackHelper.triggerFailHaptic(context)
            }
        }
        lastPassState = depthResult.isPass
    }

    // AI Analysis Result state
    val analysisResult by viewModel.analysisResult.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val bulkProgress by viewModel.bulkAnalysisProgress.collectAsState()
    val inspectionActivities by viewModel.inspectionActivities.collectAsState()
    val currentInspectionRecord by viewModel.currentInspectionRecord.collectAsState()
    val uiError by viewModel.uiError.collectAsState()
    var showDetailDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Double-click debounce handlers for production safety
    val debouncedToggleMeasure = rememberDebouncedClick {
        if (isMeasuring) {
            viewModel.stopMeasurement()
        } else {
            HapticFeedbackHelper.triggerInspectionStarted(context)
            viewModel.startMeasurement()
        }
    }

    val debouncedBulkAnalysis = rememberDebouncedClick {
        HapticFeedbackHelper.triggerInspectionStarted(context)
        multiplePhotoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // Haptic confirmation when automated AI analysis completes and meets threshold
    LaunchedEffect(analysisResult) {
        analysisResult?.let { result ->
            if (!result.startsWith("Analysis Error") && !result.startsWith("Batch Analysis Error")) {
                HapticFeedbackHelper.triggerThresholdMet(context)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        StatusHud(
            depthResult = depthResult,
            isMeasuring = isMeasuring,
            isVirtualSimulation = isVirtualSimulation,
            gapTarget = gapTarget,
            gapTolerance = gapTolerance,
            flushTolerance = flushTolerance
        )
        
        // Modern Runtime Camera Permission Dialog
        if (showPermissionDialog && !hasCameraPermission) {
            ModernPermissionDialog(
                onDismiss = { showPermissionDialog = false },
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onOpenSettings = {
                    openAppSettings(context)
                }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            if (hasCameraPermission && isCameraActive) {
                CameraPreview(
                    analyzer = viewModel.engine,
                    onImageCaptured = { file ->
                        HapticFeedbackHelper.triggerInspectionStarted(context)
                        viewModel.analyzeImage(file)
                    },
                    confidenceThreshold = aiConfidenceThreshold,
                    onConfidenceThresholdChange = { viewModel.updateAiConfidenceThreshold(it) },
                    isMacroMode = isMacroMode,
                    onMacroModeToggle = { viewModel.toggleMacroMode() },
                    macroFocusDistance = macroFocusDistance,
                    onMacroFocusDistanceChange = { viewModel.updateMacroFocusDistance(it) },
                    isArtificialLight = isArtificialLight,
                    onArtificialLightToggle = { viewModel.toggleArtificialLight() },
                    isMeasuring = isMeasuring,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (!hasCameraPermission) {
                CameraPermissionFallbackCard(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }
            
            // Keyence-grade AR Interactive Electronic Caliper & Real-time Tolerance Band HUD
            ElectronicCaliperHud(
                depthResult = depthResult,
                gapTarget = gapTarget,
                gapTolerance = gapTolerance,
                flushTolerance = flushTolerance,
                isMeasuring = isMeasuring,
                modifier = Modifier.fillMaxSize()
            )

            // Real-time Surface Defect (Prophesee Metavision) Alert Pill (Auxiliary Mode)
            if (depthResult.isDefectDetectionEnabled && depthResult.defects.isNotEmpty()) {
                val firstDefect = depthResult.defects.first()
                Surface(
                    color = Color(0xEE991B1B),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${firstDefect.defectType} · ${String.format("%.1f", firstDefect.lengthMm)}mm",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Bulk Progress Banner if multiple images are being processed
            if (bulkProgress != null) {
                Surface(
                    color = Color(0xDD0F172A),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Analyzing ${bulkProgress!!.first} of ${bulkProgress!!.second}...",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Real-time Inspection Activity Overlay (Only visible when active analysis occurs)
            if (inspectionActivities.isNotEmpty() || isAnalyzing) {
                InspectionActivityOverlay(
                    activities = inspectionActivities,
                    isAnalyzing = isAnalyzing,
                    onSharePdfReport = {
                        val record = currentInspectionRecord ?: com.example.data.AiAnalysisRecord(
                            imageUri = "",
                            apiResponse = analysisResult ?: "Drosophila inspection findings verified."
                        )
                        com.example.util.PdfReportGenerator.generateAndShareReport(context, record)
                    },
                    onViewDetailReport = {
                        showDetailDialog = true
                    },
                    onClearActivities = {
                        viewModel.clearInspectionActivities()
                        viewModel.clearAnalysis()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                )
            }

            // Detail Modal if requested
            if (showDetailDialog && (currentInspectionRecord != null || analysisResult != null)) {
                val record = currentInspectionRecord ?: com.example.data.AiAnalysisRecord(
                    imageUri = "",
                    apiResponse = analysisResult ?: ""
                )
                IndividualInspectionResultDialog(
                    record = record,
                    onDismiss = { showDetailDialog = false }
                )
            }

            // Api Settings Modal Dialog
            if (showSettingsDialog) {
                ApiSettingsDialog(
                    viewModel = viewModel,
                    onDismiss = { showSettingsDialog = false }
                )
            }

            // User-friendly Error Alert Dialog
            if (uiError != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearUiError() },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "시스템 알림",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = uiError ?: "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearUiError() }) {
                            Text("확인")
                        }
                    },
                    dismissButton = {
                        if (uiError?.contains("API 키") == true || uiError?.contains("설정") == true) {
                            TextButton(onClick = {
                                viewModel.clearUiError()
                                showSettingsDialog = true
                            }) {
                                Text("API 설정 열기")
                            }
                        }
                    }
                )
            }
        }
        
        // Clean White Minimal Capsule Chips for Mode & Tolerance Selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = isDefectDetectionEnabled,
                onClick = { viewModel.toggleDefectDetection() },
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, if (isDefectDetectionEnabled) DotViolet else TossGray200),
                label = {
                    Text(
                        text = if (isDefectDetectionEnabled) "스크래치 감지 ON" else "스크래치 감지 OFF",
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.5.sp,
                            fontWeight = if (isDefectDetectionEnabled) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DotViolet,
                    selectedLabelColor = TossWhite,
                    containerColor = TossWhite,
                    labelColor = TossGray800
                )
            )

            AssistChip(
                onClick = {
                    when {
                        abs(gapTarget - 3.5f) < 0.1f -> {
                            viewModel.updateTolerances(2.0f, 0.4f, 0.2f)
                            Toast.makeText(context, "헤드램프 공차 (2.0±0.4mm, 단차±0.2) 적용", Toast.LENGTH_SHORT).show()
                        }
                        abs(gapTarget - 2.0f) < 0.1f -> {
                            viewModel.updateTolerances(4.0f, 0.6f, 0.4f)
                            Toast.makeText(context, "트렁크 리드 공차 (4.0±0.6mm, 단차±0.4) 적용", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            viewModel.updateTolerances(3.5f, 0.5f, 0.3f)
                            Toast.makeText(context, "도어 패널 공차 (3.5±0.5mm, 단차±0.3) 적용", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, TossGray200),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = TossWhite,
                    labelColor = TossGray800
                ),
                label = {
                    val partLabel = when {
                        abs(gapTarget - 2.0f) < 0.1f -> "헤드램프 2.0mm"
                        abs(gapTarget - 4.0f) < 0.1f -> "트렁크 리드 4.0mm"
                        else -> "도어 패널 3.5mm"
                    }
                    Text(
                        text = partLabel,
                        maxLines = 1,
                        softWrap = false,
                        color = TossGray800,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp)
                    )
                }
            )
        }

        // Clean White Minimal Capsule Action Buttons (Height: 46dp, Radius: 23dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = debouncedToggleMeasure,
                modifier = Modifier.weight(1.3f).height(46.dp),
                shape = RoundedCornerShape(23.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMeasuring) DotFailRed else DotViolet,
                    contentColor = TossWhite
                )
            ) {
                Text(
                    text = if (isMeasuring) "측정 정지" else "검사 시작",
                    maxLines = 1,
                    softWrap = false,
                    color = TossWhite,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            
            Button(
                onClick = debouncedBulkAnalysis,
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(23.dp),
                border = BorderStroke(1.dp, TossGray200),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TossWhite,
                    contentColor = TossGray800
                )
            ) {
                Text(
                    text = "일괄 분석",
                    maxLines = 1,
                    softWrap = false,
                    color = TossGray800,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

@Composable
fun StatusHud(
    depthResult: DepthResult,
    isMeasuring: Boolean,
    isVirtualSimulation: Boolean = false,
    gapTarget: Float = 3.5f,
    gapTolerance: Float = 0.5f,
    flushTolerance: Float = 0.30f
) {
    // Cinematic Dark Hero Charcoal Chrome (#282b32) with Clean White Minimal Capsules
    val bannerColor = if (!isMeasuring) DotHeroCharcoal
                      else if (depthResult.isPass) DotPassGreen
                      else DotFailRed

    Surface(
        color = bannerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)) {
            // Row 1: Concise Title, Spec/FPS pill, and Pass/Fail Badge (Clean Capsules)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "실시간 갭/단차",
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
                    color = TossWhite
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, TossGray200.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "${String.format("%.1f", gapTarget)}±${String.format("%.1f", gapTolerance)}mm · 60fps",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = TossWhite
                        )
                    }

                    Surface(
                        color = if (depthResult.isPass) DotPassGreen else DotFailRed,
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Text(
                            text = if (depthResult.isPass) "PASS" else "FAIL",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.5.dp),
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TossWhite
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Row 2: Responsive Metrics & Scratch Status Clean Capsule
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "단차 ${String.format("%+.3f", depthResult.flushHeight)}mm (±${String.format("%.2f", flushTolerance)}) · 틈새 ${String.format("%.2f", depthResult.gapWidth)}mm",
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        letterSpacing = (-0.2).sp
                    ),
                    color = TossWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, TossGray200.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (!depthResult.isDefectDetectionEnabled) "Scratch OFF"
                               else if (depthResult.defects.isEmpty()) "Scratch 0"
                               else "Defect ${depthResult.defects.size}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (depthResult.isDefectDetectionEnabled && depthResult.defects.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (!depthResult.isDefectDetectionEnabled) DotSlate
                                else if (depthResult.defects.isEmpty()) TossWhite
                                else Color(0xFFFDE047),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
