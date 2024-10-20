package com.example.weatherapp.background

import com.example.weatherapp.weatherdata.model.WeatherResponse

interface IWeatherDataFetcherCallBack {
    fun onSuccess(result: WeatherResponse)
    fun onFailure(error: String)
}
