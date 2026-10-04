package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.domain.ActivityStatus
import com.example.domain.ActivityType
import com.example.domain.InspectionActivityItem
import com.example.ui.InspectionActivityOverlay
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Visual regression test verifying that InspectionActivityOverlay renders
 * feature and anomaly inspection cards cleanly without visual clipping.
 * Replaces legacy GreetingScreenshotTest with domain-specific naming.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class InspectionActivityScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun captureInspectionActivityOverlay_displaysFeaturesAndAnomalies() {
        val inspectionItems = createSampleInspectionItems()

        composeTestRule.setContent {
            MyApplicationTheme {
                InspectionActivityOverlay(
                    activities = inspectionItems,
                    isAnalyzing = false,
                    onSharePdfReport = {},
                    onViewDetailReport = {},
                    onClearActivities = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/inspection_activity_overlay.png")
    }

    companion object {
        fun createSampleInspectionItems(): List<InspectionActivityItem> = listOf(
            InspectionActivityItem(
                type = ActivityType.FEATURE,
                anatomicalRegion = "Compound Eye",
                status = ActivityStatus.PASS,
                confidence = 96,
                details = "Wild-type brick-red pigmentation with intact hexagonal ommatidia."
            ),
            InspectionActivityItem(
                type = ActivityType.ANOMALY,
                anatomicalRegion = "Wing Margin",
                status = ActivityStatus.DEFECT,
                confidence = 92,
                details = "Notched wing blade anomaly on distal posterior margin."
            )
        )
    }
}
