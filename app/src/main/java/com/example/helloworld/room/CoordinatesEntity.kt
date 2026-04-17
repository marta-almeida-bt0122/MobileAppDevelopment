package com.example.helloworld.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coordinates")
data class CoordinatesEntity(
    @PrimaryKey
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double
)

