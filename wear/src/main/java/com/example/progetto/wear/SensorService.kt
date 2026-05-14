package com.example.progetto.wear

import android.app.Service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat

class SensorService : Service() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val repo = SensorRepository.getInstance(applicationContext)
        isRunning = true
        repo.startSensors()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Starting measurements")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "Service started")
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        val repo = SensorRepository.getInstance(applicationContext)
        repo.stopSensors()
        isRunning = false
        Log.d(TAG, "Service terminated")
        super.onDestroy()
    }

    companion object {
        const val TAG = "WearSensorService"
        private const val CHANNEL_ID = "heartmusic_sensors"
        private const val NOTIFICATION_ID = 21
        @Volatile var isRunning: Boolean = false
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "HeartMusic sensors",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
