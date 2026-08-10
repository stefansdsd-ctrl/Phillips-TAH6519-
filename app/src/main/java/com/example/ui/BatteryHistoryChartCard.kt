package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
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

enum class BatteryChartStyle {
    BAR, LINE
}

data class DailyBatteryRecord(
    val dayLabel: String,
    val dateStr: String,
    val drainPercent: Int,      // Batterijverbruik %
    val rechargedPercent: Int,  // Opgeladen %
    val playTimeHours: Float,   // Gebruiksuren
    val ancActivePercent: Int,  // % van tijd met ANC aan
    val peakDrainRate: String   // Peak verbruik b.v. "12%/uur"
)

@Composable
fun BatteryHistoryChartCard(
    viewModel: HeadphoneViewModel,
    settings: HeadphoneSettings,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var chartStyle by remember { mutableStateOf(BatteryChartStyle.BAR) }
    var selectedDayIndex by remember { mutableIntStateOf(6) } // Standaard vandaag (Zondag/Laatste dag)

    // 7-Dagen Geschiedenis Data
    val batteryHistory = remember {
        listOf(
            DailyBatteryRecord("Ma", "01 Aug", drainPercent = 45, rechargedPercent = 0, playTimeHours = 6.5f, ancActivePercent = 85, peakDrainRate = "8.2%/u"),
            DailyBatteryRecord("Di", "02 Aug", drainPercent = 30, rechargedPercent = 50, playTimeHours = 4.2f, ancActivePercent = 60, peakDrainRate = "7.1%/u"),
            DailyBatteryRecord("Wo", "03 Aug", drainPercent = 65, rechargedPercent = 0, playTimeHours = 8.8f, ancActivePercent = 90, peakDrainRate = "9.5%/u"),
            DailyBatteryRecord("Do", "04 Aug", drainPercent = 20, rechargedPercent = 80, playTimeHours = 3.0f, ancActivePercent = 40, peakDrainRate = "6.0%/u"),
            DailyBatteryRecord("Vr", "05 Aug", drainPercent = 55, rechargedPercent = 0, playTimeHours = 7.4f, ancActivePercent = 95, peakDrainRate = "8.8%/u"),
            DailyBatteryRecord("Za", "06 Aug", drainPercent = 15, rechargedPercent = 0, playTimeHours = 2.1f, ancActivePercent = 30, peakDrainRate = "5.5%/u"),
            DailyBatteryRecord("Zo", "07 Aug", drainPercent = 40, rechargedPercent = 30, playTimeHours = 5.8f, ancActivePercent = 75, peakDrainRate = "7.8%/u")
        )
    }

    val selectedRecord = batteryHistory.getOrNull(selectedDayIndex) ?: batteryHistory.last()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("battery_history_chart_card"),
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
                            .background(StatusSuccess.copy(alpha = 0.15f))
                            .border(1.dp, StatusSuccess.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = "Battery History",
                            tint = StatusSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Batterijverbruik (7-Dagen)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Philips TAH6519 Accuhistorie & Laadcycli",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                // Chart Style Selector
                Row(
                    modifier = Modifier
                        .background(DarkBg, shape = RoundedCornerShape(8.dp))
                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (chartStyle == BatteryChartStyle.BAR) StatusSuccess.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                chartStyle = BatteryChartStyle.BAR
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("chart_type_bar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BarChart,
                            contentDescription = "Bar Chart",
                            tint = if (chartStyle == BatteryChartStyle.BAR) StatusSuccess else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (chartStyle == BatteryChartStyle.LINE) StatusSuccess.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                chartStyle = BatteryChartStyle.LINE
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("chart_type_line_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShowChart,
                            contentDescription = "Line Chart",
                            tint = if (chartStyle == BatteryChartStyle.LINE) StatusSuccess else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Metric Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Stat 1: Avg Daily Drain
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkBg),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("Gem. Verbruik", color = TextMuted, fontSize = 9.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("38.5%", color = StatusOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("/ dag", color = TextMuted, fontSize = 9.sp)
                        }
                    }
                }

                // Stat 2: Total Hours
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkBg),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("Totale Speeltijd", color = TextMuted, fontSize = 9.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("38.2u", color = HighlightSky, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("deze week", color = TextMuted, fontSize = 9.sp)
                        }
                    }
                }

                // Stat 3: Charge Cycles
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkBg),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("Laadcycli", color = TextMuted, fontSize = 9.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("1.6x", color = StatusSuccess, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("geladen", color = TextMuted, fontSize = 9.sp)
                        }
                    }
                }
            }

            // Interactive Canvas Battery Chart Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(DarkBg, shape = RoundedCornerShape(12.dp))
                    .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                val widthPerBar = size.width / batteryHistory.size
                                val clickedIdx = (tapOffset.x / widthPerBar).toInt().coerceIn(0, batteryHistory.size - 1)
                                selectedDayIndex = clickedIdx
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val bottomPadding = 24.dp.toPx()
                    val topPadding = 16.dp.toPx()
                    val chartHeight = canvasHeight - bottomPadding - topPadding
                    val count = batteryHistory.size
                    val itemWidth = canvasWidth / count

                    // Horizontal Grid Lines (0%, 25%, 50%, 75%, 100%)
                    val gridPercents = listOf(0.25f, 0.50f, 0.75f, 1.00f)
                    gridPercents.forEach { pct ->
                        val y = topPadding + chartHeight * (1f - pct)
                        drawLine(
                            color = DarkBorder.copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }

                    if (chartStyle == BatteryChartStyle.BAR) {
                        // Render Bar Chart
                        batteryHistory.forEachIndexed { idx, record ->
                            val xCenter = idx * itemWidth + itemWidth / 2f
                            val barWidth = itemWidth * 0.48f
                            val barLeft = xCenter - barWidth / 2f

                            val drainRatio = (record.drainPercent / 100f).coerceIn(0f, 1f)
                            val barHeight = chartHeight * drainRatio
                            val barTop = topPadding + (chartHeight - barHeight)

                            val isSelected = idx == selectedDayIndex

                            // Background highlight strip for selected day
                            if (isSelected) {
                                drawRect(
                                    color = StatusSuccess.copy(alpha = 0.12f),
                                    topLeft = Offset(idx * itemWidth, 0f),
                                    size = Size(itemWidth, canvasHeight)
                                )

                                drawLine(
                                    color = StatusSuccess,
                                    start = Offset(xCenter, topPadding),
                                    end = Offset(xCenter, topPadding + chartHeight),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                                )
                            }

                            // Main Drain Bar
                            val barColor = when {
                                record.drainPercent > 60 -> StatusDanger
                                record.drainPercent > 35 -> StatusOrange
                                else -> StatusSuccess
                            }

                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(barColor, barColor.copy(alpha = 0.4f))
                                ),
                                topLeft = Offset(barLeft, barTop),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )

                            // Charging Event Indicator Dot above bar if recharged
                            if (record.rechargedPercent > 0) {
                                drawCircle(
                                    color = HighlightSky,
                                    radius = 3.5.dp.toPx(),
                                    center = Offset(xCenter, barTop - 8.dp.toPx())
                                )
                            }
                        }
                    } else {
                        // Render Line Chart with Area Fill
                        val points = batteryHistory.mapIndexed { idx, record ->
                            val x = idx * itemWidth + itemWidth / 2f
                            val drainRatio = (record.drainPercent / 100f).coerceIn(0f, 1f)
                            val y = topPadding + chartHeight * (1f - drainRatio)
                            Offset(x, y)
                        }

                        // Path & Gradient Fill
                        val fillPath = Path().apply {
                            moveTo(points.first().x, topPadding + chartHeight)
                            lineTo(points.first().x, points.first().y)

                            for (i in 0 until points.size - 1) {
                                val p1 = points[i]
                                val p2 = points[i + 1]
                                val controlX1 = p1.x + (p2.x - p1.x) / 2f
                                val controlY1 = p1.y
                                val controlX2 = p1.x + (p2.x - p1.x) / 2f
                                val controlY2 = p2.y
                                cubicTo(controlX1, controlY1, controlX2, controlY2, p2.x, p2.y)
                            }

                            lineTo(points.last().x, topPadding + chartHeight)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    StatusSuccess.copy(alpha = 0.35f),
                                    StatusSuccess.copy(alpha = 0.02f)
                                )
                            )
                        )

                        // Smooth Line
                        val strokePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 0 until points.size - 1) {
                                val p1 = points[i]
                                val p2 = points[i + 1]
                                val controlX1 = p1.x + (p2.x - p1.x) / 2f
                                val controlY1 = p1.y
                                val controlX2 = p1.x + (p2.x - p1.x) / 2f
                                val controlY2 = p2.y
                                cubicTo(controlX1, controlY1, controlX2, controlY2, p2.x, p2.y)
                            }
                        }

                        drawPath(
                            path = strokePath,
                            color = StatusSuccess,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // Draw points and highlight
                        points.forEachIndexed { idx, pt ->
                            val isSelected = idx == selectedDayIndex

                            if (isSelected) {
                                drawLine(
                                    color = StatusSuccess,
                                    start = Offset(pt.x, topPadding),
                                    end = Offset(pt.x, topPadding + chartHeight),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                                )

                                drawCircle(
                                    color = StatusSuccess,
                                    radius = 7.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 3.dp.toPx(),
                                    center = pt
                                )
                            } else {
                                drawCircle(
                                    color = DarkBg,
                                    radius = 5.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = StatusSuccess,
                                    radius = 3.dp.toPx(),
                                    center = pt
                                )
                            }
                        }
                    }
                }

                // Day Labels Row Overlay at bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    batteryHistory.forEachIndexed { idx, record ->
                        val isSelected = idx == selectedDayIndex
                        Text(
                            text = record.dayLabel,
                            color = if (isSelected) StatusSuccess else TextMuted,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .testTag("day_bar_item_$idx")
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    selectedDayIndex = idx
                                }
                        )
                    }
                }
            }

            // Detailed Breakdown Card for Selected Day
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("selected_day_details_card"),
                colors = CardDefaults.cardColors(containerColor = DarkBg),
                border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
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
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                tint = StatusSuccess,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Details: ${selectedRecord.dayLabel} (${selectedRecord.dateStr})",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        if (selectedRecord.rechargedPercent > 0) {
                            Box(
                                modifier = Modifier
                                    .background(HighlightSky.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp))
                                    .border(1.dp, HighlightSky.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Bolt,
                                        contentDescription = null,
                                        tint = HighlightSky,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "+${selectedRecord.rechargedPercent}% Geladen",
                                        color = HighlightSky,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = DarkBorder)

                    // Details Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Totaal Verbruikt", color = TextMuted, fontSize = 9.sp)
                            Text("${selectedRecord.drainPercent}%", color = StatusOrange, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Column {
                            Text("Actieve Luistertijd", color = TextMuted, fontSize = 9.sp)
                            Text("${selectedRecord.playTimeHours} uur", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Column {
                            Text("ANC Inschakeling", color = TextMuted, fontSize = 9.sp)
                            Text("${selectedRecord.ancActivePercent}% van de tijd", color = HighlightSky, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Column {
                            Text("Piek Snelheid", color = TextMuted, fontSize = 9.sp)
                            Text(selectedRecord.peakDrainRate, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
