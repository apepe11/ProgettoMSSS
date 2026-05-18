package com.example.progetto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.progetto.utils.LiveSignalSnapshot
import com.example.progetto.utils.SensorAvailability
import com.example.progetto.utils.SensorCollectionViewModel
import java.util.UUID
import java.util.Locale

@Composable
@Suppress("UNUSED_PARAMETER")
fun EmotionAnalysisScreen(
    onOpenDrawer: () -> Unit = {},
    onReviewSong: () -> Unit = {},
    onGoOn: () -> Unit = {},
    advanceAfterReview: Boolean = false,
    onAdvanceAfterReviewHandled: () -> Unit = {},
    onPlaySong: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    sensorViewModel: SensorCollectionViewModel? = null,
    viewModel: EmotionAnalysisViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionSensorViewModel: SensorCollectionViewModel = sensorViewModel ?: viewModel(
        factory = SensorCollectionViewModel.Factory(context)
    )
    val userId = authViewModel.currentUser?.userId
    val currentSong by viewModel.currentSong.collectAsState()
    val currentEmotion by viewModel.currentEmotion.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val sensorsAvailable by viewModel.sensorsAvailable.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val playingSongId by playerViewModel.currentSongId.collectAsState()
    val currentPosition by playerViewModel.currentPosition.collectAsState()
    val duration by playerViewModel.duration.collectAsState()
    val signalSnapshot by sessionSensorViewModel.signalSnapshot.collectAsState()
    val predictedEmotion by sessionSensorViewModel.predictedEmotion.collectAsState()
    var autoPlayedSongId by rememberSaveable { mutableStateOf<String?>(null) }
    var completedSongId by rememberSaveable { mutableStateOf<String?>(null) }

    val eegActive = signalSnapshot.eegSamples > 0 || SensorAvailability.hasEegSignal()
    val watchActive = signalSnapshot.heartRateSamples > 0 || SensorAvailability.hasWatchSignal(context)
    val missingSensors = buildList {
        if (!eegActive) add("EEG")
        if (!watchActive) add("Watch HR")
    }

    val songFinished = currentSong?.song_id == completedSongId

    LaunchedEffect(predictedEmotion) {
        val emotion = predictedEmotion
        if (!emotion.isNullOrBlank()) {
            viewModel.setCurrentEmotion(emotion)
        }
    }

    LaunchedEffect(advanceAfterReview) {
        if (advanceAfterReview) {
            sessionSensorViewModel.stopListeningSession()
            completedSongId = null
            viewModel.loadRandomSong()
            onAdvanceAfterReviewHandled()
        }
    }

    val startCurrentListeningSession = {
        val song = currentSong
        val songId = song?.song_id?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        val userUuid = userId
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: UUID.randomUUID()
        if (songId != null) {
            if (currentEmotion.contains("unavailable", ignoreCase = true)) {
                viewModel.setCurrentEmotion("Analyzing...")
            }
            sessionSensorViewModel.startOrResumeListeningSession(userUuid, songId)
        }
    }

    val openCurrentSong = {
        val song = currentSong
        if (song != null && song.url.isNotEmpty()) {
            onPlaySong(song.title, song.artist, song.url, song.song_id)
        }
    }

    val toggleCurrentSong = {
        val song = currentSong
        if (song != null && song.url.isNotEmpty()) {
            if (playingSongId == song.song_id) {
                playerViewModel.togglePlayPause()
            } else {
                completedSongId = null
                playerViewModel.updatePlaylist(emptyList(), -1)
                playerViewModel.playSong(song.title, song.artist, song.url, song.song_id)
            }
        }
    }


    LaunchedEffect(currentSong?.song_id, userId, playingSongId, isPlaying) {
        val song = currentSong
        if (song != null && song.url.isNotEmpty()) {
            val alreadyPlayingThis = isPlaying && playingSongId == song.song_id
            if (!alreadyPlayingThis && autoPlayedSongId != song.song_id) {
                autoPlayedSongId = song.song_id
                completedSongId = null
                playerViewModel.updatePlaylist(emptyList(), -1)
                playerViewModel.playSong(song.title, song.artist, song.url, song.song_id)
            }
        }
    }

    LaunchedEffect(isPlaying, playingSongId, currentSong?.song_id, sensorsAvailable) {
        val song = currentSong
        val isCurrentSongPlaying = isPlaying && song != null && playingSongId == song.song_id

        if (song != null && currentSong?.song_id != null) {
            val songUuid = runCatching { UUID.fromString(song.song_id) }.getOrNull()
            if (isCurrentSongPlaying && songUuid != null) {
                startCurrentListeningSession()
            } else if (songUuid != null && sessionSensorViewModel.isCollecting()) {
                sessionSensorViewModel.pauseListeningSession()
            }
        }
    }

    LaunchedEffect(currentSong?.song_id, playingSongId, isPlaying, currentPosition, duration) {
        val song = currentSong
        val songId = song?.song_id
        val isCurrentSongAtEnd = song != null &&
            playingSongId == songId &&
            duration > 0 &&
            currentPosition >= duration - 1000 &&
            !isPlaying

        if (isCurrentSongAtEnd && completedSongId != songId) {
            completedSongId = songId
            sessionSensorViewModel.stopListeningSession()
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
            val missingText = if (missingSensors.isNotEmpty()) {
                "Missing: ${missingSensors.joinToString(", ")}"
            } else {
                "Signals not detected"
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = "⚠️ $missingText\nL'analisi resta disponibile, ma i dati reali non sono attivi.",
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
                        .height(56.dp)
                        .clickable(
                            enabled = currentSong?.url?.isNotEmpty() == true,
                            onClick = openCurrentSong
                        ),
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
                                .clickable(
                                    enabled = currentSong?.url?.isNotEmpty() == true,
                                    onClick = openCurrentSong
                                )
                        ) {
                            Text(
                                text = currentSong?.title ?: "Unknown",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.clickable(
                                    enabled = currentSong?.url?.isNotEmpty() == true,
                                    onClick = openCurrentSong
                                )
                            )
                            Text(
                                text = currentSong?.artist ?: "Unknown Artist",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                modifier = Modifier.clickable(
                                    enabled = currentSong?.url?.isNotEmpty() == true,
                                    onClick = openCurrentSong
                                )
                            )
                        }
                        IconButton(
                            onClick = toggleCurrentSong,
                            enabled = currentSong?.url?.isNotEmpty() == true
                        ) {
                            Icon(
                                imageVector = if (isPlaying && playingSongId == currentSong?.song_id) {
                                    Icons.Default.Pause
                                } else {
                                    Icons.Default.PlayArrow
                                },
                                contentDescription = if (isPlaying && playingSongId == currentSong?.song_id) {
                                    "Pause"
                                } else {
                                    "Play"
                                },
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (songFinished) {
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
                        sessionSensorViewModel.stopListeningSession()
                        onReviewSong()
                    },
                    modifier = Modifier.fillMaxWidth(0.8f),
                    enabled = !isLoading && currentSong != null
                )
            } else {
                Text(
                    text = "SIGNALS ARE PERCEIVING:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                SignalPerceptionPanel(
                    snapshot = signalSnapshot,
                    sensorsAvailable = sensorsAvailable,
                    missingSensors = missingSensors,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        HeartButton(
            text = "Review another song",
            onClick = {
                sessionSensorViewModel.stopListeningSession()
                viewModel.loadRandomSong()
                onGoOn()
            },
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SignalPerceptionPanel(
    snapshot: LiveSignalSnapshot,
    sensorsAvailable: Boolean,
    missingSensors: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SignalLine(
            label = "EEG activity",
            value = snapshot.latestEeg?.let { "${formatSignalValue(it)} uV" } ?: "Waiting",
            samples = snapshot.eegSamples,
            active = snapshot.eegSamples > 0
        )
        SignalLine(
            label = "Heart rate",
            value = snapshot.latestHeartRate?.let { "${it.toInt()} bpm" } ?: "Waiting",
            samples = snapshot.heartRateSamples,
            active = snapshot.heartRateSamples > 0
        )

        Text(
            text = if (snapshot.isCollecting) {
                "Listening to live physiological signals..."
            } else if (sensorsAvailable) {
                "Preparing signal collection..."
            } else if (missingSensors.isNotEmpty()) {
                "Missing sensors: ${missingSensors.joinToString(", ")}"
            } else {
                "No live wearable signal detected yet."
            },
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SignalLine(
    label: String,
    value: String,
    samples: Int,
    active: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.LightGray.copy(alpha = 0.25f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "$samples samples", fontSize = 11.sp, color = Color.Gray)
            }
            Text(
                text = value,
                fontSize = 14.sp,
                color = if (active) MaterialTheme.colorScheme.primary else Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatSignalValue(value: Float): String =
    String.format(Locale.getDefault(), "%.1f", value)

@Preview(showBackground = true)
@Composable
fun EmotionAnalysisScreenPreview() {
    HeartMusicTheme {
        val context = LocalContext.current
        val authViewModel = remember { AuthViewModel(context.applicationContext as android.app.Application) }
        EmotionAnalysisScreen(authViewModel = authViewModel)
    }
}
