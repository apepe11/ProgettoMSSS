package com.example.progetto.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.progetto.data.RetrofitClient
import com.example.progetto.utils.SensorAvailability
import com.example.progetto.utils.SongData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmotionAnalysisViewModel(application: Application) : AndroidViewModel(application) {
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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var shuffleQueue = mutableListOf<SongData>()

    private fun buildShuffleQueue(songs: List<SongData>, excludeSongId: String?): MutableList<SongData> {
        val selectableSongs = songs.filter { it.song_id != excludeSongId }
        val queue = if (selectableSongs.isNotEmpty()) selectableSongs.shuffled() else songs.shuffled()
        return queue.toMutableList()
    }

    private fun nextShuffledSong(songs: List<SongData>, previousSongId: String?): SongData {
        if (shuffleQueue.isEmpty()) {
            shuffleQueue = buildShuffleQueue(songs, previousSongId)
        }
        if (shuffleQueue.isEmpty()) {
            // Fallback if somehow no songs were available in the queue
            shuffleQueue = songs.shuffled().toMutableList()
        }
        return shuffleQueue.removeAt(0)
    }

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

    fun setCurrentEmotion(emotion: String) {
        _currentEmotion.value = emotion
    }

    fun loadRandomSong() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            if (!_sensorsAvailable.value) {
                Log.w("EmotionAnalysisVM", "Emotion sensors not available, continuing with fallback analysis")
            }
            
            try {
                val songsResponse = RetrofitClient.playlistApiService.getSongs()
                val songs = songsResponse.body()?.songs.orEmpty()
                if (songs.isNotEmpty()) {
                    val previousSongId = _currentSong.value?.song_id
                    val nextSong = nextShuffledSong(songs.map {
                        SongData(
                            song_id = it.songId,
                            title = it.title,
                            artist = it.artist,
                            album = "",
                            url = it.url
                        )
                    }, previousSongId)
                    _currentSong.value = nextSong
                    _currentEmotion.value = "Analyzing..."
                } else {
                    Log.w("EmotionAnalysisVM", "Songs endpoint empty, trying top songs")
                    val currentSongId = _currentSong.value?.song_id
                    val topSongs = RetrofitClient.playlistApiService.getTopSongs().body()?.songs.orEmpty()
                    val topSongData = topSongs.map {
                        SongData(
                            song_id = it.songId,
                            title = it.title,
                            artist = it.artist,
                            album = "",
                            url = it.url
                        )
                    }
                    val selectedSong = if (topSongData.isNotEmpty()) {
                        nextShuffledSong(topSongData, currentSongId)
                    } else {
                        fallbackSong
                    }
                    _currentSong.value = selectedSong
                    _error.value = null
                    _currentEmotion.value = "Analyzing..."
                }
            } catch (e: Exception) {
                Log.e("EmotionAnalysisVM", "Error loading song: ${e.message}")
                _error.value = "Failed to load song"
                _currentSong.value = fallbackSong
                _currentEmotion.value = "Unable to analyze"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
