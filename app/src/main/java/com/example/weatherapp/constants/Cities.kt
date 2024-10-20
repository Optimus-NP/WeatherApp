package com.example.weatherapp.constants

enum class City (val cityName: String, val countryCode: String, val cityId: String) {
    LONDON("London", "GB", "2643743"),
    NEW_YORK("New York", "US", "5128638"),
    LOS_ANGELES("Los Angeles", "US", "5368361"),
    TOKYO("Tokyo", "JP","1850147"),
    MUMBAI("Mumbai","IN","1275339"),
    DELHI("Delhi","IN","1273294"),
    BENGALURU("Bengaluru","IN","1277333"),
    CHENNAI("Chennai","IN","1264527"),
    KOLKATA("Kolkata","IN","1275004");

    override fun toString(): String {
        return "City(cityName='$cityName', countryCode='$countryCode', cityId='$cityId')"
    }

    companion object {
        fun fromCityName(cityName: String): City? {
            return entries.find { it.cityName.equals(cityName, ignoreCase = true) }
        }
    }
}