package com.example.progetto.data

import android.os.Build

object BackendUrlProvider {
    private const val EMULATOR_BASE_URL = "http://10.0.2.2:5005/"
    private const val DEVICE_BASE_URL = "http://127.0.0.1:5005/"

    fun getBaseUrl(): String {
        val isEmulator = Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("Android SDK built for", ignoreCase = true) ||
            Build.MANUFACTURER.contains("Genymotion", ignoreCase = true) ||
            Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
            Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
            Build.PRODUCT.contains("sdk", ignoreCase = true)

        return if (isEmulator) EMULATOR_BASE_URL else DEVICE_BASE_URL
    }
}