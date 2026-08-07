package com.example.ui.theme
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppTheme {
    PHILIPS_STUDIO, HIGH_CONTRAST, NORDIC_FROST, CYBERPUNK_NEON, CARBON_AMBER
}

enum class ThemeMode {
    LIGHT, AMBIENT, CUSTOM, DARK, SYSTEM
}

object ThemeState {
    var activeTheme by mutableStateOf(AppTheme.PHILIPS_STUDIO)
    var themeMode by mutableStateOf(ThemeMode.DARK)
    var isLightMode by mutableStateOf(false)
    var isAmbientMode by mutableStateOf(false)
    var isCustomMode by mutableStateOf(false)
    var customAccentColor by mutableStateOf(Color(0xFF00E5FF))
}

private val DarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    background = DarkBg,
    onBackground = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = AccentPrimary,
    background = Color(0xFFF2F4F8),
    onBackground = Color(0xFF0A1626)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    ThemeState.isLightMode = when (ThemeState.themeMode) {
        ThemeMode.LIGHT -> true
        ThemeMode.SYSTEM -> !darkTheme
        else -> false
    }
    ThemeState.isAmbientMode = (ThemeState.themeMode == ThemeMode.AMBIENT)
    ThemeState.isCustomMode = (ThemeState.themeMode == ThemeMode.CUSTOM)

    val colorScheme = if (ThemeState.isLightMode) {
        LightColorScheme
    } else {
        DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
