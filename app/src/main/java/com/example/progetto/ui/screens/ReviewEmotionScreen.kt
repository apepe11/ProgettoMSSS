package com.example.progetto.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onOpenDrawer: () -> Unit = {}, // You can actually delete this parameter later if this screen doesn't use it directly anymore!
    onNavigateBack: () -> Unit = {},
    onSaveFeeling: () -> Unit = {}
) {
    // 1. Dropdown State Variables
    val emotionsList = listOf("Happy", "Sad", "Anxious", "Calm", "Energetic")
    var expanded by remember { mutableStateOf(false) }
    var selectedEmotion by remember { mutableStateOf("") }

    // Other State Variables
    var strengthValue by remember { mutableFloatStateOf(0.5f) }
    var description by remember { mutableStateOf("") }

    // OUTER COLUMN: Holds everything. Notice the Scaffold is completely gone!
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp) // We removed paddingValues because the MainActivity handles the top spacing now
    ) {

        // INNER COLUMN: Takes up all empty space (weight 1f) and centers the form
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Domanda 1: The Dropdown Menu
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "How do you feel?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedEmotion,
                        onValueChange = {}, // Left empty because it's read-only
                        readOnly = true,
                        placeholder = { Text("Select an emotion") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(), // Connects the menu to this text field
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Gray,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        emotionsList.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    selectedEmotion = selectionOption
                                    expanded = false // Close menu after selecting
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Domanda 2: Strength Slider
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

            // Domanda 3: Descrizione
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
        } // End of inner centered column

        // Bottoni finali: Pushed all the way to the bottom!
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
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
    }
}

@Preview(showBackground = true)
@Composable
fun ReviewEmotionScreenPreview() {
    HeartMusicTheme {
        ReviewEmotionScreen()
    }
}