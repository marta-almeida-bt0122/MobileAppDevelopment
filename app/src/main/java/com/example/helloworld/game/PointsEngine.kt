package com.example.helloworld.game

import com.example.helloworld.room.SunscreenLogEntry
import java.util.Calendar

/**
 * Pure Kotlin scoring engine for the sunscreen habit. No Android / network
 * dependencies, same spirit as [GameEngine].
 *
 * Points = base + UV bonus + heat bonus, scaled by a solar-elevation
 * multiplier (same UV reading is riskier at solar noon than at 9am) and by
 * the current streak of consecutive days.
 */
object PointsEngine {

    const val BASE_POINTS = 10.0

    private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L
    private const val MAX_STREAK_BONUS_DAYS = 20
    private const val STREAK_BONUS_PER_DAY = 0.05 // +5% per consecutive day, capped at +100%

    /**
     * Points earned for logging sunscreen right now, given the environment
     * reading, the moment it was applied and the streak of consecutive days
     * (including the day being logged).
     */
    fun computePoints(
        env: GameEngine.EnvReading,
        appliedAtMillis: Long,
        streakDays: Int
    ): Double {
        var points = BASE_POINTS

        // UV bonus: protecting yourself when the UV is high is worth more
        // than doing it on a cloudy day.
        points += env.uvIndex.coerceAtLeast(0.0) * 3.0

        // Heat bonus: extreme heat means more skin exposed / more time
        // outdoors, so protecting against it is worth more.
        if (env.temperatureC >= 25.0) {
            points += (env.temperatureC - 25.0).coerceAtMost(20.0) * 0.6
        }

        // Third factor: solar elevation via hour of day. The same
        // instantaneous UV index is riskier around solar noon than in the
        // early morning or late afternoon, so applying then is worth more.
        val hour = hourOfDay(appliedAtMillis)
        val solarMultiplier = when (hour) {
            in 11..15 -> 1.3
            in 9..10, in 16..17 -> 1.15
            else -> 1.0
        }
        points *= solarMultiplier

        // Streak multiplier: consistency is rewarded, resets if a day is skipped.
        points *= streakMultiplier(streakDays)

        return points
    }

    fun streakMultiplier(streakDays: Int): Double =
        1.0 + streakDays.coerceAtMost(MAX_STREAK_BONUS_DAYS) * STREAK_BONUS_PER_DAY

    /**
     * Streak length (in days) if the user logs sunscreen right now, counting
     * today plus any consecutive prior days already logged in [previousLogs].
     */
    fun computeStreakForNewLog(previousLogs: List<SunscreenLogEntry>, now: Long = System.currentTimeMillis()): Int {
        val loggedDays = previousLogs.mapTo(HashSet()) { dayEpoch(it.timestamp) }
        var streak = 1
        var cursor = dayEpoch(now) - ONE_DAY_MS
        while (loggedDays.contains(cursor)) {
            streak++
            cursor -= ONE_DAY_MS
        }
        return streak
    }

    /**
     * Streak length as it stands right now (no new log yet). Zero if the
     * most recent log isn't from today or yesterday, since a day was skipped.
     */
    fun currentStreak(previousLogs: List<SunscreenLogEntry>, now: Long = System.currentTimeMillis()): Int {
        if (previousLogs.isEmpty()) return 0
        val today = dayEpoch(now)
        val mostRecentDay = dayEpoch(previousLogs.maxOf { it.timestamp })
        if (mostRecentDay != today && mostRecentDay != today - ONE_DAY_MS) return 0

        val loggedDays = previousLogs.mapTo(HashSet()) { dayEpoch(it.timestamp) }
        var streak = 1
        var cursor = mostRecentDay - ONE_DAY_MS
        while (loggedDays.contains(cursor)) {
            streak++
            cursor -= ONE_DAY_MS
        }
        return streak
    }

    private fun dayEpoch(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun hourOfDay(millis: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        return cal.get(Calendar.HOUR_OF_DAY)
    }
}
