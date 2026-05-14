package com.example.progetto.wear

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requiredPermissions = buildList {
            add(Manifest.permission.BODY_SENSORS)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val missing = requiredPermissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), 10)
        } else {
            startSensorService()
        }

        setContentView(
            TextView(this).apply {
                text = "HeartMusic\nWear sensors ready"
                gravity = Gravity.CENTER
                textSize = 18f
            }
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startSensorService()
        }
    }

    private fun startSensorService() {
        if (!SensorService.isRunning) {
            val intent = Intent(this, SensorService::class.java)
            ContextCompat.startForegroundService(this, intent)
        }
    }
}
