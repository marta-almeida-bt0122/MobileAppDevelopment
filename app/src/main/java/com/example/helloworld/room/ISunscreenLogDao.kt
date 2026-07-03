package com.example.helloworld.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ISunscreenLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SunscreenLogEntry)

    @Query("SELECT * FROM sunscreen_logs WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllForUser(userId: String): List<SunscreenLogEntry>

    @Query("SELECT * FROM sunscreen_logs WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastForUser(userId: String): SunscreenLogEntry?

    @Query("SELECT COALESCE(SUM(pointsEarned), 0) FROM sunscreen_logs WHERE userId = :userId")
    suspend fun getTotalPointsForUser(userId: String): Double
}
