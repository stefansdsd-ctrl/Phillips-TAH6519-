package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HeadphoneRepository(private val headphoneDao: HeadphoneDao) {

    // Emits settings. If database has no settings, emit a default object.
    val settingsFlow: Flow<HeadphoneSettings> = headphoneDao.getSettingsFlow().map {
        it ?: HeadphoneSettings()
    }

    suspend fun getSettings(): HeadphoneSettings {
        return headphoneDao.getSettings() ?: HeadphoneSettings()
    }

    suspend fun updateSettings(settings: HeadphoneSettings) {
        headphoneDao.insertSettings(settings)
    }

    suspend fun resetSettings() {
        headphoneDao.insertSettings(HeadphoneSettings())
    }
}
