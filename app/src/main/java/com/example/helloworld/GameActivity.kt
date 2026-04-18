package com.example.helloworld

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.helloworld.game.GameEngine
import com.example.helloworld.game.GameRepository
import com.example.helloworld.room.CharacterEntity
import kotlinx.coroutines.launch

/**
 * The main game screen. While in foreground, ticks every 60s, fetches
 * environment (throttled to every 15min) and updates character HP/score.
 */
class GameActivity : AppCompatActivity() {

    private val TAG = "GameActivity"
    private lateinit var repo: GameRepository
    private var character: CharacterEntity? = null
    private var lastEnv: GameEngine.EnvReading? = null
    private var lastEnvFetchMs: Long = 0L
    private val envFetchIntervalMs = 15 * 60 * 1000L

    private var recommendations: List<GameEngine.Recommendation> = emptyList()
    private var recIndex = 0

    private val handler = Handler(Looper.getMainLooper())
    private val tickInterval = 60_000L  // 60 seconds

    private val tickRunnable = object : Runnable {
        override fun run() {
            tickOnce()
            handler.postDelayed(this, tickInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.gameRoot)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        repo = GameRepository(this)

        findViewById<Button>(R.id.btnBackToMenu).setOnClickListener { finish() }

        findViewById<TextView>(R.id.tvRecommendation).setOnClickListener {
            cycleRecommendation()
        }

        loadCharacter()
    }

    override fun onResume() {
        super.onResume()
        handler.post(tickRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tickRunnable)
    }

    private fun loadCharacter() {
        val characterId = intent.getStringExtra("characterId")
        lifecycleScope.launch {
            character = if (characterId != null) {
                val repo = GameRepository(this@GameActivity)
                // Simpler: fetch all and find the one (small list)
                repo.getRanking().firstOrNull { it.id == characterId }
                    ?: repo.getActiveCharacter()
            } else {
                repo.getActiveCharacter()
            }
            renderCharacter()
            tickOnce()
        }
    }

    private fun tickOnce() {
        val char = character ?: return
        if (!char.alive) {
            renderCharacter()
            return
        }

        val (lat, lon) = currentLocation(char)

        lifecycleScope.launch {
            // Fetch environment only every 15 minutes
            val now = System.currentTimeMillis()
            val env = if (lastEnv == null || now - lastEnvFetchMs > envFetchIntervalMs) {
                val fetched = repo.fetchEnvironment(lat, lon)
                if (fetched != null) {
                    lastEnv = fetched
                    lastEnvFetchMs = now
                    renderTelemetry(fetched)
                }
                fetched ?: lastEnv
            } else {
                lastEnv
            } ?: return@launch

            val minutesElapsed = ((now - char.lastUpdate) / 60000.0).coerceIn(1.0, 30.0)
            val tick = GameEngine.computeTick(char, env, minutesElapsed)
            val updated = GameEngine.applyTick(char, tick, now, lat, lon)

            repo.updateCharacter(updated)
            character = updated
            recommendations = tick.recommendations
            recIndex = 0

            renderCharacter()
            renderRecommendation()
        }
    }

    private fun renderCharacter() {
        val char = character ?: return
        val skin = GameEngine.skinTypeById(char.skinType)
        val stage = GameEngine.lifeStage(char)
        val ageDays = GameEngine.ageInDays(char)

        findViewById<TextView>(R.id.tvName).text = char.name
        findViewById<TextView>(R.id.tvAge).text = "$ageDays days"
        findViewById<TextView>(R.id.tvHp).text = "${char.hp.toInt()} HP"
        findViewById<TextView>(R.id.tvScore).text = "${char.score.toInt()} pts"
        findViewById<TextView>(R.id.tvStage).text = "${stage.emoji} ${stage.label}"
        findViewById<TextView>(R.id.tvMascot).text = skin.emoji
        findViewById<TextView>(R.id.tvFace).text =
            GameEngine.faceEmoji(char.hp, char.alive)
    }

    private fun renderTelemetry(env: GameEngine.EnvReading) {
        findViewById<TextView>(R.id.tvTemp).text = "🌡 ${env.temperatureC.toInt()}°C"
        findViewById<TextView>(R.id.tvHumidity).text = "💧 ${env.humidity}%"
        findViewById<TextView>(R.id.tvUv).text = "☀️ UV %.1f".format(env.uvIndex)
        findViewById<TextView>(R.id.tvAir).text = "💨 PM2.5 ${env.pm25.toInt()}"
    }

    private fun renderRecommendation() {
        val tvRec = findViewById<TextView>(R.id.tvRecommendation)
        val tvIdx = findViewById<TextView>(R.id.tvRecIndex)
        if (recommendations.isEmpty()) {
            tvRec.text = "Your character is in perfect condition!"
            tvIdx.text = ""
            return
        }
        val rec = recommendations[recIndex]
        tvRec.text = rec.text
        tvIdx.text = "${recIndex + 1} of ${recommendations.size}"
    }

    private fun cycleRecommendation() {
        if (recommendations.isEmpty()) return
        recIndex = (recIndex + 1) % recommendations.size
        renderRecommendation()
    }

    private fun currentLocation(char: CharacterEntity): Pair<Double, Double> {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            try {
                val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val loc: Location? =
                    lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) {
                    getSharedPreferences("AppPreferences", Context.MODE_PRIVATE).edit()
                        .putFloat("last_lat", loc.latitude.toFloat())
                        .putFloat("last_lon", loc.longitude.toFloat())
                        .apply()
                    return loc.latitude to loc.longitude
                }
            } catch (e: Exception) {
                Log.e(TAG, "Location error: ${e.message}")
            }
        }
        return char.lastLat to char.lastLon
    }
}
