package com.example.data.repository

import com.example.data.remote.OpenMeteoForecastApi
import com.example.data.remote.OpenMeteoGeocodingApi
import com.example.model.CitySearchResult
import com.example.model.DailyForecastItem
import com.example.model.HourlyForecastItem
import com.example.model.WeatherSnapshot

class OpenMeteoWeatherDataSource(
    private val forecastApi: OpenMeteoForecastApi,
    private val geocodingApi: OpenMeteoGeocodingApi,
) : WeatherDataSource {

    override suspend fun fetchCurrentWeather(
        latitude: Double,
        longitude: Double,
        cityName: String,
    ): WeatherSnapshot {
        val response = forecastApi.getForecast(
            latitude = latitude,
            longitude = longitude,
        )
        val current = response.current ?: error("Sin datos actuales del clima")
        val daily = response.daily
        val hourly = response.hourly
        val currentTime = current.time.orEmpty()
        val sunrise = daily?.sunrise?.firstOrNull().orEmpty()
        val sunset = daily?.sunset?.firstOrNull().orEmpty()
        val sunriseClock = sunrise.substringAfter("T", "").take(5)
        val sunsetClock = sunset.substringAfter("T", "").take(5)

        val hourlyTimes = hourly?.time.orEmpty()
        val hourlyTemps = hourly?.temperature2m.orEmpty()
        val hourlyProbs = hourly?.precipitationProbability.orEmpty()
        val hourlyCodes = hourly?.weatherCode.orEmpty()

        val currentHourPrefix = currentTime.take(13)
        val startIdx = hourlyTimes.indexOfFirst { it.take(13) >= currentHourPrefix }
            .takeIf { it >= 0 } ?: 0

        val hourlyItems = hourlyTimes.indices
            .drop(startIdx)
            .take(24)
            .map { i ->
                val timeStr = hourlyTimes[i]
                val clock = timeStr.substringAfter("T", "").take(5)
                val isHourDay = if (sunriseClock.length == 5 && sunsetClock.length == 5 && clock.length == 5) {
                    clock >= sunriseClock && clock < sunsetClock
                } else {
                    (current.isDay ?: 1) == 1
                }
                HourlyForecastItem(
                    timeIso = timeStr,
                    temperatureC = hourlyTemps.getOrElse(i) { current.temperature2m ?: 20.0 },
                    precipitationProbability = hourlyProbs.getOrElse(i) { 0 },
                    weatherCode = hourlyCodes.getOrElse(i) { current.weatherCode ?: 3 },
                    isDay = isHourDay,
                )
            }

        val dailyDates = daily?.time.orEmpty()
        val dailyMax = daily?.temperature2mMax.orEmpty()
        val dailyMin = daily?.temperature2mMin.orEmpty()
        val dailyProbs = daily?.precipitationProbabilityMax.orEmpty()
        val dailyCodes = daily?.weatherCode.orEmpty()
        val dailyItems = dailyDates.indices.take(10).map { i ->
            DailyForecastItem(
                dateIso = dailyDates[i],
                tempMaxC = dailyMax.getOrElse(i) { current.temperature2m ?: 20.0 },
                tempMinC = dailyMin.getOrElse(i) { current.temperature2m ?: 20.0 },
                precipitationProbabilityMax = dailyProbs.getOrElse(i) { 0 },
                weatherCode = dailyCodes.getOrElse(i) { current.weatherCode ?: 3 },
            )
        }

        val tempNow = current.temperature2m ?: 20.0
        val visibilityMeters = current.visibility ?: 16_000.0

        return WeatherSnapshot(
            cityName = cityName,
            temperatureC = tempNow,
            apparentTemperatureC = current.apparentTemperature ?: tempNow,
            humidityPercent = current.relativeHumidity2m ?: 50,
            dewPointC = current.dewPoint2m ?: (tempNow - 4.0),
            windSpeedKmh = current.windSpeed10m ?: 0.0,
            windDirectionDegrees = current.windDirection10m ?: 0,
            pressureHpa = current.pressureMsl ?: 1013.0,
            visibilityKm = visibilityMeters / 1000.0,
            precipitationTodayMm = daily?.precipitationSum?.firstOrNull() ?: (current.precipitation ?: 0.0),
            precipitationProbabilityMax = dailyProbs.firstOrNull() ?: 0,
            uvIndexMax = daily?.uvIndexMax?.firstOrNull() ?: 0.0,
            tempMaxC = dailyMax.firstOrNull() ?: tempNow,
            tempMinC = dailyMin.firstOrNull() ?: tempNow,
            weatherCode = current.weatherCode ?: 3,
            isDay = (current.isDay ?: 1) == 1,
            cloudCoverPercent = current.cloudCover ?: 0,
            localTimeIso = currentTime,
            sunriseIso = sunrise,
            sunsetIso = sunset,
            updatedAtMillis = System.currentTimeMillis(),
            hourlyForecast = hourlyItems,
            dailyForecast = dailyItems,
        )
    }

    override suspend fun searchCities(query: String): List<CitySearchResult> {
        val response = geocodingApi.searchCities(name = query)
        return response.results.orEmpty().map { dto ->
            CitySearchResult(
                id = dto.id,
                name = dto.name,
                admin1 = dto.admin1,
                country = dto.country,
                latitude = dto.latitude,
                longitude = dto.longitude,
            )
        }
    }
}
