package com.example.helloworld

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.Manifest
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.navigation.NavigationView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.helloworld.network.WeatherApiService
import com.example.helloworld.room.AppDatabase
import com.example.helloworld.room.CoordinatesEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 2
    private lateinit var locationSwitch: SwitchMaterial
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var weatherTextView: TextView
    private lateinit var weatherIcon: ImageView
    private lateinit var db: AppDatabase
    var latestLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize UI first
        try {
            db = AppDatabase.getDatabase(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "DB init error: ${e.message}")
        }

        // ... existing code...
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_second_activity -> {
                    val intent = Intent(this, Activity2::class.java)
                    startActivity(intent)
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_open_street_map -> {
                    if (latestLocation != null) {
                        val intent = Intent(this, OpenStreetMapsActivity::class.java)
                        intent.putExtra("latitude", latestLocation!!.latitude)
                        intent.putExtra("longitude", latestLocation!!.longitude)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Waiting for location...", Toast.LENGTH_SHORT).show()
                    }
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    drawerLayout.closeDrawers()
                    true
                }
                else -> false
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        weatherTextView = findViewById(R.id.weatherTextView)
        weatherIcon = findViewById(R.id.weatherIcon)
        weatherTextView.text = "Weather: Ready"

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

        // Check if user identifier is set, but don't force dialog on startup
        val userIdentifier = getUserIdentifier()
        if (userIdentifier != null) {
            android.util.Log.d("MainActivity", "User ID: $userIdentifier")
        }
    }

    override fun onResume() {
        super.onResume()
        // Only fetch weather if we have internet and a location
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
            try {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
                }
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 5f, this)
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error requesting location updates: ${e.message}")
            }
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

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val coordinate = CoordinatesEntity(
                    timestamp = System.currentTimeMillis(),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude
                )
                db.coordinatesDao().insert(coordinate)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "DB insert error: ${e.message}")
            }
        }

        // Only fetch weather if we have a network connection
        if (isNetworkAvailable()) {
            getWeatherForecast(location.latitude, location.longitude)
        }
    }

    private fun getWeatherForecast(lat: Double, lon: Double) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isEmpty()) {
                weatherTextView.text = "API Key not set"
                return
            }

            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(WeatherApiService::class.java)
            val call = service.getWeatherForecast(lat, lon, "metric", apiKey)

            call.enqueue(object : retrofit2.Callback<com.example.helloworld.network.WeatherItem> {
                override fun onResponse(
                    call: retrofit2.Call<com.example.helloworld.network.WeatherItem>,
                    response: retrofit2.Response<com.example.helloworld.network.WeatherItem>
                ) {
                    try {
                        if (response.isSuccessful) {
                            val weather = response.body()
                            if (weather != null) {
                                val main = weather.main
                                val weatherCondition = weather.weather[0]
                                val weatherInfo = "${weather.name}\n${main.temp.toInt()}°C\n${weatherCondition.description}"
                                weatherTextView.text = weatherInfo

                                val iconUrl = "https://openweathermap.org/img/wn/${weatherCondition.icon}@2x.png"
                                Glide.with(this@MainActivity)
                                    .load(iconUrl)
                                    .override(64, 64)
                                    .into(weatherIcon)
                            }
                        } else {
                            weatherTextView.text = "Weather unavailable: ${response.code()}"
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("Weather", "Response error: ${e.message}")
                        weatherTextView.text = "Weather error"
                    }
                }

                override fun onFailure(
                    call: retrofit2.Call<com.example.helloworld.network.WeatherItem>,
                    t: Throwable
                ) {
                    android.util.Log.e("Weather", "Failure: ${t.message}")
                    weatherTextView.text = "Network error"
                }
            })
        } catch (e: Exception) {
            android.util.Log.e("Weather", "Exception: ${e.message}")
            weatherTextView.text = "Error"
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun getApiKey(): String {
        val sharedPreferences = this.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("API_KEY", "") ?: ""
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

