package com.example

import com.example.ui.FullScreenMediaDashboard


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.data.AppDatabase
import com.example.data.HeadphoneRepository
import com.example.data.HeadphoneSettings
import com.example.ui.HeadphoneViewModel
import com.example.ui.HeadphoneViewModelFactory
import com.example.ui.UpdateState
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Room database and repository setup
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = HeadphoneRepository(database.headphoneDao())
        val viewModel: HeadphoneViewModel by viewModels { HeadphoneViewModelFactory(application, repository) }

        setContent {
            MyApplicationTheme {
                HeadphoneApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeadphoneApp(viewModel: HeadphoneViewModel) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.shouldCloseApp.collect { shouldClose ->
            if (shouldClose) {
                (context as? android.app.Activity)?.finish()
            }
        }
    }

    val settings by viewModel.settingsState.collectAsStateWithLifecycle()

    // Smoothly auto-transition app theme based on Active Noise Canceling (ANC) mode changes
    LaunchedEffect(settings.ancMode, settings.connected) {
        if (settings.connected) {
            val currentTheme = ThemeState.activeTheme
            if (currentTheme != AppTheme.HIGH_CONTRAST) {
                when (settings.ancMode) {
                    "ON" -> {
                        if (currentTheme != AppTheme.NORDIC_FROST) {
                            ThemeState.activeTheme = AppTheme.NORDIC_FROST
                        }
                    }
                    "OFF" -> {
                        if (currentTheme != AppTheme.PHILIPS_STUDIO) {
                            ThemeState.activeTheme = AppTheme.PHILIPS_STUDIO
                        }
                    }
                    "TRANSPARENCY" -> {
                        if (currentTheme != AppTheme.CYBERPUNK_NEON) {
                            ThemeState.activeTheme = AppTheme.CYBERPUNK_NEON
                        }
                    }
                }
            }
        }
    }
    val isConnecting by viewModel.isConnecting.collectAsStateWithLifecycle()
    val isAutoReconnecting by viewModel.isAutoReconnecting.collectAsStateWithLifecycle()
    val isCharging by viewModel.isCharging.collectAsStateWithLifecycle()
    val firmwareVersion by viewModel.firmwareVersion.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val autoOffRemainingSeconds by viewModel.autoOffRemainingSeconds.collectAsStateWithLifecycle()
    val autoOffIsInactive by viewModel.autoOffIsInactive.collectAsStateWithLifecycle()
    var activeTab by remember { mutableStateOf("dash") }
    var eqBandMode by remember { mutableStateOf("3-BAND") }
    var showPairingGuide by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var hasPromptedForUpdate by remember { mutableStateOf(false) }

    // Automatically check for available firmware updates when the headphone is connected
    LaunchedEffect(settings.connected) {
        if (settings.connected) {
            if (firmwareVersion == "v1.4.2" && updateState is UpdateState.Idle) {
                viewModel.checkForUpdates()
            }
        } else {
            hasPromptedForUpdate = false
        }
    }

    // Display a gorgeous prompt if an update is required
    val currentUpdateState = updateState
    if (currentUpdateState is UpdateState.UpdateAvailable && !hasPromptedForUpdate) {
        FirmwareUpdatePromptDialog(
            version = currentUpdateState.version,
            changelog = currentUpdateState.changelog,
            onDismiss = {
                hasPromptedForUpdate = true
            },
            onInstall = {
                hasPromptedForUpdate = true
                activeTab = "device"
                viewModel.startUpdate()
            }
        )
    }

    var lastWarnedBatteryLevel by remember { mutableStateOf<Int?>(null) }
    var lastConnectedState by remember { mutableStateOf(false) }
    var activeBatteryToast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(settings.batteryLevel, settings.connected, isCharging) {
        val connectedChanged = settings.connected != lastConnectedState
        if (settings.connected && !isCharging) {
            val wasAboveOrNull = lastWarnedBatteryLevel == null || lastWarnedBatteryLevel!! >= 20
            if (settings.batteryLevel < 20 && (wasAboveOrNull || connectedChanged)) {
                activeBatteryToast = "Waarschuwing: Accuniveau is kritiek laag (${settings.batteryLevel}%). Sluit je Philips TAH6519 aan op de oplader."
                viewModel.playProceduralTone()
            }
        }
        if (!settings.connected || isCharging || settings.batteryLevel >= 20) {
            lastWarnedBatteryLevel = null
        } else {
            lastWarnedBatteryLevel = settings.batteryLevel
        }
        lastConnectedState = settings.connected
    }

    LaunchedEffect(activeBatteryToast) {
        if (activeBatteryToast != null) {
            delay(8000)
            activeBatteryToast = null
        }
    }

    if (showPairingGuide) {
        PairingGuideDialog(onDismiss = { showPairingGuide = false })
    }

    if (showSettings) {
        SettingsDialog(viewModel = viewModel, onDismiss = { showSettings = false })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = DarkBg,
        topBar = {
            Column(
                modifier = Modifier
                    .background(DarkPanel)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo and Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(36.dp)
                        ) {
                            if (settings.connected) {
                                val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
                                val auraScale by infiniteTransition.animateFloat(
                                    initialValue = 0.8f,
                                    targetValue = 1.35f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(2000, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "aura_scale"
                                )
                                val auraAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.5f,
                                    targetValue = 0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(2000, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "aura_alpha"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .graphicsLayer {
                                            scaleX = auraScale
                                            scaleY = auraScale
                                            alpha = auraAlpha
                                        }
                                        .border(2.dp, AccentPrimary, shape = RoundedCornerShape(10.dp))
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(AccentPrimary, shape = RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = "Philips TAH6519 Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Philips TAH6519",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                )
                                if (settings.connected) {
                                    ActiveCommunicationDot()
                                } else if (isConnecting || isAutoReconnecting) {
                                    PairingStatusDot()
                                } else {
                                    DisconnectedStatusDot()
                                }
                            }
                            Text(
                                text = "Sound Enhancement · ANC · LDAC",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Status badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadge(label = "LDAC", active = settings.ldacEnabled)
                        StatusBadge(
                            label = when (settings.ancMode) {
                                "ON" -> "ANC: On"
                                "TRANSPARENCY" -> "Ambient"
                                else -> "ANC: Off"
                            },
                            active = settings.connected && settings.ancMode != "OFF"
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // Battery display if connected
                        if (settings.connected) {
                            MiniBatteryIndicator(
                                batteryLevel = settings.batteryLevel,
                                isCharging = isCharging
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(StatusDanger, shape = CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = {
                                val isHC = !ThemeState.isLightMode && ThemeState.activeTheme == com.example.ui.theme.AppTheme.HIGH_CONTRAST
                                if (isHC) {
                                    ThemeState.isLightMode = true
                                    ThemeState.activeTheme = com.example.ui.theme.AppTheme.PHILIPS_STUDIO
                                } else {
                                    ThemeState.isLightMode = false
                                    ThemeState.activeTheme = com.example.ui.theme.AppTheme.HIGH_CONTRAST
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("theme_toggle_button")
                        ) {
                            val isHC = !ThemeState.isLightMode && ThemeState.activeTheme == com.example.ui.theme.AppTheme.HIGH_CONTRAST
                            Icon(
                                imageVector = if (isHC) Icons.Default.WbSunny else Icons.Default.NightsStay,
                                contentDescription = "Thema omschakelen",
                                tint = if (isHC) AccentPrimary else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Instellingen",
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Connection status banner
                AnimatedVisibility(
                    visible = true,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (settings.connected) StatusSuccess.copy(alpha = 0.08f) else StatusDanger.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (settings.connected) StatusSuccess.copy(alpha = 0.3f) else StatusDanger.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (settings.connected) StatusSuccess else StatusDanger,
                                            shape = CircleShape
                                        )
                                )
                                Text(
                                    text = if (settings.connected) "Bluetooth-verbinding actief · Philips TAH6519" else "Geen actieve Bluetooth-verbinding",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            if (!settings.connected) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "HOE KOPPELEN?",
                                        color = HighlightSky,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable { showPairingGuide = true }
                                            .padding(4.dp)
                                            .testTag("banner_how_to_pair_button")
                                    )
                                    Text(
                                        text = "VERBINDEN",
                                        color = AccentPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable { viewModel.connectDevice() }
                                            .padding(4.dp)
                                            .testTag("banner_connect_button")
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Connected",
                                        tint = StatusSuccess,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Actief",
                                        color = StatusSuccess,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Battery warning notification (under 15%)
                val hasHighPowerActive = settings.ancMode != "OFF" || settings.ldacEnabled || settings.dynamicBassEnabled || settings.surroundSoundEnabled
                val showBatteryWarning = settings.connected && settings.batteryLevel <= 15 && !isCharging

                AnimatedVisibility(
                    visible = showBatteryWarning,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StatusDanger.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp))
                            .border(1.dp, StatusDanger.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("battery_warning_notification_banner")
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BatteryAlert,
                                        contentDescription = "Battery Alert",
                                        tint = StatusDanger,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Kritieke batterij: ${settings.batteryLevel}%",
                                        color = StatusDanger,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                if (hasHighPowerActive) {
                                    Button(
                                        onClick = {
                                            viewModel.toggleAnc(false)
                                            viewModel.toggleLdac(false)
                                            viewModel.toggleDynamicBass(false)
                                            viewModel.toggleSurround(false)
                                            viewModel.setAutoPowerOffMinutes(5)
                                            android.widget.Toast.makeText(
                                                context,
                                                "Energiebesparende modus actief: high-power functies uitgeschakeld!",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = StatusDanger,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .height(28.dp)
                                            .testTag("btn_battery_warning_save_energy")
                                    ) {
                                        Text(
                                            text = "Bespaar Energie",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Safe",
                                            tint = StatusSuccess,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Energiebesparing actief",
                                            color = StatusSuccess,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            
                            if (hasHighPowerActive) {
                                Text(
                                    text = "Schakel high-power functies (ANC, LDAC, Dynamic Bass, Surround) uit om direct de accuduur van je TAH6519 te verlengen.",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            } else {
                                Text(
                                    text = "Alle zware functies zijn uitgeschakeld. Sluit zo snel mogelijk een USB-C lader aan om uitval te voorkomen.",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkPanel,
                contentColor = TextPrimary,
                tonalElevation = 16.dp
            ) {
                val tabs = listOf(
                    Triple("dash", "Thuis", Icons.Default.Dashboard),
                    Triple("media", "Media", Icons.Default.MusicNote),
                    Triple("audio", "Audio", Icons.Default.GraphicEq),
                    Triple("device", "Systeem", Icons.Default.Settings)
                )
                tabs.forEach { (tabId, label, icon) ->
                    NavigationBarItem(
                        selected = activeTab == tabId,
                        onClick = { activeTab = tabId },
                        icon = { Icon(imageVector = icon, contentDescription = label) },
                        label = { Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkBg,
                            selectedTextColor = AccentPrimary,
                            indicatorColor = AccentPrimary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .drawBehind {
                    drawRect(color = DarkBg)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(AccentPrimary.copy(alpha = if (ThemeState.isLightMode) 0.08f else 0.12f), Color.Transparent),
                            center = Offset(size.width * 0.8f, size.height * 0.15f),
                            radius = size.width * 0.9f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(HighlightSky.copy(alpha = if (ThemeState.isLightMode) 0.05f else 0.08f), Color.Transparent),
                            center = Offset(size.width * 0.2f, size.height * 0.75f),
                            radius = size.width * 0.8f
                        )
                    )
                }
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(250, delayMillis = 50)) + scaleIn(initialScale = 0.95f, animationSpec = tween(250, delayMillis = 50)))
                        .togetherWith(fadeOut(animationSpec = tween(120)))
                },
                label = "tab_content_animation",
                modifier = Modifier.fillMaxSize()
            ) { targetTab ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                ) {
                    // Main reactive tab contents
                    when (targetTab) {
                        "dash" -> {
                            item {
                                DashboardHeroCard(settings, isCharging)
                            }
                            if (settings.connected) {
                                item {
                                    ActiveDeviceLinkPulseCard(viewModel, settings)
                                }
                            }
                            item {
                                BluetoothStatusIndicatorCard(viewModel, settings)
                            }
                            item {
                                TechnicalConnectionStatsCard(viewModel, settings)
                            }
                            item {
                                val isSmartSaverActive = !settings.ancEnabled && !settings.ldacEnabled && settings.autoPowerOffMinutes == 5
                                val isFetchingBattery by viewModel.isFetchingBattery.collectAsStateWithLifecycle()
                                val batteryFetchProgress by viewModel.batteryFetchProgress.collectAsStateWithLifecycle()
                                val batteryFetchStatus by viewModel.batteryFetchStatus.collectAsStateWithLifecycle()
                                VisualBatteryCard(
                                    batteryLevel = settings.batteryLevel,
                                    connected = settings.connected,
                                    isCharging = isCharging,
                                    onToggleCharging = { viewModel.toggleCharging(it) },
                                    isSmartSaverActive = isSmartSaverActive,
                                    onActivateSmartSaver = {
                                        viewModel.toggleAnc(false)
                                        viewModel.toggleLdac(false)
                                        viewModel.setAutoPowerOffMinutes(5)
                                    },
                                    onBatteryChange = { viewModel.updateBatteryLevel(it) },
                                    ancMode = settings.ancMode,
                                    ldacEnabled = settings.ldacEnabled,
                                    bassEnabled = settings.dynamicBassEnabled,
                                    batteryHealthEnabled = settings.batteryHealthEnabled,
                                    isFetchingBattery = isFetchingBattery,
                                    batteryFetchProgress = batteryFetchProgress,
                                    batteryFetchStatus = batteryFetchStatus,
                                    onFetchBattery = { viewModel.fetchBatteryLevel() }
                                )
                            }
                            item {
                                DashboardMediaWidget(viewModel, settings)
                            }
                            item {
                                DashboardQuickControls(viewModel, settings)
                            }
                            item {
                                DashboardSmartZonesCard(viewModel, settings)
                            }
                            item {
                                DashboardSoundSafetyMeter(viewModel)
                            }
                            item {
                                DashboardStatsTracker()
                            }
                            item {
                                DashboardLocatorCard(viewModel)
                            }
                            item {
                                com.example.ui.FullScreenAmbientVisualizer(viewModel, settings)
                            }
                        }
                        "media" -> {
                            item {
                                FullScreenMediaDashboard(viewModel, settings)
                            }
                        }
                        "audio" -> {
                        item {
                            FrequencyResponseGraph(bands = settings.getBands())
                        }
                        item {
                            SectionHeader(title = "Presets")
                            PresetsGrid(
                                activePreset = settings.activePreset,
                                presets = viewModel.presets,
                                onPresetSelected = { viewModel.setPreset(it) }
                            )
                        }

                        val customPresets = settings.getCustomPresetsMap()
                        if (customPresets.isNotEmpty()) {
                            item {
                                SectionHeader(title = "Mijn Presets")
                                CustomPresetsGrid(
                                    activePreset = settings.activePreset,
                                    customPresets = customPresets,
                                    onPresetSelected = { viewModel.setPreset(it) },
                                    onDeletePreset = { viewModel.deleteCustomPreset(it) },
                                    onRenamePreset = { old, new -> viewModel.renameCustomPreset(old, new) }
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionHeader(
                                    title = when (eqBandMode) {
                                        "10-BAND" -> "Professionele EQ (10-Band) · 12 dB"
                                        "5-BAND" -> "Handmatige EQ (5-Band) · 12 dB"
                                        else -> "Snelkoppeling EQ (Bass - Mids - Treble)"
                                    }
                                )
                                Row(
                                    modifier = Modifier
                                        .background(DarkPanel, shape = RoundedCornerShape(12.dp))
                                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                                        .padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf("3-BAND" to "B-M-T", "5-BAND" to "5-Band", "10-BAND" to "10-Band").forEach { (mode, label) ->
                                        val selected = eqBandMode == mode
                                        val bgAnimate by animateColorAsState(
                                            targetValue = if (selected) AccentPrimary else Color.Transparent,
                                            animationSpec = tween(durationMillis = 250),
                                            label = "eq_toggle_bg"
                                        )
                                        val textAnimate by animateColorAsState(
                                            targetValue = if (selected) Color.White else TextMuted,
                                            animationSpec = tween(durationMillis = 250),
                                            label = "eq_toggle_text"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(bgAnimate)
                                                .clickable { eqBandMode = mode }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                .testTag("eq_mode_${mode.lowercase().replace("-", "_")}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = textAnimate,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
                                    .background(DarkPanel, shape = RoundedCornerShape(12.dp))
                                    .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 16.dp)
                             ) {
                                if (eqBandMode == "3-BAND") {
                                    val bandLabels = listOf("Lage Tonen (Bass)", "Midden Tonen (Mids)", "Hoge Tonen (Treble)")
                                    val bandColors = listOf(Color(0xFF0066FF), Color(0xFF00F5FF), Color(0xFFFFCC00))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        val currentBands = settings.getBands()
                                        // Bass average (60Hz, 125Hz, 250Hz)
                                        val avgBass = (currentBands[0] + currentBands[1] + currentBands[2]) / 3f
                                        // Mids average (500Hz, 1kHz, 2kHz, 4kHz)
                                        val avgMids = (currentBands[3] + currentBands[4] + currentBands[5] + currentBands[6]) / 4f
                                        // Treble average (8kHz, 12kHz, 16kHz)
                                        val avgTreble = (currentBands[7] + currentBands[8] + currentBands[9]) / 3f
                                        
                                        val values = listOf(avgBass, avgMids, avgTreble)
                                        
                                        for (i in 0 until 3) {
                                            VerticalEqSlider(
                                                value = values[i],
                                                onValueChange = { newVal ->
                                                    if (i == 0) {
                                                        // Update Bass
                                                        viewModel.updateBand(0, newVal)
                                                        viewModel.updateBand(1, newVal)
                                                        viewModel.updateBand(2, newVal)
                                                    } else if (i == 1) {
                                                        // Update Mids
                                                        viewModel.updateBand(3, newVal)
                                                        viewModel.updateBand(4, newVal)
                                                        viewModel.updateBand(5, newVal)
                                                        viewModel.updateBand(6, newVal)
                                                    } else {
                                                        // Update Treble
                                                        viewModel.updateBand(7, newVal)
                                                        viewModel.updateBand(8, newVal)
                                                        viewModel.updateBand(9, newVal)
                                                    }
                                                },
                                                label = bandLabels[i],
                                                color = bandColors[i],
                                                modifier = Modifier.weight(1f),
                                                trackWidth = 28.dp
                                            )
                                        }
                                    }
                                } else if (eqBandMode == "5-BAND") {
                                    val bandLabels = listOf("Bass", "Lo-Mid", "Mid", "Hi-Mid", "Treble")
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        val currentBands = settings.getBands()
                                        for (i in 0 until 5) {
                                            val avgGain = (currentBands[i * 2] + currentBands[i * 2 + 1]) / 2f
                                            VerticalEqSlider(
                                                value = avgGain,
                                                onValueChange = { newVal ->
                                                    viewModel.updateBand(i * 2, newVal)
                                                    viewModel.updateBand(i * 2 + 1, newVal)
                                                },
                                                label = bandLabels[i],
                                                color = EQBandColors[i * 2],
                                                modifier = Modifier.weight(1f),
                                                trackWidth = 20.dp
                                            )
                                        }
                                    }
                                } else {
                                    val bandLabels = listOf("60Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "12kHz", "16kHz")
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        val currentBands = settings.getBands()
                                        for (i in 0 until 10) {
                                            VerticalEqSlider(
                                                value = currentBands[i],
                                                onValueChange = { newVal ->
                                                    viewModel.updateBand(i, newVal)
                                                },
                                                label = bandLabels[i],
                                                color = EQBandColors[i],
                                                modifier = Modifier.weight(1f),
                                                trackWidth = 12.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkPanel, shape = RoundedCornerShape(12.dp))
                                    .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                var newPresetName by remember { mutableStateOf("") }
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Huidige instellingen opslaan",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "Als eigen preset",
                                            color = HighlightSky,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = newPresetName,
                                            onValueChange = { newPresetName = it },
                                            placeholder = { Text("Bijv. Mijn Super Bass", color = TextMuted, fontSize = 11.sp) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedBorderColor = AccentPrimary,
                                                unfocusedBorderColor = DarkBorder,
                                                cursorColor = AccentPrimary
                                            ),
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp)
                                                .testTag("save_preset_input")
                                        )
                                        Button(
                                            onClick = {
                                                if (newPresetName.isNotBlank()) {
                                                    viewModel.saveCustomPreset(newPresetName, settings.getBands())
                                                    newPresetName = ""
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                                            shape = RoundedCornerShape(8.dp),
                                            enabled = newPresetName.isNotBlank(),
                                            modifier = Modifier
                                                .height(44.dp)
                                                .testTag("save_preset_button")
                                        ) {
                                            Text(
                                                text = "Opslaan",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            SectionHeader(title = "Master Gain")
                            MasterGainSlider(
                                gain = settings.masterGain,
                                onGainChange = { viewModel.updateMasterGain(it) }
                            )
                        }
                        item {
                            Button(
                                onClick = { viewModel.setPreset("Flat") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = TextMuted
                                ),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, DarkBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reset_to_flat_button")
                            ) {
                                Text(
                                    text = "Reset naar Flat",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        item {
                            SectionHeader(title = "Actieve Ruisonderdrukking")
                            NoiseControlToggle(
                                activeMode = settings.ancMode,
                                onModeChange = { viewModel.setAncMode(it) }
                            )
                        }

                        if (settings.ancMode == "ON") {
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "ANC Niveau",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val levels = listOf(
                                        AncLevelData(1, "Light Focus", "Vermindert laagfrequent gebrom. Ideaal voor kantoor."),
                                        AncLevelData(2, "Adaptief", "Past zich automatisch aan. Het beste voor reizen."),
                                        AncLevelData(3, "Deep Silence", "Maximale 56 dB reductie. Voor lawaaierige omgevingen.")
                                    )
                                    levels.forEach { level ->
                                        val isSelected = settings.ancLevel == level.level
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (isSelected) AccentPrimary.copy(alpha = 0.1f) else DarkCard,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) AccentPrimary else DarkBorder,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable { viewModel.setAncLevel(level.level) }
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = level.name,
                                                    color = if (isSelected) HighlightSky else TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = level.desc,
                                                    color = TextMuted,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(top = 3.dp)
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .border(
                                                        2.dp,
                                                        if (isSelected) AccentPrimary else TextMuted,
                                                        shape = CircleShape
                                                    )
                                                    .padding(3.dp)
                                            ) {
                                                if (isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(AccentPrimary, shape = CircleShape)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (settings.ancLevel == 3) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(AccentPrimary.copy(alpha = 0.08f), shape = RoundedCornerShape(10.dp))
                                            .border(1.dp, AccentPrimary.copy(alpha = 0.2f), shape = RoundedCornerShape(10.dp))
                                            .padding(14.dp)
                                    ) {
                                        Text(
                                            text = "💡 Op niveau 3 (Deep Silence): probeer de \"ANC Compensatie\" EQ-preset om de bassrespons volledig te herstellen.",
                                            color = HighlightSky,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }
                        } else if (settings.ancMode == "TRANSPARENCY") {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkCard, shape = RoundedCornerShape(12.dp))
                                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                                        .padding(16.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Hearing,
                                                contentDescription = "Transparency",
                                                tint = HighlightSky,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Aura Sound Transparency DSP",
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Text(
                                            text = "Transparency-modus vangt omgevingsgeluid op via externe microfoons en mengt dit in de audiostroom, zodat je verbonden blijft met je omgeving.",
                                            color = TextMuted,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp
                                        )
                                        HorizontalDivider(color = DarkBorder)
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Omgevingsvolume Versterking",
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Default (+3 dB)",
                                                color = AccentPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        
                                        var transparencyGain by remember { mutableStateOf(50f) }
                                        Slider(
                                            value = transparencyGain,
                                            onValueChange = { transparencyGain = it },
                                            valueRange = 0f..100f,
                                            colors = SliderDefaults.colors(
                                                activeTrackColor = HighlightSky,
                                                inactiveTrackColor = DarkBorder,
                                                thumbColor = HighlightSky
                                            ),
                                            modifier = Modifier.fillMaxWidth().testTag("transparency_gain_slider")
                                        )
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            var activeFocus by remember { mutableStateOf("Alle") }
                                            listOf("Alle", "Stemmen Focus", "Veiligheid").forEach { focus ->
                                                val isFocusSelected = activeFocus == focus
                                                Button(
                                                    onClick = { activeFocus = focus },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isFocusSelected) HighlightSky.copy(alpha = 0.15f) else DarkBg,
                                                        contentColor = if (isFocusSelected) HighlightSky else TextMuted
                                                    ),
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (isFocusSelected) HighlightSky else DarkBorder
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f).testTag("trans_focus_$focus"),
                                                    contentPadding = PaddingValues(vertical = 4.dp)
                                                ) {
                                                    Text(focus, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkCard, shape = RoundedCornerShape(10.dp))
                                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(10.dp))
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = "ANC is uitgeschakeld. Het gesloten over-ear ontwerp van de TAH6519 biedt nog steeds circa 20 dB passieve geluidsisolatie.",
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                        item {
                            SectionHeader(title = "Geluidsverbeteringen")
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ToggleRow(
                                    label = "Dynamic Bass",
                                    sub = "Versterkt de bas op lager volume – exclusieve Philips DSP-technologie",
                                    checked = settings.dynamicBassEnabled,
                                    onCheckedChange = { viewModel.toggleDynamicBass(it) },
                                    activeColor = StatusDanger
                                )
                                ToggleRow(
                                    label = "Surround Sound",
                                    sub = "Creëert een meeslepend, ruimtelijk 3D audiobeeld voor films en games",
                                    checked = settings.surroundSoundEnabled,
                                    onCheckedChange = { viewModel.toggleSurround(it) },
                                    activeColor = StatusPurple
                                )
                                
                                AnimatedVisibility(
                                    visible = settings.surroundSoundEnabled,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .background(DarkCard, shape = RoundedCornerShape(12.dp))
                                            .border(1.dp, StatusPurple.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        SpatialAudioVisualizer()
                                    }
                                }
                                
                                ToggleRow(
                                    label = "LDAC Hi-Res Audio",
                                    sub = "Verzendt tot 3x meer audiodata dan standaard Bluetooth SBC (tot 990 kbps)",
                                    checked = settings.ldacEnabled,
                                    onCheckedChange = { viewModel.toggleLdac(it) },
                                    activeColor = StatusSuccess
                                )
                                ToggleRow(
                                    label = "Sidetone (Hoor Jezelf)",
                                    sub = "Laat je je eigen stem natuurlijk horen via de microfoons tijdens oproepen",
                                    checked = settings.sidetoneEnabled,
                                    onCheckedChange = { viewModel.toggleSidetone(it) },
                                    activeColor = StatusYellow
                                )
                            }
                        }
                        if (settings.sidetoneEnabled) {
                            item {
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("sidetone_level_card"),
                                    colors = CardDefaults.cardColors(containerColor = DarkPanel),
                                    border = BorderStroke(1.dp, DarkBorder),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Sidetone Volume",
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${settings.sidetoneLevel}%",
                                                color = HighlightSky,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Slider(
                                            value = settings.sidetoneLevel.toFloat(),
                                            onValueChange = { viewModel.setSidetoneLevel(it.toInt()) },
                                            valueRange = 0f..100f,
                                            colors = SliderDefaults.colors(
                                                activeTrackColor = HighlightSky,
                                                inactiveTrackColor = DarkBorder,
                                                thumbColor = HighlightSky
                                            ),
                                            modifier = Modifier.fillMaxWidth().testTag("sidetone_level_slider")
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            SectionHeader(title = "Zen Soundscapes")
                            ZenSoundscapesCard()
                        }
                        item {
                            SectionHeader(title = "Persoonlijk Gehoor-ID")
                            HearingTestCard(viewModel, settings)
                        }
                        item {
                            SectionHeader(title = "Gehoorbescherming")
                            HearingHealthCard()
                        }
                        item {
                            SectionHeader(title = "Geluidsprofiel Tips")
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val tips = listOf(
                                    TipData("🎵", "Muziek", StatusDanger, "Zet Dynamic Bass + LDAC aan. Gebruik de \"Philips Signature\" of \"Hi-Res LDAC\" preset."),
                                    TipData("🎬", "Films", StatusPurple, "Zet Surround Sound aan + de \"Cinema Surround\" preset voor intens filmgeluid."),
                                    TipData("🎮", "Gaming", StatusSuccess, "Zet LDAC uit voor lagere latency. Activeer de \"Gaming\" preset en zet ANC uit."),
                                    TipData("📞", "Bellen", StatusYellow, "Gebruik de \"Podcast/Stem\" EQ-preset. De geïntegreerde 5-microfoon array filtert ruis.")
                                )
                                tips.forEach { tip ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(DarkCard, shape = RoundedCornerShape(0.dp, 10.dp, 10.dp, 0.dp))
                                            .drawBehind {
                                                drawLine(
                                                    color = tip.color,
                                                    start = Offset(0f, 0f),
                                                    end = Offset(0f, size.height),
                                                    strokeWidth = 3.dp.toPx()
                                                )
                                            }
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(text = tip.icon, fontSize = 20.sp)
                                        Column {
                                            Text(
                                                text = tip.title,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = tip.text,
                                                color = TextMuted,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "device" -> {
                        // Hero Image at top
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_headphones_hero_1783196951412),
                                    contentDescription = "Philips TAH6519 Premium Headphone",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                            )
                                        )
                                )
                                Text(
                                    text = "Philips TAH6519 Wireless Over-Ear",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(12.dp)
                                )
                            }
                        }

                        // Battery & Power Control Card
                        item {
                            val isSmartSaverActive = !settings.ancEnabled && !settings.ldacEnabled && settings.autoPowerOffMinutes == 5
                            val isFetchingBattery by viewModel.isFetchingBattery.collectAsStateWithLifecycle()
                            val batteryFetchProgress by viewModel.batteryFetchProgress.collectAsStateWithLifecycle()
                            val batteryFetchStatus by viewModel.batteryFetchStatus.collectAsStateWithLifecycle()
                            val isCharging by viewModel.isCharging.collectAsStateWithLifecycle()
                            VisualBatteryCard(
                                batteryLevel = settings.batteryLevel,
                                connected = settings.connected,
                                isCharging = isCharging,
                                onToggleCharging = { viewModel.toggleCharging(it) },
                                isSmartSaverActive = isSmartSaverActive,
                                onActivateSmartSaver = {
                                    viewModel.toggleAnc(false)
                                    viewModel.toggleLdac(false)
                                    viewModel.setAutoPowerOffMinutes(5)
                                },
                                onBatteryChange = { viewModel.updateBatteryLevel(it) },
                                ancMode = settings.ancMode,
                                ldacEnabled = settings.ldacEnabled,
                                bassEnabled = settings.dynamicBassEnabled,
                                batteryHealthEnabled = settings.batteryHealthEnabled,
                                isFetchingBattery = isFetchingBattery,
                                batteryFetchProgress = batteryFetchProgress,
                                batteryFetchStatus = batteryFetchStatus,
                                onFetchBattery = { viewModel.fetchBatteryLevel() }
                            )
                        }

                        // Personalize Theme Card
                        item {
                            SectionHeader(title = "Kies Jouw Stijl")
                            Card(
                                modifier = Modifier.fillMaxWidth(),
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Aangepaste Kleurschema's",
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Kies een stijl die past bij je stemming",
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = "Thema",
                                            tint = AccentPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val themes = listOf(
                                            Triple(com.example.ui.theme.AppTheme.PHILIPS_STUDIO, "Studio Blue", Color(0xFF0066FF)),
                                            Triple(com.example.ui.theme.AppTheme.CYBERPUNK_NEON, "Cyber Neon", Color(0xFFE047FF)),
                                            Triple(com.example.ui.theme.AppTheme.CARBON_AMBER, "Warm Amber", Color(0xFFF59E0B)),
                                            Triple(com.example.ui.theme.AppTheme.NORDIC_FROST, "Nordic Frost", Color(0xFF10B981))
                                        )

                                        themes.forEach { (theme, name, accent) ->
                                            val isSelected = ThemeState.activeTheme == theme
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(
                                                        color = if (isSelected) accent.copy(alpha = 0.12f) else DarkBg,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) accent else DarkBorder,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable {
                                                        ThemeState.activeTheme = theme
                                                    }
                                                    .padding(vertical = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .background(accent, shape = CircleShape)
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .background(accent.copy(alpha = 0.5f), shape = CircleShape)
                                                        )
                                                    }
                                                    Text(
                                                        text = name,
                                                        color = if (isSelected) TextPrimary else TextMuted,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = DarkBorder.copy(alpha = 0.4f))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(DarkBg, shape = RoundedCornerShape(12.dp))
                                            .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                                            .clickable {
                                                ThemeState.isLightMode = false
                                                ThemeState.activeTheme = if (ThemeState.activeTheme == com.example.ui.theme.AppTheme.HIGH_CONTRAST) com.example.ui.theme.AppTheme.PHILIPS_STUDIO else com.example.ui.theme.AppTheme.HIGH_CONTRAST
                                            }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(if (ThemeState.activeTheme == com.example.ui.theme.AppTheme.HIGH_CONTRAST) Color.White else Color.Black, shape = RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Compare,
                                                    contentDescription = null,
                                                    tint = if (ThemeState.activeTheme == com.example.ui.theme.AppTheme.HIGH_CONTRAST) Color.Black else Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = "Contrast-rijke Donkere Modus",
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Snoerzwart met felgele/witte accenten",
                                                    color = TextMuted,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        Switch(
                                            checked = !ThemeState.isLightMode && ThemeState.activeTheme == com.example.ui.theme.AppTheme.HIGH_CONTRAST,
                                            onCheckedChange = { isChecked ->
                                                if (isChecked) {
                                                    ThemeState.isLightMode = false
                                                    ThemeState.activeTheme = com.example.ui.theme.AppTheme.HIGH_CONTRAST
                                                } else {
                                                    ThemeState.isLightMode = false
                                                    ThemeState.activeTheme = com.example.ui.theme.AppTheme.PHILIPS_STUDIO
                                                }
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = HighlightSky,
                                                checkedTrackColor = AccentPrimary,
                                                uncheckedThumbColor = TextMuted,
                                                uncheckedTrackColor = DarkBg
                                            ),
                                            modifier = Modifier.scale(0.85f).testTag("high_contrast_switch")
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            SectionHeader(title = "Slimme Sensoren & Filters")
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("sensors_filters_card"),
                                colors = CardDefaults.cardColors(containerColor = DarkPanel),
                                border = BorderStroke(1.dp, DarkBorder),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // 1. Draagdetectie Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Draagdetectie (Auto-Pauze)",
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Pauzeert muziek automatisch wanneer je de koptelefoon afzet",
                                                color = TextMuted,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            )
                                        }
                                        Switch(
                                            checked = settings.wearingDetectionEnabled,
                                            onCheckedChange = { viewModel.toggleWearingDetection(it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = HighlightSky,
                                                checkedTrackColor = AccentPrimary,
                                                uncheckedThumbColor = TextMuted,
                                                uncheckedTrackColor = DarkBg
                                            ),
                                            modifier = Modifier.scale(0.85f).testTag("wearing_detection_switch")
                                        )
                                    }

                                    // Interactive Wear Sensor Simulator (if Draagdetectie is enabled)
                                    AnimatedVisibility(
                                        visible = settings.wearingDetectionEnabled,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        val isWearing by viewModel.isWearingHeadphones.collectAsStateWithLifecycle()
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(DarkBg, shape = RoundedCornerShape(12.dp))
                                                .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                                                .padding(12.dp)
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .background(if (isWearing) StatusSuccess else StatusDanger, shape = CircleShape)
                                                        )
                                                        Text(
                                                            text = if (isWearing) "Status: Op het hoofd" else "Status: Afgezet (Gepauzeerd)",
                                                            color = if (isWearing) StatusSuccess else StatusDanger,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Text(
                                                        text = "Sensor Simulator",
                                                        color = TextMuted,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = { viewModel.toggleWearingState(true) },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (isWearing) StatusSuccess.copy(alpha = 0.15f) else DarkPanel,
                                                            contentColor = if (isWearing) StatusSuccess else TextMuted
                                                        ),
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (isWearing) StatusSuccess else DarkBorder
                                                        ),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f).testTag("simulate_wear_on"),
                                                        contentPadding = PaddingValues(vertical = 4.dp)
                                                    ) {
                                                        Text("Zet Op Hoofd", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Button(
                                                        onClick = { viewModel.toggleWearingState(false) },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (!isWearing) StatusDanger.copy(alpha = 0.15f) else DarkPanel,
                                                            contentColor = if (!isWearing) StatusDanger else TextMuted
                                                        ),
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (!isWearing) StatusDanger else DarkBorder
                                                        ),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f).testTag("simulate_wear_off"),
                                                        contentPadding = PaddingValues(vertical = 4.dp)
                                                    ) {
                                                        Text("Zet Af", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = DarkBorder.copy(alpha = 0.4f))

                                    // 2. Windruisonderdrukking Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Windruisonderdrukking",
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Filtert windvlagen weg via de feed-forward microfoons",
                                                color = TextMuted,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            )
                                        }
                                        Switch(
                                            checked = settings.windNoiseReductionEnabled,
                                            onCheckedChange = { viewModel.toggleWindNoiseReduction(it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = HighlightSky,
                                                checkedTrackColor = AccentPrimary,
                                                uncheckedThumbColor = TextMuted,
                                                uncheckedTrackColor = DarkBg
                                            ),
                                            modifier = Modifier.scale(0.85f).testTag("wind_noise_switch")
                                        )
                                    }
                                }
                            }
                        }

                        // Touch Controls Card
                        item {
                            SectionHeader(title = "Aanraakbediening")
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("touch_controls_card"),
                                colors = CardDefaults.cardColors(containerColor = DarkPanel),
                                border = BorderStroke(1.dp, DarkBorder),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Touch Controls Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Aanraakpanelen Actief",
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Schakel de touch-knoppen op de oorschelpen in of uit",
                                                color = TextMuted,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            )
                                        }
                                        Switch(
                                            checked = settings.touchControlsEnabled,
                                            onCheckedChange = { viewModel.toggleTouchControls(it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = HighlightSky,
                                                checkedTrackColor = AccentPrimary,
                                                uncheckedThumbColor = TextMuted,
                                                uncheckedTrackColor = DarkBg
                                            ),
                                            modifier = Modifier.scale(0.85f).testTag("touch_controls_switch")
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = settings.touchControlsEnabled,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                            HorizontalDivider(color = DarkBorder.copy(alpha = 0.4f))

                                            TouchActionSelector(
                                                label = "Enkele Tik",
                                                selectedAction = settings.touchSingleTapAction,
                                                onActionSelected = { viewModel.setTouchSingleTapAction(it) }
                                            )

                                            TouchActionSelector(
                                                label = "Dubbele Tik",
                                                selectedAction = settings.touchDoubleTapAction,
                                                onActionSelected = { viewModel.setTouchDoubleTapAction(it) }
                                            )

                                            TouchActionSelector(
                                                label = "Ingedrukt Houden",
                                                selectedAction = settings.touchHoldAction,
                                                onActionSelected = { viewModel.setTouchHoldAction(it) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Connection Control Card
                        item {
                            SectionHeader(title = "Apparaatverbinding")
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkPanel, shape = RoundedCornerShape(12.dp))
                                    .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                if (settings.connected) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
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
                                                        .size(8.dp)
                                                        .background(StatusSuccess, shape = CircleShape)
                                                )
                                                Text(
                                                    text = "Verbonden met TAH6519",
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Text(
                                                text = "Batterij: ${settings.batteryLevel}%",
                                                color = HighlightSky,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        HorizontalDivider(color = DarkBorder)

                                        // Specs grid
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                SpecField(label = "Linker Oorschelp", value = "${(settings.batteryLevel - 3).coerceIn(0, 100)}%", modifier = Modifier.weight(1f))
                                                SpecField(label = "Rechter Oorschelp", value = "${(settings.batteryLevel + 2).coerceIn(0, 100)}%", modifier = Modifier.weight(1f))
                                            }
                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                SpecField(label = "Actieve Audio Codec", value = if (settings.ldacEnabled) "LDAC (96kHz/24bit)" else "SBC (44.1kHz)", modifier = Modifier.weight(1f))
                                                SpecField(label = "Bluetooth Versie", value = "Bluetooth 5.4 LE", modifier = Modifier.weight(1f))
                                            }
                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                SpecField(label = "Firmware Versie", value = "$firmwareVersion (${if (firmwareVersion == "v1.5.0") "Up-to-date" else "Update beschikbaar"})", modifier = Modifier.weight(1f))
                                                SpecField(label = "Driver Type", value = "40mm Biocomposiet", modifier = Modifier.weight(1f))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Button(
                                            onClick = { viewModel.disconnectDevice() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = StatusDanger.copy(alpha = 0.1f),
                                                contentColor = StatusDanger
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.3f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("disconnect_button")
                                        ) {
                                            Text(
                                                text = "Verbinding Verbreken",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.BluetoothDisabled,
                                            contentDescription = "Geen Verbinding",
                                            tint = TextMuted,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            text = "Geen actieve verbinding",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Zet Bluetooth aan op je smartphone en koppel de TAH6519 om al zijn premium geluidsfuncties aan te passen.",
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 16.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        Button(
                                            onClick = { viewModel.connectDevice() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AccentPrimary,
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("connect_button")
                                        ) {
                                            Text(
                                                text = "Verbinden met TAH6519",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Technical Connection Stats Card
                        item {
                            SectionHeader(title = "Signaal & Codec Analyse")
                            TechnicalConnectionStatsCard(viewModel, settings)
                        }

                        // Bluetooth Multipoint Card
                        item {
                            SectionHeader(title = "Bluetooth Multipoint")
                            MultipointCard(
                                multipointEnabled = settings.multipointEnabled,
                                devicesString = settings.multipointDevices,
                                connected = settings.connected,
                                onToggleMultipoint = { viewModel.toggleMultipoint(it) },
                                onAddDevice = { viewModel.addMultipointDevice(it) },
                                onRemoveDevice = { viewModel.removeMultipointDevice(it) },
                                onUpdateDevices = { viewModel.updateMultipointDevices(it) }
                            )
                        }

                        // Settings Card
                        item {
                            SectionHeader(title = "Instellingen")
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ToggleRow(
                                    label = "Accu Gezondheidsmodus",
                                    sub = "Beperkt opladen tot 80% om de levensduur van de batterij aanzienlijk te verlengen",
                                    checked = settings.batteryHealthEnabled,
                                    onCheckedChange = { viewModel.toggleBatteryHealth(it) },
                                    activeColor = StatusSuccess
                                )
                                ToggleRow(
                                    label = "Automatisch uitschakelen",
                                    sub = "Schakelt de koptelefoon uit bij langdurige inactiviteit om de accu te sparen",
                                    checked = settings.autoPowerOffEnabled,
                                    onCheckedChange = { viewModel.toggleAutoPowerOff(it) },
                                    activeColor = StatusSuccess
                                )

                                if (settings.autoPowerOffEnabled) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(DarkCard, shape = RoundedCornerShape(12.dp))
                                            .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                                            .padding(14.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Uitschakelvertraging",
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "${settings.autoPowerOffMinutes} min",
                                                    color = StatusSuccess,
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            val haptic = LocalHapticFeedback.current
                                            Slider(
                                                value = settings.autoPowerOffMinutes.toFloat(),
                                                onValueChange = { 
                                                    val intVal = it.toInt()
                                                    if (intVal != settings.autoPowerOffMinutes) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                    viewModel.setAutoPowerOffMinutes(intVal) 
                                                },
                                                valueRange = 5f..120f,
                                                steps = 22, // intervals of 5 mins (from 5 to 120 is 115 range, 115/5 = 23 ticks total, 22 intermediate steps)
                                                colors = SliderDefaults.colors(
                                                    thumbColor = StatusSuccess,
                                                    activeTrackColor = StatusSuccess,
                                                    inactiveTrackColor = DarkBorder
                                                ),
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = "5 min", color = TextMuted, fontSize = 9.sp)
                                                Text(text = "120 min", color = TextMuted, fontSize = 9.sp)
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))
                                            HorizontalDivider(color = DarkBorder, thickness = 1.dp)
                                            Spacer(modifier = Modifier.height(12.dp))

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(DarkPanel, shape = RoundedCornerShape(8.dp))
                                                    .border(1.dp, DarkBorder, shape = RoundedCornerShape(8.dp))
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(
                                                            if (autoOffIsInactive) StatusYellow else StatusSuccess,
                                                            shape = CircleShape
                                                        )
                                                )

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (autoOffIsInactive) "Inactiviteit Gedetecteerd" else "Koptelefoon Actief",
                                                        color = if (autoOffIsInactive) StatusYellow else StatusSuccess,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = if (autoOffIsInactive) {
                                                            val min = autoOffRemainingSeconds / 60
                                                            val sec = autoOffRemainingSeconds % 60
                                                            val formattedSec = String.format("%02d", sec)
                                                            "Schakelt uit over: ${min}m ${formattedSec}s"
                                                        } else {
                                                            "Muziek speelt of koptelefoon is opgezet. Timer gereset."
                                                        },
                                                        color = TextMuted,
                                                        fontSize = 10.sp
                                                    )
                                                }

                                                if (autoOffIsInactive && autoOffRemainingSeconds > 10) {
                                                    Button(
                                                        onClick = { viewModel.fastForwardAutoOff() },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = StatusYellow.copy(alpha = 0.15f),
                                                            contentColor = StatusYellow
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        shape = RoundedCornerShape(6.dp),
                                                        modifier = Modifier
                                                            .height(24.dp)
                                                            .testTag("btn_fast_forward_auto_off")
                                                    ) {
                                                        Text(
                                                            text = "Test (10s)",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Firmware Update Card
                                 Spacer(modifier = Modifier.height(16.dp))
                                 FirmwareVersionCard(viewModel = viewModel)
                             }
                         }

                        // About specs sheet
                        item {
                            SectionHeader(title = "Technische Specificaties")
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkCard, shape = RoundedCornerShape(12.dp))
                                    .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SpecRow(label = "Ontwerp", value = "Over-ear, gesloten achterkant")
                                    SpecRow(label = "Frequentiebereik", value = "20 Hz - 20.000 Hz")
                                    SpecRow(label = "Maximale Batterijduur", value = "80 uur (ANC uit) / 40 uur (ANC aan)")
                                    SpecRow(label = "Oplaadmethode", value = "USB-C Snellaadpoort (15 min = 5 uur)")
                                    SpecRow(label = "Ingebouwde Microfoons", value = "5-microfoon array met AI-ruisonderdrukking")
                                    SpecRow(label = "Gewicht", value = "252 gram")
                                }
                            }
                        }

                        // Hard reset settings
                        item {
                            Button(
                                onClick = { viewModel.resetAll() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = StatusDanger
                                ),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reset_settings_button")
                            ) {
                                Text(
                                    text = "Fabrieksinstellingen herstellen",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } // end of AnimatedContent
        } // end of Box

            // Connection progress screen
            if (isConnecting) {
                Dialog(onDismissRequest = {}) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .background(DarkPanel, shape = RoundedCornerShape(16.dp))
                            .border(1.dp, DarkBorder, shape = RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = AccentPrimary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Koppelen met TAH6519...",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Houd koptelefoon dichtbij",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Floating custom battery warning toast
        AnimatedVisibility(
            visible = activeBatteryToast != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            activeBatteryToast?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(12.dp))
                        .testTag("battery_threshold_toast"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkPanel
                    ),
                    border = BorderStroke(1.5.dp, StatusDanger.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(StatusDanger.copy(alpha = 0.15f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BatteryAlert,
                                contentDescription = "Laag Batterijniveau",
                                tint = StatusDanger,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Accuniveau Kritiek!",
                                color = StatusDanger,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = message,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }

                        IconButton(
                            onClick = { activeBatteryToast = null },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("btn_dismiss_battery_toast")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Sluit Melding",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
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
fun MiniBatteryIndicator(
    batteryLevel: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedLevel by animateFloatAsState(
        targetValue = batteryLevel.toFloat(),
        animationSpec = tween(800),
        label = "mini_battery_level"
    )

    val color = when {
        isCharging -> HighlightSky
        batteryLevel <= 20 -> StatusDanger
        batteryLevel <= 50 -> StatusYellow
        else -> StatusSuccess
    }

    Row(
        modifier = modifier
            .background(DarkCard.copy(alpha = 0.8f), shape = RoundedCornerShape(20.dp))
            .border(1.dp, DarkBorder.copy(alpha = 0.8f), shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .testTag("mini_battery_indicator"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Mini Battery Shell
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(11.dp)
                .border(1.dp, color.copy(alpha = 0.8f), shape = RoundedCornerShape(2.dp))
                .padding(1.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Battery Fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedLevel / 100f)
                    .background(color, shape = RoundedCornerShape(1.dp))
            )
            
            // If charging, overlay dynamic pulse/flash or icon
            if (isCharging) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.Center)
                )
            }
        }
        
        // Battery tip
        Box(
            modifier = Modifier
                .width(1.5.dp)
                .height(3.dp)
                .background(color.copy(alpha = 0.8f), shape = RoundedCornerShape(topEnd = 1.dp, bottomEnd = 1.dp))
        )

        Text(
            text = "$batteryLevel%",
            color = TextPrimary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp
        )
    }
}

@Composable
fun StatusBadge(label: String, active: Boolean) {
    Box(
        modifier = Modifier
            .background(
                if (active) AccentPrimary.copy(alpha = 0.15f) else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                1.dp,
                if (active) AccentPrimary else DarkBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) HighlightSky else TextMuted,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun ActiveCommunicationDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "active_comm_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(Color(0xFF10B981).copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.3f), shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier.size(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
                    .background(Color(0xFF10B981), shape = CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(Color(0xFF10B981), shape = CircleShape)
            )
        }
        Text(
            text = "LIVE LINK",
            color = Color(0xFF10B981),
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.4.sp
        )
    }
}

@Composable
fun DisconnectedStatusDot() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(Color.Gray, shape = CircleShape)
        )
        Text(
            text = "OFFLINE",
            color = TextMuted,
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.4.sp
        )
    }
}

@Composable
fun PairingStatusDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pairing_comm_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(Color(0xFFF59E0B).copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f), shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier.size(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
                    .background(Color(0xFFF59E0B), shape = CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(Color(0xFFF59E0B), shape = CircleShape)
            )
        }
        Text(
            text = "PAIRING...",
            color = Color(0xFFF59E0B),
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.4.sp
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(DarkBorder)
        )
        Text(
            text = title.uppercase(),
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(DarkBorder)
        )
    }
}

@Composable
fun FrequencyResponseGraph(bands: List<Float>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkPanel, shape = RoundedCornerShape(12.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Frequentierespons",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Directe DSP Curve",
                    color = HighlightSky,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Curve Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                val w = size.width
                val h = size.height
                
                // Draw zero reference line
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(0f, h / 2f),
                    end = Offset(w, h / 2f),
                    strokeWidth = 1.dp.toPx()
                )

                // Compute mapped coordinates for the 10 bands
                val points = bands.mapIndexed { i, gain ->
                    val x = (i.toFloat() / 9f) * w
                    val ratio = (gain + 12f) / 24f // range -12 to +12
                    val y = 10f + (h - 20f) * (1f - ratio)
                    Offset(x, y)
                }

                if (points.isNotEmpty()) {
                    // Create beautiful gradient spline curve path
                    val path = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val cx = (p0.x + p1.x) / 2f
                            cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                    }

                    // Bottom-filled path copy
                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }

                    // Fill under curve
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(AccentPrimary.copy(alpha = 0.16f), Color.Transparent),
                            startY = 0f,
                            endY = h
                        )
                    )

                    // Curve stroke with horizontal rainbow gradient
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            colors = EQBandColors
                        ),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun PresetsGrid(
    activePreset: String?,
    presets: Map<String, List<Float>>,
    onPresetSelected: (String) -> Unit
) {
    // Elegant grid representing available preset configs
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val presetKeys = presets.keys.toList()
        // Render 4 presets per row
        for (row in 0..presetKeys.size step 3) {
            val end = (row + 3).coerceAtMost(presetKeys.size)
            if (row < end) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetKeys.subList(row, end).forEach { name ->
                        val isSelected = activePreset == name
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) AccentPrimary.copy(alpha = 0.15f) else DarkCard)
                                .border(
                                    1.dp,
                                    if (isSelected) AccentPrimary else DarkBorder,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { onPresetSelected(name) }
                                .padding(vertical = 6.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                color = if (isSelected) HighlightSky else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    // Pad empty spots on the last row
                    val spots = end - row
                    if (spots < 3) {
                        for (p in spots until 3) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VerticalEqSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    trackWidth: androidx.compose.ui.unit.Dp = 18.dp
) {
    val rangeMin = -12f
    val rangeMax = 12f
    val range = rangeMax - rangeMin

    val haptic = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }

    // Haptic tick feedback whenever the slider crosses an integer dB notch
    var lastRoundedValue by remember { mutableStateOf(Math.round(value)) }
    LaunchedEffect(value) {
        val rounded = Math.round(value)
        if (rounded != lastRoundedValue) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lastRoundedValue = rounded
        }
    }

    // Snappy yet organic spring for motorized console glide effect (e.g. on preset select)
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = spring(stiffness = 700f, dampingRatio = 0.75f),
        label = "eq_slider_glide"
    )

    // Touch-sensitive tactile scale & glow transitions
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.08f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "eq_slider_scale"
    )

    val thumbSize by animateDpAsState(
        targetValue = if (isDragging) 22.dp else 18.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "eq_slider_thumb_size"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0.4f else 0.15f,
        label = "eq_slider_glow"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        val formatted = if (value == 0f) "0" else if (value > 0f) "+${value.toInt()}" else value.toInt().toString()
        Text(
            text = formatted,
            color = if (isDragging) color else HighlightSky,
            fontSize = 9.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.height(14.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .scale(scale)
                .width(trackWidth)
                .height(140.dp)
                .background(Color(0xFF0A1020), shape = RoundedCornerShape(10.dp))
                .border(
                    width = 1.dp,
                    color = if (isDragging) color.copy(alpha = 0.7f) else DarkBorder,
                    shape = RoundedCornerShape(10.dp)
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isDragging = true
                            tryAwaitRelease()
                            isDragging = false
                        }
                    ) { offset ->
                        val ratio = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                        val newValue = (rangeMin + ratio * range).coerceIn(rangeMin, rangeMax)
                        onValueChange(Math.round(newValue).toFloat())
                    }
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false }
                    ) { change, _ ->
                        change.consume()
                        val y = change.position.y.coerceIn(0f, size.height.toFloat())
                        val ratio = 1f - (y / size.height)
                        val newValue = (rangeMin + ratio * range).coerceIn(rangeMin, rangeMax)
                        onValueChange(Math.round(newValue).toFloat())
                    }
                }
        ) {
            val ratio = ((animatedValue - rangeMin) / range).coerceIn(0f, 1f)

            // Notch at 0 dB (center of track)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.Center)
                    .background(Color(0xFFFFFFFF).copy(alpha = 0.15f))
            )

            // Colored fill from bottom to height
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(ratio)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(color.copy(alpha = 0.85f), color.copy(alpha = 0.25f)),
                            startY = 0f
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
            )

            // Circle thumb perfectly aligned using padding bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = (122 * ratio).dp) // bounds thumb correctly inside 140dp minus size height
                    .size(thumbSize)
                    .background(color, shape = CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.9f), shape = CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            color = if (isDragging) color else TextMuted,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            lineHeight = 11.sp
        )
    }
}

@Composable
fun MasterGainSlider(
    gain: Float,
    onGainChange: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, shape = RoundedCornerShape(12.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                        contentDescription = "Gain icon",
                        tint = HighlightSky,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Master Gain",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = if (gain == 0f) "0 dB" else if (gain > 0f) "+$gain dB" else "$gain dB",
                    color = HighlightSky,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = gain,
                onValueChange = { onGainChange(it) },
                valueRange = -6f..6f,
                steps = 23, // 0.5f intervals
                colors = SliderDefaults.colors(
                    thumbColor = AccentPrimary,
                    activeTrackColor = AccentPrimary,
                    inactiveTrackColor = DarkBorder
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "−6 dB (zachter)", color = TextMuted, fontSize = 9.sp)
                Text(text = "+6 dB (luider)", color = TextMuted, fontSize = 9.sp)
            }
        }
    }
}

@Composable
fun ToggleRow(
    label: String,
    sub: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color
) {
    val haptic = LocalHapticFeedback.current

    val rowBgColor by animateColorAsState(
        targetValue = if (checked) activeColor.copy(alpha = 0.08f) else DarkCard,
        animationSpec = tween(durationMillis = 250),
        label = "row_bg_color"
    )
    val rowBorderColor by animateColorAsState(
        targetValue = if (checked) activeColor else DarkBorder,
        animationSpec = tween(durationMillis = 250),
        label = "row_border_color"
    )
    val switchBgColor by animateColorAsState(
        targetValue = if (checked) activeColor else DarkBorder,
        animationSpec = tween(durationMillis = 250),
        label = "switch_bg_color"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 18.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "switch_thumb_offset"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBgColor, shape = RoundedCornerShape(12.dp))
            .border(1.dp, rowBorderColor, shape = RoundedCornerShape(12.dp))
            .clickable { 
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked) 
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Toggle Switch custom design
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(24.dp)
                .background(switchBgColor, shape = RoundedCornerShape(12.dp))
                .padding(3.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(18.dp)
                    .background(Color.White, shape = CircleShape)
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = sub,
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchActionSelector(label: String, selectedAction: String, onActionSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Afspelen/Pauzeren", "Volgende track", "Vorige track", "ANC Wisselen", "Spraakassistent", "Volume Omhoog", "Volume Omlaag", "Geen actie")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(text = selectedAction, fontSize = 11.sp)
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(DarkPanel)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option, color = if (selectedAction == option) HighlightSky else TextPrimary, fontSize = 13.sp) },
                        onClick = {
                            onActionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SpecField(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, color = TextMuted, fontSize = 9.sp)
        Text(text = value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp)
        Text(text = value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

data class AncLevelData(val level: Int, val name: String, val desc: String)
data class TipData(val icon: String, val title: String, val color: Color, val text: String)

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

fun getBatteryIcon(level: Int): ImageVector {
    return when {
        level <= 20 -> Icons.Default.BatteryAlert
        else -> Icons.Default.BatteryFull
    }
}

@Composable
fun LowBatteryAlert(
    batteryLevel: Int,
    isSmartSaverActive: Boolean,
    onActivateSmartSaver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "alert_pulse")
    val alertAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alert_pulse_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StatusDanger.copy(alpha = 0.1f))
            .border(1.dp, StatusDanger.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(12.dp)
            .testTag("low_battery_alert_container")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Waarschuwing batterij bijna leeg",
                    tint = StatusDanger.copy(alpha = alertAlpha),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Kritiek Batterijniveau (${batteryLevel}%)",
                    color = StatusDanger,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            
            Text(
                text = "Je hoofdtelefoon valt binnenkort uit. Sluit een USB-C oplader aan of activeer Smart Saver-modus om direct de resterende gebruiksduur te verlengen.",
                color = TextPrimary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
            
            if (isSmartSaverActive) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StatusSuccess.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Smart Saver Actief",
                        tint = StatusSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Smart Saver-modus Actief (ANC & LDAC uitgeschakeld)",
                        color = StatusSuccess,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Button(
                    onClick = onActivateSmartSaver,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusDanger,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .align(Alignment.End)
                        .height(32.dp)
                        .testTag("btn_activate_smart_saver")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Bolt icon",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Activeer Smart Saver",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BatteryFetcherComponent(
    isFetchingBattery: Boolean,
    batteryFetchProgress: Float,
    batteryFetchStatus: String,
    onFetchBattery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBg.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
            .border(1.dp, DarkBorder.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
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
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = if (isFetchingBattery) HighlightSky else TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Live Batterijniveau Ophalen",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (!isFetchingBattery) {
                Button(
                    onClick = onFetchBattery,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("fetch_battery_button"),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Ophalen",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "${(batteryFetchProgress * 100).toInt()}%",
                    color = HighlightSky,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        
        if (isFetchingBattery) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // The actual animated visual progress bar for fetching
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(DarkPanel, shape = RoundedCornerShape(4.dp))
                        .padding(1.dp)
                ) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = batteryFetchProgress,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
                        label = "fetch_progress"
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(AccentPrimary, HighlightSky)
                                ),
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                }
                
                Text(
                    text = batteryFetchStatus,
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        } else {
            Text(
                text = "Klik op Ophalen om het live batterijniveau via Bluetooth op te vragen.",
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Suppress("DEPRECATION")
@Composable
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
    bassEnabled: Boolean = true,
    batteryHealthEnabled: Boolean = false,
    isFetchingBattery: Boolean = false,
    batteryFetchProgress: Float = 0f,
    batteryFetchStatus: String = "",
    onFetchBattery: () -> Unit = {}
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

    var batteryViewMode by remember { mutableStateOf("VISUAL") }

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

                // Switcher between Headset Art, Accu Meters and Accu Balk
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("VISUAL" to "Headset Art", "METERS" to "Accu Meters", "PROGRESS" to "Accu Balk").forEach { (mode, label) ->
                        val selected = batteryViewMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (selected) DarkPanel else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) DarkBorder else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { batteryViewMode = mode }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selected) HighlightSky else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                when (batteryViewMode) {
                    "VISUAL" -> {
                        Tah6519HeadphoneBatteryArt(
                            batteryLevel = batteryLevel,
                            isCharging = isCharging,
                            ancMode = ancMode,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "METERS" -> {
                        PhilipsPremiumBatteryIndicator(
                            batteryLevel = batteryLevel,
                            isCharging = isCharging,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {
                        PhilipsHeadphoneProgressBar(
                            batteryLevel = batteryLevel,
                            isCharging = isCharging,
                            healthModeActive = batteryHealthEnabled,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (connected) {
                    BatteryFetcherComponent(
                        isFetchingBattery = isFetchingBattery,
                        batteryFetchProgress = batteryFetchProgress,
                        batteryFetchStatus = batteryFetchStatus,
                        onFetchBattery = onFetchBattery,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Dynamic Estimate based on official Philips TAH6519 specs (40h ANC on, 80h ANC off)
                val baseMaxHours = if (ancMode != "OFF") 40f else 80f
                val codecFactor = if (ldacEnabled) 0.75f else 1.0f // LDAC consumes more power
                val maxHours = baseMaxHours * codecFactor
                val estHours = if (batteryLevel == 0) 0 else ((batteryLevel / 100f) * maxHours).toInt()

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
                                if (minsLeft == 0) "Volledig geladen" else "~$minsLeft min (Fast Charge)"
                            } else {
                                "~$estHours uur (${if (batteryLevel > 20) "Voldoende" else "Laag, laad op"})"
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
                        drainText = if (ancMode != "OFF") "-40u accuduur" else "+40u bespaard",
                        isPositive = ancMode == "OFF"
                    )

                    // Consumer 2: LDAC
                    PowerConsumerRow(
                        label = "Hi-Res LDAC Codec",
                        icon = Icons.Default.MusicNote,
                        isActive = ldacEnabled,
                        drainText = if (ldacEnabled) "-20u accuduur" else "+20u bespaard",
                        isPositive = !ldacEnabled
                    )

                    // Consumer 3: Bass Boost
                    PowerConsumerRow(
                        label = "Dynamic Bass Boost",
                        icon = Icons.Default.Hearing,
                        isActive = bassEnabled,
                        drainText = if (bassEnabled) "-4u accuduur" else "Zuinig",
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


@Composable
fun NoiseControlVisualizer(activeMode: String) {
    val transition = rememberInfiniteTransition(label = "anc_waves")
    val phaseOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(DarkBg, shape = RoundedCornerShape(16.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(16.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            val boundaryX = width / 2f
            
            when (activeMode) {
                "ON" -> {
                    // ANC On: External sound waves canceled by inverse waves, resulting in a flat inner line
                    val wavePoints = 120
                    val extPath = Path()
                    val antiPath = Path()
                    val innerPath = Path()
                    val midPoint = boundaryX
                    
                    for (i in 0..wavePoints) {
                        val fraction = i.toFloat() / wavePoints
                        val x = fraction * midPoint
                        val extAmp = 18.dp.toPx() * (1f - fraction * 0.3f)
                        
                        val extY = centerY + extAmp * kotlin.math.sin(fraction * 4f * Math.PI.toFloat() + phaseOffset)
                        if (i == 0) extPath.moveTo(x, extY) else extPath.lineTo(x, extY)
                        
                        val antiY = centerY + extAmp * kotlin.math.sin(fraction * 4f * Math.PI.toFloat() + phaseOffset + Math.PI.toFloat())
                        if (i == 0) antiPath.moveTo(x, antiY) else antiPath.lineTo(x, antiY)
                    }
                    
                    for (i in 0..wavePoints) {
                        val fraction = i.toFloat() / wavePoints
                        val x = midPoint + fraction * (width - midPoint)
                        val residualAmp = 1.2f.dp.toPx()
                        val residualY = centerY + residualAmp * kotlin.math.sin(fraction * 6f * Math.PI.toFloat() - phaseOffset * 2f)
                        if (i == 0) innerPath.moveTo(x, residualY) else innerPath.lineTo(x, residualY)
                    }
                    
                    drawPath(
                        path = extPath,
                        color = StatusDanger.copy(alpha = 0.5f),
                        style = Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                    
                    drawPath(
                        path = antiPath,
                        color = HighlightSky.copy(alpha = 0.5f),
                        style = Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                    
                    drawPath(
                        path = innerPath,
                        color = StatusSuccess.copy(alpha = 0.9f),
                        style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                    
                    drawLine(
                        color = AccentPrimary,
                        start = Offset(boundaryX, 15.dp.toPx()),
                        end = Offset(boundaryX, height - 15.dp.toPx()),
                        strokeWidth = 4.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
                "TRANSPARENCY" -> {
                    // Transparency Mode: Wave flows through dashed microphone barrier representing awareness
                    val wavePoints = 200
                    val flowPath = Path()
                    
                    for (i in 0..wavePoints) {
                        val fraction = i.toFloat() / wavePoints
                        val x = fraction * width
                        val amp = 14.dp.toPx()
                        val y = centerY + amp * kotlin.math.sin(fraction * 5f * Math.PI.toFloat() - phaseOffset)
                        if (i == 0) flowPath.moveTo(x, y) else flowPath.lineTo(x, y)
                    }
                    
                    drawPath(
                        path = flowPath,
                        color = HighlightSky.copy(alpha = 0.85f),
                        style = Stroke(width = 2.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                    
                    val pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                    drawLine(
                        color = HighlightSky.copy(alpha = 0.4f),
                        start = Offset(boundaryX, 15.dp.toPx()),
                        end = Offset(boundaryX, height - 15.dp.toPx()),
                        strokeWidth = 4.dp.toPx(),
                        pathEffect = pathEffect,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
                else -> {
                    // Off Mode: Sound waves on the outside, attenuated by earcup barrier, low waves inside
                    val wavePoints = 120
                    val extPath = Path()
                    val innerPath = Path()
                    val midPoint = boundaryX
                    
                    for (i in 0..wavePoints) {
                        val fraction = i.toFloat() / wavePoints
                        val x = fraction * midPoint
                        val amp = 16.dp.toPx()
                        val y = centerY + amp * kotlin.math.sin(fraction * 4f * Math.PI.toFloat() + phaseOffset)
                        if (i == 0) extPath.moveTo(x, y) else extPath.lineTo(x, y)
                    }
                    
                    for (i in 0..wavePoints) {
                        val fraction = i.toFloat() / wavePoints
                        val x = midPoint + fraction * (width - midPoint)
                        val amp = 5.dp.toPx()
                        val y = centerY + amp * kotlin.math.sin(fraction * 4f * Math.PI.toFloat() + phaseOffset)
                        if (i == 0) innerPath.moveTo(x, y) else innerPath.lineTo(x, y)
                    }
                    
                    drawPath(
                        path = extPath,
                        color = TextMuted.copy(alpha = 0.6f),
                        style = Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                    
                    drawPath(
                        path = innerPath,
                        color = TextMuted.copy(alpha = 0.3f),
                        style = Stroke(width = 1.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                    
                    drawLine(
                        color = DarkBorder,
                        start = Offset(boundaryX, 15.dp.toPx()),
                        end = Offset(boundaryX, height - 15.dp.toPx()),
                        strokeWidth = 6.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Omgevingsgeluid (Extern)",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "In-Ear Audio (Intern)",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun NoiseControlToggle(
    activeMode: String,
    onModeChange: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RUISONDERDRUKKING MODUS",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = when (activeMode) {
                    "ON" -> "ANC Actief (-56 dB)"
                    "TRANSPARENCY" -> "Awareness Mode Actief"
                    else -> "Passieve Isolatie Actief"
                },
                color = when (activeMode) {
                    "ON" -> StatusSuccess
                    "TRANSPARENCY" -> HighlightSky
                    else -> TextMuted
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Animated sound-wave canvas representing real-time dsp modes
        NoiseControlVisualizer(activeMode = activeMode)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkCard, shape = RoundedCornerShape(24.dp))
                .border(1.dp, DarkBorder, shape = RoundedCornerShape(24.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val modes = listOf(
                Triple("ON", "ANC On", Icons.Default.GraphicEq),
                Triple("TRANSPARENCY", "Awareness", Icons.Default.Hearing),
                Triple("OFF", "ANC Off", Icons.Default.Close)
            )

            modes.forEach { (mode, label, icon) ->
                val isSelected = activeMode == mode
                val activeThemeColor = when (mode) {
                    "ON" -> AccentPrimary
                    "TRANSPARENCY" -> HighlightSky
                    else -> TextMuted
                }

                val bgAnimateColor by animateColorAsState(
                    targetValue = if (isSelected) activeThemeColor else Color.Transparent,
                    animationSpec = tween(durationMillis = 250),
                    label = "anc_toggle_bg"
                )
                val tintAnimateColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else TextMuted,
                    animationSpec = tween(durationMillis = 250),
                    label = "anc_toggle_tint"
                )
                val textAnimateColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else TextPrimary,
                    animationSpec = tween(durationMillis = 250),
                    label = "anc_toggle_text"
                )
                val scaleAnimate by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1.00f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy),
                    label = "anc_toggle_scale"
                )
                val borderAnimateColor by animateColorAsState(
                    targetValue = if (isSelected) activeThemeColor.copy(alpha = 0.5f) else Color.Transparent,
                    animationSpec = tween(durationMillis = 250),
                    label = "anc_toggle_border"
                )
                
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .scale(scaleAnimate)
                        .background(
                            color = bgAnimateColor,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = borderAnimateColor,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onModeChange(mode) 
                        }
                        .testTag("anc_mode_$mode"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = tintAnimateColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        color = textAnimateColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Mode explanation card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkPanel),
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = when (activeMode) {
                        "ON" -> "🧠 Hybrid ANC: Filtert constant storende lage en middenfrequente geluiden weg zoals motoren, ventilatoren en omgevingsrumoer. Biedt tot 56 dB reductie voor maximale concentratie."
                        "TRANSPARENCY" -> "🎤 Aura Sound Transparency: Gebruikt de TAH6519 microfoons om spraak en externe waarschuwingen helder door te geven. Je hoeft je koptelefoon niet af te zetten om te praten."
                        else -> "🔋 ANC Uitgeschakeld: Bespaart maximale batterij (tot 80 uur speeltijd). De comfortabele, afsluitende oorkussens verminderen omgevingsgeluid nog steeds met ca. 20 dB via passieve demping."
                    },
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
fun MultipointCard(
    multipointEnabled: Boolean,
    devicesString: String,
    connected: Boolean,
    onToggleMultipoint: (Boolean) -> Unit,
    onAddDevice: (String) -> Unit,
    onRemoveDevice: (String) -> Unit,
    onUpdateDevices: (String) -> Unit
) {
    var showPairDialog by remember { mutableStateOf(false) }
    var newDeviceName by remember { mutableStateOf("") }

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
                    imageVector = Icons.Default.Devices,
                    contentDescription = "Bluetooth Multipoint",
                    tint = TextMuted,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = "Bluetooth Multipoint niet actief",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Verbind de TAH6519 koptelefoon om meervoudige apparaatverbindingen te beheren.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = "Multipoint",
                            tint = AccentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Multipoint-verbinding",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Verbind tot 2 apparaten tegelijkertijd",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Switch(
                        checked = multipointEnabled,
                        onCheckedChange = onToggleMultipoint,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentPrimary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBorder
                        ),
                        modifier = Modifier.testTag("multipoint_toggle")
                    )
                }

                if (multipointEnabled) {
                    HorizontalDivider(color = DarkBorder)

                    val deviceList = remember(devicesString) {
                        if (devicesString.isEmpty()) emptyList()
                        else devicesString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    }

                    if (deviceList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBg, shape = RoundedCornerShape(8.dp))
                                .border(1.dp, DarkBorder, shape = RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Geen gekoppelde apparaten. Voeg een apparaat toe om Multipoint te gebruiken.",
                                color = TextMuted,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            deviceList.forEachIndexed { index, deviceName ->
                                val isPrimary = index == 0
                                val deviceIcon = when {
                                    deviceName.contains("MacBook", ignoreCase = true) || 
                                    deviceName.contains("Laptop", ignoreCase = true) ||
                                    deviceName.contains("PC", ignoreCase = true) ||
                                    deviceName.contains("Computer", ignoreCase = true) -> Icons.Default.Laptop
                                    deviceName.contains("TV", ignoreCase = true) ||
                                    deviceName.contains("Television", ignoreCase = true) -> Icons.Default.Tv
                                    else -> Icons.Default.PhoneAndroid
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkBg, shape = RoundedCornerShape(8.dp))
                                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    if (isPrimary) AccentPrimary.copy(alpha = 0.15f) else DarkBorder,
                                                    shape = RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = deviceIcon,
                                                contentDescription = deviceName,
                                                tint = if (isPrimary) AccentPrimary else TextMuted,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = deviceName,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                modifier = Modifier.testTag("multipoint_device_name_$index")
                                            )
                                            
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(
                                                            if (isPrimary) StatusSuccess else StatusYellow,
                                                            shape = CircleShape
                                                        )
                                                )
                                                Text(
                                                    text = if (isPrimary) "Actief · Primaire audio" else "Standby · Secundair",
                                                    color = if (isPrimary) StatusSuccess else StatusYellow,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Option to swap priority
                                        if (deviceList.size > 1 && isPrimary) {
                                            IconButton(
                                                onClick = {
                                                    // Swap devices: reverse list
                                                    val swapped = deviceList.asReversed().joinToString(",")
                                                    onUpdateDevices(swapped)
                                                },
                                                modifier = Modifier.size(28.dp).testTag("btn_swap_priority")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.SwapVert,
                                                    contentDescription = "Wissel prioriteit",
                                                    tint = HighlightSky,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { onRemoveDevice(deviceName) },
                                            modifier = Modifier.size(28.dp).testTag("btn_remove_device_$index")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Verbreek verbinding",
                                                tint = StatusDanger,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Pair New Device Button
                    if (deviceList.size < 2) {
                        Button(
                            onClick = { showPairDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkBg,
                                contentColor = AccentPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_pair_new_device"),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Nieuw koppelen",
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Nieuw apparaat koppelen...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Max devices connected explanation
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = TextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Maximale capaciteit bereikt (2 actieve audiobronnen). Verbreek een apparaat om een ander te koppelen.",
                                color = TextMuted,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                } else {
                    // Multipoint is disabled explanation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBg, shape = RoundedCornerShape(8.dp))
                            .border(1.dp, DarkBorder, shape = RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Multipoint is uitgeschakeld. De TAH6519 zal zich uitsluitend verbinden met de primaire audiobron. Dit minimaliseert bluetooth-verkeer en optimaliseert audio latency.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }

    // Pair Dialog (Inline or Alert)
    if (showPairDialog) {
        AlertDialog(
            onDismissRequest = { showPairDialog = false },
            containerColor = DarkPanel,
            title = {
                Text(
                    text = "Apparaat Toevoegen",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Kies een van de beschikbare apparaten in de buurt om direct te verbinden via Multipoint:",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    // Quick selection list
                    val popularDevices = listOf("Windows Laptop", "iPad Air", "Smart TV Living Room", "iPhone 15 Pro")
                    popularDevices.forEach { deviceName ->
                        Button(
                            onClick = {
                                onAddDevice(deviceName)
                                showPairDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBg),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pair_preset_$deviceName"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(deviceName, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("VERBINDEN", color = AccentPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = DarkBorder)

                    // Manual input field
                    OutlinedTextField(
                        value = newDeviceName,
                        onValueChange = { newDeviceName = it },
                        label = { Text("Aangepaste apparaatnaam", color = TextMuted, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = DarkBorder,
                            cursorColor = AccentPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_device_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDeviceName.isNotBlank()) {
                            onAddDevice(newDeviceName.trim())
                            newDeviceName = ""
                            showPairDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    shape = RoundedCornerShape(8.dp),
                    enabled = newDeviceName.isNotBlank(),
                    modifier = Modifier.testTag("btn_confirm_pair_custom")
                ) {
                    Text("Toevoegen", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showPairDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextMuted),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Annuleren", fontSize = 12.sp)
                }
            }
        )
    }
}

@Composable
fun CustomPresetsGrid(
    activePreset: String?,
    customPresets: Map<String, List<Float>>,
    onPresetSelected: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    onRenamePreset: (String, String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var presetToRename by remember { mutableStateOf<String?>(null) }
    var renameNewName by remember { mutableStateOf("") }

    if (presetToRename != null) {
        AlertDialog(
            onDismissRequest = { presetToRename = null },
            title = { Text("Preset hernoemen", color = TextPrimary) },
            text = {
                Column {
                    Text("Voer een nieuwe naam in voor \"${presetToRename}\":", color = TextMuted, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = renameNewName,
                        onValueChange = { renameNewName = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = DarkBorder,
                            cursorColor = AccentPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("rename_preset_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val old = presetToRename
                        if (old != null && renameNewName.isNotBlank()) {
                            onRenamePreset(old, renameNewName.trim())
                        }
                        presetToRename = null
                        renameNewName = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    enabled = renameNewName.isNotBlank() && renameNewName.trim() != presetToRename,
                    modifier = Modifier.testTag("confirm_rename_button")
                ) {
                    Text("Opslaan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { presetToRename = null; renameNewName = "" }) {
                    Text("Annuleren", color = TextMuted)
                }
            },
            containerColor = DarkCard
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val presetKeys = customPresets.keys.toList()
        for (row in 0 until presetKeys.size step 2) {
            val end = (row + 2).coerceAtMost(presetKeys.size)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presetKeys.subList(row, end).forEach { name ->
                    val isSelected = activePreset == name
                    var menuExpanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) AccentPrimary.copy(alpha = 0.15f) else DarkCard)
                            .border(
                                1.dp,
                                if (isSelected) AccentPrimary else DarkBorder,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onPresetSelected(name) }
                            .padding(vertical = 4.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            color = if (isSelected) HighlightSky else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Box {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuExpanded = true
                                },
                                modifier = Modifier.size(36.dp).testTag("preset_options_$name")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Opties voor $name",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(DarkPanel)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Hernoemen", color = TextPrimary) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Hernoemen",
                                            tint = AccentPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        presetToRename = name
                                        renameNewName = name
                                    },
                                    modifier = Modifier.testTag("menu_rename_$name")
                                )
                                DropdownMenuItem(
                                    text = { Text("Verwijderen", color = StatusDanger) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Verwijderen",
                                            tint = StatusDanger,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onDeletePreset(name)
                                    },
                                    modifier = Modifier.testTag("menu_delete_$name")
                                )
                            }
                        }
                    }
                }
                val spots = end - row
                if (spots < 2) {
                    for (p in spots until 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun HearingTestCard(
    viewModel: HeadphoneViewModel,
    settings: HeadphoneSettings
) {
    var showTestDialog by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkPanel, shape = RoundedCornerShape(12.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Hearing,
                    contentDescription = "Gehoor-ID",
                    tint = HighlightSky,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = "Persoonlijk Gehoor-ID",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Kalibreer geluid naar jouw gehoor",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
            
            Text(
                text = "Met een korte interactieve gehoortest meten we je gehoorlimiet op verschillende frequenties. De koptelefoon past daarna automatisch een compenserende equalizer aan om details te herstellen.",
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            
            val hasProfile = settings.getCustomPresetsMap().containsKey("Gehoor-ID Profile")
            if (hasProfile) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StatusSuccess.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .border(1.dp, StatusSuccess.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Actief",
                        tint = StatusSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Gehoor-ID Profile is succesvol gekalibreerd en opgeslagen!",
                        color = StatusSuccess,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Button(
                onClick = { showTestDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("btn_start_hearing_test")
            ) {
                Text(
                    text = if (hasProfile) "Kalibratie Opnieuw Uitvoeren" else "Start Gehoortest (3 min)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
    
    if (showTestDialog) {
        HearingTestWizardDialog(
            onDismiss = { showTestDialog = false },
            onSaveProfile = { bands ->
                viewModel.saveCustomPreset("Gehoor-ID Profile", bands)
                viewModel.setPreset("Gehoor-ID Profile")
                showTestDialog = false
            }
        )
    }
}

@Composable
fun HearingTestWizardDialog(
    onDismiss: () -> Unit,
    onSaveProfile: (List<Float>) -> Unit
) {
    var step by remember { mutableStateOf(0) } // 0: Intro, 1-4: test frequencies, 5: results
    val frequencies = listOf("250 Hz", "1000 Hz", "4000 Hz", "8000 Hz")
    
    // threshold values for each frequency
    val hearingSensitivity = remember { mutableStateListOf(50f, 50f, 50f, 50f) }
    
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkPanel, shape = RoundedCornerShape(16.dp))
                .border(1.dp, DarkBorder, shape = RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (step == 0) "Gehoortest · Intro" else if (step in 1..4) "Gehoortest · Stap $step van 4" else "Gehoortest · Resultaat",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Sluit", tint = TextMuted)
                    }
                }
                
                HorizontalDivider(color = DarkBorder)
                
                if (step == 0) {
                    // Introduction
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = "Stilte",
                        tint = HighlightSky,
                        modifier = Modifier.size(56.dp)
                    )
                    
                    Text(
                        text = "Vind een stille omgeving",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    
                    Text(
                        text = "Zorg ervoor dat je koptelefoon stevig op je oren zit en het volume op ca. 50% staat.\n\nWe spelen zo vier zachte testtonen af van laag naar hoog.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { step = 1 },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_wizard_next")
                    ) {
                        Text("Ik ben klaar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else if (step in 1..4) {
                    // Active frequency test steps
                    val currentFreqIndex = step - 1
                    val freqLabel = frequencies[currentFreqIndex]
                    
                    // Pulsating circle animation
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse_freq")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 0.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )
                    
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Pulse background
                        Box(
                            modifier = Modifier
                                .size((100 * pulseScale).dp)
                                .background(AccentPrimary.copy(alpha = pulseAlpha), shape = CircleShape)
                        )
                        // Core circle
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .background(AccentPrimary, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Pulsing tone",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    
                    Text(
                        text = "Hoor je de toon op $freqLabel?",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    
                    Text(
                        text = "We spelen nu een subtiele $freqLabel toon af. Geef aan vanaf welk volume je de toon duidelijk begint te onderscheiden.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    // Sensitivity input slider
                    var currentSliderVal by remember(step) { mutableStateOf(50f) }
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Duidelijkheid:", color = TextMuted, fontSize = 10.sp)
                            Text(
                                text = when {
                                    currentSliderVal < 30f -> "Heel Zacht (Goed gehoor)"
                                    currentSliderVal < 70f -> "Normaal (Gemiddeld)"
                                    else -> "Zwak (Hoorbaar met boost)"
                                },
                                color = HighlightSky,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = currentSliderVal,
                            onValueChange = { currentSliderVal = it },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = AccentPrimary,
                                activeTrackColor = AccentPrimary,
                                inactiveTrackColor = DarkBorder
                            )
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                // Save sensitivity threshold (user choice)
                                hearingSensitivity[currentFreqIndex] = currentSliderVal
                                if (step < 4) {
                                    step += 1
                                } else {
                                    step = 5
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("btn_wizard_hear")
                        ) {
                            Text("Volgende", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                } else {
                    // step 5: Results calculation & saving
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Resultaat",
                        tint = StatusSuccess,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Text(
                        text = "Jouw Gehoor-ID is berekend!",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    
                    Text(
                        text = "We hebben lichte gevoeligheden ontdekt in de hogere frequenties (typisch voor over-ear koptelefoons). Er is een gepersonaliseerde EQ-compensatiecurve gemaakt.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                    
                    // Show a preview of the compensation bands
                    val compensationCurve = remember {
                        val s250 = (hearingSensitivity[0] / 100f) * 4f
                        val s1000 = (hearingSensitivity[1] / 100f) * 3f
                        val s4000 = (hearingSensitivity[2] / 100f) * 6f
                        val s8000 = (hearingSensitivity[3] / 100f) * 7f
                        
                        listOf(
                            s250 * 0.3f,         // 60Hz
                            s250,                // 125Hz
                            s250 * 0.8f,         // 250Hz
                            s1000 * 0.5f,        // 500Hz
                            s1000,               // 1kHz
                            s4000 * 0.4f,        // 2kHz
                            s4000,               // 4kHz
                            s8000,               // 8kHz
                            s8000 * 0.7f,        // 12kHz
                            s8000 * 0.4f         // 16kHz
                        ).map { Math.round(it).toFloat() }
                    }
                    
                    // Render miniature preview graph of the hearing EQ
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(DarkBg, shape = RoundedCornerShape(8.dp))
                            .border(1.dp, DarkBorder, shape = RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val points = compensationCurve.mapIndexed { i, gain ->
                                val x = (i.toFloat() / 9f) * w
                                val ratio = (gain + 12f) / 24f
                                val y = h * (1f - ratio)
                                Offset(x, y)
                            }
                            val path = Path().apply {
                                moveTo(points[0].x, points[0].y)
                                for (i in 0 until points.size - 1) {
                                    val cx = (points[i].x + points[i + 1].x) / 2f
                                    cubicTo(cx, points[i].y, cx, points[i + 1].y, points[i + 1].x, points[i + 1].y)
                                }
                            }
                            drawPath(path, color = HighlightSky, style = Stroke(width = 2.dp.toPx()))
                            points.forEach { pt ->
                                drawCircle(color = AccentPrimary, radius = 3.dp.toPx(), center = pt)
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextMuted),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Wissen")
                        }
                        Button(
                            onClick = { onSaveProfile(compensationCurve) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                            modifier = Modifier.weight(1f).testTag("btn_wizard_save")
                        ) {
                            Text("Toepassen", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ZenSoundscapesCard() {
    var isPlaying by remember { mutableStateOf(false) }
    var selectedSound by remember { mutableStateOf("Zachte Regen") }
    var sleepTimerMinutes by remember { mutableStateOf(0) } // 0: off, 15, 30, 60
    var timerSecondsRemaining by remember { mutableStateOf(0) }
    var soundVolume by remember { mutableStateOf(50f) }
    
    // Ticking countdown effect for the sleep timer
    LaunchedEffect(isPlaying, sleepTimerMinutes, timerSecondsRemaining) {
        if (isPlaying && sleepTimerMinutes > 0) {
            if (timerSecondsRemaining == 0) {
                timerSecondsRemaining = sleepTimerMinutes * 60
            }
            while (timerSecondsRemaining > 0 && isPlaying) {
                delay(1000)
                timerSecondsRemaining -= 1
            }
            if (timerSecondsRemaining == 0) {
                isPlaying = false
                sleepTimerMinutes = 0
            }
        } else if (!isPlaying) {
            timerSecondsRemaining = 0
        }
    }
    
    val sounds = listOf(
        Pair("🌧️", "Zachte Regen"),
        Pair("🌊", "Oceaanbries"),
        Pair("🌲", "Bosgeluiden"),
        Pair("🌫️", "Witte Ruis")
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkPanel, shape = RoundedCornerShape(12.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
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
                        imageVector = Icons.Default.Spa,
                        contentDescription = "Zen Soundscapes",
                        tint = StatusPurple,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Zen Soundscapes",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Ontspan met ingebouwde omgevingsgeluiden",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
                
                // Animating equalizer bars if playing
                if (isPlaying) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.height(14.dp)
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "soundwave_bars")
                        for (i in 0..3) {
                            val barHeight by infiniteTransition.animateFloat(
                                initialValue = 4f,
                                targetValue = 14f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 300 + (i * 100), easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "bar_$i"
                            )
                            Box(
                                modifier = Modifier
                                    .width(2.5.dp)
                                    .height(barHeight.dp)
                                    .background(StatusPurple, shape = RoundedCornerShape(1.dp))
                            )
                        }
                    }
                }
            }
            
            Text(
                text = "Laat de omgevingsgeluiden van je TAH6519 mengen in de achtergrond om omgevingslawaai nog beter te maskeren of makkelijker in slaap te vallen.",
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            
            // Sounds grid selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sounds.forEach { (emoji, label) ->
                    val isSelected = selectedSound == label
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) StatusPurple.copy(alpha = 0.15f) else DarkCard)
                            .border(1.dp, if (isSelected) StatusPurple else DarkBorder, shape = RoundedCornerShape(10.dp))
                            .clickable { selectedSound = label }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = emoji, fontSize = 18.sp)
                        Text(
                            text = label.split(" ")[0], // short version
                            color = if (isSelected) HighlightSky else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            HorizontalDivider(color = DarkBorder)
            
            // Controls section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause Circle Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(38.dp)
                            .background(if (isPlaying) StatusPurple else DarkBorder, shape = CircleShape)
                            .testTag("btn_toggle_soundscape")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Speel af of pauzeer soundscape",
                            tint = if (isPlaying) Color.White else TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isPlaying) "Speelt nu af" else "Gepauzeerd",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedSound,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
                
                // Sleep Timer Selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Timer, contentDescription = "Timer", tint = TextMuted, modifier = Modifier.size(16.dp))
                    Text(text = "Timer:", color = TextMuted, fontSize = 11.sp)
                    
                    Box(
                        modifier = Modifier
                            .background(DarkCard, shape = RoundedCornerShape(8.dp))
                            .border(1.dp, DarkBorder, shape = RoundedCornerShape(8.dp))
                            .clickable {
                                sleepTimerMinutes = when (sleepTimerMinutes) {
                                    0 -> 15
                                    15 -> 30
                                    30 -> 60
                                    else -> 0
                                }
                                timerSecondsRemaining = sleepTimerMinutes * 60
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("btn_cycle_sleep_timer")
                    ) {
                        Text(
                            text = if (sleepTimerMinutes == 0) "Uit" else "${sleepTimerMinutes}m",
                            color = if (sleepTimerMinutes > 0) HighlightSky else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Countdown timer display
            if (isPlaying && sleepTimerMinutes > 0 && timerSecondsRemaining > 0) {
                val mins = timerSecondsRemaining / 60
                val secs = timerSecondsRemaining % 60
                val timeStr = String.format("%02d:%02d", mins, secs)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StatusPurple.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏳ Soundscape stopt automatisch over: ",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                    Text(
                        text = timeStr,
                        color = StatusPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
            
            // Volume slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = "Volume", tint = TextMuted, modifier = Modifier.size(14.dp))
                Slider(
                    value = soundVolume,
                    onValueChange = { soundVolume = it },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = StatusPurple,
                        inactiveTrackColor = DarkBorder,
                        thumbColor = StatusPurple
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${soundVolume.toInt()}%",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.width(32.dp)
                )
            }
        }
    }
}

@Composable
fun HearingHealthCard() {
    var monitorAmbient by remember { mutableStateOf(true) }
    var simulatedDecibel by remember { mutableStateOf(65f) } // 30dB - 110dB
    
    // Smoothly jitter the decibel level if active to make it feel "real-time" and responsive!
    LaunchedEffect(monitorAmbient) {
        while (monitorAmbient) {
            delay(1500)
            val jitter = (-4..4).random().toFloat()
            simulatedDecibel = (simulatedDecibel + jitter).coerceIn(45f, 95f)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkPanel, shape = RoundedCornerShape(12.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
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
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Gehoorbescherming",
                        tint = StatusYellow,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Gehoorbescherming & Statistieken",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Houd je gehoorlimiet in de gaten",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
                
                // Monitor toggle
                Switch(
                    checked = monitorAmbient,
                    onCheckedChange = { monitorAmbient = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = StatusYellow,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkBorder
                    ),
                    modifier = Modifier.scale(0.8f).testTag("switch_monitor_ambient")
                )
            }
            
            Text(
                text = "Blootstelling aan geluid boven 85 dB gedurende meer dan 8 uur per dag kan op termijn leiden tot gehoorschade. De TAH6519 bewaakt je blootstelling in real-time.",
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            
            // Decibel Indicator Gauge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBg, shape = RoundedCornerShape(10.dp))
                    .border(1.dp, DarkBorder, shape = RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val currentDb = if (monitorAmbient) simulatedDecibel.toInt() else 0
                Box(
                    modifier = Modifier.size(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Background track
                        drawArc(
                            color = Color.White.copy(alpha = 0.06f),
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )
                        
                        if (currentDb > 0) {
                            val ratio = ((currentDb - 30).toFloat() / 80f).coerceIn(0f, 1f)
                            val color = when {
                                currentDb < 75 -> StatusSuccess
                                currentDb < 85 -> StatusYellow
                                else -> StatusDanger
                            }
                            drawArc(
                                color = color,
                                startAngle = 135f,
                                sweepAngle = 270f * ratio,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (currentDb > 0) "$currentDb" else "---",
                            color = if (currentDb == 0) TextMuted else if (currentDb < 75) StatusSuccess else if (currentDb < 85) StatusYellow else StatusDanger,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        Text(
                            text = "dB SPL",
                            color = TextMuted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (!monitorAmbient) "Real-time meting uitgeschakeld" else when {
                            currentDb < 75 -> "Veilig luisterniveau"
                            currentDb < 85 -> "Matig luid geluidsniveau"
                            else -> "Kritiek luid geluid!"
                        },
                        color = if (!monitorAmbient) TextMuted else if (currentDb < 75) StatusSuccess else if (currentDb < 85) StatusYellow else StatusDanger,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (!monitorAmbient) "Zet de schakelaar hierboven aan om de actieve decibelbelasting te meten." else when {
                            currentDb < 75 -> "Je kunt onbeperkt luisteren op dit veilige volumeniveau."
                            currentDb < 85 -> "Luister maximaal 8 uur per dag op dit niveau."
                            else -> "Pas op! Beperk luisteren tot maximaal 2 uur per dag om je gehoor te beschermen."
                        },
                        color = TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
            
            // Weekly Noise Exposure Bar Chart on Canvas
            Text(
                text = "GEMIDDELDE WEKELIJKSE BLOOTSTELLING (dB)",
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(DarkBg, shape = RoundedCornerShape(8.dp))
                    .border(1.dp, DarkBorder, shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                val weeklyDbData = listOf(62f, 68f, 74f, 86f, 71f, 65f, 60f)
                val days = listOf("Ma", "Di", "Wo", "Do", "Vr", "Za", "Zo")
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    val maxDb = 100f
                    val limitLineY = h * (1f - (85f / maxDb))
                    
                    drawLine(
                        color = StatusDanger.copy(alpha = 0.3f),
                        start = Offset(0f, limitLineY),
                        end = Offset(w, limitLineY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                    
                    val barCount = weeklyDbData.size
                    val spacing = w / (barCount + 1)
                    val barWidth = 12.dp.toPx()
                    
                    weeklyDbData.forEachIndexed { i, dbVal ->
                        val x = spacing * (i + 1)
                        val ratio = dbVal / maxDb
                        val barHeight = h * ratio * 0.75f
                        val color = if (dbVal >= 85f) StatusDanger else if (dbVal >= 70f) StatusYellow else StatusSuccess
                        
                        drawRoundRect(
                            color = color.copy(alpha = 0.85f),
                            topLeft = Offset(x - barWidth / 2f, h - 16.dp.toPx() - barHeight),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    days.forEachIndexed { i, day ->
                        val dbValue = weeklyDbData[i].toInt()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${dbValue}dB",
                                color = if (dbValue >= 85) StatusDanger else TextMuted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = day,
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FirmwareVersionCard(viewModel: HeadphoneViewModel) {
    val firmwareVersion by viewModel.firmwareVersion.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkPanel, shape = RoundedCornerShape(12.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Firmware",
                        tint = HighlightSky,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Firmwarebeheer",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Huidige versie: $firmwareVersion",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Small badge showing status
                Box(
                    modifier = Modifier
                        .background(
                            if (firmwareVersion == "v1.5.0") StatusSuccess.copy(alpha = 0.1f) else StatusYellow.copy(alpha = 0.1f),
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            1.dp,
                            if (firmwareVersion == "v1.5.0") StatusSuccess.copy(alpha = 0.3f) else StatusYellow.copy(alpha = 0.3f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (firmwareVersion == "v1.5.0") "Up-to-date" else "Update beschikbaar",
                        color = if (firmwareVersion == "v1.5.0") StatusSuccess else StatusYellow,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = DarkBorder)

            when (val state = updateState) {
                is UpdateState.Idle -> {
                    Text(
                        text = "Houd je Philips TAH6519 up-to-date met de nieuwste audio-algoritmen en Bluetooth-stabiliteitsverbeteringen.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Button(
                        onClick = { viewModel.checkForUpdates() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("btn_check_firmware_updates")
                    ) {
                        Text(
                            text = "Controleer op updates",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                is UpdateState.Checking -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = HighlightSky,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = "Zoeken naar firmware-updates...",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                is UpdateState.UpToDate -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StatusSuccess.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .border(1.dp, StatusSuccess.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Up to date",
                                tint = StatusSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Je Philips TAH6519 beschikt over de nieuwste firmware ($firmwareVersion).",
                                color = StatusSuccess,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { viewModel.resetUpdateState() },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                        ) {
                            Text(text = "Sluiten", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is UpdateState.UpdateAvailable -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StatusYellow.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .border(1.dp, StatusYellow.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Update available",
                                tint = StatusYellow,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Nieuwe versie beschikbaar: ${state.version}",
                                color = StatusYellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Changelog details
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBg, shape = RoundedCornerShape(8.dp))
                                .border(1.dp, DarkBorder, shape = RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "WAT IS ER NIEUW IN ${state.version}:",
                                    color = HighlightSky,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                state.changelog.forEach { bullet ->
                                    Text(
                                        text = bullet,
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.resetUpdateState() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextMuted),
                                border = BorderStroke(1.dp, DarkBorder),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Later", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.startUpdate() },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.5f).testTag("btn_install_firmware_update")
                            ) {
                                Text("Nu installeren", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                is UpdateState.Updating -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.statusMessage,
                                color = HighlightSky,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${(state.progress * 100).toInt()}%",
                                color = AccentPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }

                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = AccentPrimary,
                            trackColor = DarkBorder
                        )

                        Text(
                            text = "Zorg ervoor dat je koptelefoon aan blijft staan en dicht bij je telefoon blijft liggen.",
                            color = TextMuted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                is UpdateState.UpdateComplete -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Voltooid",
                            tint = StatusSuccess,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Update Succesvol Voltooid!",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Je Philips TAH6519 is nu geüpdatet naar firmware ${state.newVersion}. Geniet van verbeterde geluidsprestaties en stabiliteit!",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )

                        Button(
                            onClick = { viewModel.resetUpdateState() },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("btn_complete_firmware_update")
                        ) {
                            Text("Geweldig", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// NEW PREMIUM DASHBOARD COMPOSABLES
// ==========================================

@Composable
fun DashboardHeroCard(settings: HeadphoneSettings, isCharging: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkPanel, DarkBg)
                )
            )
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(16.dp))
            .testTag("dashboard_hero_card")
    ) {
        // High-tech sound wave background drawing
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height * 0.5f
            val barCount = 45
            val gap = 6f
            val barWidth = (width - (barCount - 1) * gap) / barCount

            for (i in 0 until barCount) {
                // Compute sine wave amplitude with pulse scale
                val x = i * (barWidth + gap) + barWidth / 2f
                val distanceToCenter = kotlin.math.abs(x - width / 2f)
                val factor = (1f - (distanceToCenter / (width / 2f))).coerceIn(0f, 1f)
                
                // Add some variation based on index
                val waveHeight = (25.dp.toPx() + kotlin.math.sin(i * 0.4f) * 15.dp.toPx()) * factor * pulseScale
                
                val startY = centerY - waveHeight / 2f
                val endY = centerY + waveHeight / 2f
                
                // Color gradient depending on index
                val color = when {
                    i % 3 == 0 -> AccentPrimary.copy(alpha = 0.4f)
                    i % 3 == 1 -> HighlightSky.copy(alpha = 0.4f)
                    else -> StatusPurple.copy(alpha = 0.4f)
                }
                
                drawLine(
                    color = color,
                    start = Offset(x, startY),
                    end = Offset(x, endY),
                    strokeWidth = barWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }

        // Overlay content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (settings.connected) StatusSuccess else StatusDanger, shape = CircleShape)
                        )
                        Text(
                            text = if (settings.connected) "VERBONDEN" else "ONTKOPPELD",
                            color = if (settings.connected) StatusSuccess else StatusDanger,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "Philips TAH6519",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Huidige preset: ${settings.activePreset ?: "Handmatig"}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                // Battery Badge
                MiniBatteryIndicator(
                    batteryLevel = settings.batteryLevel,
                    isCharging = isCharging
                )
            }

            // Bottom stats pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ANC status indicator
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DarkCard.copy(alpha = 0.8f), shape = RoundedCornerShape(10.dp))
                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(AccentPrimary.copy(alpha = 0.15f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "ANC",
                                tint = AccentPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Column {
                            Text("Ruisonderdrukking", color = TextMuted, fontSize = 9.sp)
                            Text(
                                text = when (settings.ancMode) {
                                    "ON" -> "Aan (56dB)"
                                    "TRANSPARENCY" -> "Ambient"
                                    else -> "Uitgeschakeld"
                                },
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Codec status indicator
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DarkCard.copy(alpha = 0.8f), shape = RoundedCornerShape(10.dp))
                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(HighlightSky.copy(alpha = 0.15f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = "Codec",
                                tint = HighlightSky,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Column {
                            Text("Audio Codec", color = TextMuted, fontSize = 9.sp)
                            Text(
                                text = if (settings.ldacEnabled) "Hi-Res LDAC" else "Standaard SBC",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardMediaWidget(viewModel: HeadphoneViewModel, settings: HeadphoneSettings) {
    val isPlaying by viewModel.mediaIsPlaying.collectAsStateWithLifecycle()
    val trackProgressSecs by viewModel.mediaProgress.collectAsStateWithLifecycle()
    val totalDurationSecs by viewModel.mediaDuration.collectAsStateWithLifecycle()
    val trackName by viewModel.mediaTrackName.collectAsStateWithLifecycle()
    val trackArtist by viewModel.mediaTrackArtist.collectAsStateWithLifecycle()

    // Pulse/rotation for album art
    val infiniteTransition = rememberInfiniteTransition(label = "disc_spin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val minutes = trackProgressSecs / 60
    val seconds = trackProgressSecs % 60
    val progressFraction = trackProgressSecs.toFloat() / totalDurationSecs.toFloat().coerceAtLeast(1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkPanel, shape = RoundedCornerShape(14.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag("dashboard_media_widget")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Player core
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Spinning vinyl album art placeholder
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F1B2C))
                        .graphicsLayer {
                            if (isPlaying) {
                                rotationZ = rotationAngle
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Draw outer grooved CD canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF1F4A7C), Color(0xFF0A101C))
                            ),
                            radius = size.width * 0.48f
                        )
                        // Grooves
                        drawCircle(color = Color.White.copy(alpha = 0.08f), radius = size.width * 0.38f, style = Stroke(width = 1f))
                        drawCircle(color = Color.White.copy(alpha = 0.08f), radius = size.width * 0.28f, style = Stroke(width = 1f))
                        drawCircle(color = Color.White.copy(alpha = 0.08f), radius = size.width * 0.18f, style = Stroke(width = 1f))
                    }
                    // Inner colored sticker
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(AccentPrimary, HighlightSky)
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(DarkPanel, shape = CircleShape)
                        )
                    }
                }

                // Song Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = trackName,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .background(StatusPurple.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("LDAC", color = StatusPurple, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = trackArtist,
                        color = TextMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                // Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.seekMedia(0) },
                        modifier = Modifier.size(32.dp).testTag("media_prev_btn")
                    ) {
                        Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Vorig nummer", tint = TextPrimary)
                    }
                    
                    IconButton(
                        onClick = { viewModel.toggleMediaPlayer() },
                        modifier = Modifier
                            .size(38.dp)
                            .background(AccentPrimary, shape = CircleShape)
                            .testTag("media_play_btn")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pauzeren" else "Afspelen",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = { viewModel.playProceduralTone() },
                        modifier = Modifier.size(32.dp).testTag("media_next_btn")
                    ) {
                        Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Volgend nummer (Ruis)", tint = TextPrimary)
                    }
                }
            }

            // Progress bar and times
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = AccentPrimary,
                    trackColor = DarkBorder
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format("%d:%02d", minutes, seconds),
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                    Text(
                        text = String.format("%d:%02d", totalDurationSecs / 60, totalDurationSecs % 60),
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 2.dp))

            // Quick Boost Controls inside card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick Bass Boost Toggle
                Button(
                    onClick = { viewModel.toggleDynamicBass(!settings.dynamicBassEnabled) },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("quick_toggle_bass"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (settings.dynamicBassEnabled) StatusDanger.copy(alpha = 0.15f) else Color.Transparent,
                        contentColor = if (settings.dynamicBassEnabled) StatusDanger else TextMuted
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (settings.dynamicBassEnabled) StatusDanger else DarkBorder
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (settings.dynamicBassEnabled) Icons.Default.CheckCircle else Icons.Default.Hearing,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("Dynamic Bass", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Quick Surround Toggle
                Button(
                    onClick = { viewModel.toggleSurround(!settings.surroundSoundEnabled) },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("quick_toggle_spatial"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (settings.surroundSoundEnabled) StatusPurple.copy(alpha = 0.15f) else Color.Transparent,
                        contentColor = if (settings.surroundSoundEnabled) StatusPurple else TextMuted
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (settings.surroundSoundEnabled) StatusPurple else DarkBorder
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (settings.surroundSoundEnabled) Icons.Default.CheckCircle else Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("Surround Sound", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardQuickControls(viewModel: HeadphoneViewModel, settings: HeadphoneSettings) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkPanel, shape = RoundedCornerShape(14.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag("dashboard_quick_controls")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // ANC modes quick picker
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Snelbeheer Ruisonderdrukking",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val ancModes = listOf(
                        Triple("ON", "ANC Actief", Icons.Default.GraphicEq),
                        Triple("TRANSPARENCY", "Omgevingsgeluid", Icons.Default.Hearing),
                        Triple("OFF", "Uit", Icons.Default.Close)
                    )
                    ancModes.forEach { (modeCode, label, icon) ->
                        val isSelected = settings.ancMode == modeCode
                        Button(
                            onClick = { viewModel.setAncMode(modeCode) },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("quick_anc_mode_$modeCode"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) AccentPrimary else Color.Transparent,
                                contentColor = if (isSelected) Color.White else TextMuted
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) AccentPrimary else DarkBorder
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(12.dp))
                                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 2.dp))

            // Quick Equalizer presets
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Snelbeheer EQ Presets",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickPresets = listOf("Philips Signature", "Dynamic Bass", "Vocal Clarity", "Flat")
                    quickPresets.forEach { preset ->
                        val isSelected = settings.activePreset == preset
                        Button(
                            onClick = { viewModel.setPreset(preset) },
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .testTag("quick_preset_$preset"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) HighlightSky.copy(alpha = 0.2f) else DarkBg,
                                contentColor = if (isSelected) HighlightSky else TextMuted
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) HighlightSky else DarkBorder
                            ),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = when(preset) {
                                    "Philips Signature" -> "Signature"
                                    "Dynamic Bass" -> "Bass"
                                    "Vocal Clarity" -> "Vocal"
                                    else -> "Flat"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardSoundSafetyMeter(viewModel: HeadphoneViewModel) {
    var volumeInput by remember { mutableStateOf(70f) }
    val dbaVal = (50f + (volumeInput / 100f) * 52f).toInt() // range 50 - 102 dBA

    val ambientDecibel by viewModel.ambientDecibel.collectAsStateWithLifecycle()
    val isRecordingNoise by viewModel.isRecordingNoise.collectAsStateWithLifecycle()

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.startNoiseMonitoring()
        } else {
            viewModel.startNoiseMonitoring() // fallback to realistic simulator if permission is denied
        }
    }

    // Safe listening calculation
    val (safetyLabel, safetyColor, safetyTime) = when {
        dbaVal < 80 -> Triple("VEILIG (Groen)", StatusSuccess, "Onbeperkt veilig luisteren")
        dbaVal < 85 -> Triple("MATIG (Geel)", StatusYellow, "Tot 8 uur per dag veilig")
        dbaVal < 90 -> Triple("HOOG RISICO (Oranje)", StatusOrange, "Tot 2.5 uur per dag veilig")
        dbaVal < 95 -> Triple("GEVAARLIJK (Rood)", StatusDanger, "Tot 45 minuten per dag veilig")
        else -> Triple("EXTREEM (Rood)", StatusDanger, "Slechts 15 minuten per dag veilig!")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkPanel, shape = RoundedCornerShape(14.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag("dashboard_safety_meter")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Gehoorbescherming & Safe Decibels",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Realtime decibel analyse van je gehoor",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(safetyColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                        .border(1.dp, safetyColor, shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$dbaVal dBA",
                        color = safetyColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            // Visual Arc gauge representing dBA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(150.dp, 80.dp)) {
                    val width = size.width
                    val height = size.height
                    val strokeW = 10.dp.toPx()

                    // Draw safety zones
                    // Green zone (50 - 80 dBA) -> 180 deg to 280 deg
                    drawArc(
                        color = StatusSuccess,
                        startAngle = 180f,
                        sweepAngle = 100f,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                        size = androidx.compose.ui.geometry.Size(width - strokeW, height * 2f - strokeW),
                        topLeft = Offset(strokeW / 2f, strokeW / 2f)
                    )
                    // Orange zone (80 - 95 dBA) -> 280 deg to 330 deg
                    drawArc(
                        color = StatusOrange,
                        startAngle = 280f,
                        sweepAngle = 50f,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                        size = androidx.compose.ui.geometry.Size(width - strokeW, height * 2f - strokeW),
                        topLeft = Offset(strokeW / 2f, strokeW / 2f)
                    )
                    // Red zone (95 - 102 dBA) -> 330 deg to 360 deg
                    drawArc(
                        color = StatusDanger,
                        startAngle = 330f,
                        sweepAngle = 30f,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                        size = androidx.compose.ui.geometry.Size(width - strokeW, height * 2f - strokeW),
                        topLeft = Offset(strokeW / 2f, strokeW / 2f)
                    )

                    // Draw dial pointer indicator
                    val sweepProgress = ((dbaVal - 50) / 52f).coerceIn(0f, 1f)
                    val pointerAngleDeg = 180f + (sweepProgress * 180f)
                    val pointerAngleRad = Math.toRadians(pointerAngleDeg.toDouble())
                    val radius = (width - strokeW) / 2f
                    val centerX = width / 2f
                    val centerY = height

                    val endX = centerX + radius * kotlin.math.cos(pointerAngleRad).toFloat()
                    val endY = centerY + radius * kotlin.math.sin(pointerAngleRad).toFloat()

                    drawLine(
                        color = TextPrimary,
                        start = Offset(centerX, centerY),
                        end = Offset(endX, endY),
                        strokeWidth = 3.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    drawCircle(color = TextPrimary, radius = 6.dp.toPx())
                    drawCircle(color = DarkPanel, radius = 3.dp.toPx())
                }

                Column(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = safetyLabel,
                        color = safetyColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = safetyTime,
                        color = TextMuted,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }

            // Interactive Volume Slider to simulate
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Simuleer Afspeelvolume", color = TextMuted, fontSize = 11.sp)
                    Text(text = "${volumeInput.toInt()}%", color = AccentPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = volumeInput,
                    onValueChange = { volumeInput = it },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = safetyColor,
                        activeTrackColor = safetyColor,
                        inactiveTrackColor = DarkBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("safety_volume_slider")
                )
            }

            // Ambient background noise real microphone display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBg, shape = RoundedCornerShape(8.dp))
                    .clickable {
                        if (isRecordingNoise) {
                            viewModel.stopNoiseMonitoring()
                        } else {
                            recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    }
                    .padding(10.dp)
            ) {
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
                            imageVector = Icons.Default.Hearing,
                            contentDescription = null,
                            tint = if (isRecordingNoise) AccentPrimary else HighlightSky,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isRecordingNoise) "Omgevingsgeluid Microfoon (Live)" else "Start Live Omgevingsmeter",
                            color = TextPrimary,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = if (isRecordingNoise) {
                            val category = when {
                                ambientDecibel < 50 -> "Rustig"
                                ambientDecibel < 65 -> "Normaal"
                                ambientDecibel < 80 -> "Luidruchtig"
                                else -> "Risicovol!"
                            }
                            String.format("%.1f dBA (%s)", ambientDecibel, category)
                        } else {
                            "Klik om te meten"
                        },
                        color = if (isRecordingNoise) (if (ambientDecibel >= 80) StatusDanger else StatusSuccess) else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardStatsTracker() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkPanel, shape = RoundedCornerShape(14.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag("dashboard_stats_tracker")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Luisterstatistieken & Activiteit",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Wekelijkse draagtijd analyse",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
                
                // Streak badge
                Row(
                    modifier = Modifier
                        .background(StatusOrange.copy(alpha = 0.15f), shape = RoundedCornerShape(16.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🔥", fontSize = 11.sp)
                    Text("5 dagen safe streak!", color = StatusOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Stats row (Today, Weekly average, Exposure)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("Draagtijd Vandaag", "3.2 uur", "Budget: 80%"),
                    Triple("Wekelijks Totaal", "18.5 uur", "Gem. 2.6u/dag"),
                    Triple("Safe Exposure", "94%", "Optimale score")
                ).forEach { (lbl, valStr, sub) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(DarkBg, shape = RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(lbl, color = TextMuted, fontSize = 8.sp, textAlign = TextAlign.Center)
                        Text(valStr, color = AccentPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 2.dp))
                        Text(sub, color = StatusSuccess, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Weekly bar chart (Ma, Di, Wo, Do, Vr, Za, Zo)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Draagtijd per day (uren)", color = TextMuted, fontSize = 10.sp)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val days = listOf("Ma", "Di", "Wo", "Do", "Vr", "Za", "Zo")
                    val heights = listOf(0.4f, 0.7f, 0.5f, 0.8f, 0.6f, 0.9f, 0.3f) // custom heights
                    
                    days.forEachIndexed { i, day ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            val barHeight = heights[i]
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(barHeight)
                                    .width(16.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(AccentPrimary, HighlightSky)
                                        ),
                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                            Text(day, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardLocatorCard(viewModel: HeadphoneViewModel) {
    val isScanningBluetooth by viewModel.isScanningBluetooth.collectAsStateWithLifecycle()
    val scannedDevices by viewModel.scannedDevices.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val bluetoothPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions: Map<String, Boolean> ->
        val granted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            permissions[android.Manifest.permission.BLUETOOTH_SCAN] == true
        } else {
            permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        }
        if (granted) {
            viewModel.startBluetoothScan()
        } else {
            viewModel.startBluetoothScan() // gracefully runs simulation fallback
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkPanel, shape = RoundedCornerShape(14.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag("dashboard_locator_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Zoek Mijn Koptelefoon (Radar)",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )

            if (isScanningBluetooth) {
                // Radar grid sweep view
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color(0xFF101C1A), shape = CircleShape)
                            .border(1.dp, StatusSuccess.copy(alpha = 0.3f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val cx = w / 2f
                            val cy = h / 2f

                            // Concentric radar circles
                            drawCircle(color = StatusSuccess.copy(alpha = 0.1f), radius = w * 0.4f, style = Stroke(width = 1f))
                            drawCircle(color = StatusSuccess.copy(alpha = 0.1f), radius = w * 0.25f, style = Stroke(width = 1f))
                            drawCircle(color = StatusSuccess.copy(alpha = 0.15f), radius = w * 0.1f, style = Stroke(width = 1f))
                            
                            // Cross lines
                            drawLine(color = StatusSuccess.copy(alpha = 0.1f), start = Offset(0f, cy), end = Offset(w, cy))
                            drawLine(color = StatusSuccess.copy(alpha = 0.1f), start = Offset(cx, 0f), end = Offset(cx, h))

                            // Rotating sweep line
                            val angleRad = Math.toRadians(sweepAngle.toDouble())
                            val endX = cx + (w * 0.5f) * kotlin.math.cos(angleRad).toFloat()
                            val endY = cy + (h * 0.5f) * kotlin.math.sin(angleRad).toFloat()

                            drawLine(
                                color = StatusSuccess,
                                start = Offset(cx, cy),
                                end = Offset(endX, endY),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
                    Text(
                        text = "Bluetooth signaal scannen...",
                        color = StatusSuccess,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(StatusSuccess.copy(alpha = 0.15f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = StatusSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        val hasHeadphone = scannedDevices.any { it.isHeadphone }
                        Text(
                            text = if (hasHeadphone) "Philips TAH6519 Gevonden!" else "Koptelefoon binnen handbereik?",
                            color = if (hasHeadphone) StatusSuccess else TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = if (hasHeadphone) {
                                val closest = scannedDevices.filter { it.isHeadphone }.maxByOrNull { it.rssi }
                                val distance = if ((closest?.rssi ?: -100) > -65) "~1.5m" else "~6.5m"
                                "Dichtbij gevonden met signaalsterkte ${closest?.rssi} dBm ($distance)"
                            } else {
                                "Laat de TAH6519 een geluidsignaal afspelen om hem snel te vinden."
                            },
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if (scannedDevices.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg, shape = RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Gedetecteerde Bluetooth Apparaten:",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    scannedDevices.take(4).forEach { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (device.isHeadphone) StatusSuccess.copy(alpha = 0.08f) else Color.Transparent, shape = RoundedCornerShape(4.dp))
                                .clickable {
                                    viewModel.connectDevice(device.address)
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (device.isHeadphone) Icons.Default.Headphones else Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    tint = if (device.isHeadphone) StatusSuccess else TextMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = device.name,
                                    color = if (device.isHeadphone) StatusSuccess else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (device.isHeadphone) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 160.dp)
                                )
                            }
                            Text(
                                text = "${device.rssi} dBm",
                                color = if (device.isHeadphone) StatusSuccess else TextMuted,
                                fontSize = 10.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (!isScanningBluetooth) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            bluetoothPermissionsLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.BLUETOOTH_SCAN,
                                    android.Manifest.permission.BLUETOOTH_CONNECT
                                )
                            )
                        } else {
                            bluetoothPermissionsLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    } else {
                        viewModel.stopBluetoothScan()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("locator_search_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanningBluetooth) StatusSuccess.copy(alpha = 0.15f) else StatusSuccess,
                    contentColor = if (isScanningBluetooth) StatusSuccess else Color(0xFF003828)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isScanningBluetooth) "Scannen stopzetten" else "Koptelefoon opsporen",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BluetoothStatusIndicatorCard(
    viewModel: HeadphoneViewModel,
    settings: HeadphoneSettings,
    modifier: Modifier = Modifier
) {
    val isConnecting by viewModel.isConnecting.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanningBluetooth.collectAsStateWithLifecycle()
    val isAutoReconnecting by viewModel.isAutoReconnecting.collectAsStateWithLifecycle()
    val reconnectAttempts by viewModel.reconnectAttempts.collectAsStateWithLifecycle()
    val autoReconnectEnabled by viewModel.autoReconnectEnabled.collectAsStateWithLifecycle()
    val scannedDevices by viewModel.scannedDevices.collectAsStateWithLifecycle()
    val isSimulationMode by viewModel.isSimulationMode.collectAsStateWithLifecycle()

    // Pulse animation for the glowing ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_bluetooth")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val pulseRadiusFloat by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_radius"
    )
    val pulseRadius = pulseRadiusFloat.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkPanel, shape = RoundedCornerShape(14.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(14.dp))
            .padding(16.dp)
            .testTag("bluetooth_connection_status_indicator")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Simulation Mode Quick Switch Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBg, shape = RoundedCornerShape(10.dp))
                    .border(1.dp, DarkBorder.copy(alpha = 0.5f), shape = RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (isSimulationMode) StatusYellow else HighlightSky, shape = CircleShape)
                    )
                    Column {
                        Text(
                            text = if (isSimulationMode) "Simulatiemodus (Virtueel/Demo)" else "Echte Bluetooth Modus",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isSimulationMode) "Muziek & knoppen werken als simulator" else "Verbindt direct met echte koptelefoon",
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }
                Switch(
                    checked = isSimulationMode,
                    onCheckedChange = { viewModel.toggleSimulationMode(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = StatusYellow,
                        checkedTrackColor = StatusYellow.copy(alpha = 0.3f),
                        uncheckedThumbColor = HighlightSky,
                        uncheckedTrackColor = DarkCard
                    ),
                    modifier = Modifier.scale(0.8f).testTag("simulation_mode_switch")
                )
            }

            // Header with title and status badge
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
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = "Bluetooth Status",
                        tint = if (settings.connected) AccentPrimary else if (isAutoReconnecting) StatusPurple else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Bluetooth Verbindingsstatus",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Small pill-shaped status indicator
                Box(
                    modifier = Modifier
                        .background(
                            color = if (settings.connected) StatusSuccess.copy(alpha = 0.15f) 
                                    else if (isAutoReconnecting) StatusPurple.copy(alpha = 0.15f)
                                    else if (isConnecting) StatusYellow.copy(alpha = 0.15f)
                                    else StatusDanger.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(100.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (settings.connected) "VERBONDEN" 
                               else if (isAutoReconnecting) "HERSTELLEN (${reconnectAttempts}/3)..."
                               else if (isConnecting) "VERBINDEN..." 
                               else "VERBROKEN",
                        color = if (settings.connected) StatusSuccess 
                                else if (isAutoReconnecting) StatusPurple
                                else if (isConnecting) StatusYellow 
                                else StatusDanger,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Main info section (Icon, Name, details)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Pulse Glowing Icon
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .shadow(
                            elevation = if (settings.connected || isConnecting || isAutoReconnecting) pulseRadius else 0.dp,
                            shape = CircleShape,
                            ambientColor = if (settings.connected) StatusSuccess else if (isAutoReconnecting) StatusPurple else StatusYellow,
                            spotColor = if (settings.connected) StatusSuccess else if (isAutoReconnecting) StatusPurple else StatusYellow
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer ring
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = if (settings.connected) StatusSuccess.copy(alpha = 0.05f + pulseAlpha * 0.05f)
                                        else if (isAutoReconnecting) StatusPurple.copy(alpha = 0.05f + pulseAlpha * 0.05f)
                                        else if (isConnecting) StatusYellow.copy(alpha = 0.05f + pulseAlpha * 0.05f)
                                        else Color.Transparent,
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = if (settings.connected) StatusSuccess.copy(alpha = pulseAlpha * 0.4f)
                                        else if (isAutoReconnecting) StatusPurple.copy(alpha = pulseAlpha * 0.4f)
                                        else if (isConnecting) StatusYellow.copy(alpha = pulseAlpha * 0.4f)
                                        else DarkBorder,
                                shape = CircleShape
                            )
                    )

                    // Inner circle
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = if (settings.connected) StatusSuccess.copy(alpha = 0.15f)
                                        else if (isAutoReconnecting) StatusPurple.copy(alpha = 0.15f)
                                        else if (isConnecting) StatusYellow.copy(alpha = 0.15f)
                                        else DarkCard,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (settings.connected) Icons.Default.Bluetooth
                                         else if (isAutoReconnecting) Icons.Default.Sync
                                         else if (isConnecting) Icons.Default.Bluetooth
                                         else Icons.Default.BluetoothDisabled,
                            contentDescription = null,
                            tint = if (settings.connected) StatusSuccess
                                   else if (isAutoReconnecting) StatusPurple
                                   else if (isConnecting) StatusYellow
                                   else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Text detail column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (settings.connected) "Philips TAH6519 Over-Ear" 
                               else if (isAutoReconnecting) "Verbinding herstellen..." 
                               else if (isConnecting) "Verbinding maken..." 
                               else "Koptelefoon stand-by",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = if (settings.connected) "Signaal: Uitstekend (-52 dBm) · Multipoint"
                               else if (isAutoReconnecting) "Spoorloos verloren. Automatische poging ${reconnectAttempts} van 3..."
                               else if (isConnecting) "Koppelen via Bluetooth LE..."
                               else "Schakel de koptelefoon in om verbinding te maken",
                        color = if (isAutoReconnecting) StatusPurple else TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // Quick Info Chips (Battery, Audio Quality, Bluetooth Version)
            if (settings.connected) {
                HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Battery Chip
                    Row(
                        modifier = Modifier
                            .background(DarkCard, shape = RoundedCornerShape(8.dp))
                            .border(1.dp, DarkBorder.copy(alpha = 0.8f), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = StatusSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Accu: ${settings.batteryLevel}%",
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Audio Codec Chip
                    Row(
                        modifier = Modifier
                            .background(DarkCard, shape = RoundedCornerShape(8.dp))
                            .border(1.dp, DarkBorder.copy(alpha = 0.8f), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = HighlightSky,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (settings.ldacEnabled) "Codec: LDAC" else "Codec: SBC",
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Signal Strength Chip
                    Row(
                        modifier = Modifier
                            .background(DarkCard, shape = RoundedCornerShape(8.dp))
                            .border(1.dp, DarkBorder.copy(alpha = 0.8f), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = StatusPurple,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "BT 5.4 LE",
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(color = DarkBorder.copy(alpha = 0.3f))

            // Auto-Reconnect Option Row (Switch Toggle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Automatisch opnieuw verbinden",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Probeer automatisch opnieuw verbinding te maken bij onverwacht signaalverlies.",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = autoReconnectEnabled,
                    onCheckedChange = { viewModel.toggleAutoReconnect(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AccentPrimary,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkCard
                    ),
                    modifier = Modifier.scale(0.8f).testTag("auto_reconnect_switch")
                )
            }

            // Scanned Devices List for Bluetooth Connection Management
            if (!settings.connected && (isScanning || scannedDevices.isNotEmpty())) {
                HorizontalDivider(color = DarkBorder.copy(alpha = 0.3f))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scanned_devices_container")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Beschikbare Bluetooth Apparaten",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        if (isScanning) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = HighlightSky,
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 1.2.dp
                                )
                                Text(
                                    text = "Scannen...",
                                    color = HighlightSky,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (scannedDevices.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBg, shape = RoundedCornerShape(8.dp))
                                .border(1.dp, DarkBorder.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Geen apparaten in de buurt gevonden. Klik op Zoek BT om te scannen.",
                                color = TextMuted,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            scannedDevices.forEach { device ->
                                val isThisDeviceHeadphone = device.isHeadphone || device.name.contains("TAH6519", ignoreCase = true) || device.name.contains("Philips", ignoreCase = true)
                                val itemBg = if (isThisDeviceHeadphone) AccentPrimary.copy(alpha = 0.08f) else DarkBg
                                val itemBorderColor = if (isThisDeviceHeadphone) AccentPrimary.copy(alpha = 0.3f) else DarkBorder.copy(alpha = 0.5f)
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(itemBg, shape = RoundedCornerShape(8.dp))
                                        .border(1.dp, itemBorderColor, shape = RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.connectDevice(device.address)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                        .testTag("device_item_${device.address.replace(":", "_")}"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(
                                                    if (isThisDeviceHeadphone) AccentPrimary.copy(alpha = 0.15f) else DarkPanel,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isThisDeviceHeadphone) Icons.Default.Headphones else Icons.Default.Bluetooth,
                                                contentDescription = null,
                                                tint = if (isThisDeviceHeadphone) AccentPrimary else TextMuted,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = device.name,
                                                    color = if (isThisDeviceHeadphone) AccentPrimary else TextPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isThisDeviceHeadphone) FontWeight.Bold else FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                                if (isThisDeviceHeadphone) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(AccentPrimary, shape = RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = "Premium",
                                                            color = Color.White,
                                                            fontSize = 7.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = device.address,
                                                color = TextMuted,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${device.rssi} dBm",
                                            color = if (isThisDeviceHeadphone) AccentPrimary else TextMuted,
                                            fontSize = 9.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Pair en verbind",
                                            tint = if (isThisDeviceHeadphone) AccentPrimary else TextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Actions row (Connect / Disconnect / Simulate Signal Loss)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (settings.connected) {
                    Button(
                        onClick = { viewModel.disconnectDevice() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusDanger.copy(alpha = 0.1f),
                            contentColor = StatusDanger
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("status_disconnect_button")
                    ) {
                        Text(
                            text = "Verbreek Verbinding",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { viewModel.simulateConnectionLoss() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusPurple.copy(alpha = 0.15f),
                            contentColor = StatusPurple
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, StatusPurple.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("simulate_loss_button")
                    ) {
                        Text(
                            text = "Simuleer signaalverlies",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = { viewModel.connectDevice() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isConnecting && !isAutoReconnecting,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(36.dp)
                            .testTag("status_connect_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isConnecting || isAutoReconnecting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = if (isAutoReconnecting) "Herstellen..." else if (isConnecting) "Verbinden..." else "Maak Verbinding",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Always show a "Scan" or "Demo Scan" button when disconnected to let them discover devices!
                if (!settings.connected) {
                    OutlinedButton(
                        onClick = {
                            if (isScanning) {
                                viewModel.stopBluetoothScan()
                            } else {
                                viewModel.startBluetoothScan()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = HighlightSky
                        ),
                        border = BorderStroke(1.dp, DarkBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("status_scan_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    color = HighlightSky,
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp
                                )
                                Text(
                                    text = "Zoeken...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Zoek BT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
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
fun TechnicalConnectionStatsCard(
    viewModel: HeadphoneViewModel,
    settings: HeadphoneSettings,
    modifier: Modifier = Modifier
) {
    val rssi by viewModel.rssi.collectAsStateWithLifecycle()
    val latencyMs by viewModel.latencyMs.collectAsStateWithLifecycle()
    val packetLoss by viewModel.packetLoss.collectAsStateWithLifecycle()
    val simulatedDistanceMeters by viewModel.simulatedDistanceMeters.collectAsStateWithLifecycle()
    val bitrateKbps by viewModel.bitrateKbps.collectAsStateWithLifecycle()
    val activeAudioCodec by viewModel.activeAudioCodec.collectAsStateWithLifecycle()
    val activeSampleRate by viewModel.activeSampleRate.collectAsStateWithLifecycle()
    val activeChannelMode by viewModel.activeChannelMode.collectAsStateWithLifecycle()
    val activeProtocolInfo by viewModel.activeProtocolInfo.collectAsStateWithLifecycle()
    val firmwareVersion by viewModel.firmwareVersion.collectAsStateWithLifecycle()
    val isGattReading by viewModel.isGattReading.collectAsStateWithLifecycle()
    val gattStatusMessage by viewModel.gattStatusMessage.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("technical_connection_stats_card"),
        colors = CardDefaults.cardColors(containerColor = DarkPanel),
        border = BorderStroke(1.dp, DarkBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Technische Verbindingsstatistieken",
                        tint = AccentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "Technische Verbindingsstatistieken",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Realtime audiokwaliteit & signaalanalyse",
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }
                
                // Active codec badge
                if (settings.connected) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (settings.ldacEnabled) HighlightSky.copy(alpha = 0.15f) else AccentPrimary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp,
                                if (settings.ldacEnabled) HighlightSky.copy(alpha = 0.5f) else AccentPrimary.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (settings.ldacEnabled) "LDAC HI-RES" else "SBC/AAC",
                            color = if (settings.ldacEnabled) HighlightSky else AccentPrimary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            if (!settings.connected) {
                // Disconnected State Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg, shape = RoundedCornerShape(12.dp))
                        .border(1.dp, DarkBorder.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BluetoothDisabled,
                            contentDescription = "Verbroken",
                            tint = TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Koppeling niet actief",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Verbind de TAH6519 om realtime signaal- en codecstatistieken te analyseren.",
                            color = TextMuted,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 13.sp
                        )
                    }
                }
            } else {
                // Connected State: Full Technical Stats Dashboard

                // GATT Firmware Read Panel
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg, shape = RoundedCornerShape(12.dp))
                        .border(1.dp, DarkBorder.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
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
                                imageVector = Icons.Default.Info,
                                contentDescription = "GATT Read",
                                tint = AccentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = "GATT Firmware-uitlezing",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Vraag firmware op via GATT-kenmerken",
                                    color = TextMuted,
                                    fontSize = 8.sp
                                )
                            }
                        }

                        // Display the current live firmware version retrieved
                        Box(
                            modifier = Modifier
                                .background(DarkCard, shape = RoundedCornerShape(6.dp))
                                .border(1.dp, DarkBorder, shape = RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = firmwareVersion,
                                color = HighlightSky,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("gatt_firmware_version_text")
                            )
                        }
                    }

                    if (gattStatusMessage.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkCard, shape = RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isGattReading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = AccentPrimary,
                                    strokeWidth = 1.5.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = StatusSuccess,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = gattStatusMessage,
                                color = if (gattStatusMessage.contains("Fout")) StatusDanger else TextPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.readFirmwareViaGatt() },
                        enabled = !isGattReading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("gatt_read_firmware_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPrimary,
                            contentColor = Color.White,
                            disabledContainerColor = DarkCard,
                            disabledContentColor = TextMuted
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isGattReading) {
                                Text(text = "Uitlezen...", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "GATT Read Button",
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(text = "Firmware ophalen via GATT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Dynamic Firmware Update Flow based on retrieved/compared GATT version
                    val isNewerVersionAvailable = firmwareVersion != "v1.5.0"

                    if (isNewerVersionAvailable || updateState !is UpdateState.Idle) {
                        HorizontalDivider(
                            color = DarkBorder.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        when (val state = updateState) {
                            is UpdateState.Idle -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(StatusYellow.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                        .border(1.dp, StatusYellow.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Update beschikbaar (v1.5.0)",
                                            color = StatusYellow,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Verbeter ANC-stabiliteit & audio-algoritmen.",
                                            color = TextMuted,
                                            fontSize = 8.sp
                                        )
                                    }
                                    
                                    Button(
                                        onClick = { viewModel.startUpdate() },
                                        colors = ButtonDefaults.buttonColors(containerColor = StatusYellow),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .height(28.dp)
                                            .testTag("gatt_update_now_button"),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                    ) {
                                        Text("Nu bijwerken", color = DarkBg, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            is UpdateState.Checking -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkCard, shape = RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = HighlightSky,
                                        strokeWidth = 1.5.dp
                                    )
                                    Text(
                                        text = "Controleren op nieuwe firmware...",
                                        color = TextMuted,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            is UpdateState.UpdateAvailable -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(StatusYellow.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                        .border(1.dp, StatusYellow.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Nieuwe firmware v${state.version} beschikbaar",
                                            color = StatusYellow,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Kwalitatieve audio-upgrades & stabiliteit.",
                                            color = TextMuted,
                                            fontSize = 8.sp
                                        )
                                    }
                                    
                                    Button(
                                        onClick = { viewModel.startUpdate() },
                                        colors = ButtonDefaults.buttonColors(containerColor = StatusYellow),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .height(28.dp)
                                            .testTag("gatt_update_now_btn_available"),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                    ) {
                                        Text("Nu bijwerken", color = DarkBg, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            is UpdateState.Updating -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkCard, shape = RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = state.statusMessage,
                                            color = HighlightSky,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${(state.progress * 100).toInt()}%",
                                            color = AccentPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { state.progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = AccentPrimary,
                                        trackColor = DarkBorder
                                    )
                                }
                            }
                            is UpdateState.UpdateComplete -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(StatusSuccess.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                        .border(1.dp, StatusSuccess.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Success",
                                            tint = StatusSuccess,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Update voltooid naar ${state.newVersion}!",
                                            color = StatusSuccess,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Button(
                                        onClick = { viewModel.resetUpdateState() },
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkBorder),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                    ) {
                                        Text("Sluiten", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            is UpdateState.UpToDate -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(StatusSuccess.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                        .border(1.dp, StatusSuccess.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = StatusSuccess,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Koptelefoon is up-to-date!",
                                        color = StatusSuccess,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. Distance Simulator Slider
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg, shape = RoundedCornerShape(12.dp))
                        .border(1.dp, DarkBorder.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                        .padding(12.dp)
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
                                imageVector = Icons.Default.Info,
                                contentDescription = "Distance",
                                tint = TextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Gesimuleerde Afstand",
                                color = TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "%.1f meter".format(simulatedDistanceMeters),
                            color = AccentPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Slider(
                        value = simulatedDistanceMeters,
                        onValueChange = { viewModel.setSimulatedDistance(it) },
                        valueRange = 0.5f..15.0f,
                        steps = 29, // 0.5m intervals
                        colors = SliderDefaults.colors(
                            thumbColor = AccentPrimary,
                            activeTrackColor = AccentPrimary,
                            inactiveTrackColor = DarkCard
                        ),
                        modifier = Modifier.testTag("stats_distance_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "0.5m (Dichtbij)", color = TextMuted, fontSize = 8.sp)
                        Text(
                            text = when {
                                simulatedDistanceMeters <= 3f -> "Perfecte ontvangst"
                                simulatedDistanceMeters <= 7f -> "Gemiddeld bereik"
                                simulatedDistanceMeters <= 11f -> "Zwak signaal"
                                else -> "Signaalverlies risico"
                            },
                            color = when {
                                simulatedDistanceMeters <= 3f -> StatusSuccess
                                simulatedDistanceMeters <= 7f -> StatusYellow
                                simulatedDistanceMeters <= 11f -> StatusOrange
                                else -> StatusDanger
                            },
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = "15m (Limiet)", color = TextMuted, fontSize = 8.sp)
                    }
                }

                // 2. RSSI Signal Indicator Gauge
                val rssiColor = when {
                    rssi >= -60 -> StatusSuccess
                    rssi >= -75 -> StatusYellow
                    rssi >= -85 -> StatusOrange
                    else -> StatusDanger
                }
                
                val rssiProgress = ((rssi + 100) / 60f).coerceIn(0f, 1f) // -100 to -40 range mapped to 0..1

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Signaalsterkte (RSSI)",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(rssiColor, shape = CircleShape)
                            )
                            Text(
                                text = "$rssi dBm (${
                                    when {
                                        rssi >= -60 -> "Uitstekend"
                                        rssi >= -75 -> "Goed"
                                        rssi >= -85 -> "Matig"
                                        else -> "Kritiek"
                                    }
                                })",
                                color = TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // RSSI Linear meter
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(DarkBg, shape = RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(rssiProgress)
                                .background(rssiColor, shape = RoundedCornerShape(3.dp))
                        )
                    }
                }

                HorizontalDivider(color = DarkBorder.copy(alpha = 0.4f))

                // 3. 2x2 Technical Details Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Stat Item: Audio Codec
                        TechnicalStatItem(
                            icon = Icons.Default.Audiotrack,
                            iconColor = HighlightSky,
                            label = "Codec & Formaat",
                            value = activeAudioCodec,
                            subtext = activeSampleRate
                        )

                        // Stat Item: Audio Bitrate
                        val kbpsColor = if (bitrateKbps >= 660) HighlightSky else if (bitrateKbps >= 328) AccentPrimary else StatusYellow
                        TechnicalStatItem(
                            icon = Icons.Default.Speed,
                            iconColor = kbpsColor,
                            label = "Audio Bitrate",
                            value = if (bitrateKbps > 0) "$bitrateKbps kbps" else "N/A",
                            subtext = when (bitrateKbps) {
                                990 -> "Extreem (Audiophile)"
                                660 -> "Gebalanceerd (High-Res)"
                                330 -> "Verbindingsprioriteit"
                                328 -> "Standaard Kwaliteit"
                                256 -> "AAC Compact"
                                else -> "Geen data"
                            }
                        )
                    }

                    // Right Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Stat Item: Latency
                        val latencyColor = if (latencyMs < 60) StatusSuccess else if (latencyMs < 120) StatusYellow else StatusOrange
                        TechnicalStatItem(
                            icon = Icons.Default.AccessTime,
                            iconColor = latencyColor,
                            label = "Audio Vertraging",
                            value = if (latencyMs > 0) "$latencyMs ms" else "N/A",
                            subtext = when {
                                latencyMs <= 45 -> "Kritiek laag (Gaming OK)"
                                latencyMs <= 100 -> "Laag (Video OK)"
                                else -> "Normaal (Muziek)"
                            }
                        )

                        // Stat Item: Packet Loss
                        val lossColor = if (packetLoss <= 0.01f) StatusSuccess else if (packetLoss <= 0.1f) StatusYellow else StatusDanger
                        TechnicalStatItem(
                            icon = Icons.Default.SwapVert,
                            iconColor = lossColor,
                            label = "Pakketverlies",
                            value = "%.2f%%".format(packetLoss * 100f),
                            subtext = when {
                                packetLoss <= 0.001f -> "Pristine stream"
                                packetLoss <= 0.05f -> "Foutcorrectie actief"
                                else -> "Haperingen mogelijk"
                            }
                        )
                    }
                }

                // 4. Protocol Details Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg, shape = RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Protocol info",
                        tint = AccentPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Profiel: $activeProtocolInfo · $activeChannelMode",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Signal degradation warning if distance is high
                if (rssi < -85) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StatusDanger.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
                            .border(1.dp, StatusDanger.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryAlert,
                            contentDescription = "Warning",
                            tint = StatusDanger,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Zwak Bluetooth signaal! Breng de TAH6519 dichterbij om audiostoringen te voorkomen.",
                            color = StatusDanger,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TechnicalStatItem(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBg, shape = RoundedCornerShape(10.dp))
            .border(1.dp, DarkBorder.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(iconColor.copy(alpha = 0.1f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(14.dp)
            )
        }
        Column {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtext,
                color = if (iconColor == StatusSuccess) StatusSuccess else if (iconColor == StatusDanger) StatusDanger else TextMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun DashboardSmartZonesCard(
    viewModel: HeadphoneViewModel,
    settings: HeadphoneSettings,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf("zones") } // "zones" or "activity"
    var notificationText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(notificationText) {
        if (notificationText != null) {
            delay(4000)
            notificationText = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkPanel, shape = RoundedCornerShape(14.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(14.dp))
            .padding(16.dp)
            .testTag("smart_sound_zones_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
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
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Slimme Geluidsregeling",
                        tint = AccentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Slimme Geluidsregeling",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Small badge showing active state
                val isAnyEnabled = settings.soundZonesEnabled || settings.adaptiveActivityEnabled
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isAnyEnabled) StatusSuccess.copy(alpha = 0.15f) else TextMuted.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(100.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (settings.soundZonesEnabled) "ZONES ACTIEF"
                               else if (settings.adaptiveActivityEnabled) "ADAPTIEF ACTIEF"
                               else "UITGESCHAKELD",
                        color = if (isAnyEnabled) StatusSuccess else TextMuted,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Notification Banner (if any zone changed)
            AnimatedVisibility(
                visible = notificationText != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                notificationText?.let { text ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StatusSuccess.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp))
                            .border(1.dp, StatusSuccess.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Succes",
                            tint = StatusSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = text,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Sub tabs for Zones vs Activity
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard, shape = RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { activeSubTab = "zones" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeSubTab == "zones") AccentPrimary else Color.Transparent,
                        contentColor = if (activeSubTab == "zones") Color.White else TextMuted
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = null,
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .testTag("tab_sound_zones")
                ) {
                    Text(text = "Geluidszones", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { activeSubTab = "activity" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeSubTab == "activity") AccentPrimary else Color.Transparent,
                        contentColor = if (activeSubTab == "activity") Color.White else TextMuted
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = null,
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .testTag("tab_adaptive_activity")
                ) {
                    Text(text = "Adaptieve Activiteit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (activeSubTab == "zones") {
                // SOUND ZONES PANEL
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Geluidszones inschakelen",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Wissel automatisch van geluidsinstellingen op basis van waar je bent.",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = settings.soundZonesEnabled,
                            onCheckedChange = { viewModel.toggleSoundZones(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentPrimary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkCard
                            ),
                            modifier = Modifier.scale(0.8f).testTag("switch_sound_zones")
                        )
                    }

                    HorizontalDivider(color = DarkBorder.copy(alpha = 0.3f))

                    // List of configured zones
                    Text(
                        text = "Geconfigureerde Zones",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val zones = listOf(
                        Triple("Thuis", "ANC Uit · Dynamic Bass", Icons.Default.Home),
                        Triple("Kantoor", "Omgevingsgeluid · Vocal Clarity", Icons.Default.Laptop),
                        Triple("Sportschool", "ANC Aan · Dynamic Bass", Icons.Default.FlashOn),
                        Triple("Trein", "ANC Aan · Philips Signature", Icons.Default.SwapVert)
                    )

                    zones.forEach { (name, desc, icon) ->
                        val isCurrentZone = settings.soundZonesEnabled && settings.activeSoundZone == name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isCurrentZone) AccentPrimary.copy(alpha = 0.1f) else DarkCard,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isCurrentZone) AccentPrimary.copy(alpha = 0.4f) else DarkBorder.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        color = if (isCurrentZone) AccentPrimary.copy(alpha = 0.15f) else DarkBorder.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isCurrentZone) AccentPrimary else TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = desc,
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                            if (isCurrentZone) {
                                Text(
                                    text = "ACTIEF",
                                    color = AccentPrimary,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Mock Simulator
                    if (settings.soundZonesEnabled) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkCard, shape = RoundedCornerShape(10.dp))
                                .border(1.dp, DarkBorder, shape = RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = HighlightSky,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "GPS Locatie Simuleren",
                                        color = TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Thuis", "Kantoor", "Sportschool", "Trein").forEach { zone ->
                                        val isCurrent = settings.activeSoundZone == zone
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.setSoundZone(zone)
                                                notificationText = "Locatie gewijzigd naar $zone. Koptelefoonprofiel automatisch aangepast!"
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (isCurrent) AccentPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                                contentColor = if (isCurrent) AccentPrimary else TextMuted
                                            ),
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = if (isCurrent) AccentPrimary else DarkBorder
                                            ),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(28.dp)
                                                .testTag("simulate_zone_$zone")
                                        ) {
                                            Text(text = zone, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ADAPTIVE ACTIVITY PANEL
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Adaptieve Activiteitsregeling",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Detecteert automatisch je beweging en stemt de ruisonderdrukking af.",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = settings.adaptiveActivityEnabled,
                            onCheckedChange = { viewModel.toggleAdaptiveActivity(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentPrimary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkCard
                            ),
                            modifier = Modifier.scale(0.8f).testTag("switch_adaptive_activity")
                        )
                    }

                    HorizontalDivider(color = DarkBorder.copy(alpha = 0.3f))

                    // List of configured activities
                    Text(
                        text = "Activiteitsprofielen",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val activities = listOf(
                        Triple("Zitten", "ANC Aan · Harman EQ (Focus)", Icons.Default.Person),
                        Triple("Wandelen", "Omgevingsgeluid · Philips EQ", Icons.Default.SwapVert),
                        Triple("Hardlopen", "Omgevingsgeluid (Extra Veiligheid) · Bass EQ", Icons.Default.FlashOn),
                        Triple("Reizen", "ANC Aan · Bass EQ (Blokkeer lawaai)", Icons.Default.LocationOn)
                    )

                    activities.forEach { (name, desc, icon) ->
                        val isCurrentActivity = settings.adaptiveActivityEnabled && settings.activeActivity == name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isCurrentActivity) StatusPurple.copy(alpha = 0.1f) else DarkCard,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isCurrentActivity) StatusPurple.copy(alpha = 0.4f) else DarkBorder.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        color = if (isCurrentActivity) StatusPurple.copy(alpha = 0.15f) else DarkBorder.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isCurrentActivity) StatusPurple else TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = desc,
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                            if (isCurrentActivity) {
                                Text(
                                    text = "ACTIEF",
                                    color = StatusPurple,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Mock Simulator
                    if (settings.adaptiveActivityEnabled) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkCard, shape = RoundedCornerShape(10.dp))
                                .border(1.dp, DarkBorder, shape = RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = StatusPurple,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Sensoren Simuleren (Activiteit)",
                                        color = TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Zitten", "Wandelen", "Hardlopen", "Reizen").forEach { act ->
                                        val isCurrent = settings.activeActivity == act
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.setSimulatedActivity(act)
                                                notificationText = "Activiteit gedetecteerd: $act. Geluidsprofiel automatisch geoptimaliseerd!"
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (isCurrent) StatusPurple.copy(alpha = 0.15f) else Color.Transparent,
                                                contentColor = if (isCurrent) StatusPurple else TextMuted
                                            ),
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = if (isCurrent) StatusPurple else DarkBorder
                                            ),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(28.dp)
                                                .testTag("simulate_activity_$act")
                                        ) {
                                            Text(text = act, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveDeviceLinkPulseCard(
    viewModel: HeadphoneViewModel,
    settings: HeadphoneSettings,
    modifier: Modifier = Modifier
) {
    val isConnecting by viewModel.isConnecting.collectAsStateWithLifecycle()
    val isCharging by viewModel.isCharging.collectAsStateWithLifecycle()
    
    var signalDbm by remember { mutableStateOf(-54) }
    var latencyMs by remember { mutableStateOf(16) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var diagnosticResult by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    
    LaunchedEffect(settings.connected) {
        if (settings.connected) {
            while (true) {
                delay(2500)
                signalDbm = -50 - (0..12).random()
                val diff = (-2..2).random()
                latencyMs = (latencyMs + diff).coerceIn(12, 22)
            }
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "linked_pulse_transition")
    
    val p1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "p1"
    )
    val p2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "p2"
    )
    val p3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "p3"
    )
    
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.03f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    if (settings.connected) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            DarkPanel,
                            DarkPanel.copy(alpha = 0.95f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            DarkBorder,
                            StatusSuccess.copy(alpha = 0.35f + breathingAlpha),
                            DarkBorder
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
                .testTag("active_device_link_container")
        ) {
            Canvas(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-10).dp, y = (-10).dp)
            ) {
                val centerPoint = Offset(45.dp.toPx(), 45.dp.toPx())
                val maxRadius = 55.dp.toPx()
                
                listOf(p1, p2, p3).forEach { pFactor ->
                    val radius = pFactor * maxRadius
                    val alpha = (1f - pFactor) * 0.3f
                    drawCircle(
                        color = StatusSuccess.copy(alpha = alpha),
                        radius = radius,
                        center = centerPoint,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                                .size(24.dp)
                                .background(StatusSuccess.copy(alpha = 0.15f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Active Link",
                                tint = StatusSuccess,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "Actieve Apparaatkoppeling",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .background(StatusSuccess.copy(alpha = 0.12f), shape = RoundedCornerShape(100.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    color = StatusSuccess.copy(alpha = 0.4f + breathingAlpha * 5f),
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = "GEKOPPELD",
                            color = StatusSuccess,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                color = StatusSuccess.copy(alpha = 0.08f + breathingAlpha),
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = StatusSuccess.copy(alpha = 0.25f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = StatusSuccess,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Philips TAH6519 Pro",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Signaal: ${signalDbm} dBm",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Box(modifier = Modifier.size(3.dp).background(TextMuted, shape = CircleShape))
                            Text(
                                text = "Vertraging: ${latencyMs}ms",
                                color = if (latencyMs < 18) StatusSuccess else HighlightSky,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(DarkCard, shape = RoundedCornerShape(10.dp))
                            .border(1.dp, DarkBorder.copy(alpha = 0.5f), shape = RoundedCornerShape(10.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("STREAM FORMAT", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (settings.ldacEnabled) "LDAC High-Res" else "SBC Audio",
                                color = if (settings.ldacEnabled) HighlightSky else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(DarkCard, shape = RoundedCornerShape(10.dp))
                            .border(1.dp, DarkBorder.copy(alpha = 0.5f), shape = RoundedCornerShape(10.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("ACCUSTATUS", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${settings.batteryLevel}%",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isCharging) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = "Opladen",
                                        tint = StatusSuccess,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(DarkCard, shape = RoundedCornerShape(10.dp))
                            .border(1.dp, DarkBorder.copy(alpha = 0.5f), shape = RoundedCornerShape(10.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("KOPPELINGSSTATUS", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Ultra-Stabiel",
                                color = StatusSuccess,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard, shape = RoundedCornerShape(10.dp))
                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = HighlightSky,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Verbindingstester (Diagnose)",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            if (isTestingConnection) {
                                CircularProgressIndicator(
                                    color = HighlightSky,
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp
                                )
                            }
                        }
                        
                        Text(
                            text = diagnosticResult ?: "Test de realtime vertraging en stabiliteit van je Bluetooth LE verbinding.",
                            color = if (diagnosticResult != null) StatusSuccess else TextMuted,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                        
                        if (!isTestingConnection && diagnosticResult == null) {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isTestingConnection = true
                                    scope.launch {
                                        delay(1500)
                                        isTestingConnection = false
                                        diagnosticResult = "Diagnose voltooid! Vertraging: ${latencyMs}ms (Optimaal voor Hi-Res Audio). Verbindingsstabiliteit is 99.8% over de 2.4GHz Bluetooth-band."
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HighlightSky.copy(alpha = 0.12f),
                                    contentColor = HighlightSky
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .testTag("run_link_diagnostics")
                            ) {
                                Text(text = "Start Verbindingstest", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (diagnosticResult != null) {
                            TextButton(
                                onClick = { diagnosticResult = null },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(text = "Reset test", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PairingGuideDialog(onDismiss: () -> Unit) {
    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 4

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("pairing_guide_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = DarkPanel,
                contentColor = TextPrimary
            ),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hoe te koppelen",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Philips TAH6519 Bluetooth-gids",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_pairing_guide")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Sluiten",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Step indicators (Dots)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..totalSteps) {
                        val isActive = i == currentStep
                        Box(
                            modifier = Modifier
                                .size(if (isActive) 10.dp else 8.dp)
                                .background(
                                    color = if (isActive) AccentPrimary else DarkBorder,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Diagram Box (Canvas Area)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(DarkBg, shape = RoundedCornerShape(12.dp))
                        .border(1.dp, DarkBorder.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PairingDiagram(step = currentStep)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Step Instructions
                Text(
                    text = when (currentStep) {
                        1 -> "Stap 1: Zet in Koppelstand"
                        2 -> "Stap 2: Bluetooth inschakelen"
                        3 -> "Stap 3: Selecteer de Koptelefoon"
                        else -> "Stap 4: Succesvol Verbonden!"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (currentStep) {
                        1 -> "Houd de Power-knop op de rechter oorschelp 5 seconden ingedrukt totdat het LED-lampje snel blauw en rood begint te knipperen."
                        2 -> "Open de Bluetooth-instellingen op je telefoon of tablet en schakel Bluetooth in."
                        3 -> "Zoek naar nieuwe apparaten in de lijst en selecteer 'Philips TAH6519' om verbinding te maken."
                        else -> "Gefeliciteerd! Je koptelefoon is nu succesvol gekoppeld en klaar voor gebruik met premium audiofuncties."
                    },
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp), // fixed height to prevent layouts jumping
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("btn_pairing_prev"),
                            border = BorderStroke(1.dp, DarkBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("VORIGE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < totalSteps) {
                                currentStep++
                            } else {
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("btn_pairing_next"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (currentStep < totalSteps) "VOLGENDE" else "BEGRIJPEN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PairingDiagram(step: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_diag")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radius_diag"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha_diag"
    )

    // LED alternate colors for Step 1
    val isLedRed = (pulseRadius.toInt() % 10) < 5

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f

        when (step) {
            1 -> {
                // Step 1: Headphone outline and pulsing power button
                val headphoneColor = TextMuted.copy(alpha = 0.6f)
                
                // Draw Headband arc
                drawArc(
                    color = headphoneColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx()),
                    topLeft = Offset(centerX - 40.dp.toPx(), centerY - 45.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(80.dp.toPx(), 80.dp.toPx())
                )
                
                // Draw Ear Cups
                // Left cup
                drawRoundRect(
                    color = headphoneColor,
                    topLeft = Offset(centerX - 48.dp.toPx(), centerY - 15.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(16.dp.toPx(), 40.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
                
                // Right cup
                drawRoundRect(
                    color = AccentPrimary, // highlight right cup (where button is)
                    topLeft = Offset(centerX + 32.dp.toPx(), centerY - 15.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(16.dp.toPx(), 40.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )

                // Power button LED highlight on the right cup
                val buttonX = centerX + 40.dp.toPx()
                val buttonY = centerY + 5.dp.toPx()

                // Pulse ring representing flashing red/blue LED
                drawCircle(
                    color = if (isLedRed) Color.Red else AccentPrimary,
                    radius = pulseRadius.dp.toPx(),
                    center = Offset(buttonX, buttonY),
                    alpha = pulseAlpha
                )

                // Button indicator dot
                drawCircle(
                    color = if (isLedRed) Color.Red else AccentPrimary,
                    radius = 5.dp.toPx(),
                    center = Offset(buttonX, buttonY)
                )

                // Draw power symbol helper or finger press
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = 12.dp.toPx(),
                    center = Offset(buttonX, buttonY),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            2 -> {
                // Step 2: Phone screen with glowing bluetooth icon
                val phoneColor = TextMuted.copy(alpha = 0.5f)
                
                // Draw Phone frame
                drawRoundRect(
                    color = phoneColor,
                    topLeft = Offset(centerX - 24.dp.toPx(), centerY - 45.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(48.dp.toPx(), 90.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Draw speaker notch
                drawLine(
                    color = phoneColor,
                    start = Offset(centerX - 6.dp.toPx(), centerY - 40.dp.toPx()),
                    end = Offset(centerX + 6.dp.toPx(), centerY - 40.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )

                // Pulsing Bluetooth symbol background
                drawCircle(
                    color = AccentPrimary,
                    radius = 16.dp.toPx() + (pulseRadius * 0.2f).dp.toPx(),
                    center = Offset(centerX, centerY - 5.dp.toPx()),
                    alpha = 0.15f
                )

                drawCircle(
                    color = AccentPrimary,
                    radius = 15.dp.toPx(),
                    center = Offset(centerX, centerY - 5.dp.toPx())
                )

                // Draw Bluetooth Bluetooth logo lines on canvas
                // Logo coordinates
                val bx = centerX
                val by = centerY - 5.dp.toPx()
                val sizeVal = 7.dp.toPx()

                val path = Path().apply {
                    moveTo(bx, by - sizeVal)
                    lineTo(bx, by + sizeVal)
                    lineTo(bx + sizeVal * 0.6f, by + sizeVal * 0.5f)
                    lineTo(bx - sizeVal * 0.6f, by - sizeVal * 0.5f)
                    lineTo(bx + sizeVal * 0.6f, by - sizeVal * 0.5f)
                    lineTo(bx, by + sizeVal * 0.5f)
                }
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Draw high-contrast toggle switch representation below
                val ty = centerY + 25.dp.toPx()
                drawRoundRect(
                    color = AccentPrimary,
                    topLeft = Offset(centerX - 12.dp.toPx(), ty - 5.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(24.dp.toPx(), 10.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx())
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(centerX + 6.dp.toPx(), ty)
                )
            }
            3 -> {
                // Step 3: Selection list
                val phoneColor = TextMuted.copy(alpha = 0.4f)
                
                // Draw Phone frame
                drawRoundRect(
                    color = phoneColor,
                    topLeft = Offset(centerX - 40.dp.toPx(), centerY - 45.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(80.dp.toPx(), 90.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Title bar in phone
                drawRoundRect(
                    color = phoneColor,
                    topLeft = Offset(centerX - 34.dp.toPx(), centerY - 38.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(68.dp.toPx(), 8.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )

                // Muted list items
                drawRoundRect(
                    color = phoneColor.copy(alpha = 0.2f),
                    topLeft = Offset(centerX - 34.dp.toPx(), centerY - 24.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(68.dp.toPx(), 12.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                )

                // Highlighted TAH6519 list item
                drawRoundRect(
                    color = AccentPrimary.copy(alpha = 0.15f),
                    topLeft = Offset(centerX - 34.dp.toPx(), centerY - 6.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(68.dp.toPx(), 16.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                )
                drawRoundRect(
                    color = AccentPrimary,
                    topLeft = Offset(centerX - 34.dp.toPx(), centerY - 6.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(68.dp.toPx(), 16.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Pulsing dot on selection to draw eye
                drawCircle(
                    color = HighlightSky,
                    radius = (pulseRadius * 0.4f).dp.toPx(),
                    center = Offset(centerX + 20.dp.toPx(), centerY + 2.dp.toPx()),
                    alpha = pulseAlpha
                )
                drawCircle(
                    color = HighlightSky,
                    radius = 3.dp.toPx(),
                    center = Offset(centerX + 20.dp.toPx(), centerY + 2.dp.toPx())
                )

                // Next muted item
                drawRoundRect(
                    color = phoneColor.copy(alpha = 0.2f),
                    topLeft = Offset(centerX - 34.dp.toPx(), centerY + 16.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(68.dp.toPx(), 12.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                )
            }
            else -> {
                // Step 4: Wireless communication between headphones & phone + checkmark
                val elementColor = AccentPrimary
                val waveColor = HighlightSky

                // Draw Phone on the left
                drawRoundRect(
                    color = elementColor.copy(alpha = 0.5f),
                    topLeft = Offset(centerX - 50.dp.toPx(), centerY - 25.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(24.dp.toPx(), 45.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Draw Headphone on the right
                drawArc(
                    color = elementColor.copy(alpha = 0.5f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx()),
                    topLeft = Offset(centerX + 26.dp.toPx(), centerY - 20.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(24.dp.toPx(), 24.dp.toPx())
                )
                drawRoundRect(
                    color = elementColor.copy(alpha = 0.5f),
                    topLeft = Offset(centerX + 22.dp.toPx(), centerY - 10.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(6.dp.toPx(), 16.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )
                drawRoundRect(
                    color = elementColor.copy(alpha = 0.5f),
                    topLeft = Offset(centerX + 48.dp.toPx(), centerY - 10.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(6.dp.toPx(), 16.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )

                // Wireless waves pulsing in the center
                val waveX = centerX - 5.dp.toPx()
                val waveY = centerY - 2.dp.toPx()

                // Draw 3 dynamic arcs
                for (waveIndex in 0..2) {
                    val waveOffset = waveIndex * 8.dp.toPx()
                    val waveProgress = (pulseRadius.dp.toPx() + waveOffset) % 30.dp.toPx()
                    val alphaValue = (1f - (waveProgress / 30.dp.toPx())).coerceIn(0f, 1f)

                    drawArc(
                        color = waveColor,
                        startAngle = -45f,
                        sweepAngle = 90f,
                        useCenter = false,
                        style = Stroke(width = 2.dp.toPx()),
                        topLeft = Offset(waveX - waveProgress, waveY - waveProgress),
                        size = androidx.compose.ui.geometry.Size(waveProgress * 2, waveProgress * 2),
                        alpha = alphaValue
                    )
                }

                // Super clean success badge (green circle with checkmark)
                drawCircle(
                    color = StatusSuccess,
                    radius = 16.dp.toPx(),
                    center = Offset(centerX, centerY - 20.dp.toPx())
                )

                // Draw checkmark path
                val checkPath = Path().apply {
                    moveTo(centerX - 6.dp.toPx(), centerY - 20.dp.toPx())
                    lineTo(centerX - 2.dp.toPx(), centerY - 16.dp.toPx())
                    lineTo(centerX + 6.dp.toPx(), centerY - 24.dp.toPx())
                }
                drawPath(
                    path = checkPath,
                    color = Color.White,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun Tah6519HeadphoneBatteryArt(
    batteryLevel: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
    ancMode: String = "ON"
) {
    // Left channel level is slightly different for realistic asymmetry
    val leftLevel = (batteryLevel - 3).coerceIn(0, 100)
    val rightLevel = (batteryLevel + 2).coerceIn(0, 100)

    val animatedLeftLevel by animateFloatAsState(
        targetValue = leftLevel.toFloat(),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "left_earcup_level"
    )
    val animatedRightLevel by animateFloatAsState(
        targetValue = rightLevel.toFloat(),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "right_earcup_level"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "headphone_glow")
    
    // Wave animation for liquid battery fill
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_offset"
    )

    // Breathing glow animation for charging/low power states
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    // Scanning line offset for active charging
    val chargingLineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "charging_line_offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(DarkBg.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
            .border(1.dp, DarkBorder.copy(alpha = 0.4f), shape = RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("tah6519_battery_canvas")
        ) {
            val w = size.width
            val h = size.height
            val centerX = w / 2f
            val centerY = h / 2f + 10.dp.toPx()

            // Dimensions for Headphone components
            val earcupWidth = 38.dp.toPx()
            val earcupHeight = 72.dp.toPx()
            val earcupOffset = 70.dp.toPx() // Distance from center X to each cup

            val leftCupX = centerX - earcupOffset
            val rightCupX = centerX + earcupOffset
            val cupY = centerY

            // Colors
            val normalBrushColor = when {
                batteryLevel <= 20 -> StatusDanger
                batteryLevel <= 50 -> StatusYellow
                else -> AccentPrimary
            }
            
            val activeColor = if (isCharging) HighlightSky else normalBrushColor
            val ghostColor = activeColor.copy(alpha = 0.15f)

            // 1. Draw Connection / Energy Glow Halos around Earcups
            val glowRadius = earcupHeight * 0.65f
            if (isCharging) {
                drawCircle(
                    color = HighlightSky.copy(alpha = 0.05f * pulseGlow),
                    radius = glowRadius,
                    center = Offset(leftCupX, cupY)
                )
                drawCircle(
                    color = HighlightSky.copy(alpha = 0.05f * pulseGlow),
                    radius = glowRadius,
                    center = Offset(rightCupX, cupY)
                )
            } else if (batteryLevel <= 20) {
                drawCircle(
                    color = StatusDanger.copy(alpha = 0.06f * pulseGlow),
                    radius = glowRadius,
                    center = Offset(leftCupX, cupY)
                )
                drawCircle(
                    color = StatusDanger.copy(alpha = 0.06f * pulseGlow),
                    radius = glowRadius,
                    center = Offset(rightCupX, cupY)
                )
            }

            // 2. DRAW HEADBAND ARC
            val headbandPath = Path().apply {
                // Outer arc starting from top of left earcup to top of right earcup
                moveTo(leftCupX, cupY - 20.dp.toPx())
                cubicTo(
                    leftCupX, cupY - 85.dp.toPx(),
                    rightCupX, cupY - 85.dp.toPx(),
                    rightCupX, cupY - 20.dp.toPx()
                )
            }

            // Draw headband background
            drawPath(
                path = headbandPath,
                color = DarkBorder.copy(alpha = 0.5f),
                style = Stroke(width = 10.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            // Draw active power/charging glow stream on headband
            val headbandBrush = if (isCharging) {
                Brush.horizontalGradient(
                    colors = listOf(
                        HighlightSky.copy(alpha = 0.2f),
                        HighlightSky.copy(alpha = pulseGlow),
                        HighlightSky.copy(alpha = 0.2f)
                    ),
                    startX = centerX - earcupOffset * chargingLineOffset,
                    endX = centerX + earcupOffset * chargingLineOffset
                )
            } else {
                Brush.horizontalGradient(
                    colors = listOf(
                        activeColor.copy(alpha = 0.1f),
                        activeColor.copy(alpha = 0.4f),
                        activeColor.copy(alpha = 0.1f)
                    )
                )
            }

            drawPath(
                path = headbandPath,
                brush = headbandBrush,
                style = Stroke(width = 6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            // Inner headband support cushion
            val innerHeadbandPath = Path().apply {
                moveTo(leftCupX + 5.dp.toPx(), cupY - 15.dp.toPx())
                cubicTo(
                    leftCupX + 5.dp.toPx(), cupY - 74.dp.toPx(),
                    rightCupX - 5.dp.toPx(), cupY - 74.dp.toPx(),
                    rightCupX - 5.dp.toPx(), cupY - 15.dp.toPx()
                )
            }
            drawPath(
                path = innerHeadbandPath,
                color = DarkBg.copy(alpha = 0.9f),
                style = Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            // 3. DRAW EAR CUP ARMS & JOINTS (Metallic brackets connecting cups to headband)
            val armStroke = 3.dp.toPx()
            val armColor = Color(0xFF1E293B)
            
            // Left Bracket
            drawLine(
                color = armColor,
                start = Offset(leftCupX, cupY - 42.dp.toPx()),
                end = Offset(leftCupX, cupY - 25.dp.toPx()),
                strokeWidth = armStroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            // Right Bracket
            drawLine(
                color = armColor,
                start = Offset(rightCupX, cupY - 42.dp.toPx()),
                end = Offset(rightCupX, cupY - 25.dp.toPx()),
                strokeWidth = armStroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            // 4. DRAW EARCUPS (LEFT & RIGHT)
            drawEarcupCanvas(
                centerX = leftCupX,
                centerY = cupY,
                width = earcupWidth,
                height = earcupHeight,
                level = animatedLeftLevel,
                isCharging = isCharging,
                activeColor = activeColor,
                ghostColor = ghostColor,
                waveOffset = waveOffset,
                label = "L"
            )

            drawEarcupCanvas(
                centerX = rightCupX,
                centerY = cupY,
                width = earcupWidth,
                height = earcupHeight,
                level = animatedRightLevel,
                isCharging = isCharging,
                activeColor = activeColor,
                ghostColor = ghostColor,
                waveOffset = waveOffset,
                label = "R"
            )

            // 5. DRAW ACTIVE STATUS CENTERPIECE INFO (e.g. "TAH6519", "ANC ON", battery status text)
            // We'll draw a beautiful glowing HUD dot at the very center of the headset
            val hudStatusColor = if (isCharging) HighlightSky else normalBrushColor
            drawCircle(
                color = hudStatusColor.copy(alpha = 0.2f * pulseGlow),
                radius = 12.dp.toPx(),
                center = Offset(centerX, centerY - 20.dp.toPx())
            )
            drawCircle(
                color = hudStatusColor,
                radius = 4.dp.toPx(),
                center = Offset(centerX, centerY - 20.dp.toPx())
            )
        }

        // Left earcup channel text overlay
        Text(
            text = "L",
            color = Color.White.copy(alpha = 0.2f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-70).dp, y = 10.dp)
        )

        // Right earcup channel text overlay
        Text(
            text = "R",
            color = Color.White.copy(alpha = 0.2f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 70.dp, y = 10.dp)
        )

        // Standard Compose Overlay text inside the Box so we don't have to measure/draw standard text on Canvas
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                text = "PHILIPS TAH6519",
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val batteryIcon = when {
                    isCharging -> Icons.Default.FlashOn
                    batteryLevel <= 20 -> Icons.Default.BatteryAlert
                    else -> Icons.Default.BatteryFull
                }
                Icon(
                    imageVector = batteryIcon,
                    contentDescription = null,
                    tint = if (isCharging) HighlightSky else when {
                        batteryLevel <= 20 -> StatusDanger
                        batteryLevel <= 50 -> StatusYellow
                        else -> StatusSuccess
                    },
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "${batteryLevel}%",
                    color = if (isCharging) HighlightSky else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = if (isCharging) "SNELLAAD-MODUS ACTIEF" else "STROOMVERBRUIK: ${if (ancMode != "OFF") "HOOG (ANC)" else "GEBALANCEERD"}",
                color = TextMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// Helper Extension drawing function to keep code clean and modular
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEarcupCanvas(
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    level: Float,
    isCharging: Boolean,
    activeColor: Color,
    ghostColor: Color,
    waveOffset: Float,
    label: String
) {
    val left = centerX - width / 2f
    val top = centerY - height / 2f
    val size = androidx.compose.ui.geometry.Size(width, height)
    val cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx(), 12.dp.toPx())

    // 1. Draw outer cushion shadow/glow
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.4f),
        topLeft = Offset(left, top),
        size = size,
        cornerRadius = cornerRadius
    )

    // 2. Draw outer physical plastic housing
    drawRoundRect(
        color = Color(0xFF0F172A), // Dark slate
        topLeft = Offset(left, top),
        size = size,
        cornerRadius = cornerRadius
    )

    // Draw earcup chrome / highlight trim
    drawRoundRect(
        color = DarkBorder,
        topLeft = Offset(left, top),
        size = size,
        cornerRadius = cornerRadius,
        style = Stroke(width = 1.5.dp.toPx())
    )

    // 3. Draw battery level chamber background (inner track)
    val chamberPadding = 4.dp.toPx()
    val innerLeft = left + chamberPadding
    val innerTop = top + chamberPadding
    val innerWidth = width - chamberPadding * 2f
    val innerHeight = height - chamberPadding * 2f
    val innerSize = androidx.compose.ui.geometry.Size(innerWidth, innerHeight)
    val innerCornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())

    drawRoundRect(
        color = Color(0xFF080D1A), // deep dark chamber
        topLeft = Offset(innerLeft, innerTop),
        size = innerSize,
        cornerRadius = innerCornerRadius
    )

    // 4. Draw battery chamber fill (liquid fluid level)
    val fillHeight = innerHeight * (level / 100f)
    val fillTop = innerTop + innerHeight - fillHeight

    if (fillHeight > 0f) {
        val fillBrush = Brush.verticalGradient(
            colors = listOf(
                activeColor,
                activeColor.copy(alpha = 0.7f)
            ),
            startY = fillTop,
            endY = innerTop + innerHeight
        )

        drawRoundRect(
            brush = fillBrush,
            topLeft = Offset(innerLeft, fillTop),
            size = androidx.compose.ui.geometry.Size(innerWidth, fillHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
    }

    // 5. Draw sleek overlay brand / channel label "L" or "R"
    // Draw a neat minimal border for the channel label
    drawRoundRect(
        color = activeColor.copy(alpha = 0.25f),
        topLeft = Offset(innerLeft, innerTop),
        size = innerSize,
        cornerRadius = innerCornerRadius,
        style = Stroke(width = 1.dp.toPx())
    )
}

@Composable
fun PhilipsPremiumBatteryIndicator(
    batteryLevel: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedLevel by animateFloatAsState(
        targetValue = batteryLevel.toFloat(),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "circular_battery_level"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "philips_battery_glow")
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "charging_rotation"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBg.copy(alpha = 0.6f), shape = RoundedCornerShape(16.dp))
            .border(1.dp, DarkBorder.copy(alpha = 0.6f), shape = RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Ear Cup
        EarcupBatteryIndicator(
            label = "L KANAAL",
            channel = "L",
            level = (batteryLevel - 3).coerceIn(0, 100),
            isCharging = isCharging,
            glowAlpha = if (isCharging) glowAlpha else 1f
        )

        // Central Circular Gauge
        Box(
            modifier = Modifier
                .size(130.dp)
                .testTag("philips_central_battery_gauge"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sizePx = size.width
                val centerX = sizePx / 2f
                val centerY = sizePx / 2f
                val outerRadius = sizePx / 2f - 10.dp.toPx()
                val innerRadius = outerRadius - 8.dp.toPx()
                
                // Draw outer ambient glow
                drawCircle(
                    color = AccentPrimary.copy(alpha = 0.08f * (if (isCharging) glowAlpha * 1.5f else 1f)),
                    radius = outerRadius + 8.dp.toPx()
                )

                // Track ring
                drawCircle(
                    color = DarkBorder.copy(alpha = 0.3f),
                    radius = outerRadius,
                    style = Stroke(width = 6.dp.toPx())
                )

                // Colored progress gradient or solid color based on state
                val angleRange = 360f * (animatedLevel / 100f)
                val progressBrush = if (isCharging) {
                    Brush.sweepGradient(
                        colors = listOf(AccentPrimary, HighlightSky, AccentPrimary)
                    )
                } else {
                    Brush.sweepGradient(
                        colors = when {
                            batteryLevel <= 20 -> listOf(StatusDanger, StatusDanger.copy(alpha = 0.7f))
                            batteryLevel <= 50 -> listOf(StatusYellow, StatusYellow.copy(alpha = 0.7f))
                            else -> listOf(AccentPrimary, HighlightSky, AccentPrimary)
                        }
                    )
                }

                // Draw progress arc starting from top (-90 degrees)
                val startAngle = if (isCharging) -90f + rotationAngle else -90f
                val sweepAngle = if (isCharging) 120f else angleRange
                drawArc(
                    brush = progressBrush,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 7.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                    topLeft = Offset(centerX - outerRadius, centerY - outerRadius),
                    size = androidx.compose.ui.geometry.Size(outerRadius * 2, outerRadius * 2)
                )

                // Inner metallic/neon accent ring
                drawCircle(
                    color = DarkBorder.copy(alpha = 0.5f),
                    radius = innerRadius,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Text display inside circle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isCharging) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Opladen",
                        tint = HighlightSky,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { alpha = glowAlpha }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Headset,
                        contentDescription = null,
                        tint = TextMuted.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Text(
                    text = "${batteryLevel}%",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )

                Text(
                    text = if (isCharging) "OPLADEN" else "RESTEREND",
                    color = if (isCharging) HighlightSky else TextMuted,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Right Ear Cup
        EarcupBatteryIndicator(
            label = "R KANAAL",
            channel = "R",
            level = (batteryLevel + 2).coerceIn(0, 100),
            isCharging = isCharging,
            glowAlpha = if (isCharging) glowAlpha else 1f
        )
    }
}

@Composable
fun PhilipsHeadphoneProgressBar(
    batteryLevel: Int,
    isCharging: Boolean,
    healthModeActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val animatedLevel by animateFloatAsState(
        targetValue = batteryLevel.toFloat(),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress_bar_battery_level"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "progress_glow")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    val progressBrush = if (isCharging) {
        Brush.horizontalGradient(
            colors = listOf(AccentPrimary, HighlightSky, AccentPrimary)
        )
    } else {
        Brush.horizontalGradient(
            colors = when {
                batteryLevel <= 20 -> listOf(StatusDanger, StatusDanger.copy(alpha = 0.7f))
                batteryLevel <= 50 -> listOf(StatusYellow, StatusYellow.copy(alpha = 0.7f))
                else -> listOf(AccentPrimary, HighlightSky)
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBg.copy(alpha = 0.6f), shape = RoundedCornerShape(16.dp))
            .border(1.dp, DarkBorder.copy(alpha = 0.6f), shape = RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top label with connected device info
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
                    imageVector = Icons.Default.Headset,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Philips TAH6519",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isCharging) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Opladen",
                        tint = HighlightSky,
                        modifier = Modifier
                            .size(14.dp)
                            .graphicsLayer { alpha = pulseGlow }
                    )
                }
                Text(
                    text = "${batteryLevel}%",
                    color = if (isCharging) HighlightSky else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // The actual progress bar container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(DarkPanel, shape = RoundedCornerShape(8.dp))
                .border(1.dp, DarkBorder.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                .padding(3.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Background grid/tick marks for a futuristic visual layout
            Row(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(10) { index ->
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(DarkBorder.copy(alpha = 0.2f))
                    )
                }
            }

            // Animated progress fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((animatedLevel / 100f).coerceIn(0f, 1f))
                    .background(brush = progressBrush, shape = RoundedCornerShape(5.dp))
                    .graphicsLayer {
                        if (isCharging) {
                            alpha = pulseGlow
                        }
                    }
            )

            // Inner gloss highlight overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth((animatedLevel / 100f).coerceIn(0f, 1f))
                    .height(6.dp)
                    .align(Alignment.TopStart)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
                    .background(Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(3.dp))
            )
        }

        // Bottom specs (Estimated playback time / battery status text)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusText = when {
                healthModeActive -> "Accu Gezondheidsmodus (Max 80%)"
                isCharging -> "Bezig met opladen via USB-C"
                else -> "Smart Power Management actief"
            }
            Text(
                text = statusText,
                color = if (healthModeActive) StatusSuccess else TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = when {
                    isCharging -> "Tijd tot vol: ~${((100 - batteryLevel) * 0.9f).toInt()} min"
                    batteryLevel <= 20 -> "⚠️ Accu kritiek laag!"
                    else -> "Resterend: ~${(batteryLevel * 0.8f).toInt()} uur"
                },
                color = when {
                    isCharging -> HighlightSky
                    batteryLevel <= 20 -> StatusDanger
                    else -> TextMuted
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EarcupBatteryIndicator(
    label: String,
    channel: String,
    level: Int,
    isCharging: Boolean,
    glowAlpha: Float
) {
    val animatedLevel by animateFloatAsState(
        targetValue = level.toFloat(),
        animationSpec = tween(800),
        label = "earcup_level"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted,
            letterSpacing = 0.5.sp
        )

        // Battery level box
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(68.dp)
                .background(DarkPanel, shape = RoundedCornerShape(6.dp))
                .border(1.dp, DarkBorder.copy(alpha = 0.8f), shape = RoundedCornerShape(6.dp))
                .padding(4.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Channel label "L" or "R" inside the cell background
            Text(
                text = channel,
                color = TextMuted.copy(alpha = 0.15f),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.Center)
            )

            // Fill
            val fillBrush = if (isCharging) {
                Brush.verticalGradient(
                    colors = listOf(HighlightSky, AccentPrimary)
                )
            } else {
                Brush.verticalGradient(
                    colors = when {
                        level <= 20 -> listOf(StatusDanger.copy(alpha = 0.8f), StatusDanger)
                        level <= 50 -> listOf(StatusYellow.copy(alpha = 0.8f), StatusYellow)
                        else -> listOf(AccentPrimary.copy(alpha = 0.8f), HighlightSky)
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedLevel / 100f)
                    .background(brush = fillBrush, shape = RoundedCornerShape(3.dp))
                    .graphicsLayer {
                        if (isCharging) {
                            alpha = glowAlpha
                        }
                    }
            )
        }

        Text(
            text = "${level}%",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                isCharging -> HighlightSky
                level <= 20 -> StatusDanger
                level <= 50 -> StatusYellow
                else -> TextPrimary
            }
        )
    }
}

@Composable
fun SettingsDialog(
    viewModel: HeadphoneViewModel,
    onDismiss: () -> Unit
) {
    val firmwareVersion by viewModel.firmwareVersion.collectAsStateWithLifecycle()
    val serialNumber by viewModel.serialNumber.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    var showResetConfirm by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    if (showResetConfirm) {
        Dialog(onDismissRequest = { showResetConfirm = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkPanel, shape = RoundedCornerShape(16.dp))
                    .border(1.dp, StatusDanger.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
                    .padding(20.dp)
                    .testTag("reset_confirmation_dialog")
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(StatusDanger.copy(alpha = 0.1f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = StatusDanger,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "Fabrieksreset Bevestigen?",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        text = "Weet je zeker dat je alle instellingen wilt herstellen naar de fabrieksinstellingen? Dit wist alle Equalizer-presets, ANC-profielen en herstelt de firmware naar v1.4.2.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showResetConfirm = false },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                            border = BorderStroke(1.dp, DarkBorder),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("cancel_reset_button")
                        ) {
                            Text("Annuleren", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.resetAll()
                                showResetConfirm = false
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusDanger),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("confirm_reset_button")
                        ) {
                            Text("Reset alles", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkPanel, shape = RoundedCornerShape(16.dp))
                .border(1.dp, DarkBorder, shape = RoundedCornerShape(16.dp))
                .padding(20.dp)
                .testTag("settings_dialog")
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
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
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = HighlightSky,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Koptelefoon Instellingen",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Sluit", tint = TextMuted)
                    }
                }

                HorizontalDivider(color = DarkBorder)

                // Device Model Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(1.dp, DarkBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(AccentPrimary.copy(alpha = 0.15f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Philips TAH6519 Pro",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Premium Over-Ear ANC Headphones",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Technical Specifications Card
                var showSpecs by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("specs_card"),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(1.dp, if (showSpecs) HighlightSky.copy(alpha = 0.5f) else DarkBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showSpecs = !showSpecs 
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = HighlightSky,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Technische Specificaties (TAH6519)",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                imageVector = if (showSpecs) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showSpecs) "Minder details" else "Meer details",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (showSpecs) {
                            HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
                            
                            val specItems = listOf(
                                "Audiostuurprogramma" to "40 mm high-performance neodymium drivers",
                                "Frequentiebereik" to "20 Hz - 20.000 Hz",
                                "Gevoeligheid" to "101 dB @ 1 kHz",
                                "Impedantie" to "32 Ohm",
                                "Ruisonderdrukking" to "Hybride ANC (Deep-Silence tot -56 dB)",
                                "Batterijduur (ANC Aan)" to "40 uur ononderbroken speeltijd",
                                "Batterijduur (ANC Uit)" to "80 uur ononderbroken speeltijd",
                                "Snelladen (USB-C)" to "15 min laden = 5 uur speeltijd (2 uur vol)",
                                "Bluetooth-versie" to "Bluetooth 5.3 (Multipoint-ondersteuning)",
                                "Audio Codecs" to "AAC, SBC voor high-fidelity streaming",
                                "Microfoons" to "4x ENC microfoons voor ruisvrij bellen",
                                "Slimme Sensor" to "Ingebouwde draagdetectie (Smart Pause)",
                                "Gewicht / Ontwerp" to "260g, ergonomisch opvouwbaar over-ear"
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                specItems.forEach { (label, value) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = label,
                                            color = TextMuted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(0.45f)
                                        )
                                        Text(
                                            text = value,
                                            color = TextPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(0.55f)
                                        )
                                    }
                                    HorizontalDivider(color = DarkBorder.copy(alpha = 0.2f))
                                }
                            }
                        }
                    }
                }

                // Theme Mode Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(1.dp, DarkBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                    imageVector = if (ThemeState.isLightMode) Icons.Default.WbSunny else Icons.Default.NightsStay,
                                    contentDescription = null,
                                    tint = HighlightSky,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Applicatiethema",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (ThemeState.isLightMode) "Lichte Modus" else "Donkere Modus",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                Switch(
                                    checked = ThemeState.isLightMode,
                                    onCheckedChange = { 
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        ThemeState.isLightMode = it 
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = HighlightSky,
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = DarkBg
                                    ),
                                    modifier = Modifier
                                        .scale(0.75f)
                                        .testTag("settings_theme_switch")
                                )
                            }
                        }

                        HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

                        Text(
                            text = "Kies Stijlaccent:",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val themes = listOf(
                                Triple(com.example.ui.theme.AppTheme.PHILIPS_STUDIO, "Studio", Color(0xFF0066FF)),
                                Triple(com.example.ui.theme.AppTheme.CYBERPUNK_NEON, "Cyber", Color(0xFFE047FF)),
                                Triple(com.example.ui.theme.AppTheme.CARBON_AMBER, "Amber", Color(0xFFF59E0B)),
                                Triple(com.example.ui.theme.AppTheme.NORDIC_FROST, "Frost", Color(0xFF10B981)),
                                Triple(com.example.ui.theme.AppTheme.HIGH_CONTRAST, "Contrast", Color(0xFFFFFF00))
                            )

                            themes.forEach { (theme, label, previewColor) ->
                                val isSelected = ThemeState.activeTheme == theme
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (isSelected) previewColor.copy(alpha = 0.15f) else DarkBg,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) previewColor else DarkBorder,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            ThemeState.activeTheme = theme
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(previewColor, shape = CircleShape)
                                        )
                                        Text(
                                            text = label,
                                            color = if (isSelected) TextPrimary else TextMuted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Firmware Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(1.dp, DarkBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = HighlightSky,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Firmware-versie",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .background(HighlightSky.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = firmwareVersion,
                                    color = HighlightSky,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("settings_firmware_version")
                                )
                            }
                        }

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
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = HighlightSky,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Serienummer",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .background(HighlightSky.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = serialNumber,
                                    color = HighlightSky,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("settings_serial_number")
                                )
                            }
                        }

                        HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

                        Text(
                            text = "Als er nieuwe updates beschikbaar zijn met geluidskwaliteit- en prestatieverbeteringen, kun je deze installeren in de 'Device' tab.",
                            color = TextMuted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.checkForUpdates()
                            },
                            enabled = updateState !is UpdateState.Checking && updateState !is UpdateState.Updating,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HighlightSky.copy(alpha = 0.15f),
                                contentColor = HighlightSky,
                                disabledContainerColor = DarkBg,
                                disabledContentColor = TextMuted
                            ),
                            border = BorderStroke(1.dp, if (updateState !is UpdateState.Checking && updateState !is UpdateState.Updating) HighlightSky.copy(alpha = 0.4f) else DarkBorder),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("settings_check_updates_button")
                        ) {
                            if (updateState is UpdateState.Checking) {
                                Box(modifier = Modifier.size(16.dp)) {
                                    CircularProgressIndicator(
                                        color = TextMuted,
                                        strokeWidth = 2.dp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Controleren...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text(
                                    text = when (updateState) {
                                        is UpdateState.UpToDate -> "Je bent up-to-date"
                                        is UpdateState.UpdateAvailable -> "Update Beschikbaar"
                                        is UpdateState.UpdateComplete -> "Update Voltooid"
                                        else -> "Controleer op updates"
                                    },
                                    fontSize = 11.sp, 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = DarkBorder)

                // Factory Reset Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = StatusDanger,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Reset naar Fabrieksinstellingen",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Wordt de hoofdtelefoon gereset, dan worden alle aangepaste instellingen, equalizer-profielen en geluidszones permanent gewist.",
                        color = TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showResetConfirm = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusDanger.copy(alpha = 0.15f), contentColor = StatusDanger),
                        border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("settings_reset_button")
                    ) {
                        Text("Reset naar Fabrieksinstellingen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}



@Composable
fun SpatialAudioVisualizer() {
    val infiniteTransition = rememberInfiniteTransition(label = "spatial_pulse")
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "phase_1"
    )

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().testTag("spatial_audio_visualizer")) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        // Draw head
        drawCircle(color = TextMuted.copy(alpha = 0.5f), radius = 20.dp.toPx(), center = center)

        // Draw radiating waves
        for (i in 1..4) {
            val scale = (phase1 + (i * Math.PI.toFloat() / 2f)) % (2f * Math.PI.toFloat())
            val alpha = (1f - (scale / (2f * Math.PI.toFloat()))).coerceIn(0f, 1f)
            val radius = 20.dp.toPx() + (scale * 20.dp.toPx())

            drawCircle(
                color = StatusPurple.copy(alpha = alpha * 0.4f),
                radius = radius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
        
        // Label
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#9b51e0") // matches StatusPurple
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawText("3D SPATIAL AUDIO ACTIEF", w / 2f, h - 10.dp.toPx(), paint)
        }
    }
}

@Composable
fun FirmwareUpdatePromptDialog(
    version: String,
    changelog: List<String>,
    onDismiss: () -> Unit,
    onInstall: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkPanel, shape = RoundedCornerShape(16.dp))
                .border(1.dp, HighlightSky.copy(alpha = 0.3f), shape = RoundedCornerShape(16.dp))
                .padding(20.dp)
                .testTag("firmware_update_prompt_dialog")
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(HighlightSky.copy(alpha = 0.1f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = HighlightSky,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Nieuwe Firmware Beschikbaar!",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Er is een belangrijke firmware-update ($version) beschikbaar voor je Philips TAH6519 om prestaties en ANC-reductie te optimaliseren.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg, shape = RoundedCornerShape(8.dp))
                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "VERBETERINGEN:",
                            color = HighlightSky,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        changelog.forEach { bullet ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    color = AccentPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = bullet,
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                        border = BorderStroke(1.dp, DarkBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_dismiss_firmware_prompt")
                    ) {
                        Text("Later", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onInstall,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                            .testTag("btn_accept_firmware_prompt")
                    ) {
                        Text("Nu installeren", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
