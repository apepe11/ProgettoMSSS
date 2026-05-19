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
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
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
    private val _predictedEmotion = MutableStateFlow<String?>(null)
    val predictedEmotion: StateFlow<String?> = _predictedEmotion.asStateFlow()
    private var loggedFirstEegSample = false
    private var eegFrameCount = 0
    private var lastEegFrameId: Long = -1L
    private var lastWearableHrTimestamp: Long = 0
    private val wearableMessageListener = WearableMessageListener()
    private val wearableHeartRateBuffer = mutableListOf<SensorReading>()
    private val sessionTransitionMutex = Mutex()

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
        viewModelScope.launch {
            sessionTransitionMutex.withLock {
                when {
                    isSessionPaused && currentSongId == songId -> {
                        resumeListeningSession()
                        return@withLock
                    }
                    isSessionActive && currentSongId == songId -> {
                        Log.d(TAG, "Listening session already active for this song, skipping duplicate start")
                        return@withLock
                    }
                    isSessionActive && currentSongId != songId -> {
                        Log.d(TAG, "Stopping previous paused/active session before starting a new song")
                        stopListeningSessionInternal()
                    }
                }

                startListeningSessionInternal(userId, songId)
            }
        }
    }
    
    /**
     * Start a new listening session and begin collecting sensor data
     */
    fun startListeningSession(userId: UUID, songId: UUID) {
        viewModelScope.launch {
            startListeningSessionInternal(userId, songId)
        }
    }

    private suspend fun startListeningSessionInternal(userId: UUID, songId: UUID) {
        if (isSessionActive) {
            Log.d(TAG, "Listening session already active, skipping duplicate start")
            return
        }

        isSessionActive = true
        try {
            val sessionResult = networkManager.createTrainingSession(userId, songId)
            val sessionId = sessionResult.getOrNull()
            if (sessionId == null) {
                isSessionActive = false
                savedStateHandle[KEY_SESSION_ACTIVE] = false
                Log.e(TAG, "Failed to create training session", sessionResult.exceptionOrNull())
                return
            }

            currentSessionId = sessionId
            currentSongId = songId
            _predictedEmotion.value = null
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
            sessionTransitionMutex.withLock {
                stopListeningSessionInternal(userRating)
            }
        }
    }

    private suspend fun stopListeningSessionInternal(userRating: Int = 3) {
        try {
            val sessionId = currentSessionId
            if (!isSessionActive || sessionId == null) {
                Log.w(TAG, "No active session to stop")
                return
            }

            Log.d(TAG, "Stopping listening session: $sessionId")

            // Stop sensor collection
            sensorManager.stopCollecting()
            isSessionActive = false
            isSessionPaused = false
            savedStateHandle[KEY_SESSION_ACTIVE] = false
            savedStateHandle[KEY_SESSION_PAUSED] = false
            stopWearableMessageCollection()
            stopSignalSnapshotUpdates()

            // Get collected data before clearing buffers
            val hrData = sensorManager.getHeartRateBuffer() + wearableHeartRateBuffer.toList()
            val eegData = sensorManager.getEegBuffer()

            Log.d(TAG, "Collected HR samples: ${hrData.size}")
            Log.d(TAG, "Collected EEG samples: ${eegData.size}")

            // Send data to backend for training
            if (hrData.isNotEmpty()) {
                val hrResult = networkManager.sendWearableData(
                    sessionId = sessionId,
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
                    sessionId = sessionId,
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

            if (hrData.isEmpty() && eegData.isEmpty()) {
                _predictedEmotion.value = "No instrumented sensor data was collected for this session."
            } else {
                _predictedEmotion.value = "Analysis in progress..."

                val predictionResult = withTimeoutOrNull(30000) {
                    networkManager.predictEmotion(sessionId)
                }

                if (predictionResult == null) {
                    Log.e(TAG, "Emotion prediction timed out for session: $sessionId")
                    _predictedEmotion.value = "Analysis unavailable (timeout)"
                } else {
                    predictionResult.onSuccess {
                        _predictedEmotion.value = it
                        Log.d(TAG, "Emotion prediction completed: $it")
                    }.onFailure {
                        Log.e(TAG, "Emotion prediction failed", it)
                        _predictedEmotion.value = "Analysis unavailable"
                    }
                }
            }

            // Clear buffers and reset snapshot state after stopping
            sensorManager.clearBuffers()
            wearableHeartRateBuffer.clear()
            lastWearableHrTimestamp = 0L
            eegFrameCount = 0
            lastEegFrameId = -1L

            updateSignalSnapshot(isCollecting = false)

            currentSessionId = null
            currentSongId = null
            savedStateHandle[KEY_SESSION_ID] = null
            savedStateHandle[KEY_SONG_ID] = null

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping session", e)
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
                delay(1000)
                updateSignalSnapshot()
            }
        }
    }

    private fun stopSignalSnapshotUpdates() {
        signalSnapshotJob?.cancel()
        signalSnapshotJob = null
        updateSignalSnapshot(isCollecting = false)
    }

    private fun updateSignalSnapshot(isCollecting: Boolean = true) {
        val hr = sensorManager.getHeartRateBuffer() + wearableHeartRateBuffer.toList()
        val eeg = sensorManager.getEegBuffer()
        
        _signalSnapshot.value = LiveSignalSnapshot(
            isCollecting = isCollecting && isSessionActive && !isSessionPaused,
            heartRateSamples = hr.size,
            eegSamples = eeg.size,
            latestHeartRate = hr.lastOrNull()?.value,
            latestEeg = eeg.lastOrNull()?.value
        )
    }

    private fun startWearableMessageCollection() {
        Log.d(TAG, "Registering WearableMessageListener")
        Wearable.getMessageClient(context).addListener(wearableMessageListener)
    }

    private fun stopWearableMessageCollection() {
        Log.d(TAG, "Removing WearableMessageListener")
        Wearable.getMessageClient(context).removeListener(wearableMessageListener)
    }

    inner class WearableMessageListener : com.google.android.gms.wearable.MessageClient.OnMessageReceivedListener {
        override fun onMessageReceived(messageEvent: com.google.android.gms.wearable.MessageEvent) {
            when (messageEvent.path) {
                "/sensor_series" -> {
                    val payload = String(messageEvent.data)
                    try {
                        val json = JSONObject(payload)
                        val heartRateJsonArray = json.getJSONArray("heart_rate")
                        for (i in 0 until heartRateJsonArray.length()) {
                            val entry = heartRateJsonArray.getJSONObject(i)
                            val timestamp = entry.getLong("timestamp")
                            val value = entry.getDouble("value").toFloat()
                            if (timestamp > lastWearableHrTimestamp) {
                                lastWearableHrTimestamp = timestamp
                                if (isSessionActive && !isSessionPaused && wearableHeartRateBuffer.size < WEARABLE_MAX_SAMPLES) {
                                    wearableHeartRateBuffer.add(SensorReading(timestamp, "hr", value))
                                }
                            }
                        }
                        Log.d(TAG, "Wearable HR packet received: ${heartRateJsonArray.length()} samples")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing wearable JSON", e)
                    }
                }
                "/hr_data" -> {
                    val data = String(messageEvent.data).split(",")
                    if (data.size >= 2) {
                        val value = data[0].toFloatOrNull() ?: return
                        val timestamp = data[1].toLongOrNull() ?: System.currentTimeMillis()

                        if (timestamp > lastWearableHrTimestamp) {
                            lastWearableHrTimestamp = timestamp
                            if (isSessionActive && !isSessionPaused && wearableHeartRateBuffer.size < WEARABLE_MAX_SAMPLES) {
                                wearableHeartRateBuffer.add(SensorReading(timestamp, "hr", value))
                            }
                        }
                    }
                }
                else -> {
                    Log.w(TAG, "Ignoring wearable path: ${messageEvent.path}")
                }
            }
        }
    }
}
