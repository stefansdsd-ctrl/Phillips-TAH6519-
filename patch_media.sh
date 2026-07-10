#!/bin/bash
awk '
/val playlist = listOf/ {
    print "    private val _playlist = MutableStateFlow(listOf("
    print "        Track(\"1\", \"Let It Happen (Audio Stream)\", \"Tame Impala (SoundHelix Demo)\", \"https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3\", false),"
    print "        Track(\"2\", \"Synthwave Sunset\", \"Neon Night\", \"https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3\", false),"
    print "        Track(\"3\", \"Focus Ambient Noise\", \"Philips Offline\", \"offline_brown_noise\", true),"
    print "        Track(\"4\", \"Lofi Beats\", \"Chillhop\", \"https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3\", false)"
    print "    ))"
    print "    val playlist: StateFlow<List<Track>> = _playlist.asStateFlow()"
    in_skip = 1
    next
}
/private val _currentTrackIndex = MutableStateFlow/ {
    in_skip = 0
}
{
    if (!in_skip) print $0
}
' app/src/main/java/com/example/ui/HeadphoneViewModel.kt > tmp.kt
mv tmp.kt app/src/main/java/com/example/ui/HeadphoneViewModel.kt

awk '
/fun playTrack\(index: Int\)/ {
    print "    fun toggleTrackOfflineStatus(trackId: String) {"
    print "        _playlist.value = _playlist.value.map {"
    print "            if (it.id == trackId) it.copy(isOffline = !it.isOffline) else it"
    print "        }"
    print "    }"
    print ""
    print $0
    next
}
{ print $0 }
' app/src/main/java/com/example/ui/HeadphoneViewModel.kt > tmp.kt
mv tmp.kt app/src/main/java/com/example/ui/HeadphoneViewModel.kt

awk '
/val playlist = playlist.indices/ {
    // Actually we need to fix the playlist references to .value
}
/if \(index in playlist.indices\)/ {
    gsub(/playlist\.indices/, "_playlist.value.indices")
}
/val next = \(_currentTrackIndex.value \+ 1\) % playlist.size/ {
    gsub(/playlist\.size/, "_playlist.value.size")
}
/val prev = if \(_currentTrackIndex.value - 1 < 0\) playlist.size - 1 else _currentTrackIndex.value - 1/ {
    gsub(/playlist\.size/, "_playlist.value.size")
}
/val track = playlist\[_currentTrackIndex.value\]/ {
    gsub(/playlist/, "_playlist.value")
}
{ print $0 }
' app/src/main/java/com/example/ui/HeadphoneViewModel.kt > tmp.kt
mv tmp.kt app/src/main/java/com/example/ui/HeadphoneViewModel.kt

