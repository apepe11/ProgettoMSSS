package com.example.progetto.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.progetto.ui.theme.HeartMusicTheme
import com.example.progetto.utils.SensorAvailability
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit = {}, // You can likely delete this parameter later since the global bar handles the drawer now!
    onNavigateToEmotionAnalysis: () -> Unit = {},
    onNavigateToListeningMode: () -> Unit = {}
) {
    val context = LocalContext.current
    var sensorsAvailable by remember {
        mutableStateOf(SensorAvailability.hasEmotionSensors(context))
    }

    LaunchedEffect(Unit) {
        while (true) {
            sensorsAvailable = SensorAvailability.hasEmotionSensors(context)
            delay(3000)
        }
    }

    // Removed the Scaffold and TopAppBar entirely!
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp) // Replaced paddingValues with standard padding
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Area Modalità
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ModeButton(
                title = "Emotion Analysis",
                subtitle = "Analyze your feelings with music",
                isEnabled = sensorsAvailable,
                onClick = onNavigateToEmotionAnalysis
            )

            if (!sensorsAvailable) {
                Text(
                    text = "Connect ECG/HR sensors to enable Emotion Analysis.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            ModeButton(
                title = "Listening Mode",
                subtitle = "Discover your library",
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clickable(enabled = isEnabled) { onClick() },
        shape = RoundedCornerShape(28.dp),
        color = if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        else Color.LightGray.copy(alpha = 0.4f),
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
                    color = if (isEnabled) Color.White else Color.Gray
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = subtitle,
                        fontSize = 16.sp,
                        color = if (isEnabled) Color.White.copy(alpha = 0.9f) else Color.Gray
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