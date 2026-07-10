#!/bin/bash
awk '
/fun pauseMediaPlayer\(\)/ {
    print $0
    print "        abandonExclusiveAudioFocus()"
    next
}
/fun stopMediaPlayer\(\)/ {
    print $0
    print "        abandonExclusiveAudioFocus()"
    next
}
{ print $0 }
' app/src/main/java/com/example/ui/HeadphoneViewModel.kt > tmp.kt
mv tmp.kt app/src/main/java/com/example/ui/HeadphoneViewModel.kt
