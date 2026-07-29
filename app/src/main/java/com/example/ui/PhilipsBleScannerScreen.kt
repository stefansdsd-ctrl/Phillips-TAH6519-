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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NoiseAware
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bluetooth.BleConnectionState
import com.example.bluetooth.BleDevice
import com.example.bluetooth.BluetoothLEManager
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkPanel
import com.example.ui.theme.HighlightSky
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusOrange
import com.example.ui.theme.StatusPurple
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun PhilipsBleScannerScreen(
    viewModel: HeadphoneViewModel,
    modifier: Modifier = Modifier
) {
    val bleManager = viewModel.bleManager

    val isScanning by bleManager.isScanning.collectAsStateWithLifecycle()
    val scannedDevices by bleManager.scannedDevices.collectAsStateWithLifecycle()
    val connectionState by bleManager.connectionState.collectAsStateWithLifecycle()
    val isTah6519Connected by bleManager.isTah6519Connected.collectAsStateWithLifecycle()
    val batteryLevel by bleManager.batteryLevel.collectAsStateWithLifecycle()
    val firmwareVersion by bleManager.firmwareVersion.collectAsStateWithLifecycle()
    val deviceName by bleManager.deviceName.collectAsStateWithLifecycle()
    val statusMessage by bleManager.statusMessage.collectAsStateWithLifecycle()
    val rssi by bleManager.rssi.collectAsStateWithLifecycle()

    var filterTah6519Only by remember { mutableStateOf(false) }

    val displayedDevices by remember(scannedDevices, filterTah6519Only) {
        derivedStateOf {
            if (filterTah6519Only) scannedDevices.filter { it.isTah6519 } else scannedDevices
        }
    }

    // Scanning pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "ble_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ble_scanner_header_card"),
            colors = CardDefaults.cardColors(containerColor = DarkPanel),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(16.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(AccentPrimary, HighlightSky)
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isTah6519Connected) Icons.Filled.BluetoothConnected else Icons.Filled.Bluetooth,
                                contentDescription = "BLE Status",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Philips TAH6519 BLE Scanner",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Bluetooth Low Energy Services & GATT",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Connection Badge
                    val connectionBadgeColor = when (connectionState) {
                        BleConnectionState.CONNECTED -> StatusSuccess
                        BleConnectionState.CONNECTING -> StatusYellow
                        BleConnectionState.DISCONNECTED -> StatusDanger
                    }
                    Box(
                        modifier = Modifier
                            .background(connectionBadgeColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .border(1.dp, connectionBadgeColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when (connectionState) {
                                BleConnectionState.CONNECTED -> "VERBONDEN"
                                BleConnectionState.CONNECTING -> "VERBINDEN..."
                                BleConnectionState.DISCONNECTED -> "OFFLINE"
                            },
                            color = connectionBadgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Live Telemetry Bar (Battery, Firmware, RSSI)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Battery
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Filled.BatteryChargingFull,
                            contentDescription = "Accu",
                            tint = if (batteryLevel != null) StatusSuccess else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = batteryLevel?.let { "$it%" } ?: "Accu --",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Firmware
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Filled.SystemUpdate,
                            contentDescription = "Firmware",
                            tint = HighlightSky,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = firmwareVersion ?: "Firmware --",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Signal Strength
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Filled.SignalCellularAlt,
                            contentDescription = "Signaal",
                            tint = AccentPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = rssi?.let { "$it dBm" } ?: "Signaal --",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Status Message Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = statusMessage,
                        color = HighlightSky,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        // Active Connected Philips TAH6519 Controls Panel
        AnimatedVisibility(
            visible = isTah6519Connected,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("connected_gatt_panel"),
                colors = CardDefaults.cardColors(containerColor = DarkPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = StatusSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Philips TAH6519 GATT Besturing",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { bleManager.disconnect() },
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("ble_disconnect_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDanger),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusDanger.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Text("Verbreken", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // GATT Commands Action Buttons
                    Text("Directe GATT Karakteristieken opvragen:", color = TextMuted, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { bleManager.readBatteryLevel() },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("read_gatt_battery_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCard)
                        ) {
                            Text("Lees Accu (0x180F)", fontSize = 10.sp, color = TextPrimary)
                        }

                        Button(
                            onClick = { bleManager.readFirmwareVersion() },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("read_gatt_fw_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCard)
                        ) {
                            Text("Lees FW (0x180A)", fontSize = 10.sp, color = TextPrimary)
                        }
                    }

                    // GATT ANC Controls
                    Text("ANC Stand schakelen via BLE:", color = TextMuted, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { bleManager.writeAncMode("ON") },
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .testTag("ble_anc_on_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                        ) {
                            Icon(Icons.Filled.NoiseAware, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ANC AAN", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { bleManager.writeAncMode("TRANSPARENCY") },
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .testTag("ble_anc_transparency_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = HighlightSky)
                        ) {
                            Text("AMBIDENT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { bleManager.writeAncMode("OFF") },
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .testTag("ble_anc_off_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCard)
                        ) {
                            Text("UIT", fontSize = 10.sp, color = TextPrimary)
                        }
                    }
                }
            }
        }

        // Filter and Scan Control Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = filterTah6519Only,
                onClick = { filterTah6519Only = !filterTah6519Only },
                label = { Text(if (filterTah6519Only) "Alleen TAH6519" else "Alle BLE Apparaten", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Headphones,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.testTag("ble_filter_tah6519_chip"),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = AccentPrimary
                )
            )

            Button(
                onClick = {
                    if (isScanning) {
                        bleManager.stopScan()
                    } else {
                        bleManager.startScan(filterTah6519Only = filterTah6519Only)
                    }
                },
                modifier = Modifier
                    .testTag("ble_scan_action_button")
                    .alpha(if (isScanning) pulseAlpha else 1.0f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) StatusDanger else AccentPrimary
                )
            ) {
                Icon(
                    imageVector = if (isScanning) Icons.Filled.Refresh else Icons.Filled.BluetoothSearching,
                    contentDescription = "Scan Toggle",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isScanning) "Stoppen" else "BLE Scannen",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // Progress bar during scanning
        if (isScanning) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = HighlightSky,
                trackColor = DarkCard
            )
        }

        // Scanned Devices List
        Text(
            text = "Gedetecteerde BLE Apparaten (${displayedDevices.size}):",
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        if (displayedDevices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(DarkPanel, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.BluetoothDisabled,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = if (isScanning) "Scannen naar apparaten in de buurt..." else "Geen BLE apparaten gevonden",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                    if (!isScanning) {
                        Text(
                            text = "Druk op 'BLE Scannen' om te zoeken naar de Philips TAH6519.",
                            color = TextMuted.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayedDevices) { device ->
                    BleDeviceListItem(
                        device = device,
                        isConnecting = connectionState == BleConnectionState.CONNECTING,
                        onConnectClick = {
                            bleManager.connectToDevice(device.address)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BleDeviceListItem(
    device: BleDevice,
    isConnecting: Boolean,
    onConnectClick: () -> Unit
) {
    val isTah = device.isTah6519

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ble_device_item_${device.address.replace(":", "_")}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isTah) DarkPanel else DarkCard
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isTah) 1.5.dp else 1.dp,
            color = if (isTah) AccentPrimary.copy(alpha = 0.8f) else DarkBorder
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isTah) AccentPrimary.copy(alpha = 0.2f) else DarkCard,
                            CircleShape
                        )
                        .border(
                            1.dp,
                            if (isTah) AccentPrimary else DarkBorder,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isTah) Icons.Filled.Headphones else Icons.Filled.Bluetooth,
                        contentDescription = null,
                        tint = if (isTah) AccentPrimary else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = device.name,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isTah) {
                            Box(
                                modifier = Modifier
                                    .background(StatusSuccess.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "TAH6519 MATCH",
                                    color = StatusSuccess,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Text(
                        text = "MAC: ${device.address}  •  Signaal: ${device.rssi} dBm",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    if (isTah) {
                        Text(
                            text = "Specs: 40mm Neodymium · Hybrid ANC · Multipoint Dual Sync",
                            color = HighlightSky,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Button(
                onClick = onConnectClick,
                modifier = Modifier
                    .height(36.dp)
                    .testTag("connect_ble_btn_${device.address.replace(":", "_")}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTah) AccentPrimary else DarkCard
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "Verbinden",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isTah) Color.White else TextPrimary
                )
            }
        }
    }
}
