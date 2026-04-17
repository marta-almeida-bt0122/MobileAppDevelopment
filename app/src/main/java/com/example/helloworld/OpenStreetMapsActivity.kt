package com.example.helloworld

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class OpenStreetMapsActivity : AppCompatActivity() {

    private val TAG = "OpenStreetMapsActivity"
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_street_maps)

        // Configure the user agent for osmdroid
        val userAgent = this.packageName
        Configuration.getInstance().userAgentValue = userAgent

        // Get references
        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        // Get location from Intent
        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)

        Log.d(TAG, "onCreate: Latitude=$latitude, Longitude=$longitude")

        if (latitude != 0.0 && longitude != 0.0) {
            // Create a GEO point with the received location
            val geoPoint = GeoPoint(latitude, longitude)

            // Set map zoom and position
            val controller = mapView.controller
            controller.setZoom(18.0)
            controller.setCenter(geoPoint)

            // Add a marker at the current position
            val marker = Marker(mapView)
            marker.position = geoPoint
            marker.title = "My Position"
            marker.snippet = "Lat: $latitude, Lon: $longitude"
            mapView.overlays.add(marker)

            Log.d(TAG, "onCreate: Marker added at position: $latitude, $longitude")
        } else {
            Log.w(TAG, "onCreate: Valid coordinates were not received")
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }
}

