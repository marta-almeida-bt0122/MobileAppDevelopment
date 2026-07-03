package com.example.helloworld.network

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface AirQualityApi {

    @GET("v1/air-quality")
    fun getCurrentAirQuality(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") fields: String = "pm2_5"
    ): Call<AirQualityResponse>
}

data class AirQualityResponse(
    @SerializedName("current") val current: AirQualityCurrent
)

data class AirQualityCurrent(
    @SerializedName("pm2_5") val pm25: Double?
)

object AirQualityClient {
    private const val BASE_URL = "https://air-quality-api.open-meteo.com/"

    val service: AirQualityApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AirQualityApi::class.java)
    }
}
