package com.example.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.YouTubeApi
import com.example.data.HeadphoneRepository
import com.example.data.HeadphoneSettings
import kotlinx.coroutines.delay
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
    val isOffline: Boolean
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

class HeadphoneViewModel(application: Application, private val repository: HeadphoneRepository) : ViewModel() {
    private val _settingsState = MutableStateFlow(HeadphoneSettings())
    val settingsState: StateFlow<HeadphoneSettings> = _settingsState.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    val firmwareVersion = MutableStateFlow("v1.4.2")
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
    val isFetchingBattery = MutableStateFlow(false)
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
        AppTrack("Philips Signature Sound", "Philips Studio", true),
        AppTrack("Spatial Audio Demo", "Dolby Atmos", false),
        AppTrack("Focus White Noise", "Ambient Master", true),
        AppTrack("Deep Bass Test", "Bass Shaker", false)
    )

    val youtubePlaylistTracks = MutableStateFlow(listOf(
        YouTubeTrack("dQw4w9WgXcQ", "Never Gonna Give You Up", "Rick Astley", 212, true),
        YouTubeTrack("L_jWHffIx5E", "Smells Like Teen Spirit", "Nirvana", 301, false),
        YouTubeTrack("9bZkp7q19f0", "PSY - GANGNAM STYLE", "Official PSY", 252, true)
    ))

    val scannedDevices = MutableStateFlow(listOf(
        ScannedDevice("Philips TAH6519", "00:11:22:33:44:55", -45, true),
        ScannedDevice("Stefan's Pixel 8", "AA:BB:CC:DD:EE:FF", -78, false),
        ScannedDevice("Office Smart TV", "11:22:33:44:55:66", -92, false),
        ScannedDevice("Unknown BLE Beacon", "FE:DC:BA:98:76:54", -88, false)
    ))

    val defaultPresets = mapOf(
        "Flat" to listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        "Balanced" to listOf(1f, 1f, 0.5f, 0f, 0f, 0.5f, 1f, 1.5f, 1.5f, 1f),
        "Bass Boost" to listOf(8f, 6f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f),
        "Voice Clarity" to listOf(-2f, -1f, 1f, 3f, 5f, 5f, 4f, 2f, -1f, -2f),
        "Treble Boost" to listOf(0f, 0f, 0f, 0f, 1f, 2f, 4f, 6f, 8f, 6f),
        "Vocal Boost" to listOf(-2f, -1f, 1f, 3f, 4f, 4f, 3f, 1f, -1f, -2f),
        "Pop" to listOf(3.0f, 2.0f, 0.5f, -1.0f, -1.5f, -1.0f, 0.5f, 1.5f, 2.5f, 3.0f),
        "Philips Signature" to listOf(3f, 2f, 1f, 0f, -1f, 0f, 1f, 2f, 3f, 2f)
    )
    val presets: Map<String, List<Float>> = defaultPresets

    init {
        viewModelScope.launch {
            repository.settingsFlow.collect { settings ->
                _settingsState.value = settings
            }
        }
        
        // Background loop for media progress, sleep timer, and auto off timer
        viewModelScope.launch {
            while (true) {
                delay(1000)
                
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
                    } else {
                        playNextTrack()
                    }
                }
                
                // Auto Off Timer
                val currentSettings = _settingsState.value
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

    private fun updateSettings(update: (HeadphoneSettings) -> HeadphoneSettings) {
        val updated = update(_settingsState.value)
        _settingsState.value = updated
        viewModelScope.launch {
            repository.updateSettings(updated)
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
            }
        } else {
            val idx = currentTrackIndex.value
            if (idx in playlist.indices) {
                val track = playlist[idx]
                mediaTrackName.value = track.title
                mediaTrackArtist.value = track.artist
                mediaDuration.value = 240 // Default duration for local demo
                mediaProgress.value = 0
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
        val finalUrl = if (url.isBlank()) lastYoutubePlaylistUrl.value else url
        viewModelScope.launch {
            isYoutubeImporting.value = true
            youtubeImportMessage.value = "Bezig met verbinding maken..."
            lastYoutubePlaylistUrl.value = finalUrl
            
            // Extract playlist ID
            var listId = ""
            if (finalUrl.contains("list=")) {
                val idx = finalUrl.indexOf("list=")
                val start = idx + 5
                var end = finalUrl.indexOf("&", start)
                if (end == -1) end = finalUrl.length
                listId = finalUrl.substring(start, end)
            } else if (finalUrl.isNotBlank()) {
                listId = finalUrl.trim()
            }
            
            val apiKey = try {
                BuildConfig.YOUTUBE_API_KEY
            } catch (e: Exception) {
                ""
            }
            
            if (apiKey.isNotBlank() && apiKey != "MY_YOUTUBE_API_KEY" && listId.isNotBlank()) {
                try {
                    youtubeImportMessage.value = "Gegevens ophalen via YouTube API..."
                    val api = YouTubeApi.create()
                    val response = api.getPlaylistItems(playlistId = listId, apiKey = apiKey)
                    val items = response.items
                    if (items != null && items.isNotEmpty()) {
                        val tracks = items.map { item ->
                            val snippet = item.snippet
                            val vId = snippet?.resourceId?.videoId ?: ""
                            val title = snippet?.title ?: "Onbekend Nummer"
                            val artist = snippet?.videoOwnerChannelTitle ?: "YouTube Artiest"
                            YouTubeTrack(
                                youtubeId = vId,
                                title = title,
                                artist = artist,
                                durationSecs = (180..300).random(), // snippet has no duration, pick random realistic
                                isOffline = (0..1).random() == 1
                            )
                        }
                        youtubePlaylistTracks.value = tracks
                        youtubePlaylistName.value = "Geïmporteerde Playlist ($listId)"
                        val currentLocaleTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                        youtubeLastSyncedTime.value = "Vandaag, $currentLocaleTime"
                        youtubeImportMessage.value = "Succesvol geïmporteerd!"
                    } else {
                        youtubeImportMessage.value = "Geen nummers gevonden in deze playlist."
                    }
                } catch (e: Exception) {
                    youtubeImportMessage.value = "Fout bij ophalen: ${e.localizedMessage ?: "Verbindingsfout"}"
                    loadFallbackPlaylist(finalUrl)
                }
            } else {
                loadFallbackPlaylist(finalUrl)
            }
            isYoutubeImporting.value = false
        }
    }

    private fun loadFallbackPlaylist(url: String) {
        val currentLocaleTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        youtubeLastSyncedTime.value = "Vandaag, $currentLocaleTime"
        
        if (url.contains("PL_CHILL_LOFI_BEATS") || url.lowercase().contains("lofi") || url.lowercase().contains("chill")) {
            youtubePlaylistName.value = "Chill Lofi Cafe ☕"
            youtubePlaylistTracks.value = listOf(
                YouTubeTrack("5qap5aO4i9A", "Lofi Hip Hop Radio - Beats to Relax/Study", "Lofi Girl", 240, true),
                YouTubeTrack("DWcJFNfaw9c", "Coffee Shop Ambient Music", "Lofi Records", 195, false),
                YouTubeTrack("7NOSDKb0HgM", "Rainy Night in Tokyo", "Jazz Hop Café", 320, true),
                YouTubeTrack("j81SDFJpGfg", "Midnight Study Session", "Chillhop Music", 185, false),
                YouTubeTrack("tntOCGkgt98", "Cozy Fireside Beats", "Ambient Rhythms", 215, true)
            )
            youtubeImportMessage.value = "Lofi Playlist geladen (Demo modus)"
        } else if (url.contains("PL_WORKOUT_ENERGY_BEATS") || url.lowercase().contains("workout") || url.lowercase().contains("gym")) {
            youtubePlaylistName.value = "High-Energy Workout 🏃‍♂️"
            youtubePlaylistTracks.value = listOf(
                YouTubeTrack("hHW1oY26kxQ", "Gym Beats Motivational Mix", "Workout Club", 180, true),
                YouTubeTrack("9bZkp7q19f0", "PSY - GANGNAM STYLE", "Official PSY", 252, true),
                YouTubeTrack("kJQP7kiw5Fk", "Luis Fonsi - Despacito ft. Daddy Yankee", "Luis Fonsi", 280, false),
                YouTubeTrack("L_jWHffIx5E", "Nirvana - Smells Like Teen Spirit", "Nirvana", 301, true),
                YouTubeTrack("dQw4w9WgXcQ", "Rick Astley - Never Gonna Give You Up", "Rick Astley", 212, false)
            )
            youtubeImportMessage.value = "Workout Playlist geladen (Demo modus)"
        } else {
            youtubePlaylistName.value = "Stefan's Super Mix 🔥"
            youtubePlaylistTracks.value = listOf(
                YouTubeTrack("dQw4w9WgXcQ", "Never Gonna Give You Up", "Rick Astley", 212, true),
                YouTubeTrack("L_jWHffIx5E", "Smells Like Teen Spirit", "Nirvana", 301, false),
                YouTubeTrack("9bZkp7q19f0", "PSY - GANGNAM STYLE", "Official PSY", 252, true),
                YouTubeTrack("kJQP7kiw5Fk", "Despacito ft. Daddy Yankee", "Luis Fonsi", 280, true)
            )
            youtubeImportMessage.value = "Custom Playlist geladen (Demo modus)"
        }
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

    fun fastForwardAutoOff() {
        if (autoOffRemainingSeconds.value > 10) {
            autoOffRemainingSeconds.value = 10
        }
    }

    fun connectDevice(device: String = "") {
        viewModelScope.launch {
            isConnecting.value = true
            isAutoReconnecting.value = false
            gattStatusMessage.value = "Koppelen via Bluetooth..."
            delay(1000)
            updateSettings { current ->
                current.copy(connected = true)
            }
            gattStatusMessage.value = "Connected"
            isConnecting.value = false
        }
    }

    fun disconnectDevice(device: String = "") {
        updateSettings { current ->
            current.copy(connected = false)
        }
        gattStatusMessage.value = "Disconnected"
        isConnecting.value = false
        isAutoReconnecting.value = false
    }

    fun simulateConnectionLoss() {
        viewModelScope.launch {
            updateSettings { current ->
                current.copy(connected = false)
            }
            gattStatusMessage.value = "Disconnected (Signaal verloren)"
            isAutoReconnecting.value = true
            reconnectAttempts.value = 1
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

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            delay(1500)
            _updateState.value = UpdateState.UpdateAvailable(
                version = "v1.5.0",
                changelog = listOf(
                    "Verbeterde stabiliteit van de hybride ANC-algoritmen.",
                    "Hogere audiokwaliteit en lagere latency bij Bluetooth Multipoint.",
                    "Nieuwe energiebesparende modus voor de TAH6519.",
                    "Sneller schakelen tussen 5-Band en 10-Band EQ-modi."
                )
            )
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
        viewModelScope.launch {
            isScanningBluetooth.value = true
            delay(1500)
            isScanningBluetooth.value = false
        }
    }

    fun stopBluetoothScan() {
        isScanningBluetooth.value = false
    }

    fun toggleMediaPlayer() {
        mediaIsPlaying.value = !mediaIsPlaying.value
    }

    fun seekMedia(progress: Float) {
        mediaProgress.value = (progress * mediaDuration.value).toInt()
    }

    fun seekMedia(progress: Int) {
        mediaProgress.value = progress.coerceIn(0, mediaDuration.value)
    }

    fun fetchBatteryLevel() {
        viewModelScope.launch {
            isFetchingBattery.value = true
            batteryFetchStatus.value = "Lezen via GATT..."
            for (i in 1..10) {
                delay(150)
                batteryFetchProgress.value = i * 10f
            }
            batteryFetchStatus.value = "Klaar"
            isFetchingBattery.value = false
        }
    }

    fun readFirmwareViaGatt() {
        viewModelScope.launch {
            isGattReading.value = true
            gattStatusMessage.value = "Firmware-versie opvragen..."
            delay(1000)
            gattStatusMessage.value = "Versie: ${firmwareVersion.value}"
            isGattReading.value = false
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

    fun playProceduralTone(freq: Int = 440, dur: Int = 1000) {}
    fun renameCustomPreset(old: String, new: String) {}
    fun setSimulatedDistance(dist: Float) {
        simulatedDistanceMeters.value = dist
    }
    fun toggleAutoReconnect(enabled: Boolean) {}
    fun updateMultipointDevices(devices: String) {}

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
