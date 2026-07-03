package com.example.helloworld.network

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {

    @GET("v1/forecast")
    fun getCurrentUv(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") fields: String = "uv_index,temperature_2m,relative_humidity_2m",
        @Query("timezone") timezone: String = "auto"
    ): Call<OpenMeteoResponse>
}

data class OpenMeteoResponse(
    @SerializedName("current") val current: OpenMeteoCurrent
)

data class OpenMeteoCurrent(
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("relative_humidity_2m") val humidity: Int,
    @SerializedName("uv_index") val uvIndex: Double
)

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
