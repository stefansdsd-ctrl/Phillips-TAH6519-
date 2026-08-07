awk '/val tagKey =/ {
    print "            if (webView.tag == null) {"
    print "                webView.tag = \"\""
    print "            }"
    print "            val currentTag = webView.tag as? String ?: \"\""
    print "            val parts = currentTag.split(\":\")"
    print "            val currentVideoId = parts.getOrNull(0) ?: \"\""
    print "            val currentPlayingState = parts.getOrNull(1) == \"play\""
    print "            "
    print "            if (currentVideoId != youtubeId) {"
    print "                webView.tag = \"$youtubeId:${if (isPlaying) \"play\" else \"pause\"}\""
    print "                val autoplayParam = if (isPlaying) 1 else 0"
    in_skip = 1
    next
}
/if \(currentVideoId != youtubeId\) \{/ {
    in_skip = 1
    next
}
/webView\.loadDataWithBaseURL/ {
    if (in_skip) {
        print $0
        print "            } else if (currentPlayingState != isPlaying) {"
        print "                webView.tag = \"$youtubeId:${if (isPlaying) \"play\" else \"pause\"}\""
        print "                if (isPlaying) {"
        print "                    webView.evaluateJavascript(\"play();\", null)"
        print "                } else {"
        print "                    webView.evaluateJavascript(\"pause();\", null)"
        print "                }"
        print "            }"
        in_skip = 0
        next
    }
}
{
    if (!in_skip) print $0
}
' app/src/main/java/com/example/ui/YouTubePlayer.kt > tmp.kt
mv tmp.kt app/src/main/java/com/example/ui/YouTubePlayer.kt
