package com.example.progetto.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.progetto.data.PlaylistResponse
import com.example.progetto.data.RetrofitClient
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ListeningViewModel : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _playlists = MutableStateFlow<List<PlaylistResponse>>(emptyList())
    val playlists: StateFlow<List<PlaylistResponse>> = _playlists

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
                    searchPlaylists(query)
                }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun searchPlaylists(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Ora usiamo il servizio specifico per le playlist
                val response = RetrofitClient.playlistApiService.getPlaylists(query)
                if (response.isSuccessful) {
                    _playlists.value = response.body()?.playlists ?: emptyList()
                } else {
                    _playlists.value = emptyList()
                }
            } catch (e: Exception) {
                _playlists.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
