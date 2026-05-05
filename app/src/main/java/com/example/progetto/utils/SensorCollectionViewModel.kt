package com.example.progetto.utils

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.Wearable
import com.example.progetto.data.UserPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class LiveSignalSnapshot(
    val isCollecting: Boolean = false,
    val heartRateSamples: Int = 0,
    val edaSamples: Int = 0,
    val eegSamples: Int = 0,
    val latestHeartRate: Float? = null,
    val latestEda: Float? = null,
    val latestEeg: Float? = null
)

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
    private var signalSnapshotJob: Job? = null
    private var wearableHeartRateJob: Job? = null
    private var wearableEdaJob: Job? = null
    private var loggedFirstEegSample = false
    private val wearableMessageListener = WearableMessageListener()
    private val wearableHeartRateBuffer = mutableListOf<SensorReading>()
    private val wearableEdaBuffer = mutableListOf<SensorReading>()

    private val _signalSnapshot = MutableStateFlow(LiveSignalSnapshot())
    val signalSnapshot: StateFlow<LiveSignalSnapshot> = _signalSnapshot.asStateFlow()
    
    companion object {
        private const val TAG = "SensorCollectionViewModel"
        @Volatile private var instance: SensorCollectionViewModel? = null

        fun get(context: Context): SensorCollectionViewModel {
            val appContext = context.applicationContext
            return instance ?: synchronized(this) {
                instance ?: SensorCollectionViewModel(appContext).also { instance = it }
            }
        }
    }
    
    /**
     * Start a new listening session and begin collecting sensor data
     */
    fun startListeningSession(userId: UUID, songId: UUID) {
        if (isSessionActive) {
            Log.d(TAG, "Listening session already active, skipping duplicate start")
            return
        }

        isSessionActive = true
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
                wearableHeartRateBuffer.clear()
                wearableEdaBuffer.clear()
                loggedFirstEegSample = false
                startWearableMessageCollection()
                WearableSamplingSender.sendStartSampling(context, isInference = false)

                startSignalSnapshotUpdates()
                Log.d(TAG, "Session active with sensors: hr=${status.heartRateRegistered}, eda=${status.edaRegistered}")
                
            } catch (e: Exception) {
                isSessionActive = false
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
                stopWearableMessageCollection()
                stopSignalSnapshotUpdates()
                userPreferences.clearLastSessionId()
                
                // Get collected data
                val hrData = sensorManager.getHeartRateBuffer() + wearableHeartRateBuffer.toList()
                val edaData = sensorManager.getEdaBuffer() + wearableEdaBuffer.toList()
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
            if (!loggedFirstEegSample) {
                loggedFirstEegSample = true
                Log.d(TAG, "First EEG sample received: channel=$channel value=$value")
            }
            sensorManager.addEegData(channel, value)
            updateSignalSnapshot()
        }
    }
    
    /**
     * Check if a session is currently active
     */
    fun isCollecting(): Boolean = isSessionActive

    private fun startSignalSnapshotUpdates() {
        signalSnapshotJob?.cancel()
        signalSnapshotJob = viewModelScope.launch {
            while (isSessionActive) {
                updateSignalSnapshot()
                delay(500)
            }
        }
    }

    private fun stopSignalSnapshotUpdates() {
        signalSnapshotJob?.cancel()
        updateSignalSnapshot(isCollecting = false)
    }

    private fun updateSignalSnapshot(isCollecting: Boolean = isSessionActive) {
        val hrData = sensorManager.getHeartRateBuffer() + wearableHeartRateBuffer.toList()
        val edaData = sensorManager.getEdaBuffer() + wearableEdaBuffer.toList()
        val eegData = sensorManager.getEegBuffer()

        _signalSnapshot.value = LiveSignalSnapshot(
            isCollecting = isCollecting,
            heartRateSamples = hrData.size,
            edaSamples = edaData.size,
            eegSamples = eegData.size,
            latestHeartRate = hrData.lastOrNull()?.value,
            latestEda = edaData.lastOrNull()?.value,
            latestEeg = eegData.lastOrNull()?.value
        )
    }

    private fun startWearableMessageCollection() {
        Log.d(TAG, "Registering WearableMessageListener")
        Wearable.getMessageClient(context).addListener(wearableMessageListener)

        wearableHeartRateJob?.cancel()
        wearableHeartRateJob = viewModelScope.launch {
            WearableMessageListener.heartRateFlow.collect { heartRates ->
                Log.d(TAG, "Wearable HR packet received: ${heartRates.size} samples")
                if (heartRates.isNotEmpty()) {
                    wearableHeartRateBuffer.clear()
                    wearableHeartRateBuffer.addAll(
                        heartRates.map {
                            SensorReading(
                                timestamp = it.timestamp,
                                type = "hr",
                                value = it.value
                            )
                        }
                    )
                    updateSignalSnapshot()
                }
            }
        }

        wearableEdaJob?.cancel()
        wearableEdaJob = viewModelScope.launch {
            WearableMessageListener.edaFlow.collect { edaValues ->
                Log.d(TAG, "Wearable EDA packet received: ${edaValues.size} samples")
                if (edaValues.isNotEmpty()) {
                    wearableEdaBuffer.clear()
                    wearableEdaBuffer.addAll(
                        edaValues.map {
                            SensorReading(
                                timestamp = it.timestamp,
                                type = "eda",
                                value = it.value
                            )
                        }
                    )
                    updateSignalSnapshot()
                }
            }
        }
    }

    private fun stopWearableMessageCollection() {
        wearableHeartRateJob?.cancel()
        wearableEdaJob?.cancel()
        Log.d(TAG, "Removing WearableMessageListener")
        Wearable.getMessageClient(context).removeListener(wearableMessageListener)
    }

    override fun onCleared() {
        super.onCleared()
        if (isSessionActive) {
            sensorManager.stopCollecting()
        }
        stopWearableMessageCollection()
        signalSnapshotJob?.cancel()
    }
}
