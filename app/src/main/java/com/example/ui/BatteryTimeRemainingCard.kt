package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SettingsPower
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HeadphoneSettings
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkPanel
import com.example.ui.theme.HighlightSky
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusOrange
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun BatteryTimeRemainingCard(
    viewModel: HeadphoneViewModel,
    settings: HeadphoneSettings,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showSimulator by remember { mutableStateOf(false) }

    // Simulated local overrides for real-time mode testing in simulator
    var simAncEnabled by remember(settings.ancMode) { mutableStateOf(settings.ancMode != "OFF") }
    var simLdacEnabled by remember(settings.ldacEnabled) { mutableStateOf(settings.ldacEnabled) }
    var simHighVolume by remember { mutableStateOf(false) }

    val batteryLevel = settings.batteryLevel

    // Calculate theoretical max hours based on specs & active codec/ANC settings
    val effectiveAnc = if (showSimulator) simAncEnabled else (settings.ancMode != "OFF")
    val effectiveLdac = if (showSimulator) simLdacEnabled else settings.ldacEnabled
    val effectiveVolFactor = if (showSimulator && simHighVolume) 0.85f else 1.0f

    val baseMaxHours = if (effectiveAnc) 40f else 80f
    val codecFactor = if (effectiveLdac) 0.75f else 1.0f
    val maxPlaytimeHours = baseMaxHours * codecFactor * effectiveVolFactor

    // Remaining hours calculation
    val remainingHoursFloat = (batteryLevel / 100f) * maxPlaytimeHours
    val fullHours = remainingHoursFloat.toInt()
    val remainingMinutes = ((remainingHoursFloat - fullHours) * 60).toInt()

    // Historical average drain calculation (e.g. based on user's 3.2 hours/day listening history)
    val avgDailyUsageHours = 3.2f
    val estimatedDaysRemaining = if (avgDailyUsageHours > 0) (remainingHoursFloat / avgDailyUsageHours) else 0f

    // Charging time calculation
    val timeToFullChargeMins = if (batteryLevel >= 100) 0 else ((100 - batteryLevel) * 0.9f).toInt()

    val batteryColor = when {
        isCharging -> HighlightSky
        batteryLevel <= 20 -> StatusDanger
        batteryLevel <= 50 -> StatusYellow
        else -> StatusSuccess
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("battery_time_remaining_card"),
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
                            .background(batteryColor.copy(alpha = 0.15f))
                            .border(1.dp, batteryColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.Schedule,
                            contentDescription = "Battery Schedule",
                            tint = batteryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Accuduur & Resterende Tijd",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Geprojecteerd op basis van historie & actieve modus",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                // Battery Level Badge
                Box(
                    modifier = Modifier
                        .background(batteryColor.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp))
                        .border(1.dp, batteryColor.copy(alpha = 0.4f), shape = RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isCharging) "⚡ $batteryLevel% Laden" else "$batteryLevel%",
                        color = batteryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Main Time Remaining Feature Display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkBg),
                border = BorderStroke(1.dp, batteryColor.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isCharging) "TIJD TOT VOLLEDIG GELADEN" else "RESTERENDE LUISTERTIJD",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = if (isCharging) {
                                if (batteryLevel >= 100) "Accu Vol (100%)" else "~$timeToFullChargeMins min"
                            } else {
                                if (fullHours == 0 && remainingMinutes == 0) "0 min" else "${fullHours}u ${remainingMinutes}m"
                            },
                            color = batteryColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text(
                            text = if (isCharging) {
                                "Snelladen via USB-C • 15 min laden = +5u speeltijd"
                            } else {
                                "Gebaseerd op weekgemiddelde: ~${String.format("%.1f", estimatedDaysRemaining)} dagen luisteren"
                            },
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }

                    // Circular Progress Dial
                    Box(
                        modifier = Modifier.size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(54.dp)) {
                            drawCircle(
                                color = DarkBorder,
                                style = Stroke(width = 5.dp.toPx())
                            )
                            drawArc(
                                color = batteryColor,
                                startAngle = -90f,
                                sweepAngle = (batteryLevel / 100f) * 360f,
                                useCenter = false,
                                style = Stroke(width = 5.dp.toPx())
                            )
                        }
                        Icon(
                            imageVector = when {
                                isCharging -> Icons.Filled.Bolt
                                batteryLevel <= 20 -> Icons.Filled.BatteryAlert
                                else -> Icons.Filled.BatteryFull
                            },
                            contentDescription = "Battery State",
                            tint = batteryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Historical Usage Trends & Modes Breakdown Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Usage Trend Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DarkBg, shape = RoundedCornerShape(10.dp))
                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.History, contentDescription = null, tint = HighlightSky, modifier = Modifier.size(12.dp))
                            Text("Gem. Verbruik", color = TextMuted, fontSize = 9.sp)
                        }
                        Text(
                            text = if (effectiveAnc) "2.1% / uur" else "1.1% / uur",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Afgelopen 7 dagen", color = TextMuted, fontSize = 8.sp)
                    }
                }

                // Optimal Charge Cycle Recommendation
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DarkBg, shape = RoundedCornerShape(10.dp))
                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Speed, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(12.dp))
                            Text("Laadadvies", color = TextMuted, fontSize = 9.sp)
                        }
                        Text(
                            text = if (batteryLevel <= 20) "Nu Opladen!" else "Over ~${String.format("%.1f", estimatedDaysRemaining)} dagen",
                            color = if (batteryLevel <= 20) StatusDanger else StatusSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Drempel 15% bereikt", color = TextMuted, fontSize = 8.sp)
                    }
                }

                // Battery Health Mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DarkBg, shape = RoundedCornerShape(10.dp))
                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.SettingsPower, contentDescription = null, tint = StatusYellow, modifier = Modifier.size(12.dp))
                            Text("Accuconditie", color = TextMuted, fontSize = 9.sp)
                        }
                        Text(
                            text = if (settings.batteryHealthEnabled) "80% Limiet Actief" else "100% Volledig",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Levensduur bescherming", color = TextMuted, fontSize = 8.sp)
                    }
                }
            }

            // Historical Battery Discharge Sparkline Canvas
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HISTORISCH ONTLAADVERLOOP (LAATSTE 7 DAGEN)",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Gem. 3.2u/dag",
                        color = HighlightSky,
                        fontSize = 9.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(DarkBg, shape = RoundedCornerShape(8.dp))
                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    HistoricalDischargeGraph()
                }
            }

            // Interactive Scenario Impact Simulator Toggle Button
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
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Simulate",
                        tint = HighlightSky,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Simuleer instellingen & accuduur impact",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        showSimulator = !showSimulator
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Text(
                        text = if (showSimulator) "▲ Sluit" else "▼ Open",
                        color = HighlightSky,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Expandable Interactive Simulator Controls
            AnimatedVisibility(
                visible = showSimulator,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(1.dp, HighlightSky.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Test direct hoe audio-instellingen de resterende uren beïnvloeden:",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Simulator Row 1: ANC Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Hybrid ANC (Ruisonderdrukking)", color = TextPrimary, fontSize = 11.sp)
                                Text("40 uur max vs 80 uur zonder ANC", color = TextMuted, fontSize = 9.sp)
                            }
                            Switch(
                                checked = simAncEnabled,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    simAncEnabled = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = HighlightSky,
                                    checkedTrackColor = HighlightSky.copy(alpha = 0.4f)
                                )
                            )
                        }

                        // Simulator Row 2: LDAC Audio
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Hi-Res LDAC Codec (990kbps)", color = TextPrimary, fontSize = 11.sp)
                                Text("Verhoogt processorgebruik met ~25%", color = TextMuted, fontSize = 9.sp)
                            }
                            Switch(
                                checked = simLdacEnabled,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    simLdacEnabled = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = HighlightSky,
                                    checkedTrackColor = HighlightSky.copy(alpha = 0.4f)
                                )
                            )
                        }

                        // Simulator Row 3: High Volume Level (>80%)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Hoog Volume (>80% dB)", color = TextPrimary, fontSize = 11.sp)
                                Text("Extra versterkerbelasting (~15% snellere ontlading)", color = TextMuted, fontSize = 9.sp)
                            }
                            Switch(
                                checked = simHighVolume,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    simHighVolume = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = StatusOrange,
                                    checkedTrackColor = StatusOrange.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoricalDischargeGraph() {
    Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
        val width = size.width
        val height = size.height
        
        // 7 days simulated historical battery level trace (100% -> 80% -> 55% -> 95% -> 70% -> 40% -> current)
        val dataPoints = listOf(1.0f, 0.82f, 0.58f, 0.95f, 0.72f, 0.45f, 0.88f)
        val stepX = width / (dataPoints.size - 1)

        val points = dataPoints.mapIndexed { index, value ->
            val x = index * stepX
            val y = height - (value * height)
            Offset(x, y)
        }

        val path = Path()
        val fillPath = Path()

        if (points.isNotEmpty()) {
            path.moveTo(points[0].x, points[0].y)
            fillPath.moveTo(points[0].x, height)
            fillPath.lineTo(points[0].x, points[0].y)

            for (i in 0 until points.size - 1) {
                val p1 = points[i]
                val p2 = points[i + 1]
                val controlX1 = p1.x + (p2.x - p1.x) / 2f
                val controlY1 = p1.y
                val controlX2 = p1.x + (p2.x - p1.x) / 2f
                val controlY2 = p2.y

                path.cubicTo(controlX1, controlY1, controlX2, controlY2, p2.x, p2.y)
                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p2.x, p2.y)
            }

            fillPath.lineTo(points.last().x, height)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(HighlightSky.copy(alpha = 0.25f), Color.Transparent)
                )
            )

            drawPath(
                path = path,
                color = HighlightSky,
                style = Stroke(width = 2.dp.toPx())
            )

            points.forEach { pt ->
                drawCircle(color = AccentPrimary, radius = 3.dp.toPx(), center = pt)
            }
        }
    }
}
