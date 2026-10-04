package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM Unit tests for InspectionResponseParser.
 * Validates domain-level Gemini AI prompt formatting, structured regex parsing, and fallback extraction.
 */
class InspectionResponseParserTest {

    private val parser = InspectionResponseParser

    @Test
    fun parse_structuredPipeResponse_parsesCorrectAttributes() {
        val response = """
            - [FEATURE] | Compound Eye | PASS | 96% | Hexagonal ommatidial lattice intact
            - [ANOMALY] | Wing Margin | DEFECT | 92% | Irregular micro-tear observed at posterior tip
        """.trimIndent()

        val items = parser.parse(response, sourceTag = "Specimen-01")

        assertEquals(2, items.size)

        val eye = items[0]
        assertEquals(ActivityType.FEATURE, eye.type)
        assertEquals("Compound Eye", eye.anatomicalRegion)
        assertEquals(ActivityStatus.PASS, eye.status)
        assertEquals(96, eye.confidence)
        assertEquals("Hexagonal ommatidial lattice intact", eye.details)
        assertEquals("Specimen-01", eye.specimenSource)

        val wing = items[1]
        assertEquals(ActivityType.ANOMALY, wing.type)
        assertEquals("Wing Margin", wing.anatomicalRegion)
        assertEquals(ActivityStatus.DEFECT, wing.status)
        assertEquals(92, wing.confidence)
        assertEquals("Irregular micro-tear observed at posterior tip", wing.details)
        assertEquals("Specimen-01", wing.specimenSource)
    }

    @Test
    fun parse_unstructuredNaturalLanguageResponse_detectsFeaturesAndAnomalies() {
        val naturalText = """
            Visual inspection shows the thoracic bristles are normally aligned.
            However, there is an obvious notch defect on the right wing margin.
        """.trimIndent()

        val items = parser.parse(naturalText, sourceTag = "Batch-02")

        assertTrue("Should extract features from natural language", items.isNotEmpty())
        val wingDefect = items.firstOrNull { it.anatomicalRegion.contains("Wing") }
        assertNotNull("Should detect wing defect from natural text", wingDefect)
        assertEquals(ActivityStatus.DEFECT, wingDefect?.status)
        assertEquals(ActivityType.ANOMALY, wingDefect?.type)
    }

    @Test
    fun parse_emptyOrBlankResponse_returnsFallbackDefaultItem() {
        val items = parser.parse("", sourceTag = "Empty-Test")

        assertEquals(1, items.size)
        val item = items[0]
        assertEquals("Drosophila Surface Morphology", item.anatomicalRegion)
        assertEquals(ActivityStatus.PASS, item.status)
        assertEquals(92, item.confidence)
        assertEquals("Empty-Test", item.specimenSource)
    }

    @Test
    fun getInspectionPrompt_containsRequiredPromptDirectives() {
        val prompt = parser.getInspectionPrompt()

        assertTrue("Prompt should specify Drosophila melanogaster", prompt.contains("Drosophila melanogaster"))
        assertTrue("Prompt should define format structure", prompt.contains("[FEATURE") || prompt.contains("INSPECTION_ITEMS"))
        assertTrue("Prompt should include confidence requirement", prompt.contains("Confidence"))
    }

    @Test
    fun createInitialStagedItems_producesFiveStagedMorphologyItems() {
        val staged = parser.createInitialStagedItems()

        assertEquals(5, staged.size)
        assertTrue(staged.all { it.type == ActivityType.FEATURE })
        assertTrue(staged.all { it.status == ActivityStatus.PROCESSING })
    }

    @Test
    fun createErrorItem_producesDefectAnomalyWithContextMessage() {
        val errorItem = parser.createErrorItem("API Key invalid or quota exceeded")

        assertEquals(ActivityType.ANOMALY, errorItem.type)
        assertEquals("Gemini Processing Pipeline", errorItem.anatomicalRegion)
        assertEquals(ActivityStatus.DEFECT, errorItem.status)
        assertEquals(0, errorItem.confidence)
        assertTrue(errorItem.details.contains("API Key invalid"))
    }

    @Test
    fun createBatchProcessingItem_producesStagedItemWithCustomIndex() {
        val batchItem = parser.createBatchProcessingItem(index = 2, totalCount = 8, modelName = "gemini-2.5-flash")

        assertEquals("Batch Specimen 3/8", batchItem.anatomicalRegion)
        assertEquals(ActivityType.FEATURE, batchItem.type)
        assertEquals(ActivityStatus.PROCESSING, batchItem.status)
        assertTrue(batchItem.details.contains("gemini-2.5-flash"))
    }
}
