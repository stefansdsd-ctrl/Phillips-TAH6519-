#!/bin/bash
# A script to modify HeadphoneViewModel.kt to support playlist

cat app/src/main/java/com/example/ui/HeadphoneViewModel.kt | awk '
BEGIN { in_companion = 0; inserted = 0; }
/^data class / { if (!inserted) { print "data class Track(val id: String, val title: String, val artist: String, val sourceUrl: String, val isOffline: Boolean)"; inserted=1; } }
{ print $0 }
' > tmp.kt
mv tmp.kt app/src/main/java/com/example/ui/HeadphoneViewModel.kt
