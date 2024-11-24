package com.example.weatherapp.background

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.weatherapp.weatherdata.WeatherDataFetcher
import java.util.concurrent.TimeUnit

class WeatherDataProcessorService : Service() {
    private var intervalInMillis = TimeUnit.MINUTES.toMillis(5L);
    private var initialDelayInMillis = TimeUnit.SECONDS.toMillis(30L);
    private var dailyRollUpsIntervalInMillis = TimeUnit.HOURS.toMillis(1L);

    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var dataFetcher: WeatherDataFetcher? = null
    private var dailyRollUpsThread: HandlerThread? = null
    private var dailyRollUpsHandler: Handler? = null
    private var weatherDataFetcherCallBackHandler: WeatherDataFetcherCallBackHandler? = null
    private var dailyRollUpsAggregatorHandler: DailyRollUpsAggregatorHandler? = null

    override fun onBind(intent: Intent): IBinder? {
        return null // This service doesn't support binding
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            Log.e(TAG, "Received null intent")
            return START_NOT_STICKY // Handle null case appropriately
        }

        Log.d(TAG, "onStartCommand called")

        // Create a HandlerThread
        handlerThread = HandlerThread(TAG + "WeatherDataFetcherThread")
        handlerThread!!.start()
        dataFetcher = WeatherDataFetcher()
        weatherDataFetcherCallBackHandler = WeatherDataFetcherCallBackHandler(this, contentResolver)

        // Get a Handler on the HandlerThread's Looper
        handler = Handler(handlerThread!!.looper)

        handler!!.postDelayed(object : Runnable {
            override fun run() {
                // Perform your background tasks here
                Log.d(TAG, "Performing background tasks")

                dataFetcher!!.fetchData(weatherDataFetcherCallBackHandler!!)

                // Schedule the next task
                handler!!.postDelayed(this, intervalInMillis)
            }
        }, initialDelayInMillis)

        Log.d(TAG, "Handler thread initiated")

        // Create the Rollups Aggregator Thread

        dailyRollUpsAggregatorHandler = DailyRollUpsAggregatorHandler(contentResolver)

        dailyRollUpsThread = HandlerThread(TAG + "DailyRollUpsThread")
        dailyRollUpsThread!!.start()
        dailyRollUpsHandler = Handler(dailyRollUpsThread!!.looper)

        dailyRollUpsHandler!!.postDelayed(object : Runnable {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun run() {
                // Perform your background tasks here
                dailyRollUpsAggregatorHandler!!.dailyRollUps()
                // Schedule the next task
                dailyRollUpsHandler!!.postDelayed(this, dailyRollUpsIntervalInMillis)
            }
        }, initialDelayInMillis)

        Log.d(TAG, "Aggregator thread initiated")

        return START_STICKY // Restart the service if it's killed
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")

        // Stop the HandlerThread
        if (handlerThread != null) {
            handlerThread!!.quit()
        }
    }

    companion object {
        private val TAG = WeatherDataProcessorService::class.simpleName
    }
}