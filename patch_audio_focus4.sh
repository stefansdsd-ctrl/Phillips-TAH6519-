#!/bin/bash
awk '
/stopMediaPlayer\(\)/ {
    if (in_proc) {
        print "        requestExclusiveAudioFocus()"
        in_proc = 0
    }
}
/fun playProceduralTone\(\)/ {
    in_proc = 1
}
{ print $0 }
' app/src/main/java/com/example/ui/HeadphoneViewModel.kt > tmp.kt
mv tmp.kt app/src/main/java/com/example/ui/HeadphoneViewModel.kt
