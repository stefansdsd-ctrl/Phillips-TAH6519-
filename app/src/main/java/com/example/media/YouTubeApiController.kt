package com.example.media

import android.content.Context
import com.example.api.YouTubeRssFetcher
import com.example.ui.YouTubeTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * YouTube API Controller for audio streaming, search, and Media3 MediaSession synchronization.
 */
class YouTubeApiController(private val context: Context) {

    private val exoPlayerController = ExoPlayerController.getInstance(context)

    private val _isStreamingActive = MutableStateFlow(false)
    val isStreamingActive: StateFlow<Boolean> = _isStreamingActive

    private val _currentStreamUrl = MutableStateFlow("")
    val currentStreamUrl: StateFlow<String> = _currentStreamUrl

    private val _youtubeSearchTracks = MutableStateFlow<List<YouTubeTrack>>(emptyList())
    val youtubeSearchTracks: StateFlow<List<YouTubeTrack>> = _youtubeSearchTracks

    /**
     * Prepares and streams audio track from YouTube using Media3 ExoPlayer & MediaSession
     */
    fun streamYouTubeAudioTrack(
        youtubeId: String,
        title: String,
        artist: String,
        durationSecs: Int = 240
    ) {
        val streamUrl = getAudioStreamUrl(youtubeId)
        _currentStreamUrl.value = streamUrl
        _isStreamingActive.value = true

        val artworkUrl = "https://img.youtube.com/vi/$youtubeId/hqdefault.jpg"

        exoPlayerController.playTrack(
            title = title,
            artist = artist,
            streamUrl = streamUrl,
            youtubeId = youtubeId,
            artworkUrl = artworkUrl
        )
    }

    /**
     * Resolves YouTube video ID into an audio stream URL
     */
    fun getAudioStreamUrl(youtubeId: String): String {
        return "https://www.youtube.com/embed/$youtubeId?enablejsapi=1&autoplay=1&playsinline=1"
    }

    /**
     * Perform YouTube API query to search for audio streams
     */
    suspend fun searchYouTubeAudio(playlistId: String): List<YouTubeTrack> = withContext(Dispatchers.IO) {
        try {
            val results = YouTubeRssFetcher.fetchPlaylistTracks(playlistId)
            _youtubeSearchTracks.value = results
            results
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Syncs YouTube player events (play, pause, skip) with Media3 ExoPlayer session
     */
    fun syncPlaybackState(isPlaying: Boolean, title: String, artist: String) {
        if (isPlaying) {
            exoPlayerController.play()
        } else {
            exoPlayerController.pause()
        }
    }

    /**
     * Toggles Media3 ExoPlayer & MediaSession play/pause
     */
    fun togglePlayPause() {
        exoPlayerController.togglePlayPause()
    }

    companion object {
        @Volatile
        private var INSTANCE: YouTubeApiController? = null

        fun getInstance(context: Context): YouTubeApiController {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: YouTubeApiController(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
