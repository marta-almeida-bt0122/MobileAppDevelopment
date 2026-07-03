package com.example.helloworld

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.helloworld.game.GameRepository
import com.example.helloworld.room.SunscreenLogEntry
import com.example.helloworld.worker.GameTickWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * "Apply sunscreen now" screen: grabs GPS + live environment, scores the
 * habit via PointsEngine and stores/syncs the result. Independent of the
 * character/HP layer so it works whether or not the user has a character.
 */
class SunscreenActivity : AppCompatActivity() {

    private val spfOptions = listOf(15, 30, 50, 70)
    private var spfIndex = 1 // default SPF 30

    private lateinit var repo: GameRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sunscreen)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sunscreenRoot)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        repo = GameRepository(this)
        GameTickWorker.schedule(applicationContext)

        renderSpf()
        findViewById<Button>(R.id.btnSpfPrev).setOnClickListener {
            spfIndex = (spfIndex - 1 + spfOptions.size) % spfOptions.size
            renderSpf()
        }
        findViewById<Button>(R.id.btnSpfNext).setOnClickListener {
            spfIndex = (spfIndex + 1) % spfOptions.size
            renderSpf()
        }
        findViewById<Button>(R.id.btnApply).setOnClickListener { applySunscreen() }
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        loadSummary()
    }

    private fun renderSpf() {
        findViewById<TextView>(R.id.tvSpf).text = "SPF ${spfOptions[spfIndex]}"
    }

    private fun loadSummary() {
        lifecycleScope.launch {
            renderTotals(repo.getMyTotalPoints(), repo.getMyStreak())
            renderLastLog(repo.getLastSunscreenLog())
        }
    }

    private fun applySunscreen() {
        val btnApply = findViewById<Button>(R.id.btnApply)
        btnApply.isEnabled = false

        lifecycleScope.launch {
            val location = currentLocation()
            if (location == null) {
                Toast.makeText(this@SunscreenActivity, "Location unavailable. Enable GPS and try again.", Toast.LENGTH_SHORT).show()
                btnApply.isEnabled = true
                return@launch
            }

            val entry = repo.logSunscreen(location.first, location.second, spfOptions[spfIndex])
            btnApply.isEnabled = true
            if (entry != null) {
                findViewById<TextView>(R.id.tvUv).text = "☀️ UV %.1f".format(entry.uvIndex)
                findViewById<TextView>(R.id.tvTemp).text = "🌡 ${entry.temperatureC.toInt()}°C"
                Toast.makeText(this@SunscreenActivity, "+${entry.pointsEarned.toInt()} points!", Toast.LENGTH_SHORT).show()
                renderTotals(repo.getMyTotalPoints(), repo.getMyStreak())
                renderLastLog(entry)
            } else {
                Toast.makeText(this@SunscreenActivity, "Could not fetch environment. Try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderTotals(totalPoints: Double, streak: Int) {
        findViewById<TextView>(R.id.tvTotalPoints).text = "${totalPoints.toInt()} pts"
        findViewById<TextView>(R.id.tvStreak).text = "🔥 $streak day streak"
    }

    private fun renderLastLog(last: SunscreenLogEntry?) {
        val tvLastLog = findViewById<TextView>(R.id.tvLastLog)
        if (last == null) {
            tvLastLog.text = "No sunscreen logged yet"
            return
        }
        val minutesAgo = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - last.timestamp)
        tvLastLog.text = when {
            minutesAgo < 1 -> "Last applied just now (SPF ${last.spfUsed})"
            minutesAgo < 60 -> "Last applied $minutesAgo min ago (SPF ${last.spfUsed})"
            else -> "Last applied ${TimeUnit.MINUTES.toHours(minutesAgo)}h ago (SPF ${last.spfUsed})"
        }
    }

    private suspend fun currentLocation(): Pair<Double, Double>? {
        val loc = LocationHelper.getFreshLocation(this)
        if (loc != null) {
            getSharedPreferences("AppPreferences", Context.MODE_PRIVATE).edit()
                .putFloat("last_lat", loc.latitude.toFloat())
                .putFloat("last_lon", loc.longitude.toFloat())
                .apply()
            return loc.latitude to loc.longitude
        }
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val cachedLat = prefs.getFloat("last_lat", 0f)
        val cachedLon = prefs.getFloat("last_lon", 0f)
        if (cachedLat != 0f || cachedLon != 0f) return cachedLat.toDouble() to cachedLon.toDouble()
        return null
    }
}
