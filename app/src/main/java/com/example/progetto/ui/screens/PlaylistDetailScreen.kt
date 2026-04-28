package com.example.progetto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.progetto.ui.theme.HeartMusicTheme

@Composable
fun PlaylistDetailScreen(
    playlistName: String,
    onOpenDrawer: () -> Unit = {}, // Can be removed later if unused
    onNavigateToPlayer: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val songs = listOf("SONG 1", "SONG 2", "SONG 3", "SONG 4", "SONG 5")

    // Removed the Scaffold entirely!
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 1. Search Bar at the top
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search for Playlist, Emotion, Song", fontSize = 12.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.LightGray,
                unfocusedBorderColor = Color.LightGray
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Middle Content (Takes up all remaining space with weight(1f))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // Header Playlist
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text = playlistName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Lista Canzoni
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(songs) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToPlayer() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = song,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 3. MiniPlayer pinned to the bottom
        // (Note: This automatically uses the MiniPlayer function you defined in ListeningModeScreen.kt!)
        MiniPlayer(onClick = onNavigateToPlayer)
    }
}

@Preview(showBackground = true)
@Composable
fun PlaylistDetailScreenPreview() {
    HeartMusicTheme {
        PlaylistDetailScreen(playlistName = "My Awesome Playlist")
    }
}