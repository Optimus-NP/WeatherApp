package com.example.weatherapp.weatherdata.model

import com.google.gson.annotations.SerializedName

data class WeatherCondition(
    @SerializedName("id") val conditionId: Int,
    @SerializedName("main") val conditionType: String,
    @SerializedName("description") val conditionDescription: String,
    @SerializedName("icon") val iconCode: String
)
