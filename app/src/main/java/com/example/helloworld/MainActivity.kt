package com.example.helloworld

import android.util.Log
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

        private val TAG = "MyAppMainActivity"
        private lateinit var locationManager: LocationManager
        private lateinit var textViewLocation: TextView
        private var latestLocation: Location? = null

        // Create the "launcher" that will show the window asking for permission
        private val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate: The main activity is being created.")
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.secondActivity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val button = findViewById<Button>(R.id.mainButton)
        button.setOnClickListener {
            val intent = Intent(this, Activity2::class.java)
            startActivity(intent)
        }

        // Button to navigate to the map
        val mapButton = findViewById<Button>(R.id.mapButton)
        mapButton.setOnClickListener {
            if (latestLocation != null) {
                val intent = Intent(this, OpenStreetMapsActivity::class.java)
                intent.putExtra("latitude", latestLocation!!.latitude)
                intent.putExtra("longitude", latestLocation!!.longitude)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Waiting for location...", Toast.LENGTH_SHORT).show()
            }
        }
            // Bind the TextView (make sure to use the ID you set in the XML)
            textViewLocation = findViewById(R.id.textViewLocation)

            // Initialize the location manager
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // Check permissions
            checkLocationPermission()
    }
    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // If we don't have permission, we launch the window to request it
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            // If we already have it, we start reading the GPS
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Request updates: GPS Provider, Time (5000 ms = 5s), Distance (5f = 5 meters)
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000L,
                5f,
                locationListener
            )
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            latestLocation = location // We save the latest location

            // Write the coordinates in the screen TextView
            textViewLocation.text = "Latitude: ${location.latitude}, Longitude: ${location.longitude}"
            Log.i(TAG, "onLocationChanged: New Location [${location.latitude}] [${location.longitude}]")
        }
    }
}