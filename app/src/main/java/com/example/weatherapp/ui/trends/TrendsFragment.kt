package com.example.weatherapp.ui.trends

import android.content.ContentResolver
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import com.example.weatherapp.R
import com.example.weatherapp.constants.City
import com.example.weatherapp.constants.Settings
import com.example.weatherapp.constants.TemperatureUnit
import com.example.weatherapp.database.WeatherDataContentProvider
import com.example.weatherapp.database.WeatherDatabaseHelper
import com.example.weatherapp.databinding.FragmentTrendsBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.gson.Gson

class TrendsFragment : Fragment() {
    private var _binding: FragmentTrendsBinding? = null
    private val binding get() = _binding!!
    private val gson: Gson = Gson()

    private var temperatureUnit: TemperatureUnit =
        TemperatureUnit.fromSymbol(Settings.TEMPERATURE_UNIT.defaultValue)!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrendsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Retrieve the data
        val city = arguments?.getString("city") ?: City.BENGALURU.cityName
        Toast.makeText(requireContext(), "Showing the data for $city", Toast.LENGTH_LONG).show()

        // Fetch data
        temperatureUnit = getTemperatureSetting(requireContext().contentResolver)
        val temperatureList = fetchWeatherData(city)
        viewTemperatures(temperatureList, root)

        setupBarChart(fetchWeatherConditionsFreq(city), root)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getTemperatureSetting(contentResolver: ContentResolver?): TemperatureUnit {
        val projection = arrayOf(WeatherDatabaseHelper.UserSettingsTable.COLUMN_SETTING_VALUE)
        val selection = "${WeatherDatabaseHelper.UserSettingsTable.COLUMN_SETTING_KEY} = ?"
        val selectionArgs = arrayOf(Settings.TEMPERATURE_UNIT.settingKey)

        val cursor = contentResolver?.query(
            WeatherDataContentProvider.SETTING_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val value =
                    it.getString(it.getColumnIndexOrThrow(WeatherDatabaseHelper.UserSettingsTable.COLUMN_SETTING_VALUE))
                Log.i(TAG, "The Temperature Unit is set as $value")
                return TemperatureUnit.fromName(value)!!
            }
        }

        return TemperatureUnit.KELVIN
    }

    inner class WeatherConditionsFreq(
        val date: String,
        val weatherConditionsFreq: MutableMap<String, Int>
    ) {
        override fun toString(): String {
            return "WeatherConditionsFreq(date='$date', weatherConditionsFreq=$weatherConditionsFreq)"
        }
    }

    private fun fetchWeatherConditionsFreq(city: String): List<WeatherConditionsFreq> {
        val projection = arrayOf(
            WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_WEATHER_CONDITION_FREQUENCY_MAP,
            WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_YEAR,
            WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_MONTH,
            WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_DAY
        )

        val selection = "${WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_CITY} = ?"
        val selectionArgs = arrayOf(city)

        val dailyWeatherConditionFrequencies = mutableListOf<WeatherConditionsFreq>()

        val cursor = requireContext().contentResolver.query(
            WeatherDataContentProvider.AGGREGATED_WEATHER_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "year ASC, month ASC, day ASC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val year =
                    it.getString(it.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_YEAR))
                val month =
                    it.getString(it.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_MONTH))
                val day =
                    it.getString(it.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_DAY))
                val date = "$year/$month/$day"

                val weatherConditionsFreqAsStr =
                    it.getString(it.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_WEATHER_CONDITION_FREQUENCY_MAP))

                dailyWeatherConditionFrequencies.add(
                    WeatherConditionsFreq(
                        date,
                        gson.fromJson(
                            weatherConditionsFreqAsStr,
                            Map::class.java
                        ) as MutableMap<String, Int>
                    )
                )

            }
        }

        Log.i(
            TAG,
            "Weather Conditions Frequencies found from database is $dailyWeatherConditionFrequencies"
        )

        return dailyWeatherConditionFrequencies
    }

    inner class TemperatureData(
        val date: String,
        val minTemp: Float,
        val maxTemp: Float,
        val avgTemp: Float,
        val minTempFeelLike: Float,
        val maxTempFeelLike: Float,
        val avgTempFeelLike: Float
    )

    private fun fetchWeatherData(city: String): List<TemperatureData> {
        val projection = arrayOf(
            WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_MINIMUM_TEMPERATURE,
            WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_MAXIMUM_TEMPERATURE,
            WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_AVERAGE_TEMPERATURE,
            WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_MINIMUM_TEMPERATURE_FEEL_LIKE,
            WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_MAXIMUM_TEMPERATURE_FEEL_LIKE,
            WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_AVERAGE_TEMPERATURE_FEEL_LIKE,
            WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_YEAR,
            WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_MONTH,
            WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_DAY
        )

        val selection = "${WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_CITY} = ?"
        val selectionArgs = arrayOf(city)

        val temperatureList = mutableListOf<TemperatureData>()

        val cursor = requireContext().contentResolver.query(
            WeatherDataContentProvider.AGGREGATED_WEATHER_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "year ASC, month ASC, day ASC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val year =
                    it.getString(it.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_YEAR))
                val month =
                    it.getString(it.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_MONTH))
                val day =
                    it.getString(it.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_DAY))
                val date = "$year/$month/$day"

                val minTemp =
                    it.getFloat(it.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_MINIMUM_TEMPERATURE))
                val avgTemp =
                    it.getFloat(it.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_AVERAGE_TEMPERATURE))
                val maxTemp =
                    it.getFloat(it.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_MAXIMUM_TEMPERATURE))

                val minTempFeelLike =
                    it.getFloat(it.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_MINIMUM_TEMPERATURE_FEEL_LIKE))
                val avgTempFeelLike =
                    it.getFloat(it.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_AVERAGE_TEMPERATURE_FEEL_LIKE))
                val maxTempFeelLike =
                    it.getFloat(it.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherAggregatedDataTable.COLUMN_MAXIMUM_TEMPERATURE_FEEL_LIKE))

                temperatureList.add(
                    TemperatureData(
                        date,
                        TemperatureUnit.KELVIN.convert(minTemp, temperatureUnit),
                        TemperatureUnit.KELVIN.convert(maxTemp, temperatureUnit),
                        TemperatureUnit.KELVIN.convert(avgTemp, temperatureUnit),
                        TemperatureUnit.KELVIN.convert(minTempFeelLike, temperatureUnit),
                        TemperatureUnit.KELVIN.convert(maxTempFeelLike, temperatureUnit),
                        TemperatureUnit.KELVIN.convert(avgTempFeelLike, temperatureUnit)
                    )
                )
            }
        }

        Log.i(TAG, "Temperature List found from database is $temperatureList")

        return temperatureList
    }

    private fun viewTemperatures(temperatureList: List<TemperatureData>, rootView: View) {
        val lineChart = rootView.findViewById<LineChart>(R.id.lineChartTemperature)

        val darkBlue = ContextCompat.getColor(requireContext(), R.color.dark_blue)
        val lightBlue = ContextCompat.getColor(requireContext(), R.color.light_blue)
        val darkRed = ContextCompat.getColor(requireContext(), R.color.dark_red)
        val lightRed = ContextCompat.getColor(requireContext(), R.color.light_red)
        val darkGreen = ContextCompat.getColor(requireContext(), R.color.dark_green)
        val lightGreen = ContextCompat.getColor(requireContext(), R.color.light_green)

        // Prepare entries for LineChart
        val minEntries = mutableListOf<Entry>()
        val avgEntries = mutableListOf<Entry>()
        val maxEntries = mutableListOf<Entry>()

        val minFeelLikeEntries = mutableListOf<Entry>()
        val avgFeelLikeEntries = mutableListOf<Entry>()
        val maxFeelLikeEntries = mutableListOf<Entry>()

        temperatureList.forEachIndexed { index, data ->
            minEntries.add(Entry(index.toFloat(), data.minTemp))
            avgEntries.add(Entry(index.toFloat(), data.avgTemp))
            maxEntries.add(Entry(index.toFloat(), data.maxTemp))

            minFeelLikeEntries.add(Entry(index.toFloat(), data.minTempFeelLike))
            avgFeelLikeEntries.add(Entry(index.toFloat(), data.avgTempFeelLike))
            maxFeelLikeEntries.add(Entry(index.toFloat(), data.maxTempFeelLike))
        }

        // Create LineDataSet for each type of temperature
        val minDataSet = LineDataSet(minEntries, "Min").apply {
            color = darkBlue
            setCircleColor(ColorUtils.setAlphaComponent(darkBlue, 200))
            lineWidth = 2f
        }

        val minFeelLikeDataSet = LineDataSet(minFeelLikeEntries, "Min Feel Like").apply {
            color = lightBlue
            setCircleColor(lightBlue)
            lineWidth = 2f
        }

        val avgDataSet = LineDataSet(avgEntries, "Avg").apply {
            color = darkGreen
            setCircleColor(ColorUtils.setAlphaComponent(darkGreen, 200))
            lineWidth = 2f
        }

        val avgFeelLikeDataSet = LineDataSet(avgFeelLikeEntries, "Avg Feel Like").apply {
            color = lightGreen
            setCircleColor(lightGreen)
            lineWidth = 2f
        }

        val maxDataSet = LineDataSet(maxEntries, "Max").apply {
            color = darkRed
            setCircleColor(ColorUtils.setAlphaComponent(darkRed, 200))
            lineWidth = 2f
        }

        val maxFeelLikeDataSet = LineDataSet(maxFeelLikeEntries, "Max Feel Like").apply {
            color = lightRed
            setCircleColor(lightRed)
            lineWidth = 2f
        }

        val lineData = LineData(
            minDataSet,
            avgDataSet,
            maxDataSet,
            minFeelLikeDataSet,
            avgFeelLikeDataSet,
            maxFeelLikeDataSet
        )
        lineChart.data = lineData

        lineChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = IndexAxisValueFormatter(temperatureList.map { it.date })
            labelRotationAngle = 45f
            setDrawGridLines(false)
        }
        lineChart.axisLeft.apply { setDrawGridLines(false) }
        lineChart.axisRight.isEnabled = false

        lineChart.invalidate() // Refresh the chart

        lineChart.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            // Configure the description
            val description = lineChart.description
            description.isEnabled = true
            description.text =
                "Daily Temperature Overview (Temperature Unit: ${temperatureUnit.symbol})"
            description.textSize = 12f
            description.textColor = ContextCompat.getColor(requireContext(), R.color.fancyPurple)

            val paint = Paint()
            paint.textSize = description.textSize
            val textWidth = paint.measureText(description.text)

            val chartWidth = lineChart.width
            val xPosition = (chartWidth / 2f) + (textWidth / 2f)
            val yPosition = 30f

            description.setPosition(xPosition, yPosition)

            // Refresh the chart
            lineChart.invalidate()
        }
    }

    private fun setupBarChart(
        weatherConditionsFrequencies: List<WeatherConditionsFreq>,
        rootView: View
    ) {
        val barChart = rootView.findViewById<BarChart>(R.id.barChart)

        val allUniqueConditions = mutableSetOf<String>()
        weatherConditionsFrequencies.forEach { weatherConditionsFreq ->
            allUniqueConditions.addAll(weatherConditionsFreq.weatherConditionsFreq.keys)
        }

        val sortedUniqueConditionsMap = allUniqueConditions.mapIndexed { index, condition ->
            index to condition
        }.toMap().toSortedMap()

        val groupsSet = mutableListOf<BarDataSet>()
        sortedUniqueConditionsMap.forEach { conditionIndex, condition ->
            val group = ArrayList<BarEntry>()
            weatherConditionsFrequencies.forEachIndexed { index, weatherConditionsFreq ->
                run {
                    val conditionCount = weatherConditionsFreq.weatherConditionsFreq.getOrDefault(condition, 0).toFloat()
                    val indexAsFloat  = index.toFloat()
                    group.add(BarEntry(indexAsFloat, conditionCount))
                }
            }
            val barDataSet = BarDataSet(group, condition).apply {
                color = ColorTemplate.COLORFUL_COLORS[conditionIndex]
            }
            groupsSet.add(barDataSet)
        }


        val data = BarData(groupsSet as List<IBarDataSet>?)

        val groupSpace = 0.08f // Space between groups
        val barSpace = 0.02f   // Space between individual bars in a group
        val barWidth = 0.4f    // Width of each bar

        data.barWidth = barWidth
        barChart.data = data

        barChart.barData.groupBars(0f, groupSpace, barSpace)

        barChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = IndexAxisValueFormatter(weatherConditionsFrequencies.map { it.date })
            labelRotationAngle = 45f
            setDrawGridLines(false)
        }

        // Customize the chart (optional)
        barChart.description.isEnabled = false
        barChart.xAxis.setDrawGridLines(false)
        barChart.axisLeft.setDrawGridLines(false)
        barChart.axisRight.isEnabled = false
        barChart.legend.isEnabled = true

        barChart.invalidate()

        barChart.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            // Configure the description
            val description = barChart.description
            description.isEnabled = true
            description.text =
                "Daily Weather Conditions Overview"
            description.textSize = 12f
            description.textColor = ContextCompat.getColor(requireContext(), R.color.fancyPurple)

            val paint = Paint()
            paint.textSize = description.textSize
            val textWidth = paint.measureText(description.text)

            val chartWidth = barChart.width
            val xPosition = (chartWidth / 2f) + (textWidth / 2f)
            val yPosition = 30f

            description.setPosition(xPosition, yPosition)

            // Refresh the chart
            barChart.invalidate()
        }
    }

    companion object {
        private val TAG = TrendsFragment::class.simpleName
    }
}
