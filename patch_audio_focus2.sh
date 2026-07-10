#!/bin/bash
awk '
/fun playMediaPlayer\(\)/ {
    print "    private fun requestExclusiveAudioFocus() {"
    print "        try {"
    print "            val audioManager = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as AudioManager"
    print "            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {"
    print "                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)"
    print "                    .setAudioAttributes("
    print "                        android.media.AudioAttributes.Builder()"
    print "                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)"
    print "                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)"
    print "                            .build()"
    print "                    )"
    print "                    .setAcceptsDelayedFocusGain(true)"
    print "                    .setOnAudioFocusChangeListener { focusChange ->"
    print "                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {"
    print "                            pauseMediaPlayer()"
    print "                        }"
    print "                    }"
    print "                    .build()"
    print "                audioManager.requestAudioFocus(focusRequest)"
    print "            } else {"
    print "                audioManager.requestAudioFocus("
    print "                    { focusChange ->"
    print "                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {"
    print "                            pauseMediaPlayer()"
    print "                        }"
    print "                    },"
    print "                    AudioManager.STREAM_MUSIC,"
    print "                    AudioManager.AUDIOFOCUS_GAIN"
    print "                )"
    print "            }"
    print "        } catch (e: Exception) {"
    print "            e.printStackTrace()"
    print "        }"
    print "    }"
    print ""
    print $0
    next
}
{ print $0 }
' app/src/main/java/com/example/ui/HeadphoneViewModel.kt > tmp.kt
mv tmp.kt app/src/main/java/com/example/ui/HeadphoneViewModel.kt
