package com.example.weatherapp.ui.settings

import android.content.ContentResolver
import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.example.weatherapp.R
import com.example.weatherapp.constants.Settings
import com.example.weatherapp.constants.TemperatureUnit
import com.example.weatherapp.database.WeatherDataContentProvider
import com.example.weatherapp.database.WeatherDatabaseHelper
import com.example.weatherapp.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    companion object {
        private val TAG = SettingsFragment::class.simpleName
    }

    private lateinit var notificationSwitch: Switch

    private lateinit var temperatureUnitSpinner: Spinner

    private var isNotificationEnabled: Boolean = Settings.NOTIFICATIONS_ENABLED.defaultValue.toBoolean()

    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        notificationSwitch = root.findViewById(R.id.notification_switch)

        loadNotificationSettings()

        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Save the setting when toggled
            saveNotificationSetting(isChecked)
        }

        temperatureUnitSpinner = root.findViewById(R.id.temperatureUnitSpinner)

        setupSpinner()

        return root
    }

    private fun setupSpinner() {
        // Get the temperature unit display names from the enum
        val units = TemperatureUnit.values().map { it.symbol }

        // Create an ArrayAdapter using the string array and a default spinner layout
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, units)

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Apply the adapter to the spinner
        temperatureUnitSpinner.adapter = adapter

        temperatureUnitSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                // Get the selected temperature unit
                val selectedUnit = TemperatureUnit.entries[position]

                val contentValues = ContentValues().apply {
                    put(WeatherDatabaseHelper.UserSettingsTable.COLUMN_SETTING_KEY, Settings.TEMPERATURE_UNIT.settingKey)
                    put(WeatherDatabaseHelper.UserSettingsTable.COLUMN_SETTING_VALUE, selectedUnit.name)
                }

                // Update the setting in the Content Provider
                requireActivity().contentResolver.insert(
                    WeatherDataContentProvider.SETTING_CONTENT_URI,
                    contentValues,
                )

                Log.i(TAG, "Updated the setting")
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Handle case where nothing is selected if necessary
            }
        })

        // Retrieve the existing setting
        val currentSetting: TemperatureUnit = getTemperatureSetting(requireContext().contentResolver)

        // Set the Spinner selection based on the existing setting
        val position = currentSetting.ordinal
        if (position >= 0) {
            temperatureUnitSpinner.setSelection(position) // Set the selected item
        }
    }

    private fun getTemperatureSetting(contentResolver: ContentResolver?): TemperatureUnit {
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
                val value = it.getString(it.getColumnIndexOrThrow(WeatherDatabaseHelper.UserSettingsTable.COLUMN_SETTING_VALUE))

                return TemperatureUnit.fromName(value)!!
            }
        }
        return TemperatureUnit.KELVIN
    }

    private fun saveNotificationSetting(checked: Boolean) {
        // Prepare content values to update the Content Provider
        val contentValues = ContentValues().apply {
            put(WeatherDatabaseHelper.UserSettingsTable.COLUMN_SETTING_KEY, Settings.NOTIFICATIONS_ENABLED.settingKey)
            put(WeatherDatabaseHelper.UserSettingsTable.COLUMN_SETTING_VALUE, checked.toString())
        }

        // Update the setting in the Content Provider
        requireActivity().contentResolver.insert(
            WeatherDataContentProvider.SETTING_CONTENT_URI,
            contentValues,
        )
    }

    private fun loadNotificationSettings() {
        // Query the Content Provider for the notification setting
        val projection = arrayOf(WeatherDatabaseHelper.UserSettingsTable.COLUMN_SETTING_VALUE)
        val selection = "${WeatherDatabaseHelper.UserSettingsTable.COLUMN_SETTING_KEY} = ?"
        val selectionArgs = arrayOf(Settings.NOTIFICATIONS_ENABLED.settingKey)

        val cursor = requireActivity().contentResolver.query(
            WeatherDataContentProvider.SETTING_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val value = it.getString(it.getColumnIndexOrThrow(WeatherDatabaseHelper.UserSettingsTable.COLUMN_SETTING_VALUE))
                isNotificationEnabled = value.toBoolean() // Convert to boolean
                notificationSwitch.isChecked = isNotificationEnabled
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}