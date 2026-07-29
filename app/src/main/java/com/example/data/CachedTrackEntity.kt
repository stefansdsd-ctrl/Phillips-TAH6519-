package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_tracks")
data class CachedTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val durationSecs: Int,
    val artworkUrl: String? = null,
    val isYoutube: Boolean = false,
    val youtubeId: String? = null,
    val orderIndex: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val playlistSource: String = "NOW_PLAYING"
)
