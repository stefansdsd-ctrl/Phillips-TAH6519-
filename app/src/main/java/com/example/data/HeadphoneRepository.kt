package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HeadphoneRepository(
    private val headphoneDao: HeadphoneDao,
    private val cachedTrackDao: CachedTrackDao? = null
) {

    // Emits settings. If database has no settings, emit a default object.
    val settingsFlow: Flow<HeadphoneSettings> = headphoneDao.getSettingsFlow().map {
        it ?: HeadphoneSettings()
    }

    val cachedTracksFlow: Flow<List<CachedTrackEntity>> = cachedTrackDao?.getAllCachedTracksFlow()
        ?: kotlinx.coroutines.flow.flowOf(emptyList())

    val cachedYoutubeTracksFlow: Flow<List<CachedTrackEntity>> = cachedTrackDao?.getYoutubeTracksFlow()
        ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun getCachedTracks(): List<CachedTrackEntity> {
        return cachedTrackDao?.getAllCachedTracks() ?: emptyList()
    }

    suspend fun saveCachedTracks(tracks: List<CachedTrackEntity>) {
        cachedTrackDao?.clearQueueTracks()
        cachedTrackDao?.insertAll(tracks.map { it.copy(playlistSource = "NOW_PLAYING") })
    }

    suspend fun clearCachedTracks() {
        cachedTrackDao?.clearQueueTracks()
    }

    suspend fun getCachedYoutubeTracks(): List<CachedTrackEntity> {
        return cachedTrackDao?.getYoutubeTracks() ?: emptyList()
    }

    suspend fun saveCachedYoutubeTracks(tracks: List<CachedTrackEntity>) {
        cachedTrackDao?.clearYoutubeTracks()
        cachedTrackDao?.insertAll(tracks.map { it.copy(playlistSource = "YOUTUBE") })
    }

    suspend fun clearCachedYoutubeTracks() {
        cachedTrackDao?.clearYoutubeTracks()
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
