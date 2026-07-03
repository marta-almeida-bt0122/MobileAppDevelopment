package com.example.helloworld.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One "I applied sunscreen" event. `id` is the Firebase push key so Room
 * and Firebase stay in sync, mirroring [CharacterEntity].
 */
@Entity(tableName = "sunscreen_logs")
data class SunscreenLogEntry(
    @PrimaryKey val id: String,
    val userId: String,
    val timestamp: Long,
    val lat: Double,
    val lon: Double,
    val uvIndex: Double,
    val temperatureC: Double,
    val spfUsed: Int,
    val pointsEarned: Double
)
