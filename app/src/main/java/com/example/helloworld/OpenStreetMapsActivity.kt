package com.example.helloworld

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.helloworld.room.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class OpenStreetMapsActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_street_maps)

        try {
            db = AppDatabase.getDatabase(this)
        } catch (e: Exception) {
            android.util.Log.e("OpenStreetMapsActivity", "DB init error: ${e.message}")
            finish()
            return
        }

        val userAgent = this.packageName
        Configuration.getInstance().userAgentValue = userAgent

        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)

        if (latitude != 0.0 && longitude != 0.0) {
            val geoPoint = GeoPoint(latitude, longitude)
            val controller = mapView.controller
            controller.setZoom(18.0)
            controller.setCenter(geoPoint)

            val marker = Marker(mapView)
            marker.position = geoPoint
            marker.title = "My Position"
            mapView.overlays.add(marker)
        }

        // Load markers from database
        loadDatabaseMarkers()
    }

    private fun loadDatabaseMarkers() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val coordinates = db.coordinatesDao().getAll()

                withContext(Dispatchers.Main) {
                    for (coord in coordinates) {
                        try {
                            val geoPoint = GeoPoint(coord.latitude, coord.longitude)
                            val marker = Marker(mapView)
                            marker.position = geoPoint
                            marker.title = "Saved"
                            mapView.overlays.add(marker)
                        } catch (e: Exception) {
                            android.util.Log.e("Maps", "Marker error: ${e.message}")
                        }
                    }
                    mapView.invalidate()
                }
            } catch (e: Exception) {
                android.util.Log.e("OpenStreetMapsActivity", "Load markers error: ${e.message}")
            }
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
