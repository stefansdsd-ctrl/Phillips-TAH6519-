package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

object ThemeState {
    var isLightMode by mutableStateOf(false)
}

val DarkBg: Color
    get() = if (ThemeState.isLightMode) Color(0xFFF0F4FC) else Color(0xFF090D1A)

val DarkPanel: Color
    get() = if (ThemeState.isLightMode) Color(0xFFFFFFFF) else Color(0xFF10172A)

val DarkCard: Color
    get() = if (ThemeState.isLightMode) Color(0xFFE4EFFE) else Color(0xFF1E2942)

val DarkBorder: Color
    get() = if (ThemeState.isLightMode) Color(0xFFCCDDF7) else Color(0xFF2B3A5E)

val AccentPrimary: Color
    get() = if (ThemeState.isLightMode) Color(0xFF0054E6) else Color(0xFF0066FF)

val HighlightSky: Color
    get() = if (ThemeState.isLightMode) Color(0xFF007BFF) else Color(0xFF4FA1FF)

val TextPrimary: Color
    get() = if (ThemeState.isLightMode) Color(0xFF0F172A) else Color(0xFFF1F5FA)

val TextMuted: Color
    get() = if (ThemeState.isLightMode) Color(0xFF576F93) else Color(0xFF90A4C4)

val StatusDanger = Color(0xFFFF4D6D)
val StatusSuccess = Color(0xFF00C896)
val StatusPurple = Color(0xFF9B47FF)
val StatusOrange = Color(0xFFFF7C3E)
val StatusYellow = Color(0xFFFFB347)
val StatusLime = Color(0xFFA8FF47)
val StatusTeal = Color(0xFF47FFB3)
val StatusMagenta = Color(0xFFE047FF)

// 10 EQ Band colors matching original template design
val EQBandColors = listOf(
    Color(0xFFFF4D6D), // 60Hz
    Color(0xFFFF7C3E), // 125Hz
    Color(0xFFFFB347), // 250Hz
    Color(0xFFFFE44D), // 500Hz
    Color(0xFFA8FF47), // 1kHz
    Color(0xFF47FFB3), // 2kHz
    Color(0xFF47D4FF), // 4kHz
    Color(0xFF4791FF), // 8kHz
    Color(0xFF9B47FF), // 12kHz
    Color(0xFFE047FF)  // 16kHz
)
