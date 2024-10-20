package com.example.weatherapp.weatherdata

import android.util.Log
import com.example.weatherapp.background.IWeatherDataFetcherCallBack
import com.example.weatherapp.constants.City
import com.example.weatherapp.weatherdata.model.WeatherResponse
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException

class WeatherDataFetcher {
    private val API_KEY: String = "e02e6cde06f5fdd0a7ebeabb61d146c0"
    private val gson: Gson = Gson()

    fun fetchData(weatherCallBackHandler: IWeatherDataFetcherCallBack) {
        for (city in City.entries) {
            val cityId = city.cityId
            Log.i(TAG, "Processing City: $city")
            // URL to request
            val url =
                "https://api.openweathermap.org/data/2.5/weather?id=${cityId}&appid=${API_KEY}"

            // Create OkHttpClient instance
            val client = OkHttpClient()

            // Build the request
            val request = Request.Builder()
                .url(url)
                .build()

            // Perform asynchronous GET request
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // Handle request failure
                    Log.e(TAG, "Request Failed: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    // Handle request success
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        Log.d(TAG, "Response: $responseBody")
                        val weatherResponse =
                            gson.fromJson(responseBody, WeatherResponse::class.java)
                        Log.i(TAG, weatherResponse.toString())
                        weatherCallBackHandler.onSuccess(weatherResponse)
                    } else {
                        Log.e(TAG, "Request Not Successful")
                        weatherCallBackHandler.onFailure("Request Not Successful")
                    }
                }
            })
        }
    }

    companion object {
        private val TAG = WeatherDataFetcher::class.simpleName
    }
}