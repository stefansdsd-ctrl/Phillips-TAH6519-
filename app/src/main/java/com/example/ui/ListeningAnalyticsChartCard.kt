package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkPanel
import com.example.ui.theme.HighlightSky
import com.example.ui.theme.StatusOrange
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

enum class TimeframeMode {
    DAILY, WEEKLY
}

enum class ChartType {
    BAR, TREND_LINE
}

data class ListeningDataPoint(
    val label: String,
    val musicHours: Float,
    val ancHours: Float,
    val callsHours: Float,
    val avgDb: Int
) {
    val totalHours: Float get() = musicHours + ancHours + callsHours
}

@Composable
fun ListeningAnalyticsChartCard(viewModel: HeadphoneViewModel) {
    val haptic = LocalHapticFeedback.current
    var timeframe by remember { mutableStateOf(TimeframeMode.WEEKLY) }
    var chartType by remember { mutableStateOf(ChartType.BAR) }
    var selectedBarIndex by remember { mutableStateOf<Int?>(3) } // default selected day/hour

    val weeklyData = remember {
        listOf(
            ListeningDataPoint("Ma", 2.1f, 1.2f, 0.5f, 72),
            ListeningDataPoint("Di", 3.0f, 1.8f, 0.4f, 75),
            ListeningDataPoint("Wo", 1.8f, 2.0f, 0.8f, 68),
            ListeningDataPoint("Do", 3.5f, 2.2f, 0.6f, 78),
            ListeningDataPoint("Vr", 4.2f, 1.5f, 0.3f, 81),
            ListeningDataPoint("Za", 5.0f, 0.8f, 0.2f, 74),
            ListeningDataPoint("Zo", 2.4f, 1.0f, 0.1f, 70)
        )
    }

    val dailyData = remember {
        listOf(
            ListeningDataPoint("08:00", 0.5f, 0.5f, 0.2f, 68),
            ListeningDataPoint("10:00", 0.8f, 0.2f, 0.5f, 74),
            ListeningDataPoint("12:00", 1.2f, 0.3f, 0.1f, 76),
            ListeningDataPoint("14:00", 0.6f, 0.8f, 0.4f, 70),
            ListeningDataPoint("16:00", 1.0f, 0.5f, 0.3f, 72),
            ListeningDataPoint("18:00", 1.5f, 0.2f, 0.0f, 82),
            ListeningDataPoint("20:00", 0.9f, 0.4f, 0.1f, 69)
        )
    }

    val activeDataset = if (timeframe == TimeframeMode.WEEKLY) weeklyData else dailyData
    val maxHours = (activeDataset.maxOfOrNull { it.totalHours } ?: 6f).coerceAtLeast(4f)
    val selectedItem = selectedBarIndex?.let { activeDataset.getOrNull(it) } ?: activeDataset.last()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("listening_analytics_chart_card"),
        colors = CardDefaults.cardColors(containerColor = DarkPanel),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
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
                            .background(AccentPrimary.copy(alpha = 0.15f))
                            .border(1.dp, AccentPrimary.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Analytics,
                            contentDescription = "Analytics",
                            tint = HighlightSky,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Luisterstatistieken & Analyse",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Gedetailleerd overzicht van draagtijd & volume",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                // Timeframe Selector Toggle (Dag / Week)
                Row(
                    modifier = Modifier
                        .background(DarkBg, shape = RoundedCornerShape(20.dp))
                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(20.dp))
                        .padding(2.dp)
                ) {
                    TimeframeMode.values().forEach { mode ->
                        val isSelected = timeframe == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) HighlightSky.copy(alpha = 0.25f) else Color.Transparent)
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) HighlightSky else Color.Transparent,
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    timeframe = mode
                                    selectedBarIndex = 3
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (mode == TimeframeMode.WEEKLY) "Wekelijks" else "Dagelijks",
                                color = if (isSelected) HighlightSky else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Summary Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    label = if (timeframe == TimeframeMode.WEEKLY) "Totaal Deze Week" else "Totaal Vandaag",
                    value = String.format("%.1f uur", activeDataset.sumOf { it.totalHours.toDouble() }),
                    subtext = "40h ANC Capaciteit",
                    accentColor = HighlightSky,
                    modifier = Modifier.weight(1f)
                )

                MetricPill(
                    label = "Gemiddelde Sessie",
                    value = String.format("%.1f uur", activeDataset.map { it.totalHours }.average()),
                    subtext = "Optimale balans",
                    accentColor = StatusSuccess,
                    modifier = Modifier.weight(1f)
                )

                MetricPill(
                    label = "Volume Belasting",
                    value = "${activeDataset.map { it.avgDb }.average().toInt()} dB",
                    subtext = "Veilige limiet (<85dB)",
                    accentColor = StatusYellow,
                    modifier = Modifier.weight(1f)
                )
            }

            // Chart Controls Row (Bar vs Trend) & Category Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = AccentPrimary, label = "Muziek")
                    LegendItem(color = HighlightSky, label = "ANC Stilte")
                    LegendItem(color = StatusOrange, label = "Bellen")
                }

                // Chart Style Selector (Bar vs Trend Line)
                Row(
                    modifier = Modifier
                        .background(DarkBg, shape = RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            chartType = ChartType.BAR
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BarChart,
                            contentDescription = "Staafgrafiek",
                            tint = if (chartType == ChartType.BAR) HighlightSky else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            chartType = ChartType.TREND_LINE
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShowChart,
                            contentDescription = "Lijngrafiek",
                            tint = if (chartType == ChartType.TREND_LINE) HighlightSky else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Main Interactive Chart Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(DarkBg, shape = RoundedCornerShape(12.dp))
                    .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                if (chartType == ChartType.BAR) {
                    StackedBarChart(
                        dataset = activeDataset,
                        maxHours = maxHours,
                        selectedIndex = selectedBarIndex,
                        onSelectIndex = { idx ->
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            selectedBarIndex = idx
                        }
                    )
                } else {
                    TrendLineChart(
                        dataset = activeDataset,
                        maxHours = maxHours,
                        selectedIndex = selectedBarIndex,
                        onSelectIndex = { idx ->
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            selectedBarIndex = idx
                        }
                    )
                }
            }

            // Detailed Selected Data Point Breakdown Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, HighlightSky.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Geselecteerd: ${selectedItem.label}",
                            color = HighlightSky,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Totale luisterduur: ${String.format("%.1f", selectedItem.totalHours)} uur",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("🎵 Muziek: ${selectedItem.musicHours}u", color = TextMuted, fontSize = 10.sp)
                            Text("🛡️ ANC Stilte: ${selectedItem.ancHours}u", color = TextMuted, fontSize = 10.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("📞 Bellen: ${selectedItem.callsHours}u", color = TextMuted, fontSize = 10.sp)
                            Text("🔊 Gem. Volume: ${selectedItem.avgDb} dB", color = StatusSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    subtext: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(DarkBg, shape = RoundedCornerShape(10.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(10.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = TextMuted, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 1)
        Text(value, color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 2.dp))
        Text(subtext, color = TextMuted.copy(alpha = 0.8f), fontSize = 8.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, color = TextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun StackedBarChart(
    dataset: List<ListeningDataPoint>,
    maxHours: Float,
    selectedIndex: Int?,
    onSelectIndex: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        dataset.forEachIndexed { idx, point ->
            val isSelected = selectedIndex == idx
            val totalFraction = (point.totalHours / maxHours).coerceIn(0.05f, 1f)

            val musicFraction = if (point.totalHours > 0) point.musicHours / point.totalHours else 0f
            val ancFraction = if (point.totalHours > 0) point.ancHours / point.totalHours else 0f
            val callsFraction = if (point.totalHours > 0) point.callsHours / point.totalHours else 0f

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectIndex(idx) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // Stacked Bar Container
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.82f)
                        .width(if (isSelected) 22.dp else 16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight(totalFraction)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                            )
                    ) {
                        // Top segment: Calls
                        if (callsFraction > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(callsFraction)
                                    .fillMaxWidth()
                                    .background(StatusOrange)
                            )
                        }
                        // Middle segment: ANC
                        if (ancFraction > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(ancFraction)
                                    .fillMaxWidth()
                                    .background(HighlightSky)
                            )
                        }
                        // Bottom segment: Music
                        if (musicFraction > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(musicFraction)
                                    .fillMaxWidth()
                                    .background(AccentPrimary)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // X-Axis Label
                Text(
                    text = point.label,
                    color = if (isSelected) HighlightSky else TextMuted,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun TrendLineChart(
    dataset: List<ListeningDataPoint>,
    maxHours: Float,
    selectedIndex: Int?,
    onSelectIndex: (Int) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val segmentWidth = size.width / dataset.size
                    val clickedIdx = (offset.x / segmentWidth).toInt().coerceIn(0, dataset.size - 1)
                    onSelectIndex(clickedIdx)
                }
            }
    ) {
        val width = size.width
        val height = size.height - 24.dp.toPx() // leave space for bottom labels
        val stepX = width / (dataset.size - 1)

        val points = dataset.mapIndexed { index, dataPoint ->
            val x = index * stepX
            val y = height - ((dataPoint.totalHours / maxHours) * height)
            Offset(x, y)
        }

        // Draw smooth path
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

            // Draw Area Fill Gradient
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(HighlightSky.copy(alpha = 0.35f), Color.Transparent)
                )
            )

            // Draw Line
            drawPath(
                path = path,
                color = HighlightSky,
                style = Stroke(width = 3.dp.toPx())
            )

            // Draw Points
            points.forEachIndexed { i, pt ->
                val isSelected = selectedIndex == i
                drawCircle(
                    color = if (isSelected) Color.White else AccentPrimary,
                    radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                    center = pt
                )
                if (isSelected) {
                    drawCircle(
                        color = HighlightSky,
                        radius = 10.dp.toPx(),
                        center = pt,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}
