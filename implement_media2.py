import re

content = open("/app/applet/app/src/main/java/com/example/ui/HeadphoneViewModel.kt").read()

media_impl = """
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var audioFocusRequest: Any? = null

    fun playTrack(index: Int) {
        if (index in playlist.indices) {
            // we can't directly assign to StateFlow, but wait, it's a StateFlow
            // wait, currentTrackIndex is an asStateFlow() from a MutableStateFlow, so we can't directly assign.
        }
    }

    private fun requestExclusiveAudioFocus() {
        try {
            val audioManager = getApplication<Application>().getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val focusRequest = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { focusChange ->
                        if (focusChange == android.media.AudioManager.AUDIOFOCUS_LOSS) {
                            pauseMediaPlayer()
                        }
                    }
                    .build()
                audioFocusRequest = focusRequest
                audioManager.requestAudioFocus(focusRequest)
            } else {
                audioManager.requestAudioFocus(
                    { focusChange ->
                        if (focusChange == android.media.AudioManager.AUDIOFOCUS_LOSS) {
                            pauseMediaPlayer()
                        }
                    },
                    android.media.AudioManager.STREAM_MUSIC,
                    android.media.AudioManager.AUDIOFOCUS_GAIN
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun abandonExclusiveAudioFocus() {
        try {
            val audioManager = getApplication<Application>().getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                (audioFocusRequest as? android.media.AudioFocusRequest)?.let { req ->
                    audioManager.abandonAudioFocusRequest(req)
                }
            } else {
                audioManager.abandonAudioFocus(null)
            }
        } catch (e: Exception) {}
    }
"""

content = content.replace("}", media_impl + "\n}")
content = content.replace("fun playMediaPlayer(vararg args: Any?) {}", "fun playMediaPlayer(vararg args: Any?) { requestExclusiveAudioFocus(); _mediaIsPlaying.value = true }")
content = content.replace("fun pauseMediaPlayer(vararg args: Any?) {}", "fun pauseMediaPlayer(vararg args: Any?) { abandonExclusiveAudioFocus(); _mediaIsPlaying.value = false }")
content = content.replace("fun stopMediaPlayer(vararg args: Any?) {}", "fun stopMediaPlayer(vararg args: Any?) { abandonExclusiveAudioFocus(); _mediaIsPlaying.value = false }")
content = content.replace("fun toggleMediaPlayer(vararg args: Any?) {}", "fun toggleMediaPlayer(vararg args: Any?) { if (_mediaIsPlaying.value) pauseMediaPlayer() else playMediaPlayer() }")

with open("/app/applet/app/src/main/java/com/example/ui/HeadphoneViewModel.kt", "w") as f:
    f.write(content)
