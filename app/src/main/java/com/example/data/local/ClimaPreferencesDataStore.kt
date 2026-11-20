package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.SavedLocation
import com.example.model.WeatherSnapshot
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.climaDataStore by preferencesDataStore(name = "clima_v12_prefs")

class ClimaPreferencesDataStore(
    private val context: Context,
    private val moshi: Moshi,
) {
    private object Keys {
        val CACHED_WEATHER_JSON = stringPreferencesKey("cached_weather_json")
        val SAVED_LOCATION_JSON = stringPreferencesKey("saved_location_json")
        val IS_MANUAL_LOCATION = booleanPreferencesKey("is_manual_location")
        val REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")
    }

    private val weatherAdapter = moshi.adapter(WeatherSnapshot::class.java)
    private val locationAdapter = moshi.adapter(SavedLocation::class.java)

    val reduceAnimationsFlow: Flow<Boolean> = context.climaDataStore.data.map { prefs ->
        prefs[Keys.REDUCE_ANIMATIONS] ?: false
    }

    suspend fun setReduceAnimations(enabled: Boolean) {
        context.climaDataStore.edit { prefs ->
            prefs[Keys.REDUCE_ANIMATIONS] = enabled
        }
    }

    suspend fun getCachedWeather(): WeatherSnapshot? {
        val json = context.climaDataStore.data.first()[Keys.CACHED_WEATHER_JSON] ?: return null
        return runCatching { weatherAdapter.fromJson(json) }.getOrNull()
    }

    suspend fun saveCachedWeather(snapshot: WeatherSnapshot) {
        val json = weatherAdapter.toJson(snapshot)
        context.climaDataStore.edit { prefs ->
            prefs[Keys.CACHED_WEATHER_JSON] = json
        }
    }

    suspend fun getSavedLocation(): SavedLocation? {
        val json = context.climaDataStore.data.first()[Keys.SAVED_LOCATION_JSON] ?: return null
        return runCatching { locationAdapter.fromJson(json) }.getOrNull()
    }

    suspend fun isManualLocation(): Boolean {
        return context.climaDataStore.data.first()[Keys.IS_MANUAL_LOCATION] ?: false
    }

    suspend fun saveLocation(
        location: SavedLocation,
        isManual: Boolean = false,
    ) {
        val json = locationAdapter.toJson(location)
        context.climaDataStore.edit { prefs ->
            prefs[Keys.SAVED_LOCATION_JSON] = json
            prefs[Keys.IS_MANUAL_LOCATION] = isManual
        }
    }
}
