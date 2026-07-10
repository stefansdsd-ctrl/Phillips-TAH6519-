#!/bin/bash
awk '
/if \(_mediaIsPlaying.value\) return/ {
    print $0
    print "        requestExclusiveAudioFocus()"
    next
}
{ print $0 }
' app/src/main/java/com/example/ui/HeadphoneViewModel.kt > tmp.kt
mv tmp.kt app/src/main/java/com/example/ui/HeadphoneViewModel.kt
