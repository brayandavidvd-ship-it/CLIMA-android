package com.example.data.repository

import com.example.model.CitySearchResult
import com.example.model.DailyForecastItem
import com.example.model.HourlyForecastItem
import com.example.model.WeatherProviderOption
import com.example.model.WeatherSnapshot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

/**
 * Alternative functional [WeatherDataSource] simulating an OpenWeather provider feed
 * with stable, distinguishable metrics and full hourly/multi-day forecasts.
 */
class OpenWeatherDemoDataSource : WeatherDataSource {

    private val demoCatalog = listOf(
        CitySearchResult(1001L, "Ciudad de México", "CDMX", "México", 19.4326, -99.1332),
        CitySearchResult(1002L, "Guadalajara", "Jalisco", "México", 20.6597, -103.3496),
        CitySearchResult(1003L, "Monterrey", "Nuevo León", "México", 25.6866, -100.3161),
        CitySearchResult(1004L, "Cancún", "Quintana Roo", "México", 21.1619, -86.8515),
        CitySearchResult(1005L, "Puebla", "Puebla", "México", 19.0414, -98.2063),
        CitySearchResult(1006L, "Tijuana", "Baja California", "México", 32.5149, -117.0382),
        CitySearchResult(1007L, "Mérida", "Yucatán", "México", 20.9674, -89.5926),
        CitySearchResult(1008L, "Madrid", "Comunidad de Madrid", "España", 40.4168, -3.7038),
    )

    override suspend fun fetchCurrentWeather(
        latitude: Double,
        longitude: Double,
        cityName: String,
    ): WeatherSnapshot {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val isDay = hour in 6..19
        val seed = abs(cityName.hashCode() % 3)

        val (baseTemp, code, rainProb, uv, humidity, wind, pressure) = when (seed) {
            0 -> DemoSeed(22.0, 2, 35, 6.0, 54, 15.0, 1015.0)
            1 -> DemoSeed(18.0, 53, 70, 3.0, 79, 19.0, 1009.0)
            else -> DemoSeed(27.0, 0, 10, 8.0, 38, 12.0, 1018.0)
        }

        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val hourFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:00", Locale.US)
        val todayStr = dateFmt.format(now.time)

        val hourlyItems = (0 until 18).map { offset ->
            val cal = (now.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, offset) }
            val h = cal.get(Calendar.HOUR_OF_DAY)
            val tempCurve = when {
                h in 13..16 -> baseTemp + 3.0
                h in 0..6 -> baseTemp - 4.0
                else -> baseTemp
            }
            val afternoonRainBoost = if (h in 16..20) (rainProb + 25).coerceAtMost(90) else rainProb
            val hourlyCode = if (afternoonRainBoost >= 60) 61 else code
            HourlyForecastItem(
                timeIso = hourFmt.format(cal.time),
                temperatureC = tempCurve,
                precipitationProbability = afternoonRainBoost,
                weatherCode = hourlyCode,
            )
        }

        val dailyItems = (0 until 4).map { dayOffset ->
            val cal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
            DailyForecastItem(
                dateIso = dateFmt.format(cal.time),
                tempMaxC = baseTemp + 4.0 - dayOffset * 0.5,
                tempMinC = baseTemp - 6.0 - (dayOffset % 2),
                precipitationProbabilityMax = (rainProb + dayOffset * 12).coerceAtMost(85),
            )
        }

        return WeatherSnapshot(
            cityName = cityName,
            temperatureC = baseTemp,
            apparentTemperatureC = baseTemp + 0.5,
            humidityPercent = humidity,
            windSpeedKmh = wind,
            pressureHpa = pressure,
            precipitationMm = if (code >= 50) 0.8 else 0.0,
            precipitationProbabilityMax = rainProb,
            uvIndexMax = if (isDay) uv else 0.0,
            tempMaxC = baseTemp + 4.0,
            tempMinC = baseTemp - 6.0,
            weatherCode = code,
            isDay = isDay,
            cloudCoverPercent = if (code == 0) 10 else 58,
            localTimeIso = hourFmt.format(now.time),
            sunriseIso = "${todayStr}T06:44",
            sunsetIso = "${todayStr}T18:01",
            updatedAtMillis = System.currentTimeMillis(),
            providerId = WeatherProviderOption.OPEN_WEATHER_DEMO.id,
            hourlyForecast = hourlyItems,
            dailyForecast = dailyItems,
        )
    }

    override suspend fun searchCities(query: String): List<CitySearchResult> {
        val q = query.trim().lowercase()
        val matches = demoCatalog.filter {
            it.name.lowercase().contains(q) || (it.country?.lowercase()?.contains(q) == true)
        }
        return matches.ifEmpty {
            listOf(
                CitySearchResult(
                    id = q.hashCode().toLong(),
                    name = query.trim(),
                    admin1 = "Demo",
                    country = "OpenWeather",
                    latitude = 19.4326,
                    longitude = -99.1332,
                ),
            )
        }
    }

    private data class DemoSeed(
        val temp: Double,
        val code: Int,
        val rainProb: Int,
        val uv: Double,
        val humidity: Int,
        val wind: Double,
        val pressure: Double,
    )
}
