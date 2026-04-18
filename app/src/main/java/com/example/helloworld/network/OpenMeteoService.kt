package com.example.helloworld.network

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo is a free weather API that requires no API key.
 * Docs: https://open-meteo.com/en/docs
 *
 * We use it exclusively for the UV index because OpenWeather's free
 * tier doesn't expose it (they moved it to the One Call 3.0 plan).
 */
interface OpenMeteoApi {

    @GET("v1/forecast")
    fun getCurrentUv(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") fields: String = "uv_index,temperature_2m,relative_humidity_2m,weather_code",
        @Query("timezone") timezone: String = "auto"
    ): Call<OpenMeteoResponse>
}

data class OpenMeteoResponse(
    @SerializedName("current") val current: OpenMeteoCurrent
)

data class OpenMeteoCurrent(
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("relative_humidity_2m") val humidity: Int,
    @SerializedName("uv_index") val uvIndex: Double,
    @SerializedName("weather_code") val weatherCode: Int
)

/**
 * Open-Meteo returns a numeric weather code (WMO codes).
 * We map the ones we care about to OpenWeather-style strings
 * so SkincareAdvisor can treat both APIs uniformly.
 */
object WeatherCodeMapper {
    fun toMainString(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2, 3 -> "Clouds"
        45, 48 -> "Fog"
        in 51..57 -> "Drizzle"
        in 61..67 -> "Rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Rain"
        in 85..86 -> "Snow"
        in 95..99 -> "Thunderstorm"
        else -> "Clear"
    }
}

/**
 * Singleton client for Open-Meteo. Separate from RetrofitClient because
 * it uses a different base URL.
 */
object OpenMeteoClient {
    private const val BASE_URL = "https://api.open-meteo.com/"

    val service: OpenMeteoApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoApi::class.java)
    }
}
