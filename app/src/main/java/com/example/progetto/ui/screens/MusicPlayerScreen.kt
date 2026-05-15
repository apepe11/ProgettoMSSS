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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.progetto.ui.viewmodels.PlayerViewModel
import java.util.Locale
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.progetto.R

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
    
    val actualTitle by viewModel.currentSongTitle.collectAsState()
    val actualArtist by viewModel.currentArtistName.collectAsState()
    
    LaunchedEffect(songUrl) {
        if (songUrl.isNotEmpty()) {
            viewModel.playSong(songTitle, artistName, songUrl, songId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.player_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.player_close_description)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            val (cover, info, controls, progress) = createRefs()

            // 1. Cover Image
            Surface(
                modifier = Modifier
                    .size(260.dp)
                    .constrainAs(cover) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(info.top)
                    },
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Song Info (Title, Artist, Favorite)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(info) {
                        top.linkTo(cover.bottom, margin = 32.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
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

                val favoriteStateDescription = if (isFavorite) stringResource(R.string.player_added_favorite) else stringResource(R.string.player_removed_favorite)
                IconButton(
                    onClick = { viewModel.toggleFavorite() },
                    modifier = Modifier.semantics {
                        stateDescription = favoriteStateDescription
                    }
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(R.string.player_favorites_description),
                        tint = if (isFavorite) Color.Red else LocalContentColor.current
                    )
                }
            }

            // 3. Progress Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(progress) {
                        top.linkTo(info.bottom, margin = 24.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
            ) {
                val progressDescription = stringResource(R.string.player_progress_description)
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { viewModel.seekTo(it.toInt()) },
                    valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                    modifier = Modifier.semantics { contentDescription = progressDescription }
                )
                Row(
                    modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val currentTime = formatTime(currentPosition)
                    val totalTime = formatTime(duration)
                    val currentPositionDescription = stringResource(R.string.player_current_position_description, currentTime)
                    val totalDurationDescription = stringResource(R.string.player_total_duration_description, totalTime)
                    Text(
                        text = currentTime, 
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.semantics { contentDescription = currentPositionDescription }
                    )
                    Text(
                        text = totalTime, 
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.semantics { contentDescription = totalDurationDescription }
                    )
                }
            }

            // 4. Playback Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(controls) {
                        bottom.linkTo(parent.bottom, margin = 24.dp)
                        top.linkTo(progress.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally)
            ) {
                IconButton(
                    onClick = { viewModel.playPrevious() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = stringResource(R.string.player_previous_description),
                        modifier = Modifier.size(36.dp)
                    )
                }

                val playPauseDescription = if (isPlaying) stringResource(R.string.player_pause_description) else stringResource(R.string.player_play_description)
                FilledIconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.size(82.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = playPauseDescription,
                        modifier = Modifier.size(44.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.playNext() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = stringResource(R.string.player_next_description),
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
