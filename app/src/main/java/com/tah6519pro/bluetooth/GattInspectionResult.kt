package com.tah6519pro.bluetooth

import java.util.UUID

data class GattCharacteristicModel(
    val uuid: UUID,
    val properties: Int
)

data class GattServiceModel(
    val uuid: UUID,
    val characteristics: List<GattCharacteristicModel>
)

data class GattInspectionResult(
    val deviceName: String,
    val services: List<GattServiceModel>
)

// Convenience extension to convert ByteArray to hex (space-separated by default)
internal fun ByteArray.toHexString(separator: String = " "): String =
    joinToString(separator) { "%02X".format(it.toInt() and 0xFF) }
