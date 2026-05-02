package com.example.progetto.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.progetto.data.RetrofitClient
import com.example.progetto.data.SongResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TopSongsViewModel : ViewModel() {

    private val _topSongs = MutableStateFlow<List<SongResponse>>(emptyList())
    val topSongs: StateFlow<List<SongResponse>> = _topSongs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    init {
        loadTopSongs()
    }

    fun loadTopSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Chiamiamo l'endpoint specifico per le Top Songs
                val response = RetrofitClient.playlistApiService.getTopSongs()
                if (response.isSuccessful) {
                    // Filtriamo le canzoni che hanno 0 like o nullo
                    val allSongs = response.body()?.songs ?: emptyList()
                    _topSongs.value = allSongs.filter { (it.likes ?: 0) > 0 }
                }
            } catch (e: Exception) {
                _topSongs.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        // Logica di filtraggio locale basata sulla ricerca, mantenendo solo quelle con > 0 like
        viewModelScope.launch {
             // In una implementazione reale, potresti voler rifare la chiamata o filtrare la lista corrente
        }
    }
}
