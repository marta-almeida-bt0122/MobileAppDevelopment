package com.example.helloworld.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.example.helloworld.game.GameEngine
import com.example.helloworld.game.GameNotifier
import com.example.helloworld.game.GameRepository
import com.example.helloworld.room.CharacterEntity
import java.util.concurrent.TimeUnit

class GameTickWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val TAG = "GameTickWorker"

    override suspend fun doWork(): Result {
        return try {
            val repo = GameRepository(applicationContext)
            val character = repo.getActiveCharacter()

            val location = character?.let { it.lastLat to it.lastLon } ?: cachedLocation()
                ?: return Result.success()

            val env = repo.fetchEnvironment(location.first, location.second)
                ?: return Result.retry()

            if (character != null) {
                tickCharacter(repo, character, env)
            }

            if (repo.shouldRemindSunscreen(env)) {
                GameNotifier.notify(
                    applicationContext,
                    "High UV right now ☀️",
                    "UV index is %.1f. Have you applied sunscreen?".format(env.uvIndex)
                )
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Worker error: ${e.message}")
            Result.retry()
        }
    }

    private suspend fun tickCharacter(
        repo: GameRepository,
        character: CharacterEntity,
        env: GameEngine.EnvReading
    ) {
        if (!character.alive) return

        val now = System.currentTimeMillis()
        val minutesElapsed = ((now - character.lastUpdate) / 60000.0).coerceIn(1.0, 30.0)

        val tick = GameEngine.computeTick(character, env, minutesElapsed)
        val updated = GameEngine.applyTick(character, tick, now)

        repo.updateCharacter(updated)

        val bad = tick.recommendations.firstOrNull {
            it.severity == GameEngine.Recommendation.Severity.BAD
        }
        if (bad != null) {
            GameNotifier.notify(
                applicationContext,
                "${updated.name} needs your attention",
                bad.text
            )
        }
        if (!updated.alive && character.alive) {
            GameNotifier.notify(
                applicationContext,
                "${updated.name} has died 💀",
                "Final score: ${updated.score.toInt()}. Create a new character."
            )
        }
    }

    private fun cachedLocation(): Pair<Double, Double>? {
        val prefs = applicationContext.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("last_lat", 0f)
        val lon = prefs.getFloat("last_lon", 0f)
        if (lat == 0f && lon == 0f) return null
        return lat.toDouble() to lon.toDouble()
    }

    companion object {
        private const val WORK_NAME = "skintagotchi_tick"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<GameTickWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
