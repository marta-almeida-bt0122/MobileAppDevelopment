package com.example.helloworld.game

import com.example.helloworld.room.CharacterEntity
import java.util.concurrent.TimeUnit

/**
 * Pure Kotlin game engine. No Android / network dependencies.
 * Handles HP drain, score accumulation, life stage and recommendations.
 */
object GameEngine {

    // ---- Life stages (days alive) ----
    enum class LifeStage(val emoji: String, val label: String) {
        EGG("🥚",     "Egg"),
        BABY("👶",    "Baby"),
        ADULT("🧑",   "Adult"),
        OLD("🧓",     "Old"),
        DEAD("💀",    "Dead")
    }

    // ---- Skin types ----
    data class SkinType(val id: String, val emoji: String, val label: String, val description: String)

    val SKIN_TYPES = listOf(
        SkinType("dry",       "🌵", "Dry",       "Flakes in cold & dry air"),
        SkinType("mixed",     "🌸", "Mixed",     "Balanced, moderate sensitivity"),
        SkinType("oily",      "🫒", "Oily",      "Prone to shine in humid heat"),
        SkinType("sensitive", "🐚", "Sensitive", "Reacts to UV and pollution")
    )

    fun skinTypeById(id: String): SkinType =
        SKIN_TYPES.firstOrNull { it.id == id } ?: SKIN_TYPES[1]

    // ---- Life stage from age ----
    fun lifeStage(character: CharacterEntity, now: Long = System.currentTimeMillis()): LifeStage {
        if (!character.alive || character.hp <= 0.0) return LifeStage.DEAD
        val ageMs = now - character.createdAt
        val ageHours = TimeUnit.MILLISECONDS.toHours(ageMs)
        val ageDays = TimeUnit.MILLISECONDS.toDays(ageMs)
        return when {
            ageHours < 1 -> LifeStage.EGG
            ageDays < 2  -> LifeStage.BABY
            ageDays < 8  -> LifeStage.ADULT
            ageDays < 15 -> LifeStage.OLD
            else         -> LifeStage.DEAD
        }
    }

    fun ageInDays(character: CharacterEntity, now: Long = System.currentTimeMillis()): Long =
        TimeUnit.MILLISECONDS.toDays(now - character.createdAt)

    // ---- Character emoji (face based on HP) ----
    fun faceEmoji(hp: Double, alive: Boolean): String = when {
        !alive || hp <= 0 -> "💀"
        hp > 70           -> "😊"
        hp > 40           -> "😟"
        else              -> "😰"
    }

    // ---- Environmental conditions ----
    data class EnvReading(
        val uvIndex: Double,
        val temperatureC: Double,
        val humidity: Int,
        val pm25: Double  // µg/m³
    )

    data class Recommendation(val severity: Severity, val text: String) {
        enum class Severity { INFO, WARN, BAD }
    }

    /**
     * Compute HP drain per minute + recommendations, based on the environment
     * reading and the skin type. Skin type multipliers make dry skin suffer
     * more in cold/dry air, oily skin more in heat, etc.
     */
    data class TickResult(
        val hpDelta: Double,          // negative = drain
        val scoreDelta: Double,       // always ≥ 0
        val recommendations: List<Recommendation>
    )

    fun computeTick(
        character: CharacterEntity,
        env: EnvReading,
        minutesElapsed: Double
    ): TickResult {
        val skin = skinTypeById(character.skinType)
        val recs = mutableListOf<Recommendation>()
        var drainPerMinute = 0.0

        // UV — hurts everyone but sensitive skin gets extra
        val uvMult = if (skin.id == "sensitive") 1.5 else 1.0
        when {
            env.uvIndex >= 8 -> {
                drainPerMinute += 1.5 * uvMult
                recs += Recommendation(Recommendation.Severity.BAD,
                    "Very high UV (${"%.1f".format(env.uvIndex)}). Use SPF 50+ and avoid midday sun.")
            }
            env.uvIndex >= 6 -> {
                drainPerMinute += 1.0 * uvMult
                recs += Recommendation(Recommendation.Severity.WARN,
                    "High UV. Apply SPF 30-50 and reapply every 2h.")
            }
            env.uvIndex >= 3 -> {
                drainPerMinute += 0.3 * uvMult
                recs += Recommendation(Recommendation.Severity.INFO,
                    "Moderate UV. SPF 30 recommended.")
            }
        }

        // Temperature
        when {
            env.temperatureC >= 30 -> {
                val mult = if (skin.id == "oily") 1.5 else 1.0
                drainPerMinute += 0.8 * mult
                recs += Recommendation(Recommendation.Severity.WARN,
                    "Heat (${env.temperatureC.toInt()}°C). Hydrate and use lightweight moisturizer.")
            }
            env.temperatureC <= 5 -> {
                val mult = if (skin.id == "dry") 1.5 else 1.0
                drainPerMinute += 0.8 * mult
                recs += Recommendation(Recommendation.Severity.WARN,
                    "Cold (${env.temperatureC.toInt()}°C). Use rich cream and lip balm.")
            }
        }

        // Humidity
        when {
            env.humidity < 30 -> {
                val mult = if (skin.id == "dry") 1.4 else 1.0
                drainPerMinute += 0.5 * mult
                recs += Recommendation(Recommendation.Severity.WARN,
                    "Dry air (${env.humidity}%). Use hydrating serum with hyaluronic acid.")
            }
            env.humidity > 75 -> {
                val mult = if (skin.id == "oily") 1.4 else 1.0
                drainPerMinute += 0.3 * mult
                recs += Recommendation(Recommendation.Severity.INFO,
                    "High humidity (${env.humidity}%). Prefer oil-free products.")
            }
        }

        // Air quality (PM2.5)
        when {
            env.pm25 >= 55 -> {
                drainPerMinute += 1.2
                recs += Recommendation(Recommendation.Severity.BAD,
                    "Unhealthy air (PM2.5 ${env.pm25.toInt()}). Deep cleanse skin when home.")
            }
            env.pm25 >= 35 -> {
                drainPerMinute += 0.6
                recs += Recommendation(Recommendation.Severity.WARN,
                    "Polluted air (PM2.5 ${env.pm25.toInt()}). Use antioxidant serum.")
            }
        }

        if (recs.isEmpty()) {
            recs += Recommendation(Recommendation.Severity.INFO,
                "Conditions are good. Keep your routine.")
        }

        val hpDelta = -drainPerMinute * minutesElapsed
        // Score: time survived rewards you; bad conditions reward less
        val scoreDelta = minutesElapsed * (1.0 - minOf(drainPerMinute / 5.0, 0.8))

        return TickResult(hpDelta, scoreDelta, recs)
    }

    /**
     * Apply tick to a character and return the updated one.
     * Caps HP at [0, 100], marks dead if hp <= 0 or lifetime exceeded.
     */
    fun applyTick(
        character: CharacterEntity,
        result: TickResult,
        now: Long = System.currentTimeMillis(),
        newLat: Double = character.lastLat,
        newLon: Double = character.lastLon
    ): CharacterEntity {
        val newHp = (character.hp + result.hpDelta).coerceIn(0.0, 100.0)
        val newScore = character.score + result.scoreDelta
        val stage = lifeStage(character.copy(hp = newHp), now)
        val stillAlive = newHp > 0.0 && stage != LifeStage.DEAD
        return character.copy(
            hp = newHp,
            score = newScore,
            alive = stillAlive,
            lastUpdate = now,
            lastLat = newLat,
            lastLon = newLon
        )
    }
}
