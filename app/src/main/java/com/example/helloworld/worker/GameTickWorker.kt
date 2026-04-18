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
import java.util.concurrent.TimeUnit

/**
 * Background tick: every ~15 minutes, fetch environment, apply drain,
 * push updated state to Firebase, and optionally notify.
 *
 * WorkManager minimum interval is 15 minutes. The user's foreground
 * tick in GameActivity updates more frequently when the screen is open.
 */
class GameTickWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val TAG = "GameTickWorker"

    override suspend fun doWork(): Result {
        return try {
            val repo = GameRepository(applicationContext)
            val character = repo.getActiveCharacter() ?: return Result.success()

            val env = repo.fetchEnvironment(character.lastLat, character.lastLon)
                ?: return Result.retry()

            // minutes elapsed since last update
            val now = System.currentTimeMillis()
            val minutesElapsed = ((now - character.lastUpdate) / 60000.0).coerceIn(1.0, 30.0)

            val tick = GameEngine.computeTick(character, env, minutesElapsed)
            val updated = GameEngine.applyTick(character, tick, now)

            repo.updateCharacter(updated)

            // Notify when a BAD recommendation exists or HP dropped hard
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
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Worker error: ${e.message}")
            Result.retry()
        }
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

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
