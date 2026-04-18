package com.example.helloworld

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.helloworld.game.GameEngine
import com.example.helloworld.game.GameRepository
import com.example.helloworld.worker.GameTickWorker
import kotlinx.coroutines.launch

class CreateCharacterActivity : AppCompatActivity() {

    private var currentIndex = 0
    private val types = GameEngine.SKIN_TYPES

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_character)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.createRoot)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        render()

        findViewById<Button>(R.id.btnPrev).setOnClickListener {
            currentIndex = (currentIndex - 1 + types.size) % types.size
            render()
        }
        findViewById<Button>(R.id.btnNext).setOnClickListener {
            currentIndex = (currentIndex + 1) % types.size
            render()
        }
        findViewById<Button>(R.id.btnCreate).setOnClickListener { create() }
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun render() {
        val skin = types[currentIndex]
        findViewById<TextView>(R.id.tvEmoji).text = skin.emoji
        findViewById<TextView>(R.id.tvSkinLabel).text = skin.label
        findViewById<TextView>(R.id.tvSkinDesc).text = skin.description
    }

    private fun create() {
        val name = findViewById<EditText>(R.id.etName).text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Enter a name", Toast.LENGTH_SHORT).show()
            return
        }
        val skin = types[currentIndex]
        val (lat, lon) = lastKnownOrDefault()

        lifecycleScope.launch {
            val character = GameRepository(this@CreateCharacterActivity)
                .createCharacter(name, skin.id, lat, lon)
            if (character != null) {
                Toast.makeText(this@CreateCharacterActivity,
                    "${character.name} was born!", Toast.LENGTH_SHORT).show()
                GameTickWorker.schedule(applicationContext)
                val intent = Intent(this@CreateCharacterActivity, GameActivity::class.java)
                intent.putExtra("characterId", character.id)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this@CreateCharacterActivity,
                    "Could not create character", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun lastKnownOrDefault(): Pair<Double, Double> {
        // Try GPS
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            try {
                val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val loc: Location? =
                    lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) return loc.latitude to loc.longitude
            } catch (_: Exception) {}
        }
        // Try cached prefs
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val cachedLat = prefs.getFloat("last_lat", 0f)
        val cachedLon = prefs.getFloat("last_lon", 0f)
        if (cachedLat != 0f || cachedLon != 0f) return cachedLat.toDouble() to cachedLon.toDouble()
        // Default: Madrid
        return 40.4168 to -3.7038
    }
}
