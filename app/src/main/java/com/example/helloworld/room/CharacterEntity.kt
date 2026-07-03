package com.example.helloworld.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val skinType: String,
    val createdAt: Long,
    val hp: Double,
    val score: Double,
    val alive: Boolean,
    val lastUpdate: Long,
    val lastLat: Double,
    val lastLon: Double
)
