package com.example.progetto.ui.viewmodels

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.progetto.data.RetrofitClient
import com.example.progetto.data.SongResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlayerViewModel : ViewModel() {

    private val _currentSongId = MutableStateFlow<String?>(null)
    val currentSongId: StateFlow<String?> = _currentSongId

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

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite

    private var userId: String? = null

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var isPreparing = false

    // Playlist management
    private val _currentPlaylist = MutableStateFlow<List<SongResponse>>(emptyList())
    private var _currentIndex = -1

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
                    // Auto-play next song if available
                    playNext()
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
                    break
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
    }

    fun updatePlaylist(songs: List<SongResponse>, startIndex: Int) {
        _currentPlaylist.value = songs
        _currentIndex = startIndex
    }

    fun setUserId(id: String?) {
        userId = id
    }

    fun toggleFavorite() {
        val sId = _currentSongId.value
        val uId = userId
        
        Log.d("PlayerViewModel", "toggleFavorite: songId=$sId, userId=$uId")
        
        if (sId == null || uId == null) {
            Log.w("PlayerViewModel", "Impossibile fare toggleFavorite: mancano ID")
            return
        }

        // Update ottimistico della UI per feedback immediato
        val previousState = _isFavorite.value
        _isFavorite.value = !previousState

        viewModelScope.launch {
            try {
                val response = RetrofitClient.playlistApiService.toggleFavorite(sId, mapOf("user_id" to uId))
                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("PlayerViewModel", "toggleFavorite successo: $body")
                    // Confermiamo lo stato dal server
                    _isFavorite.value = (body?.get("is_favorite") == true)
                } else {
                    Log.e("PlayerViewModel", "toggleFavorite errore API: ${response.code()}")
                    _isFavorite.value = previousState // Rollback in caso di errore
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Errore toggleFavorite", e)
                _isFavorite.value = previousState // Rollback
            }
        }
    }

    private fun checkFavoriteStatus(sId: String, uId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.playlistApiService.checkFavorite(sId, uId)
                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("PlayerViewModel", "checkFavoriteStatus: $body")
                    _isFavorite.value = (body?.get("is_favorite") == true)
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Errore checkFavoriteStatus", e)
            }
        }
    }

    fun playSong(title: String, artist: String, url: String, songId: String? = null) {
        Log.d("PlayerViewModel", "playSong richiesta per: $url (ID: $songId)")
        
        val mp = getOrCreateModelPlayer()
        
        if (_currentSongUrl.value == url && (_isPlaying.value || isPreparing)) {
            try {
                if (mp.isPlaying) {
                    Log.d("PlayerViewModel", "La canzone è già in riproduzione, salto il caricamento")
                    return
                }
            } catch (e: Exception) {}
        }

        _currentSongId.value = songId
        _currentSongTitle.value = title
        _currentArtistName.value = artist
        _currentSongUrl.value = url
        _currentPosition.value = 0
        _duration.value = 0
        _isPlaying.value = false
        _isFavorite.value = false // Reset favorite state
        isPreparing = true
        
        // Check favorite status if we have both IDs
        if (songId != null && userId != null) {
            checkFavoriteStatus(songId, userId!!)
        }
        _duration.value = 0 // Reset duration to prevent old progress bar state
        stopProgressUpdate()

        try {
            mp.reset()
            val baseUrl = "http://10.0.2.2:5005"
            val fullUrl = when {
                url.startsWith("http") -> url
                url.startsWith("/") -> "$baseUrl$url"
                else -> "$baseUrl/static/music/$url" // Changed to match backend structure if just a path is given
            }
            Log.d("PlayerViewModel", "Avvio caricamento DataSource: $fullUrl")
            mp.setDataSource(fullUrl)
            mp.prepareAsync()
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Errore critico in playSong", e)
            isPreparing = false
            _isPlaying.value = false
        }
    }

    fun playNext() {
        val playlist = _currentPlaylist.value
        if (playlist.isNotEmpty() && _currentIndex < playlist.size - 1) {
            _currentIndex++
            val nextSong = playlist[_currentIndex]
            playSong(nextSong.title, nextSong.artist, nextSong.url, nextSong.songId)
        }
    }

    fun playPrevious() {
        val playlist = _currentPlaylist.value
        if (playlist.isNotEmpty() && _currentIndex > 0) {
            _currentIndex--
            val prevSong = playlist[_currentIndex]
            playSong(prevSong.title, prevSong.artist, prevSong.url, prevSong.songId)
        } else if (playlist.isNotEmpty() && _currentIndex == 0) {
            // Se è la prima canzone, ricomincia da capo
            seekTo(0)
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        try {
            if (mp.isPlaying) {
                mp.pause()
                _isPlaying.value = false
                stopProgressUpdate()
            } else {
                val isAtEnd = _duration.value > 0 && mp.currentPosition >= _duration.value - 1500
                if (isAtEnd || (mp.currentPosition == 0 && !isPreparing)) {
                    playSong(_currentSongTitle.value, _currentArtistName.value, _currentSongUrl.value)
                } else {
                    mp.start()
                    _isPlaying.value = true
                    startProgressUpdate()
                }
            }
        } catch (e: Exception) {
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
