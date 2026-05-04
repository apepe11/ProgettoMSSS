package com.example.progetto.ui.viewmodels

import android.util.Log
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
                Log.d("ListeningVM", "Fetching playlists with query='$query'")
                val playlistResponse = RetrofitClient.playlistApiService.getPlaylists(query)
                Log.d("ListeningVM", "Playlist response code: ${playlistResponse.code()}, successful: ${playlistResponse.isSuccessful}")
                if (playlistResponse.isSuccessful) {
                    val playlists = playlistResponse.body()?.playlists ?: emptyList()
                    Log.d("ListeningVM", "Got ${playlists.size} playlists")
                    _playlists.value = playlists
                } else {
                    Log.e("ListeningVM", "Playlist error: ${playlistResponse.errorBody()?.string()}")
                }

                // Ricerca Canzoni
                Log.d("ListeningVM", "Fetching songs with query='$query'")
                val songResponse = RetrofitClient.playlistApiService.getSongs(query)
                Log.d("ListeningVM", "Song response code: ${songResponse.code()}, successful: ${songResponse.isSuccessful}")
                if (songResponse.isSuccessful) {
                    val songs = songResponse.body()?.songs ?: emptyList()
                    Log.d("ListeningVM", "Got ${songs.size} songs")
                    _songs.value = songs
                } else {
                    Log.e("ListeningVM", "Song error: ${songResponse.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("ListeningVM", "Exception during search: ${e.message}", e)
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
