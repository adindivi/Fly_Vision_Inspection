package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Validates application runtime configuration, context integrity,
 * and essential string/package resources under Robolectric environment.
 * Replaces the legacy template ExampleRobolectricTest dummy test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppConfigurationRobolectricTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun applicationContext_isNotNullAndAccessible() {
        assertNotNull("Application context should be initialized by Robolectric", context)
    }

    @Test
    fun applicationPackageName_matchesExpectedNamespace() {
        assertEquals("com.flyvision.inspector.xyzab", context.packageName)
    }

    @Test
    fun appNameResource_returnsFlyVisionIdentifier() {
        val appName = context.getString(R.string.app_name)
        assertEquals("FlyVision", appName)
    }

    @Test
    fun resourceSystem_resolvesStringResourcesWithoutException() {
        val resources = context.resources
        assertNotNull("Resources should be accessible from context", resources)
        assertTrue("Display density should be greater than zero", resources.configuration.densityDpi > 0)
    }
}
