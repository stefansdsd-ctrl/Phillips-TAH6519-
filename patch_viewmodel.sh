awk '/exoPlayerController.pause\(\)/ {
    print $0
    in_skip = 1
    next
}
/youtubeApiController.streamYouTubeAudioTrack/ {
    if (in_skip) {
        # Skip the streamYouTubeAudioTrack call
        next
    }
}
/youtubeId = track.youtubeId/ {
    if (in_skip) next
}
/title = track.title/ {
    if (in_skip) next
}
/artist = track.artist/ {
    if (in_skip) next
}
/durationSecs = track.durationSecs/ {
    if (in_skip) next
}
/)/ {
    if (in_skip) {
        in_skip = 0
        next
    }
}
{ print $0 }' app/src/main/java/com/example/ui/HeadphoneViewModel.kt > tmp.kt
mv tmp.kt app/src/main/java/com/example/ui/HeadphoneViewModel.kt
