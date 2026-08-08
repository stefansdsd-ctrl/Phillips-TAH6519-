package com.example.headphonecompanion.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.UUID

object GattUuids {
    val BATTERY_SERVICE: UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb")
    val BATTERY_LEVEL_CHAR: UUID = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb")
}

class BatteryGattReader(private val context: Context) {
    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var gatt: BluetoothGatt? = null
    private val _batteryEvents = Channel<Int>(Channel.CONFLATED)
    val batteryEvents: Flow<Int> = _batteryEvents.receiveAsFlow()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val svc: BluetoothGattService? = g.getService(GattUuids.BATTERY_SERVICE)
            val char: BluetoothGattCharacteristic? = svc?.getCharacteristic(GattUuids.BATTERY_LEVEL_CHAR)
            char?.let { g.readCharacteristic(it) }
        }
        override fun onCharacteristicRead(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (characteristic.uuid == GattUuids.BATTERY_LEVEL_CHAR && status == BluetoothGatt.GATT_SUCCESS) {
                val level = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) ?: -1
                _batteryEvents.trySend(level)
            }
        }
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                g.discoverServices()
            }
        }
    }

    fun connectAndReadBattery(device: BluetoothDevice) {
        gatt?.close()
        gatt = device.connectGatt(context, false, gattCallback)
    }

    fun close() {
        gatt?.close()
        gatt = null
    }

    fun listPairedAudioDevices(): List<BluetoothDevice> {
        val bonded = adapter?.bondedDevices ?: emptySet()
        return bonded.filter { it.bluetoothClass?.deviceClass != null }.toList()
    }
}
