package com.example.data.repository

import com.example.data.local.ClimaPreferencesDataStore
import com.example.data.local.LocationHelper
import com.example.model.CitySearchResult
import com.example.model.SavedLocation
import com.example.model.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface WeatherFetchResult {
    data class Success(
        val snapshot: WeatherSnapshot,
        val isOfflineFallback: Boolean,
    ) : WeatherFetchResult

    data object NeedsLocationPermission : WeatherFetchResult
    data class Error(val message: String) : WeatherFetchResult
}

class WeatherRepository(
    private val weatherDataSource: WeatherDataSource,
    private val prefs: ClimaPreferencesDataStore,
    private val locationHelper: LocationHelper,
) {
    suspend fun getCachedWeather(): WeatherSnapshot? = prefs.getCachedWeather()

    suspend fun loadWeather(): WeatherFetchResult = withContext(Dispatchers.IO) {
        val cached = prefs.getCachedWeather()
        val hasPermission = locationHelper.hasCoarseLocationPermission()
        val isManual = prefs.isManualLocation()
        val savedLocation = prefs.getSavedLocation()

        val targetLocation: SavedLocation? = when {
            isManual && savedLocation != null -> savedLocation
            hasPermission -> {
                val resolved = locationHelper.getCurrentCoordinatesAndCity()
                if (resolved != null) {
                    SavedLocation(
                        cityName = resolved.cityName,
                        latitude = resolved.latitude,
                        longitude = resolved.longitude,
                    ).also { prefs.saveLocation(it, isManual = false) }
                } else {
                    savedLocation
                }
            }
            else -> savedLocation
        }

        if (targetLocation == null) {
            return@withContext if (!hasPermission) {
                WeatherFetchResult.NeedsLocationPermission
            } else {
                WeatherFetchResult.Error("No se pudo obtener tu ubicación actual.")
            }
        }

        return@withContext fetchForLocation(targetLocation, cached)
    }

    suspend fun switchToAutoLocation(): WeatherFetchResult = withContext(Dispatchers.IO) {
        val cached = prefs.getCachedWeather()
        if (!locationHelper.hasCoarseLocationPermission()) {
            return@withContext WeatherFetchResult.NeedsLocationPermission
        }
        val resolved = locationHelper.getCurrentCoordinatesAndCity()
            ?: return@withContext WeatherFetchResult.Error("No se pudo obtener tu ubicación actual.")
        val location = SavedLocation(
            cityName = resolved.cityName,
            latitude = resolved.latitude,
            longitude = resolved.longitude,
        )
        prefs.saveLocation(location, isManual = false)
        fetchForLocation(location, cached)
    }

    suspend fun selectManualCity(city: CitySearchResult): WeatherFetchResult = withContext(Dispatchers.IO) {
        val cached = prefs.getCachedWeather()
        val label = listOfNotNull(
            city.name,
            city.admin1?.takeIf { it.isNotBlank() && it != city.name },
        ).joinToString(", ")
        val location = SavedLocation(
            cityName = label,
            latitude = city.latitude,
            longitude = city.longitude,
        )
        prefs.saveLocation(location, isManual = true)
        fetchForLocation(location, cached)
    }

    suspend fun searchCities(query: String): Result<List<CitySearchResult>> = withContext(Dispatchers.IO) {
        runCatching {
            weatherDataSource.searchCities(query.trim())
        }
    }

    private suspend fun fetchForLocation(
        targetLocation: SavedLocation,
        cached: WeatherSnapshot?,
    ): WeatherFetchResult {
        return runCatching {
            val snapshot = weatherDataSource.fetchCurrentWeather(
                latitude = targetLocation.latitude,
                longitude = targetLocation.longitude,
                cityName = targetLocation.cityName,
            )
            prefs.saveCachedWeather(snapshot)
            WeatherFetchResult.Success(
                snapshot = snapshot,
                isOfflineFallback = false,
            )
        }.getOrElse { error ->
            if (cached != null) {
                WeatherFetchResult.Success(
                    snapshot = cached,
                    isOfflineFallback = true,
                )
            } else {
                WeatherFetchResult.Error(
                    error.localizedMessage ?: "No pudimos obtener el clima en este momento.",
                )
            }
        }
    }
}
