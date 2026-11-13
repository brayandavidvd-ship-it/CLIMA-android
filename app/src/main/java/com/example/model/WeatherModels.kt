package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HourlyForecastItem(
    val timeIso: String,
    val temperatureC: Double,
    val precipitationProbability: Int,
    val weatherCode: Int,
)

@JsonClass(generateAdapter = true)
data class DailyForecastItem(
    val dateIso: String,
    val tempMaxC: Double,
    val tempMinC: Double,
    val precipitationProbabilityMax: Int,
)

@JsonClass(generateAdapter = true)
data class WeatherSnapshot(
    val cityName: String,
    val temperatureC: Double,
    val apparentTemperatureC: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val pressureHpa: Double = 1013.0,
    val precipitationMm: Double,
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
    val providerId: String = "open_meteo",
    val hourlyForecast: List<HourlyForecastItem> = emptyList(),
    val dailyForecast: List<DailyForecastItem> = emptyList(),
) {
    val visualState: WeatherVisualState
        get() = WeatherVisualState.fromWmoCode(weatherCode, isDay)

    val conditionLabel: String
        get() = visualState.conditionLabelEs

    /**
     * Stable key representing the weather snapshot for AI response caching.
     */
    fun cacheSignature(): String {
        val roundedTemp = kotlin.math.round(temperatureC).toInt()
        val roundedUv = kotlin.math.round(uvIndexMax).toInt()
        return "$providerId|$cityName|${visualState.name}|$roundedTemp|$precipitationProbabilityMax|$roundedUv|$isDay"
    }
}

@JsonClass(generateAdapter = true)
data class SavedLocation(
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
    val isAutoLocation: Boolean,
)

data class CitySearchResult(
    val id: Long,
    val name: String,
    val admin1: String?,
    val country: String?,
    val latitude: Double,
    val longitude: Double,
) {
    val displayTitle: String
        get() = listOfNotNull(
            name.takeIf { it.isNotBlank() },
            admin1?.takeIf { it.isNotBlank() },
            country?.takeIf { it.isNotBlank() },
        ).joinToString(", ")
}

enum class RecommendationIconType {
    SUNSCREEN,
    HYDRATE,
    UMBRELLA,
    COAT,
    FOG_DRIVE,
    STORM_SHELTER,
    STARS,
}

data class LocalRecommendation(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconType: RecommendationIconType,
)

enum class WeatherChipType(val label: String) {
    WHAT_TO_WEAR("¿Qué me pongo?"),
    UMBRELLA("¿Llevo paraguas?"),
    TODAY_PLAN("Plan para hoy"),
}

enum class GeminiModelOption(
    val modelId: String,
    val displayName: String,
) {
    FLASH_LITE("gemini-3.5-flash-lite", "gemini-3.5-flash-lite (Predeterminado)"),
    FLASH("gemini-3.5-flash", "gemini-3.5-flash");

    companion object {
        fun fromModelId(id: String?): GeminiModelOption =
            entries.firstOrNull { it.modelId == id } ?: FLASH_LITE
    }
}

enum class WeatherProviderOption(
    val id: String,
    val displayName: String,
    val shortLabel: String,
) {
    OPEN_METEO(
        id = "open_meteo",
        displayName = "Open-Meteo (Predeterminado · Gratis)",
        shortLabel = "Open-Meteo",
    ),
    OPEN_WEATHER_DEMO(
        id = "open_weather_demo",
        displayName = "OpenWeather (Demo / Simulado)",
        shortLabel = "OpenWeather (Demo)",
    );

    companion object {
        fun fromId(id: String?): WeatherProviderOption =
            entries.firstOrNull { it.id == id } ?: OPEN_METEO
    }
}
