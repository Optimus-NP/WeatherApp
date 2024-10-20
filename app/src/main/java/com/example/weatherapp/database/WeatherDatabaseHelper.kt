package com.example.weatherapp.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class WeatherDatabaseHelper(context: Context?) :
    SQLiteOpenHelper(context, "weather_app.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_CREATE_TABLE_WEATHER_DATA)
        db?.execSQL(SQL_CREATE_TABLE_AGGREGATED_WEATHER_DATA)
        db?.execSQL(SQL_CREATE_TABLE_USER_SETTINGS)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS ${WeatherDataTable.WEATHER_TABLE}")
        db?.execSQL("DROP TABLE IF EXISTS ${WeatherAggregatedDataTable.AGGREGATED_WEATHER_TABLE}")
        db?.execSQL("DROP TABLE IF EXISTS ${UserSettingsTable.SETTINGS_TABLE}")
        onCreate(db)
    }

    class WeatherDataTable {
        companion object {
            const val WEATHER_TABLE = "CityWiseWeatherData"
            const val COLUMN_TIMESTAMP = "timestamp"
            const val COLUMN_CITY = "city"
            const val COLUMN_TEMPERATURE = "temperature"
            const val COLUMN_WEATHER_CONDITION = "weather_condition"
            const val COLUMN_TEMPERATURE_FEEL_LIKE = "temperature_feel_like"
        }
    }

    class UserSettingsTable {
        companion object {
            const val SETTINGS_TABLE = "UserSettings"
            const val COLUMN_SETTING_KEY = "setting_key"
            const val COLUMN_SETTING_VALUE = "setting_value"
        }
    }

    class WeatherAggregatedDataTable {
        companion object {
            const val AGGREGATED_WEATHER_TABLE = "CityWiseDailyRollUpWeatherData"
            const val COLUMN_TIMESTAMP = "timestamp"
            const val COLUMN_CITY = "city"
            const val COLUMN_AVERAGE_TEMPERATURE = "average_temperature"
            const val COLUMN_MINIMUM_TEMPERATURE = "minimum_temperature"
            const val COLUMN_MAXIMUM_TEMPERATURE = "maximum_temperature"
            const val COLUMN_WEATHER_CONDITION_FREQUENCY_MAP = "weather_condition_frequency_map"
            const val COLUMN_AVERAGE_TEMPERATURE_FEEL_LIKE = "average_temperature_feel_like"
            const val COLUMN_MINIMUM_TEMPERATURE_FEEL_LIKE = "minimum_temperature_feel_like"
            const val COLUMN_MAXIMUM_TEMPERATURE_FEEL_LIKE = "maximum_temperature_feel_like"
            const val COLUMN_YEAR = "year"
            const val COLUMN_MONTH = "month"
            const val COLUMN_DAY = "day"
            const val COLUMN_RECORDS_IN_A_DAY = "recordsInADay"
        }
    }

    companion object {
        private const val SQL_CREATE_TABLE_USER_SETTINGS =
            """
                CREATE TABLE ${UserSettingsTable.SETTINGS_TABLE} (
                    ${UserSettingsTable.COLUMN_SETTING_KEY} TEXT NOT NULL,
                    ${UserSettingsTable.COLUMN_SETTING_VALUE} TEXT NOT NULL,
                    PRIMARY KEY (${UserSettingsTable.COLUMN_SETTING_KEY})
                );
            """

        private const val SQL_CREATE_TABLE_WEATHER_DATA =
            """
                CREATE TABLE ${WeatherDataTable.WEATHER_TABLE} (
                    ${WeatherDataTable.COLUMN_TIMESTAMP} INTEGER NOT NULL,
                    ${WeatherDataTable.COLUMN_CITY} TEXT NOT NULL,
                    ${WeatherDataTable.COLUMN_TEMPERATURE} REAL,
                    ${WeatherDataTable.COLUMN_WEATHER_CONDITION} TEXT,
                    ${WeatherDataTable.COLUMN_TEMPERATURE_FEEL_LIKE} REAL,
                    PRIMARY KEY (${WeatherDataTable.COLUMN_TIMESTAMP}, ${WeatherDataTable.COLUMN_CITY})
                );
            """

        private const val SQL_CREATE_TABLE_AGGREGATED_WEATHER_DATA =
            """
                CREATE TABLE ${WeatherAggregatedDataTable.AGGREGATED_WEATHER_TABLE} (
                    ${WeatherAggregatedDataTable.COLUMN_TIMESTAMP} INTEGER NOT NULL,
                    ${WeatherAggregatedDataTable.COLUMN_CITY} TEXT NOT NULL,
                    ${WeatherAggregatedDataTable.COLUMN_AVERAGE_TEMPERATURE} REAL,
                    ${WeatherAggregatedDataTable.COLUMN_MINIMUM_TEMPERATURE} REAL,
                    ${WeatherAggregatedDataTable.COLUMN_MAXIMUM_TEMPERATURE} REAL,
                    ${WeatherAggregatedDataTable.COLUMN_WEATHER_CONDITION_FREQUENCY_MAP} TEXT,
                    ${WeatherAggregatedDataTable.COLUMN_AVERAGE_TEMPERATURE_FEEL_LIKE} REAL,
                    ${WeatherAggregatedDataTable.COLUMN_MINIMUM_TEMPERATURE_FEEL_LIKE} REAL,
                    ${WeatherAggregatedDataTable.COLUMN_MAXIMUM_TEMPERATURE_FEEL_LIKE} REAL,
                    ${WeatherAggregatedDataTable.COLUMN_YEAR} INTEGER,
                    ${WeatherAggregatedDataTable.COLUMN_MONTH} INTEGER,
                    ${WeatherAggregatedDataTable.COLUMN_DAY} INTEGER,
                    ${WeatherAggregatedDataTable.COLUMN_RECORDS_IN_A_DAY} INTEGER,
                    PRIMARY KEY (${WeatherDataTable.COLUMN_TIMESTAMP}, ${WeatherDataTable.COLUMN_CITY})
                );
            """
    }
}