package com.example.progetto.utils

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.progetto.data.UserPreferences
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Example ViewModel showing how to integrate SensorManager with your app
 * 
 * Usage in your listening/emotion analysis screen:
 * val viewModel = SensorCollectionViewModel(context)
 * viewModel.startListeningSession(songId, userId)
 * viewModel.stopListeningSession()
 */
class SensorCollectionViewModel(private val context: Context) : ViewModel() {
    
    private val sensorManager = SensorManager(context)
    private val networkManager = NetworkManager()
    private val userPreferences = UserPreferences(context)
    
    private var currentSessionId: UUID? = null
    private var isSessionActive = false
    
    companion object {
        private const val TAG = "SensorCollectionViewModel"
    }
    
    /**
     * Start a new listening session and begin collecting sensor data
     */
    fun startListeningSession(userId: UUID, songId: UUID) {
        viewModelScope.launch {
            try {
                // Create session on backend
                val sessionId = UUID.randomUUID()  // In production, create via backend API
                currentSessionId = sessionId
                userPreferences.saveLastSessionId(sessionId.toString())
                
                Log.d(TAG, "Starting listening session: $sessionId")
                
                // Start sensor collection
                val status = sensorManager.startCollecting()
                if (!status.hasAnySignal) {
                    Log.w(TAG, "No wearable HR/EDA sensors available, continuing to collect EEG only")
                }

                isSessionActive = true
                Log.d(TAG, "Session active with sensors: hr=${status.heartRateRegistered}, eda=${status.edaRegistered}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting session", e)
            }
        }
    }
    
    /**
     * Stop the listening session and upload collected data
     */
    fun stopListeningSession(userRating: Int = 3) {
        viewModelScope.launch {
            try {
                if (!isSessionActive || currentSessionId == null) {
                    Log.w(TAG, "No active session to stop")
                    return@launch
                }
                
                Log.d(TAG, "Stopping listening session: $currentSessionId")
                
                // Stop sensor collection
                sensorManager.stopCollecting()
                isSessionActive = false
                userPreferences.clearLastSessionId()
                
                // Get collected data
                val hrData = sensorManager.getHeartRateBuffer()
                val edaData = sensorManager.getEdaBuffer()
                val eegData = sensorManager.getEegBuffer()
                
                Log.d(TAG, "Collected HR samples: ${hrData.size}")
                Log.d(TAG, "Collected EDA samples: ${edaData.size}")
                Log.d(TAG, "Collected EEG samples: ${eegData.size}")
                
                // Send data to backend for training
                if (hrData.isNotEmpty()) {
                    val hrResult = networkManager.sendWearableData(
                        sessionId = currentSessionId!!,
                        sensorType = "hr",
                        subject = "user",
                        rating = userRating,
                        sensorBuffer = hrData
                    )
                    hrResult.onSuccess {
                        Log.d(TAG, "HR data sent successfully")
                    }.onFailure {
                        Log.e(TAG, "Failed to send HR data", it)
                    }
                }
                
                if (edaData.isNotEmpty()) {
                    val edaResult = networkManager.sendWearableData(
                        sessionId = currentSessionId!!,
                        sensorType = "eda",
                        subject = "user",
                        rating = userRating,
                        sensorBuffer = edaData
                    )
                    edaResult.onSuccess {
                        Log.d(TAG, "EDA data sent successfully")
                    }.onFailure {
                        Log.e(TAG, "Failed to send EDA data", it)
                    }
                }
                
                if (eegData.isNotEmpty()) {
                    val eegResult = networkManager.sendEegData(
                        sessionId = currentSessionId!!,
                        subject = "user",
                        rating = userRating,
                        eegBuffer = eegData
                    )
                    eegResult.onSuccess {
                        Log.d(TAG, "EEG data sent successfully")
                    }.onFailure {
                        Log.e(TAG, "Failed to send EEG data", it)
                    }
                }
                
                // Clear buffers
                sensorManager.clearBuffers()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping session", e)
            }
        }
    }
    
    /**
     * Add EEG data point from MindRove SDK
     * Call this from your ServerManager callback
     */
    fun onEegDataReceived(channel: Int, value: Double) {
        if (isSessionActive) {
            sensorManager.addEegData(channel, value)
        }
    }
    
    /**
     * Check if a session is currently active
     */
    fun isCollecting(): Boolean = isSessionActive
    
    override fun onCleared() {
        super.onCleared()
        if (isSessionActive) {
            sensorManager.stopCollecting()
        }
    }
}
