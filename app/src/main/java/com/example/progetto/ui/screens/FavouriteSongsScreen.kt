package com.example.progetto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.progetto.R
import com.example.progetto.data.SongResponse
import com.example.progetto.ui.theme.HeartMusicTheme
import com.example.progetto.ui.viewmodels.TopSongsViewModel

// ==========================================
// 1. THE STATEFUL WRAPPER
// ==========================================
@Composable
fun FavouriteSongsScreen(
    currentUserId: String,
    onOpenDrawer: () -> Unit = {},
    onNavigateToPlayer: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    viewModel: TopSongsViewModel = viewModel()
) {
    val topSongs by viewModel.topSongs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Refresh data when entering the screen
    LaunchedEffect(currentUserId) {
        viewModel.loadFavoriteSongs(currentUserId)
    }

    FavouriteSongsScreenContent(
        topSongs = topSongs,
        isLoading = isLoading,
        onNavigateToPlayer = onNavigateToPlayer,
        onRemoveFavorite = { songId -> viewModel.toggleFavoriteOff(currentUserId, songId) }
    )
}

// ==========================================
// 2. THE STATELESS UI
// ==========================================
@Composable
fun FavouriteSongsScreenContent(
    topSongs: List<SongResponse>,
    isLoading: Boolean,
    onNavigateToPlayer: (String, String, String, String) -> Unit,
    onRemoveFavorite: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.favorites_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (topSongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.favorites_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(topSongs) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToPlayer(song.title, song.artist, song.url, song.songId) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp)
                        ) {
                            Text(
                                text = song.title,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = song.artist,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { onRemoveFavorite(song.songId) }) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = stringResource(R.string.favorites_remove_description),
                                tint = Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. THE PREVIEWS
// ==========================================

@Preview(showBackground = true, name = "1. Populated List")
@Composable
fun PreviewFavouriteSongsScreen_Populated() {
    HeartMusicTheme {
        val mockSongs = listOf(
            SongResponse(songId = "1", title = "Bohemian Rhapsody", artist = "Queen", url = "", likes = 124),
            SongResponse(songId = "2", title = "Shape of You", artist = "Ed Sheeran", url = "", likes = 89),
            SongResponse(songId = "3", title = "Blinding Lights", artist = "The Weeknd", url = "", likes = 230)
        )

        FavouriteSongsScreenContent(
            topSongs = mockSongs,
            isLoading = false,
            onNavigateToPlayer = { _, _, _, _ -> },
            onRemoveFavorite = {}
        )
    }
}