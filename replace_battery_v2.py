import sys

filepath = "app/src/main/java/com/example/MainActivity.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

start_marker = "@Composable\nfun VisualBatteryCard("
end_marker = "@Composable\nfun NoiseControlToggle("

start_idx = content.find(start_marker)
end_idx = content.find(end_marker)

if start_idx == -1 or end_idx == -1:
    print(f"Error: Could not find markers in file! Start: {start_idx}, End: {end_idx}")
    sys.exit(1)

replacement = """@Composable
fun VisualBatteryCard(
    batteryLevel: Int,
    connected: Boolean,
    isCharging: Boolean,
    onToggleCharging: (Boolean) -> Unit,
    isSmartSaverActive: Boolean,
    onActivateSmartSaver: () -> Unit,
    onBatteryChange: (Int) -> Unit,
    ancMode: String = "ON",
    ldacEnabled: Boolean = true,
    bassEnabled: Boolean = true
) {
    val animatedBatteryLevel by animateFloatAsState(
        targetValue = batteryLevel.toFloat(),
        animationSpec = tween(durationMillis = 600),
        label = "battery_level_animation"
    )

    // Infinite transition for charging scanline / pulse effect
    val infiniteTransition = rememberInfiniteTransition(label = "battery_charging")
    val chargingOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "charging_offset"
    )

    val pulsingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsing_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkPanel, shape = RoundedCornerShape(12.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        if (!connected) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryUnknown,
                    contentDescription = "Batterij onbekend",
                    tint = TextMuted,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = "Batterijstatus niet beschikbaar",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Verbind je Philips TAH6519 om de resterende accucapaciteit te bekijken.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header of the card
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
                            imageVector = when {
                                isCharging -> Icons.Default.BatteryChargingFull
                                batteryLevel <= 20 -> Icons.Default.BatteryAlert
                                else -> Icons.Default.BatteryFull
                            },
                            contentDescription = "Batterij status",
                            tint = when {
                                isCharging -> AccentPrimary
                                batteryLevel <= 20 -> StatusDanger
                                else -> StatusSuccess
                            },
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    if (isCharging) {
                                        alpha = pulsingAlpha
                                    }
                                }
                        )
                        Column {
                            Text(
                                text = "Accu & Energiebeheer",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Philips Smart Power Management",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isCharging) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Opladen",
                                tint = AccentPrimary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .graphicsLayer { alpha = pulsingAlpha }
                            )
                        }
                        Text(
                            text = "${animatedBatteryLevel.toInt()}%",
                            color = when {
                                isCharging -> AccentPrimary
                                batteryLevel > 50 -> StatusSuccess
                                batteryLevel > 20 -> StatusYellow
                                else -> StatusDanger
                            },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.testTag("battery_large_percentage")
                        )
                    }
                }

                // Visual Battery shell representation with dynamic animations
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Battery Body
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .background(DarkBg, shape = RoundedCornerShape(8.dp))
                            .border(1.dp, DarkBorder, shape = RoundedCornerShape(8.dp))
                            .padding(3.dp)
                    ) {
                        // Battery Fill (using animated float for perfectly smooth transitions)
                        val brush = if (isCharging) {
                            Brush.linearGradient(
                                colors = listOf(AccentPrimary, HighlightSky, AccentPrimary),
                                start = Offset(chargingOffset * 400f - 200f, 0f),
                                end = Offset(chargingOffset * 400f + 200f, 0f)
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = when {
                                    batteryLevel <= 20 -> listOf(StatusDanger, StatusDanger.copy(alpha = 0.7f))
                                    batteryLevel <= 50 -> listOf(StatusYellow, StatusYellow.copy(alpha = 0.7f))
                                    else -> listOf(StatusSuccess, StatusSuccess.copy(alpha = 0.7f))
                                }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedBatteryLevel / 100f)
                                .background(
                                    brush = brush,
                                    shape = RoundedCornerShape(5.dp)
                                )
                        )
                        
                        // Smooth charging/discharging grid overlay for rich UI look
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 1..10) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(14.dp)
                                        .background(
                                            color = if (animatedBatteryLevel >= i * 10) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                                            shape = RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                        }
                    }

                    // Battery Tip
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(14.dp)
                            .background(DarkBorder, shape = RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                    )
                }

                // Dynamic Estimate and consumption tip
                val maxHours = 60f
                val currentUsageFactor = when {
                    ancMode != "OFF" && ldacEnabled -> 0.58f // Both ANC and LDAC active (~35 hours max)
                    ancMode != "OFF" -> 0.75f // Only ANC active (~45 hours max)
                    ldacEnabled -> 0.83f // Only LDAC active (~50 hours max)
                    else -> 1.0f // Pure standard mode (~60 hours max)
                }
                val estHours = if (batteryLevel == 0) 0 else ((batteryLevel / 100f) * maxHours * currentUsageFactor).toInt()

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .border(1.dp, DarkBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
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
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Tijd resterend",
                                tint = HighlightSky,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isCharging) "Tijd tot vol:" else "Resterende luistertijd:",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        
                        Text(
                            text = if (isCharging) {
                                val minsLeft = ((100 - batteryLevel) * 0.9f).toInt()
                                if (minsLeft == 0) "Volledig geladen" else f"~{minsLeft} min (Fast Charge)"
                            } else {
                                f"~{estHours} uur ({if (batteryLevel > 20) "Voldoende" else "Laag, laad op"})"
                            },
                            color = when {
                                isCharging -> AccentPrimary
                                batteryLevel > 20 -> TextPrimary
                                else -> StatusDanger
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = DarkBorder.copy(alpha = 0.3f))

                    // Detail about energy mode
                    Text(
                        text = when {
                            isCharging -> "Hoofdtelefoon laadt momenteel snel op via USB-C."
                            isSmartSaverActive -> "🔋 Smart Power Saving actief: Maximale energiezuinigheid."
                            batteryLevel <= 20 -> "⚠️ Kritiek batterijniveau! Activeer Smart Power Saving om accuduur te sparen."
                            ancMode != "OFF" && ldacEnabled -> "⚡ High Performance: ANC & LDAC verbruiken meer stroom."
                            ancMode != "OFF" -> "🎧 Ruisonderdrukking actief: Matig stroomverbruik."
                            ldacEnabled -> "🎵 Hi-Res LDAC actief: Matig stroomverbruik."
                            else -> "✨ Gebalanceerd: Optimale geluidskwaliteit en energieverbruik."
                        },
                        color = when {
                            isCharging -> AccentPrimary
                            isSmartSaverActive -> StatusSuccess
                            batteryLevel <= 20 -> StatusDanger
                            else -> TextMuted
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Smart Power Cost Analyzer (Active Consumers panel)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .border(1.dp, DarkBorder.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "⚡ STROOMVERBRUIK ANALYSE",
                        color = HighlightSky,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Consumer 1: ANC
                    PowerConsumerRow(
                        label = "Actieve Ruisonderdrukking (ANC)",
                        icon = Icons.Default.GraphicEq,
                        isActive = ancMode != "OFF",
                        drainText = if (ancMode != "OFF") "-15u accuduur" else "+15u bespaard",
                        isPositive = ancMode == "OFF"
                    )

                    // Consumer 2: LDAC
                    PowerConsumerRow(
                        label = "Hi-Res LDAC Codec",
                        icon = Icons.Default.MusicNote,
                        isActive = ldacEnabled,
                        drainText = if (ldacEnabled) "-10u accuduur" else "+10u bespaard",
                        isPositive = !ldacEnabled
                    )

                    // Consumer 3: Bass Boost
                    PowerConsumerRow(
                        label = "Dynamic Bass Boost",
                        icon = Icons.Default.Hearing,
                        isActive = bassEnabled,
                        drainText = if (bassEnabled) "-3u accuduur" else "Zuinig",
                        isPositive = !bassEnabled
                    )
                }

                // Battery Telemetry specs grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(DarkBg.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .border(1.dp, DarkBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("Conditie", color = TextMuted, fontSize = 9.sp)
                            Text("98% (Uitstekend)", color = StatusSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(DarkBg.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .border(1.dp, DarkBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("Temperatuur", color = TextMuted, fontSize = 9.sp)
                            Text("26°C (Optimaal)", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .background(DarkBg.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .border(1.dp, DarkBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("Accu Model", color = TextMuted, fontSize = 9.sp)
                            Text("Li-Poly 750mAh", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Conditional Low Battery Alert component
                if (batteryLevel <= 20 && !isCharging) {
                    LowBatteryAlert(
                        batteryLevel = batteryLevel,
                        isSmartSaverActive = isSmartSaverActive,
                        onActivateSmartSaver = onActivateSmartSaver
                    )
                }

                HorizontalDivider(color = DarkBorder)

                // Interactive Simulator Controls
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Simuleer Batterijniveau (Test)",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Charging Toggle Switch!
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Simuleer Opladen",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                            Switch(
                                checked = isCharging,
                                onCheckedChange = { onToggleCharging(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AccentPrimary,
                                    checkedTrackColor = AccentPrimary.copy(alpha = 0.4f),
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = DarkBg
                                ),
                                modifier = Modifier
                                    .scale(0.7f)
                                    .height(20.dp)
                                    .testTag("charging_simulator_switch")
                            )
                        }
                    }
                    
                    Slider(
                        value = batteryLevel.toFloat(),
                        onValueChange = { onBatteryChange(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = AccentPrimary,
                            inactiveTrackColor = DarkBorder,
                            thumbColor = AccentPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("battery_simulator_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onBatteryChange(20) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_sim_low"),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text("🚨 Low (20%)", fontSize = 10.sp, color = TextPrimary)
                        }
                        Button(
                            onClick = { onBatteryChange(50) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_sim_mid"),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text("⚡ Mid (50%)", fontSize = 10.sp, color = TextPrimary)
                        }
                        Button(
                            onClick = { onBatteryChange(100) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_sim_full"),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text("🔋 Vol (100%)", fontSize = 10.sp, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PowerConsumerRow(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    drainText: String,
    isPositive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1.5f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) AccentPrimary else TextMuted,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                color = if (isActive) TextPrimary else TextMuted,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
            )
        }
        Box(
            modifier = Modifier
                .background(
                    color = if (isPositive) StatusSuccess.copy(alpha = 0.1f) else if (isActive) StatusYellow.copy(alpha = 0.1f) else DarkBg,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = drainText,
                color = if (isPositive) StatusSuccess else if (isActive) StatusYellow else TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

"""

# Note we prepend \\n to end_marker back as it is matched in split
new_content = content[:start_idx] + replacement + "\\n" + content[end_idx:]

with open(filepath, "w", encoding="utf-8") as f:
    f.write(new_content)

print("Replacement Complete!")
