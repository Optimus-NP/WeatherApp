package com.example.weatherapp.ui.current

import SpaceItemDecoration
import android.database.Cursor
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.constants.City
import com.example.weatherapp.constants.Settings
import com.example.weatherapp.constants.TemperatureUnit
import com.example.weatherapp.database.WeatherDataContentProvider
import com.example.weatherapp.database.WeatherDatabaseHelper
import com.example.weatherapp.databinding.FragmentCitiesWeatherBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fragment that demonstrates a responsive layout pattern where the format of the content
 * transforms depending on the size of the screen. Specifically this Fragment shows items in
 * the [RecyclerView] using LinearLayoutManager in a small screen
 * and shows items using GridLayoutManager in a large screen.
 */
class CurrentWeatherFragment : Fragment() {
    private var _binding: FragmentCitiesWeatherBinding? = null

    private var temperatureUnit: TemperatureUnit =
        TemperatureUnit.fromSymbol(Settings.TEMPERATURE_UNIT.defaultValue)!!

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var dbHelper: WeatherDatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCitiesWeatherBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val recyclerView = binding.recyclerviewTransform
        val spacingInPixels =
            resources.getDimensionPixelSize(R.dimen.item_spacing) // Get from dimens.xml
        recyclerView.addItemDecoration(SpaceItemDecoration(spacingInPixels))
        dbHelper = WeatherDatabaseHelper(context)
        temperatureUnit = getTemperatureSetting()
        fetchWeatherData(recyclerView)
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun openTrendsFragment(city: String) {
        val navController = findNavController()
        val action = CurrentWeatherFragmentDirections.actionNavTransformToNavTrends(city)
        navController.navigate(action)
    }

    /*fun openTrendsFragment(data: String) {
        val trendsFragment = TrendsFragment()
        val bundle = Bundle()
        bundle.putString("City", data) // Pass the data
        trendsFragment.arguments = bundle

        val parentFragmentManager = getParentFragmentManager()

        val existingFragment =
            parentFragmentManager.findFragmentById(R.id.activity_container)
        if (existingFragment != null) {
            Log.i(TAG, "Existing fragment: ${existingFragment.id} removed.");
            parentFragmentManager.beginTransaction().remove(existingFragment)
                .commit()
        }

        var fragmentTransaction = parentFragmentManager.beginTransaction()
        fragmentTransaction = fragmentTransaction.replace(R.id.activity_container, trendsFragment)
        fragmentTransaction = fragmentTransaction.addToBackStack(null) // Add to back stack to handle back navigation
        val committed = fragmentTransaction.commit()
        Log.i(TAG, "The fragment was Committed: $committed")
    }*/

    private fun fetchWeatherData(recyclerView: RecyclerView) {
        val db = dbHelper.readableDatabase
        val query = """
            SELECT ${WeatherDatabaseHelper.WeatherDataTable.COLUMN_CITY}, 
                MAX(${WeatherDatabaseHelper.WeatherDataTable.COLUMN_TIMESTAMP}) as ${WeatherDatabaseHelper.WeatherDataTable.COLUMN_TIMESTAMP}, 
                ${WeatherDatabaseHelper.WeatherDataTable.COLUMN_TEMPERATURE},
                ${WeatherDatabaseHelper.WeatherDataTable.COLUMN_WEATHER_CONDITION},
                ${WeatherDatabaseHelper.WeatherDataTable.COLUMN_TEMPERATURE_FEEL_LIKE}
            FROM ${WeatherDatabaseHelper.WeatherDataTable.WEATHER_TABLE}
            GROUP BY city
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        cursor.setNotificationUri(
            requireActivity().contentResolver,
            WeatherDataContentProvider.WEATHER_CONTENT_URI
        )

        if (cursor != null) {
            val weatherAdapter = WeatherAdapter(cursor, temperatureUnit)
            recyclerView.adapter = weatherAdapter
        }
    }

    inner class WeatherAdapter(
        private val cursor: Cursor,
        private val temperatureUnit: TemperatureUnit,

        ) : RecyclerView.Adapter<WeatherAdapter.WeatherViewHolder>() {
        private val city_drawables = mapOf(
            City.DELHI to R.mipmap.delhi,
            City.LONDON to R.mipmap.london,
            City.CHENNAI to R.mipmap.chennai,
            City.MUMBAI to R.mipmap.mumbai,
            City.BENGALURU to R.mipmap.bengaluru,
            City.LOS_ANGELES to R.mipmap.losangeles,
            City.TOKYO to R.mipmap.tokyo,
            City.NEW_YORK to R.mipmap.newyork,
            City.KOLKATA to R.mipmap.kolkata
        )

        inner class WeatherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val cardView: View = itemView
            val cityImageView: ImageView = itemView.findViewById(R.id.image_view_item_transform)
            val cityTextView: TextView = itemView.findViewById(R.id.tvCity)
            val temperatureTextView: TextView = itemView.findViewById(R.id.tvTemperature)
            val temperatureFeelLikeTextView: TextView =
                itemView.findViewById(R.id.tvTemperatureFeelLike)
            val weatherConditionTextView: TextView = itemView.findViewById(R.id.tvWeatherCondition)
            val timeTextView: TextView = itemView.findViewById(R.id.tvTime)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeatherViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.city_weather_card, parent, false)

            return WeatherViewHolder(view)
        }

        override fun onBindViewHolder(holder: WeatherViewHolder, position: Int) {
            if (cursor.moveToPosition(position)) {
                val city =
                    cursor.getString(cursor.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherDataTable.COLUMN_CITY))
                val temperature = TemperatureUnit.KELVIN.convert(
                    cursor.getDouble(
                        cursor.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherDataTable.COLUMN_TEMPERATURE)
                    ), temperatureUnit
                )
                val temperatureFeelLike = TemperatureUnit.KELVIN.convert(
                    cursor.getDouble(
                        cursor.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherDataTable.COLUMN_TEMPERATURE_FEEL_LIKE)
                    ), temperatureUnit
                )
                val weatherCondition =
                    cursor.getString(cursor.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherDataTable.COLUMN_WEATHER_CONDITION))
                val timestamp =
                    cursor.getLong(cursor.getColumnIndexOrThrow(WeatherDatabaseHelper.WeatherDataTable.COLUMN_TIMESTAMP))

                holder.cityImageView.setImageDrawable(
                    city_drawables.get(City.fromCityName(city))?.let {
                        ResourcesCompat.getDrawable(
                            holder.cityImageView.resources,
                            it, null
                        )
                    }
                )

                val citySpannableString = SpannableString("City: $city")
                citySpannableString.setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    4,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                holder.cityTextView.text = citySpannableString

                val temperatureSpannableString =
                    SpannableString("Temperature: ${"%.2f".format(temperature)} ${temperatureUnit.symbol}")
                temperatureSpannableString.setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    12,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                ) // "Temperature" bold
                holder.temperatureTextView.text = temperatureSpannableString

                val temperatureFeelLikeSpannableString =
                    SpannableString("Temperature Feel Like: ${"%.2f".format(temperatureFeelLike)} ${temperatureUnit.symbol}")
                temperatureFeelLikeSpannableString.setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    21,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                ) // "Temperature Feel Like" bold
                holder.temperatureFeelLikeTextView.text = temperatureFeelLikeSpannableString

                val weatherConditionSpannableString =
                    SpannableString("Weather Condition: $weatherCondition")
                weatherConditionSpannableString.setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    17,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                ) // "Weather Condition" bold
                holder.weatherConditionTextView.text = weatherConditionSpannableString

                val timeSpannableString =
                    SpannableString("Time: ${convertTimestampToDate(timestamp)}")
                timeSpannableString.setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    5,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                ) // "Time:" bold
                holder.timeTextView.text = timeSpannableString

                holder.cardView.setOnClickListener {
                    Log.i(TAG, "Configured the $city");
                    this@CurrentWeatherFragment.openTrendsFragment(city)
                }
            }
        }

        override fun getItemCount(): Int {
            return cursor.count
        }

        private fun convertTimestampToDate(timestamp: Long): String {
            val date = Date(timestamp * 1000L)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            return sdf.format(date)
        }

    }

    private fun getTemperatureSetting(): TemperatureUnit {
        // Query the Content Provider for the notification setting
        val projection = arrayOf(WeatherDatabaseHelper.UserSettingsTable.COLUMN_SETTING_VALUE)
        val selection = "${WeatherDatabaseHelper.UserSettingsTable.COLUMN_SETTING_KEY} = ?"
        val selectionArgs = arrayOf(Settings.TEMPERATURE_UNIT.settingKey)

        val cursor = requireActivity().contentResolver.query(
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

                return TemperatureUnit.fromName(value)!!
            }
        }
        return TemperatureUnit.KELVIN
    }

    companion object {
        private val TAG = CurrentWeatherFragment::class.simpleName
    }
}