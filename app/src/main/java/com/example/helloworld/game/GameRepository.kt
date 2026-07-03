package com.example.helloworld.game

import android.content.Context
import android.util.Log
import com.example.helloworld.network.AirQualityClient
import com.example.helloworld.network.OpenMeteoClient
import com.example.helloworld.room.AppDatabase
import com.example.helloworld.room.CharacterEntity
import com.example.helloworld.room.SunscreenLogEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class GameRepository(context: Context) {

    private val TAG = "GameRepository"
    private val db = AppDatabase.getDatabase(context)
    private val characterDao = db.characterDao()
    private val sunscreenLogDao = db.sunscreenLogDao()
    private val firebase = FirebaseDatabase.getInstance().reference.child("characters")
    private val sunscreenLogsFirebase = FirebaseDatabase.getInstance().reference.child("sunscreenLogs")
    private val userPointsFirebase = FirebaseDatabase.getInstance().reference.child("userPoints")

    private fun currentUserId(): String? =
        FirebaseAuth.getInstance().currentUser?.uid

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

    suspend fun getActiveCharacter(): CharacterEntity? {
        val uid = currentUserId() ?: return null
        return withContext(Dispatchers.IO) { characterDao.getActiveForUser(uid) }
    }

    suspend fun getRanking(): List<CharacterEntity> {
        val uid = currentUserId() ?: return emptyList()
        return withContext(Dispatchers.IO) { characterDao.getRankingForUser(uid) }
    }

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

    suspend fun logSunscreen(lat: Double, lon: Double, spfUsed: Int): SunscreenLogEntry? {
        val uid = currentUserId() ?: return null
        val env = fetchEnvironment(lat, lon) ?: return null
        val now = System.currentTimeMillis()

        val previousLogs = withContext(Dispatchers.IO) { sunscreenLogDao.getAllForUser(uid) }
        val streak = PointsEngine.computeStreakForNewLog(previousLogs, now)
        val points = PointsEngine.computePoints(env, now, streak)

        val id = sunscreenLogsFirebase.push().key ?: return null
        val entry = SunscreenLogEntry(
            id = id,
            userId = uid,
            timestamp = now,
            lat = lat,
            lon = lon,
            uvIndex = env.uvIndex,
            temperatureC = env.temperatureC,
            spfUsed = spfUsed,
            pointsEarned = points
        )
        withContext(Dispatchers.IO) { sunscreenLogDao.insert(entry) }
        pushLogToFirebase(entry)
        syncTotalPointsToFirebase(uid)
        return entry
    }

    suspend fun getMyTotalPoints(): Double {
        val uid = currentUserId() ?: return 0.0
        return withContext(Dispatchers.IO) { sunscreenLogDao.getTotalPointsForUser(uid) }
    }

    suspend fun getMyStreak(): Int {
        val uid = currentUserId() ?: return 0
        val logs = withContext(Dispatchers.IO) { sunscreenLogDao.getAllForUser(uid) }
        return PointsEngine.currentStreak(logs)
    }

    suspend fun getLastSunscreenLog(): SunscreenLogEntry? {
        val uid = currentUserId() ?: return null
        return withContext(Dispatchers.IO) { sunscreenLogDao.getLastForUser(uid) }
    }

    suspend fun shouldRemindSunscreen(env: GameEngine.EnvReading, now: Long = System.currentTimeMillis()): Boolean {
        if (env.uvIndex < 6.0) return false
        val uid = currentUserId() ?: return false
        val last = withContext(Dispatchers.IO) { sunscreenLogDao.getLastForUser(uid) }
        val twoHoursMs = 2 * 60 * 60 * 1000L
        return last == null || (now - last.timestamp) > twoHoursMs
    }

    suspend fun getGlobalRanking(): List<UserPoints> = suspendCancellableCoroutine { cont ->
        userPointsFirebase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ranking = snapshot.children.mapNotNull { child ->
                    val uid = child.key ?: return@mapNotNull null
                    val displayName = child.child("displayName").getValue(String::class.java) ?: "Anonymous"
                    val totalPoints = child.child("totalPoints").getValue(Double::class.java) ?: 0.0
                    UserPoints(uid, displayName, totalPoints)
                }.sortedByDescending { it.totalPoints }
                cont.resume(ranking)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Ranking fetch cancelled: ${error.message}")
                cont.resume(emptyList())
            }
        })
    }

    private fun pushLogToFirebase(entry: SunscreenLogEntry) {
        try {
            sunscreenLogsFirebase.child(entry.userId).child(entry.id).setValue(
                mapOf(
                    "id" to entry.id,
                    "userId" to entry.userId,
                    "timestamp" to entry.timestamp,
                    "lat" to entry.lat,
                    "lon" to entry.lon,
                    "uvIndex" to entry.uvIndex,
                    "temperatureC" to entry.temperatureC,
                    "spfUsed" to entry.spfUsed,
                    "pointsEarned" to entry.pointsEarned
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Firebase sunscreen log push error: ${e.message}")
        }
    }

    private suspend fun syncTotalPointsToFirebase(uid: String) {
        val total = withContext(Dispatchers.IO) { sunscreenLogDao.getTotalPointsForUser(uid) }
        val user = FirebaseAuth.getInstance().currentUser
        val displayName = user?.displayName ?: user?.email ?: "Anonymous"
        try {
            userPointsFirebase.child(uid).setValue(
                mapOf("displayName" to displayName, "totalPoints" to total)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Firebase points sync error: ${e.message}")
        }
    }

    data class UserPoints(val userId: String, val displayName: String, val totalPoints: Double)

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
