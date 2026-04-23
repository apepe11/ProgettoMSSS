package com.example.progetto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.progetto.ui.components.HeartButton
import com.example.progetto.ui.theme.HeartMusicTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewEmotionScreen(
    onOpenDrawer: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onSaveFeeling: () -> Unit = {}
) {
    var feelingValue by remember { mutableFloatStateOf(0.5f) }
    var strengthValue by remember { mutableFloatStateOf(0.5f) }
    var description by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Answer the question",
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

            // Domanda 1
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "How do you feel?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Slider(
                    value = feelingValue,
                    onValueChange = { feelingValue = it },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.LightGray
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Domanda 2
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "How strong is your emotion?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Slider(
                    value = strengthValue,
                    onValueChange = { strengthValue = it },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.LightGray
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Domanda 3 - Descrizione
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Describe your feeling") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Gray,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            // Bottoni finali
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HeartButton(
                    text = "Go back",
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                )
                HeartButton(
                    text = "Save feeling",
                    onClick = onSaveFeeling,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReviewEmotionScreenPreview() {
    HeartMusicTheme {
        ReviewEmotionScreen()
    }
}
