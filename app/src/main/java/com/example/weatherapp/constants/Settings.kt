package com.example.weatherapp.constants

enum class Settings (val settingKey: String, val defaultValue: String) {
    TEMPERATURE_UNIT("Temperature Unit", TemperatureUnit.KELVIN.symbol),
    NOTIFICATIONS_ENABLED("Notifications", false.toString())


}