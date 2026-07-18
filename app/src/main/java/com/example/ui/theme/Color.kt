package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

enum class AppTheme {
    PHILIPS_STUDIO, // Royal Blue & Sky Blue
    CYBERPUNK_NEON, // Neon Violet & Electric Cyan
    CARBON_AMBER,   // Jet Black & Warm Amber Gold
    NORDIC_FROST,   // Deep Slate & Aurora Green
    HIGH_CONTRAST   // High-Contrast Pure Black & Pure White/Yellow
}

object ThemeState {
    var isLightMode by mutableStateOf(false)
    var activeTheme by mutableStateOf(AppTheme.PHILIPS_STUDIO)

    // Animated color states (instantly accessible anywhere as regular Colors!)
    var darkBg by mutableStateOf(Color(0xFF090D1A))
    var darkPanel by mutableStateOf(Color(0xFF10172A))
    var darkCard by mutableStateOf(Color(0xFF1E2942))
    var darkBorder by mutableStateOf(Color(0xFF2B3A5E))
    var accentPrimary by mutableStateOf(Color(0xFF0066FF))
    var highlightSky by mutableStateOf(Color(0xFF4FA1FF))
    var textPrimary by mutableStateOf(Color(0xFFF1F5FA))
    var textMuted by mutableStateOf(Color(0xFF90A4C4))
}

val DarkBg: Color get() = ThemeState.darkBg
val DarkPanel: Color get() = ThemeState.darkPanel
val DarkCard: Color get() = ThemeState.darkCard
val DarkBorder: Color get() = ThemeState.darkBorder
val AccentPrimary: Color get() = ThemeState.accentPrimary
val HighlightSky: Color get() = ThemeState.highlightSky
val TextPrimary: Color get() = ThemeState.textPrimary
val TextMuted: Color get() = ThemeState.textMuted

fun getStaticDarkBg(isLightMode: Boolean, activeTheme: AppTheme): Color {
    return if (isLightMode) {
        when (activeTheme) {
            AppTheme.PHILIPS_STUDIO -> Color(0xFFF0F4FC)
            AppTheme.CYBERPUNK_NEON -> Color(0xFFFAF5FF)
            AppTheme.CARBON_AMBER -> Color(0xFFFFFBEB)
            AppTheme.NORDIC_FROST -> Color(0xFFF0FDF4)
            AppTheme.HIGH_CONTRAST -> Color(0xFFFFFFFF)
        }
    } else {
        when (activeTheme) {
            AppTheme.PHILIPS_STUDIO -> Color(0xFF090D1A)
            AppTheme.CYBERPUNK_NEON -> Color(0xFF0D061A)
            AppTheme.CARBON_AMBER -> Color(0xFF0A0A0A)
            AppTheme.NORDIC_FROST -> Color(0xFF0B1211)
            AppTheme.HIGH_CONTRAST -> Color(0xFF000000)
        }
    }
}

fun getStaticDarkPanel(isLightMode: Boolean, activeTheme: AppTheme): Color {
    return if (isLightMode) {
        when (activeTheme) {
            AppTheme.PHILIPS_STUDIO -> Color(0xFFFFFFFF)
            AppTheme.CYBERPUNK_NEON -> Color(0xFFFFFFFF)
            AppTheme.CARBON_AMBER -> Color(0xFFFFFFFF)
            AppTheme.NORDIC_FROST -> Color(0xFFFFFFFF)
            AppTheme.HIGH_CONTRAST -> Color(0xFFFFFFFF)
        }
    } else {
        when (activeTheme) {
            AppTheme.PHILIPS_STUDIO -> Color(0xFF10172A)
            AppTheme.CYBERPUNK_NEON -> Color(0xFF150B26)
            AppTheme.CARBON_AMBER -> Color(0xFF141414)
            AppTheme.NORDIC_FROST -> Color(0xFF111D1B)
            AppTheme.HIGH_CONTRAST -> Color(0xFF0D0D0D)
        }
    }
}

fun getStaticDarkCard(isLightMode: Boolean, activeTheme: AppTheme): Color {
    return if (isLightMode) {
        when (activeTheme) {
            AppTheme.PHILIPS_STUDIO -> Color(0xFFE4EFFE)
            AppTheme.CYBERPUNK_NEON -> Color(0xFFF3E8FF)
            AppTheme.CARBON_AMBER -> Color(0xFFFEF3C7)
            AppTheme.NORDIC_FROST -> Color(0xFFDCFCE7)
            AppTheme.HIGH_CONTRAST -> Color(0xFFE2E8F0)
        }
    } else {
        when (activeTheme) {
            AppTheme.PHILIPS_STUDIO -> Color(0xFF1E2942)
            AppTheme.CYBERPUNK_NEON -> Color(0xFF25143E)
            AppTheme.CARBON_AMBER -> Color(0xFF222222)
            AppTheme.NORDIC_FROST -> Color(0xFF192C28)
            AppTheme.HIGH_CONTRAST -> Color(0xFF1A1A1A)
        }
    }
}

fun getStaticDarkBorder(isLightMode: Boolean, activeTheme: AppTheme): Color {
    return if (isLightMode) {
        when (activeTheme) {
            AppTheme.PHILIPS_STUDIO -> Color(0xFFCCDDF7)
            AppTheme.CYBERPUNK_NEON -> Color(0xFFE8D5FF)
            AppTheme.CARBON_AMBER -> Color(0xFFFDE68A)
            AppTheme.NORDIC_FROST -> Color(0xFFBBF7D0)
            AppTheme.HIGH_CONTRAST -> Color(0xFF000000)
        }
    } else {
        when (activeTheme) {
            AppTheme.PHILIPS_STUDIO -> Color(0xFF2B3A5E)
            AppTheme.CYBERPUNK_NEON -> Color(0xFF3B1E63)
            AppTheme.CARBON_AMBER -> Color(0xFF333333)
            AppTheme.NORDIC_FROST -> Color(0xFF243F3A)
            AppTheme.HIGH_CONTRAST -> Color(0xFFFFFFFF)
        }
    }
}

fun getStaticAccentPrimary(isLightMode: Boolean, activeTheme: AppTheme): Color {
    return if (isLightMode) {
        when (activeTheme) {
            AppTheme.PHILIPS_STUDIO -> Color(0xFF0054E6)
            AppTheme.CYBERPUNK_NEON -> Color(0xFFD946EF)
            AppTheme.CARBON_AMBER -> Color(0xFFD97706)
            AppTheme.NORDIC_FROST -> Color(0xFF16A34A)
            AppTheme.HIGH_CONTRAST -> Color(0xFF0000FF)
        }
    } else {
        when (activeTheme) {
            AppTheme.PHILIPS_STUDIO -> Color(0xFF0066FF)
            AppTheme.CYBERPUNK_NEON -> Color(0xFFE047FF)
            AppTheme.CARBON_AMBER -> Color(0xFFF59E0B)
            AppTheme.NORDIC_FROST -> Color(0xFF10B981)
            AppTheme.HIGH_CONTRAST -> Color(0xFF00FFFF)
        }
    }
}

fun getStaticHighlightSky(isLightMode: Boolean, activeTheme: AppTheme): Color {
    return if (isLightMode) {
        when (activeTheme) {
            AppTheme.PHILIPS_STUDIO -> Color(0xFF007BFF)
            AppTheme.CYBERPUNK_NEON -> Color(0xFF06B6D4)
            AppTheme.CARBON_AMBER -> Color(0xFFB45309)
            AppTheme.NORDIC_FROST -> Color(0xFF0891B2)
            AppTheme.HIGH_CONTRAST -> Color(0xFF0000FF)
        }
    } else {
        when (activeTheme) {
            AppTheme.PHILIPS_STUDIO -> Color(0xFF4FA1FF)
            AppTheme.CYBERPUNK_NEON -> Color(0xFF00F0FF)
            AppTheme.CARBON_AMBER -> Color(0xFFFBBF24)
            AppTheme.NORDIC_FROST -> Color(0xFF2DD4BF)
            AppTheme.HIGH_CONTRAST -> Color(0xFFFFCC00)
        }
    }
}

fun getStaticTextPrimary(isLightMode: Boolean, activeTheme: AppTheme): Color {
    return if (isLightMode) {
        when (activeTheme) {
            AppTheme.PHILIPS_STUDIO -> Color(0xFF0F172A)
            AppTheme.CYBERPUNK_NEON -> Color(0xFF1E1B4B)
            AppTheme.CARBON_AMBER -> Color(0xFF27272A)
            AppTheme.NORDIC_FROST -> Color(0xFF14532D)
            AppTheme.HIGH_CONTRAST -> Color(0xFF000000)
        }
    } else {
        when (activeTheme) {
            AppTheme.PHILIPS_STUDIO -> Color(0xFFF1F5FA)
            AppTheme.CYBERPUNK_NEON -> Color(0xFFFDF8FF)
            AppTheme.CARBON_AMBER -> Color(0xFFFAFAF9)
            AppTheme.NORDIC_FROST -> Color(0xFFECFDF5)
            AppTheme.HIGH_CONTRAST -> Color(0xFFFFFFFF)
        }
    }
}

fun getStaticTextMuted(isLightMode: Boolean, activeTheme: AppTheme): Color {
    return if (isLightMode) {
        when (activeTheme) {
            AppTheme.PHILIPS_STUDIO -> Color(0xFF576F93)
            AppTheme.CYBERPUNK_NEON -> Color(0xFF7E22CE)
            AppTheme.CARBON_AMBER -> Color(0xFF78350F)
            AppTheme.NORDIC_FROST -> Color(0xFF15803D)
            AppTheme.HIGH_CONTRAST -> Color(0xFF334155)
        }
    } else {
        when (activeTheme) {
            AppTheme.PHILIPS_STUDIO -> Color(0xFF90A4C4)
            AppTheme.CYBERPUNK_NEON -> Color(0xFFB197FC)
            AppTheme.CARBON_AMBER -> Color(0xFFA8A29E)
            AppTheme.NORDIC_FROST -> Color(0xFF86EFAC)
            AppTheme.HIGH_CONTRAST -> Color(0xFFCBD5E1)
        }
    }
}

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
