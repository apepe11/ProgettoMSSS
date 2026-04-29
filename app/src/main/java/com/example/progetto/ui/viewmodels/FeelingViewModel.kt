package com.example.progetto.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.progetto.data.RetrofitClient
import com.example.progetto.data.SongReviewRequest
import com.example.progetto.data.SongReviewResponse
import kotlinx.coroutines.launch

sealed class FeelingUiState {
    object Loading : FeelingUiState()
    data class Success(val reviews: List<SongReviewResponse>) : FeelingUiState()
    data class Error(val message: String) : FeelingUiState()
}

class FeelingViewModel : ViewModel() {
    var uiState by mutableStateOf<FeelingUiState>(FeelingUiState.Loading)
        private set

    fun loadReviews(userId: String) {
        viewModelScope.launch {
            uiState = FeelingUiState.Loading
            try {
                val response = RetrofitClient.authApiService.getReviews(userId)
                if (response.isSuccessful && response.body() != null) {
                    uiState = FeelingUiState.Success(response.body()!!.reviews)
                } else {
                    uiState = FeelingUiState.Error("Failed to load feelings")
                }
            } catch (e: Exception) {
                uiState = FeelingUiState.Error("Network error: ${e.localizedMessage}")
            }
        }
    }

    fun saveReview(userId: String, valence: Int, arousal: Int, description: String, detectedEmotion: String?, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val request = SongReviewRequest(userId, valence, arousal, description, detectedEmotion)
                val response = RetrofitClient.authApiService.saveReview(request)
                if (response.isSuccessful) {
                    loadReviews(userId)
                    onComplete()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
