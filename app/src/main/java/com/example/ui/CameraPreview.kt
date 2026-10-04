package com.example.ui

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Environment
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.util.concurrent.Executors

@Composable
fun CameraPreview(
    analyzer: ImageAnalysis.Analyzer? = null,
    onImageCaptured: ((File) -> Unit)? = null,
    confidenceThreshold: Float = 0.2f,
    onConfidenceThresholdChange: (Float) -> Unit = {},
    isMacroMode: Boolean = false,
    onMacroModeToggle: (() -> Unit)? = null,
    macroFocusDistance: Float = 12.0f,
    onMacroFocusDistanceChange: (Float) -> Unit = {},
    isArtificialLight: Boolean = false,
    onArtificialLightToggle: (() -> Unit)? = null,
    isMeasuring: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember { ImageCapture.Builder().build() }

    var autoCaptureInterval by remember { mutableStateOf(0L) }
    var isTimerMenuExpanded by remember { mutableStateOf(false) }
    var isSettingsExpanded by remember { mutableStateOf(false) }

    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // Clean resource disposal: unbind all CameraX use cases & shutdown executor on unmount
    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProviderRef?.unbindAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                executor.shutdown()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Fallback internal macro state if caller doesn't provide a toggle callback
    var localMacroMode by remember { mutableStateOf(false) }
    val effectiveMacroMode = if (onMacroModeToggle != null) isMacroMode else localMacroMode
    val toggleMacro: () -> Unit = {
        if (onMacroModeToggle != null) {
            onMacroModeToggle()
        } else {
            localMacroMode = !localMacroMode
        }
    }

    // Fallback internal artificial light state
    var localArtificialLight by remember { mutableStateOf(false) }
    val effectiveArtificialLight = if (onArtificialLightToggle != null) isArtificialLight else localArtificialLight
    val toggleArtificialLight: () -> Unit = {
        if (onArtificialLightToggle != null) {
            onArtificialLightToggle()
        } else {
            localArtificialLight = !localArtificialLight
        }
    }

    val takePhoto = {
        HapticFeedbackHelper.triggerInspectionStarted(context)
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "flyvision_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Toast.makeText(context, "Saved to ${file.name}", Toast.LENGTH_SHORT).show()
                    onImageCaptured?.invoke(file)
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(context, "Capture failed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    LaunchedEffect(autoCaptureInterval) {
        if (autoCaptureInterval > 0) {
            while (isActive) {
                delay(autoCaptureInterval * 1000)
                HapticFeedbackHelper.triggerInspectionStarted(context)
                takePhoto()
            }
        }
    }

    // Apply Camera2 Macro Lens Focus optimizations whenever macro state or focus distance changes
    LaunchedEffect(effectiveMacroMode, macroFocusDistance, boundCamera) {
        applyMacroFocus(boundCamera, previewViewRef, effectiveMacroMode, macroFocusDistance)
    }

    // Control Artificial Light (Flashlight/Torch) via CameraX
    LaunchedEffect(effectiveArtificialLight, boundCamera) {
        boundCamera?.let { cam ->
            try {
                if (cam.cameraInfo.hasFlashUnit()) {
                    cam.cameraControl.enableTorch(effectiveArtificialLight)
                } else if (effectiveArtificialLight) {
                    Toast.makeText(context, "No flashlight available on this device", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Turn off torch when leaving
    DisposableEffect(boundCamera) {
        onDispose {
            try {
                boundCamera?.cameraControl?.enableTorch(false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // [FR-01] Auto Exposure & Auto Focus Lock during Measurement
    LaunchedEffect(isMeasuring, boundCamera, previewViewRef) {
        val cam = boundCamera ?: return@LaunchedEffect
        val pv = previewViewRef ?: return@LaunchedEffect
        try {
            if (isMeasuring) {
                val cx = if (pv.width > 0) pv.width / 2f else 540f
                val cy = if (pv.height > 0) pv.height / 2f else 960f
                val point = pv.meteringPointFactory.createPoint(cx, cy)
                val lockAction = FocusMeteringAction.Builder(
                    point,
                    FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                ).disableAutoCancel().build()
                cam.cameraControl.startFocusAndMetering(lockAction)
            } else {
                cam.cameraControl.cancelFocusAndMetering()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                previewViewRef = previewView
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProviderRef = cameraProvider
                    
                    val preview = androidx.camera.core.Preview.Builder()
                        .build()
                        .also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val camera = if (analyzer != null) {
                            var lastAnalyzedTime = 0L
                            val throttledAnalyzer = ImageAnalysis.Analyzer { imageProxy ->
                                val now = System.currentTimeMillis()
                                // When not actively measuring, throttle analysis to ~2 FPS (every 500ms)
                                // to eliminate heavy CPU/GPU load and prevent device heating during idle preview
                                if (!isMeasuring && (now - lastAnalyzedTime < 500L)) {
                                    imageProxy.close()
                                    return@Analyzer
                                }
                                lastAnalyzedTime = now
                                analyzer.analyze(imageProxy)
                            }
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                                .build()
                                .also {
                                    it.setAnalyzer(executor, throttledAnalyzer)
                                }
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture,
                                imageAnalysis
                            )
                        } else {
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        }

                        boundCamera = camera
                        applyMacroFocus(camera, previewView, effectiveMacroMode, macroFocusDistance)
                        if (effectiveArtificialLight && camera.cameraInfo.hasFlashUnit()) {
                            camera.cameraControl.enableTorch(true)
                        }
                        
                        previewView.setOnTouchListener { view, event ->
                            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                                val factory = previewView.meteringPointFactory
                                val point = factory.createPoint(event.x, event.y)
                                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF).build()
                                camera.cameraControl.startFocusAndMetering(action)
                                view.performClick()
                                return@setOnTouchListener true
                            }
                            return@setOnTouchListener false
                        }
                    } catch(e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top-End Minimal Camera Floating Toolbar (Flashlight, Macro, Quick Snapshot)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flashlight / Artificial Light Toggle
            Surface(
                onClick = {
                    val newState = !effectiveArtificialLight
                    toggleArtificialLight()
                    Toast.makeText(
                        context,
                        if (newState) "조명 ON" else "조명 OFF",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                shape = CircleShape,
                color = if (effectiveArtificialLight) Color(0xFFF59E0B) else Color(0x990F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (effectiveArtificialLight) Color(0xFFFBBF24) else Color(0x44FFFFFF)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (effectiveArtificialLight) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                        contentDescription = "Torch Toggle",
                        tint = if (effectiveArtificialLight) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Macro Mode Toggle
            Surface(
                onClick = {
                    val newState = !effectiveMacroMode
                    toggleMacro()
                    Toast.makeText(
                        context,
                        if (newState) "매크로 모드 ON" else "매크로 모드 OFF",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                shape = CircleShape,
                color = if (effectiveMacroMode) Color(0xFF10B981) else Color(0x990F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (effectiveMacroMode) Color(0xFF34D399) else Color(0x44FFFFFF)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.CenterFocusStrong,
                        contentDescription = "Macro Toggle",
                        tint = if (effectiveMacroMode) Color.White else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Quick Snapshot Button
            Surface(
                onClick = { takePhoto() },
                shape = CircleShape,
                color = Color(0x990F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = "Snapshot",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Settings Overlay (Macro Mode Config, Artificial Light, & Confidence Threshold Slider)
        if (isSettingsExpanded) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Macro Mode Toggle Setting
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Macro Mode (Specimen Close-Up)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Optimizes lens focus distance for small drosophila",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = effectiveMacroMode,
                            onCheckedChange = {
                                val newState = !effectiveMacroMode
                                toggleMacro()
                                Toast.makeText(
                                    context,
                                    if (newState) "Macro Mode: Enabled" else "Macro Mode: Disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }

                    if (effectiveMacroMode) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Specimen Focus Distance: ${String.format("%.1f", macroFocusDistance)} Diopters (~${String.format("%.1f", 100f / macroFocusDistance.coerceAtLeast(1f))} cm)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = macroFocusDistance,
                            onValueChange = onMacroFocusDistanceChange,
                            valueRange = 5.0f..25.0f
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Far (~20cm)", style = MaterialTheme.typography.labelSmall)
                            Text("Ultra-Close (~4cm)", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Artificial Light (Flashlight) Setting
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Artificial Light (Flashlight)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ensures consistent illumination for drosophila inspection",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = effectiveArtificialLight,
                            onCheckedChange = {
                                val newState = !effectiveArtificialLight
                                toggleArtificialLight()
                                Toast.makeText(
                                    context,
                                    if (newState) "Artificial Light: Enabled" else "Artificial Light: Disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "AI Confidence Threshold (Temperature)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = confidenceThreshold,
                        onValueChange = onConfidenceThresholdChange,
                        valueRange = 0.0f..1.0f
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Strict (0.0)", style = MaterialTheme.typography.bodySmall)
                        Text("Value: ${String.format("%.2f", confidenceThreshold)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("Loose (1.0)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

/**
 * Uses Camera2 interop via CameraX to configure the physical lens focus distance
 * and auto-focus algorithms for close-up imaging of small biological specimens.
 */
@OptIn(ExperimentalCamera2Interop::class)
private fun applyMacroFocus(
    camera: Camera?,
    previewView: PreviewView?,
    isMacro: Boolean,
    focusDistance: Float
) {
    if (camera == null) return
    try {
        val camera2Control = Camera2CameraControl.from(camera.cameraControl)
        val camera2Info = Camera2CameraInfo.from(camera.cameraInfo)

        val minFocusDistance = camera2Info.getCameraCharacteristic(
            CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE
        ) ?: 10f

        val afAvailableModes = camera2Info.getCameraCharacteristic(
            CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES
        ) ?: intArrayOf()

        val hasMacroAf = afAvailableModes.contains(CameraMetadata.CONTROL_AF_MODE_MACRO)

        if (isMacro) {
            val builder = CaptureRequestOptions.Builder()
            if (hasMacroAf) {
                // If device supports dedicated AF macro mode
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CameraMetadata.CONTROL_AF_MODE_MACRO
                )
            } else {
                // Fallback to manual focus with fixed close-up diopter distance
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CameraMetadata.CONTROL_AF_MODE_OFF
                )
            }

            // In diopters: higher values = closer distance (10 D = ~10 cm, 20 D = ~5 cm)
            val effectiveDistance = when {
                focusDistance > 0f -> focusDistance
                minFocusDistance > 0f -> minFocusDistance
                else -> 12.0f
            }
            builder.setCaptureRequestOption(
                CaptureRequest.LENS_FOCUS_DISTANCE,
                effectiveDistance
            )

            camera2Control.setCaptureRequestOptions(builder.build())

            // Immediately trigger close-range focus metering at center of viewfinder
            previewView?.let { pv ->
                if (pv.width > 0 && pv.height > 0) {
                    val factory = pv.meteringPointFactory
                    val centerPoint = factory.createPoint(pv.width / 2f, pv.height / 2f)
                    val action = FocusMeteringAction.Builder(centerPoint, FocusMeteringAction.FLAG_AF).build()
                    camera.cameraControl.startFocusAndMetering(action)
                }
            }
        } else {
            camera2Control.clearCaptureRequestOptions()
            camera.cameraControl.cancelFocusAndMetering()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
