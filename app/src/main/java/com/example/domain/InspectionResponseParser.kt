package com.example.domain

/**
 * Single Responsibility: Parsing and constructing Gemini AI drosophila inspection prompts and responses.
 * Extracted from MainViewModel to ensure separation of concerns and pure testability.
 */
object InspectionResponseParser {

    private val STRUCTURED_LINE_REGEX = Regex(
        """^-\s*\[?\s*(FEATURE|ANOMALY)\s*\]?\s*\|\s*(.*?)\s*\|\s*\[?\s*(PASS|DEFECT|WARNING)\s*\]?\s*\|\s*(\d+)%?\s*\|\s*(.*)$""",
        RegexOption.IGNORE_CASE
    )

    private val ANATOMICAL_REGION_KEYWORDS = listOf(
        "Compound Eye" to listOf("eye", "ommatidi", "ocelli", "facet"),
        "Wing Morphology" to listOf("wing", "blade", "haltere", "vein"),
        "Thorax & Bristles" to listOf("thorax", "bristle", "chaetae", "scutum"),
        "Abdomen & Tergites" to listOf("abdomen", "tergite", "segment", "pleural"),
        "Cuticle & Surface Flush" to listOf("cuticle", "flush", "gap", "surface", "scratch", "suture")
    )

    private val ANOMALY_KEYWORDS = listOf(
        "defect", "scratch", "anomal", "notched", "tear", "mutation", "deform"
    )

    /**
     * Parses raw Gemini response text into structured InspectionActivityItems.
     * Supports both structured pipe syntax and intelligent natural language fallback.
     */
    fun parse(
        responseText: String,
        sourceTag: String = "Live Capture"
    ): List<InspectionActivityItem> {
        val parsedItems = mutableListOf<InspectionActivityItem>()
        val candidateLines = responseText.lines()

        for (line in candidateLines) {
            val trimmedLine = line.trim()
            val match = STRUCTURED_LINE_REGEX.find(trimmedLine)
            if (match != null) {
                val typeStr = match.groupValues[1].uppercase()
                val region = match.groupValues[2].trim()
                val statusStr = match.groupValues[3].uppercase()
                val confidence = match.groupValues[4].toIntOrNull() ?: 90
                val details = match.groupValues[5].trim()

                val isAnomaly = typeStr == "ANOMALY" || statusStr == "DEFECT"
                val type = if (isAnomaly) ActivityType.ANOMALY else ActivityType.FEATURE
                val status = when (statusStr) {
                    "DEFECT" -> ActivityStatus.DEFECT
                    "WARNING" -> ActivityStatus.WARNING
                    else -> ActivityStatus.PASS
                }

                parsedItems.add(
                    InspectionActivityItem(
                        type = type,
                        anatomicalRegion = region,
                        status = status,
                        confidence = confidence,
                        details = details,
                        specimenSource = sourceTag
                    )
                )
            }
        }

        // Fallback natural language keyword heuristic if no structured lines were found
        if (parsedItems.isEmpty()) {
            val lowerResponse = responseText.lowercase()

            for ((regionName, keywords) in ANATOMICAL_REGION_KEYWORDS) {
                val matchingSentence = candidateLines.firstOrNull { candidateLine ->
                    keywords.any { keyword -> candidateLine.contains(keyword, ignoreCase = true) }
                }

                if (matchingSentence != null) {
                    val isAnomaly = ANOMALY_KEYWORDS.any { matchingSentence.contains(it, ignoreCase = true) }
                    parsedItems.add(
                        InspectionActivityItem(
                            type = if (isAnomaly) ActivityType.ANOMALY else ActivityType.FEATURE,
                            anatomicalRegion = regionName,
                            status = if (isAnomaly) ActivityStatus.DEFECT else ActivityStatus.PASS,
                            confidence = 92,
                            details = matchingSentence.trim().removePrefix("-").removePrefix("*").trim(),
                            specimenSource = sourceTag
                        )
                    )
                }
            }

            if (parsedItems.isEmpty()) {
                val hasAnomaly = ANOMALY_KEYWORDS.any { lowerResponse.contains(it) }
                parsedItems.add(
                    InspectionActivityItem(
                        type = if (hasAnomaly) ActivityType.ANOMALY else ActivityType.FEATURE,
                        anatomicalRegion = "Drosophila Surface Morphology",
                        status = if (hasAnomaly) ActivityStatus.DEFECT else ActivityStatus.PASS,
                        confidence = 92,
                        details = responseText.take(150),
                        specimenSource = sourceTag
                    )
                )
            }
        }

        return parsedItems
    }

    /**
     * Standard structured prompt instruction for Gemini multimodal vision model.
     */
    fun getInspectionPrompt(): String {
        return """
            You are an expert Drosophila melanogaster (fruit fly) visual morphology and neurological surface inspection assistant.
            Analyze this specimen image for anatomical features and detect any anomalies, defects, or mutations.
            
            You MUST format your output with a section named "INSPECTION_ITEMS:" containing line-by-line items in this exact syntax:
            INSPECTION_ITEMS:
            - [FEATURE or ANOMALY] | [Anatomical Area: Compound Eye / Wing Margin / Wing Venation / Thoracic Bristles / Thoracic Cuticle / Abdominal Tergite / Cuticle Surface / Appendages] | [PASS or DEFECT] | [Confidence 0-100%] | [Detailed observation of feature or anomaly]
            
            Examples:
            - FEATURE | Compound Eye | PASS | 96% | Wild-type brick-red pigmentation with intact hexagonal ommatidial array.
            - ANOMALY | Wing Margin | DEFECT | 92% | Notched wing blade anomaly on distal posterior margin.
            - FEATURE | Thoracic Bristles | PASS | 95% | Intact standard bilateral macrochaetae arrangement.
            - ANOMALY | Cuticle Surface | DEFECT | 88% | Micro-abrasion scratch and surface flush step mismatch on scutum.
            - FEATURE | Abdominal Tergites | PASS | 94% | Normal pigmented tergite bands A1-A6 without melanotic nodules.
            
            SUMMARY:
            [Provide a 2-3 sentence overall diagnostic summary of specimen health and defect flags]
        """.trimIndent()
    }

    /**
     * Staged scanning items shown in the live UI overlay during image capture and transmission.
     */
    fun createInitialStagedItems(): List<InspectionActivityItem> {
        return listOf(
            InspectionActivityItem(
                type = ActivityType.FEATURE,
                anatomicalRegion = "Optical Specimen Alignment",
                status = ActivityStatus.PROCESSING,
                confidence = 98,
                details = "Verifying drosophila orientation, focus, and macro lens framing...",
                specimenSource = "Live Capture"
            ),
            InspectionActivityItem(
                type = ActivityType.FEATURE,
                anatomicalRegion = "Compound Eye & Ommatidia",
                status = ActivityStatus.PROCESSING,
                confidence = 90,
                details = "Scanning hexagonal ommatidial facet array and brick-red pigmentation...",
                specimenSource = "Live Capture"
            ),
            InspectionActivityItem(
                type = ActivityType.FEATURE,
                anatomicalRegion = "Wing Margins & Venation",
                status = ActivityStatus.PROCESSING,
                confidence = 90,
                details = "Tracing longitudinal veins L1-L5 and examining blade border integrity...",
                specimenSource = "Live Capture"
            ),
            InspectionActivityItem(
                type = ActivityType.FEATURE,
                anatomicalRegion = "Thoracic Chaetae & Bristles",
                status = ActivityStatus.PROCESSING,
                confidence = 90,
                details = "Assessing symmetry of 4 dorsocentral macrochaetae on scutum...",
                specimenSource = "Live Capture"
            ),
            InspectionActivityItem(
                type = ActivityType.FEATURE,
                anatomicalRegion = "Abdominal Tergites & Flush",
                status = ActivityStatus.PROCESSING,
                confidence = 90,
                details = "Evaluating A1-A6 pigment stripes and lateral cuticle flush level...",
                specimenSource = "Live Capture"
            )
        )
    }

    /**
     * Live batch item indicator for bulk file processing.
     */
    fun createBatchProcessingItem(index: Int, totalCount: Int, modelName: String): InspectionActivityItem {
        return InspectionActivityItem(
            type = ActivityType.FEATURE,
            anatomicalRegion = "Batch Specimen ${index + 1}/$totalCount",
            status = ActivityStatus.PROCESSING,
            confidence = 90,
            details = "Transmitting image payload to $modelName...",
            specimenSource = "Batch File ${index + 1}"
        )
    }

    /**
     * Error activity item when network or API processing fails.
     */
    fun createErrorItem(errorMessage: String): InspectionActivityItem {
        return InspectionActivityItem(
            type = ActivityType.ANOMALY,
            anatomicalRegion = "Gemini Processing Pipeline",
            status = ActivityStatus.DEFECT,
            confidence = 0,
            details = errorMessage,
            specimenSource = "Error"
        )
    }
}
