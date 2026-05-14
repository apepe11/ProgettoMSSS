package com.example.progetto.utils

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
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
    val eegSamples: Int = 0,
    val latestHeartRate: Float? = null,
    val latestEeg: Float? = null
)

/**
 * Example ViewModel showing how to integrate SensorManager with your app
 * 
 * Usage in your listening/emotion analysis screen:
 * val viewModel = viewModel<SensorCollectionViewModel>(
 *     factory = SensorCollectionViewModel.Factory(context, owner)
 * )
 * viewModel.startListeningSession(songId, userId)
 * viewModel.stopListeningSession()
 */
class SensorCollectionViewModel(
    private val context: Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val sensorManager = SensorManager(context)
    private val networkManager = NetworkManager()
    private val userPreferences = UserPreferences(context)
    
    private var currentSessionId: UUID? = null
    private var currentSongId: UUID? = null
    private var isSessionActive = false
    private var isSessionPaused = false
    private var signalSnapshotJob: Job? = null
    private var wearableHeartRateJob: Job? = null
    private var loggedFirstEegSample = false
    private var eegFrameCount = 0
    private var lastEegFrameId: Long = -1L
    private var lastWearableHrTimestamp: Long = 0
    private val wearableMessageListener = WearableMessageListener()
    private val wearableHeartRateBuffer = mutableListOf<SensorReading>()

    private val _signalSnapshot = MutableStateFlow(LiveSignalSnapshot())
    val signalSnapshot: StateFlow<LiveSignalSnapshot> = _signalSnapshot.asStateFlow()
    
    companion object {
        private const val TAG = "SensorCollectionViewModel"
        private const val EEG_MAX_SAMPLES = 15000
        private const val WEARABLE_MAX_SAMPLES = 6000
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_SESSION_ACTIVE = "session_active"
        private const val KEY_SESSION_PAUSED = "session_paused"
        private const val KEY_SONG_ID = "song_id"
    }

    class Factory(
        private val context: Context,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            if (modelClass.isAssignableFrom(SensorCollectionViewModel::class.java)) {
                val handle = extras.createSavedStateHandle()
                @Suppress("UNCHECKED_CAST")
                return SensorCollectionViewModel(context.applicationContext, handle) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    init {
        val restoredSession = savedStateHandle.get<String>(KEY_SESSION_ID)
        if (!restoredSession.isNullOrBlank()) {
            currentSessionId = runCatching { UUID.fromString(restoredSession) }.getOrNull()
        }
        val restoredSong = savedStateHandle.get<String>(KEY_SONG_ID)
        if (!restoredSong.isNullOrBlank()) {
            currentSongId = runCatching { UUID.fromString(restoredSong) }.getOrNull()
        }
        isSessionActive = savedStateHandle.get<Boolean>(KEY_SESSION_ACTIVE) ?: false
        isSessionPaused = savedStateHandle.get<Boolean>(KEY_SESSION_PAUSED) ?: false
    }

    fun startOrResumeListeningSession(userId: UUID, songId: UUID) {
        when {
            isSessionPaused && currentSongId == songId -> {
                resumeListeningSession()
                return
            }
            isSessionActive && currentSongId == songId -> {
                Log.d(TAG, "Listening session already active for this song, skipping duplicate start")
                return
            }
        }

        startListeningSession(userId, songId)
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
                val sessionResult = networkManager.createTrainingSession(userId, songId)
                val sessionId = sessionResult.getOrNull()
                if (sessionId == null) {
                    isSessionActive = false
                    savedStateHandle[KEY_SESSION_ACTIVE] = false
                    Log.e(TAG, "Failed to create training session", sessionResult.exceptionOrNull())
                    return@launch
                }

                currentSessionId = sessionId
                currentSongId = songId
                savedStateHandle[KEY_SESSION_ID] = sessionId.toString()
                savedStateHandle[KEY_SONG_ID] = songId.toString()
                savedStateHandle[KEY_SESSION_ACTIVE] = true
                savedStateHandle[KEY_SESSION_PAUSED] = false
                userPreferences.saveLastSessionId(sessionId.toString())

                Log.d(TAG, "Starting listening session: $sessionId")
                
                // Start sensor collection
                val status = sensorManager.startCollecting()
                if (!status.hasAnySignal) {
                    Log.w(TAG, "No wearable HR sensors available, continuing to collect EEG only")
                }
                wearableHeartRateBuffer.clear()
                loggedFirstEegSample = false
                eegFrameCount = 0
                lastEegFrameId = -1L
                startWearableMessageCollection()
                WearableSamplingSender.sendStartSampling(context, isInference = false)

                startSignalSnapshotUpdates()
                Log.d(TAG, "Session active with sensors: hr=${status.heartRateRegistered}")
                
            } catch (e: Exception) {
                isSessionActive = false
                savedStateHandle[KEY_SESSION_ACTIVE] = false
                isSessionPaused = false
                savedStateHandle[KEY_SESSION_PAUSED] = false
                Log.e(TAG, "Error starting session", e)
            }
        }
    }

    fun pauseListeningSession() {
        if (!isSessionActive || currentSessionId == null || isSessionPaused) {
            return
        }

        Log.d(TAG, "Pausing listening session: $currentSessionId")
        isSessionPaused = true
        savedStateHandle[KEY_SESSION_PAUSED] = true
        savedStateHandle[KEY_SESSION_ACTIVE] = true
        sensorManager.stopCollecting()
        updateSignalSnapshot(isCollecting = false)
    }

    fun resumeListeningSession() {
        if (!isSessionPaused || currentSessionId == null || currentSongId == null) {
            Log.w(TAG, "No paused session available to resume")
            return
        }

        Log.d(TAG, "Resuming listening session: $currentSessionId")
        isSessionActive = true
        isSessionPaused = false
        savedStateHandle[KEY_SESSION_ACTIVE] = true
        savedStateHandle[KEY_SESSION_PAUSED] = false
        sensorManager.startCollecting()
        updateSignalSnapshot()
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
                isSessionPaused = false
                savedStateHandle[KEY_SESSION_ACTIVE] = false
                savedStateHandle[KEY_SESSION_PAUSED] = false
                stopWearableMessageCollection()
                stopSignalSnapshotUpdates()
                
                // Get collected data
                val hrData = sensorManager.getHeartRateBuffer() + wearableHeartRateBuffer.toList()
                val eegData = sensorManager.getEegBuffer()
                
                Log.d(TAG, "Collected HR samples: ${hrData.size}")
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
                wearableHeartRateBuffer.clear()
                lastWearableHrTimestamp = 0L
                eegFrameCount = 0
                lastEegFrameId = -1L
                currentSessionId = null
                currentSongId = null
                savedStateHandle[KEY_SESSION_ID] = null
                savedStateHandle[KEY_SONG_ID] = null
                
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping session", e)
            }
        }
    }
    
    /**
     * Add EEG data point from MindRove SDK
     * Call this from your ServerManager callback
     */
    fun onEegDataReceived(channel: Int, value: Double, frameId: Long) {
        if (!isSessionActive || isSessionPaused) {
            return
        }

        if (frameId != lastEegFrameId) {
            lastEegFrameId = frameId
            eegFrameCount += 1
        }
        if (eegFrameCount > EEG_MAX_SAMPLES) {
            return
        }
        if (!loggedFirstEegSample) {
            loggedFirstEegSample = true
            Log.d(TAG, "First EEG sample received: channel=$channel value=$value")
        }
        sensorManager.addEegData(channel, value, frameId)
        updateSignalSnapshot()
    }
    
    /**
     * Check if a session is currently active
     */
    fun isCollecting(): Boolean = isSessionActive && !isSessionPaused

    fun isPausedFor(songId: UUID): Boolean = isSessionPaused && currentSongId == songId

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

    private fun updateSignalSnapshot(isCollecting: Boolean = isSessionActive && !isSessionPaused) {
        val hrData = sensorManager.getHeartRateBuffer() + wearableHeartRateBuffer.toList()
        val eegData = sensorManager.getEegBuffer()

        _signalSnapshot.value = LiveSignalSnapshot(
            isCollecting = isCollecting,
            heartRateSamples = hrData.size,
            eegSamples = eegFrameCount,
            latestHeartRate = hrData.lastOrNull()?.value,
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
                if (!isSessionActive || isSessionPaused) {
                    return@collect
                }
                if (heartRates.isNotEmpty()) {
                    appendWearableSamples(
                        source = heartRates,
                        buffer = wearableHeartRateBuffer,
                        type = "hr",
                        maxSize = WEARABLE_MAX_SAMPLES
                    ) { lastWearableHrTimestamp = it }
                    updateSignalSnapshot()
                }
            }
        }
    }

    private fun appendWearableSamples(
        source: List<WearableData>,
        buffer: MutableList<SensorReading>,
        type: String,
        maxSize: Int,
        updateLastTimestamp: (Long) -> Unit
    ) {
        val lastTimestamp = when (type) {
            "hr" -> lastWearableHrTimestamp
            else -> 0L
        }

        val newSamples = source
            .filter { it.timestamp > lastTimestamp }
            .map {
                SensorReading(
                    timestamp = it.timestamp,
                    type = type,
                    value = it.value
                )
            }

        if (newSamples.isEmpty()) return

        buffer.addAll(newSamples)
        if (buffer.size > maxSize) {
            val trimCount = buffer.size - maxSize
            buffer.subList(0, trimCount).clear()
        }
        updateLastTimestamp(newSamples.last().timestamp)
    }

    private fun stopWearableMessageCollection() {
        wearableHeartRateJob?.cancel()
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
