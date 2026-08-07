package com.example.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
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
import kotlinx.coroutines.launch

@Composable
fun Tah6519PairingSetupScreen(
    viewModel: HeadphoneViewModel,
    onClose: () -> Unit,
    onNavigateToTab: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()

    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 4

    // Simulation states
    var isSimulatingHold by remember { mutableStateOf(false) }
    var holdCountdown by remember { mutableStateOf(5) }
    var simulationPairingActive by remember { mutableStateOf(false) }
    var audioTestPlaying by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tah6519_pairing_setup_screen"),
        color = DarkBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(AccentPrimary, HighlightSky)
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bluetooth,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Philips TAH6519 Instellen",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Interactieve Bluetooth Koppeling Handleiding",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("btn_close_pairing_setup")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Sluiten",
                        tint = TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Step Progress Bar and Pills
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkPanel),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STAP $currentStep VAN $totalSteps",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HighlightSky,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = when (currentStep) {
                                1 -> "Koppelstand Activeren"
                                2 -> "Bluetooth Inschakelen"
                                3 -> "Selecteer Headset"
                                else -> "Koppeling Voltooid"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { currentStep.toFloat() / totalSteps.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AccentPrimary,
                        trackColor = DarkBorder
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Interactive Step Indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (stepIdx in 1..totalSteps) {
                            val isCompleted = stepIdx < currentStep
                            val isCurrent = stepIdx == currentStep

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        when {
                                            isCurrent -> AccentPrimary.copy(alpha = 0.2f)
                                            isCompleted -> StatusSuccess.copy(alpha = 0.15f)
                                            else -> DarkCard
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = when {
                                            isCurrent -> AccentPrimary
                                            isCompleted -> StatusSuccess
                                            else -> DarkBorder
                                        },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { currentStep = stepIdx }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("step_pill_$stepIdx"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isCompleted) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusSuccess,
                                        modifier = Modifier.size(12.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(
                                                color = if (isCurrent) AccentPrimary else TextMuted.copy(alpha = 0.3f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stepIdx.toString(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) Color.White else TextMuted
                                        )
                                    }
                                }

                                Text(
                                    text = "Stap $stepIdx",
                                    fontSize = 10.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) HighlightSky else TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Scrollable Content for Current Step
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    label = "step_content_anim"
                ) { step ->
                    when (step) {
                        1 -> StepOneContent(
                            isSimulatingHold = isSimulatingHold,
                            holdCountdown = holdCountdown,
                            simulationPairingActive = simulationPairingActive,
                            onStartHoldSimulation = {
                                if (!isSimulatingHold) {
                                    isSimulatingHold = true
                                    holdCountdown = 5
                                    viewModel.playProceduralTone(523, 200)
                                    coroutineScope.launch {
                                        while (holdCountdown > 0) {
                                            delay(1000)
                                            holdCountdown--
                                            if (holdCountdown > 0) {
                                                viewModel.playProceduralTone(440 + (5 - holdCountdown) * 80, 150)
                                            }
                                        }
                                        isSimulatingHold = false
                                        simulationPairingActive = true
                                        viewModel.playProceduralTone(880, 400)
                                    }
                                }
                            },
                            onResetSimulation = {
                                isSimulatingHold = false
                                holdCountdown = 5
                                simulationPairingActive = false
                            }
                        )

                        2 -> StepTwoContent(
                            onOpenSystemBtSettings = {
                                try {
                                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        )

                        3 -> StepThreeContent(
                            isConnected = settings.connected,
                            connectedDeviceName = settings.connectedDeviceName,
                            onConnectDevice = {
                                viewModel.connectDevice("Philips TAH6519")
                                viewModel.playProceduralTone(659, 300)
                            }
                        )

                        else -> StepFourContent(
                            settingsConnected = settings.connected,
                            audioTestPlaying = audioTestPlaying,
                            onPlayTestChime = {
                                audioTestPlaying = true
                                viewModel.playProceduralTone(523, 300)
                                coroutineScope.launch {
                                    delay(350)
                                    viewModel.playProceduralTone(659, 300)
                                    delay(350)
                                    viewModel.playProceduralTone(784, 500)
                                    delay(500)
                                    audioTestPlaying = false
                                }
                            },
                            onNavigateToTab = { tab ->
                                onClose()
                                onNavigateToTab(tab)
                            }
                        )
                    }
                }
            }

            // Bottom Navigation Actions
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkPanel),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_pairing_prev"),
                            border = BorderStroke(1.dp, DarkBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("VORIGE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Button(
                        onClick = {
                            if (currentStep < totalSteps) {
                                currentStep++
                                viewModel.playProceduralTone(600, 100)
                            } else {
                                onClose()
                            }
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                            .testTag("btn_pairing_next"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (currentStep < totalSteps) "VOLGENDE STAP" else "SETUP VOLTOOIEN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (currentStep < totalSteps) Icons.Filled.ArrowForward else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepOneContent(
    isSimulatingHold: Boolean,
    holdCountdown: Int,
    simulationPairingActive: Boolean,
    onStartHoldSimulation: () -> Unit,
    onResetSimulation: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Step Title Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PowerSettingsNew,
                contentDescription = null,
                tint = HighlightSky,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Stap 1: Zet TAH6519 in Koppelstand",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Generated Step 1 Image Card with Fallback
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Try rendering image from drawables
                var imageLoadFailed by remember { mutableStateOf(false) }

                if (!imageLoadFailed) {
                    Image(
                        painter = painterResource(id = R.drawable.img_pairing_step1),
                        contentDescription = "Philips TAH6519 Koppelstand LED",
                        modifier = Modifier.fillMaxSize().testTag("pairing_step_image_1"),
                        contentScale = ContentScale.Crop
                    )
                }

                // Overlay pulse indicator if image failed or as graphic accent
                if (imageLoadFailed) {
                    CanvasPairingDiagram(step = 1, pairingActive = simulationPairingActive)
                }

                // Interactive Badge overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .background(DarkPanel.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                        .border(1.dp, HighlightSky.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (simulationPairingActive) StatusDanger else StatusSuccess,
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (simulationPairingActive) "LED: Blauw/Rood Knipperend" else "Power Knop: Rechter Cup",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighlightSky
                        )
                    }
                }
            }
        }

        // Written Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkPanel),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Instructies voor Rechter Oorschelp:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighlightSky
                )
                Text(
                    text = "1. Zorg dat de Philips TAH6519 is uitgeschakeld.\n" +
                            "2. Houd de Power/Bluetooth knop op de rechter cup 5 seconden ingedrukt.\n" +
                            "3. Laat de knop pas los zodra de LED-indicator afwisselend BLAUW en ROOD knippert met een geluidssignaal.",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
            }
        }

        // Interactive Simulation Tool
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "🎮 Test Koppelstand Simulatie",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                if (isSimulatingHold) {
                    Text(
                        text = "Power-knop ingedrukt... $holdCountdown sec",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = StatusYellow
                    )
                    LinearProgressIndicator(
                        progress = { (5 - holdCountdown) / 5f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = StatusYellow,
                        trackColor = DarkBorder
                    )
                } else if (simulationPairingActive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FlashingLedIndicator()
                        Text(
                            text = "Koppelstand Actief! (LED Knippert Blauw/Rood)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusSuccess
                        )
                    }

                    OutlinedButton(
                        onClick = onResetSimulation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_reset_simulation"),
                        border = BorderStroke(1.dp, DarkBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Simulatie", fontSize = 11.sp, color = TextMuted)
                    }
                } else {
                    Button(
                        onClick = onStartHoldSimulation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("btn_simulate_hold_power"),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simuleer 5s Knop Ingedrukt", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepTwoContent(
    onOpenSystemBtSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Bluetooth,
                contentDescription = null,
                tint = HighlightSky,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Stap 2: Bluetooth Inschakelen",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Generated Step 2 Image Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                var imageLoadFailed by remember { mutableStateOf(false) }

                if (!imageLoadFailed) {
                    Image(
                        painter = painterResource(id = R.drawable.img_pairing_step2),
                        contentDescription = "Bluetooth Inschakelen Instellingen",
                        modifier = Modifier.fillMaxSize().testTag("pairing_step_image_2"),
                        contentScale = ContentScale.Crop
                    )
                }

                if (imageLoadFailed) {
                    CanvasPairingDiagram(step = 2, pairingActive = false)
                }
            }
        }

        // Action Card to Open Bluetooth Settings directly
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkPanel),
            border = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "⚙️ Systeem-Instellingen Snelkoppeling",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighlightSky
                )

                Text(
                    text = "Open de Bluetooth instellingen op je Android toestel om te controleren dat Bluetooth AAN staat en zoekt naar nieuwe apparaten.",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    lineHeight = 17.sp
                )

                Button(
                    onClick = onOpenSystemBtSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("btn_open_bt_settings"),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Bluetooth Systeeminstellingen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StepThreeContent(
    isConnected: Boolean,
    connectedDeviceName: String,
    onConnectDevice: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Headphones,
                contentDescription = null,
                tint = HighlightSky,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Stap 3: Selecteer 'Philips TAH6519'",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Generated Step 3 Image Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                var imageLoadFailed by remember { mutableStateOf(false) }

                if (!imageLoadFailed) {
                    Image(
                        painter = painterResource(id = R.drawable.img_pairing_step3),
                        contentDescription = "Selecteer Philips TAH6519 uit lijst",
                        modifier = Modifier.fillMaxSize().testTag("pairing_step_image_3"),
                        contentScale = ContentScale.Crop
                    )
                }

                if (imageLoadFailed) {
                    CanvasPairingDiagram(step = 3, pairingActive = false)
                }
            }
        }

        // Direct Connect Action
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkPanel),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "📱 Koppelingsverzoek Bevestigen",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighlightSky
                )

                Text(
                    text = "Klik op 'Philips TAH6519' in je apparatenlijst. Tik op 'Koppelen' als je telefoon om toestemming vraagt voor audio en oproepen.",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    lineHeight = 17.sp
                )

                if (isConnected) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StatusSuccess.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, StatusSuccess, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.BluetoothConnected, contentDescription = null, tint = StatusSuccess)
                        Text(
                            text = "Koptelefoon $connectedDeviceName is nu Actief Verbonden!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusSuccess
                        )
                    }
                } else {
                    Button(
                        onClick = onConnectDevice,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("btn_direct_connect_tah6519"),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.BluetoothConnected, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verbind Nu Met TAH6519 (App-Simulatie)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepFourContent(
    settingsConnected: Boolean,
    audioTestPlaying: Boolean,
    onPlayTestChime: () -> Unit,
    onNavigateToTab: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = StatusSuccess,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Stap 4: Succesvol Verbonden!",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Generated Step 4 Image Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                var imageLoadFailed by remember { mutableStateOf(false) }

                if (!imageLoadFailed) {
                    Image(
                        painter = painterResource(id = R.drawable.img_pairing_step4),
                        contentDescription = "Philips TAH6519 Verbinding Voltooid",
                        modifier = Modifier.fillMaxSize().testTag("pairing_step_image_4"),
                        contentScale = ContentScale.Crop
                    )
                }

                if (imageLoadFailed) {
                    CanvasPairingDiagram(step = 4, pairingActive = true)
                }
            }
        }

        // Interactive Audio Chime Test
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkPanel),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "🔊 Test je Audio Verbinding",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighlightSky
                )

                Text(
                    text = "Speel een ruimtelijke test-toon af om te controleren of het geluid helder doorkomt op zowel de linker als rechter oorschelp.",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    lineHeight = 17.sp
                )

                Button(
                    onClick = onPlayTestChime,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("btn_test_audio_chime"),
                    colors = ButtonDefaults.buttonColors(containerColor = if (audioTestPlaying) StatusYellow else AccentPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (audioTestPlaying) Icons.Filled.GraphicEq else Icons.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (audioTestPlaying) "Test-Toon Wordt Afgespeeld..." else "Speel Spatial Test Chime Af",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (audioTestPlaying) Color.Black else Color.White
                    )
                }
            }
        }

        // Quick Shortcuts
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "🚀 Directe Snelkoppelingen",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onNavigateToTab("eq") },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_shortcut_eq"),
                        border = BorderStroke(1.dp, DarkBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Equalizer, contentDescription = null, modifier = Modifier.size(14.dp), tint = HighlightSky)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Equalizer", fontSize = 11.sp, color = TextPrimary)
                    }

                    OutlinedButton(
                        onClick = { onNavigateToTab("anc") },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_shortcut_anc"),
                        border = BorderStroke(1.dp, DarkBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Headphones, contentDescription = null, modifier = Modifier.size(14.dp), tint = HighlightSky)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ANC Stand", fontSize = 11.sp, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashingLedIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "flashing_led")
    val ledAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "led_alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(AccentPrimary.copy(alpha = ledAlpha), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(StatusDanger.copy(alpha = 1.0f - ledAlpha), CircleShape)
        )
    }
}

@Composable
private fun CanvasPairingDiagram(step: Int, pairingActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "canvas_diag")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radius_diag"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f

        val headphoneColor = TextMuted.copy(alpha = 0.6f)

        // Draw Headband
        drawArc(
            color = headphoneColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            style = Stroke(width = 6.dp.toPx()),
            topLeft = Offset(centerX - 40.dp.toPx(), centerY - 45.dp.toPx()),
            size = Size(80.dp.toPx(), 80.dp.toPx())
        )

        // Left Cup
        drawRoundRect(
            color = headphoneColor,
            topLeft = Offset(centerX - 48.dp.toPx(), centerY - 15.dp.toPx()),
            size = Size(16.dp.toPx(), 40.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
        )

        // Right Cup
        drawRoundRect(
            color = AccentPrimary,
            topLeft = Offset(centerX + 32.dp.toPx(), centerY - 15.dp.toPx()),
            size = Size(16.dp.toPx(), 40.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
        )

        if (pairingActive || step == 1) {
            val buttonX = centerX + 40.dp.toPx()
            val buttonY = centerY + 5.dp.toPx()
            drawCircle(
                color = AccentPrimary,
                radius = pulseRadius.dp.toPx(),
                center = Offset(buttonX, buttonY),
                alpha = 0.5f
            )
        }
    }
}
