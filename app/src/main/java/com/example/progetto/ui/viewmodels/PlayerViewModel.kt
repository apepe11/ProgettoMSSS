package com.example.progetto.ui.viewmodels

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlayerViewModel : ViewModel() {

    private val _currentSongTitle = MutableStateFlow("Unknown")
    val currentSongTitle: StateFlow<String> = _currentSongTitle

    private val _currentArtistName = MutableStateFlow("Unknown")
    val currentArtistName: StateFlow<String> = _currentArtistName

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentSongUrl = MutableStateFlow("")
    val currentSongUrl: StateFlow<String> = _currentSongUrl

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var isPreparing = false

    private fun getOrCreateModelPlayer(): MediaPlayer {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnPreparedListener { mp ->
                    Log.d("PlayerViewModel", "Canzone preparata. Durata: ${mp.duration}")
                    isPreparing = false
                    _duration.value = mp.duration
                    mp.start()
                    _isPlaying.value = true
                    startProgressUpdate()
                }
                setOnCompletionListener {
                    Log.d("PlayerViewModel", "Canzone terminata regolarmente")
                    _isPlaying.value = false
                    _currentPosition.value = it.duration
                    stopProgressUpdate()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e("PlayerViewModel", "Errore MediaPlayer: what=$what, extra=$extra")
                    isPreparing = false
                    _isPlaying.value = false
                    stopProgressUpdate()
                    mp.reset()
                    true
                }
            }
        }
        return mediaPlayer!!
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            var lastPos = -1
            var stallCount = 0
            while (true) {
                try {
                    mediaPlayer?.let { mp ->
                        if (mp.isPlaying) {
                            val pos = mp.currentPosition
                            _currentPosition.value = pos
                            
                            // Controllo stallo (se la posizione non cambia per 5 secondi mentre dovrebbe suonare)
                            if (pos == lastPos && pos < _duration.value - 500) {
                                stallCount++
                                if (stallCount >= 5) {
                                    Log.w("PlayerViewModel", "Rilevato stallo della riproduzione, ricarico...")
                                    playSong(_currentSongTitle.value, _currentArtistName.value, _currentSongUrl.value)
                                }
                            } else {
                                stallCount = 0
                            }
                            lastPos = pos
                        }
                    }
                } catch (e: Exception) {
                    // Stato invalido del player, usciamo dal loop
                    break
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
    }

    fun playSong(title: String, artist: String, url: String) {
        Log.d("PlayerViewModel", "playSong richiesta per: $url")
        
        val mp = getOrCreateModelPlayer()
        
        // Se è la stessa canzone, verifichiamo se sta effettivamente suonando
        if (_currentSongUrl.value == url && (_isPlaying.value || isPreparing)) {
            try {
                if (mp.isPlaying) {
                    Log.d("PlayerViewModel", "La canzone è già in riproduzione, salto il caricamento")
                    return
                }
            } catch (e: Exception) {
                // Se isPlaying fallisce, il player è in uno stato che richiede reset
            }
        }

        // Reset stati e caricamento
        _currentSongTitle.value = title
        _currentArtistName.value = artist
        _currentSongUrl.value = url
        _currentPosition.value = 0
        _duration.value = 0
        _isPlaying.value = false
        isPreparing = true
        stopProgressUpdate()

        try {
            mp.reset()
            val baseUrl = "http://10.0.2.2:5000"
            val fullUrl = when {
                url.startsWith("http") -> url
                url.startsWith("/") -> "$baseUrl$url"
                else -> "$baseUrl/song/$url"
            }
            Log.d("PlayerViewModel", "Avvio caricamento DataSource: $fullUrl")
            mp.setDataSource(fullUrl)
            mp.prepareAsync()
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Errore critico in playSong", e)
            isPreparing = false
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        try {
            if (mp.isPlaying) {
                Log.d("PlayerViewModel", "Pausa richiesta")
                mp.pause()
                _isPlaying.value = false
                stopProgressUpdate()
            } else {
                // Verifichiamo se dobbiamo ricaricare (fine canzone o stallo)
                val isAtEnd = _duration.value > 0 && mp.currentPosition >= _duration.value - 1500
                if (isAtEnd || mp.currentPosition == 0 && !isPreparing) {
                    Log.d("PlayerViewModel", "Riavvio canzone (fine o inizio)")
                    playSong(_currentSongTitle.value, _currentArtistName.value, _currentSongUrl.value)
                } else {
                    Log.d("PlayerViewModel", "Ripresa riproduzione")
                    mp.start()
                    _isPlaying.value = true
                    startProgressUpdate()
                }
            }
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Eccezione in togglePlayPause, forzo ricaricamento", e)
            playSong(_currentSongTitle.value, _currentArtistName.value, _currentSongUrl.value)
        }
    }

    fun seekTo(position: Int) {
        mediaPlayer?.let {
            try {
                it.seekTo(position)
                _currentPosition.value = position
            } catch (e: Exception) { }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
        stopProgressUpdate()
    }
}
