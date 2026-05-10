package com.example.progetto.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.progetto.data.repositories.PlaylistRepository
import com.example.progetto.utils.SensorAvailability
import com.example.progetto.utils.SongData
import com.example.progetto.utils.UiText
import com.example.progetto.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmotionAnalysisViewModel(application: Application) : AndroidViewModel(application) {
    private val playlistRepository = PlaylistRepository()
    private val _sensorsAvailable = MutableStateFlow(false)
    val sensorsAvailable: StateFlow<Boolean> = _sensorsAvailable.asStateFlow()
    private val fallbackSong = SongData(
        song_id = "a5de389e-cb69-4eae-bf4f-5715ee4d7f5b",
        title = "Bulldog Down in Sunny Tennessee",
        artist = "Charlie Poole",
        album = "",
        url = "/static/music/MT0000004637.mp3"
    )

    private val _currentSong = MutableStateFlow<SongData?>(null)
    val currentSong: StateFlow<SongData?> = _currentSong.asStateFlow()

    private val _currentEmotion = MutableStateFlow("Analyzing...")
    val currentEmotion: StateFlow<String> = _currentEmotion.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<UiText?>(null)
    val error: StateFlow<UiText?> = _error.asStateFlow()

    private val emotionsList = listOf("Happy", "Sad", "Energetic", "Calm", "Anxious", "Peaceful", "Motivated", "Melancholic")

    init {
        refreshSensorAvailability()

        viewModelScope.launch {
            var lastAvailability = _sensorsAvailable.value
            while (true) {
                refreshSensorAvailability()
                val currentAvailability = _sensorsAvailable.value
                if (currentAvailability && !lastAvailability && _currentSong.value == null) {
                    loadRandomSong()
                }
                lastAvailability = currentAvailability
                delay(3000)
            }
        }

        loadRandomSong()
    }

    private fun refreshSensorAvailability() {
        _sensorsAvailable.value = SensorAvailability.hasEmotionSensors(getApplication())
    }

    fun loadRandomSong() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val songsResponse = playlistRepository.getSongs()
                val songs = songsResponse.body()?.songs.orEmpty()
                if (songs.isNotEmpty()) {
                    val randomSong = songs.random()
                    _currentSong.value = SongData(
                        randomSong.songId,
                        randomSong.title,
                        randomSong.artist,
                        "",
                        randomSong.url
                    )
                    _currentEmotion.value = emotionsList.random()
                } else {
                    val topSongs = playlistRepository.getTopSongs().body()?.songs.orEmpty()
                    val selectedSong = topSongs.randomOrNull()?.let {
                        SongData(it.songId, it.title, it.artist, "", it.url)
                    } ?: fallbackSong
                    _currentSong.value = selectedSong
                    _currentEmotion.value = emotionsList.random()
                }
            } catch (e: Exception) {
                _error.value = UiText.StringResource(R.string.error_load_song)
                _currentSong.value = fallbackSong
                _currentEmotion.value = emotionsList.random()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
