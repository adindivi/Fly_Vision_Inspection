package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.domain.ActivityStatus
import com.example.domain.ActivityType
import com.example.ui.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InspectionActivityUnitTest {

    @Test
    fun parseInspectionItems_parsesStructuredGeminiResponse() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)

        val geminiOutput = """
            INSPECTION_ITEMS:
            - FEATURE | Compound Eye | PASS | 96% | Wild-type brick-red pigmentation with intact hexagonal ommatidial array.
            - ANOMALY | Wing Margin | DEFECT | 92% | Notched wing blade anomaly on distal posterior margin.
            - FEATURE | Thoracic Bristles | PASS | 95% | Intact standard bilateral macrochaetae arrangement.
            - ANOMALY | Cuticle Surface | DEFECT | 88% | Micro-abrasion scratch and surface flush step mismatch on scutum.
            - FEATURE | Abdominal Tergites | PASS | 94% | Normal pigmented tergite bands A1-A6 without melanotic nodules.
            
            SUMMARY:
            Specimen shows localized notch mutation on right wing margin and minor cuticle abrasion on scutum.
        """.trimIndent()

        val items = viewModel.parseInspectionItems(geminiOutput, sourceTag = "Specimen #DSP-1")

        assertEquals(5, items.size)

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
    fun parseInspectionItems_handlesFallbackUnstructuredText() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)

        val naturalLanguageOutput = """
            Visual inspection summary:
            The compound eye shows regular ommatidia facet structure and red pigment.
            There is a wing margin tear defect detected on the left wing.
            Thoracic bristles and scutum appear intact.
        """.trimIndent()

        val items = viewModel.parseInspectionItems(naturalLanguageOutput, sourceTag = "Specimen #DSP-2")

        assertTrue("Should detect features from unstructured text", items.isNotEmpty())
        val wingAnomaly = items.firstOrNull { it.anatomicalRegion.contains("Wing") }
        assertTrue("Wing defect should be flagged as anomaly", wingAnomaly != null && wingAnomaly.status == ActivityStatus.DEFECT)
    }
}
