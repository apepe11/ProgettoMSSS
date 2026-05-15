package com.example.progetto.data.repositories

import com.example.progetto.utils.*
import java.util.UUID

/**
 * Repository handling Sensor data uploads.
 */
class SensorRepository {
    private val networkManager = NetworkManager()

    suspend fun sendWearableData(
        sessionId: UUID,
        sensorType: String,
        subject: String,
        rating: Int,
        sensorBuffer: List<SensorReading>
    ): Result<String> {
        return networkManager.sendWearableData(sessionId, sensorType, subject, rating, sensorBuffer)
    }

    suspend fun sendEegData(
        sessionId: UUID,
        subject: String,
        rating: Int,
        eegBuffer: List<SensorReading>
    ): Result<String> {
        return networkManager.sendEegData(sessionId, subject, rating, eegBuffer)
    }
}