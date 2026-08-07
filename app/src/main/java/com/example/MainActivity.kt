package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.sharp.*
import androidx.compose.material.icons.twotone.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.automirrored.rounded.*





import com.example.ui.FullScreenMediaDashboard


import android.os.Bundle
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.util.Random
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.R
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
    override fun attachBaseContext(newBase: android.content.Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val attributionContext = newBase.createAttributionContext("default")
            super.attachBaseContext(attributionContext)
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun getAttributionTag(): String? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            "default"
        } else {
            super.getAttributionTag()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Room database and repository setup
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = HeadphoneRepository(database.headphoneDao(), database.cachedTrackDao())
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
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        viewModel.shouldCloseApp.collect { shouldClose ->
            if (shouldClose) {
                (context as? android.app.Activity)?.finish()
            }
        }
    }

    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    
    val mediaIsPlaying by viewModel.mediaIsPlaying.collectAsStateWithLifecycle()
    val currentTrackIndex by viewModel.currentTrackIndex.collectAsStateWithLifecycle()
    val isYoutubeActive by viewModel.isYoutubeActive.collectAsStateWithLifecycle()
    val mediaTrackName by viewModel.mediaTrackName.collectAsStateWithLifecycle()
    val youtubePlaylistTracks by viewModel.youtubePlaylistTracks.collectAsStateWithLifecycle()
    val trackProgressSecs by viewModel.mediaProgress.collectAsStateWithLifecycle()
    
    val melodySynthesizer = remember { MelodySynthesizer() }
    val melodyScope = rememberCoroutineScope()

    LaunchedEffect(
        mediaIsPlaying, currentTrackIndex, isYoutubeActive, mediaTrackName
    ) {
        // Stop synthetic monotones so real stream and YouTube audio are played cleanly
        melodySynthesizer.stopMelody()
    }

    // Trigger background auto-reconnect service on launch for last paired device
    LaunchedEffect(Unit) {
        if (settings.autoReconnectOnLaunch && !settings.connected) {
            try {
                val serviceIntent = android.content.Intent(context, com.example.service.Tah6519AutoReconnectService::class.java).apply {
                    action = com.example.service.Tah6519AutoReconnectService.ACTION_START_RECONNECT
                    putExtra(com.example.service.Tah6519AutoReconnectService.EXTRA_DEVICE_NAME, settings.lastPairedDeviceName.ifBlank { "Philips TAH6519" })
                    putExtra(com.example.service.Tah6519AutoReconnectService.EXTRA_DEVICE_ADDRESS, settings.lastPairedDeviceAddress.ifBlank { "00:11:22:33:44:55" })
                }
                context.startService(serviceIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: android.content.Intent?) {
                val action = intent?.action
                if (action == com.example.service.Tah6519AutoReconnectService.ACTION_RECONNECT_STATUS_CHANGED) {
                    val status = intent.getStringExtra(com.example.service.Tah6519AutoReconnectService.EXTRA_STATUS) ?: ""
                    val isConnected = intent.getBooleanExtra(com.example.service.Tah6519AutoReconnectService.EXTRA_IS_CONNECTED, false)
                    val devName = intent.getStringExtra(com.example.service.Tah6519AutoReconnectService.EXTRA_DEVICE_NAME) ?: "Philips TAH6519"
                    val attempt = intent.getIntExtra(com.example.service.Tah6519AutoReconnectService.EXTRA_ATTEMPT, 1)

                    if (status.isNotBlank()) {
                        viewModel.gattStatusMessage.value = status
                    }
                    viewModel.reconnectAttempts.value = attempt
                    if (isConnected) {
                        viewModel.isAutoReconnecting.value = false
                        viewModel.selectHeadphoneProfile(devName)
                        viewModel.updateSettings { current ->
                            current.copy(connected = true, connectedDeviceName = devName)
                        }
                    } else if (attempt > 0) {
                        viewModel.isAutoReconnecting.value = true
                    }
                } else if (action == android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED) {
                    val device: android.bluetooth.BluetoothDevice? = intent.getParcelableExtra(android.bluetooth.BluetoothDevice.EXTRA_DEVICE)
                    val devName = try { device?.name ?: "Bluetooth Koptelefoon" } catch (e: Exception) { "Bluetooth Koptelefoon" }
                    viewModel.connectDevice(devName)
                } else if (action == android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                    viewModel.disconnectDevice()
                } else if (action == android.content.Intent.ACTION_BATTERY_CHANGED || action == "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED") {
                    val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                    val scale = intent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
                    val status = intent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
                    val isPlugged = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL
                    
                    val btLevel = intent?.getIntExtra("android.bluetooth.device.extra.BATTERY_LEVEL", -1) ?: -1
                    val calculatedLevel = if (btLevel in 0..100) {
                        btLevel
                    } else if (level in 0..100 && scale > 0) {
                        ((level.toFloat() / scale) * 100).toInt()
                    } else -1
                    
                    if (calculatedLevel in 0..100) {
                        viewModel.updateBatteryLevel(calculatedLevel)
                    }
                    if (isPlugged != viewModel.isCharging.value) {
                        viewModel.toggleCharging(isPlugged)
                    }
                }
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(com.example.service.Tah6519AutoReconnectService.ACTION_RECONNECT_STATUS_CHANGED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(android.bluetooth.BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(android.content.Intent.ACTION_BATTERY_CHANGED)
            addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
        }
        try {
            context.registerReceiver(receiver, filter)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {}
            melodySynthesizer.stopMelody()
        }
    }

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
    val connectionSuccessEvent by viewModel.connectionSuccessEvent.collectAsStateWithLifecycle()
    val isAutoReconnecting by viewModel.isAutoReconnecting.collectAsStateWithLifecycle()
    val isCharging by viewModel.isCharging.collectAsStateWithLifecycle()
    val firmwareVersion by viewModel.firmwareVersion.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val autoOffRemainingSeconds by viewModel.autoOffRemainingSeconds.collectAsStateWithLifecycle()
    val autoOffIsInactive by viewModel.autoOffIsInactive.collectAsStateWithLifecycle()
    var activeTab by remember { mutableStateOf("dash") }
    var eqBandMode by remember { mutableStateOf("5-BAND") }
    var showPairingGuide by remember { mutableStateOf(false) }
    var showQuickStartModal by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showHardwareManualDialog by remember { mutableStateOf(false) }
    var showBleScannerDialog by remember { mutableStateOf(false) }
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
        if (connectedChanged && settings.connected && connectionSuccessEvent == null) {
            viewModel.triggerConnectionSuccessAnimation(settings.connectedDeviceName)
        }
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
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showPairingGuide = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            com.example.ui.Tah6519PairingSetupScreen(
                viewModel = viewModel,
                onClose = { showPairingGuide = false },
                onNavigateToTab = { targetTab ->
                    activeTab = targetTab
                    showPairingGuide = false
                }
            )
        }
    }

    if (showQuickStartModal) {
        com.example.ui.Tah6519QuickStartModal(
            viewModel = viewModel,
            onDismiss = { showQuickStartModal = false }
        )
    }

    if (showSettings) {
        SettingsDialog(viewModel = viewModel, onDismiss = { showSettings = false })
    }

    if (showHardwareManualDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showHardwareManualDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            com.example.ui.PhilipsHardwareManualScreen(
                onClose = { showHardwareManualDialog = false },
                viewModel = viewModel
            )
        }
    }

    if (showBleScannerDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showBleScannerDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(DarkPanel).padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bluetooth,
                                contentDescription = null,
                                tint = HighlightSky,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Bluetooth LE Scanner & GATT",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        IconButton(onClick = { showBleScannerDialog = false }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Sluiten", tint = TextMuted)
                        }
                    }
                    com.example.ui.PhilipsBleScannerScreen(
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
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
                                    imageVector = Icons.Filled.Headphones,
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
                        BluetoothConnectivityBadge(
                            isConnected = settings.connected,
                            isConnecting = isConnecting || isAutoReconnecting,
                            deviceName = if (settings.connected) settings.connectedDeviceName.ifBlank { "Philips TAH6519" } else "Niet verbonden",
                            onClick = { showPairingGuide = true }
                        )
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
                                    viewModel.setThemeMode(com.example.ui.theme.ThemeMode.LIGHT)
                                    viewModel.setActiveAppTheme(com.example.ui.theme.AppTheme.PHILIPS_STUDIO)
                                } else {
                                    viewModel.setThemeMode(com.example.ui.theme.ThemeMode.DARK)
                                    viewModel.setActiveAppTheme(com.example.ui.theme.AppTheme.HIGH_CONTRAST)
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("theme_toggle_button")
                        ) {
                            val isHC = !ThemeState.isLightMode && ThemeState.activeTheme == com.example.ui.theme.AppTheme.HIGH_CONTRAST
                            Icon(
                                imageVector = if (isHC) Icons.Filled.WbSunny else Icons.Filled.NightsStay,
                                contentDescription = "Thema omschakelen",
                                tint = if (isHC) AccentPrimary else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = { showHardwareManualDialog = true },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("topbar_manual_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "Handleiding & Gids",
                                tint = AccentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = { showQuickStartModal = true },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("quick_start_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Quick Start Gids",
                                tint = HighlightSky,
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
                                imageVector = Icons.Filled.Settings,
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
                                        text = "SNELSTART GIDS",
                                        color = HighlightSky,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable { showQuickStartModal = true }
                                            .padding(4.dp)
                                            .testTag("banner_quick_start_button")
                                    )
                                    Text(
                                        text = "HOE KOPPELEN?",
                                        color = TextMuted,
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
                                        imageVector = Icons.Filled.Check,
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
                                        imageVector = Icons.Filled.BatteryAlert,
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
                                            imageVector = Icons.Filled.CheckCircle,
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
            Column(modifier = Modifier.background(DarkPanel)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    AccentPrimary.copy(alpha = 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                NavigationBar(
                    containerColor = DarkPanel,
                    contentColor = TextPrimary,
                    tonalElevation = 16.dp
                ) {
                    val tabs = listOf(
                        Triple("dash", "Thuis", Icons.Filled.Dashboard),
                        Triple("media", "Media", Icons.Filled.MusicNote),
                        Triple("audio", "Audio", Icons.Filled.GraphicEq),
                        Triple("device", "Systeem", Icons.Filled.Settings)
                    )
                    tabs.forEach { (tabId, label, icon) ->
                        NavigationBarItem(
                            selected = activeTab == tabId,
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                activeTab = tabId 
                            },
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
                    fadeIn(
                        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing)
                    ) togetherWith fadeOut(
                        animationSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing)
                    )
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
                                    Tah6519EstimatedBatteryBanner(
                                        settings = settings,
                                        isCharging = isCharging,
                                        onNavigateToBattery = { activeTab = "device" }
                                    )
                                }
                            }
                            item {
                                UniversalHeadphoneSelectorCard(viewModel, settings)
                            }
                            if (settings.connected) {
                                item {
                                    ActiveDeviceLinkPulseCard(viewModel, settings)
                                }
                            }
                            item {
                                com.example.ui.Tah6519ConnectionDashboardCard(viewModel, settings)
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
                                    onFetchBattery = { viewModel.fetchBatteryLevel() },
                                    settings = settings,
                                    viewModel = viewModel
                                )
                            }
                            item {
                                com.example.ui.BatteryTimeRemainingCard(
                                    viewModel = viewModel,
                                    settings = settings,
                                    isCharging = isCharging
                                )
                            }
                            item {
                                BluetoothStatusIndicatorCard(viewModel, settings)
                            }
                            item {
                                CompatibleBluetoothDevicesCard(viewModel)
                            }
                            item {
                                TechnicalConnectionStatsCard(viewModel, settings)
                            }
                            item {
                                DashboardMediaWidget(viewModel, settings)
                            }
                            item {
                                com.example.ui.Tah6519AncModeToggleCard(
                                    settings = settings,
                                    onModeChange = { mode -> viewModel.setAncMode(mode) },
                                    onLevelChange = { level -> viewModel.setAncLevel(level) },
                                    onTransparencyIntensityChange = { intensity -> viewModel.setTransparencyIntensity(intensity) },
                                    onWindNoiseReductionToggle = { enabled -> viewModel.toggleWindNoiseReduction(enabled) },
                                    onSpeakToAwarenessToggle = { enabled -> viewModel.toggleSpeakToAwareness(enabled) }
                                )
                            }
                            item {
                                com.example.ui.Tah6519MainAncModeSelectorCard(
                                    viewModel = viewModel,
                                    settings = settings
                                )
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
                                com.example.ui.ListeningAnalyticsChartCard(viewModel)
                            }
                            item {
                                DashboardStatsTracker()
                            }
                            item {
                                DashboardLocatorCard(viewModel)
                            }
                            item {
                                com.example.ui.BatteryPowerProfilerCard(settings, viewModel.isCharging.value)
                            }
                            item {
                                com.example.ui.FindMyHeadphonesRadarCard(viewModel)
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
                            com.example.ui.EqualizerDataStoreStatusCard(
                                settings = settings,
                                onImportBands = { bands, name ->
                                    viewModel.saveCustomPreset(name, bands)
                                    viewModel.setPreset(name)
                                }
                            )
                        }
                        item {
                            com.example.ui.SpatialReverbEngineCard(
                                settings = settings,
                                onRoomSizeChange = { size -> viewModel.updateSettings { it.copy(masterGain = size) } },
                                onDecayChange = { },
                                onPresetSelect = { env -> viewModel.setSpatialAudioMode(env) }
                            )
                        }
                        item {
                            SectionHeader(title = "Aanbevolen Sound Profiles")
                            PremiumSoundProfileSelector(
                                activePreset = settings.activePreset,
                                onPresetSelected = { viewModel.setPreset(it) },
                                hasSeenOnboarding = settings.hasSeenAudioProfilesOnboarding,
                                onDismissOnboarding = { viewModel.setAudioProfilesOnboardingSeen(true) }
                            )
                        }
                        item {
                            SectionHeader(title = "Systeem Presets")
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
                            SectionHeader(title = "Geavanceerde Sound Tuning")
                            AdvancedAudioEnhancements(viewModel, settings)
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
                                    val bandLabels = listOf("60Hz", "250Hz", "1kHz", "4kHz", "16kHz")
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
                                        val saveHaptic = LocalHapticFeedback.current
                                        Button(
                                            onClick = {
                                                saveHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                            com.example.ui.Tah6519AncController(
                                viewModel = viewModel,
                                settings = settings
                            )
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
                                    SpatialReverbEngineCard(
                                        viewModel = viewModel,
                                        settings = settings,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
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
                                        val haptic = LocalHapticFeedback.current
                                        PremiumSlider(
                                            value = settings.sidetoneLevel.toFloat(),
                                            onValueChange = { 
                                                val intVal = it.toInt()
                                                if (intVal != settings.sidetoneLevel) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                                viewModel.setSidetoneLevel(intVal) 
                                            },
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

                        // Dedicated Headphone Settings Menu
                        item {
                            com.example.ui.Tah6519HeadphoneSettingsMenuCard(
                                viewModel = viewModel,
                                settings = settings
                            )
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
                                onFetchBattery = { viewModel.fetchBatteryLevel() },
                                settings = settings,
                                viewModel = viewModel
                            )
                        }

                        item {
                            com.example.ui.BatteryTimeRemainingCard(
                                viewModel = viewModel,
                                settings = settings,
                                isCharging = isCharging
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
                                            imageVector = Icons.Filled.Palette,
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
                                                        viewModel.setActiveAppTheme(theme)
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
                                                val nextTheme = if (ThemeState.activeTheme == com.example.ui.theme.AppTheme.HIGH_CONTRAST) com.example.ui.theme.AppTheme.PHILIPS_STUDIO else com.example.ui.theme.AppTheme.HIGH_CONTRAST
                                                viewModel.setThemeMode(com.example.ui.theme.ThemeMode.DARK)
                                                viewModel.setActiveAppTheme(nextTheme)
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
                                                    imageVector = Icons.Filled.Compare,
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
                                                    viewModel.setThemeMode(com.example.ui.theme.ThemeMode.DARK)
                                                    viewModel.setActiveAppTheme(com.example.ui.theme.AppTheme.HIGH_CONTRAST)
                                                } else {
                                                    viewModel.setThemeMode(com.example.ui.theme.ThemeMode.DARK)
                                                    viewModel.setActiveAppTheme(com.example.ui.theme.AppTheme.PHILIPS_STUDIO)
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
                                            imageVector = Icons.Filled.BluetoothDisabled,
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
                                            PremiumSlider(
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
                                                        onClick = { 
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            viewModel.fastForwardAutoOff() 
                                                        },
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

                        // Interactive Manual Card
                        item {
                            SectionHeader(title = "Officiele Handleiding & Knopbediening")
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_manual_card"),
                                colors = CardDefaults.cardColors(containerColor = DarkPanel),
                                border = BorderStroke(1.dp, DarkBorder),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                                contentDescription = null,
                                                tint = AccentPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Philips TAH6519 Handleiding",
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Interactief knoppendiagram, LED status & resetgids",
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { showHardwareManualDialog = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AccentPrimary,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("btn_open_manual")
                                    ) {
                                        Text(
                                            text = "Bekijk Handleiding & LED Gids",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        // BLE Scanner Card
                        item {
                            SectionHeader(title = "Bluetooth LE Scanner & Live GATT")
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_ble_scanner_card"),
                                colors = CardDefaults.cardColors(containerColor = DarkPanel),
                                border = BorderStroke(1.dp, DarkBorder),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(HighlightSky.copy(alpha = 0.2f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Bluetooth,
                                                contentDescription = null,
                                                tint = HighlightSky,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Zoek Nabije Bluetooth LE Apparaten",
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Live RSSI signaalsterkte & TAH6519 GATT services",
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { showBleScannerDialog = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = DarkCard,
                                            contentColor = HighlightSky
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, HighlightSky.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("btn_open_ble_scanner")
                                    ) {
                                        Text(
                                            text = "Open BLE Scanner & Service Explorer",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
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
                                    SpecRow(label = "Model", value = settings.connectedDeviceName)
                                    SpecRow(label = "Type", value = settings.headphoneCategory)
                                    SpecRow(label = "Audiostuurprogramma", value = "${settings.driverSizeMm} mm High-Performance Drivers")
                                    SpecRow(label = "Frequentiebereik", value = "20 Hz - 20.000 Hz")
                                    SpecRow(label = "Maximale Batterijduur", value = if (settings.maxPlaytimeHours > 500) "Onbeperkt (3.5mm Kabel / USB-C Direct)" else "${settings.maxPlaytimeHours} uur (ANC uit) / ${settings.ancPlaytimeHours} uur (ANC aan)")
                                    SpecRow(label = "Oplaadmethode / Verbinding", value = "${settings.connectionType} (${settings.activeCodec})")
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
                                imageVector = Icons.Filled.BatteryAlert,
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
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Sluit Melding",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        connectionSuccessEvent?.let { devName ->
            com.example.ui.Tah6519ConnectionAnimationOverlay(
                deviceName = devName,
                onDismiss = { viewModel.dismissConnectionSuccessEvent() }
            )
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

    val pulseTransition = rememberInfiniteTransition(label = "mini_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mini_pulse_alpha"
    )

    val color = when {
        isCharging -> HighlightSky
        batteryLevel <= 20 -> StatusDanger
        batteryLevel <= 50 -> StatusYellow
        else -> StatusSuccess
    }
    
    val currentFillAlpha = if (isCharging) pulseAlpha else 1f

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
                .border(1.dp, color.copy(alpha = 0.8f * currentFillAlpha), shape = RoundedCornerShape(2.dp))
                .padding(1.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Battery Fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedLevel / 100f)
                    .background(color.copy(alpha = currentFillAlpha), shape = RoundedCornerShape(1.dp))
            )
            
            // If charging, overlay dynamic pulse/flash or icon
            if (isCharging) {
                Icon(
                    imageVector = Icons.Filled.FlashOn,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = pulseAlpha),
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
                .background(color.copy(alpha = 0.8f * currentFillAlpha), shape = RoundedCornerShape(topEnd = 1.dp, bottomEnd = 1.dp))
        )

        Text(
            text = "$batteryLevel%",
            color = if (isCharging) color.copy(alpha = pulseAlpha) else TextPrimary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp
        )
    }
}

@Composable
fun BluetoothConnectivityBadge(
    isConnected: Boolean,
    isConnecting: Boolean = false,
    deviceName: String = "Philips TAH6519",
    onClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bt_badge_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bt_badge_alpha"
    )

    val badgeColor by remember(isConnected, isConnecting) {
        derivedStateOf {
            when {
                isConnected -> Color(0xFF10B981) // Emerald Green
                isConnecting -> HighlightSky // Sky Blue
                else -> StatusDanger // Muted Red
            }
        }
    }

    val iconVector by remember(isConnected, isConnecting) {
        derivedStateOf {
            when {
                isConnected -> Icons.Filled.BluetoothConnected
                isConnecting -> Icons.Filled.BluetoothSearching
                else -> Icons.Filled.BluetoothDisabled
            }
        }
    }

    val labelText by remember(isConnected, isConnecting) {
        derivedStateOf {
            when {
                isConnected -> "BT: Verbonden"
                isConnecting -> "BT: Koppelen..."
                else -> "BT: Offline"
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(
                badgeColor.copy(alpha = if (isConnected || isConnecting) 0.15f else 0.08f),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                1.dp,
                badgeColor.copy(alpha = if (isConnecting) pulseAlpha else 0.4f),
                shape = RoundedCornerShape(20.dp)
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 7.dp, vertical = 2.dp)
            .testTag("bluetooth_connection_status_badge")
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = "Bluetooth Status Icon",
            tint = badgeColor,
            modifier = Modifier
                .size(12.dp)
                .testTag("bluetooth_connectivity_icon")
        )
        Text(
            text = labelText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = badgeColor,
            letterSpacing = 0.3.sp
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
fun BluetoothConnectionStatusIndicator(
    isConnected: Boolean,
    deviceName: String = "Philips TAH6519",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bt_indicator_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bt_pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bt_pulse_alpha"
    )

    val dotColor by remember(isConnected) {
        derivedStateOf { if (isConnected) Color(0xFF10B981) else Color(0xFF9CA3AF) }
    }
    val statusText by remember(isConnected) {
        derivedStateOf { if (isConnected) "Bluetooth Verbonden" else "Niet Verbonden" }
    }
    val indicatorBgColor by remember(isConnected) {
        derivedStateOf { if (isConnected) Color(0xFF10B981).copy(alpha = 0.12f) else DarkCard.copy(alpha = 0.6f) }
    }
    val indicatorBorderColor by remember(isConnected) {
        derivedStateOf { if (isConnected) Color(0xFF10B981).copy(alpha = 0.35f) else DarkBorder }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .background(
                indicatorBgColor,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                1.dp,
                indicatorBorderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .testTag("bluetooth_connection_status_indicator")
    ) {
        Box(
            modifier = Modifier.size(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isConnected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                            alpha = pulseAlpha
                        }
                        .background(dotColor, shape = CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(dotColor, shape = CircleShape)
                    .testTag("bluetooth_status_dot")
            )
        }

        Text(
            text = if (isConnected) "$statusText ($deviceName)" else statusText,
            color = if (isConnected) Color(0xFF10B981) else TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp
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
            .clip(RoundedCornerShape(16.dp))
            .background(DarkPanel)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        DarkBorder,
                        AccentPrimary.copy(alpha = 0.25f),
                        DarkBorder
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
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

data class Tah6519AudioProfile(
    val key: String,
    val title: String,
    val category: String,
    val description: String,
    val driverTuningNote: String,
    val icon: ImageVector,
    val dbBoostBadge: String,
    val bands: List<Float>
)

@Composable
fun Tah6519AudioProfilesOnboardingModal(
    onDismiss: (dontShowAgain: Boolean) -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    var dontShowAgain by remember { mutableStateOf(false) }
    val pagesCount = 3

    Dialog(onDismissRequest = { onDismiss(dontShowAgain) }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkPanel, shape = RoundedCornerShape(20.dp))
                .border(1.5.dp, HighlightSky.copy(alpha = 0.8f), shape = RoundedCornerShape(20.dp))
                .padding(20.dp)
                .testTag("audio_profile_onboarding_modal")
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(AccentPrimary.copy(alpha = 0.2f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Headphones,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Philips TAH6519 Gids",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = { onDismiss(dontShowAgain) },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("onboarding_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Sluiten",
                            tint = TextMuted
                        )
                    }
                }

                HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

                // Animated Page Content
                Crossfade(
                    targetState = currentPage,
                    animationSpec = tween(250),
                    label = "onboarding_page_crossfade"
                ) { page ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (page) {
                            0 -> {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(AccentPrimary.copy(alpha = 0.15f), shape = CircleShape)
                                        .border(1.dp, AccentPrimary.copy(alpha = 0.4f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Headphones,
                                        contentDescription = null,
                                        tint = AccentPrimary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Text(
                                    text = "40mm Acoustic Chamber Tuning",
                                    color = HighlightSky,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Onze audio-profielen zijn specifiek afgesteld op de 40mm Neodymium akoestische kamers van de Philips TAH6519 over-ear hoofdtelefoon. Elk profiel past de DSP-hardware rechtstreeks aan voor maximale geluidskwaliteit.",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                            1 -> {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(HighlightSky.copy(alpha = 0.15f), shape = CircleShape)
                                        .border(1.dp, HighlightSky.copy(alpha = 0.4f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Tune,
                                        contentDescription = null,
                                        tint = HighlightSky,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Text(
                                    text = "Snel Filteren per Categorie",
                                    color = HighlightSky,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Gebruik de categorie-chips (Philips Spec, Bass & Beats, Spraak & Acoustic, Cinema 3D) om snel de juiste sound profile voor jouw muziek, film of podcast te vinden. Schakel in één tik over!",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                            2 -> {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(StatusSuccess.copy(alpha = 0.15f), shape = CircleShape)
                                        .border(1.dp, StatusSuccess.copy(alpha = 0.4f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.GraphicEq,
                                        contentDescription = null,
                                        tint = StatusSuccess,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Text(
                                    text = "Live 10-Band Curve & Hardware Opslag",
                                    color = HighlightSky,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Bekijk de exacte 10-bands frequentie-uitslag van 60Hz tot 16kHz bij elk profiel. De instelling blijft direct actief in de TAH6519, zelfs als je verandert van afspeelapparaat!",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pagesCount) { i ->
                        val isCurrent = currentPage == i
                        Box(
                            modifier = Modifier
                                .size(if (isCurrent) 10.dp else 6.dp)
                                .background(
                                    color = if (isCurrent) HighlightSky else TextMuted.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .clickable { currentPage = i }
                        )
                    }
                }

                // Checkbox Don't show again
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clickable { dontShowAgain = !dontShowAgain }
                        .padding(vertical = 2.dp)
                ) {
                    Checkbox(
                        checked = dontShowAgain,
                        onCheckedChange = { dontShowAgain = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = HighlightSky,
                            uncheckedColor = TextMuted
                        ),
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("dont_show_again_checkbox")
                    )
                    Text(
                        text = "Niet meer automatisch tonen",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                // Navigation buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentPage > 0) {
                        OutlinedButton(
                            onClick = { currentPage-- },
                            border = BorderStroke(1.dp, DarkBorder),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("onboarding_prev_btn")
                        ) {
                            Text(text = "Vorige", fontSize = 11.sp, color = TextPrimary)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = {
                            if (currentPage < pagesCount - 1) {
                                currentPage++
                            } else {
                                onDismiss(dontShowAgain)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HighlightSky),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("onboarding_next_btn")
                    ) {
                        Text(
                            text = if (currentPage < pagesCount - 1) "Volgende" else "Begrepen & Starten",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Tah6519AudioProfilesCard(
    activePreset: String?,
    onPresetSelected: (String) -> Unit,
    hasSeenOnboarding: Boolean = true,
    onDismissOnboarding: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var selectedCategory by remember { mutableStateOf("Alle") }
    var showOnboardingDialog by remember { mutableStateOf(!hasSeenOnboarding) }
    val categories = listOf("Alle", "Philips Spec", "Bass & Beats", "Spraak & Acoustic", "Cinema & Spatial", "Studio")

    if (showOnboardingDialog) {
        Tah6519AudioProfilesOnboardingModal(
            onDismiss = { dontShowAgain ->
                showOnboardingDialog = false
                if (dontShowAgain) {
                    onDismissOnboarding()
                }
            }
        )
    }

    val profiles = listOf(
        Tah6519AudioProfile(
            key = "Philips Signature",
            title = "Philips Signature Sound",
            category = "Philips Spec",
            description = "Officiële Philips Studio acoustic tuning voor 40mm neodymium drivers. Rijke warme bas gecombineerd met sprankelend detail.",
            driverTuningNote = "Afgesteld op TAH6519 Acoustic Chamber",
            icon = Icons.Filled.Headphones,
            dbBoostBadge = "+3.5dB Bass / +3.5dB Highs",
            bands = listOf(3.5f, 2.5f, 1.0f, 0.0f, -0.5f, 0.5f, 1.5f, 2.5f, 3.5f, 2.5f)
        ),
        Tah6519AudioProfile(
            key = "Bass Boost",
            title = "Dynamic Deep Bass",
            category = "Bass & Beats",
            description = "Krachtige sub-bas versterking (+8.5dB @ 60Hz) met volle lage tonen voor EDM, Hip-Hop en stevige beats.",
            driverTuningNote = "Maximale uitslag 40mm Neodymium Drivers",
            icon = Icons.Filled.Hearing,
            dbBoostBadge = "+8.5dB Sub-Bass",
            bands = listOf(8.5f, 7.0f, 5.0f, 2.5f, 0.0f, -0.5f, 0.0f, 0.5f, 1.0f, 0.5f)
        ),
        Tah6519AudioProfile(
            key = "Acoustic",
            title = "Acoustic & Unplugged",
            category = "Spraak & Acoustic",
            description = "Natuurlijke harmonische respons geoptimaliseerd voor akoestische gitaar, piano en intieme vocale opnames.",
            driverTuningNote = "Lineaire Midrange Akoestische Helderheid",
            icon = Icons.Filled.MusicNote,
            dbBoostBadge = "+3.5dB Akoestisch Detail",
            bands = listOf(1.5f, 2.0f, 2.5f, 1.5f, 0.5f, 1.0f, 2.0f, 3.0f, 3.5f, 2.0f)
        ),
        Tah6519AudioProfile(
            key = "Voice Clarity",
            title = "Spraak & Podcast Helderheid",
            category = "Spraak & Acoustic",
            description = "Versterkt vocalen (+5.5dB @ 2kHz) en filtert storende lage bromtonen weg voor podcasts, audioboeken en videobellen.",
            driverTuningNote = "Vocal Frequency Band Pass Filter",
            icon = Icons.Filled.Mic,
            dbBoostBadge = "+5.5dB Vocal Speech",
            bands = listOf(-3.0f, -2.0f, 0.0f, 2.5f, 5.0f, 5.5f, 4.0f, 2.0f, -1.0f, -2.0f)
        ),
        Tah6519AudioProfile(
            key = "Treble Sparkle",
            title = "Treble & Detail Sparkle",
            category = "Studio",
            description = "Kraakhelder hoge-frequentie bereik voor klassieke muziek, strijkers, cymbals en fijne instrumentale details.",
            driverTuningNote = "High Frequency Extension 8kHz-16kHz",
            icon = Icons.Filled.GraphicEq,
            dbBoostBadge = "+8.0dB High Sparkle",
            bands = listOf(-1.0f, -0.5f, 0.0f, 0.5f, 1.5f, 2.5f, 4.5f, 6.5f, 8.0f, 6.5f)
        ),
        Tah6519AudioProfile(
            key = "Cinema 3D",
            title = "Cinematic 3D Spatial Stage",
            category = "Cinema & Spatial",
            description = "Brede ruimtelijke geluidsweergave met diepe filmische bas en heldere dialoogweergave voor films en gaming.",
            driverTuningNote = "Spatial Soundstage Driver Alignment",
            icon = Icons.Filled.Movie,
            dbBoostBadge = "3D Spatial Surround",
            bands = listOf(6.0f, 4.5f, 2.0f, 0.0f, -1.0f, 1.0f, 3.0f, 4.0f, 4.5f, 3.5f)
        ),
        Tah6519AudioProfile(
            key = "Flat",
            title = "Studio Flat Reference",
            category = "Studio",
            description = "Lineaire referentie-kalibratie zonder enige kleuring. Ideaal voor audio-puristen en muziekproductie.",
            driverTuningNote = "Ongekleurde Neutrale Respons",
            icon = Icons.Filled.Tune,
            dbBoostBadge = "0.0dB Flat Reference",
            bands = listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
        )
    )

    val filteredProfiles = remember(selectedCategory) {
        if (selectedCategory == "Alle") profiles
        else profiles.filter { it.category == selectedCategory }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkPanel, shape = RoundedCornerShape(16.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("tah6519_audio_profiles_card")
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(AccentPrimary.copy(alpha = 0.2f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Headphones,
                            contentDescription = null,
                            tint = AccentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Philips TAH6519 Audio Profielen",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .background(AccentPrimary, shape = RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "40mm TUNED",
                                    color = Color.White,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Speciaal gecalibreerd voor 40mm Neodymium Acoustic Chamber & Hybrid ANC",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                IconButton(
                    onClick = { showOnboardingDialog = true },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("audio_profiles_info_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "Hoe Werken Audio Profielen?",
                        tint = HighlightSky,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) HighlightSky else DarkBg,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) HighlightSky else DarkBorder.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedCategory = cat
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("category_chip_$cat")
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color.White else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(color = DarkBorder.copy(alpha = 0.4f))

            // Profile buttons / cards
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                filteredProfiles.forEach { profile ->
                    val isSelected = activePreset == profile.key
                    val cardBorderColor by animateColorAsState(
                        targetValue = if (isSelected) HighlightSky else DarkBorder.copy(alpha = 0.6f),
                        animationSpec = tween(200),
                        label = "tah6519_profile_border"
                    )
                    val cardBgColor by animateColorAsState(
                        targetValue = if (isSelected) HighlightSky.copy(alpha = 0.12f) else DarkBg,
                        animationSpec = tween(200),
                        label = "tah6519_profile_bg"
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, cardBorderColor, shape = RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPresetSelected(profile.key)
                            }
                            .testTag("audio_profile_btn_${profile.key.replace(" ", "_")}")
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
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(
                                                if (isSelected) HighlightSky.copy(alpha = 0.25f) else DarkPanel,
                                                shape = RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = profile.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) HighlightSky else AccentPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = profile.title,
                                                color = if (isSelected) HighlightSky else TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(HighlightSky, shape = RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "ACTIEF ON-AIR",
                                                        color = Color.White,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = profile.driverTuningNote,
                                            color = TextMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                }

                                // Quick Apply Button
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onPresetSelected(profile.key)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) HighlightSky else DarkPanel
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = if (isSelected) "Gekozen" else "Activeren",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                }
                            }

                            Text(
                                text = profile.description,
                                color = TextMuted,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )

                            // Specs & Frequency Curve Canvas
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // dB Boost pill
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) HighlightSky.copy(alpha = 0.2f) else DarkPanel,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) HighlightSky.copy(alpha = 0.4f) else DarkBorder,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = profile.dbBoostBadge,
                                        color = if (isSelected) HighlightSky else AccentPrimary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Mini Canvas 10-Band Curve
                                Box(
                                    modifier = Modifier
                                        .width(70.dp)
                                        .height(26.dp)
                                        .padding(horizontal = 2.dp)
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val w = size.width
                                        val h = size.height
                                        val points = profile.bands.mapIndexed { i, gain ->
                                            val x = (i.toFloat() / 9f) * w
                                            val ratio = ((gain + 12f) / 24f).coerceIn(0f, 1f)
                                            val y = h * (1f - ratio)
                                            Offset(x, y)
                                        }
                                        val path = Path().apply {
                                            moveTo(points[0].x, points[0].y)
                                            for (i in 0 until points.size - 1) {
                                                val p0 = points[i]
                                                val p1 = points[i + 1]
                                                val cx = (p0.x + p1.x) / 2f
                                                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                                            }
                                        }
                                        drawPath(
                                            path = path,
                                            color = if (isSelected) HighlightSky else AccentPrimary.copy(alpha = 0.6f),
                                            style = Stroke(width = 2.dp.toPx())
                                        )
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
fun PremiumSoundProfileSelector(
    activePreset: String?,
    onPresetSelected: (String) -> Unit,
    hasSeenOnboarding: Boolean = true,
    onDismissOnboarding: () -> Unit = {}
) {
    Tah6519AudioProfilesCard(
        activePreset = activePreset,
        onPresetSelected = onPresetSelected,
        hasSeenOnboarding = hasSeenOnboarding,
        onDismissOnboarding = onDismissOnboarding
    )
}

@Composable
fun AdvancedAudioEnhancements(
    viewModel: HeadphoneViewModel,
    settings: com.example.data.HeadphoneSettings
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            DarkBorder,
                            HighlightSky.copy(alpha = 0.25f),
                            DarkBorder
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Feature 1: Spatial Audio / Soundstage Virtualizer
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = HighlightSky,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Ruimtelijk Geluid (Spatial Audio)",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        
                        // Current mode indicator badge
                        Box(
                            modifier = Modifier
                                .background(HighlightSky.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = when (settings.spatialAudioMode) {
                                    "Stereo" -> "Stereo Classic"
                                    "Live Concert" -> "Concert Hall"
                                    "Cinematic 3D" -> "3D Cinema"
                                    else -> "Studio Vocal"
                                },
                                color = HighlightSky,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Text(
                        text = "Creëer een meeslepende 3D-geluidservaring door de akoestiek van de luisterruimte te simuleren.",
                        color = TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val modes = listOf("Stereo", "Live Concert", "Cinematic 3D", "Acoustic Studio")
                        modes.forEach { mode ->
                            val isSelected = settings.spatialAudioMode == mode
                            Button(
                                onClick = { viewModel.setSpatialAudioMode(mode) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                                    .testTag("spatial_mode_$mode"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) HighlightSky.copy(alpha = 0.2f) else DarkPanel,
                                    contentColor = if (isSelected) HighlightSky else TextMuted
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) HighlightSky else DarkBorder
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = when (mode) {
                                        "Stereo" -> "Off"
                                        "Live Concert" -> "Concert"
                                        "Cinematic 3D" -> "Cinema"
                                        else -> "Studio"
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    SpatialReverbEngineCard(
                        viewModel = viewModel,
                        settings = settings
                    )
                }

                HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                // Feature 2: High Resolution LDAC Streaming Quality
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
                                imageVector = Icons.Filled.Headphones,
                                contentDescription = null,
                                tint = if (settings.ldacEnabled) HighlightSky else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "LDAC Hi-Res Audio Bitrate",
                                color = if (settings.ldacEnabled) TextPrimary else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        
                        Text(
                            text = if (settings.ldacEnabled) "Actief (96kHz)" else "Inactief (SBC/AAC)",
                            color = if (settings.ldacEnabled) StatusSuccess else TextMuted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Beheer de Bluetooth bandbreedte. Hogere bitrate biedt studiokwaliteit, lagere biedt meer stabiliteit.",
                        color = TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val bitrates = listOf("Optimized (990kbps)", "Balanced (660kbps)", "Best Effort (330kbps)")
                        bitrates.forEach { opt ->
                            val isSelected = settings.ldacQualityMode == opt
                            val enabled = settings.ldacEnabled
                            Button(
                                onClick = { if (enabled) viewModel.setLdacQualityMode(opt) },
                                enabled = enabled,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                                    .testTag("ldac_mode_$opt"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected && enabled) HighlightSky.copy(alpha = 0.2f) else if (enabled) DarkPanel else DarkPanel.copy(alpha = 0.4f),
                                    contentColor = if (isSelected && enabled) HighlightSky else TextMuted,
                                    disabledContainerColor = DarkPanel.copy(alpha = 0.2f),
                                    disabledContentColor = TextMuted.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected && enabled) HighlightSky else DarkBorder
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = when (opt) {
                                        "Optimized (990kbps)" -> "990k Ultra"
                                        "Balanced (660kbps)" -> "660k HD"
                                        else -> "330k Eco"
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                // Feature 3: Dynamic Bass Boost (DBB) Level Selector
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
                                imageVector = Icons.Filled.Hearing,
                                contentDescription = null,
                                tint = if (settings.dynamicBassEnabled) HighlightSky else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Dynamic Bass Boost (DBB) Niveau",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            text = when (settings.dynamicBassLevel) {
                                0 -> "Uit"
                                1 -> "Warm (+3dB)"
                                2 -> "Punch (+6dB)"
                                else -> "Thunder (+10dB)"
                            },
                            color = if (settings.dynamicBassEnabled) HighlightSky else TextMuted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Versterk de laagste tonen in real-time. Past zich automatisch aan op het volume ter bescherming van de gehoorgang.",
                        color = TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val levels = listOf(0, 1, 2, 3)
                        levels.forEach { level ->
                            val isSelected = settings.dynamicBassLevel == level
                            Button(
                                onClick = { viewModel.setDynamicBassLevel(level) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                                    .testTag("bass_level_$level"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) HighlightSky.copy(alpha = 0.2f) else DarkPanel,
                                    contentColor = if (isSelected) HighlightSky else TextMuted
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) HighlightSky else DarkBorder
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = when (level) {
                                        0 -> "Off"
                                        1 -> "Warm"
                                        2 -> "Punch"
                                        else -> "Thunder"
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                // Feature 4: ANC Acoustic Bass Compensation Engine
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = null,
                                tint = if (settings.ancCompensationEnabled) HighlightSky else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ANC Akoestische Compensatie",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Herstelt sub-bass drukverlies veroorzaakt door actieve ruisonderdrukking.",
                            color = TextMuted,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }

                    Switch(
                        checked = settings.ancCompensationEnabled,
                        onCheckedChange = { viewModel.toggleAncCompensation(it) },
                        modifier = Modifier.testTag("anc_compensation_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = HighlightSky,
                            checkedTrackColor = HighlightSky.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkPanel
                        )
                    )
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
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "eq_slider_thumb_size"
    )

    val thumbElevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 2.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "eq_slider_thumb_elevation"
    )

    val thumbHaloPadding by animateDpAsState(
        targetValue = if (isDragging) 6.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioHighBouncy),
        label = "eq_slider_thumb_halo"
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
            val thumbOffsetDp by animateDpAsState(
                targetValue = (122 * ratio).dp,
                animationSpec = spring(
                    stiffness = if (isDragging) Spring.StiffnessHigh else Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioMediumBouncy
                ),
                label = "eq_slider_thumb_offset"
            )

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

            // Spring-animated halo glow ring during drag
            if (isDragging || thumbHaloPadding > 0.dp) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = (thumbOffsetDp - thumbHaloPadding / 2).coerceAtLeast(0.dp))
                        .size(thumbSize + thumbHaloPadding)
                        .background(color.copy(alpha = glowAlpha), shape = CircleShape)
                )
            }

            // Circle thumb knob perfectly aligned with spring physics
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = thumbOffsetDp)
                    .shadow(thumbElevation, shape = CircleShape)
                    .size(thumbSize)
                    .background(color, shape = CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.95f), shape = CircleShape)
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
            val haptic = LocalHapticFeedback.current
            PremiumSlider(
                value = gain,
                onValueChange = { 
                    val intVal = it.toInt()
                    if (intVal != gain.toInt()) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onGainChange(it) 
                },
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
        level <= 20 -> Icons.Filled.BatteryAlert
        else -> Icons.Filled.BatteryFull
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
                    imageVector = Icons.Filled.Warning,
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
                        imageVector = Icons.Filled.CheckCircle,
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
                            imageVector = Icons.Filled.Bolt,
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
    batteryLevel: Int = 85,
    isCharging: Boolean = false,
    isFetchingBattery: Boolean,
    batteryFetchProgress: Float,
    batteryFetchStatus: String,
    onFetchBattery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val levelColor = when {
        isCharging -> Color(0xFF00E676)
        batteryLevel > 50 -> Color(0xFF00E676)
        batteryLevel > 20 -> Color(0xFFFFD600)
        else -> Color(0xFFFF3D00)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBg.copy(alpha = 0.6f), shape = RoundedCornerShape(12.dp))
            .border(1.dp, DarkBorder.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(levelColor.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, levelColor.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryStd,
                        contentDescription = "Battery Status",
                        tint = levelColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Batterijniveau Koptelefoon",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isCharging) "Opladen via USB-C • Fast Charge" else "Actuele Lading: $batteryLevel%",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
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
                        .height(30.dp)
                        .testTag("fetch_battery_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Ophalen",
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Verversen",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = "${(batteryFetchProgress * 100).toInt()}%",
                    color = HighlightSky,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Animated Battery Progress Meter Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(DarkPanel, shape = RoundedCornerShape(5.dp))
                .border(0.5.dp, DarkBorder, shape = RoundedCornerShape(5.dp))
                .padding(1.dp)
                .testTag("battery_level_indicator")
        ) {
            val targetWidthFraction = if (isFetchingBattery) batteryFetchProgress.coerceIn(0.05f, 1f) else (batteryLevel / 100f).coerceIn(0.02f, 1f)
            val animatedWidth by animateFloatAsState(
                targetValue = targetWidthFraction,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "battery_meter_fill"
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedWidth)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = if (isFetchingBattery) {
                                listOf(AccentPrimary, HighlightSky)
                            } else {
                                listOf(levelColor.copy(alpha = 0.7f), levelColor)
                            }
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }

        if (isFetchingBattery) {
            Text(
                text = batteryFetchStatus,
                color = HighlightSky,
                fontSize = 10.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (batteryFetchStatus.isNotEmpty() && batteryFetchStatus != "Idle") batteryFetchStatus else "Status: Verbonden via Bluetooth GATT",
                    color = TextMuted,
                    fontSize = 10.sp
                )
                Text(
                    text = "$batteryLevel% Lading",
                    color = levelColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BatteryCircularProgressBar(
    batteryLevel: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 54.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 5.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (batteryLevel / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "circular_battery_progress"
    )

    val color = when {
        isCharging -> AccentPrimary
        batteryLevel > 50 -> StatusSuccess
        batteryLevel > 20 -> StatusYellow
        else -> StatusDanger
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .testTag("battery_circular_progress_bar")
    ) {
        // Track Ring
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = DarkBorder.copy(alpha = 0.5f),
            strokeWidth = strokeWidth,
            trackColor = Color.Transparent,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        // Active Progress Ring
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxSize(),
            color = color,
            strokeWidth = strokeWidth,
            trackColor = Color.Transparent,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isCharging) {
                Icon(
                    imageVector = Icons.Filled.FlashOn,
                    contentDescription = "Opladen",
                    tint = AccentPrimary,
                    modifier = Modifier.size(10.dp)
                )
            }
            Text(
                text = "${batteryLevel}%",
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.testTag("battery_large_percentage")
            )
        }
    }
}

@Composable
fun IntelligentBatteryPreservationCard(
    settings: HeadphoneSettings,
    viewModel: HeadphoneViewModel,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isPreservationEnabled = settings.intelligentBatteryPreservationEnabled
    val isPreservationActive = settings.isBatteryPreservationActive
    val threshold = settings.intelligentBatteryThreshold
    val batteryLevel = settings.batteryLevel
    val pollingIntervalMs = settings.bluetoothPollingIntervalMs
    val ancCap = settings.batteryPreservationAncCap

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("intelligent_battery_preservation_card"),
        colors = CardDefaults.cardColors(
            containerColor = if (isPreservationActive) StatusSuccess.copy(alpha = 0.08f) else DarkCard
        ),
        border = BorderStroke(
            width = if (isPreservationActive) 1.5.dp else 1.dp,
            color = if (isPreservationActive) StatusSuccess else DarkBorder
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (isPreservationActive) StatusSuccess.copy(alpha = 0.2f) else AccentPrimary.copy(alpha = 0.15f),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                if (isPreservationActive) StatusSuccess else AccentPrimary.copy(alpha = 0.3f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BatterySaver,
                            contentDescription = null,
                            tint = if (isPreservationActive) StatusSuccess else AccentPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Intelligent Battery Preservation",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Dynamische ANC & Bluetooth Polling Aanpassing",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Switch(
                    checked = isPreservationEnabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleIntelligentBatteryPreservation(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = StatusSuccess,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkBg
                    ),
                    modifier = Modifier
                        .scale(0.85f)
                        .testTag("intelligent_battery_preservation_switch")
                )
            }

            HorizontalDivider(color = DarkBorder.copy(alpha = 0.4f))

            // Dynamic Active Banner
            if (isPreservationActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StatusSuccess.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .border(1.dp, StatusSuccess.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ElectricBolt,
                        contentDescription = null,
                        tint = StatusSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⚡ BESPAARMODUS ACTIEF (Accu ${batteryLevel}% ≤ ${threshold}%)",
                            color = StatusSuccess,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "ANC dynamisch begrensd op Niveau $ancCap (Eco) • Bluetooth Polling Vertraagd tot 5s (Eco Telemetrie)",
                            color = TextPrimary,
                            fontSize = 10.sp
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .border(1.dp, DarkBorder.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isPreservationEnabled) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                        contentDescription = null,
                        tint = if (isPreservationEnabled) StatusSuccess else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isPreservationEnabled) {
                            "Stand-by: Activeert automatisch zodra batterij ≤ ${threshold}% zakt"
                        } else {
                            "Uitgeschakeld: Schakel in voor automatische energiebesparing bij lage acculading"
                        },
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            // Real-time Dynamic Adjustment Parameters Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Parameter 1: ANC Cap Intensity
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DarkBg, RoundedCornerShape(10.dp))
                        .border(
                            1.dp,
                            if (isPreservationActive) StatusSuccess.copy(alpha = 0.3f) else DarkBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = HighlightSky,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "ANC Intensiteit Cap",
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }
                        Text(
                            text = if (isPreservationActive) "Niveau $ancCap (Eco Capped)" else "Niveau $ancCap Max bij Low Power",
                            color = if (isPreservationActive) StatusSuccess else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Parameter 2: Bluetooth Telemetry Polling Rate
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DarkBg, RoundedCornerShape(10.dp))
                        .border(
                            1.dp,
                            if (isPreservationActive) StatusSuccess.copy(alpha = 0.3f) else DarkBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.BluetoothSearching,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "BT Polling Rate",
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }
                        Text(
                            text = "${pollingIntervalMs}ms (${if (pollingIntervalMs > 2000) "Eco 0.2Hz" else "Normaal 1.0Hz"})",
                            color = if (pollingIntervalMs > 2000) StatusSuccess else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Low Power Threshold Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Activeringsdrempel Batterij:",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "≤ $threshold%",
                        color = AccentPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val thresholdOptions = listOf(15, 20, 25, 30)
                    thresholdOptions.forEach { option ->
                        val isSelected = threshold == option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (isSelected) AccentPrimary.copy(alpha = 0.2f) else DarkBg,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) AccentPrimary else DarkBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.setIntelligentBatteryThreshold(option)
                                }
                                .padding(vertical = 8.dp)
                                .testTag("preservation_threshold_${option}_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$option%",
                                color = if (isSelected) AccentPrimary else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
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
    onFetchBattery: () -> Unit = {},
    settings: HeadphoneSettings? = null,
    viewModel: HeadphoneViewModel? = null
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
            .clip(RoundedCornerShape(16.dp))
            .background(DarkPanel)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        DarkBorder,
                        HighlightSky.copy(alpha = 0.25f),
                        DarkBorder
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
            .testTag("battery_status_card")
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
                    imageVector = Icons.Filled.BatteryUnknown,
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
                                isCharging -> Icons.Filled.BatteryChargingFull
                                batteryLevel <= 20 -> Icons.Filled.BatteryAlert
                                else -> Icons.Filled.BatteryFull
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
                    
                    BatteryCircularProgressBar(
                        batteryLevel = animatedBatteryLevel.toInt(),
                        isCharging = isCharging
                    )
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

                BatteryFetcherComponent(
                    batteryLevel = batteryLevel,
                    isCharging = isCharging,
                    isFetchingBattery = isFetchingBattery,
                    batteryFetchProgress = batteryFetchProgress,
                    batteryFetchStatus = batteryFetchStatus,
                    onFetchBattery = onFetchBattery,
                    modifier = Modifier.fillMaxWidth()
                )

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
                                imageVector = Icons.Filled.AccessTime,
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
                        icon = Icons.Filled.GraphicEq,
                        isActive = ancMode != "OFF",
                        drainText = if (ancMode != "OFF") "-40u accuduur" else "+40u bespaard",
                        isPositive = ancMode == "OFF"
                    )

                    // Consumer 2: LDAC
                    PowerConsumerRow(
                        label = "Hi-Res LDAC Codec",
                        icon = Icons.Filled.MusicNote,
                        isActive = ldacEnabled,
                        drainText = if (ldacEnabled) "-20u accuduur" else "+20u bespaard",
                        isPositive = !ldacEnabled
                    )

                    // Consumer 3: Bass Boost
                    PowerConsumerRow(
                        label = "Dynamic Bass Boost",
                        icon = Icons.Filled.Hearing,
                        isActive = bassEnabled,
                        drainText = if (bassEnabled) "-4u accuduur" else "Zuinig",
                        isPositive = !bassEnabled
                    )

                    // Consumer 4: Intelligent Battery Preservation
                    PowerConsumerRow(
                        label = "Intelligent Battery Preservation",
                        icon = Icons.Filled.BatterySaver,
                        isActive = settings?.isBatteryPreservationActive == true,
                        drainText = if (settings?.isBatteryPreservationActive == true) "⚡ Actief (ANC Level ${settings.batteryPreservationAncCap} & BT 5s)" else if (settings?.intelligentBatteryPreservationEnabled == true) "Stand-by (≤${settings.intelligentBatteryThreshold}%)" else "Uitgeschakeld",
                        isPositive = settings?.intelligentBatteryPreservationEnabled == true
                    )
                }

                if (settings != null && viewModel != null) {
                    IntelligentBatteryPreservationCard(
                        settings = settings,
                        viewModel = viewModel
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
                            val switchHaptic = LocalHapticFeedback.current
                            Switch(
                                checked = isCharging,
                                onCheckedChange = { 
                                    switchHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onToggleCharging(it) 
                                },
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
                    
                    val haptic = LocalHapticFeedback.current
                    PremiumSlider(
                        value = batteryLevel.toFloat(),
                        onValueChange = { 
                            val intVal = it.toInt()
                            if (intVal != batteryLevel) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onBatteryChange(intVal) 
                        },
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ACTIEVE RUISONDERDRUKKING (ANC)",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
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
            PhilipsPremiumSwitch(
                checked = activeMode != "OFF",
                onCheckedChange = { isChecked ->
                    if (isChecked) {
                        onModeChange("ON")
                    } else {
                        onModeChange("OFF")
                    }
                },
                modifier = Modifier.testTag("anc_toggle_switch"),
                activeColor = HighlightSky
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
                Triple("ON", "ANC On", Icons.Filled.GraphicEq),
                Triple("TRANSPARENCY", "Awareness", Icons.Filled.Hearing),
                Triple("OFF", "ANC Off", Icons.Filled.Close)
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
                    imageVector = Icons.Filled.Devices,
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
                            imageVector = Icons.Filled.Devices,
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
                                    deviceName.contains("Computer", ignoreCase = true) -> Icons.Filled.Laptop
                                    deviceName.contains("TV", ignoreCase = true) ||
                                    deviceName.contains("Television", ignoreCase = true) -> Icons.Filled.Tv
                                    else -> Icons.Filled.PhoneAndroid
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
                                                    imageVector = Icons.Filled.SwapVert,
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
                                                imageVector = Icons.Filled.Close,
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
                                    imageVector = Icons.Filled.Add,
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
                                imageVector = Icons.Filled.Info,
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
                                    imageVector = Icons.Filled.MoreVert,
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
                                            Icons.Filled.Edit,
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
                                            Icons.Filled.Delete,
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
                    imageVector = Icons.Filled.Hearing,
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
                        imageVector = Icons.Filled.CheckCircle,
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
    var currentSliderVal by remember(step) { mutableStateOf(50f) }
    
    // threshold values for each frequency
    val hearingSensitivity = remember { mutableStateListOf(50f, 50f, 50f, 50f) }
    
    val toneGenerator = remember { SineWaveGenerator() }
    
    LaunchedEffect(step, currentSliderVal) {
        if (step in 1..4) {
            val frequenciesFloat = listOf(250f, 1000f, 4000f, 8000f)
            val freq = frequenciesFloat[step - 1]
            val volumeFraction = (currentSliderVal / 100f) * 0.35f
            toneGenerator.startTone(freq, volumeFraction)
        } else {
            toneGenerator.stopTone()
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            toneGenerator.stopTone()
        }
    }
    
    Dialog(onDismissRequest = {
        toneGenerator.stopTone()
        onDismiss()
    }) {
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
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Sluit", tint = TextMuted)
                    }
                }
                
                HorizontalDivider(color = DarkBorder)
                
                if (step == 0) {
                    // Introduction
                    Icon(
                        imageVector = Icons.Filled.SelfImprovement,
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
                                imageVector = Icons.Filled.GraphicEq,
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
                        val haptic = LocalHapticFeedback.current
                        PremiumSlider(
                            value = currentSliderVal,
                            onValueChange = { 
                                val intVal = it.toInt()
                                if (intVal != currentSliderVal.toInt()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                currentSliderVal = it 
                            },
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
                        imageVector = Icons.Filled.Analytics,
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
    
    val synthesizer = remember { SoundscapeSynthesizer() }
    
    LaunchedEffect(isPlaying, selectedSound, soundVolume) {
        if (isPlaying) {
            synthesizer.startSoundscape(selectedSound, soundVolume / 100f)
        } else {
            synthesizer.stopSoundscape()
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            synthesizer.stopSoundscape()
        }
    }
    
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
                        imageVector = Icons.Filled.Spa,
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
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
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
                    Icon(imageVector = Icons.Filled.Timer, contentDescription = "Timer", tint = TextMuted, modifier = Modifier.size(16.dp))
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
                val haptic = LocalHapticFeedback.current
                PremiumSlider(
                    value = soundVolume,
                    onValueChange = { 
                        val intVal = it.toInt()
                        if (intVal != soundVolume.toInt()) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        soundVolume = it 
                    },
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
                        imageVector = Icons.Filled.Analytics,
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
    val isFirmwarePolling by viewModel.isFirmwarePolling.collectAsStateWithLifecycle()
    val lastFirmwarePollTime by viewModel.lastFirmwarePollTime.collectAsStateWithLifecycle()
    val simulatedFirmwareApiUrl by viewModel.simulatedFirmwareApiUrl.collectAsStateWithLifecycle()
    val simulatedApiHttpStatus by viewModel.simulatedApiHttpStatus.collectAsStateWithLifecycle()
    val isApiPollingInProgress by viewModel.isApiPollingInProgress.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("firmware_version_card")
            .background(DarkPanel, shape = RoundedCornerShape(12.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Title and Status Badge Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(HighlightSky.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, HighlightSky.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SystemUpdate,
                            contentDescription = "Firmware Polling Service",
                            tint = HighlightSky,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Firmware Update Service",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Huidige Versie: $firmwareVersion",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Small badge showing version status
                Box(
                    modifier = Modifier
                        .background(
                            if (firmwareVersion == "v1.5.0") StatusSuccess.copy(alpha = 0.1f) else StatusYellow.copy(alpha = 0.15f),
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            1.dp,
                            if (firmwareVersion == "v1.5.0") StatusSuccess.copy(alpha = 0.3f) else StatusYellow.copy(alpha = 0.4f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (firmwareVersion == "v1.5.0") "Up-to-date (v1.5.0)" else "Update beschikbaar",
                        color = if (firmwareVersion == "v1.5.0") StatusSuccess else StatusYellow,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Simulated API Endpoint Polling Information Panel
            Surface(
                color = DarkBg,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (isApiPollingInProgress) StatusYellow else if (isFirmwarePolling) StatusSuccess else TextMuted,
                                        CircleShape
                                    )
                            )
                            Text(
                                text = "Simulated OTA API Endpoint",
                                color = TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "HTTP $simulatedApiHttpStatus OK",
                            color = StatusSuccess,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }

                    Text(
                        text = simulatedFirmwareApiUrl,
                        color = HighlightSky,
                        fontSize = 9.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        maxLines = 1
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Laatste Poll: ${lastFirmwarePollTime ?: "Bezig..."}",
                            color = TextMuted,
                            fontSize = 10.sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Auto-Polling (30s)",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                            Switch(
                                checked = isFirmwarePolling,
                                onCheckedChange = { viewModel.toggleFirmwarePolling(it) },
                                modifier = Modifier
                                    .scale(0.7f)
                                    .testTag("firmware_polling_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = HighlightSky,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = DarkBorder
                                )
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = DarkBorder)

            when (val state = updateState) {
                is UpdateState.Idle -> {
                    Text(
                        text = "De periodieke polling-service controleert de Philips OTA-server op nieuwe versies voor de TAH6519.",
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Nu Controleren via API",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
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
                            text = "Aanvraag verzenden naar Philips OTA API...",
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
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Up to date",
                                tint = StatusSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Je Philips TAH6519 heeft de nieuwste firmware ($firmwareVersion).",
                                color = StatusSuccess,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { viewModel.pollFirmwareApi(manual = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .testTag("btn_recheck_firmware")
                        ) {
                            Text(text = "Opnieuw Controleren", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                .background(StatusYellow.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .border(1.dp, StatusYellow.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "Update beschikbaar",
                                tint = StatusYellow,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Nieuwe Firmware Gedetecteerd: ${state.version}",
                                    color = StatusYellow,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Huidige versie $firmwareVersion ➔ Nieuwe versie ${state.version}",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
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
                                    text = "NIEUW IN VERSIE ${state.version}:",
                                    color = HighlightSky,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                state.changelog.forEach { bullet ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("•", color = HighlightSky, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = bullet,
                                            color = TextMuted,
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
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
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HighlightSky,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1.6f)
                                    .height(40.dp)
                                    .testTag("btn_install_firmware_update")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "UPDATE NU",
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
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
                            imageVector = Icons.Filled.CheckCircle,
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

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * kotlin.math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
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
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        DarkBorder,
                        AccentPrimary.copy(alpha = 0.35f),
                        DarkBorder
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
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
                // Compute sine wave amplitude with pulse scale and sliding wavePhase
                val x = i * (barWidth + gap) + barWidth / 2f
                val distanceToCenter = kotlin.math.abs(x - width / 2f)
                val factor = (1f - (distanceToCenter / (width / 2f))).coerceIn(0f, 1f)
                
                // Add flowing wave variation using i and wavePhase
                val waveHeight = (25.dp.toPx() + kotlin.math.sin(i * 0.3f - wavePhase) * 15.dp.toPx()) * factor * pulseScale
                
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
                    BluetoothConnectionStatusIndicator(
                        isConnected = settings.connected,
                        deviceName = settings.connectedDeviceName.ifBlank { "Philips TAH6519" }
                    )
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
                                imageVector = Icons.Filled.GraphicEq,
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
                                imageVector = Icons.Filled.HighQuality,
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
                        Icon(imageVector = Icons.Filled.SkipPrevious, contentDescription = "Vorig nummer", tint = TextPrimary)
                    }
                    
                    IconButton(
                        onClick = { viewModel.toggleMediaPlayer() },
                        modifier = Modifier
                            .size(38.dp)
                            .background(AccentPrimary, shape = CircleShape)
                            .testTag("media_play_btn")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pauzeren" else "Afspelen",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = { viewModel.playProceduralTone() },
                        modifier = Modifier.size(32.dp).testTag("media_next_btn")
                    ) {
                        Icon(imageVector = Icons.Filled.SkipNext, contentDescription = "Volgend nummer (Ruis)", tint = TextPrimary)
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
                            imageVector = if (settings.dynamicBassEnabled) Icons.Filled.CheckCircle else Icons.Filled.Hearing,
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
                            imageVector = if (settings.surroundSoundEnabled) Icons.Filled.CheckCircle else Icons.Filled.MusicNote,
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
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkPanel)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        DarkBorder,
                        HighlightSky.copy(alpha = 0.25f),
                        DarkBorder
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
            .testTag("dashboard_quick_controls")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // ANC modes quick picker
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Snelbeheer Ruisonderdrukking",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    PhilipsPremiumSwitch(
                        checked = settings.ancMode != "OFF",
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                viewModel.toggleAnc(true)
                                viewModel.setAncMode("ON")
                            } else {
                                viewModel.toggleAnc(false)
                                viewModel.setAncMode("OFF")
                            }
                        },
                        modifier = Modifier.testTag("anc_dashboard_switch"),
                        activeColor = HighlightSky
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val ancModes = listOf(
                        Triple("ON", "ANC Actief", Icons.Filled.GraphicEq),
                        Triple("TRANSPARENCY", "Omgevingsgeluid", Icons.Filled.Hearing),
                        Triple("OFF", "Uit", Icons.Filled.Close)
                    )
                    ancModes.forEach { (modeCode, label, icon) ->
                        val isSelected = settings.ancMode == modeCode
                        Button(
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.setAncMode(modeCode) 
                            },
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
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.setPreset(preset) 
                            },
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
                PremiumSlider(
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
                            imageVector = Icons.Filled.Hearing,
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
                            "${ambientDecibel} dBA ($category)"
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
    val isFindMyBeeping by viewModel.isFindMyBeeping.collectAsStateWithLifecycle()
    val findMySignalStatus by viewModel.findMySignalStatus.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
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

    val beaconGenerator = remember { SineWaveGenerator() }

    // Acoustic tone generator for Find My Headphones locator signal
    LaunchedEffect(isFindMyBeeping) {
        if (isFindMyBeeping) {
            val frequencies = listOf(1200f, 1800f, 1500f, 2000f)
            var index = 0
            while (isFindMyBeeping) {
                val freq = frequencies[index % frequencies.size]
                beaconGenerator.startTone(freq, 0.65f)
                delay(280)
                beaconGenerator.stopTone()
                delay(180)
                index++
            }
        } else {
            beaconGenerator.stopTone()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            beaconGenerator.stopTone()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkPanel, shape = RoundedCornerShape(14.dp))
            .border(1.dp, if (isFindMyBeeping) StatusSuccess else DarkBorder, shape = RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag("dashboard_locator_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Zoek Mijn Koptelefoon (Radar & Beep)",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                if (isFindMyBeeping) {
                    Box(
                        modifier = Modifier
                            .background(StatusSuccess.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .border(1.dp, StatusSuccess, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "🔊 Signaal Actief",
                            color = StatusSuccess,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

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
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = StatusSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        val hasHeadphone = scannedDevices.any { it.isHeadphone } || settings.connected
                        Text(
                            text = if (settings.connected) "Philips TAH6519 Verbonden (~1.2m)" else if (hasHeadphone) "Philips TAH6519 Gevonden!" else "Koptelefoon kwijt of buiten zicht?",
                            color = if (hasHeadphone) StatusSuccess else TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = if (isFindMyBeeping) {
                                findMySignalStatus
                            } else if (settings.connected) {
                                "Verstuur direct een luid akoestisch beeping-signaal naar de TAH6519 drivers."
                            } else if (hasHeadphone) {
                                val closest = scannedDevices.filter { it.isHeadphone }.maxByOrNull { it.rssi }
                                val distance = if ((closest?.rssi ?: -100) > -65) "~1.5m" else "~6.5m"
                                "Dichtbij gevonden met signaalsterkte ${closest?.rssi} dBm ($distance)"
                            } else {
                                "Laat de TAH6519 een geluidssignaal afspelen om hem snel te vinden."
                            },
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Always present Signal Beep button
            Button(
                onClick = {
                    viewModel.toggleFindMySignal()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFindMyBeeping) StatusDanger else StatusSuccess,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .testTag("locator_play_beep_btn")
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isFindMyBeeping) Icons.Filled.VolumeUp else Icons.Filled.NotificationsActive,
                        contentDescription = "Find My Signal Beep",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isFindMyBeeping) "⏹️ Stop Geluidssignaal (Piept...)" else "📢 Speel Akoestisch Signaal Af (Beep)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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
                                    imageVector = if (device.isHeadphone) Icons.Filled.Headphones else Icons.Filled.Bluetooth,
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
fun UniversalHeadphoneSelectorCard(
    viewModel: HeadphoneViewModel,
    settings: HeadphoneSettings,
    modifier: Modifier = Modifier
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    var customNameInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Over-Ear Wireless") }

    val popularModels = remember {
        listOf(
            Triple("Philips TAH6519", "Over-Ear • 40mm • 80h", Icons.Filled.Headphones),
            Triple("Sony WH-1000XM5", "Over-Ear • 30mm • 40h", Icons.Filled.Headphones),
            Triple("Bose QC Ultra", "Over-Ear • 35mm • 30h", Icons.Filled.Headphones),
            Triple("Apple AirPods Max", "Over-Ear • 40mm • 24h", Icons.Filled.Headphones),
            Triple("Sennheiser Momentum 4", "Over-Ear • 42mm • 60h", Icons.Filled.Headphones),
            Triple("JBL Live 660NC", "Over-Ear • 40mm • 65h", Icons.Filled.Headphones),
            Triple("Anker Soundcore Q45", "Over-Ear • 40mm • 60h", Icons.Filled.Headphones),
            Triple("Samsung Galaxy Buds2 Pro", "In-Ear • 11mm • 28h", Icons.Filled.Headset),
            Triple("Wired Studio 3.5mm DAC", "Studio • 45mm • Onbeperkt", Icons.Filled.GraphicEq)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkPanel, shape = RoundedCornerShape(14.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(14.dp))
            .padding(16.dp)
            .testTag("universal_headphone_selector_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            .background(AccentPrimary.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, AccentPrimary.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (settings.headphoneCategory.contains("In-Ear")) Icons.Filled.Headset else Icons.Filled.Headphones,
                            contentDescription = "Device Type",
                            tint = AccentPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Koptelefoon Profiel & Model",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Universele ondersteuning voor alle merken & typen",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Surface(
                    color = HighlightSky.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, HighlightSky.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = settings.headphoneCategory,
                        color = HighlightSky,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Current Active Device Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBg, shape = RoundedCornerShape(10.dp))
                    .border(1.dp, AccentPrimary.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (settings.connected) StatusSuccess else StatusYellow, CircleShape)
                        )
                        Text(
                            text = settings.connectedDeviceName,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = "Drivers: ${settings.driverSizeMm}mm • Codec: ${settings.activeCodec} • ${if (settings.maxPlaytimeHours > 500) "Onbeperkt" else "tot " + settings.maxPlaytimeHours + "u speeltijd"}",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                IconButton(
                    onClick = { showCustomDialog = true },
                    modifier = Modifier.testTag("edit_custom_headphone_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Aangepast model invoeren",
                        tint = HighlightSky,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "Snelle Profiel-Selectie (Alle Merken):",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Horizontal Scrollable Model Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(popularModels) { (modelName, spec, icon) ->
                    val isSelected = settings.connectedDeviceName.contains(modelName.take(8), ignoreCase = true)
                    Surface(
                        onClick = {
                            viewModel.selectHeadphoneProfile(modelName)
                            if (!settings.connected) {
                                viewModel.connectDevice(modelName)
                            }
                        },
                        color = if (isSelected) AccentPrimary.copy(alpha = 0.2f) else DarkBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) AccentPrimary else DarkBorder.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("headphone_chip_${modelName.lowercase().replace(" ", "_").replace(".", "")}")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) AccentPrimary else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Column {
                                Text(
                                    text = modelName,
                                    color = if (isSelected) TextPrimary else TextPrimary.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = spec,
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = {
                Text(
                    text = "Aangepaste Koptelefoon Invoeren",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Voer het merk en model van jouw koptelefoon of oortjes in om de geluidsinstellingen perfect af te stemmen.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    OutlinedTextField(
                        value = customNameInput,
                        onValueChange = { customNameInput = it },
                        label = { Text("Modelnaam (bijv. Shure AONIC 50)", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedLabelColor = AccentPrimary,
                            unfocusedLabelColor = TextMuted
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("custom_headphone_name_input")
                    )

                    Text(
                        text = "Categorie / Type:",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val categories = listOf(
                        "Over-Ear Wireless",
                        "In-Ear / Earbuds",
                        "On-Ear Wireless",
                        "Gaming Headset",
                        "Wired 3.5mm / USB-C"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        categories.forEach { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCategory = cat }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat }
                                )
                                Text(
                                    text = cat,
                                    color = TextPrimary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customNameInput.isNotBlank()) {
                            viewModel.selectHeadphoneProfile(customNameInput, selectedCategory)
                            if (!settings.connected) {
                                viewModel.connectDevice(customNameInput)
                            }
                        }
                        showCustomDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    Text("Opslaan & Toepassen", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("Annuleren", color = TextMuted)
                }
            },
            containerColor = DarkPanel
        )
    }
}

@Composable
fun CompatibleBluetoothDevicesCard(
    viewModel: HeadphoneViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val compatibleDevices by viewModel.compatibleBluetoothDevices.collectAsStateWithLifecycle()
    
    var selectedBrandFilter by remember { mutableStateOf("Alle") }
    val brandFilters = listOf("Alle", "Philips", "Sony", "Bose", "Apple", "Sennheiser", "Andere Brands")
    
    val filteredDevices = remember(compatibleDevices, selectedBrandFilter) {
        when (selectedBrandFilter) {
            "Alle" -> compatibleDevices
            "Philips" -> compatibleDevices.filter { it.brand == "Philips" }
            "Sony" -> compatibleDevices.filter { it.brand == "Sony" }
            "Bose" -> compatibleDevices.filter { it.brand == "Bose" }
            "Apple" -> compatibleDevices.filter { it.brand == "Apple" }
            "Sennheiser" -> compatibleDevices.filter { it.brand == "Sennheiser" }
            "Andere Brands" -> compatibleDevices.filter { it.brand !in listOf("Philips", "Sony", "Bose", "Apple", "Sennheiser") }
            else -> compatibleDevices
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkPanel, shape = RoundedCornerShape(14.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(14.dp))
            .padding(16.dp)
            .testTag("compatible_bluetooth_devices_card")
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
                        imageVector = Icons.Filled.Headphones,
                        contentDescription = "Compatibele Apparaten",
                        tint = AccentPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Compatibele Bluetooth Apparaten",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${compatibleDevices.size} ondersteunde modellen met automatische audio-profielen",
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(AccentPrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(100.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${filteredDevices.size} MODELLEN",
                        color = AccentPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Brand Filters row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(brandFilters) { filter ->
                    val isSelected = selectedBrandFilter == filter
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) AccentPrimary else DarkBg,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) AccentPrimary else DarkBorder.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedBrandFilter = filter }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("filter_brand_$filter")
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color.White else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(color = DarkBorder.copy(alpha = 0.3f))

            // Device items list
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                filteredDevices.forEach { device ->
                    val isConnectedToThis = settings.connected && settings.connectedDeviceName.equals(device.name, ignoreCase = true)
                    val cardBg = if (isConnectedToThis) StatusSuccess.copy(alpha = 0.08f) else DarkBg
                    val cardBorder = if (isConnectedToThis) StatusSuccess.copy(alpha = 0.4f) else DarkBorder.copy(alpha = 0.5f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardBg, shape = RoundedCornerShape(10.dp))
                            .border(1.dp, cardBorder, shape = RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                        .size(36.dp)
                                        .background(
                                            if (device.isPhilips) AccentPrimary.copy(alpha = 0.2f) else DarkPanel,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (device.category.contains("In-Ear")) Icons.Filled.HeadsetMic else Icons.Filled.Headphones,
                                        contentDescription = null,
                                        tint = if (device.isPhilips) AccentPrimary else HighlightSky,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = device.name,
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (device.isPhilips) {
                                            Box(
                                                modifier = Modifier
                                                    .background(AccentPrimary, shape = RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "PHILIPS SPEC",
                                                    color = Color.White,
                                                    fontSize = 7.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${device.category} • ${device.bluetoothVersion}",
                                        color = TextMuted,
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            if (isConnectedToThis) {
                                Box(
                                    modifier = Modifier
                                        .background(StatusSuccess.copy(alpha = 0.2f), shape = RoundedCornerShape(100.dp))
                                        .border(1.dp, StatusSuccess, shape = RoundedCornerShape(100.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "ACTIEF VERBONDEN",
                                        color = StatusSuccess,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.connectCompatibleDevice(device) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp).testTag("connect_btn_${device.name.replace(" ", "_")}")
                                ) {
                                    Text(
                                        text = "Koppelen",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Specs badges row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Driver size
                            Box(
                                modifier = Modifier
                                    .background(DarkPanel, shape = RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${device.driverSizeMm}mm Driver",
                                    color = TextMuted,
                                    fontSize = 8.sp
                                )
                            }

                            // Codecs
                            device.supportedCodecs.forEach { codec ->
                                Box(
                                    modifier = Modifier
                                        .background(HighlightSky.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = codec,
                                        color = HighlightSky,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Playtime
                            Box(
                                modifier = Modifier
                                    .background(DarkPanel, shape = RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${device.maxPlaytimeHours}u Accu",
                                    color = TextMuted,
                                    fontSize = 8.sp
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
fun BluetoothStatusIndicatorCard(
    viewModel: HeadphoneViewModel,
    settings: HeadphoneSettings,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isConnecting by viewModel.isConnecting.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanningBluetooth.collectAsStateWithLifecycle()
    val isAutoReconnecting by viewModel.isAutoReconnecting.collectAsStateWithLifecycle()
    val reconnectAttempts by viewModel.reconnectAttempts.collectAsStateWithLifecycle()
    val autoReconnectEnabled by viewModel.autoReconnectEnabled.collectAsStateWithLifecycle()
    val scannedDevices by viewModel.scannedDevices.collectAsStateWithLifecycle()
    val isSimulationMode by viewModel.isSimulationMode.collectAsStateWithLifecycle()

    val statusTitle by remember(settings.connected, isAutoReconnecting, isConnecting, settings.connectedDeviceName, settings.headphoneCategory) {
        derivedStateOf {
            if (settings.connected) "${settings.connectedDeviceName} (${settings.headphoneCategory})"
            else if (isAutoReconnecting) "Verbinding herstellen..."
            else if (isConnecting) "Verbinding maken..."
            else "Koptelefoon stand-by"
        }
    }
    val statusSubtitle by remember(settings.connected, isAutoReconnecting, isConnecting, reconnectAttempts) {
        derivedStateOf {
            if (settings.connected) "Signaal: Uitstekend (-52 dBm) · Multipoint"
            else if (isAutoReconnecting) "Spoorloos verloren. Automatische poging $reconnectAttempts van 3..."
            else if (isConnecting) "Koppelen via Bluetooth LE..."
            else "Schakel de koptelefoon in om verbinding te maken"
        }
    }
    val statusIcon by remember(settings.connected, isAutoReconnecting, isConnecting) {
        derivedStateOf {
            if (settings.connected) Icons.Filled.Bluetooth
            else if (isAutoReconnecting) Icons.Filled.Sync
            else if (isConnecting) Icons.Filled.Bluetooth
            else Icons.Filled.BluetoothDisabled
        }
    }
    val statusIconTint by remember(settings.connected, isAutoReconnecting, isConnecting) {
        derivedStateOf {
            if (settings.connected) StatusSuccess
            else if (isAutoReconnecting) StatusPurple
            else if (isConnecting) StatusYellow
            else TextMuted
        }
    }
    val batteryTextFormatted by remember(settings.batteryLevel) {
        derivedStateOf { "${settings.batteryLevel}%" }
    }

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
                        imageVector = Icons.Filled.Bluetooth,
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
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusIconTint,
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
                        text = statusTitle,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = statusSubtitle,
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
                            imageVector = Icons.Filled.FlashOn,
                            contentDescription = null,
                            tint = StatusSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Accu: $batteryTextFormatted",
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
                            imageVector = Icons.Filled.Audiotrack,
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
                            imageVector = Icons.Filled.Info,
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

            // Background Service & Auto-Reconnect Settings Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard, shape = RoundedCornerShape(12.dp))
                    .border(1.dp, HighlightSky.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                .size(32.dp)
                                .background(HighlightSky.copy(alpha = 0.15f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = null,
                                tint = HighlightSky,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Auto-Reconnect op Start (Achtergrond)",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Herverbindt automatisch met de laatst gekoppelde Philips TAH6519 bij opstarten van de app.",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Switch(
                        checked = settings.autoReconnectOnLaunch,
                        onCheckedChange = { viewModel.setAutoReconnectOnLaunch(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = HighlightSky,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkPanel
                        ),
                        modifier = Modifier.scale(0.8f).testTag("auto_reconnect_switch")
                    )
                }

                HorizontalDivider(color = DarkBorder.copy(alpha = 0.4f))

                // Last paired device info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Laatst gekoppeld apparaat:",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${settings.lastPairedDeviceName} [${settings.lastPairedDeviceAddress}]",
                            color = HighlightSky,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.triggerLaunchAutoReconnect()
                            try {
                                val serviceIntent = android.content.Intent(context, com.example.service.Tah6519AutoReconnectService::class.java).apply {
                                    action = com.example.service.Tah6519AutoReconnectService.ACTION_START_RECONNECT
                                    putExtra(com.example.service.Tah6519AutoReconnectService.EXTRA_DEVICE_NAME, settings.lastPairedDeviceName.ifBlank { "Philips TAH6519" })
                                    putExtra(com.example.service.Tah6519AutoReconnectService.EXTRA_DEVICE_ADDRESS, settings.lastPairedDeviceAddress.ifBlank { "00:11:22:33:44:55" })
                                }
                                context.startService(serviceIntent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HighlightSky.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, HighlightSky.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        enabled = !isConnecting && !isAutoReconnecting,
                        modifier = Modifier.testTag("test_background_reconnect_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = HighlightSky,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Service Herstarten",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = HighlightSky
                            )
                        }
                    }
                }
            }

            // Scanned Devices List for Bluetooth Connection Management
            if (!settings.connected && (isScanning || scannedDevices.isNotEmpty())) {
                HorizontalDivider(color = DarkBorder.copy(alpha = 0.3f))
                
                // Prominent Philips TAH6519 Detected Connection Prompt Card
                val detectedTah6519 = scannedDevices.firstOrNull {
                    it.name.contains("TAH6519", ignoreCase = true) || it.name.contains("Philips", ignoreCase = true)
                }
                
                if (detectedTah6519 != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tah6519_connection_prompt_card"),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(HighlightSky, StatusPurple))),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
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
                                            .background(HighlightSky.copy(alpha = 0.2f), CircleShape)
                                            .border(1.dp, HighlightSky, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Headphones,
                                            contentDescription = "Philips TAH6519 Detected",
                                            tint = HighlightSky,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = detectedTah6519.name,
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Surface(
                                                color = HighlightSky.copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "GEVONDEN",
                                                    color = HighlightSky,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Signaal: ${detectedTah6519.rssi} dBm • Direct koppelen beschikbaar",
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                
                                Text(
                                    text = detectedTah6519.address,
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                            
                            // Feature tags
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = DarkBg,
                                    border = BorderStroke(0.5.dp, DarkBorder),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "40mm Neodynium Drivers",
                                        color = TextPrimary,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                                Surface(
                                    color = DarkBg,
                                    border = BorderStroke(0.5.dp, DarkBorder),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Hybrid ANC Deep Silence",
                                        color = StatusPurple,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                                Surface(
                                    color = DarkBg,
                                    border = BorderStroke(0.5.dp, DarkBorder),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "LDAC Hi-Res",
                                        color = HighlightSky,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            
                            // Connect Button
                            Button(
                                onClick = { viewModel.connectDevice(detectedTah6519.address) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HighlightSky,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .testTag("tah6519_prompt_connect_button"),
                                enabled = !isConnecting
                            ) {
                                if (isConnecting) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.Black,
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            text = "Verbinding maken...",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.BluetoothConnected,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "⚡ Direct Koppelen & Verbinden met TAH6519",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

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
                                                imageVector = if (isThisDeviceHeadphone) Icons.Filled.Headphones else Icons.Filled.Bluetooth,
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
                                    imageVector = Icons.Filled.Bluetooth,
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
                                    imageVector = Icons.Filled.Search,
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

            // Test Bluetooth Connection Chime Button
            Button(
                onClick = { viewModel.playBluetoothConnectChime() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HighlightSky.copy(alpha = 0.12f),
                    contentColor = HighlightSky
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, HighlightSky.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .testTag("test_bluetooth_chime_button")
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = "Test Bluetooth Chime",
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "🔊 Test Verbindingsgeluid (Chime op Koptelefoon)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
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
                        imageVector = Icons.Filled.Analytics,
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
                            imageVector = Icons.Filled.BluetoothDisabled,
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
                                imageVector = Icons.Filled.Info,
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
                                    imageVector = Icons.Filled.CheckCircle,
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
                                    imageVector = Icons.Filled.Sync,
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
                                            imageVector = Icons.Filled.CheckCircle,
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
                                        imageVector = Icons.Filled.CheckCircle,
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
                                imageVector = Icons.Filled.Info,
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
                    
                    val haptic = LocalHapticFeedback.current
                    PremiumSlider(
                        value = simulatedDistanceMeters,
                        onValueChange = { 
                            if (it != simulatedDistanceMeters) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            viewModel.setSimulatedDistance(it) 
                        },
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
                            icon = Icons.Filled.Audiotrack,
                            iconColor = HighlightSky,
                            label = "Codec & Formaat",
                            value = activeAudioCodec,
                            subtext = activeSampleRate
                        )

                        // Stat Item: Audio Bitrate
                        val kbpsColor = if (bitrateKbps >= 660) HighlightSky else if (bitrateKbps >= 328) AccentPrimary else StatusYellow
                        TechnicalStatItem(
                            icon = Icons.Filled.Speed,
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
                            icon = Icons.Filled.AccessTime,
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
                            icon = Icons.Filled.SwapVert,
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
                        imageVector = Icons.Filled.Info,
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
                            imageVector = Icons.Filled.BatteryAlert,
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
                        imageVector = Icons.Filled.LocationOn,
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
                            imageVector = Icons.Filled.CheckCircle,
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
                        Triple("Thuis", "ANC Uit · Dynamic Bass", Icons.Filled.Home),
                        Triple("Kantoor", "Omgevingsgeluid · Vocal Clarity", Icons.Filled.Laptop),
                        Triple("Sportschool", "ANC Aan · Dynamic Bass", Icons.Filled.FlashOn),
                        Triple("Trein", "ANC Aan · Philips Signature", Icons.Filled.SwapVert)
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
                                        imageVector = Icons.Filled.Info,
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
                        Triple("Zitten", "ANC Aan · Harman EQ (Focus)", Icons.Filled.Person),
                        Triple("Wandelen", "Omgevingsgeluid · Philips EQ", Icons.Filled.SwapVert),
                        Triple("Hardlopen", "Omgevingsgeluid (Extra Veiligheid) · Bass EQ", Icons.Filled.FlashOn),
                        Triple("Reizen", "ANC Aan · Bass EQ (Blokkeer lawaai)", Icons.Filled.LocationOn)
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
                                        imageVector = Icons.Filled.Info,
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
                                imageVector = Icons.Filled.Link,
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
                            imageVector = Icons.Filled.Headphones,
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
                                        imageVector = Icons.Filled.FlashOn,
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
                                    imageVector = Icons.Filled.Speed,
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
                            imageVector = Icons.Filled.Close,
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
    val leftLevel by remember(batteryLevel) {
        derivedStateOf { (batteryLevel - 3).coerceIn(0, 100) }
    }
    val rightLevel by remember(batteryLevel) {
        derivedStateOf { (batteryLevel + 2).coerceIn(0, 100) }
    }

    val normalBrushColor by remember(batteryLevel) {
        derivedStateOf {
            when {
                batteryLevel <= 20 -> StatusDanger
                batteryLevel <= 50 -> StatusYellow
                else -> AccentPrimary
            }
        }
    }

    val activeColor by remember(batteryLevel, isCharging) {
        derivedStateOf {
            if (isCharging) HighlightSky else normalBrushColor
        }
    }

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
                    isCharging -> Icons.Filled.FlashOn
                    batteryLevel <= 20 -> Icons.Filled.BatteryAlert
                    else -> Icons.Filled.BatteryFull
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
    val batteryTextFormatted by remember(batteryLevel) {
        derivedStateOf { "$batteryLevel%" }
    }
    val batteryStatusLabel by remember(isCharging) {
        derivedStateOf { if (isCharging) "OPLADEN" else "RESTEREND" }
    }
    val leftChannelLevel by remember(batteryLevel) {
        derivedStateOf { (batteryLevel - 3).coerceIn(0, 100) }
    }
    val rightChannelLevel by remember(batteryLevel) {
        derivedStateOf { (batteryLevel + 2).coerceIn(0, 100) }
    }

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
            level = leftChannelLevel,
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
                        imageVector = Icons.Filled.FlashOn,
                        contentDescription = "Opladen",
                        tint = HighlightSky,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { alpha = glowAlpha }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Headset,
                        contentDescription = null,
                        tint = TextMuted.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Text(
                    text = batteryTextFormatted,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )

                Text(
                    text = batteryStatusLabel,
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
            level = rightChannelLevel,
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
    val batteryTextLabel by remember(batteryLevel) {
        derivedStateOf { "$batteryLevel%" }
    }

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
                    imageVector = Icons.Filled.Headset,
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
                        imageVector = Icons.Filled.FlashOn,
                        contentDescription = "Opladen",
                        tint = HighlightSky,
                        modifier = Modifier
                            .size(14.dp)
                            .graphicsLayer { alpha = pulseGlow }
                    )
                }
                Text(
                    text = batteryTextLabel,
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
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val firmwareVersion by viewModel.firmwareVersion.collectAsStateWithLifecycle()
    val serialNumber by viewModel.serialNumber.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val lastPollTime by viewModel.lastFirmwarePollTime.collectAsStateWithLifecycle()
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
                            imageVector = Icons.Filled.Warning,
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
                            imageVector = Icons.Filled.Settings,
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
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Sluit", tint = TextMuted)
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
                                imageVector = Icons.Filled.Headphones,
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
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = HighlightSky,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Technische Specificaties (${settings.connectedDeviceName})",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                imageVector = if (showSpecs) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (showSpecs) "Minder details" else "Meer details",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (showSpecs) {
                            HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
                            
                            val specItems = listOf(
                                "Modelnaam" to settings.connectedDeviceName,
                                "Categorie" to settings.headphoneCategory,
                                "Audiostuurprogramma" to "${settings.driverSizeMm} mm high-performance drivers",
                                "Frequentiebereik" to "20 Hz - 20.000 Hz",
                                "Impedantie" to "32 Ohm",
                                "Ruisonderdrukking" to if (settings.ancEnabled) "Actieve Ruisonderdrukking (ANC Actief)" else "Passieve Geluidsisolatie",
                                "Batterijduur (ANC Aan)" to if (settings.ancPlaytimeHours > 500) "Onbeperkt (Kabel)" else "${settings.ancPlaytimeHours} uur speeltijd",
                                "Batterijduur (ANC Uit)" to if (settings.maxPlaytimeHours > 500) "Onbeperkt (Kabel)" else "${settings.maxPlaytimeHours} uur speeltijd",
                                "Verbindingstype" to settings.connectionType,
                                "Audio Codec" to settings.activeCodec,
                                "Microfoons" to "ENC ruisvrije microfoons voor helder bellen",
                                "Ingebouwde Sensoren" to if (settings.wearingDetectionEnabled) "Draagdetectie Actief" else "Sensoren Stand-by"
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

                // Applicatiethema & Persistent Voorkeur (DataStore)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_theme_card"),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(1.dp, DarkBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    imageVector = Icons.Filled.BrightnessAuto,
                                    contentDescription = null,
                                    tint = HighlightSky,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Voorkeur UI-Thema",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .background(StatusSuccess.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                                    .border(1.dp, StatusSuccess.copy(alpha = 0.3f), shape = RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = StatusSuccess,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "DataStore Persisted",
                                    color = StatusSuccess,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // 4-way Theme Mode Segmented Selector (Licht / Ambient / Custom / Donker)
                        val currentMode by viewModel.currentThemeMode.collectAsStateWithLifecycle()
                        val customHex by viewModel.customAccentHex.collectAsStateWithLifecycle()

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkBg, shape = RoundedCornerShape(10.dp))
                                    .border(1.dp, DarkBorder, shape = RoundedCornerShape(10.dp))
                                    .padding(3.dp)
                                    .testTag("settings_theme_mode_selector"),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val modeOptions = listOf(
                                    Triple(com.example.ui.theme.ThemeMode.LIGHT, "Licht", Icons.Filled.WbSunny),
                                    Triple(com.example.ui.theme.ThemeMode.AMBIENT, "Ambient", Icons.Filled.BlurOn),
                                    Triple(com.example.ui.theme.ThemeMode.CUSTOM, "Custom", Icons.Filled.Palette),
                                    Triple(com.example.ui.theme.ThemeMode.DARK, "Donker", Icons.Filled.NightsStay)
                                )

                                modeOptions.forEach { (mode, label, icon) ->
                                    val isSelected = currentMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                color = if (isSelected) HighlightSky.copy(alpha = 0.2f) else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                width = if (isSelected) 1.dp else 0.dp,
                                                color = if (isSelected) HighlightSky else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.setThemeMode(mode)
                                            }
                                            .padding(vertical = 8.dp)
                                            .testTag("theme_mode_${mode.name.lowercase()}_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (isSelected) HighlightSky else TextMuted,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = label,
                                                color = if (isSelected) TextPrimary else TextMuted,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }

                            if (currentMode == com.example.ui.theme.ThemeMode.CUSTOM) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkBg, shape = RoundedCornerShape(8.dp))
                                        .border(1.dp, HighlightSky.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                        .testTag("custom_theme_accent_picker"),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Aangepaste Kleuraccenten (Philips Signature)",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val customSwatches = listOf(
                                            "#0066FF" to "Philips Blauw",
                                            "#00E5FF" to "Ambilight Cyan",
                                            "#E040FB" to "Cyber Neon",
                                            "#FFAB00" to "Warm Goud",
                                            "#00E676" to "Nordic Mint",
                                            "#FF3D00" to "Vivid Oranje"
                                        )
                                        customSwatches.forEach { (hex, name) ->
                                            val swatchColor = try {
                                                Color(android.graphics.Color.parseColor(hex))
                                            } catch (e: Exception) {
                                                Color(0xFF00E5FF)
                                            }
                                            val isSelected = customHex.equals(hex, ignoreCase = true)
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(swatchColor)
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) TextPrimary else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        viewModel.setCustomAccentHex(hex)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = name,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
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
                                            viewModel.setActiveAppTheme(theme)
                                        }
                                        .padding(vertical = 10.dp)
                                        .testTag("app_theme_${label.lowercase()}_button"),
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

                // Multipoint Pairing Toggle Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_multipoint_card"),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(1.dp, if (settings.multipointEnabled) HighlightSky.copy(alpha = 0.3f) else DarkBorder),
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            if (settings.multipointEnabled) HighlightSky.copy(alpha = 0.15f) else DarkBg,
                                            shape = CircleShape
                                        )
                                        .border(
                                            1.dp,
                                            if (settings.multipointEnabled) HighlightSky.copy(alpha = 0.4f) else DarkBorder,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Devices,
                                        contentDescription = null,
                                        tint = if (settings.multipointEnabled) HighlightSky else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Multipoint Koppeling",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (settings.multipointEnabled) "Verbonden met 2 apparaten tegelijk" else "Uitgeschakeld (Enkel apparaat)",
                                        color = if (settings.multipointEnabled) StatusSuccess else TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Switch(
                                checked = settings.multipointEnabled,
                                onCheckedChange = { enabled ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.toggleMultipoint(enabled)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = HighlightSky,
                                    checkedTrackColor = AccentPrimary,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = DarkBg
                                ),
                                modifier = Modifier
                                    .scale(0.85f)
                                    .testTag("multipoint_pairing_switch")
                            )
                        }

                        Text(
                            text = "Schakel Multipoint Koppeling in om naadloos te schakelen tussen twee actieve Bluetooth-bronnen (bijv. telefoon en laptop) zonder opnieuw te hoeven koppelen.",
                            color = TextMuted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                // Intelligent Battery Preservation Card
                IntelligentBatteryPreservationCard(
                    settings = settings,
                    viewModel = viewModel
                )

                // Philips TAH6519 Device Info & Firmware Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tah6519_device_info_card"),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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
                                        .size(32.dp)
                                        .background(AccentPrimary.copy(alpha = 0.15f), CircleShape)
                                        .border(1.dp, AccentPrimary.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Headset,
                                        contentDescription = "Headphone Icon",
                                        tint = AccentPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Philips TAH6519",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "High-Performance Over-Ear ANC",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            BluetoothConnectivityBadge(
                                isConnected = settings.connected,
                                deviceName = settings.connectedDeviceName.ifBlank { "Philips TAH6519" }
                            )
                        }

                        HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

                        // Firmware Version Info Row
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
                                    imageVector = Icons.Filled.SystemUpdate,
                                    contentDescription = null,
                                    tint = HighlightSky,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(
                                        text = "Firmware-versie",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "HW Rev: 2.0 • Build 2026.08-OTA",
                                        color = TextMuted,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .background(HighlightSky.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                    .border(1.dp, HighlightSky.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = firmwareVersion,
                                    color = HighlightSky,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("settings_firmware_version")
                                )
                            }
                        }

                        // Serial Number Row
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
                                    imageVector = Icons.Filled.Fingerprint,
                                    contentDescription = null,
                                    tint = HighlightSky,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Serienummer",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .background(HighlightSky.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                    .border(1.dp, HighlightSky.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = serialNumber,
                                    color = HighlightSky,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("settings_serial_number")
                                )
                            }
                        }

                        if (!lastPollTime.isNullOrEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Laatst gecontroleerd",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = lastPollTime.orEmpty(),
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

                        // Firmware Updates Controls & Loading Progress Section
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            when (val state = updateState) {
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
                                                text = "OTA Server API",
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
                                            text = "Verbinding maken met Philips Firmware Server...",
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
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
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
                                            text = "Schakel de koptelefoon niet uit tijdens de installatie.",
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
                                            .border(1.dp, StatusSuccess.copy(alpha = 0.3f), shape = RoundedCornerShape(10.dp))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Nieuwe Firmware Beschikbaar (${state.version})",
                                                color = StatusSuccess,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
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
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.startUpdate()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(36.dp)
                                                .testTag("settings_install_update_button")
                                        ) {
                                            Text("Update Nu Installeren", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                is UpdateState.UpToDate -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(StatusSuccess.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp))
                                            .border(1.dp, StatusSuccess.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
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
                                            border = BorderStroke(1.dp, DarkBorder),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("OK", color = TextPrimary, fontSize = 11.sp)
                                        }
                                    }
                                }

                                else -> {
                                    Text(
                                        text = "Als er nieuwe updates beschikbaar zijn met geluidskwaliteit- en prestatieverbeteringen, kun je deze direct downloaden en installeren.",
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }

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
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = when (updateState) {
                                            is UpdateState.Checking -> "Controleren op updates..."
                                            is UpdateState.Updating -> "Update wordt geïnstalleerd..."
                                            is UpdateState.UpToDate -> "Opnieuw Controleren"
                                            is UpdateState.UpdateAvailable -> "Controleer Opnieuw"
                                            is UpdateState.UpdateComplete -> "Controleer op Updates"
                                            else -> "Controleer op Updates"
                                        },
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Tah6519HardwareSpecsCard(
                    settings = settings,
                    modifier = Modifier.fillMaxWidth()
                )

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
                            imageVector = Icons.Filled.DeleteForever,
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
fun SpatialReverbEngineCard(
    viewModel: HeadphoneViewModel,
    settings: com.example.data.HeadphoneSettings,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // State parameters for spatial reverb
    var activeMode by remember { mutableStateOf(settings.spatialAudioMode) }
    var roomSize by remember { mutableFloatStateOf(65f) } // 10% - 100%
    var reverbDecay by remember { mutableFloatStateOf(1.8f) } // 0.5s - 5.0s
    var wetDryMix by remember { mutableFloatStateOf(40f) } // 0% - 100%
    var spatialWidth by remember { mutableFloatStateOf(80f) } // 0% - 100%

    // Smooth animated transitions for sliders when values change
    val animatedRoomSize by animateFloatAsState(
        targetValue = roomSize,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "anim_room_size"
    )
    val animatedReverbDecay by animateFloatAsState(
        targetValue = reverbDecay,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "anim_reverb_decay"
    )
    val animatedWetDry by animateFloatAsState(
        targetValue = wetDryMix,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "anim_wet_dry"
    )
    val animatedSpatialWidth by animateFloatAsState(
        targetValue = spatialWidth,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "anim_spatial_width"
    )

    // updateTransition for acoustic mode changes
    val modeTransition = updateTransition(targetState = activeMode, label = "spatial_reverb_mode_transition")

    val auraColor by modeTransition.animateColor(
        transitionSpec = { tween(durationMillis = 400, easing = FastOutSlowInEasing) },
        label = "aura_color"
    ) { mode ->
        when (mode) {
            "Stereo" -> Color(0xFF708090)
            "Acoustic Studio" -> HighlightSky
            "Live Concert" -> StatusPurple
            "Cinematic 3D" -> Color(0xFF00E5FF)
            else -> StatusPurple
        }
    }

    val roomScaleFactor by modeTransition.animateFloat(
        transitionSpec = { spring(stiffness = Spring.StiffnessLow) },
        label = "room_scale"
    ) { mode ->
        when (mode) {
            "Stereo" -> 0.75f
            "Acoustic Studio" -> 1.0f
            "Live Concert" -> 1.35f
            "Cinematic 3D" -> 1.65f
            else -> 1.0f
        }
    }

    val echoRingDensity by modeTransition.animateFloat(
        transitionSpec = { tween(durationMillis = 350) },
        label = "echo_density"
    ) { mode ->
        when (mode) {
            "Stereo" -> 0.2f
            "Acoustic Studio" -> 0.6f
            "Live Concert" -> 0.85f
            "Cinematic 3D" -> 1.0f
            else -> 0.6f
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("spatial_reverb_engine_card"),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                colors = listOf(
                    DarkBorder,
                    auraColor.copy(alpha = 0.5f),
                    DarkBorder
                )
            )
        ),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(auraColor.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, auraColor.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = "Spatial Reverb Engine",
                            tint = auraColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Spatial Reverb & Acoustic Engine",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Acoustic Stage Virtualization & Reverb Tail Tuning",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                // Active Mode Badge
                Box(
                    modifier = Modifier
                        .background(auraColor.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp))
                        .border(1.dp, auraColor.copy(alpha = 0.5f), shape = RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = activeMode.uppercase(),
                        color = auraColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Interactive Dynamic Canvas Soundstage Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkBg)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            ) {
                val infinitePulse = rememberInfiniteTransition(label = "reverb_pulse")
                val pulsePhase by infinitePulse.animateFloat(
                    initialValue = 0f,
                    targetValue = 2f * Math.PI.toFloat(),
                    animationSpec = infiniteRepeatable(
                        animation = tween(2800, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "pulse_phase"
                )

                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("spatial_reverb_canvas")
                ) {
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2f, h / 2f)

                    // Draw acoustic room boundaries scaled by animatedRoomSize & roomScaleFactor
                    val baseRoomWidth = (w * 0.45f) * (animatedRoomSize / 100f) * roomScaleFactor
                    val baseRoomHeight = (h * 0.42f) * (animatedRoomSize / 100f) * roomScaleFactor
                    val roomRect = androidx.compose.ui.geometry.Rect(
                        center.x - baseRoomWidth,
                        center.y - baseRoomHeight,
                        center.x + baseRoomWidth,
                        center.y + baseRoomHeight
                    )

                    drawRoundRect(
                        color = auraColor.copy(alpha = 0.25f),
                        topLeft = Offset(roomRect.left, roomRect.top),
                        size = androidx.compose.ui.geometry.Size(roomRect.width, roomRect.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Draw Binaural Listener Head at Center
                    drawCircle(
                        color = TextPrimary,
                        radius = 12.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = auraColor,
                        radius = 6.dp.toPx(),
                        center = center
                    )

                    // Draw Stereo Speakers & Binaural Spread Arcs
                    val spreadPx = (w * 0.35f) * (animatedSpatialWidth / 100f)
                    val leftSpeaker = Offset((center.x - spreadPx).coerceAtLeast(16.dp.toPx()), center.y - 10.dp.toPx())
                    val rightSpeaker = Offset((center.x + spreadPx).coerceAtMost(w - 16.dp.toPx()), center.y - 10.dp.toPx())

                    // Left/Right Speaker nodes
                    drawCircle(color = auraColor, radius = 5.dp.toPx(), center = leftSpeaker)
                    drawCircle(color = auraColor, radius = 5.dp.toPx(), center = rightSpeaker)

                    // Speaker sound vectors to head
                    drawLine(
                        color = auraColor.copy(alpha = 0.4f),
                        start = leftSpeaker,
                        end = center,
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = auraColor.copy(alpha = 0.4f),
                        start = rightSpeaker,
                        end = center,
                        strokeWidth = 1.dp.toPx()
                    )

                    // Radiating Acoustic Reverb Reflection Waves driven by animatedReverbDecay & animatedWetDry
                    val ringCount = (3 * echoRingDensity).toInt().coerceAtLeast(1)
                    val maxRadius = (baseRoomWidth.coerceAtLeast(baseRoomHeight))
                    for (i in 0 until ringCount) {
                        val phaseOffset = (pulsePhase + (i * Math.PI.toFloat() / ringCount)) % (2f * Math.PI.toFloat())
                        val progress = phaseOffset / (2f * Math.PI.toFloat())
                        val waveRadius = 14.dp.toPx() + progress * maxRadius * (animatedReverbDecay / 2.5f)
                        val waveAlpha = ((1f - progress) * (animatedWetDry / 100f) * 0.7f).coerceIn(0f, 1f)

                        drawCircle(
                            color = auraColor.copy(alpha = waveAlpha),
                            radius = waveRadius,
                            center = center,
                            style = Stroke(width = (2.dp.toPx() * (1f - progress)).coerceAtLeast(0.5.dp.toPx()))
                        )
                    }

                    // Native Canvas Text Label displaying animated readouts
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(
                                (255 * 0.8f).toInt(),
                                (auraColor.red * 255).toInt(),
                                (auraColor.green * 255).toInt(),
                                (auraColor.blue * 255).toInt()
                            )
                            textSize = 9.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        drawText(
                            "STAGE: ${activeMode.uppercase()} · ${"%.1f".format(animatedReverbDecay)}s DECAY · ${animatedWetDry.toInt()}% WET",
                            w / 2f,
                            h - 8.dp.toPx(),
                            paint
                        )
                    }
                }
            }

            // Mode Selector Preset Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val modes = listOf("Stereo", "Acoustic Studio", "Live Concert", "Cinematic 3D")
                modes.forEach { mode ->
                    val isSelected = activeMode == mode
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            activeMode = mode
                            viewModel.setSpatialAudioMode(mode)
                            when (mode) {
                                "Stereo" -> {
                                    roomSize = 25f
                                    reverbDecay = 0.8f
                                    wetDryMix = 15f
                                    spatialWidth = 40f
                                }
                                "Acoustic Studio" -> {
                                    roomSize = 50f
                                    reverbDecay = 1.4f
                                    wetDryMix = 35f
                                    spatialWidth = 70f
                                }
                                "Live Concert" -> {
                                    roomSize = 80f
                                    reverbDecay = 2.8f
                                    wetDryMix = 60f
                                    spatialWidth = 90f
                                }
                                "Cinematic 3D" -> {
                                    roomSize = 95f
                                    reverbDecay = 3.6f
                                    wetDryMix = 75f
                                    spatialWidth = 100f
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .testTag("reverb_mode_button_$mode"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) auraColor.copy(alpha = 0.2f) else DarkPanel,
                            contentColor = if (isSelected) auraColor else TextMuted
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) auraColor else DarkBorder
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = when (mode) {
                                "Stereo" -> "Dry Direct"
                                "Acoustic Studio" -> "Studio"
                                "Live Concert" -> "Concert"
                                else -> "Cinema 3D"
                            },
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            HorizontalDivider(color = DarkBorder, thickness = 1.dp)

            // Sliders Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Slider 1: Acoustic Room Size
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Acoustic Room Size",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${animatedRoomSize.toInt()}%",
                            color = auraColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    PremiumSlider(
                        value = roomSize,
                        onValueChange = {
                            if (Math.abs(it - roomSize) > 2f) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            roomSize = it
                        },
                        valueRange = 10f..100f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = auraColor,
                            inactiveTrackColor = DarkBorder,
                            thumbColor = auraColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reverb_room_size_slider")
                    )
                }

                // Slider 2: Reverb Decay Time
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reverb Decay Time (RT60)",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${"%.1f".format(animatedReverbDecay)} s",
                            color = auraColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    PremiumSlider(
                        value = reverbDecay,
                        onValueChange = {
                            if (Math.abs(it - reverbDecay) > 0.15f) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            reverbDecay = it
                        },
                        valueRange = 0.5f..5.0f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = auraColor,
                            inactiveTrackColor = DarkBorder,
                            thumbColor = auraColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reverb_decay_slider")
                    )
                }

                // Slider 3: Wet / Dry Mix
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Acoustic Wet / Dry Mix",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${animatedWetDry.toInt()}% Wet",
                            color = auraColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    PremiumSlider(
                        value = wetDryMix,
                        onValueChange = {
                            if (Math.abs(it - wetDryMix) > 2f) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            wetDryMix = it
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = auraColor,
                            inactiveTrackColor = DarkBorder,
                            thumbColor = auraColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reverb_wet_dry_slider")
                    )
                }

                // Slider 4: 3D Stereo Spread Width
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3D Spatial Soundstage Width",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${animatedSpatialWidth.toInt()}% Wide",
                            color = auraColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    PremiumSlider(
                        value = spatialWidth,
                        onValueChange = {
                            if (Math.abs(it - spatialWidth) > 2f) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            spatialWidth = it
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = auraColor,
                            inactiveTrackColor = DarkBorder,
                            thumbColor = auraColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("spatial_width_slider")
                    )
                }
            }
        }
    }
}

@Composable
fun Tah6519HardwareSpecsCard(
    settings: com.example.data.HeadphoneSettings,
    modifier: Modifier = Modifier
) {
    var expandedSpecs by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tah6519_hardware_specs_card"),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, HighlightSky.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                            .background(HighlightSky.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, HighlightSky.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Memory,
                            contentDescription = "Hardware Specs",
                            tint = HighlightSky,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Philips TAH6519 Specs & Telemetrie",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "40mm Neodymium • Hybrid ANC • LDAC 990kbps",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                IconButton(
                    onClick = { expandedSpecs = !expandedSpecs },
                    modifier = Modifier.size(32.dp).testTag("toggle_hardware_specs_btn")
                ) {
                    Icon(
                        imageVector = if (expandedSpecs) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Expand Specs",
                        tint = HighlightSky
                    )
                }
            }

            HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

            // Highlighted Live Telemetry Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 1: Driver Specs
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DarkBg, shape = RoundedCornerShape(10.dp))
                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text("AKOESTIEK", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("40mm Neo", color = HighlightSky, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("7Hz - 40kHz", color = TextMuted, fontSize = 10.sp)
                    }
                }

                // Metric 2: Battery Life
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DarkBg, shape = RoundedCornerShape(10.dp))
                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text("ACCU DUUR", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(if (settings.ancMode != "OFF") "40 uur" else "80 uur", color = StatusSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Snelladen 15m=5h", color = TextMuted, fontSize = 10.sp)
                    }
                }

                // Metric 3: ANC Attenuation
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DarkBg, shape = RoundedCornerShape(10.dp))
                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text("ANC REDUCTIE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("-38 dB", color = AccentPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Quad-Mic Hybrid", color = TextMuted, fontSize = 10.sp)
                    }
                }
            }

            AnimatedVisibility(
                visible = expandedSpecs,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

                    Text("Gedetailleerde Technische Specificaties:", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    val specs = listOf(
                        "Bluetooth Versie" to "Bluetooth 5.3 met Multipoint Dual Sync",
                        "Audio Codec Support" to "LDAC (24-bit/96kHz), AAC, SBC",
                        "Transducer Formaat" to "40mm High-Performance Neodymium",
                        "Impedantie / Gevoeligheid" to "32 Ohm / 102 dB/mW (1kHz)",
                        "Microfoon Systeem" to "4x ruisonderdrukkende MEMS met Aura Voice",
                        "Snel-laad Technologie" to "USB-C (15 min laden = 5 uur luisteren)",
                        "Lage Latency Modus" to "45 ms (Audio / Video Sync)",
                        "Hardware Gewicht" to "245 gram (Ergonomische Memory Foam)"
                    )

                    specs.forEach { (label, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, color = TextMuted, fontSize = 11.sp)
                            Text(value, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Tah6519EstimatedBatteryBanner(
    settings: com.example.data.HeadphoneSettings,
    isCharging: Boolean,
    onNavigateToBattery: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val batteryLevel = settings.batteryLevel
    val ancMode = settings.ancMode
    val ldacEnabled = settings.ldacEnabled

    val baseMaxHours = if (ancMode != "OFF") 40f else 80f
    val codecFactor = if (ldacEnabled) 0.75f else 1.0f
    val maxHours = baseMaxHours * codecFactor
    val estHours = if (batteryLevel == 0) 0 else ((batteryLevel / 100f) * maxHours).toInt()

    val animatedBatteryLevel by animateFloatAsState(
        targetValue = batteryLevel.toFloat(),
        animationSpec = tween(700),
        label = "banner_battery_anim"
    )
    
    val pulseTransition = rememberInfiniteTransition(label = "banner_pulse")
    val chargingPulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "charging_pulse_alpha"
    )

    val batteryColor = when {
        isCharging -> HighlightSky
        batteryLevel <= 20 -> StatusDanger
        batteryLevel <= 50 -> StatusYellow
        else -> StatusSuccess
    }
    
    val currentIconAlpha = if (isCharging) chargingPulseAlpha else 1f
    val currentBgAlpha = if (isCharging) chargingPulseAlpha * 0.25f else 0.15f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tah6519_estimated_battery_banner")
            .clickable { onNavigateToBattery() },
        colors = CardDefaults.cardColors(containerColor = DarkPanel),
        border = BorderStroke(1.dp, batteryColor.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(batteryColor.copy(alpha = currentBgAlpha), CircleShape)
                            .border(1.dp, batteryColor.copy(alpha = 0.5f * currentIconAlpha), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isCharging -> Icons.Filled.BatteryChargingFull
                                batteryLevel <= 20 -> Icons.Filled.BatteryAlert
                                batteryLevel <= 60 -> Icons.Filled.Battery4Bar
                                else -> Icons.Filled.BatteryFull
                            },
                            contentDescription = "Estimated Battery Level",
                            tint = batteryColor.copy(alpha = currentIconAlpha),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Philips TAH6519 Accu",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = batteryColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (isCharging) "Opladen..." else if (batteryLevel <= 20) "Kritiek" else "Optimaal",
                                    color = batteryColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isCharging) {
                                val mins = ((100 - batteryLevel) * 0.9f).toInt()
                                "⚡ Snelladen via USB-C • ~${mins}m tot 100%"
                            } else {
                                "🔋 Berekend: ~$estHours uur luistertijd (${if (ancMode != "OFF") "ANC Aan" else "ANC Uit"})"
                            },
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Text(
                    text = "$batteryLevel%",
                    color = batteryColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
            }

            // Visual Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(DarkBg, shape = RoundedCornerShape(4.dp))
                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(4.dp))
                        .padding(1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((animatedBatteryLevel / 100f).coerceIn(0f, 1f))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(batteryColor.copy(alpha = 0.7f), batteryColor)
                                ),
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "L: ${batteryLevel}% · R: ${batteryLevel}%",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                    Text(
                        text = if (ldacEnabled) "Hi-Res LDAC Mode" else "SBC / AAC Mode",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
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
                        imageVector = Icons.Filled.SystemUpdate,
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
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StatusYellow.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
                        .border(1.dp, StatusYellow.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "VOORBEREIDING:",
                            color = StatusYellow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        val steps = listOf(
                            "Zorg dat je koptelefoon minimaal 50% is opgeladen.",
                            "Houd de koptelefoon dichtbij je apparaat.",
                            "Sluit de app niet af tijdens de update."
                        )
                        steps.forEachIndexed { index, step ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    color = StatusYellow,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = step,
                                    color = TextPrimary,
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

@Composable
fun PhilipsPremiumSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = HighlightSky
) {
    val haptic = LocalHapticFeedback.current
    val transition = updateTransition(targetState = checked, label = "switch_transition")

    val trackColor by transition.animateColor(
        transitionSpec = { tween(durationMillis = 250, easing = FastOutSlowInEasing) },
        label = "track_color"
    ) { isChecked ->
        if (isChecked) activeColor.copy(alpha = 0.35f) else Color(0xFF162035)
    }

    val borderColor by transition.animateColor(
        transitionSpec = { tween(durationMillis = 250, easing = FastOutSlowInEasing) },
        label = "border_color"
    ) { isChecked ->
        if (isChecked) activeColor.copy(alpha = 0.8f) else DarkBorder
    }

    val thumbColor by transition.animateColor(
        transitionSpec = { tween(durationMillis = 250, easing = FastOutSlowInEasing) },
        label = "thumb_color"
    ) { isChecked ->
        if (isChecked) activeColor else TextMuted
    }

    val thumbOffset by transition.animateDp(
        transitionSpec = { spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy) },
        label = "thumb_offset"
    ) { isChecked ->
        if (isChecked) 20.dp else 2.dp
    }

    val thumbScale by transition.animateFloat(
        transitionSpec = { spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy) },
        label = "thumb_scale"
    ) { isChecked ->
        1.0f
    }

    Box(
        modifier = modifier
            .width(48.dp)
            .height(28.dp)
            .background(color = trackColor, shape = RoundedCornerShape(14.dp))
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(!checked)
            }
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .graphicsLayer(scaleX = thumbScale, scaleY = thumbScale)
                .background(color = thumbColor, shape = CircleShape)
                .shadow(elevation = if (checked) 4.dp else 0.dp, shape = CircleShape)
                .border(
                    width = 1.dp,
                    color = if (checked) Color.White.copy(alpha = 0.6f) else Color.Transparent,
                    shape = CircleShape
                )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    colors: SliderColors = SliderDefaults.colors(),
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isInteracting = isPressed || isDragged

    val activeScale by animateFloatAsState(
        targetValue = if (isInteracting) 1.05f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "slider_active_scale"
    )

    val trackHeight by animateDpAsState(
        targetValue = if (isInteracting) 8.dp else 4.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "slider_track_height"
    )

    val thumbScale by animateFloatAsState(
        targetValue = if (isInteracting) 1.25f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "slider_thumb_scale"
    )

    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        interactionSource = interactionSource,
        colors = SliderDefaults.colors(
            thumbColor = colors.thumbColor,
            activeTrackColor = colors.activeTrackColor,
            inactiveTrackColor = colors.inactiveTrackColor,
            activeTickColor = colors.activeTickColor,
            inactiveTickColor = colors.inactiveTickColor
        ),
        thumb = {
            Box(
                modifier = Modifier
                    .graphicsLayer(scaleX = thumbScale, scaleY = thumbScale)
                    .size(20.dp)
                    .background(colors.thumbColor, shape = CircleShape)
                    .border(2.dp, Color.White, shape = CircleShape)
                    .shadow(
                        elevation = if (isInteracting) 6.dp else 2.dp,
                        shape = CircleShape,
                        clip = false
                    )
            )
        },
        track = { sliderPositions ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .background(
                        color = colors.inactiveTrackColor,
                        shape = RoundedCornerShape(trackHeight / 2)
                    )
            ) {
                val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
                    .coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(colors.activeTrackColor.copy(alpha = 0.8f), colors.activeTrackColor)
                            ),
                            shape = RoundedCornerShape(trackHeight / 2)
                        )
                )
            }
        },
        modifier = modifier
            .graphicsLayer(scaleX = activeScale, scaleY = activeScale)
    )
}

class SineWaveGenerator {
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    @Suppress("DEPRECATION")
    suspend fun startTone(frequency: Float, volumeFraction: Float) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        stopTone()
        
        val sampleRate = 44100
        val numSamples = sampleRate * 2
        val sample = DoubleArray(numSamples)
        val generatedSnd = ByteArray(2 * numSamples)

        for (i in 0 until numSamples) {
            sample[i] = kotlin.math.sin(2.0 * kotlin.math.PI * i / (sampleRate / frequency))
        }

        var idx = 0
        for (dVal in sample) {
            val valShort = (dVal * 32767).toInt().toShort()
            generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
            generatedSnd[idx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
        }

        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBufferSize, generatedSnd.size),
                AudioTrack.MODE_STATIC
            )

            track.write(generatedSnd, 0, generatedSnd.size)
            track.setLoopPoints(0, numSamples, -1)
            
            val vol = volumeFraction.coerceIn(0f, 1f)
            track.setVolume(vol)
            
            track.play()
            audioTrack = track
            isPlaying = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setVolume(volumeFraction: Float) {
        try {
            val vol = volumeFraction.coerceIn(0f, 1f)
            audioTrack?.setVolume(vol)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopTone() {
        try {
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            audioTrack = null
            isPlaying = false
        }
    }
}

class SoundscapeSynthesizer {
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private val random = Random()

    @Suppress("DEPRECATION")
    suspend fun startSoundscape(type: String, volumeFraction: Float) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        stopSoundscape()
        
        val sampleRate = 22050
        val numSamples = sampleRate * 4
        val generatedSnd = ByteArray(2 * numSamples)

        val volCoeff = volumeFraction.coerceIn(0f, 1f)

        when (type) {
            "Witte Ruis" -> {
                for (i in 0 until numSamples) {
                    val value = (random.nextGaussian() * 0.25 * 32767).toInt().coerceIn(-32768, 32767).toShort()
                    val idx = i * 2
                    generatedSnd[idx] = (value.toInt() and 0x00ff).toByte()
                    generatedSnd[idx + 1] = ((value.toInt() and 0xff00) ushr 8).toByte()
                }
            }
            "Zachte Regen" -> {
                var lastVal = 0.0
                for (i in 0 until numSamples) {
                    val white = random.nextGaussian() * 0.15
                    var current = 0.95 * lastVal + 0.05 * white
                    if (random.nextFloat() < 0.0003f) {
                        current += (random.nextFloat() * 0.4 - 0.2)
                    }
                    lastVal = current.coerceIn(-1.0, 1.0)
                    val value = (lastVal * 32767).toInt().toShort()
                    val idx = i * 2
                    generatedSnd[idx] = (value.toInt() and 0x00ff).toByte()
                    generatedSnd[idx + 1] = ((value.toInt() and 0xff00) ushr 8).toByte()
                }
            }
            "Oceaanbries" -> {
                for (i in 0 until numSamples) {
                    val lfo = 0.5 + 0.5 * kotlin.math.sin(2.0 * kotlin.math.PI * 0.15 * i / sampleRate)
                    val white = random.nextGaussian() * 0.12 * lfo
                    val value = (white * 32767).toInt().coerceIn(-32768, 32767).toShort()
                    val idx = i * 2
                    generatedSnd[idx] = (value.toInt() and 0x00ff).toByte()
                    generatedSnd[idx + 1] = ((value.toInt() and 0xff00) ushr 8).toByte()
                }
            }
            "Bosgeluiden" -> {
                var lastVal = 0.0
                val doubleSamples = DoubleArray(numSamples)
                
                for (i in 0 until numSamples) {
                    val lfo = 0.6 + 0.4 * kotlin.math.sin(2.0 * kotlin.math.PI * 0.05 * i / sampleRate)
                    val white = random.nextGaussian() * 0.05 * lfo
                    lastVal = 0.98 * lastVal + 0.02 * white
                    doubleSamples[i] = lastVal
                }

                var chirpIndex = sampleRate / 2
                while (chirpIndex < numSamples - sampleRate) {
                    val chirpLength = (sampleRate * 0.2f).toInt()
                    val baseFreq = 2200f + random.nextFloat() * 800f
                    
                    for (j in 0 until chirpLength) {
                        val t = j.toDouble() / sampleRate
                        val freq = baseFreq + 600f * kotlin.math.sin(2.0 * kotlin.math.PI * 5.0 * t)
                        val amp = 0.08 * kotlin.math.sin(kotlin.math.PI * j / chirpLength)
                        doubleSamples[chirpIndex + j] += amp * kotlin.math.sin(2.0 * kotlin.math.PI * freq * t)
                    }
                    chirpIndex += sampleRate + random.nextInt(sampleRate * 2)
                }

                for (i in 0 until numSamples) {
                    val value = (doubleSamples[i].coerceIn(-1.0, 1.0) * 32767).toInt().toShort()
                    val idx = i * 2
                    generatedSnd[idx] = (value.toInt() and 0x00ff).toByte()
                    generatedSnd[idx + 1] = ((value.toInt() and 0xff00) ushr 8).toByte()
                }
            }
        }

        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBufferSize, generatedSnd.size),
                AudioTrack.MODE_STATIC
            )

            track.write(generatedSnd, 0, generatedSnd.size)
            track.setLoopPoints(0, numSamples, -1)
            
            track.setVolume(volCoeff * 0.35f)
            track.play()
            
            audioTrack = track
            isPlaying = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setVolume(volumeFraction: Float) {
        try {
            val vol = volumeFraction.coerceIn(0f, 1f)
            audioTrack?.setVolume(vol * 0.35f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopSoundscape() {
        try {
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            audioTrack = null
            isPlaying = false
        }
    }
}

class MelodySynthesizer {
    private var audioTrack: AudioTrack? = null
    private var job: kotlinx.coroutines.Job? = null
    
    @Volatile
    private var isPlaying = false

    fun startMelody(
        trackIndex: Int, 
        isYoutube: Boolean, 
        settings: HeadphoneSettings,
        youtubeTrackName: String = "",
        scope: kotlinx.coroutines.CoroutineScope
    ) {
        stopMelody()
        if (isYoutube) return

        job = scope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val sampleRate = 44100
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufferSize, 4096)

            try {
                val track = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                )

                audioTrack = track
                isPlaying = true
                track.play()

                val bands = settings.getBands()
                val bassDb = ((bands.getOrNull(0) ?: 0f) + (bands.getOrNull(1) ?: 0f) + (bands.getOrNull(2) ?: 0f)) / 3f + 
                             (if (settings.dynamicBassEnabled) settings.dynamicBassLevel * 3.5f else 0f)
                val bassGain = Math.pow(10.0, (bassDb / 20.0).toDouble()).toFloat().coerceIn(0.2f, 2.5f)
                
                val midDb = ((bands.getOrNull(3) ?: 0f) + (bands.getOrNull(4) ?: 0f) + (bands.getOrNull(5) ?: 0f) + (bands.getOrNull(6) ?: 0f)) / 4f
                val midGain = Math.pow(10.0, (midDb / 20.0).toDouble()).toFloat().coerceIn(0.2f, 2.0f)
                
                val trebleDb = ((bands.getOrNull(7) ?: 0f) + (bands.getOrNull(8) ?: 0f) + (bands.getOrNull(9) ?: 0f)) / 3f
                val trebleGain = Math.pow(10.0, (trebleDb / 20.0).toDouble()).toFloat().coerceIn(0.2f, 2.0f)

                var phase1 = 0.0
                var phase2 = 0.0
                var sampleIdx = 0L

                val pcmBuffer = ShortArray(1024)
                val byteBuffer = ByteArray(2048)

                while (isPlaying) {
                    val freq1: Double
                    val freq2: Double
                    val volume: Double

                    when (trackIndex % 4) {
                        0 -> { // Philips Signature Sound - Warm Harmonized Melodic Chords
                            val sequence = doubleArrayOf(261.63, 329.63, 392.00, 523.25, 392.00, 329.63)
                            val noteIdx = ((sampleIdx / 7500) % sequence.size).toInt()
                            freq1 = sequence[noteIdx] * midGain
                            freq2 = sequence[noteIdx] * 0.5 * bassGain
                            volume = 0.28
                        }
                        1 -> { // Spatial Audio Demo - Binaural Sweep
                            val sequence = doubleArrayOf(110.0, 220.0, 440.0, 880.0, 440.0)
                            val noteIdx = ((sampleIdx / 5500) % sequence.size).toInt()
                            freq1 = sequence[noteIdx] * midGain
                            freq2 = (sequence[noteIdx] + 3.0) * trebleGain
                            volume = 0.25
                        }
                        2 -> { // Focus White Noise - Soothing Soft Ambient Synth Drone
                            freq1 = 130.81 * bassGain
                            freq2 = 196.00 * midGain
                            volume = 0.22
                        }
                        else -> { // Deep Bass Test - Subwoofer 45Hz/60Hz Deep Bass
                            val sequence = doubleArrayOf(45.0, 55.0, 65.0, 40.0)
                            val noteIdx = ((sampleIdx / 9000) % sequence.size).toInt()
                            freq1 = sequence[noteIdx] * bassGain
                            freq2 = sequence[noteIdx] * 2.0 * bassGain
                            volume = 0.40
                        }
                    }

                    val angleIncrement1 = 2.0 * Math.PI * freq1 / sampleRate
                    val angleIncrement2 = 2.0 * Math.PI * freq2 / sampleRate

                    for (i in 0 until pcmBuffer.size) {
                        val s1 = Math.sin(phase1)
                        val s2 = Math.sin(phase2)
                        phase1 += angleIncrement1
                        phase2 += angleIncrement2
                        if (phase1 > 2.0 * Math.PI) phase1 -= 2.0 * Math.PI
                        if (phase2 > 2.0 * Math.PI) phase2 -= 2.0 * Math.PI

                        val mixedSample = (s1 * 0.65 + s2 * 0.35) * volume
                        val shortVal = (mixedSample * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
                        pcmBuffer[i] = shortVal

                        val byteIdx = i * 2
                        byteBuffer[byteIdx] = (shortVal.toInt() and 0x00ff).toByte()
                        byteBuffer[byteIdx + 1] = ((shortVal.toInt() and 0xff00) ushr 8).toByte()

                        sampleIdx++
                    }

                    track.write(byteBuffer, 0, byteBuffer.size)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopMelody() {
        isPlaying = false
        job?.cancel()
        job = null
        try {
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            audioTrack = null
        }
    }
}



