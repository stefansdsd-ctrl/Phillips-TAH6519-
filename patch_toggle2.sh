awk '/fun toggleMediaPlayer/ {
    print $0
    print "        val nextPlayingState = !mediaIsPlaying.value"
    print "        mediaIsPlaying.value = nextPlayingState"
    print "        if (!isYoutubeActive.value) {"
    print "            if (nextPlayingState) {"
    print "                exoPlayerController.play()"
    print "            } else {"
    print "                exoPlayerController.pause()"
    print "            }"
    print "        }"
    in_skip = 1
    next
}
/val nextPlayingState = !mediaIsPlaying.value/ { if(in_skip) next }
/mediaIsPlaying.value = nextPlayingState/ { if(in_skip) next }
/if \(nextPlayingState\)/ { if(in_skip) next }
/exoPlayerController.play/ { if(in_skip) next }
/} else {/ { if(in_skip) next }
/exoPlayerController.pause/ { if(in_skip) next }
/}/ {
    if(in_skip) {
        in_skip = 0
        next
    }
}
{ print $0 }' app/src/main/java/com/example/ui/HeadphoneViewModel.kt > tmp.kt
mv tmp.kt app/src/main/java/com/example/ui/HeadphoneViewModel.kt
