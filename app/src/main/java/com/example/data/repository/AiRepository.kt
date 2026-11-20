package com.example.data.repository

import com.example.BuildConfig
import com.example.data.remote.GeminiApi
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiGenerateContentRequest
import com.example.data.remote.GeminiGenerationConfig
import com.example.data.remote.GeminiPart
import com.example.data.remote.GeminiSchema
import com.example.data.remote.GeminiSchemaProperty
import com.example.data.remote.GeminiStructuredWeatherReply
import com.example.model.HourlyRainBar
import com.example.model.WeatherSnapshot
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import kotlin.math.roundToInt

data class AiReply(
    val text: String,
    val rainBars: List<HourlyRainBar> = emptyList(),
)

sealed interface AiOutcome {
    data class Success(val reply: AiReply) : AiOutcome
    data class MissingApiKey(val message: String) : AiOutcome
    data class Error(val message: String) : AiOutcome
}

interface AiRepository {
    suspend fun generateSummary(snapshot: WeatherSnapshot): AiOutcome
    suspend fun askQuestion(question: String, snapshot: WeatherSnapshot): AiOutcome
}

class GeminiAiRepository(
    private val geminiApi: GeminiApi,
    private val moshi: Moshi,
    private val apiKeyProvider: () -> String = { BuildConfig.GEMINI_API_KEY },
) : AiRepository {

    private val replyAdapter = moshi.adapter(GeminiStructuredWeatherReply::class.java)

    private val systemPrompt = """
        Eres un asistente meteorológico práctico y directo que responde en español de México.
        Usa exclusivamente los datos del clima proporcionados en el contexto.
        Tienes prohibido inventar cifras, horas o condiciones que no aparezcan en el contexto.
        Responde en texto plano (sin Markdown, sin asteriscos, sin emojis) en 1 o 2 frases breves y útiles.
        Activa includeRainBars=true únicamente si la pregunta o respuesta trata sobre lluvia, paraguas o a qué hora deja de llover.
    """.trimIndent()

    private val responseSchema = GeminiSchema(
        type = "OBJECT",
        properties = mapOf(
            "answer" to GeminiSchemaProperty(
                type = "STRING",
                description = "Respuesta breve de 1 a 2 frases en español de México, sin Markdown ni emojis.",
            ),
            "includeRainBars" to GeminiSchemaProperty(
                type = "BOOLEAN",
                description = "Verdadero solo si la respuesta se beneficia de mostrar la gráfica de lluvia por horas.",
            ),
        ),
        required = listOf("answer", "includeRainBars"),
    )

    override suspend fun generateSummary(snapshot: WeatherSnapshot): AiOutcome {
        val prompt = buildString {
            appendLine(buildWeatherContext(snapshot))
            appendLine()
            append("Genera un resumen breve de 1 o 2 frases sobre el clima actual y qué esperar hoy, con un consejo práctico.")
        }
        return executeGemini(prompt = prompt, snapshot = snapshot, allowRainBars = false)
    }

    override suspend fun askQuestion(question: String, snapshot: WeatherSnapshot): AiOutcome {
        val prompt = buildString {
            appendLine(buildWeatherContext(snapshot))
            appendLine()
            append("Pregunta del usuario: ${question.trim()}")
        }
        return executeGemini(prompt = prompt, snapshot = snapshot, allowRainBars = true)
    }

    private suspend fun executeGemini(
        prompt: String,
        snapshot: WeatherSnapshot,
        allowRainBars: Boolean,
    ): AiOutcome = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isBlank() || apiKey == PLACEHOLDER_KEY) {
            return@withContext AiOutcome.MissingApiKey(
                "Configura GEMINI_API_KEY en el panel de Secretos para activar la IA.",
            )
        }

        try {
            val response = withTimeout(30_000L) {
                geminiApi.generateContent(
                    apiKey = apiKey,
                    request = GeminiGenerateContentRequest(
                        systemInstruction = GeminiContent(
                            parts = listOf(GeminiPart(text = systemPrompt)),
                        ),
                        contents = listOf(
                            GeminiContent(
                                role = "user",
                                parts = listOf(GeminiPart(text = prompt)),
                            ),
                        ),
                        generationConfig = GeminiGenerationConfig(
                            temperature = 0.4,
                            maxOutputTokens = 220,
                            responseMimeType = "application/json",
                            responseSchema = responseSchema,
                        ),
                    ),
                )
            }

            val rawJson = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()

            if (rawJson.isNullOrBlank()) {
                return@withContext AiOutcome.Error("La IA no devolvió respuesta. Intenta de nuevo.")
            }

            val parsed = runCatching { replyAdapter.fromJson(rawJson) }.getOrNull()
            val answerText = sanitizePlainText(parsed?.answer ?: rawJson)
            val bars = if (allowRainBars && parsed?.includeRainBars == true) {
                buildRainBars(snapshot)
            } else {
                emptyList()
            }

            AiOutcome.Success(
                AiReply(
                    text = answerText,
                    rainBars = bars,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            val message = when (e.code()) {
                401, 403 -> "La clave GEMINI_API_KEY no es válida o no tiene permisos."
                429 -> "Se alcanzó el límite temporal de consultas a Gemini. Intenta en unos segundos."
                else -> "Error al consultar Gemini (${e.code()})."
            }
            AiOutcome.Error(message)
        } catch (_: Exception) {
            AiOutcome.Error("No se pudo obtener respuesta de la IA en este momento.")
        }
    }

    fun buildWeatherContext(snapshot: WeatherSnapshot): String = buildString {
        appendLine("Ciudad: ${snapshot.cityName}")
        appendLine("Hora local actual: ${snapshot.localTimeIso}")
        appendLine("Condición: ${snapshot.conditionLabel}")
        appendLine("Temperatura: ${snapshot.temperatureC.roundToInt()}°C (sensación ${snapshot.apparentTemperatureC.roundToInt()}°C)")
        appendLine("Máxima hoy: ${snapshot.tempMaxC.roundToInt()}°C, Mínima hoy: ${snapshot.tempMinC.roundToInt()}°C")
        appendLine("Humedad: ${snapshot.humidityPercent}%, Punto de rocío: ${snapshot.dewPointC.roundToInt()}°C")
        appendLine("Viento: ${snapshot.windSpeedKmh.roundToInt()} km/h (${snapshot.windDirectionDegrees}°)")
        appendLine("Probabilidad máxima de lluvia hoy: ${snapshot.precipitationProbabilityMax}%, Lluvia acumulada hoy: ${snapshot.precipitationTodayMm.roundToInt()} mm")
        appendLine("Índice UV máximo: ${snapshot.uvIndexMax.roundToInt()}, Visibilidad: ${snapshot.visibilityKm.roundToInt()} km, Presión: ${snapshot.pressureHpa.roundToInt()} hPa")
        if (snapshot.hourlyForecast.isNotEmpty()) {
            append("Próximas horas: ")
            appendLine(
                snapshot.hourlyForecast.take(8).joinToString(" | ") { item ->
                    "${formatHourShort(item.timeIso)} ${item.temperatureC.roundToInt()}°C (${item.precipitationProbability}% lluvia)"
                },
            )
        }
        if (snapshot.dailyForecast.isNotEmpty()) {
            append("Próximos días: ")
            appendLine(
                snapshot.dailyForecast.take(5).joinToString(" | ") { day ->
                    "${day.dateIso}: máx ${day.tempMaxC.roundToInt()}°C mín ${day.tempMinC.roundToInt()}°C (${day.precipitationProbabilityMax}% lluvia)"
                },
            )
        }
    }

    fun sanitizePlainText(text: String): String {
        return text
            .replace("**", "")
            .replace("__", "")
            .replace("`", "")
            .trim()
    }

    private fun buildRainBars(snapshot: WeatherSnapshot): List<HourlyRainBar> {
        return snapshot.hourlyForecast.take(4).map { item ->
            HourlyRainBar(
                hourLabel = formatHourShort(item.timeIso),
                probabilityPercent = item.precipitationProbability,
            )
        }
    }

    private fun formatHourShort(timeIso: String): String {
        val hour24 = timeIso.substringAfter("T", "").take(2).toIntOrNull() ?: return "--"
        return when {
            hour24 == 0 -> "12 AM"
            hour24 < 12 -> "$hour24 AM"
            hour24 == 12 -> "12 PM"
            else -> "${hour24 - 12} PM"
        }
    }

    companion object {
        const val PLACEHOLDER_KEY = "PON_AQUI_TU_API_KEY"
    }
}
