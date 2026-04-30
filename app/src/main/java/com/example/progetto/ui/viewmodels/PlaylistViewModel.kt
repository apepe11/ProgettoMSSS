package com.example.progetto.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.progetto.data.PlaylistDetailResponse
import com.example.progetto.data.RetrofitClient
import com.example.progetto.data.SongResponse
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PlaylistViewModel : ViewModel() {

    private val _playlistDetail = MutableStateFlow<PlaylistDetailResponse?>(null)
    val playlistDetail: StateFlow<PlaylistDetailResponse?> = _playlistDetail

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _filteredSongs = MutableStateFlow<List<SongResponse>>(emptyList())
    val filteredSongs: StateFlow<List<SongResponse>> = _filteredSongs

    fun loadPlaylistDetails(playlistId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.playlistApiService.getPlaylistDetails(playlistId)
                if (response.isSuccessful) {
                    _playlistDetail.value = response.body()
                    _filteredSongs.value = response.body()?.songs ?: emptyList()
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        filterSongs(query)
    }

    private fun filterSongs(query: String) {
        val allSongs = _playlistDetail.value?.songs ?: emptyList()
        if (query.isBlank()) {
            _filteredSongs.value = allSongs
        } else {
            _filteredSongs.value = allSongs.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.artist.contains(query, ignoreCase = true)
            }
        }
    }
}
