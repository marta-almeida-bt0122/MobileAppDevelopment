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

        private val TAG = "MiAppMainActivity"
        private lateinit var locationManager: LocationManager
        private lateinit var textViewLocation: TextView
        private var latestLocation: Location? = null

        // Creamos el "lanzador" que mostrará la ventanita preguntando por el permiso
        private val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                iniciarActualizacionesDeUbicacion()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate: La actividad principal se esta creando.")
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
            // Vincular el TextView (asegúrate de usar el ID que le pusiste en el XML)
            textViewLocation = findViewById(R.id.textViewLocation)

            // Inicializar el gestor de ubicación
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // Comprobar permisos
            checkLocationPermission()
    }
    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Si no tenemos permiso, lanzamos la ventanita para pedirlo
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            // Si ya lo tenemos, empezamos a leer el GPS
            iniciarActualizacionesDeUbicacion()
        }
    }

    private fun iniciarActualizacionesDeUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Pedimos actualizaciones: Proveedor GPS, Tiempo (5000 ms = 5s), Distancia (5f = 5 metros)
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
            latestLocation = location // Guardamos la última ubicación

            // Escribimos las coordenadas en el TextView de la pantalla
            textViewLocation.text = "Latitude: ${location.latitude}, Longitude: ${location.longitude}"
            Log.i(TAG, "onLocationChanged: New Location [${location.latitude}] [${location.longitude}]")
        }
    }
}