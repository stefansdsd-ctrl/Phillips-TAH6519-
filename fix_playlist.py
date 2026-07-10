import re

content = open("/app/applet/app/src/main/java/com/example/ui/HeadphoneViewModel.kt").read()

content = content.replace("val currentTrackIndex = MutableStateFlow(0).asStateFlow()", "val currentTrackIndex = MutableStateFlow(0)")
content = content.replace("fun playTrack(index: Int) {}", "fun playTrack(index: Int) { currentTrackIndex.value = index; playMediaPlayer() }")
content = content.replace("fun playNextTrack() {}", "fun playNextTrack() { currentTrackIndex.value = (currentTrackIndex.value + 1) % playlist.size; playMediaPlayer() }")
content = content.replace("fun playPreviousTrack() {}", "fun playPreviousTrack() { currentTrackIndex.value = if (currentTrackIndex.value - 1 < 0) playlist.size - 1 else currentTrackIndex.value - 1; playMediaPlayer() }")

with open("/app/applet/app/src/main/java/com/example/ui/HeadphoneViewModel.kt", "w") as f:
    f.write(content)
