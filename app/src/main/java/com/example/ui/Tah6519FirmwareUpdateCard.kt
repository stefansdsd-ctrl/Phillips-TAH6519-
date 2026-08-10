package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkPanel
import com.example.ui.theme.HighlightSky
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun Tah6519FirmwareUpdateCard(
    viewModel: HeadphoneViewModel,
    settings: HeadphoneSettings,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val firmwareVersion by viewModel.firmwareVersion.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val isAutoCheckEnabled by viewModel.isFirmwarePolling.collectAsStateWithLifecycle()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tah6519_firmware_update_card"),
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
            // Header Row
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
                            imageVector = Icons.Filled.SystemUpdate,
                            contentDescription = "Firmware Upgrade",
                            tint = HighlightSky,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Firmware & Software Updates",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Philips TAH6519 OTA Updatebeheer",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                // Current Version Badge
                Box(
                    modifier = Modifier
                        .background(DarkBg, shape = RoundedCornerShape(20.dp))
                        .border(1.dp, HighlightSky.copy(alpha = 0.3f), shape = RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = firmwareVersion,
                        color = HighlightSky,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Device Hardware Specs Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkBg),
                border = BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Memory,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Philips TAH6519 Hardware",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "HW Rev: 2.0 • Build 2026.08-OTA",
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = StatusSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Officieel Geverifieerd",
                            color = StatusSuccess,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Update Dynamic State Flow Panel
            when (val state = updateState) {
                is UpdateState.Idle -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Houd je Philips TAH6519 voorzien van de nieuwste audio-algoritmes en Bluetooth 5.4-stabiliteit.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.pollFirmwareApi(manual = true)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("btn_check_firmware_updates"),
                            colors = ButtonDefaults.buttonColors(containerColor = HighlightSky),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Check for updates",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Controleer op Nieuwe Firmware",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                is UpdateState.Checking -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBg, shape = RoundedCornerShape(10.dp))
                            .border(1.dp, HighlightSky.copy(alpha = 0.3f), shape = RoundedCornerShape(10.dp))
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
                                CircularProgressIndicator(
                                    color = HighlightSky,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Controleren op updates...",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Philips OTA Server",
                                color = HighlightSky,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .testTag("firmware_checking_progress_bar"),
                            color = HighlightSky,
                            trackColor = DarkBorder
                        )

                        Text(
                            text = "Verbinding maken met Philips Firmware Server API...",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                is UpdateState.Updating -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBg, shape = RoundedCornerShape(10.dp))
                            .border(1.dp, AccentPrimary.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                color = HighlightSky,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .testTag("firmware_updating_progress_bar"),
                            color = AccentPrimary,
                            trackColor = DarkBorder
                        )

                        Text(
                            text = "Houd de koptelefoon ingeschakeld en dicht bij de telefoon.",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                is UpdateState.UpdateAvailable -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StatusSuccess.copy(alpha = 0.08f), shape = RoundedCornerShape(10.dp))
                            .border(1.dp, StatusSuccess.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp))
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
                                    imageVector = Icons.Filled.CloudDownload,
                                    contentDescription = null,
                                    tint = StatusSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Nieuwe Firmware Beschikbaar (${state.version})",
                                    color = StatusSuccess,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        state.changelog.forEach { change ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("•", color = StatusSuccess, fontSize = 10.sp)
                                Text(change, color = TextPrimary, fontSize = 10.sp, lineHeight = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.startUpdate()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("btn_install_firmware_update")
                        ) {
                            Text("Update Nu Installeren", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is UpdateState.UpToDate -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StatusSuccess.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp))
                                .border(1.dp, StatusSuccess.copy(alpha = 0.25f), shape = RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Up to date",
                                tint = StatusSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Je Philips TAH6519 is up-to-date ($firmwareVersion)",
                                color = StatusSuccess,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                viewModel.pollFirmwareApi(manual = true)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            border = BorderStroke(1.dp, DarkBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Opnieuw Controleren", color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }

                is UpdateState.UpdateComplete -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StatusSuccess.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp))
                            .border(1.dp, StatusSuccess, shape = RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Voltooid",
                            tint = StatusSuccess,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Firmware Succesvol Geüpdatet naar ${state.newVersion}!",
                            color = StatusSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { viewModel.resetUpdateState() },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBg),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("btn_complete_firmware_update")
                        ) {
                            Text("Klaar", color = TextPrimary, fontSize = 10.sp)
                        }
                    }
                }
            }

            HorizontalDivider(color = DarkBorder)

            // Auto-check background setting toggle
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
                        imageVector = Icons.Filled.AutoMode,
                        contentDescription = "Auto Updates",
                        tint = HighlightSky,
                        modifier = Modifier.size(16.dp)
                    )
                    Column {
                        Text(
                            text = "Automatisch op de achtergrond controleren",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Melding bij nieuwe Philips OTA firmware release",
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }

                Switch(
                    checked = isAutoCheckEnabled,
                    onCheckedChange = { enabled ->
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        viewModel.toggleFirmwarePolling(enabled)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = HighlightSky,
                        checkedTrackColor = HighlightSky.copy(alpha = 0.4f)
                    )
                )
            }
        }
    }
}
