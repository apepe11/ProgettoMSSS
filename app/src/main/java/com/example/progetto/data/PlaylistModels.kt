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
