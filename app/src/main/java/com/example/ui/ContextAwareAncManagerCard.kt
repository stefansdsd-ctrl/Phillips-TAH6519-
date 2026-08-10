package com.example.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HeadphoneSettings
import com.example.ui.theme.*
import kotlin.math.sqrt

@Composable
fun ContextAwareAncManagerCard(
    viewModel: HeadphoneViewModel,
    settings: HeadphoneSettings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    var isMoving by remember { mutableStateOf(false) }
    var currentAcceleration by remember { mutableStateOf(0f) }

    val adaptiveEnabled = settings.adaptiveActivityEnabled

    DisposableEffect(adaptiveEnabled) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    // Calculate total acceleration minus gravity
                    val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat() - SensorManager.GRAVITY_EARTH
                    currentAcceleration = acceleration

                    // Simple movement detection threshold (e.g. > 2.5 means walking/moving)
                    val moving = Math.abs(acceleration) > 2.5f

                    if (moving != isMoving) {
                        isMoving = moving
                        if (adaptiveEnabled) {
                            if (moving && settings.ancMode != "TRANSPARENCY") {
                                viewModel.setAncMode("TRANSPARENCY") // Awareness mode
                            } else if (!moving && settings.ancMode != "ON") {
                                viewModel.setAncMode("ON") // ANC On
                            }
                        }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (adaptiveEnabled && accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("context_aware_anc_manager_card"),
        colors = CardDefaults.cardColors(containerColor = DarkPanel),
        border = BorderStroke(1.dp, if (adaptiveEnabled) HighlightSky else DarkBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(HighlightSky.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, HighlightSky.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.DirectionsWalk, contentDescription = "Context Aware", tint = HighlightSky, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Context-Aware ANC", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Automatisch schakelen op beweging", color = TextMuted, fontSize = 11.sp)
                    }
                }
                Switch(
                    checked = adaptiveEnabled,
                    onCheckedChange = { 
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        viewModel.toggleAdaptiveActivity(it) 
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = HighlightSky, 
                        checkedTrackColor = HighlightSky.copy(alpha = 0.4f)
                    )
                )
            }

            AnimatedVisibility(visible = adaptiveEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Huidige Activiteit (Sensor Data)", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = if (isMoving) Icons.Filled.DirectionsWalk else Icons.Filled.AirlineSeatReclineNormal,
                                contentDescription = null,
                                tint = if (isMoving) StatusOrange else StatusSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isMoving) "In Beweging -> Awareness Mode" else "Stilstaand -> ANC Actief",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    // Simple live movement indicator
                    val indicatorScale by animateFloatAsState(targetValue = if (isMoving) 1.5f else 1f)
                    val indicatorColor by animateColorAsState(targetValue = if (isMoving) StatusOrange else StatusSuccess)
                    
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .scale(indicatorScale)
                            .background(indicatorColor.copy(alpha = 0.5f), CircleShape)
                            .border(1.dp, indicatorColor, CircleShape)
                    )
                }
            }

            HorizontalDivider(color = DarkBorder)
            
            // Dedicated Toggle Control Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "DEDICATED ANC MODI TOGGLE",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard, RoundedCornerShape(12.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val modes = listOf(
                        Triple("ON", "ANC On", Icons.Filled.GraphicEq),
                        Triple("TRANSPARENCY", "Awareness", Icons.Filled.Hearing),
                        Triple("OFF", "ANC Off", Icons.Filled.Close)
                    )

                    modes.forEach { (mode, label, icon) ->
                        val isSelected = settings.ancMode == mode
                        val activeThemeColor = when (mode) {
                            "ON" -> AccentPrimary
                            "TRANSPARENCY" -> HighlightSky
                            else -> TextMuted
                        }

                        val bgAnimateColor by animateColorAsState(
                            targetValue = if (isSelected) activeThemeColor else Color.Transparent,
                            animationSpec = tween(durationMillis = 200)
                        )
                        val tintAnimateColor by animateColorAsState(
                            targetValue = if (isSelected) Color.White else TextMuted,
                            animationSpec = tween(durationMillis = 200)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bgAnimateColor)
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    viewModel.setAncMode(mode)
                                    // Disable context-aware auto-switching on manual override
                                    if (adaptiveEnabled) {
                                        viewModel.toggleAdaptiveActivity(false)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(icon, contentDescription = label, tint = tintAnimateColor, modifier = Modifier.size(16.dp))
                                Text(label, color = tintAnimateColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
