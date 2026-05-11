package com.example.progetto.ui.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// ✅ ADD THESE IMPORTS
import com.example.progetto.data.SongReviewResponse
import com.example.progetto.data.SongReviewRequest
import com.example.progetto.data.repositories.AuthRepository
import com.example.progetto.utils.UiText
import com.example.progetto.R
import kotlinx.coroutines.launch

sealed class FeelingUiState {
    object Loading : FeelingUiState()
    // This will now be recognized
    data class Success(val reviews: List<SongReviewResponse>) : FeelingUiState()
    data class Error(val message: UiText) : FeelingUiState()
}

class FeelingViewModel : ViewModel() {
    private val authRepository = AuthRepository()

    var uiState by mutableStateOf<FeelingUiState>(FeelingUiState.Loading)
        private set

    companion object {
        private const val TAG = "FeelingViewModel"
    }

    fun loadReviews(userId: String) {
        viewModelScope.launch {
            uiState = FeelingUiState.Loading
            try {
                val response = authRepository.getReviews(userId)
                if (response.isSuccessful && response.body() != null) {
                    // This matches your AuthApi/Repository return type
                    uiState = FeelingUiState.Success(response.body()!!.reviews)
                } else {
                    uiState = FeelingUiState.Error(UiText.StringResource(R.string.error_load_feelings))
                }
            } catch (e: Exception) {
                uiState = FeelingUiState.Error(UiText.StringResource(R.string.error_network, e.localizedMessage ?: ""))
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
                // ✅ SongReviewRequest will now be recognized
                val request = SongReviewRequest(userId, sessionId, valence, arousal, description, detectedEmotion)
                val response = authRepository.saveReview(request)
                if (response.isSuccessful) {
                    Log.d(TAG, "Review saved successfully")
                    loadReviews(userId)
                    onComplete()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Failed to save review: ${response.code()} body=$errorBody")
                    uiState = FeelingUiState.Error(UiText.StringResource(R.string.error_save_feelings))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error saving review", e)
                uiState = FeelingUiState.Error(UiText.StringResource(R.string.error_network, e.localizedMessage ?: ""))
            }
        }
    }
}