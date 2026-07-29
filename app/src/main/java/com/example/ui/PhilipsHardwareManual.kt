package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NoiseAware
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkPanel
import com.example.ui.theme.HighlightSky
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusOrange
import com.example.ui.theme.StatusPurple
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun PhilipsHardwareManualScreen(
    onClose: () -> Unit,
    viewModel: HeadphoneViewModel? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Knoppen & Gestures", "LED Status", "Fabrieksreset", "Specificaties")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkPanel)
            .padding(top = 8.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        .background(AccentPrimary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Headphones,
                        contentDescription = "Philips TAH6519",
                        tint = AccentPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Philips TAH6519 Handleiding",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Officiele Knopbediening & Specificaties",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("close_manual_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Sluiten",
                    tint = TextMuted
                )
            }
        }

        HorizontalDivider(color = DarkBorder)

        // Navigation Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkPanel,
            contentColor = AccentPrimary,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AccentPrimary,
                        height = 3.dp
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) AccentPrimary else TextMuted
                        )
                    },
                    modifier = Modifier.testTag("manual_tab_$index")
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> HardwareButtonsTab(viewModel)
                1 -> LedStatusDecoderTab()
                2 -> InteractiveFactoryResetTab(viewModel)
                3 -> TechnicalSpecificationsTab()
            }
        }
    }
}

@Composable
private fun HardwareButtonsTab(viewModel: HeadphoneViewModel?) {
    var selectedKeyId by remember { mutableIntStateOf(1) }
    var activeSimulationText by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    val keys = listOf(
        HardwareKeyInfo(
            id = 1,
            name = "Key 1: Power / MFB Key",
            icon = Icons.Filled.PowerSettingsNew,
            subtitle = "Aan/Uit, Muziek & Oproepbeheer",
            actions = listOf(
                KeyAction("Inschakelen", "Inhouding 2 sec", "Witte LED licht 2 sec op, daarna blauw/wit knipperen"),
                KeyAction("Uitschakelen", "Inhouding 4 sec", "Witte LED dooft, uitschakeltoon"),
                KeyAction("Afspelen / Pauze", "1x Klikken", "Schakelt pauze of afspelen in"),
                KeyAction("Oproep Beantwoorden", "1x Klikken (Inkomend)", "Neemt inkomende oproep op"),
                KeyAction("Oproep Weigeren/Beëindigen", "Inhouding 1 sec", "Plaats gesprek op of weigert inkomend"),
                KeyAction("Spraakassistent (Siri/Google)", "Inhouding 1 sec", "Start spraakherkenning op je telefoon")
            )
        ),
        HardwareKeyInfo(
            id = 2,
            name = "Key 2: Volume + Key",
            icon = Icons.Filled.Add,
            subtitle = "Volume verhogen & Volgende Nummer",
            actions = listOf(
                KeyAction("Volume +", "1x Klikken", "Verhoogt het volume met 1 stap"),
                KeyAction("Volgende Nummer", "Inhouding 1 sec", "Springt direct naar de volgende track")
            )
        ),
        HardwareKeyInfo(
            id = 3,
            name = "Key 3: Volume - Key",
            icon = Icons.Filled.Remove,
            subtitle = "Volume verlagen & Vorige Nummer",
            actions = listOf(
                KeyAction("Volume -", "1x Klikken", "Verlaagt het volume met 1 stap"),
                KeyAction("Vorige Nummer", "Inhouding 1 sec", "Keert terug naar de vorige track")
            )
        ),
        HardwareKeyInfo(
            id = 4,
            name = "Key 4: ANC Modus Knop",
            icon = Icons.Filled.NoiseAware,
            subtitle = "Actieve Ruisonderdrukking & Transparantie",
            actions = listOf(
                KeyAction("ANC Modus Schakelen", "1x Klikken", "Wisselt achtereenvolgens: Ruisonderdrukking (ANC) → Normaal → Omgevingsgeluid (Transparantie)")
            )
        ),
        HardwareKeyInfo(
            id = 5,
            name = "Key 5: USB-C Oplaadpoort",
            icon = Icons.Filled.Usb,
            subtitle = "Snel Opladen & Accuvoeding",
            actions = listOf(
                KeyAction("Snel Opladen", "15 minuten laden", "Biedt tot 5 uur afspeeltijd"),
                KeyAction("Volledig Opladen", "2,5 uur op USB-C", "750 mAh Li-ion batterij volledig opgeladen (40h ANC / 80h Normaal)")
            )
        )
    )

    val activeKey = keys.find { it.id == selectedKeyId } ?: keys.first()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Visual Earcup Diagram with interactive key points
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hardware_diagram_card"),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Interactieve Oorschelp Diagram",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Tik op een knop op de oorschelp om de functies te bekijken",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Canvas drawing of headphones earcup with buttons
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(DarkPanel, RoundedCornerShape(12.dp))
                            .border(1.dp, DarkBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f

                            // Headband arc
                            drawArc(
                                color = DarkBorder,
                                startAngle = 180f,
                                sweepAngle = 180f,
                                useCenter = false,
                                topLeft = Offset(cx - 90.dp.toPx(), cy - 70.dp.toPx()),
                                size = androidx.compose.ui.geometry.Size(180.dp.toPx(), 100.dp.toPx()),
                                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Left Earcup
                            drawOval(
                                color = Color(0xFF1E2A3E),
                                topLeft = Offset(cx - 120.dp.toPx(), cy - 25.dp.toPx()),
                                size = androidx.compose.ui.geometry.Size(45.dp.toPx(), 70.dp.toPx())
                            )

                            // Right Earcup (with button panel)
                            drawOval(
                                color = Color(0xFF1E2A3E),
                                topLeft = Offset(cx + 75.dp.toPx(), cy - 25.dp.toPx()),
                                size = androidx.compose.ui.geometry.Size(45.dp.toPx(), 70.dp.toPx())
                            )
                            drawOval(
                                color = AccentPrimary.copy(alpha = 0.4f),
                                topLeft = Offset(cx + 75.dp.toPx(), cy - 25.dp.toPx()),
                                size = androidx.compose.ui.geometry.Size(45.dp.toPx(), 70.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // Hotspots for buttons 1-5
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            keys.forEach { key ->
                                val isSelected = key.id == selectedKeyId
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) AccentPrimary else DarkCard)
                                        .border(
                                            1.dp,
                                            if (isSelected) HighlightSky else DarkBorder,
                                            CircleShape
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            selectedKeyId = key.id
                                        }
                                        .testTag("key_hotspot_${key.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = key.icon,
                                        contentDescription = key.name,
                                        tint = if (isSelected) Color.White else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Key Info Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("active_key_card"),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(AccentPrimary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = activeKey.icon,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = activeKey.name,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = activeKey.subtitle,
                                color = HighlightSky,
                                fontSize = 11.sp
                            )
                        }
                    }

                    HorizontalDivider(color = DarkBorder)

                    // List of Actions
                    activeKey.actions.forEach { action ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkPanel, RoundedCornerShape(10.dp))
                                .border(1.dp, DarkBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    activeSimulationText = "Simulatie uitgevoerd: ${action.title} (${action.gesture})"
                                    if (activeKey.id == 4 && viewModel != null) {
                                        val currentMode = viewModel.settingsState.value.ancMode
                                        val nextMode = when (currentMode) {
                                            "ON" -> "OFF"
                                            "OFF" -> "TRANSPARENCY"
                                            else -> "ON"
                                        }
                                        viewModel.setAncMode(nextMode)
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = action.title,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(AccentPrimary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = action.gesture,
                                            color = AccentPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = action.response,
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    // Active simulation toast feedback
                    AnimatedVisibility(visible = activeSimulationText != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StatusSuccess.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, StatusSuccess.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = activeSimulationText ?: "",
                                    color = StatusSuccess,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LedStatusDecoderTab() {
    var selectedLedIndex by remember { mutableIntStateOf(1) }

    val ledStates = listOf(
        LedStatusInfo(
            title = "Inschakelen & Koppelen",
            subtitle = "Klaar om te verbinden met je telefoon",
            indicatorDesc = "Blauwe en Witte LED knipperen afwisselend",
            ledColor = Color(0xFF00B0FF),
            isBlinking = true,
            isAlternating = true
        ),
        LedStatusInfo(
            title = "Bluetooth Verbonden",
            subtitle = "Actief verbonden en geluid wordt gestreamd",
            indicatorDesc = "LED-indicator is gedoofd (geen storend licht)",
            ledColor = Color.Transparent,
            isBlinking = false,
            isAlternating = false
        ),
        LedStatusInfo(
            title = "Niet Verbonden / Stand-by",
            subtitle = "Ingeschakeld maar zoekt naar bekende apparaten",
            indicatorDesc = "Blauwe en Witte LED knipperen (schakelt na 5 min uit)",
            ledColor = Color.White,
            isBlinking = true,
            isAlternating = true
        ),
        LedStatusInfo(
            title = "Batterij Bijna Leeg (<15%)",
            subtitle = "Spoedig opladen via USB-C vereist",
            indicatorDesc = "Witte LED knippert 1x om de 5 seconden + Gesproken waarschuwing",
            ledColor = Color.White,
            isBlinking = true,
            isAlternating = false
        ),
        LedStatusInfo(
            title = "Aan het Opladen",
            subtitle = "Aangesloten op USB-C lader",
            indicatorDesc = "Witte LED brandt continu tijdens het laden",
            ledColor = Color.White,
            isBlinking = false,
            isAlternating = false
        ),
        LedStatusInfo(
            title = "Volledig Opgeladen",
            subtitle = "100% Acculading bereikt",
            indicatorDesc = "Witte LED gaat uit zodra de accu vol is",
            ledColor = Color.Transparent,
            isBlinking = false,
            isAlternating = false
        )
    )

    val currentLed = ledStates.getOrElse(selectedLedIndex) { ledStates.first() }

    // Animation transition for blinking simulator
    val infiniteTransition = rememberInfiniteTransition(label = "led_anim")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = if (currentLed.isBlinking) 0.1f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (currentLed.isAlternating) 500 else 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink_alpha"
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Live LED Light Visualizer Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("led_visualizer_card"),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, HighlightSky.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "LED Indicator Simulator",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    // Simulated LED Bulb
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(DarkPanel, CircleShape)
                            .border(2.dp, DarkBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentLed.ledColor != Color.Transparent) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (currentLed.isAlternating && blinkAlpha < 0.5f) Color.White else currentLed.ledColor,
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = (6 * blinkAlpha).dp,
                                        color = (if (currentLed.isAlternating && blinkAlpha < 0.5f) Color.White else currentLed.ledColor).copy(alpha = blinkAlpha * 0.5f),
                                        shape = CircleShape
                                    )
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color.DarkGray.copy(alpha = 0.5f), CircleShape)
                            )
                        }
                    }

                    Text(
                        text = currentLed.title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = currentLed.indicatorDesc,
                        color = HighlightSky,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // List of LED States
        items(ledStates.size) { index ->
            val state = ledStates[index]
            val isSelected = index == selectedLedIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSelected) DarkCard else DarkPanel, RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        if (isSelected) AccentPrimary else DarkBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { selectedLedIndex = index }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.title,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = state.subtitle,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = if (state.ledColor == Color.Transparent) Color.DarkGray else state.ledColor,
                                shape = CircleShape
                            )
                            .border(1.dp, DarkBorder, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractiveFactoryResetTab(viewModel: HeadphoneViewModel?) {
    var holdSeconds by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }
    var isResetComplete by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isHolding) {
        if (isHolding) {
            isResetComplete = false
            while (holdSeconds < 4.0f && isHolding) {
                delay(100)
                holdSeconds += 0.1f
            }
            if (holdSeconds >= 4.0f) {
                isResetComplete = true
                isHolding = false
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                viewModel?.resetAll()
            }
        } else {
            if (!isResetComplete) holdSeconds = 0f
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("factory_reset_card"),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, StatusDanger.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(StatusDanger.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = StatusDanger,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Officiele Fabrieksreset Procedure",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Herstelt alle audio- & Bluetooth koppelingen",
                                color = StatusDanger,
                                fontSize = 11.sp
                            )
                        }
                    }

                    HorizontalDivider(color = DarkBorder)

                    // Step by step list
                    ResetStepItem("Stap 1", "Verwijder 'Philips TAH6519' uit de Bluetooth-lijst van al je apparaten.")
                    ResetStepItem("Stap 2", "Schakel de Bluetooth op je telefoon tijdelijk uit.")
                    ResetStepItem("Stap 3", "Houd op de koptelefoon de Volume + (Key 2) én Volume - (Key 3) knoppen tegelijkertijd ingedrukt gedurende 4 seconden.")
                    ResetStepItem("Stap 4", "De LED knippert blauw/wit en alle koppelinformatie is gewist. Je kunt nu opnieuw koppelen.")

                    Spacer(modifier = Modifier.height(8.dp))

                    // Interactive Simulator Button
                    Text(
                        text = "Interactieve Hardware Simulator:",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isResetComplete) StatusSuccess.copy(alpha = 0.2f) else StatusDanger.copy(alpha = 0.15f))
                            .border(
                                1.dp,
                                if (isResetComplete) StatusSuccess else StatusDanger,
                                RoundedCornerShape(12.dp)
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isHolding = true
                                        tryAwaitRelease()
                                        isHolding = false
                                    }
                                )
                            }
                            .testTag("reset_simulator_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (isResetComplete) {
                                Text(
                                    text = "✓ Fabrieksreset Voltooid!",
                                    color = StatusSuccess,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            } else {
                                Text(
                                    text = if (isHolding) "HOUD VAST... (${String.format("%.1f", 4.0f - holdSeconds)}s)" else "HOU [VOL +] EN [VOL -] 4 SEC INGEDRUKT",
                                    color = StatusDanger,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                LinearProgressIndicator(
                                    progress = { holdSeconds / 4.0f },
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .padding(top = 6.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = StatusDanger,
                                    trackColor = DarkBorder
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResetStepItem(step: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .background(AccentPrimary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = step,
                color = AccentPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
        Text(
            text = desc,
            color = TextPrimary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun TechnicalSpecificationsTab() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tech_specs_card"),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Gedetailleerde Technische Gegevens",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    HorizontalDivider(color = DarkBorder)

                    SpecRow("Modelaanduiding", "Philips TAH6519 (6000 Serie)")
                    SpecRow("Acoustic Driver", "40 mm Neodymium High-Performance")
                    SpecRow("Frequentiebereik", "20 Hz – 20.000 Hz")
                    SpecRow("Bluetooth Versie", "Bluetooth 5.4 LE (HFP, A2DP, AVRCP)")
                    SpecRow("Ondersteunde Codecs", "SBC / AAC High Density")
                    SpecRow("Accu Capaciteit", "750 mAh Oplaadbare Li-ion")
                    SpecRow("Speeltijd (ANC Aan)", "Ca. 40 uur")
                    SpecRow("Speeltijd (ANC Uit)", "Ca. 80 uur")
                    SpecRow("Oplaadtijd", "2,5 uur via USB-C (15 min = 5u spelen)")
                    SpecRow("Zendvermogen & Bereik", "<10 dBm | Maximaal 10 meter")
                    SpecRow("Gewicht", "235 gram (Over-ear ergonomie)")
                    SpecRow("Bedrijfstemperatuur", "0°C tot 45°C (Opslag: -20°C tot 50°C)")
                }
            }
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp)
        Text(text = value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private data class HardwareKeyInfo(
    val id: Int,
    val name: String,
    val icon: ImageVector,
    val subtitle: String,
    val actions: List<KeyAction>
)

private data class KeyAction(
    val title: String,
    val gesture: String,
    val response: String
)

private data class LedStatusInfo(
    val title: String,
    val subtitle: String,
    val indicatorDesc: String,
    val ledColor: Color,
    val isBlinking: Boolean,
    val isAlternating: Boolean
)
