package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "headphone_settings")
data class HeadphoneSettings(
    @PrimaryKey val id: Int = 1,
    val connected: Boolean = false,
    val ancEnabled: Boolean = true,
    val ancLevel: Int = 2,
    val ancMode: String = "ON", // "ON", "OFF", "TRANSPARENCY"
    val dynamicBassEnabled: Boolean = true,
    val surroundSoundEnabled: Boolean = false,
    val ldacEnabled: Boolean = true,
    val autoPowerOffEnabled: Boolean = true,
    val autoPowerOffMinutes: Int = 30,
    val masterGain: Float = 0.0f,
    val batteryLevel: Int = 88,
    val multipointEnabled: Boolean = true,
    val multipointDevices: String = "Google Pixel 8,MacBook Pro",
    val activePreset: String? = "Philips Signature",
    val band60: Float = 3.0f,
    val band125: Float = 2.0f,
    val band250: Float = 1.0f,
    val band500: Float = 0.0f,
    val band1000: Float = -1.0f,
    val band2000: Float = 0.0f,
    val band4000: Float = 1.0f,
    val band8000: Float = 2.0f,
    val band12000: Float = 3.0f,
    val band16000: Float = 2.0f,
    val customPresets: String = "",
    // Advanced features: Sound Zones & Adaptive Activity Control
    val soundZonesEnabled: Boolean = false,
    val activeSoundZone: String = "Uit", // "Uit", "Thuis", "Kantoor", "Sportschool", "Trein"
    val adaptiveActivityEnabled: Boolean = false,
    val activeActivity: String = "Zitten", // "Zitten", "Wandelen", "Hardlopen", "Reizen"
    // Premium Web-informed features
    val sidetoneEnabled: Boolean = false,
    val sidetoneLevel: Int = 50,
    val wearingDetectionEnabled: Boolean = true,
    val windNoiseReductionEnabled: Boolean = false,
    
    // Touch Controls
    val touchControlsEnabled: Boolean = true,
    val touchSingleTapAction: String = "Afspelen/Pauzeren",
    val touchDoubleTapAction: String = "Volgende track",
    val touchHoldAction: String = "ANC Wisselen",
    
    // Battery Health
    val batteryHealthEnabled: Boolean = false
) {
    fun getBands(): List<Float> {
        return listOf(
            band60, band125, band250, band500, band1000,
            band2000, band4000, band8000, band12000, band16000
        )
    }

    fun getCustomPresetsMap(): Map<String, List<Float>> {
        if (customPresets.isEmpty()) return emptyMap()
        val map = mutableMapOf<String, List<Float>>()
        try {
            customPresets.split("|").forEach { part ->
                if (part.contains(":")) {
                    val subParts = part.split(":")
                    val name = subParts[0]
                    val values = subParts[1].split(",").map { it.toFloat() }
                    if (values.size == 10) {
                        map[name] = values
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    fun copyWithBands(bands: List<Float>): HeadphoneSettings {
        if (bands.size < 10) return this
        return this.copy(
            band60 = bands[0],
            band125 = bands[1],
            band250 = bands[2],
            band500 = bands[3],
            band1000 = bands[4],
            band2000 = bands[5],
            band4000 = bands[6],
            band8000 = bands[7],
            band12000 = bands[8],
            band16000 = bands[9]
        )
    }
}
