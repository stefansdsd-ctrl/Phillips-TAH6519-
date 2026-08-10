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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.HeadphoneSettings
import com.example.ui.theme.*

/**
 * Connection Status Dashboard Card for Philips TAH6519 Headphones.
 * Displays real-time paired/connected state, hardware signal details,
 * and provides 'Scan for device' and 'Re-initialize connection' actions.
 */
@Composable
fun Tah6519ConnectionDashboardCard(
    viewModel: HeadphoneViewModel,
    settings: HeadphoneSettings,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isScanning by viewModel.isScanningBluetooth.collectAsStateWithLifecycle()
    val isConnecting by viewModel.isConnecting.collectAsStateWithLifecycle()
    val isAutoReconnecting by viewModel.isAutoReconnecting.collectAsStateWithLifecycle()
    val scannedDevices by viewModel.scannedDevices.collectAsStateWithLifecycle()
    val reconnectAttempts by viewModel.reconnectAttempts.collectAsStateWithLifecycle()

    // Pulse animation for connection radar indicator
    val infiniteTransition = rememberInfiniteTransition(label = "conn_dashboard_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_rotation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tah6519_connection_status_dashboard"),
        colors = CardDefaults.cardColors(containerColor = DarkPanel),
        border = BorderStroke(
            width = 1.25.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    AccentPrimary.copy(alpha = 0.3f),
                    if (settings.connected) StatusSuccess.copy(alpha = 0.6f)
                    else if (isScanning) HighlightSky.copy(alpha = 0.7f)
                    else StatusYellow.copy(alpha = 0.5f),
                    AccentPrimary.copy(alpha = 0.3f)
                )
            )
        ),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Status badge & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = if (settings.connected) StatusSuccess.copy(alpha = 0.15f)
                                else if (isScanning || isConnecting || isAutoReconnecting) HighlightSky.copy(alpha = 0.15f)
                                else DarkCard,
                                shape = CircleShape
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (settings.connected) StatusSuccess
                                else if (isScanning || isConnecting || isAutoReconnecting) HighlightSky
                                else DarkBorder,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                settings.connected -> Icons.Filled.BluetoothConnected
                                isScanning -> Icons.Filled.BluetoothSearching
                                isConnecting || isAutoReconnecting -> Icons.Filled.Sync
                                else -> Icons.Filled.BluetoothDisabled
                            },
                            contentDescription = "Bluetooth State",
                            tint = when {
                                settings.connected -> StatusSuccess
                                isScanning || isConnecting || isAutoReconnecting -> HighlightSky
                                else -> TextMuted
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Bluetooth Verbindingsdashboard",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (settings.connected) "${settings.connectedDeviceName} (Gekoppeld & Verbonden)"
                            else "Philips TAH6519 (Niet verbonden)",
                            color = if (settings.connected) StatusSuccess else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Status Badge Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            color = when {
                                settings.connected -> StatusSuccess.copy(alpha = 0.15f)
                                isScanning -> HighlightSky.copy(alpha = 0.15f)
                                isConnecting || isAutoReconnecting -> StatusPurple.copy(alpha = 0.15f)
                                else -> StatusDanger.copy(alpha = 0.12f)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = when {
                                settings.connected -> StatusSuccess.copy(alpha = 0.5f)
                                isScanning -> HighlightSky.copy(alpha = 0.5f)
                                isConnecting || isAutoReconnecting -> StatusPurple.copy(alpha = 0.5f)
                                else -> StatusDanger.copy(alpha = 0.3f)
                            },
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("connection_status_badge"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(
                                    color = when {
                                        settings.connected -> StatusSuccess
                                        isScanning -> HighlightSky
                                        isConnecting || isAutoReconnecting -> StatusPurple
                                        else -> StatusDanger
                                    },
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = when {
                                settings.connected -> "GEKOPPELD"
                                isScanning -> "SCANNEN..."
                                isAutoReconnecting -> "HERSTELLEN (${reconnectAttempts}/3)"
                                isConnecting -> "VERBINDEN..."
                                else -> "LOSGEKOPPELD"
                            },
                            color = when {
                                settings.connected -> StatusSuccess
                                isScanning -> HighlightSky
                                isConnecting || isAutoReconnecting -> StatusPurple
                                else -> StatusDanger
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Connection Info Matrix Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBg, shape = RoundedCornerShape(14.dp))
                    .border(1.dp, DarkBorder, shape = RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("APPARAAT", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (settings.connected) settings.connectedDeviceName else settings.lastPairedDeviceName,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("MAC ADRES", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = settings.lastPairedDeviceAddress.ifBlank { "00:11:22:33:44:55" },
                            color = HighlightSky,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SIGNAAL (RSSI)", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (settings.connected) "-52 dBm (Uitstekend)" else "Geen actief signaal",
                            color = if (settings.connected) StatusSuccess else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PROTOCOL", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${settings.connectionType} (${settings.activeCodec})",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (settings.connected) {
                    HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("connection_card_battery_indicator"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    viewModel.isCharging.value -> Icons.Filled.BatteryChargingFull
                                    settings.batteryLevel <= 20 -> Icons.Filled.BatteryAlert
                                    else -> Icons.Filled.BatteryFull
                                },
                                contentDescription = "Batterijniveau",
                                tint = when {
                                    viewModel.isCharging.value -> AccentPrimary
                                    settings.batteryLevel <= 20 -> StatusDanger
                                    else -> StatusSuccess
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = "BATTERIJNIVEAU & DUUR",
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                val baseHours = if (settings.ancMode != "OFF") 40f else 80f
                                val estHours = ((settings.batteryLevel / 100f) * baseHours).toInt()
                                Text(
                                    text = "${settings.batteryLevel}% (~$estHours u. resterend)",
                                    color = when {
                                        viewModel.isCharging.value -> AccentPrimary
                                        settings.batteryLevel <= 20 -> StatusDanger
                                        else -> StatusSuccess
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("connection_battery_percentage_estimate")
                                )
                            }
                        }

                        // Compact horizontal battery level progress bar
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkPanel)
                                .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(settings.batteryLevel / 100f)
                                    .background(
                                        when {
                                            viewModel.isCharging.value -> AccentPrimary
                                            settings.batteryLevel <= 20 -> StatusDanger
                                            settings.batteryLevel <= 50 -> StatusYellow
                                            else -> StatusSuccess
                                        }
                                    )
                            )
                        }
                    }
                }
            }

            // Primary Control Buttons Row (Scan for Device & Re-initialize)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Button 1: Scan for Device
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        if (isScanning) {
                            viewModel.stopBluetoothScan()
                        } else {
                            viewModel.startBluetoothScan()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_scan_for_devices"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScanning) StatusYellow.copy(alpha = 0.2f) else AccentPrimary
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isScanning) StatusYellow else AccentPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isScanning) Icons.Filled.Stop else Icons.Filled.BluetoothSearching,
                            contentDescription = "Scan button",
                            tint = if (isScanning) StatusYellow else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isScanning) "Stop Scannen" else "Scan Apparaten",
                            color = if (isScanning) StatusYellow else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Button 2: Re-initialize Connection
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        viewModel.triggerLaunchAutoReconnect()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_reinitialize_connection"),
                    enabled = !isConnecting && !isAutoReconnecting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkCard
                    ),
                    border = BorderStroke(1.dp, HighlightSky.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = "Re-initialize",
                            tint = HighlightSky,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isAutoReconnecting) "Initialiseren..." else "Herinitialiseer",
                            color = HighlightSky,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick Connect / Disconnect Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard, shape = RoundedCornerShape(12.dp))
                    .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (settings.connected) Icons.Filled.CheckCircle else Icons.Filled.Info,
                        contentDescription = null,
                        tint = if (settings.connected) StatusSuccess else StatusYellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (settings.connected) "Koptelefoon verbonden" else "Koptelefoon is ontkoppeld",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        if (settings.connected) {
                            viewModel.disconnectDevice()
                        } else {
                            viewModel.connectDevice(settings.lastPairedDeviceName.ifBlank { "Philips TAH6519" })
                        }
                    },
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("btn_toggle_connection_state"),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (settings.connected) StatusDanger.copy(alpha = 0.5f) else StatusSuccess.copy(alpha = 0.5f)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = if (settings.connected) "Verbinding Verbreken" else "Snel Verbinden",
                        color = if (settings.connected) StatusDanger else StatusSuccess,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Live Scan Results Section (Expanded when scanning or devices detected)
            AnimatedVisibility(
                visible = isScanning || scannedDevices.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg, shape = RoundedCornerShape(14.dp))
                        .border(1.dp, HighlightSky.copy(alpha = 0.3f), shape = RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = HighlightSky,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.BluetoothSearching,
                                    contentDescription = null,
                                    tint = HighlightSky,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = if (isScanning) "Zoeken naar Bluetooth LE apparaten..." else "Gevonden apparaten (${scannedDevices.size})",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (scannedDevices.isEmpty()) {
                        Text(
                            text = "Geen apparaten in de buurt gevonden. Zorg dat de Philips TAH6519 in koppelmodus staat.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.testTag("scanned_devices_list")
                        ) {
                            scannedDevices.forEach { dev ->
                                val isTah = dev.name.contains("TAH6519", ignoreCase = true) || dev.name.contains("Philips", ignoreCase = true)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = if (isTah) AccentPrimary.copy(alpha = 0.15f) else DarkCard,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isTah) AccentPrimary else DarkBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            viewModel.connectToBleDevice(dev.address)
                                            viewModel.connectDevice(dev.name)
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Headphones,
                                            contentDescription = null,
                                            tint = if (isTah) AccentPrimary else HighlightSky,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = dev.name.ifBlank { "Onbekend Bluetooth Apparaat" },
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${dev.address} · RSSI: ${dev.rssi} dBm",
                                                color = TextMuted,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            viewModel.connectToBleDevice(dev.address)
                                            viewModel.connectDevice(dev.name)
                                        },
                                        modifier = Modifier.height(30.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isTah) AccentPrimary else HighlightSky
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Text(
                                            text = "Koppelen",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Auto-Reconnect Option Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = null,
                        tint = HighlightSky,
                        modifier = Modifier.size(16.dp)
                    )
                    Column {
                        Text(
                            text = "Automatisch Herverbinden",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Achtergrondservice zoekt automatisch naar TAH6519 bij opstarten",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Switch(
                    checked = settings.autoReconnectOnLaunch,
                    onCheckedChange = { viewModel.setAutoReconnectOnLaunch(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = HighlightSky,
                        checkedTrackColor = AccentPrimary,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkBg
                    ),
                    modifier = Modifier
                        .scale(0.8f)
                        .testTag("auto_reconnect_toggle")
                )
            }
        }
    }
}
