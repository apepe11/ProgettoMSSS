package com.example.progetto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.progetto.ui.components.HeartButton
import com.example.progetto.ui.theme.HeartMusicTheme
import com.example.progetto.ui.viewmodels.AuthViewModel
import com.example.progetto.ui.viewmodels.EmotionAnalysisViewModel
import com.example.progetto.ui.viewmodels.PlayerViewModel
import com.example.progetto.utils.SensorCollectionViewModel
import mylibrary.mindrove.SensorData
import mylibrary.mindrove.ServerManager
import java.util.UUID
import android.util.Log

@Composable
fun EmotionAnalysisScreen(
    onOpenDrawer: () -> Unit = {},
    onReviewSong: () -> Unit = {},
    onGoOn: () -> Unit = {},
    onPlaySong: (String, String, String) -> Unit = { _, _, _ -> },
    viewModel: EmotionAnalysisViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val sensorViewModel = remember { SensorCollectionViewModel(context) }
    val userId = authViewModel.currentUser?.userId
    val currentSong by viewModel.currentSong.collectAsState()
    val currentEmotion by viewModel.currentEmotion.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val sensorsAvailable by viewModel.sensorsAvailable.collectAsState()
    var autoPlayedSongId by remember { mutableStateOf<String?>(null) }

    // MindRove EEG integration (ServerManager callback)
    val serverManager = remember {
        ServerManager { sensorData: SensorData ->
            sensorViewModel.onEegDataReceived(0, sensorData.channel1.toDouble())
            sensorViewModel.onEegDataReceived(1, sensorData.channel2.toDouble())
            sensorViewModel.onEegDataReceived(2, sensorData.channel3.toDouble())
            sensorViewModel.onEegDataReceived(3, sensorData.channel4.toDouble())
            sensorViewModel.onEegDataReceived(4, sensorData.channel5.toDouble())
            sensorViewModel.onEegDataReceived(5, sensorData.channel6.toDouble())
        }
    }

    LaunchedEffect(Unit) {
        Log.d("MindRove", "Starting ServerManager")
        serverManager.start()
    }

    LaunchedEffect(currentSong?.song_id) {
        val song = currentSong
        if (song != null && song.url.isNotEmpty() && autoPlayedSongId != song.song_id) {
            autoPlayedSongId = song.song_id
            playerViewModel.playSong(song.title, song.artist, song.url, song.song_id)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d("MindRove", "Stopping ServerManager")
            serverManager.stop()
        }
    }

    // Removed the Scaffold entirely! Now it's just a clean Column.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp), // Removed paddingValues since the Scaffold is gone
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Warning banner if no sensors
        if (!sensorsAvailable) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = "⚠️ EEG non in streaming o watch non collegata\nL'analisi resta disponibile, ma i dati reali non sono attivi.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

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

            // Show loading or error
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else if (error != null) {
                Text(
                    text = "Error: $error",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            } else if (currentSong != null) {
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
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = currentSong?.title ?: "Unknown",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = currentSong?.artist ?: "Unknown Artist",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                        IconButton(
                            onClick = {
                                val song = currentSong
                                if (song != null && song.url.isNotEmpty()) {
                                    val songId = runCatching { UUID.fromString(song.song_id) }.getOrNull()
                                    val userUuid = userId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                                    if (songId != null && userUuid != null) {
                                        sensorViewModel.startListeningSession(userUuid, songId)
                                    }
                                    onPlaySong(song.title, song.artist, song.url)
                                }
                            },
                            enabled = currentSong?.url?.isNotEmpty() == true
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White
                            )
                        }
                    }
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
                onClick = {
                    sensorViewModel.stopListeningSession()
                    onReviewSong()
                },
                modifier = Modifier.fillMaxWidth(0.8f),
                enabled = !isLoading && currentSong != null
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        HeartButton(
            text = "Review another song",
            onClick = {
                sensorViewModel.stopListeningSession()
                // Load a new random song
                viewModel.loadRandomSong()
                onGoOn()
            },
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun EmotionAnalysisScreenPreview() {
    HeartMusicTheme {
        val context = LocalContext.current
        val authViewModel = remember { AuthViewModel(context.applicationContext as android.app.Application) }
        EmotionAnalysisScreen(authViewModel = authViewModel)
    }
}