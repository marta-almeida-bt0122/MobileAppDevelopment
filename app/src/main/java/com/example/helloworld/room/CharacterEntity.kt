package com.example.helloworld.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persistent representation of a skin character.
 * `id` is the Firebase push key so Room and Firebase stay in sync.
 */
@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val skinType: String,        // "dry" | "mixed" | "oily" | "sensitive"
    val createdAt: Long,
    val hp: Double,              // 0-100
    val score: Double,
    val alive: Boolean,
    val lastUpdate: Long,
    val lastLat: Double,
    val lastLon: Double
)
