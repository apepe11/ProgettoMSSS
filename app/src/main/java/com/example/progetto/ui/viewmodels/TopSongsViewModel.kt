package com.example.progetto.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.progetto.utils.NetworkManager
import com.example.progetto.utils.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TopSongsViewModel : ViewModel() {
    // Initialize our network manager
    private val networkManager = NetworkManager()

    // Holds the currently displayed songs (changes when user types in search bar)
    private val _topSongs = MutableStateFlow<List<Song>>(emptyList())
    val topSongs: StateFlow<List<Song>> = _topSongs.asStateFlow()

    // Holds the full list of favorites so we don't lose them when searching
    private var allFavoriteSongs: List<Song> = emptyList()

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

            // Fetch from backend (already sorted newest-to-oldest by our SQL query!)
            val songs = networkManager.getFavoriteSongs(userId)

            if (songs != null) {
                allFavoriteSongs = songs
                _topSongs.value = allFavoriteSongs
            }

            _isLoading.value = false
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
            // Send the unlike request to the database
            val result = networkManager.toggleFavorite(userId, songId)

            // If successful, remove it from the UI instantly
            if (result.isSuccess) {
                allFavoriteSongs = allFavoriteSongs.filter { it.songId != songId }
                onSearchQueryChange(_searchQuery.value) // Re-apply search filter
            }
        }
    }
}