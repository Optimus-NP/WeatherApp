package com.example.weatherapp.weatherdata.model

import com.google.gson.annotations.SerializedName

data class SystemData(
    @SerializedName("type") val systemType: Int,
    @SerializedName("id") val systemId: Int,
    @SerializedName("country") val countryCode: String,
    @SerializedName("sunrise") val sunriseTimestamp: Long,
    @SerializedName("sunset") val sunsetTimestamp: Long
)
