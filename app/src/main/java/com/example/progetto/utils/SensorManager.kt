package com.example.progetto.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager as AndroidSensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Data class for sensor measurements
 */
data class SensorReading(
    val timestamp: Long,
    val type: String,  // "hr", "eda", "eeg"
    val value: Float,
    val channel: Int = 0  // for EEG channels
)

data class SensorCollectionStatus(
    val heartRateRegistered: Boolean,
    val edaRegistered: Boolean
) {
    val hasAnySignal: Boolean
        get() = heartRateRegistered || edaRegistered
}

/**
 * SensorManager handles real-time collection of:
 * - Heart Rate (HR) from wearable
 * - Electrodermal Activity (EDA) from wearable
 * - EEG data from MindRove SDK
 */
class SensorManager(private val context: Context) : SensorEventListener {

    private val androidSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as AndroidSensorManager
    
    private var heartRateSensor: Sensor? = null
    private var edaSensor: Sensor? = null
    
    private val _heartRateFlow = MutableSharedFlow<SensorReading>()
    val heartRateFlow: SharedFlow<SensorReading> = _heartRateFlow
    
    private val _edaFlow = MutableSharedFlow<SensorReading>()
    val edaFlow: SharedFlow<SensorReading> = _edaFlow
    
    private val _eegFlow = MutableSharedFlow<SensorReading>()
    val eegFlow: SharedFlow<SensorReading> = _eegFlow
    
    // Buffers for collecting data
    private val heartRateBuffer = mutableListOf<SensorReading>()
    private val edaBuffer = mutableListOf<SensorReading>()
    private val eegBuffer = mutableListOf<SensorReading>()
    
    private var isCollecting = false
    
    companion object {
        private const val TAG = "SensorManager"
        private const val SENSOR_TYPE_EDA = 28  // Generic sensor ID for EDA
    }
    
    fun startCollecting(): SensorCollectionStatus {
        Log.d(TAG, "Starting sensor collection")
        heartRateBuffer.clear()
        edaBuffer.clear()
        eegBuffer.clear()

        var heartRateRegistered = false
        var edaRegistered = false
        
        // Register Heart Rate sensor
        heartRateSensor = androidSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        heartRateSensor?.let {
            androidSensorManager.registerListener(this, it, AndroidSensorManager.SENSOR_DELAY_FASTEST)
            Log.d(TAG, "Heart Rate sensor registered")
            heartRateRegistered = true
        } ?: Log.w(TAG, "Heart Rate sensor not available")
        
        // Register EDA sensor (if available)
        edaSensor = androidSensorManager.getDefaultSensor(SENSOR_TYPE_EDA)
        edaSensor?.let {
            androidSensorManager.registerListener(this, it, AndroidSensorManager.SENSOR_DELAY_FASTEST)
            Log.d(TAG, "EDA sensor registered")
            edaRegistered = true
        } ?: Log.w(TAG, "EDA sensor not available")

        isCollecting = true
        Log.d(TAG, "Collection status: heartRate=$heartRateRegistered, eda=$edaRegistered, active=$isCollecting")
        return SensorCollectionStatus(
            heartRateRegistered = heartRateRegistered,
            edaRegistered = edaRegistered
        )
    }
    
    fun stopCollecting() {
        Log.d(TAG, "Stopping sensor collection")
        isCollecting = false
        androidSensorManager.unregisterListener(this)
        EegSignalTracker.clear()
    }
    
    fun getHeartRateBuffer(): List<SensorReading> = heartRateBuffer.toList()
    fun getEdaBuffer(): List<SensorReading> = edaBuffer.toList()
    fun getEegBuffer(): List<SensorReading> = eegBuffer.toList()

    fun hasAvailableSignalSensors(): Boolean =
        SensorAvailability.hasEmotionSensors(context)

    fun isCollecting(): Boolean = isCollecting
    
    fun clearBuffers() {
        heartRateBuffer.clear()
        edaBuffer.clear()
        eegBuffer.clear()
    }
    
    /**
     * Called when sensor values change
     */
    override fun onSensorChanged(event: SensorEvent?) {
        if (!isCollecting || event == null) return
        
        val timestamp = System.currentTimeMillis()
        
        when (event.sensor.type) {
            Sensor.TYPE_HEART_RATE -> {
                val hrValue = event.values[0]
                val reading = SensorReading(
                    timestamp = timestamp,
                    type = "hr",
                    value = hrValue
                )
                heartRateBuffer.add(reading)
                Log.d(TAG, "HR Reading: $hrValue bpm")
            }
            
            SENSOR_TYPE_EDA -> {
                val edaValue = event.values[0]
                val reading = SensorReading(
                    timestamp = timestamp,
                    type = "eda",
                    value = edaValue
                )
                edaBuffer.add(reading)
                Log.d(TAG, "EDA Reading: $edaValue")
            }
        }
    }
    
    /**
     * Called when sensor accuracy changes
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Sensor accuracy changed: ${sensor?.name} -> $accuracy")
    }
    
    /**
     * Add EEG data from MindRove SDK
     * Call this from your ServerManager callback
     */
    fun addEegData(
        channel: Int,
        value: Double,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (!isCollecting) return
        
        val reading = SensorReading(
            timestamp = timestamp,
            type = "eeg",
            value = value.toFloat(),
            channel = channel
        )
        eegBuffer.add(reading)
        EegSignalTracker.markSample(timestamp)
    }
}
