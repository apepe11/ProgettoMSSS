package com.example.progetto.wear

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

class SensorRepository private constructor(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val isCollecting = AtomicBoolean(false)
    private var heartRateSensor: Sensor? = null
    @Volatile private var latestHeartRate: Float? = null

    private val heartRateSamples = Collections.synchronizedList(mutableListOf<WearableSample>())

    companion object {
        private const val TAG = "WearSensorRepository"

        @Volatile private var instance: SensorRepository? = null

        fun getInstance(context: Context): SensorRepository {
            return instance ?: synchronized(this) {
                instance ?: SensorRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    fun startSensors() {
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        val hrRegistered = heartRateSensor?.let { sensor ->
            registerSensor("hr", sensor)
        } ?: false

        Log.d(TAG, "Sensors registered: hr=$hrRegistered")
    }

    fun stopSensors() {
        sensorManager.unregisterListener(this)
    }

    fun startCollecting() {
        clear()
        isCollecting.set(true)
        Log.d(TAG, "Collection started")
    }

    fun stopCollecting() {
        isCollecting.set(false)
        Log.d(TAG, "Collection stopped: hr=${heartRateSamples.size}")
    }

    fun clear() {
        heartRateSamples.clear()
    }

    fun drainSnapshotWithFallback(): List<WearableSample> {
        val heartRates = heartRateSamples.toList()
        clear()
        val now = System.currentTimeMillis()
        return if (heartRates.isEmpty()) {
            latestHeartRate?.let { listOf(WearableSample(now, it)) } ?: emptyList()
        } else {
            heartRates
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isCollecting.get()) return

        val sample = WearableSample(
            timestamp = System.currentTimeMillis(),
            value = event.values.firstOrNull() ?: return
        )

        when (event.sensor.type) {
            Sensor.TYPE_HEART_RATE -> {
                latestHeartRate = sample.value
                heartRateSamples.add(sample)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun registerSensor(label: String, sensor: Sensor): Boolean {
        return try {
            val registered = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST)
            Log.d(TAG, "Register $label sensor result=$registered sensor=${sensor.describe()}")
            registered
        } catch (e: SecurityException) {
            Log.e(TAG, "Register $label sensor blocked by permission: ${sensor.describe()}", e)
            false
        } catch (e: RuntimeException) {
            Log.e(TAG, "Register $label sensor failed: ${sensor.describe()}", e)
            false
        }
    }

    private fun Sensor.describe(): String {
        val permission = runCatching {
            Sensor::class.java.getMethod("getRequiredPermission").invoke(this) as? String
        }.getOrNull().orEmpty().ifBlank { "unknown" }
        return "name=$name type=$type stringType=$stringType vendor=$vendor permission=$permission wakeUp=$isWakeUpSensor"
    }
}
