package com.example.progetto.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.progetto.R
import com.example.progetto.ui.theme.HeartMusicTheme
import com.example.progetto.utils.SensorAvailability
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit = {},
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

    // Main container filling the full screen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // This Column uses weight(1f) on children to fill all vertical space
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- BUTTON 1: EMOTION ANALYSIS ---
            ModeButton(
                title = "Emotion Analysis",
                subtitle = "Analyze your feelings with music",
                isEnabled = sensorsAvailable,
                imageRes = R.drawable.emotion_analysis,
                modifier = Modifier.weight(1f), // Takes up 50% of the screen
                onClick = onNavigateToEmotionAnalysis
            )

            // Sensor text tucked neatly in the middle
            if (!sensorsAvailable) {
                Text(
                    text = "Connect EEG and watch to enable Emotion Analysis.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // --- BUTTON 2: LISTENING MODE ---
            ModeButton(
                title = "Listening Mode",
                subtitle = "Discover your library",
                isEnabled = true,
                imageRes = R.drawable.listening_mode,
                modifier = Modifier.weight(1f), // Takes up the other 50%
                onClick = onNavigateToListeningMode
            )

            // Extra spacer at bottom to ensure it doesn't hit the navigation bar
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ModeButton(
    title: String,
    subtitle: String?,
    isEnabled: Boolean,
    imageRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val purpleBorder = Color(0xFF8E44AD)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .border(
                width = 3.dp,
                color = if (isEnabled) purpleBorder else Color.LightGray.copy(alpha = 0.5f),
                shape = RoundedCornerShape(28.dp)
            )
            .clickable(
                enabled = isEnabled,
                role = Role.Button
            ) { onClick() },
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Image now scales dynamically based on button height
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxHeight(0.6f) // Larger images: 60% of button height
                    .aspectRatio(1f)      // Keep it perfectly square
                    .padding(bottom = 12.dp)
                    .alpha(if (isEnabled) 1f else 0.35f)
            )

            // Smaller Title
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isEnabled) purpleBorder else purpleBorder.copy(alpha = 0.4f)
            )

            if (subtitle != null) {
                // Smaller Subtitle
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = if (isEnabled) Color.DarkGray else Color.Gray.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Normal
                )
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