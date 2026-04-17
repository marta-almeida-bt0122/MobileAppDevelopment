package com.example.helloworld

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Activity3 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_3)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.thirdActivity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get coordinates from Intent
        val latitude = intent.getStringExtra("latitude") ?: "N/A"
        val longitude = intent.getStringExtra("longitude") ?: "N/A"
        val altitude = intent.getStringExtra("altitude") ?: "N/A"

        // Log coordinates
        android.util.Log.d("Activity3", "Latitude: $latitude, Longitude: $longitude, Altitude: $altitude")

        // Display coordinates
        val tvCoordinates: TextView = findViewById(R.id.tvCoordinates)
        tvCoordinates.text = "Latitude: $latitude\nLongitude: $longitude\nAltitude: $altitude"

        val btnBackToActivity2 = findViewById<Button>(R.id.btnGoToSecondActivity)
        btnBackToActivity2.setOnClickListener {
            val intent = Intent(this, Activity2::class.java)
            startActivity(intent)
        }
    }
}


