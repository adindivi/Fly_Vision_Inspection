package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

/**
 * Manages persistent app settings including custom Gemini API key and model selection.
 * Enables user to enter custom API key or custom model in-app without rebuilding.
 */
class AppSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("fly_vision_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"
        private const val KEY_MODEL = "gemini_model_name"
        const val DEFAULT_MODEL = "gemini-3.5-flash"

        val AVAILABLE_MODELS = listOf(
            "gemini-3.5-flash",
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-1.5-pro"
        )
    }

    var customApiKey: String
        get() = prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_API_KEY, value.trim()).apply()

    var selectedModel: String
        get() = prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) = prefs.edit().putString(KEY_MODEL, value.trim()).apply()

    /**
     * Returns custom API key if set by user; falls back to BuildConfig.GEMINI_API_KEY.
     */
    fun getEffectiveApiKey(): String {
        val custom = customApiKey
        return if (custom.isNotBlank()) custom else BuildConfig.GEMINI_API_KEY
    }

    /**
     * Checks if a valid non-empty non-placeholder API key is available.
     */
    fun hasValidApiKey(): Boolean {
        val key = getEffectiveApiKey()
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * Returns true if the user configured a custom key in-app.
     */
    fun isUsingCustomKey(): Boolean {
        return customApiKey.isNotBlank()
    }

    /**
     * Clears user custom key to revert to build default.
     */
    fun clearCustomApiKey() {
        prefs.edit().remove(KEY_CUSTOM_API_KEY).apply()
    }
}
