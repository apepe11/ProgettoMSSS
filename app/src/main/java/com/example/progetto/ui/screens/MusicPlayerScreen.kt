package com.example.progetto.ui.screens

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    songTitle: String = "Unknown",
    artistName: String = "Unknown",
    songUrl: String = "",
    onNavigateBack: () -> Unit = {}
) {
    var isPlaying by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }

    // Initialize MediaPlayer
    val mediaPlayer = remember {
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }
    }

    // Effect to handle song loading and playback
    LaunchedEffect(songUrl) {
        if (songUrl.isNotEmpty()) {
            try {
                // MODIFICA: Usiamo la porta 5001 come definito nel docker-compose per l'host esterno
                // 10.0.2.2 è l'indirizzo per accedere al localhost del PC dall'emulatore Android
                val baseUrl = "http://10.0.2.2:5001"
                val fullUrl = if (songUrl.startsWith("/")) {
                    "$baseUrl$songUrl"
                } else if (!songUrl.startsWith("http")) {
                    "$baseUrl/song/$songUrl"
                } else {
                    songUrl
                }

                Log.d("MusicPlayer", "Tentativo di caricamento: $fullUrl")
                mediaPlayer.reset()
                
                mediaPlayer.setOnPreparedListener {
                    Log.d("MusicPlayer", "MediaPlayer pronto, avvio riproduzione")
                    it.start()
                    isPlaying = true
                }
                
                mediaPlayer.setOnErrorListener { mp, what, extra ->
                    Log.e("MusicPlayer", "Errore MediaPlayer: what=$what, extra=$extra")
                    isPlaying = false
                    false
                }

                mediaPlayer.setOnCompletionListener {
                    Log.d("MusicPlayer", "Riproduzione completata")
                    isPlaying = false
                }

                mediaPlayer.setDataSource(fullUrl)
                mediaPlayer.prepareAsync()
            } catch (e: Exception) {
                Log.e("MusicPlayer", "Eccezione durante il caricamento di $songUrl", e)
            }
        } else {
            Log.w("MusicPlayer", "L'URL della canzone è vuoto")
        }
    }

    // Release MediaPlayer when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Music Player") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Chiudi"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Album Art Placeholder
            Surface(
                modifier = Modifier.size(300.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = songTitle, style = MaterialTheme.typography.headlineSmall)
                    Text(text = artistName, style = MaterialTheme.typography.bodyLarge)
                }


                IconButton(onClick = { isFavorite = !isFavorite }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Mi piace",
                        tint = if (isFavorite) Color.Red else LocalContentColor.current
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(onClick = { /* Precedente */ }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                }

                FilledIconButton(
                    onClick = {
                        if (isPlaying) {
                            mediaPlayer.pause()
                        } else {
                            mediaPlayer.start()
                        }
                        isPlaying = !isPlaying
                    },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = { /* Successivo */ }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next")
                }
            }
        }
    }
}
