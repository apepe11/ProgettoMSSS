package com.example.progetto.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.progetto.R
import com.example.progetto.ui.theme.HeartMusicTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userImageUrl: String? = null,
    onOpenDrawer: () -> Unit = {},
    onNavigateToEmotionAnalysis: () -> Unit = {},
    onNavigateToListeningMode: () -> Unit = {}
) {
    var isDeviceConnected by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(80.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Fit
                    )
                },
                title = { 
                    Text(
                        "HeartMusic", 
                        fontSize = 20.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 18.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onOpenDrawer() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (userImageUrl != null) {
                            Image(
                                painter = rememberAsyncImagePainter(userImageUrl),
                                contentDescription = "User Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Area Modalità
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ModeButton(
                    title = "Emotion Analysis",
                    subtitle = if (isDeviceConnected) "Device detected" else "Connect device to start",
                    isEnabled = isDeviceConnected,
                    onClick = onNavigateToEmotionAnalysis
                )

                ModeButton(
                    title = "Listening Mode",
                    subtitle = "Discover your library", 
                    isEnabled = true,
                    onClick = onNavigateToListeningMode
                )
            }
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
