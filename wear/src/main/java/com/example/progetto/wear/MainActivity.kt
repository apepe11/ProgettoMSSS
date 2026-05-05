package com.example.progetto.wear

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.BODY_SENSORS), 10)
        }

        setContentView(
            TextView(this).apply {
                text = "HeartMusic\nWear sensors ready"
                gravity = Gravity.CENTER
                textSize = 18f
            }
        )
    }
}
