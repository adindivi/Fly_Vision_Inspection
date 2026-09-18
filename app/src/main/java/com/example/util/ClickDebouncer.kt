package com.example.util

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * Utility for preventing rapid multiple clicks / debounce issues on Compose components.
 */
object ClickDebouncer {
    const val DEFAULT_DEBOUNCE_INTERVAL_MS = 500L

    /**
     * State holder to track the timestamp of the last click event.
     */
    class DebounceState(private val intervalMs: Long = DEFAULT_DEBOUNCE_INTERVAL_MS) {
        private var lastClickTime = 0L

        fun tryClick(action: () -> Unit) {
            val currentTime = SystemClock.uptimeMillis()
            if (currentTime - lastClickTime >= intervalMs) {
                lastClickTime = currentTime
                action()
            }
        }
    }
}

/**
 * Compose helper that returns a debounced click callback.
 */
@Composable
fun rememberDebouncedClick(
    intervalMs: Long = ClickDebouncer.DEFAULT_DEBOUNCE_INTERVAL_MS,
    onClick: () -> Unit
): () -> Unit {
    val debouncer = remember { ClickDebouncer.DebounceState(intervalMs) }
    return remember(onClick) {
        {
            debouncer.tryClick(onClick)
        }
    }
}

/**
 * Modifier extension to apply click debounce to any clickable component.
 */
fun Modifier.debouncedClickable(
    intervalMs: Long = ClickDebouncer.DEFAULT_DEBOUNCE_INTERVAL_MS,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val debouncedAction = rememberDebouncedClick(intervalMs = intervalMs, onClick = onClick)
    this.clickable(enabled = enabled, onClick = debouncedAction)
}
