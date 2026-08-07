package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.equalizerDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_equalizer_preferences")

data class PersistedEqData(
    val activePreset: String,
    val bands: List<Float>,
    val customPresetsSerialized: String,
    val masterGain: Float,
    val is10BandMode: Boolean,
    val dynamicBassLevel: Int
)

class EqualizerPreferencesRepository(private val context: Context) {

    companion object {
        val KEY_ACTIVE_PRESET = stringPreferencesKey("active_eq_preset")
        val KEY_BANDS_CSV = stringPreferencesKey("eq_bands_csv")
        val KEY_CUSTOM_PRESETS = stringPreferencesKey("custom_eq_presets_serialized")
        val KEY_MASTER_GAIN = floatPreferencesKey("eq_master_gain")
        val KEY_EQ_MODE_10_BAND = booleanPreferencesKey("eq_mode_10_band")
        val KEY_DYNAMIC_BASS_LEVEL = intPreferencesKey("eq_dynamic_bass_level")
        
        val DEFAULT_BANDS = listOf(3.0f, 2.0f, 1.0f, 0.0f, -1.0f, 0.0f, 1.0f, 2.0f, 3.0f, 2.0f)
    }

    val equalizerPreferencesFlow: Flow<PersistedEqData> = context.equalizerDataStore.data.map { preferences ->
        val activePreset = preferences[KEY_ACTIVE_PRESET] ?: "Philips Signature"
        val bandsCsv = preferences[KEY_BANDS_CSV] ?: "3.0,2.0,1.0,0.0,-1.0,0.0,1.0,2.0,3.0,2.0"
        val customPresets = preferences[KEY_CUSTOM_PRESETS] ?: ""
        val masterGain = preferences[KEY_MASTER_GAIN] ?: 0.0f
        val is10Band = preferences[KEY_EQ_MODE_10_BAND] ?: true
        val bassLevel = preferences[KEY_DYNAMIC_BASS_LEVEL] ?: 1

        val bands = try {
            val parsed = bandsCsv.split(",").map { it.trim().toFloat() }
            if (parsed.size == 10) parsed else DEFAULT_BANDS
        } catch (e: Exception) {
            DEFAULT_BANDS
        }

        PersistedEqData(
            activePreset = activePreset,
            bands = bands,
            customPresetsSerialized = customPresets,
            masterGain = masterGain,
            is10BandMode = is10Band,
            dynamicBassLevel = bassLevel
        )
    }

    suspend fun saveEqualizerPreferences(
        activePreset: String,
        bands: List<Float>,
        customPresetsSerialized: String,
        masterGain: Float = 0.0f,
        is10BandMode: Boolean = true,
        dynamicBassLevel: Int = 1
    ) {
        context.equalizerDataStore.edit { preferences ->
            preferences[KEY_ACTIVE_PRESET] = activePreset
            preferences[KEY_BANDS_CSV] = bands.joinToString(",")
            preferences[KEY_CUSTOM_PRESETS] = customPresetsSerialized
            preferences[KEY_MASTER_GAIN] = masterGain
            preferences[KEY_EQ_MODE_10_BAND] = is10BandMode
            preferences[KEY_DYNAMIC_BASS_LEVEL] = dynamicBassLevel
        }
    }

    suspend fun resetEqualizerPreferences() {
        context.equalizerDataStore.edit { preferences ->
            preferences[KEY_ACTIVE_PRESET] = "Philips Signature"
            preferences[KEY_BANDS_CSV] = DEFAULT_BANDS.joinToString(",")
            preferences[KEY_CUSTOM_PRESETS] = ""
            preferences[KEY_MASTER_GAIN] = 0.0f
            preferences[KEY_EQ_MODE_10_BAND] = true
            preferences[KEY_DYNAMIC_BASS_LEVEL] = 1
        }
    }
}
