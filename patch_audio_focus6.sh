#!/bin/bash
awk '
/val focusRequest = AudioFocusRequest.Builder/ {
    print "                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)"
    print "                    .setAudioAttributes("
    print "                        android.media.AudioAttributes.Builder()"
    print "                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)"
    print "                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)"
    print "                            .build()"
    print "                    )"
    print "                    .setAcceptsDelayedFocusGain(true)"
    print "                    .setOnAudioFocusChangeListener {"
    print "                        pauseMediaPlayer()"
    print "                    }"
    print "                    .build()"
    print "                audioFocusRequest = focusRequest"
    print "                audioManager.requestAudioFocus(focusRequest)"
    in_skip = 1
    next
}
/audioManager.requestAudioFocus\(focusRequest\)/ {
    in_skip = 0
    next
}
{
    if (!in_skip) print $0
}
' app/src/main/java/com/example/ui/HeadphoneViewModel.kt > tmp.kt
mv tmp.kt app/src/main/java/com/example/ui/HeadphoneViewModel.kt
