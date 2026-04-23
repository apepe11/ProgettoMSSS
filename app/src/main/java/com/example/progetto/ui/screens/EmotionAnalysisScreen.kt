package com.example.progetto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.progetto.ui.components.HeartButton
import com.example.progetto.ui.theme.HeartMusicTheme
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotionAnalysisScreen(
    onOpenDrawer: () -> Unit = {},
    onReviewSong: () -> Unit = {},
    onGoOn: () -> Unit = {}
) {
    // Sample data to simulate random selection
    val songs = listOf("Bohemian Rhapsody", "Imagine", "Stayin' Alive", "Billie Jean", "Like a Rolling Stone")
    val emotions = listOf("Happy", "Sad", "Energetic", "Calm", "Anxious")

    // State variables to hold current random values
    var currentSong by remember { mutableStateOf(songs.random()) }
    var currentEmotion by remember { mutableStateOf(emotions.random()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Emotion Analysis Mode", 
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Sezione centrale evidenziata (dal disegno)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "THE SONG CURRENTLY IS:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Barra della canzone
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White, RoundedCornerShape(4.dp))
                        )
                        Text(
                            text = currentSong,
                            color = Color.White,
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "THE EMOTION DETECTED IS:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = currentEmotion,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                HeartButton(
                    text = "Review the song",
                    onClick = onReviewSong,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            HeartButton(
                text = "Review another song",
                onClick = {
                    // Update states with new random values to "reload" the page content
                    currentSong = songs.random()
                    currentEmotion = emotions.random()
                    onGoOn()
                }

            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EmotionAnalysisScreenPreview() {
    HeartMusicTheme {
        EmotionAnalysisScreen()
    }
}
