package com.example.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Helper utility to provide distinctive haptic feedback patterns
 * for automated inspection events and analysis threshold confirmations.
 */
object HapticFeedbackHelper {

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Triggered when an automated inspection capture or scan is initiated.
     * Provides a crisp, snappy tactile click.
     */
    fun triggerInspectionStarted(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } catch (e: Exception) {
                    vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(35)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Triggered when the analysis threshold is met (e.g. specimen passes tolerances or AI confidence criteria).
     * Emits a distinct, double-pulse confirmation pattern.
     */
    fun triggerThresholdMet(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Waveform: wait 0ms, vibrate 45ms, pause 65ms, vibrate 70ms
                val timings = longArrayOf(0, 45, 65, 70)
                val amplitudes = intArrayOf(0, 180, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 45, 65, 70), -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * [FR-06.4] Immediate 30ms haptic tactile feedback on tolerance FAIL condition.
     */
    fun triggerFailHaptic(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(30)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
