package com.example.progetto.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface SongApi {
    @GET("/api/songs/{song_id}")
    suspend fun getSongDetails(
        @Path("song_id") songId: String
    ): Response<SongResponse>
}
