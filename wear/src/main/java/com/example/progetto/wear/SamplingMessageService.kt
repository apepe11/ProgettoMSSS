package com.example.progetto.wear

import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONObject

class SamplingMessageService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            PATH_START -> {
                val message = String(messageEvent.data)
                Log.d(TAG, "Start sampling received: $message")

                val json = runCatching { JSONObject(message) }.getOrNull()
                val isInference = json?.optBoolean("isInference", false) ?: false

                val intent = Intent(this, SamplingService::class.java)
                    .putExtra(SamplingService.EXTRA_IS_INFERENCE, isInference)
                ContextCompat.startForegroundService(this, intent)
            }

            PATH_HEALTH -> {
                Log.d(TAG, "Health check received")
                DataSender.sendHealthStatus(this)
            }

            else -> Log.w(TAG, "Unknown path: ${messageEvent.path}")
        }
    }

    companion object {
        private const val TAG = "SamplingMessageService"
        private const val PATH_START = "/start_sampling"
        private const val PATH_HEALTH = "/check_health"
    }
}
