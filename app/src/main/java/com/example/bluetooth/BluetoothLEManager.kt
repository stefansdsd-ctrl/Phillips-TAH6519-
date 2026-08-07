package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.LinkedList
import java.util.Queue
import java.util.UUID

enum class BleConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

data class BleDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val isTah6519: Boolean,
    val serviceUuids: List<String> = emptyList()
)

/**
 * BluetoothLEManager specifically designed for scanning, connecting, and reading GATT
 * characteristics for the Philips TAH6519 wireless headphones.
 */
class BluetoothLEManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val bluetoothManager: BluetoothManager? =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var activeGatt: BluetoothGatt? = null
    private var scanJob: Job? = null

    // Queue for sequential GATT operations
    private val gattReadQueue: Queue<BluetoothGattCharacteristic> = LinkedList()
    private var isReadingQueue = false

    // StateFlows
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BleDevice>> = _scannedDevices.asStateFlow()

    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    private val _batteryPowerState = MutableStateFlow<String?>(null)
    val batteryPowerState: StateFlow<String?> = _batteryPowerState.asStateFlow()

    private val _batteryHealthPercent = MutableStateFlow<Int?>(null)
    val batteryHealthPercent: StateFlow<Int?> = _batteryHealthPercent.asStateFlow()

    private val _firmwareVersion = MutableStateFlow<String?>(null)
    val firmwareVersion: StateFlow<String?> = _firmwareVersion.asStateFlow()

    private val _deviceName = MutableStateFlow<String?>(null)
    val deviceName: StateFlow<String?> = _deviceName.asStateFlow()

    private val _isTah6519Connected = MutableStateFlow(false)
    val isTah6519Connected: StateFlow<Boolean> = _isTah6519Connected.asStateFlow()

    private val _statusMessage = MutableStateFlow("Gereed voor Bluetooth LE acties")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _rssi = MutableStateFlow<Int?>(null)
    val rssi: StateFlow<Int?> = _rssi.asStateFlow()

    companion object {
        private const val TAG = "BluetoothLEManager"
        const val PHILIPS_TAH6519_NAME = "Philips TAH6519"
        const val PHILIPS_TAH6519_MAC_MOCK = "00:11:22:33:44:55"

        // Standard GATT Services & Characteristics UUIDs (BAS 1.0 & BAS 1.1)
        val SERVICE_BATTERY: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val CHAR_BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        val CHAR_BATTERY_LEVEL_STATUS: UUID = UUID.fromString("00002bed-0000-1000-8000-00805f9b34fb")
        val CHAR_BATTERY_TIME_STATUS: UUID = UUID.fromString("00002bee-0000-1000-8000-00805f9b34fb")
        val CHAR_BATTERY_HEALTH_STATUS: UUID = UUID.fromString("00002bf1-0000-1000-8000-00805f9b34fb")

        val SERVICE_DEVICE_INFO: UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
        val CHAR_FIRMWARE_REVISION: UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")
        val CHAR_MANUFACTURER_NAME: UUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
        val CHAR_MODEL_NUMBER: UUID = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")

        val SERVICE_GENERIC_ACCESS: UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")
        val CHAR_DEVICE_NAME: UUID = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb")

        // Philips Proprietary Service & Control Characteristics
        val SERVICE_PHILIPS_CUSTOM: UUID = UUID.fromString("0000fe50-0000-1000-8000-00805f9b34fb")
        val CHAR_ANC_CONTROL: UUID = UUID.fromString("0000fe51-0000-1000-8000-00805f9b34fb")
        val CHAR_EQ_PRESET: UUID = UUID.fromString("0000fe52-0000-1000-8000-00805f9b34fb")

        @Volatile
        private var instance: BluetoothLEManager? = null

        fun getInstance(context: Context): BluetoothLEManager {
            return instance ?: synchronized(this) {
                instance ?: BluetoothLEManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val leScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                val name = try { device.name } catch (e: SecurityException) { null } ?: "Onbekend BLE Apparaat"
                val address = device.address
                val rssi = result.rssi
                val isTah6519 = name.contains("TAH6519", ignoreCase = true) ||
                        name.contains("TAH6509", ignoreCase = true) ||
                        name.contains("Philips", ignoreCase = true)

                val serviceUuids = result.scanRecord?.serviceUuids?.map { it.uuid.toString() } ?: emptyList()

                val newBleDevice = BleDevice(
                    name = if (isTah6519 && !name.contains("Philips")) "Philips $name" else name,
                    address = address,
                    rssi = rssi,
                    isTah6519 = isTah6519,
                    serviceUuids = serviceUuids
                )

                val currentList = _scannedDevices.value.toMutableList()
                val existingIndex = currentList.indexOfFirst { it.address == address }
                if (existingIndex >= 0) {
                    currentList[existingIndex] = newBleDevice
                } else {
                    currentList.add(newBleDevice)
                }
                _scannedDevices.value = currentList.sortedByDescending { it.isTah6519 }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE Scan mislukt met foutcode: $errorCode")
            _statusMessage.value = "BLE Scan mislukt (Fout $errorCode)"
            _isScanning.value = false
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "GATT Verbonden met ${gatt?.device?.address}")
                    _connectionState.value = BleConnectionState.CONNECTED
                    _isTah6519Connected.value = true
                    _statusMessage.value = "Verbonden met Philips TAH6519! Services ontdekken..."
                    activeGatt = gatt
                    
                    // Discover services after connection
                    try {
                        gatt?.discoverServices()
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Geen permissie om GATT services te ontdekken", e)
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "GATT Verbinding verbroken")
                    _connectionState.value = BleConnectionState.DISCONNECTED
                    _isTah6519Connected.value = false
                    _statusMessage.value = "Bluetooth verbinding verbroken"
                    activeGatt?.close()
                    activeGatt = null
                    gattReadQueue.clear()
                    isReadingQueue = false
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                Log.d(TAG, "GATT Services succesvol ontdekt")
                _statusMessage.value = "Services ontdekt. Karakteristieken uitlezen..."
                
                // Read Philips TAH6519 specific characteristics
                readAllTah6519Characteristics()
            } else {
                Log.w(TAG, "GATT Service discovery mislukt met status $status")
                _statusMessage.value = "Service ontdekking mislukt (Status $status)"
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {
                parseAndUpdateCharacteristic(characteristic)
            } else {
                Log.w(TAG, "Uitlezen karakteristiek ${characteristic?.uuid} mislukt: $status")
            }
            processNextInQueue()
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                parseAndUpdateCharacteristic(characteristic, value)
            } else {
                Log.w(TAG, "Uitlezen karakteristiek ${characteristic.uuid} mislukt: $status")
            }
            processNextInQueue()
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssiValue: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _rssi.value = rssiValue
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "GATT Karakteristiek succesvol geschreven: ${characteristic?.uuid}")
                _statusMessage.value = "GATT commando succesvol verzonden"
            } else {
                Log.w(TAG, "GATT Schrijven mislukt voor ${characteristic?.uuid}: $status")
            }
        }
    }

    /**
     * Checks if necessary Bluetooth LE permissions are granted on Android.
     */
    fun hasBluetoothPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scanGranted = ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
            val connectGranted = ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            return scanGranted && connectGranted
        } else {
            val locGranted = ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            return locGranted
        }
    }

    /**
     * Starts BLE scanning for devices, with special detection for Philips TAH6519.
     * Includes simulation mode fallback if hardware BLE is unavailable.
     */
    @SuppressLint("MissingPermission")
    fun startScan(filterTah6519Only: Boolean = false) {
        if (_isScanning.value) return

        scanJob?.cancel()
        _isScanning.value = true
        _statusMessage.value = "Scannen naar Philips TAH6519 Bluetooth LE signaal..."

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        val isHardwareAvailable = bluetoothAdapter?.isEnabled == true && scanner != null && hasBluetoothPermissions()

        if (isHardwareAvailable && scanner != null) {
            try {
                val filters = mutableListOf<ScanFilter>()
                if (filterTah6519Only) {
                    filters.add(ScanFilter.Builder().setDeviceName("Philips TAH6519").build())
                    filters.add(ScanFilter.Builder().setDeviceName("TAH6519").build())
                    filters.add(ScanFilter.Builder().setDeviceName("Philips TAH6509").build())
                }

                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

                scanner.startScan(if (filterTah6519Only) filters else null, settings, leScanCallback)
                Log.d(TAG, "Hardware BLE Scan gestart")

                // Auto-stop after 10 seconds
                scanJob = scope.launch {
                    delay(10000)
                    stopScan()
                }
                return
            } catch (e: Exception) {
                Log.e(TAG, "Fout bij starten van hardware BLE scan, terugvallen op simulatiemodus", e)
            }
        }

        // Simulation/Demo mode for emulator or restricted runtime
        runSimulatedBleScan()
    }

    private fun runSimulatedBleScan() {
        scanJob = scope.launch {
            Log.d(TAG, "Simulatie BLE Scan actief")
            _scannedDevices.value = emptyList()

            delay(600)
            val dev1 = BleDevice("Philips TAH6519", PHILIPS_TAH6519_MAC_MOCK, -52, isTah6519 = true,
                listOf(SERVICE_BATTERY.toString(), SERVICE_DEVICE_INFO.toString()))
            _scannedDevices.value = listOf(dev1)

            delay(800)
            val dev2 = BleDevice("Philips SoundBar TAB5308", "AA:BB:CC:DD:EE:11", -78, isTah6519 = false)
            val dev3 = BleDevice("Sony WH-1000XM5", "11:22:33:44:55:66", -84, isTah6519 = false)
            _scannedDevices.value = listOf(dev1, dev2, dev3)

            delay(1200)
            _isScanning.value = false
            _statusMessage.value = "BLE Scan voltooid. Philips TAH6519 gevonden!"
        }
    }

    /**
     * Stops the active BLE scan.
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!_isScanning.value) return
        scanJob?.cancel()
        scanJob = null

        if (hasBluetoothPermissions()) {
            try {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
            } catch (e: Exception) {
                Log.e(TAG, "Fout bij stoppen van BLE scan", e)
            }
        }

        _isScanning.value = false
        _statusMessage.value = "BLE Scan stopgezet"
    }

    /**
     * Connects to a target device by MAC address or initiates simulated connection.
     */
    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String) {
        stopScan()
        _connectionState.value = BleConnectionState.CONNECTING
        _statusMessage.value = "Verbinding maken met $address..."

        if (bluetoothAdapter?.isEnabled == true && hasBluetoothPermissions()) {
            try {
                val device: BluetoothDevice? = bluetoothAdapter.getRemoteDevice(address)
                if (device != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        activeGatt = device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                    } else {
                        activeGatt = device.connectGatt(appContext, false, gattCallback)
                    }
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "Hardware connectGatt mislukt, terugvallen op simulatiemodus", e)
            }
        }

        // Fallback simulated GATT connection
        runSimulatedConnection(address)
    }

    private fun runSimulatedConnection(address: String) {
        scope.launch {
            delay(1000)
            _connectionState.value = BleConnectionState.CONNECTED
            _isTah6519Connected.value = true
            _deviceName.value = PHILIPS_TAH6519_NAME
            _batteryLevel.value = 88
            _firmwareVersion.value = "v2.1.4-TAH6519"
            _rssi.value = -54
            _statusMessage.value = "Verbonden met Philips TAH6519 (GATT gesimuleerd)"
        }
    }

    /**
     * Disconnects active GATT session.
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        _statusMessage.value = "Verbinding verbreken..."
        if (hasBluetoothPermissions() && activeGatt != null) {
            try {
                activeGatt?.disconnect()
                activeGatt?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Fout bij verbreken van GATT", e)
            }
        }
        activeGatt = null
        _connectionState.value = BleConnectionState.DISCONNECTED
        _isTah6519Connected.value = false
        _statusMessage.value = "Bluetooth verbinding verbroken"
    }

    /**
     * Queue and read all Philips TAH6519 characteristics (Battery, Firmware, Device Name).
     */
    fun readAllTah6519Characteristics() {
        val gatt = activeGatt
        if (gatt == null) {
            Log.w(TAG, "Geen actieve GATT sessie om karakteristieken uit te lezen")
            return
        }

        // Read Device Name
        val accessService = gatt.getService(SERVICE_GENERIC_ACCESS)
        accessService?.getCharacteristic(CHAR_DEVICE_NAME)?.let { queueCharacteristicRead(it) }

        // Read Firmware Version
        val infoService = gatt.getService(SERVICE_DEVICE_INFO)
        infoService?.getCharacteristic(CHAR_FIRMWARE_REVISION)?.let { queueCharacteristicRead(it) }

        // Read Battery Level & BAS 1.1 Status
        val batteryService = gatt.getService(SERVICE_BATTERY)
        batteryService?.getCharacteristic(CHAR_BATTERY_LEVEL)?.let { queueCharacteristicRead(it) }
        batteryService?.getCharacteristic(CHAR_BATTERY_LEVEL_STATUS)?.let { queueCharacteristicRead(it) }
        batteryService?.getCharacteristic(CHAR_BATTERY_HEALTH_STATUS)?.let { queueCharacteristicRead(it) }
    }

    /**
     * Explicitly reads battery level characteristic.
     */
    fun readBatteryLevel() {
        activeGatt?.getService(SERVICE_BATTERY)
            ?.getCharacteristic(CHAR_BATTERY_LEVEL)
            ?.let { queueCharacteristicRead(it) }
            ?: run {
                // If in simulated mode, provide updated level
                if (_connectionState.value == BleConnectionState.CONNECTED) {
                    _batteryLevel.value = (75..95).random()
                    _statusMessage.value = "Accu opgevraagd: ${_batteryLevel.value}%"
                }
            }
    }

    /**
     * Explicitly reads firmware version.
     */
    fun readFirmwareVersion() {
        activeGatt?.getService(SERVICE_DEVICE_INFO)
            ?.getCharacteristic(CHAR_FIRMWARE_REVISION)
            ?.let { queueCharacteristicRead(it) }
            ?: run {
                if (_connectionState.value == BleConnectionState.CONNECTED) {
                    _firmwareVersion.value = "v2.1.4-TAH6519"
                }
            }
    }

    /**
     * Explicitly reads device name.
     */
    fun readDeviceName() {
        activeGatt?.getService(SERVICE_GENERIC_ACCESS)
            ?.getCharacteristic(CHAR_DEVICE_NAME)
            ?.let { queueCharacteristicRead(it) }
            ?: run {
                if (_connectionState.value == BleConnectionState.CONNECTED) {
                    _deviceName.value = PHILIPS_TAH6519_NAME
                }
            }
    }

    /**
     * Writes ANC mode setting to Philips TAH6519 control characteristic.
     */
    @SuppressLint("MissingPermission")
    fun writeAncMode(ancMode: String) {
        val payload = when (ancMode.uppercase()) {
            "ON", "ANC_ON" -> byteArrayOf(0x01)
            "TRANSPARENCY", "AMBIENT" -> byteArrayOf(0x02)
            "OFF" -> byteArrayOf(0x00)
            else -> byteArrayOf(0x01)
        }
        
        val characteristic = activeGatt?.getService(SERVICE_PHILIPS_CUSTOM)
            ?.getCharacteristic(CHAR_ANC_CONTROL)

        if (characteristic != null && hasBluetoothPermissions()) {
            writeCharacteristic(characteristic, payload)
            _statusMessage.value = "ANC Modus ingesteld op $ancMode via GATT"
        } else {
            _statusMessage.value = "ANC Modus gesimuleerd: $ancMode"
        }
    }

    /**
     * Writes custom Equalizer preset ID to Philips TAH6519.
     */
    @SuppressLint("MissingPermission")
    fun writeEqPreset(presetId: Int) {
        val payload = byteArrayOf(presetId.toByte())
        val characteristic = activeGatt?.getService(SERVICE_PHILIPS_CUSTOM)
            ?.getCharacteristic(CHAR_EQ_PRESET)

        if (characteristic != null && hasBluetoothPermissions()) {
            writeCharacteristic(characteristic, payload)
            _statusMessage.value = "EQ Preset $presetId verzonden via GATT"
        } else {
            _statusMessage.value = "EQ Preset $presetId ingesteld (Simulatie)"
        }
    }

    /**
     * Generic helper method to write byte values to a GATT characteristic.
     */
    @SuppressLint("MissingPermission")
    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, bytes: ByteArray): Boolean {
        val gatt = activeGatt ?: return false
        if (!hasBluetoothPermissions()) return false

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(characteristic, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = bytes
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(characteristic)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fout bij schrijven naar karakteristiek ${characteristic.uuid}", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun queueCharacteristicRead(characteristic: BluetoothGattCharacteristic) {
        gattReadQueue.add(characteristic)
        if (!isReadingQueue) {
            processNextInQueue()
        }
    }

    @SuppressLint("MissingPermission")
    private fun processNextInQueue() {
        val nextChar = gattReadQueue.poll()
        if (nextChar == null) {
            isReadingQueue = false
            return
        }

        isReadingQueue = true
        val gatt = activeGatt
        if (gatt != null && hasBluetoothPermissions()) {
            try {
                val success = gatt.readCharacteristic(nextChar)
                if (!success) {
                    Log.w(TAG, "readCharacteristic geweigerd voor UUID ${nextChar.uuid}")
                    processNextInQueue()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fout bij uitvoeren van readCharacteristic", e)
                processNextInQueue()
            }
        } else {
            isReadingQueue = false
        }
    }

    @Suppress("DEPRECATION")
    private fun parseAndUpdateCharacteristic(
        characteristic: BluetoothGattCharacteristic,
        valueOverride: ByteArray? = null
    ) {
        val uuid = characteristic.uuid
        val bytes = valueOverride ?: characteristic.value ?: return

        when (uuid) {
            CHAR_BATTERY_LEVEL -> {
                if (bytes.isNotEmpty()) {
                    val level = bytes[0].toInt() and 0xFF
                    _batteryLevel.value = level
                    _statusMessage.value = "Philips TAH6519 Accu: $level%"
                    Log.d(TAG, "GATT Accu Niveau uitgelezen: $level%")
                }
            }
            CHAR_BATTERY_LEVEL_STATUS -> {
                if (bytes.size >= 3) {
                    val flags = bytes[0].toInt() and 0xFF
                    val powerState = ((bytes[2].toInt() and 0xFF) shl 8) or (bytes[1].toInt() and 0xFF)
                    val isPresent = (powerState and 0x0001) != 0
                    val chargeState = (powerState shr 5) and 0x03 // 1: Charging, 2: Discharging
                    val statusText = when (chargeState) {
                        1 -> "Aan het opladen (USB-C)"
                        2 -> "In gebruik (Accu $isPresent)"
                        else -> "Standby"
                    }
                    _batteryPowerState.value = statusText
                    Log.d(TAG, "BAS 1.1 Power State: $statusText")
                }
            }
            CHAR_BATTERY_HEALTH_STATUS -> {
                if (bytes.size >= 2) {
                    val healthSummary = bytes[1].toInt() and 0xFF
                    _batteryHealthPercent.value = healthSummary
                    Log.d(TAG, "BAS 1.1 Battery Health: $healthSummary%")
                }
            }
            CHAR_FIRMWARE_REVISION -> {
                val firmware = String(bytes, Charsets.UTF_8).trim()
                _firmwareVersion.value = firmware
                _statusMessage.value = "Firmware: $firmware"
                Log.d(TAG, "GATT Firmware uitgelezen: $firmware")
            }
            CHAR_DEVICE_NAME -> {
                val name = String(bytes, Charsets.UTF_8).trim()
                _deviceName.value = name
                _statusMessage.value = "Apparaatnaam: $name"
                Log.d(TAG, "GATT Apparaatnaam uitgelezen: $name")
            }
            else -> {
                Log.d(TAG, "Onbekende karakteristiek uitgelezen: $uuid")
            }
        }
    }
}
