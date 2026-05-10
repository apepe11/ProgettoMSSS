package com.example.progetto.data.repositories

import com.example.progetto.data.*
import retrofit2.Response

/**
 * Repository handling Playlist and Song data operations.
 */
class PlaylistRepository {
    private val playlistApi = RetrofitClient.playlistApiService

    suspend fun getPlaylists(query: String = ""): Response<PlaylistResponse> {
        return playlistApi.getPlaylists(query)
    }

    suspend fun getPlaylistDetails(playlistId: String): Response<PlaylistDetailResponse> {
        return playlistApi.getPlaylistDetails(playlistId)
    }

    suspend fun getSongs(query: String = ""): Response<SongsResponse> {
        return playlistApi.getSongs(query)
    }

    suspend fun getTopSongs(): Response<SongsResponse> {
        return playlistApi.getTopSongs()
    }

    suspend fun toggleFavorite(songId: String, body: Map<String, String>): Response<Map<String, Any>> {
        return playlistApi.toggleFavorite(songId, body)
    }

    suspend fun getFavoriteSongs(userId: String): Response<List<SongItem>> {
        return playlistApi.getFavoriteSongs(userId)
    }
    
    suspend fun checkFavorite(songId: String, userId: String): Response<Map<String, Boolean>> {
        return playlistApi.checkFavorite(songId, userId)
    }
}