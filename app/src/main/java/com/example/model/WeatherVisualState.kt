package com.example.model

enum class WeatherVisualState(
    val displayNameEs: String,
    val conditionLabelEs: String,
) {
    SUNNY("Soleado", "Soleado"),
    CLEAR_NIGHT("Despejado de noche", "Despejado"),
    PARTLY_CLOUDY("Parcialmente nublado", "Parcialmente nublado"),
    CLOUDY("Nublado", "Nublado"),
    FOG("Niebla", "Con niebla"),
    DRIZZLE("Llovizna", "Llovizna"),
    RAIN("Lluvia", "Lluvia"),
    STORM("Tormenta", "Tormenta eléctrica"),
    SNOW("Nieve", "Nevando");

    companion object {
        fun fromWmoCode(weatherCode: Int, isDay: Boolean): WeatherVisualState =
            when (weatherCode) {
                0 -> if (isDay) SUNNY else CLEAR_NIGHT
                1, 2 -> PARTLY_CLOUDY
                3 -> CLOUDY
                45, 48 -> FOG
                in 51..57 -> DRIZZLE
                in 61..67, in 80..82 -> RAIN
                in 71..77, 85, 86 -> SNOW
                95, 96, 99 -> STORM
                else -> CLOUDY
            }
    }
}
