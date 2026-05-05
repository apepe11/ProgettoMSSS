package com.example.progetto.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject

object WearableSamplingSender {
    private const val TAG = "WearableSamplingSender"

    fun sendStartSampling(context: Context, isInference: Boolean = false) {
        val message = JSONObject()
            .put("start", true)
            .put("isInference", isInference)
            .toString()
            .toByteArray()

        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    Log.w(TAG, "No connected wearable nodes")
                    return@addOnSuccessListener
                }

                nodes.forEach { node ->
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, "/start_sampling", message)
                        .addOnSuccessListener {
                            Log.d(TAG, "Sent sampling request to ${node.displayName}")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed sending sampling request to ${node.displayName}", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed loading wearable nodes", e)
            }
    }
}
