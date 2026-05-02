package com.example.progetto.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.progetto.data.PlaylistResponse
import com.example.progetto.data.SongResponse
import com.example.progetto.data.RetrofitClient
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ListeningViewModel : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _playlists = MutableStateFlow<List<PlaylistResponse>>(emptyList())
    val playlists: StateFlow<List<PlaylistResponse>> = _playlists

    private val _songs = MutableStateFlow<List<SongResponse>>(emptyList())
    val songs: StateFlow<List<SongResponse>> = _songs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        // Carica tutte le playlist all'avvio
        searchPlaylists("")
        
        // Setup della ricerca reattiva
        setupSearch()
    }

    @OptIn(FlowPreview::class)
    private fun setupSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(500) // Aspetta 500ms dopo l'ultimo input
                .distinctUntilChanged()
                .collect { query ->
                    searchAll(query)
                }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun searchAll(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Ricerca Playlist
                val playlistResponse = RetrofitClient.playlistApiService.getPlaylists(query)
                if (playlistResponse.isSuccessful) {
                    _playlists.value = playlistResponse.body()?.playlists ?: emptyList()
                }

                // Ricerca Canzoni
                val songResponse = RetrofitClient.playlistApiService.getSongs(query)
                if (songResponse.isSuccessful) {
                    _songs.value = songResponse.body()?.songs ?: emptyList()
                }
            } catch (e: Exception) {
                // In caso di errore svuotiamo le liste
                _playlists.value = emptyList()
                _songs.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Mantengo per retrocompatibilità se usato altrove, ma reindirizzo a searchAll
    fun searchPlaylists(query: String) {
        searchAll(query)
    }
}
