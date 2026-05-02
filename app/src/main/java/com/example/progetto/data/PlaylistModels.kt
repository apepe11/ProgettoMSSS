package com.example.progetto.data

import com.google.gson.annotations.SerializedName

/**
 * Modelli per le Playlist (Listening Mode)
 */
data class PlaylistResponse(
    @SerializedName("playlist_id") val playlistId: String,
    val title: String,
    val emotion: String
)

data class PlaylistSearchResponse(
    val playlists: List<PlaylistResponse>
)

data class SongSearchResponse(
    val songs: List<SongResponse>
)

data class SongResponse(
    @SerializedName("song_id") val songId: String,
    val title: String,
    val artist: String,
    val url: String,
    val duration: Int? = null,
    val likes: Int? = 0
)

data class PlaylistDetailResponse(
    @SerializedName("playlist_id") val playlistId: String,
    val title: String,
    val emotion: String,
    val songs: List<SongResponse>
)
