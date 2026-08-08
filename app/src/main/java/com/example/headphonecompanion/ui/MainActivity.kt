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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.headphonecompanion.bluetooth.BatteryGattReader
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var reader: BatteryGattReader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reader = BatteryGattReader(applicationContext)

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
                    MainScreen(reader, { perms -> requestPermissionLauncher.launch(perms.associateWith { true }) }, permissions.toTypedArray())
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        reader.close()
    }
}

@Composable
fun MainScreen(reader: BatteryGattReader, requestPermission: (Array<String>) -> Unit, perms: Array<String>) {
    val scope = rememberCoroutineScope()
    var devices by remember { mutableStateOf<List<android.bluetooth.BluetoothDevice>>(emptyList()) }
    var selectedBattery by remember { mutableStateOf<Int?>(null) }

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

    Column {
        Button(onClick = { requestPermission(perms) }) {
            Text("Grant Bluetooth permissions")
        }

        Text("Paired devices:")
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
            Text("Battery level: $it%")
        } ?: Text("No battery reading yet")
    }
}
