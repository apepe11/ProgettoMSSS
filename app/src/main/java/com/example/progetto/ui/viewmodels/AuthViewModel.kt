package com.example.progetto.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.progetto.data.*
import com.example.progetto.data.repositories.AuthRepository
import com.example.progetto.utils.UiText
import com.example.progetto.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Rappresenta i possibili stati della schermata di login e registrazione
sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val response: LoginResponse) : LoginUiState()
    data class Error(val message: UiText) : LoginUiState()
}

sealed class ForgotPasswordUiState {
    object Idle : ForgotPasswordUiState()
    object Loading : ForgotPasswordUiState()
    data class Success(val message: String) : ForgotPasswordUiState()
    data class Error(val message: UiText) : ForgotPasswordUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val userPreferences = UserPreferences(application)
    private val authRepository = AuthRepository()
    private val TAG = "AuthViewModel"

    // Questo è lo stato che la UI osserverà per il login/reg
    var uiState by mutableStateOf<LoginUiState>(LoginUiState.Idle)
        private set

    var forgotPasswordState by mutableStateOf<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
        private set

    // Memorizziamo i dati dell'utente loggato
    var currentUser by mutableStateOf<LoginResponse?>(null)

    init {
        // Carichiamo l'utente salvato all'avvio
        viewModelScope.launch {
            val savedId = userPreferences.userId.first()
            val savedName = userPreferences.username.first()
            if (savedId != null && savedName != null) {
                currentUser = LoginResponse("Auto-login", savedId, savedName)
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            uiState = LoginUiState.Loading
            Log.d(TAG, "Login attempt started for email: $email")

            try {
                val cleanPassword = password.trim()
                val request = LoginRequest(email.trim(), cleanPassword)
                
                val response = authRepository.login(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    currentUser = loginResponse
                    userPreferences.saveUser(loginResponse.userId, loginResponse.username ?: "")
                    uiState = LoginUiState.Success(loginResponse)
                } else {
                    uiState = LoginUiState.Error(UiText.StringResource(R.string.error_login_failed))
                }
            } catch (e: Exception) {
                uiState = LoginUiState.Error(UiText.StringResource(R.string.error_network, e.localizedMessage ?: ""))
            }
        }
    }

    fun register(username: String, email: String, password: String, deviceId: String) {
        viewModelScope.launch {
            uiState = LoginUiState.Loading

            try {
                val request = RegisterRequest(username, email, password, deviceId)
                val response = authRepository.register(request)

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    uiState = LoginUiState.Success(loginResponse)
                } else {
                    uiState = LoginUiState.Error(UiText.StringResource(R.string.error_registration_failed, response.message()))
                }
            } catch (e: Exception) {
                uiState = LoginUiState.Error(UiText.StringResource(R.string.error_network, e.localizedMessage ?: ""))
            }
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            forgotPasswordState = ForgotPasswordUiState.Loading
            try {
                val response = authRepository.forgotPassword(email.trim())
                if (response.isSuccessful && response.body() != null) {
                    val message = response.body()!!["message"] ?: "Instructions sent"
                    forgotPasswordState = ForgotPasswordUiState.Success(message)
                } else {
                    val errorMsg = if (response.code() == 404) {
                        UiText.StringResource(R.string.error_user_not_found)
                    } else {
                        UiText.StringResource(R.string.error_unknown)
                    }
                    forgotPasswordState = ForgotPasswordUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                forgotPasswordState = ForgotPasswordUiState.Error(UiText.StringResource(R.string.error_network, e.localizedMessage ?: ""))
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userPreferences.clear()
            currentUser = null
            uiState = LoginUiState.Idle
        }
    }

    fun resetState() {
        uiState = LoginUiState.Idle
    }
}
