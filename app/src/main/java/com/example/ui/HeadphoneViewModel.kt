package com.example.ui

import android.app.Application
import com.example.data.HeadphoneRepository
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.data.HeadphoneSettings

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val version: String, val changelog: List<String>) : UpdateState()
    data class Updating(val statusMessage: String, val progress: Float) : UpdateState()
    object UpToDate : UpdateState()
    data class UpdateComplete(val newVersion: String) : UpdateState()
}

data class ScannedDevice(val name: String, val address: String, val rssi: Int = 0, val isHeadphone: Boolean = false)


class HeadphoneViewModelFactory(private val application: android.app.Application, private val repository: HeadphoneRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HeadphoneViewModel(application, repository) as T
    }
}

class HeadphoneViewModel(application: Application, private val repository: HeadphoneRepository) : AndroidViewModel(application) {
    private val _settingsState = MutableStateFlow<HeadphoneSettings>(HeadphoneSettings())
    val settingsState: StateFlow<HeadphoneSettings> = _settingsState.asStateFlow()

    private val _isConnecting = MutableStateFlow<Boolean>(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _autoReconnectEnabled = MutableStateFlow<Boolean>(false)
    val autoReconnectEnabled: StateFlow<Boolean> = _autoReconnectEnabled.asStateFlow()

    private val _isAutoReconnecting = MutableStateFlow<Boolean>(false)
    val isAutoReconnecting: StateFlow<Boolean> = _isAutoReconnecting.asStateFlow()

    private val _reconnectAttempts = MutableStateFlow<Int>(0)
    val reconnectAttempts: StateFlow<Int> = _reconnectAttempts.asStateFlow()

    private val _mediaIsPlaying = MutableStateFlow<Boolean>(false)
    val mediaIsPlaying: StateFlow<Boolean> = _mediaIsPlaying.asStateFlow()

    private val _mediaProgress = MutableStateFlow<Int>(0)
    val mediaProgress: StateFlow<Int> = _mediaProgress.asStateFlow()

    private val _mediaTrackName = MutableStateFlow<String>("")
    val mediaTrackName: StateFlow<String> = _mediaTrackName.asStateFlow()

    private val _mediaTrackArtist = MutableStateFlow<String>("")
    val mediaTrackArtist: StateFlow<String> = _mediaTrackArtist.asStateFlow()

    private val _mediaDuration = MutableStateFlow<Int>(0)
    val mediaDuration: StateFlow<Int> = _mediaDuration.asStateFlow()

    private val _ambientDecibel = MutableStateFlow<Float>(0f)
    val ambientDecibel: StateFlow<Float> = _ambientDecibel.asStateFlow()

    private val _isRecordingNoise = MutableStateFlow<Boolean>(false)
    val isRecordingNoise: StateFlow<Boolean> = _isRecordingNoise.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val scannedDevices: StateFlow<List<ScannedDevice>> = _scannedDevices.asStateFlow()

    private val _isScanningBluetooth = MutableStateFlow<Boolean>(false)
    val isScanningBluetooth: StateFlow<Boolean> = _isScanningBluetooth.asStateFlow()

    private val _firmwareVersion = MutableStateFlow<String>("")
    val firmwareVersion: StateFlow<String> = _firmwareVersion.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _isCharging = MutableStateFlow<Boolean>(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    fun playProceduralTone(vararg args: Any?) {}
    fun toggleNoiseMonitoring(vararg args: Any?) {}
    fun startNoiseMonitoring(vararg args: Any?) {}
    fun stopNoiseMonitoring(vararg args: Any?) {}
    fun startBluetoothScan(vararg args: Any?) {}
    fun stopBluetoothScan(vararg args: Any?) {}
    fun connectDevice(vararg args: Any?) {}
    fun disconnectDevice(vararg args: Any?) {}
    fun simulateConnectionLoss(vararg args: Any?) {}
    fun toggleAutoReconnect(vararg args: Any?) {}
    fun triggerAutoReconnect(vararg args: Any?) {}
    fun updateBand(vararg args: Any?) {}
    fun saveCustomPreset(vararg args: Any?) {}
    fun deleteCustomPreset(vararg args: Any?) {}
    fun updateMasterGain(vararg args: Any?) {}
    fun toggleAnc(vararg args: Any?) {}
    fun toggleDynamicBass(vararg args: Any?) {}
    fun toggleSurround(vararg args: Any?) {}
    fun toggleSoundZones(vararg args: Any?) {}
    fun toggleAdaptiveActivity(vararg args: Any?) {}
    fun toggleLdac(vararg args: Any?) {}
    fun toggleAutoPowerOff(vararg args: Any?) {}
    fun updateBatteryLevel(vararg args: Any?) {}
    fun toggleMultipoint(vararg args: Any?) {}
    fun addMultipointDevice(vararg args: Any?) {}
    fun removeMultipointDevice(vararg args: Any?) {}
    fun updateMultipointDevices(vararg args: Any?) {}
    fun resetAll(vararg args: Any?) {}
    fun checkForUpdates(vararg args: Any?) {}
    fun startUpdate(vararg args: Any?) {}
    fun resetUpdateState(vararg args: Any?) {}
    fun toggleCharging(vararg args: Any?) {}
    fun setAncLevel(vararg args: Any?) {}
    fun setAncMode(vararg args: Any?) {}
    fun setAutoPowerOffMinutes(vararg args: Any?) {}
    fun setPreset(vararg args: Any?) {}
    fun setSimulatedActivity(vararg args: Any?) {}
    fun setSoundZone(vararg args: Any?) {}
    fun toggleTrackOfflineStatus(vararg args: Any?) {}

    data class Track(val id: String, val title: String, val artist: String, val sourceUrl: String, val isOffline: Boolean)
    val playlist = listOf(
        Track("1", "Let It Happen", "Tame Impala", "", false),
        Track("2", "Synthwave Sunset", "Neon Night", "", false),
        Track("3", "Focus Ambient Noise", "Philips Offline", "", true),
        Track("4", "Lofi Beats", "Chillhop", "", false)
    )
    
    val presets: Map<String, List<Float>> = emptyMap()
    val currentTrackIndex = MutableStateFlow(0)
    
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var audioFocusRequest: Any? = null

    fun playTrack(index: Int) {
        if (index in playlist.indices) {
            currentTrackIndex.value = index
            stopMediaPlayer()
            playMediaPlayer()
        }
    }

    fun playNextTrack() {
        val next = (currentTrackIndex.value + 1) % playlist.size
        playTrack(next)
    }

    fun playPreviousTrack() {
        val prev = if (currentTrackIndex.value - 1 < 0) playlist.size - 1 else currentTrackIndex.value - 1
        playTrack(prev)
    }

    fun seekMedia(positionSecs: Int) {
        mediaPlayer?.seekTo(positionSecs * 1000)
    }

    private fun requestExclusiveAudioFocus() {
        try {
            val audioManager = getApplication<Application>().getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val focusRequest = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { focusChange ->
                        if (focusChange == android.media.AudioManager.AUDIOFOCUS_LOSS) {
                            pauseMediaPlayer()
                        }
                    }
                    .build()
                audioFocusRequest = focusRequest
                audioManager.requestAudioFocus(focusRequest)
            } else {
                audioManager.requestAudioFocus(
                    { focusChange ->
                        if (focusChange == android.media.AudioManager.AUDIOFOCUS_LOSS) {
                            pauseMediaPlayer()
                        }
                    },
                    android.media.AudioManager.STREAM_MUSIC,
                    android.media.AudioManager.AUDIOFOCUS_GAIN
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun abandonExclusiveAudioFocus() {
        try {
            val audioManager = getApplication<Application>().getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                (audioFocusRequest as? android.media.AudioFocusRequest)?.let { req ->
                    audioManager.abandonAudioFocusRequest(req)
                }
            } else {
                audioManager.abandonAudioFocus(null)
            }
        } catch (e: Exception) {}
    }

    fun playMediaPlayer(vararg args: Any?) {
        requestExclusiveAudioFocus()
        _mediaIsPlaying.value = true
    }

    fun pauseMediaPlayer(vararg args: Any?) {
        abandonExclusiveAudioFocus()
        _mediaIsPlaying.value = false
    }

    fun stopMediaPlayer(vararg args: Any?) {
        abandonExclusiveAudioFocus()
        _mediaIsPlaying.value = false
    }

    fun toggleMediaPlayer(vararg args: Any?) {
        if (_mediaIsPlaying.value) pauseMediaPlayer() else playMediaPlayer()
    }
}
