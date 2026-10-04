package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// 42dot Reference Design System - Color Tokens
// ============================================================================

// 1. Primary / Single Accent Token
val DotViolet = Color(0xFF786EFA)           // Periwinkle Violet - The single "selected / live" accent

// 2. Ink & Text Tokens
val DotBlack = Color(0xFF000000)            // Pure Black - Primary text and headings on light surfaces
val DotSlate = Color(0xFF737D8C)            // Muted Slate - Secondary text, metadata, inactive tag border
val DotWhite = Color(0xFFFFFFFF)            // Pure White - Text on dark chrome & active tag labels

// 3. Dark Chrome Tokens
val DotHeroCharcoal = Color(0xFF282B32)     // Hero Charcoal - Dark hero & StatusHud background
val DotNavGraphite = Color(0xFF32353F)      // Nav Graphite - Navigation panels & top tab bar

// 4. Surface & Neutral Tokens
val DotCanvasLight = Color(0xFFFFFFFF)      // Pure White page canvas
val DotOffWhite = Color(0xFFFBFBFB)         // Off-White light content band
val DotCardMist = Color(0xFFF6F6F9)         // Card Mist - Flat tint separation (0px cards)
val DotTagBorder = Color(0xFF737D8C)        // 1px hairline border for inactive tags

// 5. Industrial Inspection Status Feedback Tokens
val DotPassGreen = Color(0xFF10B981)        // Status PASS (Emerald)
val DotFailRed = Color(0xFFEF4444)          // Status REJECT/FAIL (Red)
val DotWarningOrange = Color(0xFFF59E0B)    // Status WARNING (Amber)

// 6. Clean White Minimal & Toss UI Tokens (Clean Capsule Standard)
val TossWhite = Color(0xFFFFFFFF)           // Pure White
val TossGray50 = Color(0xFFF9FAFB)          // Ultra light canvas background
val TossGray100 = Color(0xFFF2F4F6)         // Soft gray container & tab track
val TossGray200 = Color(0xFFE5E8EB)         // 1px Fine light gray border (Toss standard)
val TossGray300 = Color(0xFFD1D6DB)         // Border hover / divider
val TossGray600 = Color(0xFF6B7684)         // Caption / muted text
val TossGray700 = Color(0xFF4E5968)         // Secondary body text
val TossGray800 = Color(0xFF333D4B)         // Primary body text
val TossGray900 = Color(0xFF191F28)         // Deep dark headline
val TossBlue = Color(0xFF3182F6)            // Toss signature blue

// Compatibility aliases for Coinone / default tokens
val CoinoneBlue = DotViolet
val CoinonePointBlue = DotViolet
val CoinoneSupportingNavy = DotNavGraphite
val CoinoneDarkNavy = DotHeroCharcoal
val CoinoneHomeBlue = DotViolet
val CoinoneCanvasLight = DotCanvasLight
val CoinoneCanvasDark = DotHeroCharcoal
val CoinoneBorderLight = DotCardMist
val CoinoneBorderDark = DotSlate
val CoinoneMutedText = DotSlate
val CoinoneOutline = DotSlate
val CoinonePassGreen = DotPassGreen
val CoinoneFailRed = DotFailRed
val CoinoneWarningOrange = DotWarningOrange

val Purple80 = DotViolet
val PurpleGrey80 = DotSlate
val Pink80 = DotCardMist
val Purple40 = DotViolet
val PurpleGrey40 = DotHeroCharcoal
val Pink40 = DotSlate

