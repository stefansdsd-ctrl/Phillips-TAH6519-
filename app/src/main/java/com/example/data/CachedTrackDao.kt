package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedTrackDao {
    @Query("SELECT * FROM cached_tracks WHERE playlistSource = 'NOW_PLAYING' OR playlistSource IS NULL ORDER BY orderIndex ASC")
    fun getAllCachedTracksFlow(): Flow<List<CachedTrackEntity>>

    @Query("SELECT * FROM cached_tracks WHERE playlistSource = 'NOW_PLAYING' OR playlistSource IS NULL ORDER BY orderIndex ASC")
    suspend fun getAllCachedTracks(): List<CachedTrackEntity>

    @Query("SELECT * FROM cached_tracks WHERE playlistSource = 'YOUTUBE' ORDER BY orderIndex ASC")
    fun getYoutubeTracksFlow(): Flow<List<CachedTrackEntity>>

    @Query("SELECT * FROM cached_tracks WHERE playlistSource = 'YOUTUBE' ORDER BY orderIndex ASC")
    suspend fun getYoutubeTracks(): List<CachedTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<CachedTrackEntity>)

    @Query("DELETE FROM cached_tracks WHERE playlistSource = 'NOW_PLAYING' OR playlistSource IS NULL")
    suspend fun clearQueueTracks()

    @Query("DELETE FROM cached_tracks WHERE playlistSource = 'YOUTUBE'")
    suspend fun clearYoutubeTracks()

    @Query("DELETE FROM cached_tracks")
    suspend fun clearAll()

    @Query("DELETE FROM cached_tracks WHERE id = :id")
    suspend fun deleteById(id: String)
}
