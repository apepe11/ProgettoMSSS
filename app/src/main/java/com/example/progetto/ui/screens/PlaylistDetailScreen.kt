package com.example.progetto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.progetto.R
import com.example.progetto.data.SongResponse // ✅ MUST HAVE THIS
import com.example.progetto.ui.viewmodels.PlaylistViewModel
import com.example.progetto.ui.viewmodels.PlayerViewModel

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onNavigateToPlayer: (String, String, String, String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PlaylistViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel()
) {
    val playlistDetail by viewModel.playlistDetail.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredSongs by viewModel.filteredSongs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(playlistId) {
        viewModel.loadPlaylistDetails(playlistId)
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top Bar
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            Text(text = "Playlist Details", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(text = playlistDetail?.title ?: "", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(text = playlistDetail?.emotion ?: "", color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ✅ Explicitly defining the type (song: SongResponse) fixes the "Cannot infer type" error
                itemsIndexed(filteredSongs) { index, song: SongResponse ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playerViewModel.updatePlaylist(filteredSongs, index)
                                playerViewModel.playSong(song.title, song.artist, song.url, song.songId)
                                onNavigateToPlayer(song.title, song.artist, song.url, song.songId)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = song.title, fontWeight = FontWeight.Bold)
                            Text(text = song.artist, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}