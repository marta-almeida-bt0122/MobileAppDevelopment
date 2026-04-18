package com.example.helloworld

import android.app.Activity
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
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.Manifest
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
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

    private val TAG = "MyAppMainActivity"
    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 2
    private lateinit var locationSwitch: SwitchMaterial
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var weatherTextView: TextView
    private lateinit var weatherIcon: ImageView
    private var db: AppDatabase? = null
    private lateinit var auth: FirebaseAuth
    var latestLocation: Location? = null

    companion object {
        private const val RC_SIGN_IN = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        try {
            db = AppDatabase.getDatabase(this)
        } catch (e: Exception) {
            Log.e(TAG, "DB init error: ${e.message}")
        }

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
                R.id.nav_home -> { drawerLayout.closeDrawers(); true }
                R.id.nav_second_activity -> {
                    startActivity(Intent(this, Activity2::class.java))
                    drawerLayout.closeDrawers(); true
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
                    drawerLayout.closeDrawers(); true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    drawerLayout.closeDrawers(); true
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

        findViewById<Button>(R.id.mapButton).setOnClickListener {
            if (latestLocation != null) {
                val intent = Intent(this, OpenStreetMapsActivity::class.java)
                intent.putExtra("latitude", latestLocation!!.latitude)
                intent.putExtra("longitude", latestLocation!!.longitude)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Waiting for location...", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.mainButton).setOnClickListener {
            startActivity(Intent(this, Activity2::class.java))
        }

        findViewById<Button>(R.id.userIdentifierButton).setOnClickListener {
            showUserIdentifierDialog()
        }

        val userIdentifier = getUserIdentifier()
        if (userIdentifier != null) Log.d(TAG, "User ID: $userIdentifier")

        // Init Firebase Auth and launch sign-in
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            launchSignInFlow()
        } else {
            updateUIWithUsername()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
                Toast.makeText(this, R.string.signed_in, Toast.LENGTH_SHORT).show()
                Log.i(TAG, "onActivityResult: ${getString(R.string.signed_in)} — ${user?.email}")
                updateUIWithUsername()
            } else {
                Log.e(TAG, "Error starting auth session: ${response?.error?.errorCode}")
                Toast.makeText(this, R.string.signed_cancelled, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun launchSignInFlow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build()
        )
        @Suppress("DEPRECATION")
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN
        )
    }

    private fun logout() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                Toast.makeText(this, R.string.signed_out, Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
    }

    private fun updateUIWithUsername() {
        val user = FirebaseAuth.getInstance().currentUser
        val userNameTextView: TextView = findViewById(R.id.userNameTextView)
        user?.let {
            val name = it.displayName ?: "No Name"
            userNameTextView.text = "🤵 $name"
        }
    }

    override fun onResume() {
        super.onResume()
        updateUIWithUsername()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                locationPermissionCode)
        } else {
            try {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 5f, this)
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting location updates: ${e.message}")
            }
        }
    }

    private fun stopLocationUpdates() { locationManager.removeUpdates(this) }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
                    locationSwitch.isChecked = true
                }
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        latestLocation = location
        val textView: TextView = findViewById(R.id.textViewLocation)
        textView.text = getString(R.string.location_text, location.latitude, location.longitude)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                db?.coordinatesDao()?.insert(CoordinatesEntity(
                    timestamp = System.currentTimeMillis(),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude
                ))
            } catch (e: Exception) { Log.e(TAG, "DB insert error: ${e.message}") }
        }

        if (isNetworkAvailable()) getWeatherForecast(location.latitude, location.longitude)
    }

    private fun getWeatherForecast(lat: Double, lon: Double) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isEmpty()) { weatherTextView.text = "API Key not set"; return }
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory.create()).build()
            val call = retrofit.create(WeatherApiService::class.java).getWeatherForecast(lat, lon, "metric", apiKey)
            call.enqueue(object : retrofit2.Callback<com.example.helloworld.network.WeatherItem> {
                override fun onResponse(call: retrofit2.Call<com.example.helloworld.network.WeatherItem>,
                    response: retrofit2.Response<com.example.helloworld.network.WeatherItem>) {
                    if (response.isSuccessful) {
                        val weather = response.body() ?: return
                        weatherTextView.text = "${weather.name}\n${weather.main.temp.toInt()}°C\n${weather.weather[0].description}"
                        Glide.with(this@MainActivity)
                            .load("https://openweathermap.org/img/wn/${weather.weather[0].icon}@2x.png")
                            .override(64, 64).into(weatherIcon)
                    } else weatherTextView.text = "Weather unavailable: ${response.code()}"
                }
                override fun onFailure(call: retrofit2.Call<com.example.helloworld.network.WeatherItem>, t: Throwable) {
                    Log.e(TAG, "Failure: ${t.message}"); weatherTextView.text = "Network error"
                }
            })
        } catch (e: Exception) { Log.e(TAG, "Exception: ${e.message}"); weatherTextView.text = "Error" }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.getNetworkCapabilities(cm.activeNetwork ?: return false)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
    }

    private fun getApiKey(): String =
        getSharedPreferences("AppPreferences", Context.MODE_PRIVATE).getString("API_KEY", "")?.trim() ?: ""

    private fun showUserIdentifierDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter User Identifier")
        val input = EditText(this)
        getUserIdentifier()?.let { input.setText(it) }
        builder.setView(input)
        builder.setPositiveButton("OK") { _, _ ->
            val userInput = input.text.toString()
            if (userInput.isNotBlank()) {
                saveUserIdentifier(userInput)
                Toast.makeText(this, "User ID saved: $userInput", Toast.LENGTH_LONG).show()
            } else Toast.makeText(this, "User ID cannot be blank", Toast.LENGTH_LONG).show()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
            if (getUserIdentifier() == null) finish()
        }
        builder.show()
    }

    private fun saveUserIdentifier(userIdentifier: String) {
        getSharedPreferences("AppPreferences", Context.MODE_PRIVATE).edit()
            .putString("userIdentifier", userIdentifier).apply()
    }

    private fun getUserIdentifier(): String? =
        getSharedPreferences("AppPreferences", Context.MODE_PRIVATE).getString("userIdentifier", null)

    @Suppress("DEPRECATION")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
