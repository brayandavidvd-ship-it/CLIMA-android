package com.example.model

import kotlin.math.roundToInt

/**
 * Generates isolated mock [WeatherSnapshot] payloads for Debug Test Mode
 * without triggering network calls or modifying persisted cache.
 */
object MockWeatherFactory {

    fun createForState(
        state: WeatherVisualState,
        cityName: String = "Ciudad de México",
        nowMillis: Long = System.currentTimeMillis(),
    ): WeatherSnapshot {
        return when (state) {
            WeatherVisualState.SUNNY -> WeatherSnapshot(
                cityName = cityName,
                temperatureC = 29.0,
                apparentTemperatureC = 30.0,
                humidityPercent = 32,
                windSpeedKmh = 11.0,
                pressureHpa = 1018.0,
                precipitationMm = 0.0,
                precipitationProbabilityMax = 5,
                uvIndexMax = 8.4,
                tempMaxC = 31.0,
                tempMinC = 17.0,
                weatherCode = 0,
                isDay = true,
                cloudCoverPercent = 8,
                localTimeIso = "2026-11-13T14:00",
                sunriseIso = "2026-11-13T06:42",
                sunsetIso = "2026-11-13T17:58",
                updatedAtMillis = nowMillis,
            )

            WeatherVisualState.CLEAR_NIGHT -> WeatherSnapshot(
                cityName = cityName,
                temperatureC = 15.0,
                apparentTemperatureC = 14.0,
                humidityPercent = 48,
                windSpeedKmh = 8.0,
                pressureHpa = 1020.0,
                precipitationMm = 0.0,
                precipitationProbabilityMax = 0,
                uvIndexMax = 0.0,
                tempMaxC = 25.0,
                tempMinC = 12.0,
                weatherCode = 0,
                isDay = false,
                cloudCoverPercent = 5,
                localTimeIso = "2026-11-13T22:15",
                sunriseIso = "2026-11-13T06:42",
                sunsetIso = "2026-11-13T17:58",
                updatedAtMillis = nowMillis,
            )

            WeatherVisualState.PARTLY_CLOUDY -> WeatherSnapshot(
                cityName = cityName,
                temperatureC = 23.0,
                apparentTemperatureC = 23.0,
                humidityPercent = 45,
                windSpeedKmh = 14.0,
                precipitationMm = 0.0,
                precipitationProbabilityMax = 20,
                uvIndexMax = 5.2,
                tempMaxC = 25.0,
                tempMinC = 14.0,
                weatherCode = 2,
                isDay = true,
                cloudCoverPercent = 45,
                localTimeIso = "2026-11-13T13:10",
                sunriseIso = "2026-11-13T06:42",
                sunsetIso = "2026-11-13T17:58",
                updatedAtMillis = nowMillis,
            )

            WeatherVisualState.CLOUDY -> WeatherSnapshot(
                cityName = cityName,
                temperatureC = 19.0,
                apparentTemperatureC = 18.0,
                humidityPercent = 62,
                windSpeedKmh = 16.0,
                pressureHpa = 1014.0,
                precipitationMm = 0.0,
                precipitationProbabilityMax = 35,
                uvIndexMax = 3.1,
                tempMaxC = 21.0,
                tempMinC = 13.0,
                weatherCode = 3,
                isDay = true,
                cloudCoverPercent = 92,
                localTimeIso = "2026-11-13T16:00",
                sunriseIso = "2026-11-13T06:42",
                sunsetIso = "2026-11-13T17:58",
                updatedAtMillis = nowMillis,
            )

            WeatherVisualState.FOG -> WeatherSnapshot(
                cityName = cityName,
                temperatureC = 12.0,
                apparentTemperatureC = 11.0,
                humidityPercent = 93,
                windSpeedKmh = 6.0,
                precipitationMm = 0.0,
                precipitationProbabilityMax = 15,
                uvIndexMax = 1.5,
                tempMaxC = 17.0,
                tempMinC = 9.0,
                weatherCode = 45,
                isDay = true,
                cloudCoverPercent = 85,
                localTimeIso = "2026-11-13T07:30",
                sunriseIso = "2026-11-13T06:42",
                sunsetIso = "2026-11-13T17:58",
                updatedAtMillis = nowMillis,
            )

            WeatherVisualState.DRIZZLE -> WeatherSnapshot(
                cityName = cityName,
                temperatureC = 17.0,
                apparentTemperatureC = 16.0,
                humidityPercent = 84,
                windSpeedKmh = 13.0,
                precipitationMm = 0.6,
                precipitationProbabilityMax = 65,
                uvIndexMax = 2.0,
                tempMaxC = 20.0,
                tempMinC = 13.0,
                weatherCode = 53,
                isDay = true,
                cloudCoverPercent = 88,
                localTimeIso = "2026-11-13T15:20",
                sunriseIso = "2026-11-13T06:42",
                sunsetIso = "2026-11-13T17:58",
                updatedAtMillis = nowMillis,
            )

            WeatherVisualState.RAIN -> WeatherSnapshot(
                cityName = cityName,
                temperatureC = 16.0,
                apparentTemperatureC = 15.0,
                humidityPercent = 90,
                windSpeedKmh = 21.0,
                pressureHpa = 1008.0,
                precipitationMm = 4.2,
                precipitationProbabilityMax = 88,
                uvIndexMax = 1.4,
                tempMaxC = 19.0,
                tempMinC = 12.0,
                weatherCode = 63,
                isDay = true,
                cloudCoverPercent = 96,
                localTimeIso = "2026-11-13T17:10",
                sunriseIso = "2026-11-13T06:42",
                sunsetIso = "2026-11-13T17:58",
                updatedAtMillis = nowMillis,
            )

            WeatherVisualState.STORM -> WeatherSnapshot(
                cityName = cityName,
                temperatureC = 18.0,
                apparentTemperatureC = 17.0,
                humidityPercent = 94,
                windSpeedKmh = 34.0,
                pressureHpa = 1003.0,
                precipitationMm = 12.5,
                precipitationProbabilityMax = 95,
                uvIndexMax = 1.0,
                tempMaxC = 22.0,
                tempMinC = 14.0,
                weatherCode = 95,
                isDay = true,
                cloudCoverPercent = 100,
                localTimeIso = "2026-11-13T18:05",
                sunriseIso = "2026-11-13T06:42",
                sunsetIso = "2026-11-13T17:58",
                updatedAtMillis = nowMillis,
            )

            WeatherVisualState.SNOW -> WeatherSnapshot(
                cityName = cityName,
                temperatureC = 1.0,
                apparentTemperatureC = -2.0,
                humidityPercent = 86,
                windSpeedKmh = 15.0,
                precipitationMm = 2.1,
                precipitationProbabilityMax = 80,
                uvIndexMax = 1.8,
                tempMaxC = 3.0,
                tempMinC = -3.0,
                weatherCode = 73,
                isDay = true,
                cloudCoverPercent = 95,
                localTimeIso = "2026-11-13T10:30",
                sunriseIso = "2026-11-13T06:42",
                sunsetIso = "2026-11-13T17:58",
                updatedAtMillis = nowMillis,
            )
        }.let { base ->
            base.copy(
                hourlyForecast = listOf(
                    HourlyForecastItem("2026-11-13T15:00", base.temperatureC, base.precipitationProbabilityMax, base.weatherCode),
                    HourlyForecastItem("2026-11-13T17:00", base.temperatureC - 1.5, (base.precipitationProbabilityMax + 10).coerceAtMost(100), base.weatherCode),
                    HourlyForecastItem("2026-11-13T20:00", base.temperatureC - 4.0, base.precipitationProbabilityMax, base.weatherCode),
                ),
                dailyForecast = listOf(
                    DailyForecastItem("2026-11-13", base.tempMaxC, base.tempMinC, base.precipitationProbabilityMax),
                    DailyForecastItem("2026-11-14", base.tempMaxC + 1.0, base.tempMinC + 1.0, (base.precipitationProbabilityMax / 2)),
                    DailyForecastItem("2026-11-15", base.tempMaxC + 2.0, base.tempMinC + 1.0, 15),
                ),
            )
        }
    }

    fun simulatedAiSummary(snapshot: WeatherSnapshot): String {
        val temp = snapshot.temperatureC.roundToInt()
        val max = snapshot.tempMaxC.roundToInt()
        val min = snapshot.tempMinC.roundToInt()
        return when (snapshot.visualState) {
            WeatherVisualState.SUNNY ->
                "Tarde muy soleada en ${snapshot.cityName} con $temp° (máx $max°). Usa bloqueador y mantente bien hidratado."
            WeatherVisualState.CLEAR_NIGHT ->
                "Noche despejada y tranquila en ${snapshot.cityName} a $temp°, bajando hasta $min°. Ideal para caminar sin prisas."
            WeatherVisualState.PARTLY_CLOUDY ->
                "Cielo parcialmente nublado con $temp° agradables en ${snapshot.cityName}. Se espera máxima de $max° y mínima de $min°."
            WeatherVisualState.CLOUDY ->
                "Día nublado en ${snapshot.cityName} con $temp° y ambiente templado. Lleva una capa ligera por si refresca a $min°."
            WeatherVisualState.FOG ->
                "Niebla densa y ambiente fresco de $temp° en ${snapshot.cityName}. Maneja con luces bajas y toma tiempo extra."
            WeatherVisualState.DRIZZLE ->
                "Llovizna ligera en ${snapshot.cityName} con $temp° y humedad alta. Conviene salir con impermeable o paraguas."
            WeatherVisualState.RAIN ->
                "Lluvia constante en ${snapshot.cityName} a $temp° y ${snapshot.precipitationProbabilityMax}% de probabilidad. Abrígate y protege tus trayectos."
            WeatherVisualState.STORM ->
                "Tormenta eléctrica en ${snapshot.cityName} con $temp° y rachas de ${snapshot.windSpeedKmh.roundToInt()} km/h. Mejor resguárdate bajo techo."
            WeatherVisualState.SNOW ->
                "Nevando en ${snapshot.cityName} con $temp° y sensación de ${snapshot.apparentTemperatureC.roundToInt()}°. Usa abrigo térmico y calzado antiderrapante."
        }
    }
}
