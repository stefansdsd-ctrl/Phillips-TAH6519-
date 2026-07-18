package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = !ThemeState.isLightMode,
    dynamicColor: Boolean = false, // Disable dynamic colors to keep our customized aesthetic
    content: @Composable () -> Unit,
) {
    // 1. Get the current light/dark state
    val targetIsLightMode = !darkTheme
    val targetTheme = ThemeState.activeTheme

    // 2. Fetch static target colors for the current theme configurations
    val targetBg = getStaticDarkBg(targetIsLightMode, targetTheme)
    val targetPanel = getStaticDarkPanel(targetIsLightMode, targetTheme)
    val targetCard = getStaticDarkCard(targetIsLightMode, targetTheme)
    val targetBorder = getStaticDarkBorder(targetIsLightMode, targetTheme)
    val targetAccent = getStaticAccentPrimary(targetIsLightMode, targetTheme)
    val targetSky = getStaticHighlightSky(targetIsLightMode, targetTheme)
    val targetTextPrimary = getStaticTextPrimary(targetIsLightMode, targetTheme)
    val targetTextMuted = getStaticTextMuted(targetIsLightMode, targetTheme)

    // 3. Define a smooth, comfortable transition animation (600ms Linear Out Slow In)
    val animSpec = tween<Color>(durationMillis = 600, easing = LinearOutSlowInEasing)

    // 4. Animate each color state smoothly
    val animatedBg by animateColorAsState(targetValue = targetBg, animationSpec = animSpec, label = "bg")
    val animatedPanel by animateColorAsState(targetValue = targetPanel, animationSpec = animSpec, label = "panel")
    val animatedCard by animateColorAsState(targetValue = targetCard, animationSpec = animSpec, label = "card")
    val animatedBorder by animateColorAsState(targetValue = targetBorder, animationSpec = animSpec, label = "border")
    val animatedAccent by animateColorAsState(targetValue = targetAccent, animationSpec = animSpec, label = "accent")
    val animatedSky by animateColorAsState(targetValue = targetSky, animationSpec = animSpec, label = "sky")
    val animatedTextPrimary by animateColorAsState(targetValue = targetTextPrimary, animationSpec = animSpec, label = "textPrimary")
    val animatedTextMuted by animateColorAsState(targetValue = targetTextMuted, animationSpec = animSpec, label = "textMuted")

    // 5. Update ThemeState properties cleanly during side-effect to propagate animated values globally without Composable restrictions!
    SideEffect {
        ThemeState.darkBg = animatedBg
        ThemeState.darkPanel = animatedPanel
        ThemeState.darkCard = animatedCard
        ThemeState.darkBorder = animatedBorder
        ThemeState.accentPrimary = animatedAccent
        ThemeState.highlightSky = animatedSky
        ThemeState.textPrimary = animatedTextPrimary
        ThemeState.textMuted = animatedTextMuted
    }

    // 6. Compute material design color scheme using animated colors
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) {
            darkColorScheme(
                primary = animatedAccent,
                secondary = animatedSky,
                tertiary = StatusSuccess,
                background = animatedBg,
                surface = animatedPanel,
                surfaceVariant = animatedCard,
                outline = animatedBorder,
                onPrimary = Color.White,
                onSecondary = animatedBg,
                onBackground = animatedTextPrimary,
                onSurface = animatedTextPrimary,
                onSurfaceVariant = animatedTextPrimary,
                error = StatusDanger
            )
        } else {
            lightColorScheme(
                primary = animatedAccent,
                secondary = animatedSky,
                tertiary = StatusSuccess,
                background = animatedBg,
                surface = animatedPanel,
                surfaceVariant = animatedCard,
                outline = animatedBorder,
                onPrimary = Color.White,
                onSecondary = animatedBg,
                onBackground = animatedTextPrimary,
                onSurface = animatedTextPrimary,
                onSurfaceVariant = animatedTextPrimary,
                error = StatusDanger
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
