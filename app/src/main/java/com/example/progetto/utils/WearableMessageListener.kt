package com.example.progetto.utils

import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

data class WearableData(
    val timestamp: Long,
    val value: Float
)

class WearableMessageListener : MessageClient.OnMessageReceivedListener {
    companion object {
        private const val TAG = "WearableMessageListener"
        private const val PATH_SAMPLING = "/sensor_series"

        val heartRateFlow = MutableSharedFlow<List<WearableData>>()
        val edaFlow = MutableSharedFlow<List<WearableData>>()
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "Message received on path=${messageEvent.path}, size=${messageEvent.data.size}")

        if (messageEvent.path != PATH_SAMPLING) {
            Log.w(TAG, "Ignoring unknown wearable path: ${messageEvent.path}")
            return
        }

        val message = String(messageEvent.data)
        Log.d(TAG, "Received sensor data: $message")

        try {
            val json = JSONObject(message)
            val heartRateJsonArray = json.getJSONArray("heart_rate")
            val edaJsonArray = json.getJSONArray("eda")

            val heartRateData = List(heartRateJsonArray.length()) { index ->
                val entry = heartRateJsonArray.getJSONObject(index)
                WearableData(
                    timestamp = entry.getLong("timestamp"),
                    value = entry.getDouble("value").toFloat()
                )
            }

            val edaData = List(edaJsonArray.length()) { index ->
                val entry = edaJsonArray.getJSONObject(index)
                WearableData(
                    timestamp = entry.getLong("timestamp"),
                    value = entry.getDouble("value").toFloat()
                )
            }

            CoroutineScope(Dispatchers.Main).launch {
                Log.d(TAG, "Parsed wearable samples: hr=${heartRateData.size}, eda=${edaData.size}")
                heartRateFlow.emit(heartRateData)
                edaFlow.emit(edaData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing wearable JSON", e)
        }
    }
}
