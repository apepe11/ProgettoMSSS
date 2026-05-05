package com.example.progetto.utils

object EegSignalTracker {
    private const val TTL_MS: Long = 5000
    @Volatile private var lastSampleTs: Long = 0

    fun markSample(timestamp: Long = System.currentTimeMillis()) {
        lastSampleTs = timestamp
    }

    fun hasRecentSignal(now: Long = System.currentTimeMillis()): Boolean {
        return lastSampleTs > 0 && (now - lastSampleTs) <= TTL_MS
    }

    fun clear() {
        lastSampleTs = 0
    }
}
