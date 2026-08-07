package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.HeadphoneSettings
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Visual DataStore Equalizer Persistence Badge & Share/Import Action Card
 */
@Composable
fun EqualizerDataStoreStatusCard(
    settings: HeadphoneSettings,
    onImportBands: (List<Float>, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showShareImportDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("eq_datastore_status_card"),
        colors = CardDefaults.cardColors(containerColor = DarkPanel),
        border = BorderStroke(1.dp, HighlightSky.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
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
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(HighlightSky.copy(alpha = 0.2f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = null,
                            tint = HighlightSky,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "DataStore EQ Persistentie",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Automatisch opgeslagen via Jetpack DataStore",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(StatusSuccess.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp))
                        .border(1.dp, StatusSuccess.copy(alpha = 0.4f), shape = RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(StatusSuccess, shape = CircleShape)
                        )
                        Text(
                            text = "PERSISTENT BEWAARD",
                            color = StatusSuccess,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(color = DarkBorder.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Actief Profiel: ${settings.activePreset ?: "Philips Signature"}",
                        color = HighlightSky,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val bandsCsv = settings.getBands().joinToString(", ") { String.format("%.1f", it) }
                    Text(
                        text = "DSP Banden: $bandsCsv",
                        color = TextMuted,
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = { showShareImportDialog = true },
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("btn_open_eq_share_dialog"),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, HighlightSky.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        tint = HighlightSky,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Delen / Import",
                        color = HighlightSky,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showShareImportDialog) {
        EqShareImportDialog(
            currentBands = settings.getBands(),
            onImportBands = onImportBands,
            onDismiss = { showShareImportDialog = false }
        )
    }
}

/**
 * Custom EQ Preset Export & Import String Share Engine
 */
@Composable
fun EqShareImportDialog(
    currentBands: List<Float>,
    onImportBands: (List<Float>, String) -> Unit,
    onDismiss: () -> Unit
) {
    var shareCodeInput by remember {
        val bandsCsv = currentBands.joinToString(",") { String.format("%.1f", it) }
        mutableStateOf("PHILIPS_EQ10:[$bandsCsv]")
    }
    var importError by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("eq_share_import_dialog"),
            colors = CardDefaults.cardColors(containerColor = DarkPanel),
            border = BorderStroke(1.dp, AccentPrimary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "EQ Preset Delen & Importeren",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Kopieer je huidige EQ-code om deze te delen, of plak een code van een vriend om de instellingen over te nemen.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                OutlinedTextField(
                    value = shareCodeInput,
                    onValueChange = {
                        shareCodeInput = it
                        importError = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("eq_code_text_field"),
                    label = { Text("EQ Profiel Code", color = TextMuted, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = DarkBorder
                    ),
                    maxLines = 3
                )

                importError?.let { err ->
                    Text(text = err, color = StatusDanger, fontSize = 11.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(shareCodeInput))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_copy_eq_code"),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DarkBorder)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = HighlightSky)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kopiëren", color = HighlightSky, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            try {
                                val clean = shareCodeInput.trim()
                                if (clean.startsWith("PHILIPS_EQ10:[") && clean.endsWith("]")) {
                                    val numbersStr = clean.substringAfter("PHILIPS_EQ10:[").substringBefore("]")
                                    val parsed = numbersStr.split(",").map { it.trim().replace(",", ".").toFloat() }
                                    if (parsed.size == 10) {
                                        onImportBands(parsed, "Geïmporteerd Profiel")
                                        onDismiss()
                                    } else {
                                        importError = "Code moet exact 10 frequentiebanden bevatten."
                                    }
                                } else {
                                    importError = "Ongeldig formaat. Code moet beginnen met PHILIPS_EQ10:"
                                }
                            } catch (e: Exception) {
                                importError = "Fout bij verwerken van code: ${e.message}"
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_apply_imported_eq"),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Toepassen", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Dynamic Spatial Reverb & Room Modeling Engine
 */
@Composable
fun SpatialReverbEngineCard(
    settings: HeadphoneSettings,
    onRoomSizeChange: (Float) -> Unit,
    onDecayChange: (Float) -> Unit,
    onPresetSelect: (String) -> Unit
) {
    var roomSize by remember { mutableFloatStateOf(0.6f) }
    var decayTime by remember { mutableFloatStateOf(1.2f) }
    var selectedEnv by remember { mutableStateOf("Concertzaal") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("spatial_reverb_card"),
        colors = CardDefaults.cardColors(containerColor = DarkPanel),
        border = BorderStroke(1.dp, StatusPurple.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = "3D Acoustic Model",
                        tint = StatusPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "3D Ruimtelijke Akoestiek DSP",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = selectedEnv,
                    color = StatusPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Environment preset buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Studio", "Concertzaal", "Bioscoop", "Kathedraal").forEach { env ->
                    val isSelected = selectedEnv == env
                    Button(
                        onClick = {
                            selectedEnv = env
                            when (env) {
                                "Studio" -> { roomSize = 0.2f; decayTime = 0.4f }
                                "Concertzaal" -> { roomSize = 0.6f; decayTime = 1.2f }
                                "Bioscoop" -> { roomSize = 0.8f; decayTime = 1.8f }
                                "Kathedraal" -> { roomSize = 1.0f; decayTime = 3.5f }
                            }
                            onPresetSelect(env)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) StatusPurple.copy(alpha = 0.2f) else DarkBg,
                            contentColor = if (isSelected) StatusPurple else TextMuted
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) StatusPurple else DarkBorder
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Text(env, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = DarkBorder.copy(alpha = 0.4f))

            // Room Size Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Virtuele Kamergrootte", color = TextMuted, fontSize = 11.sp)
                    Text("${(roomSize * 100).toInt()} m²", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = roomSize,
                    onValueChange = { roomSize = it; onRoomSizeChange(it) },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = StatusPurple,
                        thumbColor = StatusPurple,
                        inactiveTrackColor = DarkBorder
                    ),
                    modifier = Modifier.testTag("spatial_room_size_slider")
                )
            }

            // Decay Time Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Galmtijd (Decay RT60)", color = TextMuted, fontSize = 11.sp)
                    Text(String.format("%.1fs", decayTime), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = decayTime,
                    onValueChange = { decayTime = it; onDecayChange(it) },
                    valueRange = 0.2f..4.0f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = StatusPurple,
                        thumbColor = StatusPurple,
                        inactiveTrackColor = DarkBorder
                    ),
                    modifier = Modifier.testTag("spatial_decay_slider")
                )
            }
        }
    }
}

/**
 * Realtime Power Draw Profiler & Battery Lifespan Card
 */
@Composable
fun BatteryPowerProfilerCard(
    settings: HeadphoneSettings,
    isCharging: Boolean
) {
    val ancDraw = if (settings.ancMode == "ON") (settings.ancLevel * 4) else if (settings.ancMode == "TRANSPARENCY") 6 else 0
    val ldacDraw = if (settings.ldacEnabled) 8 else 2
    val spatialDraw = if (settings.surroundSoundEnabled) 5 else 0
    val totalDrawMa = 10 + ancDraw + ldacDraw + spatialDraw

    val batteryCapacityMah = 800f
    val remainingCapacity = batteryCapacityMah * (settings.batteryLevel / 100f)
    val estimatedHoursRemaining = if (totalDrawMa > 0) remainingCapacity / totalDrawMa else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("battery_profiler_card"),
        colors = CardDefaults.cardColors(containerColor = DarkPanel),
        border = BorderStroke(1.dp, DarkBorder),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ElectricBolt,
                        contentDescription = "Power Draw Profiler",
                        tint = StatusYellow,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Energieverbruik Analyse",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "$totalDrawMa mA/u",
                    color = StatusYellow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = DarkBorder.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Geschatte Resterende Tijd", color = TextMuted, fontSize = 11.sp)
                    Text(
                        text = if (isCharging) "Aan het opladen..." else String.format("%.1f Uur", estimatedHoursRemaining),
                        color = StatusSuccess,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Batterijconditie", color = TextMuted, fontSize = 11.sp)
                    Text(
                        text = if (settings.batteryHealthEnabled) "Optimaal (80% Begrensd)" else "Standaard (100%)",
                        color = HighlightSky,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(DarkBg)
            ) {
                val baseRatio = 10f / totalDrawMa
                val ancRatio = ancDraw.toFloat() / totalDrawMa
                val ldacRatio = ldacDraw.toFloat() / totalDrawMa
                val spatialRatio = spatialDraw.toFloat() / totalDrawMa

                Box(modifier = Modifier.fillMaxHeight().weight(baseRatio.coerceAtLeast(0.01f)).background(AccentPrimary))
                if (ancDraw > 0) Box(modifier = Modifier.fillMaxHeight().weight(ancRatio.coerceAtLeast(0.01f)).background(StatusDanger))
                if (ldacDraw > 0) Box(modifier = Modifier.fillMaxHeight().weight(ldacRatio.coerceAtLeast(0.01f)).background(StatusSuccess))
                if (spatialDraw > 0) Box(modifier = Modifier.fillMaxHeight().weight(spatialRatio.coerceAtLeast(0.01f)).background(StatusPurple))
            }
        }
    }
}

/**
 * Proximity Signal Radar for "Find My Headphones"
 */
@Composable
fun FindMyHeadphonesRadarCard(
    viewModel: HeadphoneViewModel
) {
    var isSearching by remember { mutableStateOf(false) }
    var rssiSignalPercent by remember { mutableIntStateOf(78) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("find_my_headphones_radar_card"),
        colors = CardDefaults.cardColors(containerColor = DarkPanel),
        border = BorderStroke(1.dp, HighlightSky.copy(alpha = 0.4f)),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Radar,
                        contentDescription = "Radar Locator",
                        tint = HighlightSky,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Zoek Koptelefoon Radar",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (isSearching) "Zoeken..." else "Standby",
                    color = if (isSearching) HighlightSky else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(DarkBg, shape = RoundedCornerShape(12.dp))
                    .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isSearching) {
                    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
                    val radarRadius by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "radar_radius"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize(radarRadius)
                            .border(1.5.dp, HighlightSky.copy(alpha = 1f - radarRadius), shape = CircleShape)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Headphones,
                        contentDescription = null,
                        tint = if (isSearching) HighlightSky else TextMuted,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isSearching) {
                            when {
                                rssiSignalPercent > 80 -> "Afstand: Zeer dichtbij (~0.5m)"
                                rssiSignalPercent > 50 -> "Afstand: In dezelfde kamer (~2m)"
                                else -> "Afstand: Zwak signaal (>5m)"
                            }
                        } else "Druk op de knop om te zoeken",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        isSearching = !isSearching
                        if (isSearching) {
                            scope.launch {
                                while (isSearching) {
                                    delay(1000)
                                    rssiSignalPercent = (60..95).random()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_toggle_radar"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSearching) StatusDanger.copy(alpha = 0.2f) else HighlightSky.copy(alpha = 0.2f),
                        contentColor = if (isSearching) StatusDanger else HighlightSky
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isSearching) StatusDanger else HighlightSky)
                ) {
                    Text(if (isSearching) "Stop Radar" else "Start Signal Radar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.playProceduralTone() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_ping_headphone_sound"),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Speel Geluidsbaken", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
