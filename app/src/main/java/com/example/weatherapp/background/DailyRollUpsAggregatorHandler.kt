package com.example.weatherapp.background

import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.weatherapp.database.WeatherDataContentProvider
import com.example.weatherapp.database.WeatherDatabaseHelper
import com.google.gson.Gson
import okhttp3.internal.closeQuietly
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.max
import kotlin.math.min

class DailyRollUpsAggregatorHandler(private var contentResolver: ContentResolver) {
    private val timestampProjection = arrayOf(WeatherDatabaseHelper.WeatherDataTable.COLUMN_TIMESTAMP)
    private val gson: Gson = Gson()
    @RequiresApi(Build.VERSION_CODES.O)
    fun dailyRollUps() {
        Log.i(TAG, "called Daily Rollups");
        val cursor: Cursor? = contentResolver.query(
            WeatherDataContentProvider.WEATHER_CONTENT_URI,
            timestampProjection,
            null,
            null,
            "timestamp DESC LIMIT 1"
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val latestTimestamp = it.getLong(it.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherDataTable.COLUMN_TIMESTAMP))
                val truncateDayEpoch = latestTimestamp - (latestTimestamp % 86400)
                Log.d(TAG, "Timestamp: $latestTimestamp TruncateDayEpoch: $truncateDayEpoch")
                val nextDayEpoch = truncateDayEpoch + 86400

                val completeData: Cursor? = contentResolver.query(
                    WeatherDataContentProvider.WEATHER_CONTENT_URI,
                    null,
                    "timestamp >= ? and timestamp < ?",
                    arrayOf("$truncateDayEpoch", "$nextDayEpoch"),
                    null,
                )

                val records: MutableMap<String, AggregatedRecord> = mutableMapOf<String, AggregatedRecord>()

                completeData?.use {
                    if (it.moveToFirst()) {
                        do {
                            val _id = it.getString(it.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherDataTable.COLUMN_TIMESTAMP))
                            val city = it.getString(it.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherDataTable.COLUMN_CITY))
                            val temperature = it.getDouble(it.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherDataTable.COLUMN_TEMPERATURE))
                            val temperatureFeelLike = it.getDouble(it.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherDataTable.COLUMN_TEMPERATURE_FEEL_LIKE))
                            val weatherCondition = it.getString(it.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherDataTable.COLUMN_WEATHER_CONDITION))

                            val record = records.getOrPut(city) { AggregatedRecord() }

                            record.totalTemperature += temperature
                            record.totalTemperatureFeelLike += temperatureFeelLike
                            record.recordCount++
                            record.maxTemperature = max(record.maxTemperature, temperature)
                            record.minTemperature = min(record.minTemperature, temperature)
                            record.maxTemperatureFeelLike = max(record.maxTemperatureFeelLike, temperatureFeelLike)
                            record.minTemperatureFeelLike = min(record.minTemperatureFeelLike, temperatureFeelLike)
                            record.weatherConditionsFreq[weatherCondition] = record.weatherConditionsFreq.getOrDefault(weatherCondition, 0) + 1

                            Log.d(TAG, "_id: $_id, city: $city record: $record")

                        } while (it.moveToNext())
                    }
                }

                completeData?.closeQuietly()

                val utcDate = Instant.ofEpochSecond(truncateDayEpoch)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate()

                for ((key, value) in records) {
                    val contentValues = ContentValues().apply {
                        put(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_TIMESTAMP, truncateDayEpoch)
                        put(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_CITY, key)
                        put(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_DAY, utcDate.dayOfMonth)
                        put(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_MONTH, utcDate.monthValue)
                        put(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_YEAR, utcDate.year)
                        put(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_AVERAGE_TEMPERATURE, value.totalTemperature / value.recordCount)
                        put(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_AVERAGE_TEMPERATURE_FEEL_LIKE, value.totalTemperatureFeelLike / value.recordCount)
                        put(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_MAXIMUM_TEMPERATURE, value.maxTemperature)
                        put(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_MAXIMUM_TEMPERATURE_FEEL_LIKE, value.maxTemperatureFeelLike)
                        put(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_MINIMUM_TEMPERATURE, value.minTemperature)
                        put(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_MINIMUM_TEMPERATURE_FEEL_LIKE, value.minTemperatureFeelLike)
                        put(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_RECORDS_IN_A_DAY, value.recordCount)
                        put(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_WEATHER_CONDITION_FREQUENCY_MAP, gson.toJson(value.weatherConditionsFreq))
                    }

                    // Insert the data into the Content Provider
                    val newUri: Uri? =
                        contentResolver.insert(WeatherDataContentProvider.AGGREGATED_WEATHER_CONTENT_URI, contentValues)

                    // Check if the insertion was successful
                    if (newUri != null) {
                        Log.d(TAG, "Inserted timestamp at: $newUri")
                    } else {
                        Log.d(TAG, "Failed to insert timestamp")
                    }
                }
            }
        }

        cursor?.closeQuietly()

    }

    companion object {
        private val TAG = WeatherDataFetcherCallBackHandler::class.simpleName
    }

    data class AggregatedRecord(
        var recordCount: Int,
        var totalTemperature: Double,
        var totalTemperatureFeelLike: Double,
        var maxTemperature: Double,
        var minTemperature: Double,
        var maxTemperatureFeelLike: Double,
        var minTemperatureFeelLike: Double,
        val weatherConditionsFreq: MutableMap<String, Int>
    ) {
        // Secondary constructor providing default values for all parameters
        constructor() : this(0, 0.0, 0.0, Double.MIN_VALUE, Double.MAX_VALUE, Double.MIN_VALUE, Double.MAX_VALUE, mutableMapOf())
    }

}