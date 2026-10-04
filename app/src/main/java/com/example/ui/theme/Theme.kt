package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 42dot Near-Monochrome Dark Chrome Scheme
private val DarkColorScheme = darkColorScheme(
    primary = DotViolet,
    onPrimary = DotWhite,
    primaryContainer = DotNavGraphite,
    onPrimaryContainer = DotWhite,
    secondary = DotSlate,
    onSecondary = DotWhite,
    background = DotHeroCharcoal,
    onBackground = DotWhite,
    surface = DotHeroCharcoal,
    onSurface = DotWhite,
    surfaceVariant = DotNavGraphite,
    onSurfaceVariant = DotSlate,
    outline = DotSlate,
    outlineVariant = DotSlate.copy(alpha = 0.5f),
    error = DotFailRed
)

// 42dot Near-Monochrome Light Off-White Content Band Scheme
private val LightColorScheme = lightColorScheme(
    primary = DotViolet,
    onPrimary = DotWhite,
    primaryContainer = DotCardMist,
    onPrimaryContainer = DotBlack,
    secondary = DotSlate,
    onSecondary = DotWhite,
    background = DotOffWhite,
    onBackground = DotBlack,
    surface = DotCanvasLight,
    onSurface = DotBlack,
    surfaceVariant = DotCardMist,
    onSurfaceVariant = DotSlate,
    outline = DotTagBorder,
    outlineVariant = DotCardMist,
    error = DotFailRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 42dot brand identity: false to maintain clean monochrome + periwinkle violet
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
