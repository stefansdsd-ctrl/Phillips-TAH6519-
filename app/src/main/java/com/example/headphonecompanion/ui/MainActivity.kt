package com.example.headphonecompanion.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.headphonecompanion.audio.EqualizerManager
import com.example.headphonecompanion.bluetooth.BatteryGattReader
import com.example.headphonecompanion.dsp.ParametricEq
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.google.android.exoplayer2.ExoPlayer

class MainActivity : ComponentActivity() {
    private lateinit var reader: BatteryGattReader
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reader = BatteryGattReader(applicationContext)

        player = ExoPlayer.Builder(this).build()

        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_SCAN
            permissions += Manifest.permission.BLUETOOTH_CONNECT
        } else {
            permissions += Manifest.permission.BLUETOOTH
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            permissions += Manifest.permission.ACCESS_FINE_LOCATION // older BLE scanning
        }

        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppContent(reader, { perms -> requestPermissionLauncher.launch(perms.associateWith { true }) }, permissions.toTypedArray(), player)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        reader.close()
        player?.release()
    }
}

@Composable
fun AppContent(reader: BatteryGattReader, requestPermission: (Array<String>) -> Unit, perms: Array<String>, player: ExoPlayer?) {
    val scope = rememberCoroutineScope()
    var devices by remember { mutableStateOf<List<android.bluetooth.BluetoothDevice>>(emptyList()) }
    var selectedBattery by remember { mutableStateOf<Int?>(null) }
    var showEq by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        devices = reader.listPairedAudioDevices()
    }

    LaunchedEffect(reader) {
        scope.launch {
            reader.batteryEvents.collectLatest { lvl ->
                selectedBattery = lvl
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = { requestPermission(perms) }) {
            Text("Grant Bluetooth permissions")
        }

        Button(onClick = { showEq = !showEq }, modifier = Modifier.padding(top = 8.dp)) {
            Text(if (showEq) "Hide EQ" else "Open EQ")
        }

        Text("Paired devices:", modifier = Modifier.padding(top = 12.dp))
        LazyColumn {
            items(devices.size) { idx ->
                val d = devices[idx]
                ListItem(
                    headlineText = { Text(d.name ?: d.address ?: "Unknown") },
                    modifier = Modifier.clickable {
                        reader.connectAndReadBattery(d)
                    }
                )
            }
        }

        selectedBattery?.let {
            Text("Battery level: $it%", modifier = Modifier.padding(top = 8.dp))
        } ?: Text("No battery reading yet", modifier = Modifier.padding(top = 8.dp))

        if (showEq) {
            EqScreen(onApply = { eq ->
                // Use active player's audioSessionId if available, otherwise fallback to 0
                val audioSessionId = player?.audioSessionId ?: 0
                val manager = EqualizerManager(audioSessionId)
                manager.applyParametricEq(eq)
            })
        }
    }
}
