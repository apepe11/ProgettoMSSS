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
import java.util.concurrent.TimeUnit

/**
 * NetworkManager handles sending sensor data to the backend API
 */
class NetworkManager(private val baseUrl: String = BackendUrlProvider.getBaseUrl().removeSuffix("/")) {

    private val client = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        private const val TAG = "NetworkManager"
    }

    suspend fun createTrainingSession(userId: UUID, songId: UUID): Result<UUID> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("user_id", userId.toString())
                put("song_id", songId.toString())
            }

            val request = Request.Builder()
                .url("$baseUrl/api/training/sessions")
                .post(json.toString().toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val sessionId = JSONObject(body).getString("session_id")
                Result.success(UUID.fromString(sessionId))
            } else {
                Log.e(TAG, "Create session failed: ${response.code} - $body")
                Result.failure(Exception("HTTP ${response.code}: $body"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating training session", e)
            Result.failure(e)
        }
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
            val grouped = mutableMapOf<Long, FloatArray>()
            for (reading in eegBuffer) {
                if (reading.type != "eeg") continue
                val frame = grouped.getOrPut(reading.timestamp) { FloatArray(6) }
                if (reading.channel in 0..5) {
                    frame[reading.channel] = reading.value
                }
            }

            val sortedFrames = grouped.toSortedMap()

            // Build JSON payload
            val json = JSONObject().apply {
                put("session_id", sessionId.toString())
                put("subject", subject)
                put("rating", rating)
                put("samples", JSONArray().apply {
                    var sampleIndex = 0
                    for ((_, values) in sortedFrames) {
                        val sample = JSONObject().apply {
                            put("sample", sampleIndex)
                            put("ch1", values.getOrNull(0) ?: 0f)
                            put("ch2", values.getOrNull(1) ?: 0f)
                            put("ch3", values.getOrNull(2) ?: 0f)
                            put("ch4", values.getOrNull(3) ?: 0f)
                            put("ch5", values.getOrNull(4) ?: 0f)
                            put("ch6", values.getOrNull(5) ?: 0f)
                        }
                        put(sample)
                        sampleIndex += 1
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
     * Send wearable sensor data (HR) to backend
     */
    suspend fun sendWearableData(
        sessionId: UUID,
        sensorType: String,  // "hr"
        subject: String,
        rating: Int,
        sensorBuffer: List<SensorReading>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resampled = resampleWearable(sensorBuffer, targetHz = 4.0)
            // Build samples array
            val samplesArray = JSONArray().apply {
                for (reading in resampled) {
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

    private fun resampleWearable(
        samples: List<SensorReading>,
        targetHz: Double
    ): List<SensorReading> {
        if (samples.isEmpty()) return samples
        if (samples.size == 1) return samples

        val sorted = samples.sortedBy { it.timestamp }
        val start = sorted.first().timestamp
        val end = sorted.last().timestamp
        if (end <= start) return samples

        val stepMs = (1000.0 / targetHz).toLong().coerceAtLeast(1L)
        val output = ArrayList<SensorReading>()

        var idx = 0
        var t = start
        while (t <= end) {
            while (idx < sorted.size - 2 && sorted[idx + 1].timestamp <= t) {
                idx += 1
            }

            val left = sorted[idx]
            val right = sorted[(idx + 1).coerceAtMost(sorted.size - 1)]
            val value = if (right.timestamp == left.timestamp) {
                left.value
            } else {
                val ratio = (t - left.timestamp).toDouble() / (right.timestamp - left.timestamp).toDouble()
                (left.value + (right.value - left.value) * ratio).toFloat()
            }

            output.add(
                SensorReading(
                    timestamp = t,
                    type = left.type,
                    value = value,
                    channel = left.channel
                )
            )
            t += stepMs
        }

        return output
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

    /**
     * Ask the backend to run the emotion prediction model for a completed session.
     */
    suspend fun predictEmotion(sessionId: UUID): Result<String> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("session_id", sessionId.toString())
            }

            val request = Request.Builder()
                .url("$baseUrl/api/model/predict")
                .post(json.toString().toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val responseJson = JSONObject(body)
                val prediction = responseJson.optJSONObject("prediction")
                val emotion = prediction?.optString("predicted_emotion")
                if (!emotion.isNullOrEmpty()) {
                    return@withContext Result.success(emotion)
                }
                return@withContext Result.failure(Exception("Invalid prediction response: $body"))
            }

            Log.e(TAG, "Predict emotion failed: ${'$'}{response.code} - $body")
            return@withContext Result.failure(Exception("HTTP ${'$'}{response.code}: $body"))
        } catch (e: Exception) {
            Log.e(TAG, "Error predicting emotion", e)
            Result.failure(e)
        }
    }

    /**
     * Trigger backend model training using the current collected dataset.
     */
    suspend fun trainEmotionModel(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/model/train")
                .post("{}".toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val responseJson = JSONObject(body)
                val metrics = responseJson.optJSONObject("metrics")
                return@withContext Result.success(metrics?.toString() ?: "Model trained")
            }
            Log.e(TAG, "Model training failed: ${'$'}{response.code} - $body")
            return@withContext Result.failure(Exception("HTTP ${'$'}{response.code}: $body"))
        } catch (e: Exception) {
            Log.e(TAG, "Error training model", e)
            Result.failure(e)
        }
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
