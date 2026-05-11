package com.example.progetto.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.progetto.R
import com.example.progetto.ui.viewmodels.PlayerViewModel
import java.util.Locale

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

            Surface(
                modifier = Modifier
                    .size(260.dp)
                    .constrainAs(cover) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(info) {
                        top.linkTo(cover.bottom, margin = 32.dp)
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = actualTitle, style = MaterialTheme.typography.headlineSmall)
                    Text(text = actualArtist, style = MaterialTheme.typography.bodyLarge)
                }

                IconButton(onClick = { viewModel.toggleFavorite() }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) Color.Red else LocalContentColor.current
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(progress) {
                        top.linkTo(info.bottom, margin = 24.dp)
                    }
            ) {
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { viewModel.seekTo(it.toInt()) },
                    valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = formatTime(currentPosition), style = MaterialTheme.typography.bodySmall)
                    Text(text = formatTime(duration), style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(controls) {
                        bottom.linkTo(parent.bottom, margin = 24.dp)
                        top.linkTo(progress.bottom)
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally)
            ) {
                IconButton(onClick = { viewModel.playPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = null, modifier = Modifier.size(36.dp))
                }

                FilledIconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                }

                IconButton(onClick = { viewModel.playNext() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(36.dp))
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