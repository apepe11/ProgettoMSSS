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
import mylibrary.mindrove.SensorData
import mylibrary.mindrove.ServerManager

data class LiveSignalSnapshot(
    val isCollecting: Boolean = false,
    val heartRateSamples: Int = 0,
    val edaSamples: Int = 0,
    val eegSamples: Int = 0,
    val latestHeartRate: Float? = null,
    val latestEda: Float? = null,
    val latestEeg: Float? = null,
    val isEegConnected: Boolean = false
)

/**
 * ViewModel managing sensor data collection from both Wear OS and EEG (MindRove).
 * Following MVVM, this class encapsulates the logic for data gathering, 
 * keeping it out of the Activity/UI layer.
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

    // MindRove ServerManager integrated in ViewModel (Point 1: Logic in ViewModel)
    private val serverManager = ServerManager { sensorData: SensorData ->
        handleEegData(sensorData)
    }
    
    init {
        serverManager.start()
        Log.d(TAG, "ServerManager started in ViewModel, ip=${serverManager.ipAddress}")
    }

    private fun handleEegData(sensorData: SensorData) {
        if (!loggedFirstEegSample) {
            loggedFirstEegSample = true
            Log.d(TAG, "First EEG sample: measurements=${sensorData.numberOfMeasurement}")
        }
        EegSignalTracker.markSample(System.currentTimeMillis())
        
        // Feed data to the manager
        onEegDataReceived(0, sensorData.channel1.toDouble())
        onEegDataReceived(1, sensorData.channel2.toDouble())
        onEegDataReceived(2, sensorData.channel3.toDouble())
        onEegDataReceived(3, sensorData.channel4.toDouble())
        onEegDataReceived(4, sensorData.channel5.toDouble())
        onEegDataReceived(5, sensorData.channel6.toDouble())
    }

    companion object {
        private const val TAG = "SensorCollectionVM"
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
                val sessionId = UUID.randomUUID() 
                currentSessionId = sessionId
                userPreferences.saveLastSessionId(sessionId.toString())
                
                Log.d(TAG, "Starting listening session: $sessionId")
                
                // Start sensor collection
                val status = sensorManager.startCollecting()
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
                
                // Send data to backend...
                if (hrData.isNotEmpty()) {
                    networkManager.sendWearableData(currentSessionId!!, "hr", "user", userRating, hrData)
                }
                if (edaData.isNotEmpty()) {
                    networkManager.sendWearableData(currentSessionId!!, "eda", "user", userRating, edaData)
                }
                if (eegData.isNotEmpty()) {
                    networkManager.sendEegData(currentSessionId!!, "user", userRating, eegData)
                }
                
                sensorManager.clearBuffers()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping session", e)
            }
        }
    }
    
    private fun onEegDataReceived(channel: Int, value: Double) {
        if (isSessionActive) {
            sensorManager.addEegData(channel, value)
            updateSignalSnapshot()
        }
    }
    
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
            latestEeg = eegData.lastOrNull()?.value,
            isEegConnected = true // If we are receiving data, it's connected
        )
    }

    private fun startWearableMessageCollection() {
        Wearable.getMessageClient(context).addListener(wearableMessageListener)

        wearableHeartRateJob?.cancel()
        wearableHeartRateJob = viewModelScope.launch {
            WearableMessageListener.heartRateFlow.collect { heartRates ->
                if (heartRates.isNotEmpty()) {
                    wearableHeartRateBuffer.clear()
                    wearableHeartRateBuffer.addAll(heartRates.map { SensorReading(it.timestamp, "hr", it.value) })
                    updateSignalSnapshot()
                }
            }
        }

        wearableEdaJob?.cancel()
        wearableEdaJob = viewModelScope.launch {
            WearableMessageListener.edaFlow.collect { edaValues ->
                if (edaValues.isNotEmpty()) {
                    wearableEdaBuffer.clear()
                    wearableEdaBuffer.addAll(edaValues.map { SensorReading(it.timestamp, "eda", it.value) })
                    updateSignalSnapshot()
                }
            }
        }
    }

    private fun stopWearableMessageCollection() {
        wearableHeartRateJob?.cancel()
        wearableEdaJob?.cancel()
        Wearable.getMessageClient(context).removeListener(wearableMessageListener)
    }

    override fun onCleared() {
        super.onCleared()
        serverManager.stop()
        if (isSessionActive) {
            sensorManager.stopCollecting()
        }
        stopWearableMessageCollection()
        signalSnapshotJob?.cancel()
    }
}
