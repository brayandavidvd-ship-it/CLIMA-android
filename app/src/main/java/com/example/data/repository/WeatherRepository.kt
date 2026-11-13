package com.example.data.repository

import com.example.data.local.ClimaPreferencesDataStore
import com.example.data.local.LocationHelper
import com.example.model.CitySearchResult
import com.example.model.SavedLocation
import com.example.model.WeatherProviderOption
import com.example.model.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

sealed interface WeatherFetchResult {
    data class Success(
        val snapshot: WeatherSnapshot,
        val fromCache: Boolean,
        val isOfflineFallback: Boolean,
    ) : WeatherFetchResult

    data class NeedsLocation(val message: String?) : WeatherFetchResult
    data class Error(val message: String) : WeatherFetchResult
}

class WeatherRepository(
    private val dataSources: Map<WeatherProviderOption, WeatherDataSource>,
    private val prefs: ClimaPreferencesDataStore,
    private val locationHelper: LocationHelper,
) {
    companion object {
        const val CACHE_TTL_MS = 20L * 60L * 1000L
    }

    private fun resolveDataSource(provider: WeatherProviderOption): WeatherDataSource =
        dataSources[provider] ?: dataSources.getValue(WeatherProviderOption.OPEN_METEO)

    suspend fun getCachedWeather(): WeatherSnapshot? = prefs.getCachedWeather()

    suspend fun getSavedLocation(): SavedLocation? = prefs.getSavedLocation()

    suspend fun loadWeather(
        forceRefresh: Boolean = false,
        useAutoLocationIfAvailable: Boolean = true,
        providerOverride: WeatherProviderOption? = null,
    ): WeatherFetchResult = withContext(Dispatchers.IO) {
        val activeProvider = providerOverride ?: prefs.settingsFlow.first().selectedWeatherProvider
        val dataSource = resolveDataSource(activeProvider)
        val cachedWeather = prefs.getCachedWeather()
        val savedLocation = prefs.getSavedLocation()

        val targetLocation: SavedLocation? = when {
            savedLocation != null && !savedLocation.isAutoLocation -> savedLocation
            useAutoLocationIfAvailable && locationHelper.hasCoarseLocationPermission() -> {
                val resolved = locationHelper.getCurrentCoordinatesAndCity()
                if (resolved != null) {
                    SavedLocation(
                        cityName = resolved.cityName,
                        latitude = resolved.latitude,
                        longitude = resolved.longitude,
                        isAutoLocation = true,
                    ).also { prefs.saveLocation(it) }
                } else {
                    savedLocation
                }
            }
            else -> savedLocation
        }

        if (targetLocation == null) {
            return@withContext WeatherFetchResult.NeedsLocation(null)
        }

        val now = System.currentTimeMillis()
        val isCacheFresh = cachedWeather != null &&
            cachedWeather.providerId == activeProvider.id &&
            cachedWeather.cityName == targetLocation.cityName &&
            (now - cachedWeather.updatedAtMillis) < CACHE_TTL_MS

        if (!forceRefresh && isCacheFresh && cachedWeather != null) {
            return@withContext WeatherFetchResult.Success(
                snapshot = cachedWeather,
                fromCache = true,
                isOfflineFallback = false,
            )
        }

        return@withContext runCatching {
            val snapshot = dataSource.fetchCurrentWeather(
                latitude = targetLocation.latitude,
                longitude = targetLocation.longitude,
                cityName = targetLocation.cityName,
            )
            prefs.saveCachedWeather(snapshot)
            WeatherFetchResult.Success(
                snapshot = snapshot,
                fromCache = false,
                isOfflineFallback = false,
            )
        }.getOrElse { error ->
            if (cachedWeather != null) {
                WeatherFetchResult.Success(
                    snapshot = cachedWeather,
                    fromCache = true,
                    isOfflineFallback = true,
                )
            } else {
                WeatherFetchResult.Error(
                    error.localizedMessage ?: "No pudimos consultar el clima en este momento.",
                )
            }
        }
    }

    suspend fun selectManualCity(city: CitySearchResult): WeatherFetchResult {
        val location = SavedLocation(
            cityName = city.name,
            latitude = city.latitude,
            longitude = city.longitude,
            isAutoLocation = false,
        )
        prefs.saveLocation(location)
        return loadWeather(forceRefresh = true, useAutoLocationIfAvailable = false)
    }

    suspend fun switchToAutoLocation(): WeatherFetchResult {
        val resolved = locationHelper.getCurrentCoordinatesAndCity()
            ?: return WeatherFetchResult.NeedsLocation("No se pudo obtener tu ubicación actual.")
        val location = SavedLocation(
            cityName = resolved.cityName,
            latitude = resolved.latitude,
            longitude = resolved.longitude,
            isAutoLocation = true,
        )
        prefs.saveLocation(location)
        return loadWeather(forceRefresh = true, useAutoLocationIfAvailable = true)
    }

    suspend fun searchCities(query: String): Result<List<CitySearchResult>> =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.length < 2) return@withContext Result.success(emptyList())
            val activeProvider = prefs.settingsFlow.first().selectedWeatherProvider
            runCatching {
                resolveDataSource(activeProvider).searchCities(trimmed)
            }
        }
}
