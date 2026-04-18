package com.example.helloworld.game

import android.content.Context
import android.util.Log
import com.example.helloworld.network.AirQualityClient
import com.example.helloworld.network.OpenMeteoClient
import com.example.helloworld.room.AppDatabase
import com.example.helloworld.room.CharacterEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Single point of access to characters. Writes to both Firebase and Room.
 * Room is the source of truth during offline periods; Firebase syncs when
 * online. Last-write-wins (good enough for a student project).
 */
class GameRepository(context: Context) {

    private val TAG = "GameRepository"
    private val db = AppDatabase.getDatabase(context)
    private val characterDao = db.characterDao()
    private val firebase = FirebaseDatabase.getInstance().reference.child("characters")

    private fun currentUserId(): String? =
        FirebaseAuth.getInstance().currentUser?.uid

    // ---- Create ----
    suspend fun createCharacter(
        name: String,
        skinType: String,
        lat: Double,
        lon: Double
    ): CharacterEntity? {
        val uid = currentUserId() ?: return null
        val id = firebase.push().key ?: return null
        val now = System.currentTimeMillis()
        val entity = CharacterEntity(
            id = id,
            userId = uid,
            name = name,
            skinType = skinType,
            createdAt = now,
            hp = 100.0,
            score = 0.0,
            alive = true,
            lastUpdate = now,
            lastLat = lat,
            lastLon = lon
        )
        withContext(Dispatchers.IO) { characterDao.insert(entity) }
        pushToFirebase(entity)
        return entity
    }

    // ---- Read ----
    suspend fun getActiveCharacter(): CharacterEntity? {
        val uid = currentUserId() ?: return null
        return withContext(Dispatchers.IO) { characterDao.getActiveForUser(uid) }
    }

    suspend fun getRanking(): List<CharacterEntity> {
        val uid = currentUserId() ?: return emptyList()
        return withContext(Dispatchers.IO) { characterDao.getRankingForUser(uid) }
    }

    // ---- Update ----
    suspend fun updateCharacter(character: CharacterEntity) {
        withContext(Dispatchers.IO) { characterDao.update(character) }
        pushToFirebase(character)
    }

    private fun pushToFirebase(character: CharacterEntity) {
        try {
            firebase.child(character.id).setValue(
                mapOf(
                    "id" to character.id,
                    "userId" to character.userId,
                    "name" to character.name,
                    "skinType" to character.skinType,
                    "createdAt" to character.createdAt,
                    "hp" to character.hp,
                    "score" to character.score,
                    "alive" to character.alive,
                    "lastUpdate" to character.lastUpdate,
                    "lastLat" to character.lastLat,
                    "lastLon" to character.lastLon
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Firebase push error: ${e.message}")
        }
    }

    // ---- Fetch environment (Open-Meteo + Air Quality) ----
    suspend fun fetchEnvironment(lat: Double, lon: Double): GameEngine.EnvReading? {
        val weather = fetchWeather(lat, lon) ?: return null
        val pm25 = fetchPm25(lat, lon) ?: 0.0
        return GameEngine.EnvReading(
            uvIndex = weather.uvIndex,
            temperatureC = weather.temperature,
            humidity = weather.humidity,
            pm25 = pm25
        )
    }

    private suspend fun fetchWeather(lat: Double, lon: Double): WeatherBundle? =
        suspendCancellableCoroutine { cont ->
            OpenMeteoClient.service.getCurrentUv(lat, lon).enqueue(
                object : retrofit2.Callback<com.example.helloworld.network.OpenMeteoResponse> {
                    override fun onResponse(
                        call: retrofit2.Call<com.example.helloworld.network.OpenMeteoResponse>,
                        response: retrofit2.Response<com.example.helloworld.network.OpenMeteoResponse>
                    ) {
                        val body = response.body()
                        if (body != null) {
                            cont.resume(
                                WeatherBundle(
                                    uvIndex = body.current.uvIndex,
                                    temperature = body.current.temperature,
                                    humidity = body.current.humidity
                                )
                            )
                        } else cont.resume(null)
                    }
                    override fun onFailure(
                        call: retrofit2.Call<com.example.helloworld.network.OpenMeteoResponse>,
                        t: Throwable
                    ) {
                        Log.e(TAG, "Weather fail: ${t.message}")
                        cont.resume(null)
                    }
                }
            )
        }

    private suspend fun fetchPm25(lat: Double, lon: Double): Double? =
        suspendCancellableCoroutine { cont ->
            AirQualityClient.service.getCurrentAirQuality(lat, lon).enqueue(
                object : retrofit2.Callback<com.example.helloworld.network.AirQualityResponse> {
                    override fun onResponse(
                        call: retrofit2.Call<com.example.helloworld.network.AirQualityResponse>,
                        response: retrofit2.Response<com.example.helloworld.network.AirQualityResponse>
                    ) {
                        cont.resume(response.body()?.current?.pm25 ?: 0.0)
                    }
                    override fun onFailure(
                        call: retrofit2.Call<com.example.helloworld.network.AirQualityResponse>,
                        t: Throwable
                    ) {
                        Log.e(TAG, "Air quality fail: ${t.message}")
                        cont.resume(null)
                    }
                }
            )
        }

    private data class WeatherBundle(
        val uvIndex: Double,
        val temperature: Double,
        val humidity: Int
    )
}
