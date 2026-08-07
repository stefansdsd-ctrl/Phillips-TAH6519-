awk '/data class PipedSearchItem/ {
    print "data class PipedSearchResponse("
    print "    val items: List<PipedSearchItem>?"
    print ")"
    print ""
    print $0
    next
}
/suspend fun searchMusic/ {
    print $0
    in_skip = 1
    next
}
/): List<PipedSearchItem>/ {
    print "    ): PipedSearchResponse"
    in_skip = 0
    next
}
{
    if (!in_skip) print $0
}
' app/src/main/java/com/example/api/YouTubeApi.kt > tmp.kt
mv tmp.kt app/src/main/java/com/example/api/YouTubeApi.kt

awk '/val results = piped.searchMusic/ {
    print "                            val response = piped.searchMusic(query = rawInput)"
    print "                            val results = response.items ?: emptyList()"
    next
}
{ print $0 }' app/src/main/java/com/example/ui/HeadphoneViewModel.kt > tmp2.kt
mv tmp2.kt app/src/main/java/com/example/ui/HeadphoneViewModel.kt
