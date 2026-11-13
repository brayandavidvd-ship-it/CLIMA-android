package com.example.domain

import com.example.model.LocalRecommendation
import com.example.model.RecommendationIconType
import com.example.model.WeatherChipType
import com.example.model.WeatherSnapshot
import com.example.model.WeatherVisualState
import kotlin.math.roundToInt

/**
 * Deterministic rule engine for offline/non-AI recommendations and fallback answers.
 */
object LocalRecommendationsEngine {

    private const val MAX_RECOMMENDATIONS = 2
    private const val MAX_SUBTITLE_LENGTH = 35

    fun evaluate(snapshot: WeatherSnapshot): List<LocalRecommendation> {
        val results = mutableListOf<LocalRecommendation>()
        val isActiveRain = snapshot.visualState in setOf(
            WeatherVisualState.DRIZZLE,
            WeatherVisualState.RAIN,
            WeatherVisualState.STORM,
        )

        // 1. UV >= 6 -> "Protector solar"
        if (snapshot.uvIndexMax >= 6.0) {
            results += LocalRecommendation(
                id = "uv_protection",
                title = "Protector solar",
                subtitle = fitSubtitle("UV alto (${snapshot.uvIndexMax.roundToInt()}), aplica FPS 50+."),
                iconType = RecommendationIconType.SUNSCREEN,
            )
        }

        // 2. Temperatura >= 28° -> "Hidrátate"
        if (snapshot.temperatureC >= 28.0) {
            results += LocalRecommendation(
                id = "hydrate",
                title = "Hidrátate",
                subtitle = fitSubtitle("Calor de ${snapshot.temperatureC.roundToInt()}°, toma agua seguido."),
                iconType = RecommendationIconType.HYDRATE,
            )
        }

        // 3. Llovizna, lluvia o tormenta -> "Lleva paraguas"
        if (isActiveRain) {
            results += LocalRecommendation(
                id = "bring_umbrella",
                title = "Lleva paraguas",
                subtitle = fitSubtitle("Hay precipitación activa afuera."),
                iconType = RecommendationIconType.UMBRELLA,
            )
        }

        // 4. Probabilidad de lluvia >= 30% (sin precipitación activa) -> "Paraguas a la mano"
        if (!isActiveRain && snapshot.precipitationProbabilityMax >= 30) {
            results += LocalRecommendation(
                id = "umbrella_handy",
                title = "Paraguas a la mano",
                subtitle = fitSubtitle("Hasta ${snapshot.precipitationProbabilityMax}% de lluvia hoy."),
                iconType = RecommendationIconType.UMBRELLA,
            )
        }

        // 5. Temperatura <= 14° -> "Abrígate un poco"
        if (snapshot.temperatureC <= 14.0) {
            results += LocalRecommendation(
                id = "bundle_up",
                title = "Abrígate un poco",
                subtitle = fitSubtitle("Ambiente frío con ${snapshot.temperatureC.roundToInt()}° ahora."),
                iconType = RecommendationIconType.COAT,
            )
        }

        // 6. Niebla -> "Maneja con precaución"
        if (snapshot.visualState == WeatherVisualState.FOG) {
            results += LocalRecommendation(
                id = "fog_caution",
                title = "Maneja con precaución",
                subtitle = fitSubtitle("Visibilidad baja por niebla."),
                iconType = RecommendationIconType.FOG_DRIVE,
            )
        }

        // 7. Tormenta -> "Mejor quédate bajo techo"
        if (snapshot.visualState == WeatherVisualState.STORM) {
            results += LocalRecommendation(
                id = "storm_shelter",
                title = "Mejor quédate bajo techo",
                subtitle = fitSubtitle("Evita espacios abiertos por rayos."),
                iconType = RecommendationIconType.STORM_SHELTER,
            )
        }

        // 8. Noche despejada -> "Se ven las estrellas"
        if (snapshot.visualState == WeatherVisualState.CLEAR_NIGHT) {
            results += LocalRecommendation(
                id = "clear_stars",
                title = "Se ven las estrellas",
                subtitle = fitSubtitle("Cielo limpio ideal esta noche."),
                iconType = RecommendationIconType.STARS,
            )
        }

        return results.take(MAX_RECOMMENDATIONS)
    }

    fun buildFallbackSummary(snapshot: WeatherSnapshot): String {
        val temp = snapshot.temperatureC.roundToInt()
        val max = snapshot.tempMaxC.roundToInt()
        val min = snapshot.tempMinC.roundToInt()
        val prob = snapshot.precipitationProbabilityMax
        return when (snapshot.visualState) {
            WeatherVisualState.SUNNY ->
                "Cielo despejado en ${snapshot.cityName} con $temp° y máxima de $max°. Disfruta el día cuidándote del sol."
            WeatherVisualState.CLEAR_NIGHT ->
                "Noche despejada en ${snapshot.cityName} a $temp°, con mínima de $min°. El cielo está ideal para salir."
            WeatherVisualState.PARTLY_CLOUDY ->
                "Clima ${snapshot.conditionLabel.lowercase()} con $temp° en ${snapshot.cityName}. Máxima de $max° y mínima de $min°."
            WeatherVisualState.CLOUDY ->
                "Cielo nublado en ${snapshot.cityName} con $temp° y ${prob}% de probabilidad de lluvia. Temperatura entre $min° y $max°."
            WeatherVisualState.FOG ->
                "Presencia de niebla en ${snapshot.cityName} con $temp°. Maneja con cuidado si sales temprano."
            WeatherVisualState.DRIZZLE ->
                "Llovizna en ${snapshot.cityName} con $temp° y ${snapshot.humidityPercent}% de humedad. Conviene llevar impermeable."
            WeatherVisualState.RAIN ->
                "Lluvia activa en ${snapshot.cityName} con $temp° y $prob% de probabilidad hoy. Ten tu paraguas listo."
            WeatherVisualState.STORM ->
                "Tormenta en ${snapshot.cityName} a $temp° con viento de ${snapshot.windSpeedKmh.roundToInt()} km/h. Evita zonas descubiertas."
            WeatherVisualState.SNOW ->
                "Nieve en ${snapshot.cityName} con $temp° y mínima de $min°. Usa ropa térmica y calzado seguro."
        }
    }

    fun buildFallbackChipAnswer(
        chip: WeatherChipType,
        snapshot: WeatherSnapshot,
    ): String {
        val temp = snapshot.temperatureC.roundToInt()
        val feelsLike = snapshot.apparentTemperatureC.roundToInt()
        val min = snapshot.tempMinC.roundToInt()
        val prob = snapshot.precipitationProbabilityMax
        return when (chip) {
            WeatherChipType.WHAT_TO_WEAR -> when {
                snapshot.temperatureC <= 10.0 ->
                    "Con $temp° (sensación de $feelsLike°), usa chamarra abrigadora, suéter y pantalón cerrado."
                snapshot.temperatureC <= 18.0 ->
                    "Con $temp° y mínima de $min°, una sudadera o chamarra ligera en capas funciona muy bien."
                snapshot.temperatureC >= 28.0 ->
                    "Hace calor ($temp°), mejor usa ropa fresca, ligera y colores claros."
                else ->
                    "Temperatura agradable de $temp°; playera cómoda y una prenda ligera por si refresca a $min°."
            }

            WeatherChipType.UMBRELLA -> when {
                snapshot.visualState in setOf(
                    WeatherVisualState.DRIZZLE,
                    WeatherVisualState.RAIN,
                    WeatherVisualState.STORM,
                ) ->
                    "Sí, llévalo sin duda: hay ${snapshot.conditionLabel.lowercase()} en ${snapshot.cityName} con $prob% de probabilidad."
                prob >= 30 ->
                    "Conviene llevarlo en la mochila; hay $prob% de probabilidad de lluvia hoy."
                else ->
                    "No parece necesario por ahora, la probabilidad de lluvia es de apenas $prob%."
            }

            WeatherChipType.TODAY_PLAN -> when (snapshot.visualState) {
                WeatherVisualState.STORM, WeatherVisualState.RAIN, WeatherVisualState.SNOW ->
                    "Con ${snapshot.conditionLabel.lowercase()} y $temp°, es mejor optar por planes en interiores como café, cine o museos."
                WeatherVisualState.FOG ->
                    "Espera a que levante la niebla para manejar lejos; en la tarde llegará a ${snapshot.tempMaxC.roundToInt()}°."
                WeatherVisualState.CLEAR_NIGHT ->
                    "Noche despejada a $temp°, ideal para cenar al aire libre o dar una caminata tranquila."
                else ->
                    "Buen clima (${snapshot.conditionLabel.lowercase()}, $temp°) para actividades al aire libre; cuida el horario de mayor sol si UV es ${snapshot.uvIndexMax.roundToInt()}."
            }
        }
    }

    private fun fitSubtitle(text: String): String =
        if (text.length <= MAX_SUBTITLE_LENGTH) text else text.take(MAX_SUBTITLE_LENGTH - 1) + "…"
}
