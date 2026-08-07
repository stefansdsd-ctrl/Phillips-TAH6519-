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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
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

@Composable
fun Tah6519AncController(
    viewModel: HeadphoneViewModel,
    settings: HeadphoneSettings,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Environment Scan state
    var isScanningAmbient by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(0f) }
    var scanRecommendation by remember { mutableStateOf<ScanResult?>(null) }

    // Speak to awareness simulation state
    var isSpeakSimulationActive by remember { mutableStateOf(false) }
    var speakCountdown by remember { mutableStateOf(5) }
    var savedAncModeBeforeSpeak by remember { mutableStateOf("ON") }

    // Wind simulation state
    var isWindSimulationActive by remember { mutableStateOf(false) }
    var simulatedWindSpeed by remember { mutableStateOf(0f) }

    // Wave/ambient animation helper
    val transition = rememberInfiniteTransition(label = "anc_panel_waves")
    val phaseOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_offset"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tah6519_anc_controller"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // MAIN MODE SELECTOR
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkPanel),
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ACTIEVE RUISONDERDRUKKING (ANC)",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (settings.ancMode) {
                                "ON" -> "Hybrid ANC Actief (-56 dB)"
                                "TRANSPARENCY" -> "Aura Awareness Mode"
                                else -> "Uit (Passieve Isolatie)"
                            },
                            color = when (settings.ancMode) {
                                "ON" -> StatusSuccess
                                "TRANSPARENCY" -> HighlightSky
                                else -> TextMuted
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Simple quick master switch
                    Switch(
                        checked = settings.ancMode != "OFF",
                        onCheckedChange = { checked ->
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            if (checked) viewModel.setAncMode("ON") else viewModel.setAncMode("OFF")
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = HighlightSky,
                            checkedTrackColor = AccentPrimary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBg
                        ),
                        modifier = Modifier.scale(0.85f).testTag("anc_master_switch")
                    )
                }

                // Wave Animation Canvas (Dynamic sound-wave representation)
                NoiseControlVisualizer(activeMode = settings.ancMode)

                // The Three Large Toggle Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard, shape = RoundedCornerShape(16.dp))
                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val modes = listOf(
                        Triple("ON", "ANC On", Icons.Filled.GraphicEq),
                        Triple("TRANSPARENCY", "Awareness", Icons.Filled.Hearing),
                        Triple("OFF", "ANC Off", Icons.Filled.Close)
                    )

                    modes.forEach { (mode, label, icon) ->
                        val isSelected = settings.ancMode == mode
                        val activeThemeColor = when (mode) {
                            "ON" -> AccentPrimary
                            "TRANSPARENCY" -> HighlightSky
                            else -> TextMuted
                        }

                        val bgAnimateColor by animateColorAsState(
                            targetValue = if (isSelected) activeThemeColor else Color.Transparent,
                            animationSpec = tween(durationMillis = 200),
                            label = "tah_bg"
                        )
                        val tintAnimateColor by animateColorAsState(
                            targetValue = if (isSelected) Color.White else TextMuted,
                            animationSpec = tween(durationMillis = 200),
                            label = "tah_tint"
                        )
                        val scaleAnimate by animateFloatAsState(
                            targetValue = if (isSelected) 1.04f else 1.0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy),
                            label = "tah_scale"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .scale(scaleAnimate)
                                .background(bgAnimateColor, shape = RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) activeThemeColor.copy(alpha = 0.5f) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    viewModel.setAncMode(mode)
                                }
                                .testTag("btn_anc_mode_$mode"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = tintAnimateColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    color = tintAnimateColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = settings.ancMode == "ON"
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text(
                            text = "ANC Niveau Handmatig",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val levels = listOf(
                                Triple(1, "Light Focus", "Kantoor"),
                                Triple(2, "Adaptief", "Reizen"),
                                Triple(3, "Deep Silence", "Metro/Max")
                            )
                            levels.forEach { (lvl, name, usage) ->
                                val isSelected = settings.ancLevel == lvl
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .background(
                                            if (isSelected) AccentPrimary.copy(alpha = 0.15f) else DarkCard,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) AccentPrimary else DarkBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            viewModel.setAncLevel(lvl)
                                        }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = name,
                                            color = if (isSelected) HighlightSky else TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = usage,
                                            color = TextMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = settings.ancMode == "TRANSPARENCY"
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        TransparencyAwarenessSliderControl(
                            settings = settings,
                            onIntensityChanged = { level -> viewModel.setTransparencyIntensity(level) }
                        )
                    }
                }

                // Dynamic Contextual Advice
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = when (settings.ancMode) {
                            "ON" -> "🧠 Hybrid ANC: Filtert constant storende lage en middenfrequente geluiden weg zoals motoren, ventilatoren en omgevingsrumoer. Biedt tot 56 dB reductie voor maximale concentratie."
                            "TRANSPARENCY" -> "🎤 Aura Sound Transparency: Gebruikt de TAH6519 microfoons om spraak en externe waarschuwingen helder door te geven. Je hoeft je koptelefoon niet af te zetten om te praten."
                            else -> "🔋 ANC Uitgeschakeld: Bespaart maximale batterij (tot 80 uur speeltijd). De comfortabele, afsluitende oorkussens verminderen omgevingsgeluid nog steeds met ca. 20 dB via passieve demping."
                        },
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // DYNAMIC ENV SCANNER (NEW IMPROVEMENT)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkPanel),
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(HighlightSky.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = null,
                                tint = HighlightSky,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Intelligente Omgevingsscanner",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Meet live dB om de optimale ANC in te stellen",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    if (!isScanningAmbient) {
                        Button(
                            onClick = {
                                isScanningAmbient = true
                                scanProgress = 0f
                                scanRecommendation = null
                                scope.launch {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    for (i in 1..25) {
                                        delay(100)
                                        scanProgress = i / 25f
                                    }
                                    val randomDb = (40..92).random()
                                    viewModel.setAmbientEnvironmentDb(randomDb)
                                    scanRecommendation = getScanResultForDb(randomDb)
                                    isScanningAmbient = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkCard,
                                contentColor = HighlightSky
                            ),
                            border = BorderStroke(1.dp, HighlightSky.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp).testTag("btn_start_ambient_scan")
                        ) {
                            Text("Scan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Scanning Progress bar
                if (isScanningAmbient) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Microfoons kalibreren...", color = HighlightSky, fontSize = 11.sp)
                            Text("${(scanProgress * 100).toInt()}%", color = TextPrimary, fontSize = 11.sp)
                        }
                        LinearProgressIndicator(
                            progress = { scanProgress },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = HighlightSky,
                            trackColor = DarkBorder
                        )
                    }
                }

                // Scan Result Recommendation
                val currentDb = settings.ambientEnvironmentDb
                val defaultResult = getScanResultForDb(currentDb)
                val activeResult = scanRecommendation ?: defaultResult

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decibel Radial Gauge
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = DarkBorder,
                                style = Stroke(width = 4.dp.toPx())
                            )
                            val sweepAngle = (currentDb / 120f) * 360f
                            drawArc(
                                color = when {
                                    currentDb < 55 -> StatusSuccess
                                    currentDb < 75 -> HighlightSky
                                    else -> StatusDanger
                                },
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$currentDb",
                                color = TextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Text(text = "dB", color = TextMuted, fontSize = 8.sp)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Omgeving: ${activeResult.environmentName}",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Advies: ${activeResult.recommendationText}",
                            color = TextMuted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.setAncMode(activeResult.recommendedMode)
                            if (activeResult.recommendedMode == "ON") {
                                viewModel.setAncLevel(activeResult.recommendedLevel)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeResult.accentColor.copy(alpha = 0.15f),
                            contentColor = activeResult.accentColor
                        ),
                        border = BorderStroke(1.dp, activeResult.accentColor.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(30.dp).testTag("btn_apply_recommendation")
                    ) {
                        Text("Pas toe", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // SPEAK-TO-AWARENESS / CONVERSATION AWARENESS (NEW IMPROVEMENT)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkPanel),
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(AccentPrimary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.RecordVoiceOver,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Spraak-naar-Awareness",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Auto-Awareness & volume dempen als je praat",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Switch(
                        checked = settings.speakToAwarenessEnabled,
                        onCheckedChange = { viewModel.toggleSpeakToAwareness(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = HighlightSky,
                            checkedTrackColor = AccentPrimary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBg
                        ),
                        modifier = Modifier.scale(0.85f).testTag("speak_to_awareness_switch")
                    )
                }

                if (settings.speakToAwarenessEnabled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkCard, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Gevoeligheid microfoons:",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        // 3-Segment selector for voice sensitivity
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1 to "Laag", 2 to "Medium", 3 to "Hoog").forEach { (level, name) ->
                                val active = settings.speakToAwarenessSensitivity == level
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                        .background(
                                            if (active) AccentPrimary else DarkBg,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (active) AccentPrimary else DarkBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            viewModel.setSpeakToAwarenessSensitivity(level)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name,
                                        color = if (active) Color.White else TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // SPEAK SIMULATION TRIGGER
                        if (!isSpeakSimulationActive) {
                            Button(
                                onClick = {
                                    isSpeakSimulationActive = true
                                    savedAncModeBeforeSpeak = settings.ancMode
                                    speakCountdown = 5
                                    viewModel.setAncMode("TRANSPARENCY")
                                    scope.launch {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        while (speakCountdown > 0) {
                                            delay(1000)
                                            speakCountdown -= 1
                                        }
                                        viewModel.setAncMode(savedAncModeBeforeSpeak)
                                        isSpeakSimulationActive = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StatusSuccess.copy(alpha = 0.15f),
                                    contentColor = StatusSuccess
                                ),
                                border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(36.dp).testTag("btn_simulate_speaking")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Simuleer Spreken (Test activeert live)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            // ACTIVE COUNTDOWN SIMULATION
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(StatusSuccess.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .border(1.dp, StatusSuccess.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(StatusSuccess, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Spraak gedetecteerd! Geluid gedempt...",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "Restauratie in ${speakCountdown}s",
                                    color = StatusSuccess,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // AUTO-WIND SHIELDING & GUSTS SIMULATION (NEW IMPROVEMENT)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkPanel),
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(HighlightSky.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Air,
                                contentDescription = null,
                                tint = HighlightSky,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Auto-Windruisschild",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Demp stormgeruis op de microfoons automatisch",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Switch(
                        checked = settings.autoWindShieldingEnabled,
                        onCheckedChange = { viewModel.toggleAutoWindShielding(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = HighlightSky,
                            checkedTrackColor = AccentPrimary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBg
                        ),
                        modifier = Modifier.scale(0.85f).testTag("auto_wind_shield_switch")
                    )
                }

                // Standard Wind Shielding Toggle as well
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard, RoundedCornerShape(12.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Windruisonderdrukking handmatig",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Filtert direct windgeluiden weg op de oorschelpen",
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }
                    Switch(
                        checked = settings.windNoiseReductionEnabled,
                        onCheckedChange = { viewModel.toggleWindNoiseReduction(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = HighlightSky,
                            checkedTrackColor = AccentPrimary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBg
                        ),
                        modifier = Modifier.scale(0.75f).testTag("manual_wind_switch")
                    )
                }

                // Interactive Wind Tunnel Canvas Simulation
                if (!isWindSimulationActive) {
                    Button(
                        onClick = {
                            isWindSimulationActive = true
                            scope.launch {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                // Gradually ramp up simulated wind speed
                                for (step in 1..20) {
                                    delay(80)
                                    simulatedWindSpeed = (step / 20f) * 12f // up to 12 m/s
                                    // If auto shielding is on and wind is heavy (> 5 m/s), auto-activate wind reduction!
                                    if (settings.autoWindShieldingEnabled && simulatedWindSpeed > 5f && !settings.windNoiseReductionEnabled) {
                                        viewModel.toggleWindNoiseReduction(true)
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    }
                                }
                                delay(2000)
                                // Ramp down
                                for (step in 20 downTo 0) {
                                    delay(80)
                                    simulatedWindSpeed = (step / 20f) * 12f
                                }
                                isWindSimulationActive = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkCard,
                            contentColor = HighlightSky
                        ),
                        border = BorderStroke(1.dp, HighlightSky.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp).testTag("btn_simulate_wind_tunnel")
                    ) {
                        Text("Start Windtunnel Test (Simuleer storm)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkCard, RoundedCornerShape(12.dp))
                            .border(1.dp, HighlightSky.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Windtunnel Test Actief",
                                color = HighlightSky,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Windsnelheid: ${String.format("%.1f", simulatedWindSpeed)} m/s",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        // Custom wind visualizer on canvas
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            val w = size.width
                            val h = size.height
                            val strokeWidth = 2.dp.toPx()

                            // Draw horizontal flowing lines representing wind
                            val lineCount = 3
                            for (row in 0 until lineCount) {
                                val path = Path()
                                val startY = h / (lineCount + 1) * (row + 1)
                                path.moveTo(0f, startY)

                                val segments = 30
                                for (seg in 0..segments) {
                                    val x = (seg.toFloat() / segments) * w
                                    // Sinusoid amplitude depends on wind speed
                                    val amp = (simulatedWindSpeed * 2.5f) * sin(seg.toFloat() * 0.4f + phaseOffset * 2f)
                                    path.lineTo(x, startY + amp)
                                }

                                drawPath(
                                    path = path,
                                    color = if (settings.windNoiseReductionEnabled) {
                                        StatusSuccess.copy(alpha = 0.4f) // Attenuated wind
                                    } else {
                                        HighlightSky.copy(alpha = 0.7f) // Loud raw wind
                                    },
                                    style = Stroke(
                                        width = if (settings.windNoiseReductionEnabled) strokeWidth / 2 else strokeWidth,
                                        cap = StrokeCap.Round
                                    )
                                )
                            }
                        }

                        if (settings.windNoiseReductionEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = StatusSuccess,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Windruisschild Actief! Windgeluid verminderd met 92%",
                                    color = StatusSuccess,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = StatusDanger,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Zware windruis gedetecteerd! Schakel het windruisschild in.",
                                    color = StatusDanger,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Struct of scanner recommendation results
private data class ScanResult(
    val environmentName: String,
    val recommendationText: String,
    val recommendedMode: String,
    val recommendedLevel: Int,
    val accentColor: Color
)

private fun getScanResultForDb(db: Int): ScanResult {
    return when {
        db < 52 -> ScanResult(
            environmentName = "Stille kamer / Bibliotheek",
            recommendationText = "Lage geluidsdruk. ANC kan uitblijven of op Light Focus voor maximale accu.",
            recommendedMode = "OFF",
            recommendedLevel = 1,
            accentColor = StatusSuccess
        )
        db < 72 -> ScanResult(
            environmentName = "Kantoor / Huiskamer",
            recommendationText = "Middelmatige ruis. Light Focus ANC filtert achtergrondbrom perfect weg.",
            recommendedMode = "ON",
            recommendedLevel = 1,
            accentColor = HighlightSky
        )
        db < 84 -> ScanResult(
            environmentName = "Stadsverkeer / Treincoupé",
            recommendationText = "Luid omgevingsgeluid. Adaptieve ANC dempt spoor- en motorrumoer dynamisch.",
            recommendedMode = "ON",
            recommendedLevel = 2,
            accentColor = AccentPrimary
        )
        else -> ScanResult(
            environmentName = "Metrogang / Vliegtuigcabine",
            recommendationText = "Kritiek geluidsniveau! Deep Silence ANC (-56 dB) is sterk aanbevolen om gehoor te beschermen.",
            recommendedMode = "ON",
            recommendedLevel = 3,
            accentColor = StatusDanger
        )
    }
}

@Composable
fun NoiseControlVisualizer(activeMode: String) {
    val transition = rememberInfiniteTransition(label = "anc_waves")
    val phaseOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(DarkBg, shape = RoundedCornerShape(16.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(16.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            val boundaryX = width / 2f
            
            when (activeMode) {
                "ON" -> {
                    // ANC On: External sound waves canceled by inverse waves, resulting in a flat inner line
                    val wavePoints = 120
                    val extPath = Path()
                    val antiPath = Path()
                    val innerPath = Path()
                    val midPoint = boundaryX
                    
                    for (i in 0..wavePoints) {
                        val fraction = i.toFloat() / wavePoints
                        val x = fraction * midPoint
                        val extAmp = 18.dp.toPx() * (1f - fraction * 0.3f)
                        
                        val extY = centerY + extAmp * kotlin.math.sin(fraction * 4f * Math.PI.toFloat() + phaseOffset)
                        if (i == 0) extPath.moveTo(x, extY) else extPath.lineTo(x, extY)
                        
                        val antiY = centerY + extAmp * kotlin.math.sin(fraction * 4f * Math.PI.toFloat() + phaseOffset + Math.PI.toFloat())
                        if (i == 0) antiPath.moveTo(x, antiY) else antiPath.lineTo(x, antiY)
                    }
                    
                    for (i in 0..wavePoints) {
                        val fraction = i.toFloat() / wavePoints
                        val x = midPoint + fraction * (width - midPoint)
                        val residualAmp = 1.2f.dp.toPx()
                        val residualY = centerY + residualAmp * kotlin.math.sin(fraction * 6f * Math.PI.toFloat() - phaseOffset * 2f)
                        if (i == 0) innerPath.moveTo(x, residualY) else innerPath.lineTo(x, residualY)
                    }
                    
                    drawPath(
                        path = extPath,
                        color = StatusDanger.copy(alpha = 0.5f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                    
                    drawPath(
                        path = antiPath,
                        color = HighlightSky.copy(alpha = 0.5f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                    
                    drawPath(
                        path = innerPath,
                        color = StatusSuccess.copy(alpha = 0.9f),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    
                    drawLine(
                        color = AccentPrimary,
                        start = Offset(boundaryX, 15.dp.toPx()),
                        end = Offset(boundaryX, height - 15.dp.toPx()),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                "TRANSPARENCY" -> {
                    // Transparency Mode: Wave flows through dashed microphone barrier representing awareness
                    val wavePoints = 200
                    val flowPath = Path()
                    
                    for (i in 0..wavePoints) {
                        val fraction = i.toFloat() / wavePoints
                        val x = fraction * width
                        val amp = 14.dp.toPx()
                        val y = centerY + amp * kotlin.math.sin(fraction * 5f * Math.PI.toFloat() - phaseOffset)
                        if (i == 0) flowPath.moveTo(x, y) else flowPath.lineTo(x, y)
                    }
                    
                    drawPath(
                        path = flowPath,
                        color = HighlightSky.copy(alpha = 0.85f),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    
                    val pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                    drawLine(
                        color = HighlightSky.copy(alpha = 0.4f),
                        start = Offset(boundaryX, 15.dp.toPx()),
                        end = Offset(boundaryX, height - 15.dp.toPx()),
                        strokeWidth = 4.dp.toPx(),
                        pathEffect = pathEffect,
                        cap = StrokeCap.Round
                    )
                }
                else -> {
                    // Off Mode: Sound waves on the outside, attenuated by earcup barrier, low waves inside
                    val wavePoints = 120
                    val extPath = Path()
                    val innerPath = Path()
                    val midPoint = boundaryX
                    
                    for (i in 0..wavePoints) {
                        val fraction = i.toFloat() / wavePoints
                        val x = fraction * midPoint
                        val amp = 16.dp.toPx()
                        val y = centerY + amp * kotlin.math.sin(fraction * 4f * Math.PI.toFloat() + phaseOffset)
                        if (i == 0) extPath.moveTo(x, y) else extPath.lineTo(x, y)
                    }
                    
                    for (i in 0..wavePoints) {
                        val fraction = i.toFloat() / wavePoints
                        val x = midPoint + fraction * (width - midPoint)
                        val amp = 5.dp.toPx()
                        val y = centerY + amp * kotlin.math.sin(fraction * 4f * Math.PI.toFloat() + phaseOffset)
                        if (i == 0) innerPath.moveTo(x, y) else innerPath.lineTo(x, y)
                    }
                    
                    drawPath(
                        path = extPath,
                        color = TextMuted.copy(alpha = 0.6f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                    
                    drawPath(
                        path = innerPath,
                        color = TextMuted.copy(alpha = 0.3f),
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    
                    drawLine(
                        color = DarkBorder,
                        start = Offset(boundaryX, 15.dp.toPx()),
                        end = Offset(boundaryX, height - 15.dp.toPx()),
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Omgevingsgeluid (Extern)",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "In-Ear Audio (Intern)",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
