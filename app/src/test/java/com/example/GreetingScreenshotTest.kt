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

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val mockItems = listOf(
        InspectionActivityItem(
            type = ActivityType.FEATURE,
            anatomicalRegion = "Compound Eye",
            status = ActivityStatus.PASS,
            confidence = 96,
            details = "Wild-type brick-red pigmentation with intact hexagonal ommatidia."
        )
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        InspectionActivityOverlay(
            activities = mockItems,
            isAnalyzing = false,
            onSharePdfReport = {},
            onViewDetailReport = {},
            onClearActivities = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
