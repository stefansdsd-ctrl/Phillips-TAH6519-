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

object ThemeState {
    var activeTheme by mutableStateOf(AppTheme.PHILIPS_STUDIO)
    var isLightMode by mutableStateOf(false)
}

private val DarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    background = DarkBg,
    onBackground = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = AccentPrimary,
    background = Color.White,
    onBackground = Color.Black
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
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
