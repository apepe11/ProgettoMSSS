package com.example.progetto.utils

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.util.Log

object SensorAvailability {
    private const val TAG = "SensorCheck"

    fun hasEmotionSensors(context: Context): Boolean {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return false

        val heartRateAvailable = hasSensor(sensorManager, Sensor.TYPE_HEART_RATE)
        val bluetoothConnected = hasConnectedBluetoothDevice(context)
        val eegAvailable = hasEegSignal()
        val available = heartRateAvailable || bluetoothConnected || eegAvailable

        Log.d(
            TAG,
            "heartRate=$heartRateAvailable bluetoothConnected=$bluetoothConnected eeg=$eegAvailable available=$available"
        )
        return available
    }

    fun hasEmotionSensorsStrict(context: Context): Boolean {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return false

        val heartRateAvailable = hasSensor(sensorManager, Sensor.TYPE_HEART_RATE)
        val bluetoothConnected = hasConnectedBluetoothDevice(context)
        val available = heartRateAvailable || bluetoothConnected

        Log.d(
            TAG,
            "[strict] heartRate=$heartRateAvailable bluetoothConnected=$bluetoothConnected available=$available"
        )
        return available
    }

    fun hasEegSignal(): Boolean {
        val available = EegSignalTracker.hasRecentSignal()
        Log.d(TAG, "[eeg] recentSignal=$available")
        return available
    }

    fun hasWatchSignal(context: Context): Boolean {
        val available = hasConnectedBluetoothDevice(context)
        Log.d(TAG, "[watch] bluetoothConnected=$available")
        return available
    }

    private fun hasSensor(sensorManager: SensorManager, sensorType: Int): Boolean {
        return sensorManager.getDefaultSensor(sensorType) != null ||
            sensorManager.getDynamicSensorList(sensorType).isNotEmpty()
    }

    private fun hasConnectedBluetoothDevice(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "BLUETOOTH_CONNECT permission not granted")
            return false
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            ?: return false

        if (bluetoothManager.adapter == null) return false

        // Check each profile safely, catching both SecurityException and IllegalArgumentException
        val profiles = listOf(BluetoothProfile.GATT, BluetoothProfile.HEADSET, BluetoothProfile.A2DP)
        for (profile in profiles) {
            try {
                if (bluetoothManager.getConnectedDevices(profile).isNotEmpty()) {
                    Log.d(TAG, "Found connected bluetooth device for profile: $profile")
                    return true
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "SecurityException querying profile $profile", e)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "IllegalArgumentException for profile $profile (not supported on this device)", e)
            }
        }

        // Fallback: some BLE wearables are bonded but not exposed via getConnectedDevices.
        // Consider paired (bonded) devices as possible sensors if their names suggest a wearable.
        try {
            val adapter = bluetoothManager.adapter
            val bonded = adapter?.bondedDevices.orEmpty()
            if (bonded.isNotEmpty()) {
                // Check for likely wearable keywords in device name
                val wearableKeywords = listOf("watch", "band", "fit", "mi", "garmin", "polar", "huawei", "amazfit", "oppo")
                for (d in bonded) {
                    val name = d.name?.lowercase() ?: ""
                    if (wearableKeywords.any { name.contains(it) }) {
                        Log.d(TAG, "Found bonded wearable device: ${d.address} (${d.name})")
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking bonded devices", e)
        }

        return false
    }
}