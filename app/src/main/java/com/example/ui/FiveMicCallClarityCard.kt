package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HeadphoneSettings
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FiveMicCallClarityCard(
    viewModel: HeadphoneViewModel,
    settings: HeadphoneSettings,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isTestingMic by remember { mutableStateOf(false) }
    var aiNoiseReductionEnabled by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_radar_pulse")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("five_mic_call_clarity_card"),
        colors = CardDefaults.cardColors(containerColor = DarkPanel),
        border = BorderStroke(1.dp, DarkBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
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
                            .clip(CircleShape)
                            .background(HighlightSky.copy(alpha = 0.15f))
                            .border(1.dp, HighlightSky.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhoneInTalk,
                            contentDescription = "Mic Array",
                            tint = HighlightSky,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "5-Microphone HD Call Clarity",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "AI Smart Call ruisonderdrukking & beamforming",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .background(StatusSuccess.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp))
                        .border(1.dp, StatusSuccess.copy(alpha = 0.4f), shape = RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "5 Mics Actief",
                        color = StatusSuccess,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Beamforming Mic Array Radar Canvas Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(DarkBg, shape = RoundedCornerShape(12.dp))
                    .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = Math.min(size.width, size.height) * 0.38f

                    // Draw concentric radar rings
                    for (r in listOf(0.3f, 0.65f, 1.0f)) {
                        drawCircle(
                            color = DarkBorder.copy(alpha = 0.6f),
                            radius = radius * r,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // Pulsing soundwave ring
                    drawCircle(
                        color = HighlightSky.copy(alpha = 0.3f * (1f - pulsePhase)),
                        radius = radius * pulsePhase,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // 5 Beamforming Microphones positioned radially
                    val micAngles = listOf(0.0, 72.0, 144.0, 216.0, 288.0)
                    micAngles.forEachIndexed { idx, angleDeg ->
                        val rad = Math.toRadians(angleDeg)
                        val micPos = Offset(
                            (center.x + radius * cos(rad)).toFloat(),
                            (center.y + radius * sin(rad)).toFloat()
                        )

                        // Beam line from center speaker to mic
                        drawLine(
                            color = if (aiNoiseReductionEnabled) HighlightSky.copy(alpha = 0.6f) else TextMuted.copy(alpha = 0.3f),
                            start = center,
                            end = micPos,
                            strokeWidth = 1.5.dp.toPx()
                        )

                        // Mic Point
                        drawCircle(
                            color = HighlightSky,
                            radius = 4.5.dp.toPx(),
                            center = micPos
                        )

                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = micPos
                        )
                    }

                    // Center Voice Pickup Point
                    drawCircle(
                        color = StatusSuccess,
                        radius = 8.dp.toPx(),
                        center = center
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "SNR Signaal: +22 dB",
                        color = StatusSuccess,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Winddemping: Max (-35 dB)",
                        color = TextMuted,
                        fontSize = 8.sp
                    )
                }
            }

            // Controls Grid
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Control 1: AI Smart Call Noise Isolation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg, shape = RoundedCornerShape(10.dp))
                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Psychology,
                            contentDescription = "AI Call",
                            tint = HighlightSky,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "AI Smart Call StemIsolatie",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Filtert verkeer & achtergrondlawaai tijdens bellen",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Switch(
                        checked = aiNoiseReductionEnabled,
                        onCheckedChange = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            aiNoiseReductionEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = HighlightSky,
                            checkedTrackColor = HighlightSky.copy(alpha = 0.4f)
                        )
                    )
                }

                // Control 2: Sidetone (Mic Monitoring)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg, shape = RoundedCornerShape(10.dp))
                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(10.dp))
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.RecordVoiceOver,
                                contentDescription = "Sidetone",
                                tint = StatusYellow,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Sidetone (Eigen stem horen)",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Hoor je eigen stem natuurlijk terug in de headset",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Switch(
                            checked = settings.sidetoneEnabled,
                            onCheckedChange = { enabled ->
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                viewModel.toggleSidetone(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = StatusYellow,
                                checkedTrackColor = StatusYellow.copy(alpha = 0.4f)
                            )
                        )
                    }

                    AnimatedVisibility(visible = settings.sidetoneEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Sidetone Volume", color = TextMuted, fontSize = 10.sp)
                                Text("${settings.sidetoneLevel}%", color = StatusYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = settings.sidetoneLevel.toFloat(),
                                onValueChange = { viewModel.setSidetoneLevel(it.toInt()) },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = StatusYellow,
                                    activeTrackColor = StatusYellow
                                )
                            )
                        }
                    }
                }

                // Control 3: Wind Noise Reduction
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg, shape = RoundedCornerShape(10.dp))
                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Air,
                            contentDescription = "Wind Reduction",
                            tint = StatusOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Windruis Onderdrukking (Buiten)",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Automatische demping van windvlagen op de mics",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Switch(
                        checked = settings.windNoiseReductionEnabled,
                        onCheckedChange = { enabled ->
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            viewModel.toggleWindNoiseReduction(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StatusOrange,
                            checkedTrackColor = StatusOrange.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            // Interactive Microphone Test Loopback Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    isTestingMic = !isTestingMic
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTestingMic) StatusDanger else HighlightSky.copy(alpha = 0.2f)
                ),
                border = BorderStroke(1.dp, if (isTestingMic) StatusDanger else HighlightSky)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isTestingMic) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = "Test Mic",
                        tint = if (isTestingMic) Color.White else HighlightSky,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isTestingMic) "Stop Microfoontest" else "🎙️ Start Live Microfoontest",
                        color = if (isTestingMic) Color.White else HighlightSky,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // Live Mic Audio Waveform Visualizer
            AnimatedVisibility(visible = isTestingMic) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(DarkBg, shape = RoundedCornerShape(8.dp))
                        .border(1.dp, StatusSuccess.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val centerY = height / 2f
                        val wavePath = Path()

                        for (x in 0..width.toInt() step 4) {
                            val amp = 15.dp.toPx() * (0.3f + 0.7f * Math.sin(x.toDouble() * 0.05 + pulsePhase * 10).toFloat())
                            val y = centerY + amp * Math.sin(x.toDouble() * 0.1).toFloat()
                            if (x == 0) wavePath.moveTo(x.toFloat(), y) else wavePath.lineTo(x.toFloat(), y)
                        }

                        drawPath(
                            path = wavePath,
                            color = StatusSuccess,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    Text(
                        text = "Spreek nu... Real-time HD Voice Monitor Actief",
                        color = StatusSuccess,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
