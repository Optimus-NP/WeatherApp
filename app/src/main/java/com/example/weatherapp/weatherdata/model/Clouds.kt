package com.example.weatherapp.weatherdata.model

import com.google.gson.annotations.SerializedName

data class Clouds (
    @SerializedName("all") val cloudinessPercentage: Int
)