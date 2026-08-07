package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkPanel
import com.example.ui.theme.HighlightSky
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun Tah6519QuickStartModal(
    viewModel: HeadphoneViewModel,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 4

    var isSimulatingPairing by remember { mutableStateOf(false) }
    var simulationProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(isSimulatingPairing) {
        if (isSimulatingPairing) {
            simulationProgress = 0f
            while (simulationProgress < 1f) {
                delay(100)
                simulationProgress += 0.05f
            }
            delay(500)
            isSimulatingPairing = false
            currentStep = 3
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, HighlightSky.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .testTag("quick_start_modal"),
            color = DarkBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Modal Header
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
                                .background(AccentPrimary.copy(alpha = 0.2f))
                                .border(1.dp, AccentPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Headphones,
                                contentDescription = "Quick Start",
                                tint = AccentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "PHILIPS TAH6519",
                                color = HighlightSky,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Snelstart Handleiding & Koppelen",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("quick_start_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Sluiten",
                            tint = TextMuted
                        )
                    }
                }

                // Step Indicator Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (step in 1..totalSteps) {
                        val isCurrent = step == currentStep
                        val isPassed = step < currentStep
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    when {
                                        isCurrent -> HighlightSky
                                        isPassed -> StatusSuccess
                                        else -> DarkPanel
                                    }
                                )
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    currentStep = step
                                }
                        )
                    }
                }

                // Animated Illustration Box
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(1.dp, DarkBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CanvasPairingAnimatedIllustration(
                            step = currentStep,
                            isSimulating = isSimulatingPairing,
                            simulationProgress = simulationProgress
                        )
                    }
                }

                // Step Description Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkPanel),
                    border = BorderStroke(1.dp, DarkBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = when (currentStep) {
                                1 -> "Stap 1: Inschakelen & Koppelmodus Activeer"
                                2 -> "Stap 2: Bluetooth Zoeken op je Telefoon"
                                3 -> "Stap 3: Selecteer 'Philips TAH6519'"
                                else -> "Stap 4: Klaar voor Gebruik & Multipoint"
                            },
                            color = HighlightSky,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (currentStep) {
                                1 -> "Houd de Power/Bluetooth-knop op de rechter oorschelp 5 seconden ingedrukt. De LED-indicator op de hoofdtelefoon knippert afwisselend blauw en rood om de koppelmodus te bevestigen."
                                2 -> "Open de Bluetooth-instellingen op je telefoon, tablet of laptop. Zorg ervoor dat Bluetooth ingeschakeld is en het zoeken naar nieuwe apparaten gestart is."
                                3 -> "Tik op 'Philips TAH6519' in de lijst met beschikbare apparaten. Een zachte gesproken melding ('Connected') en een stabiele blauwe LED geven een succesvolle verbinding aan."
                                else -> "Je Philips TAH6519 is nu verbonden! Geniet van 40 tot 80 uur speelduur en Hybrid Active Noise Canceling. Gebruik de app om eenvoudig tussen 2 verbonden apparaten te schakelen."
                            },
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Action controls row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                currentStep--
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("quick_start_prev_step"),
                            border = BorderStroke(1.dp, DarkBorder),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Vorige",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Vorige", color = TextMuted, fontSize = 12.sp)
                        }
                    }

                    if (currentStep < totalSteps) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                currentStep++
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("quick_start_next_step"),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Volgende", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.ArrowForward,
                                contentDescription = "Volgende",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.connectDevice()
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("quick_start_connect_now"),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Verbinden",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Koppel Nu Direct", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Simulation trigger button
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        isSimulatingPairing = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("quick_start_simulate_pairing"),
                    enabled = !isSimulatingPairing,
                    border = BorderStroke(1.dp, HighlightSky.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Simulatie",
                            tint = HighlightSky,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isSimulatingPairing) "Koppelen Simuleren..." else "Test Live Koppelsimulatie",
                            color = HighlightSky,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CanvasPairingAnimatedIllustration(
    step: Int,
    isSimulating: Boolean,
    simulationProgress: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PairingAnim")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = 90f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    val blinkState by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BlinkState"
    )

    Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        when (step) {
            1 -> {
                // Step 1: Headphone ear cup with power button and flashing Red/Blue LED
                // Headband arc
                drawArc(
                    color = Color(0xFF2C3B57),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(centerX - 60.dp.toPx(), centerY - 60.dp.toPx()),
                    size = Size(120.dp.toPx(), 120.dp.toPx()),
                    style = Stroke(width = 10.dp.toPx())
                )

                // Ear cups
                drawCircle(color = Color(0xFF1F2A3E), radius = 32.dp.toPx(), center = Offset(centerX - 60.dp.toPx(), centerY))
                drawCircle(color = Color(0xFF1F2A3E), radius = 32.dp.toPx(), center = Offset(centerX + 60.dp.toPx(), centerY))

                // Pulsing LED on Right ear cup
                val ledColor = if (blinkState > 0.5f) Color(0xFF0066FF) else Color(0xFFFF1744)
                drawCircle(
                    color = ledColor.copy(alpha = pulseAlpha),
                    radius = pulseRadius,
                    center = Offset(centerX + 60.dp.toPx(), centerY)
                )
                drawCircle(
                    color = ledColor,
                    radius = 8.dp.toPx(),
                    center = Offset(centerX + 60.dp.toPx(), centerY)
                )
            }
            2 -> {
                // Step 2: Phone sending BLE waves searching for headphone
                val phoneX = centerX - 70.dp.toPx()
                val hpX = centerX + 70.dp.toPx()

                // Draw Phone silhouette
                drawRoundRect(
                    color = Color(0xFF1F2A3E),
                    topLeft = Offset(phoneX - 22.dp.toPx(), centerY - 40.dp.toPx()),
                    size = Size(44.dp.toPx(), 80.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Waves emanating from phone
                for (i in 1..3) {
                    val r = (pulseRadius * (i * 0.4f)) % 70.dp.toPx()
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = (1f - (r / 70.dp.toPx())).coerceIn(0f, 1f)),
                        radius = r,
                        center = Offset(phoneX, centerY),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Headphone icon silhouette
                drawCircle(color = Color(0xFF2C3B57), radius = 24.dp.toPx(), center = Offset(hpX, centerY))
            }
            3 -> {
                // Step 3: Connected state with beam and checkmark
                val phoneX = centerX - 60.dp.toPx()
                val hpX = centerX + 60.dp.toPx()

                // Connecting beam
                drawLine(
                    color = Color(0xFF00E676),
                    start = Offset(phoneX, centerY),
                    end = Offset(hpX, centerY),
                    strokeWidth = 4.dp.toPx()
                )

                // Glow at center
                drawCircle(
                    color = Color(0xFF00E676).copy(alpha = pulseAlpha),
                    radius = pulseRadius,
                    center = Offset(centerX, centerY)
                )

                drawCircle(color = Color(0xFF00E676), radius = 18.dp.toPx(), center = Offset(centerX, centerY))
            }
            4 -> {
                // Step 4: Ready with audio wave ripples
                for (i in 1..4) {
                    val radius = (pulseRadius + i * 20.dp.toPx()) % 100.dp.toPx()
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = (1f - radius / 100.dp.toPx()).coerceIn(0f, 1f)),
                        radius = radius,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                drawCircle(color = Color(0xFF0066FF), radius = 36.dp.toPx(), center = Offset(centerX, centerY))
            }
        }

        if (isSimulating) {
            val barWidth = size.width * 0.8f
            val startX = (size.width - barWidth) / 2f
            val y = size.height - 20.dp.toPx()

            drawRoundRect(
                color = Color(0xFF160924),
                topLeft = Offset(startX, y),
                size = Size(barWidth, 8.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )

            drawRoundRect(
                color = Color(0xFF00E5FF),
                topLeft = Offset(startX, y),
                size = Size(barWidth * simulationProgress, 8.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
        }
    }
}
