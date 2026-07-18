package com.example.api

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

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
    val videoOwnerChannelTitle: String?
)

@JsonClass(generateAdapter = true)
data class YouTubeResourceId(
    val videoId: String?
)

interface YouTubeApi {
    @GET("playlistItems")
    suspend fun getPlaylistItems(
        @Query("part") part: String = "snippet",
        @Query("playlistId") playlistId: String,
        @Query("maxResults") maxResults: Int = 50,
        @Query("key") apiKey: String
    ): YouTubePlaylistResponse

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
