package com.example.progetto.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface PlaylistApi {

    @GET("/api/playlists/search")
    suspend fun searchPlaylists(
        @Query("q") query: String
    ): Response<PlaylistSearchResponse>
}
