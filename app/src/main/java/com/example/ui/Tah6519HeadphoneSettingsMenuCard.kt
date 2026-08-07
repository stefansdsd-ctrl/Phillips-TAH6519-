package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.HeadphoneSettings
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Comprehensive Headphone Settings Menu for Philips TAH6519 Over-Ear Headphones.
 * Features categorized tabs (Audio & ANC, Hardware Controls, Power & Battery, System & Firmware),
 * live toggles, dropdown selectors, dialogs for hardware calibration and factory reset.
 */
@Composable
fun Tah6519HeadphoneSettingsMenuCard(
    viewModel: HeadphoneViewModel,
    settings: HeadphoneSettings,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val firmwareVersion by viewModel.firmwareVersion.collectAsStateWithLifecycle()

    var activeCategory by remember { mutableIntStateOf(0) } // 0: Connectivity & Audio, 1: Controls & Sensors, 2: Power & Battery, 3: System & OTA
    var showFactoryResetDialog by remember { mutableStateOf(false) }
    var showFirmwareCheckDialog by remember { mutableStateOf(false) }
    var voicePromptLang by remember { mutableStateOf("Nederlands") }
    var buttonActionConfig by remember { mutableStateOf("Spraakassistent") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tah6519_headphone_settings_menu_card"),
        colors = CardDefaults.cardColors(containerColor = DarkPanel),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    DarkBorder,
                    AccentPrimary.copy(alpha = 0.5f),
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Header
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
                            .size(38.dp)
                            .background(AccentPrimary.copy(alpha = 0.15f), shape = CircleShape)
                            .border(1.dp, AccentPrimary, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Instellingen Menu",
                            tint = AccentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Koptelefoon Instellingen",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Philips TAH6519 · Hardware & Audio Configuration",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        showFirmwareCheckDialog = true
                    },
                    modifier = Modifier.testTag("btn_quick_firmware_check")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = "Firmware Check",
                        tint = HighlightSky,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Category Segmented Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard, shape = RoundedCornerShape(12.dp))
                    .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val categories = listOf(
                    Triple(0, "Audio & ANC", Icons.Filled.GraphicEq),
                    Triple(1, "Bediening", Icons.Filled.TouchApp),
                    Triple(2, "Energie", Icons.Filled.BatteryChargingFull),
                    Triple(3, "Systeem", Icons.Filled.Settings)
                )

                categories.forEach { (index, label, icon) ->
                    val isSelected = activeCategory == index
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) AccentPrimary else Color.Transparent,
                        animationSpec = tween(200),
                        label = "cat_bg"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .background(bgColor, shape = RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                activeCategory = index
                            }
                            .testTag("settings_tab_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) Color.White else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Category Content Sections
            Crossfade(
                targetState = activeCategory,
                animationSpec = tween(200),
                label = "settings_tab_crossfade"
            ) { cat ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (cat) {
                        0 -> AudioAndAncSettingsSection(viewModel, settings)
                        1 -> ControlsAndSensorsSettingsSection(
                            settings = settings,
                            voicePromptLang = voicePromptLang,
                            onVoicePromptLangChange = { voicePromptLang = it },
                            buttonActionConfig = buttonActionConfig,
                            onButtonActionChange = { buttonActionConfig = it },
                            viewModel = viewModel
                        )
                        2 -> PowerAndBatterySettingsSection(viewModel, settings)
                        3 -> SystemAndFirmwareSettingsSection(
                            viewModel = viewModel,
                            settings = settings,
                            onCheckFirmwareClick = { showFirmwareCheckDialog = true },
                            onFactoryResetClick = { showFactoryResetDialog = true }
                        )
                    }
                }
            }
        }
    }

    // Dialog: Firmware Update Check
    if (showFirmwareCheckDialog) {
        val isBusy = updateState is UpdateState.Checking || updateState is UpdateState.Updating

        AlertDialog(
            onDismissRequest = { if (!isBusy) showFirmwareCheckDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = null,
                        tint = HighlightSky
                    )
                    Text(
                        text = "Firmware Software Check",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (val state = updateState) {
                        is UpdateState.Checking -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(26.dp),
                                    color = HighlightSky,
                                    strokeWidth = 2.5.dp
                                )
                                Column {
                                    Text(
                                        text = "Verbinden met Philips OTA Server...",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Software SHA-256 verifiëren voor TAH6519",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        is UpdateState.UpToDate -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(StatusSuccess.copy(alpha = 0.2f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusSuccess,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Software is Up-to-Date",
                                        color = StatusSuccess,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Versie $firmwareVersion",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Text(
                                text = "Je Philips TAH6519 over-ear hoofdtelefoon gebruikt de nieuwste firmware software. Alle hybride ANC-algoritmen en audio-codecs presteren optimaal.",
                                color = TextMuted,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }

                        is UpdateState.UpdateAvailable -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(StatusYellow.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "NIEUWE FIRMWARE: ${state.version}",
                                            color = StatusYellow,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = "Wijzigingen & Verbeteringen:",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    state.changelog.forEach { log ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("•", color = HighlightSky, fontSize = 11.sp)
                                            Text(log, color = TextMuted, fontSize = 10.sp, lineHeight = 14.sp)
                                        }
                                    }
                                }
                            }
                        }

                        is UpdateState.Updating -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = state.statusMessage,
                                        color = HighlightSky,
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
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .testTag("firmware_update_progress_bar"),
                                    color = HighlightSky,
                                    trackColor = DarkBg,
                                )

                                Text(
                                    text = "Houd de Philips TAH6519 ingeschakeld en dicht bij je telefoon.",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        is UpdateState.UpdateComplete -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusSuccess,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Update Voltooid!",
                                    color = StatusSuccess,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "De Philips TAH6519 is geüpdatet naar software ${state.newVersion}.",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }

                        else -> {
                            Text(
                                text = "Druk op 'Controleer Nu' om een gesimuleerde netwerkcontrole uit te voeren op de Philips OTA-servers.",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                when (val state = updateState) {
                    is UpdateState.UpdateAvailable -> {
                        Button(
                            onClick = { viewModel.startUpdate() },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                            modifier = Modifier.testTag("btn_install_fw_update")
                        ) {
                            Text("Installeren (OTA)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    is UpdateState.UpdateComplete -> {
                        Button(
                            onClick = {
                                viewModel.resetUpdateState()
                                showFirmwareCheckDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                            modifier = Modifier.testTag("btn_finish_fw_update")
                        ) {
                            Text("Voltooien", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    else -> {
                        Button(
                            onClick = { viewModel.checkForUpdates() },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = HighlightSky),
                            modifier = Modifier.testTag("btn_confirm_fw_check")
                        ) {
                            Text(
                                text = if (isBusy) "Controleren..." else "Controleer Nu",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.resetUpdateState()
                        showFirmwareCheckDialog = false
                    },
                    enabled = !isBusy,
                    modifier = Modifier.testTag("btn_close_fw_dialog")
                ) {
                    Text("Sluiten", color = TextMuted, fontSize = 11.sp)
                }
            },
            containerColor = DarkPanel,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Dialog: Factory Reset
    if (showFactoryResetDialog) {
        AlertDialog(
            onDismissRequest = { showFactoryResetDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = StatusDanger)
                    Text("Fabrieksinstellingen Herstellen", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Weet je zeker dat je de Philips TAH6519 wilt herstellen naar de standaard fabrieksinstellingen? Alle gepersonaliseerde EQ-presets, gekoppelde apparaten en gehoorprofielen worden gewist.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAll()
                        showFactoryResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusDanger),
                    modifier = Modifier.testTag("btn_confirm_factory_reset")
                ) {
                    Text("Ja, Herstellen", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showFactoryResetDialog = false },
                    modifier = Modifier.testTag("btn_cancel_factory_reset")
                ) {
                    Text("Annuleren", color = TextMuted, fontSize = 12.sp)
                }
            },
            containerColor = DarkPanel,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun AudioAndAncSettingsSection(
    viewModel: HeadphoneViewModel,
    settings: HeadphoneSettings
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingToggleRow(
            title = "LDAC Hi-Res Audio Codec",
            subtitle = "Zendt audiodata uit tot 990 kbps voor maximale akoestische helderheid",
            icon = Icons.Filled.GraphicEq,
            checked = settings.ldacEnabled,
            onCheckedChange = { viewModel.toggleLdac(it) },
            activeColor = StatusSuccess,
            testTag = "setting_toggle_ldac"
        )

        SettingToggleRow(
            title = "Dynamic Bass Boost",
            subtitle = "Compenseert lage frequenties op lage volumes via Philips DSP",
            icon = Icons.Filled.FlashOn,
            checked = settings.dynamicBassEnabled,
            onCheckedChange = { viewModel.toggleDynamicBass(it) },
            activeColor = StatusDanger,
            testTag = "setting_toggle_dynamic_bass"
        )

        SettingToggleRow(
            title = "3D Surround Sound Spatial Audio",
            subtitle = "Ruimtelijke audio rendering voor films, live-muziek en gaming",
            icon = Icons.Filled.GraphicEq,
            checked = settings.surroundSoundEnabled,
            onCheckedChange = { viewModel.toggleSurround(it) },
            activeColor = StatusPurple,
            testTag = "setting_toggle_surround"
        )

        SettingToggleRow(
            title = "Sidetone Microfoon Doorvoer",
            subtitle = "Hoor je eigen stem natuurlijk via de microfoons tijdens telefoongesprekken",
            icon = Icons.Filled.RecordVoiceOver,
            checked = settings.sidetoneEnabled,
            onCheckedChange = { viewModel.toggleSidetone(it) },
            activeColor = StatusYellow,
            testTag = "setting_toggle_sidetone"
        )
    }
}

@Composable
private fun ControlsAndSensorsSettingsSection(
    settings: HeadphoneSettings,
    voicePromptLang: String,
    onVoicePromptLangChange: (String) -> Unit,
    buttonActionConfig: String,
    onButtonActionChange: (String) -> Unit,
    viewModel: HeadphoneViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Multi-function Button Remapping Option
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBg, shape = RoundedCornerShape(12.dp))
                .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.RadioButtonChecked, contentDescription = null, tint = HighlightSky, modifier = Modifier.size(18.dp))
                Column {
                    Text("Custom Knop Toewijzing (Linker Oorschelp)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Kies de actie voor 1x drukken op de fysieke instelknop", color = TextMuted, fontSize = 10.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Spraakassistent", "ANC Modus", "EQ Preset").forEach { act ->
                    val isSelected = buttonActionConfig == act
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .background(if (isSelected) AccentPrimary.copy(alpha = 0.2f) else DarkCard, shape = RoundedCornerShape(8.dp))
                            .border(1.dp, if (isSelected) AccentPrimary else DarkBorder, shape = RoundedCornerShape(8.dp))
                            .clickable { onButtonActionChange(act) }
                            .testTag("btn_custom_action_$act"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(act, color = if (isSelected) HighlightSky else TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Voice Prompt Language Option
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBg, shape = RoundedCornerShape(12.dp))
                .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Language, contentDescription = null, tint = StatusPurple, modifier = Modifier.size(18.dp))
                Column {
                    Text("Gesproken Feedback Taal", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Stemmeldingen voor batterij en ANC modus", color = TextMuted, fontSize = 10.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Nederlands", "English", "Deutsch", "Stil (Pieptoon)").forEach { lang ->
                    val isSelected = voicePromptLang == lang
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .background(if (isSelected) StatusPurple.copy(alpha = 0.2f) else DarkCard, shape = RoundedCornerShape(8.dp))
                            .border(1.dp, if (isSelected) StatusPurple else DarkBorder, shape = RoundedCornerShape(8.dp))
                            .clickable { onVoicePromptLangChange(lang) }
                            .testTag("btn_lang_$lang"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(lang, color = if (isSelected) StatusPurple else TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        SettingToggleRow(
            title = "Windruisonderdrukking Microfoons",
            subtitle = "Filtert automatisch windruis op de externe ruisonderdrukkingsmicrofoons",
            icon = Icons.Filled.Air,
            checked = settings.windNoiseReductionEnabled,
            onCheckedChange = { viewModel.toggleWindNoiseReduction(it) },
            activeColor = HighlightSky,
            testTag = "setting_toggle_wind_reduction"
        )
    }
}

@Composable
private fun PowerAndBatterySettingsSection(
    viewModel: HeadphoneViewModel,
    settings: HeadphoneSettings
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Auto Power Off Timer Selector
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBg, shape = RoundedCornerShape(12.dp))
                .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Timer, contentDescription = null, tint = StatusYellow, modifier = Modifier.size(18.dp))
                Column {
                    Text("Automatisch Uitschakelen (Standby Timer)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Schakelt de koptelefoon uit bij geen audio-invoer", color = TextMuted, fontSize = 10.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(5, 15, 30, 60, 0).forEach { mins ->
                    val isSelected = settings.autoPowerOffMinutes == mins
                    val label = if (mins == 0) "Nooit" else "$mins min"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .background(if (isSelected) StatusYellow.copy(alpha = 0.2f) else DarkCard, shape = RoundedCornerShape(8.dp))
                            .border(1.dp, if (isSelected) StatusYellow else DarkBorder, shape = RoundedCornerShape(8.dp))
                            .clickable { viewModel.setAutoPowerOffMinutes(mins) }
                            .testTag("btn_power_off_timer_$mins"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (isSelected) StatusYellow else TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        SettingToggleRow(
            title = "Accu Gezondheidsmodus (Begrensd tot 80%)",
            subtitle = "Voorkomt overladen om de levensduur van de Li-ion accu te verdubbelen",
            icon = Icons.Filled.BatterySaver,
            checked = settings.batteryHealthEnabled,
            onCheckedChange = { viewModel.toggleBatteryHealth(it) },
            activeColor = StatusSuccess,
            testTag = "setting_toggle_battery_health"
        )
    }
}

@Composable
private fun SystemAndFirmwareSettingsSection(
    viewModel: HeadphoneViewModel,
    settings: HeadphoneSettings,
    onCheckFirmwareClick: () -> Unit,
    onFactoryResetClick: () -> Unit
) {
    val firmwareVersion by viewModel.firmwareVersion.collectAsStateWithLifecycle()
    val isApiPollingInProgress by viewModel.isApiPollingInProgress.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // System & Hardware Device Info Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBg, shape = RoundedCornerShape(12.dp))
                .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Hardware & Systeem Informatie", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Model Naam", color = TextMuted, fontSize = 11.sp)
                Text("Philips TAH6519 / TAH6509", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Driver Transducers", color = TextMuted, fontSize = 11.sp)
                Text("40mm Neodymium Acoustic", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Firmware Versie", color = TextMuted, fontSize = 11.sp)
                Text("$firmwareVersion (Build 9042)", color = HighlightSky, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // OTA Check Button
        Button(
            onClick = {
                viewModel.checkForUpdates()
                onCheckFirmwareClick()
            },
            enabled = !isApiPollingInProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("btn_check_firmware_updates"),
            colors = ButtonDefaults.buttonColors(containerColor = HighlightSky.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, HighlightSky),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (isApiPollingInProgress) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = HighlightSky, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verbinden met Philips OTA...", color = HighlightSky, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = HighlightSky, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Zoek Firmware Updates (OTA)", color = HighlightSky, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Factory Reset Button
        OutlinedButton(
            onClick = onFactoryResetClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("btn_trigger_factory_reset"),
            border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Filled.Restore, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Fabrieksinstellingen Herstellen", color = StatusDanger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color,
    testTag: String
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBg, shape = RoundedCornerShape(12.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) activeColor else TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = activeColor,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkCard
            ),
            modifier = Modifier
                .scale(0.8f)
                .testTag(testTag)
        )
    }
}
