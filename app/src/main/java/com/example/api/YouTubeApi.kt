package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

@JsonClass(generateAdapter = true)
data class YouTubePlaylistResponse(
    val items: List<YouTubePlaylistItem>?
)

@JsonClass(generateAdapter = true)
data class YouTubePlaylistItem(
    val snippet: YouTubeSnippet?
)

@JsonClass(generateAdapter = true)
data class YouTubeSnippet(
    val title: String?,
    val description: String?,
    val resourceId: YouTubeResourceId?,
    val videoOwnerChannelTitle: String?,
    val channelTitle: String?
)

@JsonClass(generateAdapter = true)
data class YouTubeResourceId(
    val videoId: String?
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchResponse(
    val items: List<YouTubeSearchResultItem>?
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchResultItem(
    val id: YouTubeSearchResultId?,
    val snippet: YouTubeSnippet?
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchResultId(
    val kind: String?,
    val videoId: String?
)

@JsonClass(generateAdapter = true)
data class YouTubeOEmbedResponse(
    val title: String?,
    @Json(name = "author_name") val authorName: String?,
    @Json(name = "provider_name") val providerName: String?,
    @Json(name = "thumbnail_url") val thumbnailUrl: String?
)

@JsonClass(generateAdapter = true)
data class PipedSearchResponse(
    val items: List<PipedSearchItem>?
)

data class PipedSearchItem(
    val url: String?,
    val title: String?,
    val uploaderName: String?,
    val duration: Int?
)

interface YouTubeApi {
    @GET("playlistItems")
    suspend fun getPlaylistItems(
        @Query("part") part: String = "snippet",
        @Query("playlistId") playlistId: String,
        @Query("maxResults") maxResults: Int = 50,
        @Query("key") apiKey: String
    ): YouTubePlaylistResponse

    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("type") type: String = "video",
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 25,
        @Query("key") apiKey: String
    ): YouTubeSearchResponse

    @GET
    suspend fun getOEmbed(
        @Url url: String = "https://www.youtube.com/oembed",
        @Query("url") videoUrl: String,
        @Query("format") format: String = "json"
    ): YouTubeOEmbedResponse

    companion object {
        private const val BASE_URL = "https://www.googleapis.com/youtube/v3/"

        fun create(): YouTubeApi {
            val retrofit = retrofit2.Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create())
                .build()
            return retrofit.create(YouTubeApi::class.java)
        }
    }
}

interface PipedApi {
    @GET("search")
    suspend fun searchMusic(
        @Query("q") query: String,
        @Query("filter") filter: String = "music_songs"
    ): PipedSearchResponse

    companion object {
        private const val BASE_URL = "https://pipedapi.kavin.rocks/"

        fun create(): PipedApi {
            val retrofit = retrofit2.Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create())
                .build()
            return retrofit.create(PipedApi::class.java)
        }
    }
}

object YouTubeRssFetcher {
    suspend fun fetchPlaylistTracks(playlistId: String): List<com.example.ui.YouTubeTrack> = withContext(Dispatchers.IO) {
        try {
            val cleanListId = playlistId.replace("playlist?list=", "").replace("list=", "").trim()
            val urlString = "https://www.youtube.com/feeds/videos.xml?playlist_id=$cleanListId"
            val connection = java.net.URL(urlString).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")

            if (connection.responseCode == 200) {
                val xml = connection.inputStream.bufferedReader().use { it.readText() }
                val tracks = mutableListOf<com.example.ui.YouTubeTrack>()

                val entryRegex = Regex("<entry>(.*?)</entry>", RegexOption.DOT_MATCHES_ALL)
                val videoIdRegex = Regex("<yt:videoId>(.*?)</yt:videoId>")
                val titleRegex = Regex("<title>(.*?)</title>")
                val nameRegex = Regex("<name>(.*?)</name>")

                entryRegex.findAll(xml).forEach { match ->
                    val entryXml = match.groupValues[1]
                    var vId = videoIdRegex.find(entryXml)?.groupValues?.get(1)?.trim() ?: ""
                    if (vId.contains("&")) vId = vId.substringBefore("&")
                    val rawTitle = titleRegex.find(entryXml)?.groupValues?.get(1)?.trim() ?: "Onbekend Nummer"
                    val rawAuthor = nameRegex.find(entryXml)?.groupValues?.get(1)?.trim() ?: "YouTube Music"

                    val cleanTitle = try {
                        android.text.Html.fromHtml(rawTitle, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
                    } catch (e: Throwable) {
                        rawTitle
                    }
                    val cleanAuthor = try {
                        android.text.Html.fromHtml(rawAuthor, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
                    } catch (e: Throwable) {
                        rawAuthor
                    }

                    if (vId.isNotBlank()) {
                        tracks.add(
                            com.example.ui.YouTubeTrack(
                                youtubeId = vId,
                                title = cleanTitle,
                                artist = cleanAuthor,
                                durationSecs = (180..280).random(),
                                isOffline = true
                            )
                        )
                    }
                }
                tracks
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

