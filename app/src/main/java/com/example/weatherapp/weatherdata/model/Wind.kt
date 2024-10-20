package com.example.weatherapp.weatherdata.model

import com.google.gson.annotations.SerializedName

data class Wind(
    @SerializedName("speed") val windSpeed: Double,
    @SerializedName("deg") val windDirectionDegrees: Int
)
