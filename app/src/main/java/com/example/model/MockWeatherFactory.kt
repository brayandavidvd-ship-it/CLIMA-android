package com.example.model

/**
 * Generates sample weather payloads strictly for Debug Test Mode (BuildConfig.DEBUG).
 */
object MockWeatherFactory {

    fun createForState(
        state: WeatherVisualState,
        cityName: String = "Ciudad de México",
        nowMillis: Long = System.currentTimeMillis(),
    ): WeatherSnapshot {
        val base = when (state) {
            WeatherVisualState.STORM -> WeatherSnapshot(
                cityName = cityName,
                temperatureC = 31.0,
                apparentTemperatureC = 37.0,
                humidityPercent = 76,
                dewPointC = 26.0,
                windSpeedKmh = 11.0,
                windDirectionDegrees = 270, // Del oeste
                pressureHpa = 996.0,
                visibilityKm = 16.0,
                precipitationTodayMm = 23.0,
                precipitationProbabilityMax = 85,
                uvIndexMax = 2.0,
                tempMaxC = 31.0,
                tempMinC = 28.0,
                weatherCode = 95,
                isDay = true,
                cloudCoverPercent = 96,
                localTimeIso = "2026-11-19T14:20",
                sunriseIso = "2026-11-19T06:48",
                sunsetIso = "2026-11-19T17:52",
                updatedAtMillis = nowMillis,
            )

            WeatherVisualState.SUNNY -> WeatherSnapshot(
                cityName = cityName,
                temperatureC = 28.0,
                apparentTemperatureC = 29.0,
                humidityPercent = 34,
                dewPointC = 11.0,
                windSpeedKmh = 10.0,
                windDirectionDegrees = 45, // Del noreste
                pressureHpa = 1019.0,
                visibilityKm = 24.0,
                precipitationTodayMm = 0.0,
                precipitationProbabilityMax = 5,
                uvIndexMax = 8.0,
                tempMaxC = 30.0,
                tempMinC = 16.0,
                weatherCode = 0,
                isDay = true,
                cloudCoverPercent = 6,
                localTimeIso = "2026-11-19T13:30",
                sunriseIso = "2026-11-19T06:48",
                sunsetIso = "2026-11-19T17:52",
                updatedAtMillis = nowMillis,
            )

            WeatherVisualState.CLOUDY -> WeatherSnapshot(
                cityName = cityName,
                temperatureC = 20.0,
                apparentTemperatureC = 19.0,
                humidityPercent = 60,
                dewPointC = 12.0,
                windSpeedKmh = 15.0,
                windDirectionDegrees = 180, // Del sur
                pressureHpa = 1014.0,
                visibilityKm = 18.0,
                precipitationTodayMm = 0.0,
                precipitationProbabilityMax = 25,
                uvIndexMax = 4.0,
                tempMaxC = 22.0,
                tempMinC = 13.0,
                weatherCode = 3,
                isDay = true,
                cloudCoverPercent = 88,
                localTimeIso = "2026-11-19T15:00",
                sunriseIso = "2026-11-19T06:48",
                sunsetIso = "2026-11-19T17:52",
                updatedAtMillis = nowMillis,
            )

            WeatherVisualState.RAIN -> WeatherSnapshot(
                cityName = cityName,
                temperatureC = 18.0,
                apparentTemperatureC = 17.0,
                humidityPercent = 86,
                dewPointC = 15.0,
                windSpeedKmh = 18.0,
                windDirectionDegrees = 315, // Del noroeste
                pressureHpa = 1007.0,
                visibilityKm = 10.0,
                precipitationTodayMm = 14.0,
                precipitationProbabilityMax = 80,
                uvIndexMax = 2.0,
                tempMaxC = 20.0,
                tempMinC = 13.0,
                weatherCode = 63,
                isDay = true,
                cloudCoverPercent = 94,
                localTimeIso = "2026-11-19T16:15",
                sunriseIso = "2026-11-19T06:48",
                sunsetIso = "2026-11-19T17:52",
                updatedAtMillis = nowMillis,
            )

            WeatherVisualState.FOG -> WeatherSnapshot(
                cityName = cityName,
                temperatureC = 12.0,
                apparentTemperatureC = 11.0,
                humidityPercent = 94,
                dewPointC = 11.0,
                windSpeedKmh = 6.0,
                windDirectionDegrees = 90, // Del este
                pressureHpa = 1016.0,
                visibilityKm = 2.0,
                precipitationTodayMm = 0.0,
                precipitationProbabilityMax = 15,
                uvIndexMax = 1.0,
                tempMaxC = 17.0,
                tempMinC = 9.0,
                weatherCode = 45,
                isDay = true,
                cloudCoverPercent = 84,
                localTimeIso = "2026-11-19T07:30",
                sunriseIso = "2026-11-19T06:48",
                sunsetIso = "2026-11-19T17:52",
                updatedAtMillis = nowMillis,
            )

            WeatherVisualState.NIGHT -> WeatherSnapshot(
                cityName = cityName,
                temperatureC = 15.0,
                apparentTemperatureC = 14.0,
                humidityPercent = 48,
                dewPointC = 5.0,
                windSpeedKmh = 8.0,
                windDirectionDegrees = 0, // Del norte
                pressureHpa = 1021.0,
                visibilityKm = 22.0,
                precipitationTodayMm = 0.0,
                precipitationProbabilityMax = 5,
                uvIndexMax = 0.0,
                tempMaxC = 25.0,
                tempMinC = 12.0,
                weatherCode = 0,
                isDay = false,
                cloudCoverPercent = 4,
                localTimeIso = "2026-11-19T21:40",
                sunriseIso = "2026-11-19T06:48",
                sunsetIso = "2026-11-19T17:52",
                updatedAtMillis = nowMillis,
            )

            WeatherVisualState.SNOW -> WeatherSnapshot(
                cityName = cityName,
                temperatureC = 1.0,
                apparentTemperatureC = -2.0,
                humidityPercent = 82,
                dewPointC = -1.0,
                windSpeedKmh = 14.0,
                windDirectionDegrees = 225, // Del suroeste
                pressureHpa = 1010.0,
                visibilityKm = 5.0,
                precipitationTodayMm = 6.0,
                precipitationProbabilityMax = 75,
                uvIndexMax = 2.0,
                tempMaxC = 3.0,
                tempMinC = -3.0,
                weatherCode = 73,
                isDay = true,
                cloudCoverPercent = 92,
                localTimeIso = "2026-11-19T10:30",
                sunriseIso = "2026-11-19T06:48",
                sunsetIso = "2026-11-19T17:52",
                updatedAtMillis = nowMillis,
            )
        }

        val hourlyList = listOf(
            HourlyForecastItem("2026-11-19T14:00", base.temperatureC, base.precipitationProbabilityMax.coerceAtMost(75), base.weatherCode, !base.isLocalNight),
            HourlyForecastItem("2026-11-19T15:00", base.temperatureC - 1.0, if (state == WeatherVisualState.STORM) 70 else base.precipitationProbabilityMax, base.weatherCode, !base.isLocalNight),
            HourlyForecastItem("2026-11-19T16:00", base.temperatureC - 1.0, if (state == WeatherVisualState.STORM) 40 else (base.precipitationProbabilityMax - 15).coerceAtLeast(0), base.weatherCode, !base.isLocalNight),
            HourlyForecastItem("2026-11-19T17:00", base.temperatureC - 2.0, if (state == WeatherVisualState.STORM) 35 else (base.precipitationProbabilityMax - 20).coerceAtLeast(0), if (state == WeatherVisualState.STORM) 61 else base.weatherCode, !base.isLocalNight),
            HourlyForecastItem("2026-11-19T18:00", base.temperatureC - 2.0, if (state == WeatherVisualState.STORM) 25 else (base.precipitationProbabilityMax - 25).coerceAtLeast(0), if (state == WeatherVisualState.STORM) 3 else base.weatherCode, false),
            HourlyForecastItem("2026-11-19T19:00", base.temperatureC - 3.0, 10, 2, false),
            HourlyForecastItem("2026-11-19T20:00", base.temperatureC - 3.0, 20, 2, false),
            HourlyForecastItem("2026-11-19T21:00", base.temperatureC - 4.0, 15, 0, false),
            HourlyForecastItem("2026-11-19T22:00", base.temperatureC - 4.0, 10, 0, false),
            HourlyForecastItem("2026-11-19T23:00", base.temperatureC - 5.0, 10, 0, false),
            HourlyForecastItem("2026-11-20T00:00", base.temperatureC - 5.0, 5, 0, false),
            HourlyForecastItem("2026-11-20T01:00", base.temperatureC - 6.0, 5, 0, false),
        )

        val dailyList = (0 until 10).map { offset ->
            val dayNum = 19 + offset
            val prob = when (offset) {
                0 -> base.precipitationProbabilityMax
                1 -> 35
                2 -> 25
                else -> (15 + (offset * 7) % 30)
            }
            val code = when {
                offset == 0 -> base.weatherCode
                prob >= 50 -> 61
                prob >= 30 -> 3
                else -> 1
            }
            DailyForecastItem(
                dateIso = "2026-11-%02d".format(dayNum),
                tempMaxC = base.tempMaxC + (offset % 3) - 1.0,
                tempMinC = base.tempMinC + (offset % 2) - 1.0,
                precipitationProbabilityMax = prob,
                weatherCode = code,
            )
        }

        return base.copy(hourlyForecast = hourlyList, dailyForecast = dailyList)
    }
}
