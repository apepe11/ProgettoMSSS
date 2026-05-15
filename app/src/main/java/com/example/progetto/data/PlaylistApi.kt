package com.example.progetto.data

import com.example.progetto.utils.InsightsResponse
import retrofit2.Response
import retrofit2.http.*

interface PlaylistApi {

    @GET("/api/playlists")
    suspend fun getPlaylists(
        @Query("q") query: String? = null
    ): Response<PlaylistSearchResponse>

    @GET("/api/playlists/{playlist_id}")
    suspend fun getPlaylistDetails(
        @Path("playlist_id") playlistId: String
    ): Response<PlaylistDetailResponse>

    @GET("/api/songs")
    suspend fun getSongs(
        @Query("q") query: String? = null
    ): Response<SongSearchResponse>

    @GET("/api/songs/top")
    suspend fun getTopSongs(): Response<SongSearchResponse>

    @POST("/api/songs/{song_id}/favorite")
    suspend fun toggleFavorite(
        @Path("song_id") songId: String,
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>

    @GET("/api/songs/{song_id}/favorite/{user_id}")
    suspend fun checkFavorite(
        @Path("song_id") songId: String,
        @Path("user_id") userId: String
    ): Response<Map<String, Boolean>>

    @GET("/api/favorites/{user_id}")
    suspend fun getFavoriteSongs(
        @Path("user_id") userId: String
    ): Response<List<FavoriteSongResponse>>

    @GET("/api/insights/{user_id}")
    suspend fun getInsights(
        @Path("user_id") userId: String
    ): Response<InsightsResponse>
}
