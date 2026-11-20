package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HourlyForecastItem(
    val timeIso: String,
    val temperatureC: Double,
    val precipitationProbability: Int,
    val weatherCode: Int,
    val isDay: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class DailyForecastItem(
    val dateIso: String,
    val tempMaxC: Double,
    val tempMinC: Double,
    val precipitationProbabilityMax: Int,
    val weatherCode: Int = 3,
)

@JsonClass(generateAdapter = true)
data class WeatherSnapshot(
    val cityName: String,
    val temperatureC: Double,
    val apparentTemperatureC: Double,
    val humidityPercent: Int,
    val dewPointC: Double,
    val windSpeedKmh: Double,
    val windDirectionDegrees: Int,
    val pressureHpa: Double,
    val visibilityKm: Double,
    val precipitationTodayMm: Double,
    val precipitationProbabilityMax: Int,
    val uvIndexMax: Double,
    val tempMaxC: Double,
    val tempMinC: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val cloudCoverPercent: Int,
    val localTimeIso: String,
    val sunriseIso: String,
    val sunsetIso: String,
    val updatedAtMillis: Long,
    val hourlyForecast: List<HourlyForecastItem> = emptyList(),
    val dailyForecast: List<DailyForecastItem> = emptyList(),
) {
    /**
     * Evaluates night automatically when local clock is between sunset and sunrise,
     * falling back to Open-Meteo's is_day flag when sunrise/sunset strings are absent.
     */
    val isLocalNight: Boolean
        get() {
            val nowClock = localTimeIso.substringAfter("T", "").take(5)
            val sunriseClock = sunriseIso.substringAfter("T", "").take(5)
            val sunsetClock = sunsetIso.substringAfter("T", "").take(5)
            if (nowClock.length == 5 && sunriseClock.length == 5 && sunsetClock.length == 5) {
                return nowClock < sunriseClock || nowClock >= sunsetClock
            }
            return !isDay
        }

    val visualState: WeatherVisualState
        get() = WeatherVisualState.fromWmoCode(weatherCode, isDay = !isLocalNight)

    val conditionLabel: String
        get() = when {
            weatherCode in 1..2 && !isLocalNight -> "Parcialmente nublado"
            weatherCode in 51..57 -> "Llovizna"
            else -> visualState.conditionLabelEs
        }
}

@JsonClass(generateAdapter = true)
data class SavedLocation(
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
)

data class CitySearchResult(
    val id: Long,
    val name: String,
    val admin1: String?,
    val country: String?,
    val latitude: Double,
    val longitude: Double,
)

data class HourlyRainBar(
    val hourLabel: String,
    val probabilityPercent: Int,
)

sealed interface ChatSender {
    data object User : ChatSender
    data object Ai : ChatSender
}

data class ChatMessage(
    val id: String,
    val sender: ChatSender,
    val text: String,
    val rainBars: List<HourlyRainBar> = emptyList(),
    val isError: Boolean = false,
)
