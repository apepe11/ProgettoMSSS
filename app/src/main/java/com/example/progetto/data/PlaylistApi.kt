package com.example.progetto.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

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
}
