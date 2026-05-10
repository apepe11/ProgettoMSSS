package com.example.progetto.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import android.app.Application
import com.example.progetto.ui.components.HeartButton
import com.example.progetto.ui.theme.HeartMusicTheme
import com.example.progetto.ui.viewmodels.AuthViewModel
import com.example.progetto.ui.viewmodels.FeelingViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.progetto.data.UserPreferences
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

import androidx.compose.ui.res.stringResource
import com.example.progetto.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewEmotionScreen(
    onOpenDrawer: () -> Unit = {}, // You can actually delete this parameter later if this screen doesn't use it directly anymore!
    onNavigateBack: () -> Unit = {},
    onSaveFeeling: () -> Unit = {},
    authViewModel: AuthViewModel,
    feelingViewModel: FeelingViewModel = viewModel()
) {
    // 1. Dropdown State Variables
    val emotionsList = listOf(
        stringResource(R.string.emotion_happy),
        stringResource(R.string.emotion_sad),
        stringResource(R.string.emotion_anxious),
        stringResource(R.string.emotion_calm),
        stringResource(R.string.emotion_energetic)
    )
    var expanded by remember { mutableStateOf(false) }
    var selectedEmotion by remember { mutableStateOf("") }

    // Other State Variables
    var strengthValue by remember { mutableFloatStateOf(0.5f) }
    var description by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val userId = authViewModel.currentUser?.userId
    val sessionId by produceState<String?>(initialValue = null, key1 = userId) {
        val prefs = UserPreferences(context)
        value = prefs.lastSessionId.first()
    }

    val happy = stringResource(R.string.emotion_happy)
    val sad = stringResource(R.string.emotion_sad)
    val anxious = stringResource(R.string.emotion_anxious)
    val calm = stringResource(R.string.emotion_calm)
    val energetic = stringResource(R.string.emotion_energetic)

    val valenceByEmotion = mapOf(
        happy to 8,
        sad to 2,
        anxious to 3,
        calm to 6,
        energetic to 7
    )

    // OUTER COLUMN: Holds everything. Notice the Scaffold is completely gone!
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                    text = stringResource(R.string.review_feel_question),
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
                        placeholder = { Text(stringResource(R.string.review_select_emotion)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(), // Connects the menu to this text field
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
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
                    text = stringResource(R.string.review_strength_question),
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
                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Domanda 3: Descrizione
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.review_describe_feeling)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
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
                text = stringResource(R.string.review_go_back),
                onClick = onNavigateBack,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
            )
            HeartButton(
                text = stringResource(R.string.review_save_feeling),
                onClick = {
                    val resolvedUserId = userId
                    if (resolvedUserId.isNullOrBlank()) {
                        errorText = context.getString(R.string.review_error_session)
                        return@HeartButton
                    }
                    if (selectedEmotion.isBlank()) {
                        errorText = context.getString(R.string.review_error_select)
                        return@HeartButton
                    }
                    val valence = valenceByEmotion[selectedEmotion] ?: 5
                    val arousal = (strengthValue * 10f).roundToInt().coerceIn(0, 10)
                    errorText = null
                    feelingViewModel.saveReview(
                        userId = resolvedUserId,
                        sessionId = sessionId,
                        valence = valence,
                        arousal = arousal,
                        description = description,
                        detectedEmotion = selectedEmotion
                    ) {
                        onSaveFeeling()
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }

        errorText?.let { message ->
            Text(
                text = message,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReviewEmotionScreenPreview() {
    HeartMusicTheme {
        val context = LocalContext.current
        val authViewModel = remember { AuthViewModel(context.applicationContext as Application) }
        ReviewEmotionScreen(authViewModel = authViewModel)
    }
}