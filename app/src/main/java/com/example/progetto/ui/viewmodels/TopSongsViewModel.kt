package com.example.progetto.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.progetto.data.RetrofitClient
import com.example.progetto.data.SongResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TopSongsViewModel : ViewModel() {

    private val _allTopSongs = MutableStateFlow<List<SongResponse>>(emptyList())
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Combinazione di canzoni e ricerca per filtraggio reattivo
    val topSongs: StateFlow<List<SongResponse>> = combine(_allTopSongs, _searchQuery) { songs, query ->
        if (query.isBlank()) {
            songs
        } else {
            songs.filter { 
                it.title.contains(query, ignoreCase = true) || 
                it.artist.contains(query, ignoreCase = true) 
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadTopSongs()
    }

    fun loadTopSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.playlistApiService.getTopSongs()
                if (response.isSuccessful) {
                    val allSongs = response.body()?.songs ?: emptyList()
                    Log.d("TopSongsViewModel", "Caricate ${allSongs.size} canzoni")
                    // Solo canzoni con più di 0 like
                    _allTopSongs.value = allSongs.filter { (it.likes ?: 0) > 0 }
                } else {
                    Log.e("TopSongsViewModel", "Errore API: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("TopSongsViewModel", "Eccezione caricamento", e)
                _allTopSongs.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
}
