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
import com.example.progetto.utils.SensorCollectionViewModel
import java.util.UUID
import java.util.Locale

import androidx.compose.ui.res.stringResource
import com.example.progetto.R

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

@Composable
fun EmotionAnalysisScreen(
    onOpenDrawer: () -> Unit = {},
    onReviewSong: () -> Unit = {},
    onGoOn: () -> Unit = {},
    onPlaySong: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    viewModel: EmotionAnalysisViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // Permission check for point 4 (explicitly check before using feature)
    val hasBodySensors = ContextCompat.checkSelfPermission(context, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED
    
    val sensorViewModel = remember { SensorCollectionViewModel.get(context) }
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
    val signalSnapshot by sensorViewModel.signalSnapshot.collectAsState()
    var autoPlayedSongId by remember { mutableStateOf<String?>(null) }
    var completedSongId by remember { mutableStateOf<String?>(null) }

    val songFinished = currentSong?.song_id == completedSongId

    val startCurrentListeningSession = {
        val song = currentSong
        val songId = song?.song_id?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        val userUuid = userId
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: UUID.randomUUID()
        if (songId != null && !sensorViewModel.isCollecting()) {
            sensorViewModel.startListeningSession(userUuid, songId)
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
                startCurrentListeningSession()
                playerViewModel.updatePlaylist(emptyList(), -1)
                playerViewModel.playSong(song.title, song.artist, song.url, song.song_id)
            }
        }
    }


    LaunchedEffect(currentSong?.song_id, userId) {
        val song = currentSong
        if (song != null && song.url.isNotEmpty()) {
            if (!sensorViewModel.isCollecting()) {
                startCurrentListeningSession()
            }

            if (autoPlayedSongId != song.song_id) {
                autoPlayedSongId = song.song_id
                completedSongId = null
                playerViewModel.updatePlaylist(emptyList(), -1)
                playerViewModel.playSong(song.title, song.artist, song.url, song.song_id)
            }
        }
    }

    LaunchedEffect(currentSong?.song_id, playingSongId, isPlaying, currentPosition, duration) {
        val song = currentSong
        val isCurrentSongAtEnd = song != null &&
            playingSongId == song.song_id &&
            duration > 0 &&
            currentPosition >= duration - 1000 &&
            !isPlaying

        if (isCurrentSongAtEnd && completedSongId != song.song_id) {
            completedSongId = song.song_id
            sensorViewModel.stopListeningSession()
        }
    }

    // Removed the Scaffold entirely! Now it's just a clean Column.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp), // Removed paddingValues since the Scaffold is gone
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Warning banner if no sensors OR no permissions
        if (!sensorsAvailable || !hasBodySensors) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (!hasBodySensors) 
                            stringResource(R.string.permission_rationale)
                        else 
                            stringResource(R.string.emotion_analysis_warning),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Sezione centrale evidenziata (dal disegno)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.emotion_analysis_current_song_header),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Show loading or error
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else if (error != null) {
                Text(
                    text = error!!.asString(),
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
                                .background(MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(4.dp))
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
                                text = currentSong?.title ?: stringResource(R.string.emotion_analysis_unknown_song),
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.clickable(
                                    enabled = currentSong?.url?.isNotEmpty() == true,
                                    onClick = openCurrentSong
                                )
                            )
                            Text(
                                text = currentSong?.artist ?: stringResource(R.string.emotion_analysis_unknown_artist),
                                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f),
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
                            val isCurrentPlaying = isPlaying && playingSongId == currentSong?.song_id
                            Icon(
                                imageVector = if (isCurrentPlaying) {
                                    Icons.Default.Pause
                                } else {
                                    Icons.Default.PlayArrow
                                },
                                contentDescription = if (isCurrentPlaying) {
                                    stringResource(R.string.player_pause_description)
                                } else {
                                    stringResource(R.string.player_play_description)
                                },
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (songFinished) {
                Text(
                    text = stringResource(R.string.emotion_analysis_detected_header),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
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
                    text = stringResource(R.string.emotion_analysis_review_song),
                    onClick = {
                        onReviewSong()
                    },
                    modifier = Modifier.fillMaxWidth(0.8f),
                    enabled = !isLoading && currentSong != null
                )
            } else {
                Text(
                    text = stringResource(R.string.emotion_analysis_signals_header),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                SignalPerceptionPanel(
                    snapshot = signalSnapshot,
                    sensorsAvailable = sensorsAvailable,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        HeartButton(
            text = stringResource(R.string.emotion_analysis_review_another),
            onClick = {
                sensorViewModel.stopListeningSession()
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val waitingText = stringResource(R.string.emotion_analysis_waiting)
        SignalLine(
            label = stringResource(R.string.emotion_analysis_eeg_label),
            value = snapshot.latestEeg?.let { "${formatSignalValue(it)} uV" } ?: waitingText,
            samples = snapshot.eegSamples,
            active = snapshot.eegSamples > 0
        )
        SignalLine(
            label = stringResource(R.string.emotion_analysis_hr_label),
            value = snapshot.latestHeartRate?.let { "${it.toInt()} bpm" } ?: waitingText,
            samples = snapshot.heartRateSamples,
            active = snapshot.heartRateSamples > 0
        )
        SignalLine(
            label = stringResource(R.string.emotion_analysis_eda_label),
            value = snapshot.latestEda?.let { formatSignalValue(it) } ?: waitingText,
            samples = snapshot.edaSamples,
            active = snapshot.edaSamples > 0
        )

        Text(
            text = if (snapshot.isCollecting) {
                stringResource(R.string.emotion_analysis_listening_live)
            } else if (sensorsAvailable) {
                stringResource(R.string.emotion_analysis_preparing)
            } else {
                stringResource(R.string.emotion_analysis_no_signal)
            },
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
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
                Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = stringResource(R.string.emotion_analysis_samples_count, samples), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = value,
                fontSize = 14.sp,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
