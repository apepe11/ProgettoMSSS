package com.example.progetto.wear

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SamplingService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var samplingJob: Job? = null
    private lateinit var sensorRepository: SensorRepository

    override fun onCreate() {
        super.onCreate()
        sensorRepository = SensorRepository.getInstance(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isInference = intent?.getBooleanExtra(EXTRA_IS_INFERENCE, false) ?: false
        val durationMs = if (isInference) 2_000L else 35_000L

        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("HeartMusic")
                .setContentText("Collecting HR and EDA")
                .setSmallIcon(R.drawable.notify_icon)
                .setOngoing(true)
                .build()
        )

        samplingJob?.cancel()
        sensorRepository.startSensors()
        sensorRepository.startCollecting()

        samplingJob = serviceScope.launch {
            val startedAt = System.currentTimeMillis()

            while (isActive && System.currentTimeMillis() - startedAt < durationMs) {
                delay(1_000L)
                sendCurrentSnapshot()
            }

            sensorRepository.stopCollecting()
            sendCurrentSnapshot()
            sensorRepository.stopSensors()
            stopSelf()
        }

        Log.d(TAG, "Sampling started: isInference=$isInference durationMs=$durationMs")
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        samplingJob?.cancel()
        sensorRepository.stopCollecting()
        sensorRepository.stopSensors()
        Log.d(TAG, "Sampling service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun sendCurrentSnapshot() {
        val (heartRates, edaValues) = sensorRepository.snapshot()
        Log.d(TAG, "Sending snapshot: hr=${heartRates.size} eda=${edaValues.size}")
        DataSender.sendSensorData(this, heartRates, edaValues)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "HeartMusic sampling",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_IS_INFERENCE = "isInference"
        private const val TAG = "SamplingService"
        private const val CHANNEL_ID = "heartmusic_sampling"
        private const val NOTIFICATION_ID = 20
    }
}
