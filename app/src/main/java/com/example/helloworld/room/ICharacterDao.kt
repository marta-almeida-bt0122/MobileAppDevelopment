package com.example.helloworld.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ICharacterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(character: CharacterEntity)

    @Update
    suspend fun update(character: CharacterEntity)

    @Query("SELECT * FROM characters WHERE userId = :userId AND alive = 1 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveForUser(userId: String): CharacterEntity?

    @Query("SELECT * FROM characters WHERE userId = :userId ORDER BY score DESC")
    suspend fun getRankingForUser(userId: String): List<CharacterEntity>
}
