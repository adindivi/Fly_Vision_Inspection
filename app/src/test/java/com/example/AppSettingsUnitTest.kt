package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests verifying AppSettings in-memory and persistent storage of
 * custom Gemini API keys and model selection.
 */
@RunWith(RobolectricTestRunner::class)
class AppSettingsUnitTest {

    private lateinit var appSettings: AppSettings

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        appSettings = AppSettings(context)
        appSettings.clearCustomApiKey()
        appSettings.selectedModel = AppSettings.DEFAULT_MODEL
    }

    @Test
    fun defaultSettings_hasGemini35FlashModel() {
        assertEquals("gemini-3.5-flash", appSettings.selectedModel)
        assertEquals("", appSettings.customApiKey)
        assertFalse(appSettings.isUsingCustomKey())
    }

    @Test
    fun customApiKey_persistsAndOverridesEffectiveKey() {
        val testKey = "AIzaSyTestKey123456789"
        appSettings.customApiKey = testKey

        assertTrue(appSettings.isUsingCustomKey())
        assertEquals(testKey, appSettings.customApiKey)
        assertEquals(testKey, appSettings.getEffectiveApiKey())
        assertTrue(appSettings.hasValidApiKey())

        // Clear custom key
        appSettings.clearCustomApiKey()
        assertFalse(appSettings.isUsingCustomKey())
        assertEquals("", appSettings.customApiKey)
    }

    @Test
    fun customModel_persistsCorrectly() {
        appSettings.selectedModel = "gemini-2.0-flash"
        assertEquals("gemini-2.0-flash", appSettings.selectedModel)

        appSettings.selectedModel = "gemini-3.5-flash"
        assertEquals("gemini-3.5-flash", appSettings.selectedModel)
    }
}
