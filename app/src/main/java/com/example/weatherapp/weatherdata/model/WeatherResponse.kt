package com.example.weatherapp.weatherdata.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("coord") val coordinates: Coordinates,
    @SerializedName("weather") val weatherConditions: List<WeatherCondition>,
    @SerializedName("base") val baseStation: String,
    @SerializedName("main") val mainWeather: MainWeatherData,
    @SerializedName("visibility") val visibilityInMeters: Int,
    @SerializedName("wind") val windData: Wind,
    @SerializedName("clouds") val cloudData: Clouds,
    @SerializedName("dt") val dataTimestamp: Long,
    @SerializedName("sys") val systemData: SystemData,
    @SerializedName("timezone") val timezoneOffset: Int,
    @SerializedName("id") val cityId: Int,
    @SerializedName("name") val cityName: String,
    @SerializedName("cod") val responseCode: Int
)
