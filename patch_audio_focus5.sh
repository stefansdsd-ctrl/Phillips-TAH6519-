#!/bin/bash
awk '
/private var mediaPlayer: MediaPlayer\? = null/ {
    print "    private var audioFocusRequest: Any? = null"
    print $0
    next
}
/private fun requestExclusiveAudioFocus\(\)/ {
    in_focus = 1
    print $0
    next
}
/fun pauseMediaPlayer\(\)/ {
    print "    private fun abandonExclusiveAudioFocus() {"
    print "        try {"
    print "            val audioManager = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as AudioManager"
    print "            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {"
    print "                (audioFocusRequest as? AudioFocusRequest)?.let { req ->"
    print "                    audioManager.abandonAudioFocusRequest(req)"
    print "                }"
    print "            } else {"
    print "                audioManager.abandonAudioFocus(null)"
    print "            }"
    print "        } catch (e: Exception) {}"
    print "    }"
    print ""
    print $0
    next
}
/mediaPlayer\?\.let { mp ->/ {
    if (in_pause) {
        print "        abandonExclusiveAudioFocus()"
        in_pause = 0
    }
}
/if \(mp\.isPlaying\)/ {
    if (in_pause_inner) {
        // Wait, just insert in `pauseMediaPlayer` and `stopMediaPlayer`
    }
}
{ print $0 }
' app/src/main/java/com/example/ui/HeadphoneViewModel.kt > tmp.kt
mv tmp.kt app/src/main/java/com/example/ui/HeadphoneViewModel.kt
