package com.example.progetto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.progetto.ui.theme.HeartMusicTheme
import com.example.progetto.utils.SensorAvailability
import kotlinx.coroutines.delay

import androidx.compose.ui.res.stringResource

@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit = {}, // You can likely delete this parameter later since the global bar handles the drawer now!
    onNavigateToEmotionAnalysis: () -> Unit = {},
    onNavigateToListeningMode: () -> Unit = {}
) {
    val context = LocalContext.current

    var sensorsAvailable by remember {
        mutableStateOf(
            SensorAvailability.hasEegSignal() && SensorAvailability.hasWatchSignal(context)
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            sensorsAvailable =
                SensorAvailability.hasEegSignal() && SensorAvailability.hasWatchSignal(context)
            delay(3000)
        }
    }

    // Removed the Scaffold and TopAppBar entirely!
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Area Modalità
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ModeButton(
                title = stringResource(R.string.home_emotion_analysis),
                subtitle = stringResource(R.string.home_emotion_analysis_subtitle),
                isEnabled = sensorsAvailable,
                onClick = onNavigateToEmotionAnalysis
            )

            if (!sensorsAvailable) {
                Text(
                    text = stringResource(R.string.home_sensors_not_available),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.semantics { 
                        // Per TalkBack, leggiamo questo come un avviso
                    }
                )
            }

            ModeButton(
                title = stringResource(R.string.home_listening_mode),
                subtitle = stringResource(R.string.home_listening_mode_subtitle),
                isEnabled = true,
                onClick = onNavigateToListeningMode
            )
        }
    }
}

@Composable
fun ModeButton(
    title: String,
    subtitle: String?,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val openModeLabel = stringResource(R.string.home_open_mode_description, title)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clickable(
                enabled = isEnabled,
                onClickLabel = openModeLabel,
                role = Role.Button
            ) { onClick() }
            .semantics(mergeDescendants = true) {
                // Merge title and subtitle for a single announcement
            },
        shape = RoundedCornerShape(28.dp),
        color = if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isEnabled) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = subtitle,
                        fontSize = 16.sp,
                        color = if (isEnabled) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                } else {
                    // Placeholder spacer for precision when subtitle is missing
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HeartMusicTheme {
        HomeScreen()
    }
}
