package com.example.weatherapp.weatherdata.model

import com.google.gson.annotations.SerializedName

data class MainWeatherData(
    @SerializedName("temp") val temperature: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("temp_min") val minimumTemperature: Double,
    @SerializedName("temp_max") val maximumTemperature: Double,
    @SerializedName("pressure") val pressureInHpa: Int,
    @SerializedName("humidity") val humidityPercentage: Int,
    @SerializedName("sea_level") val seaLevelPressure: Int,
    @SerializedName("grnd_level") val groundLevelPressure: Int
)
