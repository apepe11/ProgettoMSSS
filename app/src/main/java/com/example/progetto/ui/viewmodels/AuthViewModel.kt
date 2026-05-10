package com.example.progetto.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.progetto.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Rappresenta i possibili stati della schermata di login e registrazione
sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val response: LoginResponse) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val userPreferences = UserPreferences(application)
    private val TAG = "AuthViewModel"

    // Questo è lo stato che la UI osserverà per il login/reg
    var uiState by mutableStateOf<LoginUiState>(LoginUiState.Idle)
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
                // Rimuoviamo eventuali spazi o "a capo" dalla password
                val cleanPassword = password.trim()
                val request = LoginRequest(email.trim(), cleanPassword)
                Log.d(TAG, "Sending login request: $request")
                val response = RetrofitClient.authApiService.login(request)
                Log.d(TAG, "Login response received: code=${response.code()}, isSuccessful=${response.isSuccessful}")

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    Log.d(TAG, "Login successful: user_id=${loginResponse.userId}, username=${loginResponse.username}")
                    currentUser = loginResponse
                    // Salva l'utente permanentemente
                    userPreferences.saveUser(loginResponse.userId, loginResponse.username ?: "")
                    uiState = LoginUiState.Success(loginResponse)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.w(TAG, "Login failed: ${response.code()} - $errorBody")
                    uiState = LoginUiState.Error("Email o password errati")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login exception: ${e.message}", e)
                uiState = LoginUiState.Error("Errore di connessione: ${e.localizedMessage}")
            }
        }
    }

    fun register(username: String, email: String, password: String, deviceId: String) {
        viewModelScope.launch {
            uiState = LoginUiState.Loading

            try {
                val request = RegisterRequest(username, email, password, deviceId)
                val response = RetrofitClient.authApiService.register(request)

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    // After registration, we DON'T auto-login as per user request to go through sign-in screen
                    // currentUser = finalUser
                    // userPreferences.saveUser(finalUser.userId, finalUser.username ?: "")
                    uiState = LoginUiState.Success(loginResponse)
                } else {
                    uiState = LoginUiState.Error("Errore durante la registrazione: ${response.message()}")
                }
            } catch (e: Exception) {
                uiState = LoginUiState.Error("Errore di connessione: ${e.localizedMessage}")
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
