package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HeadphoneSettings
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.math.roundToInt

/**
 * Main Screen UI component for toggling between Noise Cancelling, Awareness, and Off modes
 * for the Philips TAH6519 ANC over-ear headphones.
 */
@Composable
fun Tah6519MainAncModeSelectorCard(
    viewModel: HeadphoneViewModel,
    settings: HeadphoneSettings,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isTestingSoundCheck by remember { mutableStateOf(false) }

    // Wave Animation Helper
    val transition = rememberInfiniteTransition(label = "anc_mode_selector_waves")
    val phaseOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_offset"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tah6519_main_anc_mode_selector_card"),
        colors = CardDefaults.cardColors(containerColor = DarkPanel),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    DarkBorder,
                    HighlightSky.copy(alpha = 0.35f),
                    DarkBorder
                )
            )
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Icon + Title + Status Badge + Quick Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = when (settings.ancMode) {
                                    "ON" -> AccentPrimary.copy(alpha = 0.2f)
                                    "TRANSPARENCY" -> HighlightSky.copy(alpha = 0.2f)
                                    else -> DarkCard
                                },
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = when (settings.ancMode) {
                                    "ON" -> AccentPrimary
                                    "TRANSPARENCY" -> HighlightSky
                                    else -> DarkBorder
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (settings.ancMode) {
                                "ON" -> Icons.Filled.GraphicEq
                                "TRANSPARENCY" -> Icons.Filled.Hearing
                                else -> Icons.Filled.PowerSettingsNew
                            },
                            contentDescription = "Ruisbeheer Modus",
                            tint = when (settings.ancMode) {
                                "ON" -> AccentPrimary
                                "TRANSPARENCY" -> HighlightSky
                                else -> TextMuted
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "ANC Modus (Noise Control)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        color = when (settings.ancMode) {
                                            "ON" -> StatusSuccess
                                            "TRANSPARENCY" -> HighlightSky
                                            else -> TextMuted
                                        },
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = when (settings.ancMode) {
                                    "ON" -> "Noise Cancelling Actief (-56 dB)"
                                    "TRANSPARENCY" -> "Ambient Sound Actief (Awareness)"
                                    else -> "Off (Passieve Demping ~20 dB)"
                                },
                                color = when (settings.ancMode) {
                                    "ON" -> StatusSuccess
                                    "TRANSPARENCY" -> HighlightSky
                                    else -> TextMuted
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Quick Master Switch (On vs Off toggle)
                Switch(
                    checked = settings.ancMode != "OFF",
                    onCheckedChange = { checked ->
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        if (checked) {
                            viewModel.toggleAnc(true)
                            viewModel.setAncMode("ON")
                        } else {
                            viewModel.toggleAnc(false)
                            viewModel.setAncMode("OFF")
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = HighlightSky,
                        checkedTrackColor = AccentPrimary,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkBg
                    ),
                    modifier = Modifier
                        .scale(0.85f)
                        .testTag("tah6519_anc_master_switch")
                )
            }

            // Interactive Live Wave Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkBg)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val centerY = height / 2f

                    when (settings.ancMode) {
                        "ON" -> {
                            // Noise Cancelling: Anti-phase sound wave cancellation visualization
                            val pathOriginal = Path()
                            val pathCancelled = Path()

                            val segments = 40
                            for (i in 0..segments) {
                                val x = (i.toFloat() / segments) * width
                                val wave1 = sin((i * 0.35f) + phaseOffset) * (height * 0.32f)
                                val wave2 = -wave1 * 0.9f // Opposing phase wave

                                if (i == 0) {
                                    pathOriginal.moveTo(x, centerY + wave1)
                                    pathCancelled.moveTo(x, centerY + wave2)
                                } else {
                                    pathOriginal.lineTo(x, centerY + wave1)
                                    pathCancelled.lineTo(x, centerY + wave2)
                                }
                            }

                            // Draw incoming ambient noise wave in muted red/orange
                            drawPath(
                                path = pathOriginal,
                                color = StatusDanger.copy(alpha = 0.35f),
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                            // Draw anti-noise counter wave in vibrant ANC blue
                            drawPath(
                                path = pathCancelled,
                                color = AccentPrimary,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        "TRANSPARENCY" -> {
                            // Awareness: Pass-through microphone ripple waves
                            val pathMic = Path()
                            val segments = 40
                            for (i in 0..segments) {
                                val x = (i.toFloat() / segments) * width
                                val wave = sin((i * 0.25f) + phaseOffset * 1.5f) * (height * 0.35f)
                                if (i == 0) pathMic.moveTo(x, centerY + wave) else pathMic.lineTo(x, centerY + wave)
                            }
                            drawPath(
                                path = pathMic,
                                color = HighlightSky,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        else -> {
                            // Off: Flat baseline curve representing neutral passive isolation
                            drawLine(
                                color = TextMuted.copy(alpha = 0.5f),
                                start = androidx.compose.ui.geometry.Offset(0f, centerY),
                                end = androidx.compose.ui.geometry.Offset(width, centerY),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
                }

                Text(
                    text = when (settings.ancMode) {
                        "ON" -> "Phase Cancellation: Active Dual-Mic Signal Filter"
                        "TRANSPARENCY" -> "Aura Mic Pass-through: Live Ambient Feedback"
                        else -> "Passive Isolation: Direct Audio Bypass (80h Battery)"
                    },
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 4.dp)
                )
            }

            // 3-Way Mode Toggle Segmented Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard, shape = RoundedCornerShape(16.dp))
                    .border(1.dp, DarkBorder, shape = RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val modeOptions = listOf(
                    Triple("ON", "Noise Cancelling", Icons.Filled.GraphicEq),
                    Triple("TRANSPARENCY", "Ambient Sound", Icons.Filled.Hearing),
                    Triple("OFF", "Off", Icons.Filled.PowerSettingsNew)
                )

                modeOptions.forEach { (modeCode, label, icon) ->
                    val isSelected = settings.ancMode == modeCode
                    val activeColor = when (modeCode) {
                        "ON" -> AccentPrimary
                        "TRANSPARENCY" -> HighlightSky
                        else -> TextMuted
                    }

                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) activeColor else Color.Transparent,
                        animationSpec = tween(200),
                        label = "anc_bg_color"
                    )

                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else TextMuted,
                        animationSpec = tween(200),
                        label = "anc_text_color"
                    )

                    val scaleFactor by animateFloatAsState(
                        targetValue = if (isSelected) 1.02f else 1.0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy),
                        label = "anc_scale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .scale(scaleFactor)
                            .background(bgColor, shape = RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) activeColor.copy(alpha = 0.6f) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                viewModel.setAncMode(modeCode)
                                if (modeCode != "OFF") {
                                    viewModel.toggleAnc(true)
                                } else {
                                    viewModel.toggleAnc(false)
                                }
                            }
                            .testTag("anc_mode_toggle_${modeCode.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = textColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = label,
                                    color = textColor,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = when (modeCode) {
                                    "ON" -> "Ruisdemping"
                                    "TRANSPARENCY" -> "Awareness"
                                    else -> "Uit"
                                },
                                color = textColor.copy(alpha = 0.8f),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            // Sub-Settings Accordion based on active mode
            AnimatedVisibility(
                visible = settings.ancMode == "ON",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg, shape = RoundedCornerShape(12.dp))
                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ANC Intensiteit Niveau",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val ancLevels = listOf(
                            Triple(1, "Light Focus", "Kantoor"),
                            Triple(2, "Adaptief", "Reizen"),
                            Triple(3, "Deep Silence", "Max (-56 dB)")
                        )
                        ancLevels.forEach { (lvl, name, usage) ->
                            val isLvlSelected = settings.ancLevel == lvl
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .background(
                                        color = if (isLvlSelected) AccentPrimary.copy(alpha = 0.2f) else DarkCard,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isLvlSelected) AccentPrimary else DarkBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        viewModel.setAncLevel(lvl)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = name,
                                        color = if (isLvlSelected) HighlightSky else TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = usage,
                                        color = TextMuted,
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Air,
                                contentDescription = null,
                                tint = HighlightSky,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Windruisonderdrukking",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Switch(
                            checked = settings.windNoiseReductionEnabled,
                            onCheckedChange = { viewModel.toggleWindNoiseReduction(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = HighlightSky,
                                checkedTrackColor = AccentPrimary
                            ),
                            modifier = Modifier.scale(0.75f).testTag("tah6519_anc_wind_switch")
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = settings.ancMode == "TRANSPARENCY",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TransparencyAwarenessSliderControl(
                        settings = settings,
                        onIntensityChanged = { level -> viewModel.setTransparencyIntensity(level) }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBg, shape = RoundedCornerShape(12.dp))
                            .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = HighlightSky,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Spraak-naar-Awareness",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Switch(
                                checked = settings.speakToAwarenessEnabled,
                                onCheckedChange = { viewModel.toggleSpeakToAwareness(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = HighlightSky,
                                    checkedTrackColor = AccentPrimary
                                ),
                                modifier = Modifier.scale(0.75f).testTag("tah6519_speak_awareness_switch")
                            )
                        }
                        Text(
                            text = "Schakelt de koptelefoon automatisch naar Awareness mode zodra je begint met praten.",
                            color = TextMuted,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = settings.ancMode == "OFF",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg, shape = RoundedCornerShape(12.dp))
                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.BatteryChargingFull,
                        contentDescription = null,
                        tint = StatusSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Maximale Batterijduur (80 Uur)",
                            color = StatusSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "De over-ear kussens bieden nog steeds ~20 dB passieve geluidsisolatie zonder stroomverbruik.",
                            color = TextMuted,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
            }

            // Sound Check / Pulse Test Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    scope.launch {
                        isTestingSoundCheck = true
                        delay(1200)
                        isTestingSoundCheck = false
                    }
                },
                enabled = !isTestingSoundCheck,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HighlightSky.copy(alpha = 0.12f),
                    contentColor = HighlightSky,
                    disabledContainerColor = DarkBg,
                    disabledContentColor = TextMuted
                ),
                border = BorderStroke(1.dp, HighlightSky.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .testTag("anc_sound_check_button")
            ) {
                if (isTestingSoundCheck) {
                    CircularProgressIndicator(
                        color = HighlightSky,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Microfoons Kalibreren...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Equalizer,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("Test Acoustic Response", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Slider control to adjust the intensity of transparency / awareness mode (1 to 5 / 20% to 100%).
 */
@Composable
fun TransparencyAwarenessSliderControl(
    settings: HeadphoneSettings,
    onIntensityChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val currentLevel = settings.transparencyIntensity.coerceIn(1, 5)
    var sliderPosition by remember(currentLevel) { mutableStateOf(currentLevel.toFloat()) }

    val percentage = when (currentLevel) {
        1 -> 20
        2 -> 40
        3 -> 60
        4 -> 80
        5 -> 100
        else -> 60
    }

    val levelName = when (currentLevel) {
        1 -> "Subtiel"
        2 -> "Natuurlijke Spraak"
        3 -> "Gebalanceerd"
        4 -> "Verkeer & Veiligheid"
        5 -> "Maximale Versterking"
        else -> "Gebalanceerd"
    }

    val levelDescription = when (currentLevel) {
        1 -> "Subtiele doorlating van omgevingsgeluid, ideaal voor rustige binnenruimtes."
        2 -> "Scherpgerichte versterking van stemmen voor natuurlijk praten zonder de headset af te doen."
        3 -> "Gebalanceerde verhouding tussen spraak en achtergrondgeluid voor alledaags gebruik."
        4 -> "Verhoogde alertheid voor verkeerssignalen en omgevingsgeluiden bij wandelen of fietsen."
        5 -> "Maximale microfoonversterking (Aura Pass-Through) voor volledige akoestische transparantie."
        else -> ""
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBg, shape = RoundedCornerShape(12.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Hearing,
                    contentDescription = null,
                    tint = HighlightSky,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Awareness Intensiteit",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .background(HighlightSky.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp))
                    .border(1.dp, HighlightSky.copy(alpha = 0.4f), shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Niveau $currentLevel · $percentage% ($levelName)",
                    color = HighlightSky,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Slider
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Slider(
                value = sliderPosition,
                onValueChange = { newVal ->
                    sliderPosition = newVal
                    val newInt = kotlin.math.round(newVal).toInt().coerceIn(1, 5)
                    if (newInt != currentLevel) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onIntensityChanged(newInt)
                    }
                },
                valueRange = 1f..5f,
                steps = 3,
                colors = SliderDefaults.colors(
                    thumbColor = HighlightSky,
                    activeTrackColor = HighlightSky,
                    inactiveTrackColor = DarkCard,
                    activeTickColor = Color.White.copy(alpha = 0.8f),
                    inactiveTickColor = TextMuted.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tah6519_transparency_intensity_slider")
            )

            // Preset level labels/chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val chips = listOf(
                    1 to "20%\nSubtiel",
                    2 to "40%\nSpraak",
                    3 to "60%\nBalans",
                    4 to "80%\nVerkeer",
                    5 to "100%\nMax"
                )
                chips.forEach { (lvl, label) ->
                    val isSel = currentLevel == lvl
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSel) HighlightSky.copy(alpha = 0.25f) else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(
                                width = if (isSel) 1.dp else 0.dp,
                                color = if (isSel) HighlightSky else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                onIntensityChanged(lvl)
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .testTag("tah6519_transparency_level_$lvl"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSel) HighlightSky else TextMuted,
                            fontSize = 8.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            lineHeight = 10.sp
                        )
                    }
                }
            }
        }

        // Live Description
        Text(
            text = levelDescription,
            color = TextMuted,
            fontSize = 10.sp,
            lineHeight = 13.sp
        )
    }
}
