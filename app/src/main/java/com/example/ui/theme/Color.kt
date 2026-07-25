package com.example.ui.theme
import androidx.compose.ui.graphics.Color

val DarkBg: Color get() = when {
    ThemeState.isLightMode -> Color(0xFFF2F4F8)
    ThemeState.activeTheme == AppTheme.HIGH_CONTRAST -> Color(0xFF000000)
    ThemeState.activeTheme == AppTheme.NORDIC_FROST -> Color(0xFF061412)
    ThemeState.activeTheme == AppTheme.CYBERPUNK_NEON -> Color(0xFF0B0512)
    ThemeState.activeTheme == AppTheme.CARBON_AMBER -> Color(0xFF100C08)
    else -> Color(0xFF080C14)
}

val DarkPanel: Color get() = when {
    ThemeState.isLightMode -> Color(0xFFFFFFFF)
    ThemeState.activeTheme == AppTheme.HIGH_CONTRAST -> Color(0xFF0D0D0D)
    ThemeState.activeTheme == AppTheme.NORDIC_FROST -> Color(0xFF0C211E)
    ThemeState.activeTheme == AppTheme.CYBERPUNK_NEON -> Color(0xFF160924)
    ThemeState.activeTheme == AppTheme.CARBON_AMBER -> Color(0xFF1C150E)
    else -> Color(0xFF101726)
}

val DarkCard: Color get() = when {
    ThemeState.isLightMode -> Color(0xFFEAEEF5)
    ThemeState.activeTheme == AppTheme.HIGH_CONTRAST -> Color(0xFF181818)
    ThemeState.activeTheme == AppTheme.NORDIC_FROST -> Color(0xFF122E2A)
    ThemeState.activeTheme == AppTheme.CYBERPUNK_NEON -> Color(0xFF200D36)
    ThemeState.activeTheme == AppTheme.CARBON_AMBER -> Color(0xFF281E14)
    else -> Color(0xFF141F33)
}

val DarkBorder: Color get() = when {
    ThemeState.isLightMode -> Color(0xFFC4D0E0)
    ThemeState.activeTheme == AppTheme.HIGH_CONTRAST -> Color(0xFFFFFF00)
    ThemeState.activeTheme == AppTheme.NORDIC_FROST -> Color(0xFF1B423C)
    ThemeState.activeTheme == AppTheme.CYBERPUNK_NEON -> Color(0xFF38125B)
    ThemeState.activeTheme == AppTheme.CARBON_AMBER -> Color(0xFF423221)
    else -> Color(0xFF20304D)
}

val AccentPrimary: Color get() = when {
    ThemeState.isLightMode -> Color(0xFF0066FF)
    ThemeState.activeTheme == AppTheme.HIGH_CONTRAST -> Color(0xFFFFFF00)
    ThemeState.activeTheme == AppTheme.NORDIC_FROST -> Color(0xFF00E676)
    ThemeState.activeTheme == AppTheme.CYBERPUNK_NEON -> Color(0xFFE040FB)
    ThemeState.activeTheme == AppTheme.CARBON_AMBER -> Color(0xFFFFAB00)
    else -> Color(0xFF00E5FF)
}

val HighlightSky: Color get() = when {
    ThemeState.isLightMode -> Color(0xFF0088FF)
    ThemeState.activeTheme == AppTheme.HIGH_CONTRAST -> Color(0xFF00FFFF)
    ThemeState.activeTheme == AppTheme.NORDIC_FROST -> Color(0xFF00B0FF)
    ThemeState.activeTheme == AppTheme.CYBERPUNK_NEON -> Color(0xFF00E5FF)
    ThemeState.activeTheme == AppTheme.CARBON_AMBER -> Color(0xFFFFD600)
    else -> Color(0xFF00B0FF)
}

val TextPrimary: Color get() = when {
    ThemeState.isLightMode -> Color(0xFF0A1626)
    ThemeState.activeTheme == AppTheme.HIGH_CONTRAST -> Color(0xFFFFFFFF)
    else -> Color(0xFFFFFFFF)
}

val TextMuted: Color get() = when {
    ThemeState.isLightMode -> Color(0xFF415266)
    ThemeState.activeTheme == AppTheme.HIGH_CONTRAST -> Color(0xFFFFFFB2)
    ThemeState.activeTheme == AppTheme.NORDIC_FROST -> Color(0xFFA0C8C0)
    ThemeState.activeTheme == AppTheme.CYBERPUNK_NEON -> Color(0xFFD1B0E3)
    ThemeState.activeTheme == AppTheme.CARBON_AMBER -> Color(0xFFD6C2A8)
    else -> Color(0xFFA0B2C6)
}

val StatusDanger = Color(0xFFFF4C4C)
val StatusOrange = Color(0xFFFFA500)
val StatusPurple = Color(0xFF800080)
val StatusSuccess = Color(0xFF00C853)
val StatusYellow = Color(0xFFFFD600)
val StatusMagenta = Color(0xFFFF00FF)
val StatusLime = Color(0xFF00FF00)

val EQBandColors = listOf(
    Color(0xFF0052D4), // Deep Studio Blue
    Color(0xFF0066FF), // Classic Royal Blue
    Color(0xFF007BFF), // Brilliant Blue
    Color(0xFF0090FF), // Electric Blue
    Color(0xFF00A5FF), // Sleek Neon Blue
    Color(0xFF00B5FF), // Light Neon Blue
    Color(0xFF00C5FF), // Bright Blue-Cyan
    Color(0xFF00D5FF), // Vivid Sky-Cyan
    Color(0xFF00E5FF), // Glowing Neon Cyan
    Color(0xFF00FFFF)  // Pure Cyan Light
)
