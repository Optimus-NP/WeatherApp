package com.example.weatherapp.constants

enum class TemperatureUnit(val symbol: String) {
    KELVIN("K"),
    CELSIUS("°C"),
    FAHRENHEIT("°F");

    fun toKelvin(value: Double): Double {
        return convert(value, KELVIN)
    }

    fun toKelvin(value: Float): Float {
        return convert(value, KELVIN)
    }

    fun toCelsius(value: Double): Double {
        return convert(value, CELSIUS)
    }

    fun toCelsius(value: Float): Float {
        return convert(value, CELSIUS)
    }

    fun toFahrenheit(value: Double): Double {
        return convert(value, FAHRENHEIT)
    }

    fun toFahrenheit(value: Float): Float {
        return convert(value, FAHRENHEIT)
    }

    fun convert(value: Float, to: TemperatureUnit): Float {
        return when (this) {
            KELVIN -> when (to) {
                CELSIUS -> value - 273.15F
                FAHRENHEIT -> (value - 273.15F) * 9 / 5 + 32
                KELVIN -> value
            }
            CELSIUS -> when (to) {
                KELVIN -> value + 273.15F
                FAHRENHEIT -> (value * 9 / 5) + 32
                CELSIUS -> value
            }
            FAHRENHEIT -> when (to) {
                KELVIN -> (value - 32) * 5 / 9 + 273.15F
                CELSIUS -> (value - 32) * 5 / 9
                FAHRENHEIT -> value
            }
        }
    }

    fun convert(value: Double, to: TemperatureUnit): Double {
        return when (this) {
            KELVIN -> when (to) {
                CELSIUS -> value - 273.15
                FAHRENHEIT -> (value - 273.15) * 9 / 5 + 32
                KELVIN -> value
            }
            CELSIUS -> when (to) {
                KELVIN -> value + 273.15
                FAHRENHEIT -> (value * 9 / 5) + 32
                CELSIUS -> value
            }
            FAHRENHEIT -> when (to) {
                KELVIN -> (value - 32) * 5 / 9 + 273.15
                CELSIUS -> (value - 32) * 5 / 9
                FAHRENHEIT -> value
            }
        }
    }

    companion object {
        fun fromName(name: String): TemperatureUnit? {
            return TemperatureUnit.entries.find { it.name.equals(name, ignoreCase = true) }
        }

        fun fromSymbol(name: String): TemperatureUnit? {
            return TemperatureUnit.entries.find { it.symbol.equals(name, ignoreCase = true) }
        }
    }
}