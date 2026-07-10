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

private val DarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    secondary = HighlightSky,
    tertiary = StatusSuccess,
    background = DarkBg,
    surface = DarkPanel,
    surfaceVariant = DarkCard,
    outline = DarkBorder,
    onPrimary = Color.White,
    onSecondary = DarkBg,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextPrimary,
    error = StatusDanger
)

private val LightColorScheme = lightColorScheme(
    primary = AccentPrimary,
    secondary = HighlightSky,
    tertiary = StatusSuccess,
    background = DarkBg,
    surface = DarkPanel,
    surfaceVariant = DarkCard,
    outline = DarkBorder,
    onPrimary = Color.White,
    onSecondary = DarkBg,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextPrimary,
    error = StatusDanger
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = !ThemeState.isLightMode,
    dynamicColor: Boolean = false, // Disable dynamic colors to keep our customized aesthetic
    content: @Composable () -> Unit,
) {
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
