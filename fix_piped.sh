awk '/suspend fun searchMusic/ {
    print "    suspend fun searchMusic("
    print "        @Query(\"q\") query: String,"
    print "        @Query(\"filter\") filter: String = \"music_songs\""
    next
}
{ print $0 }' app/src/main/java/com/example/api/YouTubeApi.kt > tmp.kt
mv tmp.kt app/src/main/java/com/example/api/YouTubeApi.kt
