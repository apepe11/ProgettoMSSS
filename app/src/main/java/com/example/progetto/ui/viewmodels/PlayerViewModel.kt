package com.example.progetto.ui.viewmodels

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PlayerViewModel : ViewModel() {

    private val _currentSongTitle = MutableStateFlow("Unknown")
    val currentSongTitle: StateFlow<String> = _currentSongTitle

    private val _currentArtistName = MutableStateFlow("Unknown")
    val currentArtistName: StateFlow<String> = _currentArtistName

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentSongUrl = MutableStateFlow("")
    val currentSongUrl: StateFlow<String> = _currentSongUrl

    private var mediaPlayer: MediaPlayer? = null

    init {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnPreparedListener {
                it.start()
                _isPlaying.value = true
            }
            setOnCompletionListener {
                _isPlaying.value = false
            }
            setOnErrorListener { _, what, extra ->
                Log.e("PlayerViewModel", "MediaPlayer error: $what, $extra")
                _isPlaying.value = false
                false
            }
        }
    }

    fun playSong(title: String, artist: String, url: String) {
        // Se è la stessa canzone già in riproduzione o caricata, non resettare nulla.
        if (_currentSongUrl.value == url && url.isNotEmpty()) {
            return
        }

        _currentSongTitle.value = title
        _currentArtistName.value = artist
        _currentSongUrl.value = url

        mediaPlayer?.let { mp ->
            try {
                mp.reset()
                
                val baseUrl = "http://10.0.2.2:5001"
                val fullUrl = if (url.startsWith("/")) {
                    "$baseUrl$url"
                } else if (!url.startsWith("http") && url.isNotEmpty()) {
                    "$baseUrl/song/$url"
                } else {
                    url
                }

                if (fullUrl.isNotEmpty()) {
                    mp.setDataSource(fullUrl)
                    mp.prepareAsync()
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error playing song", e)
            }
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                _isPlaying.value = false
            } else {
                if (_currentSongUrl.value.isNotEmpty()) {
                    mp.start()
                    _isPlaying.value = true
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
