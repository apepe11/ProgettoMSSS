package com.example.progetto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.progetto.data.SongReviewResponse
import com.example.progetto.ui.theme.HeartPurple
import com.example.progetto.ui.viewmodels.AuthViewModel
import com.example.progetto.ui.viewmodels.FeelingUiState
import com.example.progetto.ui.viewmodels.FeelingViewModel

@Composable
fun YourFeelingsScreen(
    onOpenDrawer: () -> Unit = {},
    authViewModel: AuthViewModel,
    feelingViewModel: FeelingViewModel = viewModel()
) {
    val userId = authViewModel.currentUser?.userId
    val uiState = feelingViewModel.uiState

    // Carica le review all'avvio
    LaunchedEffect(userId) {
        userId?.let { feelingViewModel.loadReviews(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Header con solo il Titolo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Your Feelings",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HeartPurple
            )
        }

        when (uiState) {
            is FeelingUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = HeartPurple)
                }
            }
            is FeelingUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.message, color = Color.Red)
                }
            }
            is FeelingUiState.Success -> {
                if (uiState.reviews.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No feelings recorded yet.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(uiState.reviews) { review ->
                            FeelingEntryItem(review)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeelingEntryItem(review: SongReviewResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.height(110.dp)
            ) {
                // Parametro 1 (Valence)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(70.dp)
                        .background(HeartPurple.copy(alpha = 0.05f))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "Vibe", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        text = "${review.valence}°",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = HeartPurple
                    )
                }

                // Separatore verticale
                Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Color(0xFFEEEEEE)))

                // Parametro 2 (Arousal)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(70.dp)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "Energy", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        text = "${review.arousal}°",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = HeartPurple
                    )
                }

                // Separatore verticale
                Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Color(0xFFEEEEEE)))

                // Colonna Description
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = "DESCRIPTION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = HeartPurple.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = review.description,
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
            }
            
            // Badge Emozione Rilevata
            review.detectedEmotion?.let { emotion ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HeartPurple.copy(alpha = 0.1f))
                        .padding(vertical = 4.dp, horizontal = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Detected Emotion:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = emotion,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HeartPurple
                        )
                    }
                }
            }
        }
    }
}
