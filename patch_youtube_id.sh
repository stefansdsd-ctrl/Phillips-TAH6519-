awk '/val vId = item.url\?\.replace/ {
    print "                                    var vId = item.url?.replace(\"/watch?v=\", \"\") ?: \"\""
    print "                                    if (vId.contains(\"&\")) vId = vId.substringBefore(\"&\")"
    next
}
{ print $0 }' app/src/main/java/com/example/ui/HeadphoneViewModel.kt > tmp.kt
mv tmp.kt app/src/main/java/com/example/ui/HeadphoneViewModel.kt

awk '/val vId = videoIdRegex.find/ {
    print "                    var vId = videoIdRegex.find(entryXml)?.groupValues?.get(1)?.trim() ?: \"\""
    print "                    if (vId.contains(\"&\")) vId = vId.substringBefore(\"&\")"
    next
}
{ print $0 }' app/src/main/java/com/example/api/YouTubeApi.kt > tmp.kt
mv tmp.kt app/src/main/java/com/example/api/YouTubeApi.kt
