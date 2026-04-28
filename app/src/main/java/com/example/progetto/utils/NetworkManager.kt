package com.example.progetto.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * NetworkManager handles sending sensor data to the backend API
 */
class NetworkManager(private val baseUrl: String = "http://10.0.2.2:5000") {
    
    private val client = OkHttpClient()
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    
    companion object {
        private const val TAG = "NetworkManager"
    }
    
    /**
     * Send EEG training data to backend
     */
    suspend fun sendEegData(
        sessionId: UUID,
        subject: String,
        rating: Int,
        eegBuffer: List<SensorReading>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Group EEG data by channel
            val channelData = mutableMapOf<Int, MutableList<Float>>()
            var sampleCount = 0
            
            for (reading in eegBuffer) {
                if (reading.type != "eeg") continue
                val channel = reading.channel
                channelData.getOrPut(channel) { mutableListOf() }.add(reading.value)
                sampleCount++
            }
            
            // Build JSON payload
            val json = JSONObject().apply {
                put("session_id", sessionId.toString())
                put("subject", subject)
                put("rating", rating)
                put("samples", JSONArray().apply {
                    for (i in 0 until sampleCount) {
                        val sample = JSONObject().apply {
                            put("sample", i)
                            for ((channel, values) in channelData) {
                                if (i < values.size) {
                                    put("ch${channel + 1}", values[i])
                                }
                            }
                        }
                        put(sample)
                    }
                })
            }
            
            val request = Request.Builder()
                .url("$baseUrl/api/training/eeg")
                .post(json.toString().toRequestBody(mediaType))
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                Log.d(TAG, "EEG data sent successfully")
                Result.success(body)
            } else {
                Log.e(TAG, "EEG send failed: ${response.code} - $body")
                Result.failure(Exception("HTTP ${response.code}: $body"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending EEG data", e)
            Result.failure(e)
        }
    }
    
    /**
     * Send wearable sensor data (HR or EDA) to backend
     */
    suspend fun sendWearableData(
        sessionId: UUID,
        sensorType: String,  // "hr" or "eda"
        subject: String,
        rating: Int,
        sensorBuffer: List<SensorReading>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Build samples array
            val samplesArray = JSONArray().apply {
                for (reading in sensorBuffer) {
                    val sample = JSONObject().apply {
                        put("timestamp", reading.timestamp)
                        put("value", reading.value.toDouble())
                    }
                    put(sample)
                }
            }
            
            // Build JSON payload
            val json = JSONObject().apply {
                put("session_id", sessionId.toString())
                put("sensor_type", sensorType)
                put("subject", subject)
                put("rating", rating)
                put("samples", samplesArray)
            }
            
            val request = Request.Builder()
                .url("$baseUrl/api/training/wearable")
                .post(json.toString().toRequestBody(mediaType))
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                Log.d(TAG, "$sensorType data sent successfully")
                Result.success(body)
            } else {
                Log.e(TAG, "$sensorType send failed: ${response.code} - $body")
                Result.failure(Exception("HTTP ${response.code}: $body"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending $sensorType data", e)
            Result.failure(e)
        }
    }
}
