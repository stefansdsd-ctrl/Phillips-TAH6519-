package com.tah6519pro.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid

data class BleAdvertisement(
    val name: String,
    val address: String,
    val rssi: Int,
    val txPower: Int?,
    val serviceUuids: List<String>,
    val manufacturerData: Map<Int, String>,
    val serviceData: Map<String, String>
)

class BleAdvertisementScanner(
    context: Context,
    private val onResult: (BleAdvertisement) -> Unit,
    private val onError: (Int) -> Unit
) {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val scanner: BluetoothLeScanner?
        get() = adapter?.bluetoothLeScanner

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            onResult(result.toAdvertisement())
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { onResult(it.toAdvertisement()) }
        }

        override fun onScanFailed(errorCode: Int) {
            onError(errorCode)
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        val s = scanner
        if (s == null) {
            // scanner unavailable — report an internal error to the caller
            onError(ScanCallback.SCAN_FAILED_INTERNAL_ERROR)
            return
        }

        s.startScan(
            emptyList(),
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build(),
            callback
        )
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        scanner?.stopScan(callback)
    }

    @SuppressLint("MissingPermission")
    private fun ScanResult.toAdvertisement(): BleAdvertisement {
        val record = scanRecord
        val manufacturer = record?.manufacturerSpecificData
        val manufacturerMap = mutableMapOf<Int, String>()
        if (manufacturer != null) {
            for (i in 0 until manufacturer.size()) {
                val id = manufacturer.keyAt(i)
                val bytes = manufacturer.valueAt(i)
                manufacturerMap[id] = bytes.toHex()
            }
        }

        // Use the underlying UUID string so consumers get canonical UUIDs
        val services = record?.serviceUuids?.map { it.uuid.toString() }.orEmpty()
        val serviceData = record?.serviceData
            ?.mapKeys { it.key.uuid.toString() }
            ?.mapValues { it.value.toHex() }.orEmpty()

        val deviceName = record?.deviceName
            ?: device.name
            ?: "Unknown BLE device"

        return BleAdvertisement(
            name = deviceName,
            address = device.address,
            rssi = rssi,
            txPower = record?.txPowerLevel?.takeUnless { it == Int.MIN_VALUE },
            serviceUuids = services,
            manufacturerData = manufacturerMap,
            serviceData = serviceData
        )
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02X".format(it.toInt() and 0xFF) }
}
