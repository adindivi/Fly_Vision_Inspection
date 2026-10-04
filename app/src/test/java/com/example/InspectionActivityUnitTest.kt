package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.domain.ActivityStatus
import com.example.domain.ActivityType
import com.example.ui.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests verifying Gemini AI structured and fallback natural language response parsing.
 * Refactored to extract test fixtures (DRY) and enforce Single Responsibility Principle (SRP).
 */
@RunWith(RobolectricTestRunner::class)
class InspectionActivityUnitTest {

    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        viewModel = MainViewModel(application)
    }

    @Test
    fun parseInspectionItems_parsesStructuredGeminiResponse_verifiesItemCountAndTypes() {
        val items = viewModel.parseInspectionItems(STRUCTURED_GEMINI_RESPONSE, sourceTag = "Specimen #DSP-1")

        assertEquals("Expected 5 parsed inspection items from structured response", 5, items.size)

        val eyeItem = items[0]
        assertEquals(ActivityType.FEATURE, eyeItem.type)
        assertEquals("Compound Eye", eyeItem.anatomicalRegion)
        assertEquals(ActivityStatus.PASS, eyeItem.status)
        assertEquals(96, eyeItem.confidence)

        val wingItem = items[1]
        assertEquals(ActivityType.ANOMALY, wingItem.type)
        assertEquals("Wing Margin", wingItem.anatomicalRegion)
        assertEquals(ActivityStatus.DEFECT, wingItem.status)
        assertEquals(92, wingItem.confidence)

        val cuticleItem = items[3]
        assertEquals(ActivityType.ANOMALY, cuticleItem.type)
        assertEquals("Cuticle Surface", cuticleItem.anatomicalRegion)
        assertEquals(ActivityStatus.DEFECT, cuticleItem.status)
        assertEquals(88, cuticleItem.confidence)
    }

    @Test
    fun parseInspectionItems_handlesFallbackUnstructuredText_identifiesDefects() {
        val items = viewModel.parseInspectionItems(FALLBACK_NATURAL_LANGUAGE_RESPONSE, sourceTag = "Specimen #DSP-2")

        assertTrue("Should detect features from unstructured text", items.isNotEmpty())
        val wingAnomaly = items.firstOrNull { it.anatomicalRegion.contains("Wing") }
        assertNotNull("Wing anomaly should be present in parsed results", wingAnomaly)
        assertEquals("Wing defect should be flagged as DEFECT status", ActivityStatus.DEFECT, wingAnomaly?.status)
    }

    @Test
    fun parseInspectionItems_emptyOrBlankResponse_createsFallbackItem() {
        val emptyItems = viewModel.parseInspectionItems("", sourceTag = "Empty #1")
        assertEquals("Empty response should produce single fallback item", 1, emptyItems.size)
        assertEquals("Drosophila Surface Morphology", emptyItems.first().anatomicalRegion)
        assertEquals(ActivityStatus.PASS, emptyItems.first().status)

        val blankItems = viewModel.parseInspectionItems("   \n\t  ", sourceTag = "Blank #2")
        assertEquals("Blank response should produce single fallback item", 1, blankItems.size)
        assertEquals("Drosophila Surface Morphology", blankItems.first().anatomicalRegion)
        assertEquals(ActivityStatus.PASS, blankItems.first().status)
    }

    companion object {
        private val STRUCTURED_GEMINI_RESPONSE = """
            INSPECTION_ITEMS:
            - FEATURE | Compound Eye | PASS | 96% | Wild-type brick-red pigmentation with intact hexagonal ommatidial array.
            - ANOMALY | Wing Margin | DEFECT | 92% | Notched wing blade anomaly on distal posterior margin.
            - FEATURE | Thoracic Bristles | PASS | 95% | Intact standard bilateral macrochaetae arrangement.
            - ANOMALY | Cuticle Surface | DEFECT | 88% | Micro-abrasion scratch and surface flush step mismatch on scutum.
            - FEATURE | Abdominal Tergites | PASS | 94% | Normal pigmented tergite bands A1-A6 without melanotic nodules.
            
            SUMMARY:
            Specimen shows localized notch mutation on right wing margin and minor cuticle abrasion on scutum.
        """.trimIndent()

        private val FALLBACK_NATURAL_LANGUAGE_RESPONSE = """
            Visual inspection summary:
            The compound eye shows regular ommatidia facet structure and red pigment.
            There is a wing margin tear defect detected on the left wing.
            Thoracic bristles and scutum appear intact.
        """.trimIndent()
    }
}
