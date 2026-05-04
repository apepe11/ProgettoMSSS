package com.example.progetto.ui.viewmodels

import android.util.Log
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

    companion object {
        private const val TAG = "FeelingViewModel"
    }

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

    fun saveReview(
        userId: String,
        sessionId: String?,
        valence: Int,
        arousal: Int,
        description: String,
        detectedEmotion: String?,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val request = SongReviewRequest(userId, sessionId, valence, arousal, description, detectedEmotion)
                val response = RetrofitClient.authApiService.saveReview(request)
                if (response.isSuccessful) {
                    Log.d(TAG, "Review saved successfully")
                    loadReviews(userId)
                    onComplete()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Failed to save review: ${response.code()} ${response.message()} body=$errorBody")
                    uiState = FeelingUiState.Error("Failed to save feelings")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error saving review", e)
                uiState = FeelingUiState.Error("Network error: ${e.localizedMessage}")
            }
        }
    }
}
