package com.example.weatherapp.background

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.example.weatherapp.constants.Settings
import com.example.weatherapp.database.WeatherDataContentProvider
import com.example.weatherapp.database.WeatherDatabaseHelper
import com.example.weatherapp.notifications.NotificationHelper
import com.example.weatherapp.weatherdata.model.WeatherResponse
import okhttp3.internal.closeQuietly
import kotlin.math.abs

class WeatherDataFetcherCallBackHandler() : IWeatherDataFetcherCallBack {
    private lateinit var contentResolver: ContentResolver
    private lateinit var notificationHelper: NotificationHelper

    constructor(context: Context, contentResolver: ContentResolver) : this() {
        this.contentResolver = contentResolver
        notificationHelper = NotificationHelper(context)
        notificationHelper.createNotificationChannel()
    }

    override fun onSuccess(result: WeatherResponse) {
        // Create a ContentValues object to hold the data
        val contentValues = ContentValues().apply {
            put(WeatherDatabaseHelper.WeatherDataTable.COLUMN_TIMESTAMP, result.dataTimestamp)
            put(
                WeatherDatabaseHelper.WeatherDataTable.COLUMN_TEMPERATURE,
                result.mainWeather.temperature
            )
            put(
                WeatherDatabaseHelper.WeatherDataTable.COLUMN_WEATHER_CONDITION,
                result.weatherConditions[0].conditionType
            )
            put(
                WeatherDatabaseHelper.WeatherDataTable.COLUMN_TEMPERATURE_FEEL_LIKE,
                result.mainWeather.feelsLike
            )
            put(WeatherDatabaseHelper.WeatherDataTable.COLUMN_CITY, result.cityName)
        }

        // Insert the data into the Content Provider
        val newUri: Uri? =
            contentResolver.insert(WeatherDataContentProvider.WEATHER_CONTENT_URI, contentValues)

        handleNotifications(result)

        // Check if the insertion was successful
        if (newUri != null) {
            Log.d(TAG, "Inserted timestamp at: $newUri")
        } else {
            Log.d(TAG, "Failed to insert timestamp")
        }
    }

    private fun handleNotifications(result: WeatherResponse) {
        if (areNotificationEnabled()) {
            val truncateDayEpoch = result.dataTimestamp - (result.dataTimestamp % 86400)
            val completeData: Cursor? = contentResolver.query(
                WeatherDataContentProvider.AGGREGATED_WEATHER_CONTENT_URI,
                null,
                "timestamp = ? and city = ?",
                arrayOf("$truncateDayEpoch", result.cityName),
                null,
            )

            completeData?.use {
                if (it.moveToFirst()) {
                    val averageTemperature = it.getDouble(it.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_AVERAGE_TEMPERATURE))
                    val averageTemperatureFeelLike = it.getDouble(it.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_AVERAGE_TEMPERATURE_FEEL_LIKE))

                    val percentageChangeTemperature = (100 * abs(result.mainWeather.temperature - averageTemperature)) / averageTemperature
                    val percentageChangeTemperatureFeelLike = (100 * abs(result.mainWeather.feelsLike - averageTemperatureFeelLike)) / averageTemperatureFeelLike

                    Log.i(TAG, "Percentage Change: ${percentageChangeTemperature}, Percentage Change In Feels Like: ${percentageChangeTemperatureFeelLike}")

                    if (percentageChangeTemperatureFeelLike <= PERCENTAGE_CHANGE || percentageChangeTemperature <= PERCENTAGE_CHANGE) {
                        notificationHelper.sendNotification("Temperature Change Alert For City: ${result.cityName}", "Observed a change in the temperature. Current Temperature: ${result.mainWeather.temperature}, Average Current Temperature: ${averageTemperature}, Current Feels Like Temperature: ${result.mainWeather.temperature}, Average Feels Like Temperature: ${averageTemperatureFeelLike}")

                        Log.i(TAG, "Notification being send for the change in the temperature")
                    }
                }
            }

            completeData?.closeQuietly()
        }
    }

    private fun areNotificationEnabled(): Boolean {
        // Query the Content Provider for the notification setting
        val projection = arrayOf(WeatherDatabaseHelper.UserSettingsTable.COLUMN_SETTING_VALUE)
        val selection = "${WeatherDatabaseHelper.UserSettingsTable.COLUMN_SETTING_KEY} = ?"
        val selectionArgs = arrayOf(Settings.NOTIFICATIONS_ENABLED.settingKey)

        val cursor = contentResolver.query(
            WeatherDataContentProvider.SETTING_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val value = it.getString(it.getColumnIndexOrThrow(WeatherDatabaseHelper.UserSettingsTable.COLUMN_SETTING_VALUE))
                return value.toBoolean()
            }
        }
        return Settings.NOTIFICATIONS_ENABLED.defaultValue.toBoolean()
    }

    override fun onFailure(error: String) {
        Log.e(TAG, "Failure observed: $error")
    }

    companion object {
        private const val PERCENTAGE_CHANGE = 0.1;
        private val TAG = WeatherDataFetcherCallBackHandler::class.simpleName
    }
}