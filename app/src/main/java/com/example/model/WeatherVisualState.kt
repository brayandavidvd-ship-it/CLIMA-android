package com.example.model

enum class WeatherVisualState(
    val displayNameEs: String,
    val conditionLabelEs: String,
) {
    SUNNY("Soleado", "Despejado"),
    CLOUDY("Nublado", "Nublado"),
    RAIN("Lluvia", "Lluvia"),
    STORM("Tormenta", "Tormentas fuertes"),
    FOG("Niebla", "Niebla"),
    NIGHT("Noche", "Noche despejada"),
    SNOW("Nieve", "Nieve");

    companion object {
        fun fromWmoCode(
            weatherCode: Int,
            isDay: Boolean,
        ): WeatherVisualState {
            if (!isDay && weatherCode in 0..2) {
                return NIGHT
            }
            return when (weatherCode) {
                0 -> SUNNY
                1, 2, 3 -> CLOUDY
                45, 48 -> FOG
                in 51..67, in 80..82 -> RAIN
                in 71..77, 85, 86 -> SNOW
                95, 96, 99 -> STORM
                else -> CLOUDY
            }
        }
    }
}
