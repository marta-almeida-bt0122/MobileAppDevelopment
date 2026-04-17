package com.example.helloworld.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

@Dao
interface ICoordinatesDao {

    @Insert
    suspend fun insert(coordinate: CoordinatesEntity)

    @Query("SELECT * FROM coordinates")
    suspend fun getAll(): List<CoordinatesEntity>

    @Query("SELECT COUNT(*) FROM coordinates")
    fun getCount(): Int

    @Query("DELETE FROM coordinates WHERE timestamp = :timestamp")
    suspend fun deleteWithTimestamp(timestamp: Long)

    @Update
    suspend fun updateCoordinate(coordinate: CoordinatesEntity)

    @Query("SELECT * FROM coordinates WHERE timestamp = :timestamp")
    suspend fun getCoordinateByTimestamp(timestamp: Long): CoordinatesEntity?
}

