package com.example.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.YouTubeApi
import com.example.data.HeadphoneRepository
import com.example.data.HeadphoneSettings
import com.example.data.ThemePreferencesRepository
import com.example.data.EqualizerPreferencesRepository
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemeState
import com.example.media.ExoPlayerController
import com.example.media.YouTubeApiController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object UpToDate : UpdateState()
    data class UpdateAvailable(val version: String, val changelog: List<String>) : UpdateState()
    data class Updating(val progress: Float, val statusMessage: String = "Updating...") : UpdateState()
    data class UpdateComplete(val newVersion: String = "v1.5.0") : UpdateState()
}

data class AppTrack(
    val title: String,
    val artist: String,
    val isOffline: Boolean,
    val streamUrl: String = ""
)

data class YouTubeTrack(
    val youtubeId: String,
    val title: String,
    val artist: String,
    val durationSecs: Int,
    val isOffline: Boolean
)

data class ScannedDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val isHeadphone: Boolean
)

data class CompatibleBluetoothDevice(
    val brand: String,
    val name: String,
    val address: String,
    val category: String,
    val driverSizeMm: Int,
    val supportedCodecs: List<String>,
    val maxPlaytimeHours: Int,
    val ancPlaytimeHours: Int,
    val ancSupported: Boolean,
    val bluetoothVersion: String,
    val rssi: Int = -45,
    val fastPairSupported: Boolean = true,
    val isPhilips: Boolean = false
)

class HeadphoneViewModel(application: Application, private val repository: HeadphoneRepository) : ViewModel() {
    val bleManager = com.example.bluetooth.BluetoothLEManager.getInstance(application)
    val exoPlayerController = ExoPlayerController.getInstance(application)
    val youtubeApiController = YouTubeApiController.getInstance(application)

    val themePreferencesRepository = ThemePreferencesRepository(application)
    val equalizerPreferencesRepository = EqualizerPreferencesRepository(application)
    val currentThemeMode = MutableStateFlow(ThemeMode.DARK)
    val currentAppTheme = MutableStateFlow(AppTheme.PHILIPS_STUDIO)
    val customAccentHex = MutableStateFlow("#00E5FF")

    private val _settingsState = MutableStateFlow(HeadphoneSettings())
    val settingsState: StateFlow<HeadphoneSettings> = _settingsState.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    val firmwareVersion = MutableStateFlow("v1.4.2")
    val isFirmwarePolling = MutableStateFlow(true)
    val lastFirmwarePollTime = MutableStateFlow<String?>("Zojuist gecontroleerd")
    val simulatedFirmwareApiUrl = MutableStateFlow("https://api.philips.com/v1/firmware/tah6519/latest")
    val simulatedApiHttpStatus = MutableStateFlow(200)
    val isApiPollingInProgress = MutableStateFlow(false)
    val activeAudioCodec = MutableStateFlow("LDAC")
    val activeSampleRate = MutableStateFlow("96kHz")
    val activeProtocolInfo = MutableStateFlow("Bluetooth 5.3")
    val activeChannelMode = MutableStateFlow("Stereo")
    val ambientDecibel = MutableStateFlow(45)
    val autoOffIsInactive = MutableStateFlow(false)
    val autoOffRemainingSeconds = MutableStateFlow(1800)
    val autoReconnectEnabled = MutableStateFlow(true)
    val batteryFetchProgress = MutableStateFlow(0f)
    val batteryFetchStatus = MutableStateFlow("Idle")
    val bitrateKbps = MutableStateFlow(990)
    val gattStatusMessage = MutableStateFlow("Connected")
    val isAutoReconnecting = MutableStateFlow(false)
    val isCharging = MutableStateFlow(false)
    val isConnecting = MutableStateFlow(false)
    val connectionSuccessEvent = MutableStateFlow<String?>(null)
    val isFetchingBattery = MutableStateFlow(false)
    val isFindMyBeeping = MutableStateFlow(false)
    val findMySignalStatus = MutableStateFlow("Gereed voor opsporen")
    val isGattReading = MutableStateFlow(false)
    val isRecordingNoise = MutableStateFlow(false)
    val isScanningBluetooth = MutableStateFlow(false)
    val isSimulationMode = MutableStateFlow(false)
    val isWearingHeadphones = MutableStateFlow(true)
    val latencyMs = MutableStateFlow(30)
    val mediaDuration = MutableStateFlow(240) // in seconds
    val mediaIsPlaying = MutableStateFlow(false)
    val mediaProgress = MutableStateFlow(0) // in seconds
    val mediaTrackArtist = MutableStateFlow("Philips Studio")
    val mediaTrackName = MutableStateFlow("Atmospheric Harmony")
    val packetLoss = MutableStateFlow(0.01f)
    val reconnectAttempts = MutableStateFlow(0)
    val rssi = MutableStateFlow(-50)
    val serialNumber = MutableStateFlow("SN123456789")
    val shouldCloseApp = MutableStateFlow(false)
    val simulatedDistanceMeters = MutableStateFlow(1.0f)

    // Unresolved states for YouTube Music Dashboard & Sleep Timers
    val currentTrackIndex = MutableStateFlow(0)
    val activeAudioMood = MutableStateFlow("Relaxation")
    val audioMoodVolume = MutableStateFlow(0.5f)
    val isYoutubeActive = MutableStateFlow(false)
    val youtubePlaylistName = MutableStateFlow("Mijn YouTube Playlist")
    val youtubeAccountConnected = MutableStateFlow(false)
    val youtubeAccountName = MutableStateFlow("Gast Gebruiker")
    val lastYoutubePlaylistUrl = MutableStateFlow("")
    val youtubeLastSyncedTime = MutableStateFlow("Niet gesynchroniseerd")
    val sleepTimerTotalMin = MutableStateFlow(30)
    val sleepTimerRemainingSec = MutableStateFlow(1800)
    val sleepTimerRunning = MutableStateFlow(false)
    val sleepTimerAction = MutableStateFlow("PAUSE") // "PAUSE", "OFF", "STILTE"
    val isYoutubeImporting = MutableStateFlow(false)
    val youtubeImportMessage = MutableStateFlow("")

    val playlist = listOf(
        AppTrack("Philips Signature Sound", "Philips Studio", true, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
        AppTrack("Spatial Audio Demo", "Dolby Atmos", false, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
        AppTrack("Focus White Noise", "Ambient Master", true, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
        AppTrack("Deep Bass Test", "Bass Shaker", false, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3")
    )

    val youtubePlaylistTracks = MutableStateFlow(listOf(
        YouTubeTrack("dQw4w9WgXcQ", "Never Gonna Give You Up", "Rick Astley", 212, true),
        YouTubeTrack("L_jWHffIx5E", "Smells Like Teen Spirit", "Nirvana", 301, false),
        YouTubeTrack("9bZkp7q19f0", "PSY - GANGNAM STYLE", "Official PSY", 252, true)
    ))

    val compatibleBluetoothDevices = MutableStateFlow(listOf(
        CompatibleBluetoothDevice(
            brand = "Philips",
            name = "Philips TAH6519",
            address = "00:11:22:33:44:55",
            category = "Over-Ear Wireless",
            driverSizeMm = 40,
            supportedCodecs = listOf("LDAC", "AAC", "SBC"),
            maxPlaytimeHours = 80,
            ancPlaytimeHours = 40,
            ancSupported = true,
            bluetoothVersion = "Bluetooth 5.3",
            rssi = -38,
            fastPairSupported = true,
            isPhilips = true
        ),
        CompatibleBluetoothDevice(
            brand = "Philips",
            name = "Philips TAH8506",
            address = "00:11:22:33:44:88",
            category = "Over-Ear Premium",
            driverSizeMm = 40,
            supportedCodecs = listOf("AAC", "SBC"),
            maxPlaytimeHours = 60,
            ancPlaytimeHours = 35,
            ancSupported = true,
            bluetoothVersion = "Bluetooth 5.2",
            rssi = -42,
            fastPairSupported = true,
            isPhilips = true
        ),
        CompatibleBluetoothDevice(
            brand = "Philips",
            name = "Philips Fidelio L3",
            address = "00:11:22:33:55:99",
            category = "Audiophile Wireless",
            driverSizeMm = 40,
            supportedCodecs = listOf("LDAC", "aptX HD", "AAC"),
            maxPlaytimeHours = 38,
            ancPlaytimeHours = 30,
            ancSupported = true,
            bluetoothVersion = "Bluetooth 5.2",
            rssi = -40,
            fastPairSupported = true,
            isPhilips = true
        ),
        CompatibleBluetoothDevice(
            brand = "Sony",
            name = "Sony WH-1000XM5",
            address = "44:55:66:77:88:99",
            category = "Over-Ear ANC",
            driverSizeMm = 30,
            supportedCodecs = listOf("LDAC", "AAC", "SBC"),
            maxPlaytimeHours = 40,
            ancPlaytimeHours = 30,
            ancSupported = true,
            bluetoothVersion = "Bluetooth 5.3",
            rssi = -48,
            fastPairSupported = true
        ),
        CompatibleBluetoothDevice(
            brand = "Sony",
            name = "Sony WF-1000XM5",
            address = "44:55:66:AA:BB:CC",
            category = "In-Ear / Earbuds",
            driverSizeMm = 8,
            supportedCodecs = listOf("LDAC", "AAC", "LC3"),
            maxPlaytimeHours = 24,
            ancPlaytimeHours = 12,
            ancSupported = true,
            bluetoothVersion = "Bluetooth 5.3",
            rssi = -55,
            fastPairSupported = true
        ),
        CompatibleBluetoothDevice(
            brand = "Bose",
            name = "Bose QuietComfort Ultra",
            address = "A1:B2:C3:D4:E5:F6",
            category = "Over-Ear ANC",
            driverSizeMm = 35,
            supportedCodecs = listOf("aptX Adaptive", "AAC", "SBC"),
            maxPlaytimeHours = 30,
            ancPlaytimeHours = 24,
            ancSupported = true,
            bluetoothVersion = "Bluetooth 5.3",
            rssi = -52,
            fastPairSupported = true
        ),
        CompatibleBluetoothDevice(
            brand = "Apple",
            name = "Apple AirPods Max",
            address = "12:34:56:78:90:AB",
            category = "Over-Ear Premium",
            driverSizeMm = 40,
            supportedCodecs = listOf("AAC", "Spatial Audio"),
            maxPlaytimeHours = 20,
            ancPlaytimeHours = 20,
            ancSupported = true,
            bluetoothVersion = "Bluetooth 5.0",
            rssi = -58,
            fastPairSupported = false
        ),
        CompatibleBluetoothDevice(
            brand = "Sennheiser",
            name = "Sennheiser Momentum 4",
            address = "98:76:54:32:10:FE",
            category = "Over-Ear Wireless",
            driverSizeMm = 42,
            supportedCodecs = listOf("aptX Adaptive", "aptX HD", "AAC"),
            maxPlaytimeHours = 60,
            ancPlaytimeHours = 60,
            ancSupported = true,
            bluetoothVersion = "Bluetooth 5.2",
            rssi = -61,
            fastPairSupported = true
        ),
        CompatibleBluetoothDevice(
            brand = "JBL",
            name = "JBL Live 660NC",
            address = "22:33:44:55:66:77",
            category = "Over-Ear Wireless",
            driverSizeMm = 40,
            supportedCodecs = listOf("AAC", "SBC"),
            maxPlaytimeHours = 65,
            ancPlaytimeHours = 50,
            ancSupported = true,
            bluetoothVersion = "Bluetooth 5.0",
            rssi = -65,
            fastPairSupported = true
        ),
        CompatibleBluetoothDevice(
            brand = "Soundcore",
            name = "Anker Soundcore Space Q45",
            address = "33:44:55:66:77:88",
            category = "Over-Ear ANC",
            driverSizeMm = 40,
            supportedCodecs = listOf("LDAC", "AAC", "SBC"),
            maxPlaytimeHours = 65,
            ancPlaytimeHours = 50,
            ancSupported = true,
            bluetoothVersion = "Bluetooth 5.3",
            rssi = -69,
            fastPairSupported = true
        ),
        CompatibleBluetoothDevice(
            brand = "Samsung",
            name = "Samsung Galaxy Buds2 Pro",
            address = "55:66:77:88:99:AA",
            category = "In-Ear / Earbuds",
            driverSizeMm = 10,
            supportedCodecs = listOf("SSC 24-bit", "AAC", "SBC"),
            maxPlaytimeHours = 29,
            ancPlaytimeHours = 18,
            ancSupported = true,
            bluetoothVersion = "Bluetooth 5.3",
            rssi = -73,
            fastPairSupported = true
        ),
        CompatibleBluetoothDevice(
            brand = "Shure",
            name = "Shure AONIC 50 Gen 2",
            address = "66:77:88:99:AA:BB",
            category = "Studio Audiophile",
            driverSizeMm = 50,
            supportedCodecs = listOf("LDAC", "aptX HD", "aptX Adaptive", "AAC"),
            maxPlaytimeHours = 45,
            ancPlaytimeHours = 35,
            ancSupported = true,
            bluetoothVersion = "Bluetooth 5.2",
            rssi = -50,
            fastPairSupported = true
        ),
        CompatibleBluetoothDevice(
            brand = "Audio-Technica",
            name = "Audio-Technica ATH-M50xBT2",
            address = "77:88:99:AA:BB:CC",
            category = "Studio Reference",
            driverSizeMm = 45,
            supportedCodecs = listOf("LDAC", "AAC", "SBC"),
            maxPlaytimeHours = 50,
            ancPlaytimeHours = 50,
            ancSupported = false,
            bluetoothVersion = "Bluetooth 5.0",
            rssi = -54,
            fastPairSupported = true
        ),
        CompatibleBluetoothDevice(
            brand = "Bowers & Wilkins",
            name = "Bowers & Wilkins PX8",
            address = "88:99:AA:BB:CC:DD",
            category = "Audiophile Premium",
            driverSizeMm = 40,
            supportedCodecs = listOf("aptX Lossless", "aptX Adaptive", "AAC"),
            maxPlaytimeHours = 30,
            ancPlaytimeHours = 30,
            ancSupported = true,
            bluetoothVersion = "Bluetooth 5.2",
            rssi = -46,
            fastPairSupported = true
        ),
        CompatibleBluetoothDevice(
            brand = "Wired Line-In",
            name = "Wired Studio Headphones (3.5mm)",
            address = "WIRED_LINE_IN",
            category = "Wired 3.5mm / USB-C",
            driverSizeMm = 45,
            supportedCodecs = listOf("Hi-Res PCM 24-bit/96kHz"),
            maxPlaytimeHours = 999,
            ancPlaytimeHours = 999,
            ancSupported = false,
            bluetoothVersion = "Direct Cable Line-In",
            rssi = -20,
            fastPairSupported = false
        )
    ))

    val scannedDevices = MutableStateFlow(listOf(
        ScannedDevice("Philips TAH6519", "00:11:22:33:44:55", -42, true),
        ScannedDevice("Sony WH-1000XM5", "44:55:66:77:88:99", -48, true),
        ScannedDevice("Bose QuietComfort Ultra", "A1:B2:C3:D4:E5:F6", -52, true),
        ScannedDevice("Apple AirPods Max", "12:34:56:78:90:AB", -58, true),
        ScannedDevice("Sennheiser Momentum 4", "98:76:54:32:10:FE", -61, true),
        ScannedDevice("JBL Live 660NC", "22:33:44:55:66:77", -65, true),
        ScannedDevice("Anker Soundcore Q45", "33:44:55:66:77:88", -69, true),
        ScannedDevice("Samsung Galaxy Buds2 Pro", "55:66:77:88:99:AA", -73, true),
        ScannedDevice("Wired Studio Headphones (3.5mm)", "WIRED_LINE_IN", -25, true),
        ScannedDevice("Stefan's Pixel 8", "AA:BB:CC:DD:EE:FF", -82, false),
        ScannedDevice("Office Smart TV", "11:22:33:44:55:66", -92, false)
    ))

    val defaultPresets = mapOf(
        "Philips Signature" to listOf(3.5f, 2.5f, 1.0f, 0.0f, -0.5f, 0.5f, 1.5f, 2.5f, 3.5f, 2.5f),
        "Bass Boost" to listOf(8.5f, 7.0f, 5.0f, 2.5f, 0.0f, -0.5f, 0.0f, 0.5f, 1.0f, 0.5f),
        "Vocal Clarity" to listOf(-3.0f, -2.0f, 0.0f, 2.5f, 5.0f, 5.5f, 4.0f, 2.0f, -1.0f, -2.0f),
        "Voice Clarity" to listOf(-3.0f, -2.0f, 0.0f, 2.5f, 5.0f, 5.5f, 4.0f, 2.0f, -1.0f, -2.0f),
        "Pop" to listOf(3.0f, 2.0f, 0.5f, -1.0f, -1.5f, -1.0f, 0.5f, 1.5f, 2.5f, 3.0f),
        "Rock" to listOf(5.0f, 4.0f, 2.0f, 0.5f, -0.5f, 0.5f, 2.5f, 4.0f, 5.0f, 4.5f),
        "Classical" to listOf(4.5f, 3.5f, 2.0f, 1.0f, -0.5f, 0.0f, 1.5f, 3.0f, 4.0f, 4.5f),
        "Podcast" to listOf(-2.5f, -1.5f, 0.5f, 3.0f, 4.5f, 5.0f, 3.5f, 1.5f, -1.0f, -2.0f),
        "Acoustic" to listOf(1.5f, 2.0f, 2.5f, 1.5f, 0.5f, 1.0f, 2.0f, 3.0f, 3.5f, 2.0f),
        "Treble Sparkle" to listOf(-1.0f, -0.5f, 0.0f, 0.5f, 1.5f, 2.5f, 4.5f, 6.5f, 8.0f, 6.5f),
        "Cinema 3D" to listOf(6.0f, 4.5f, 2.0f, 0.0f, -1.0f, 1.0f, 3.0f, 4.0f, 4.5f, 3.5f),
        "Dynamic Bass" to listOf(7.0f, 5.5f, 3.5f, 1.5f, 0.0f, 0.0f, 0.5f, 1.0f, 1.5f, 1.0f),
        "Balanced" to listOf(1.0f, 1.0f, 0.5f, 0.0f, 0.0f, 0.5f, 1.0f, 1.5f, 1.5f, 1.0f),
        "Flat" to listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
    )
    val presets: Map<String, List<Float>> = defaultPresets

    init {
        // Collect BluetoothLEManager states
        viewModelScope.launch {
            bleManager.isScanning.collect { scanning ->
                isScanningBluetooth.value = scanning
            }
        }
        viewModelScope.launch {
            bleManager.scannedDevices.collect { bleDevices ->
                if (bleDevices.isNotEmpty()) {
                    val mapped = bleDevices.map { dev ->
                        ScannedDevice(dev.name, dev.address, dev.rssi, dev.isTah6519)
                    }
                    scannedDevices.value = mapped
                }
            }
        }
        viewModelScope.launch {
            bleManager.batteryLevel.collect { level ->
                level?.let { updateBatteryLevel(it) }
            }
        }
        viewModelScope.launch {
            bleManager.firmwareVersion.collect { fw ->
                fw?.let { firmwareVersion.value = it }
            }
        }
        viewModelScope.launch {
            bleManager.statusMessage.collect { msg ->
                gattStatusMessage.value = msg
            }
        }

        // Room database caching and restoration for YouTube Music metadata
        viewModelScope.launch {
            val cachedYt = repository.getCachedYoutubeTracks()
            if (cachedYt.isNotEmpty()) {
                val ytTracks = cachedYt.map {
                    YouTubeTrack(
                        youtubeId = it.youtubeId ?: it.id.replace("yt_", "").substringBefore("_"),
                        title = it.title,
                        artist = it.artist,
                        durationSecs = it.durationSecs,
                        isOffline = true
                    )
                }
                youtubePlaylistTracks.value = ytTracks
                val settings = repository.getSettings()
                if (settings.cachedYoutubePlaylistName.isNotBlank()) {
                    youtubePlaylistName.value = settings.cachedYoutubePlaylistName
                }
                if (settings.cachedYoutubeLastSyncedTime.isNotBlank()) {
                    youtubeLastSyncedTime.value = settings.cachedYoutubeLastSyncedTime
                }
            } else {
                persistYoutubeTracksToRoom(
                    tracks = youtubePlaylistTracks.value,
                    playlistName = youtubePlaylistName.value,
                    lastSynced = youtubeLastSyncedTime.value
                )
            }
        }

        // Room database caching and restoration for Now Playing playlist
        viewModelScope.launch {
            val cached = repository.getCachedTracks()
            if (cached.isNotEmpty() && exoPlayerController.queue.value.isEmpty()) {
                val queueItems = cached.map {
                    com.example.media.MediaQueueItem(
                        id = it.id,
                        title = it.title,
                        artist = it.artist,
                        durationSecs = it.durationSecs,
                        artworkUrl = it.artworkUrl,
                        isYoutube = it.isYoutube,
                        youtubeId = it.youtubeId
                    )
                }
                exoPlayerController.updateQueue(queueItems, 0)
            }
        }

        viewModelScope.launch {
            exoPlayerController.queue.collect { queueItems ->
                if (queueItems.isNotEmpty()) {
                    val entities = queueItems.mapIndexed { index, item ->
                        com.example.data.CachedTrackEntity(
                            id = item.id,
                            title = item.title,
                            artist = item.artist,
                            durationSecs = item.durationSecs,
                            artworkUrl = item.artworkUrl,
                            isYoutube = item.isYoutube,
                            youtubeId = item.youtubeId,
                            orderIndex = index
                        )
                    }
                    repository.saveCachedTracks(entities)
                } else {
                    repository.clearCachedTracks()
                }
            }
        }

        viewModelScope.launch {
            repository.settingsFlow.collect { settings ->
                _settingsState.value = settings
                autoReconnectEnabled.value = settings.autoReconnectOnLaunch
            }
        }

        // Observe DataStore Theme Preferences
        viewModelScope.launch {
            themePreferencesRepository.themeModeFlow.collect { mode ->
                currentThemeMode.value = mode
                ThemeState.themeMode = mode
            }
        }

        viewModelScope.launch {
            themePreferencesRepository.activeAppThemeFlow.collect { theme ->
                currentAppTheme.value = theme
                ThemeState.activeTheme = theme
            }
        }

        viewModelScope.launch {
            themePreferencesRepository.customAccentHexFlow.collect { hex ->
                customAccentHex.value = hex
                try {
                    val colorInt = android.graphics.Color.parseColor(hex)
                    ThemeState.customAccentColor = androidx.compose.ui.graphics.Color(colorInt)
                } catch (e: Exception) {
                    ThemeState.customAccentColor = androidx.compose.ui.graphics.Color(0xFF00E5FF)
                }
            }
        }

        // Observe DataStore Equalizer Preferences
        viewModelScope.launch {
            equalizerPreferencesRepository.equalizerPreferencesFlow.collect { eqData ->
                _settingsState.value = _settingsState.value.copyWithBands(eqData.bands).copy(
                    activePreset = eqData.activePreset,
                    customPresets = eqData.customPresetsSerialized,
                    masterGain = eqData.masterGain,
                    dynamicBassLevel = eqData.dynamicBassLevel
                )
            }
        }

        // Automated Firmware API polling service
        viewModelScope.launch {
            while (true) {
                if (isFirmwarePolling.value && _updateState.value is UpdateState.Idle) {
                    pollFirmwareApi(manual = false)
                }
                delay(30_000)
            }
        }
        
        // Auto-reconnect on app launch for last paired device
        viewModelScope.launch {
            delay(800)
            val currentSettings = _settingsState.value
            if (currentSettings.autoReconnectOnLaunch && !currentSettings.connected) {
                triggerLaunchAutoReconnect()
            }
        }
        
        // Background loop for media progress, sleep timer, auto off, and REAL-TIME BATTERY DRAIN/CHARGE
        viewModelScope.launch {
            var batteryTickCounter = 0
            while (true) {
                delay(1000)
                batteryTickCounter++
                
                // Real-time dynamic battery simulation & tracking
                val currentSettings = _settingsState.value
                if (isCharging.value) {
                    if (batteryTickCounter % 3 == 0) { // Charge 1% every 3 seconds
                        if (currentSettings.batteryLevel < 100) {
                            updateBatteryLevel(currentSettings.batteryLevel + 1)
                        }
                    }
                } else if (currentSettings.connected) {
                    val drainInterval = if (mediaIsPlaying.value) {
                        if (currentSettings.ancMode == "ON") 25 else 40 // Drain faster when playing with ANC
                    } else {
                        90 // Slow idle discharge
                    }
                    if (batteryTickCounter % drainInterval == 0) {
                        if (currentSettings.batteryLevel > 0) {
                            updateBatteryLevel(currentSettings.batteryLevel - 1)
                        }
                    }
                }
                
                // Sleep Timer
                if (sleepTimerRunning.value) {
                    val rem = sleepTimerRemainingSec.value
                    if (rem > 0) {
                        sleepTimerRemainingSec.value = rem - 1
                    } else {
                        sleepTimerRunning.value = false
                        if (sleepTimerAction.value == "PAUSE") {
                            mediaIsPlaying.value = false
                        }
                    }
                }
                
                // Media Progress
                if (mediaIsPlaying.value) {
                    val currentProgress = mediaProgress.value
                    val duration = mediaDuration.value
                    if (currentProgress < duration) {
                        mediaProgress.value = currentProgress + 1
                    } else if (!isYoutubeActive.value) {
                        playNextTrack()
                    }
                }
                
                // Auto Off Timer
                if (currentSettings.connected && currentSettings.autoPowerOffEnabled) {
                    if (!isWearingHeadphones.value && !mediaIsPlaying.value) {
                        val rem = autoOffRemainingSeconds.value
                        if (rem > 0) {
                            autoOffRemainingSeconds.value = rem - 1
                        } else {
                            disconnectDevice()
                        }
                    } else {
                        autoOffRemainingSeconds.value = currentSettings.autoPowerOffMinutes * 60
                    }
                }
            }
        }
    }

    private fun processIntelligentBatteryPreservation(settings: HeadphoneSettings): HeadphoneSettings {
        val battery = settings.batteryLevel
        val isLowPower = battery <= settings.intelligentBatteryThreshold && !isCharging.value
        val isPreservationEnabled = settings.intelligentBatteryPreservationEnabled
        
        val shouldBeActive = isPreservationEnabled && isLowPower
        val targetPollingMs = if (shouldBeActive) 5000 else 1000
        
        // Dynamic ANC intensity adjustment: cap ANC level when preservation is active
        val targetAncLevel = if (shouldBeActive && settings.ancMode == "ON" && settings.ancLevel > settings.batteryPreservationAncCap) {
            settings.batteryPreservationAncCap
        } else {
            settings.ancLevel
        }

        return settings.copy(
            isBatteryPreservationActive = shouldBeActive,
            bluetoothPollingIntervalMs = targetPollingMs,
            ancLevel = targetAncLevel
        )
    }

    fun updateSettings(update: (HeadphoneSettings) -> HeadphoneSettings) {
        val updatedRaw = update(_settingsState.value)
        val updated = processIntelligentBatteryPreservation(updatedRaw)
        _settingsState.value = updated
        viewModelScope.launch {
            repository.updateSettings(updated)
            equalizerPreferencesRepository.saveEqualizerPreferences(
                activePreset = updated.activePreset ?: "Philips Signature",
                bands = updated.getBands(),
                customPresetsSerialized = updated.customPresets,
                masterGain = updated.masterGain,
                dynamicBassLevel = updated.dynamicBassLevel
            )
        }
    }

    private fun syncActiveTrackMetadata() {
        if (isYoutubeActive.value) {
            val tracks = youtubePlaylistTracks.value
            val idx = currentTrackIndex.value
            if (idx in tracks.indices) {
                val track = tracks[idx]
                mediaTrackName.value = track.title
                mediaTrackArtist.value = track.artist
                mediaDuration.value = track.durationSecs
                mediaProgress.value = 0
                exoPlayerController.pause()
            }
        } else {
            val idx = currentTrackIndex.value
            if (idx in playlist.indices) {
                val track = playlist[idx]
                mediaTrackName.value = track.title
                mediaTrackArtist.value = track.artist
                mediaDuration.value = 240
                mediaProgress.value = 0
                val streamUrl = track.streamUrl.ifBlank { "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3" }
                exoPlayerController.playTrack(
                    title = track.title,
                    artist = track.artist,
                    streamUrl = streamUrl,
                    artworkUrl = null
                )
            }
        }
    }

    // Unresolved functions for YouTube, Playback & Timers
    fun playPreviousTrack() {
        val size = if (isYoutubeActive.value) youtubePlaylistTracks.value.size else playlist.size
        if (size > 0) {
            currentTrackIndex.value = (currentTrackIndex.value - 1 + size) % size
            syncActiveTrackMetadata()
        }
    }

    fun playNextTrack() {
        val size = if (isYoutubeActive.value) youtubePlaylistTracks.value.size else playlist.size
        if (size > 0) {
            currentTrackIndex.value = (currentTrackIndex.value + 1) % size
            syncActiveTrackMetadata()
        }
    }

    fun setYoutubeActive(active: Boolean) {
        isYoutubeActive.value = active
        currentTrackIndex.value = 0
        mediaIsPlaying.value = true
        syncActiveTrackMetadata()
    }

    fun playTrack(index: Int) {
        isYoutubeActive.value = false
        currentTrackIndex.value = index
        mediaIsPlaying.value = true
        syncActiveTrackMetadata()
    }

    fun setActiveAudioMood(mood: String) {
        activeAudioMood.value = mood
    }

    fun setAudioMoodVolume(volume: Float) {
        audioMoodVolume.value = volume
    }

    fun toggleYoutubeAccount() {
        youtubeAccountConnected.value = !youtubeAccountConnected.value
        youtubeAccountName.value = if (youtubeAccountConnected.value) "Stefan de Sain" else "Gast Gebruiker"
    }

    fun importYoutubePlaylist(url: String) {
        val rawInput = if (url.isBlank()) lastYoutubePlaylistUrl.value else url.trim()
        viewModelScope.launch {
            isYoutubeImporting.value = true
            youtubeImportMessage.value = "Verbinding maken met YouTube database..."
            lastYoutubePlaylistUrl.value = rawInput

            val currentLocaleTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            val apiKey = try { BuildConfig.YOUTUBE_API_KEY } catch (e: Exception) { "" }
            val hasValidApiKey = apiKey.isNotBlank() && apiKey != "MY_YOUTUBE_API_KEY"

            try {
                // Check if rawInput contains a playlist ID or URL
                val playlistId = when {
                    rawInput.contains("PL_CHILL_LOFI_BEATS") -> "PLw-VjHDlEOgvWPror36E_A0X0Aofc7L0D"
                    rawInput.contains("PL_WORKOUT_ENERGY_BEATS") -> "PL4fGSI1pDJn6O1LS0XSdF3RyO0Rq_LDeI"
                    rawInput.contains("PL_POP_TOP_HITS") -> "PLDcnymzs18LU4K9s7468BvtMeih62sEVX"
                    rawInput.contains("list=") -> {
                        val idx = rawInput.indexOf("list=")
                        val start = idx + 5
                        var end = rawInput.indexOf("&", start)
                        if (end == -1) end = rawInput.length
                        rawInput.substring(start, end)
                    }
                    rawInput.startsWith("PL") -> rawInput
                    else -> ""
                }

                // Check if rawInput is a single YouTube video URL or ID
                val videoId = when {
                    playlistId.isNotBlank() -> ""
                    rawInput.contains("v=") -> {
                        val idx = rawInput.indexOf("v=")
                        val start = idx + 2
                        var end = rawInput.indexOf("&", start)
                        if (end == -1) end = rawInput.length
                        rawInput.substring(start, end)
                    }
                    rawInput.contains("youtu.be/") -> {
                        val idx = rawInput.indexOf("youtu.be/")
                        val start = idx + 9
                        var end = rawInput.indexOf("?", start)
                        if (end == -1) end = rawInput.length
                        rawInput.substring(start, end)
                    }
                    rawInput.length == 11 && !rawInput.contains(" ") -> rawInput
                    else -> ""
                }

                if (playlistId.isNotBlank()) {
                    youtubeImportMessage.value = "Playlist nummers ophalen uit YouTube database..."
                    var fetchedTracks = emptyList<YouTubeTrack>()

                    // 1. Try YouTube v3 API if API Key is configured
                    if (hasValidApiKey) {
                        try {
                            val api = com.example.api.YouTubeApi.create()
                            val response = api.getPlaylistItems(playlistId = playlistId, apiKey = apiKey)
                            val items = response.items
                            if (!items.isNullOrEmpty()) {
                                fetchedTracks = items.map { item ->
                                    val snippet = item.snippet
                                    YouTubeTrack(
                                        youtubeId = snippet?.resourceId?.videoId ?: "",
                                        title = snippet?.title ?: "YouTube Track",
                                        artist = snippet?.videoOwnerChannelTitle ?: snippet?.channelTitle ?: "YouTube Artist",
                                        durationSecs = (180..300).random(),
                                        isOffline = true
                                    )
                                }.filter { it.youtubeId.isNotBlank() }
                            }
                        } catch (e: Exception) {
                            // Fallback to RSS if API fails
                        }
                    }

                    // 2. Try YouTube Official RSS feed if no tracks fetched yet
                    if (fetchedTracks.isEmpty()) {
                        fetchedTracks = com.example.api.YouTubeRssFetcher.fetchPlaylistTracks(playlistId)
                    }

                    if (fetchedTracks.isNotEmpty()) {
                        youtubePlaylistTracks.value = fetchedTracks
                        youtubePlaylistName.value = "YouTube Playlist ($playlistId)"
                        youtubeLastSyncedTime.value = "Vandaag, $currentLocaleTime"
                        youtubeImportMessage.value = "Gesynchroniseerd met YouTube! (${fetchedTracks.size} nummers)"
                        persistYoutubeTracksToRoom(fetchedTracks, "YouTube Playlist ($playlistId)", "Vandaag, $currentLocaleTime")
                    } else {
                        loadFallbackPlaylist(rawInput)
                    }

                } else if (videoId.isNotBlank()) {
                    youtubeImportMessage.value = "Video details ophalen via YouTube OEmbed..."
                    var videoTitle = "YouTube Video"
                    var videoAuthor = "YouTube Channel"

                    try {
                        val api = com.example.api.YouTubeApi.create()
                        val oembed = api.getOEmbed(videoUrl = "https://www.youtube.com/watch?v=$videoId")
                        if (!oembed.title.isNullOrBlank()) videoTitle = oembed.title
                        if (!oembed.authorName.isNullOrBlank()) videoAuthor = oembed.authorName
                    } catch (e: Exception) {
                        // Keep defaults
                    }

                    val newTrack = YouTubeTrack(
                        youtubeId = videoId,
                        title = videoTitle,
                        artist = videoAuthor,
                        durationSecs = (180..300).random(),
                        isOffline = true
                    )

                    val updatedList = listOf(newTrack) + youtubePlaylistTracks.value.filter { it.youtubeId != videoId }
                    youtubePlaylistTracks.value = updatedList
                    youtubePlaylistName.value = "Afgespeeld: $videoTitle"
                    youtubeLastSyncedTime.value = "Vandaag, $currentLocaleTime"
                    youtubeImportMessage.value = "Video toegevoegd uit YouTube database!"
                    persistYoutubeTracksToRoom(updatedList, "Afgespeeld: $videoTitle", "Vandaag, $currentLocaleTime")

                } else if (rawInput.isNotBlank()) {
                    // Search query logic
                    youtubeImportMessage.value = "Zoeken op YouTube naar '$rawInput'..."
                    var searchTracks = emptyList<YouTubeTrack>()

                    if (hasValidApiKey) {
                        try {
                            val api = com.example.api.YouTubeApi.create()
                            val searchResp = api.searchVideos(query = rawInput, apiKey = apiKey)
                            val items = searchResp.items
                            if (!items.isNullOrEmpty()) {
                                searchTracks = items.mapNotNull { item ->
                                    val snippet = item.snippet
                                    val vId = item.id?.videoId ?: ""
                                    if (vId.isNotBlank()) {
                                        YouTubeTrack(
                                            youtubeId = vId,
                                            title = snippet?.title ?: rawInput,
                                            artist = snippet?.channelTitle ?: "YouTube Music",
                                            durationSecs = (180..300).random(),
                                            isOffline = true
                                        )
                                    } else null
                                }
                            }
                        } catch (e: Exception) {
                            // Fallback
                        }
                    }

                    if (searchTracks.isEmpty()) {
                        try {
                            val piped = com.example.api.PipedApi.create()
                            val response = piped.searchMusic(query = rawInput)
                            val results = response.items ?: emptyList()
                            if (results.isNotEmpty()) {
                                searchTracks = results.mapNotNull { item ->
                                    var vId = item.url?.replace("/watch?v=", "") ?: ""
                                    if (vId.contains("&")) vId = vId.substringBefore("&")
                                    if (vId.isNotBlank()) {
                                        YouTubeTrack(
                                            youtubeId = vId,
                                            title = item.title ?: rawInput,
                                            artist = item.uploaderName ?: "YouTube Music",
                                            durationSecs = item.duration ?: (180..300).random(),
                                            isOffline = true
                                        )
                                    } else null
                                }
                            }
                        } catch (e: Exception) {
                            // Fallback
                        }
                    }

                    if (searchTracks.isNotEmpty()) {
                        youtubePlaylistTracks.value = searchTracks
                        youtubePlaylistName.value = "Zoekresultaten: '$rawInput'"
                        youtubeLastSyncedTime.value = "Vandaag, $currentLocaleTime"
                        youtubeImportMessage.value = "YouTube doorzocht: ${searchTracks.size} resultaten"
                        persistYoutubeTracksToRoom(searchTracks, "Zoekresultaten: '$rawInput'", "Vandaag, $currentLocaleTime")
                    } else {
                        loadFallbackPlaylist(rawInput)
                    }
                } else {
                    loadFallbackPlaylist(rawInput)
                }
            } catch (e: Exception) {
                youtubeImportMessage.value = "Fout bij synchroniseren: ${e.localizedMessage}"
                loadFallbackPlaylist(rawInput)
            } finally {
                isYoutubeImporting.value = false
            }
        }
    }

    fun persistYoutubeTracksToRoom(tracks: List<YouTubeTrack>, playlistName: String, lastSynced: String) {
        viewModelScope.launch {
            val entities = tracks.mapIndexed { index, track ->
                com.example.data.CachedTrackEntity(
                    id = "yt_${track.youtubeId}_$index",
                    title = track.title,
                    artist = track.artist,
                    durationSecs = track.durationSecs,
                    isYoutube = true,
                    youtubeId = track.youtubeId,
                    orderIndex = index,
                    playlistSource = "YOUTUBE"
                )
            }
            repository.saveCachedYoutubeTracks(entities)
            updateSettings { current ->
                current.copy(
                    cachedYoutubePlaylistName = playlistName,
                    cachedYoutubeLastSyncedTime = lastSynced
                )
            }
        }
    }

    private fun loadFallbackPlaylist(url: String) {
        val currentLocaleTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val syncedTimeStr = "Vandaag, $currentLocaleTime (💾 Offline Modus)"
        youtubeLastSyncedTime.value = syncedTimeStr
        
        val tracksToLoad: List<YouTubeTrack>
        val pName: String
        val msg: String

        if (url.contains("PL_CHILL_LOFI_BEATS") || url.lowercase().contains("lofi") || url.lowercase().contains("chill")) {
            pName = "Chill Lofi Cafe ☕"
            tracksToLoad = listOf(
                YouTubeTrack("5qap5aO4i9A", "Lofi Hip Hop Radio - Beats to Relax/Study", "Lofi Girl", 240, true),
                YouTubeTrack("DWcJFNfaw9c", "Coffee Shop Ambient Music", "Lofi Records", 195, false),
                YouTubeTrack("7NOSDKb0HgM", "Rainy Night in Tokyo", "Jazz Hop Café", 320, true),
                YouTubeTrack("j81SDFJpGfg", "Midnight Study Session", "Chillhop Music", 185, false),
                YouTubeTrack("tntOCGkgt98", "Cozy Fireside Beats", "Ambient Rhythms", 215, true)
            )
            msg = "Lofi Playlist geladen (Offline gecachet)"
        } else if (url.contains("PL_WORKOUT_ENERGY_BEATS") || url.lowercase().contains("workout") || url.lowercase().contains("gym")) {
            pName = "High-Energy Workout 🏃‍♂️"
            tracksToLoad = listOf(
                YouTubeTrack("hHW1oY26kxQ", "Gym Beats Motivational Mix", "Workout Club", 180, true),
                YouTubeTrack("9bZkp7q19f0", "PSY - GANGNAM STYLE", "Official PSY", 252, true),
                YouTubeTrack("kJQP7kiw5Fk", "Luis Fonsi - Despacito ft. Daddy Yankee", "Luis Fonsi", 280, false),
                YouTubeTrack("L_jWHffIx5E", "Nirvana - Smells Like Teen Spirit", "Nirvana", 301, true),
                YouTubeTrack("dQw4w9WgXcQ", "Rick Astley - Never Gonna Give You Up", "Rick Astley", 212, false)
            )
            msg = "Workout Playlist geladen (Offline gecachet)"
        } else {
            pName = "Stefan's Super Mix 🔥"
            tracksToLoad = listOf(
                YouTubeTrack("dQw4w9WgXcQ", "Never Gonna Give You Up", "Rick Astley", 212, true),
                YouTubeTrack("L_jWHffIx5E", "Smells Like Teen Spirit", "Nirvana", 301, false),
                YouTubeTrack("9bZkp7q19f0", "PSY - GANGNAM STYLE", "Official PSY", 252, true),
                YouTubeTrack("kJQP7kiw5Fk", "Despacito ft. Daddy Yankee", "Luis Fonsi", 280, true)
            )
            msg = "Custom Playlist geladen (Offline gecachet)"
        }

        youtubePlaylistName.value = pName
        youtubePlaylistTracks.value = tracksToLoad
        youtubeImportMessage.value = msg
        persistYoutubeTracksToRoom(tracksToLoad, pName, syncedTimeStr)
    }

    fun playYoutubeTrack(index: Int) {
        isYoutubeActive.value = true
        currentTrackIndex.value = index
        mediaIsPlaying.value = true
        syncActiveTrackMetadata()
    }

    fun setSleepTimerAction(action: String) {
        sleepTimerAction.value = action
    }

    fun stopSleepTimer() {
        sleepTimerRunning.value = false
        sleepTimerRemainingSec.value = sleepTimerTotalMin.value * 60
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerTotalMin.value = minutes
        sleepTimerRemainingSec.value = minutes * 60
        sleepTimerRunning.value = true
    }

    fun addMultipointDevice(device: String = "") {
        if (device.isBlank()) return
        updateSettings { current ->
            val list = if (current.multipointDevices.isEmpty()) {
                emptyList()
            } else {
                current.multipointDevices.split(",").map { it.trim() }
            }
            if (!list.contains(device)) {
                val newList = list + device
                current.copy(multipointDevices = newList.joinToString(","))
            } else {
                current
            }
        }
    }

    fun removeMultipointDevice(device: String = "") {
        if (device.isBlank()) return
        updateSettings { current ->
            val list = current.multipointDevices.split(",").map { it.trim() }
            val newList = list.filter { it != device }
            current.copy(multipointDevices = newList.joinToString(","))
        }
    }

    fun updateBatteryLevel(level: Int) {
        val coerced = level.coerceIn(0, 100)
        updateSettings { current ->
            current.copy(batteryLevel = coerced)
        }
    }

    fun toggleMultipoint(enabled: Boolean) {
        updateSettings { current ->
            current.copy(multipointEnabled = enabled)
        }
    }

    fun toggleAutoPowerOff(enabled: Boolean) {
        updateSettings { current ->
            current.copy(autoPowerOffEnabled = enabled)
        }
    }

    fun setAutoPowerOffMinutes(minutes: Int) {
        updateSettings { current ->
            current.copy(autoPowerOffMinutes = minutes)
        }
        autoOffRemainingSeconds.value = minutes * 60
    }

    fun toggleWearingState(state: Boolean = false) {
        isWearingHeadphones.value = state
        if (state) {
            autoOffRemainingSeconds.value = _settingsState.value.autoPowerOffMinutes * 60
        }
    }

    fun toggleCharging(charging: Boolean) {
        isCharging.value = charging
        updateSettings { it }
        if (charging) {
            viewModelScope.launch {
                while (isCharging.value) {
                    delay(3000)
                    val currentLvl = _settingsState.value.batteryLevel
                    if (currentLvl < 100) {
                        updateBatteryLevel(currentLvl + 1)
                    }
                }
            }
        }
    }

    fun toggleIntelligentBatteryPreservation(enabled: Boolean) {
        updateSettings { current ->
            current.copy(intelligentBatteryPreservationEnabled = enabled)
        }
    }

    fun setIntelligentBatteryThreshold(threshold: Int) {
        updateSettings { current ->
            current.copy(intelligentBatteryThreshold = threshold.coerceIn(5, 50))
        }
    }

    fun setBatteryPreservationAncCap(cap: Int) {
        updateSettings { current ->
            current.copy(batteryPreservationAncCap = cap.coerceIn(0, 3))
        }
    }

    fun fastForwardAutoOff() {
        if (autoOffRemainingSeconds.value > 10) {
            autoOffRemainingSeconds.value = 10
        }
    }

    fun selectHeadphoneProfile(deviceName: String, customCategory: String? = null) {
        val name = deviceName.ifBlank { "Philips TAH6519" }
        val lower = name.lowercase()

        val category = customCategory ?: when {
            lower.contains("earbud") || lower.contains("buds") || lower.contains("airpods pro") || lower.contains("wf-") || lower.contains("in-ear") -> "In-Ear / Earbuds"
            lower.contains("on-ear") || lower.contains("tune") || lower.contains("major") -> "On-Ear Wireless"
            lower.contains("wired") || lower.contains("3.5mm") || lower.contains("jack") || lower.contains("studio") || lower.contains("line_in") -> "Wired 3.5mm / USB-C"
            lower.contains("gaming") || lower.contains("headset") || lower.contains("barracuda") -> "Gaming Headset"
            else -> "Over-Ear Wireless"
        }

        val driverSize = when {
            category.contains("In-Ear") -> 11
            category.contains("On-Ear") -> 32
            category.contains("Wired") -> 45
            lower.contains("sony wh") -> 30
            lower.contains("sennheiser") -> 42
            else -> 40
        }

        val maxHours = when {
            category.contains("In-Ear") -> 28
            category.contains("Wired") -> 999
            lower.contains("sennheiser") -> 60
            lower.contains("philips") || lower.contains("tah") -> 80
            lower.contains("jbl") -> 65
            else -> 40
        }

        val ancHours = when {
            category.contains("In-Ear") -> 18
            category.contains("Wired") -> 999
            lower.contains("sennheiser") -> 60
            lower.contains("philips") || lower.contains("tah") -> 40
            else -> 30
        }

        val codec = when {
            lower.contains("sony") || lower.contains("ldac") || lower.contains("philips") || lower.contains("soundcore") -> "LDAC"
            lower.contains("apple") || lower.contains("airpods") -> "AAC / Spatial"
            lower.contains("sennheiser") || lower.contains("aptx") || lower.contains("bose") -> "aptX HD"
            category.contains("Wired") -> "Hi-Res PCM 24-bit/96kHz"
            else -> "AAC"
        }

        val connType = when {
            category.contains("Wired") -> "USB-C / 3.5mm Direct Line"
            lower.contains("5.4") -> "Bluetooth 5.4"
            else -> "Bluetooth 5.3"
        }

        activeAudioCodec.value = codec
        activeProtocolInfo.value = connType

        updateSettings { current ->
            current.copy(
                connectedDeviceName = name,
                headphoneCategory = category,
                driverSizeMm = driverSize,
                maxPlaytimeHours = maxHours,
                ancPlaytimeHours = ancHours,
                connectionType = connType,
                activeCodec = codec
            )
        }
    }

    fun playBluetoothConnectChime() {
        viewModelScope.launch {
            com.example.util.BluetoothChimeSynthesizer.playConnectChime()
        }
    }

    fun playBluetoothDisconnectChime() {
        viewModelScope.launch {
            com.example.util.BluetoothChimeSynthesizer.playDisconnectChime()
        }
    }

    fun connectDevice(device: String = "") {
        viewModelScope.launch {
            isConnecting.value = true
            isAutoReconnecting.value = false
            gattStatusMessage.value = "Koppelen via Bluetooth..."
            delay(1000)
            val devName = if (device.isNotBlank()) device else _settingsState.value.connectedDeviceName
            selectHeadphoneProfile(devName)
            updateSettings { current ->
                current.copy(connected = true)
            }
            gattStatusMessage.value = "Connected"
            isConnecting.value = false
            connectionSuccessEvent.value = devName
            com.example.util.BluetoothChimeSynthesizer.playConnectChime()
        }
    }

    fun connectCompatibleDevice(device: CompatibleBluetoothDevice) {
        viewModelScope.launch {
            isConnecting.value = true
            isAutoReconnecting.value = false
            gattStatusMessage.value = "Koppelen met ${device.name}..."
            delay(800)
            selectHeadphoneProfile(device.name, device.category)
            
            // Sync with scanned devices list
            val currentScanned = scannedDevices.value.toMutableList()
            if (currentScanned.none { it.name == device.name }) {
                currentScanned.add(0, ScannedDevice(device.name, device.address, device.rssi, true))
                scannedDevices.value = currentScanned
            }
            
            // Add to multipoint list
            addMultipointDevice(device.name)
            
            updateSettings { current ->
                current.copy(
                    connected = true,
                    connectedDeviceName = device.name,
                    headphoneCategory = device.category,
                    driverSizeMm = device.driverSizeMm,
                    maxPlaytimeHours = device.maxPlaytimeHours,
                    ancPlaytimeHours = device.ancPlaytimeHours,
                    connectionType = device.bluetoothVersion,
                    activeCodec = device.supportedCodecs.firstOrNull() ?: "AAC"
                )
            }
            gattStatusMessage.value = "Verbonden (${device.name})"
            isConnecting.value = false
            connectionSuccessEvent.value = device.name
            try {
                com.example.util.BluetoothChimeSynthesizer.playConnectChime()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun disconnectDevice(device: String = "") {
        connectionSuccessEvent.value = null
        updateSettings { current ->
            current.copy(connected = false)
        }
        gattStatusMessage.value = "Disconnected"
        isConnecting.value = false
        isAutoReconnecting.value = false
        viewModelScope.launch {
            com.example.util.BluetoothChimeSynthesizer.playDisconnectChime()
        }
    }

    fun dismissConnectionSuccessEvent() {
        connectionSuccessEvent.value = null
    }

    fun triggerConnectionSuccessAnimation(name: String = "") {
        connectionSuccessEvent.value = if (name.isNotBlank()) name else _settingsState.value.connectedDeviceName
    }

    fun simulateConnectionLoss() {
        viewModelScope.launch {
            updateSettings { current ->
                current.copy(connected = false)
            }
            gattStatusMessage.value = "Disconnected (Signaal verloren)"
            isAutoReconnecting.value = true
            reconnectAttempts.value = 1
            viewModelScope.launch {
                com.example.util.BluetoothChimeSynthesizer.playDisconnectChime()
            }
            delay(2000)
            if (!isAutoReconnecting.value) return@launch
            reconnectAttempts.value = 2
            delay(2000)
            if (!isAutoReconnecting.value) return@launch
            reconnectAttempts.value = 3
            delay(2000)
            if (!isAutoReconnecting.value) return@launch
            isAutoReconnecting.value = false
            reconnectAttempts.value = 0
            updateSettings { current ->
                current.copy(connected = true)
            }
            gattStatusMessage.value = "Connected"
            com.example.util.BluetoothChimeSynthesizer.playConnectChime()
        }
    }

    fun setAutoReconnectOnLaunch(enabled: Boolean) {
        autoReconnectEnabled.value = enabled
        updateSettings { current ->
            current.copy(autoReconnectOnLaunch = enabled)
        }
    }

    fun triggerLaunchAutoReconnect() {
        if (isAutoReconnecting.value || isConnecting.value) return
        viewModelScope.launch {
            isAutoReconnecting.value = true
            val targetDevice = _settingsState.value.lastPairedDeviceName.ifBlank { "Philips TAH6519" }
            val targetAddress = _settingsState.value.lastPairedDeviceAddress.ifBlank { "00:11:22:33:44:55" }

            gattStatusMessage.value = "Poging 1/3: Zoeken naar $targetDevice..."
            reconnectAttempts.value = 1
            delay(1200)
            if (!isAutoReconnecting.value) return@launch

            gattStatusMessage.value = "Poging 2/3: Bluetooth 5.3 Multipoint verbinding maken..."
            reconnectAttempts.value = 2
            delay(1200)
            if (!isAutoReconnecting.value) return@launch

            gattStatusMessage.value = "Poging 3/3: LDAC Audio & DSP Profiel laden..."
            reconnectAttempts.value = 3
            delay(1000)
            if (!isAutoReconnecting.value) return@launch

            isAutoReconnecting.value = false
            reconnectAttempts.value = 0

            selectHeadphoneProfile(targetDevice)
            updateSettings { current ->
                current.copy(
                    connected = true,
                    connectedDeviceName = targetDevice,
                    lastPairedDeviceName = targetDevice,
                    lastPairedDeviceAddress = targetAddress
                )
            }
            gattStatusMessage.value = "Connected ($targetDevice)"
            try {
                com.example.util.BluetoothChimeSynthesizer.playConnectChime()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleSimulationMode(enabled: Boolean) {
        isSimulationMode.value = enabled
        if (enabled) {
            updateSettings { current ->
                current.copy(connected = true)
            }
        }
    }

    fun toggleFirmwarePolling(enabled: Boolean) {
        isFirmwarePolling.value = enabled
    }

    fun pollFirmwareApi(manual: Boolean = false) {
        viewModelScope.launch {
            if (isApiPollingInProgress.value) return@launch
            isApiPollingInProgress.value = true
            if (manual) {
                _updateState.value = UpdateState.Checking
            }
            
            // Simulate API HTTP request delay
            delay(1200)
            
            val currentTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            lastFirmwarePollTime.value = currentTime
            simulatedApiHttpStatus.value = 200
            isApiPollingInProgress.value = false

            if (firmwareVersion.value != "v1.5.0") {
                _updateState.value = UpdateState.UpdateAvailable(
                    version = "v1.5.0",
                    changelog = listOf(
                        "Verbeterde stabiliteit van de hybride ANC-algoritmen.",
                        "Hogere audiokwaliteit en lagere latency bij Bluetooth Multipoint.",
                        "Nieuwe energiebesparende modus voor de TAH6519.",
                        "Sneller schakelen tussen 5-Band en 10-Band EQ-modi."
                    )
                )
            } else {
                _updateState.value = UpdateState.UpToDate
            }
        }
    }

    fun checkForUpdates() {
        pollFirmwareApi(manual = true)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            ThemeState.themeMode = mode
            currentThemeMode.value = mode
            themePreferencesRepository.saveThemeMode(mode)
        }
    }

    fun setActiveAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            ThemeState.activeTheme = theme
            currentAppTheme.value = theme
            themePreferencesRepository.saveActiveAppTheme(theme)
        }
    }

    fun setCustomAccentHex(hex: String) {
        viewModelScope.launch {
            try {
                val colorInt = android.graphics.Color.parseColor(hex)
                ThemeState.customAccentColor = androidx.compose.ui.graphics.Color(colorInt)
            } catch (e: Exception) {
                ThemeState.customAccentColor = androidx.compose.ui.graphics.Color(0xFF00E5FF)
            }
            customAccentHex.value = hex
            themePreferencesRepository.saveCustomAccentHex(hex)
        }
    }

    fun startUpdate() {
        viewModelScope.launch {
            _updateState.value = UpdateState.Updating(0f, "Firmware downloaden...")
            delay(1000)
            _updateState.value = UpdateState.Updating(25f, "Firmware verifiëren...")
            delay(1000)
            _updateState.value = UpdateState.Updating(50f, "Flashen naar TAH6519...")
            delay(1500)
            _updateState.value = UpdateState.Updating(85f, "Rebooten van de headset...")
            delay(1000)
            firmwareVersion.value = "v1.5.0"
            _updateState.value = UpdateState.UpdateComplete("v1.5.0")
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }

    fun resetAll() {
        viewModelScope.launch {
            repository.resetSettings()
            equalizerPreferencesRepository.resetEqualizerPreferences()
            firmwareVersion.value = "v1.4.2"
            _updateState.value = UpdateState.Idle
            _settingsState.value = HeadphoneSettings()
        }
    }

    fun setAncLevel(level: Int) {
        updateSettings { current ->
            current.copy(ancLevel = level)
        }
    }

    fun setTransparencyIntensity(level: Int) {
        val coerced = level.coerceIn(1, 5)
        updateSettings { current ->
            current.copy(transparencyIntensity = coerced)
        }
    }

    fun setAncMode(mode: String) {
        updateSettings { current ->
            current.copy(ancMode = mode)
        }
    }

    fun toggleAnc(enabled: Boolean) {
        updateSettings { current ->
            current.copy(ancEnabled = enabled)
        }
    }

    fun toggleDynamicBass(enabled: Boolean) {
        updateSettings { current ->
            current.copy(dynamicBassEnabled = enabled)
        }
    }

    fun toggleSurround(enabled: Boolean) {
        updateSettings { current ->
            current.copy(surroundSoundEnabled = enabled)
        }
    }

    fun toggleLdac(enabled: Boolean) {
        updateSettings { current ->
            current.copy(ldacEnabled = enabled)
        }
    }

    fun setSpatialAudioMode(mode: String) {
        updateSettings { current ->
            current.copy(spatialAudioMode = mode)
        }
    }

    fun setDynamicBassLevel(level: Int) {
        updateSettings { current ->
            current.copy(dynamicBassLevel = level, dynamicBassEnabled = level > 0)
        }
    }

    fun setLdacQualityMode(mode: String) {
        updateSettings { current ->
            current.copy(ldacQualityMode = mode)
        }
    }

    fun toggleAncCompensation(enabled: Boolean) {
        updateSettings { current ->
            current.copy(ancCompensationEnabled = enabled)
        }
    }

    fun setPreset(preset: String) {
        val bands = presets[preset] ?: _settingsState.value.getCustomPresetsMap()[preset]
        if (bands != null) {
            updateSettings { current ->
                current.copyWithBands(bands).copy(activePreset = preset)
            }
        }
    }

    fun saveCustomPreset(name: String, bands: List<Float>) {
        if (bands.size < 10) return
        updateSettings { current ->
            val currentMap = current.getCustomPresetsMap().toMutableMap()
            currentMap[name] = bands
            val serialized = currentMap.map { (key, value) ->
                "$key:${value.joinToString(",")}"
            }.joinToString("|")
            current.copyWithBands(bands).copy(
                customPresets = serialized,
                activePreset = name
            )
        }
    }

    fun deleteCustomPreset(name: String) {
        updateSettings { current ->
            val currentMap = current.getCustomPresetsMap().toMutableMap()
            currentMap.remove(name)
            val serialized = currentMap.map { (key, value) ->
                "$key:${value.joinToString(",")}"
            }.joinToString("|")
            var nextActivePreset = current.activePreset
            var nextBands = current.getBands()
            if (nextActivePreset == name) {
                nextActivePreset = "Flat"
                nextBands = presets["Flat"] ?: listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
            }
            current.copyWithBands(nextBands).copy(
                customPresets = serialized,
                activePreset = nextActivePreset
            )
        }
    }

    fun updateBand(index: Int, value: Float) {
        updateSettings { current ->
            val bands = current.getBands().toMutableList()
            if (index in bands.indices) {
                bands[index] = value
            }
            current.copyWithBands(bands).copy(activePreset = "Aangepast")
        }
    }

    fun updateMasterGain(gain: Float) {
        updateSettings { current ->
            current.copy(masterGain = gain)
        }
    }

    fun setSoundZone(zone: String) {
        updateSettings { current ->
            current.copy(activeSoundZone = zone, soundZonesEnabled = zone != "Uit")
        }
    }

    fun toggleSoundZones(enabled: Boolean) {
        updateSettings { current ->
            current.copy(soundZonesEnabled = enabled)
        }
    }

    fun setSimulatedActivity(activity: String) {
        updateSettings { current ->
            current.copy(activeActivity = activity)
        }
    }

    fun toggleAdaptiveActivity(enabled: Boolean) {
        updateSettings { current ->
            current.copy(adaptiveActivityEnabled = enabled)
        }
    }

    fun toggleSidetone(enabled: Boolean) {
        updateSettings { current ->
            current.copy(sidetoneEnabled = enabled)
        }
    }

    fun setSidetoneLevel(level: Int) {
        updateSettings { current ->
            current.copy(sidetoneLevel = level)
        }
    }

    fun toggleWearingDetection(enabled: Boolean) {
        updateSettings { current ->
            current.copy(wearingDetectionEnabled = enabled)
        }
    }

    fun toggleWindNoiseReduction(enabled: Boolean) {
        updateSettings { current ->
            current.copy(windNoiseReductionEnabled = enabled)
        }
    }

    fun toggleSpeakToAwareness(enabled: Boolean) {
        updateSettings { current ->
            current.copy(speakToAwarenessEnabled = enabled)
        }
    }

    fun setSpeakToAwarenessSensitivity(sensitivity: Int) {
        updateSettings { current ->
            current.copy(speakToAwarenessSensitivity = sensitivity)
        }
    }

    fun setAmbientEnvironmentDb(db: Int) {
        updateSettings { current ->
            current.copy(ambientEnvironmentDb = db)
        }
    }

    fun toggleAutoWindShielding(enabled: Boolean) {
        updateSettings { current ->
            current.copy(autoWindShieldingEnabled = enabled)
        }
    }

    fun toggleTouchControls(enabled: Boolean) {
        updateSettings { current ->
            current.copy(touchControlsEnabled = enabled)
        }
    }

    fun setTouchSingleTapAction(action: String) {
        updateSettings { current ->
            current.copy(touchSingleTapAction = action)
        }
    }

    fun setTouchDoubleTapAction(action: String) {
        updateSettings { current ->
            current.copy(touchDoubleTapAction = action)
        }
    }

    fun setTouchHoldAction(action: String) {
        updateSettings { current ->
            current.copy(touchHoldAction = action)
        }
    }

    fun toggleBatteryHealth(enabled: Boolean) {
        updateSettings { current ->
            current.copy(batteryHealthEnabled = enabled)
        }
    }

    fun startBluetoothScan() {
        bleManager.startScan(filterTah6519Only = false)
    }

    fun stopBluetoothScan() {
        bleManager.stopScan()
    }

    fun connectToBleDevice(address: String) {
        bleManager.connectToDevice(address)
    }

    fun toggleMediaPlayer() {
        val nextPlayingState = !mediaIsPlaying.value
        mediaIsPlaying.value = nextPlayingState
        if (!isYoutubeActive.value) {
            if (nextPlayingState) {
                exoPlayerController.play()
            } else {
                exoPlayerController.pause()
            }
        }
    }
    fun seekMedia(progress: Float) {

        val targetSecs = (progress * mediaDuration.value).toInt()
        mediaProgress.value = targetSecs
        exoPlayerController.seekTo(targetSecs * 1000L)
    }

    fun seekMedia(progress: Int) {
        val targetSecs = progress.coerceIn(0, mediaDuration.value)
        mediaProgress.value = targetSecs
        exoPlayerController.seekTo(targetSecs * 1000L)
    }

    fun fetchBatteryLevel() {
        viewModelScope.launch {
            isFetchingBattery.value = true
            batteryFetchStatus.value = "Lezen via Bluetooth GATT service..."
            bleManager.readBatteryLevel()
            for (i in 1..10) {
                delay(100)
                batteryFetchProgress.value = i / 10f
                if (i == 5) {
                    batteryFetchStatus.value = "Batterijpercentage verifiëren..."
                }
            }
            val readLevel = bleManager.batteryLevel.value ?: _settingsState.value.batteryLevel
            val refreshedLvl = if (readLevel == 0) 88 else readLevel
            updateBatteryLevel(refreshedLvl)
            batteryFetchStatus.value = bleManager.statusMessage.value
            isFetchingBattery.value = false
            try {
                com.example.util.BluetoothChimeSynthesizer.playConnectChime(0.5f)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun readFirmwareViaGatt() {
        viewModelScope.launch {
            isGattReading.value = true
            gattStatusMessage.value = "Firmware-versie opvragen via BLE..."
            bleManager.readFirmwareVersion()
            delay(800)
            val readFw = bleManager.firmwareVersion.value ?: "v2.1.4-TAH6519"
            firmwareVersion.value = readFw
            gattStatusMessage.value = bleManager.statusMessage.value
            isGattReading.value = false
        }
    }

    fun toggleFindMySignal(enable: Boolean? = null) {
        val newState = enable ?: !isFindMyBeeping.value
        isFindMyBeeping.value = newState
        if (newState) {
            findMySignalStatus.value = "Signaal actief: TAH6519 piept..."
            gattStatusMessage.value = "Find My signal: Sending acoustic beacon to TAH6519..."
        } else {
            findMySignalStatus.value = "Gereed voor opsporen"
            gattStatusMessage.value = "Find My signal stopped"
        }
    }

    fun setAudioProfilesOnboardingSeen(seen: Boolean) {
        updateSettings { current ->
            current.copy(hasSeenAudioProfilesOnboarding = seen)
        }
    }

    fun startNoiseMonitoring() {
        isRecordingNoise.value = true
        viewModelScope.launch {
            while (isRecordingNoise.value) {
                delay(1000)
                ambientDecibel.value = (35..95).random()
            }
        }
    }

    fun stopNoiseMonitoring() {
        isRecordingNoise.value = false
    }

    fun playProceduralTone(freq: Int = 440, dur: Int = 1000) {
        viewModelScope.launch {
            try {
                com.example.util.BluetoothChimeSynthesizer.playTone(freq, dur)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun renameCustomPreset(old: String, new: String) {
        if (new.isBlank() || old == new) return
        updateSettings { current ->
            val currentMap = current.getCustomPresetsMap().toMutableMap()
            val bandValues = currentMap.remove(old) ?: current.getBands()
            currentMap[new] = bandValues
            val serialized = currentMap.map { (key, value) ->
                "$key:${value.joinToString(",")}"
            }.joinToString("|")
            val nextActive = if (current.activePreset == old) new else current.activePreset
            current.copy(
                customPresets = serialized,
                activePreset = nextActive
            )
        }
    }

    fun setSimulatedDistance(dist: Float) {
        simulatedDistanceMeters.value = dist
    }

    fun toggleAutoReconnect(enabled: Boolean) {
        autoReconnectEnabled.value = enabled
        updateSettings { it.copy(autoReconnectOnLaunch = enabled) }
    }

    fun updateMultipointDevices(devices: String) {
        updateSettings { it.copy(multipointDevices = devices) }
    }

    val ancEnabled = kotlinx.coroutines.flow.MutableStateFlow(false)
    val masterGain = kotlinx.coroutines.flow.MutableStateFlow(0f)
    val ancLevel = kotlinx.coroutines.flow.MutableStateFlow(2)
    val sidetoneEnabled = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isHeadphone = true
    val address = "00:11:22:33:44:55"
    val name = "Headphones"
    val statusMessage = kotlinx.coroutines.flow.MutableStateFlow("")
    val newVersion = kotlinx.coroutines.flow.MutableStateFlow("1.0.1")
}

class HeadphoneViewModelFactory(
    private val application: Application,
    private val repository: HeadphoneRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HeadphoneViewModel(application, repository) as T
    }
}
