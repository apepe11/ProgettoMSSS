package com.example.progetto.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import org.json.JSONArray
import org.json.JSONObject

object DataSender {
    private const val TAG = "WearDataSender"

    fun sendHealthStatus(context: Context) {
        sendMessage(context, "/check_health", JSONObject().put("health_status", true))
    }

    fun sendSensorData(
        context: Context,
        heartRates: List<WearableSample>
    ) {
        val payload = JSONObject()
            .put("heart_rate", heartRates.toJsonArray())

        sendMessage(context, "/sensor_series", payload)
    }

    private fun sendMessage(context: Context, path: String, payload: JSONObject) {
        val bytes = payload.toString().toByteArray()

        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    Log.w(TAG, "No connected phone nodes")
                    return@addOnSuccessListener
                }

                nodes.forEach { node ->
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, path, bytes)
                        .addOnSuccessListener {
                            Log.d(TAG, "Sent $path to ${node.displayName}: $payload")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed sending $path to ${node.displayName}", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed loading connected phone nodes", e)
            }
    }

    private fun List<WearableSample>.toJsonArray(): JSONArray {
        val array = JSONArray()
        forEach { sample ->
            array.put(
                JSONObject()
                    .put("timestamp", sample.timestamp)
                    .put("value", sample.value)
            )
        }
        return array
    }
}
