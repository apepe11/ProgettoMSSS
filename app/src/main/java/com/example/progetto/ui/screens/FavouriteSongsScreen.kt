package com.example.progetto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.progetto.ui.theme.HeartMusicTheme
import com.example.progetto.ui.viewmodels.TopSongsViewModel
import com.example.progetto.utils.Song

// ==========================================
// 1. THE STATEFUL WRAPPER (Used by your App)
// ==========================================
@Composable
fun FavouriteSongsScreen(
    currentUserId: String, // <-- Added this parameter!
    onOpenDrawer: () -> Unit = {},
    onNavigateToPlayer: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    viewModel: TopSongsViewModel = viewModel()
) {
    val topSongs by viewModel.topSongs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Refresh data when entering the screen, specifically for this user
    LaunchedEffect(currentUserId) {
        viewModel.loadFavoriteSongs(currentUserId)
    }

    // Pass everything down to the "dumb" UI
    FavouriteSongsScreenContent(
        topSongs = topSongs,
        searchQuery = searchQuery,
        isLoading = isLoading,
        onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
        onNavigateToPlayer = onNavigateToPlayer,
        onRemoveFavorite = { songId -> viewModel.toggleFavoriteOff(currentUserId, songId) }
    )
}

// ==========================================
// 2. THE STATELESS UI (Used by the Preview)
// ==========================================
@Composable
fun FavouriteSongsScreenContent(
    topSongs: List<Song>,
    searchQuery: String,
    isLoading: Boolean,
    onSearchQueryChange: (String) -> Unit,
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
            text = "Favourite Songs",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search for Playlist, Emotion, Song", fontSize = 12.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.LightGray,
                unfocusedBorderColor = Color.LightGray
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (topSongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No favourite songs yet", color = Color.Gray)
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
                            Text(text = song.title, fontWeight = FontWeight.Bold)
                            Text(text = song.artist, fontSize = 12.sp, color = Color.Gray)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "Remove from favorites",
                                tint = Color.Red,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onRemoveFavorite(song.songId) }
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
            Song(songId = "1", title = "Bohemian Rhapsody", artist = "Queen", url = "", likes = 124),
            Song(songId = "2", title = "Shape of You", artist = "Ed Sheeran", url = "", likes = 89),
            Song(songId = "3", title = "Blinding Lights", artist = "The Weeknd", url = "", likes = 230)
        )

        FavouriteSongsScreenContent(
            topSongs = mockSongs,
            searchQuery = "",
            isLoading = false,
            onSearchQueryChange = {},
            onNavigateToPlayer = { _, _, _, _ -> },
            onRemoveFavorite = {}
        )
    }
}

@Preview(showBackground = true, name = "2. Loading State")
@Composable
fun PreviewFavouriteSongsScreen_Loading() {
    HeartMusicTheme {
        FavouriteSongsScreenContent(
            topSongs = emptyList(),
            searchQuery = "",
            isLoading = true,
            onSearchQueryChange = {},
            onNavigateToPlayer = { _, _, _, _ -> },
            onRemoveFavorite = {}
        )
    }
}

@Preview(showBackground = true, name = "3. Empty State")
@Composable
fun PreviewFavouriteSongsScreen_Empty() {
    HeartMusicTheme {
        FavouriteSongsScreenContent(
            topSongs = emptyList(),
            searchQuery = "",
            isLoading = false,
            onSearchQueryChange = {},
            onNavigateToPlayer = { _, _, _, _ -> },
            onRemoveFavorite = {}
        )
    }
}