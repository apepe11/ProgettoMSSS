package com.example.progetto.data.repositories

import com.example.progetto.utils.NetworkManager
import com.example.progetto.utils.SensorReading
import java.util.UUID

/**
 * Repository handling Sensor data uploads.
 */
class SensorRepository {
    private val networkManager = NetworkManager()

    /**
     * Sends Wearable data (HR, EDA) using the universal SensorReading class.
     */
    suspend fun sendWearableData(
        sessionId: UUID,
        sensorType: String,
        subject: String,
        rating: Int,
        sensorBuffer: List<SensorReading>
    ): Result<String> {
        return networkManager.sendWearableData(sessionId, sensorType, subject, rating, sensorBuffer)
    }

    /**
     * Sends EEG data. Note: We use SensorReading here because EegSample does not exist
     * in your SensorManager.kt definition.
     */
    suspend fun sendEegData(
        sessionId: UUID,
        subject: String,
        rating: Int,
        eegBuffer: List<SensorReading>
    ): Result<String> {
        return networkManager.sendEegData(sessionId, subject, rating, eegBuffer)
    }
}