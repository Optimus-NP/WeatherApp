package com.example.weatherapp.database

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.example.weatherapp.database.WeatherDatabaseHelper.UserSettingsTable.Companion.SETTINGS_TABLE
import com.example.weatherapp.database.WeatherDatabaseHelper.WeatherAggregatedDataTable.Companion.AGGREGATED_WEATHER_TABLE
import com.example.weatherapp.database.WeatherDatabaseHelper.WeatherDataTable.Companion.WEATHER_TABLE

class WeatherDataContentProvider : ContentProvider() {
    companion object {
        private val TAG = WeatherDataContentProvider::class.simpleName
        private const val AUTHORITY = "com.example.weatherapp.provider"
        val WEATHER_CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$WEATHER_TABLE")
        val AGGREGATED_WEATHER_CONTENT_URI: Uri =
            Uri.parse("content://$AUTHORITY/$AGGREGATED_WEATHER_TABLE")
        val SETTING_CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$SETTINGS_TABLE")

        private const val TABLE_WEATHER = 1
        private const val TABLE_AGGREGATED_WEATHER = 2
        private const val TABLE_SETTING = 3

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, WEATHER_TABLE, TABLE_WEATHER)
            addURI(AUTHORITY, AGGREGATED_WEATHER_TABLE, TABLE_AGGREGATED_WEATHER)
            addURI(AUTHORITY, SETTINGS_TABLE, TABLE_SETTING)
        }
    }

    private lateinit var dbHelper: WeatherDatabaseHelper

    override fun onCreate(): Boolean {
        Log.i(TAG, "onCreate was called.")
        // Ensure the context is not null
        context?.let {
            dbHelper = WeatherDatabaseHelper(it)
            Log.i(TAG, "Context present, db initialized.")
        } ?: run {
            Log.e(TAG, "Context is null, cannot initialize dbHelper")
            return false // Prevents the Content Provider from being created if the context is null
        }
        return true
    }

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? {
        val db = dbHelper.readableDatabase
        return db.query(getTable(uri), projection, selection, selectionArgs, null, null, sortOrder)
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (!::dbHelper.isInitialized) {
            Log.e(TAG, "dbHelper not initialized")
            return null
        }
        var id: Long = 0
        val db = dbHelper.writableDatabase

        val table = getTable(uri)

        when (table) {

            SETTINGS_TABLE -> {
                val settingsKey = values?.getAsString("setting_key")

                if (settingsKey != null) {
                    val exists = doesSettingExist(uri, settingsKey)

                    if (exists) {
                        val selection = "setting_key = ?"
                        val selectionArgs = arrayOf(settingsKey)
                        val records = db.update(table, values, selection, selectionArgs)
                        Log.i(TAG, "Update Succeeded for ID: $id, uri: $uri, records: $records")
                    }  else {
                        // Update failed, insert new record
                        id = db.insert(getTable(uri), null, values)
                        Log.i(TAG, "Insert Succeeded for ID: $id, uri: $uri")
                    }
                } else {
                    // If ID is null, insert a new record
                    id = db.insert(getTable(uri), null, values)
                    Log.i(TAG, "Insert Succeeded for ID: $id, uri: $uri")
                }
            }

            else -> {
                val timestamp = values?.getAsLong("timestamp")
                val city = values?.getAsString("city")

                if (timestamp != null && city != null) {
                    // Try to update first
                    val exists = doesRecordExist(uri, timestamp, city)
                    if (exists) {
                        // Update successful, return the updated URI
                        // Define the selection criteria
                        val selection = "timestamp = ? and city = ?"
                        // Define the selection arguments (the value for the ? in the selection string)
                        val selectionArgs = arrayOf(timestamp.toString(), city)
                        val records = db.update(table, values, selection, selectionArgs)
                        Log.i(TAG, "Update Succeeded for ID: $id, uri: $uri, records: $records")
                    } else {
                        // Update failed, insert new record
                        id = db.insert(getTable(uri), null, values)
                        Log.i(TAG, "Insert Succeeded for ID: $id, uri: $uri")
                    }
                } else {
                    // If ID is null, insert a new record
                    id = db.insert(getTable(uri), null, values)
                    Log.i(TAG, "Insert Succeeded for ID: $id, uri: $uri")
                }
            }
        }

        context?.contentResolver?.notifyChange(uri, null)
        return ContentUris.withAppendedId(uri, id)
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val db = dbHelper.writableDatabase
        val count = db.delete(getTable(uri), selection, selectionArgs)
        context?.contentResolver?.notifyChange(uri, null)
        return count
    }

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        val db = dbHelper.writableDatabase
        val count = db.update(getTable(uri), values, selection, selectionArgs)
        context?.contentResolver?.notifyChange(uri, null)
        return count
    }

    override fun getType(uri: Uri): String {
        return "vnd.android.cursor.dir/vnd.com.example.weatherapp.${getTable(uri)}"
    }

    private fun getTable(uri: Uri): String {
        return when (uriMatcher.match(uri)) {
            TABLE_WEATHER -> WEATHER_TABLE
            TABLE_AGGREGATED_WEATHER -> AGGREGATED_WEATHER_TABLE
            TABLE_SETTING -> SETTINGS_TABLE
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    private fun doesSettingExist(contentUri: Uri, settingsKey: String): Boolean {
        val selection = "setting_key = ?"
        val selectionArgs = arrayOf(settingsKey)

        return doesExist(contentUri, arrayOf("setting_key"), selection, selectionArgs)
    }

    private fun doesRecordExist(contentUri: Uri, timestamp: Long, city: String): Boolean {
        // Define the selection criteria
        val selection = "timestamp = ? and city = ?"

        // Define the selection arguments (the value for the ? in the selection string)
        val selectionArgs = arrayOf(timestamp.toString(), city)

        return doesExist(contentUri, arrayOf("timestamp"), selection, selectionArgs)
    }

    private fun doesExist(uri: Uri, projection: Array<String>?, selection: String?,
                          selectionArgs: Array<String>?): Boolean {
        val cursor = query(
            uri,           // The URI of the content provider
            projection,           // The columns to return
            selection,            // The selection criteria (WHERE clause)
            selectionArgs,        // The selection arguments (for WHERE clause)
            null                  // No need for sorting
        )

        // Check if cursor contains any records
        val recordExists = cursor?.use {
            it.count > 0
        } ?: false

        return recordExists
    }
}