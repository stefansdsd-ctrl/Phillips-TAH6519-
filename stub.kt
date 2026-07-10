package com.example.ui
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
class HeadphoneViewModel : ViewModel() {
    val settingsState = MutableStateFlow<HeadphoneSettings>(null as HeadphoneSettings)
    val isConnecting = MutableStateFlow<Boolean>(null as Boolean)
    val autoReconnectEnabled = MutableStateFlow<Boolean>(null as Boolean)
    val isAutoReconnecting = MutableStateFlow<Boolean>(null as Boolean)
    val reconnectAttempts = MutableStateFlow<Integer>(null as Integer)
    val mediaIsPlaying = MutableStateFlow<Boolean>(null as Boolean)
    val mediaProgress = MutableStateFlow<Integer>(null as Integer)
    val mediaTrackName = MutableStateFlow<String>(null as String)
    val mediaTrackArtist = MutableStateFlow<String>(null as String)
    val mediaDuration = MutableStateFlow<Integer>(null as Integer)
    val ambientDecibel = MutableStateFlow<Float>(null as Float)
    val isRecordingNoise = MutableStateFlow<Boolean>(null as Boolean)
    val scannedDevices = MutableStateFlow<List<ScannedDevice>>(null as List<ScannedDevice>)
    val isScanningBluetooth = MutableStateFlow<Boolean>(null as Boolean)
    val firmwareVersion = MutableStateFlow<String>(null as String)
    val updateState = MutableStateFlow<UpdateState>(null as UpdateState)
    val isCharging = MutableStateFlow<Boolean>(null as Boolean)
}
