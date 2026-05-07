package com.example.progetto.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.progetto.ui.viewmodels.PlayerViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    songTitle: String = "Unknown",
    artistName: String = "Unknown",
    songUrl: String = "",
    songId: String = "",
    onNavigateBack: () -> Unit = {},
    viewModel: PlayerViewModel
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    
    // Osserviamo i cambiamenti del titolo e artista dal ViewModel perché possono cambiare con Next/Prev
    val actualTitle by viewModel.currentSongTitle.collectAsState()
    val actualArtist by viewModel.currentArtistName.collectAsState()
    
    // Carichiamo la canzone solo se è diversa da quella attualmente in canna
    LaunchedEffect(songUrl) {
        if (songUrl.isNotEmpty()) {
            viewModel.playSong(songTitle, artistName, songUrl, songId)
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
                            contentDescription = "Close player"
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
            Surface(
                modifier = Modifier.size(220.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(76.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) { }
                ) {
                    Text(text = actualTitle, style = MaterialTheme.typography.headlineSmall)
                    Text(text = actualArtist, style = MaterialTheme.typography.bodyLarge)
                }

                IconButton(
                    onClick = { viewModel.toggleFavorite() },
                    modifier = Modifier.semantics {
                        stateDescription = if (isFavorite) "Added to favourite" else "Removed to favourite"
                    }
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Preferiti",
                        tint = if (isFavorite) Color.Red else LocalContentColor.current
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar (Slider)
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { viewModel.seekTo(it.toInt()) },
                    valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                    modifier = Modifier.semantics { 
                        contentDescription = "Song progress"
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val currentTime = formatTime(currentPosition)
                    val totalTime = formatTime(duration)
                    Text(
                        text = currentTime, 
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.semantics { contentDescription = "Current position: $currentTime" }
                    )
                    Text(
                        text = totalTime, 
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.semantics { contentDescription = "Total duration: $totalTime" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                IconButton(
                    onClick = { viewModel.playPrevious() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous song",
                        modifier = Modifier.size(36.dp)
                    )
                }

                FilledIconButton(
                    onClick = {
                        viewModel.togglePlayPause()
                    },
                    modifier = Modifier.size(82.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(44.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.playNext() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next song",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

private fun formatTime(milliseconds: Int): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
