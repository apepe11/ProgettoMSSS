package com.example.progetto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.progetto.ui.theme.HeartMusicTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningModeScreen(
    onNavigateToPlaylist: (String) -> Unit = {},
    onNavigateToPlayer: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val playlists = listOf("PLAYLIST 1", "PLAYLIST 2", "PLAYLIST 3", "PLAYLIST 4", "PLAYLIST 5", "PLAYLIST 6", "PLAYLIST 7", "PLAYLIST 8")

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { 
                        Text(
                            "Listening Mode", 
                            fontSize = 20.sp,
                            color = Color.Black
                        ) 
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 18.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { onOpenDrawer() }
                        )
                    }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search for Playlist, Emotion, Song", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.LightGray,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        singleLine = true
                    )
                }
            }
        },
        bottomBar = { 
            MiniPlayer(onClick = onNavigateToPlayer)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
        ) {
            items(playlists) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToPlaylist(playlist) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = playlist,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MiniPlayer(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Name of the song",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { }) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListeningModeScreenPreview() {
    HeartMusicTheme {
        ListeningModeScreen()
    }
}
