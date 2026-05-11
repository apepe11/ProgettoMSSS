package com.example.progetto.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.progetto.data.SongResponse // ✅ Import the correct model
import com.example.progetto.data.repositories.PlaylistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TopSongsViewModel : ViewModel() {
    private val playlistRepository = PlaylistRepository()

    // ✅ Changed type from Song to SongResponse
    private val _topSongs = MutableStateFlow<List<SongResponse>>(emptyList())
    val topSongs: StateFlow<List<SongResponse>> = _topSongs.asStateFlow()

    // ✅ Changed type from Song to SongResponse
    private var allFavoriteSongs: List<SongResponse> = emptyList()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * Fetches the user's favorite songs from the database
     */
    fun loadFavoriteSongs(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val response = playlistRepository.getFavoriteSongs(userId)
                if (response.isSuccessful) {
                    // ✅ Simplified mapping: Since Repository returns List<SongResponse>,
                    // we can use the list directly.
                    val songs = response.body() ?: emptyList()
                    allFavoriteSongs = songs
                    _topSongs.value = allFavoriteSongs
                }
            } catch (e: Exception) {
                // Error handling - log e.message here if needed
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Filters the list locally when the user types in the search bar
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _topSongs.value = allFavoriteSongs
        } else {
            _topSongs.value = allFavoriteSongs.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.artist.contains(query, ignoreCase = true)
            }
        }
    }

    /**
     * If they click the red heart on this screen, instantly remove it from the list
     */
    fun toggleFavoriteOff(userId: String, songId: String) {
        viewModelScope.launch {
            try {
                val response = playlistRepository.toggleFavorite(songId, mapOf("user_id" to userId))
                if (response.isSuccessful) {
                    // ✅ Filter using SongResponse fields
                    allFavoriteSongs = allFavoriteSongs.filter { it.songId != songId }
                    onSearchQueryChange(_searchQuery.value)
                }
            } catch (e: Exception) {
                // Error handling
            }
        }
    }
}