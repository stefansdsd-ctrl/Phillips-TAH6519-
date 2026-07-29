package com.example.media

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MediaQueueItem(
    val id: String,
    val title: String,
    val artist: String,
    val durationSecs: Int,
    val artworkUrl: String? = null,
    val isYoutube: Boolean = false,
    val youtubeId: String? = null
)

/**
 * Media3 ExoPlayer & MediaSession Controller for Philips TAH6519 Companion App.
 * Manages audio streaming, MediaSession binding, and hardware Bluetooth controls.
 */
class ExoPlayerController private constructor(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentTrackTitle = MutableStateFlow("Geen nummer geselecteerd")
    val currentTrackTitle: StateFlow<String> = _currentTrackTitle

    private val _currentArtist = MutableStateFlow("Philips TAH6519 Studio")
    val currentArtist: StateFlow<String> = _currentArtist

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs

    private val _durationMs = MutableStateFlow(240000L)
    val durationMs: StateFlow<Long> = _durationMs

    private val _isMedia3SessionActive = MutableStateFlow(false)
    val isMedia3SessionActive: StateFlow<Boolean> = _isMedia3SessionActive

    private val _queue = MutableStateFlow<List<MediaQueueItem>>(emptyList())
    val queue: StateFlow<List<MediaQueueItem>> = _queue

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressTrackerJob: Job? = null

    init {
        initializePlayer()
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            val player = try {
                ExoPlayer.Builder(context)
                    .setAudioAttributes(audioAttributes, true)
                    .setHandleAudioBecomingNoisy(true)
                    .build()
            } catch (e: Throwable) {
                // Fallback for JVM Robolectric test environment
                ExoPlayer.Builder(context).build()
            }

            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                    _isPlaying.value = isPlayingNow
                    if (isPlayingNow) {
                        startProgressTracker()
                    } else {
                        stopProgressTracker()
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    mediaItem?.mediaMetadata?.let { meta ->
                        meta.title?.let { _currentTrackTitle.value = it.toString() }
                        meta.artist?.let { _currentArtist.value = it.toString() }
                    }
                    val index = player.currentMediaItemIndex
                    if (index in _queue.value.indices) {
                        _currentIndex.value = index
                        val item = _queue.value[index]
                        _currentTrackTitle.value = item.title
                        _currentArtist.value = item.artist
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        exoPlayer?.let { p ->
                            if (p.duration > 0) {
                                _durationMs.value = p.duration
                            }
                        }
                    }
                }
            })

            exoPlayer = player

            // Build MediaSession for hardware Bluetooth buttons (Philips TAH6519) and system media controls
            val session = MediaSession.Builder(context, player)
                .setId("Tah6519MediaSession_${System.currentTimeMillis()}")
                .build()

            mediaSession = session
            _isMedia3SessionActive.value = true
        } catch (e: Exception) {
            e.printStackTrace()
            _isMedia3SessionActive.value = false
        }
    }

    fun updateQueue(items: List<MediaQueueItem>, startIndex: Int = 0) {
        _queue.value = items
        val validIndex = startIndex.coerceIn(0, maxOf(0, items.size - 1))
        _currentIndex.value = validIndex

        val mediaItems = items.map { item ->
            val metadata = MediaMetadata.Builder()
                .setTitle(item.title)
                .setArtist(item.artist)
                .setDisplayTitle(item.title)
                .apply {
                    item.artworkUrl?.let { setArtworkUri(Uri.parse(it)) }
                }
                .build()

            val uriToPlay = when {
                !item.youtubeId.isNullOrBlank() -> Uri.parse("https://www.youtube.com/watch?v=${item.youtubeId}")
                else -> Uri.parse("asset:///sample_audio.mp3")
            }

            MediaItem.Builder()
                .setMediaId(item.id)
                .setUri(uriToPlay)
                .setMediaMetadata(metadata)
                .build()
        }

        exoPlayer?.let { player ->
            player.setMediaItems(mediaItems, validIndex, 0L)
            player.prepare()
            if (items.isNotEmpty()) {
                val current = items[validIndex]
                _currentTrackTitle.value = current.title
                _currentArtist.value = current.artist
                _durationMs.value = current.durationSecs * 1000L
            }
        }
    }

    fun skipToQueueItem(index: Int) {
        if (index in _queue.value.indices) {
            _currentIndex.value = index
            val item = _queue.value[index]
            _currentTrackTitle.value = item.title
            _currentArtist.value = item.artist
            _durationMs.value = item.durationSecs * 1000L

            exoPlayer?.let { player ->
                if (index < player.mediaItemCount) {
                    player.seekToDefaultPosition(index)
                    player.playWhenReady = true
                    _isPlaying.value = true
                }
            }
        }
    }

    fun playNext() {
        val nextIdx = _currentIndex.value + 1
        if (nextIdx in _queue.value.indices) {
            skipToQueueItem(nextIdx)
        }
    }

    fun playPrevious() {
        val prevIdx = _currentIndex.value - 1
        if (prevIdx in _queue.value.indices) {
            skipToQueueItem(prevIdx)
        }
    }

    fun removeQueueItem(index: Int) {
        val currentList = _queue.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _queue.value = currentList
            exoPlayer?.removeMediaItem(index)
            if (_currentIndex.value >= currentList.size) {
                _currentIndex.value = maxOf(0, currentList.size - 1)
            }
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
        _currentIndex.value = 0
        exoPlayer?.clearMediaItems()
        _currentTrackTitle.value = "Geen nummer geselecteerd"
        _currentArtist.value = "Philips TAH6519 Studio"
    }

    fun playTrack(
        title: String,
        artist: String,
        streamUrl: String? = null,
        youtubeId: String? = null,
        artworkUrl: String? = null
    ) {
        _currentTrackTitle.value = title
        _currentArtist.value = artist

        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setDisplayTitle(title)

        artworkUrl?.let { metadataBuilder.setArtworkUri(Uri.parse(it)) }
        val metadata = metadataBuilder.build()

        val uriToPlay = when {
            !streamUrl.isNullOrBlank() -> Uri.parse(streamUrl)
            !youtubeId.isNullOrBlank() -> Uri.parse("https://www.youtube.com/watch?v=$youtubeId")
            else -> Uri.parse("asset:///sample_audio.mp3")
        }

        val mediaItem = MediaItem.Builder()
            .setUri(uriToPlay)
            .setMediaMetadata(metadata)
            .build()

        exoPlayer?.let { player ->
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
            _isPlaying.value = true
        }
    }

    fun play() {
        exoPlayer?.play()
        _isPlaying.value = true
    }

    fun pause() {
        exoPlayer?.pause()
        _isPlaying.value = false
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
            } else {
                player.play()
                _isPlaying.value = true
            }
        } ?: run {
            _isPlaying.value = !_isPlaying.value
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _positionMs.value = positionMs
    }

    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume.coerceIn(0f, 1f)
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressTrackerJob = scope.launch {
            while (_isPlaying.value) {
                exoPlayer?.let { player ->
                    _positionMs.value = player.currentPosition
                    if (player.duration > 0) {
                        _durationMs.value = player.duration
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = null
    }

    fun release() {
        stopProgressTracker()
        try {
            mediaSession?.release()
            mediaSession = null
            exoPlayer?.release()
            exoPlayer = null
            _isMedia3SessionActive.value = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ExoPlayerController? = null

        fun getInstance(context: Context): ExoPlayerController {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ExoPlayerController(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
