package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.InlineData
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.data.AppDatabase
import com.example.data.MeasurementRecord
import com.example.domain.NeuromorphicEngine
import com.example.domain.SpcCalculator
import com.example.domain.SpcResult
import com.example.sensor.HaltereSensorFusion
import com.example.util.AppSettings
import com.example.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

enum class DateFilter {
    ALL, TODAY, LAST_7_DAYS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "MainViewModel"
    }

    private val db = AppDatabase.getDatabase(application)
    private val measurementDao = db.measurementDao()
    private val aiAnalysisDao = db.aiAnalysisDao()

    val engine = NeuromorphicEngine()
    val sensorFusion = HaltereSensorFusion(application)

    val depthData = engine.depthData
    val velocity = sensorFusion.velocity
    val accelMagnitude = sensorFusion.accelMagnitude
    val gyroMagnitude = sensorFusion.gyroMagnitude

    private val _isCalibrated = MutableStateFlow(false)
    val isCalibrated = _isCalibrated.asStateFlow()

    private val _calibratedFocalLength = MutableStateFlow(350f)
    val calibratedFocalLength = _calibratedFocalLength.asStateFlow()

    private val _lastCalibrationTime = MutableStateFlow<String?>(null)
    val lastCalibrationTime = _lastCalibrationTime.asStateFlow()

    private val _isVirtualSimulation = MutableStateFlow(false)
    val isVirtualSimulation = _isVirtualSimulation.asStateFlow()

    private val _isDefectDetectionEnabled = MutableStateFlow(false)
    val isDefectDetectionEnabled = _isDefectDetectionEnabled.asStateFlow()
    
    val measurementHistory = measurementDao.getRecentMeasurements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val aiAnalysisHistoryRaw = aiAnalysisDao.getAll()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _dateFilter = MutableStateFlow(DateFilter.ALL)
    val dateFilter = _dateFilter.asStateFlow()

    private val _selectedAiRecords = MutableStateFlow<Set<Int>>(emptySet())
    val selectedAiRecords = _selectedAiRecords.asStateFlow()
    
    // Low temperature = High confidence/certainty. Scale 0.0f to 1.0f.
    private val _aiConfidenceThreshold = MutableStateFlow(0.2f)
    val aiConfidenceThreshold = _aiConfidenceThreshold.asStateFlow()

    // Macro Mode state for close-up imaging of drosophila specimens
    private val _isMacroMode = MutableStateFlow(false)
    val isMacroMode = _isMacroMode.asStateFlow()

    private val _macroFocusDistance = MutableStateFlow(12.0f) // Diopters (~8.3 cm distance)
    val macroFocusDistance = _macroFocusDistance.asStateFlow()

    // Artificial Light (Flashlight/Torch) state for consistent specimen illumination
    private val _isArtificialLight = MutableStateFlow(false)
    val isArtificialLight = _isArtificialLight.asStateFlow()

    // Tolerance specification states [FR-06]
    private val _gapTarget = MutableStateFlow(3.5f)
    val gapTarget = _gapTarget.asStateFlow()

    private val _gapTolerance = MutableStateFlow(0.5f)
    val gapTolerance = _gapTolerance.asStateFlow()

    private val _flushTolerance = MutableStateFlow(0.30f)
    val flushTolerance = _flushTolerance.asStateFlow()

    fun updateTolerances(targetGap: Float, gapTol: Float, flushTol: Float) {
        _gapTarget.value = targetGap
        _gapTolerance.value = gapTol
        _flushTolerance.value = flushTol
        engine.setTolerances(targetGap, gapTol, flushTol)
    }

    val filteredAiHistory = combine(
        aiAnalysisHistoryRaw,
        _searchQuery,
        _dateFilter
    ) { history, query, filter ->
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val weekStart = todayStart - 7 * 24 * 60 * 60 * 1000L

        history.filter { record ->
            val matchesQuery = if (query.isBlank()) true else {
                record.apiResponse.contains(query, ignoreCase = true) ||
                record.formattedDate.contains(query, ignoreCase = true)
            }
            val matchesDate = when (filter) {
                DateFilter.ALL -> true
                DateFilter.TODAY -> record.timestamp >= todayStart
                DateFilter.LAST_7_DAYS -> record.timestamp >= weekStart
            }
            matchesQuery && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _spcResult = MutableStateFlow(SpcResult(0f, 0f, 0f))
    val spcResult = _spcResult.asStateFlow()

    private val _isMeasuring = MutableStateFlow(false)
    val isMeasuring = _isMeasuring.asStateFlow()
    
    private val _analysisResult = MutableStateFlow<String?>(null)
    val analysisResult = _analysisResult.asStateFlow()

    private val _currentInspectionRecord = MutableStateFlow<com.example.data.AiAnalysisRecord?>(null)
    val currentInspectionRecord = _currentInspectionRecord.asStateFlow()

    private val _selectedInspectionDetail = MutableStateFlow<com.example.data.AiAnalysisRecord?>(null)
    val selectedInspectionDetail = _selectedInspectionDetail.asStateFlow()

    fun selectInspectionDetail(record: com.example.data.AiAnalysisRecord?) {
        _selectedInspectionDetail.value = record
    }

    fun clearInspectionDetail() {
        _selectedInspectionDetail.value = null
    }

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()

    private val _inspectionActivities = MutableStateFlow<List<com.example.domain.InspectionActivityItem>>(emptyList())
    val inspectionActivities = _inspectionActivities.asStateFlow()

    fun clearInspectionActivities() {
        _inspectionActivities.value = emptyList()
    }

    private val _bulkAnalysisProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val bulkAnalysisProgress = _bulkAnalysisProgress.asStateFlow()

    val appSettings = AppSettings(application)

    private val _customApiKey = MutableStateFlow(appSettings.customApiKey)
    val customApiKey = _customApiKey.asStateFlow()

    private val _selectedModel = MutableStateFlow(appSettings.selectedModel)
    val selectedModel = _selectedModel.asStateFlow()

    private val _isTestingApiKey = MutableStateFlow(false)
    val isTestingApiKey = _isTestingApiKey.asStateFlow()

    private val _apiTestResult = MutableStateFlow<String?>(null)
    val apiTestResult = _apiTestResult.asStateFlow()

    fun updateApiSettings(apiKey: String, model: String) {
        appSettings.customApiKey = apiKey
        appSettings.selectedModel = model
        _customApiKey.value = apiKey
        _selectedModel.value = model
    }

    fun clearCustomApiKey() {
        appSettings.clearCustomApiKey()
        _customApiKey.value = ""
    }

    fun clearApiTestResult() {
        _apiTestResult.value = null
    }

    fun testApiConnection(testKey: String, testModel: String) {
        viewModelScope.launch {
            _isTestingApiKey.value = true
            _apiTestResult.value = null
            try {
                val effectiveKey = if (testKey.isNotBlank()) testKey else appSettings.getEffectiveApiKey()
                if (effectiveKey.isBlank() || effectiveKey == "MY_GEMINI_API_KEY") {
                    _apiTestResult.value = "API 키를 입력해주세요."
                    return@launch
                }
                withContext(Dispatchers.IO) {
                    val req = GenerateContentRequest(
                        contents = listOf(
                            Content(parts = listOf(Part(text = "Hello, respond with 'OK'.")))
                        )
                    )
                    val resp = RetrofitClient.service.generateContent(
                        model = testModel,
                        apiKey = effectiveKey,
                        request = req
                    )
                    val text = resp.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                    _apiTestResult.value = "연결 성공 (${testModel})\n응답: ${text.trim().take(40)}"
                }
            } catch (e: Exception) {
                _apiTestResult.value = "연결 실패: ${e.localizedMessage ?: e.message}"
            } finally {
                _isTestingApiKey.value = false
            }
        }
    }

    private val _uiError = MutableStateFlow<String?>(null)
    val uiError = _uiError.asStateFlow()

    fun clearUiError() {
        _uiError.value = null
    }

    fun setUiError(message: String) {
        _uiError.value = message
    }

    init {
        viewModelScope.launch {
            velocity.collect { v ->
                engine.updateVelocity(v)
            }
        }
        viewModelScope.launch {
            sensorFusion.omegaYaw.collect { omega ->
                engine.updateRotationalVelocity(omega)
            }
        }
        viewModelScope.launch {
            measurementHistory.collect { records ->
                _spcResult.value = SpcCalculator.calculateStats(records)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateDateFilter(filter: DateFilter) {
        _dateFilter.value = filter
    }
    
    fun updateAiConfidenceThreshold(threshold: Float) {
        _aiConfidenceThreshold.value = threshold
    }

    fun toggleMacroMode() {
        _isMacroMode.value = !_isMacroMode.value
    }

    fun setMacroMode(enabled: Boolean) {
        _isMacroMode.value = enabled
    }

    fun updateMacroFocusDistance(distance: Float) {
        _macroFocusDistance.value = distance
    }

    fun toggleArtificialLight() {
        _isArtificialLight.value = !_isArtificialLight.value
    }

    fun setArtificialLight(enabled: Boolean) {
        _isArtificialLight.value = enabled
    }

    fun toggleSelection(id: Int) {
        _selectedAiRecords.value = _selectedAiRecords.value.toMutableSet().apply {
            if (contains(id)) remove(id) else add(id)
        }
    }

    fun clearSelection() {
        _selectedAiRecords.value = emptySet()
    }

    fun exportSelectedToCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val recordsToExport = filteredAiHistory.value.filter { it.id in _selectedAiRecords.value }
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write("ID,Date,Image URI,AI Response\n")
                    for (record in recordsToExport) {
                        val escapedResponse = record.apiResponse.replace("\"", "\"\"")
                        writer.write("${record.id},\"${record.formattedDate}\",\"${record.imageUri}\",\"${escapedResponse}\"\n")
                    }
                }
                withContext(Dispatchers.Main) {
                    clearSelection()
                    Toast.makeText(context, "Export successful", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun startMeasurement() {
        if (_isMeasuring.value) {
            Log.w(TAG, "Measurement is already active, ignoring duplicate start request.")
            return
        }
        _isMeasuring.value = true
        engine.resetPeakHold()
        sensorFusion.start()
    }

    fun stopMeasurement() {
        if (!_isMeasuring.value) {
            Log.w(TAG, "Measurement is already stopped, ignoring duplicate stop request.")
            return
        }
        _isMeasuring.value = false
        sensorFusion.stop()
        
        // Save current record
        val currentDepth = depthData.value
        viewModelScope.launch {
            try {
                measurementDao.insert(
                    MeasurementRecord(
                        partName = "Door Panel", // Mock location
                        gapWidth = currentDepth.gapWidth,
                        flushHeight = currentDepth.flushHeight,
                        isPass = currentDepth.isPass
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist measurement record", e)
                _uiError.value = "측정 데이터 저장 중 오류 발생: ${e.localizedMessage ?: e.message}"
            }
        }
    }
    
    fun calibrate(targetFocalLength: Float = 350f) {
        engine.setFocalLength(targetFocalLength)
        sensorFusion.calibrateZeroPoint()
        _calibratedFocalLength.value = targetFocalLength
        _isCalibrated.value = true
        val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _lastCalibrationTime.value = timeStr
    }

    fun resetSensorZero() {
        sensorFusion.calibrateZeroPoint()
    }

    fun toggleVirtualSimulation() {
        _isVirtualSimulation.value = !_isVirtualSimulation.value
        engine.isVirtualMode = _isVirtualSimulation.value
    }

    fun toggleDefectDetection() {
        val next = !_isDefectDetectionEnabled.value
        _isDefectDetectionEnabled.value = next
        engine.isDefectDetectionEnabled = next
    }

    fun applyCalibrationPreset(distanceMm: Float, pixelScale: Float) {
        engine.setFocalLength(350f)
        engine.setPixelToMm(pixelScale)
        engine.resetPeakHold()
        sensorFusion.calibrateZeroPoint()
        _calibratedFocalLength.value = distanceMm
        _isCalibrated.value = true
        val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _lastCalibrationTime.value = timeStr
    }

    fun parseInspectionItems(
        responseText: String,
        sourceTag: String = "Live Capture"
    ): List<com.example.domain.InspectionActivityItem> {
        return com.example.domain.InspectionResponseParser.parse(responseText, sourceTag)
    }

    private fun getDrosophilaInspectionPrompt(): String {
        return com.example.domain.InspectionResponseParser.getInspectionPrompt()
    }

    private fun createGenerateContentRequest(base64Image: String): GenerateContentRequest {
        return GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = getDrosophilaInspectionPrompt()),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                    )
                )
            ),
            generationConfig = GenerationConfig(temperature = _aiConfidenceThreshold.value)
        )
    }

    private fun formatFriendlyErrorMessage(e: Exception, contextLabel: String = "AI 분석"): String {
        return when {
            e.message?.contains("404") == true -> "AI 분석 모델을 찾을 수 없습니다. (API 엔드포인트 404)"
            e.message?.contains("403") == true || e.message?.contains("401") == true -> "API 키 인증에 실패했습니다. Gemini API 키를 확인해주세요."
            e.message?.contains("Unable to resolve host") == true -> "네트워크 연결을 확인해주세요."
            else -> "$contextLabel 중 오류가 발생했습니다: ${e.localizedMessage ?: e.message}"
        }
    }

    fun analyzeImage(imageFile: File) {
        if (_isAnalyzing.value) {
            Log.w(TAG, "Analysis is already running, skipping duplicate analyzeImage call.")
            return
        }

        if (!appSettings.hasValidApiKey()) {
            val errorMsg = "Gemini API 키가 설정되지 않았습니다. 상단 설정에서 API 키를 입력해주세요."
            Log.e(TAG, errorMsg)
            _uiError.value = errorMsg
            _analysisResult.value = errorMsg
            return
        }

        val apiKey = appSettings.getEffectiveApiKey()
        val model = appSettings.selectedModel

        _isAnalyzing.value = true
        _analysisResult.value = null

        // Pre-populate real-time staged scanning items into the activity overlay
        _inspectionActivities.value = com.example.domain.InspectionResponseParser.createInitialStagedItems()

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val bitmap = ImageUtils.decodeSampledBitmapFromFile(imageFile)
                        ?: throw IllegalStateException("Failed to decode image file: ${imageFile.name}")
                    val base64Image = ImageUtils.bitmapToBase64Jpeg(bitmap)

                    val request = createGenerateContentRequest(base64Image)
                    
                    val response = RetrofitClient.service.generateContent(
                        model = model,
                        apiKey = apiKey,
                        request = request
                    )
                    val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No anomalies detected by AI."
                    
                    val record = com.example.data.AiAnalysisRecord(
                        imageUri = imageFile.absolutePath,
                        apiResponse = responseText
                    )
                    val insertedId = aiAnalysisDao.insert(record)
                    val recordWithId = record.copy(id = insertedId.toInt())
                    _currentInspectionRecord.value = recordWithId

                    // Parse real-time detected drosophila features or anomalies
                    val detectedItems = parseInspectionItems(responseText, sourceTag = "Specimen #DSP-${recordWithId.id}")
                    _inspectionActivities.value = detectedItems

                    responseText
                }
                _analysisResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Gemini image analysis failed: ${e.message}", e)
                val friendlyMessage = formatFriendlyErrorMessage(e, "AI 분석")
                _uiError.value = friendlyMessage
                _analysisResult.value = "Analysis Error: ${e.message}"
                _inspectionActivities.value = listOf(com.example.domain.InspectionResponseParser.createErrorItem(friendlyMessage))
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun analyzeImageUris(uris: List<Uri>, context: Context) {
        if (uris.isEmpty()) return
        if (_isAnalyzing.value) {
            Log.w(TAG, "Analysis is already running, skipping duplicate analyzeImageUris call.")
            return
        }

        if (!appSettings.hasValidApiKey()) {
            val errorMsg = "Gemini API 키가 설정되지 않았습니다. 상단 설정에서 API 키를 입력해주세요."
            Log.e(TAG, errorMsg)
            _uiError.value = errorMsg
            _analysisResult.value = errorMsg
            return
        }

        val apiKey = appSettings.getEffectiveApiKey()
        val model = appSettings.selectedModel

        _isAnalyzing.value = true
        _bulkAnalysisProgress.value = Pair(0, uris.size)
        _analysisResult.value = null

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    uris.forEachIndexed { index, uri ->
                        _bulkAnalysisProgress.value = Pair(index + 1, uris.size)
                        
                        // Add live processing item for this batch specimen
                        val liveBatchItem = com.example.domain.InspectionResponseParser.createBatchProcessingItem(index, uris.size, model)
                        _inspectionActivities.value = listOf(liveBatchItem) + _inspectionActivities.value

                        val bitmap = ImageUtils.decodeSampledBitmapFromUri(context, uri) ?: return@forEachIndexed
                        val base64Image = ImageUtils.bitmapToBase64Jpeg(bitmap)
                        
                        val request = createGenerateContentRequest(base64Image)
                        
                        val response = RetrofitClient.service.generateContent(
                            model = model,
                            apiKey = apiKey,
                            request = request
                        )
                        val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No anomalies detected by AI."
                        
                        val record = com.example.data.AiAnalysisRecord(
                            imageUri = uri.toString(),
                            apiResponse = responseText
                        )
                        val insertedId = aiAnalysisDao.insert(record)

                        // Real-time parsed items appended as each image is processed
                        val detectedItems = parseInspectionItems(responseText, sourceTag = "Batch #${insertedId}")
                        // Remove the temporary PROCESSING item and prepend the detected items
                        val currentList = _inspectionActivities.value.filter { it.id != liveBatchItem.id }
                        _inspectionActivities.value = detectedItems + currentList
                    }
                }
                _analysisResult.value = "Batch analysis completed for ${uris.size} images."
            } catch (e: Exception) {
                Log.e(TAG, "Batch analysis failed: ${e.message}", e)
                val friendlyMessage = formatFriendlyErrorMessage(e, "일괄 분석")
                _uiError.value = friendlyMessage
                _analysisResult.value = "Batch Analysis Error: ${e.message}"
            } finally {
                _isAnalyzing.value = false
                _bulkAnalysisProgress.value = null
            }
        }
    }

    fun clearAnalysis() {
        _analysisResult.value = null
        _currentInspectionRecord.value = null
    }

    override fun onCleared() {
        super.onCleared()
        sensorFusion.stop()
    }
}
