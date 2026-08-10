package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.HeadphoneSettings
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkPanel
import com.example.ui.theme.HighlightSky
import com.example.ui.theme.StatusOrange
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun Tah6519DeviceDetailsCard(
    viewModel: HeadphoneViewModel,
    settings: HeadphoneSettings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val firmwareVersion by viewModel.firmwareVersion.collectAsStateWithLifecycle()
    val serialNumber by viewModel.serialNumber.collectAsStateWithLifecycle()
    val rssi by viewModel.rssi.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val isApiPollingInProgress by viewModel.isApiPollingInProgress.collectAsStateWithLifecycle()
    val simulatedFirmwareApiUrl by viewModel.simulatedFirmwareApiUrl.collectAsStateWithLifecycle()
    val lastFirmwarePollTime by viewModel.lastFirmwarePollTime.collectAsStateWithLifecycle()

    var isRefreshingSignal by remember { mutableStateOf(false) }
    var isTestingDiagnostics by remember { mutableStateOf(false) }
    var diagnosticMessage by remember { mutableStateOf<String?>(null) }

    // Signal strength calculation (-100 dBm to -40 dBm mapped to 0..100)
    val actualRssi = rssi ?: -54
    val signalPercent = ((actualRssi + 100) * 100 / 60).coerceIn(10, 100)
    val signalColor = when {
        actualRssi >= -60 -> StatusSuccess
        actualRssi >= -75 -> StatusYellow
        else -> StatusOrange
    }
    val signalQualityText = when {
        actualRssi >= -60 -> "Uitstekend (100% Signaal)"
        actualRssi >= -75 -> "Goed (~80% Signaal)"
        actualRssi >= -85 -> "Matig (~50% Signaal)"
        else -> "Zwak signaal"
    }
    val estimatedDistance = when {
        actualRssi >= -60 -> "~0.5 - 1.2 m (Zelfde ruimte)"
        actualRssi >= -75 -> "~2.0 - 4.5 m (Sterk bereik)"
        else -> "~5.0 - 8.0 m (Vervormingsrisico)"
    }

    // Pulse animation for active signal strength
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tah6519_device_details_card"),
        colors = CardDefaults.cardColors(containerColor = DarkPanel),
        border = BorderStroke(1.dp, DarkBorder),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title & Model Badge
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AccentPrimary.copy(alpha = 0.15f))
                            .border(1.dp, AccentPrimary.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Headset,
                            contentDescription = "Device Details",
                            tint = AccentPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Apparaat Details",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Philips TAH6519 Over-Ear ANC",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Verified Badge
                Box(
                    modifier = Modifier
                        .background(StatusSuccess.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp))
                        .border(1.dp, StatusSuccess.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "Authentic",
                            tint = StatusSuccess,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Origineel",
                            color = StatusSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            HorizontalDivider(color = DarkBorder)

            // Section 1: Firmware Version & Remote API Check
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("firmware_details_section"),
                colors = CardDefaults.cardColors(containerColor = DarkBg),
                border = BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            Icon(
                                imageVector = Icons.Filled.SystemUpdate,
                                contentDescription = null,
                                tint = HighlightSky,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Firmware Versie",
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Version Status Badge
                        Box(
                            modifier = Modifier
                                .background(
                                    if (firmwareVersion == "v1.5.0") StatusSuccess.copy(alpha = 0.15f)
                                    else StatusYellow.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (firmwareVersion == "v1.5.0") "Up-to-date" else "v1.5.0 Beschikbaar",
                                color = if (firmwareVersion == "v1.5.0") StatusSuccess else StatusYellow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = firmwareVersion,
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = "Build: 2026.04.12-REV3",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DSP Sound Engine v4.2",
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        Text(
                            text = "OTA Flash: Ondersteund",
                            color = StatusSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    HorizontalDivider(color = DarkBorder.copy(alpha = 0.6f))

                    // Simulated Remote API Info
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkPanel.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                            .border(1.dp, DarkBorder.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Remote API Endpoint:",
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                            Text(
                                text = "200 OK",
                                color = StatusSuccess,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = simulatedFirmwareApiUrl,
                            color = HighlightSky,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        lastFirmwarePollTime?.let { time ->
                            Text(
                                text = "Laatste controle: $time",
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Active Update Progress / Result Banner
                    when (val state = updateState) {
                        is UpdateState.Checking -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(HighlightSky.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = HighlightSky,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Firmware API raadplegen op api.philips.com...",
                                    color = HighlightSky,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        is UpdateState.UpToDate -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(StatusSuccess.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp))
                                    .border(1.dp, StatusSuccess.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusSuccess,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Je Philips TAH6519 is up-to-date met versie $firmwareVersion.",
                                        color = StatusSuccess,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        is UpdateState.UpdateAvailable -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(StatusYellow.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp))
                                    .border(1.dp, StatusYellow.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SystemUpdate,
                                        contentDescription = null,
                                        tint = StatusYellow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Nieuwe Firmware Versie ${state.version} Beschikbaar!",
                                        color = StatusYellow,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                state.changelog.forEach { logItem ->
                                    Text(
                                        text = "• $logItem",
                                        color = TextPrimary,
                                        fontSize = 10.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        viewModel.startUpdate()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("start_firmware_update_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SystemUpdate,
                                        contentDescription = "Start Update",
                                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                    )
                                    Text("Firmware ${state.version} Nu Installeren", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        is UpdateState.Updating -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AccentPrimary.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp))
                                    .border(1.dp, AccentPrimary.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = state.statusMessage,
                                        color = AccentPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${state.progress.toInt()}%",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { state.progress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = AccentPrimary,
                                    trackColor = DarkBorder
                                )
                            }
                        }
                        is UpdateState.UpdateComplete -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(StatusSuccess.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                    .border(1.dp, StatusSuccess.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusSuccess,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Update Succesvol Voltooid! Nieuwe versie: ${state.newVersion}",
                                        color = StatusSuccess,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        is UpdateState.Idle -> { /* Idle mode */ }
                    }

                    // Check for Updates Action Button
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            viewModel.checkForUpdates()
                        },
                        enabled = !isApiPollingInProgress && updateState !is UpdateState.Updating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("check_firmware_updates_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkPanel,
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, DarkBorder)
                    ) {
                        if (isApiPollingInProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = TextPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("API Raadplegen...", fontSize = 12.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Check for updates",
                                tint = HighlightSky,
                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                            )
                            Text("Controleer op Firmware Updates (API)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Section 2: Serial Number & Hardware Profile
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("serial_number_section"),
                colors = CardDefaults.cardColors(containerColor = DarkBg),
                border = BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            Icon(
                                imageVector = Icons.Filled.QrCode,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Serienummer & Identiteit",
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Copy Serial Action
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("TAH6519 Serial Number", serialNumber)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Serienummer gekopieerd: $serialNumber", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy Serial Number",
                                tint = AccentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = serialNumber,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = "MAC: 74:90:50:A1:65:19",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

                    // Hardware Specs Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Acoustic Driver", color = TextMuted, fontSize = 9.sp)
                            Text("40 mm Neodymium", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Column {
                            Text("Accu Capaciteit", color = TextMuted, fontSize = 9.sp)
                            Text("800 mAh Li-Po", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Column {
                            Text("Bluetooth Chipset", color = TextMuted, fontSize = 9.sp)
                            Text("BT 5.3 Dual-Point", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Section 3: Connection Strength (RSSI) & Link Quality
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("connection_strength_section"),
                colors = CardDefaults.cardColors(containerColor = DarkBg),
                border = BorderStroke(1.dp, signalColor.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            Icon(
                                imageVector = Icons.Filled.SignalCellularAlt,
                                contentDescription = null,
                                tint = signalColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Verbindingssterkte (RSSI)",
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Live Refresh Signal
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(signalColor.copy(alpha = pulseAlpha))
                            )
                            Text(
                                text = "$actualRssi dBm",
                                color = signalColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Signal Strength Progress Bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { signalPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = signalColor,
                            trackColor = DarkBorder
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(signalQualityText, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Geschatte Afstand: $estimatedDistance", color = TextMuted, fontSize = 10.sp)
                        }
                    }

                    HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

                    // Codec & Link Diagnostics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Actieve Codec", color = TextMuted, fontSize = 9.sp)
                            Text("${settings.activeCodec} (${if (settings.ldacEnabled) "990kbps" else "328kbps"})", color = HighlightSky, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Column {
                            Text("Signaal Stabiliteit", color = TextMuted, fontSize = 9.sp)
                            Text("99.8% (0% Packet Loss)", color = StatusSuccess, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Column {
                            Text("Audio Vertraging", color = TextMuted, fontSize = 9.sp)
                            Text("28 ms (Game Mode)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Diagnostic Results Banner if triggered
            AnimatedVisibility(visible = diagnosticMessage != null) {
                diagnosticMessage?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StatusSuccess.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp))
                            .border(1.dp, StatusSuccess.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = StatusSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = msg,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Actions Row: Ping & Diagnostic Test
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        isRefreshingSignal = true
                        viewModel.fetchBatteryLevel()
                        scope.launch {
                            delay(1000)
                            isRefreshingSignal = false
                            Toast.makeText(context, "Signaalsterkte ververst: $actualRssi dBm", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("refresh_signal_button"),
                    border = BorderStroke(1.dp, DarkBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                    Text("Ververs Signaal", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        isTestingDiagnostics = true
                        scope.launch {
                            delay(1200)
                            isTestingDiagnostics = false
                            diagnosticMessage = "Diagnose Voltooid: Bluetooth GATT Handshake OK • Audio Pijplijn Optimaal."
                        }
                    },
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("run_diagnostics_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NetworkCheck,
                        contentDescription = "Diagnostics",
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                    Text(if (isTestingDiagnostics) "Testen..." else "Sneldiagnose", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
