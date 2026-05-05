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
    private var edaSensor: Sensor? = null

    private val heartRateSamples = Collections.synchronizedList(mutableListOf<WearableSample>())
    private val edaSamples = Collections.synchronizedList(mutableListOf<WearableSample>())

    companion object {
        private const val TAG = "WearSensorRepository"
        private const val SENSOR_TYPE_EDA_FOODBACK = 65554
        private const val SENSOR_TYPE_EDA_GENERIC = 28

        @Volatile private var instance: SensorRepository? = null

        fun getInstance(context: Context): SensorRepository {
            return instance ?: synchronized(this) {
                instance ?: SensorRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    fun startSensors() {
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        edaSensor = findEdaSensor()

        val hrRegistered = heartRateSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        } ?: false

        val edaRegistered = edaSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        } ?: false

        Log.d(TAG, "Sensors registered: hr=$hrRegistered eda=$edaRegistered edaSensor=${edaSensor?.name}")
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
        Log.d(TAG, "Collection stopped: hr=${heartRateSamples.size} eda=${edaSamples.size}")
    }

    fun snapshot(): Pair<List<WearableSample>, List<WearableSample>> {
        return heartRateSamples.toList() to edaSamples.toList()
    }

    fun clear() {
        heartRateSamples.clear()
        edaSamples.clear()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isCollecting.get()) return

        val sample = WearableSample(
            timestamp = System.currentTimeMillis(),
            value = event.values.firstOrNull() ?: return
        )

        when (event.sensor.type) {
            Sensor.TYPE_HEART_RATE -> heartRateSamples.add(sample)
            SENSOR_TYPE_EDA_FOODBACK, SENSOR_TYPE_EDA_GENERIC -> edaSamples.add(sample)
            else -> {
                if (event.sensor == edaSensor) {
                    edaSamples.add(sample)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun findEdaSensor(): Sensor? {
        sensorManager.getDefaultSensor(SENSOR_TYPE_EDA_FOODBACK)?.let { return it }
        sensorManager.getDefaultSensor(SENSOR_TYPE_EDA_GENERIC)?.let { return it }

        return sensorManager.getSensorList(Sensor.TYPE_ALL).firstOrNull { sensor ->
            val name = sensor.name.lowercase()
            name.contains("eda") ||
                name.contains("electrodermal") ||
                name.contains("skin conductance") ||
                name.contains("galvanic")
        }
    }
}
