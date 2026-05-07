package com.example.progetto.utils

import android.util.Log
import com.example.progetto.data.BackendUrlProvider
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
class NetworkManager(private val baseUrl: String = BackendUrlProvider.getBaseUrl().removeSuffix("/")) {

    private val client = OkHttpClient()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        private const val TAG = "NetworkManager"
    }

    /**
     * Fetch Insights Data for the Donut Chart
     */
    suspend fun getInsights(userId: String): InsightsResponse? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/insights/$userId")
            .get()
            .build()

        try {
            // Execute the network call
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val jsonString = response.body?.string()
                if (jsonString != null) {
                    return@withContext parseInsightsJson(jsonString)
                }
            } else {
                Log.e(TAG, "Server error: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network request failed", e)
            Log.e("NetworkManager", "CRASH REASON: ${e.message}", e)
        }
        return@withContext null
    }

    /**
     * Helper function to parse the Insights JSON response
     */
    private fun parseInsightsJson(jsonString: String): InsightsResponse {
        val json = JSONObject(jsonString)
        val appDetected = json.getJSONObject("app_detected")
        val userExperienced = json.getJSONObject("user_experienced")

        return InsightsResponse(
            app_detected = EmotionStats(
                // We use optDouble to safely fallback to 0.0 if the database returns nothing
                happy = appDetected.optDouble("happy", 0.0).toFloat(),
                sad = appDetected.optDouble("sad", 0.0).toFloat(),
                calm = appDetected.optDouble("calm", 0.0).toFloat(),
                anxious = appDetected.optDouble("anxious", 0.0).toFloat(),
                energetic = appDetected.optDouble("energetic", 0.0).toFloat()
            ),
            user_experienced = EmotionStats(
                happy = userExperienced.optDouble("happy", 0.0).toFloat(),
                sad = userExperienced.optDouble("sad", 0.0).toFloat(),
                calm = userExperienced.optDouble("calm", 0.0).toFloat(),
                anxious = userExperienced.optDouble("anxious", 0.0).toFloat(),
                energetic = userExperienced.optDouble("energetic", 0.0).toFloat()
            )
        )
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
    /**
     * Toggle a song as a favorite (Like / Unlike)
     */
    suspend fun toggleFavorite(userId: String, songId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Build JSON payload matching the Flask backend
            val json = JSONObject().apply {
                put("user_id", userId)
                put("song_id", songId)
            }

            val request = Request.Builder()
                .url("$baseUrl/api/favorites/toggle")
                .post(json.toString().toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                // Parse the response to see if it is now favorited (true) or unfavorited (false)
                val responseJson = JSONObject(body)
                val isFavorite = responseJson.optBoolean("is_favorite", false)
                Log.d(TAG, "Toggle favorite successful: is_favorite=$isFavorite")
                Result.success(isFavorite)
            } else {
                Log.e(TAG, "Toggle favorite failed: ${response.code} - $body")
                Result.failure(Exception("HTTP ${response.code}: $body"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling favorite", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch the user's list of favorite songs
     */
    suspend fun getFavoriteSongs(userId: String): List<Song>? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/favorites/$userId")
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val jsonString = response.body?.string()
                if (jsonString != null) {
                    val jsonArray = JSONArray(jsonString)
                    val favoriteSongs = mutableListOf<Song>()

                    for (i in 0 until jsonArray.length()) {
                        val songJson = jsonArray.getJSONObject(i)
                        favoriteSongs.add(
                            Song(
                                songId = songJson.getString("songId"),
                                title = songJson.getString("title"),
                                artist = songJson.getString("artist"),
                                url = songJson.getString("url"),
                                likes = songJson.optInt("likes", 0)
                            )
                        )
                    }
                    return@withContext favoriteSongs
                }
            } else {
                Log.e(TAG, "Server error fetching favorites: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network request to fetch favorites failed", e)
        }
        return@withContext null
    }

    /**
     * Fetch songs from backend (for Emotion Analysis screen)
     */
    suspend fun getSongs(): List<SongData> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/songs")
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val jsonString = response.body?.string()
                if (jsonString != null) {
                    val json = JSONObject(jsonString)
                    val songsArray = json.getJSONArray("songs")
                    val songs = mutableListOf<SongData>()

                    for (i in 0 until songsArray.length()) {
                        val songObj = songsArray.getJSONObject(i)
                        songs.add(
                            SongData(
                                song_id = songObj.optString("song_id", ""),
                                title = songObj.optString("title", "Unknown"),
                                artist = songObj.optString("artist", "Unknown"),
                                album = songObj.optString("album", ""),
                                url = songObj.optString("url", "")
                            )
                        )
                    }
                    Log.d(TAG, "Fetched ${songs.size} songs from backend")
                    return@withContext songs
                }
            } else {
                Log.e(TAG, "Failed to fetch songs: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching songs", e)
        }
        return@withContext emptyList()
    }
}
data class SongData(
    val song_id: String,
    val title: String,
    val artist: String,
    val album: String,
    val url: String
)
data class InsightsResponse(
    val app_detected: EmotionStats,
    val user_experienced: EmotionStats
)
data class EmotionStats(
    val happy: Float = 0f,
    val sad: Float = 0f,
    val calm: Float = 0f,
    val anxious: Float = 0f,
    val energetic: Float = 0f
)
data class Song(
    val songId: String,
    val title: String,
    val artist: String,
    val url: String,
    val likes: Int = 0
)
