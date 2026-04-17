package com.example.helloworld

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.Manifest
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 2
    private lateinit var locationSwitch: SwitchMaterial
    var latestLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        locationSwitch = findViewById(R.id.locationSwitch)
        locationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                locationSwitch.text = "Disable location"
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates()
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        locationPermissionCode
                    )
                }
            } else {
                locationSwitch.text = "Enable location"
                stopLocationUpdates()
            }
        }

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

        val mainButton = findViewById<Button>(R.id.mainButton)
        mainButton.setOnClickListener {
            val intent = Intent(this, Activity2::class.java)
            startActivity(intent)
        }

        val userIdentifierButton = findViewById<Button>(R.id.userIdentifierButton)
        userIdentifierButton.setOnClickListener {
            showUserIdentifierDialog()
        }

        val userIdentifier = getUserIdentifier()
        if (userIdentifier == null) {
            showUserIdentifierDialog()
        } else {
            Toast.makeText(this, "User ID: $userIdentifier", Toast.LENGTH_LONG).show()
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                locationPermissionCode
            )
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
        }
    }

    private fun stopLocationUpdates() {
        locationManager.removeUpdates(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
                    locationSwitch.isChecked = true
                }
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        latestLocation = location
        val textView: TextView = findViewById(R.id.textViewLocation)
        val locationText = getString(R.string.location_text, location.latitude, location.longitude)
        textView.text = locationText
        saveCoordinatesToFile(location.latitude, location.longitude, location.altitude, System.currentTimeMillis())
        val toastText = "New location: ${location.latitude}, ${location.longitude}, ${location.altitude}"
        Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show()
    }

    private fun saveCoordinatesToFile(latitude: Double, longitude: Double, altitude: Double, timestamp: Long) {
        val fileName = "gps_coordinates.csv"
        val file = File(filesDir, fileName)
        val formattedLatitude = String.format("%.4f", latitude)
        val formattedLongitude = String.format("%.4f", longitude)
        val formattedAltitude = String.format("%.2f", altitude)
        file.appendText("$timestamp;$formattedLatitude;$formattedLongitude;$formattedAltitude\n")
    }

    private fun showUserIdentifierDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter User Identifier")
        val input = EditText(this)
        val userIdentifier = getUserIdentifier()
        if (userIdentifier != null) {
            input.setText(userIdentifier)
        }
        builder.setView(input)
        builder.setPositiveButton("OK") { _, _ ->
            val userInput = input.text.toString()
            if (userInput.isNotBlank()) {
                saveUserIdentifier(userInput)
                Toast.makeText(this, "User ID saved: $userInput", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "User ID cannot be blank", Toast.LENGTH_LONG).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
            if (getUserIdentifier() == null) {
                // No ID set and user cancelled — close the app
                finish()
            }
        }
        builder.show()
    }

    private fun saveUserIdentifier(userIdentifier: String) {
        val sharedPreferences = this.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putString("userIdentifier", userIdentifier)
            apply()
        }
    }

    private fun getUserIdentifier(): String? {
        val sharedPreferences = this.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("userIdentifier", null)
    }

    @Suppress("DEPRECATION")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}