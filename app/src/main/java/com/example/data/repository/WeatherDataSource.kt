package com.example.data.repository

import com.example.model.CitySearchResult
import com.example.model.WeatherSnapshot

/**
 * Provider abstraction for fetching weather forecasts and searching cities.
 * Allows swapping Open-Meteo for another provider in the future without changing
 * the repository, ViewModel, or UI layers.
 */
interface WeatherDataSource {
    suspend fun fetchCurrentWeather(
        latitude: Double,
        longitude: Double,
        cityName: String,
    ): WeatherSnapshot

    suspend fun searchCities(query: String): List<CitySearchResult>
}
