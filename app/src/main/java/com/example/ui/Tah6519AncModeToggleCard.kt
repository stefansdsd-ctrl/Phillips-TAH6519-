package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HeadphoneSettings
import com.example.ui.theme.*
import kotlin.math.sin

/**
 * Modern Compose UI component to toggle and adjust Active Noise Cancellation modes
 * (On, Awareness, Off) for the Philips TAH6519 Over-Ear Headphones.
 */
@Composable
fun Tah6519AncModeToggleCard(
    settings: HeadphoneSettings,
    onModeChange: (String) -> Unit,
    onLevelChange: (Int) -> Unit,
    onTransparencyIntensityChange: (Int) -> Unit,
    onWindNoiseReductionToggle: (Boolean) -> Unit,
    onSpeakToAwarenessToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Infinite wave animation for dynamic canvas
    val infiniteTransition = rememberInfiniteTransition(label = "anc_wave_transition")
    val phaseOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_offset"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tah6519_anc_mode_toggle_card"),
        colors = CardDefaults.cardColors(containerColor = DarkPanel),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    DarkBorder,
                    HighlightSky.copy(alpha = 0.4f),
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
            // Header: Title & Master Switch
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
                            contentDescription = "Ruisbeheer Modus Icon",
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
                            text = "Actieve Ruisonderdrukking (ANC)",
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
                                    "ON" -> "Hybrid ANC Actief (-56 dB)"
                                    "TRANSPARENCY" -> "Awareness (Omgevingsgeluid)"
                                    else -> "Uit (Passieve Isolatie ~20 dB)"
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

                // Quick Master Power Switch
                Switch(
                    checked = settings.ancMode != "OFF",
                    onCheckedChange = { checked ->
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        if (checked) onModeChange("ON") else onModeChange("OFF")
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = HighlightSky,
                        checkedTrackColor = AccentPrimary,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkBg
                    ),
                    modifier = Modifier
                        .scale(0.85f)
                        .testTag("tah6519_anc_quick_switch")
                )
            }

            // Real-time Wave Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
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
                            val pathOriginal = Path()
                            val pathCancelled = Path()
                            val segments = 40
                            for (i in 0..segments) {
                                val x = (i.toFloat() / segments) * width
                                val wave1 = sin((i * 0.35f) + phaseOffset) * (height * 0.3f)
                                val wave2 = -wave1 * 0.9f
                                if (i == 0) {
                                    pathOriginal.moveTo(x, centerY + wave1)
                                    pathCancelled.moveTo(x, centerY + wave2)
                                } else {
                                    pathOriginal.lineTo(x, centerY + wave1)
                                    pathCancelled.lineTo(x, centerY + wave2)
                                }
                            }
                            drawPath(
                                path = pathOriginal,
                                color = StatusDanger.copy(alpha = 0.35f),
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawPath(
                                path = pathCancelled,
                                color = AccentPrimary,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        "TRANSPARENCY" -> {
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
                        "ON" -> "DSP Antiphase Filter (Invert 180°)"
                        "TRANSPARENCY" -> "Awareness Microfoon Doorvoer"
                        else -> "Passieve Demping (80h Batterijduur)"
                    },
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 4.dp)
                )
            }

            // 3-Way Mode Segmented Control Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard, shape = RoundedCornerShape(14.dp))
                    .border(1.dp, DarkBorder, shape = RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val modes = listOf(
                    Triple("ON", "Noise Cancelling", Icons.Filled.GraphicEq),
                    Triple("TRANSPARENCY", "Awareness", Icons.Filled.Hearing),
                    Triple("OFF", "Uit", Icons.Filled.PowerSettingsNew)
                )

                modes.forEach { (modeCode, label, icon) ->
                    val isSelected = settings.ancMode == modeCode
                    val activeColor = when (modeCode) {
                        "ON" -> AccentPrimary
                        "TRANSPARENCY" -> HighlightSky
                        else -> TextMuted
                    }

                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) activeColor else Color.Transparent,
                        animationSpec = tween(200),
                        label = "mode_bg"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else TextMuted,
                        animationSpec = tween(200),
                        label = "mode_text"
                    )
                    val scaleFactor by animateFloatAsState(
                        targetValue = if (isSelected) 1.02f else 1.0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy),
                        label = "mode_scale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .scale(scaleFactor)
                            .background(bgColor, shape = RoundedCornerShape(10.dp))
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) activeColor.copy(alpha = 0.6f) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                onModeChange(modeCode)
                            }
                            .testTag("btn_anc_mode_${modeCode.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = textColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = label,
                                color = textColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Sub-Settings: ANC Levels (If ON)
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
                                        onLevelChange(lvl)
                                    }
                                    .testTag("anc_level_chip_$lvl"),
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

                    // Wind noise reduction toggle
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
                            onCheckedChange = onWindNoiseReductionToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = HighlightSky,
                                checkedTrackColor = AccentPrimary
                            ),
                            modifier = Modifier.scale(0.75f).testTag("tah6519_wind_switch")
                        )
                    }
                }
            }

            // Sub-Settings: Awareness Slider (If TRANSPARENCY)
            AnimatedVisibility(
                visible = settings.ancMode == "TRANSPARENCY",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                            Text(
                                text = "Awareness Geluidsniveau",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Niveau ${settings.transparencyIntensity} / 5",
                                color = HighlightSky,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Slider(
                            value = settings.transparencyIntensity.toFloat(),
                            onValueChange = { onTransparencyIntensityChange(it.toInt()) },
                            valueRange = 1f..5f,
                            steps = 3,
                            colors = SliderDefaults.colors(
                                activeTrackColor = HighlightSky,
                                thumbColor = HighlightSky,
                                inactiveTrackColor = DarkBorder
                            ),
                            modifier = Modifier.testTag("tah6519_awareness_slider")
                        )
                    }

                    // Speak to awareness
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
                                onCheckedChange = onSpeakToAwarenessToggle,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = HighlightSky,
                                    checkedTrackColor = AccentPrimary
                                ),
                                modifier = Modifier.scale(0.75f).testTag("tah6519_speak_to_awareness_switch")
                            )
                        }
                    }
                }
            }

            // Sub-Settings: Off Info (If OFF)
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
                            text = "De over-ear kussens bieden passieve isolatie (~20 dB) zonder accugebruik.",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
