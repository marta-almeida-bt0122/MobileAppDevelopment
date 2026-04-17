package com.example.helloworld.network

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("list")
    val list: List<WeatherItem>
)

data class WeatherItem(
    @SerializedName("name")
    val name: String,
    @SerializedName("main")
    val main: WeatherMain,
    @SerializedName("weather")
    val weather: List<WeatherCondition>
)

data class WeatherMain(
    @SerializedName("temp")
    val temp: Double,
    @SerializedName("feels_like")
    val feelsLike: Double,
    @SerializedName("temp_min")
    val tempMin: Double,
    @SerializedName("temp_max")
    val tempMax: Double,
    @SerializedName("pressure")
    val pressure: Int,
    @SerializedName("humidity")
    val humidity: Int
)

data class WeatherCondition(
    @SerializedName("main")
    val main: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("icon")
    val icon: String
)

