package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HeadphoneSettings
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

enum class VisualizerMode(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    SPECTRUM_BARS("Spectrum Staven", Icons.Default.GraphicEq),
    RADIAL_PULSE("Cirkel Pulse", Icons.Default.RadioButtonChecked),
    LIQUID_WAVE("Liquid Waves", Icons.Default.Waves)
}

@Composable
fun YouTubeMusicVisualizer(
    viewModel: HeadphoneViewModel,
    settings: HeadphoneSettings,
    modifier: Modifier = Modifier
) {
    val isPlaying by viewModel.mediaIsPlaying.collectAsState()
    val isYoutubeActive by viewModel.isYoutubeActive.collectAsState()
    val currentTrackIndex by viewModel.currentTrackIndex.collectAsState()
    val youtubePlaylistTracks by viewModel.youtubePlaylistTracks.collectAsState()
    val mediaProgressSecs by viewModel.mediaProgress.collectAsState()

    var activeMode by remember { mutableStateOf(VisualizerMode.SPECTRUM_BARS) }
    var sensitivity by remember { mutableFloatStateOf(1.0f) }
    var selectedColorIndex by remember { mutableIntStateOf(0) }

    val colorSchemes = listOf(
        "YouTube Red" to listOf(Color(0xFFFF0000), Color(0xFFFF5252), Color(0xFFFF8A8A)),
        "Cyber Neon" to listOf(Color(0xFFE047FF), Color(0xFF00E5FF), Color(0xFF7000FF)),
        "Studio Blue" to listOf(Color(0xFF0066FF), Color(0xFF00D2FF), Color(0xFF0044B3)),
        "Amber Gold" to listOf(Color(0xFFFFD700), Color(0xFFFF9100), Color(0xFFFF5722)),
        "Aurora Green" to listOf(Color(0xFF10B981), Color(0xFF00F5A0), Color(0xFF059669))
    )

    val currentPalette = colorSchemes[selectedColorIndex].second
    val primaryColor = currentPalette[0]
    val secondaryColor = currentPalette[1]
    val accentColor = currentPalette[2]

    // Animation drivers
    val infiniteTransition = rememberInfiniteTransition(label = "yt_vis_transition")
    
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "yt_phase"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "yt_pulse"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("youtube_music_visualizer_card"),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, if (isYoutubeActive && isPlaying) primaryColor.copy(alpha = 0.5f) else DarkBorder),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with YouTube badge and mode selector
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
                            .size(32.dp)
                            .background(primaryColor.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, primaryColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Visualizer Icon",
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Live Audio Visualizer",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isYoutubeActive && isPlaying) "Reactief op YouTube Music Stream" else "Pauzeer / Speel af op YouTube",
                            color = if (isPlaying) primaryColor else TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Live status dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(
                            if (isPlaying) primaryColor.copy(alpha = 0.12f) else DarkBg,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (isPlaying) primaryColor else TextMuted, CircleShape)
                    )
                    Text(
                        text = if (isPlaying) "LIVE BEAT" else "IDLE",
                        color = if (isPlaying) primaryColor else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Mode Toggle Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VisualizerMode.entries.forEach { mode ->
                    val selected = activeMode == mode
                    FilterChip(
                        selected = selected,
                        onClick = { activeMode = mode },
                        label = {
                            Text(
                                text = mode.label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = mode.icon,
                                contentDescription = mode.label,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("youtube_visualizer_mode_${mode.name.lowercase()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primaryColor.copy(alpha = 0.2f),
                            selectedLabelColor = primaryColor,
                            selectedLeadingIconColor = primaryColor,
                            containerColor = DarkBg,
                            labelColor = TextMuted,
                            iconColor = TextMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = DarkBorder,
                            selectedBorderColor = primaryColor.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // MAIN VISUALIZER CANVAS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFF0D0F14), RoundedCornerShape(16.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("youtube_visualizer_canvas"),
                contentAlignment = Alignment.Center
            ) {
                val activeTrack = if (isYoutubeActive && youtubePlaylistTracks.isNotEmpty() && currentTrackIndex in youtubePlaylistTracks.indices) {
                    youtubePlaylistTracks[currentTrackIndex]
                } else null

                val title = activeTrack?.title ?: viewModel.mediaTrackName.collectAsState().value
                val artist = activeTrack?.artist ?: viewModel.mediaTrackArtist.collectAsState().value

                when (activeMode) {
                    VisualizerMode.SPECTRUM_BARS -> {
                        SpectrumBarsCanvas(
                            isPlaying = isPlaying,
                            phase = phase,
                            sensitivity = sensitivity,
                            progress = mediaProgressSecs,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            accentColor = accentColor
                        )
                    }
                    VisualizerMode.RADIAL_PULSE -> {
                        RadialPulseCanvas(
                            isPlaying = isPlaying,
                            phase = phase,
                            pulseScale = pulseScale,
                            sensitivity = sensitivity,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            title = title
                        )
                    }
                    VisualizerMode.LIQUID_WAVE -> {
                        LiquidWaveCanvas(
                            isPlaying = isPlaying,
                            phase = phase,
                            sensitivity = sensitivity,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            accentColor = accentColor
                        )
                    }
                }

                // Overlay info banner at bottom of canvas
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = artist,
                                color = TextMuted,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = "${(sensitivity * 100).toInt()}% Sens",
                            color = primaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // CONTROLS BAR: Sensitivity Slider & Color Palette Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sensitivity slider
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Gevoeligheid",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = String.format("%.1fx", sensitivity),
                            color = primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = sensitivity,
                        onValueChange = { sensitivity = it },
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier.testTag("youtube_visualizer_sensitivity_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = primaryColor,
                            activeTrackColor = primaryColor,
                            inactiveTrackColor = DarkBorder
                        )
                    )
                }

                // Palette dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("youtube_visualizer_theme_selector")
                ) {
                    colorSchemes.forEachIndexed { idx, (name, colors) ->
                        val selected = selectedColorIndex == idx
                        Box(
                            modifier = Modifier
                                .size(if (selected) 24.dp else 18.dp)
                                .background(colors[0], CircleShape)
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorIndex = idx }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpectrumBarsCanvas(
    isPlaying: Boolean,
    phase: Float,
    sensitivity: Float,
    progress: Int,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color
) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val barCount = 32
        val barWidth = size.width / (barCount * 1.5f)
        val gap = barWidth * 0.5f
        val startX = (size.width - (barCount * (barWidth + gap))) / 2f
        val maxHeight = size.height * 0.75f

        for (i in 0 until barCount) {
            val freqOffset = i * 0.25f
            val rawAmp = if (isPlaying) {
                val wave1 = sin(phase * 1.8f + freqOffset)
                val wave2 = cos(phase * 2.4f - freqOffset * 0.5f)
                val beat = sin(phase * 4f + (i % 4) * 0.5f)
                ((wave1 + wave2 + beat + 3f) / 6f) * sensitivity
            } else 0.08f

            val barHeight = (rawAmp.coerceIn(0.05f, 1.0f) * maxHeight)
            val x = startX + i * (barWidth + gap)
            val y = (size.height + maxHeight) / 2f - barHeight

            val brush = Brush.verticalGradient(
                colors = listOf(accentColor, primaryColor, secondaryColor),
                startY = y,
                endY = y + barHeight
            )

            // Draw bar
            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2, barWidth / 2)
            )

            // Draw peak dot above bar
            if (isPlaying) {
                val peakY = (y - 8f).coerceAtLeast(20f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = barWidth / 3f,
                    center = Offset(x + barWidth / 2f, peakY)
                )
            }
        }
    }
}

@Composable
private fun RadialPulseCanvas(
    isPlaying: Boolean,
    phase: Float,
    pulseScale: Float,
    sensitivity: Float,
    primaryColor: Color,
    secondaryColor: Color,
    title: String
) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = 42.dp.toPx()
        val currentRadius = if (isPlaying) baseRadius * pulseScale else baseRadius

        // Surrounding radial sound spikes
        val spikeCount = 48
        for (i in 0 until spikeCount) {
            val angle = (i.toFloat() / spikeCount) * 2 * PI.toFloat()
            val noise = if (isPlaying) {
                sin(phase * 2f + angle * 3f) * 0.35f + cos(phase * 1.5f - angle * 2f) * 0.25f
            } else 0f

            val spikeLength = (15.dp.toPx() + (noise * 25.dp.toPx() * sensitivity)).coerceAtLeast(4.dp.toPx())
            val startRad = currentRadius + 6.dp.toPx()
            val endRad = startRad + spikeLength

            val startOffset = Offset(
                center.x + cos(angle) * startRad,
                center.y + sin(angle) * startRad
            )
            val endOffset = Offset(
                center.x + cos(angle) * endRad,
                center.y + sin(angle) * endRad
            )

            drawLine(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor, secondaryColor),
                    center = center,
                    radius = currentRadius + 50.dp.toPx()
                ),
                start = startOffset,
                end = endOffset,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Inner glowing core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = 0.8f), secondaryColor.copy(alpha = 0.3f), Color.Transparent),
                center = center,
                radius = currentRadius * 1.3f
            ),
            radius = currentRadius * 1.2f,
            center = center
        )

        drawCircle(
            color = Color(0xFF151922),
            radius = currentRadius,
            center = center
        )

        drawCircle(
            color = primaryColor,
            radius = currentRadius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
private fun LiquidWaveCanvas(
    isPlaying: Boolean,
    phase: Float,
    sensitivity: Float,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color
) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val layers = 3
        for (layer in 0 until layers) {
            val layerPhase = phase + (layer * PI / 3).toFloat()
            val layerColor = when (layer) {
                0 -> primaryColor.copy(alpha = 0.7f)
                1 -> secondaryColor.copy(alpha = 0.5f)
                else -> accentColor.copy(alpha = 0.3f)
            }

            val path = Path()
            path.moveTo(0f, height)

            val points = 50
            for (i in 0..points) {
                val x = (i.toFloat() / points) * width
                val normalizedX = (i.toFloat() / points) * 2 * PI.toFloat()

                val wave = if (isPlaying) {
                    sin(normalizedX * 2f + layerPhase) * 35.dp.toPx() * sensitivity +
                            cos(normalizedX * 3f - layerPhase * 1.2f) * 20.dp.toPx() * sensitivity
                } else {
                    sin(normalizedX * 1.5f + layerPhase) * 6.dp.toPx()
                }

                val y = centerY + wave
                if (i == 0) path.lineTo(x, y) else path.lineTo(x, y)
            }

            path.lineTo(width, height)
            path.close()

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(layerColor, Color.Transparent),
                    startY = centerY - 50.dp.toPx(),
                    endY = height
                )
            )
        }
    }
}
