package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HeadphoneSettings
import com.example.SectionHeader
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Particles data model for orbital rendering
data class OrbitParticle(
    val baseAngle: Float,
    val orbitSpeed: Float,
    val baseRadius: Float,
    val size: Float,
    val baseColor: Color,
    val radiusPhase: Float,
    val verticalSpeed: Float
)

@Composable
fun FullScreenAmbientVisualizer(
    viewModel: HeadphoneViewModel,
    settings: HeadphoneSettings
) {
    val haptic = LocalHapticFeedback.current
    
    // Ambient styles configuration
    var selectedVisualTheme by remember { mutableStateOf("Cyber Neon") }
    var particleCountInput by remember { mutableStateOf(50f) }
    var rotationSpeedMultiplier by remember { mutableStateOf(1f) }
    var isFlowModeActive by remember { mutableStateOf(true) }
    var visualizerStyle by remember { mutableStateOf("Universe") } // "Universe", "Liquid", "Vortex"

    // Backlight Glow Color Scheme
    val themeColors = when (selectedVisualTheme) {
        "Cyber Neon" -> listOf(AccentPrimary, StatusMagenta, StatusPurple)
        "Ocean Studio" -> listOf(Color(0xFF0054E6), Color(0xFF00C896), HighlightSky)
        "Amber Gold" -> listOf(StatusOrange, StatusYellow, Color(0xFFD97706))
        "Nordic Aurora" -> listOf(Color(0xFF10B981), Color(0xFF00C896), StatusLime)
        else -> listOf(AccentPrimary, HighlightSky, TextMuted)
    }

    // Dynamic morphing theme loop
    if (isFlowModeActive) {
        val transition = rememberInfiniteTransition(label = "flow_theme")
        val themeStep by transition.animateFloat(
            initialValue = 0f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "theme_step"
        )
        
        LaunchedEffect(themeStep.toInt()) {
            val nextTheme = when (themeStep.toInt()) {
                0 -> "Cyber Neon"
                1 -> "Ocean Studio"
                2 -> "Amber Gold"
                3 -> "Nordic Aurora"
                else -> "Cyber Neon"
            }
            if (nextTheme != selectedVisualTheme) {
                selectedVisualTheme = nextTheme
            }
        }
    }

    // User manual drag rotation angle
    var manualDragAngle by remember { mutableStateOf(0f) }

    // Master play status derived scale pulse
    val playPulseTransition = rememberInfiniteTransition(label = "play_pulse")
    val ambientScaleMultiplier by playPulseTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_scale"
    )

    val kineticOrbitTime by playPulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween((12000 / rotationSpeedMultiplier).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_time"
    )

    // Precalculate fixed orbit particles to maintain stable identity across frames
    val particlesList = remember {
        List(100) { index ->
            OrbitParticle(
                baseAngle = Random.nextFloat() * 2f * Math.PI.toFloat(),
                orbitSpeed = (0.3f + Random.nextFloat() * 1.2f) * (if (Random.nextBoolean()) 1f else -1f),
                baseRadius = 55f + Random.nextFloat() * 85f,
                size = 2f + Random.nextFloat() * 6f,
                baseColor = when (index % 3) {
                    0 -> Color.White
                    1 -> AccentPrimary
                    else -> HighlightSky
                },
                radiusPhase = Random.nextFloat() * 100f,
                verticalSpeed = 0.1f + Random.nextFloat() * 0.4f
            )
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ambient_visualizer_tab")
    ) {
        // Advanced Interactive Canvas Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DarkPanel, DarkBg)
                    )
                )
                .border(1.dp, DarkBorder, shape = RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        manualDragAngle += dragAmount.x * 0.4f
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Draw Interactive Kinetic Particle Systems
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val centerX = w / 2f
                val centerY = h / 2f

                // 1. Draw glowing background aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            themeColors[0].copy(alpha = 0.22f * ambientScaleMultiplier),
                            themeColors[1].copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(centerX, centerY),
                        radius = 160.dp.toPx()
                    ),
                    radius = 160.dp.toPx(),
                    center = Offset(centerX, centerY)
                )

                // 2. Draw sound radiating energy waves
                val waveCount = 3
                for (i in 0 until waveCount) {
                    val progress = ((kineticOrbitTime / (2f * Math.PI.toFloat())) + (i.toFloat() / waveCount)) % 1f
                    val waveRadius = (60.dp.toPx() + progress * 110.dp.toPx()) * ambientScaleMultiplier
                    val opacity = (1f - progress) * 0.18f
                    drawCircle(
                        color = themeColors[i % themeColors.size].copy(alpha = opacity),
                        radius = waveRadius,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // 3. Draw particles based on user-selected density
                val maxParticles = particleCountInput.toInt()
                for (i in 0 until maxParticles) {
                    val particle = particlesList[i]
                    val angle = particle.baseAngle + kineticOrbitTime * particle.orbitSpeed
                    
                    val expansion = sin(kineticOrbitTime * 1.5f + particle.radiusPhase) * 12.dp.toPx()
                    val radius = (particle.baseRadius.dp.toPx() + expansion) * when (visualizerStyle) {
                        "Liquid" -> 0.75f + 0.25f * sin(angle * 2.5f)
                        "Vortex" -> 1f - (kineticOrbitTime / (2f * Math.PI.toFloat())) * 0.3f
                        else -> 1.0f
                    }

                    val px = centerX + cos(angle) * radius
                    val py = centerY + sin(angle) * radius + sin(kineticOrbitTime * particle.verticalSpeed) * 8.dp.toPx()

                    // Interpolate particle color with current dynamic active theme palette
                    val pColor = when (i % 3) {
                        0 -> themeColors[0].copy(alpha = 0.8f)
                        1 -> themeColors[1].copy(alpha = 0.7f)
                        else -> Color.White.copy(alpha = 0.9f)
                    }

                    drawCircle(
                        color = pColor,
                        radius = particle.size.dp.toPx(),
                        center = Offset(px, py)
                    )
                }

                // 4. Draw modern geometric grid overlays
                val gridOpacity = 0.04f
                drawLine(
                    color = Color.White.copy(alpha = gridOpacity),
                    start = Offset(0f, centerY),
                    end = Offset(w, centerY),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = gridOpacity),
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, h),
                    strokeWidth = 1.dp.toPx()
                )
                
                // Draw surrounding dynamic sound bars (Visual Spectrum Circular)
                val barsCount = 36
                val stepAngle = (2 * Math.PI / barsCount).toFloat()
                val innerRadius = 70.dp.toPx()
                for (b in 0 until barsCount) {
                    val barAngle = b * stepAngle + manualDragAngle * 0.01f
                    val waveAmp = sin(b * 0.5f + kineticOrbitTime * 3f) * 15.dp.toPx() * ambientScaleMultiplier
                    val barHeight = (8.dp.toPx() + kotlin.math.abs(waveAmp)).coerceAtLeast(3.dp.toPx())
                    
                    val startX = centerX + cos(barAngle) * innerRadius
                    val startY = centerY + sin(barAngle) * innerRadius
                    val endX = centerX + cos(barAngle) * (innerRadius + barHeight)
                    val endY = centerY + sin(barAngle) * (innerRadius + barHeight)
                    
                    drawLine(
                        color = themeColors[b % themeColors.size].copy(alpha = 0.45f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 2.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }

            // Interactive Rotating 3D-Look Headphone Image Drawing
            val tiltAngle = manualDragAngle + sin(kineticOrbitTime) * 8f
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer {
                        rotationZ = tiltAngle
                        scaleX = ambientScaleMultiplier * 1.05f
                        scaleY = ambientScaleMultiplier * 1.05f
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = "Dynamic Rotating Headphone",
                    tint = Color.White,
                    modifier = Modifier.size(96.dp)
                )
            }

            // Visual indicator instructions at top right of canvas
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.RotateRight,
                    contentDescription = null,
                    tint = HighlightSky,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = "Sleep om te draaien",
                    color = TextPrimary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Current theme badge at top left
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(themeColors[0].copy(alpha = 0.2f), shape = RoundedCornerShape(20.dp))
                    .border(1.dp, themeColors[0].copy(alpha = 0.4f), shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(themeColors[0], shape = CircleShape)
                )
                Text(
                    text = selectedVisualTheme.uppercase(),
                    color = TextPrimary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Section header for Sfeer en Effecten
        SectionHeader(title = "Sfeer & Licht Effecten")

        // 1. Style Selection Row (Universe, Liquid, Vortex)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkPanel, shape = RoundedCornerShape(12.dp))
                .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            val visualStylesList = listOf(
                "Universe" to Icons.Default.Language,
                "Liquid" to Icons.Default.WaterDrop,
                "Vortex" to Icons.Default.Cyclone
            )
            visualStylesList.forEach { (style, icon) ->
                val isSelected = visualizerStyle == style
                val activeBgColor by animateColorAsState(
                    targetValue = if (isSelected) AccentPrimary else Color.Transparent,
                    animationSpec = tween(250),
                    label = "style_bg"
                )
                val activeTextColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else TextMuted,
                    animationSpec = tween(250),
                    label = "style_text"
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(activeBgColor)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            visualizerStyle = style
                        }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = style,
                        color = activeTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Custom Controls Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkPanel, shape = RoundedCornerShape(16.dp))
                .border(1.dp, DarkBorder, shape = RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Color preset selector buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Achtergrond Sfeerthema",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val visualThemes = listOf(
                            "Cyber Neon" to Color(0xFFE047FF),
                            "Ocean Studio" to Color(0xFF0066FF),
                            "Amber Gold" to Color(0xFFF59E0B),
                            "Nordic Aurora" to Color(0xFF10B981)
                        )
                        
                        visualThemes.forEach { (themeName, color) ->
                            val isSelected = selectedVisualTheme == themeName
                            val borderThickness = if (isSelected) 2.dp else 1.dp
                            val borderColor = if (isSelected) Color.White else DarkBorder
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(borderThickness, borderColor, shape = RoundedCornerShape(8.dp))
                                    .background(color.copy(alpha = 0.15f))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isFlowModeActive = false
                                        selectedVisualTheme = themeName
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(color, shape = CircleShape)
                                    )
                                    Text(
                                        text = themeName.split(" ").first(),
                                        color = if (isSelected) TextPrimary else TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Auto Color Flow Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lichtstroom Autopiloot",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Vloeit automatisch en continu over in alle sfeerthema's",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = isFlowModeActive,
                        onCheckedChange = { isFlowModeActive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentPrimary
                        ),
                        modifier = Modifier.testTag("flow_autopilot_switch")
                    )
                }

                HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

                // Density slider
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Deeltjesdichtheid (Sterrenstof)",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${particleCountInput.toInt()} deeltjes",
                            color = HighlightSky,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = particleCountInput,
                        onValueChange = { particleCountInput = it },
                        valueRange = 10f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = HighlightSky,
                            activeTrackColor = HighlightSky,
                            inactiveTrackColor = DarkBorder
                        )
                    )
                }

                // Rotation Speed slider
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Animatie & Rotatiesnelheid",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = String.format("%.1fx", rotationSpeedMultiplier),
                            color = HighlightSky,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = rotationSpeedMultiplier,
                        onValueChange = { rotationSpeedMultiplier = it },
                        valueRange = 0.2f..3.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = HighlightSky,
                            activeTrackColor = HighlightSky,
                            inactiveTrackColor = DarkBorder
                        )
                    )
                }
            }
        }

        // Live Audio Spectrum card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkPanel, shape = RoundedCornerShape(16.dp))
                .border(1.dp, DarkBorder, shape = RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Live Spectrum Analyser",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Frequentiebanden in real-time gesimuleerd",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Dynamic vertical bar chart animating rapidly
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    val frequencyBarsCount = 18
                    for (f in 0 until frequencyBarsCount) {
                        val infiniteBarTransition = rememberInfiniteTransition(label = "freq_bar_$f")
                        val barHeightFactor by infiniteBarTransition.animateFloat(
                            initialValue = 0.1f + (f % 4) * 0.2f,
                            targetValue = 0.9f - (f % 3) * 0.15f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(300 + (f * 50) % 600, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "freq_bar_height_$f"
                        )
                        
                        val col = when {
                            f < 4 -> EQBandColors[0]
                            f < 8 -> EQBandColors[2]
                            f < 12 -> EQBandColors[5]
                            else -> EQBandColors[8]
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(barHeightFactor)
                                .background(col, shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        )
                    }
                }
            }
        }
    }
}
